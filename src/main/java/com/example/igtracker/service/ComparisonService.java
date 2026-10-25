package com.example.igtracker.service;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ComparisonService {

    /**
     * Parsira korisnička imena iz proizvoljnog teksta (po jedno u redu, zarezi, razmaci...).
     * Podržava "@ime" i linkove tipa https://www.instagram.com/ime/
     */
    public Set<String> parseUsernames(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new TreeSet<>();
        }
        return Stream.of(rawText.split("\\R|,|;|\\s+"))
                .map(String::trim)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    String normalize(String token) {
        String s = token.trim().toLowerCase();
        if (s.startsWith("http://") || s.startsWith("https://")) {
            int q = s.indexOf('?');
            if (q >= 0) {
                s = s.substring(0, q);
            }
            while (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }
            s = s.substring(s.lastIndexOf('/') + 1);
        }
        return s.startsWith("@") ? s.substring(1) : s;
    }

    public Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new TreeSet<>(left);
        result.removeAll(right);
        return result;
    }

    public Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> result = new TreeSet<>(left);
        result.retainAll(right);
        return result;
    }

    public Set<String> union(Set<String> left, Set<String> right) {
        Set<String> result = new TreeSet<>(left);
        result.addAll(right);
        return result;
    }

    public String format(Set<String> usernames) {
        return usernames.isEmpty() ? "Nema rezultata." : String.join(System.lineSeparator(), usernames);
    }

    public String toCsv(Set<String> usernames) {
        StringBuilder sb = new StringBuilder("username").append(System.lineSeparator());
        usernames.forEach(u -> sb.append(u).append(System.lineSeparator()));
        return sb.toString();
    }

    /** Kratak pregled odnosa followers/following. Procenat je 0 kad je lista prazna. */
    public String summary(Set<String> followers, Set<String> following) {
        Set<String> mutuals = intersection(followers, following);
        Set<String> notBack = difference(following, followers);
        Set<String> fans = difference(followers, following);
        double ratio = following.isEmpty() ? 0 : 100.0 * mutuals.size() / following.size();
        return String.format(Locale.ROOT,
                "Followers: %d%nFollowing: %d%nMutuals: %d%nKoje ti pratiš a oni tebe ne: %d%nKoji tebe prate a ti njih ne: %d%nProcenat uzvraćenih (od following): %.1f%%",
                followers.size(), following.size(), mutuals.size(), notBack.size(), fans.size(), ratio);
    }
}
