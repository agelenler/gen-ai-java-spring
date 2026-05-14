package com.genai.java.spring.aiagent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.genai.java.spring.aiagent.agent.SecurityReviewAgent;
import com.genai.java.spring.aiagent.dataaccess.helper.ReviewStateRepositoryHelper;
import com.genai.java.spring.aiagent.dataaccess.repository.ReviewStateRepository;
import com.genai.java.spring.aiagent.dto.AgentRunResult;
import com.genai.java.spring.aiagent.dto.ApprovalRequest;
import com.genai.java.spring.aiagent.dto.ApprovalRequestData;
import com.genai.java.spring.aiagent.dto.ApprovalType;
import com.genai.java.spring.aiagent.dto.ReviewDto;
import com.genai.java.spring.aiagent.dto.ReviewState;
import com.genai.java.spring.aiagent.dto.ReviewStatus;
import com.genai.java.spring.aiagent.mapper.ReviewStateMapper;
import com.genai.java.spring.aiagent.service.FileStorageService;
import com.genai.java.spring.aiagent.service.PromptSerializer;
import com.genai.java.spring.aiagent.service.SecurityReviewService;
import com.genai.java.spring.aiagent.tools.exception.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class SecurityReviewServiceImpl implements SecurityReviewService {

    private final Executor executor;
    private final FileStorageService fileStorageService;
    private final SecurityReviewAgent securityReviewAgent;
    private final ReviewStateMapper reviewStateMapper;
    private final ReviewStateRepositoryHelper reviewStateRepositoryHelper;
    private final PromptSerializer promptSerializer;

    public SecurityReviewServiceImpl(@Qualifier("traceableAsyncExecutor") Executor executor,
                                     FileStorageService fileStorageService,
                                     SecurityReviewAgent securityReviewAgent,
                                     ReviewStateMapper reviewStateMapper, ReviewStateRepositoryHelper reviewStateRepositoryHelper,
                                     PromptSerializer promptSerializer) {
        this.executor = executor;
        this.fileStorageService = fileStorageService;
        this.securityReviewAgent = securityReviewAgent;
        this.reviewStateMapper = reviewStateMapper;
        this.reviewStateRepositoryHelper = reviewStateRepositoryHelper;
        this.promptSerializer = promptSerializer;
    }

    @Override
    public String enqueueAndExecute(MultipartFile diagram) {
        String reviewId = UUID.randomUUID().toString();
        String fileName = fileStorageService.save(diagram);
        var state = ReviewState.builder()
                .id(reviewId)
                .status(ReviewStatus.QUEUED)
                .fileName(fileName)
                .build();
        reviewStateRepositoryHelper.saveReviewState(state);
        log.info("Enqueued security review id={} for file={}. Triggering agent!", reviewId, fileName);
        executor.execute(() -> {
            try {
                ReviewState newState = runAgent(state);
                log.info("Completed security review id={} with status={}", reviewId, newState.getStatus());
            } catch (Exception e) {
                log.error("Security review id={} failed with exception={}", reviewId, e.getMessage(), e);
            }
        });

        return reviewId;
    }

    @Override
    public ReviewDto getSecurityReview(String id) {
        var reviewState = reviewStateRepositoryHelper.loadReviewState(id);
        if (reviewState == null) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return reviewStateMapper.toDto(reviewState);
    }

    @Override
    public String followUpWithVision(String id, String question) {
        return securityReviewAgent.followUp(id, question);
    }

    /**
     * Handle approval or rejection with optional edits
     * Records the approval decision, applies edits if approved, and resumes execution
     */
    @Override
    public void approveWithEdits(String id, ApprovalRequest approvalRequest, ApprovalType approvalType) {
        ReviewState state = reviewStateRepositoryHelper.loadReviewState(id);
        if (state == null) {
            throw new ResponseStatusException(NOT_FOUND, "Review not found");
        }

        recordApprovalIntoState(id, approvalRequest, approvalType, state);

        if (!approvalRequest.approved()) {
            checkStatusForRejection(approvalType, state);
            // Rejection: save state and mark as rejected
            state.updateStatus(ReviewStatus.REJECTED);
            state.clearPendingApproval();
            reviewStateRepositoryHelper.saveReviewState(state);
            log.info("Review id={} rejected at checkpoint={}", id, state.getCheckpoint());
            return;
        }

        if (approvalRequest.edits() != null && !approvalRequest.edits().isEmpty()) {
            applyEditsToPrompt(state, approvalRequest.edits());
            log.info("Applied edits to prompt for reviewId={}", id);
        }

        state.archivePromptSnapshot(state.getCheckpoint().name());

        state.clearPendingApproval();
        reviewStateRepositoryHelper.saveReviewState(state);

        // Resume execution from next checkpoint
        log.info("Approval granted for reviewId={}, resuming execution", id);
        executeExisting(id);
    }

    private ReviewState runAgent(ReviewState state) {
        try {
            log.info("Starting security review agent for reviewId={} on file={}", state.getId(), state.getFileName());
            state.updateStatus(ReviewStatus.RUNNING);

            reviewStateRepositoryHelper.saveReviewState(state);

            AgentRunResult result = securityReviewAgent.execute(state);

            if (result.isPaused()) {
                log.info("Agent paused at checkpoint={} for human approval", state.getCheckpoint());
                // Status is already set by the agent
                reviewStateRepositoryHelper.saveReviewState(state);
                return state;
            }

            if (result.isDone()) {
                log.info("Agent completed with final report for reviewId={}", state.getId());
                state.updateReportMarkdown(result.report());
                state.updateStatus(ReviewStatus.DONE);
                reviewStateRepositoryHelper.saveReviewState(state);
            }

        } catch (ToolExecutionException e) {
            state.updateStatus(ReviewStatus.ERROR);
            state.setErrorMessage("""
                    ### Review failed with tool calling exception!
                    **Tool:** %s
                    **Error:** %s  
                    Try again after fixing the issue (e.g., verify the file exists and is readable).
                    """.formatted(e.getToolName(), e.getErrorCode()));
            log.error("Tool execution error in review id={}: {}", state.getId(), e.getMessage(), e);
            reviewStateRepositoryHelper.saveReviewState(state);
        } catch (Exception e) {
            state.updateStatus(ReviewStatus.ERROR);
            state.setErrorMessage("Unknown exception: " + e.getMessage());
            log.error("Unexpected error in review id={}: {}", state.getId(), e.getMessage(), e);
            reviewStateRepositoryHelper.saveReviewState(state);
        }
        return state;
    }

    private void recordApprovalIntoState(String id, ApprovalRequest approvalRequest, ApprovalType approvalType, ReviewState state) {
        ApprovalRequestData approvalRequestData = ApprovalRequestData.builder()
                .approved(approvalRequest.approved())
                .note(approvalRequest.note())
                .edits(approvalRequest.edits())
                .approvedAt(Instant.now())
                .checkpointName(state.getCheckpoint().name())
                .build();

        state.recordApproval(approvalType, approvalRequestData);
        log.info("Recorded approval for reviewId={} type={} approved={}", id, approvalType, approvalRequest.approved());
    }

    private void checkStatusForRejection(ApprovalType approvalType, ReviewState state) {
        if (approvalType == ApprovalType.DIAGRAM_CONFIRMATION && state.getStatus() != ReviewStatus.PENDING_APPROVAL_DIAGRAM_EXTRACT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To reject a diagram review," +
                    " status must be " + ReviewStatus.PENDING_APPROVAL_DIAGRAM_EXTRACT.name());
        }

        if (approvalType == ApprovalType.FINAL_REPORT_APPROVAL && state.getStatus() != ReviewStatus.PENDING_APPROVAL_FINAL_REPORT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To reject a final report review," +
                    " status must be " + ReviewStatus.PENDING_APPROVAL_FINAL_REPORT.name());
        }
    }

    /**
     * Apply human edits to the current prompt
     * Edits are merged as new UserMessages to maintain context history
     */
    private void applyEditsToPrompt(ReviewState state, Map<String, Object> edits) {
        try {
            // Deserialize current prompt
            Prompt prompt = promptSerializer.deserialize(state.getPromptSnapshot());
            List<Message> messages = new ArrayList<>(prompt.getInstructions());

            //Built edit message summarizing the changes
            StringBuilder editSummary = new StringBuilder("**Human Edits Applied**\n");
            for (Map.Entry<String, Object> edit: edits.entrySet()) {
                editSummary.append(String.format("- %s: %s\n", edit.getKey(), edit.getValue()));
            }

            // Append edit message to conversation
            messages.add(new UserMessage(editSummary.toString()));

            Prompt updatedPrompt = new Prompt(messages);
            String updatedSnapshot = promptSerializer.serialize(
                    updatedPrompt,
                    state.getId(),
                    state.getCheckpoint().name());

            state.setPromptSnapshot(updatedSnapshot);
            log.info("Applied {} edits to prompt for reviewId={}", edits.size(), state.getId());
        } catch (Exception e) {
            log.error("Failed to apply edits to prompt for reviewId={}: {}", state.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to apply approval edits", e);
        }
    }

    private void executeExisting(String id) {
        ReviewState state = reviewStateRepositoryHelper.loadReviewState(id);
        if (state != null) {
            executor.execute(() -> runAgent(state));
        } else {
            log.warn("ReviewState not found for id: {}", id);
        }
    }

}
