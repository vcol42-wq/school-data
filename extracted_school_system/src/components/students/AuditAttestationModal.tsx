import React from 'react';
import { ShieldCheck, X } from 'lucide-react';

interface AuditAttestationModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedAuditStage: string;
  setSelectedAuditStage: (stage: 'الفصل الأول' | 'نصف السنة' | 'الفصل الثاني' | 'أخر السنة' | 'الدور الثاني') => void;
  auditPasscode: string;
  setAuditPasscode: (passcode: string) => void;
  onExecute: () => void;
}

export const AuditAttestationModal: React.FC<AuditAttestationModalProps> = ({
  isOpen,
  onClose,
  selectedAuditStage,
  setSelectedAuditStage,
  auditPasscode,
  setAuditPasscode,
  onExecute
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white border-2 border-indigo-400 rounded-3xl p-6 md:p-8 max-w-md w-full shadow-2xl space-y-4 text-slate-900">

        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
          <div className="flex items-center gap-2 text-indigo-900 font-black">
            <ShieldCheck className="w-5 h-5 text-indigo-600" />
            <span>المطابقة والتدقيق والمصادقة الإدارية</span>
          </div>
          <button onClick={onClose} className="text-slate-600 hover:text-slate-900 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-slate-700 leading-relaxed font-bold">
          يقوم المدير بالمطابقة والتدقيق النهائي لدرجات الطلاب، ثم المصادقة عليها تمهيداً لرفعها إلى الجهة الأعلى.
        </p>

        <div className="space-y-3">
          <div>
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
              مرحلة الامتحانات المراد مصادقتها:
            </label>
            <select
              value={selectedAuditStage}
              onChange={e => setSelectedAuditStage(e.target.value as any)}
              className="w-full p-2.5 rounded-xl border bg-white text-xs font-bold"
            >
              <option value="الفصل الأول">امتحانات الفصل الأول</option>
              <option value="نصف السنة">امتحانات نصف السنة</option>
              <option value="الفصل الثاني">امتحانات الفصل الثاني</option>
              <option value="أخر السنة">امتحانات أخر السنة</option>
              <option value="الدور الثاني">امتحانات الدور الثاني</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
              رمز المصادقة الإدارية (رمز المدير):
            </label>
            <input
              type="password"
              value={auditPasscode}
              onChange={e => setAuditPasscode(e.target.value)}
              placeholder="أدخل رمز المدير للمصادقة"
              className="w-full p-2.5 rounded-xl border bg-white text-xs font-mono font-bold"
            />
          </div>

          <div className="flex gap-2 pt-2">
            <button
              onClick={onClose}
              className="flex-1 py-2.5 rounded-xl bg-slate-200 dark:bg-slate-800 text-xs font-bold"
            >
              إلغاء
            </button>
            <button
              onClick={onExecute}
              className="flex-1 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-black shadow flex items-center justify-center gap-1"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>تثبيت المصادقة والتوقيع</span>
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
