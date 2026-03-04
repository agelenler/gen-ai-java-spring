package com.genai.java.spring.aiagent.agent.records;

import java.util.List;

record PlanStep(int step, String goal, String toolHint, List<String> targets) {}