import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

try {
  execSync('taskkill /f /im "The Principal.exe" /im electron.exe 2>nul');
} catch (e) {}

const builtExe = path.resolve('dist_electron/The Principal - بدون تثبيت 1.0.0.exe');
const desktopPath = path.resolve('C:/Users/vcol4/Desktop/The Principal - برنامج مباشر بدون تثبيت.exe');
const parentFolderExe = path.resolve('c:/the boss/The Principal - برنامج مباشر بدون تثبيت.exe');

if (fs.existsSync(builtExe)) {
  try {
    fs.copyFileSync(builtExe, desktopPath);
    console.log('Successfully deployed built executable to Desktop!');
  } catch (err) {
    console.log('Could not copy to desktop (file busy):', err.message);
  }

  try {
    fs.copyFileSync(builtExe, parentFolderExe);
    console.log('Successfully deployed built executable to c:/the boss!');
  } catch (err) {
    console.log('Could not copy to parent folder (file busy):', err.message);
  }
} else {
  console.log('Built executable not found:', builtExe);
}
