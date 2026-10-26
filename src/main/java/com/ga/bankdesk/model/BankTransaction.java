package com.ga.bankdesk.model;


import com.ga.bankdesk.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "bank_transaction", indexes = @Index(name = "idx_tx_customer_time", columnList = "customer_id, occurred_at"))
public class BankTransaction {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private User customer;

    //reference to the bank account involved in the transaction
    @Column(nullable = false)
    private String accountRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false, length = 3)
    private String currency;

    //transaction occurrence
    @Column(nullable = false)
    private LocalDateTime occurredAt;

    //record creation
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

}
