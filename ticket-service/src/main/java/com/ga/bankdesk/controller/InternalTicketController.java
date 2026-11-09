package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.SystemTicketRequest;
import com.ga.bankdesk.dto.SystemTicketResponse;
import com.ga.bankdesk.service.SystemTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//service-to-service only, used by fraud-service
@RequiredArgsConstructor
@RestController
@RequestMapping("/internal/tickets")
public class InternalTicketController {

    private final SystemTicketService systemTicketService;

    @PostMapping("/system")
    public ResponseEntity<SystemTicketResponse> createSystemTicket(@Valid @RequestBody SystemTicketRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(new SystemTicketResponse(systemTicketService.create(request)));
    }
}
