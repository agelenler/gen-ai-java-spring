package com.genai.java.spring.aiagent.tools.diagram.records;

import java.util.List;

public record ExtractArgs(String fileName, String id, List<String> hints) {
}
