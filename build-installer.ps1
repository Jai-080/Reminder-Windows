# build-installer.ps1
# Automates the build and installer generation of Reminder Desktop

$ErrorActionPreference = "Stop"

# 1. Download WiX Toolset if not present
$wixDir = Join-Path $PSScriptRoot "wix-bin"
$wixZip = Join-Path $PSScriptRoot "wix314-binaries.zip"
$candlePath = Join-Path $wixDir "candle.exe"

if (-not (Test-Path $candlePath)) {
    Write-Host "WiX Toolset not found. Downloading v3.14.1 binaries..." -ForegroundColor Cyan
    if (-not (Test-Path $wixDir)) {
        New-Item -ItemType Directory -Path $wixDir | Out-Null
    }
    
    $source = 'https://github.com/wixtoolset/wix3/releases/download/wix314rtm/wix314-binaries.zip'
    Invoke-WebRequest -Uri $source -OutFile $wixZip
    
    Write-Host "Extracting WiX Toolset..." -ForegroundColor Cyan
    Expand-Archive -Path $wixZip -DestinationPath $wixDir -Force
    Remove-Item -Path $wixZip -Force
    Write-Host "WiX Toolset successfully set up at: $wixDir" -ForegroundColor Green
} else {
    Write-Host "WiX Toolset found at: $wixDir" -ForegroundColor Green
}

# 2. Add WiX to path temporarily for this session
$env:PATH = "$wixDir;" + $env:PATH

# 3. Clean and Package Application using Maven
Write-Host "Building application with Maven..." -ForegroundColor Cyan
& .\mvnw.cmd clean package -DskipTests

# 4. Copy dependency jars to target/libs
Write-Host "Copying dependencies to target/libs..." -ForegroundColor Cyan
& .\mvnw.cmd dependency:copy-dependencies -DoutputDirectory=target/libs

# 5. Copy main application jar to target/libs
Write-Host "Copying main application jar to target/libs..." -ForegroundColor Cyan
Copy-Item -Path "target\ReminderWindows-1.0-SNAPSHOT.jar" -Destination "target\libs\ReminderWindows-1.0-SNAPSHOT.jar" -Force

# 6. Locate jpackage
$jpackagePath = "jpackage"
if (Test-Path "C:\Program Files\Java\jdk-24\bin\jpackage.exe") {
    $jpackagePath = "C:\Program Files\Java\jdk-24\bin\jpackage.exe"
} elseif (Get-Command jpackage -ErrorAction SilentlyContinue) {
    $jpackagePath = "jpackage"
} else {
    $jdks = Get-ChildItem "C:\Program Files\Java" -Filter "jdk-*" -ErrorAction SilentlyContinue
    if ($jdks.Count -gt 0) {
        $latestJdk = $jdks | Sort-Object Name -Descending | Select-Object -First 1
        $jpackagePath = Join-Path $latestJdk.FullName "bin\jpackage.exe"
    }
}

Write-Host "Using jpackage: $jpackagePath" -ForegroundColor Cyan

# 7. Create destination directory if not exists
$installerDest = Join-Path $PSScriptRoot "target\installer"
if (-not (Test-Path $installerDest)) {
    New-Item -ItemType Directory -Path $installerDest | Out-Null
}

# 8. Run jpackage
Write-Host "Generating MSI installer..." -ForegroundColor Cyan
& $jpackagePath `
  --type msi `
  --dest "$installerDest" `
  --input "target\libs" `
  --main-jar "ReminderWindows-1.0-SNAPSHOT.jar" `
  --main-class com.reminder.desktop.Launcher `
  --name "Reminder" `
  --icon "src\main\resources\ic_launcher.ico" `
  --app-version "1.2.0" `
  --vendor "Jai" `
  --win-menu `
  --win-shortcut `
  --win-dir-chooser `
  --win-per-user-install

Write-Host "Build completed successfully! Installer generated in target/installer/." -ForegroundColor Green
