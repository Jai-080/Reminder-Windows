package com.reminder.desktop.ui;

import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.models.QuickNote;
import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.repository.MonthlyPaymentRepository;
import com.reminder.desktop.repository.QuickNoteRepository;
import com.reminder.desktop.repository.ReminderRepository;
import com.reminder.desktop.sync.SyncService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardView extends ScrollPane {
    private final MainLayout parentLayout;
    private final QuickNoteRepository noteRepo;
    private final ReminderRepository reminderRepo;
    private final MonthlyPaymentRepository paymentRepo;
    
    private Label lastSyncLabel;
    private Label pendingLabel;
    private Button syncBtn;
    private VBox remindersContainer;
    private VBox paymentsContainer;
    private VBox notesContainer;

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

        // Welcome Header
        String username = TokenStorage.getUsername();
        Label welcomeTitle = new Label("Hello, " + (username != null ? username : "User") + "!");
        welcomeTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        Label welcomeSubtitle = new Label("Here's what is on your agenda today.");
        welcomeSubtitle.getStyleClass().add("subtitle-label");
        VBox welcomeBox = new VBox(4, welcomeTitle, welcomeSubtitle);

        // Setup Main Grid (Responsive HBox/GridPane layout)
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // 1. Reminders Card
        VBox remindersCard = createSectionCard("Upcoming Reminders", "Reminders");
        remindersContainer = new VBox(10);
        remindersCard.getChildren().add(remindersContainer);
        grid.add(remindersCard, 0, 0);

        // 2. Recent Notes Card
        VBox notesCard = createSectionCard("Recent Notes", "Notes");
        notesContainer = new VBox(10);
        notesCard.getChildren().add(notesContainer);
        grid.add(notesCard, 1, 0);

        // 3. Payments Card
        VBox paymentsCard = createSectionCard("Upcoming Payments", "Payments");
        paymentsContainer = new VBox(10);
        paymentsCard.getChildren().add(paymentsContainer);
        grid.add(paymentsCard, 0, 1);

        // 4. Sync Status Card
        VBox syncCard = new VBox(14);
        syncCard.getStyleClass().add("card");
        Label syncTitle = new Label("Synchronization Status");
        syncTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        lastSyncLabel = new Label("Last Synced: Loading...");
        pendingLabel = new Label("Pending Changes: Checking...");
        
        syncBtn = new Button("Sync Now");
        syncBtn.getStyleClass().add("button-accent");
        syncBtn.setMaxWidth(Double.MAX_VALUE);
        syncBtn.setOnAction(e -> triggerSync());

        syncCard.getChildren().addAll(syncTitle, lastSyncLabel, pendingLabel, syncBtn);
        grid.add(syncCard, 1, 1);

        content.getChildren().addAll(welcomeBox, grid);
        this.setContent(content);

        // Populate details
        refreshData();
    }

    private VBox createSectionCard(String title, String viewName) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button viewAllBtn = new Button("Manage");
        viewAllBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        viewAllBtn.setOnAction(e -> parentLayout.showView(viewName));

        header.getChildren().addAll(titleLbl, spacer, viewAllBtn);
        card.getChildren().add(header);
        return card;
    }

    private void refreshData() {
        new Thread(() -> {
            try {
                // Fetch data in background thread
                List<QuickNote> incompleteNotes = noteRepo.getAllNotes().stream()
                        .filter(n -> !n.isCompleted())
                        .limit(3)
                        .collect(Collectors.toList());

                List<Reminder> pendingReminders = reminderRepo.getPendingReminders().stream()
                        .limit(3)
                        .collect(Collectors.toList());

                List<MonthlyPayment> pendingPayments = paymentRepo.getAllPayments().stream()
                        .filter(p -> !p.isCompleted())
                        .limit(3)
                        .collect(Collectors.toList());

                long notesPending = noteRepo.getAllNotes().stream().filter(n -> "PENDING".equalsIgnoreCase(n.getSyncStatus())).count();
                long remindersPending = reminderRepo.getAllReminders().stream().filter(r -> "PENDING".equalsIgnoreCase(r.getSyncStatus())).count();
                long paymentsPending = paymentRepo.getAllPayments().stream().filter(p -> "PENDING".equalsIgnoreCase(p.getSyncStatus())).count();
                long pendingCount = notesPending + remindersPending + paymentsPending;

                long lastSync = TokenStorage.getLastSyncTimestamp();

                // Update UI on JavaFX application thread
                Platform.runLater(() -> {
                    // Update Notes
                    notesContainer.getChildren().clear();
                    if (incompleteNotes.isEmpty()) {
                        notesContainer.getChildren().add(new Label("No recent notes."));
                    } else {
                        for (QuickNote note : incompleteNotes) {
                            Label lbl = new Label("• " + note.getText());
                            notesContainer.getChildren().add(lbl);
                        }
                    }

                    // Update Reminders
                    remindersContainer.getChildren().clear();
                    if (pendingReminders.isEmpty()) {
                        remindersContainer.getChildren().add(new Label("No upcoming reminders."));
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a");
                        for (Reminder reminder : pendingReminders) {
                            Label lbl = new Label("• " + reminder.getText() + " (" + sdf.format(new Date(reminder.getTime())) + ")");
                            remindersContainer.getChildren().add(lbl);
                        }
                    }

                    // Update Payments
                    paymentsContainer.getChildren().clear();
                    if (pendingPayments.isEmpty()) {
                        paymentsContainer.getChildren().add(new Label("No upcoming payments."));
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
                        for (MonthlyPayment p : pendingPayments) {
                            Label lbl = new Label("• " + p.getName() + " (Due: " + sdf.format(new Date(p.getDueDate())) + ")");
                            paymentsContainer.getChildren().add(lbl);
                        }
                    }

                    // Update Sync Metadata
                    if (lastSync == 0) {
                        lastSyncLabel.setText("Last Synced: Never");
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
                        lastSyncLabel.setText("Last Synced: " + sdf.format(new Date(lastSync)));
                    }

                    pendingLabel.setText("Pending Changes to Upload: " + pendingCount);
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
                    syncBtn.setText("Sync Now");
                    refreshData();
                });
            }
        });
    }
}
