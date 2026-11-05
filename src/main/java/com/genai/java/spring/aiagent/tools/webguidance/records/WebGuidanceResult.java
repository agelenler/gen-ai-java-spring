package com.genai.java.spring.aiagent.tools.webguidance.records;

import java.util.List;

/**
 * Structured output from the web guidance sub-agent.
 * Represents distilled, compressed findings from multiple web searches.
 *
 * @param findings         List of structured guidance findings, each with source, recommendation, and severity
 * @param citations        Deduplicated list of URLs for citations in final report
 * @param summary          Compressed synthesis of all findings for main agent reasoning
 * @param confidence       Confidence level of findings: HIGH, MEDIUM, or LOW
 * @param missingEvidence  Topics where searches were insufficient or unsuccessful
 * @param rejectedResults  URLs/sources filtered out during processing (for observability)
 */
public record WebGuidanceResult(
    List<WebGuidanceFinding> findings,
    List<String> citations,
    String summary,
    String confidence,
    List<String> missingEvidence,
    List<String> rejectedResults
) {}

