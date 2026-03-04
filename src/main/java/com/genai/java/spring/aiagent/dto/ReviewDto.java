package com.genai.java.spring.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDto {
    private String id;
    private ReviewStatus status;
    private String reportMarkdown; // null until DONE
    private String errorMessage;   // set when ERROR
    private Instant createdAt;
    private Instant updatedAt;
}
