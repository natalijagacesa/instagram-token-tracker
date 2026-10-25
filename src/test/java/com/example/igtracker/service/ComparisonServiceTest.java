package com.example.igtracker.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComparisonServiceTest {
    private final ComparisonService service = new ComparisonService();

    @Test
    void parsesMixedSeparatorsAtSignsAndCase() {
        Set<String> parsed = service.parseUsernames("@Ana, marko;\nJELENA   @petar");
        assertEquals(Set.of("ana", "marko", "jelena", "petar"), parsed);
    }

    @Test
    void parsesProfileLinks() {
        Set<String> parsed = service.parseUsernames("https://www.instagram.com/ana.k/\nhttps://instagram.com/_u/marko?igsh=abc");
        assertEquals(Set.of("ana.k", "marko"), parsed);
    }

    @Test
    void blankInputGivesEmptySet() {
        assertTrue(service.parseUsernames("  \n ").isEmpty());
        assertTrue(service.parseUsernames(null).isEmpty());
    }

    @Test
    void differenceIntersectionUnion() {
        Set<String> a = Set.of("a", "b", "c");
        Set<String> b = Set.of("b", "c", "d");
        assertEquals(Set.of("a"), service.difference(a, b));
        assertEquals(Set.of("b", "c"), service.intersection(a, b));
        assertEquals(Set.of("a", "b", "c", "d"), service.union(a, b));
    }

    @Test
    void summaryHandlesEmptyFollowingWithoutDividingByZero() {
        String summary = service.summary(Set.of("a"), Set.of());
        assertTrue(summary.contains("0.0%"));
    }

    @Test
    void csvHasHeaderAndOneUserPerLine() {
        String csv = service.toCsv(new java.util.TreeSet<>(Set.of("b", "a")));
        assertEquals(java.util.List.of("username", "a", "b"), csv.lines().toList());
    }
}
