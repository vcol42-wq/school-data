
$path = "c:\the boss\ارشيف طباعة.docx"
$target = "$env:TEMP\docx_unpack"
if (Test-Path $target) { Remove-Item -Recurse -Force $target }
Expand-Archive -Path $path -DestinationPath $target
Get-ChildItem -Path "$target\word" -Recurse | Select-Object FullName
