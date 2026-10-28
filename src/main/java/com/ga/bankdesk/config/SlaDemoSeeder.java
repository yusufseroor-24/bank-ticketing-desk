package com.ga.bankdesk.config;

import com.ga.bankdesk.enums.SourceOfTicket;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.TicketRepository;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeder created for the demo to showcase SLA at 75% and breached
 * No business logic just manipulation of data and utilizing SQL to show different SLA stages
 */

@Component
@Order(5)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.sla-demo", havingValue = "true")
public class SlaDemoSeeder implements CommandLineRunner {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        Category category = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("CARD_DISPUTE")).findFirst().orElseThrow();
        User customer = userRepository.findByEmail("practice24in+customer1@gmail.com").orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        seed("DEMO: SLA at 75%", "Seeded ticket that is past 75% of its SLA",
                category, customer, now.minusDays(76), now.plusDays(24));

        seed("DEMO: SLA breached", "Seeded ticket that already breached its SLA",
                category, customer, now.minusDays(10), now.minusDays(2));
    }

    private void seed(String title, String description, Category category, User customer,
                      LocalDateTime createdAt, LocalDateTime dueAt) {
        boolean exists = ticketRepository.findAll().stream().anyMatch(t -> t.getTitle().equals(title));
        if (exists) return;

        Ticket t = new Ticket();
        t.setTitle(title);
        t.setDescription(description);
        t.setCategory(category);
        t.setCustomer(customer);
        t.setCreatedBy(customer);
        t.setSource(SourceOfTicket.CUSTOMER);
        t.setPriority(TicketPriority.HIGH);
        t.setStatus(TicketStatus.OPEN);
        t.setDueAt(dueAt);
        t.setFirstResponseDueAt(createdAt.plusDays(1));
        Ticket saved = ticketRepository.save(t);

        //@CreationTimestamp overwrote createdAt, so set the real dates directly
        jdbc.update("UPDATE tickets SET created_at = ?, due_at = ?, first_response_due_at = ? WHERE id = ?",
                createdAt, dueAt, createdAt.plusDays(1), saved.getId());
    }
}