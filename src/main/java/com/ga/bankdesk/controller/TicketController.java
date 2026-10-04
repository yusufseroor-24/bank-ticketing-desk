package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.CreateInternalTicketRequest;
import com.ga.bankdesk.dto.CreateTicketRequest;
import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<TicketCreationResponse> createInternalTicket(@AuthenticationPrincipal AppUserDetails userDetails,
                                                                       @Valid @RequestBody CreateInternalTicketRequest request){
        TicketCreationResponse response = ticketService.internalTicketCreation(userDetails.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
