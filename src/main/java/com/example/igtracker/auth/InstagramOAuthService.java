package com.example.igtracker.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.example.igtracker.service.AppConfig;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class InstagramOAuthService {
    private static final String AUTHORIZE_URL = "https://www.instagram.com/oauth/authorize";
    private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
    private static final String ME_URL = "https://graph.instagram.com/v21.0/me?fields=user_id,username&access_token=%s";
    private final Gson gson = new Gson();

    private static final long LOGIN_TIMEOUT_MINUTES = 3;
    private final SecureRandom secureRandom = new SecureRandom();

    public CompletableFuture<AuthResult> login(AppConfig config) {
        URI redirectUri = URI.create(config.getRedirectUri());
        int port = redirectUri.getPort();
        String path = redirectUri.getPath();
        String state = generateState();

        OAuthCallbackServer callbackServer = new OAuthCallbackServer();
        CompletableFuture<String> codeFuture = callbackServer.start(port, path, state)
                .orTimeout(LOGIN_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        codeFuture.whenCompleteAsync((code, error) -> callbackServer.stop());

        try {
            openBrowser(buildAuthorizeUrl(config, state));
        } catch (Exception e) {
            callbackServer.stop();
            throw new IllegalStateException("Nije moguće otvoriti browser za Instagram prijavu.", e);
        }

        return codeFuture.thenApply(code -> {
            TokenResponse tokenResponse = exchangeCodeForToken(config, code);
            UserResponse userResponse = fetchUser(tokenResponse.accessToken());
            return new AuthResult(userResponse.userId(), userResponse.username(), tokenResponse.accessToken(), tokenResponse.expiresAt());
        });
    }

    private String generateState() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String buildAuthorizeUrl(AppConfig config, String state) {
        return AUTHORIZE_URL
                + "?enable_fb_login=0"
                + "&force_authentication=1"
                + "&client_id=" + urlEncode(config.getInstagramClientId())
                + "&redirect_uri=" + urlEncode(config.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + urlEncode(config.getScopes())
                + "&state=" + urlEncode(state);
    }

    private TokenResponse exchangeCodeForToken(AppConfig config, String code) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = formEncode(Map.of(
                    "client_id", config.getInstagramClientId(),
                    "client_secret", config.getInstagramClientSecret(),
                    "grant_type", "authorization_code",
                    "redirect_uri", config.getRedirectUri(),
                    "code", code
            ));

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            String responseBody = readBody(status < 400 ? connection.getInputStream() : connection.getErrorStream());
            if (status >= 400) {
                throw new IllegalStateException("Instagram token exchange nije uspeo: " + responseBody);
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            String accessToken = json.get("access_token").getAsString();
            long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600L;
            String expiresAt = Instant.now().plusSeconds(expiresIn).toString();
            return new TokenResponse(accessToken, expiresAt);
        } catch (IOException e) {
            throw new IllegalStateException("Greška pri razmeni authorization code za token.", e);
        }
    }

    private UserResponse fetchUser(String accessToken) {
        try {
            String url = ME_URL.formatted(urlEncode(accessToken));
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            int status = connection.getResponseCode();
            String responseBody = readBody(status < 400 ? connection.getInputStream() : connection.getErrorStream());
            if (status >= 400) {
                throw new IllegalStateException("Nije moguće pročitati Instagram korisnika: " + responseBody);
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            String userId = json.has("user_id") ? json.get("user_id").getAsString() : "unknown";
            String username = json.has("username") ? json.get("username").getAsString() : "unknown";
            return new UserResponse(userId, username);
        } catch (IOException e) {
            throw new IllegalStateException("Greška pri čitanju Instagram profila.", e);
        }
    }

    private void openBrowser(String url) throws IOException {
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("Desktop browser nije podržan.");
        }
        Desktop.getDesktop().browse(URI.create(url));
    }

    private String formEncode(Map<String, String> values) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            joiner.add(urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()));
        }
        return joiner.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    public record AuthResult(String igUserId, String username, String accessToken, String expiresAt) { }
    private record TokenResponse(String accessToken, String expiresAt) { }
    private record UserResponse(String userId, String username) { }
}
