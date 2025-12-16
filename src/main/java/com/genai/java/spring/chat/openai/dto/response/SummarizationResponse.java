package com.genai.java.spring.chat.openai.dto.response;

import java.util.List;

public record SummarizationResponse(
        List<String> actionItems,
        List<String> decisions,
        String errorMessage) {
}
