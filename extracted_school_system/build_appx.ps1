if ($PSScriptRoot) {
    Set-Location $PSScriptRoot
} elseif (Test-Path "e:\boss\extracted_school_system") {
    Set-Location "e:\boss\extracted_school_system"
} elseif (Test-Path "c:\boss\extracted_school_system") {
    Set-Location "c:\boss\extracted_school_system"
}

Write-Host ">>> [0/7] Cleaning up previous processes..."
Stop-Process -Name "electron", "The Principal v6.4", "The Principal v6.3", "The Principal v6.0" -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 500

Write-Host ">>> [1/7] Running Vite Build (Clean initial state - zero default students)..."
cmd.exe /c "set ELECTRON_RUN_AS_NODE=1 && .\node_modules\electron\dist\electron.exe .\node_modules\vite\bin\vite.js build"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Vite build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host ">>> [2/7] Bundling Internal Node Server (esbuild)..."
cmd.exe /c ".\node_modules\@esbuild\win32-x64\esbuild.exe server.ts --bundle --platform=node --format=cjs --outfile=dist/server.cjs --external:vite --external:electron"
if ($LASTEXITCODE -ne 0) {
    Write-Error "esbuild failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host ">>> [3/7] Preparing Staging Directory..."
Remove-Item -Path "app_staging" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "app_staging" -Force | Out-Null
Copy-Item -Path "dist" -Destination "app_staging\dist" -Recurse -Force
Copy-Item -Path "public" -Destination "app_staging\public" -Recurse -Force
Copy-Item -Path "electron-main.js" -Destination "app_staging\electron-main.js" -Force
Copy-Item -Path "preload.cjs" -Destination "app_staging\preload.cjs" -Force
Copy-Item -Path "package.json" -Destination "app_staging\package.json" -Force
Copy-Item -Path "dist\server.cjs" -Destination "app_staging\server.cjs" -Force

Write-Host ">>> [4/7] Preparing Binaries & Packing ASAR Archive..."
New-Item -ItemType Directory -Path "dist_electron\win-unpacked\resources" -Force | Out-Null
Copy-Item -Path ".\node_modules\electron\dist\*" -Destination "dist_electron\win-unpacked\" -Recurse -Force -Exclude "default_app.asar"
Copy-Item -Path ".\node_modules\electron\dist\electron.exe" -Destination "dist_electron\win-unpacked\The Principal v6.4.exe" -Force
Copy-Item -Path ".\node_modules\electron\dist\electron.exe" -Destination "dist_electron\win-unpacked\The Principal v6.4 Super Edition.exe" -Force
cmd.exe /c "set ELECTRON_RUN_AS_NODE=1 && .\node_modules\electron\dist\electron.exe .\node_modules\@electron\asar\bin\asar.js pack .\app_staging .\dist_electron\win-unpacked\resources\app.asar"
if ($LASTEXITCODE -ne 0) {
    Write-Error "ASAR packaging failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host ">>> [5/7] Building Microsoft Store Package (.appx)..."
cmd.exe /c "set ELECTRON_RUN_AS_NODE=1 && .\node_modules\electron\dist\electron.exe .\scripts\build_store.cjs"
if ($LASTEXITCODE -ne 0) {
    Write-Error "APPX build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host ">>> [6/7] Building Standalone Trial Setup Installer (.exe)..."
cmd.exe /c "set ELECTRON_RUN_AS_NODE=1 && .\node_modules\electron\dist\electron.exe .\scripts\run_builder.cjs"
if ($LASTEXITCODE -ne 0) {
    Write-Error "NSIS build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host ">>> [7/7] Exporting Builds to RELEASE_DESKTOP_V6 and dist_releases..."
New-Item -ItemType Directory -Path "..\RELEASE_DESKTOP_V6" -Force | Out-Null
New-Item -ItemType Directory -Path "..\dist_releases" -Force | Out-Null

$appxFiles = Get-ChildItem -Path "dist_electron\*MicrosoftStore.appx" -ErrorAction SilentlyContinue
foreach ($f in $appxFiles) {
    Copy-Item -Path $f.FullName -Destination "..\RELEASE_DESKTOP_V6\$($f.Name)" -Force
    Copy-Item -Path $f.FullName -Destination "..\dist_releases\$($f.Name)" -Force
    $sizeMB = [math]::Round($f.Length / 1MB, 2)
    Write-Host "SUCCESS: Exported $($f.Name) ($sizeMB MB)"
}

$setupFiles = Get-ChildItem -Path "dist_electron\*Setup*.exe" -ErrorAction SilentlyContinue
foreach ($f in $setupFiles) {
    Copy-Item -Path $f.FullName -Destination "..\RELEASE_DESKTOP_V6\$($f.Name)" -Force
    Copy-Item -Path $f.FullName -Destination "..\dist_releases\$($f.Name)" -Force
    $sizeMB = [math]::Round($f.Length / 1MB, 2)
    Write-Host "SUCCESS: Exported $($f.Name) ($sizeMB MB)"
}

# Clean temporary staging directory after successful build
Remove-Item -Path "app_staging" -Recurse -Force -ErrorAction SilentlyContinue
