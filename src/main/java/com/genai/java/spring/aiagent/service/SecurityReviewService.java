package com.genai.java.spring.aiagent.service;

import com.genai.java.spring.aiagent.dto.ReviewDto;
import org.springframework.web.multipart.MultipartFile;

public interface SecurityReviewService {
    String enqueueAndExecute(MultipartFile diagram);

    ReviewDto getSecurityReview(String id);

    String followUpWithVision(String id, String question);
}
