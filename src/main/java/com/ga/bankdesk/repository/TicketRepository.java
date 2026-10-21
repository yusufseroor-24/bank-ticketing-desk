package com.ga.bankdesk.repository;

import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

//JpaSpecificationExecutor - build queries dynamically
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    List<Ticket> findByCustomerId(Long customerId);


    //atomic claim, assigns, changes status, increments version, and updates timestamp of a ticket if conditions are met

    //flushAutomatically writes pending changes first and clearAutomatically drops the copies
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    //JPQL Query that allows change and increases the version
    //AND t.status = :expectedStatus prevents agent from claiming a ticket that has moved to a different status
    // and only allow claim to OPEN tickets
    @Query("""
            UPDATE Ticket t
            SET t.assignedTo = :agent,
                t.status = :newStatus,
                t.version = t.version + 1,
                t.updatedAt = :now
            WHERE t.id = :ticketId
                AND t.assignedTo IS NULL
                AND t.status = :expectedStatus
""")
    int claimIfUnassigned(@Param("ticketId") Long ticketId,
                          @Param("agent") User agent,
                          @Param("newStatus")TicketStatus newStatus,
                          @Param("expectedStatus") TicketStatus expectedStatus,
                          @Param("now") LocalDateTime now);
}
