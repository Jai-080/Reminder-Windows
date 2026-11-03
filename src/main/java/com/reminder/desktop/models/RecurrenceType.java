package com.reminder.desktop.models;

public enum RecurrenceType {
    ONE_TIME,
    MONTHLY,
    BI_MONTHLY,
    QUARTERLY,
    HALF_YEARLY,
    YEARLY,
    CUSTOM_DAYS,
    UNKNOWN;

    /**
     * Determines whether the current backend server API supports this recurrence type.
     * The backend supports all standard seven recurrence types.
     */
    public boolean isSupportedByBackend() {
        return this != UNKNOWN;
    }

    /**
     * Determines whether this recurrence type must be kept LOCAL_ONLY to prevent
     * backend errors during synchronization.
     */
    public boolean isLocalOnly() {
        return !isSupportedByBackend();
    }

    public static RecurrenceType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MONTHLY;
        }
        try {
            return RecurrenceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
