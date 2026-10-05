package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.CommentResponse;
import com.ga.bankdesk.dto.CreateInternalTicketRequest;
import com.ga.bankdesk.dto.CreateTicketRequest;
import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.enums.*;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ConflictException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.mapper.CommentMapper;
import com.ga.bankdesk.mapper.TicketMapper;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.TicketComments;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.*;
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
    private final TicketCommentsRepository ticketCommentsRepository;
    private final CommentMapper commentMapper;

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

    public TicketCreationResponse reassignTicket(Long ticketId, Long newAgentId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));

        User agent = userRepository.findById(newAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent with ID " + newAgentId + " is not found"));

        if(agent.getRole() !=Role.AGENT){
            throw new BusinessRuleException("The ticket can only be assigned to a user agent role");
        }
        if(agent.getStatus() != UserStatus.ACTIVE){
            throw new BusinessRuleException("The ticket cannot be assigned to an inactive agent");
        }
        //checks if the agent is authorized to view the category tickets / claim
        boolean allowedCategory = agentCategoryRepository.existsByAgentIdAndCategoryId(agent.getId(), ticket.getCategory().getId());
        if(!allowedCategory){
            throw new BusinessRuleException("The agent cannot view this category tickets, out of scope category");
        }

        ticket.setAssignedTo(agent);
        if(ticket.getStatus() == TicketStatus.OPEN){
            ticket.setStatus(TicketStatus.ASSIGNED);
        }
        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }

    //same workflow but changes priority to high and updated due time according to SLA priority
    public TicketCreationResponse escalateTicket(Long ticketId, String note){
        if(note == null || note.isBlank()){
            throw new BusinessRuleException("A note is required to escalate a ticket");
        }
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        if (!categoryTicketWorkflow.isValidTransition(ticket.getCategory().getName(), ticket.getStatus(), TicketStatus.ESCALATED)){
            throw new BusinessRuleException("This ticket cannot be escalated from its current status");
        }

        ticket.setStatus(TicketStatus.ESCALATED);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setDueAt(LocalDateTime.now().plusHours(slaHours(TicketPriority.HIGH)));
        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }

    public CommentResponse addComment(User currentUser, Long ticketId, String commentContent){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        checkCanUerAccessTicket(currentUser, ticket);
        if(ticket.getStatus() == TicketStatus.CLOSED){
            throw new BusinessRuleException("Cannot comment on a closed ticket");
        }
        TicketComments comment = new TicketComments();
        comment.setTicket(ticket);
        comment.setAuthor(currentUser);
        comment.setCommentContent(commentContent);

        TicketComments save = ticketCommentsRepository.save(comment);
        return commentMapper.toResponse(save);
    }

    public List<CommentResponse> listComments(User currentUser, Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));

        checkCanUerAccessTicket(currentUser, ticket);
        return ticketCommentsRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    //permission check: customers can only edit their own ticket but staff can touch any
    private void checkCanUerAccessTicket(User currentUser, Ticket ticket){
        if(currentUser.getRole() == Role.CUSTOMER){
            boolean isOwner = ticket.getCustomer() != null && ticket.getCustomer().getId().equals(currentUser.getId());
            if(!isOwner){
                throw  new ResourceNotFoundException("Ticket with ID " + ticket.getId() + " was not found");
            }
        }

    }


}
