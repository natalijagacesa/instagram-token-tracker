package com.example.igtracker.ui;

import com.example.igtracker.auth.InstagramOAuthService;
import com.example.igtracker.crypto.AesGcmEncryptor;
import com.example.igtracker.db.DatabaseManager;
import com.example.igtracker.model.InstagramSession;
import com.example.igtracker.model.Snapshot;
import com.example.igtracker.service.AppConfig;
import com.example.igtracker.service.ComparisonService;
import com.example.igtracker.service.InstagramExportImporter;
import com.example.igtracker.service.SessionRepository;
import com.example.igtracker.service.SettingsService;
import com.example.igtracker.service.SnapshotRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

public class MainFrame extends JFrame {
    private final DatabaseManager databaseManager;
    private final SettingsService settingsService;
    private AppConfig config;
    private final SessionRepository sessionRepository;
    private final SnapshotRepository snapshotRepository;
    private final ComparisonService comparisonService = new ComparisonService();
    private final InstagramOAuthService oAuthService = new InstagramOAuthService();
    private final InstagramExportImporter importer = new InstagramExportImporter();
    private final JButton loginButton = new JButton("Log in with Instagram");
    private Set<String> lastResult = Set.of();

    private final JLabel sessionLabel = new JLabel("Nema prijavljenog Instagram naloga.");
    private final JTextArea followersArea = new JTextArea();
    private final JTextArea followingArea = new JTextArea();
    private final JTextArea oldFollowersArea = new JTextArea();
    private final JTextArea resultArea = new JTextArea();
    private final DefaultListModel<Snapshot> snapshotListModel = new DefaultListModel<>();
    private final JList<Snapshot> snapshotList = new JList<>(snapshotListModel);

    public MainFrame(DatabaseManager databaseManager, SettingsService settingsService, AppConfig config) {
        this.databaseManager = databaseManager;
        this.settingsService = settingsService;
        this.config = config;
        this.sessionRepository = new SessionRepository(databaseManager);
        this.snapshotRepository = new SnapshotRepository(databaseManager);

        setTitle("Instagram Token Tracker");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);

        initUi();
        refreshSessionStatus();
        refreshSnapshots();
    }

    private void initUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        root.add(buildTopPanel(), BorderLayout.NORTH);
        root.add(buildCenterPanel(), BorderLayout.CENTER);
        root.add(buildRightPanel(), BorderLayout.EAST);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createTitledBorder("Instagram prijava i bezbednost"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton settingsButton = new JButton("Podesi Meta app podatke");
        settingsButton.addActionListener(e -> openSettingsDialog());

        loginButton.addActionListener(e -> loginWithInstagram());

        JButton showTokenButton = new JButton("Prikaži dekriptovan token");
        showTokenButton.addActionListener(e -> showDecryptedToken());

        buttonPanel.add(settingsButton);
        buttonPanel.add(loginButton);
        buttonPanel.add(showTokenButton);

        sessionLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        panel.add(sessionLabel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        return panel;
    }

    private JSplitPane buildCenterPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.72);

        JPanel left = new JPanel(new GridLayout(2, 2, 12, 12));
        left.add(wrapTextArea("Followers (trenutni unos)", followersArea));
        left.add(wrapTextArea("Following (trenutni unos)", followingArea));
        left.add(wrapTextArea("Stari followers snapshot", oldFollowersArea));
        left.add(wrapTextArea("Rezultat analize", resultArea));

        JPanel actions = new JPanel(new BorderLayout(8, 8));
        actions.setBorder(BorderFactory.createTitledBorder("Analiza"));
        JPanel buttons = new JPanel(new GridLayout(0, 1, 6, 6));

        buttons.add(button("Uvezi followers iz fajla", e -> importInto(followersArea)));
        buttons.add(button("Uvezi following iz fajla", e -> importInto(followingArea)));
        buttons.add(button("Uvezi stari followers iz fajla", e -> importInto(oldFollowersArea)));
        buttons.add(button("Ko te ne prati nazad", e -> showNotFollowingBack()));
        buttons.add(button("Koga ti ne pratiš nazad", e -> showNotFollowedBackByYou()));
        buttons.add(button("Mutuals", e -> showMutuals()));
        buttons.add(button("Ko te je otpratio", e -> showUnfollowers()));
        buttons.add(button("Novi pratioci", e -> showNewFollowers()));
        buttons.add(button("Statistika", e -> showStats()));
        buttons.add(button("Sačuvaj followers snapshot", e -> saveSnapshot("followers", followersArea.getText())));
        buttons.add(button("Sačuvaj following snapshot", e -> saveSnapshot("following", followingArea.getText())));
        buttons.add(button("Izvezi rezultat (CSV)", e -> exportResult()));
        buttons.add(button("Očisti sva polja", e -> clearAll()));

        actions.add(new JScrollPane(buttons), BorderLayout.CENTER);
        splitPane.setLeftComponent(left);
        splitPane.setRightComponent(actions);
        return splitPane;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setPreferredSize(new Dimension(290, 100));
        panel.setBorder(BorderFactory.createTitledBorder("Sačuvani snapshots"));

        snapshotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snapshotList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Snapshot selected = snapshotList.getSelectedValue();
                if (selected != null) {
                    if ("followers".equalsIgnoreCase(selected.getSnapshotType())) {
                        oldFollowersArea.setText(selected.getUsernamesText());
                    } else {
                        followingArea.setText(selected.getUsernamesText());
                    }
                }
            }
        });

        JPanel south = new JPanel(new GridLayout(1, 0, 6, 6));
        south.add(button("Osveži listu", e -> refreshSnapshots()));
        south.add(button("Obriši", e -> deleteSelectedSnapshot()));

        panel.add(new JScrollPane(snapshotList), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel wrapTextArea(String title, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private void openSettingsDialog() {
        JTextField clientIdField = new JTextField(config.getInstagramClientId());
        JPasswordField secretField = new JPasswordField(config.getInstagramClientSecret());
        JTextField redirectUriField = new JTextField(config.getRedirectUri());
        JTextField scopesField = new JTextField(config.getScopes());
        JTextField encryptionKeyField = new JTextField(config.getEncryptionKeyBase64());

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(new JLabel("Instagram Client ID"));
        form.add(clientIdField);
        form.add(new JLabel("Instagram Client Secret"));
        form.add(secretField);
        form.add(new JLabel("Redirect URI"));
        form.add(redirectUriField);
        form.add(new JLabel("Scopes"));
        form.add(scopesField);
        form.add(new JLabel("AES ključ u Base64 (32 bytes)"));
        form.add(encryptionKeyField);

        int result = JOptionPane.showConfirmDialog(this, form, "Podešavanja", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            config.setInstagramClientId(clientIdField.getText().trim());
            config.setInstagramClientSecret(new String(secretField.getPassword()));
            config.setRedirectUri(redirectUriField.getText().trim());
            config.setScopes(scopesField.getText().trim());
            config.setEncryptionKeyBase64(encryptionKeyField.getText().trim());
            settingsService.save(config);
            JOptionPane.showMessageDialog(this,
                    "Podešavanja su sačuvana.\nConfig fajl: " + settingsService.getConfigFile(),
                    "Uspeh",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void loginWithInstagram() {
        if (config.getInstagramClientId().contains("YOUR_") || config.getInstagramClientSecret().contains("YOUR_")) {
            JOptionPane.showMessageDialog(this,
                    "Prvo unesi svoj Meta/Instagram Client ID i Client Secret u podešavanjima.",
                    "Nedostaju podaci",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (config.getEncryptionKeyBase64().contains("CHANGE_ME")) {
            JOptionPane.showMessageDialog(this,
                    "Prvo generiši i unesi pravi AES ključ u podešavanjima.",
                    "Nedostaje AES ključ",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        sessionLabel.setText("Otvaram Instagram prijavu u browseru... (ističe za 3 min)");
        loginButton.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                InstagramOAuthService.AuthResult result = oAuthService.login(config).join();
                AesGcmEncryptor encryptor = new AesGcmEncryptor(config.getEncryptionKeyBase64());
                String encryptedToken = encryptor.encrypt(result.accessToken());
                sessionRepository.saveOrUpdate(result.igUserId(), result.username(), encryptedToken, result.expiresAt());
                return null;
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    get();
                    refreshSessionStatus();
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Instagram prijava je uspešna i token je sačuvan enkriptovan u SQLite bazi.",
                            "Uspeh",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    sessionLabel.setText("Prijava nije uspela.");
                    JOptionPane.showMessageDialog(MainFrame.this,
                            friendlyMessage(ex),
                            "Greška",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showDecryptedToken() {
        try {
            InstagramSession session = sessionRepository.findLatest();
            if (session == null) {
                JOptionPane.showMessageDialog(this, "Nema sačuvanog tokena.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Access token daje pristup tvom nalogu. Prikazati ga?",
                    "Potvrda", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            AesGcmEncryptor encryptor = new AesGcmEncryptor(config.getEncryptionKeyBase64());
            String token = encryptor.decrypt(session.getEncryptedAccessToken());
            JTextArea area = new JTextArea(token);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Dekriptovan access token", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Set<String> followers() {
        return comparisonService.parseUsernames(followersArea.getText());
    }

    private Set<String> following() {
        return comparisonService.parseUsernames(followingArea.getText());
    }

    private void showResult(Set<String> result) {
        lastResult = result;
        resultArea.setText(comparisonService.format(result));
    }

    private void showNotFollowingBack() {
        showResult(comparisonService.difference(following(), followers()));
    }

    private void showNotFollowedBackByYou() {
        showResult(comparisonService.difference(followers(), following()));
    }

    private void showMutuals() {
        showResult(comparisonService.intersection(followers(), following()));
    }

    private void showUnfollowers() {
        Set<String> oldFollowers = comparisonService.parseUsernames(oldFollowersArea.getText());
        if (oldFollowers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Prvo učitaj stari followers snapshot (desno) ili uvezi stari fajl.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showResult(comparisonService.difference(oldFollowers, followers()));
    }

    private void showNewFollowers() {
        Set<String> oldFollowers = comparisonService.parseUsernames(oldFollowersArea.getText());
        if (oldFollowers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Prvo učitaj stari followers snapshot (desno) ili uvezi stari fajl.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showResult(comparisonService.difference(followers(), oldFollowers));
    }

    private void showStats() {
        lastResult = Set.of();
        resultArea.setText(comparisonService.summary(followers(), following()));
    }

    private void importInto(JTextArea target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Izaberi fajl (Instagram export .json/.html ili .txt/.csv)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            Set<String> users = importer.importFile(file.toPath());
            if (users.isEmpty()) {
                JOptionPane.showMessageDialog(this, "U fajlu nisu pronađena korisnička imena.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            target.setText(String.join(System.lineSeparator(), users));
            resultArea.setText("Uvezeno " + users.size() + " korisnika iz fajla " + file.getName() + ".");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, friendlyMessage(e), "Greška pri uvozu", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportResult() {
        if (lastResult.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nema rezultata za izvoz. Prvo pokreni neku analizu.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("rezultat.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.writeString(chooser.getSelectedFile().toPath(), comparisonService.toCsv(lastResult), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Rezultat je sačuvan (" + lastResult.size() + " korisnika).",
                    "Uspeh", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, friendlyMessage(e), "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAll() {
        followersArea.setText("");
        followingArea.setText("");
        oldFollowersArea.setText("");
        resultArea.setText("");
        lastResult = Set.of();
    }

    private void deleteSelectedSnapshot() {
        Snapshot selected = snapshotList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Izaberi snapshot iz liste.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Obrisati snapshot \"" + selected.getSnapshotName() + "\"?",
                "Potvrda", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            snapshotRepository.delete(selected.getId());
            refreshSnapshots();
        }
    }

    private JButton button(String text, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        b.addActionListener(listener);
        return b;
    }

    /** Vraća razumljivu poruku umesto "java.util.concurrent.CompletionException: ...". */
    private String friendlyMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            if (root instanceof TimeoutException) {
                break;
            }
            root = root.getCause();
        }
        if (root instanceof TimeoutException) {
            return "Isteklo je vreme za Instagram prijavu (3 minuta). Pokušaj ponovo.";
        }
        return root.getMessage() == null ? root.toString() : root.getMessage();
    }

    private void saveSnapshot(String type, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            JOptionPane.showMessageDialog(this, "Nema podataka za snapshot.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name = type + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        snapshotRepository.save(name, type, rawText);
        refreshSnapshots();
        JOptionPane.showMessageDialog(this, "Snapshot je sačuvan.", "Uspeh", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshSessionStatus() {
        InstagramSession session = sessionRepository.findLatest();
        Path dbPath = databaseManager.getDatabaseFile();
        if (session == null) {
            sessionLabel.setText("Nema prijavljenog Instagram naloga. Baza: " + dbPath);
        } else {
            sessionLabel.setText("Prijavljen nalog: @" + session.getUsername() + " | Token enkriptovan u bazi | Expires: " + session.getExpiresAt());
        }
    }

    private void refreshSnapshots() {
        snapshotListModel.clear();
        List<Snapshot> snapshots = snapshotRepository.findAll();
        for (Snapshot snapshot : snapshots) {
            snapshotListModel.addElement(snapshot);
        }
    }
}
