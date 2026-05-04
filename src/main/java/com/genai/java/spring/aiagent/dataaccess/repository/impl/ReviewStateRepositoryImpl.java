package com.genai.java.spring.aiagent.dataaccess.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aiagent.agent.records.Plan;
import com.genai.java.spring.aiagent.dataaccess.repository.ReviewStateRepository;
import com.genai.java.spring.aiagent.dto.ApprovalRequestData;
import com.genai.java.spring.aiagent.dto.PendingApproval;
import com.genai.java.spring.aiagent.dto.ReviewCheckpoint;
import com.genai.java.spring.aiagent.dto.ReviewState;
import com.genai.java.spring.aiagent.dto.ReviewStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class ReviewStateRepositoryImpl implements ReviewStateRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ReviewStateRepositoryImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public int upsertReviewState(ReviewState state) throws JsonProcessingException {
        String planJson = state.getPlan() != null ? objectMapper.writeValueAsString(state.getPlan()) : null;
        String pendingApprovalJson = state.getPendingApproval() != null ?
                objectMapper.writeValueAsString(state.getPendingApproval()) : null;
        String promptHistoryJson = state.getPromptHistory() != null ?
                objectMapper.writeValueAsString(state.getPromptHistory()) : null;
        String approvalHistoryJson = state.getApprovalHistory() != null ?
                objectMapper.writeValueAsString(state.getApprovalHistory()) : null;

        return jdbcTemplate.update(REVIEW_STATE_SNAPSHOT_UPSERT_SQL,
                state.getId(),
                state.getStatus().name(),
                state.getCheckpoint() != null ? state.getCheckpoint().name() : null,
                state.getFileName(),
                state.getReportMarkdown(),
                state.getErrorMessage(),
                state.getPromptSnapshot(),
                promptHistoryJson,
                planJson,
                pendingApprovalJson,
                approvalHistoryJson,
                java.sql.Timestamp.from(state.getCreatedAt()), // Convert Instant to java.sql.Timestamp for PostgreSQL
                java.sql.Timestamp.from(state.getUpdatedAt()));
    }

    @Override
    public ReviewState getReviewStateById(String id) {
        try {
            return jdbcTemplate.queryForObject(REVIEW_STATE_SNAPSHOT_SELECT_SQL,
                    (rs, rowNum) -> {
                        ReviewState state = new ReviewState();
                        state.setId(rs.getString("id"));
                        state.setStatus(ReviewStatus.valueOf(rs.getString("status")));

                        String checkpointStr = rs.getString("checkpoint");
                        if (checkpointStr != null) {
                            state.setCheckpoint(ReviewCheckpoint.valueOf(checkpointStr));
                        }

                        state.setFileName(rs.getString("file_name"));
                        state.setReportMarkdown(rs.getString("report_markdown"));
                        state.setErrorMessage(rs.getString("error_message"));
                        state.setPromptSnapshot(rs.getString("prompt_snapshot"));

                        // Deserialize the prompt history
                        setPromptHistory(id, rs, state);

                        // Deserialize the plan Json
                        setPlan(id, rs, state);

                        // Deserialize pending approval
                        setPendingApproval(id, rs, state);

                        // Deserialize approval history
                        setApprovalHistory(id, rs, state);

                        state.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                        state.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());

                        return state;
                    },
                    id);
        } catch (EmptyResultDataAccessException e) {
            log.warn("ReviewState not found for id={}", id);
            return null;
        } catch (Exception e) {
            log.error("Failed to load ReviewState id={}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to load review state", e);
        }
    }

    private void setPromptHistory(String id, ResultSet rs, ReviewState state) {
        try {
            String promptHistoryJson = rs.getString("prompt_history");
            if (promptHistoryJson != null) {
                Map<String, String> promptHistory = objectMapper.readValue(promptHistoryJson,
                        new TypeReference<Map<String, String>>() {
                        });
                state.setPromptHistory(promptHistory);
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize prompt history for id={}: {}", id, e.getMessage());
            state.setPromptHistory(new LinkedHashMap<>());
        }
    }

    private void setPlan(String id, ResultSet rs, ReviewState state) {
        try {
            String planJson = rs.getString("plan_json");
            if (planJson != null) {
                Plan plan = objectMapper.readValue(planJson, Plan.class);
                state.setPlan(plan);
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize plan for id={}: {}", id, e.getMessage());
        }
    }

    private void setPendingApproval(String id, ResultSet rs, ReviewState state) {
        try {
            String pendingApprovalJson = rs.getString("pending_approval");
            if (pendingApprovalJson != null) {
                PendingApproval pendingApproval = objectMapper.readValue(pendingApprovalJson, PendingApproval.class);
                state.setPendingApproval(pendingApproval);
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize pending approval for id={}: {}", id, e.getMessage());
        }
    }

    private void setApprovalHistory(String id, ResultSet rs, ReviewState state) {
        try {
            String approvalHistoryJson = rs.getString("approval_history");
            if (approvalHistoryJson != null) {
                Map<String, List<ApprovalRequestData>> approvalHistory = objectMapper.readValue(approvalHistoryJson,
                        new TypeReference<Map<String, List<ApprovalRequestData>>>() {
                        });
                state.setApprovalHistory(approvalHistory);
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize approval history for id={}: {}", id, e.getMessage());
            state.setApprovalHistory(new LinkedHashMap<>());
        }
    }


}
