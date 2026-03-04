package com.genai.java.spring.aiagent.config.data;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.agent")
public class AIAgentConfigData {
    private String uploadDir;
    private DiagramToolProperties diagramTool;
    private PostureToolProperties postureTool;
    private WebToolProperties webTool;
    private RagToolProperties ragTool;

    @Data
    public static class DiagramToolProperties {
        private Double temperature;
    }

    @Data
    public static class PostureToolProperties {
        private String url;
        private String env;
    }

    @Data
    public static class WebToolProperties {
        private Integer topK;
        private GoogleVertexSearch googleVertexSearch;
        private Owasp owasp;
    }

    @Data
    public static class RagToolProperties {
        private Double similarityThreshold;
        private Integer minTopK;
        private Integer maxTopK;
        private Integer defaultTopK;
    }

    @Data
    public static class GoogleVertexSearch {
        private String endpointBaseUrl;
        private String servingConfig;
    }

    @Data
    public static class Owasp {
        private String cheatSheetProtocol;
        private String cheatSheetUrl;
        private String asvsUrl;
    }
}
