package com.genai.java.spring.aiagent.agent.subagents.helper;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for sub-agent prompt builders with common reusable logic.
 * Provides shared methods for creating initial prompts and initializing messages.
 * Subclasses should implement buildSystemPrompt() and buildUserPrompt() to provide domain-specific content.
 */
public abstract class SubAgentPromptBuilder {

    /**
     * Create the initial prompt for the sub-agent.
     * Initializes messages with system and user prompts and returns a Prompt with the given tool calling options.
     *
     * @param args Agent-specific arguments
     * @param toolCallingChatOptions Tool calling options for the prompt
     * @return Prompt with initialized messages and tool calling options
     */
    public Prompt createInitialPrompt(Object args, ToolCallingChatOptions toolCallingChatOptions) {
        // Initialize messages
        List<Message> messages = initializeMessages(args);
        // Create prompt and begin agentic loop
        return new Prompt(messages, toolCallingChatOptions);
    }

    /**
     * Initialize the message list with system and user prompts.
     * Calls abstract methods to allow subclasses to provide domain-specific content.
     *
     * @param args Agent-specific arguments
     * @return List of messages (SystemMessage followed by UserMessage)
     */
    private List<Message> initializeMessages(Object args) {
        // Build system prompt tailored for the sub-agent domain
        String systemPrompt = buildSystemPrompt(args);
        // Build user prompt specific to the sub-agent task
        String userPrompt = buildUserPrompt(args);
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userPrompt));
        return messages;
    }

    /**
     * Build system prompt for the sub-agent.
     * Subclasses must implement to provide domain-specific system prompt content.
     *
     * @param args Agent-specific arguments
     * @return System prompt string
     */
    protected abstract String buildSystemPrompt(Object args);

    /**
     * Build user prompt that decomposes topics into search strategy.
     * Subclasses must implement to provide domain-specific user prompt content.
     *
     * @param args Agent-specific arguments
     * @return User prompt string
     */
    protected abstract String buildUserPrompt(Object args);
}

