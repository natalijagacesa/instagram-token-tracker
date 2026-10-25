package com.example.igtracker.service;

import com.example.igtracker.db.DatabaseManager;
import com.example.igtracker.model.Snapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SnapshotRepository {
    private final DatabaseManager databaseManager;

    public SnapshotRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(String snapshotName, String snapshotType, String usernamesText) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO snapshots(snapshot_name, snapshot_type, usernames_text, created_at) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, snapshotName);
            statement.setString(2, snapshotType);
            statement.setString(3, usernamesText);
            statement.setString(4, Instant.now().toString());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Nije moguće sačuvati snapshot.", e);
        }
    }

    public List<Snapshot> findAll() {
        List<Snapshot> snapshots = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM snapshots ORDER BY id DESC")) {
            while (rs.next()) {
                Snapshot snapshot = new Snapshot();
                snapshot.setId(rs.getLong("id"));
                snapshot.setSnapshotName(rs.getString("snapshot_name"));
                snapshot.setSnapshotType(rs.getString("snapshot_type"));
                snapshot.setUsernamesText(rs.getString("usernames_text"));
                snapshot.setCreatedAt(rs.getString("created_at"));
                snapshots.add(snapshot);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Nije moguće učitati snapshots.", e);
        }
        return snapshots;
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM snapshots WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Nije moguće obrisati snapshot.", e);
        }
    }
}
