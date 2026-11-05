package com.genai.java.spring.aiagent.agent.subagents.webguidance;

import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceArgs;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceResult;

public interface WebGuidanceAgent {

    WebGuidanceResult generateGuidance(WebGuidanceArgs args);
}
