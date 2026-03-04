package com.genai.java.spring.aiagent.agent.records;

import java.util.List;

public record Plan(List<PlanStep> steps) {

    @Override
    public String toString() {
        if (steps == null) {
            return "Plan{steps=null}";
        }
        String stepsString = steps.stream()
                .map(step -> step == null ? "null" : step.toString())
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("");
        return "Plan{steps=" + System.lineSeparator() + stepsString + "}";
    }

}