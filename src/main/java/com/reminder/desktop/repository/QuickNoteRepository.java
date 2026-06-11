package com.reminder.desktop.repository;

import com.reminder.desktop.database.QuickNoteDao;
import com.reminder.desktop.models.QuickNote;
import com.reminder.desktop.sync.ApiClient;
import com.reminder.desktop.sync.SyncService;

import java.util.ArrayList;
import java.util.List;

public class QuickNoteRepository {
    private final QuickNoteDao noteDao;

    public QuickNoteRepository() {
        this.noteDao = new QuickNoteDao();
    }

    public List<QuickNote> getAllNotes() {
        try {
            return noteDao.getAllNotes();
        } catch (Exception e) {
            System.err.println("Error reading notes from database: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public QuickNote addNote(String text) throws Exception {
        int nextPos = 0;
        try {
            nextPos = noteDao.getMaxPosition() + 1;
        } catch (Exception ignored) {}

        QuickNote note = new QuickNote(
                null,
                null,
                text,
                false,
                nextPos,
                System.currentTimeMillis(),
                "PENDING"
        );
        
        noteDao.insertNote(note);
        
        // Trigger background sync (non-blocking)
        SyncService.getInstance().triggerSyncAsync(null);
        
        return note;
    }

    public void updateNote(QuickNote note) throws Exception {
        note.setSyncStatus("PENDING");
        note.setUpdatedAt(System.currentTimeMillis());
        noteDao.updateNote(note);
        
        // Trigger background sync (non-blocking)
        SyncService.getInstance().triggerSyncAsync(null);
    }

    public void updateNotePosition(QuickNote note, int newPosition) throws Exception {
        note.setPosition(newPosition);
        note.setSyncStatus("PENDING");
        note.setUpdatedAt(System.currentTimeMillis());
        noteDao.updateNote(note);
        
        // Trigger background sync (non-blocking)
        SyncService.getInstance().triggerSyncAsync(null);
    }

    public void deleteNote(QuickNote note) throws Exception {
        if (note.getId() != null) {
            noteDao.deleteNote(note.getId());
        }

        if (note.getServerId() != null) {
            // Trigger background API deletion (best effort)
            long serverId = note.getServerId();
            new Thread(() -> {
                try {
                    ApiClient.getInstance().delete("/api/notes/" + serverId);
                } catch (Exception e) {
                    System.err.println("Failed to delete note on server: " + e.getMessage());
                }
            }).start();
        }
    }
}
