import { app, BrowserWindow, ipcMain, dialog, shell } from 'electron';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function createWindow() {
  const appIconPath = path.join(__dirname, 'public', 'logo.png');
  const fallbackIconPath = path.join(__dirname, 'build', 'icon.png');
  const iconPath = fs.existsSync(appIconPath) ? appIconPath : fallbackIconPath;

  const win = new BrowserWindow({
    width: 1300,
    height: 850,
    title: 'نظام الإدارة المدرسية المتكامل - The Principal',
    icon: iconPath,
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  if (fs.existsSync(iconPath)) {
    win.setIcon(iconPath);
  }

  win.loadFile(path.join(__dirname, 'dist', 'index.html'));
}

// IPC Handlers for external print file chooser
ipcMain.handle('select-external-print-file', async () => {
  const { canceled, filePaths } = await dialog.showOpenDialog({
    title: 'اختر ملف الطباعة الخارجي الخاص بك',
    properties: ['openFile'],
    filters: [
      { name: 'مستندات وتطبيقات', extensions: ['exe', 'pdf', 'docx', 'doc', 'txt', 'bat'] },
      { name: 'جميع الملفات', extensions: ['*'] }
    ]
  });
  if (canceled || filePaths.length === 0) return null;
  return filePaths[0];
});

ipcMain.handle('open-external-print-file', async (event, filePath) => {
  if (filePath && fs.existsSync(filePath)) {
    await shell.openPath(filePath);
    return true;
  }
  return false;
});

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
