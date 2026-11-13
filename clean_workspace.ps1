# ==============================================================================
# تنظيف شامل للمشروع من ملفات البناء المؤقتة ومخلفات الكاش
# Comprehensive Workspace Cleanup Script (Preserves source code & releases)
# ==============================================================================

$ErrorActionPreference = "SilentlyContinue"
$root = if ($PSScriptRoot) { $PSScriptRoot } else { "e:\boss" }
Set-Location $root

Write-Host ">>> Starting cleanup of temporary build artifacts across all apps..." -ForegroundColor Cyan

# 1. Desktop App Build Residues (extracted_school_system)
$desktopTrash = @(
    "extracted_school_system\app_staging",
    "extracted_school_system\dist",
    "extracted_school_system\dist_electron\win-unpacked",
    "extracted_school_system\dist_electron\__appx-x64",
    "extracted_school_system\coverage",
    "extracted_school_system\.vite",
    "extracted_school_system\scratch_sync.js"
)
foreach ($p in $desktopTrash) {
    $full = Join-Path $root $p
    if (Test-Path $full) {
        Write-Host "[-] Cleaning desktop staging/build: $p" -ForegroundColor Yellow
        Remove-Item -Path $full -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# 2. Android Projects Temporary Build Dirs
$androidProjects = @(
    "SchoolSystem_Android",
    "The_Principal_KeyGen_Admin_Android"
)
foreach ($proj in $androidProjects) {
    $projPath = Join-Path $root $proj
    if (Test-Path $projPath) {
        $subDirs = @(
            "$proj\build",
            "$proj\app\build",
            "$proj\.gradle",
            "$proj\.kotlin",
            "$proj\app\.kotlin",
            "$proj\app\.cxx",
            "$proj\.artifacts"
        )
        foreach ($sd in $subDirs) {
            $target = Join-Path $root $sd
            if (Test-Path $target) {
                Write-Host "[-] Cleaning Android cache: $sd" -ForegroundColor Yellow
                Remove-Item -Path $target -Recurse -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

# 3. Log files and temp images
$logPatterns = @(
    "*.log",
    "extracted_school_system\*.log",
    "SchoolSystem_Android\*.log",
    "test_screen.png"
)
foreach ($pat in $logPatterns) {
    Get-ChildItem -Path (Join-Path $root $pat) -File -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "[-] Removing temp file: $($_.FullName)" -ForegroundColor DarkYellow
        Remove-Item -Path $_.FullName -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ">>> Cleanup completed successfully while preserving all source code and final releases!" -ForegroundColor Green
