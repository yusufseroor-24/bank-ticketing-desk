package com.ga.bankdesk.model;

import com.ga.bankdesk.enums.ScanStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "attachments")
public class TicketAttachments {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    private String contentType;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(length = 64)
    //has of file
    private String sha256;

    @Enumerated(EnumType.STRING)
    private ScanStatus scanStatus;

    @Column(length = 300)
    private String scanSummary;

    private LocalDateTime scannedAt;
}
