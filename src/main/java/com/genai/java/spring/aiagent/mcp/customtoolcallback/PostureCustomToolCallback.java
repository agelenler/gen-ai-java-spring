package com.genai.java.spring.aiagent.mcp.customtoolcallback;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PostureCustomToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String env;
    private final TextMapPropagator textMapPropagator;

    public PostureCustomToolCallback(ToolCallback delegate, String env, OpenTelemetry openTelemetry) {
        this.delegate = delegate;
        this.env = env;
        this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
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

        //Inject W3C trace context
        Map<String, String> carrier = new HashMap<>();
        log.info("Injecting trace context into posture tool call with context: {}", Context.current());
        textMapPropagator.inject(Context.current(), carrier, Map::put);
        if (carrier.get("traceparent") != null) {
            args.put("_traceparent", carrier.get("traceparent"));
        }
        return ModelOptionsUtils.toJsonString(args);
    }

}
