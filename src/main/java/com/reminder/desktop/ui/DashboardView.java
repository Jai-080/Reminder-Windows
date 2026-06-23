package com.reminder.desktop.ui;

import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.models.QuickNote;
import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.repository.MonthlyPaymentRepository;
import com.reminder.desktop.repository.QuickNoteRepository;
import com.reminder.desktop.repository.ReminderRepository;
import com.reminder.desktop.sync.SyncService;
import com.reminder.desktop.sync.WebSocketManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardView extends ScrollPane {
    private final MainLayout parentLayout;
    private final QuickNoteRepository noteRepo;
    private final ReminderRepository reminderRepo;
    private final MonthlyPaymentRepository paymentRepo;

    // Header Components
    private Label connectionBadge;
    private Label lastSyncLabel;
    private Button syncBtn;

    // Metric Labels
    private Label notesMainMetric;
    private Label notesSubMetric;
    private Label remindersMainMetric;
    private Label remindersSubMetric;
    private Label paymentsMainMetric;
    private Label paymentsSubMetric;

    // Activity Containers
    private VBox notesContainer;
    private VBox remindersContainer;
    private VBox paymentsContainer;

    // Timeline for dynamic connection updates
    private Timeline connectionPoller;

    public DashboardView(MainLayout parentLayout) {
        this.parentLayout = parentLayout;
        this.noteRepo = new QuickNoteRepository();
        this.reminderRepo = new ReminderRepository();
        this.paymentRepo = new MonthlyPaymentRepository();

        this.setFitToWidth(true);
        this.setPannable(true);

        VBox content = new VBox(24);
        content.setPadding(new Insets(32));
        content.setAlignment(Pos.TOP_LEFT);

        // --- 1. TOP HEADER SECTION ---
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerRow, Priority.ALWAYS);

        // Left Side: Title and Welcome
        String username = TokenStorage.getUsername();
        Label welcomeTitle = new Label("Dashboard");
        welcomeTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
        Label welcomeSubtitle = new Label("Welcome back, " + (username != null ? username : "User") + "! Here's your agenda.");
        welcomeSubtitle.getStyleClass().add("subtitle-label");
        VBox welcomeBox = new VBox(4, welcomeTitle, welcomeSubtitle);
        HBox.setHgrow(welcomeBox, Priority.ALWAYS);

        // Right Side: Status Badges and Sync Button
        HBox statusBox = new HBox(12);
        statusBox.setAlignment(Pos.CENTER_RIGHT);

        connectionBadge = new Label("● Online");
        connectionBadge.getStyleClass().addAll("badge-pill", "badge-online");

        lastSyncLabel = new Label("Last Synced: Loading...");
        lastSyncLabel.getStyleClass().add("subtitle-label");
        lastSyncLabel.setStyle("-fx-font-size: 11px;");

        syncBtn = new Button("Sync");
        syncBtn.getStyleClass().add("button-accent");
        syncBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 12px;");
        syncBtn.setOnAction(e -> triggerSync());

        statusBox.getChildren().addAll(connectionBadge, lastSyncLabel, syncBtn);

        headerRow.getChildren().addAll(welcomeBox, statusBox);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: -color-border; -fx-opacity: 0.5;");

        // --- 2. SUMMARY METRICS CARDS ---
        HBox summaryRow = new HBox(16);
        summaryRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(summaryRow, Priority.ALWAYS);

        // Card 1: Notes
        VBox notesCard = createMetricCard("🗒 QUICK NOTES", "Notes");
        notesMainMetric = new Label("0");
        notesMainMetric.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");
        notesSubMetric = new Label("0 incomplete");
        notesSubMetric.getStyleClass().add("subtitle-label");
        notesCard.getChildren().addAll(notesMainMetric, notesSubMetric);

        // Card 2: Reminders
        VBox remindersCard = createMetricCard("🔔 REMINDERS", "Reminders");
        remindersMainMetric = new Label("0");
        remindersMainMetric.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");
        remindersSubMetric = new Label("0 due today");
        remindersSubMetric.getStyleClass().add("subtitle-label");
        remindersCard.getChildren().addAll(remindersMainMetric, remindersSubMetric);

        // Card 3: Payments
        VBox paymentsCard = createMetricCard("💳 MONTHLY PAYMENTS", "Payments");
        paymentsMainMetric = new Label("0");
        paymentsMainMetric.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");
        paymentsSubMetric = new Label("0 overdue");
        paymentsSubMetric.getStyleClass().add("subtitle-label");
        paymentsCard.getChildren().addAll(paymentsMainMetric, paymentsSubMetric);

        // Add proportional growth
        HBox.setHgrow(notesCard, Priority.ALWAYS);
        HBox.setHgrow(remindersCard, Priority.ALWAYS);
        HBox.setHgrow(paymentsCard, Priority.ALWAYS);

        summaryRow.getChildren().addAll(notesCard, remindersCard, paymentsCard);

        // --- 3. RECENT ACTIVITY & CHECKLIST SECTION ---
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        HBox.setHgrow(grid, Priority.ALWAYS);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // Left: Quick Notes Checklist Card
        VBox notesSection = new VBox(12);
        notesSection.getStyleClass().add("card");
        HBox notesHeader = createSectionHeader("Recent Checklist", "Notes");
        notesContainer = new VBox(6);
        notesSection.getChildren().addAll(notesHeader, notesContainer);
        grid.add(notesSection, 0, 0);

        // Right: Agenda (Reminders & Payments) Card
        VBox agendaSection = new VBox(16);
        agendaSection.getStyleClass().add("card");
        
        Label agendaTitle = new Label("Upcoming Agenda");
        agendaTitle.getStyleClass().add("section-header");
        agendaTitle.setStyle("-fx-font-size: 15px;");

        VBox remindersSubBox = new VBox(8);
        Label remTitle = new Label("Reminders");
        remTitle.getStyleClass().add("section-header");
        remTitle.setStyle("-fx-font-size: 12px;");
        remindersContainer = new VBox(6);
        remindersSubBox.getChildren().addAll(remTitle, remindersContainer);

        VBox paymentsSubBox = new VBox(8);
        Label payTitle = new Label("Payments");
        payTitle.getStyleClass().add("section-header");
        payTitle.setStyle("-fx-font-size: 12px;");
        paymentsContainer = new VBox(6);
        paymentsSubBox.getChildren().addAll(payTitle, paymentsContainer);

        agendaSection.getChildren().addAll(agendaTitle, remindersSubBox, new Separator(), paymentsSubBox);
        grid.add(agendaSection, 1, 0);

        content.getChildren().addAll(headerRow, sep, summaryRow, grid);
        this.setContent(content);

        // Connection status scheduler (Runs every 3s)
        connectionPoller = new Timeline(new KeyFrame(Duration.seconds(3), e -> updateConnectionStatus()));
        connectionPoller.setCycleCount(Timeline.INDEFINITE);
        connectionPoller.play();

        // Initial Load
        refreshData();
        updateConnectionStatus();

        // Sync finished listener setup
        Runnable syncListener = () -> {
            System.out.println("DashboardView: Sync completed, refreshing dashboard.");
            refreshData();
        };

        this.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                com.reminder.desktop.sync.SyncService.addSyncFinishedListener(syncListener);
                connectionPoller.play();
                refreshData();
            } else {
                com.reminder.desktop.sync.SyncService.removeSyncFinishedListener(syncListener);
                connectionPoller.stop();
            }
        });

        if (this.getScene() != null) {
            com.reminder.desktop.sync.SyncService.addSyncFinishedListener(syncListener);
        }
    }

    private VBox createMetricCard(String title, String viewName) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> parentLayout.showView(viewName));

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("section-header");
        titleLbl.setStyle("-fx-font-size: 11px;");
        card.getChildren().add(titleLbl);
        return card;
    }

    private HBox createSectionHeader(String title, String viewName) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("section-header");
        titleLbl.setStyle("-fx-font-size: 15px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button manageBtn = new Button("Manage");
        manageBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        manageBtn.setOnAction(e -> parentLayout.showView(viewName));

        header.getChildren().addAll(titleLbl, spacer, manageBtn);
        return header;
    }

    private void updateConnectionStatus() {
        boolean connected = WebSocketManager.getInstance().isConnected();
        Platform.runLater(() -> {
            if (connected) {
                connectionBadge.setText("● Online");
                connectionBadge.getStyleClass().removeAll("badge-offline");
                if (!connectionBadge.getStyleClass().contains("badge-online")) {
                    connectionBadge.getStyleClass().add("badge-online");
                }
            } else {
                connectionBadge.setText("○ Offline");
                connectionBadge.getStyleClass().removeAll("badge-online");
                if (!connectionBadge.getStyleClass().contains("badge-offline")) {
                    connectionBadge.getStyleClass().add("badge-offline");
                }
            }
        });
    }

    private void refreshData() {
        new Thread(() -> {
            try {
                // Fetch Notes Metrics
                List<QuickNote> allNotes = noteRepo.getAllNotes();
                long totalNotes = allNotes.size();
                List<QuickNote> incompleteNotes = allNotes.stream()
                        .filter(n -> !n.isCompleted())
                        .collect(Collectors.toList());
                long incompleteNotesCount = incompleteNotes.size();
                List<QuickNote> dashboardChecklist = incompleteNotes.stream()
                        .limit(3)
                        .collect(Collectors.toList());

                // Fetch Reminders Metrics
                List<Reminder> pendingReminders = reminderRepo.getPendingReminders();
                List<Reminder> expiredReminders = reminderRepo.getExpiredReminders();
                long activeReminders = pendingReminders.size();
                long expiredRemindersCount = expiredReminders.size();
                List<Reminder> dashboardReminders = pendingReminders.stream()
                        .limit(3)
                        .collect(Collectors.toList());

                LocalDate today = LocalDate.now();
                long startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long endOfToday = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

                long dueTodayReminders = pendingReminders.stream()
                        .filter(r -> r.getTime() >= startOfToday && r.getTime() < endOfToday)
                        .count();

                // Fetch Payments Metrics
                List<MonthlyPayment> allPaymentsList = paymentRepo.getAllPayments();
                List<MonthlyPayment> pendingPayments = allPaymentsList.stream()
                        .filter(p -> !p.isCompleted())
                        .collect(Collectors.toList());
                long activePayments = pendingPayments.size();
                List<MonthlyPayment> dashboardPayments = pendingPayments.stream()
                        .limit(3)
                        .collect(Collectors.toList());

                long nowMs = System.currentTimeMillis();
                long overduePayments = pendingPayments.stream()
                        .filter(p -> p.getDueDate() < nowMs)
                        .count();

                // Compute payments due this calendar month
                int currentMonth = today.getMonthValue();
                int currentYear = today.getYear();

                long dueThisMonthCount = pendingPayments.stream()
                        .filter(p -> {
                            LocalDate dueDate = java.time.Instant.ofEpochMilli(p.getDueDate())
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate();
                            return dueDate.getMonthValue() == currentMonth && dueDate.getYear() == currentYear;
                        })
                        .count();

                double pendingAmountThisMonth = pendingPayments.stream()
                        .filter(p -> {
                            LocalDate dueDate = java.time.Instant.ofEpochMilli(p.getDueDate())
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate();
                            return dueDate.getMonthValue() == currentMonth && dueDate.getYear() == currentYear;
                        })
                        .filter(p -> p.getAmount() != null)
                        .mapToDouble(MonthlyPayment::getAmount)
                        .sum();

                long lastSync = TokenStorage.getLastSyncTimestamp();

                // Update UI elements
                Platform.runLater(() -> {
                    // Update connection status
                    updateConnectionStatus();

                    // Summary Metrics counts
                    notesMainMetric.setText(String.valueOf(totalNotes));
                    notesSubMetric.setText(incompleteNotesCount + " incomplete");

                    remindersMainMetric.setText(String.valueOf(activeReminders));
                    remindersSubMetric.setText(activeReminders + " pending | " + expiredRemindersCount + " expired");

                    paymentsMainMetric.setText(String.valueOf(activePayments));
                    paymentsSubMetric.setText(overduePayments + " overdue | " + dueThisMonthCount + " due this month (Total: ₹" + String.format("%.2f", pendingAmountThisMonth) + ")");

                    // 1. Checklist Items
                    notesContainer.getChildren().clear();
                    if (dashboardChecklist.isEmpty()) {
                        VBox emptyBox = new VBox(6);
                        emptyBox.setAlignment(Pos.CENTER);
                        emptyBox.setPadding(new Insets(12));
                        Label icon = new Label("🗒");
                        icon.setStyle("-fx-font-size: 20px; -fx-opacity: 0.5;");
                        Label emptyLbl = new Label("Checklist empty");
                        emptyLbl.getStyleClass().add("subtitle-label");
                        emptyBox.getChildren().addAll(icon, emptyLbl);
                        notesContainer.getChildren().add(emptyBox);
                    } else {
                        for (QuickNote note : dashboardChecklist) {
                            HBox row = new HBox(8);
                            row.getStyleClass().add("row-card-item");
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.setPadding(new Insets(8, 12, 8, 12));

                            CheckBox cb = new CheckBox();
                            cb.setSelected(false);
                            cb.setOnAction(e -> {
                                note.setCompleted(cb.isSelected());
                                new Thread(() -> {
                                    try {
                                        noteRepo.updateNote(note);
                                        Platform.runLater(this::refreshData);
                                    } catch (Exception ex) {
                                        System.err.println("Error completing note from dashboard: " + ex.getMessage());
                                    }
                                }).start();
                            });

                            Label text = new Label(note.getText());
                            text.setWrapText(true);
                            HBox.setHgrow(text, Priority.ALWAYS);

                            row.getChildren().addAll(cb, text);
                            notesContainer.getChildren().add(row);
                        }
                    }

                    // 2. Upcoming Reminders List
                    remindersContainer.getChildren().clear();
                    if (dashboardReminders.isEmpty()) {
                        Label lbl = new Label("No upcoming reminders");
                        lbl.getStyleClass().add("subtitle-label");
                        lbl.setStyle("-fx-font-size: 11px;");
                        remindersContainer.getChildren().add(lbl);
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a");
                        for (Reminder reminder : dashboardReminders) {
                            Label lbl = new Label("• " + reminder.getText() + " (" + sdf.format(new Date(reminder.getTime())) + ")");
                            lbl.setStyle("-fx-font-size: 12px;");
                            remindersContainer.getChildren().add(lbl);
                        }
                    }

                    // 3. Upcoming Payments List
                    paymentsContainer.getChildren().clear();
                    if (dashboardPayments.isEmpty()) {
                        Label lbl = new Label("No upcoming payments");
                        lbl.getStyleClass().add("subtitle-label");
                        lbl.setStyle("-fx-font-size: 11px;");
                        paymentsContainer.getChildren().add(lbl);
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
                        for (MonthlyPayment p : dashboardPayments) {
                            String amtStr = p.getAmount() == null ? "Amount Unknown" : String.format("₹%.2f", p.getAmount());
                            Label lbl = new Label("• " + p.getName() + " (" + amtStr + ") - Due: " + sdf.format(new Date(p.getDueDate())));
                            lbl.setStyle("-fx-font-size: 12px;");
                            paymentsContainer.getChildren().add(lbl);
                        }
                    }

                    // Sync Metadata timestamp
                    if (lastSync == 0) {
                        lastSyncLabel.setText("Last Synced: Never");
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
                        lastSyncLabel.setText("Last Synced: " + sdf.format(new Date(lastSync)));
                    }
                });

            } catch (Exception e) {
                System.err.println("Error loading dashboard metrics: " + e.getMessage());
            }
        }, "DashboardRefreshThread").start();
    }

    private void triggerSync() {
        syncBtn.setDisable(true);
        syncBtn.setText("Syncing...");

        SyncService.getInstance().triggerSyncAsync(new SyncService.SyncListener() {
            @Override
            public void onSyncStarted() {}

            @Override
            public void onSyncFinished(boolean success, String message) {
                Platform.runLater(() -> {
                    syncBtn.setDisable(false);
                    syncBtn.setText("Sync");
                    refreshData();
                });
            }
        });
    }
}
