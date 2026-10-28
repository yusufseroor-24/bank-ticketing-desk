package com.ga.bankdesk.repository;

import com.ga.bankdesk.enums.ScanStatus;
import com.ga.bankdesk.model.TicketAttachments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachments, Long> {

    List<TicketAttachments> findByTicketId(Long ticketId);

    List<TicketAttachments> findByScanStatus(ScanStatus status);

    //return 0 if the attachment was deleted while scanning so finished scan never bring deleted row
    @Transactional
    @Modifying
    @Query("UPDATE TicketAttachments a SET a.scanStatus = :status, a.scanSummary = :summary, a.scannedAt = :at WHERE a.id = :id")
    int updateScan(@Param("id") Long id, @Param("status") ScanStatus status,
                   @Param("summary") String summary, @Param("at") LocalDateTime at);
}
