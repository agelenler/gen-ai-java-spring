package com.genai.java.spring.aiagent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aiagent.dto.StoredMessage;
import com.genai.java.spring.aiagent.service.PromptSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PromptSerializerImpl implements PromptSerializer {

    private final ObjectMapper objectMapper;

    public PromptSerializerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Prompt prompt, String runId, String checkpoint) {
        try {
            // Convert prompt messages to StoredMessage format
            List<StoredMessage> storedMessages = toStoredMessage(prompt);
            String conversationJson = objectMapper.writeValueAsString(storedMessages);
            log.info("Serialized prompt for runId={} checkpoint={}", runId, checkpoint);
            return conversationJson;
        } catch (JsonProcessingException e) {
            log.error("Error serializing prompt: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize prompt", e);
        }
    }

    @Override
    public Prompt deserialize(String conversationJson) {
        try {
            log.info("Deserializing prompt from Json={}", conversationJson);
            List<StoredMessage> storedMessages = objectMapper.readValue(conversationJson,
                    new TypeReference<>() {
                    });
            List<Message> messages = storedMessages.stream().map(storedMessage -> {
                Message message = switch (storedMessage.role()) {
                    case "system" -> new SystemMessage(storedMessage.content());
                    case "user" -> new UserMessage(storedMessage.content());
                    case "assistant" -> new AssistantMessage(storedMessage.content());
                    case "tool" -> new UserMessage("""
                            Previous tool observation:
                            %s
                            """.formatted(storedMessage.content())); // Tool messages stored as user messages
                    default -> throw new IllegalArgumentException("Unknown role: " + storedMessage.role());
                };
                return message;
            }).toList();

            return new Prompt(messages);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing prompt: {}", e.getMessage());
            throw new RuntimeException("Failed to deserialize prompt", e);
        }
    }

    private List<StoredMessage> toStoredMessage(Prompt prompt) {
        return prompt.getInstructions().stream().map(message -> {
            String role = deriveRole(message);
            String content = message.getText();
            String name = null;
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                var responses = toolResponseMessage.getResponses();
                if (!responses.isEmpty()) {
                    name = responses.getFirst().name();
                }
            }
            Map<String, Object> metadata =
                    message.getMetadata() != null ? message.getMetadata() : Map.of();
            return new StoredMessage(role, content, name, metadata);
        }).toList();
    }

    private String deriveRole(Message message) {
        return switch (message) {
            case SystemMessage _ -> "system";
            case UserMessage _ -> "user";
            case AssistantMessage _ -> "assistant";
            case ToolResponseMessage _ -> "tool";
            default -> message.getMessageType().name().toLowerCase();
        };
    }
}
