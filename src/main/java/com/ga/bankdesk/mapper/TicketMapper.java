package com.ga.bankdesk.mapper;

import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.model.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketCreationResponse toResponse(Ticket ticket){
        return new TicketCreationResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory().getName(),
                ticket.getSource(),
                ticket.getCustomer() !=null ? ticket.getCustomer().getEmail() : null, //system tickets dont have customer
                ticket.getAssignedTo() !=null ? ticket.getAssignedTo().getEmail() : null,
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getDueAt(),
                ticket.getCreatedAt()
        );

    }
}
