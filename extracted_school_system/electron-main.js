import { app, BrowserWindow, ipcMain, dialog, shell } from 'electron';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

// Set production environment if packaged
if (app.isPackaged) {
  process.env.NODE_ENV = 'production';
}

app.commandLine.appendSwitch('allow-file-access-from-files');
app.commandLine.appendSwitch('disable-web-security');
app.commandLine.appendSwitch('disable-site-isolation-trials');

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Start the internal API server safely in background without blocking UI
try {
  const serverPath = path.join(__dirname, 'dist', 'server.cjs');
  const localServerPath = path.join(__dirname, 'server.cjs');
  if (fs.existsSync(serverPath)) {
    import(serverPath).catch(() => {});
  } else if (fs.existsSync(localServerPath)) {
    import(localServerPath).catch(() => {});
  }
} catch (error) {}

function createWindow() {
  const appIconPath = path.join(__dirname, 'public', 'logo.png');
  const fallbackIconPath = path.join(__dirname, 'build', 'icon.png');
  const iconPath = fs.existsSync(appIconPath) ? appIconPath : fallbackIconPath;
  const preloadPath = path.join(__dirname, 'preload.cjs');

  const win = new BrowserWindow({
    width: 1320,
    height: 860,
    minWidth: 1024,
    minHeight: 700,
    title: 'نظام الإدارة المدرسية المتكامل - The Principal v6.0 Super Edition',
    icon: iconPath,
    backgroundColor: '#f8fafc',
    show: true,
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: false,
      webSecurity: false,
      allowRunningInsecureContent: true,
      preload: fs.existsSync(preloadPath) ? preloadPath : undefined
    }
  });

  if (fs.existsSync(iconPath)) {
    win.setIcon(iconPath);
  }

  // Determine the best file/URL to load
  const distIndexPath = path.join(__dirname, 'dist', 'index.html');
  const rootIndexPath = path.join(__dirname, 'index.html');

  if (fs.existsSync(distIndexPath)) {
    win.loadFile(distIndexPath);
  } else if (fs.existsSync(rootIndexPath)) {
    win.loadFile(rootIndexPath);
  } else {
    win.loadURL('http://localhost:3000');
  }

  // Handle load failure with automatic fallback
  win.webContents.on('did-fail-load', (event, errorCode, errorDescription) => {
    if (fs.existsSync(distIndexPath)) {
      win.loadFile(distIndexPath);
    }
  });
}

// IPC Handlers for external print file chooser
ipcMain.handle('select-external-print-file', async () => {
  try {
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
  } catch (e) {
    return null;
  }
});

ipcMain.handle('open-external-print-file', async (event, filePath) => {
  try {
    if (filePath && fs.existsSync(filePath)) {
      await shell.openPath(filePath);
      return true;
    }
  } catch (e) {}
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
