package com.genai.java.spring.aiagent.tools.web;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class GcpTokenProvider {

    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/cloud-platform");

    public String getAccessTokenValue() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials.getApplicationDefault().createScoped(SCOPES);
        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }
}
