package com.genai.java.spring.aiagent.tools.knowledgeevidence.records;

/**
 * Represents a single guidance finding from Knowledge evidence results.
 * Structured to provide actionable, contextual security guidance from internal policies.
 *
 * @param docId              Document ID from the internal knowledge base
 * @param source             Origin of guidance (e.g., policy name, handbook section)
 * @param title              Human-readable title of the document/section
 * @param relevantGuidance   Extracted snippet of the actual guidance text
 * @param appliesTo          Which service/risk/component this guidance applies to
 * @param recommendation     Actionable mitigation or implementation step
 * @param severityImpact     Urgency level: HIGH, MEDIUM, or LOW
 * @param score              Relevance score from similarity search
 */
public record KnowledgeEvidenceFinding(
    String docId,
    String source,
    String title,
    String relevantGuidance,
    String appliesTo,
    String recommendation,
    String severityImpact,
    Double score
) {}

