package com.genai.java.spring.aiagent.tools.asyncevidences.records;

import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceResult;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceResult;

/**
 * Combined output from async evidence collection.
 * Bundles results from parallel RAG and WEB guidance collection.
 *
 * @param knowledgeEvidenceResult Results from internal knowledge base search (RAG)
 * @param webGuidanceResult       Results from external guidance search (WEB)
 */
public record EvidenceBundle(
    KnowledgeEvidenceResult knowledgeEvidenceResult,
    WebGuidanceResult webGuidanceResult
) {}

