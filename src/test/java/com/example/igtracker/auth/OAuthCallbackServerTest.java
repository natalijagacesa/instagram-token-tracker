package com.example.igtracker.auth;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthCallbackServerTest {
    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    private static HttpResponse<String> get(String url) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void returnsCodeWhenStateMatches() throws Exception {
        int port = freePort();
        OAuthCallbackServer server = new OAuthCallbackServer();
        CompletableFuture<String> future = server.start(port, "/callback", "stanje123");
        try {
            HttpResponse<String> response = get("http://127.0.0.1:" + port + "/callback?code=abc%20def&state=stanje123");
            assertEquals(200, response.statusCode());
            assertEquals("abc def", future.get(5, TimeUnit.SECONDS));
        } finally {
            server.stop();
        }
    }

    @Test
    void rejectsWrongState() throws Exception {
        int port = freePort();
        OAuthCallbackServer server = new OAuthCallbackServer();
        CompletableFuture<String> future = server.start(port, "/callback", "ocekivano");
        try {
            get("http://127.0.0.1:" + port + "/callback?code=abc&state=napadac");
            ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            assertTrue(ex.getCause().getMessage().contains("state"));
        } finally {
            server.stop();
        }
    }

    @Test
    void reportsProviderError() throws Exception {
        int port = freePort();
        OAuthCallbackServer server = new OAuthCallbackServer();
        CompletableFuture<String> future = server.start(port, "/callback", "s");
        try {
            get("http://127.0.0.1:" + port + "/callback?error=access_denied&state=s");
            ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            assertTrue(ex.getCause().getMessage().contains("access_denied"));
        } finally {
            server.stop();
        }
    }

    @Test
    void portIsFreeAgainAfterStop() throws Exception {
        int port = freePort();
        OAuthCallbackServer first = new OAuthCallbackServer();
        first.start(port, "/callback", "s");
        first.stop();
        OAuthCallbackServer second = new OAuthCallbackServer();
        second.start(port, "/callback", "s");
        second.stop();
    }
}
