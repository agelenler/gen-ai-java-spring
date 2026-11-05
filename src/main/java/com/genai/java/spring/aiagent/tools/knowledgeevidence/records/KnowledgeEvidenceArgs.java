package com.genai.java.spring.aiagent.tools.knowledgeevidence.records;

import java.util.List;

/**
 * Input contract for the Knowledge evidence sub-agent.
 * Allows batch processing of multiple related topics from internal knowledge base.
 *
 * @param topics     List of search topics (e.g., ["password policy", "encryption standards", "API security"])
 * @param context    Additional context about the security finding (e.g., diagram findings, web guidance insights)
 * @param reviewId   Unique identifier for the security review (for conversation tracking)
 * @param topK       Maximum number of results to return per search
 */
public record KnowledgeEvidenceArgs(
    List<String> topics,
    String context,
    String reviewId,
    int topK
) {}

