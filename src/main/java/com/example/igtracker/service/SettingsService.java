package com.example.igtracker.service;

import com.example.igtracker.crypto.AesGcmEncryptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsService {
    private static final Path DEFAULT_DIR = Paths.get(System.getProperty("user.home"), ".ig-token-tracker");
    private final Path configDir;
    private final Path configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public SettingsService() {
        this(DEFAULT_DIR);
    }

    public SettingsService(Path configDir) {
        this.configDir = configDir;
        this.configFile = configDir.resolve("config.json");
    }

    /**
     * Učitava config; ako ne postoji, pravi ga. Ako AES ključ nije podešen,
     * automatski se generiše (korisnik ne mora ručno da pokreće Java kod).
     */
    public AppConfig loadOrCreate() {
        try {
            Files.createDirectories(configDir);
            AppConfig config = null;
            if (Files.exists(configFile)) {
                config = gson.fromJson(Files.readString(configFile, StandardCharsets.UTF_8), AppConfig.class);
            }
            boolean changed = false;
            if (config == null) {
                config = new AppConfig();
                changed = true;
            }
            if (!hasValidKey(config.getEncryptionKeyBase64())) {
                config.setEncryptionKeyBase64(AesGcmEncryptor.generateBase64Key());
                changed = true;
            }
            if (changed) {
                save(config);
            }
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Nije moguće učitati config fajl.", e);
        }
    }

    private boolean hasValidKey(String key) {
        if (key == null || key.isBlank() || key.contains("CHANGE_ME")) {
            return false;
        }
        try {
            new AesGcmEncryptor(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void save(AppConfig config) {
        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, gson.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Nije moguće sačuvati config fajl.", e);
        }
    }

    public Path getConfigFile() {
        return configFile;
    }
}
