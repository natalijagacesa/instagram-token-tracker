package com.example.igtracker.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uvoz lista korisnika iz fajla. Podržano:
 * - Instagram "Download your information" export (followers_1.json, following.json, ili HTML varijanta),
 * - obični TXT/CSV (jedno korisničko ime po redu).
 *
 * Ovo je i praktičan način da se dobiju liste, jer zvanični Instagram API
 * za profesionalne naloge ne vraća spisak followers/following.
 */
public class InstagramExportImporter {
    private static final Pattern HTML_PROFILE_LINK =
            Pattern.compile("href=\"https?://(?:www\\.)?instagram\\.com/(?:_u/)?([A-Za-z0-9._]+)/?\"");

    private final ComparisonService comparisonService = new ComparisonService();

    public Set<String> importFile(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        return parseContent(content, file.getFileName().toString());
    }

    public Set<String> parseContent(String content, String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        String trimmed = stripBom(content).trim();

        // Format se prepoznaje po sadržaju, jer ekstenzija zna da bude pogrešna

        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return parseJson(trimmed);
        }
        if (name.endsWith(".html") || name.endsWith(".htm") || looksLikeHtml(trimmed)) {
            return parseHtml(trimmed);
        }
        if (name.endsWith(".csv")) {
            return parseCsv(trimmed);
        }
        return comparisonService.parseUsernames(trimmed);
    }

    private String stripBom(String content) {
        return content != null && content.startsWith("\uFEFF") ? content.substring(1) : (content == null ? "" : content);
    }

    private boolean looksLikeHtml(String content) {
        String head = content.length() > 2000 ? content.substring(0, 2000) : content;
        String lower = head.toLowerCase(Locale.ROOT);
        return lower.contains("<html") || lower.contains("<!doctype html") || lower.contains("<a href=");
    }

    Set<String> parseJson(String json) {
        Set<String> result = new TreeSet<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Fajl nije ispravan JSON.", e);
        }
        walk(root, result);
        return result;
    }

    private void walk(JsonElement element, Set<String> out) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                walk(child, out);
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("string_list_data") && obj.get("string_list_data").isJsonArray()) {
                collectFromStringListData(obj, out);
            } else if (obj.has("username") && obj.get("username").isJsonPrimitive()) {
                add(out, obj.get("username").getAsString());
            }
            for (var entry : obj.entrySet()) {
                if (!"string_list_data".equals(entry.getKey())) {
                    walk(entry.getValue(), out);
                }
            }
        }
    }


    private void collectFromStringListData(JsonObject parent, Set<String> out) {
        JsonArray items = parent.getAsJsonArray("string_list_data");
        for (JsonElement item : items) {
            if (!item.isJsonObject()) {
                continue;
            }
            JsonObject o = item.getAsJsonObject();
            if (o.has("value") && o.get("value").isJsonPrimitive() && !o.get("value").getAsString().isBlank()) {
                add(out, o.get("value").getAsString());
            } else if (o.has("href") && o.get("href").isJsonPrimitive()) {
                add(out, o.get("href").getAsString());
            } else if (parent.has("title") && parent.get("title").isJsonPrimitive()) {
                add(out, parent.get("title").getAsString());
            }
        }
    }

    Set<String> parseHtml(String html) {
        Set<String> result = new TreeSet<>();
        Matcher m = HTML_PROFILE_LINK.matcher(html);
        while (m.find()) {
            add(result, m.group(1));
        }
        return result;
    }

    private Set<String> parseCsv(String csv) {
        String[] lines = csv.split("\\R", -1);
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i == 0 && lines[i].trim().equalsIgnoreCase("username")) {
                continue;
            }
            body.append(lines[i]).append('\n');
        }
        return comparisonService.parseUsernames(body.toString());
    }

    private void add(Set<String> out, String raw) {
        out.addAll(comparisonService.parseUsernames(raw));
    }
}
