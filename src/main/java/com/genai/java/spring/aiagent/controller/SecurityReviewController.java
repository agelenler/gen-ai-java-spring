package com.genai.java.spring.aiagent.controller;

import com.genai.java.spring.aiagent.dto.ApprovalRequest;
import com.genai.java.spring.aiagent.dto.ApprovalType;
import com.genai.java.spring.aiagent.dto.FollowUpRequestDto;
import com.genai.java.spring.aiagent.dto.ReviewDto;
import com.genai.java.spring.aiagent.dto.ReviewStatus;
import com.genai.java.spring.aiagent.service.SecurityReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/security-review")
public class SecurityReviewController {

    private final SecurityReviewService securityReviewService;

    public SecurityReviewController(SecurityReviewService securityReviewService) {
        this.securityReviewService = securityReviewService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> reviewDiagram(@RequestPart("diagram") MultipartFile diagram) {
        log.info("Received diagram for security review: {}", diagram.getOriginalFilename());
        String reviewId = securityReviewService.enqueueAndExecute(diagram);
        return Map.of("reviewId", reviewId);
    }

    @GetMapping("/{id}")
    public ReviewDto getReviewById(@PathVariable String id) {
        log.info("Fetching security review status for id: {}", id);
        return securityReviewService.getSecurityReview(id);
    }

    @PostMapping(path = "/{id}/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_MARKDOWN_VALUE)
    public String followUp(@PathVariable String id, @RequestBody FollowUpRequestDto followUpRequestDto) {
        log.info("Received follow-up question for review id {}: {}", id, followUpRequestDto.question());
        var state = securityReviewService.getSecurityReview(id);
        if (state.getStatus() == ReviewStatus.QUEUED || state.getStatus() == ReviewStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review still running. Try again when it is DONE!");
        }

        if (followUpRequestDto == null || followUpRequestDto.question() == null || followUpRequestDto.question().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required!");
        }
        return securityReviewService.followUpWithVision(id, followUpRequestDto.question());
    }


    @PostMapping(path = "/{id}/human-approve-diagram-extract", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> humanApproveDiagramExtract(@PathVariable String id,
                                                          @RequestBody ApprovalRequest approvalRequest) {
        log.info("Received human approval request for diagram extract with review id {}: approved={}, note={}",
                id, approvalRequest.approved(), approvalRequest.note());

        securityReviewService.approveWithEdits(id, approvalRequest, ApprovalType.DIAGRAM_CONFIRMATION);

        return Map.of("message", approvalRequest.approved()
                ? "Diagram extract approved. Resuming review..."
                : "Diagram extract rejected. Review paused.",
                "reviewId", id);
    }

    @PostMapping(path = "/{id}/human-approve-final-report", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> humanApproveFinalReport(@PathVariable String id,
                                                          @RequestBody ApprovalRequest approvalRequest) {
        log.info("Received human approval request for final report with review id {}: approved={}, note={}",
                id, approvalRequest.approved(), approvalRequest.note());

        securityReviewService.approveWithEdits(id, approvalRequest, ApprovalType.FINAL_REPORT_APPROVAL);

        return Map.of("message", approvalRequest.approved()
                        ? "Final report approved. Completing review..."
                        : "Final report rejected. Review paused.",
                "reviewId", id);
    }


}
