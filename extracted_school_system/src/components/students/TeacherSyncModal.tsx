import React from 'react';
import { RefreshCw } from 'lucide-react';

interface TeacherSyncModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedSubject: string;
  teacherSyncProgress: number;
}

export const TeacherSyncModal: React.FC<TeacherSyncModalProps> = ({
  isOpen,
  onClose,
  selectedSubject,
  teacherSyncProgress
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white border-2 border-amber-400 rounded-3xl p-6 md:p-8 max-w-md w-full shadow-2xl space-y-4 text-center text-slate-900">
        <div className="w-12 h-12 bg-amber-100 text-amber-800 rounded-full flex items-center justify-center mx-auto animate-pulse">
          <RefreshCw className="w-6 h-6 animate-spin" />
        </div>

        <h3 className="text-base font-black text-slate-950">
          جاري استيراد ومزامنة الدرجات من تطبيق المدرس
        </h3>

        <p className="text-xs text-slate-700 font-bold">
          يرجى الانتظار بينما يتم سحب وتدقيق درجات مادة <strong>[{selectedSubject}]</strong> للصف والجروب المختار.
        </p>

        {/* Progress Bar Container */}
        <div className="space-y-1.5">
          <div className="w-full bg-slate-200 dark:bg-slate-700 h-3.5 rounded-full overflow-hidden relative border border-slate-300 dark:border-slate-600">
            <div
              className="bg-gradient-to-r from-orange-500 to-amber-500 h-full rounded-full transition-all duration-300 shadow-inner"
              style={{ width: `${teacherSyncProgress}%` }}
            />
          </div>
          <div className="flex justify-between text-[11px] font-black text-slate-500">
            <span>{teacherSyncProgress}%</span>
            <span>
              {teacherSyncProgress < 30 ? 'جاري الاتصال بالسيرفر...' :
               teacherSyncProgress < 60 ? 'جاري مطابقة قيود الطلاب...' :
               teacherSyncProgress < 90 ? 'جاري استيراد وتدقيق الدرجات...' : 'اكتملت المزامنة!'}
            </span>
          </div>
        </div>

        <div className="pt-2">
          <button
            disabled={teacherSyncProgress < 100}
            onClick={onClose}
            className="w-full py-2.5 rounded-xl bg-slate-900 text-white font-bold text-xs shadow hover:bg-slate-800 disabled:opacity-40"
          >
            موافق، إغلاق نافذة المزامنة
          </button>
        </div>
      </div>
    </div>
  );
};
