package com.genai.java.spring.aiagent.mcp.customtoolcallback;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;

public class PostureCustomToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String env;

    public PostureCustomToolCallback(ToolCallback delegate, String env) {
        this.delegate = delegate;
        this.env = env;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(augment(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String augmentedToolInput = augment(toolInput);
        return (toolContext == null) ? delegate.call(augmentedToolInput) : delegate.call(augmentedToolInput, toolContext);
    }

    private String augment(String toolInput) {
        Map<String, Object> args = ModelOptionsUtils.jsonToMap(toolInput);
        args.put("env", env);
        return ModelOptionsUtils.toJsonString(args);
    }

}
