package com.genai.java.spring.aiagent.tools.webguidance.records;

/**
 * Represents a single guidance finding from web search results.
 * Structured to provide actionable, contextual security guidance.
 *
 * @param source              Origin of guidance (e.g., "OWASP", "NIST", "CWE", "RFC")
 * @param url                 Citation URL for verification and reference
 * @param title               Human-readable title of the guidance document/section
 * @param relevantGuidance    Extracted snippet of the actual guidance text
 * @param appliesTo           Which service/risk/component this guidance applies to
 * @param recommendation      Actionable mitigation or implementation step
 * @param severityImpact      Urgency level: HIGH, MEDIUM, or LOW
 */
public record WebGuidanceFinding(
    String source,
    String url,
    String title,
    String relevantGuidance,
    String appliesTo,
    String recommendation,
    String severityImpact
) {}

