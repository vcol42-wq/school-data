const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  selectExternalPrintFile: () => ipcRenderer.invoke('select-external-print-file'),
  openExternalPrintFile: (filePath) => ipcRenderer.invoke('open-external-print-file', filePath)
});

// Fallback compatibility for legacy window.require
try {
  window.require = (moduleName) => {
    if (moduleName === 'electron') {
      return { ipcRenderer };
    }
    throw new Error(`Module ${moduleName} is not available in renderer`);
  };
} catch (e) {}
