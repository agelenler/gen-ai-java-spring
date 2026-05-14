package com.genai.java.spring.aiagent.agent;

import com.genai.java.spring.aiagent.dto.AgentRunResult;
import com.genai.java.spring.aiagent.dto.ReviewState;

public interface SecurityReviewAgent {

    /**
     * Execute the Plan-then-Act + ReAct loop and return a Markdown report.
     * @param reviewState   The uploaded diagram reviewState
     * @return Markdown report (final)
     */
    AgentRunResult execute(ReviewState reviewState);

    /** Answer a follow-up question using the same conversation (memory) keyed by reviewId. */
    String followUp(String reviewId, String question);
}
