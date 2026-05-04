package com.genai.java.spring.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Stores approval/rejection data for audit trail
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalRequestData {
    /**
     * Whether this approval was approved (true) or rejected (false)
     */
    private boolean approved;

    /**
     * Human's note explaining the decision
     */
    private String note;

    /**
     * Edits to apply to the prompt (if approved with changes)
     * Key: edit identifier (e.g., "service_1", "edge_2")
     * Value: new value or definition
     */
    private Map<String, Object> edits;

    /**
     * When this approval was recorded
     */
    private Instant approvedAt;

    /**
     * Which checkpoint this approval was for
     */
    private String checkpointName;
}
