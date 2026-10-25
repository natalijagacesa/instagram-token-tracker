package com.example.igtracker.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Path DEFAULT_DIR = Paths.get(System.getProperty("user.home"), ".ig-token-tracker");

    private final Path dbDir;
    private final Path dbFile;

    public DatabaseManager() {
        this(DEFAULT_DIR);
    }

    public DatabaseManager(Path dbDir) {
        this.dbDir = dbDir;
        this.dbFile = dbDir.resolve("tracker.db");
    }

    public void initialize() {
        try {
            Files.createDirectories(dbDir);
        } catch (Exception e) {
            throw new IllegalStateException("Nije moguće kreirati folder za bazu.", e);
        }

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS instagram_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        ig_user_id TEXT,
                        username TEXT,
                        encrypted_access_token TEXT NOT NULL,
                        expires_at TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        snapshot_name TEXT NOT NULL,
                        snapshot_type TEXT NOT NULL,
                        usernames_text TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Nije moguće inicijalizovati bazu.", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile);
    }

    public Path getDatabaseFile() {
        return dbFile;
    }
}
