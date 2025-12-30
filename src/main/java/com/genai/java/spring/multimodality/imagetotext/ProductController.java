package com.genai.java.spring.multimodality.imagetotext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "You are a compliance officer. Your job is to ensure that product descriptions and images adhere to company policies and legal regulations. "
            + "When a user uploads a product image and description, analyze both to identify any potential compliance issues such as inappropriate content, misleading information, or violations of intellectual property rights. "
            + "Provide clear feedback on whether the product is compliant or not, and if not, specify the reasons for non-compliance and suggest necessary changes.";

    public ProductController(@Qualifier("openAIGeneralChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping(value = "/upload-with-compliance-check", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadAndAnalyzeProductImage(@RequestPart("file") MultipartFile file, @RequestPart("description") String description) {
        log.info("Received file: {} ({} bytes) with description: {}", file.getOriginalFilename(), file.getSize(), description);

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> {
                    u.text(description);
                    u.media(MimeType.valueOf(Objects.requireNonNull(file.getContentType())), file.getResource());
                })
                .call()
                .content();

    }
}
