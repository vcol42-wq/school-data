@echo off
chcp 65001 >nul
echo ======================================================================
echo   The Principal Master KeyGen - بناء تطبيق إصدار التراخيص للأندرويد
echo ======================================================================
echo.
echo جاري تجميع تطبيق الأندرويد المستقل...
call .\gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ======================================================================
    echo   ✓ تم بناء تطبيق الـ APK بنجاح تام!
    echo   مسار الملف:
    echo   app\build\outputs\apk\debug\app-admin-debug.apk
    echo ======================================================================
    if not exist "..\apk_release" mkdir "..\apk_release"
    copy "app\build\outputs\apk\debug\app-admin-debug.apk" "..\apk_release\The_Principal_KeyGen_Master_v1.0.apk" /y >nul
    copy "app\build\outputs\apk\debug\app-admin-debug.apk" ".\The_Principal_KeyGen_Master_v1.0.apk" /y >nul
    echo تم نسخ الـ APK إلى:
    echo The_Principal_KeyGen_Master_v1.0.apk
) else (
    echo.
    echo [خطأ] فشل تجميع التطبيق، يرجى مراجعة سجلات البناء.
)
pause
