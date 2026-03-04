package com.genai.java.spring.aiagent.tools.posture;

import com.genai.java.spring.aiagent.config.data.AIAgentConfigData;
import com.genai.java.spring.aiagent.tools.posture.records.PostureArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class PostureTools {
    private final WebClient webClient;
    private final AIAgentConfigData.PostureToolProperties postureToolProperties;

    public PostureTools(WebClient.Builder builder, AIAgentConfigData aiAgentConfigData) {
        this.webClient = builder.baseUrl(aiAgentConfigData.getPostureTool().getUrl()).build();
        this.postureToolProperties = aiAgentConfigData.getPostureTool();
    }

    @Tool(name = "security_posture", description = "Get security posture for a service (internetFacing, data classes, TLS, vulnerabilities, secrets).")
    Map<String, Object> getSecurityPosture(PostureArgs postureArgs) {
        try {
            if (postureArgs == null) {
                return Collections.emptyMap();
            }
            log.info("Calling security_posture tool with service id: {}", postureArgs.serviceId());
            return this.webClient.get()
                    .uri(uri -> uri.queryParam("env", postureToolProperties.getEnv()).build(postureArgs.serviceId()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error in posture service tool", e);
            return Map.of("error", "POSTURE_SERVICE_CALL_FAILED", "message", e.getMessage());
        }
    }
}
