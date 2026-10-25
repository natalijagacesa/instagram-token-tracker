package com.example.igtracker.service;

import com.example.igtracker.db.DatabaseManager;
import com.example.igtracker.model.InstagramSession;
import com.example.igtracker.model.Snapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryAndSettingsTest {
    @TempDir
    Path tmp;

    @Test
    void snapshotsAreSavedListedAndDeleted() {
        DatabaseManager db = new DatabaseManager(tmp);
        db.initialize();
        SnapshotRepository repo = new SnapshotRepository(db);

        repo.save("followers_1", "followers", "ana\nmarko");
        repo.save("following_1", "following", "jelena");
        List<Snapshot> all = repo.findAll();
        assertEquals(2, all.size());
        assertEquals("following_1", all.get(0).getSnapshotName()); // najnoviji prvi

        repo.delete(all.get(0).getId());
        assertEquals(1, repo.findAll().size());
    }

    @Test
    void sessionIsUpsertedNotDuplicated() {
        DatabaseManager db = new DatabaseManager(tmp);
        db.initialize();
        SessionRepository repo = new SessionRepository(db);
        assertNull(repo.findLatest());

        repo.saveOrUpdate("1", "ana", "enc1", "2030-01-01T00:00:00Z");
        repo.saveOrUpdate("1", "ana2", "enc2", "2031-01-01T00:00:00Z");
        InstagramSession s = repo.findLatest();
        assertEquals("ana2", s.getUsername());
        assertEquals("enc2", s.getEncryptedAccessToken());
    }

    @Test
    void firstRunGeneratesWorkingAesKeyAndKeepsItOnNextRun() {
        SettingsService settings = new SettingsService(tmp);
        AppConfig first = settings.loadOrCreate();
        String key = first.getEncryptionKeyBase64();
        assertFalse(key.contains("CHANGE_ME"));
        assertTrue(settings.getConfigFile().toFile().exists());
        assertEquals("instagram_business_basic", first.getScopes());

        AppConfig second = new SettingsService(tmp).loadOrCreate();
        assertEquals(key, second.getEncryptionKeyBase64());
        assertNotNull(second.getRedirectUri());
    }
}
