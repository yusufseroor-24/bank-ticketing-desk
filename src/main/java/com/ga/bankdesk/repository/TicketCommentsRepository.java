package com.ga.bankdesk.repository;

import com.ga.bankdesk.model.TicketComments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentsRepository extends JpaRepository<TicketComments, Long> {

    List<TicketComments> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
