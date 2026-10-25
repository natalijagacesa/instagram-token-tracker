package com.example.igtracker.service;

import com.example.igtracker.db.DatabaseManager;
import com.example.igtracker.model.InstagramSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;

public class SessionRepository {
    private final DatabaseManager databaseManager;

    public SessionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveOrUpdate(String igUserId, String username, String encryptedAccessToken, String expiresAt) {
        String now = Instant.now().toString();
        String selectSql = "SELECT id FROM instagram_sessions LIMIT 1";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement select = connection.prepareStatement(selectSql);
             ResultSet rs = select.executeQuery()) {

            if (rs.next()) {
                long id = rs.getLong("id");
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE instagram_sessions
                        SET ig_user_id=?, username=?, encrypted_access_token=?, expires_at=?, updated_at=?
                        WHERE id=?
                        """)) {
                    update.setString(1, igUserId);
                    update.setString(2, username);
                    update.setString(3, encryptedAccessToken);
                    update.setString(4, expiresAt);
                    update.setString(5, now);
                    update.setLong(6, id);
                    update.executeUpdate();
                }
            } else {
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO instagram_sessions(ig_user_id, username, encrypted_access_token, expires_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)) {
                    insert.setString(1, igUserId);
                    insert.setString(2, username);
                    insert.setString(3, encryptedAccessToken);
                    insert.setString(4, expiresAt);
                    insert.setString(5, now);
                    insert.setString(6, now);
                    insert.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Nije moguće sačuvati Instagram sesiju.", e);
        }
    }

    public InstagramSession findLatest() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM instagram_sessions ORDER BY id DESC LIMIT 1");
             ResultSet rs = statement.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            InstagramSession session = new InstagramSession();
            session.setId(rs.getLong("id"));
            session.setIgUserId(rs.getString("ig_user_id"));
            session.setUsername(rs.getString("username"));
            session.setEncryptedAccessToken(rs.getString("encrypted_access_token"));
            session.setExpiresAt(rs.getString("expires_at"));
            session.setCreatedAt(rs.getString("created_at"));
            session.setUpdatedAt(rs.getString("updated_at"));
            return session;
        } catch (Exception e) {
            throw new IllegalStateException("Nije moguće učitati Instagram sesiju.", e);
        }
    }
}
