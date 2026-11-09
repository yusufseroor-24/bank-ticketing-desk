package com.ga.bankdesk.repository;

import com.ga.bankdesk.model.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {
    List<FraudRule> findByEnabledTrue();
}
