package com.example.igtracker.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lokalni HTTP server (samo 127.0.0.1) koji prima OAuth redirect.
 * Ako je prosleđen expectedState, odbija callback čiji se state ne poklapa (zaštita od CSRF).
 */
public class OAuthCallbackServer {
    private HttpServer httpServer;
    private ExecutorService executor;
    private final CompletableFuture<String> codeFuture = new CompletableFuture<>();

    public CompletableFuture<String> start(int port, String path) {
        return start(port, path, null);
    }

    public CompletableFuture<String> start(int port, String path, String expectedState) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            httpServer.createContext(path, new CallbackHandler(expectedState));
            executor = Executors.newSingleThreadExecutor();
            httpServer.setExecutor(executor);
            httpServer.start();
            return codeFuture;
        } catch (IOException e) {
            throw new IllegalStateException("Nije moguće pokrenuti lokalni callback server (port zauzet?).", e);
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    private class CallbackHandler implements HttpHandler {
        private final String expectedState;

        private CallbackHandler(String expectedState) {
            this.expectedState = expectedState;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getRawQuery();
            String code = null;
            String error = null;
            String state = null;

            if (query != null) {
                for (String part : query.split("&")) {
                    String[] pair = part.split("=", 2);
                    if (pair.length == 2) {
                        String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        switch (pair[0]) {
                            case "code" -> code = value;
                            case "error" -> error = value;
                            case "state" -> state = value;
                            default -> { }
                        }
                    }
                }
            }

            boolean stateOk = expectedState == null
                    || (state != null && MessageDigest.isEqual(
                            expectedState.getBytes(StandardCharsets.UTF_8), state.getBytes(StandardCharsets.UTF_8)));

            String response;
            Throwable failure = null;
            if (!stateOk) {
                failure = new IllegalStateException("OAuth greška: neispravan state parametar (moguć CSRF pokušaj).");
                response = "Prijava odbijena: neispravan state parametar.";
            } else if (code != null) {
                response = "Prijava uspešna. Možeš da zatvoriš ovaj prozor i vratiš se u aplikaciju.";
            } else {
                failure = new IllegalStateException("OAuth greška: " + (error == null ? "nije vraćen code" : error));
                response = "Prijava nije uspela. Vrati se u aplikaciju.";
            }

            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }

            if (failure != null) {
                codeFuture.completeExceptionally(failure);
            } else {
                codeFuture.complete(code);
            }
        }
    }
}
