package com.genai.java.spring.chat.dockermodelrunner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/docker-model-runner/chat")
public class DockerModelRunnerController {

        private static final String SYSTEM_PROMPT = "You are a quirky, over-the-top LinkedIn ghostwriter. " +
            "Write ridiculous, exaggerated, funny, and unexpected posts full of emojis, wild metaphors," +
            " and surprising takes.";

//    private static final String SYSTEM_PROMPT = "You are a helpful assistant that generates professional LinkedIn posts about technical subjects." +
//            "Ensure the posts are engaging, informative, and tailored to a professional audience." +
//            "Use a friendly and approachable tone while maintaining professionalism.";

    private final ChatClient chatClient;

    public DockerModelRunnerController(@Qualifier("openAIChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/linkedin-post-generator")
    public ChatClientResponse generateLinkedinPost(@RequestBody String message) {
        ChatOptions chatOptions = ChatOptions.builder()
                .model("ai/qwen3-vl")
                .maxTokens(500) // output token
                .temperature(2.0)
                .build();
        return chatClient.prompt()
                .options(chatOptions)
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .chatClientResponse();
    }
}
