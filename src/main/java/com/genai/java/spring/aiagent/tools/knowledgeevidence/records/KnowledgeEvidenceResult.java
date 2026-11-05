package com.genai.java.spring.aiagent.tools.knowledgeevidence.records;

import java.util.List;

/**
 * Structured output from the Knowledge evidence sub-agent.
 * Represents distilled, compressed findings from internal knowledge base searches.
 *
 * @param findings         List of structured guidance findings from internal policies
 * @param citations        Deduplicated list of document IDs/sections for citations in final report
 * @param summary          Compressed synthesis of all findings for main agent reasoning
 * @param confidence       Confidence level of findings: HIGH, MEDIUM, or LOW
 * @param missingEvidence  Topics where internal searches were insufficient or unsuccessful
 * @param rejectedResults  Document IDs filtered out during processing (for observability)
 */
public record KnowledgeEvidenceResult(
    List<KnowledgeEvidenceFinding> findings,
    List<String> citations,
    String summary,
    String confidence,
    List<String> missingEvidence,
    List<String> rejectedResults
) {}
