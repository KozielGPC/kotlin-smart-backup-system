# Kotlin Smart Backup System

A smart backup system built with Kotlin and Spring Boot, designed to efficiently manage file and directory backups with integrity checks and a simple REST API.

## Features
- Backup files and directories with SHA-256 integrity checks
- Restore files and directories from backup
- Upload and download files via REST API
- List, delete, and clear backup contents
- Simple configuration via YAML

## Technology Stack
- **Kotlin** (JVM 17)
- **Spring Boot** 3.2.x (Web)
- **Gradle** (build tool)
- **JUnit** (testing)

## Project Structure
```
├── src
│   ├── main
│   │   ├── kotlin
│   │   │   └── br/com/backupkotlin
│   │   │       ├── Main.kt                # Application entry point
│   │   │       ├── BackupManager.kt        # Core backup logic
│   │   │       ├── BackupController.kt     # REST API endpoints
│   │   │       ├── HomeController.kt       # (Optional) Home endpoint
│   │   │       └── WebServerConfiguration.kt # Web server config
│   │   └── resources
│   │       └── application.yml             # App configuration
│   └── test
│       └── kotlin
│           └── br/com/backupkotlin
│               └── Main.kt                # Example test file
├── build.gradle                           # Build configuration
├── settings.gradle                        # Gradle settings
└── README.md                              # Project documentation
```

## How to Run

### Prerequisites
- Java 17+
- Gradle (or use the included `gradlew` wrapper)

### Running the Application

```bash
./gradlew bootRun
```

The server will start on the default port (usually 8080). You can configure ports and other settings in `src/main/resources/application.yml`.

### Building a JAR
```bash
./gradlew build
java -jar build/libs/kotlin-smart-backup-system-0.0.1-SNAPSHOT.jar
```

## API Endpoints

- `POST   /upload?file=<file>` — Upload a file to backup
- `GET    /backup?sourcePath=<path>` — Backup a file or directory
- `GET    /download?sourcePath=<src>&destinationPath=<dst>` — Restore a file
- `GET    /downloadAll?destinationPath=<dst>` — Restore all files
- `GET    /listBackup` — List files and directories in backup
- `GET    /deleteBackup?filePath=<file>` — Delete a file or folder from backup
- `GET    /clearBackup` — Clear the entire backup folder

## Configuration
Edit `src/main/resources/application.yml` to set CORS, environments, and other properties:
```yaml
cors:
  allowed-origins:
    - "https://dev.frontend.com"
    - "http://localhost:8000"

environments:
  dev:
    url: "http://localhost:8000"
    name: "Developer Setup"
  prod:
    url: "http://localhost:8000"
    name: "My Cool App"
```

## Example Usage
- Upload a file:
  ```bash
  curl -F "file=@/path/to/file.txt" http://localhost:8080/upload
  ```
- Backup a directory:
  ```bash
  curl "http://localhost:8080/backup?sourcePath=/path/to/dir"
  ```
- List backup contents:
  ```bash
  curl http://localhost:8080/listBackup
  ```

## Testing
Run tests with:
```bash
./gradlew test
```