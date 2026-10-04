package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.CreateTicketRequest;
import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
