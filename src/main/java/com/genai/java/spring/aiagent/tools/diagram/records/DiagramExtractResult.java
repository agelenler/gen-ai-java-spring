package com.genai.java.spring.aiagent.tools.diagram.records;

import java.util.List;

public record DiagramExtractResult(
    List<Node> nodes,
    List<Edge> edges,
    List<DataStore> dataStores,
    List<TrustBoundary> trustBoundaries
) {}