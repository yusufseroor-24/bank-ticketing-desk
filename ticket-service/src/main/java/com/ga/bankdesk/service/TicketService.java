package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.*;
import com.ga.bankdesk.enums.*;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ConflictException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.mapper.CommentMapper;
import com.ga.bankdesk.mapper.TicketMapper;
import com.ga.bankdesk.model.*;
import com.ga.bankdesk.notifications.LiveNotifier;
import com.ga.bankdesk.client.IdentityClient;
import com.ga.bankdesk.exception.ExternalServiceException;
import com.ga.bankdesk.security.CurrentUser;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;
    private final IdentityClient identityClient;
    private final CategoryTicketWorkflow categoryTicketWorkflow;
    private final AgentCategoryRepository agentCategoryRepository;
    private final TicketCommentsRepository ticketCommentsRepository;
    private final CommentMapper commentMapper;
    private final FileStorageService fileStorageService;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final SlaService slaService;
    private final CategoryRules categoryRules;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final LiveNotifier liveNotifier;


    public TicketCreationResponse createTicket(CurrentUser customer, CreateTicketRequest request){
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + request.categoryId() + " is not found"));
        //customer must be verified (KYC APPROVED) before opening tickets
        boolean needsKyc = customer.getRole() == Role.CUSTOMER && !category.getName().equals("KYC_REVIEW")
                && kycStatusOf(customer.getId()) != KycStatus.APPROVED;
        if (needsKyc) {
            throw new BusinessRuleException("Please verify your identity (KYC) before opening a ticket. "
                    + "You can still open a KYC Review ticket if you need help.");
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(category);
        ticket.setCustomerId(customer.getId());
        ticket.setCreatedById(customer.getId());
        ticket.setSource(SourceOfTicket.CUSTOMER);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(slaService.correctPriority(category, request.priority()));
        slaService.applyDeadlines(ticket, LocalDateTime.now());

        Ticket save = ticketRepository.save(ticket);

        recordHistory(save, customer, "CREATED", null, save.getStatus().toString());
        log.info("Ticket created: id={}, category={}, customer={}", save.getId(), category.getName(), customer.getEmail());
        emailService.sendTicketCreatedEmail(customer.getEmail(), save.getId(), save.getTitle(), category.getName());
        liveNotifier.send(liveNotifier.staffFor(save), null, save.getId(), save.getTitle(),
                "NEW_TICKET", "New " + category.getName().replace('_', ' ') + " ticket: " + save.getTitle());
        return ticketMapper.toResponse(save);
    }

    public TicketCreationResponse getTicketById(CurrentUser user, Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        if(user.getRole() == Role.CUSTOMER){
            boolean isOwner = ticket.getCustomerId() != null && ticket.getCustomerId().equals(user.getId());
            if(!isOwner){
                //to avoid revealing that it exists
                throw new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found");
            }
        }
        return ticketMapper.toResponse(ticket);
    }

    public List<TicketCreationResponse> myTickets(CurrentUser user){
        List<Ticket> tickets = ticketRepository.findByCustomerId(user.getId());
        Map<Long, UserSummary> users = ticketMapper.lookupUsers(tickets);
        return tickets.stream()
                .map(t -> ticketMapper.toResponse(t, users))
                .toList();
    }

    //employye/agent ticket creation (internal)
    public TicketCreationResponse internalTicketCreation(CurrentUser agent, CreateInternalTicketRequest request){
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + request.categoryId() + " is not found"));

        Long customerId = null; //optional
        if(request.customerId() != null){
            identityClient.findUser(request.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer with ID " + request.customerId() + " is not found"));
            customerId = request.customerId();
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(category);
        ticket.setCustomerId(customerId);
        ticket.setCreatedById(agent.getId());
        ticket.setSource(SourceOfTicket.INTERNAL);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(slaService.correctPriority(category, request.priority()));
        slaService.applyDeadlines(ticket, LocalDateTime.now());

        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }


    public TicketCreationResponse changeStatus(CurrentUser currentUser, Long ticketId, TicketStatus newStatus, String note){
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
        if(save.getCustomerId() != null) {
            emailOf(save.getCustomerId()).ifPresent(email ->
                    emailService.sendTicketStatusChangedEmail(email, save.getId(), save.getTitle(), newStatus.toString()));
        }
        log.info("Ticket {} status changed: {} -> {} by {}", save.getId(), oldStatus, newStatus, currentUser.getEmail());
        return ticketMapper.toResponse(save);
    }

    //allows admin to reopen a closed ticket
    public TicketCreationResponse reopenTicket(CurrentUser currentUser, Long ticketId, String reason){
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
    public TicketCreationResponse claimTicket(CurrentUser agent, Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));

        //checks if the agent is authorized to view the category tickets / claim
        boolean allowedCategory = agentCategoryRepository.existsByAgentIdAndCategoryId(agent.getId(), ticket.getCategory().getId());
        if(!allowedCategory){
            throw new BusinessRuleException("You cannot view this category tickets, out of scope category");
        }
        //check if it's already assigned by another agent (and prevent double claim)
        if(ticket.getAssignedToId() != null){
            throw new ConflictException("This ticket has already been assigned to an agent");
        }
        //check if its already assigned and not opened
        if(!categoryTicketWorkflow.isValidTransition(ticket.getCategory().getName(), ticket.getStatus(), TicketStatus.ASSIGNED)){
            throw new BusinessRuleException("This ticket can't be claimed from its current status");
        }

        //guard and query to update database
        int updated = ticketRepository.claimIfUnassigned(ticketId, agent.getId(), TicketStatus.ASSIGNED, ticket.getStatus(), LocalDateTime.now());
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

        UserSummary agent = identityClient.findUser(newAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent with ID " + newAgentId + " is not found"));

        if(agent.role() != Role.AGENT){
            throw new BusinessRuleException("The ticket can only be assigned to a user agent role");
        }
        if(agent.status() != UserStatus.ACTIVE){
            throw new BusinessRuleException("The ticket cannot be assigned to an inactive agent");
        }
        //checks if the agent is authorized to view the category tickets / claim
        boolean allowedCategory = agentCategoryRepository.existsByAgentIdAndCategoryId(agent.id(), ticket.getCategory().getId());
        if(!allowedCategory){
            throw new BusinessRuleException("The agent cannot view this category tickets, out of scope category");
        }

        ticket.setAssignedToId(agent.id());
        if(ticket.getStatus() == TicketStatus.OPEN){
            ticket.setStatus(TicketStatus.ASSIGNED);
        }
        Ticket save = ticketRepository.save(ticket);
        return ticketMapper.toResponse(save);
    }

    //same workflow but changes priority to high and updated due time according to SLA priority
    public TicketCreationResponse escalateTicket(CurrentUser currentUser, Long ticketId, String note){
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
        Set<Long> w = new LinkedHashSet<>(liveNotifier.admins());
        w.addAll(liveNotifier.staffFor(save));
        liveNotifier.send(w, currentUser.getId(), save.getId(), save.getTitle(),
                "ESCALATED", "Ticket #" + save.getId() + " was escalated: " + note);
        if (save.getCustomerId() != null) {
            liveNotifier.send(Set.of(save.getCustomerId()), null, save.getId(), save.getTitle(),
                    "ESCALATED", "Your ticket has been escalated for further review");
        }
        return ticketMapper.toResponse(save);
    }

    public CommentResponse addComment(CurrentUser currentUser, Long ticketId, String commentContent){
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
        comment.setAuthorId(currentUser.getId());
        comment.setCommentContent(commentContent);

        TicketComments save = ticketCommentsRepository.save(comment);
        return commentMapper.toResponse(save);
    }

    public List<CommentResponse> listComments(CurrentUser currentUser, Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));

        checkCanUerAccessTicket(currentUser, ticket);
        return commentMapper.toResponses(ticketCommentsRepository.findByTicketIdOrderByCreatedAtAsc(ticketId));
    }

    //permission check: customers can only edit their own ticket but staff can touch any
    private void checkCanUerAccessTicket(CurrentUser currentUser, Ticket ticket){
        if(currentUser.getRole() == Role.CUSTOMER){
            boolean isOwner = ticket.getCustomerId() != null && ticket.getCustomerId().equals(currentUser.getId());
            if(!isOwner){
                throw  new ResourceNotFoundException("Ticket with ID " + ticket.getId() + " was not found");
            }
        }
    }

    public List<String> addAttachments(CurrentUser currentUser, Long ticketId, List<MultipartFile> files){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));

        checkCanUerAccessTicket(currentUser, ticket);

        List<String> saved = fileStorageService.storeAll(files);

        for(int i=0; i < files.size(); i++){
            TicketAttachments attachment = new TicketAttachments();
            attachment.setTicket(ticket);
            attachment.setUploadedById(currentUser.getId());
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

    public TicketAttachments getAttachment(CurrentUser currentUser, Long attachmentId){
        TicketAttachments attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment with ID " + attachmentId + " is not found"));

        checkCanUerAccessTicket(currentUser, attachment.getTicket());
        return attachment;
    }

    public void deleteAttachment(CurrentUser currentUser, Long attachmentId){
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
        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        Map<Long, UserSummary> users = ticketMapper.lookupUsers(page.getContent());
        return page.map(t -> ticketMapper.toResponse(t, users));

    }

    //SSE
    private void notifyTicketEvent(Ticket ticket, String eventType, String message){
        if(ticket.getCustomerId() == null){
            //internal tickets have no customer
            return;
        }
        liveNotifier.send(Set.of(ticket.getCustomerId()), null, ticket.getId(), ticket.getTitle(), eventType, message);
    }

    //used to track ticket history
    private void recordHistory(Ticket ticket, CurrentUser changedBy, String action, String oldValue, String newValue){
        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setChangedById(changedBy.getId());
        history.setAction(action);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        ticketHistoryRepository.save(history);
    }

    public List<HistoryResponse> getTicketHistory(CurrentUser currentUser, Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        checkCanUerAccessTicket(currentUser, ticket);

        List<TicketHistory> rows = ticketHistoryRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        Map<Long, String> emails = emailsOf(rows.stream().map(TicketHistory::getChangedById)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        return rows.stream()
                .map(TH -> new HistoryResponse(
                        TH.getAction(),
                        TH.getChangedById() != null ? emails.getOrDefault(TH.getChangedById(), "UNKNOWN") : "SYSTEM",
                        TH.getOldValue(),
                        TH.getNewValue(),
                        TH.getCreatedAt()))
                .toList();
    }

    public List<AttachmentResponse> listAttachments(CurrentUser currentUser, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + ticketId + " is not found"));
        checkCanUerAccessTicket(currentUser, ticket);
        boolean staff = currentUser.getRole() != Role.CUSTOMER;
        return ticketAttachmentRepository.findByTicketId(ticketId).stream()
                .map(a -> AttachmentResponse.from(a, staff))
                .toList();
    }

    //the customer's current KYC status, asked from identity-service
    private KycStatus kycStatusOf(Long userId){
        return identityClient.findUser(userId).map(UserSummary::kycStatus).orElse(null);
    }

    //email of a user from identity-service (empty if not found or identity is down, an email must never break the request)
    private Optional<String> emailOf(Long userId){
        try {
            return identityClient.findUser(userId).map(UserSummary::email);
        } catch (ExternalServiceException e) {
            log.warn("Could not get the email of user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    //id -> email for many users in one call
    private Map<Long, String> emailsOf(Set<Long> ids){
        return identityClient.getUsers(ids).stream()
                .collect(Collectors.toMap(UserSummary::id, UserSummary::email));
    }
}
