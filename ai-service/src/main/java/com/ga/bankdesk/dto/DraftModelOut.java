package com.ga.bankdesk.dto;

import java.util.List;

/**
 * raw returned data from OpenAI
 * contains the token (filtered)
 * @param title
 * @param description
 * @param categoryId
 * @param priority
 * @param missingInfo
 */

public record DraftModelOut(String title, String description, Long categoryId, String priority,
                            List<String> missingInfo) {
}