package com.genai.java.spring.aiagent.tools.asyncevidences.records;

import java.util.List;

/**
 * Input contract for async evidence collection.
 * Coordinates parallel execution of RAG and WEB guidance collection.
 *
 * @param reviewId             Unique identifier for the security review
 * @param knowledgeTopics      Topics for internal knowledge base search (RAG)
 * @param knowledgeContext     Context for RAG queries
 * @param webTopics            Topics for external guidance search (WEB)
 * @param webContext           Context for WEB queries
 * @param includeWebGuidance   Whether to include external guidance collection
 * @param topK                 Maximum number of results per search
 */
public record EvidenceCollectionArgs(
    String reviewId,
    List<String> knowledgeTopics,
    String knowledgeContext,
    List<String> webTopics,
    String webContext,
    boolean includeWebGuidance,
    int topK
) {}

