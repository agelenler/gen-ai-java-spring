package com.genai.java.spring.aiagent.tools.webguidance.records;

import java.util.List;

/**
 * Input contract for the web guidance sub-agent.
 * Allows batch processing of multiple related topics with context.
 *
 * @param topics     List of search topics (e.g., ["OWASP secrets", "NIST secrets", "CWE hardcoded credentials"])
 * @param context    Additional context about the security finding (e.g., diagram findings, posture issues)
 * @param reviewId   Unique identifier for the security review (for conversation tracking)
 * @param topK       Maximum number of results to return per search
 */
public record WebGuidanceArgs(
    List<String> topics,
    String context,
    String reviewId,
    int topK
) {}

