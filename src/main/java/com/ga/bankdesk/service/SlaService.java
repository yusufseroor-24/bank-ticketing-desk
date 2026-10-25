package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.SlaPolicy;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.repository.SlaPolicyRepository;
import com.ga.bankdesk.workflow.CategoryRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * use SLA policies to calculate a ticket deadline, priority, and updated SLA rules
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class SlaService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final CategoryRules categoryRules;

    //statuses that means the ticket is "done"
    private static final Set<TicketStatus> RESOLUTION_STATUSES = EnumSet.of(
            TicketStatus.RESOLVED, TicketStatus.APPROVED, TicketStatus.REJECTED, TicketStatus.FALSE_POSITIVE,
            TicketStatus.CONFIRMED, TicketStatus.CLOSED);

    public boolean isResolutionStatus(TicketStatus status){
        return RESOLUTION_STATUSES.contains(status);
    }

    //makes sure ticket priority is not lower than the minimum required for that ticket category
    public TicketPriority correctPriority(Category category, TicketPriority requested){
        TicketPriority minimum = categoryRules.minimumPriority(category.getName());
        //check if requested priority meet or exceed the min
        return requested.ordinal() >= minimum.ordinal() ? requested : minimum;
    }

    //adds both deadlines onto the ticket
    public void applyDeadlines(Ticket ticket, LocalDateTime startTime){
        long responseMinutes;
        long resolutionMinutes;

        SlaPolicy policy = slaPolicyRepository
                .findByCategoryIdAndPriority(ticket.getCategory().getId(), ticket.getPriority())
                .orElse(null);
        if(policy !=null){
            responseMinutes = policy.getFirstResponseMinutes();
            resolutionMinutes = policy.getResolutionMinutes();
        }else{
            //ticket creation never fails if a policy row is missing
            resolutionMinutes = fallbackResolutionMinutes(ticket.getPriority());
            responseMinutes = resolutionMinutes / 4;
            log.warn("No SLA policy for category={} priority={}, using fallback", ticket.getCategory().getName(), ticket.getPriority());

        }
        ticket.setFirstResponseDueAt(startTime.plusMinutes(responseMinutes));
        ticket.setDueAt(startTime.plusMinutes(resolutionMinutes));
    }

    //admin list of policies
    public List<SlaPolicy> listPolicies(){
        return slaPolicyRepository.findAll();
    }

    private long fallbackResolutionMinutes(TicketPriority priority){
        return switch(priority){
            case CRITICAL -> 4 * 60;
            case HIGH -> 24 * 60;
            case MEDIUM -> 72 * 60;
            case LOW -> 120 * 60;
            };
    }

    public SlaPolicy updatePolicy(Long policyId, int firstResponseMinute, int resolutionMinutes){
        if(firstResponseMinute > resolutionMinutes){
            throw new BusinessRuleException("First response time cannot be longer than the resolution time");
        }
        SlaPolicy policy = slaPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy with ID " + policyId + " is not found"));
        policy.setFirstResponseMinutes(firstResponseMinute);
        policy.setResolutionMinutes(resolutionMinutes);
        log.info("SLA policy {} updated: category={}, priority={}, response={}min, resolution={}min",
                policyId, policy.getCategory().getName(), policy.getPriority(), firstResponseMinute, resolutionMinutes);
        return slaPolicyRepository.save(policy);
    }



}

