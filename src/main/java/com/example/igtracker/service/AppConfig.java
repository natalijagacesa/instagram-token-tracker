package com.example.igtracker.service;

public class AppConfig {
    private String instagramClientId = "YOUR_INSTAGRAM_CLIENT_ID";
    private String instagramClientSecret = "YOUR_INSTAGRAM_CLIENT_SECRET";
    private String redirectUri = "http://127.0.0.1:8085/callback";
    private String scopes = "instagram_business_basic";
    private String encryptionKeyBase64 = "CHANGE_ME_WITH_32_BYTE_BASE64_KEY";

    public String getInstagramClientId() {
        return instagramClientId;
    }

    public void setInstagramClientId(String instagramClientId) {
        this.instagramClientId = instagramClientId;
    }

    public String getInstagramClientSecret() {
        return instagramClientSecret;
    }

    public void setInstagramClientSecret(String instagramClientSecret) {
        this.instagramClientSecret = instagramClientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getEncryptionKeyBase64() {
        return encryptionKeyBase64;
    }

    public void setEncryptionKeyBase64(String encryptionKeyBase64) {
        this.encryptionKeyBase64 = encryptionKeyBase64;
    }
}
