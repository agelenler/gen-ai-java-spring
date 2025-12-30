package com.genai.java.spring.multimodality.speechtotext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/meeting-assistant")
public class MeetingAssistantController {

    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "You are a helpful meeting assistant. Your job is to summarize meeting notes transcribed by audio model. "
            + "When a user uploads an audio clip along with a message describing the meeting context, an audio model will analyze the audio to produce an accurate transcription. "
            + "Then, you generate a concise summary of the key points discussed in the meeting, tailored to the context provided by the user. "
            + "Ensure the summary captures the main topics and action items.";


    public MeetingAssistantController(OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel,
                                      @Qualifier("openAIGeneralChatClient") ChatClient chatClient) {
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
        this.chatClient = chatClient;
    }

    @PostMapping(value = "/transcribe-summarize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String transcribeAndSummarizeMeetingNotes(@RequestPart("file") MultipartFile file, @RequestPart("message") String message) {
        log.info("Received file: {} ({} bytes) with message: {}", file.getOriginalFilename(), file.getSize(), message);

//        OpenAiAudioTranscriptionOptions audioOptions = OpenAiAudioTranscriptionOptions.builder()
//                .model("whisper-1")
//                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.VTT) // json, srt, text
//                .temperature(0.7f)
//                .language("en")
//                .prompt(message) // provide context to improve the transcription accuracy
//                .build();

        AudioTranscriptionPrompt transcriptionPrompt = new AudioTranscriptionPrompt(file.getResource());

        var transcriptResponse = openAiAudioTranscriptionModel.call(transcriptionPrompt);

        var transcript = transcriptResponse.getResult().getOutput();

        log.info("transcript result: {}", transcript);

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("User message: {message} - Audio transcript: {transcript}" +
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
                        .param("message", message)
                        .param("transcript", transcript))
                .call()
                .content();



    }
}
