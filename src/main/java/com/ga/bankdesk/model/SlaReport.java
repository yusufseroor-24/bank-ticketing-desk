package com.ga.bankdesk.model;


import com.ga.bankdesk.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "sla_reports", uniqueConstraints = @UniqueConstraint(columnNames = "report_period"))
public class SlaReport {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_period", nullable = false, length = 7)
    private String reportPeriod; //unique: one report per month

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    private String filePath;

    private Integer totalTickets;

    private Double resolutionCompliancePercentage;

    @ManyToOne
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
