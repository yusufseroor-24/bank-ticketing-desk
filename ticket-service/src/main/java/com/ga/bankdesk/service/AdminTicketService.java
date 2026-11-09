package com.ga.bankdesk.service;

import com.ga.bankdesk.client.IdentityClient;
import com.ga.bankdesk.dto.UserSummary;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.model.AgentCategory;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.repository.AgentCategoryRepository;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The ticket half of the monolith AdminService (the user half is AdminUserService in identity-service)
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AdminTicketService {

    private final IdentityClient identityClient;
    private final AgentCategoryRepository agentCategoryRepository;
    private final CategoryRepository categoryRepository;

    //allows the admin to assign the agent to a specific category to resolve tickets of that catgeory only
    public void assignCategoryToAgent(CurrentUser admin, Long agentId, List<Long> categoryIds){
        UserSummary agent = identityClient.findUser(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + agentId + " is not found"));
        if(agent.role() != Role.AGENT){
            throw new BusinessRuleException("Categories can only be assigned to agents");
        }
        for(Long categoryId : categoryIds){
            //checks table if it exists
            if(agentCategoryRepository.existsByAgentIdAndCategoryId(agentId, categoryId)){
                continue;
            }
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + categoryId + "is not found"));
            AgentCategory assign = new AgentCategory();
            assign.setAgentId(agentId);
            assign.setCategory(category);
            agentCategoryRepository.save(assign);
            //the audit log lives in identity-service, for now this is only logged (Kafka event later)
            log.info("CATEGORY_ASSIGNED: {} -> {} (by admin {})", agent.email(), category.getName(), admin.getEmail());
        }
    }
}
