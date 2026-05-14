package com.genai.java.spring.aiagent.dto;

import com.genai.java.spring.aiagent.agent.records.Plan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewState {
    private String id;
    @Builder.Default
    private ReviewStatus status = ReviewStatus.QUEUED;

    /**
     * Final Markdown report (present when DONE)
     */
    private String reportMarkdown;

    /**
     * Uploaded diagram reference (e.g., fileName in your storage)
     */
    private String fileName;

    /**
     * Xml definition of diagram
     */
    private String xml;

    /**
     * Error details (present when ERROR)
     */
    private String errorMessage;

    /**
     * Current serialized prompt (conversation history)
     */
    private String promptSnapshot;

    /**
     * Current plan information
     */
    private Plan plan;

    /**
     * Current checkpoint
     */
    private ReviewCheckpoint checkpoint;

    /**
     * Pending approval
     * non-null when awaiting human approval
     */
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
     * Value: List<ApprovalRequestData> (approved/rejected with notes and edits)
     */
    @Builder.Default
    private Map<String, List<ApprovalRequestData>> approvalHistory = new LinkedHashMap<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    public void updateStatus(ReviewStatus status) {
        this.status = Objects.requireNonNull(status);
        this.updatedAt = Instant.now();
    }

    public void updateReportMarkdown(String reportMarkdown) {
        this.reportMarkdown = reportMarkdown;
        this.updatedAt = Instant.now();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    /**
     * Archive current prompt snapshot in history before moving to next checkpoint
     */
    public void archivePromptSnapshot(String checkpointName) {
        if (promptSnapshot != null && !promptSnapshot.isEmpty()) {
            promptHistory.put(checkpointName, promptSnapshot);
        }
    }

    /**
     * Record an approval or rejection for a specific approval type
     */
    public void recordApproval(ApprovalType type, ApprovalRequestData approvalData) {
        List<ApprovalRequestData> approvalRequestData = approvalHistory.get(type.name());
        if (approvalRequestData == null) {
            approvalRequestData = new ArrayList<>();
        }
        approvalRequestData.add(approvalData);
        approvalHistory.put(type.name(), approvalRequestData);
        this.updatedAt = Instant.now();
    }

    /**
     * Clear pending approval after human has responded
     */
    public void clearPendingApproval() {
        this.pendingApproval = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if conversation history exists
     */
    public boolean hasPromptSnapshot() {
        return promptSnapshot != null && !promptSnapshot.isEmpty();
    }

}
