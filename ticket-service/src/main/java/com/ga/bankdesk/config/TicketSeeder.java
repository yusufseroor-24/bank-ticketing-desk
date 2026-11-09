package com.ga.bankdesk.config;


import com.ga.bankdesk.enums.SourceOfTicket;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.client.IdentityClient;
import com.ga.bankdesk.dto.UserSummary;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Profile("dev")
@RequiredArgsConstructor
@Component
@Order(5)
//seeding ticket data
public class TicketSeeder implements CommandLineRunner {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final IdentityClient identityClient;

    @Override
    public void run(String... args) {
        if (ticketRepository.count() > 0) {
            return;
        }

        //the demo users live in identity-service, so the ids are looked up there (identity must be running)
        UserSummary customer1 = identityClient.findByEmail("practice24in+customer1@gmail.com").orElseThrow();
        UserSummary customer2 = identityClient.findByEmail("practice24in+customer2@gmail.com").orElseThrow();
        UserSummary agentGeneral = identityClient.findByEmail("practice24in+agent.general@gmail.com").orElseThrow();

        Category cardDispute = findCategory("CARD_DISPUTE");
        Category complaint = findCategory("COMPLAINT");
        Category loanAccount = findCategory("LOAN_ACCOUNT");

        //opened ticket, unclaimed
        seedTicket(customer1, null, cardDispute, "Unrecognized charge on my card",
                "I see a $200 charge I never made.", TicketPriority.HIGH, TicketStatus.OPEN);

        //claimed ticket, in progress
        seedTicket(customer2, agentGeneral, complaint, "Rude branch staff",
                "I had a bad experience at the downtown branch.", TicketPriority.LOW, TicketStatus.IN_PROGRESS);

        //A fully closed ticket (reopen)
        seedTicket(customer1, agentGeneral, loanAccount, "Loan statement missing",
                "I never received my latest loan statement.", TicketPriority.MEDIUM, TicketStatus.CLOSED);
    }

    private Category findCategory(String name) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private void seedTicket(UserSummary customer, UserSummary assignedTo, Category category, String title, String description,
                            TicketPriority priority, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setSource(SourceOfTicket.CUSTOMER);
        ticket.setCustomerId(customer.id());
        ticket.setCreatedById(customer.id());
        ticket.setAssignedToId(assignedTo == null ? null : assignedTo.id());
        ticket.setPriority(priority);
        ticket.setStatus(status);
        ticket.setDueAt(LocalDateTime.now().plusHours(72));
        ticketRepository.save(ticket);
    }

}
