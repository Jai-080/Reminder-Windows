package com.reminder.desktop.ui;

import com.reminder.desktop.models.QuickNote;
import com.reminder.desktop.repository.QuickNoteRepository;
import javafx.application.Platform;

import java.util.Collections;
import java.util.List;

public class NotesController {
    private final QuickNoteRepository repository;

    public NotesController() {
        this.repository = new QuickNoteRepository();
    }

    public void loadNotes(NotesView view) {
        new Thread(() -> {
            try {
                List<QuickNote> notes = repository.getAllNotes();
                notes.sort((n1, n2) -> Integer.compare(n1.getPosition(), n2.getPosition()));
                Platform.runLater(() -> view.displayNotes(notes));
            } catch (Exception e) {
                System.err.println("Error loading notes in background: " + e.getMessage());
            }
        }, "LoadNotesThread").start();
    }

    public void addNote(String text, NotesView view) {
        if (text == null || text.trim().isEmpty()) return;
        new Thread(() -> {
            try {
                repository.addNote(text.trim());
                Platform.runLater(() -> loadNotes(view));
            } catch (Exception e) {
                System.err.println("Error adding note: " + e.getMessage());
            }
        }).start();
    }

    public void toggleComplete(QuickNote note, boolean completed, NotesView view) {
        note.setCompleted(completed);
        new Thread(() -> {
            try {
                repository.updateNote(note);
                Platform.runLater(() -> loadNotes(view));
            } catch (Exception e) {
                System.err.println("Error toggling note complete: " + e.getMessage());
            }
        }).start();
    }

    public void updateNoteText(QuickNote note, String newText, NotesView view) {
        if (newText == null || newText.trim().isEmpty() || newText.equals(note.getText())) return;
        note.setText(newText.trim());
        new Thread(() -> {
            try {
                repository.updateNote(note);
                Platform.runLater(() -> loadNotes(view));
            } catch (Exception e) {
                System.err.println("Error updating note text: " + e.getMessage());
            }
        }).start();
    }

    public void deleteNote(QuickNote note, NotesView view) {
        new Thread(() -> {
            try {
                repository.deleteNote(note);
                Platform.runLater(() -> loadNotes(view));
            } catch (Exception e) {
                System.err.println("Error deleting note: " + e.getMessage());
            }
        }).start();
    }

    public void moveNoteUp(QuickNote note, List<QuickNote> allNotes, NotesView view) {
        int idx = allNotes.indexOf(note);
        if (idx <= 0) return; // Already at top

        QuickNote previous = allNotes.get(idx - 1);
        int currentPos = note.getPosition();
        int prevPos = previous.getPosition();

        // If positions are identical, force separation
        if (currentPos == prevPos) {
            currentPos = prevPos + 1;
        }

        note.setPosition(prevPos);
        previous.setPosition(currentPos);

        final int finalPrevPos = prevPos;
        final int finalCurrentPos = currentPos;

        new Thread(() -> {
            try {
                repository.updateNotePosition(note, finalPrevPos);
                repository.updateNotePosition(previous, finalCurrentPos);
                Platform.runLater(() -> loadNotes(view));
            } catch (Exception e) {
                System.err.println("Error moving note up: " + e.getMessage());
            }
        }).start();
    }

    public void moveNoteDown(QuickNote note, List<QuickNote> allNotes, NotesView view) {
        int idx = allNotes.indexOf(note);
        if (idx < 0 || idx >= allNotes.size() - 1) return; // Already at bottom

        QuickNote next = allNotes.get(idx + 1);
        int currentPos = note.getPosition();
        int nextPos = next.getPosition();

        // If positions are identical, force separation
        if (currentPos == nextPos) {
            nextPos = currentPos + 1;
        }

        note.setPosition(nextPos);
        next.setPosition(currentPos);

        final int finalNextPos = nextPos;
        final int finalCurrentPos = currentPos;

        new Thread(() -> {
            try {
                repository.updateNotePosition(note, finalNextPos);
                repository.updateNotePosition(next, finalCurrentPos);
                Platform.runLater(() -> loadNotes(view));
            } catch (Exception e) {
                System.err.println("Error moving note down: " + e.getMessage());
            }
        }).start();
    }
}
