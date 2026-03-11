package com.genai.java.spring.aiagent.tools.posture;

import com.genai.java.spring.aiagent.config.data.AIAgentConfigData;
import com.genai.java.spring.aiagent.tools.posture.records.PostureArgs;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class PostureToolsWithMcpClient {

    private final McpSyncClient postureMcpClient;
    private final McpContentUtils mcpContentUtils;
    private final AIAgentConfigData.PostureToolProperties postureToolProperties;

    public PostureToolsWithMcpClient(@Qualifier("posture") McpSyncClient postureMcpClient,
                                     McpContentUtils mcpContentUtils,
                                     AIAgentConfigData aiAgentConfigData) {
        this.postureMcpClient = postureMcpClient;
        this.mcpContentUtils = mcpContentUtils;
        this.postureToolProperties = aiAgentConfigData.getPostureTool();
    }

    @Tool(name = "security_posture", description = "Get security posture for a service (internetFacing, data classes, TLS, vulnerabilities, secrets).")
    Map<String, Object> getSecurityPosture(PostureArgs postureArgs) {
        try {
            if (postureArgs == null) {
                return Collections.emptyMap();
            }

            log.info("Calling security_posture tool using mcp client with service id: {}", postureArgs.serviceId());

            McpSchema.CallToolResult toolResult = postureMcpClient.callTool(
                    new McpSchema.CallToolRequest(
                            "security_posture", // tool name
                            Map.of("serviceId", postureArgs.serviceId(), "env", postureToolProperties.getEnv())));

            return mcpContentUtils.asMap(toolResult.content());
        } catch (Exception e) {
            log.error("Error in posture service tool", e);
            return Map.of("error", "POSTURE_SERVICE_CALL_FAILED", "message", e.getMessage());
        }

    }
}
