package com.genai.java.spring.aiagent.service.impl;

import com.genai.java.spring.aiagent.agent.SecurityReviewAgent;
import com.genai.java.spring.aiagent.dto.ReviewDto;
import com.genai.java.spring.aiagent.dto.ReviewState;
import com.genai.java.spring.aiagent.dto.ReviewStatus;
import com.genai.java.spring.aiagent.mapper.ReviewStateMapper;
import com.genai.java.spring.aiagent.service.FileStorageService;
import com.genai.java.spring.aiagent.service.SecurityReviewService;
import com.genai.java.spring.aiagent.tools.exception.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class SecurityReviewServiceImpl implements SecurityReviewService {

    private final Executor executor;
    private final Map<String, ReviewState> reviewStateStore = new ConcurrentHashMap<>();
    private final FileStorageService fileStorageService;
    private final SecurityReviewAgent securityReviewAgent;
    private final ReviewStateMapper reviewStateMapper;

    public SecurityReviewServiceImpl(@Qualifier("traceableAsyncExecutor") Executor executor,
                                     FileStorageService fileStorageService,
                                     SecurityReviewAgent securityReviewAgent,
                                     ReviewStateMapper reviewStateMapper) {
        this.executor = executor;
        this.fileStorageService = fileStorageService;
        this.securityReviewAgent = securityReviewAgent;
        this.reviewStateMapper = reviewStateMapper;
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
        reviewStateStore.put(reviewId, state);
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
        var reviewState = reviewStateStore.get(id);
        if (reviewState == null) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return reviewStateMapper.toDto(reviewState);
    }

    @Override
    public String followUpWithVision(String id, String question) {
        return securityReviewAgent.followUp(id, question);
    }

    private ReviewState runAgent(ReviewState state) {
        try {
            log.info("Starting security review agent for reviewId={} on file={}", state.getId(), state.getFileName());
            state.updateStatus(ReviewStatus.RUNNING);
            String goal = "Review security for fileName=%s; use posture, RAG, Web; produce final report.".formatted(state.getFileName());
            String report = securityReviewAgent.execute(goal, state);
            log.info("Final report: {}", report);
            state.updateReportMarkdown(report);
            state.updateStatus(ReviewStatus.DONE);
        } catch (ToolExecutionException e) {
            state.updateStatus(ReviewStatus.ERROR);
            state.setErrorMessage("""
                    ### Review failed with tool calling exception!
                    **Tool:** %s
                    **Error:** %s  
                    Try again after fixing the issue (e.g., verify the file exists and is readable).
                    """.formatted(e.getToolName(), e.getErrorCode()));
        } catch (Exception e) {
            state.updateStatus(ReviewStatus.ERROR);
            state.setErrorMessage("Unknown exception: " + e.getMessage());
        }
        return state;
    }
}
