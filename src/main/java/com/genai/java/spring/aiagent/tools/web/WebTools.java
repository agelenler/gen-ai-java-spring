package com.genai.java.spring.aiagent.tools.web;

import com.genai.java.spring.aiagent.config.data.AIAgentConfigData;
import com.genai.java.spring.aiagent.tools.web.records.WebArgs;
import com.genai.java.spring.aiagent.tools.web.records.WebItem;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WebTools {

    private final WebClient webClient;
    private final GcpTokenProvider gcpTokenProvider;
    private final AIAgentConfigData.WebToolProperties webToolProperties;
    private final ObservationRegistry registry;

    public WebTools(WebClient.Builder builder, GcpTokenProvider gcpTokenProvider, AIAgentConfigData aiAgentConfigData, ObservationRegistry registry) {
        this.registry = registry;
        this.webClient = builder.baseUrl(aiAgentConfigData.getWebTool().getGoogleVertexSearch().getEndpointBaseUrl()).build();
        this.gcpTokenProvider = gcpTokenProvider;
        this.webToolProperties = aiAgentConfigData.getWebTool();
    }

    @Tool(name = "web_search", description = "Fetch OWASP/NIST/CWE guidance (allowlisted domains only). Returns title/url/snippet.")
    public Map<String, Object> search(WebArgs webArgs) {
        try {
            Observation toolCallObservation = Observation.start("web_search", registry);
            try (Observation.Scope scope = toolCallObservation.openScope()) {
                if (webArgs == null) {
                    return Collections.emptyMap();
                }

                log.info("Calling web_search tool with topic: {} and topK: {}", webArgs.topic(), webArgs.topK());

                int topK = this.webToolProperties.getTopK();
                String topic = webArgs.topic();

                if (topic.isEmpty()) {
                    return Map.of("matches", List.of(), "warning", "EMPTY_TOPIC");
                }

                List<WebItem> items = this.webToolProperties.getGoogleVertexSearch().getEndpointBaseUrl().isEmpty()
                        ? fallbackOwaspIndexSearch(topic, topK)
                        : googleVertexSearchFiltered(topic, topK);

                return Map.of("matches", items);
            } finally {
                toolCallObservation.stop();
            }
        } catch (Exception e) {
            return Map.of("error", "WEB_SEARCH_FAILED", "message", e.getMessage());
        }
    }

    private List<WebItem> fallbackOwaspIndexSearch(String topic, int topK) {
        List<WebItem> results = new ArrayList<>();

        try {
            results.addAll(scanCheatSheets(topic));
            results.addAll(scanASVS(topic));
        } catch (Exception e) {
            log.warn("Got exception on fallbackOwaspIndexSearch", e);
        }

        LinkedHashMap<String, WebItem> uniqueLinks = new LinkedHashMap<>();
        for (var result : results) {
            uniqueLinks.putIfAbsent(result.url(), result);
        }

        return uniqueLinks.values().stream().limit(topK).toList();
    }

    private List<WebItem> scanCheatSheets(String topic) {
        String html = this.webClient.get()
                .uri(this.webToolProperties.getOwasp().getCheatSheetProtocol()
                        + this.webToolProperties.getOwasp().getCheatSheetUrl())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (html == null || html.isEmpty()) {
            return Collections.emptyList();
        }

        List<WebItem> results = new ArrayList<>();
        var matcher = Pattern
                .compile("<a\\s+href=\"([^\"]+)\"[^>]*>([^<]+)</a>", Pattern.CASE_INSENSITIVE)
                .matcher(html);

        while (matcher.find()) {
            String href = matcher.group(1);
            String text = matcher.group(2);
            if (href.startsWith(this.webToolProperties.getOwasp().getCheatSheetProtocol())
                    && href.contains(this.webToolProperties.getOwasp().getCheatSheetUrl())) {
               if (text != null && text.toLowerCase().contains(topic.toLowerCase())) {
                   results.add(new WebItem(text.trim(), href, "OWASP Cheat Sheet: " + text.trim()));
               }
            }
        }

        return results;
    }

    private List<WebItem> scanASVS(String topic) {
        // ASVS main page contains section anchors; again, minimal parsing for demo
        String html = webClient.get().uri(this.webToolProperties.getOwasp().getAsvsUrl())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (html == null || html.isEmpty()) {
            return List.of();
        }

        List<WebItem> results = new ArrayList<>();
        var matcher = java.util.regex.Pattern
                .compile("<a\\s+href=\"(#[^\"]+)\"[^>]*>([^<]{3,120})</a>", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html);

        String topicLowerCase = topic.toLowerCase();
        while (matcher.find()) {
            String anchor = matcher.group(1);
            String text = matcher.group(2);
            if (text != null && text.toLowerCase().contains(topicLowerCase)) {
                results.add(new WebItem(
                        text.trim(),
                        this.webToolProperties.getOwasp().getAsvsUrl() + anchor,
                        "OWASP ASVS section: " + text.trim()
                ));
            }
        }
        return results;
    }

    private List<WebItem> googleVertexSearchFiltered(String topic, int topK) {
        var googleSearchResponse = searchWithGoogleVertex(topic, topK);

        List<Map<String, Object>> results = (List<Map<String, Object>>) googleSearchResponse.getOrDefault("results", List.of());

        return results.stream()
                .map(it -> {
                    Map<String, Object> doc = (Map<String, Object>) it.get("document");
                    Map<String, Object> derived = (Map<String, Object>) doc.get("derivedStructData");
                    return new WebItem(
                            (String) derived.getOrDefault("title", ""),
                            (String) derived.getOrDefault("link", ""),
                            getSnippets(derived)
                    );
                })
                .limit(topK)
                .toList();
    }

    private String getSnippets(Map<String, Object> derived) {
        List<String> snippets = new ArrayList<>();
        Object snippetsObj = derived.get("snippets");

        if (snippetsObj instanceof List<?> snippetList) {
            for (Object s : snippetList) {
                Map<String, Object> snippetMap = (Map<String, Object>) s;
                String snippet = (String) snippetMap.get("snippet");

                if (snippet != null && !snippet.isBlank()) {
                    snippets.add(snippet);
                }
            }
        }
        return String.join(", ", snippets);
    }


    private Map<String, Object> searchWithGoogleVertex(String topic, int topK) {
        Map<String, Object> body = Map.of(
                "query", topic,
                "pageSize", topK
        );
        String token = getToken();
        return webClient.post()
                .uri("/v1/" + this.webToolProperties.getGoogleVertexSearch().getServingConfig() + ":search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();
    }

    private String getToken() {
        try {
            return gcpTokenProvider.getAccessTokenValue();
        } catch (IOException e) {
            throw new RuntimeException("Failed to obtain GCP access token.", e);
        }
    }
}
