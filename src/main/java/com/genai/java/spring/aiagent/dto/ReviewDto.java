package com.genai.java.spring.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private ReviewCheckpoint checkpoint;
    private PendingApproval pendingApproval;

    /**
     * History of prompt snapshots by checkpoint name (for audit trail)
     * Key: checkpoint name (e.g., "AFTER_PLAN", "BEFORE_DIAGRAM_APPROVAL")
     * Value: serialized prompt at that checkpoint
     */
    @Builder.Default
    private Map<String, String> promptHistory = new LinkedHashMap<>();

    /**
     * Approval requests keyed by approval type
     * Key: ApprovalType (DIAGRAM_CONFIRMATION, FINAL_REPORT_APPROVAL)
     * Value: ApprovalRequestData (approved/rejected with notes and edits)
     */
    @Builder.Default
    private Map<String, List<ApprovalRequestData>> approvalHistory = new LinkedHashMap<>();

}
