import React, { useState } from 'react';
import { AppConfig } from '../types';
import { 
  Laptop, 
  Code2, 
  Download, 
  Terminal, 
  ShieldCheck, 
  Cpu, 
  Copy, 
  Check, 
  Sparkles, 
  ExternalLink, 
  Layers, 
  Box, 
  CheckCircle2, 
  Server,
  Smartphone
} from 'lucide-react';

interface DesktopGuideViewProps {
  config: AppConfig;
}

export const DesktopGuideView: React.FC<DesktopGuideViewProps> = ({ config }) => {
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  const handleCopy = (code: string, index: number) => {
    navigator.clipboard.writeText(code);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const electronPackageJson = `{
  "name": "${config.schoolName.replace(/\s+/g, '-').toLowerCase()}-desktop",
  "version": "1.0.0",
  "main": "electron-main.js",
  "scripts": {
    "start": "electron .",
    "build:win": "electron-builder --win nsis",
    "build:mac": "electron-builder --mac",
    "build:linux": "electron-builder --linux AppImage"
  },
  "devDependencies": {
    "electron": "^30.0.0",
    "electron-builder": "^24.13.0"
  }
}`;

  const electronMainJs = `const { app, BrowserWindow } = require('electron');
const path = require('path');

function createWindow() {
  const mainWindow = new BrowserWindow({
    width: 1366,
    height: 768,
    title: "${config.schoolName} - نظام الإدارة المدرسية",
    icon: path.join(__dirname, 'build/icon.ico'),
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  // Load the built Vite index.html file
  mainWindow.loadFile(path.join(__dirname, 'dist/index.html'));
  mainWindow.setMenuBarVisibility(false); // Hide default menu
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});`;

  return (
    <div className="max-w-6xl mx-auto px-4 py-6 space-y-6">
      
      {/* Header Banner */}
      <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-md relative overflow-hidden">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 relative z-10">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300 text-xs font-bold mb-2 border border-indigo-200 dark:border-indigo-800">
              <Laptop className="w-4 h-4" />
              <span>دليل تحويل التطبيق إلى برنامج سطح مكتب خامل ومستقل (Offline Desktop EXE)</span>
            </div>
            <h2 className="text-2xl font-black text-[var(--theme-text-main)]">
              لغة البرمجة وكيفية التحويل إلى تطبيق سطح مكتب حقيقي
            </h2>
            <p className="text-xs text-[var(--theme-text-muted)] mt-1 max-w-2xl">
              هذا المنشور يوضح بالتفصيل التقنيات المعتمدة وطريقة تحويل المشروع الحالية إلى برنامج مثبت يعمل على الويندوز (.exe) دون الحاجة للإنترنت، مع المزامنة مع الأندرويد.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <div className="p-3 bg-amber-500/10 text-amber-600 rounded-2xl border border-amber-500/20 text-center">
              <span className="block text-[10px] font-bold">حالة النظام</span>
              <span className="text-xs font-black">جاهز للتصدير المباشر</span>
            </div>
          </div>
        </div>
      </div>

      {/* Tech Stack Explanation Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        
        <div className="bg-[var(--theme-card)] p-5 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-3">
          <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300 flex items-center justify-center font-bold">
            <Code2 className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[var(--theme-text-main)]">1. لغة البرمجة والأكواد</h3>
          <ul className="text-xs space-y-1.5 text-slate-600 dark:text-slate-300 list-disc list-inside">
            <li><strong className="text-slate-900 dark:text-white">TypeScript / JavaScript:</strong> لغة الأكواد الرئيسية لضمان الأمان ودقة البيانات.</li>
            <li><strong className="text-slate-900 dark:text-white">React 19 Framework:</strong> محرك الواجهات والتفاعل السريع.</li>
            <li><strong className="text-slate-900 dark:text-white">Tailwind CSS 4:</strong> للتنسيقات والألوان والأزرار الاحترافية.</li>
          </ul>
        </div>

        <div className="bg-[var(--theme-card)] p-5 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300 flex items-center justify-center font-bold">
            <Server className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[var(--theme-text-main)]">2. المحرك السحابي والخلفي</h3>
          <ul className="text-xs space-y-1.5 text-slate-600 dark:text-slate-300 list-disc list-inside">
            <li><strong className="text-slate-900 dark:text-white">Express.js (Node.js):</strong> خادم محلي وسحابي لإدارة البيانات والطلبات.</li>
            <li><strong className="text-slate-900 dark:text-white">Gemini 2.5 Flash AI:</strong> محرك الذكاء الاصطناعي لقراءة السجلات الورقية (OCR).</li>
            <li><strong className="text-slate-900 dark:text-white">LocalStorage / Firestore:</strong> حفظ دائم وفوري لقواعد البيانات.</li>
          </ul>
        </div>

        <div className="bg-[var(--theme-card)] p-5 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-3">
          <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300 flex items-center justify-center font-bold">
            <Smartphone className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[var(--theme-text-main)]">3. الربط مع تطبيق الأندرويد</h3>
          <ul className="text-xs space-y-1.5 text-slate-600 dark:text-slate-300 list-disc list-inside">
            <li><strong className="text-slate-900 dark:text-white">Cloud Firebase / REST API:</strong> مزامنة فورية بختم رمزي (Seal Token).</li>
            <li><strong className="text-slate-900 dark:text-white">رمز مدير المدرسة:</strong> حماية عملية قفل الدرجات ومنع التغيير.</li>
            <li><strong className="text-slate-900 dark:text-white">نقطة اللا عودة:</strong> حفظ وتأكيد الدرجات بنقرة واحدة.</li>
          </ul>
        </div>

      </div>

      {/* STEP BY STEP GUIDE TO CONVERT TO DESKTOP .EXE */}
      <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-lg space-y-6">
        
        <div className="border-b pb-3 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-black text-[var(--theme-text-main)] flex items-center gap-2">
              <Box className="w-5 h-5 text-indigo-600" />
              <span>خطوات تحويل التطبيق الحالية إلى ملف تنفيذي (.EXE) حقيقي للويندوز</span>
            </h3>
            <p className="text-xs text-[var(--theme-text-muted)] mt-0.5">
              باستخدام بيئة <strong className="text-indigo-600">Electron.js</strong> المعتمدة عالمياً لتطبيقات سطح المكتب (مثل Visual Studio Code و Slack)
            </p>
          </div>
        </div>

        {/* Step 1 */}
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-xs font-black text-indigo-700 dark:text-indigo-300">
            <span className="w-6 h-6 rounded-full bg-indigo-100 dark:bg-indigo-900 text-indigo-800 dark:text-indigo-200 flex items-center justify-center font-mono">1</span>
            <span>الخطوة الأولى: تنزيل ملفات المشروع من زر الخيارات (Export ZIP) وتثبيت Electron</span>
          </div>
          <div className="bg-slate-900 text-slate-100 p-4 rounded-xl text-xs font-mono relative dir-ltr">
            <code>npm install --save-dev electron electron-builder</code>
            <button 
              onClick={() => handleCopy('npm install --save-dev electron electron-builder', 1)}
              className="absolute top-3 left-3 p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300"
            >
              {copiedIndex === 1 ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Step 2 */}
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-xs font-black text-indigo-700 dark:text-indigo-300">
            <span className="w-6 h-6 rounded-full bg-indigo-100 dark:bg-indigo-900 text-indigo-800 dark:text-indigo-200 flex items-center justify-center font-mono">2</span>
            <span>الخطوة الثانية: إنشاء ملف التشغيل الرئيسي لسطح المكتب (electron-main.js)</span>
          </div>
          <div className="bg-slate-900 text-slate-100 p-4 rounded-xl text-xs font-mono relative dir-ltr overflow-x-auto">
            <pre>{electronMainJs}</pre>
            <button 
              onClick={() => handleCopy(electronMainJs, 2)}
              className="absolute top-3 left-3 p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300"
            >
              {copiedIndex === 2 ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Step 3 */}
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-xs font-black text-indigo-700 dark:text-indigo-300">
            <span className="w-6 h-6 rounded-full bg-indigo-100 dark:bg-indigo-900 text-indigo-800 dark:text-indigo-200 flex items-center justify-center font-mono">3</span>
            <span>الخطوة الثالثة: تشغيل أمر التجميع وبناء البرنامج التنفيذي (.exe / setup installer)</span>
          </div>
          <div className="bg-slate-900 text-slate-100 p-4 rounded-xl text-xs font-mono relative dir-ltr">
            <code>npm run build && npx electron-builder --win nsis</code>
            <button 
              onClick={() => handleCopy('npm run build && npx electron-builder --win nsis', 3)}
              className="absolute top-3 left-3 p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300"
            >
              {copiedIndex === 3 ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
            </button>
          </div>
          <p className="text-[11px] text-emerald-700 dark:text-emerald-400 font-bold">
            ✨ سينتج ملف حزمة تثبيت للويندوز داخل مجلد dist باسم (School-Management-Setup.exe) يمكن تثبيته بنقرة واحدة على أي جهاز حاسوب بالمدرسة بدون إنترنت.
          </p>
        </div>

      </div>

    </div>
  );
};
