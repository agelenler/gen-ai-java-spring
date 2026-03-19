package com.genai.java.spring.aiagent.tools.diagram;

import com.genai.java.spring.aiagent.config.data.AIAgentConfigData;
import com.genai.java.spring.aiagent.service.FileStorageService;
import com.genai.java.spring.aiagent.tools.diagram.records.DiagramExtractResult;
import com.genai.java.spring.aiagent.tools.diagram.records.ExtractArgs;
import com.genai.java.spring.aiagent.tools.diagram.records.Node;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class DiagramTools {

    private final ChatClient chatClient;
    private final FileStorageService fileStorage;
    private final AIAgentConfigData.DiagramToolProperties diagramToolProperties;
    private final ObservationRegistry registry;

    private static final String SYSTEM_PROMPT = """
            You are a vision parser that extracts a software architecture graph from a diagram image.
            Output STRICT JSON ONLY (no prose). Use this shape:
            {
              "nodes":[{"id":"","type":"","zone":"","technology":"","labels":[""],"meta":{}}],
              "edges":[{"from":"","to":"","protocol":"","port":0,"auth":"","encrypted":true,"notes":""}],
              "dataStores":[{"id":"","type":"","classification":"","encryptedAtRest":true}],
              "trustBoundaries":[{"name":"","includes":["nodeId","nodeId2"]}]
            }
            Rules:
            - Use stable, machine-friendly IDs (kebab or snake case).
            - Infer protocol/auth/encryption if clearly shown (TLS lock, HTTPS, mTLS notes).
            - Prefer concise fields; omit unknowns with empty string or null.
            - Do NOT add commentary. JSON only.
            """;

    public DiagramTools(@Qualifier("openAIAgentChatClientVision") ChatClient chatClient,
                        FileStorageService fileStorage,
                        AIAgentConfigData aiAgentConfigData,
                        ObservationRegistry registry) {
        this.chatClient = chatClient;
        this.fileStorage = fileStorage;
        this.diagramToolProperties = aiAgentConfigData.getDiagramTool();
        this.registry = registry;
    }

    @Tool(name = "diagram_extract", description = "Extract components/edges from an uploaded architecture diagram (image, Draw.io PNG export, or screenshot). Returns a normalized JSON graph.")
    Map<String, Object> extractDiagram(ExtractArgs extractArgs) {
        try {
            Observation toolCallObservation = Observation.start("diagram_extract", registry);
            try (Observation.Scope scope = toolCallObservation.openScope()) {
                log.info("Calling diagram_extract tool with file: {} and id: {}", extractArgs.fileName(), extractArgs.id());
                var path = fileStorage.resolve(extractArgs.fileName());
                var resource = new FileSystemResource(path);
                var mime = Files.probeContentType(path);
                var userText = buildUserText(extractArgs.hints());
                var chatOptions = getChatOptions();
                DiagramExtractResult diagramExtractResult = doExtractDiagram(extractArgs.id(), userText, mime, resource, chatOptions);
                return toMap(diagramExtractResult);
            } finally {
                toolCallObservation.stop();
            }
        } catch (IOException e) {
            log.error("Error in diagram extract tool", e);
            return Map.of("error", "DIAGRAM_PARSE_FAILED", "message", e.getMessage());
        }
    }

    private String buildUserText(List<String> hints) {
        String base = "Extract nodes, edges, data stores, message brokers(kafka), event-driven communications and trust boundaries.";
        if (hints == null || hints.isEmpty()) {
            return base;
        }
        return base + " Hints: " + String.join(",", hints);
    }

    private ChatOptions getChatOptions() {
        return OpenAiChatOptions.builder()
                .temperature(diagramToolProperties.getTemperature())
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();
    }

    private DiagramExtractResult doExtractDiagram(String id, String userText, String mime, FileSystemResource resource, ChatOptions chatOptions) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> {
                    u.text(userText);
                    u.media(MimeType.valueOf(mime), resource);
                })
                .options(chatOptions)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))
                .call()
                .entity(DiagramExtractResult.class);
    }

    private Map<String, Object> toMap(DiagramExtractResult diagramExtractResult) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nodes", normalizeServiceIds(diagramExtractResult.nodes()));
        map.put("edges", diagramExtractResult.edges());
        map.put("dataStores", diagramExtractResult.dataStores());
        map.put("trustBoundaries", diagramExtractResult.trustBoundaries());
        return map;
    }

    private List<Node> normalizeServiceIds(List<Node> nodes) {
        List<Node> normalizedNodes = new ArrayList<>();
        // track canonical service ids we've added so we can skip duplicates like "Order Service-1", "Order Service-2" or "order_service_1"
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        if (nodes == null) return normalizedNodes;
        for (var node : nodes) {
            if (node == null) continue;
            // compute a canonical id for the node; if it looks like a service id (endsWith -service)
            // then treat duplicates as the same entity regardless of original separators or numbering
            String canonical = toKebabService(node.id());
            if (canonical != null && canonical.endsWith("-service")) {
                if (seen.contains(canonical)) {
                    // skip duplicate numbered/underscored service
                    continue;
                }
                seen.add(canonical);
                normalizedNodes.add(node.withId(canonical));
            } else {
                // non-service or non-canonicalizable nodes are preserved as-is
                normalizedNodes.add(node);
            }
        }
        return normalizedNodes;
    }

    private String toKebabService(String id) {
        if (id == null) return null;
        String serviceId = id.trim();
        // replace whitespace with dash first
        serviceId = serviceId.replaceAll("\\s+", "-");
        // snake_case -> kebab-case
        serviceId = serviceId.replace('_', '-');
        // drop double dashes
        serviceId = serviceId.replaceAll("-{2,}", "-");
        // strip trailing numeric suffix like -1, -2 which are diagram instance markers
        serviceId = serviceId.replaceAll("-\\d+$", "");
        // strip common suffixes
        serviceId = serviceId.replaceAll("(?i)-(svc|service)$", "");
        // lowercase
        serviceId = serviceId.toLowerCase(Locale.ROOT);
        // append "-service" if it looks like a service id (not db/client)
        if (!serviceId.endsWith("-service") && !serviceId.endsWith("-api") && !serviceId.endsWith("-db") && !serviceId.endsWith("-client")) {
            serviceId = serviceId + "-service";
        }
        return serviceId;
    }


}
