package com.ga.bankdesk.enums;

public enum KycStatus {
    NOT_STARTED, PENDING, IN_REVIEW, APPROVED, DECLINED, ABANDONED, EXPIRED;

    //Approved and declined are final
    public boolean isFinal(){
        return this == APPROVED || this == DECLINED;
    }

    //convert Didit status from text into enum
    public static KycStatus fromDidit(String raw){
            if (raw == null) return null;
            return switch (raw.trim().toLowerCase().replace('_', ' ')) {
                case "approved" -> APPROVED;
                case "declined" -> DECLINED;
                case "in review" -> IN_REVIEW;
                case "in progress", "not started", "not finished", "resubmitted", "awaiting user" -> PENDING;
                case "abandoned" -> ABANDONED;
                case "expired", "kyc expired" -> EXPIRED;
                default -> null;
            };
        }
}
