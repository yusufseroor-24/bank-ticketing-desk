package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.CreateInternalTicketRequest;
import com.ga.bankdesk.dto.CreateTicketRequest;
import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.SourceOfTicket;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ConflictException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.mapper.TicketMapper;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.AgentCategoryRepository;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.TicketRepository;
import com.ga.bankdesk.repository.UserRepository;
import com.ga.bankdesk.workflow.CategoryTicketWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;
    private final UserRepository userRepository;
    private final CategoryTicketWorkflow categoryTicketWorkflow;
    private final AgentCategoryRepository agentCategoryRepository;

    public TicketCreationResponse createTicket(User customer, CreateTicketRequest request){
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + request.categoryId() + " is not found"));
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(category);
        ticket.setCustomer(customer);
        ticket.setCreatedBy(customer);
        ticket.setSource(SourceOfTicket.CUSTOMER);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(request.priority());
        ticket.setDueAt(LocalDateTime.now().plusHours(slaHours(request.priority())));

        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }

    int slaHours(TicketPriority priority){
        return switch(priority) {
            case CRITICAL -> 4;
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 120;
        };
    }

    public TicketCreationResponse getTicketById(User user, Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        if(user.getRole() == Role.CUSTOMER){
            boolean isOwner = ticket.getCustomer() !=null && ticket.getCustomer().getId().equals(user.getId());
            if(!isOwner){
                //to avoid revealing that it exists
                throw new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found");
            }
        }
        return ticketMapper.toResponse(ticket);
    }

    public List<TicketCreationResponse> myTickets(User user){
        List<Ticket> tickets = ticketRepository.findByCustomerId(user.getId());
        return tickets.stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    public TicketCreationResponse internalTicketCreation(User agent, CreateInternalTicketRequest request){
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + request.categoryId() + " is not found"));

        User customer = null; //optional
        if(request.customerId() != null){
            customer = userRepository.findById(request.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer with ID " + request.customerId() + " is not found"));
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(category);
        ticket.setCustomer(customer);
        ticket.setCreatedBy(agent);
        ticket.setSource(SourceOfTicket.INTERNAL);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(request.priority());
        ticket.setDueAt(LocalDateTime.now().plusHours(slaHours(request.priority())));

        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }

    private static final Set<TicketStatus> REQUIRED_NOTE = Set.of(TicketStatus.ESCALATED, TicketStatus.RESOLVED);

    public TicketCreationResponse changeStatus(Long ticketId, TicketStatus newStatus, String note){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));

        if(ticket.getStatus() == TicketStatus.CLOSED){
            throw new BusinessRuleException("This ticket is closed and can't be modified");
        }

        String categoryName = ticket.getCategory().getName();
        TicketStatus currentTicketStatus = ticket.getStatus();

        if(!categoryTicketWorkflow.isValidTransition(categoryName, currentTicketStatus, newStatus)){
            Set<TicketStatus> allowed = categoryTicketWorkflow.getAllowedNextStatus(categoryName, currentTicketStatus);
            throw new BusinessRuleException("Cannot change status from " + currentTicketStatus + " to " + newStatus + ". " +
                    "Allowed next Statuses: " + allowed);
        }

        if(REQUIRED_NOTE.contains(newStatus) && (note == null || note.isBlank())){ //check empty string too
            throw new BusinessRuleException("A note is required to move this ticket to " + newStatus);
        }
        ticket.setStatus(newStatus);
        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }

    public TicketCreationResponse reopenTicket(Long ticketId, String reason){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        if(ticket.getStatus() != TicketStatus.CLOSED){
            throw new BusinessRuleException("Only closed tickets can be reopened");
        }
        if(reason == null || reason.isBlank()){
            throw new BusinessRuleException("A reason is required to reopen a ticket");
        }
        ticket.setStatus(TicketStatus.OPEN);
        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }

    public TicketCreationResponse claimTicket(User agent, Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));

        //checks if the agent is authorized to view the category tickets / claim
        boolean allowedCategory = agentCategoryRepository.existsByAgentIdAndCategoryId(agent.getId(), ticket.getCategory().getId());
        if(!allowedCategory){
            throw new BusinessRuleException("You cannot view this category tickets, out of scope category");
        }
        //check if it's already assigned by another agent (and prevent double claim)
        if(ticket.getAssignedTo() != null){
            throw new ConflictException("This ticket has already been assigned to an agent");
        }
        //check if its already assigned and not opened
        if(!categoryTicketWorkflow.isValidTransition(ticket.getCategory().getName(), ticket.getStatus(), TicketStatus.ASSIGNED)){
            throw new BusinessRuleException("This ticket can't be claimed from its current status");
        }
        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.ASSIGNED);
        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }
}
