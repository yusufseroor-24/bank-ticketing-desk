package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.ReportStatus;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ConflictException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.model.SlaReport;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.notifications.LiveNotifier;
import com.ga.bankdesk.repository.SlaReportRepository;
import com.ga.bankdesk.repository.TicketRepository;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates the monthly SLA report as a CSV (per category and total)
 * Runs automatically at 02:00 on the 1st of each month (Asia/Bahrain), or on demand by an admin
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class SlaReportService {

    private final SlaReportRepository reportRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final LiveNotifier liveNotifier;

    @Value("${app.upload.dir}")
    private String uploadDir;

    //at 2:00 on the 1st of every month, generate the report for the month that just finished
    @Scheduled(cron = "${app.report.cron:0 0 2 1 * *}", zone = "Asia/Bahrain")
    public void generateLastMonthReport() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        try {
            SlaReport report = generate(lastMonth, null);
            notifyAdmins(report);
        } catch (ConflictException e) {
            log.info("Scheduled SLA report skipped: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Scheduled SLA report for {} failed: {}", lastMonth, e.getMessage());
        }
    }

    public YearMonth parseMonth(String text) {
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("Month must look like 2026-09");
        }
    }

    public SlaReport generate(YearMonth month, User generatedBy) {
        if (month.isAfter(YearMonth.now())) {
            throw new BusinessRuleException("Cannot generate a report for a future month");
        }
        String period = month.toString();
        Long userId = (generatedBy == null) ? null : generatedBy.getId();

        //atomic claim, only one caller
        if (reportRepository.claim(period, userId) == 0) {
            throw new ConflictException("The SLA report for " + period + " already exists or is being generated");
        }

        SlaReport report = reportRepository.findByReportPeriod(period).orElseThrow();
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Ticket> tickets = ticketRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());

            Path file = writeCsv(period, tickets, now);
            Stats total = stats(tickets, now);

            report.setFilePath(file.toString());
            report.setTotalTickets(total.created());
            report.setResolutionCompliancePercentage(percent(total.resolutionMet(), total.resolutionBreached()));
            report.setStatus(ReportStatus.COMPLETED);
            report.setGeneratedAt(LocalDateTime.now());
            SlaReport saved = reportRepository.save(report);
            log.info("SLA report {} generated: {} tickets, file {}", period, total.created(), file);
            liveNotifier.send(liveNotifier.admins(), userId, null, "SLA report " + period, "SLA_REPORT_READY",
                    "The SLA report for " + period + " is ready (" + total.created() + " tickets)");
            return saved;
        } catch (Exception e) {
            //failed month can be claimed again
            report.setStatus(ReportStatus.FAILED);
            reportRepository.save(report);
            log.error("SLA report {} failed: {}", period, e.getMessage());
            throw new IllegalStateException("Could not generate the SLA report for " + period, e);
        }
    }

    public List<SlaReport> listReports() {
        return reportRepository.findAllByOrderByReportPeriodDesc();
    }

    public Path latestReportFile() {
        SlaReport latest = reportRepository.findFirstByStatusOrderByReportPeriodDesc(ReportStatus.COMPLETED)
                .orElseThrow(() -> new ResourceNotFoundException("No SLA report has been generated yet"));
        Path file = Paths.get(latest.getFilePath());
        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("The report file for " + latest.getReportPeriod() + " is missing");
        }
        return file;
    }

    private void notifyAdmins(SlaReport report) {
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            emailService.sendSlaReportEmail(admin.getEmail(), report.getReportPeriod(),
                    report.getTotalTickets(), report.getResolutionCompliancePercentage());
        }
    }

    private Path writeCsv(String period, List<Ticket> tickets, LocalDateTime now) throws IOException {
        Path dir = Paths.get(uploadDir, "reports");
        Files.createDirectories(dir);
        Path file = dir.resolve("sla-report-" + period + ".csv");

        List<String> lines = new ArrayList<>();
        lines.add("Category,Tickets created,Resolved,Resolution SLA met,Resolution SLA breached,"
                + "Resolution compliance %,First response met,First response breached,"
                + "First response compliance %,Avg resolution hours");

        Map<String, List<Ticket>> byCategory = tickets.stream().collect(
                Collectors.groupingBy(t -> t.getCategory().getName(), TreeMap::new, Collectors.toList()));
        byCategory.forEach((name, list) -> lines.add(csvLine(name, stats(list, now))));
        lines.add(csvLine("TOTAL", stats(tickets, now)));

        Files.write(file, lines, StandardCharsets.UTF_8);
        return file;
    }

    private String csvLine(String label, Stats s) {
        return String.join(",", label,
                String.valueOf(s.created()), String.valueOf(s.resolved()),
                String.valueOf(s.resolutionMet()), String.valueOf(s.resolutionBreached()),
                pct(s.resolutionMet(), s.resolutionBreached()),
                String.valueOf(s.firstResponseMet()), String.valueOf(s.firstResponseBreached()),
                pct(s.firstResponseMet(), s.firstResponseBreached()),
                String.format(Locale.ROOT, "%.1f", s.avgResolutionHours()));
    }

    record Stats(int created, int resolved, int resolutionMet, int resolutionBreached,
                 int firstResponseMet, int firstResponseBreached, double avgResolutionHours) {}

    static Stats stats(List<Ticket> list, LocalDateTime now) {
        int resolved = 0, resMet = 0, resBreached = 0, frMet = 0, frBreached = 0;
        double totalHours = 0;

        for (Ticket t : list) {
            if (t.getResolvedAt() != null) {
                resolved++;
                totalHours += Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes() / 60.0;
            }
            if (t.getDueAt() != null) {
                if (t.getResolvedAt() != null) {
                    if (t.getResolvedAt().isAfter(t.getDueAt())) resBreached++; else resMet++;
                } else if (now.isAfter(t.getDueAt())) {
                    resBreached++;
                }
            }
            if (t.getFirstResponseDueAt() != null) {
                if (t.getFirstRespondedAt() != null) {
                    if (t.getFirstRespondedAt().isAfter(t.getFirstResponseDueAt())) frBreached++; else frMet++;
                } else if (now.isAfter(t.getFirstResponseDueAt())) {
                    frBreached++;
                }
            }
        }
        double avg = (resolved == 0) ? 0 : totalHours / resolved;
        return new Stats(list.size(), resolved, resMet, resBreached, frMet, frBreached, avg);
    }

    //Percentage of measured tickets that met the deadline
    static Double percent(int met, int breached) {
        int measured = met + breached;
        return measured == 0 ? null : 100.0 * met / measured;
    }

    private static String pct(int met, int breached) {
        Double p = percent(met, breached);
        return p == null ? "n/a" : String.format(Locale.ROOT, "%.1f", p);
    }
}
