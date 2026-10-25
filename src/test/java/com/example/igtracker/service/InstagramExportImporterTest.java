package com.example.igtracker.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstagramExportImporterTest {
    private final InstagramExportImporter importer = new InstagramExportImporter();

    @Test
    void parsesFollowersJsonWithValueField() {
        String json = """
                [{"title":"","media_list_data":[],"string_list_data":[
                  {"href":"https://www.instagram.com/Ana","value":"Ana","timestamp":1700000000},
                  {"href":"https://www.instagram.com/marko","value":"marko","timestamp":1700000001}]}]
                """;
        assertEquals(Set.of("ana", "marko"), importer.parseContent(json, "followers_1.json"));
    }

    @Test
    void parsesFollowingJsonWhereNameIsInTitleOrHref() {
        String json = """
                {"relationships_following":[
                  {"title":"jelena","string_list_data":[{"href":"https://www.instagram.com/_u/jelena","timestamp":1}]},
                  {"title":"petar","string_list_data":[{"timestamp":2}]}]}
                """;
        assertEquals(Set.of("jelena", "petar"), importer.parseContent(json, "following.json"));
    }

    @Test
    void parsesHtmlExport() {
        String html = "<a target=\"_blank\" href=\"https://www.instagram.com/ana\">ana</a>"
                + "<a href=\"https://www.instagram.com/_u/marko\">marko</a>";
        assertEquals(Set.of("ana", "marko"), importer.parseContent(html, "followers_1.html"));
    }

    @Test
    void parsesCsvAndSkipsHeader() {
        assertEquals(Set.of("ana", "marko"), importer.parseContent("username\nana\nmarko\n", "lista.csv"));
    }

    @Test
    void parsesPlainText() {
        assertEquals(Set.of("ana", "marko"), importer.parseContent("@ana\nmarko", "lista.txt"));
    }

    @Test
    void brokenJsonGivesClearError() {
        assertThrows(IllegalArgumentException.class, () -> importer.parseContent("{not json", "x.json"));
    }

    @Test
    void detectsJsonByContentEvenWithTxtExtensionAndBom() {
        String json = "\uFEFF[{\"string_list_data\":[{\"href\":\"https://www.instagram.com/ana\",\"value\":\"ana\",\"timestamp\":1}]},"
                + "{\"string_list_data\":[{\"href\":\"https://www.instagram.com/marko\",\"value\":\"marko\",\"timestamp\":2}]}]";
        assertEquals(Set.of("ana", "marko"), importer.parseContent(json, "followers_1.json.txt"));
    }

    @Test
    void detectsHtmlByContent() {
        String html = "<html><body><a href=\"https://www.instagram.com/ana\">ana</a></body></html>";
        assertEquals(Set.of("ana"), importer.parseContent(html, "export.txt"));
    }
}
