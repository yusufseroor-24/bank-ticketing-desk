package com.ga.bankdesk.service;


import com.ga.bankdesk.dto.CreateTicketRequest;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.exception.ConflictException;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TicketClaimConflictTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void secondClaimOnAlreadyAssignedTicketThrowsConflict() {
        Category cardDispute = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("CARD_DISPUTE"))
                .findFirst().orElseThrow();
        User customer = userRepository.findByEmail("customer1@bankdesk.com").orElseThrow();
        User agent = userRepository.findByEmail("agent.general@bankdesk.com").orElseThrow();

        Long ticketId = ticketService.createTicket(customer,
                new CreateTicketRequest("Test dispute", "A test charge dispute",
                        cardDispute.getId(), TicketPriority.MEDIUM)).id();

        ticketService.claimTicket(agent, ticketId); //first claim succeeds
        assertThrows(ConflictException.class, () -> ticketService.claimTicket(agent, ticketId));
    }
}
