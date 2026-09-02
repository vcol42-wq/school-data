import React from 'react';
import { Camera, X, Upload, RefreshCw, ScanLine, Sparkles } from 'lucide-react';

interface OcrModalProps {
  isOpen: boolean;
  onClose: () => void;
  ocrPreviewUrl: string | null;
  setOcrPreviewUrl: (url: string | null) => void;
  setOcrImageFile: (file: File | null) => void;
  isOcrScanning: boolean;
  ocrExtractedStudents: any[];
  onScan: () => void;
  onApply: () => void;
}

export const OcrModal: React.FC<OcrModalProps> = ({
  isOpen,
  onClose,
  ocrPreviewUrl,
  setOcrPreviewUrl,
  setOcrImageFile,
  isOcrScanning,
  ocrExtractedStudents,
  onScan,
  onApply
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white border-2 border-purple-400 rounded-3xl p-6 md:p-8 max-w-2xl w-full shadow-2xl space-y-5 text-slate-900">

        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
          <div className="flex items-center gap-2 text-purple-950 font-black">
            <div className="w-9 h-9 rounded-xl bg-purple-100 text-purple-800 flex items-center justify-center font-black">
              <Camera className="w-5 h-5" />
            </div>
            <span>ماسح السجل الورقي بالذكاء الاصطناعي (Gemini Vision OCR)</span>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-slate-100 text-slate-700 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-slate-700 font-bold">
          قم بالتقاط أو رفع صورة ورقة الدرجات المطبوعة أو السجل الورقي المطابق للجدول، وسيقوم الذكاء الاصطناعي بقراءة أسماء الطلاب والدرجات آلياً.
        </p>

        <div className="space-y-3">
          <div className="border-2 border-dashed border-slate-300 dark:border-slate-700 rounded-2xl p-4 text-center hover:bg-slate-50 dark:hover:bg-slate-900 transition-colors">
            <input
              type="file"
              accept="image/*"
              capture="environment"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) {
                  setOcrImageFile(file);
                  const reader = new FileReader();
                  reader.onload = (ev) => setOcrPreviewUrl(ev.target?.result as string);
                  reader.readAsDataURL(file);
                }
              }}
              className="hidden"
              id="paper-sheet-input"
            />
            <label htmlFor="paper-sheet-input" className="cursor-pointer flex flex-col items-center gap-2">
              {ocrPreviewUrl ? (
                <img src={ocrPreviewUrl} alt="معاينة السجل" className="max-h-48 rounded-xl object-contain shadow" />
              ) : (
                <>
                  <div className="w-12 h-12 rounded-full bg-purple-100 dark:bg-purple-900/40 text-purple-600 flex items-center justify-center">
                    <Upload className="w-6 h-6" />
                  </div>
                  <span className="text-xs font-bold text-slate-700 dark:text-slate-300">اضغط لالتقاط أو اختيار صورة السجل الورقي</span>
                </>
              )}
            </label>
          </div>

          {ocrPreviewUrl && (
            <button
              onClick={onScan}
              disabled={isOcrScanning}
              className="w-full py-2.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs shadow flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {isOcrScanning ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>جاري قراءة وتحليل جدول السجل الورقي...</span>
                </>
              ) : (
                <>
                  <ScanLine className="w-4 h-4" />
                  <span>فحص واستخراج الدرجات بالذكاء الاصطناعي</span>
                </>
              )}
            </button>
          )}
        </div>

        {/* Extracted OCR Table Preview */}
        {ocrExtractedStudents.length > 0 && (
          <div className="space-y-3 pt-3 border-t">
            <h4 className="text-xs font-black text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
              <Sparkles className="w-4 h-4 text-amber-500" />
              <span>الدرجات المستخرجة من الصورة ({ocrExtractedStudents.length} طالب):</span>
            </h4>

            <div className="max-h-44 overflow-y-auto border rounded-xl p-2 text-xs divide-y bg-slate-50 dark:bg-slate-900">
              {ocrExtractedStudents.map((st, i) => (
                <div key={i} className="py-1.5 flex items-center justify-between text-xs">
                  <span className="font-bold text-slate-900 dark:text-white">{st.studentName}</span>
                  <div className="flex items-center gap-3">
                    <span className="text-slate-500">القيد: {st.recordNumber}</span>
                    <span className="font-black font-mono text-emerald-600">الدرجة: {st.finalMark}</span>
                    <span className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 text-[10px] font-bold">{st.status}</span>
                  </div>
                </div>
              ))}
            </div>

            <button
              onClick={onApply}
              className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs shadow cursor-pointer"
            >
              تطبيق ودمج هذه الدرجات في السجل الموحد
            </button>
          </div>
        )}

      </div>
    </div>
  );
};
