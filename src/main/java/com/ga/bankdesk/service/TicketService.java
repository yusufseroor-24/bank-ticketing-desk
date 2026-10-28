package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.*;
import com.ga.bankdesk.enums.*;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ConflictException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.mapper.CommentMapper;
import com.ga.bankdesk.mapper.TicketMapper;
import com.ga.bankdesk.model.*;
import com.ga.bankdesk.notifications.SseEmitterRegistry;
import com.ga.bankdesk.repository.*;
import com.ga.bankdesk.specification.TicketSpecifications;
import com.ga.bankdesk.workflow.CategoryRules;
import com.ga.bankdesk.workflow.CategoryTicketWorkflow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
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
    private final FileStorageService fileStorageService;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final SseEmitterRegistry emitterRegistry;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final SlaService slaService;
    private final CategoryRules categoryRules;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;


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
        ticket.setPriority(slaService.correctPriority(category, request.priority()));
        slaService.applyDeadlines(ticket, LocalDateTime.now());

        Ticket save = ticketRepository.save(ticket);

        recordHistory(save, customer, "CREATED", null, save.getStatus().toString());
        log.info("Ticket created: id={}, category={}, customer={}", save.getId(), category.getName(), customer.getEmail());
        emailService.sendTicketCreatedEmail(customer.getEmail(), save.getId(), save.getTitle(), category.getName());
        return ticketMapper.toResponse(save);
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

    //employye/agent ticket creation (internal)
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
        ticket.setPriority(slaService.correctPriority(category, request.priority()));
        slaService.applyDeadlines(ticket, LocalDateTime.now());

        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }


    public TicketCreationResponse changeStatus(User currentUser, Long ticketId, TicketStatus newStatus, String note){
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

        if(categoryRules.isNoteRequired(categoryName, newStatus) && (note == null || note.isBlank())){ //check empty string too
            throw new BusinessRuleException("A note is required to move this ticket to " + newStatus);
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setStatus(newStatus);
        if(slaService.isResolutionStatus(newStatus) && ticket.getResolvedAt() == null){
            ticket.setResolvedAt(LocalDateTime.now()); //SLA resolution clock stops
        }
        Ticket save = ticketRepository.save(ticket);

        recordHistory(save, currentUser, "STATUS_CHANGED", oldStatus.toString(), newStatus.toString());
        notifyTicketEvent(save,"STATUS_CHANGED", "Your ticket status changed to " + newStatus);
        if(save.getCustomer() != null) {
            emailService.sendTicketStatusChangedEmail(save.getCustomer().getEmail(), save.getId(), save.getTitle(), newStatus.toString());
        }
        log.info("Ticket {} status changed: {} -> {} by {}", save.getId(), oldStatus, newStatus, currentUser.getEmail());
        return ticketMapper.toResponse(save);
    }

    //allows admin to reopen a closed ticket
    public TicketCreationResponse reopenTicket(User currentUser, Long ticketId, String reason){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        if(ticket.getStatus() != TicketStatus.CLOSED){
            throw new BusinessRuleException("Only closed tickets can be reopened");
        }
        if(reason == null || reason.isBlank()){
            throw new BusinessRuleException("A reason is required to reopen a ticket");
        }
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setResolvedAt(null);
        slaService.applyDeadlines(ticket, LocalDateTime.now()); //fresh SLA clock for reopened tickets
        Ticket save = ticketRepository.save(ticket);

        recordHistory(save, currentUser, "REOPENED", "CLOSED", reason);
        return ticketMapper.toResponse(save);
    }

    //allows an agent to claim a ticket
    //transactional - method is treated as one transaction (all succeed or roll back)
    @Transactional
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

        //guard and query to update database
        int updated = ticketRepository.claimIfUnassigned(ticketId, agent, TicketStatus.ASSIGNED, ticket.getStatus(), LocalDateTime.now());
        if(updated == 0){
            log.warn("Claim Conflict: ticketId={}, agent={}, thread={}", ticketId, agent.getEmail(), Thread.currentThread().getName());
            throw new ConflictException("This ticket was just claimed by another agent");
        }

        //re-read because the update cleared the previous copy
        Ticket save = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID "+ ticketId + " is not found"));

        if(save.getFirstRespondedAt() == null){
            save.setFirstRespondedAt(LocalDateTime.now());//claim is the first response
            ticketRepository.save(save);
        }

        recordHistory(save, agent, "ASSIGNED", "UNASSIGNED", agent.getEmail());
        notifyTicketEvent(save, "ASSIGNED", "Your ticket has been assigned to an agent");
        log.info("Ticket {} claimed by agent {}", save.getId(), agent.getEmail());
        return ticketMapper.toResponse(save);
    }

    //allows the admin to reassign a ticket to a specific agent
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
    public TicketCreationResponse escalateTicket(User currentUser, Long ticketId, String note){
        if(note == null || note.isBlank()){
            throw new BusinessRuleException("A note is required to escalate a ticket");
        }
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        if (!categoryTicketWorkflow.isValidTransition(ticket.getCategory().getName(), ticket.getStatus(), TicketStatus.ESCALATED)){
            throw new BusinessRuleException("This ticket cannot be escalated from its current status");
        }

        ticket.setStatus(TicketStatus.ESCALATED);
        if(ticket.getPriority().ordinal() < TicketPriority.HIGH.ordinal()){
            ticket.setPriority(TicketPriority.HIGH);
        }
        slaService.applyDeadlines(ticket, ticket.getCreatedAt());
        Ticket save = ticketRepository.save(ticket);

        recordHistory(save, currentUser, "ESCALATED", null, note);
        return ticketMapper.toResponse(save);
    }

    public CommentResponse addComment(User currentUser, Long ticketId, String commentContent){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        checkCanUerAccessTicket(currentUser, ticket);
        if(ticket.getStatus() == TicketStatus.CLOSED){
            throw new BusinessRuleException("Cannot comment on a closed ticket");
        }

        if(currentUser.getRole() != Role.CUSTOMER && ticket.getFirstRespondedAt() == null){
            ticket.setFirstRespondedAt(LocalDateTime.now());  //first staff reply counts as the first response
            ticketRepository.save(ticket);
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

    public List<String> addAttachments(User currentUser, Long ticketId, List<MultipartFile> files){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));

        checkCanUerAccessTicket(currentUser, ticket);

        List<String> saved = fileStorageService.storeAll(files);

        for(int i=0; i < files.size(); i++){
            TicketAttachments attachment = new TicketAttachments();
            attachment.setTicket(ticket);
            attachment.setUploadedBy(currentUser);
            attachment.setFileName(saved.get(i));
            attachment.setFilePath(saved.get(i));
            attachment.setContentType(files.get(i).getContentType());

            attachment.setSha256(fileStorageService.sha256(saved.get(i)));
            attachment.setScanStatus(ScanStatus.PENDING);
            TicketAttachments stored = ticketAttachmentRepository.save(attachment);
            eventPublisher.publishEvent(new AttachmentStoredEvent(stored.getId()));
        }
        return saved;
    }

    public TicketAttachments getAttachment(User currentUser, Long attachmentId){
        TicketAttachments attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment with ID " + attachmentId + " is not found"));

        checkCanUerAccessTicket(currentUser, attachment.getTicket());
        return attachment;
    }

    public void deleteAttachment(User currentUser, Long attachmentId){
        TicketAttachments attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment with ID " + attachmentId + " is not found"));

        checkCanUerAccessTicket(currentUser, attachment.getTicket());
        ticketAttachmentRepository.delete(attachment);
    }

    public Page<TicketCreationResponse> searchTickets(TicketStatus status, Long categoryId, TicketPriority priority,
                                                      Long assignedToId, Pageable pageable){
        Specification<Ticket> spec = Specification
                .where(TicketSpecifications.hasStatus(status))
                .and(TicketSpecifications.hasCategory(categoryId))
                .and(TicketSpecifications.hasPriority(priority))
                .and(TicketSpecifications.isAssignedTo(assignedToId));

        //sends to repo to find and return all satisfied in spec and return with pagination info
        return ticketRepository.findAll(spec, pageable).map(ticketMapper::toResponse);

    }

    //SSE
    private void notifyTicketEvent(Ticket ticket, String eventType, String message){
        if(ticket.getCustomer() == null){
            //internal tickets have no customer
            return;
        }
        TicketNotification notification = new TicketNotification(ticket.getId(), ticket.getTitle(), eventType,
                message, LocalDateTime.now());
        emitterRegistry.sendToUser(ticket.getCustomer().getId(), "ticket-update", notification);
    }

    //used to track ticket history
    private void recordHistory(Ticket ticket, User changedBy, String action, String oldValue, String newValue){
        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setChangedBy(changedBy);
        history.setAction(action);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        ticketHistoryRepository.save(history);
    }

    public List<HistoryResponse> getTicketHistory(User currentUser, Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        checkCanUerAccessTicket(currentUser, ticket);

        return ticketHistoryRepository.findByTicketIdOrderByCreatedAtDesc(ticketId).stream()
                .map(TH -> new HistoryResponse(
                        TH.getAction(),
                        TH.getChangedBy() !=null ? TH.getChangedBy().getEmail() : "SYSTEM",
                        TH.getOldValue(),
                        TH.getNewValue(),
                        TH.getCreatedAt()))
                .toList();
    }


}
