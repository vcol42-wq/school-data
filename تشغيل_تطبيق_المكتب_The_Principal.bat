@echo off
chcp 65001 >nul
title تشغيل تطبيق الإدارة المدرسية الموحد (The Principal v6.5 Super Edition)

echo ========================================================
echo  جاري تشغيل تطبيق سطح المكتب (The Principal v6.5) ...
echo ========================================================

:: 1. تشغيل أحدث نسخة مفرغة مباشرة من مجلد البناء
if exist "%~dp0extracted_school_system\dist_electron\win-unpacked\The Principal v6.5 Super Edition.exe" (
    start "" "%~dp0extracted_school_system\dist_electron\win-unpacked\The Principal v6.5 Super Edition.exe"
    exit /b
)

:: 2. تشغيل النسخة المستقرة من مجلد أحدث الإصدارات
if exist "%~dp0LATEST_BUILDS\The_Principal_v6_Desktop_App\The Principal v6.5 Super Edition.exe" (
    start "" "%~dp0LATEST_BUILDS\The_Principal_v6_Desktop_App\The Principal v6.5 Super Edition.exe"
    exit /b
)

:: 3. تشغيل النسخة المحمولة المستقلة
if exist "%~dp0extracted_school_system\dist_electron\The_Principal_Portable_v6.5.0.exe" (
    start "" "%~dp0extracted_school_system\dist_electron\The_Principal_Portable_v6.5.0.exe"
    exit /b
)

if exist "%~dp0LATEST_BUILDS\The_Principal_Portable_v6.5.0.exe" (
    start "" "%~dp0LATEST_BUILDS\The_Principal_Portable_v6.5.0.exe"
    exit /b
)

if exist "%~dp0The_Principal_Portable\The Principal v6.5 Super Edition.exe" (
    start "" "%~dp0The_Principal_Portable\The Principal v6.5 Super Edition.exe"
    exit /b
)

echo [!] لم يتم العثور على مشغل التطبيق. يرجى إعادة البناء.
pause
