package com.reminder.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MonthlyPaymentDto {
    private Long id;
    private String name;
    private Long dueDate;
    private Boolean completed;
    private Long lastPaidAt;
    private Double amount;
    private String recurrence;
    private String notificationOffsets;
    private String createdAt;
    private String updatedAt;

    public MonthlyPaymentDto() {
    }

    public MonthlyPaymentDto(Long id, String name, Long dueDate, Boolean completed) {
        this.id = id;
        this.name = name;
        this.dueDate = dueDate;
        this.completed = completed;
        this.amount = null;
        this.recurrence = "MONTHLY";
        this.notificationOffsets = "0";
    }

    public MonthlyPaymentDto(Long id, String name, Long dueDate, Boolean completed, Double amount) {
        this.id = id;
        this.name = name;
        this.dueDate = dueDate;
        this.completed = completed;
        this.amount = amount;
        this.recurrence = "MONTHLY";
        this.notificationOffsets = "0";
    }

    public MonthlyPaymentDto(Long id, String name, Long dueDate, Boolean completed, Double amount, String recurrence) {
        this.id = id;
        this.name = name;
        this.dueDate = dueDate;
        this.completed = completed;
        this.amount = amount;
        this.recurrence = recurrence;
        this.notificationOffsets = "0";
    }

    public MonthlyPaymentDto(Long id, String name, Long dueDate, Boolean completed, Double amount, String recurrence, String notificationOffsets) {
        this.id = id;
        this.name = name;
        this.dueDate = dueDate;
        this.completed = completed;
        this.amount = amount;
        this.recurrence = recurrence;
        this.notificationOffsets = notificationOffsets;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDueDate() {
        return dueDate;
    }

    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    public String getNotificationOffsets() {
        return notificationOffsets;
    }

    public void setNotificationOffsets(String notificationOffsets) {
        this.notificationOffsets = notificationOffsets;
    }

    public Long getLastPaidAt() {
        return lastPaidAt;
    }

    public void setLastPaidAt(Long lastPaidAt) {
        this.lastPaidAt = lastPaidAt;
    }
}
