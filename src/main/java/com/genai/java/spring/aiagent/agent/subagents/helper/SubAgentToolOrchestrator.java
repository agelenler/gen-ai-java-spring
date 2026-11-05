package com.genai.java.spring.aiagent.agent.subagents.helper;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

/**
 * Generic tool orchestration helper for sub-agents.
 * Encapsulates common patterns: retry logic, tool calling loop, graceful degradation.
 * Designed to be used by sub-agent implementations (WebGuidanceAgentImpl, KnowledgeEvidenceAgentImpl, etc.)
 *
 * @param <ARGS> Sub-agent argument type (e.g., WebGuidanceArgs, KnowledgeEvidenceArgs)
 * @param <RESULT> Sub-agent result type (e.g., WebGuidanceResult, KnowledgeEvidenceResult)
 */
@Slf4j
public abstract class SubAgentToolOrchestrator<ARGS, RESULT> {

    protected final ChatClient chatClient;
    protected final ToolCallingManager toolCallingManager;
    protected final ObservationRegistry registry;
    protected final SubAgentJsonParser<RESULT> jsonParser;
    protected final SubAgentPromptBuilder promptBuilder;

    protected static final int MAX_TOTAL_SEARCHES = 5;
    protected static final int MAX_PARSE_RETRIES = 3;

    protected SubAgentToolOrchestrator(ChatClient chatClient,
                                       ObservationRegistry registry,
                                       SubAgentJsonParser<RESULT> jsonParser,
                                       SubAgentPromptBuilder promptBuilder) {
        this.chatClient = chatClient;
        this.registry = registry;
        this.jsonParser = jsonParser;
        this.promptBuilder = promptBuilder;
        this.toolCallingManager = ToolCallingManager.builder().build();
    }

    /**
     * Generate guidance with retry logic for resilience against LLM formatting failures.
     * Orchestrates the full flow: validation, retries, tool calling, parsing, and graceful degradation.
     *
     * @param args Sub-agent arguments containing topics, context, reviewId, etc.
     * @param observationName Name for the observation metric
     * @return Structured result with findings, citations, and confidence
     */
    public RESULT generateGuidance(ARGS args, String observationName) {
        Observation observation = Observation.start(observationName, registry);
        try {
            if (!isValidArgs(args)) {
                log.warn("generateGuidance called with invalid args");
                return createEmptyResult();
            }

            String reviewId = extractReviewId(args);
            log.info("{} starting for reviewId: {}, topics: {}", getAgentName(), reviewId, extractTopics(args));

            // Retry logic: attempt up to MAX_PARSE_RETRIES times
            for (int attempt = 1; attempt <= MAX_PARSE_RETRIES; attempt++) {
                try {
                    log.info("Attempt {}/{} to generate guidance for reviewId: {}", attempt, MAX_PARSE_RETRIES, reviewId);
                    RESULT result = doGenerateGuidance(args);
                    log.info("Successfully generated guidance on attempt {}", attempt);
                    return result;
                } catch (Exception e) {
                    log.warn("Failed to generate/parse guidance on attempt {}: {}", attempt, e.getMessage());
                    if (attempt == MAX_PARSE_RETRIES) {
                        log.error("All {} attempts failed for reviewId: {}", MAX_PARSE_RETRIES, reviewId);
                        return attemptGracefulDegradation(args);
                    }
                }
            }

            return createErrorResult(new RuntimeException("Unexpected error in retry logic"));

        } catch (Exception e) {
            log.error("{} execution failed", getAgentName(), e);
            return createErrorResult(e);
        } finally {
            observation.stop();
        }
    }

    /**
     * Core orchestration logic: create initial prompt, run tool group, parse response.
     * Called by generateGuidance() with retry wrapper.
     *
     * @param args Sub-agent arguments
     * @return Structured result with findings and confidence
     * @throws Exception if LLM response is malformed or parsing fails
     */
    protected RESULT doGenerateGuidance(ARGS args) throws Exception {
        String conversationId = getConversationPrefix() + extractReviewId(args);
        ToolCallingChatOptions toolCallingChatOptions = getToolCallingChatOptions();
        Prompt prompt = promptBuilder.createInitialPrompt(args, toolCallingChatOptions);
        ChatResponse chatResponse = runToolGroup(prompt, toolCallingChatOptions, conversationId);
        String finalResponse = chatResponse.getResult().getOutput().getText();
        return jsonParser.parseGuidanceResult(finalResponse);
    }

    /**
     * Agentic loop: iteratively execute tool calls until satisfied or max searches reached.
     *
     * @param prompt Initial or intermediate prompt
     * @param toolCallingChatOptions Tool calling options with callbacks
     * @param conversationId Conversation ID for memory management
     * @return Final ChatResponse with completed guidance
     */
    protected ChatResponse runToolGroup(Prompt prompt, ToolCallingChatOptions toolCallingChatOptions, String conversationId) {
        ChatResponse chatResponse = getChatResponse(conversationId, prompt);
        int searchCount = 0;

        while (chatResponse.hasToolCalls() && searchCount < MAX_TOTAL_SEARCHES) {
            log.info("[{}] Executing search #{}", getLogPrefix(), ++searchCount);
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
            prompt = new Prompt(toolExecutionResult.conversationHistory(), toolCallingChatOptions);
            chatResponse = getChatResponse(conversationId, prompt);
        }

        if (searchCount >= MAX_TOTAL_SEARCHES) {
            log.warn("[{}] Reached max searches ({}); terminating search loop", getLogPrefix(), MAX_TOTAL_SEARCHES);
        }
        return chatResponse;
    }

    /**
     * Get chat response with conversation memory context.
     *
     * @param conversationId Conversation ID for memory management
     * @param prompt Prompt to send to the model
     * @return ChatResponse from the model
     */
    protected ChatResponse getChatResponse(String conversationId, Prompt prompt) {
        return chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();
    }

    /**
     * Check if args are valid (non-null, has topics, etc.)
     *
     * @param args Sub-agent arguments
     * @return true if valid, false otherwise
     */
    protected abstract boolean isValidArgs(ARGS args);

    /**
     * Extract reviewId from args.
     *
     * @param args Sub-agent arguments
     * @return reviewId string
     */
    protected abstract String extractReviewId(ARGS args);

    /**
     * Extract topics from args.
     *
     * @param args Sub-agent arguments
     * @return topics or description
     */
    protected abstract Object extractTopics(ARGS args);

    /**
     * Get the conversation prefix for this sub-agent.
     * e.g., "web-guidance-", "rag-guidance-"
     *
     * @return conversation prefix
     */
    protected abstract String getConversationPrefix();

    /**
     * Get tool calling chat options with appropriate tool callbacks.
     *
     * @return ToolCallingChatOptions configured for this sub-agent's tools
     */
    protected abstract ToolCallingChatOptions getToolCallingChatOptions();

    /**
     * Get the agent name for logging.
     *
     * @return agent name
     */
    protected abstract String getAgentName();

    /**
     * Get the log prefix for this sub-agent.
     *
     * @return log prefix (e.g., "web_guidance", "knowledge_evidence")
     */
    protected abstract String getLogPrefix();

    /**
     * Graceful degradation fallback when all retry attempts fail.
     *
     * @param args Sub-agent arguments
     * @return Result with missing evidence marked and low confidence
     */
    protected abstract RESULT attemptGracefulDegradation(ARGS args);

    /**
     * Create an empty result (when args are invalid).
     *
     * @return Result with default empty values
     */
    protected abstract RESULT createEmptyResult();

    /**
     * Create an error result.
     *
     * @param e Exception that occurred
     * @return Result with error information
     */
    protected abstract RESULT createErrorResult(Exception e);
}

