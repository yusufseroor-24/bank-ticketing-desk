package com.ga.bankdesk.service;
/**
 *Published after an attachment is saved. Carries only the ID so the scanner loads a fresh copy.
 * @param attachmentId
 */
public record AttachmentStoredEvent(Long attachmentId) {
}
