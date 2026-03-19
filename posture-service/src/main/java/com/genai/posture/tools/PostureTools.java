package com.genai.posture.tools;

import com.genai.posture.service.PostureService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PostureTools {

    private final PostureService postureService;
    private final OpenTelemetry openTelemetry;
    private final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<Map<String, String>>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier != null ? carrier.keySet() : Collections.emptyList();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            if (carrier == null) {
                return null;
            }
            return carrier.get(key);
        }
    };

    public PostureTools(PostureService postureService, OpenTelemetry openTelemetry) {
        this.postureService = postureService;
        this.openTelemetry = openTelemetry;
    }

    @Tool(name = "security_posture", description = "Returns the posture of a service by service id and environment.")
    public Map<String, Object> getPostureByServiceIdAndEnv(String _traceparent, String serviceId, String env) {
        Map<String, String> carrier = new HashMap<>();
        carrier.put("traceparent", _traceparent);

        Context extracted = openTelemetry.getPropagators().getTextMapPropagator()
                        .extract(Context.current(), carrier, MAP_GETTER);

        try (Scope parentScope = extracted.makeCurrent()) {
            // continue trace (same traceId, new spanId)
            var tracer = openTelemetry.getTracer("posture-service");
            var child = tracer.spanBuilder("mcp.security_posture")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan();
            try (Scope childScope = child.makeCurrent()) {
                log.info("Getting posture for serviceId: {} in env: {}", serviceId, env);
                return postureService.getPosture(serviceId, env);
            } finally {
                child.end();
            }
        }
    }
}
