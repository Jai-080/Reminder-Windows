# Configuration Guide — Windows Desktop Application

This guide outlines the configuration options, token storage details, and local directory setups for the Windows desktop client.

---

## 1. Server URL Setup

By default, the client is configured with a placeholder server address inside `src/main/java/com/reminder/desktop/config/ServerConfig.java`:

* **Default Placeholder**: `http://your-server-address:50000/`

To set your actual backend server IP or hostname:
1. Open [ServerConfig.java](src/main/java/com/reminder/desktop/config/ServerConfig.java).
2. Modify the `SERVER_URL` constant:
   ```java
   public static final String SERVER_URL = "http://your-server-domain-or-ip:port/";
   ```
3. Alternatively, you can override the base server URL in-app via the **Server URL** input field on the Login/Registration screens. This value is persisted in secure local storage.

---

## 2. Local Database & Directory Setup

* **Database Type**: SQLite
* **Database Path**: Resolves dynamically to `%LOCALAPPDATA%\Reminder\reminder.db`. If `%LOCALAPPDATA%` is missing, it falls back to `{user.home}/Reminder/reminder.db`.
* **Database Files Ignores**: All database files (including WAL and journaling temp files) are automatically ignored by Git inside `.gitignore`:
  ```
  reminder.db
  *.db
  *.db-journal
  *.db-wal
  *.db-shm
  ```

---

## 3. Session Token Storage (Security Warning)

* **Location**: Properties are written to the file `{user.home}/.reminder_desktop.properties`.
* **State**: Access and Refresh tokens are saved in **plain text**.
* **Recommendation**: If deploying on shared machines, ensure access permissions to `{user.home}/` are restricted. In future versions, local storage will support native Windows credential encryption (DPAPI).
