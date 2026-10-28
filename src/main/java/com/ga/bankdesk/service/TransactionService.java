package com.ga.bankdesk.service;


import com.ga.bankdesk.dto.CreateTransactionRequest;
import com.ga.bankdesk.dto.FraudAlertResponse;
import com.ga.bankdesk.dto.TransactionResponse;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.SimulationScenario;
import com.ga.bankdesk.enums.TransactionType;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.model.BankTransaction;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.BankTransactionRepository;
import com.ga.bankdesk.repository.FraudAlertRepository;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Records bank transactions and publishes a TransactionRecordedEvent
 * Demo scenarios
 * record() method: transaction details receive request -> validate customer -> build & save transaction -> publish an event
 * simulate() method: (create sample transactions and go through record method) instead of manually submitting transactions, a scenario is selected and the method generates transactions
 * listAlerts() method: retrieves all fraud alerts and return them as response DTO
 * req() method: helper method to create a transaction request
 * Overall workflow: simulate() -> req() -> record()
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionService {

    private final BankTransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TransactionResponse record(CreateTransactionRequest request){
        User customer = userRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer with ID " + request.customerId() + " is not found"));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new BusinessRuleException("Transactions can only be recorded for customers");
        }

        BankTransaction t = new BankTransaction();
        t.setCustomer(customer);
        t.setAccountRef(request.accountRef());
        t.setType(request.type());
        t.setAmount(request.amount());
        t.setCurrency(request.currency().toUpperCase());
        t.setCountry(request.country().toUpperCase());
        t.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : LocalDateTime.now());

        BankTransaction saved = transactionRepository.save(t);
        log.info("Transaction {} recorded: customer={}, {} {} {}", saved.getId(), customer.getId(),
                saved.getType(), saved.getAmount(), saved.getCurrency());

        //notify
        eventPublisher.publishEvent(new TransactionRecordedEvent(saved.getId()));
        return TransactionResponse.from(saved);
    }

    public List<TransactionResponse> simulate(Long customerId, SimulationScenario scenario){
        LocalDateTime now = LocalDateTime.now();
        List<CreateTransactionRequest> batch = switch (scenario){
            case LARGE -> List.of(
                    req(customerId, TransactionType.TRANSFER, "15000.00", "BH", now));
            //3 deposit spaced 60 min apart
            case STRUCTURING -> List.of(
                    req(customerId, TransactionType.CASH_DEPOSIT, "9200.00", "BH", now.minusMinutes(120)),
                    req(customerId, TransactionType.CASH_DEPOSIT, "9400.00", "BH", now.minusMinutes(60)),
                    req(customerId, TransactionType.CASH_DEPOSIT, "9100.00", "BH", now));
            //create 5 card payments of 75.00 that are 20 seconds apart
            case VELOCITY -> IntStream.range(0, 5)
                    .mapToObj(i -> req(customerId, TransactionType.CARD_PAYMENT, "75.00", "BH",
                            now.minusSeconds((4 - i) * 20L))) //subtract 20 after each range number
                    .toList();
            case RISKY_COUNTRY -> List.of(
                    req(customerId, TransactionType.TRANSFER, "2500.00", "IR", now));
            case GREY_LIST_COUNTRY -> List.of(
                    req(customerId, TransactionType.TRANSFER, "2500.00", "LB", now));
        };
        return batch.stream()
                .map(this::record)
                .toList();
    }

    public List<FraudAlertResponse> listAlerts() {
        return fraudAlertRepository.findAll(
                //fetch alerts starting from newest first
                Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                //FraudAlert to FraudAlertResponse
                .map(FraudAlertResponse::from)
                .toList();
    }

    private CreateTransactionRequest req(Long customerId, TransactionType type, String amount,
                                         String country, LocalDateTime when) {
        return new CreateTransactionRequest(customerId, "ACC-" + customerId, type,
                new BigDecimal(amount), "USD", country, when);
    }


}
