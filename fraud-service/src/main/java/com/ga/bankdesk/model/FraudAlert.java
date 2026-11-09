package com.ga.bankdesk.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
//uniqueConstraints & dedup_key make sure the database cant have the same dedup_key to prevent duplication
@Table(name = "fraud_alerts", uniqueConstraints = @UniqueConstraint(columnNames = "dedup_key"))
public class FraudAlert {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //the customer the alert is about (links alerts to customers)
    @Column(name = "subject_user_id", nullable = false)
    private Long subjectUserId;

    //which rule triggered the alert
    @ManyToOne(optional = false)
    @JoinColumn(name = "rule_id")
    private FraudRule rule;

    //which transaction triggered the alert (links alert to BankTransaction)
    @ManyToOne
    @JoinColumn(name = "trigger_transaction_id")
    private BankTransaction triggerTransaction;

    //unique identifier, one alert, per rule, per customer, per time window
    @Column(name = "dedup_key", nullable = false, length = 120)
    private String dedupKey;

    //links the alert to the ticket (alert to ticket ID)
    @Column(name = "created_ticket_id")
    private Long createdTicketId; //the ticket lives in ticket-service

    //alert creation time
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
