package com.genai.java.spring.aiagent.agent.subagents.knowledgeevidence.impl;

import com.genai.java.spring.aiagent.agent.subagents.helper.SubAgentToolOrchestrator;
import com.genai.java.spring.aiagent.agent.subagents.knowledgeevidence.KnowledgeEvidenceAgent;
import com.genai.java.spring.aiagent.agent.subagents.knowledgeevidence.helper.KnowledgeEvidenceJsonParser;
import com.genai.java.spring.aiagent.agent.subagents.knowledgeevidence.helper.KnowledgeEvidencePromptBuilder;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceArgs;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceResult;
import com.genai.java.spring.aiagent.tools.subagents.rag.RagTools;
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
 * Knowledge Evidence Sub-Agent.
 * <p>
 * Responsible for decomposing security topics into targeted internal knowledge base searches,
 * retrieving policy guidance, and synthesizing findings into structured recommendations
 * for the main security review agent.
 * <p>
 * Internal behavior:
 * 1. Accepts a list of topics (e.g., ["password policy", "encryption standards", "API security"])
 * 2. Uses rag_query tool internally to find matching internal policies
 * 3. May refine searches based on confidence (e.g., "I found password policy, now search encryption")
 * 4. Returns distilled findings with citations, confidence levels, and recommendations
 */
@Slf4j
@Component
public class KnowledgeEvidenceAgentImpl extends SubAgentToolOrchestrator<KnowledgeEvidenceArgs, KnowledgeEvidenceResult> implements KnowledgeEvidenceAgent {

    private final ToolCallback[] ragTools;

    public KnowledgeEvidenceAgentImpl(@Qualifier("openAIAgentChatClientSubAgent") ChatClient chatClient,
                                      RagTools ragTools,
                                      ObservationRegistry registry,
                                      KnowledgeEvidenceJsonParser jsonParser,
                                      KnowledgeEvidencePromptBuilder promptBuilder) {
        super(chatClient, registry, jsonParser, promptBuilder);
        this.ragTools = ToolCallbacks.from(ragTools);
    }

    /**
     * Generate guidance by orchestrating decomposed RAG searches.
     * Delegates to parent class orchestration with retry logic.
     *
     * @param args Input containing topics, context, reviewId, and topK limit
     * @return Structured KnowledgeEvidenceResult with findings, citations, and confidence
     */
    @Override
    public KnowledgeEvidenceResult generateGuidance(KnowledgeEvidenceArgs args) {
        return super.generateGuidance(args, "knowledge_evidence_agent");
    }

    @Override
    protected boolean isValidArgs(KnowledgeEvidenceArgs args) {
        return args != null && args.topics() != null && !args.topics().isEmpty();
    }

    @Override
    protected String extractReviewId(KnowledgeEvidenceArgs args) {
        return args.reviewId();
    }

    @Override
    protected Object extractTopics(KnowledgeEvidenceArgs args) {
        return args.topics();
    }

    @Override
    protected String getConversationPrefix() {
        return "rag-guidance-";
    }

    @Override
    protected ToolCallingChatOptions getToolCallingChatOptions() {
        return ToolCallingChatOptions.builder()
                .toolCallbacks(ragTools)
                .build();
    }

    @Override
    protected String getAgentName() {
        return "KnowledgeEvidenceAgent";
    }

    @Override
    protected String getLogPrefix() {
        return "knowledge_evidence";
    }

    @Override
    protected KnowledgeEvidenceResult attemptGracefulDegradation(KnowledgeEvidenceArgs args) {
        log.info("Attempting graceful degradation for reviewId: {}", args.reviewId());
        return new KnowledgeEvidenceResult(
                List.of(),
                List.of(),
                "RAG guidance retrieval failed after " + MAX_PARSE_RETRIES + " attempts. Please review findings manually.",
                "LOW",
                args.topics(),
                List.of()
        );
    }

    @Override
    protected KnowledgeEvidenceResult createEmptyResult() {
        return new KnowledgeEvidenceResult(
                List.of(),
                List.of(),
                "No topics provided",
                "LOW",
                List.of(),
                List.of()
        );
    }

    @Override
    protected KnowledgeEvidenceResult createErrorResult(Exception e) {
        return new KnowledgeEvidenceResult(
                List.of(),
                List.of(),
                "RAG guidance retrieval failed: " + e.getMessage(),
                "LOW",
                List.of("ERROR: " + e.getClass().getSimpleName()),
                List.of()
        );
    }
}

