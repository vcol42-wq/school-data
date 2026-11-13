@echo off
chcp 65001 > nul
title بناء نسخة التجربة السريعة والمثبت v7.0.1 (.EXE)
color 0a
echo ==============================================================================
echo        [1] نظام الإدارة المدرسية الموحد - بناء نسخة التجربة (EXE)
echo        الإصدار: v7.0.1 Super Edition
echo ==============================================================================
echo.
cd /d "%~dp0"

echo [0/3] إغلاق أي نسخة مفتوحة للتطبيق في الخلفية لفك قفل الملفات...
taskkill /f /im "The Principal*" /t >nul 2>nul
taskkill /f /im "electron.exe" /t >nul 2>nul
timeout /t 1 /nobreak >nul 2>nul

if exist dist_electron (
    echo [*] تنظيف مجلد البناء المؤقت dist_electron...
    rd /s /q dist_electron 2>nul
)

echo [1/3] تجميع واجهات النظام (Vite)...
call npx vite build
if %ERRORLEVEL% NEQ 0 (
    color 0c
    echo [X] فشل تجميع Vite.
    pause
    exit /b 1
)

echo.
echo [2/3] تجميع خادم التطبيق الداخلي (esbuild)...
call npx esbuild server.ts --bundle --platform=node --format=cjs --outfile=dist/server.cjs --external:vite --external:electron
if %ERRORLEVEL% NEQ 0 (
    color 0c
    echo [X] فشل تجميع خادم server.ts.
    pause
    exit /b 1
)

echo.
echo [3/3] حزم تطبيق التجربة المحمول (.exe) وملف التثبيت...
call npx electron-builder --win nsis portable

if %ERRORLEVEL% EQU 0 (
    color 0a
    echo.
    echo ==============================================================================
    echo [✓] تم بناء نسخ التجربة بنجاح تام!
    echo.
    echo 1. النسخة المحمولة للتجربة الفورية بدون تثبيت:
    echo    The_Principal_Portable_v7.0.1.exe
    echo.
    echo 2. ملف التثبيت القياسي مع اختصار سطح المكتب:
    echo    The_Principal_Setup_v7.0.1.exe
    echo.
    echo مجلد الملفات الناتج: %~dp0dist_electron
    echo ==============================================================================
    explorer "%~dp0dist_electron"
) else (
    color 0c
    echo [X] فشل التحزيم عبر electron-builder.
)
pause
