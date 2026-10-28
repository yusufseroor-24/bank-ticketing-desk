package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.ScanStatus;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.TicketAttachments;
import com.ga.bankdesk.notifications.LiveNotifier;
import com.ga.bankdesk.repository.TicketAttachmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Scans uploaded files in the background
 * After an upload commits, the scan job goes onto a single-worker queue
 * The worker waits at least 16 seconds between calls (free tier: 4 per minute) and sends only the file's hash to VirusTotal
 * 3 or more malicious engines = MALICIOUS
 */

@Slf4j
@Service
public class AttachmentScanService {

    //3 or more engines saying "malicious"
    private static final int MALICIOUS_THRESHOLD = 3;

    private final TicketAttachmentRepository attachmentRepository;
    private final VirusTotalClient virusTotalClient;
    private final ThreadPoolTaskExecutor executor;
    private final long minIntervalMs;
    private long lastCallAt = 0;
    private final LiveNotifier liveNotifier;

    public AttachmentScanService(TicketAttachmentRepository attachmentRepository,
                                 VirusTotalClient virusTotalClient,
                                 LiveNotifier liveNotifier,
                                 @Qualifier("virusTotalExecutor") ThreadPoolTaskExecutor executor,
                                 @Value("${app.virustotal.min-interval-ms:16000}") long minIntervalMs) {
        this.attachmentRepository = attachmentRepository;
        this.virusTotalClient = virusTotalClient;
        this.liveNotifier = liveNotifier;
        this.executor = executor;
        this.minIntervalMs = minIntervalMs;
    }

    //Runs after the attachment row is committed
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAttachmentStored(AttachmentStoredEvent event) {
        enqueue(event.attachmentId());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void requeuePending() {
        attachmentRepository.findByScanStatus(ScanStatus.PENDING)
                .forEach(a -> enqueue(a.getId()));
    }

    public void enqueue(Long attachmentId) {
        try {
            executor.execute(() -> scan(attachmentId));
        } catch (TaskRejectedException e) {
            log.warn("Scan queue is full, attachment {} not queued", attachmentId);
            attachmentRepository.updateScan(attachmentId, ScanStatus.ERROR,
                    "Scan queue is full, please rescan later", LocalDateTime.now());
        }
    }

    public void rescan(Long attachmentId) {
        attachmentRepository.updateScan(attachmentId, ScanStatus.PENDING, null, null);
        enqueue(attachmentId);
    }

    void scan(Long attachmentId) {
        TicketAttachments attachment = attachmentRepository.findById(attachmentId).orElse(null);
        if (attachment == null || attachment.getSha256() == null) {
            return;
        }
        try {
            waitForSlot();
            Optional<VirusTotalClient.Stats> result = virusTotalClient.lookup(attachment.getSha256());
            ScanStatus status;
            String summary;
            if (result.isEmpty()) {
                status = ScanStatus.UNKNOWN;
                summary = "VirusTotal has never seen this file";
            } else {
                VirusTotalClient.Stats stats = result.get();
                status = classify(stats);
                summary = stats.malicious() + " of " + stats.total() + " engines flagged this file as malicious, "
                        + stats.suspicious() + " as suspicious";
            }
            attachmentRepository.updateScan(attachmentId, status, summary, LocalDateTime.now());
            log.info("Attachment {} scanned: {} ({})", attachmentId, status, summary);
            if (status == ScanStatus.MALICIOUS) {
                Ticket ticket = attachment.getTicket();
                Set<Long> w = new LinkedHashSet<>(liveNotifier.admins());
                w.addAll(liveNotifier.staffFor(ticket));
                liveNotifier.send(w, null, ticket.getId(), ticket.getTitle(), "MALICIOUS_FILE",
                        "File \"" + attachment.getFileName().replaceFirst("^[0-9a-fA-F-]{36}_", "")
                                + "\" on ticket #" + ticket.getId() + " was flagged as malicious");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            attachmentRepository.updateScan(attachmentId, ScanStatus.ERROR, "Scan was interrupted", LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Scan of attachment {} failed: {}", attachmentId, e.getMessage());
            attachmentRepository.updateScan(attachmentId, ScanStatus.ERROR, trim(e.getMessage()), LocalDateTime.now());
        }
    }

    static ScanStatus classify(VirusTotalClient.Stats stats) {
        if (stats.malicious() >= MALICIOUS_THRESHOLD) {
            return ScanStatus.MALICIOUS;
        }
        if (stats.malicious() > 0 || stats.suspicious() > 0) {
            return ScanStatus.SUSPICIOUS;
        }
        return ScanStatus.CLEAN;
    }

    private synchronized void waitForSlot() throws InterruptedException {
        long waitMs = lastCallAt + minIntervalMs - System.currentTimeMillis();
        if (waitMs > 0) {
            Thread.sleep(waitMs);
        }
        lastCallAt = System.currentTimeMillis();
    }

    private static String trim(String message) {
        if (message == null) return "Unknown error";
        return message.length() > 250 ? message.substring(0, 250) : message;
    }
}
