package com.ga.bankdesk.repository;

import com.ga.bankdesk.model.AgentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentCategoryRepository extends JpaRepository<AgentCategory, Long> {

    List<AgentCategory> findByAgentId(Long agentId);

    boolean existsByAgentIdAndCategoryId(Long agentId, Long CategoryId);
}
