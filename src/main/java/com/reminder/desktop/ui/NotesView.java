package com.reminder.desktop.ui;

import com.reminder.desktop.models.QuickNote;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

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
    }

    public void displayNotes(List<QuickNote> notes) {
        this.currentNotesList = notes;
        listContainer.getChildren().clear();

        if (notes.isEmpty()) {
            Label placeholder = new Label("No quick notes available. Write one above!");
            placeholder.getStyleClass().add("subtitle-label");
            listContainer.getChildren().add(placeholder);
            return;
        }

        for (QuickNote note : notes) {
            HBox noteCard = new HBox(16);
            noteCard.getStyleClass().add("card");
            noteCard.setAlignment(Pos.CENTER_LEFT);
            noteCard.setPadding(new Insets(12, 16, 12, 16));

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
            syncLbl.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 4px;");
            if ("PENDING".equalsIgnoreCase(note.getSyncStatus())) {
                syncLbl.setText("Pending");
                syncLbl.setStyle(syncLbl.getStyle() + " -fx-background-color: -color-accent-light; -fx-text-fill: -color-accent;");
            } else {
                syncLbl.setText("Synced");
                syncLbl.setStyle(syncLbl.getStyle() + " -fx-background-color: #E2F6EA; -fx-text-fill: -color-success;");
            }

            // Move Up Button
            Button upBtn = new Button("▲");
            upBtn.setStyle("-fx-padding: 4 6; -fx-font-size: 10px;");
            upBtn.setOnAction(e -> controller.moveNoteUp(note, currentNotesList, this));

            // Move Down Button
            Button downBtn = new Button("▼");
            downBtn.setStyle("-fx-padding: 4 6; -fx-font-size: 10px;");
            downBtn.setOnAction(e -> controller.moveNoteDown(note, currentNotesList, this));

            // Edit Button
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
            editBtn.setOnAction(e -> triggerEditDialog(note));

            // Delete Button
            Button delBtn = new Button("Delete");
            delBtn.getStyleClass().add("button-danger");
            delBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
            delBtn.setOnAction(e -> controller.deleteNote(note, this));

            HBox controls = new HBox(8, syncLbl, upBtn, downBtn, editBtn, delBtn);
            controls.setAlignment(Pos.CENTER_RIGHT);

            noteCard.getChildren().addAll(completeBox, textLbl, controls);
            listContainer.getChildren().add(noteCard);
        }
    }

    private void triggerEditDialog(QuickNote note) {
        TextInputDialog dialog = new TextInputDialog(note.getText());
        dialog.setTitle("Edit Note");
        dialog.setHeaderText("Modify your quick note content:");
        dialog.setContentText("Note content:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newText -> controller.updateNoteText(note, newText, this));
    }
}
