package com.genai.java.spring.aiagent.mapper;

import com.genai.java.spring.aiagent.dto.ReviewDto;
import com.genai.java.spring.aiagent.dto.ReviewState;
import org.springframework.stereotype.Component;

@Component
public class ReviewStateMapper {

    public ReviewDto toDto(ReviewState reviewState) {
        return ReviewDto.builder()
                .id(reviewState.getId())
                .status(reviewState.getStatus())
                .reportMarkdown(reviewState.getReportMarkdown())
                .errorMessage(reviewState.getErrorMessage())
                .createdAt(reviewState.getCreatedAt())
                .updatedAt(reviewState.getUpdatedAt())
                .build();
    }
}
