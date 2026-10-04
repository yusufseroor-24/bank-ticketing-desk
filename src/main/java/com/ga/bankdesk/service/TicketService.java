package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.CreateTicketRequest;
import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.enums.SourceOfTicket;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.mapper.TicketMapper;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;

    public TicketCreationResponse createTicket(User customer, CreateTicketRequest request){
        Category category = categoryRepository.findById(request.CategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + request.CategoryId() + " is not found"));
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
}
