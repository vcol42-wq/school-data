import React from 'react';
import { Printer, X } from 'lucide-react';
import { Student, AppConfig } from '../../types';
import { printElement } from '../../utils/printHelper';
import { MinistryLogo } from '../MinistryLogo';

interface PrintReportModalProps {
  student: Student | null;
  config: AppConfig;
  onClose: () => void;
}

export const PrintReportModal: React.FC<PrintReportModalProps> = ({
  student,
  config,
  onClose
}) => {
  if (!student) return null;

  const handlePrint = () => {
    printElement('student-print-document', {
      title: `وثيقة درجات - ${student.firstName} ${student.secondName} ${student.thirdName} ${student.fourthName || ''} ${student.titleName || ''}`,
      orientation: 'portrait'
    });
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto no-print-modal">
      <div className="bg-white text-slate-900 rounded-2xl p-8 max-w-2xl w-full shadow-2xl space-y-6 my-8 border-2 border-slate-300 font-amiri" id="student-print-document">

        {/* Header Official Letterhead */}
        <div className="flex items-start justify-between border-b-2 border-slate-900 pb-4">
          <div className="text-right text-xs font-bold space-y-1">
            <p className="font-bold">جمهورية العراق - وزارة التربية</p>
            <p className="font-bold">{config.directorateName || 'مديرية تربية ديالى'}</p>
            <p className="font-black text-sm">{config.schoolName || 'المدرسة النموذجية'}</p>
          </div>

          <div className="text-center space-y-1">
            <MinistryLogo className="w-14 h-14 mx-auto" />
            <span className="text-[11px] font-black tracking-widest text-slate-950 block">وثيقة درجات وسجل دراسي رسمي</span>
          </div>

          <div className="text-left text-xs font-mono space-y-1 font-bold">
            <p>الرقم: {student.recordNumber || '---'}</p>
            <p>التاريخ: {new Date().toLocaleDateString('ar-IQ')}</p>
          </div>
        </div>

        {/* Student Info Box */}
        <div className="bg-slate-50 p-4 rounded-xl border border-slate-300 grid grid-cols-2 gap-3 text-xs">
          <p className="font-bold"><strong>الاسم الرباعي واللقب:</strong> <span className="font-black text-slate-950">{student.firstName} {student.secondName} {student.thirdName} {student.fourthName || ''} {student.titleName || ''}</span></p>
          <p className="font-bold"><strong>اسم الأم الثلاثي:</strong> <span className="text-purple-900 font-black">{student.motherName}</span></p>
          <p className="font-bold"><strong>الصف والشعبة:</strong> <span className="font-black">{student.currentGrade} ({student.section})</span></p>
          <p className="font-bold"><strong>رقم القيد والصفحة:</strong> <span className="font-mono font-black">قيد {student.recordNumber} / ص {student.registerPageNumber}</span></p>
        </div>

        {/* Marks Table */}
        <div>
          <h4 className="font-black text-xs mb-2 border-r-4 border-emerald-600 pr-2">درجات ومواظبة الطالب المقيدة بالسجلات الرسمية:</h4>
          <table className="data-grid w-full text-center border-collapse border border-slate-400 text-xs">
            <thead>
              <tr className="bg-slate-100 font-black border-b border-slate-400">
                <th className="py-2 border-r border-slate-400">المادة الدراسية</th>
                <th className="py-2 border-r border-slate-400">نصف السنة</th>
                <th className="py-2 border-r border-slate-400">الامتحان النهائي</th>
                <th className="py-2">الدرجة النهائية</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-300">
              {(student.marksHistory || []).map((m, i) => (
                <tr key={i}>
                  <td className="py-2 border-r border-slate-300 font-bold">{m.subject}</td>
                  <td className="py-2 border-r border-slate-300 font-bold">{m.midterm}</td>
                  <td className="py-2 border-r border-slate-300 font-bold">{m.final}</td>
                  <td className="py-2 font-black text-emerald-800">{m.total} درجة</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Manager Signature Line */}
        <div className="pt-6 flex justify-between items-end text-xs">
          <div className="text-right space-y-1">
            <p className="font-bold">ملاحظات الإدارة:</p>
            <p className="text-slate-700 font-bold">{student.healthStatus || 'مستمر بالدوام ومستوفٍ للشروط'}</p>
          </div>

          <div className="text-center space-y-1">
            <p className="font-bold text-slate-800">توقيع مدير المدرسة والختم:</p>
            <p className="font-black text-sm text-slate-950">{config.managerName || 'مدير المدرسة'}</p>
            <p className="text-[10px] text-slate-500">التوقيع والختم الرسمي</p>
          </div>
        </div>

        {/* Print & Close Controls */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200 no-print">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-black border-2 border-slate-300 cursor-pointer"
          >
            إغلاق
          </button>
          <button
            onClick={handlePrint}
            className="px-6 py-2 rounded-xl bg-emerald-600 text-white text-xs font-black hover:bg-emerald-700 flex items-center gap-1.5 shadow-md cursor-pointer"
          >
            <Printer className="w-4 h-4" />
            <span>طباعة الوثيقة الرسمية 🖨️</span>
          </button>
        </div>

      </div>
    </div>
  );
};
