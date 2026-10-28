package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.CreateTransactionRequest;
import com.ga.bankdesk.dto.FraudAlertResponse;
import com.ga.bankdesk.dto.SimulateTransactionsRequest;
import com.ga.bankdesk.dto.TransactionResponse;
import com.ga.bankdesk.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> record(@Valid @RequestBody CreateTransactionRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.record(request));
    }

    //records a ready-made suspicious pattern
    @PostMapping("/transactions/simulate")
    public ResponseEntity<List<TransactionResponse>> simulate(@Valid @RequestBody SimulateTransactionsRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.simulate(request.customerId(), request.scenario()));
    }

    @GetMapping("/fraud-alerts")
    public List<FraudAlertResponse> alerts() {
        return transactionService.listAlerts();
    }
}
