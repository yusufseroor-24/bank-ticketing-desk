package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.*;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.TicketAttachments;
import com.ga.bankdesk.security.AppUserDetails;
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

    @PostMapping("/create")
    public ResponseEntity<TicketCreationResponse> createTicket(@AuthenticationPrincipal AppUserDetails userDetails,
                                                               @Valid @RequestBody CreateTicketRequest request){
        TicketCreationResponse response = ticketService.createTicket(userDetails.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ticketId}")
    public TicketCreationResponse getTicketById(@AuthenticationPrincipal AppUserDetails userDetails,
                                                                @PathVariable Long ticketId){
        return ticketService.getTicketById(userDetails.getUser(), ticketId);
    }

    @GetMapping("/my-tickets")
    public List<TicketCreationResponse> myTickets(@AuthenticationPrincipal AppUserDetails userDetails){
        return ticketService.myTickets(userDetails.getUser());
    }

    @PostMapping("/create/internal")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<TicketCreationResponse> createInternalTicket(@AuthenticationPrincipal AppUserDetails userDetails,
                                                                       @Valid @RequestBody CreateInternalTicketRequest request){
        TicketCreationResponse response = ticketService.internalTicketCreation(userDetails.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{ticketId}/status")
    @PreAuthorize(("hasAnyRole('AGENT', 'ADMIN')"))
    public TicketCreationResponse changeStatus(@AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long ticketId, @Valid @RequestBody ChangeTicketStatusRequest request){
        return ticketService.changeStatus(userDetails.getUser(), ticketId, request.newStatus(), request.note());
    }

    @PutMapping("/{ticketId}/claim")
    @PreAuthorize("hasAnyRole('AGENT')")
    public TicketCreationResponse claimTicket(@PathVariable Long ticketId, @AuthenticationPrincipal AppUserDetails userDetails){
        return ticketService.claimTicket(userDetails.getUser(), ticketId);
    }

    @PutMapping("/{ticketId}/escalate")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketCreationResponse escalateTicket(@AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long ticketId, @Valid @RequestBody EscalateTicketRequest request){
        return  ticketService.escalateTicket(userDetails.getUser(), ticketId, request.note());
    }

    @PostMapping("/{ticketId}/comments")
    public CommentResponse addComment(@AuthenticationPrincipal AppUserDetails userDetails,
                                      @PathVariable Long ticketId,
                                      @Valid @RequestBody AddCommentRequest request){
        return ticketService.addComment(userDetails.getUser(), ticketId, request.commentContent());
    }

    @GetMapping("/{ticketId}/comments")
    public List<CommentResponse> listComments(@AuthenticationPrincipal AppUserDetails userDetails,
                                              @PathVariable Long ticketId){
        return ticketService.listComments(userDetails.getUser(), ticketId);
    }

    @PostMapping("/{ticketId}/attachments")
    public List<String> uploadAttachments(@AuthenticationPrincipal AppUserDetails userDetails,
                                          @PathVariable Long ticketId,
                                          @RequestParam("file") List<MultipartFile> file){
        return ticketService.addAttachments(userDetails.getUser(), ticketId, file);
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@AuthenticationPrincipal AppUserDetails userDetails,
                                                       @PathVariable Long attachmentId) throws IOException {
        TicketAttachments attachment = ticketService.getAttachment(userDetails.getUser(), attachmentId);
        Path path = Paths.get("uploads").resolve(attachment.getFilePath());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(resource);

    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@AuthenticationPrincipal AppUserDetails userDetails,
                                                 @PathVariable Long attachmentId){
        ticketService.deleteAttachment(userDetails.getUser(), attachmentId);
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
    public List<HistoryResponse> getHistory(@AuthenticationPrincipal AppUserDetails userDetails,
                                            @PathVariable Long ticketId){
        return ticketService.getTicketHistory(userDetails.getUser(), ticketId);
    }
}
