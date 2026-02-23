package com.genai.java.spring.rag.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal-qa")
public class InternalQAController {

    private final ChatClient chatClient;

    private final String SYSTEM_PROMPT = "You are an internal-docs assistant. " +
            "Provide clear and concise solutions, troubleshooting steps, and recommendations. " +
            "Use ONLY the provided CONTEXT. If not found, say I don't know. " +
            "Cite sources as [source:page_number]." +
            "Each CONTEXT passage starts with a header like [source:filename.pdf, page_number:12]. " +
            "When citing, copy the header’s values exactly (do not simplify to [source:12] or change labels).";

    public InternalQAController(@Qualifier("openAIAdvancedRAGChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/ask")
    public String getInformationFromInternalDocs(@RequestBody String message,
                                                 @RequestParam(value = "filter", required = false) String filter) {
        var prompt = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message);

        //Optionally pass a runtime metadata filter to the retriever
        if (filter != null && !filter.isBlank()) {
            prompt = prompt.advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filter));
        }

        return prompt.call().content();
    }
}
