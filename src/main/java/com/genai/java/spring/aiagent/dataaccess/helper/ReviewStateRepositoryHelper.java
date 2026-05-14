package com.genai.java.spring.aiagent.dataaccess.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.genai.java.spring.aiagent.dataaccess.repository.ReviewStateRepository;
import com.genai.java.spring.aiagent.dto.ReviewState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReviewStateRepositoryHelper {

    private final ReviewStateRepository reviewStateRepository;

    public ReviewStateRepositoryHelper(ReviewStateRepository reviewStateRepository) {
        this.reviewStateRepository = reviewStateRepository;
    }

    /**
     * Save ReviewState to database (insert or update)
     */
    public void saveReviewState(ReviewState state) {
        try {
            int updated = reviewStateRepository.upsertReviewState(state);
            if (updated > 0) {
                log.info("Saved ReviewState id={} status={} checkpoint={}",
                        state.getId(), state.getStatus(), state.getCheckpoint());
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to save ReviewState id={}: {}", state.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to persist review state", e);
        }
    }

    /**
     * Load ReviewState from database
     */
    public ReviewState loadReviewState(String id) {
        try {
            return reviewStateRepository.getReviewStateById(id);
        } catch (EmptyResultDataAccessException e) {
            log.warn("ReviewState not found for id={}", id);
            return null;
        } catch (Exception e) {
            log.error("Failed to load ReviewState id={}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to load review state", e);
        }
    }
}
