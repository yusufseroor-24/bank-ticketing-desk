package com.ga.bankdesk.dto;

import java.util.List;

public record SummaryCall(String title, String description, String status, String category,
                          String priority, List<CommentItem> comments){
}
