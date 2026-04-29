package com.genai.java.spring.aiagent.agent.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aiagent.agent.SecurityReviewAgent;
import com.genai.java.spring.aiagent.agent.records.Plan;
import com.genai.java.spring.aiagent.dto.ReviewState;
import com.genai.java.spring.aiagent.tools.diagram.DiagramTools;
import com.genai.java.spring.aiagent.tools.exception.ToolExecutionException;
import com.genai.java.spring.aiagent.tools.posture.PostureTools;
import com.genai.java.spring.aiagent.tools.rag.RagTools;
import com.genai.java.spring.aiagent.tools.web.WebTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app", name = "agent.use-chain-workflow", havingValue = "true")
public class SecurityReviewAgentChainWorkflow implements SecurityReviewAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ToolCallingManager toolCallingManager;
    private final ToolCallback[] diagramTools;
    private final ToolCallback[] postureTools;
    private final ToolCallback[] ragTools;
    private final ToolCallback[] webTools;
    private final ToolCallback[] followUpTools;

    private static final int MAX_TOOL_CALL_STEPS_DIAGRAM_REVIEW = 6;
    private static final int MAX_TOOL_CALL_STEPS_FOLLOW_UP = 4; // keep follow-ups tight

    public SecurityReviewAgentChainWorkflow(@Qualifier("openAIAgentChatClient") ChatClient chatClient,
                                            ObjectMapper objectMapper,
                                            DiagramTools diagramTools,
                                            PostureTools postureTools,
                                            RagTools ragTools,
                                            WebTools webTools) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.diagramTools = ToolCallbacks.from(diagramTools);
        this.postureTools = ToolCallbacks.from(postureTools);
        this.ragTools = ToolCallbacks.from(ragTools);
        this.webTools = ToolCallbacks.from(webTools);
        // During follow-up, we usually don’t need diagram_extract again; allow retrieval + posture
        this.followUpTools = ToolCallbacks.from(postureTools, ragTools, webTools);
    }

    @Override
    public String execute(ReviewState reviewState) {
        // 1.) Plan
        var plan = getPlan(reviewState.getFileName(), reviewState.getId());
        log.info("Received following plan from model: {}", plan);

        var messages = getMessages(plan);
        Prompt prompt = new Prompt(messages);

        // Execute diagram group (may make multiple tool calls until the model is satisfied)
        prompt = runToolGroup(reviewState.getId(), prompt, getToolCallingChatOptions(diagramTools), MAX_TOOL_CALL_STEPS_DIAGRAM_REVIEW);
        // Execute posture group
        prompt = runToolGroup(reviewState.getId(), prompt, getToolCallingChatOptions(postureTools), MAX_TOOL_CALL_STEPS_DIAGRAM_REVIEW);
        // Execute rag group
        prompt = runToolGroup(reviewState.getId(), prompt, getToolCallingChatOptions(ragTools), MAX_TOOL_CALL_STEPS_DIAGRAM_REVIEW);
        // Execute web group
        prompt = runToolGroup(reviewState.getId(), prompt, getToolCallingChatOptions(webTools), MAX_TOOL_CALL_STEPS_DIAGRAM_REVIEW);

        return getFinalReport(reviewState.getId(), prompt);
    }

    @Override
    public String followUp(String reviewId, String question) {
        var messages = getMessagesForFollowUp(question);
        //Use user-controlled tool execution
        var toolCallingChatOptions = getToolCallingChatOptions(followUpTools);
        var prompt = new Prompt(messages, toolCallingChatOptions);
        var chatResponse = getChatResponse(reviewId, prompt);
        prompt = executeToolCalls(reviewId, chatResponse, prompt, toolCallingChatOptions, MAX_TOOL_CALL_STEPS_FOLLOW_UP);
        //Final answer
        log.info("Returning follow up result with final prompt: {}", prompt.getInstructions());
        return getChatContent(reviewId, new Prompt(prompt.getInstructions(), toolCallingChatOptions));
    }

    private Plan getPlan(String fileName, String id) {
        String goal = """
                Task: Review security risks for the uploaded architecture diagram (fileName=%s, id=%s).
                Follow this MANDATORY ORDER when planning (no tool calls in this message):
                1) Extract the diagram structure (diagram_extract).
                2) Decide which services need posture (internet-facing OR PII OR critical Kafka topics), then fetch posture for those targets (security_posture).
                3) Retrieve internal policy snippets relevant to the *posture findings* and diagram (rag_query).
                4) Fetch OWASP/NIST/CWE guidance for the same topics (web_search).
                5) Synthesize the final report (synthesize).
                Return ONLY JSON matching {"steps":[...]} with toolHint and targets. Do NOT place rag_query or web_search before security_posture.
                """.formatted(fileName, id);

        String system = """
                You are planning a security architecture review. Produce a 3–5 step JSON plan.
                DO NOT call tools in this message; PLANNING ONLY.
                
                Hard constraints (MANDATORY ORDER):
                1) Extract the diagram structure. toolHint=diagram_extract (input: fileName).
                   Use kebab-case ids for services (e.g., "order-service", "payment-service").
                   Never use underscores in your response for the service names.
                2) Determine which services need posture (internet-facing OR handle PII OR participate in critical Kafka topics).
                   Then fetch posture for those services. toolHint=security_posture for each target.
                   Do NOT perform policy retrieval or web search before posture is fetched.
                3) Retrieve internal policy snippets relevant to the posture findings and diagram (topics like TLS, OAuth2, secrets, Kafka ACLs, data retention). toolHint=rag_query (rag).
                4) Fetch external guidance (OWASP/NIST/CWE) for the same topics. toolHint=web_search.
                5) Synthesize the final report.
                
                Output STRICT JSON ONLY:
                {"steps":[
                  {"step":1,"goal":"...","toolHint":"diagram_extract","targets":["file:<fileName>"]},
                  {"step":2,"goal":"...","toolHint":"security_posture","targets":["order-service","payment-service","customer-service","restaurant-service","shipment-service"]},
                  {"step":3,"goal":"...","toolHint":"rag_query","targets":["TLS","OAuth2","Secrets","Kafka Security"]},
                  {"step":4,"goal":"...","toolHint":"web_search","targets":["TLS","OAuth2","Secrets","Kafka Security"]},
                  {"step":5,"goal":"...","toolHint":"","targets":[]}
                ]}
                
                You MUST NOT produce a final or interim textual answer until you have:
                1) called diagram_extract once;
                2) called security_posture for each target service that are defined in step with targets; Do not call security posture more than once for the same service
                3) called rag_query for selected topics;
                4.) called web_search for selected topics;
                Then produce the report. If any tool is unavailable, say which one and stop.
                
                You must not return more than one tool calling request for each step! But keep your tool call requests in a list and return
                If you have a list of information that requires multiple tool calls, pass all the information back to allow calling
                the tools in a loop once and get all tool calling results back at once.
                For rag_query and web_query never send a multi-topic query in a single text, return a list of strings so that
                each of the elements in list will trigger a tool call in a loop on application layer.
                
                Rules:
                - Node types can be one of service|client|database|message_broker|topic|redis
                - Edge can be from service to service using HTTPS or event-driven with either publisher to kafka-topic or kafka-topic to subscriber
                - If Kafka (icon or text) is present, create a node: {"id":"kafka","type":"message_broker","labels":["Kafka"]}.
                - Prefer pub/sub edges over direct HTTP when events are shown.
                - If unsure but evidence suggests eventing, emit pub/sub edges with source="inferred" (never invent HTTP calls).
                - Never place rag_query or web_search before security_posture.
                - Do not omit rag_query or web_search, use both in any case.
                - Keep steps minimal and executable; prefer 4–5 steps.
                - Call at most one tool per assistant turn. If multiple tools are needed, call them sequentially in separate turns in this order: diagram_extract → security_posture → rag_query → web_search.
                - Targets is an array of ids or topic hints; "file:<fileName>" for the diagram step.
                - No prose; JSON only.
                """;

        return chatClient.prompt()
                .system(system)
                .user(goal)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))
                .call()
                .entity(Plan.class);

    }

    private List<Message> getMessages(Plan plan) {
        var messages = new ArrayList<Message>();
        messages.add(new SystemMessage("""
                You are executing a security review with a strict step budget = %s.
                Think briefly, call a tool if needed, observe, and continue.
                Call each tool exactly once in order diagram_extract → security_posture → rag_query → web_search.
                Do NOT skip any tool calls.
                After calling a tool, wait for the observation before proceeding.
                If it succeeds, DO NOT call it again; proceed to the next phase.
                Only cite evidence from diagram_extract, security_posture, rag_query and web_search outputs.
                """.formatted(MAX_TOOL_CALL_STEPS_DIAGRAM_REVIEW)));

        // Seed context with the plan so the model knows the roadmap
        messages.add(new UserMessage("Execution plan:\n" + plan));
        return messages;
    }

    private ToolCallingChatOptions getToolCallingChatOptions(ToolCallback[] tools) {
        return ToolCallingChatOptions.builder()
                .toolCallbacks(tools)
                .internalToolExecutionEnabled(false) // we will drive the loop
                .build();
    }

    private Prompt runToolGroup(String conversationId, Prompt startingPrompt, ToolCallingChatOptions toolCallingChatOptions, int maxSteps) {
        Prompt prompt = new Prompt(startingPrompt.getInstructions(), toolCallingChatOptions);
        ChatResponse chatResponse = getChatResponse(conversationId, prompt);

        int steps = 0;
        ToolExecutionResult toolExecutionResult = null;

        while (chatResponse.hasToolCalls() && steps++ < maxSteps) {
            log.info("[chain] Executing tool call for group step: {}", steps);
            toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
            assertNoToolError(toolExecutionResult);
            prompt = new Prompt(toolExecutionResult.conversationHistory(), toolCallingChatOptions);
            chatResponse = getChatResponse(conversationId, prompt);
        }

        if (toolExecutionResult != null) {
            logToolExecutionResults(toolExecutionResult);
        }

        return new Prompt(prompt.getInstructions());
    }

    private ChatResponse getChatResponse(String id, Prompt prompt) {
        return chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))
                .call()
                .chatResponse();
    }

    public void assertNoToolError(ToolExecutionResult exec) {
        for (Message message : exec.conversationHistory()) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                List<ToolResponseMessage.ToolResponse> toolResponses = toolResponseMessage.getResponses();
                for (ToolResponseMessage.ToolResponse toolResponse : toolResponses) {
                    try {
                        var map = objectMapper.readValue(toolResponse.responseData(), new TypeReference<Map<String, Object>>() {
                        });
                        Object err = map.get("error");
                        if (err != null && !String.valueOf(err).isBlank()) {
                            String code = String.valueOf(err);
                            String detail = String.valueOf(map.getOrDefault("message", ""));
                            ToolExecutionException toolExecutionException =
                                    new ToolExecutionException(toolResponseMessage.getMessageType().name(), code, detail);
                            log.warn("Tool Execution Failed! Continue without this tool call!", toolExecutionException);
                        }
                    } catch (ToolExecutionException exception) {
                        throw exception;
                    } catch (Exception jsonEx) {
                        // If it's not valid JSON but contains "error", still fail fast
                        throw new ToolExecutionException(toolResponseMessage.getMessageType().name(), "TOOL_ERROR", toolResponse.responseData());
                    }
                }
            }
        }
    }

    private void logToolExecutionResults(ToolExecutionResult toolExecutionResult) {
        log.info("Tool execution results: {}",
                toolExecutionResult.conversationHistory().stream()
                        .filter(executionResult -> MessageType.TOOL.equals(executionResult.getMessageType()))
                        .map(ToolResponseMessage.class::cast)
                        .flatMap(toolResponseMessage ->
                                toolResponseMessage.getResponses().stream()
                                        .map(resp -> "tool=" + resp.name() + " data=" + resp.responseData()))
                        .collect(Collectors.joining(" | ")));
    }

    private String getFinalReport(String id, Prompt prompt) {
        List<Message> messages;
        messages = new ArrayList<>(prompt.getInstructions());
        messages.add(new SystemMessage("""
                  Produce the final Security Review Report (Markdown):
                  - Summary
                  - Detailed analysis per service, per service-service, per service-messagebroker and per service-database connection
                  - Top 5 risks (with impact/likelihood)
                  - Evidence: diagram element + RAG citation + Web link + posture snippet
                  - Mitigations (actionable)
                  - Diagram changes to apply (bullets)
                """));

        return getChatContent(id, new Prompt(messages));
    }

    private String getChatContent(String id, Prompt prompt) {
        return chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))
                .call()
                .content();
    }

    private List<Message> getMessagesForFollowUp(String question) {
        var messages = new ArrayList<Message>();
        messages.add(new SystemMessage("""
                You are answering a follow-up about the SAME architecture review as before.
                Use conversation memory for prior diagram extraction, posture results, and retrievals.
                If new evidence is required, you may call rag_query, web_search (allowlisted), or security_posture.
                Keep answers concise and grounded; include citations/links when applicable.
                """));
        messages.add(new UserMessage(question));
        return messages;
    }

    private Prompt executeToolCalls(String id, ChatResponse chatResponse, Prompt prompt, ToolCallingChatOptions toolCallingChatOptions, int maxSteps) {
        int steps = 0;
        ToolExecutionResult toolExecutionResult = null;
        while (chatResponse.hasToolCalls() && steps++ < maxSteps) {
            log.info("Executing tool call with step: {}", steps);
            //Execute any requested tool calls and append observations to the conversation
            toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
            assertNoToolError(toolExecutionResult);
            prompt = new Prompt(toolExecutionResult.conversationHistory(), toolCallingChatOptions); //add tool results as observation
            chatResponse = getChatResponse(id, prompt); // model "thinks" based on observation
        }
        if (toolExecutionResult != null) {
            logToolExecutionResults(toolExecutionResult);
        }
        return prompt;
    }

}
