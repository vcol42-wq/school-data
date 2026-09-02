import React from 'react';
import { X, CheckCircle2, AlertCircle } from 'lucide-react';
import { Student } from '../../types';

interface AuditModalProps {
  auditPendingList: Array<{
    extractedStudent: Student;
    matchType: 'existing' | 'new';
    matchedExistingSeq?: number;
    fieldsSummary: string;
  }> | null;
  onClose: () => void;
  onConfirm: () => void;
}

export const AuditModal: React.FC<AuditModalProps> = ({
  auditPendingList,
  onClose,
  onConfirm
}) => {
  if (!auditPendingList) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white border-2 border-emerald-500 rounded-3xl p-6 md:p-8 max-w-3xl w-full shadow-2xl space-y-4 my-8 text-slate-900">
        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-800 flex items-center justify-center font-black">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-black text-emerald-950">
                نافذة التدقيق والمطابقة المعاينة البصرية للبيانات المستوردة
              </h3>
              <p className="text-xs text-slate-600 font-bold">
                تم تحليل البيانات وتدقيق الأسماء مقارنة بالسجل الحالي قبل نقلها
              </p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100 text-slate-700 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Audit Table */}
        <div className="overflow-x-auto rounded-xl border border-slate-300 dark:border-slate-700 max-h-80 overflow-y-auto">
          <table className="w-full text-center text-xs border-collapse">
            <thead>
              <tr className="bg-slate-900 text-white font-bold">
                <th className="p-2.5 border-r border-slate-700">ت</th>
                <th className="p-2.5 border-r border-slate-700 text-right">الاسم المستخرج والصف</th>
                <th className="p-2.5 border-r border-slate-700">حالة المطابقة بالسجل</th>
                <th className="p-2.5">الإجراء المقترح</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {auditPendingList.map((item, idx) => (
                <tr key={idx} className={item.matchType === 'existing' ? 'bg-amber-50/70 dark:bg-amber-950/30' : 'bg-emerald-50/70 dark:bg-emerald-950/30'}>
                  <td className="p-2 font-mono font-bold text-center border-r border-slate-200 dark:border-slate-800">{idx + 1}</td>
                  <td className="p-2 text-right border-r border-slate-200 dark:border-slate-800 font-bold">
                    <div>{item.extractedStudent.firstName} {item.extractedStudent.secondName} {item.extractedStudent.thirdName} {item.extractedStudent.titleName}</div>
                    <div className="text-[10px] text-slate-500">{item.fieldsSummary}</div>
                  </td>
                  <td className="p-2 border-r border-slate-200 dark:border-slate-800 text-center font-bold">
                    {item.matchType === 'existing' ? (
                      <span className="px-2.5 py-1 rounded-full bg-amber-200 text-amber-900 text-[11px] font-black inline-flex items-center gap-1">
                        <AlertCircle className="w-3.5 h-3.5" />
                        موجود بالسجل (تسلسل #{item.matchedExistingSeq})
                      </span>
                    ) : (
                      <span className="px-2.5 py-1 rounded-full bg-emerald-200 text-emerald-900 text-[11px] font-black inline-flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        قيد جديد غير مكرر
                      </span>
                    )}
                  </td>
                  <td className="p-2 text-center font-bold">
                    {item.matchType === 'existing'
                      ? `تحديث بيانات الطالب بالسجل رقم #${item.matchedExistingSeq}`
                      : 'إضافة كقيد جديد بالسجل'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="flex items-center justify-between pt-3 border-t">
          <span className="text-xs font-bold text-slate-600">
            إجمالي القيود الجاهزة للترحيل: {auditPendingList.length}
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-slate-200 dark:bg-slate-800 text-xs font-bold"
            >
              إلغاء الأمر
            </button>
            <button
              onClick={onConfirm}
              className="px-6 py-2 rounded-xl bg-slate-900 text-white text-xs font-black shadow-lg hover:bg-slate-800"
            >
              تأكيد الترحيل النهائي للسجل
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
