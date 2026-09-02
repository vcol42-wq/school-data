@echo off
cd /d "%~dp0"
echo ===================================================
echo Configuring Git buffer sizes to prevent timeouts...
echo ===================================================
git config http.postBuffer 524288000
git config http.maxRequestSize 524288000
git config core.compression 0

echo.
echo ===================================================
echo Pushing commits incrementally to prevent timeouts...
echo ===================================================
echo [1/6] Pushing commit 8447800...
git push origin 8447800:refs/heads/main --force
if %errorlevel% neq 0 goto error

echo [2/6] Pushing commit bb82b12...
git push origin bb82b12:refs/heads/main
if %errorlevel% neq 0 goto error

echo [3/6] Pushing commit a117924...
git push origin a117924:refs/heads/main
if %errorlevel% neq 0 goto error

echo [4/6] Pushing commit 5c6d016...
git push origin 5c6d016:refs/heads/main
if %errorlevel% neq 0 goto error

echo [5/6] Pushing commit d676d88...
git push origin d676d88:refs/heads/main
if %errorlevel% neq 0 goto error

echo [6/6] Pushing final restructured project...
git push -u origin main
if %errorlevel% neq 0 goto error

echo.
echo ===================================================
echo Success! All commits pushed successfully!
echo ===================================================
goto end

:error
echo.
echo ===================================================
echo Error occurred during push. Please check the logs.
echo ===================================================

:end
pause
