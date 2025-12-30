package com.genai.java.spring.multimodality.texttoimage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/marketing-assets")
public class MarketingAssetController {

    private final ImageModel imageModel;

    private static final String SYSTEM_PROMPT = "You are a creative marketing assistant that generates high-quality concept visuals for product campaigns." +
            "Your task is to take structured input from a marketing team — including product name, target audience, and campaign theme and produce concept images." +
            "Ensure the output is vivid, professional, and aligned with the brand’s campaign theme." +
            "Do not include text in the images; focus only on visual design elements.";

    public MarketingAssetController(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    @PostMapping("/generate-campaign-image")
    public Map<String, String> generateCampaignImage(@RequestBody String description) {
//        ImageOptions imageOptions = OpenAiImageOptions.builder()
//                .model("dall-e-3")
//                .width(1024)
//                .height(1024)
//                .style("vivid") // "natural"
//                .quality("hd")
//                .N(1)
//                .responseFormat("url")
//                .build();
        ImagePrompt imagePrompt = new ImagePrompt(SYSTEM_PROMPT + "-" + description);
        ImageResponse imageResponse = imageModel.call(imagePrompt);
        String base64Json = imageResponse.getResult().getOutput().getB64Json();

        byte[] imageBytes = Base64.getDecoder().decode(base64Json);

        try {
            Files.write(Path.of("image.png"), imageBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Map.of("prompt", description, "image-name", "image.png");
    }
}
