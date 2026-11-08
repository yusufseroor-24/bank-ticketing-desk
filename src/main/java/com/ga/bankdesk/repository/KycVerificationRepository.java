package com.ga.bankdesk.repository;

import com.ga.bankdesk.model.KycVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    Optional<KycVerification> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    //locks the row so two identical webhook deliveries arriving together are processed one after the other
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from KycVerification k where k.sessionId = :sessionId")
    Optional<KycVerification> findBySessionIdForUpdate(@Param("sessionId") String sessionId);
}

