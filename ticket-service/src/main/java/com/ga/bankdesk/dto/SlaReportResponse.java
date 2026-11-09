package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.ReportStatus;
import com.ga.bankdesk.model.SlaReport;

import java.time.LocalDateTime;

public record SlaReportResponse(Long id, String period, ReportStatus status,
                                Integer totalTickets, Double resolutionCompliancePercent,
                                LocalDateTime generatedAt,
                                String generatedBy) {

    public static SlaReportResponse from(SlaReport r) {
        return new SlaReportResponse(r.getId(), r.getReportPeriod(), r.getStatus(),
                r.getTotalTickets(), r.getResolutionCompliancePercentage(), r.getGeneratedAt(),
                r.getGeneratedBy() == null ? "Scheduler" : r.getGeneratedBy().getFullName());
    }
}
