import React, { useState } from 'react';
import { FileSpreadsheet, X, Upload, CheckCircle2, Layers, AlertCircle, Sparkles, RefreshCw } from 'lucide-react';
import { inspectExcelWorkbookForStudents, ExcelSheetPreview } from '../../utils/parser';
import { Student } from '../../types';

interface ImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  importRawText: string;
  setImportRawText: (text: string) => void;
  onExcelUpload: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onImageUpload: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onImportText: () => void;
  onImportStudents?: (students: Student[], targetGrade?: string, targetSection?: string, replaceExisting?: boolean) => Promise<void>;
}

export const ImportModal: React.FC<ImportModalProps> = ({
  isOpen,
  onClose,
  importRawText,
  setImportRawText,
  onExcelUpload,
  onImageUpload,
  onImportText,
  onImportStudents
}) => {
  const [isInspecting, setIsInspecting] = useState(false);
  const [sheetPreviews, setSheetPreviews] = useState<ExcelSheetPreview[]>([]);
  const [replaceExisting, setReplaceExisting] = useState(true);
  const [selectedFileName, setSelectedFileName] = useState('');

  if (!isOpen) return null;

  const handleClose = () => {
    setSheetPreviews([]);
    setSelectedFileName('');
    setIsInspecting(false);
    onClose();
  };

  const handleFileInputChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (onImportStudents) {
      setIsInspecting(true);
      setSelectedFileName(file.name);
      try {
        const previews = await inspectExcelWorkbookForStudents(file);
        if (previews.length === 0) {
          alert('لم يتم العثور على أوراق تحتوي على بيانات طلاب صالحة في هذا الملف.');
          setSheetPreviews([]);
        } else {
          setSheetPreviews(previews);
        }
      } catch (err: any) {
        alert(`تعذر قراءة ملف الأكسل: ${err.message}`);
      } finally {
        setIsInspecting(false);
      }
    } else {
      onExcelUpload(e);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4 dir-rtl">
      <div className="bg-white border-2 border-teal-500 rounded-3xl p-6 md:p-8 max-w-xl w-full shadow-2xl space-y-4 text-slate-900 max-h-[90vh] overflow-y-auto">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-teal-100 text-teal-800 flex items-center justify-center font-black">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-teal-950">
                استيراد بيانات الطلبة من الأكسل / الصور / النصوص
              </h3>
              <p className="text-[11px] text-slate-500 font-bold">
                دعم متكامل للملفات متعددة الأوراق (الثاني أ، الثاني ب...) وعزل كل شعبة على حده
              </p>
            </div>
          </div>
          <button onClick={handleClose} className="p-1 rounded-lg hover:bg-slate-100 text-slate-700 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Loading Spinner when inspecting Excel */}
        {isInspecting && (
          <div className="p-6 text-center space-y-2 bg-teal-50 rounded-2xl border border-teal-200">
            <RefreshCw className="w-8 h-8 text-teal-600 animate-spin mx-auto" />
            <p className="font-black text-sm text-teal-950">جارٍ فحص وتحليل أوراق ملف الأكسل وتحديد الشعب والصفوف...</p>
          </div>
        )}

        {/* When Sheets are Detected: Show Multi-Sheet Separation Options */}
        {!isInspecting && sheetPreviews.length > 0 && (
          <div className="space-y-4">
            <div className="flex items-center justify-between bg-teal-50 p-3 rounded-2xl border border-teal-200">
              <div className="space-y-0.5">
                <span className="font-black text-xs text-teal-950 block">
                  📁 الملف: {selectedFileName}
                </span>
                <span className="text-[11px] text-teal-700 font-bold">
                  تم اكتشاف {sheetPreviews.length} أوراق/شعب دراسية مستقلة بنجاح
                </span>
              </div>
              <button
                onClick={() => { setSheetPreviews([]); setSelectedFileName(''); }}
                className="text-xs text-teal-800 hover:text-teal-950 font-black underline cursor-pointer"
              >
                تغيير الملف
              </button>
            </div>

            {/* Replace Existing Section Option (Prevents Unwanted Accumulation / الجمع) */}
            <label className="flex items-start gap-2.5 p-3 rounded-2xl bg-amber-50/70 border-2 border-amber-300 cursor-pointer text-xs">
              <input 
                type="checkbox" 
                checked={replaceExisting} 
                onChange={e => setReplaceExisting(e.target.checked)} 
                className="mt-0.5 w-4 h-4 rounded text-teal-600 focus:ring-teal-500 cursor-pointer" 
              />
              <div className="space-y-0.5">
                <span className="font-black text-slate-900 block">
                  استبدال طلاب الشعبة المحددة (موصى به لمنع الجمع والتكرار)
                </span>
                <span className="text-[11px] text-slate-600 block leading-relaxed font-medium">
                  عند التفعيل: سيتم تحديث طلاب الشعبة المستوردة فقط دون التأثير على باقي الشعب والصفوف، ولن تتكرر الأسماء فوق بعضها.
                </span>
              </div>
            </label>

            {/* Individual Sheet Cards */}
            <div className="space-y-2.5 max-h-64 overflow-y-auto pr-1">
              {sheetPreviews.map((sheet, idx) => (
                <div 
                  key={idx} 
                  className="p-3.5 rounded-2xl bg-slate-50 border-2 border-slate-200 hover:border-teal-500 transition-all space-y-2"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="px-2.5 py-1 rounded-lg bg-teal-100 text-teal-950 font-black text-xs">
                        ورقة: {sheet.sheetName}
                      </span>
                      <span className="px-2.5 py-1 rounded-lg bg-indigo-100 text-indigo-900 font-black text-xs">
                        {sheet.grade} - شعبة ({sheet.section})
                      </span>
                    </div>
                    <span className="text-xs font-black text-slate-800">
                      {sheet.studentCount} طالب
                    </span>
                  </div>

                  {sheet.sampleNames.length > 0 && (
                    <div className="text-[11px] text-slate-500 truncate">
                      <span className="font-bold text-slate-700">نماذج الأسماء: </span>
                      {sheet.sampleNames.join('، ')}...
                    </div>
                  )}

                  <div className="pt-1 flex justify-end">
                    <button
                      onClick={() => onImportStudents!(sheet.students, sheet.grade, sheet.section, replaceExisting)}
                      className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-black shadow-md cursor-pointer transition-all"
                    >
                      <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                      <span>استيراد شعبة ({sheet.section}) فقط على حده</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {/* Import All Sheets as Distinct Sections Button */}
            {sheetPreviews.length > 1 && (
              <button
                onClick={() => {
                  const allStudents = sheetPreviews.flatMap(p => p.students);
                  onImportStudents!(allStudents, undefined, undefined, replaceExisting);
                }}
                className="w-full py-3 rounded-2xl bg-gradient-to-r from-indigo-600 to-teal-600 hover:from-indigo-700 hover:to-teal-700 text-white font-black text-xs shadow-lg flex items-center justify-center gap-2 cursor-pointer transition-all"
              >
                <Layers className="w-4 h-4" />
                <span>
                  استيراد كافة الشعب دفعة واحدة ({sheetPreviews.reduce((acc, p) => acc + p.studentCount, 0)} طالب في سجلات شعبهم المستقلة)
                </span>
              </button>
            )}
          </div>
        )}

        {/* When No Sheets Inspected Yet: Standard Upload Options */}
        {!isInspecting && sheetPreviews.length === 0 && (
          <>
            {/* Option A: Excel File */}
            <div className="p-4 rounded-2xl bg-teal-50/70 border-2 border-teal-300 space-y-2">
              <span className="font-black text-xs text-teal-950 block">
                الخيار الأول: رفع ملف أكسل (Excel .xlsx / .xls):
              </span>
              <p className="text-[11px] text-teal-800 font-bold">
                يدعم الملفات متعددة الأوراق (مثل ورقة "الثاني أ" وورقة "الثاني ب") ويتيح لك استيراد كل شعبة على حده.
              </p>
              <input
                type="file"
                accept=".xlsx, .xls, .csv"
                onChange={handleFileInputChange}
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
                onClick={handleClose}
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
          </>
        )}
      </div>
    </div>
  );
};
