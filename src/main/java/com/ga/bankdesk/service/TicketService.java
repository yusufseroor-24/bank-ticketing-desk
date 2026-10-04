package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.CreateInternalTicketRequest;
import com.ga.bankdesk.dto.CreateTicketRequest;
import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.enums.Role;
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
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;
    private final UserRepository userRepository;

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
        if(request.categoryId() != null){
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
}
