package com.genai.java.spring.aiagent.agent.subagents.webguidance.helper;

import com.genai.java.spring.aiagent.agent.subagents.helper.SubAgentPromptBuilder;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceArgs;
import org.springframework.stereotype.Component;

@Component
public class WebGuidancePromptBuilder extends SubAgentPromptBuilder {

    /**
     * Build system prompt for the web guidance sub-agent.
     * Narrow scope: distill OWASP/NIST/CWE guidance only.
     */
    @Override
    protected String buildSystemPrompt(Object args) {
        WebGuidanceArgs guidanceArgs = (WebGuidanceArgs) args;
        return """
                You are a specialized security guidance research agent.
                
                Your role:
                - Search for authoritative security guidance (OWASP, NIST, CWE, RFC standards)
                - Distill findings into actionable recommendations
                - Cite sources with URLs
                - Assign confidence levels and severity impacts
                
                Guidelines:
                1. For each topic, search at least once. If results are sparse, refine the query.
                2. Decompose topics into OWASP/NIST/CWE variants if needed (e.g., "NIST secrets management" after "OWASP secrets").
                3. Reject low-quality or off-topic results (blogs, vendor marketing, etc.).
                4. Synthesize findings into a JSON response matching this schema:
                {
                    "findings": [
                        {
                            "source": "OWASP|NIST|CWE|RFC",
                            "url": "https://...",
                            "title": "Guidance title",
                            "relevantGuidance": "Extracted snippet",
                            "appliesTo": "service/risk/component",
                            "recommendation": "Actionable step",
                            "severityImpact": "HIGH|MEDIUM|LOW"
                        }
                    ],
                    "citations": ["url1", "url2"],
                    "summary": "Compressed synthesis of all findings",
                    "confidence": "HIGH|MEDIUM|LOW",
                    "missingEvidence": ["topic1", "topic2"],
                    "rejectedResults": []
                }
                
                Keep responses concise and evidence-based.
                Context for this search: %s
                """.formatted(guidanceArgs.context());
    }

    /**
     * Build user prompt that decomposes topics into search strategy.
     */
    @Override
    protected String buildUserPrompt(Object args) {
        WebGuidanceArgs guidanceArgs = (WebGuidanceArgs) args;

        StringBuilder prompt = new StringBuilder();
        prompt.append("Retrieve authoritative security guidance for the following topics:\n\n");

        for (int i = 0; i < guidanceArgs.topics().size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, guidanceArgs.topics().get(i)));
        }

        prompt.append("""
                
                Search strategy:
                - For each topic, use web_search to find OWASP/NIST/CWE guidance
                - If the first search is sparse, refine it (e.g., "NIST secrets management" if "OWASP secrets" was insufficient)
                - Collect snippets, URLs, and severity impacts
                - If you find gaps (e.g., "I need CWE data too"), conduct follow-up searches
                - Return findings as a JSON object matching the schema provided in system prompt
                
                Return the final findings as a JSON object (no markdown, pure JSON).
                """);

        return prompt.toString();
    }

}
