package com.reminder.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReminderDto {
    private Long id;
    private String text;
    private Long reminderTime;
    private Boolean isExpired;
    private Long snoozedTime;
    private String createdAt;
    private String updatedAt;

    public ReminderDto() {
    }

    public ReminderDto(Long id, String text, Long reminderTime, Boolean isExpired, Long snoozedTime) {
        this.id = id;
        this.text = text;
        this.reminderTime = reminderTime;
        this.isExpired = isExpired;
        this.snoozedTime = snoozedTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(Long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(Boolean expired) {
        isExpired = expired;
    }

    public Long getSnoozedTime() {
        return snoozedTime;
    }

    public void setSnoozedTime(Long snoozedTime) {
        this.snoozedTime = snoozedTime;
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
}
