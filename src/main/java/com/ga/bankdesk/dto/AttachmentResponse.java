package com.ga.bankdesk.dto;

import com.ga.bankdesk.model.TicketAttachments;

import java.time.LocalDateTime;

public record AttachmentResponse (Long id, String fileName, String contentType, LocalDateTime uploadedAt,
                                  String scanStatus, String scanSummary, LocalDateTime scannedAt) {

    public static AttachmentResponse from(TicketAttachments a, boolean includeScan) {
        return new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType(), a.getCreatedAt(),
                includeScan ? (a.getScanStatus() == null ? "NOT_SCANNED" : a.getScanStatus().name()) : null,
                includeScan ? a.getScanSummary() : null,
                includeScan ? a.getScannedAt() : null);
    }
}
