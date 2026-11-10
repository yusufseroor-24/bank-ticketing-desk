package com.ga.bankdesk.dto;

import java.util.List;

/**
 * what is returned to ticket-service and to customer
 * built after DraftModelOut
 * token are restored
 * Add category name
 *
 * @param title
 * @param description
 * @param categoryId
 * @param categoryName
 * @param priority
 * @param missingInfo
 */

public record DraftResponse(String title, String description, Long categoryId, String categoryName,
                            String priority, List<String> missingInfo) {
}