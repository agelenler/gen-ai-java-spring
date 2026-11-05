package com.genai.java.spring.aiagent.agent.subagents.knowledgeevidence;

import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceArgs;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceResult;

public interface KnowledgeEvidenceAgent {

    KnowledgeEvidenceResult generateGuidance(KnowledgeEvidenceArgs args);
}
