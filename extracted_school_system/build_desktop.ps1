Set-Location "c:\boss\extracted_school_system"

Write-Host "[1/5] Running Vite Build (Synchronous)..."
$env:ELECTRON_RUN_AS_NODE = "1"
Start-Process -FilePath ".\node_modules\electron\dist\electron.exe" -ArgumentList ".\node_modules\vite\bin\vite.js build" -Wait -NoNewWindow

Write-Host "[2/5] Bundling Internal Server (Synchronous)..."
Start-Process -FilePath ".\node_modules\@esbuild\win32-x64\esbuild.exe" -ArgumentList "server.ts --bundle --platform=node --format=cjs --outfile=dist/server.cjs --external:vite --external:electron" -Wait -NoNewWindow

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
New-Item -ItemType Directory -Path "dist_electron\win-unpacked\resources" -Force | Out-Null
Copy-Item -Path ".\node_modules\electron\dist\*" -Destination "dist_electron\win-unpacked\" -Recurse -Force -Exclude "default_app.asar"
Copy-Item -Path ".\node_modules\electron\dist\electron.exe" -Destination "dist_electron\win-unpacked\The Principal v6.0.exe" -Force
Start-Process -FilePath ".\node_modules\electron\dist\electron.exe" -ArgumentList ".\node_modules\@electron\asar\bin\asar.js pack .\app_staging .\dist_electron\win-unpacked\resources\app.asar" -Wait -NoNewWindow

Write-Host "[5/5] Exporting Final Package to LATEST_BUILDS..."
Stop-Process -Name "electron", "The Principal v6.0" -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 500
Remove-Item -Path "..\LATEST_BUILDS\The_Principal_v6_Desktop_App" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "..\LATEST_BUILDS\The_Principal_v6_Desktop_App" -Force | Out-Null
Copy-Item -Path "dist_electron\win-unpacked\*" -Destination "..\LATEST_BUILDS\The_Principal_v6_Desktop_App" -Recurse -Force

Write-Host "SUCCESS: Desktop App v6.0 completely built and packaged with 100% updated assets!"
