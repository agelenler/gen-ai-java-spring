package com.genai.posture.service;

import java.util.Map;

public interface PostureService {
    Map<String, Object> getPosture(String serviceId, String env);
}
