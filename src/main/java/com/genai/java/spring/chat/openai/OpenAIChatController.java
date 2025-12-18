package com.genai.java.spring.chat.openai;

import com.genai.java.spring.chat.openai.dto.response.SummarizationResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/openai/chat")
public class OpenAIChatController {

    private final static String SYSTEM_PROMPT = "You are a helpful assistant that summarize any given content. " +
            "Ensure the summary is concise, informative, and captures the key points. " +
            "Use a friendly and approachable tone while maintaining professionalism.";

    private final ChatClient chatClient;
    private final OpenAIService openAIService;

    public OpenAIChatController(@Qualifier("openAIChatClient") ChatClient chatClient, OpenAIService openAIService) {
        this.chatClient = chatClient;
        this.openAIService = openAIService;
    }

    @PostMapping("/summarize")
    public ChatClientResponse summarize(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .chatClientResponse();
    }

    @PostMapping("/summarize-meeting-notes")
    public String summarizeMeetingNotes(@RequestBody String meetingNotes) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("Can you summarize the following meeting notes: {meetingNotes}" +
                                " Use the format as described in the following example while doing the summarization:" +
                                " Input: In today’s sales strategy meeting, we reviewed Q3 targets and performance gaps. The team agreed to focus on enterprise clients and strengthen partnerships." +
                                " A proposal was made to expand into two new regions. Marketing suggested aligning campaigns with sales objectives to improve lead conversion and shorten sales cycles." +
                                " Output:" +
                                " Action Items:" +
                                "* Focus on enterprise clients and partnerships." +
                                "* Explore expansion into two new regions." +
                                "* Align marketing campaigns with sales objectives." +
                                " Decisions:" +
                                "* Enterprise clients prioritized for Q3." +
                                "* Marketing and sales to work jointly on lead conversion.")
                        .param("meetingNotes", meetingNotes))
                .call()
                .content();
    }

    @PostMapping("/summarize-meeting-notes-structured")
    public SummarizationResponse summarizeMeetingNotesStructuredOutput(@RequestBody String meetingNotes) {
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text("Can you summarize the following meeting notes: {meetingNotes}" +
                                    " Use the format as described in the following example while doing the summarization:" +
                                    " Input: In today’s sales strategy meeting, we reviewed Q3 targets and performance gaps. The team agreed to focus on enterprise clients and strengthen partnerships." +
                                    " A proposal was made to expand into two new regions. Marketing suggested aligning campaigns with sales objectives to improve lead conversion and shorten sales cycles." +
                                    " Output:" +
                                    " Action Items:" +
                                    "* Focus on enterprise clients and partnerships." +
                                    "* Explore expansion into two new regions." +
                                    "* Align marketing campaigns with sales objectives." +
                                    " Decisions:" +
                                    "* Enterprise clients prioritized for Q3." +
                                    "* Marketing and sales to work jointly on lead conversion.")
                            .param("meetingNotes", meetingNotes))
                    .call()
                    .entity(SummarizationResponse.class);
    }

    @PostMapping("/summarize-meeting-notes-structured-list")
    public List<SummarizationResponse> summarizeMeetingNotesStructuredOutputList(@RequestBody String meetingNotes) {
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text("Can you summarize the following meeting notes: {meetingNotes}" +
                                    " Give me 3 different summarization in the same format so that I can choose from." +
                                    " Use the format as described in the following example while doing the summarization:" +
                                    " Input: In today’s sales strategy meeting, we reviewed Q3 targets and performance gaps. The team agreed to focus on enterprise clients and strengthen partnerships." +
                                    " A proposal was made to expand into two new regions. Marketing suggested aligning campaigns with sales objectives to improve lead conversion and shorten sales cycles." +
                                    " Output:" +
                                    " Action Items:" +
                                    "* Focus on enterprise clients and partnerships." +
                                    "* Explore expansion into two new regions." +
                                    "* Align marketing campaigns with sales objectives." +
                                    " Decisions:" +
                                    "* Enterprise clients prioritized for Q3." +
                                    "* Marketing and sales to work jointly on lead conversion.")
                            .param("meetingNotes", meetingNotes))
                    .call()
                    .entity(new ParameterizedTypeReference<>() {
                    });
    }

    @PostMapping(value = "/summarize-with-streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> summarizeWithStreaming(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .stream()
                .content()
                .bufferTimeout(1, Duration.ofMillis(200)) // ~40 tokens or every 200ms
                .map(tokenList -> String.join(",", tokenList));
    }

    @PostMapping("/summarize-with-openai-java-client")
    public String summarizeWithOpenAIJavaClient(@RequestBody String message) {
        return openAIService.chat(message);
    }

    @PostMapping("/general-chat")
    public String generalChat(@RequestBody String message) {
        ChatOptions chatOptions = ChatOptions.builder()
                .maxTokens(1000) // output token
                .stopSequences(List.of("END_OF_PARAGRAPH"))
                .build();
        return chatClient.prompt()
                .options(chatOptions)
                .user(message)
                .call()
                .content();
    }

}
