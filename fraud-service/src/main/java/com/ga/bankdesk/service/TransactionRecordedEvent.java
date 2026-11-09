package com.ga.bankdesk.service;

/**
 * Published after a bank transaction is saved. Carries only the ID so fraud detection loads a fresh copy
 * @param transactionId
 */
public record TransactionRecordedEvent(Long transactionId) {
}
