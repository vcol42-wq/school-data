import fs from 'fs';
import path from 'path';
import child_process from 'child_process';

const docxPath = path.resolve('c:/the boss/ارشيف طباعة.docx');

if (!fs.existsSync(docxPath)) {
  console.log('File not found:', docxPath);
  process.exit(1);
}

console.log('File exists, size:', fs.statSync(docxPath).size);

// Read docx using powershell or node to unpack document.xml
const script = `
$path = "c:\\the boss\\ارشيف طباعة.docx"
$target = "$env:TEMP\\docx_unpack"
if (Test-Path $target) { Remove-Item -Recurse -Force $target }
Expand-Archive -Path $path -DestinationPath $target
Get-ChildItem -Path "$target\\word" -Recurse | Select-Object FullName
`;

fs.writeFileSync('scripts/unpack.ps1', script, 'utf-8');
console.log('Wrote unpack.ps1');
