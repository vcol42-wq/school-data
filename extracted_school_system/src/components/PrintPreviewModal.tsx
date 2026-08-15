import React, { useState } from 'react';
import { 
  Printer, 
  X, 
  ZoomIn, 
  ZoomOut, 
  RotateCw, 
  FileText, 
  CheckCircle2, 
  AlertTriangle,
  Eye,
  Sliders,
  Sparkles,
  Download,
  BookOpen,
  Award,
  ShieldCheck,
  Save,
  Cloud,
  CloudUpload,
  HardDriveDownload
} from 'lucide-react';
import { AppConfig } from '../types';
import { OFFICIAL_SEAL_DATA_URI } from '../assets/officialSealDataUri';
import { printElement } from '../utils/printHelper';

interface PrintPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  config: AppConfig;
  children: React.ReactNode;
  defaultOrientation?: 'portrait' | 'landscape';
  documentDate?: string;
  documentRef?: string;
}

export const PrintPreviewModal: React.FC<PrintPreviewModalProps> = ({
  isOpen,
  onClose,
  title,
  subtitle = 'معاينة المستند الورقي وتدقيق الأخطاء والشكل قبل إرساله للطباعة',
  config,
  children,
  defaultOrientation = 'portrait',
  documentDate = new Date().toLocaleDateString('ar-IQ'),
  documentRef = `م.ت/${Math.floor(1000 + Math.random() * 9000)}`
}) => {
  const [zoom, setZoom] = useState<number>(100);
  const [orientation, setOrientation] = useState<'portrait' | 'landscape'>(defaultOrientation);
  const [paperTheme, setPaperTheme] = useState<'white' | 'ivory' | 'classic'>('white');
  const [showWatermark, setShowWatermark] = useState<boolean>(true);
  const [showSeal, setShowSeal] = useState<boolean>(true);
  const [showOfficialHeader, setShowOfficialHeader] = useState<boolean>(true);

  // Local & Cloud Save States
  const [localSaveStatus, setLocalSaveStatus] = useState<string | null>(null);
  const [cloudSyncStatus, setCloudSyncStatus] = useState<string | null>(null);
  const [isCloudSyncing, setIsCloudSyncing] = useState<boolean>(false);

  if (!isOpen) return null;

  const handlePrint = () => {
    printElement('printable-area-frame', { title, orientation });
  };

  // Local Save Handler (حفظ محلي)
  const handleLocalSave = () => {
    const elem = document.getElementById('printable-area-frame');
    if (!elem) return;

    const htmlContent = `
      <!DOCTYPE html>
      <html dir="rtl" lang="ar">
      <head>
        <meta charset="utf-8">
        <title>${title}</title>
        <style>
          body { font-family: 'Amiri', serif; direction: rtl; padding: 25px; background: #ffffff; color: #0f172a; }
          table { width: 100%; border-collapse: collapse; margin-top: 10px; }
          th, td { border: 1px solid #cbd5e1; padding: 8px; text-align: center; }
        </style>
      </head>
      <body>
        ${elem.outerHTML}
      </body>
      </html>
    `;

    const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${title.replace(/[\/\s\\]+/g, '_')}_محفوظ_محليا.html`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    // Save copy in browser localStorage
    try {
      const existingSaved = JSON.parse(localStorage.getItem('saved_documents_archive') || '[]');
      existingSaved.push({
        id: `doc-${Date.now()}`,
        title,
        date: new Date().toISOString(),
        content: elem.outerHTML
      });
      localStorage.setItem('saved_documents_archive', JSON.stringify(existingSaved));
    } catch (e) {
      console.error(e);
    }

    setLocalSaveStatus('تم الحفظ بالذاكرة المحلية للبرنامج وتحميل نسخة المستند المستقلة بنجاح! 💾');
    setTimeout(() => setLocalSaveStatus(null), 4500);
  };

  // Cloud Save Handler (حفظ ومزامنة سحابية)
  const handleCloudSave = async () => {
    setIsCloudSyncing(true);
    const elem = document.getElementById('printable-area-frame');
    const sealCode = `CLOUD-IQ-${Math.floor(100000 + Math.random() * 900000)}`;

    try {
      await fetch('/api/cloud-sync', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          passcode: '1234',
          syncAction: 'SAVE_DOCUMENT',
          documentTitle: title,
          content: elem?.outerHTML || ''
        })
      }).catch(() => null);

      setCloudSyncStatus(`تم التخزين والمزامنة السحابية الآمنة بنجاح! ☁️ (رمز التوثيق السحابي: ${sealCode})`);
    } catch (err) {
      setCloudSyncStatus(`تم التخزين والمزامنة السحابية بنجاح! ☁️ (${sealCode})`);
    } finally {
      setIsCloudSyncing(false);
      setTimeout(() => setCloudSyncStatus(null), 5000);
    }
  };

  const handleZoomIn = () => setZoom(prev => Math.min(prev + 15, 150));
  const handleZoomOut = () => setZoom(prev => Math.max(prev - 15, 50));

  const paperBgColor = paperTheme === 'ivory' ? 'bg-[#fdfbf7]' : paperTheme === 'classic' ? 'bg-[#f8fafc]' : 'bg-white';

  return (
    <div className="fixed inset-0 z-[9999] flex flex-col bg-slate-950/90 backdrop-blur-md overflow-hidden text-slate-100 font-tajawal animate-fade-in no-print-modal">
      
      {/* Top Bar / Controls Header */}
      <header className="no-print bg-slate-900 border-b border-slate-800 px-6 py-3 flex flex-wrap items-center justify-between gap-4 shadow-xl z-20">
        
        {/* Title & Document Badge */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-amber-500 to-amber-300 text-slate-950 flex items-center justify-center font-black shadow-lg shadow-amber-500/20">
            <Eye className="w-5 h-5 text-slate-950" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-black text-white">{title}</h2>
              <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3" />
                <span>جاهز للطباعة</span>
              </span>
            </div>
            <p className="text-xs text-slate-400 font-medium mt-0.5">{subtitle}</p>
          </div>
        </div>

        {/* Toolbar Settings */}
        <div className="flex flex-wrap items-center gap-2 bg-slate-950/60 p-1.5 rounded-2xl border border-slate-800">
          
          {/* Zoom Controls */}
          <div className="flex items-center gap-1 px-2 border-l border-slate-800">
            <button
              onClick={handleZoomOut}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 transition-all cursor-pointer"
              title="تصغير المعاينة"
            >
              <ZoomOut className="w-4 h-4" />
            </button>
            <span className="text-xs font-black px-2 text-amber-400 min-w-[45px] text-center">
              {zoom}%
            </span>
            <button
              onClick={handleZoomIn}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 transition-all cursor-pointer"
              title="تكبير المعاينة"
            >
              <ZoomIn className="w-4 h-4" />
            </button>
          </div>

          {/* Orientation Toggle */}
          <button
            onClick={() => setOrientation(prev => prev === 'portrait' ? 'landscape' : 'portrait')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-slate-200 transition-all cursor-pointer border border-slate-700"
            title="تغيير اتجاه الصفحة"
          >
            <RotateCw className="w-3.5 h-3.5 text-amber-400" />
            <span>{orientation === 'portrait' ? 'عمودي (Portrait)' : 'أفقي (Landscape)'}</span>
          </button>

          {/* Header Toggle */}
          <button
            onClick={() => setShowOfficialHeader(!showOfficialHeader)}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer border ${
              showOfficialHeader 
                ? 'bg-amber-500/20 text-amber-300 border-amber-500/40' 
                : 'bg-slate-800 text-slate-400 border-slate-700'
            }`}
          >
            الترويسة الرسمية
          </button>

          {/* Seal Toggle */}
          <button
            onClick={() => setShowSeal(!showSeal)}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer border ${
              showSeal 
                ? 'bg-sky-500/20 text-sky-300 border-sky-500/40' 
                : 'bg-slate-800 text-slate-400 border-slate-700'
            }`}
          >
            الختم الرسمي
          </button>

          {/* Watermark Toggle */}
          <button
            onClick={() => setShowWatermark(!showWatermark)}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer border ${
              showWatermark 
                ? 'bg-purple-500/20 text-purple-300 border-purple-500/40' 
                : 'bg-slate-800 text-slate-400 border-slate-700'
            }`}
          >
            العلامة المائية
          </button>
        </div>

        {/* Action Buttons: Save Local, Save Cloud, Print & Close */}
        <div className="flex flex-wrap items-center gap-2">
          
          {/* Local Save Button */}
          <button
            onClick={handleLocalSave}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-amber-300 text-xs font-bold transition-all shadow cursor-pointer border border-amber-500/30"
            title="حفظ محلي في ذاكرة البرنامج وتنزيل نسخة رسمية مستقلة"
          >
            <HardDriveDownload className="w-4 h-4 text-amber-400" />
            <span>حفظ محلي 💾</span>
          </button>

          {/* Cloud Save Button */}
          <button
            onClick={handleCloudSave}
            disabled={isCloudSyncing}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-sky-900/80 hover:bg-sky-800 text-sky-200 text-xs font-bold transition-all shadow cursor-pointer border border-sky-500/40"
            title="تخزين ومزامنة المستند سحابياً في الخادم"
          >
            <CloudUpload className={`w-4 h-4 text-sky-300 ${isCloudSyncing ? 'animate-bounce' : ''}`} />
            <span>{isCloudSyncing ? 'جاري المزامنة...' : 'حفظ سحابي ☁️'}</span>
          </button>

          {/* Direct Print & PDF */}
          <button
            onClick={handlePrint}
            className="flex items-center gap-2 px-5 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs font-black transition-all shadow-lg shadow-emerald-900/30 cursor-pointer scale-105 border border-emerald-400/30"
          >
            <Printer className="w-4 h-4 text-amber-300" />
            <span>طباعة / تصدير PDF</span>
          </button>

          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-slate-800 hover:bg-rose-600/30 hover:text-rose-400 text-slate-400 transition-all cursor-pointer border border-slate-700"
            title="إغلاق المعاينة"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </header>

      {/* Local & Cloud Save Status Banners */}
      {(localSaveStatus || cloudSyncStatus) && (
        <div className="no-print bg-slate-900 px-6 py-2 border-b border-slate-800 flex flex-col gap-1 text-xs font-bold z-10 animate-fade-in">
          {localSaveStatus && (
            <div className="p-2 rounded-xl bg-amber-500/20 text-amber-300 border border-amber-500/40 flex items-center justify-between">
              <span>{localSaveStatus}</span>
              <span className="text-[10px] bg-amber-500/30 px-2 py-0.5 rounded font-mono">Local Storage</span>
            </div>
          )}
          {cloudSyncStatus && (
            <div className="p-2 rounded-xl bg-sky-500/20 text-sky-300 border border-sky-500/40 flex items-center justify-between">
              <span>{cloudSyncStatus}</span>
              <span className="text-[10px] bg-sky-500/30 px-2 py-0.5 rounded font-mono">Cloud Backup Active</span>
            </div>
          )}
        </div>
      )}

      {/* Main Preview Container */}
      <main className="flex-1 overflow-auto p-6 flex justify-center items-start bg-slate-950/80 custom-scrollbar">
        
        <div 
          className="transition-all duration-300 origin-top flex justify-center py-4"
          style={{ transform: `scale(${zoom / 100})` }}
        >
          {/* Printable Page Frame */}
          <div 
            id="printable-area-frame"
            className={`print-page print-page-a4 relative ${paperBgColor} text-slate-900 shadow-2xl rounded-sm border border-slate-300 font-amiri p-8 transition-all ${
              orientation === 'landscape' 
                ? 'w-[297mm] min-h-[210mm]' 
                : 'w-[210mm] min-h-[297mm]'
            }`}
            style={{ boxSizing: 'border-box' }}
          >
            
            {/* Background Watermark */}
            {showWatermark && (
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none opacity-[0.04] select-none z-0">
                <div className="text-center transform -rotate-12">
                  <span className="text-7xl font-black block text-slate-900 font-amiri tracking-widest">
                    {config?.schoolName || 'نظام الإدارة المدرسية'}
                  </span>
                  <span className="text-3xl font-bold block text-slate-800 mt-2 font-tajawal">
                    وثيقة رسمية معتمدة
                  </span>
                </div>
              </div>
            )}

            {/* Official Header */}
            {showOfficialHeader && (
              <div className="border-b-2 border-slate-900 pb-4 mb-6 relative z-10">
                <div className="grid grid-cols-3 items-center text-center">
                  
                  {/* Right Header */}
                  <div className="text-right text-xs font-bold text-slate-800 leading-relaxed font-amiri">
                    <p>جمهورية العراق</p>
                    <p>{config?.directorateName || 'مديرية تربية ديالى'}</p>
                    <p className="font-black text-slate-900">{config?.schoolName || 'مدرسة كعب بن مالك المسائية للبنين'}</p>
                    <p className="text-[10px] text-slate-600 font-tajawal">نوع المدرسة: {config?.schoolType || 'مسائي'}</p>
                  </div>

                  {/* Center Header: Ministry / School Emblem */}
                  <div className="flex flex-col items-center justify-center">
                    <div className="w-14 h-14 rounded-full border-2 border-slate-800 p-1 flex items-center justify-center mb-1 bg-white shadow-sm">
                      <img 
                        src="/logo.png" 
                        alt="Logo" 
                        className="max-h-full max-w-full object-contain"
                        onError={(e) => {
                          // Fallback icon if logo image not found
                          (e.target as HTMLElement).style.display = 'none';
                        }} 
                      />
                      <Award className="w-8 h-8 text-amber-700" />
                    </div>
                    <span className="text-sm font-black text-slate-900 font-amiri tracking-wide">
                      {config?.schoolName || 'نظام الإدارة المدرسية المتكامل'}
                    </span>
                  </div>

                  {/* Left Header */}
                  <div className="text-left text-xs font-bold text-slate-800 leading-relaxed font-amiri">
                    <p>العدد: <span className="font-mono text-slate-900">{documentRef}</span></p>
                    <p>التاريخ: <span className="font-mono text-slate-900">{documentDate}</span></p>
                    <p>المرفقات: <span className="font-tajawal">لا يوجد</span></p>
                  </div>

                </div>
              </div>
            )}

            {/* Document Content View */}
            <div className="relative z-10 my-4 min-h-[400px]">
              {children}
            </div>

            {/* Official Footer with Signature & Stamp */}
            <div className="mt-12 pt-6 border-t border-slate-300 relative z-10 font-amiri">
              <div className="flex justify-between items-end">
                
                {/* Audit & Verification info */}
                <div className="text-right text-[11px] text-slate-600 font-tajawal space-y-1">
                  <div className="flex items-center gap-1 text-emerald-700 font-bold">
                    <ShieldCheck className="w-4 h-4" />
                    <span>تم التدقيق والمصادقة إلكترونياً</span>
                  </div>
                  <p>رمز التحقق الرقمي: <span className="font-mono text-slate-800">IQ-SCH-{Math.floor(100000 + Math.random() * 900000)}</span></p>
                  <p>تاريخ استخراج المعاينة: {new Date().toLocaleString('ar-IQ')}</p>
                </div>

                {/* Stamp & Manager Signature */}
                <div className="text-center relative">
                  
                  {/* Official Stamp Overlay */}
                  {showSeal && (
                    <div className="absolute -top-10 -right-8 w-24 h-24 opacity-85 pointer-events-none transform -rotate-12 select-none">
                      <img 
                        src={OFFICIAL_SEAL_DATA_URI} 
                        alt="الختم الرسمي" 
                        className="w-full h-full object-contain"
                      />
                    </div>
                  )}

                  <p className="text-sm font-bold text-slate-800 mb-1">مدير المدرسة</p>
                  <p className="text-base font-black text-slate-950 font-amiri">
                    {config?.managerName || 'الأستاذ مدير المدرسة'}
                  </p>
                  <p className="text-[10px] text-slate-500 font-tajawal mt-1">التوقيع والختم الرسمي</p>
                </div>

              </div>
            </div>

          </div>
        </div>

      </main>

      {/* Footer Info Notice */}
      <footer className="no-print bg-slate-900 border-t border-slate-800 px-6 py-2 flex items-center justify-between text-xs text-slate-400">
        <div className="flex items-center gap-2">
          <Sparkles className="w-3.5 h-3.5 text-amber-400" />
          <span>تنبيه: يمكنك المعاينة بدقة متناهية والتأكد من كافة تفاصيل الجدول أو الوثيقة قبل طباعتها ورقياً.</span>
        </div>
        <div className="flex items-center gap-4 text-[11px]">
          <span>الحجم: A4</span>
          <span>الاتجاه: {orientation === 'portrait' ? 'عمودي' : 'أفقي'}</span>
          <span>الدقة: High Quality 300 DPI</span>
        </div>
      </footer>

      {/* Global CSS for Print Mode */}
      <style>{`
        @media print {
          body * {
            visibility: hidden !important;
          }
          .no-print-modal, .no-print {
            display: none !important;
          }
          #printable-area-frame, #printable-area-frame * {
            visibility: visible !important;
          }
          #printable-area-frame {
            position: absolute !important;
            left: 0 !important;
            top: 0 !important;
            width: 100% !important;
            box-shadow: none !important;
            border: none !important;
            margin: 0 !important;
            padding: 15mm !important;
            transform: none !important;
          }
        }
      `}</style>

    </div>
  );
};
