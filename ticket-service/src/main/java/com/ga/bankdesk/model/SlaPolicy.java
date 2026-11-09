package com.ga.bankdesk.model;

import com.ga.bankdesk.enums.TicketPriority;
import jakarta.persistence.*;
import lombok.Data;

// SLA promise for one category at one priority

@Data
@Entity
@Table(name="sla_policies", uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "priority"}))
public class SlaPolicy {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private int firstResponseMinutes;

    @Column(nullable = false)
    private int resolutionMinutes;
}
