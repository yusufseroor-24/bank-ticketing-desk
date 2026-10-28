package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.SourceOfTicket;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.enums.TransactionType;
import com.ga.bankdesk.model.*;
import com.ga.bankdesk.notifications.LiveNotifier;
import com.ga.bankdesk.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * onTransactionRecorded(): connection between TransactionService and FraudDetectionService (listens for the published transaction)
 * evaluate(): checks transaction against enabled rules then attempts to create and alert
 * check(): chooses the appropriate rule and prints an appropriate response
 * checkStructuring(): checks pattern of repeated deposit within the amount range and time window
 * checkVelocity(): checks for too many transactions happening too quickly
 * isRiskyTransfer(): checks the country
 * raiseAlert(): create the alert and investigation ticket, when a fraud rule matches, the method attempt to record the alert and open a ticket
 * Per-transaction rules: one alert per transaction. Window rules: one per customer per window.
 */

@Slf4j
@Service
public class FraudDetectionService {

    private final FraudRuleRepository ruleRepository;
    private final FraudAlertRepository alertRepository;
    private final BankTransactionRepository transactionRepository;
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final SlaService slaService;
    //execute code inside a transaction programmatically (either all successful or fails)
    private final TransactionTemplate tx;
    private final LiveNotifier liveNotifier;

    public FraudDetectionService(FraudRuleRepository ruleRepository, FraudAlertRepository alertRepository,
                                 BankTransactionRepository transactionRepository, TicketRepository ticketRepository,
                                 CategoryRepository categoryRepository, SlaService slaService,
                                 PlatformTransactionManager transactionManager, LiveNotifier liveNotifier) {
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.slaService = slaService;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.liveNotifier = liveNotifier;
    }

    //runs after a bank transaction has been committed and starts fraud detection
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTransactionRecorded(TransactionRecordedEvent event){
        try{
            tx.executeWithoutResult(status -> evaluate(event.transactionId()));
        }catch (Exception e){
            log.error("Fraud rule check failed for transaction {}: {}", event.transactionId(), e.getMessage());
        }
    }

    //check every enabled true against the transaction
    private void evaluate(Long transactionId){
        BankTransaction t = transactionRepository.findById(transactionId).orElse(null);
        if(t == null){
            return;
        }
        for(FraudRule rule : ruleRepository.findByEnabledTrue()){
            String detail = check(rule, t);
            if(detail != null){
                raiseAlert(rule, t, detail);
            }
        }
    }

    //return human-readable reason if the rule matches
    private String check(FraudRule rule, BankTransaction t){
        return switch (rule.getRuleType()) {
            //checks transaction amount is greater than or equal to the minimum amount rule
            case LARGE_AMOUNT -> t.getAmount().compareTo(rule.getMinAmount()) >= 0
                    ? "Amount " + t.getAmount() + " " + t.getCurrency() + " is at or above " + rule.getMinAmount()
                    : null;
            case STRUCTURING -> checkStructuring(rule, t);
            case VELOCITY -> checkVelocity(rule, t);
            case RISKY_COUNTRY -> isRiskyTransfer(rule, t)
                    ? "Transfer to watch-listed country " + t.getCountry()
                    : null;
        };
    }

    private String checkStructuring(FraudRule rule, BankTransaction t) {
        if (t.getType() != TransactionType.CASH_DEPOSIT) {
            return null;
        }
        if (t.getAmount().compareTo(rule.getMinAmount()) < 0 || t.getAmount().compareTo(rule.getMaxAmount()) >= 0) {
            return null;
        }
        LocalDateTime from = t.getOccurredAt().minusMinutes(rule.getWindowMinutes());
        //count qualifying deposits from that customer
        long count = transactionRepository.countInAmountRange(
                t.getCustomer().getId(), TransactionType.CASH_DEPOSIT, rule.getMinAmount(), rule.getMaxAmount(), from);
        //compare the count with the threshold
        return count >= rule.getThreshold()
                ? count + " cash deposits between " + rule.getMinAmount() + " and " + rule.getMaxAmount()
                + " within " + rule.getWindowMinutes() + " minutes"
                : null;
    }

    //how many transactions the customer made within the time window
    private String checkVelocity(FraudRule rule, BankTransaction t) {
        LocalDateTime from = t.getOccurredAt().minusMinutes(rule.getWindowMinutes());
        long count = transactionRepository.countByCustomerIdAndOccurredAtAfter(t.getCustomer().getId(), from);
        return count >= rule.getThreshold()
                ? count + " transactions within " + rule.getWindowMinutes() + " minutes"
                : null;
    }

    private boolean isRiskyTransfer(FraudRule rule, BankTransaction t) {
        if (t.getType() != TransactionType.TRANSFER || rule.getCountries() == null) {
            return false;
        }
        for (String code : rule.getCountries().split(",")) {
            if (code.trim().equalsIgnoreCase(t.getCountry())) {
                return true;
            }
        }
        return false;
    }

    private void raiseAlert(FraudRule rule, BankTransaction trigger, String detail) {
        User customer = trigger.getCustomer();
        //create a deduplication key
        String key = dedupKey(rule, trigger);

        //prevents duplicate alerts and prevents being claimed twice
        if (alertRepository.claim(rule.getId(), customer.getId(), trigger.getId(), key) == 0) {
            log.debug("Alert {} already raised, skipping", key);
            return;
        }

        Category category = categoryRepository.findByName(rule.getAlertCategory())
                .orElseThrow(() -> new IllegalStateException("Category not found: " + rule.getAlertCategory()));

        Ticket auto = new Ticket();
        auto.setTitle(rule.getName() + " - " + customer.getFullName());
        auto.setDescription("Rule: " + rule.getName() + " (" + rule.getCode() + ")\n"
                + "Customer: " + customer.getFullName() + " (id " + customer.getId() + ")\n"
                + "Reason: " + detail + "\n"
                + "Triggered by transaction #" + trigger.getId() + ": " + trigger.getType() + " "
                + trigger.getAmount() + " " + trigger.getCurrency() + " (" + trigger.getCountry() + ")");
        auto.setCategory(category);
        auto.setCustomer(null);
        auto.setCreatedBy(null); //created by the system
        auto.setSource(SourceOfTicket.SYSTEM);
        auto.setStatus(TicketStatus.OPEN);
        auto.setPriority(rule.getAlertPriority());
        slaService.applyDeadlines(auto, LocalDateTime.now());

        Ticket saved = ticketRepository.save(auto);
        alertRepository.attachTicket(key, saved.getId());

        Set<Long> w = new LinkedHashSet<>(liveNotifier.admins());
        w.addAll(liveNotifier.staffFor(saved));
        liveNotifier.send(w, null, saved.getId(), saved.getTitle(), "FRAUD_ALERT",
                "Alert: " + rule.getName() + " (" + rule.getAlertCategory().replace('_', ' ') + ")");
        log.warn("Fraud rule {} fired for customer {}: opened {} ticket #{}",
                rule.getCode(), customer.getId(), rule.getAlertCategory(), saved.getId());
    }

    private String dedupKey(FraudRule rule, BankTransaction t) {
        return switch (rule.getRuleType()) {
            case LARGE_AMOUNT, RISKY_COUNTRY -> rule.getCode() + ":tx:" + t.getId();
            case STRUCTURING, VELOCITY -> {
                long windowSeconds = rule.getWindowMinutes() * 60L;
                long bucket = t.getOccurredAt().toEpochSecond(ZoneOffset.UTC) / windowSeconds;
                yield rule.getCode() + ":user:" + t.getCustomer().getId() + ":" + bucket;
            }
        };
    }

}
