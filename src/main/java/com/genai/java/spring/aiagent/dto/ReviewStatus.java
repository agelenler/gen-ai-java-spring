package com.genai.java.spring.aiagent.dto;

public enum ReviewStatus {
    QUEUED,
    RUNNING,
    PENDING_APPROVAL_DIAGRAM_EXTRACT,
    PENDING_APPROVAL_FINAL_REPORT,
    DONE,
    REJECTED,
    ERROR
}
