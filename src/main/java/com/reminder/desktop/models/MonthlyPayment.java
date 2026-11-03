package com.reminder.desktop.models;

import java.util.Calendar;

public class MonthlyPayment {
    private Integer id;
    private Long serverId;
    private String name;
    private long dueDate;
    private boolean completed;
    private Long lastPaidAt;
    private Long updatedAt;
    private String syncStatus;
    private Double amount;
    private RecurrenceType recurrence = RecurrenceType.MONTHLY;
    private String notificationOffsets = "0";
    private int originalDueDay;
    private int customIntervalDays = 28;

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
        this.customIntervalDays = 28;
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
        this.customIntervalDays = 28;
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
        this.customIntervalDays = 28;
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
        this.customIntervalDays = 28;
    }

    public MonthlyPayment(Integer id, Long serverId, String name, long dueDate, boolean completed, Long updatedAt, String syncStatus, Double amount, RecurrenceType recurrence, String notificationOffsets, int originalDueDay) {
        this(id, serverId, name, dueDate, completed, updatedAt, syncStatus, amount, recurrence, notificationOffsets);
        this.originalDueDay = originalDueDay;
        this.customIntervalDays = 28;
    }

    public MonthlyPayment(Integer id, Long serverId, String name, long dueDate, boolean completed, Long updatedAt, String syncStatus, Double amount, RecurrenceType recurrence, String notificationOffsets, int originalDueDay, int customIntervalDays) {
        this(id, serverId, name, dueDate, completed, updatedAt, syncStatus, amount, recurrence, notificationOffsets, originalDueDay);
        this.customIntervalDays = customIntervalDays > 0 ? customIntervalDays : 28;
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

    public int getOriginalDueDay() {
        if (originalDueDay <= 0 && dueDate > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(dueDate);
            originalDueDay = cal.get(Calendar.DAY_OF_MONTH);
        }
        return originalDueDay;
    }

    public void setOriginalDueDay(int originalDueDay) {
        this.originalDueDay = originalDueDay;
    }

    public int getCustomIntervalDays() {
        return customIntervalDays;
    }

    public void setCustomIntervalDays(int customIntervalDays) {
        this.customIntervalDays = customIntervalDays;
    }

    public Long getLastPaidAt() {
        return lastPaidAt;
    }

    public void setLastPaidAt(Long lastPaidAt) {
        this.lastPaidAt = lastPaidAt;
    }

    public static long calculateImmediateNextDueDate(long currentDueDateMillis, RecurrenceType recurrence, int originalDueDay) {
        return calculateNextDueDate(currentDueDateMillis, recurrence, originalDueDay, 28, Long.MIN_VALUE);
    }

    public static long calculateImmediateNextDueDate(long currentDueDateMillis, RecurrenceType recurrence, int originalDueDay, int customIntervalDays) {
        return calculateNextDueDate(currentDueDateMillis, recurrence, originalDueDay, customIntervalDays, Long.MIN_VALUE);
    }

    public static long calculateNextDueDate(long currentDueDateMillis, RecurrenceType recurrence, int originalDueDay) {
        return calculateNextDueDate(currentDueDateMillis, recurrence, originalDueDay, 28, System.currentTimeMillis());
    }

    public static long calculateNextDueDate(long currentDueDateMillis, RecurrenceType recurrence, int originalDueDay, int customIntervalDays) {
        return calculateNextDueDate(currentDueDateMillis, recurrence, originalDueDay, customIntervalDays, System.currentTimeMillis());
    }

    public static long calculateNextDueDate(long currentDueDateMillis, RecurrenceType recurrence, int originalDueDay, long referenceTimeMillis) {
        return calculateNextDueDate(currentDueDateMillis, recurrence, originalDueDay, 28, referenceTimeMillis);
    }

    public static long calculateNextDueDate(long currentDueDateMillis, RecurrenceType recurrence, int originalDueDay, int customIntervalDays, long referenceTimeMillis) {
        if (recurrence == null || recurrence == RecurrenceType.ONE_TIME || recurrence == RecurrenceType.UNKNOWN) {
            return currentDueDateMillis;
        }

        if (recurrence == RecurrenceType.CUSTOM_DAYS) {
            int interval = customIntervalDays;
            if (interval <= 0 || interval > 365) {
                interval = 28;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(currentDueDateMillis);

            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);
            int millis = cal.get(Calendar.MILLISECOND);

            int iterations = 0;
            do {
                cal.add(Calendar.DATE, interval);
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, second);
                cal.set(Calendar.MILLISECOND, millis);
                iterations++;
            } while (cal.getTimeInMillis() <= referenceTimeMillis && iterations < 1000);

            return cal.getTimeInMillis();
        }

        int stepMonths;
        switch (recurrence) {
            case BI_MONTHLY:
                stepMonths = 2;
                break;
            case QUARTERLY:
                stepMonths = 3;
                break;
            case HALF_YEARLY:
                stepMonths = 6;
                break;
            case YEARLY:
                stepMonths = 12;
                break;
            case MONTHLY:
            default:
                stepMonths = 1;
                break;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentDueDateMillis);

        int targetDay = originalDueDay;
        if (targetDay <= 0 || targetDay > 31) {
            targetDay = cal.get(Calendar.DAY_OF_MONTH);
        }

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int millis = cal.get(Calendar.MILLISECOND);

        int iterations = 0;
        do {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, stepMonths);
            int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int clampedDay = Math.min(targetDay, maxDays);
            cal.set(Calendar.DAY_OF_MONTH, clampedDay);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);
            cal.set(Calendar.MILLISECOND, millis);
            iterations++;
        } while (cal.getTimeInMillis() <= referenceTimeMillis && iterations < 120);

        return cal.getTimeInMillis();
    }

    public boolean isRecentlyPaid(int currentMonth, int currentYear) {
        Long lpa = getLastPaidAt();
        if (lpa == null) return false;
        if (recurrence == RecurrenceType.ONE_TIME) {
            return completed;
        }
        if (recurrence == RecurrenceType.CUSTOM_DAYS) {
            if (dueDate <= System.currentTimeMillis()) {
                return false;
            }
            long intervalMillis = (long) getCustomIntervalDays() * 24L * 60L * 60L * 1000L;
            return lpa >= (dueDate - intervalMillis - 60000L);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lpa);
        int paidMonth = cal.get(Calendar.MONTH) + 1;
        int paidYear = cal.get(Calendar.YEAR);
        return paidMonth == currentMonth && paidYear == currentYear;
    }

    public boolean isRecentlyPaid() {
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH) + 1;
        int currentYear = now.get(Calendar.YEAR);
        return isRecentlyPaid(currentMonth, currentYear);
    }

    public boolean isUpcoming(int currentMonth, int currentYear) {
        if (isRecentlyPaid(currentMonth, currentYear)) {
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dueDate);
        int dueMonth = cal.get(Calendar.MONTH) + 1;
        int dueYear = cal.get(Calendar.YEAR);
        if (recurrence == RecurrenceType.CUSTOM_DAYS) {
            return dueDate <= System.currentTimeMillis() || (dueYear < currentYear) || (dueYear == currentYear && dueMonth <= currentMonth);
        }
        return (dueYear < currentYear) || (dueYear == currentYear && dueMonth <= currentMonth);
    }

    public boolean isDueLater(int currentMonth, int currentYear) {
        if (isRecentlyPaid(currentMonth, currentYear)) {
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dueDate);
        int dueMonth = cal.get(Calendar.MONTH) + 1;
        int dueYear = cal.get(Calendar.YEAR);
        if (recurrence == RecurrenceType.CUSTOM_DAYS) {
            return (dueYear > currentYear) || (dueYear == currentYear && dueMonth > currentMonth);
        }
        return (dueYear > currentYear) || (dueYear == currentYear && dueMonth > currentMonth);
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
                ", originalDueDay=" + originalDueDay +
                ", customIntervalDays=" + customIntervalDays +
                ", lastPaidAt=" + lastPaidAt +
                '}';
    }
}
