# Clean Release Builder for SchoolSystem
$ErrorActionPreference = "Stop"

$ProjectDir = "c:\boss\SchoolSystem_Android"
$OutputDir = "c:\boss\apk_release"
$TargetApkName = "SchoolSystem_Latest.apk"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Starting Clean Build of SchoolSystem Android APK..." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Purge previous APKs in output directory
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    Write-Host "Created output directory: $OutputDir" -ForegroundColor Green
} else {
    Write-Host "Purging all previous APKs in release directory..." -ForegroundColor Yellow
    Get-ChildItem -Path $OutputDir -Filter "*.apk" -ErrorAction SilentlyContinue | Remove-Item -Force
    Write-Host "Old APKs purged." -ForegroundColor Green
}

# 2. Clean gradle APK outputs
$GradleBuildOutput = Join-Path $ProjectDir "app\build\outputs\apk"
if (Test-Path $GradleBuildOutput) {
    Get-ChildItem -Path $GradleBuildOutput -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue | Remove-Item -Force
}

# 3. Execute Gradle clean assembleDebug
Write-Host "Running Gradle clean assembleDebug..." -ForegroundColor Yellow
Set-Location -Path $ProjectDir
$startTime = Get-Date

& .\gradlew.bat clean assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Please check Gradle errors above." -ForegroundColor Red
    exit 1
}

$elapsed = (Get-Date) - $startTime
Write-Host "Gradle build completed successfully in $($elapsed.Seconds) seconds." -ForegroundColor Green

# 4. Copy resulting APK to dedicated release folder
$SourceApk = Join-Path $ProjectDir "app\build\outputs\apk\debug\app-debug.apk"
$DestinationApk = Join-Path $OutputDir $TargetApkName

if (Test-Path $SourceApk) {
    Copy-Item -Path $SourceApk -Destination $DestinationApk -Force
    $fileInfo = Get-Item -Path $DestinationApk
    $fileSizeMB = [math]::Round($fileInfo.Length / 1MB, 2)
    $fileDate = $fileInfo.LastWriteTime.ToString()

    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "Fresh APK Built and Released Successfully!" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "File Path: $DestinationApk" -ForegroundColor White
    Write-Host "File Name: $TargetApkName" -ForegroundColor Cyan
    Write-Host "File Size: $fileSizeMB MB" -ForegroundColor Yellow
    Write-Host "Build Timestamp: $fileDate" -ForegroundColor White
    Write-Host "==========================================================" -ForegroundColor Green
} else {
    Write-Host "Error: Could not find built APK at $SourceApk" -ForegroundColor Red
    exit 1
}
