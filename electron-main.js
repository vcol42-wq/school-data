import { app, BrowserWindow, ipcMain, dialog, shell } from 'electron';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';
// Set production environment if packaged
if (app.isPackaged) {
  process.env.NODE_ENV = 'production';
}

// Start the server with error handling
try {
  await import('./dist/server.cjs');
} catch (error) {
  console.error('Failed to start internal server:', error);
  app.whenReady().then(() => {
    dialog.showErrorBox(
      'خطأ في تشغيل النظام',
      'فشل تشغيل خادم البيانات الداخلي. قد يكون هناك ملفات مفقودة.\n' + error.message
    );
  });
}

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

  win.loadURL('http://localhost:3000');
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
