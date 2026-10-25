package com.example.igtracker;

import com.formdev.flatlaf.FlatLightLaf;
import com.example.igtracker.db.DatabaseManager;
import com.example.igtracker.service.AppConfig;
import com.example.igtracker.service.SettingsService;
import com.example.igtracker.ui.MainFrame;

import javax.swing.SwingUtilities;

public class InstagramTrackerApp {
    public static void main(String[] args) {
        FlatLightLaf.setup();

        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initialize();

        SettingsService settingsService = new SettingsService();
        AppConfig config = settingsService.loadOrCreate();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(databaseManager, settingsService, config);
            frame.setVisible(true);
        });
    }
}
