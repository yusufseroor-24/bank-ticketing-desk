package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.CreateTicketRequest;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.exception.ConflictException;
import com.ga.bankdesk.model.AgentCategory;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * prove that atomic SQL update prevents race condition
 * Test: 8 agents try to claim the same ticket at the same time
 * Result: One agent succeeds and gets assigned to the ticket and 7 gets a conflict + one assignment history is recorded
 */

@SpringBootTest
class TicketClaimRaceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketHistoryRepository ticketHistoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentCategoryRepository agentCategoryRepository;

    @Test
    void onlyOneAgentCanClaimWhenManyClaimTheSameTicketAtTheSameTime() throws Exception{
        int agentCount = 8;

        Category cardDispute = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("CARD_DISPUTE"))
                .findFirst().orElseThrow();

        User customer = userRepository.findByEmail("practice24in+customer1@gmail.com").orElseThrow();

        Long ticketId = ticketService.createTicket(customer, new CreateTicketRequest("Race test ticket",
                "Many agents claim this at once - test", cardDispute.getId(), TicketPriority.MEDIUM)).id();

        //create eight agents
        List<User> agents = new ArrayList<>();
        for(int i=0; i < agentCount; i++){
            agents.add(createAgent("race.agent." + UUID.randomUUID() + "@bankdesk.com", cardDispute));
        }

        //creating thread pool (8 threads)
        ExecutorService pool = Executors.newFixedThreadPool(agentCount);
        CountDownLatch allReady = new CountDownLatch(agentCount); //every thread is waiting
        CountDownLatch start = new CountDownLatch(1); //released once, so all claims are together started

        //submit a claim per agent
        List<Future<String>> outcomes = new ArrayList<>();
        for(User agent : agents){
            outcomes.add(pool.submit(() -> {
                allReady.countDown(); //reached starting
                start.await(); //don't attempt to claim until released
                try{
                    ticketService.claimTicket(agent, ticketId);
                    return "SUCCESS";
                }catch (ConflictException | OptimisticLockingFailureException e){
                    return "CONFLICT";
                }
            }));
        }
        allReady.await(); //wait until all 8 have reached
        start.countDown(); //change start from 1 to 0, releasing all waiting workers

        int successes = 0;
        int conflicts = 0;
        for(Future<String> outcome : outcomes){
            if(outcome.get(30, TimeUnit.SECONDS).equals("SUCCESS")){
                successes++;
            }else{
                conflicts++;
            }
        }
        pool.shutdown();

        //one claim, all other are conflict
        assertEquals(1, successes);
        assertEquals(agentCount - 1, conflicts);

        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.ASSIGNED, ticket.getStatus());
        assertNotNull(ticket.getAssignedTo());

        long assignedHistoryRow = ticketHistoryRepository.findByTicketIdOrderByCreatedAtDesc(ticketId).stream()
                .filter(h -> h.getAction().equals("ASSIGNED"))
                .count();
        assertEquals(1, assignedHistoryRow);
    }

    private User createAgent(String email, Category category){
        User agent = new User();
        agent.setEmail(email);
        agent.setPassword("test");
        agent.setFullName("Race Agent");
        agent.setRole(Role.AGENT);
        agent.setStatus(UserStatus.ACTIVE);
        agent.setEmailVerified(true);

        User saved = userRepository.save(agent);

        AgentCategory link = new AgentCategory();
        link.setAgent(saved);
        link.setCategory(category);
        agentCategoryRepository.save(link);
        return saved;
    }
}
