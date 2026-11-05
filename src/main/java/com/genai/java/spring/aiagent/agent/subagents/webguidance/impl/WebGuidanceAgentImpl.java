package com.genai.java.spring.aiagent.agent.subagents.webguidance.impl;

import com.genai.java.spring.aiagent.agent.subagents.helper.SubAgentToolOrchestrator;
import com.genai.java.spring.aiagent.agent.subagents.webguidance.WebGuidanceAgent;
import com.genai.java.spring.aiagent.agent.subagents.webguidance.helper.WebGuidanceJsonParser;
import com.genai.java.spring.aiagent.agent.subagents.webguidance.helper.WebGuidancePromptBuilder;
import com.genai.java.spring.aiagent.tools.subagents.websearch.WebSearchTools;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceArgs;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceResult;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Web Guidance Sub-Agent.
 * <p>
 * Responsible for decomposing security topics into targeted web searches,
 * retrieving OWASP/NIST/CWE guidance, and synthesizing findings into
 * structured recommendations for the main security review agent.
 * <p>
 * Internal behavior:
 * 1. Accepts a list of topics (e.g., ["OWASP secrets", "NIST secrets", "CWE hardcoded credentials"])
 * 2. Uses web_search tool internally to find authoritative guidance
 * 3. May refine searches based on confidence (e.g., "I found OWASP, now search NIST")
 * 4. Returns distilled findings with citations, confidence levels, and recommendations
 */
@Slf4j
@Component
public class WebGuidanceAgentImpl extends SubAgentToolOrchestrator<WebGuidanceArgs, WebGuidanceResult> implements WebGuidanceAgent {

    private final ToolCallback[] webSearchTools;

    public WebGuidanceAgentImpl(@Qualifier("openAIAgentChatClientSubAgent") ChatClient chatClient,
                                WebSearchTools webSearchTools,
                                ObservationRegistry registry,
                                WebGuidanceJsonParser jsonParser,
                                WebGuidancePromptBuilder promptBuilder) {
        super(chatClient, registry, jsonParser, promptBuilder);
        this.webSearchTools = ToolCallbacks.from(webSearchTools);
    }

    /**
     * Generate guidance by orchestrating decomposed web searches.
     * Delegates to parent class orchestration with retry logic.
     *
     * @param args Input containing topics, context, reviewId, and topK limit
     * @return Structured WebGuidanceResult with findings, citations, and confidence
     */
    @Override
    public WebGuidanceResult generateGuidance(WebGuidanceArgs args) {
        return super.generateGuidance(args, "web_guidance_agent");
    }

    @Override
    protected boolean isValidArgs(WebGuidanceArgs args) {
        return args != null && args.topics() != null && !args.topics().isEmpty();
    }

    @Override
    protected String extractReviewId(WebGuidanceArgs args) {
        return args.reviewId();
    }

    @Override
    protected Object extractTopics(WebGuidanceArgs args) {
        return args.topics();
    }

    @Override
    protected String getConversationPrefix() {
        return "web-guidance-";
    }

    @Override
    protected ToolCallingChatOptions getToolCallingChatOptions() {
        return ToolCallingChatOptions.builder()
                .toolCallbacks(webSearchTools)
                .build();
    }

    @Override
    protected String getAgentName() {
        return "WebGuidanceAgent";
    }

    @Override
    protected String getLogPrefix() {
        return "web_guidance";
    }

    @Override
    protected WebGuidanceResult attemptGracefulDegradation(WebGuidanceArgs args) {
        log.info("Attempting graceful degradation for reviewId: {}", args.reviewId());
        return new WebGuidanceResult(
                List.of(),
                List.of(),
                "Web guidance retrieval failed after " + MAX_PARSE_RETRIES + " attempts. Please review findings manually.",
                "LOW",
                args.topics(),
                List.of()
        );
    }

    @Override
    protected WebGuidanceResult createEmptyResult() {
        return new WebGuidanceResult(
                List.of(),
                List.of(),
                "No topics provided",
                "LOW",
                List.of(),
                List.of()
        );
    }

    @Override
    protected WebGuidanceResult createErrorResult(Exception e) {
        return new WebGuidanceResult(
                List.of(),
                List.of(),
                "Web guidance retrieval failed: " + e.getMessage(),
                "LOW",
                List.of("ERROR: " + e.getClass().getSimpleName()),
                List.of()
        );
    }
}

