package com.genai.posture.tools;

import com.genai.posture.service.PostureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PostureTools {

    private final PostureService postureService;

    public PostureTools(PostureService postureService) {
        this.postureService = postureService;
    }

    @Tool(name = "security_posture", description = "Returns the posture of a service by service id and environment.")
    public Map<String, Object> getPostureByServiceIdAndEnv(String serviceId, String env) {
        log.info("Getting posture for serviceId: {} in env: {}", serviceId, env);
        return postureService.getPosture(serviceId, env);
    }
}
