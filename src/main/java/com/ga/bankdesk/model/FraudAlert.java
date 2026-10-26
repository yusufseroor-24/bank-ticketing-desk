package com.ga.bankdesk.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
//uniqueConstraints & dedup_key make sure the database cant have the same dedup_key to prevent duplication
@Table(name = "fraud_alert", uniqueConstraints = @UniqueConstraint(columnNames = "dedup_key"))
public class FraudAlert {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //the customer the alert is about (links alerts to customers)
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_user_id")
    private User subjectUser;

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
    @ManyToOne
    @JoinColumn(name = "created_ticket_id")
    private Ticket createdTicket;

    //alert creation time
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
