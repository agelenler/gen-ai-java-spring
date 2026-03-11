package com.genai.posture.config;

import com.genai.posture.tools.PostureTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider postureServiceTools(PostureTools postureTools) {
        return MethodToolCallbackProvider.builder().toolObjects(postureTools).build();

    }
}
