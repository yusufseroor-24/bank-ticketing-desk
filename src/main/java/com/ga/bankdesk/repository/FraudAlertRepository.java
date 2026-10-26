package com.ga.bankdesk.repository;

import com.ga.bankdesk.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * atomic claim is used to handle a situation where multiple fraud detection workers might try to create the same alert at the same time.
 */

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    //find alerts belonging to a customer user
    List<FraudAlert> findBySubjectUserId(Long userId);

    //prevents duplicated alerts
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO fraud_alerts (rule_id, subject_user_id, trigger_transaction_id, dedup_key, created_at) " +
            "VALUES (:ruleId, :userId, :transactionId, :key, now()) ON CONFLICT (dedup_key) DO NOTHING", nativeQuery = true)
    int claim(@Param("ruleId") Long ruleId,
              @Param("userId") Long userId,
              @Param("transactionId") Long transactionId,
              @Param("key") String key);

    //links an alert to a ticket
    @Modifying
    @Query(value = "UPDATE fraud_alerts SET created_ticket_id = :ticketId WHERE dedup_key = :key", nativeQuery = true)
    int attachTicket(@Param("key") String key,
                     @Param("ticketId") Long ticketId);
}
