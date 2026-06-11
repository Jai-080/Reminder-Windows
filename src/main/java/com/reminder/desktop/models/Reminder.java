package com.reminder.desktop.models;

public class Reminder {
    private Integer id;
    private Long serverId;
    private String text;
    private long time;
    private boolean expired;
    private long snoozedTime;
    private Long updatedAt;
    private String syncStatus;

    public Reminder() {
    }

    public Reminder(Integer id, Long serverId, String text, long time, boolean expired, long snoozedTime, Long updatedAt, String syncStatus) {
        this.id = id;
        this.serverId = serverId;
        this.text = text;
        this.time = time;
        this.expired = expired;
        this.snoozedTime = snoozedTime;
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public long getSnoozedTime() {
        return snoozedTime;
    }

    public void setSnoozedTime(long snoozedTime) {
        this.snoozedTime = snoozedTime;
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
        return "Reminder{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", text='" + text + '\'' +
                ", time=" + time +
                ", expired=" + expired +
                ", snoozedTime=" + snoozedTime +
                ", updatedAt=" + updatedAt +
                ", syncStatus='" + syncStatus + '\'' +
                '}';
    }
}
