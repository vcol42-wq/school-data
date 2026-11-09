Set-Location "$PSScriptRoot"

Stop-Process -Name "electron", "The Principal*" -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 500

Write-Host "[1/5] Running Vite Build (Synchronous)..."
cmd.exe /c "set ELECTRON_RUN_AS_NODE=1 && .\node_modules\electron\dist\electron.exe .\node_modules\vite\bin\vite.js build"

Write-Host "[2/5] Bundling Internal Server (Synchronous)..."
cmd.exe /c ".\node_modules\@esbuild\win32-x64\esbuild.exe server.ts --bundle --platform=node --format=cjs --outfile=dist/server.cjs --external:vite --external:electron"

Write-Host "[3/5] Preparing Staging Directory..."
Remove-Item -Path "app_staging" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "app_staging" -Force | Out-Null
Copy-Item -Path "dist" -Destination "app_staging\dist" -Recurse -Force
Copy-Item -Path "public" -Destination "app_staging\public" -Recurse -Force
Copy-Item -Path "electron-main.js" -Destination "app_staging\electron-main.js" -Force
Copy-Item -Path "preload.cjs" -Destination "app_staging\preload.cjs" -Force
Copy-Item -Path "package.json" -Destination "app_staging\package.json" -Force
Copy-Item -Path "dist\server.cjs" -Destination "app_staging\server.cjs" -Force

Write-Host "[4/5] Preparing Binaries & Packing ASAR Archive (Synchronous)..."
New-Item -ItemType Directory -Path "dist_electron\win-unpacked" -Force | Out-Null
Copy-Item -Path ".\node_modules\electron\dist\*" -Destination "dist_electron\win-unpacked\" -Recurse -Force -Exclude "default_app.asar"
Copy-Item -Path ".\node_modules\electron\dist\electron.exe" -Destination "dist_electron\win-unpacked\The Principal v6.5 Super Edition.exe" -Force
Copy-Item -Path ".\node_modules\electron\dist\electron.exe" -Destination "dist_electron\win-unpacked\The Principal v6.5.exe" -Force
Copy-Item -Path ".\node_modules\electron\dist\electron.exe" -Destination "dist_electron\win-unpacked\The Principal v6.4 Super Edition.exe" -Force
Copy-Item -Path ".\node_modules\electron\dist\electron.exe" -Destination "dist_electron\win-unpacked\The Principal v6.0 Super Edition.exe" -Force
Copy-Item -Path ".\node_modules\electron\dist\electron.exe" -Destination "dist_electron\win-unpacked\The Principal v6.0.exe" -Force
cmd.exe /c "set ELECTRON_RUN_AS_NODE=1 && .\node_modules\electron\dist\electron.exe .\node_modules\@electron\asar\bin\asar.js pack .\app_staging .\dist_electron\win-unpacked\resources\app.asar"

Write-Host "[5/6] Exporting Final Package to LATEST_BUILDS..."
Stop-Process -Name "electron", "The Principal*" -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 500
Remove-Item -Path "..\LATEST_BUILDS\The_Principal_v6_Desktop_App" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "..\LATEST_BUILDS\The_Principal_v6_Desktop_App" -Force | Out-Null
Copy-Item -Path "dist_electron\win-unpacked\*" -Destination "..\LATEST_BUILDS\The_Principal_v6_Desktop_App" -Recurse -Force

if (Test-Path "..\The_Principal_Portable") {
    Copy-Item -Path "dist_electron\win-unpacked\resources\app.asar" -Destination "..\The_Principal_Portable\resources\app.asar" -Force
    Copy-Item -Path "dist_electron\win-unpacked\The Principal v6.5 Super Edition.exe" -Destination "..\The_Principal_Portable\The Principal v6.5 Super Edition.exe" -Force
    Copy-Item -Path "dist_electron\win-unpacked\The Principal v6.0.exe" -Destination "..\The_Principal_Portable\The Principal v6.0.exe" -Force
}

Write-Host "[6/6] Packaging Professional NSIS Setup Installer..."
cmd.exe /c "set ELECTRON_RUN_AS_NODE=1 && .\node_modules\electron\dist\electron.exe .\scripts\run_builder.cjs"

$installer = Get-ChildItem -Path "dist_electron" -Filter "The_Principal_Setup_*.exe" | Select-Object -First 1
if ($installer) {
    Write-Host "[EXPORT] Copying NSIS Installer ($($installer.Name)) to LATEST_BUILDS and RELEASE_DESKTOP_V6..."
    New-Item -ItemType Directory -Path "..\LATEST_BUILDS" -Force | Out-Null
    New-Item -ItemType Directory -Path "..\RELEASE_DESKTOP_V6" -Force | Out-Null

    Copy-Item -Path $installer.FullName -Destination "..\LATEST_BUILDS\$($installer.Name)" -Force
    Copy-Item -Path $installer.FullName -Destination "..\RELEASE_DESKTOP_V6\$($installer.Name)" -Force

    # Clean zip archive containing the setup executable
    Write-Host "[ZIP] Creating clean release archive..."
    $zipPath = "..\RELEASE_DESKTOP_V6\$([System.IO.Path]::GetFileNameWithoutExtension($installer.Name)).zip"
    Remove-Item -Path $zipPath -Force -ErrorAction SilentlyContinue
    Compress-Archive -Path $installer.FullName -DestinationPath $zipPath -Force

    Write-Host "SUCCESS: $($installer.Name) ready in RELEASE_DESKTOP_V6!"
} else {
    Write-Host "Note: Standalone unpacked app is ready in LATEST_BUILDS and dist_electron\win-unpacked"
}
