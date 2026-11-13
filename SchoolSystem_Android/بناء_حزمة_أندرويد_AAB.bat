@echo off
chcp 65001 > nul
title بناء حزمة أندرويد لمتجر جوجل بلاي (AAB v7.4.1 - كود 19 - API 36)
color 0b
echo ==============================================================================
echo        بناء حزمة أندرويد الرسمية (AAB) لمتجر Google Play - إصدار 7.4.1 (كود 19)
echo        متوافقة مع أحدث معايير جوجل بلاي (Target API 36)
echo ==============================================================================
echo.
cd /d "%~dp0"
echo [1/2] جاري تنظيف وتجميع حزمة Release Bundle v7.4.1...
call gradlew.bat bundleRelease

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==============================================================================
    echo [✓] تم بناء حزمة AAB بنجاح تام!
    echo جاهزة للرفع إلى متجر Google Play.
    echo ==============================================================================
    if exist "app\build\outputs\bundle\release" explorer "app\build\outputs\bundle\release"
) else (
    echo.
    echo [X] تنبيه: حدث خطأ أثناء البناء، يرجى مراجعة رسائل الخطأ أعلاه.
)

pause
