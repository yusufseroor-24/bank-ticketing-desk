package com.ga.bankdesk.dto;

import java.util.List;

public record DraftResponse(String title, String description, Long categoryId, String categoryName,
                            String priority, List<String> missingInfo){
}