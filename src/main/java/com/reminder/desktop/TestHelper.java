package com.reminder.desktop;

import com.reminder.desktop.repository.ReminderRepository;
import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.sync.SyncService;
import com.reminder.desktop.notifications.ReminderScheduler;
import com.reminder.desktop.auth.AuthServiceImpl;

import com.reminder.desktop.database.MonthlyPaymentDao;
import com.reminder.desktop.models.MonthlyPayment;

public class TestHelper {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting TestHelper for Monthly Payment...");

        System.out.println("Logging in to get fresh token...");
        new AuthServiceImpl().login("jai@test.com", "password", true);
        
        MonthlyPaymentDao paymentDao = new MonthlyPaymentDao();
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(2026, java.util.Calendar.JUNE, 12, 9, 0, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long triggerTime = cal.getTimeInMillis();
        String name = "Test Today Payment 9AM";
        
        MonthlyPayment payment = new MonthlyPayment(
                null,
                null,
                name,
                triggerTime,
                false,
                System.currentTimeMillis(),
                "PENDING"
        );
        
        System.out.println("Adding payment to Windows DB and syncing: " + name + " triggerTime: " + triggerTime);
        paymentDao.insertPayment(payment);
        
        System.out.println("Triggering sync...");
        com.reminder.desktop.sync.SyncService.getInstance().triggerSyncAsync(null);
        
        // Wait for a few seconds to let the async sync run
        Thread.sleep(7000);
        
        System.out.println("Finished running TestHelper. Exiting.");
        System.exit(0);
    }
}
