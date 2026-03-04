package com.genai.java.spring.aiagent.tools.diagram.records;

import java.util.List;
import java.util.Map;

public record Node(
        String id,           // e.g., "order-service"
        String type,         // e.g., "gateway", "service", "web", "db", "queue"
        String zone,         // e.g., "dmz", "internal", "restricted"
        String technology,   // e.g., "Spring Boot", "NGINX", "Postgres"
        List<String> labels, // arbitrary labels/tags
        Map<String,Object> meta // optional extras (version, image, replicas…)
) {
    public Node withId(String newId) {
        return new Node(
                newId,
                this.type,
                this.zone,
                this.technology,
                this.labels,
                this.meta
        );
    }

}