package com.genai.java.spring.aiagent.dto;

public enum ReviewCheckpoint {
    INIT,
    AFTER_PLAN,
    BEFORE_DIAGRAM_APPROVAL,  // gate point
    AFTER_DIAGRAM,
    AFTER_POSTURE,
    AFTER_ASYNC_EVIDENCES,
    BEFORE_FINAL_APPROVAL, // gate point
    DONE
}
