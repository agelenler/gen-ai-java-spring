package com.genai.posture.controller;

import com.genai.posture.service.PostureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/posture")
public class PostureController {

    private final PostureService postureService;

    public PostureController(PostureService postureService) {
        this.postureService = postureService;
    }

    @GetMapping("/{serviceId}")
    public Map<String, Object> getPostureByServiceIdAndEnv(@PathVariable String serviceId,
                                                           @RequestParam(defaultValue = "prod") String env) {
        return postureService.getPosture(serviceId, env);
    }
}
