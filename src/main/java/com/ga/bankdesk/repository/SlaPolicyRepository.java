package com.ga.bankdesk.repository;

import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.model.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByCategoryIdAndPriorityId(Long categoryId, TicketPriority priority);
}
