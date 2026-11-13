@echo off
chcp 65001 > nul
title بناء حزمة متجر مايكروسوفت للتحديث v7.0.1 (.APPX)
color 0b
echo ==============================================================================
echo        [2] نظام الإدارة المدرسية الموحد - بناء حزمة متجر مايكروسوفت (APPX)
echo        الإصدار: v7.0.1 Super Edition (جاهز للتحديث في Partner Center)
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
echo [3/3] حزم ملف التحديث لمتجر مايكروسوفت ستور (.appx)...
call npx electron-builder --win appx

if %ERRORLEVEL% EQU 0 (
    color 0a
    echo.
    echo ==============================================================================
    echo [✓] تم بناء حزمة متجر مايكروسوفت بنجاح تام!
    echo.
    echo اسم الحزمة:
    echo    The_Principal_v7.0.1_MicrosoftStore.appx
    echo.
    echo هذه الحزمة جاهزة وموقعة مباشرة للرفع إلى:
    echo Microsoft Partner Center -> Packages
    echo.
    echo مجلد الحزمة الناتج: %~dp0dist_electron
    echo ==============================================================================
    explorer "%~dp0dist_electron"
) else (
    color 0c
    echo [X] فشل تحزيم AppX.
)
pause
