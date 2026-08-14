package com.reminder.desktop;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WindowsStartupManager {
    private static final String APP_NAME = "Reminder";
    
    public static File getStartupShortcutFile() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            return null;
        }
        File startupDir = new File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\Startup");
        if (!startupDir.exists()) {
            startupDir.mkdirs();
        }
        return new File(startupDir, APP_NAME + ".lnk");
    }
    
    public static File getLegacyStartupVbsFile() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            return null;
        }
        File startupDir = new File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\Startup");
        if (!startupDir.exists()) {
            startupDir.mkdirs();
        }
        return new File(startupDir, APP_NAME + ".vbs");
    }
    
    public static boolean isStartupEntryPresent() {
        File shortcut = getStartupShortcutFile();
        File legacyVbs = getLegacyStartupVbsFile();
        return (shortcut != null && shortcut.exists()) || (legacyVbs != null && legacyVbs.exists());
    }
    
    public static void createStartupEntry() {
        // Writes legacy VBScript startup entry (useful for development/testing)
        File vbsFile = getLegacyStartupVbsFile();
        if (vbsFile == null) return;
        
        try {
            String exePath = getExecutablePath();
            if (exePath == null) {
                throw new IOException("Could not locate the Reminder executable path.");
            }
            
            try (FileWriter writer = new FileWriter(vbsFile)) {
                writer.write("Set WshShell = CreateObject(\"WScript.Shell\")\n");
                writer.write("WshShell.Run \"\"\"" + exePath + "\"\" --startup\", 0, false\n");
            }
            System.out.println("Windows VBScript startup entry successfully created at: " + vbsFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to create VBScript startup entry: " + e.getMessage());
        }
    }
    
    public static void updateStartupEntry() {
        // Only update if the legacy VBScript file exists (stale path protection for legacy installs)
        File vbsFile = getLegacyStartupVbsFile();
        if (vbsFile != null && vbsFile.exists()) {
            createStartupEntry();
        }
    }
    
    public static void removeStartupEntry() {
        File shortcut = getStartupShortcutFile();
        if (shortcut != null && shortcut.exists()) {
            if (shortcut.delete()) {
                System.out.println("Removed native startup shortcut: " + shortcut.getAbsolutePath());
            }
        }
        File legacyVbs = getLegacyStartupVbsFile();
        if (legacyVbs != null && legacyVbs.exists()) {
            if (legacyVbs.delete()) {
                System.out.println("Removed legacy startup VBScript: " + legacyVbs.getAbsolutePath());
            }
        }
    }
    
    private static String getExecutablePath() {
        try {
            String path = MainApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            if (path == null) {
                return null;
            }
            File file = new File(path);
            
            // In a jpackage installation, the layout is:
            // InstallationRoot/
            //   Reminder.exe
            //   app/
            //     ReminderWindows-1.0-SNAPSHOT.jar
            if (file.getName().endsWith(".jar")) {
                File parent = file.getParentFile();
                if (parent != null) {
                    if ("app".equalsIgnoreCase(parent.getName())) {
                        File exeFile = new File(parent.getParentFile(), "Reminder.exe");
                        if (exeFile.exists()) {
                            return exeFile.getAbsolutePath();
                        }
                    }
                    // Secondary check
                    File exeFile = new File(parent.getParentFile(), "Reminder.exe");
                    if (exeFile.exists()) {
                        return exeFile.getAbsolutePath();
                    }
                }
            }
            
            // Fallback: check if local Reminder.exe exists in user directory
            File localExe = new File("Reminder.exe");
            if (localExe.exists()) {
                return localExe.getAbsolutePath();
            }
            
            return file.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Error resolving executable path: " + e.getMessage());
            return null;
        }
    }
}
