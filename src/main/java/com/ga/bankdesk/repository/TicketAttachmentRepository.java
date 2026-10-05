package com.ga.bankdesk.repository;

import com.ga.bankdesk.model.TicketAttachments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachments, Long> {

    List<TicketAttachments> findByTicketId(Long ticketId);
}
