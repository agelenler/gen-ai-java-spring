package com.genai.java.spring.aiagent.dto;

/**
 * Represents the result of an agent execution.
 * - report: the final markdown report (non-null only when isDone=true)
 * - isPaused: true when execution paused for human approval
 * - isDone: true when all stages completed and final report is ready
 */
public record AgentRunResult(
        String report,  // Final markdown (only when done)
        boolean isPaused, // Agent waiting for human
        boolean isDone // All stages complete
) {

    /**
     * Agent paused waiting for human approval
     */
    public static AgentRunResult paused(String report) {
        return new AgentRunResult(report, true, false);
    }

    /**
     * Agent execution completed with final report
     */
    public static AgentRunResult done(String report) {
        return new AgentRunResult(report, false, true);
    }

    /**
     * Agent continues running (no pause, not done yet).
     * Used when auto-progressing through stages.
     */
    public static AgentRunResult continueRunning() {
        return new AgentRunResult(null, false, false);
    }

}
