package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.*;
import com.ga.bankdesk.enums.ScanStatus;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.model.TicketAttachments;
import com.ga.bankdesk.security.CurrentUser;
import com.ga.bankdesk.service.AttachmentScanService;
import com.ga.bankdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final AttachmentScanService attachmentScanService;

    @PostMapping("/create")
    public ResponseEntity<TicketCreationResponse> createTicket(@AuthenticationPrincipal CurrentUser currentUser,
                                                               @Valid @RequestBody CreateTicketRequest request){
        TicketCreationResponse response = ticketService.createTicket(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ticketId}")
    public TicketCreationResponse getTicketById(@AuthenticationPrincipal CurrentUser currentUser,
                                                                @PathVariable Long ticketId){
        return ticketService.getTicketById(currentUser, ticketId);
    }

    @GetMapping("/my-tickets")
    public List<TicketCreationResponse> myTickets(@AuthenticationPrincipal CurrentUser currentUser){
        return ticketService.myTickets(currentUser);
    }

    @PostMapping("/create/internal")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<TicketCreationResponse> createInternalTicket(@AuthenticationPrincipal CurrentUser currentUser,
                                                                       @Valid @RequestBody CreateInternalTicketRequest request){
        TicketCreationResponse response = ticketService.internalTicketCreation(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{ticketId}/status")
    @PreAuthorize(("hasAnyRole('AGENT', 'ADMIN')"))
    public TicketCreationResponse changeStatus(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long ticketId, @Valid @RequestBody ChangeTicketStatusRequest request){
        return ticketService.changeStatus(currentUser, ticketId, request.newStatus(), request.note());
    }

    @PutMapping("/{ticketId}/claim")
    @PreAuthorize("hasAnyRole('AGENT')")
    public TicketCreationResponse claimTicket(@PathVariable Long ticketId, @AuthenticationPrincipal CurrentUser currentUser){
        return ticketService.claimTicket(currentUser, ticketId);
    }

    @PutMapping("/{ticketId}/escalate")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketCreationResponse escalateTicket(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long ticketId, @Valid @RequestBody EscalateTicketRequest request){
        return  ticketService.escalateTicket(currentUser, ticketId, request.note());
    }

    @PostMapping("/{ticketId}/comments")
    public CommentResponse addComment(@AuthenticationPrincipal CurrentUser currentUser,
                                      @PathVariable Long ticketId,
                                      @Valid @RequestBody AddCommentRequest request){
        return ticketService.addComment(currentUser, ticketId, request.commentContent());
    }

    @GetMapping("/{ticketId}/comments")
    public List<CommentResponse> listComments(@AuthenticationPrincipal CurrentUser currentUser,
                                              @PathVariable Long ticketId){
        return ticketService.listComments(currentUser, ticketId);
    }

    @PostMapping("/{ticketId}/attachments")
    public List<String> uploadAttachments(@AuthenticationPrincipal CurrentUser currentUser,
                                          @PathVariable Long ticketId,
                                          @RequestParam("file") List<MultipartFile> file){
        return ticketService.addAttachments(currentUser, ticketId, file);
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@AuthenticationPrincipal CurrentUser currentUser,
                                                       @PathVariable Long attachmentId) throws IOException {
        TicketAttachments attachment = ticketService.getAttachment(currentUser, attachmentId);
        if (attachment.getScanStatus() == ScanStatus.MALICIOUS) {
            throw new BusinessRuleException("This file was flagged as malicious and cannot be downloaded");
        }
        Path path = Paths.get("uploads").resolve(attachment.getFilePath());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(resource);

    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@AuthenticationPrincipal CurrentUser currentUser,
                                                 @PathVariable Long attachmentId){
        ticketService.deleteAttachment(currentUser, attachmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public Page<TicketCreationResponse> searchTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long assignedToId,
            Pageable pageable){
        return ticketService.searchTickets(status, categoryId, priority, assignedToId, pageable);
    }

    @GetMapping("/{ticketId}/history")
    public List<HistoryResponse> getHistory(@AuthenticationPrincipal CurrentUser currentUser,
                                            @PathVariable Long ticketId){
        return ticketService.getTicketHistory(currentUser, ticketId);
    }

    @GetMapping("/{ticketId}/attachments")
    public List<AttachmentResponse> listAttachments(@AuthenticationPrincipal CurrentUser currentUser,
                                                    @PathVariable Long ticketId) {
        return ticketService.listAttachments(currentUser, ticketId);
    }


    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PostMapping("/attachments/{attachmentId}/rescan")
    public ResponseEntity<Void> rescan(@AuthenticationPrincipal CurrentUser currentUser,
                                       @PathVariable Long attachmentId) {
        TicketAttachments attachment = ticketService.getAttachment(currentUser, attachmentId);
        attachmentScanService.rescan(attachment.getId());
        return ResponseEntity.accepted().build();
    }
}
