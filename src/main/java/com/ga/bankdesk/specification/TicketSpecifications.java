package com.ga.bankdesk.specification;

import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.Ticket;
import org.springframework.data.jpa.domain.Specification;

//filter ticket dynamically instead of separate repo methods
public class TicketSpecifications {

    public static Specification<Ticket> hasStatus(TicketStatus status){
        //root = The entity (Ticket) //cb SQL query/operation
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasCategory(Long categoryId){
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> isAssignedTo(Long agentId) {
        return (root, query, cb) -> agentId == null ? null : cb.equal(root.get("assignedTo").get("id"), agentId);
    }

}
