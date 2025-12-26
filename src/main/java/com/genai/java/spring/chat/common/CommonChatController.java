package com.genai.java.spring.chat.common;


import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/common/chat")
public class CommonChatController {

    private final ChatClient chatClient;

    public CommonChatController(@Qualifier("openAIGeneralChatClient") ChatClient chatClient,
                                SimpleLoggerAdvisor simpleLoggerAdvisor) {
        this.chatClient = chatClient.mutate().defaultAdvisors(simpleLoggerAdvisor).build();
    }

    @PostMapping("")
    public ChatClientResponse generalChat(@RequestBody String message) {
        ChatClientResponse chatClientResponse = chatClient.prompt()
                .user(message)
                .call()
                .chatClientResponse();
        logTokenInfo(message, chatClientResponse);
        return chatClientResponse;
    }

    private void logTokenInfo(String message, ChatClientResponse chatClientResponse) {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding encoding = registry.getEncodingForModel(chatClientResponse.chatResponse().getMetadata().getModel()).orElse(null);

        if (encoding == null) {
            log.warn("Encoding not found for model: {}", chatClientResponse.chatResponse().getMetadata().getModel());
            return;
        }

        int inputTokenNumber = encoding.countTokens(message);
        log.info("Input token number: " + inputTokenNumber);

        int outputTokenNumber = encoding.countTokens(chatClientResponse.chatResponse().getResult().getOutput().getText());
        log.info("Output token number: " + outputTokenNumber);
    }

}
