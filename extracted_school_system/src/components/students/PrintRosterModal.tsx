import React from 'react';
import { Printer } from 'lucide-react';
import { Student, AppConfig } from '../../types';
import { printElement } from '../../utils/printHelper';

interface PrintRosterModalProps {
  isOpen: boolean;
  onClose: () => void;
  filteredStudents: Student[];
  selectedGrade: string;
  activeTab: 'active' | 'archive';
  config: AppConfig;
}

export const PrintRosterModal: React.FC<PrintRosterModalProps> = ({
  isOpen,
  onClose,
  filteredStudents,
  selectedGrade,
  activeTab,
  config
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
      <div id="student-roster-printable-area" className="bg-white text-slate-900 rounded-3xl p-8 max-w-4xl w-full shadow-2xl space-y-6 my-8 print-page relative border-2 border-slate-800 dir-rtl">

        {/* Header Official Letterhead */}
        <div className="flex items-start justify-between border-b-2 border-slate-900 pb-4">
          <div className="text-right text-xs font-bold space-y-1">
            <p>جمهورية العراق - وزارة التربية</p>
            <p>{config.directorateName}</p>
            <p>{config.schoolName}</p>
          </div>

          <div className="text-center space-y-1">
            <div className="w-14 h-14 mx-auto rounded-full bg-slate-900 text-amber-400 flex items-center justify-center font-black text-xl shadow">
              ع
            </div>
            <h2 className="text-sm font-black tracking-wide text-slate-900">سجل الطلاب المعتمد رسمياً (قائمة الطلاب)</h2>
          </div>

          <div className="text-left text-xs font-mono space-y-1">
            <p>العام الدراسي: 2024-2025</p>
            <p>التاريخ: {new Date().toLocaleDateString('ar-IQ')}</p>
          </div>
        </div>

        {/* Sub-header info */}
        <div className="p-3 bg-slate-100 rounded-2xl border border-slate-300 flex justify-between items-center text-xs font-bold">
          <span>إجمالي الطلاب المشمولين بالقائمة: <strong className="text-blue-700 font-mono">{filteredStudents.length} طالب</strong></span>
          <span>الصف/التصفية: <strong className="text-emerald-700">{selectedGrade}</strong></span>
          <span>الحالة: <strong className="text-amber-800">{activeTab === 'active' ? 'مستمر بالدراسة' : 'الأرشيف والسجلات'}</strong></span>
        </div>

        {/* Table */}
        <div className="border border-slate-400 rounded-xl overflow-hidden">
          <table className="data-grid w-full text-center border-collapse text-xs">
            <thead>
              <tr className="bg-slate-200 border-b border-slate-400 font-black">
                <th className="py-2.5 px-2 border-r border-slate-400">ت</th>
                <th className="py-2.5 px-2 border-r border-slate-400">رقم القيد</th>
                <th className="py-2.5 px-2 border-r border-slate-400 text-right">اسم الطالب الرباعي واللقب</th>
                <th className="py-2.5 px-2 border-r border-slate-400">اسم الأم</th>
                <th className="py-2.5 px-2 border-r border-slate-400">الصف والشعبة</th>
                <th className="py-2.5 px-2 border-r border-slate-400">الدرجة العامة</th>
                <th className="py-2.5 px-2">رقم الموحدة</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-300">
              {filteredStudents.map((std, idx) => (
                <tr key={std.id} className="hover:bg-slate-50">
                  <td className="py-2 px-2 border-r border-slate-300 font-mono font-bold">{idx + 1}</td>
                  <td className="py-2 px-2 border-r border-slate-300 font-mono">{std.recordNumber}</td>
                  <td className="py-2 px-2 border-r border-slate-300 text-right font-bold text-slate-900">
                    {std.firstName} {std.secondName} {std.thirdName} {std.fourthName} {std.titleName}
                  </td>
                  <td className="py-2 px-2 border-r border-slate-300 text-purple-700 font-medium">{std.motherName}</td>
                  <td className="py-2 px-2 border-r border-slate-300 font-bold">{std.currentGrade} ({std.section})</td>
                  <td className="py-2 px-2 border-r border-slate-300 font-black font-mono text-blue-700">{std.finalYearScore || 85}</td>
                  <td className="py-2 px-2 font-mono text-[11px]">{std.nationalCardNumber}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Signatures */}
        <div className="pt-6 flex justify-between items-end text-xs border-t">
          <div className="text-center space-y-1">
            <p className="font-bold text-slate-700">توقيع مسؤول التسجيل السلوكي:</p>
            <div className="w-36 h-10 border border-dashed border-slate-300 rounded-lg"></div>
          </div>

          <div className="text-center space-y-1">
            <p className="font-bold text-slate-700">توقيع ومصادقة مدير المدرسة والختم:</p>
            <p className="font-black text-slate-900">{config.managerName}</p>
            <div className="w-36 h-10 border border-dashed border-slate-300 rounded-lg flex items-center justify-center text-[10px] text-slate-400">
              ختم المدرسة الرسمي
            </div>
          </div>
        </div>

        {/* Action Bar (Hidden on print) */}
        <div className="flex gap-2 pt-4 border-t no-print">
          <button
            onClick={onClose}
            className="flex-1 py-3 rounded-2xl bg-slate-200 text-slate-800 text-xs font-bold cursor-pointer"
          >
            إغلاق النافذة
          </button>

          <button
            onClick={() => printElement('student-roster-printable-area', { title: 'سجل الطلاب الموحد', orientation: 'portrait' })}
            className="flex-1 py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black shadow-xl flex items-center justify-center gap-2 cursor-pointer"
          >
            <Printer className="w-4 h-4" />
            <span>طباعة سجل الطلاب الموحد (A4)</span>
          </button>
        </div>

      </div>
    </div>
  );
};
