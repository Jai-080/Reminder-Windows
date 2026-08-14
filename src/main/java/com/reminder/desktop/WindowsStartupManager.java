package com.reminder.desktop;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WindowsStartupManager {
    private static final String APP_NAME = "Reminder";
    
    public static File getStartupFile() {
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
    
    public static boolean isStartupEnabled() {
        File startupFile = getStartupFile();
        return startupFile != null && startupFile.exists();
    }
    
    public static void enableStartup() throws IOException {
        File startupFile = getStartupFile();
        if (startupFile == null) {
            throw new IOException("Could not locate Windows Startup directory.");
        }
        
        String exePath = getExecutablePath();
        if (exePath == null) {
            throw new IOException("Could not locate the Reminder executable path.");
        }
        
        if (exePath.endsWith(".jar") && !new File(startupFile.getParentFile().getParentFile(), "Reminder.exe").exists()) {
            System.out.println("[StartupManager] Warning: Running in development mode. Executable path resolved to JAR: " + exePath);
        }
        
        // Write VBScript to Startup directory for silent startup (no cmd window flash)
        try (FileWriter writer = new FileWriter(startupFile)) {
            writer.write("Set WshShell = CreateObject(\"WScript.Shell\")\n");
            writer.write("WshShell.Run \"\"\"" + exePath + "\"\" --startup\", 0, false\n");
        }
        System.out.println("Windows startup entry successfully updated at: " + startupFile.getAbsolutePath());
    }
    
    public static void disableStartup() {
        File startupFile = getStartupFile();
        if (startupFile != null && startupFile.exists()) {
            if (startupFile.delete()) {
                System.out.println("Windows startup entry successfully removed.");
            } else {
                System.err.println("Failed to remove Windows startup entry.");
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
                    // Secondary check: parent folder grandparent check
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
