package com.reminder.desktop.ui;

import com.reminder.desktop.models.QuickNote;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.application.Platform;
import com.reminder.desktop.repository.QuickNoteRepository;

public class NotesView extends ScrollPane {
    private final NotesController controller;
    private final VBox listContainer;
    private List<QuickNote> currentNotesList;

    public NotesView() {
        this.controller = new NotesController();
        this.setFitToWidth(true);
        this.setPannable(true);

        VBox content = new VBox(20);
        content.setPadding(new Insets(32));
        content.setAlignment(Pos.TOP_LEFT);

        // Header
        Label title = new Label("Quick Notes");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label subtitle = new Label("Write down quick tasks. Drag-and-drop position order mirrors Android checklist.");
        subtitle.getStyleClass().add("subtitle-label");
        VBox headerBox = new VBox(4, title, subtitle);

        // Add Input Row
        HBox inputRow = new HBox(12);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        
        TextField noteInput = new TextField();
        noteInput.setPromptText("Add a new note...");
        HBox.setHgrow(noteInput, Priority.ALWAYS);

        Button addBtn = new Button("Add Note");
        addBtn.getStyleClass().add("button-accent");
        
        inputRow.getChildren().addAll(noteInput, addBtn);

        // Note List Box
        listContainer = new VBox(12);
        listContainer.setAlignment(Pos.TOP_LEFT);

        content.getChildren().addAll(headerBox, inputRow, listContainer);
        this.setContent(content);

        // Input Actions
        addBtn.setOnAction(e -> {
            controller.addNote(noteInput.getText(), this);
            noteInput.clear();
        });
        noteInput.setOnAction(e -> {
            controller.addNote(noteInput.getText(), this);
            noteInput.clear();
        });

        // Initial Load
        controller.loadNotes(this);

        // Dynamic sync finished listener registration
        Runnable syncListener = () -> {
            System.out.println("NotesView: Sync completed, reloading notes.");
            controller.loadNotes(this);
        };

        this.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                com.reminder.desktop.sync.SyncService.addSyncFinishedListener(syncListener);
                controller.loadNotes(this);
            } else {
                com.reminder.desktop.sync.SyncService.removeSyncFinishedListener(syncListener);
            }
        });

        if (this.getScene() != null) {
            com.reminder.desktop.sync.SyncService.addSyncFinishedListener(syncListener);
        }
    }

    public void displayNotes(List<QuickNote> notes) {
        this.currentNotesList = notes;
        listContainer.getChildren().clear();

        if (notes.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.getStyleClass().add("empty-state-box");
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(24));
            
            Label iconLbl = new Label("🗒");
            iconLbl.getStyleClass().add("empty-state-icon");
            
            Label titleLbl = new Label("No quick notes yet");
            titleLbl.getStyleClass().add("empty-state-title");
            
            Label descLbl = new Label("Add items above to start your checklist.");
            descLbl.getStyleClass().add("empty-state-desc");
            
            emptyBox.getChildren().addAll(iconLbl, titleLbl, descLbl);
            listContainer.getChildren().add(emptyBox);
            return;
        }

        QuickNoteRepository noteRepo = new QuickNoteRepository();

        for (QuickNote note : notes) {
            HBox noteCard = new HBox(16);
            noteCard.getStyleClass().add("card");
            noteCard.setAlignment(Pos.CENTER_LEFT);
            noteCard.setPadding(new Insets(12, 16, 12, 16));
            noteCard.setCursor(javafx.scene.Cursor.OPEN_HAND);

            // Drag and Drop Event Handlers
            noteCard.setOnDragDetected(event -> {
                Dragboard db = noteCard.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(notes.indexOf(note)));
                db.setContent(content);
                noteCard.setCursor(javafx.scene.Cursor.CLOSED_HAND);
                event.consume();
            });

            noteCard.setOnDragDone(event -> {
                noteCard.setCursor(javafx.scene.Cursor.OPEN_HAND);
                event.consume();
            });

            noteCard.setOnDragOver(event -> {
                if (event.getGestureSource() != noteCard && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            noteCard.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    try {
                        int sourceIndex = Integer.parseInt(db.getString());
                        int targetIndex = notes.indexOf(note);

                        if (sourceIndex >= 0 && sourceIndex < notes.size() && targetIndex >= 0 && targetIndex < notes.size() && sourceIndex != targetIndex) {
                            QuickNote sourceNote = notes.get(sourceIndex);
                            notes.remove(sourceIndex);
                            notes.add(targetIndex, sourceNote);

                            // Reassign order
                            for (int i = 0; i < notes.size(); i++) {
                                notes.get(i).setPosition(i);
                            }

                            // Save positions asynchronously
                            final List<QuickNote> notesToSave = List.copyOf(notes);
                            new Thread(() -> {
                                try {
                                    for (QuickNote n : notesToSave) {
                                        noteRepo.updateNotePosition(n, n.getPosition());
                                    }
                                    Platform.runLater(() -> displayNotes(notes));
                                } catch (Exception ex) {
                                    System.err.println("Error saving reordered notes: " + ex.getMessage());
                                }
                            }).start();

                            success = true;
                        }
                    } catch (Exception ex) {
                        System.err.println("Error reordering notes: " + ex.getMessage());
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });

            // Complete Checkbox
            CheckBox completeBox = new CheckBox();
            completeBox.setSelected(note.isCompleted());
            completeBox.setOnAction(e -> controller.toggleComplete(note, completeBox.isSelected(), this));

            // Note Text Label
            Label textLbl = new Label(note.getText());
            textLbl.setWrapText(true);
            HBox.setHgrow(textLbl, Priority.ALWAYS);
            if (note.isCompleted()) {
                textLbl.setStyle("-fx-text-fill: -color-text-secondary; -fx-strikethrough: true;");
            }

            // Sync Status Indicator
            Label syncLbl = new Label();
            syncLbl.getStyleClass().add("badge-pill");
            if ("PENDING".equalsIgnoreCase(note.getSyncStatus())) {
                syncLbl.setText("Pending");
                syncLbl.getStyleClass().add("badge-pending");
            } else {
                syncLbl.setText("Synced");
                syncLbl.getStyleClass().add("badge-synced");
            }

            // Edit Button
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
            editBtn.setOnAction(e -> triggerEditDialog(note));

            // Delete Button
            Button delBtn = new Button("Delete");
            delBtn.getStyleClass().add("button-danger");
            delBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
            delBtn.setOnAction(e -> controller.deleteNote(note, this));

            HBox controls = new HBox(8, syncLbl, editBtn, delBtn);
            controls.setAlignment(Pos.CENTER_RIGHT);

            noteCard.getChildren().addAll(completeBox, textLbl, controls);
            listContainer.getChildren().add(noteCard);
        }
    }

    private void triggerEditDialog(QuickNote note) {
        TextInputDialog dialog = new TextInputDialog(note.getText());
        dialog.setTitle("Edit Note");
        dialog.setHeaderText("Modify Note");
        dialog.setContentText("Note content:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        ThemeManager.registerRoot(dialog.getDialogPane());

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newText -> controller.updateNoteText(note, newText, this));
    }
}
