package com.reminder.desktop.models;

public class QuickNote {
    private Integer id;
    private Long serverId;
    private String text;
    private boolean completed;
    private int position;
    private Long updatedAt;
    private String syncStatus;

    public QuickNote() {
    }

    public QuickNote(Integer id, Long serverId, String text, boolean completed, int position, Long updatedAt, String syncStatus) {
        this.id = id;
        this.serverId = serverId;
        this.text = text;
        this.completed = completed;
        this.position = position;
        this.updatedAt = updatedAt;
        this.syncStatus = syncStatus;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    @Override
    public String toString() {
        return "QuickNote{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", text='" + text + '\'' +
                ", completed=" + completed +
                ", position=" + position +
                ", updatedAt=" + updatedAt +
                ", syncStatus='" + syncStatus + '\'' +
                '}';
    }
}
