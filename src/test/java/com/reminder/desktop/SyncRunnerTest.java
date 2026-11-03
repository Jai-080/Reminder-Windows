package com.reminder.desktop;

import com.reminder.desktop.sync.SyncService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SyncRunnerTest {

    @BeforeAll
    public static void setUp() {
        String existingUrl = System.getProperty("reminder.server.url");
        if (existingUrl == null || existingUrl.trim().isEmpty()) {
            System.setProperty("reminder.server.url", "http://localhost:8088/");
        }
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (Throwable ignored) {}
    }

    @Test
    public void testServerUrlPrecedence() {
        String currentUrl = System.getProperty("reminder.server.url");
        assertTrue(currentUrl != null && !currentUrl.trim().isEmpty(), "Server URL must be configured");
        System.out.println("SyncRunnerTest: Effective test server URL is: " + currentUrl);
    }

    @Test
    public void runSync() throws Exception {
        System.out.println("SyncRunnerTest: Starting full bidirectional sync on Windows client...");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<String> message = new AtomicReference<>("");

        SyncService.getInstance().triggerSyncAsync(new SyncService.SyncListener() {
            @Override
            public void onSyncStarted() {
                System.out.println("SyncRunnerTest: onSyncStarted");
            }

            @Override
            public void onSyncFinished(boolean s, String msg) {
                System.out.println("SyncRunnerTest: onSyncFinished: success=" + s + ", message=" + msg);
                success.set(s);
                message.set(msg);
                latch.countDown();
            }
        });

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        assertTrue(finished, "Windows sync should finish within 15 seconds");
        assertTrue(success.get(), "Windows sync should succeed: " + message.get());
        System.out.println("SyncRunnerTest: Windows sync completed successfully!");
    }
}
