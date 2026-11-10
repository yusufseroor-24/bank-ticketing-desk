package com.ga.bankdesk.dto;

import java.util.List;

public record SummaryResponse(String summary, List<String> keyPoints, List<String> openItems) {
}