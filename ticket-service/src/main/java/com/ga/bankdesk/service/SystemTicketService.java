package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.SystemTicketRequest;
import com.ga.bankdesk.enums.SourceOfTicket;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.notifications.LiveNotifier;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Opens a ticket on behalf of the system
 * ticket-creation part of FraudDetectionService.raiseAlert
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class SystemTicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final SlaService slaService;
    private final LiveNotifier liveNotifier;

    @Transactional
    public Long create(SystemTicketRequest request) {
        Category category = categoryRepository.findByName(request.categoryName())
                .orElseThrow(() -> new IllegalStateException("Category not found: " + request.categoryName()));

        Ticket auto = new Ticket();
        auto.setTitle(request.title());
        auto.setDescription(request.description());
        auto.setCategory(category);
        auto.setCustomerId(request.customerId());
        auto.setCreatedById(null); //created by the system
        auto.setSource(SourceOfTicket.SYSTEM);
        auto.setStatus(TicketStatus.OPEN);
        auto.setPriority(request.priority());
        slaService.applyDeadlines(auto, LocalDateTime.now());
        Ticket saved = ticketRepository.save(auto);

        Set<Long> w = new LinkedHashSet<>(liveNotifier.admins());
        w.addAll(liveNotifier.staffFor(saved));
        liveNotifier.send(w, null, saved.getId(), saved.getTitle(), request.eventType(), request.liveMessage());
        log.info("System ticket #{} opened in category {}", saved.getId(), request.categoryName());
        return saved.getId();
    }
}
