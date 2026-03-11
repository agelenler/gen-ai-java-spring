package com.genai.java.spring.aiagent.mcp.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpConfig {

    @Bean("posture")
    McpSyncClient postureClientAlias(List<McpSyncClient> mcpSyncClients) {
        // spring-ai-mcp-client - posture
        return mcpSyncClients.stream()
                .filter(client -> client.getClientInfo().name().endsWith(" - posture"))
                .findFirst()
                .orElseThrow();
    }
}
