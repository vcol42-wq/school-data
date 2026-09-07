@echo off
chcp 65001 >nul
title تشغيل تطبيق الإدارة المدرسية المحمول (The Principal v6.0 Portable)

echo جاري تشغيل تطبيق سطح المكتب (النسخة المحمولة)...

if exist "%~dp0The_Principal_Portable\The Principal v6.0.exe" (
    start "" "%~dp0The_Principal_Portable\The Principal v6.0.exe"
    exit /b
)

if exist "%~dp0LATEST_BUILDS\The_Principal_v6_Desktop_App\The Principal v6.0 Super Edition.exe" (
    start "" "%~dp0LATEST_BUILDS\The_Principal_v6_Desktop_App\The Principal v6.0 Super Edition.exe"
    exit /b
)

echo لم يتم العثور على مشغل التطبيق!
pause
