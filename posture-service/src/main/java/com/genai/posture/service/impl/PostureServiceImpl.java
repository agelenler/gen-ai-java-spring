package com.genai.posture.service.impl;

import com.genai.posture.dataaccess.entity.SecurityPostureEntity;
import com.genai.posture.dataaccess.repository.SecurityPostureRepository;
import com.genai.posture.service.PostureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class PostureServiceImpl implements PostureService {

    private final SecurityPostureRepository securityPostureRepository;

    public PostureServiceImpl(SecurityPostureRepository securityPostureRepository) {
        this.securityPostureRepository = securityPostureRepository;
    }

    @Override
    public Map<String, Object> getPosture(String serviceId, String env) {
        var securityPosture = securityPostureRepository.findByServiceIdAndEnvironment(serviceId, env)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Posture not found!"));
        log.info("Found posture for serviceId: {} in env: {}", serviceId, env);
        return getSecurityPostureMap(securityPosture);
    }

    private Map<String, Object> getSecurityPostureMap(SecurityPostureEntity securityPosture) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("serviceId", securityPosture.getServiceId());
        out.put("environment", securityPosture.getEnvironment());
        out.put("internetFacing", securityPosture.isInternetFacing());
        out.put("authn", securityPosture.getAuthn());
        out.put("data", securityPosture.getData());
        out.put("tls", securityPosture.getTls());
        out.put("network", securityPosture.getNetwork());
        out.put("secrets", securityPosture.getSecrets());
        out.put("vulns", securityPosture.getVulns());
        return out;
    }
}
