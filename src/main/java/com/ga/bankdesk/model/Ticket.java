package com.ga.bankdesk.model;

import com.ga.bankdesk.enums.SourceOfTicket;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT") //unlimited text length
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceOfTicket source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer; //who the ticket is about, if (internal) will be null

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy; //null for system

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    //resolution deadline
    @Column
    private LocalDateTime dueAt;

    @Column
    private LocalDateTime firstResponseDueAt;

    //when an agent first acted on the ticket
    @Column
    private LocalDateTime firstRespondedAt;

    //when the agent reached a final stage status
    @Column
    private LocalDateTime resolvedAt;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    //0 = none, 1 = 75%, 2 = 90%, 3 = breached
    private Integer slaNotifyLevel = 0;

    @Column
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version //track version of the entity
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0") //default 0 for existing rows
    private Long version;

}
