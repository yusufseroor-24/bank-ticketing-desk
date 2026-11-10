package com.ga.bankdesk.dto;

import java.util.List;

public record DraftCall(String text, String title, List<CategoryOption> categories){
}
