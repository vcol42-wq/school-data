@echo off
chcp 65001 > nul
title بناء مثبت سطح المكتب The Principal v7.0.1
color 0b
echo ==============================================================================
echo       نظام الإدارة المدرسية الموحد - بناء وتجميع تطبيق سطح المكتب v7.0.1
echo ==============================================================================
echo.
cd /d "%~dp0"

echo [1/3] فحص بيئة Node.js...
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    color 0c
    echo [X] خطأ: لم يتم العثور على Node.js على جهازك! يرجى تثبيت Node.js.
    pause
    exit /b 1
)

echo [2/3] بناء ملفات الويب والخادم (Vite + esbuild)...
call npx vite build
if %ERRORLEVEL% NEQ 0 (
    color 0c
    echo [X] فشل بناء ملفات Vite! راجع رسالة الخطأ أعلاه.
    pause
    exit /b 1
)

call npx esbuild server.ts --bundle --platform=node --format=cjs --outfile=dist/server.cjs --external:vite --external:electron
if %ERRORLEVEL% NEQ 0 (
    color 0c
    echo [X] فشل تجميع خادم server.ts عبر esbuild!
    pause
    exit /b 1
)

echo.
echo [3/3] حزم تطبيق سطح المكتب عبر electron-builder...
call npx electron-builder --win nsis portable

if %ERRORLEVEL% EQU 0 (
    color 0a
    echo.
    echo ==============================================================================
    echo [✓] تم بناء ملف التثبيت بنجاح تام!
    echo مجلد الإخراج: %~dp0dist_electron
    echo ==============================================================================
    explorer "%~dp0dist_electron"
) else (
    color 0c
    echo.
    echo ==============================================================================
    echo [X] فشل أثناء تحزيم electron-builder.
    echo يمكنك تشغيل التطبيق مباشرة للتجربة بالأمر: npm run dev
    echo ==============================================================================
)
pause
