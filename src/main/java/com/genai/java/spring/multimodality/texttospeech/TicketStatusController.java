package com.genai.java.spring.multimodality.texttospeech;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/tickets")
public class TicketStatusController {

    private final ChatClient chatClient;
    private final TicketStatusTools ticketStatusTools;
    private final TextToSpeechModel textToSpeechModel;

    private static final String SYSTEM_PROMPT = "You are a customer support assistant that provides ticket status updates. "
            + "When a user inquires about their ticket status, you generate a concise and clear response that includes the ticket ID, current status, and any next steps. "
            + "Ensure the response is professional and empathetic, addressing the user's concerns effectively.";

    public TicketStatusController(@Qualifier("openAIGeneralChatClient") ChatClient chatClient,
                                  TicketStatusTools ticketStatusTools,
                                  TextToSpeechModel textToSpeechModel) {
        this.chatClient = chatClient;
        this.ticketStatusTools = ticketStatusTools;
        this.textToSpeechModel = textToSpeechModel;
    }

    @PostMapping("/status")
    public ResponseEntity<byte[]> getTicketStatus(@RequestBody String message, @RequestHeader(value = "user-id") String userId) {
        log.info("Received ticket status request with message: {}, for user: {}", message, userId);

        String userMessage = message + ". user-id: " + userId;

        TicketResponse ticketResponse = chatClient.prompt()
                .tools(ticketStatusTools)
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .entity(TicketResponse.class);

//        var audioOptions = OpenAiAudioSpeechOptions.builder()
//                .model("tts-1")
//                .voice(OpenAiAudioApi.SpeechRequest.Voice.NOVA) //ALLOY, ECHO, FABLE, ONYX, SHIMMER
//                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
//                .speed(1.0)
//                .build();

        var speechPrompt = new TextToSpeechPrompt(ticketResponse.message());

        var speechResponse = textToSpeechModel.call(speechPrompt);

        byte[] audioBytes = speechResponse.getResult().getOutput();

        log.info("Returning audio file with size: {}", audioBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ticket-status.mp3\"")
                .body(audioBytes);

    }
}
