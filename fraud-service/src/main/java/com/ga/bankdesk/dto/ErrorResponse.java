package com.ga.bankdesk.dto;

import java.time.LocalDateTime;

//using record to create final and immutable fields
//used to shape the error response sent back to the client

public record ErrorResponse(LocalDateTime timestamp, int status,
                            String error, String message,
                            String path) {
}
