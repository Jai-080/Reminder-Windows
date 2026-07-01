# 📝 Project Summary — Reminder Desktop (Windows)

This document provides a technical summary of the **Reminder Desktop** codebase, its architectural patterns, structural design, and sub-systems.

---

## 🌟 Executive Overview

**Reminder Desktop** is a Windows client application designed to run locally, manage user data offline via SQLite, and automatically sync in the background with the Spring Boot server in the **Reminder Ecosystem**. 

### Core Specifications
*   **Language & Platform**: Java 21 & JavaFX 21.0.7
*   **Database**: Local SQLite via JDBC driver
*   **Networking Protocol**: HTTP REST APIs (JSON payload) & WebSocket (STOMP frames)
*   **Build System**: Maven
*   **Installer Packaging**: WiX Toolset and `jpackage`

---

## 🏗️ Architectural Components

The codebase is organized into modular packages, separating concerns between UI, persistent data access, sync logic, and scheduler services.

```
m:\ReminderWindows\src\main\java\com\reminder\desktop
│
├── Launcher.java                 # Non-modular application launcher entrypoint
├── MainApplication.java          # JavaFX main application controller & lifecycle setup
│
├── auth                          # Session & JWT token storage management
│   └── TokenStorage.java         # Accesses and writes session configurations
│
├── config                        # Static and dynamic properties
│   └── ServerConfig.java         # Server base URL declarations
│
├── database                      # Local Persistence layer (SQLite)
│   ├── DatabaseManager.java      # SQLite connection initialization & table schemas
│   ├── MonthlyPaymentDao.java    # CRUD operations for bill reminders
│   ├── QuickNoteDao.java         # CRUD operations for checklist notes
│   └── ReminderDao.java          # CRUD operations for timed alerts
│
├── dto                           # Data Transfer Objects for API JSON serialization
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── MonthlyPaymentDto.java
│   ├── QuickNoteDto.java
│   └── ReminderDto.java
│
├── models                        # Core entity classes
│   ├── MonthlyPayment.java
│   ├── QuickNote.java
│   ├── RecurrenceType.java
│   └── Reminder.java
│
├── notifications                 # Alarms, trigger engines & system tray popups
│   ├── NotificationManager.java  # OS tray warning/success notification trigger
│   └── ReminderScheduler.java    # In-memory thread pools scheduling future alerts
│
├── sync                          # Real-time WebSockets and asynchronous REST sync engine
│   ├── ApiClient.java            # Standard HTTP Client with JSON mapping
│   ├── SyncService.java          # Primary bidirectional sync & LWW conflict resolution
│   └── WebSocketManager.java     # Real-time WebSocket listener using STOMP frames
│
└── ui                            # Graphical views & FXML / CSS layouts
    ├── DashboardView.java        # Main status overview screen
    ├── NotesView.java / NotesController.java
    ├── PaymentView.java / PaymentController.java
    ├── ReminderView.java / ReminderController.java
    ├── LoginView.java / LoginController.java
    ├── RegisterView.java / RegisterController.java
    ├── Sidebar.java / MainLayout.java
    ├── ThemeManager.java         # Stylesheet injects & Dark Mode toggle manager
    └── TimePicker.java           # Custom numeric input spinner for custom alert times
```

---

## 🔄 Technical Deep Dive

### 1. Offline-First Synchronization
The sync engine [SyncService.java](file:///m:/ReminderWindows/src/main/java/com/reminder/desktop/sync/SyncService.java) implements a three-phase bi-directional sync loop:
1.  **Deletions First**: Identifies elements marked locally as deleted (`DELETE_PENDING`) and calls server endpoints to remove them before processing pulls.
2.  **Pull and Merge (LWW)**: Pulls updated entities from the server. If `server.updatedAt > local.updatedAt`, the local SQLite record is overwritten.
3.  **Push Local Pending**: Identifies locally modified elements (`PENDING`) and issues `POST` (create) or `PUT` (update) operations to save changes to the server.

### 2. Real-Time Sync & WebSocket Client
*   A persistent WebSocket connection is managed by [WebSocketManager.java](file:///m:/ReminderWindows/src/main/java/com/reminder/desktop/sync/WebSocketManager.java).
*   It listens for incoming change notifications from the backend. When a change notification is received, it triggers an asynchronous sync cycle (`SyncService.getInstance().triggerSyncAsync(...)`) to pull changes immediately.
*   Includes built-in reconnect logic with exponential backoff on disconnect.

### 3. Alarm & Event Scheduler
*   The [ReminderScheduler.java](file:///m:/ReminderWindows/src/main/java/com/reminder/desktop/notifications/ReminderScheduler.java) implements a multi-threaded `ScheduledThreadPoolExecutor`.
*   On startup, it loads all uncompleted reminders and recurring payments from SQLite.
*   Events that are overdue are fired immediately, while future events are scheduled as deferred tasks.
*   When a reminder expires, a tray popup is shown via [NotificationManager.java](file:///m:/ReminderWindows/src/main/java/com/reminder/desktop/notifications/NotificationManager.java) with snooze options.

### 4. Styling & Theme Engine
*   The [ThemeManager.java](file:///m:/ReminderWindows/src/main/java/com/reminder/desktop/ui/ThemeManager.java) maintains a list of active UI parent nodes and applies `style.css` styles dynamically.
*   Enabling dark mode injects a `.dark` style class to view root elements, changing color variables dynamically.

---

## 📈 Build & Release Pipeline
The project uses `jpackage` to bundle dependencies and compile installers for distribution:
*   [build-installer.ps1](file:///m:/ReminderWindows/build-installer.ps1) handles the setup of standalone WiX Toolset executables, builds classes, copies dependencies, and uses the packaging utility to compile installers.
*   Target installers output to `target/installer/` as standalone MSI or EXE files.
