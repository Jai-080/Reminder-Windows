# Reminder Desktop — Windows Client

The Windows desktop application for the Reminder Ecosystem, built with Java, JavaFX, and SQLite.

## Key Features
* **Dashboard Overview**: Summary of active notes, timed reminders, and payment tracking.
* **Quick Notes**: Fast checkbox checklists.
* **Timed Reminders**: Desktop alerts with snooze configuration.
* **Payment Reminders**: Tracking and scheduled system notifications for monthly/recurring bills.
* **Bi-directional Synchronization**: Syncs automatically with the Spring Boot Server backend, utilizing "Last-Write-Wins" (LWW) conflict resolution logic.

## Prerequisites
* Java Development Kit (JDK) 17+
* Maven 3.x
* WiX Toolset (optional, for building MSI installer package)

## Getting Started

### 1. Configuration
By default, the client is configured with a placeholder server URL. 
Please refer to [CONFIGURATION.md](CONFIGURATION.md) for server configuration instructions.

### 2. Compilation and Run
To run the client application in development mode:
```bash
./mvnw.cmd javafx:run
```

To compile and package the application into a JAR:
```bash
./mvnw.cmd clean package -DskipTests
```
The compiled JAR will be output at:
`target/ReminderWindows-1.0-SNAPSHOT.jar`

## Detailed Configuration Guide
For properties configuration, directory setup, database storage location details, and HTTPS migrations, please refer to [CONFIGURATION.md](CONFIGURATION.md).
