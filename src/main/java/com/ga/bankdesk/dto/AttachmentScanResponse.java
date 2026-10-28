package com.ga.bankdesk.dto;

import com.ga.bankdesk.model.TicketAttachments;

import java.time.LocalDateTime;

public record AttachmentScanResponse(Long id, String fileName, String sha256, String scanStatus,
                                     String scanSummary, LocalDateTime scannedAt) {
    public static AttachmentScanResponse from(TicketAttachments a) {
        return new AttachmentScanResponse(
                a.getId(),
                a.getFileName(),
                a.getSha256(),
                a.getScanStatus() == null ? "NOT_SCANNED" : a.getScanStatus().name(),
                a.getScanSummary(),
                a.getScannedAt());
    }
}
