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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
@Order(3)
//seeding ticket data
public class TicketSeeder implements CommandLineRunner {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (ticketRepository.count() > 0) {
            return;
        }

        User customer1 = userRepository.findByEmail("customer1@bankdesk.com").orElseThrow();
        User customer2 = userRepository.findByEmail("customer2@bankdesk.com").orElseThrow();
        User agentGeneral = userRepository.findByEmail("agent.general@bankdesk.com").orElseThrow();

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

    private void seedTicket(User customer, User assignedTo, Category category, String title, String description,
                            TicketPriority priority, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setSource(SourceOfTicket.CUSTOMER);
        ticket.setCustomer(customer);
        ticket.setCreatedBy(customer);
        ticket.setAssignedTo(assignedTo);
        ticket.setPriority(priority);
        ticket.setStatus(status);
        ticket.setDueAt(LocalDateTime.now().plusHours(72));
        ticketRepository.save(ticket);
    }

}
