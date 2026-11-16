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
import com.ga.bankdesk.repository.TicketRepository;
import com.ga.bankdesk.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.ga.bankdesk.dto.DeletedTicketResponse;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

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
    private final TicketRepository ticketRepository;

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

    //hides a ticket from everyone (customers, agents, SLA alerts) without removing it from the database
    @Transactional
    public void softDeleteTicket(CurrentUser admin, Long ticketId){
        int updated = ticketRepository.softDelete(ticketId, LocalDateTime.now());
        if(updated == 0){
            throw new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found or is already deleted");
        }
        log.info("TICKET_SOFT_DELETED: {} (by admin {})", ticketId, admin.getEmail());
    }

    //brings a soft-deleted ticket back
    @Transactional
    public void restoreTicket(CurrentUser admin, Long ticketId){
        int updated = ticketRepository.restore(ticketId);
        if(updated == 0){
            throw new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found or is not deleted");
        }
        log.info("TICKET_RESTORED: {} (by admin {})", ticketId, admin.getEmail());
    }

    //feeds the "deleted tickets" dropdown
    public List<DeletedTicketResponse> listDeletedTickets(){
        return ticketRepository.findAllDeleted().stream()
                .map(t -> new DeletedTicketResponse(t.getId(), t.getTitle(), t.getStatus(), t.getDeletedAt()))
                .toList();
    }
}
