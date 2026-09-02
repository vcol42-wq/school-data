import React from 'react';
import { FileSpreadsheet, X, Upload } from 'lucide-react';

interface ImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  importRawText: string;
  setImportRawText: (text: string) => void;
  onExcelUpload: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onImageUpload: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onImportText: () => void;
}

export const ImportModal: React.FC<ImportModalProps> = ({
  isOpen,
  onClose,
  importRawText,
  setImportRawText,
  onExcelUpload,
  onImageUpload,
  onImportText
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white border-2 border-teal-500 rounded-3xl p-6 md:p-8 max-w-xl w-full shadow-2xl space-y-4 text-slate-900">
        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-teal-100 text-teal-800 flex items-center justify-center font-black">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
            <h3 className="text-base font-black text-teal-950">
              استيراد بيانات الطلبة من الأكسل / الصور / النصوص
            </h3>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100 text-slate-700 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Option A: Excel File */}
        <div className="p-3.5 rounded-2xl bg-teal-50/50 border-2 border-teal-200 space-y-2">
          <span className="font-black text-xs text-teal-950 block">
            الخيار الأول: رفع ملف أكسل (Excel .xlsx / .xls):
          </span>
          <input
            type="file"
            accept=".xlsx, .xls, .csv"
            onChange={onExcelUpload}
            className="w-full text-xs text-slate-700 font-bold file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-black file:bg-teal-600 file:text-white hover:file:bg-teal-700 cursor-pointer"
          />
        </div>

        {/* Option B: Image OCR Scanner */}
        <div className="p-3.5 rounded-2xl bg-amber-50/50 border-2 border-amber-200 space-y-2">
          <span className="font-black text-xs text-amber-950 block flex items-center gap-1.5">
            <Upload className="w-4 h-4 text-amber-600" />
            الخيار الثاني: استيراد وقراءة من صورة مستند/سجل (Smart Photo Reader):
          </span>
          <input
            type="file"
            accept="image/*"
            onChange={onImageUpload}
            className="w-full text-xs text-slate-700 font-bold file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-black file:bg-amber-500 file:text-white hover:file:bg-amber-600 cursor-pointer"
          />
        </div>

        {/* Option C: Raw Text Copy-Paste */}
        <div className="space-y-2 text-xs">
          <span className="font-black text-slate-900 block">
            الخيار الثالث: لصق نص أسماء الطلبة (من مستند Word أو نص مباشر):
          </span>
          <textarea
            value={importRawText}
            onChange={e => setImportRawText(e.target.value)}
            rows={4}
            placeholder={`أدخل الأسماء بسطر منفصل لكل طالب، مثال:\nحيدر فاضل عباس كريم | 1045 | 12 | 45 | 2024 | ناجح | الصف الأول | أ | 2 | مستمر | سليم`}
            className="w-full p-3 rounded-2xl border-2 border-slate-300 bg-white text-slate-950 font-black text-xs focus:border-teal-600 outline-none"
          />
        </div>

        <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-200">
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-black border-2 border-slate-300 cursor-pointer transition-all"
          >
            إلغاء التراجع
          </button>
          <button
            onClick={onImportText}
            className="px-6 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-black shadow-lg cursor-pointer transition-all"
          >
            تدقيق ومعاينة البيانات 📋
          </button>
        </div>
      </div>
    </div>
  );
};
