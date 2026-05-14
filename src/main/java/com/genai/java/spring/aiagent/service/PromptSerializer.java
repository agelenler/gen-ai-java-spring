package com.genai.java.spring.aiagent.service;

import org.springframework.ai.chat.prompt.Prompt;

public interface PromptSerializer {

    /**
     * Serialize a Prompt to JSON string to later save to database with checkpoint and approval info.
     * @param prompt the conversation prompt
     * @param runId the unique run identifier
     * @param checkpoint current checkpoint enum name
     * @return serialized JSON string
     */
    String serialize(Prompt prompt, String runId, String checkpoint);

    /**
     * Deserialize a JSON string back into a Prompt object.
     * @param conversationJson the serialized conversation JSON
     * @return reconstructed Prompt
     */
    Prompt deserialize(String conversationJson);
}
