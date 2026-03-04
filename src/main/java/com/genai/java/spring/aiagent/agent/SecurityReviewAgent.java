package com.genai.java.spring.aiagent.agent;

import com.genai.java.spring.aiagent.dto.ReviewState;

public interface SecurityReviewAgent {

    /**
     * Execute the Plan-then-Act + ReAct loop and return a Markdown report.
     * @param userGoal A short goal/brief (you assembled this in the controller)
     * @param reviewState   The uploaded diagram reviewState
     * @return Markdown report (final)
     */
    String execute(String userGoal, ReviewState reviewState);

    /** Answer a follow-up question using the same conversation (memory) keyed by reviewId. */
    String followUp(String reviewId, String question);
}
