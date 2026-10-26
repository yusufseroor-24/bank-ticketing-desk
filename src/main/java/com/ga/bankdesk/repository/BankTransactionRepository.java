package com.ga.bankdesk.repository;

import com.ga.bankdesk.enums.TransactionType;
import com.ga.bankdesk.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    List<BankTransaction> findByCustomerId(Long customerId);

    //counts transactions for a customer that occurred after a given date and time
    long countByCustomerIdAndOccurredAtAfter(Long customerId, LocalDateTime from);

    //count transactions within an amount range and satisfies all 5 conditions
    @Query("SELECT COUNT(b) FROM BankTransaction b WHERE b.customer.id = :customerId AND b.type = :type " +
            "AND b.amount >= :minAmount AND b.amount < :maxAmount AND b.occurredAt > :from")
    long countInAmountRange(@Param("customerId") Long customerId,
                            @Param("type") TransactionType type,
                            @Param("minAmount") BigDecimal minAmount,
                            @Param("maxAmount") BigDecimal maxAmount,
                            @Param("from") LocalDateTime from);
}
