package com.aquanexus.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;

/**
 * Verifies a Google OAuth2 access token by calling Google's userinfo endpoint.
 * Returns the user's profile information from Google.
 */
@Service
public class GoogleOAuthService {

    private static final String GOOGLE_USERINFO_URL =
            "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestTemplate restTemplate;

    public GoogleOAuthService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Verifies the Google access token and fetches user profile.
     *
     * @param accessToken  Google OAuth2 access token from frontend
     * @return Map containing: sub, name, email, picture, email_verified
     * @throws RuntimeException if the token is invalid or Google call fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verifyAndGetProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_USERINFO_URL,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to verify Google token: empty response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired Google access token: " + e.getMessage());
        }
    }
}
