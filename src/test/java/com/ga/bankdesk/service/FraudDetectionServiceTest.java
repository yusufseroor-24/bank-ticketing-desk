package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.CreateTransactionRequest;
import com.ga.bankdesk.enums.*;
import com.ga.bankdesk.model.FraudAlert;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.BankTransactionRepository;
import com.ga.bankdesk.repository.FraudAlertRepository;
import com.ga.bankdesk.repository.TicketRepository;
import com.ga.bankdesk.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FraudDetectionServiceTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    TransactionService transactionService;
    @Autowired
    FraudAlertRepository alertRepository;
    @Autowired
    BankTransactionRepository transactionRepository;

    private User customer;

    @BeforeEach
    void createFreshCustomer() {
        User u = new User();
        u.setEmail("fraudtest-" + UUID.randomUUID() + "@test.local");
        u.setPassword("not-used");
        u.setFullName("Fraud Test");
        u.setRole(Role.CUSTOMER);
        u.setStatus(UserStatus.ACTIVE);
        customer = userRepository.save(u);
    }

    @AfterEach
    void cleanUp() {
        for (FraudAlert alert : alertRepository.findBySubjectUserId(customer.getId())) {
            Ticket auto = alert.getCreatedTicket();
            alertRepository.delete(alert);
            if (auto != null) {
                ticketRepository.delete(auto);
            }
        }
        transactionRepository.deleteAll(transactionRepository.findByCustomerId(customer.getId()));
        userRepository.delete(customer);
    }

    private void send(TransactionType type, String amount, String country, LocalDateTime when) {
        transactionService.record(new CreateTransactionRequest(
                customer.getId(), "ACC-TEST", type, new BigDecimal(amount), "USD", country, when));
    }

    private List<FraudAlert> alertsFor(String ruleCode) {
        return alertRepository.findBySubjectUserId(customer.getId()).stream()
                .filter(a -> a.getRule().getCode().equals(ruleCode)).toList();
    }

    @Test
    void largeTransferOpensFraudTicketAuto() {
        send(TransactionType.TRANSFER, "15000.00", "BH", null);

        List<FraudAlert> alerts = alertsFor("LARGE_TRANSACTION");
        assertEquals(1, alerts.size());
        Ticket auto = alerts.get(0).getCreatedTicket();
        assertNotNull(auto);
        assertEquals("FRAUD", auto.getCategory().getName());
        assertEquals(SourceOfTicket.SYSTEM, auto.getSource());
        assertEquals(TicketPriority.HIGH, auto.getPriority());
        assertNull(auto.getCustomer(), "the customer must not be attached or notified");
    }

    @Test
    void blackListAndGreyListCountriesGetDifferentPriorities() {
        send(TransactionType.TRANSFER, "2500.00", "IR", null);
        send(TransactionType.TRANSFER, "2500.00", "LB", null);

        assertEquals(TicketPriority.HIGH, alertsFor("HIGH_RISK_COUNTRY").get(0).getCreatedTicket().getPriority());
        assertEquals(TicketPriority.MEDIUM, alertsFor("GREY_LIST_COUNTRY").get(0).getCreatedTicket().getPriority());
        assertEquals(1, alertsFor("HIGH_RISK_COUNTRY").size());
        assertEquals(1, alertsFor("GREY_LIST_COUNTRY").size());
    }

    @Test
    void threeDepositsJustUnderTheLimitRaiseOneAmlAlert() {
        LocalDateTime now = LocalDateTime.now();
        send(TransactionType.CASH_DEPOSIT, "9200.00", "BH", now.minusMinutes(120));
        send(TransactionType.CASH_DEPOSIT, "9400.00", "BH", now.minusMinutes(60));
        assertTrue(alertsFor("STRUCTURING_DEPOSITS").isEmpty(), "two deposits are not enough");

        send(TransactionType.CASH_DEPOSIT, "9100.00", "BH", now);
        assertEquals(1, alertsFor("STRUCTURING_DEPOSITS").size());
        assertEquals("AML", alertsFor("STRUCTURING_DEPOSITS").get(0).getCreatedTicket().getCategory().getName());

        send(TransactionType.CASH_DEPOSIT, "9300.00", "BH", now);
        assertEquals(1, alertsFor("STRUCTURING_DEPOSITS").size(), "same window: no second alert");
    }

    @Test
    void fiveSimultaneousTransactionsOpenOnlyOneVelocityTicket() throws Exception {
        LocalDateTime when = LocalDateTime.now();
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                send(TransactionType.CARD_PAYMENT, "50.00", "BH", when);
                return null;
            }));
        }
        ready.await();
        go.countDown();
        for (Future<?> f : futures) {
            f.get(20, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, alertsFor("RAPID_ACTIVITY").size());
        long autoTickets = ticketRepository.findAll().stream()
                .filter(x -> x.getSource() == SourceOfTicket.SYSTEM && x.getDescription() != null
                        && x.getDescription().contains("(RAPID_ACTIVITY)")
                        && x.getDescription().contains("(id " + customer.getId() + ")"))
                .count();
        assertEquals(1, autoTickets, "5 simultaneous transactions must open exactly one ticket");
    }



}