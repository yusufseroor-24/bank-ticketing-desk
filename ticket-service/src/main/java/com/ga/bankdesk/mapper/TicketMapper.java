package com.ga.bankdesk.mapper;

import com.ga.bankdesk.client.IdentityClient;
import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.dto.UserSummary;
import com.ga.bankdesk.model.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class TicketMapper {

    //emails and the KYC status now live in identity-service, so they are fetched by user id
    private final IdentityClient identityClient;

    //one call to identity-service for all the users of these tickets (customer, creator, assigned agent)
    public Map<Long, UserSummary> lookupUsers(Collection<Ticket> tickets){
        Set<Long> ids = new HashSet<>();
        for (Ticket t : tickets) {
            if (t.getCustomerId() != null) ids.add(t.getCustomerId());
            if (t.getCreatedById() != null) ids.add(t.getCreatedById());
            if (t.getAssignedToId() != null) ids.add(t.getAssignedToId());
        }
        return identityClient.getUsers(ids).stream()
                .collect(Collectors.toMap(UserSummary::id, u -> u));
    }

    public TicketCreationResponse toResponse(Ticket ticket){
        return toResponse(ticket, lookupUsers(List.of(ticket)));
    }

    public TicketCreationResponse toResponse(Ticket ticket, Map<Long, UserSummary> users){
        UserSummary customer = ticket.getCustomerId() != null ? users.get(ticket.getCustomerId()) : null;
        UserSummary createdBy = ticket.getCreatedById() != null ? users.get(ticket.getCreatedById()) : null;
        UserSummary assignedTo = ticket.getAssignedToId() != null ? users.get(ticket.getAssignedToId()) : null;
        return new TicketCreationResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory().getName(),
                ticket.getSource(),
                customer != null ? customer.email() : null, //system tickets dont have customer
                createdBy != null ? createdBy.email() : null,
                assignedTo != null ? assignedTo.email() : null,
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getDueAt(),
                ticket.getCreatedAt(),
                ticket.getFirstResponseDueAt(),
                ticket.getFirstRespondedAt(),
                ticket.getResolvedAt(),
                customer != null ? customer.kycStatus() : null
        );
    }
}
