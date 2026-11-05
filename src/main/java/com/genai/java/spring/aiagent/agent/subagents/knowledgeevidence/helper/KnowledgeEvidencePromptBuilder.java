package com.genai.java.spring.aiagent.agent.subagents.knowledgeevidence.helper;

import com.genai.java.spring.aiagent.agent.subagents.helper.SubAgentPromptBuilder;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceArgs;
import org.springframework.stereotype.Component;

/**
 * The KnowledgeEvidencePromptBuilder class is responsible for constructing prompts
 * tailored for the Knowledge Evidence sub-agent. It extends the SubAgentPromptBuilder
 * to provide domain-specific prompt-building logic for system and user interactions.
 *
 * <p>Usage:</p>
 * This class is used to generate prompts that guide the Knowledge Evidence sub-agent
 * in retrieving and synthesizing internal security policy guidance. It ensures that
 * the prompts are aligned with the expected input and output schema for the sub-agent.
 *
 * <p>Key Methods:</p>
 * <ul>
 *   <li>{@link #buildSystemPrompt(Object)}: Constructs the system prompt that defines
 *       the sub-agent's role, guidelines, and expected JSON response schema.</li>
 *   <li>{@link #buildUserPrompt(Object)}: Constructs the user prompt that outlines
 *       the topics to search for and the strategy to follow during the search.</li>
 * </ul>
 */
@Component
public class KnowledgeEvidencePromptBuilder extends SubAgentPromptBuilder {

    /**
     * Build system prompt for the knowledge evidence sub-agent.
     * Internal policy guidance only.
     */
    @Override
    protected String buildSystemPrompt(Object args) {
        KnowledgeEvidenceArgs evidenceArgs = (KnowledgeEvidenceArgs) args;
        return """
                You are a specialized internal security policy research agent.
                
                Your role:
                - Search for internal security policies, handbooks, and guidelines
                - Distill findings into actionable recommendations aligned with company standards
                - Cite sources with document IDs and sections
                - Assign confidence levels and severity impacts
                
                Guidelines:
                1. For each topic, search at least once. If results are sparse, refine the query.
                2. Decompose topics into policy variants if needed (e.g., "encryption policy" after "password policy").
                3. Reject low-quality or off-topic results (outdated documents, conflicting standards, etc.).
                4. Synthesize findings into a JSON response matching this schema:
                {
                    "findings": [
                        {
                            "docId": "policy-doc-id",
                            "source": "policy/handbook name",
                            "title": "Guidance title",
                            "relevantGuidance": "Extracted snippet",
                            "appliesTo": "service/risk/component",
                            "recommendation": "Actionable step",
                            "severityImpact": "HIGH|MEDIUM|LOW",
                            "score": 0.95
                        }
                    ],
                    "citations": ["doc-id-1", "doc-id-2"],
                    "summary": "Compressed synthesis of all findings",
                    "confidence": "HIGH|MEDIUM|LOW",
                    "missingEvidence": ["topic1", "topic2"],
                    "rejectedResults": []
                }
                
                Keep responses concise and aligned with internal standards.
                Context for this search: %s
                """.formatted(evidenceArgs.context());
    }

    /**
     * Build user prompt that decomposes topics into search strategy.
     */
    @Override
    protected String buildUserPrompt(Object args) {
        KnowledgeEvidenceArgs evidenceArgs = (KnowledgeEvidenceArgs) args;

        StringBuilder prompt = new StringBuilder();
        prompt.append("Retrieve internal security policy guidance for the following topics:\n\n");

        for (int i = 0; i < evidenceArgs.topics().size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, evidenceArgs.topics().get(i)));
        }

        prompt.append("""
                
                Search strategy:
                - For each topic, use rag_query to find matching policies and guidelines from internal knowledge base
                - If the first search is sparse, refine it (e.g., "data encryption policy" if "encryption" was insufficient)
                - Collect snippets, document IDs, and severity impacts
                - If you find gaps (e.g., "I need compliance mapping too"), conduct follow-up searches
                - Return findings as a JSON object matching the schema provided in system prompt
                
                Return the final findings as a JSON object (no markdown, pure JSON).
                """);

        return prompt.toString();
    }
}
