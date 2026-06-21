package com.reminder.desktop.models;

public class MonthlyPayment {
    private Integer id;
    private Long serverId;
    private String name;
    private long dueDate;
    private boolean completed;
    private Long updatedAt;
    private String syncStatus;
    private Double amount;
    private RecurrenceType recurrence = RecurrenceType.MONTHLY;
    private String notificationOffsets = "0";

    public MonthlyPayment() {
    }

    public MonthlyPayment(Integer id, Long serverId, String name, long dueDate, boolean completed, Long updatedAt, String syncStatus) {
        this.id = id;
        this.serverId = serverId;
        this.name = name;
        this.dueDate = dueDate;
        this.completed = completed;
        this.updatedAt = updatedAt;
        this.syncStatus = syncStatus;
        this.amount = null;
        this.recurrence = RecurrenceType.MONTHLY;
        this.notificationOffsets = "0";
    }

    public MonthlyPayment(Integer id, Long serverId, String name, long dueDate, boolean completed, Long updatedAt, String syncStatus, Double amount) {
        this.id = id;
        this.serverId = serverId;
        this.name = name;
        this.dueDate = dueDate;
        this.completed = completed;
        this.updatedAt = updatedAt;
        this.syncStatus = syncStatus;
        this.amount = amount;
        this.recurrence = RecurrenceType.MONTHLY;
        this.notificationOffsets = "0";
    }

    public MonthlyPayment(Integer id, Long serverId, String name, long dueDate, boolean completed, Long updatedAt, String syncStatus, Double amount, RecurrenceType recurrence) {
        this.id = id;
        this.serverId = serverId;
        this.name = name;
        this.dueDate = dueDate;
        this.completed = completed;
        this.updatedAt = updatedAt;
        this.syncStatus = syncStatus;
        this.amount = amount;
        this.recurrence = recurrence;
        this.notificationOffsets = "0";
    }

    public MonthlyPayment(Integer id, Long serverId, String name, long dueDate, boolean completed, Long updatedAt, String syncStatus, Double amount, RecurrenceType recurrence, String notificationOffsets) {
        this.id = id;
        this.serverId = serverId;
        this.name = name;
        this.dueDate = dueDate;
        this.completed = completed;
        this.updatedAt = updatedAt;
        this.syncStatus = syncStatus;
        this.amount = amount;
        this.recurrence = recurrence;
        this.notificationOffsets = notificationOffsets;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
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
        return "MonthlyPayment{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", name='" + name + '\'' +
                ", dueDate=" + dueDate +
                ", completed=" + completed +
                ", updatedAt=" + updatedAt +
                ", syncStatus='" + syncStatus + '\'' +
                ", amount=" + amount +
                ", recurrence=" + recurrence +
                ", notificationOffsets='" + notificationOffsets + '\'' +
                '}';
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public RecurrenceType getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(RecurrenceType recurrence) {
        this.recurrence = recurrence;
    }

    public String getNotificationOffsets() {
        return notificationOffsets;
    }

    public void setNotificationOffsets(String notificationOffsets) {
        this.notificationOffsets = notificationOffsets;
    }
}
