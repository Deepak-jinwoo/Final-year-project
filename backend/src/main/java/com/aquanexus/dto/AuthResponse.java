package com.aquanexus.dto;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String photoUrl;
    private String role;
    private boolean isOAuth;

    public AuthResponse() {}

    public AuthResponse(String token, Long id, String fullName, String username,
                        String email, String photoUrl, String role, boolean isOAuth) {
        this.token = token;
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.photoUrl = photoUrl;
        this.role = role;
        this.isOAuth = isOAuth;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isOAuth() { return isOAuth; }
    public void setOAuth(boolean isOAuth) { this.isOAuth = isOAuth; }
}
