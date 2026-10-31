package com.reminder.desktop.models;

public enum RecurrenceType {
    ONE_TIME,
    MONTHLY,
    BI_MONTHLY,
    QUARTERLY,
    HALF_YEARLY,
    YEARLY,
    CUSTOM_DAYS;

    /**
     * Determines whether the current backend server API supports this recurrence type.
     * The backend currently supports ONE_TIME, MONTHLY, QUARTERLY, and YEARLY.
     */
    public boolean isSupportedByBackend() {
        return this == ONE_TIME || this == MONTHLY || this == QUARTERLY || this == YEARLY;
    }

    /**
     * Determines whether this recurrence type must be kept LOCAL_ONLY to prevent
     * backend 500 errors during synchronization.
     */
    public boolean isLocalOnly() {
        return !isSupportedByBackend();
    }
}
