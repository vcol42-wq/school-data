import React, { useState } from 'react';
import { Student, AppConfig } from '../types';
import { 
  UserMinus, 
  Search, 
  Printer, 
  Archive, 
  RotateCcw, 
  FileText, 
  CheckCircle2, 
  ShieldAlert,
  Info
} from 'lucide-react';

interface FormerStudentsViewProps {
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  config: AppConfig;
}

export const FormerStudentsView: React.FC<FormerStudentsViewProps> = ({
  students,
  setStudents,
  config
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedStudentForDetails, setSelectedStudentForDetails] = useState<Student | null>(null);

  // Filter departed, graduated, or transferred students
  const formerStudents = students.filter(
    s => s.status === 'غادر المدرسة' || s.status === 'متخرج' || s.status === 'مفصول'
  );

  const filteredFormer = formerStudents.filter(s => {
    const fullName = `${s.firstName} ${s.secondName} ${s.thirdName} ${s.fourthName} ${s.titleName}`.toLowerCase();
    const query = searchTerm.toLowerCase();
    return (
      fullName.includes(query) ||
      s.recordNumber.includes(query) ||
      s.registerPageNumber.includes(query) ||
      s.motherName.toLowerCase().includes(query)
    );
  });

  // Restore Student back to active register
  const handleRestoreStudent = (studentId: string) => {
    if (confirm('هل ترغب في إلغاء أرشفة الطالب وإعادته إلى سجل الطلاب المستمرين؟')) {
      setStudents(prev =>
        prev.map(s => (s.id === studentId ? { ...s, status: 'مستمر' } : s))
      );
      alert('تمت إعادة الطالب بنجاح إلى سجل الطلاب المستمرين.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6 print-page">
      
      {/* Top Banner */}
      <div className="bg-white p-5 rounded-2xl border-2 border-sky-300 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-rose-100 text-rose-950 text-xs font-black border border-rose-300 mb-2">
            <Archive className="w-4 h-4 text-rose-700" />
            <span>سجل الأرشيف والطلاب المغادرين والمتحولين</span>
          </div>
          <h2 className="text-xl md:text-2xl font-black text-slate-950">
            أرشيف الطلاب السابقين (سجل المغادرين والممتنعين)
          </h2>
          <p className="text-xs text-slate-700 font-bold mt-1">
            قائمة بجميع الطلاب الذين غادروا المدرسة أو تخرجوا مع الحفظ المضغوط المحمي للبيانات
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="bg-sky-50 border border-sky-300 px-4 py-2 rounded-xl text-center">
            <span className="text-xs text-slate-600 font-bold block">إجمالي المغادرين الأرشيفي</span>
            <span className="text-xl font-black text-rose-700 block">{formerStudents.length} طالب</span>
          </div>
          
          <button
            onClick={() => window.print()}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-black text-xs shadow-md cursor-pointer transition-all"
          >
            <Printer className="w-4 h-4" />
            <span>طباعة الأرشيف A4</span>
          </button>
        </div>
      </div>

      {/* Search & Filter Controls */}
      <div className="bg-white p-4 rounded-2xl border-2 border-sky-300 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            placeholder="بحث برقم القيد، الاسم الرباعي، أو اسم الأم..."
            className="w-full pr-10 pl-4 py-2.5 rounded-xl border-2 border-sky-300 bg-white text-slate-950 font-black text-xs placeholder-slate-400 focus:outline-none focus:border-sky-500 shadow-sm"
          />
        </div>

        <div className="text-xs font-bold text-slate-600 bg-amber-50 border border-amber-300 px-3.5 py-2 rounded-xl flex items-center gap-2">
          <Info className="w-4 h-4 text-amber-700 shrink-0" />
          <span>يتم عرض الحقول الأساسية الأربعة فقط وفق تعليمات الأرشفة الرسمية.</span>
        </div>
      </div>

      {/* Main Compressed Archive Table */}
      <div className="bg-white rounded-2xl border-2 border-sky-300 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-right border-collapse">
            <thead>
              <tr className="bg-gradient-to-r from-sky-800 via-sky-700 to-indigo-800 text-white font-black text-center">
                <th className="py-3 px-3 border-r border-sky-600 w-12">ت</th>
                <th className="py-3 px-3 border-r border-sky-600">رقم القيد</th>
                <th className="py-3 px-3 border-r border-sky-600">الصفحة والقيد</th>
                <th className="py-3 px-3 border-r border-sky-600">الاسم الرباعي واللقب</th>
                <th className="py-3 px-3 border-r border-sky-600">اسم الأم</th>
                <th className="py-3 px-3 border-r border-sky-600">حالة الترحيل والأرشفة</th>
                <th className="py-3 px-3 no-print w-32">الإجراءات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {filteredFormer.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-500 font-bold">
                    لا يوجد طلاب مؤرشفين في سجل المغادرين حالياً.
                  </td>
                </tr>
              ) : (
                filteredFormer.map((std, idx) => (
                  <tr key={std.id} className="hover:bg-sky-50/60 transition-colors">
                    <td className="py-3 px-3 text-center font-mono font-black text-slate-900 border-r border-slate-200">
                      {idx + 1}
                    </td>
                    <td className="py-3 px-3 text-center font-mono font-black text-sky-950 border-r border-slate-200">
                      {std.recordNumber}
                    </td>
                    <td className="py-3 px-3 text-center font-mono font-black text-slate-950 border-r border-slate-200">
                      ص {std.registerPageNumber} / و {std.wasatiPageNumber}
                    </td>
                    <td className="py-3 px-3 font-black text-slate-950 text-right border-r border-slate-200">
                      {std.firstName} {std.secondName} {std.thirdName} {std.fourthName} {std.titleName}
                    </td>
                    <td className="py-3 px-3 font-black text-slate-900 text-right border-r border-slate-200">
                      {std.motherName || 'غير مسجل'}
                    </td>
                    <td className="py-3 px-3 text-center border-r border-slate-200">
                      <span className={`px-2.5 py-0.5 rounded-full font-black text-[11px] border ${
                        std.status === 'غادر المدرسة'
                          ? 'bg-rose-100 text-rose-950 border-rose-300'
                          : std.status === 'متخرج'
                          ? 'bg-amber-100 text-amber-950 border-amber-300'
                          : 'bg-slate-100 text-slate-950 border-slate-300'
                      }`}>
                        {std.status} (مؤرشف)
                      </span>
                    </td>
                    <td className="py-3 px-3 text-center no-print flex items-center justify-center gap-1.5">
                      <button
                        onClick={() => setSelectedStudentForDetails(std)}
                        className="px-2.5 py-1 rounded-lg bg-sky-100 hover:bg-sky-200 text-sky-900 font-black text-[11px] border border-sky-300 transition-all"
                        title="معاينة بطاقة الأرشيف المضغوط"
                      >
                        معاينة
                      </button>
                      <button
                        onClick={() => handleRestoreStudent(std.id)}
                        className="p-1 rounded-lg bg-emerald-100 hover:bg-emerald-200 text-emerald-900 font-bold border border-emerald-300 transition-all"
                        title="إعادة للسجل المستمر"
                      >
                        <RotateCcw className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Compressed Archive Student Detail Modal */}
      {selectedStudentForDetails && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-center justify-center p-4">
          <div className="bg-white border-2 border-sky-300 rounded-2xl p-6 max-w-lg w-full shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <h3 className="text-base font-black text-slate-950 flex items-center gap-2">
                <FileText className="w-5 h-5 text-sky-600" />
                <span>بطاقة الطالب المغادر (الأرشيف المضغوط)</span>
              </h3>
              <button
                onClick={() => setSelectedStudentForDetails(null)}
                className="px-3 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 font-black text-xs text-slate-700"
              >
                إغلاق
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="p-3 bg-sky-50 rounded-xl border border-sky-200 space-y-1">
                <span className="text-[10px] text-slate-500 font-bold block">الاسم الكامل واللقب:</span>
                <p className="font-black text-base text-slate-950">
                  {selectedStudentForDetails.firstName} {selectedStudentForDetails.secondName} {selectedStudentForDetails.thirdName} {selectedStudentForDetails.fourthName} {selectedStudentForDetails.titleName}
                </p>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                  <span className="text-[10px] text-slate-500 font-bold block">رقم القيد الرسمي:</span>
                  <p className="font-black text-sm text-sky-950 font-mono">{selectedStudentForDetails.recordNumber}</p>
                </div>

                <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                  <span className="text-[10px] text-slate-500 font-bold block">الصفحة والقيد:</span>
                  <p className="font-black text-sm text-slate-950 font-mono">
                    ص {selectedStudentForDetails.registerPageNumber} / و {selectedStudentForDetails.wasatiPageNumber}
                  </p>
                </div>
              </div>

              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                <span className="text-[10px] text-slate-500 font-bold block">اسم الأم الكامل:</span>
                <p className="font-black text-sm text-slate-950">{selectedStudentForDetails.motherName || 'غير مسجل'}</p>
              </div>

              <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-amber-950 font-bold text-[11px] leading-relaxed">
                رمز التشفير والأرشفة السحابية: <span className="font-mono font-black">{selectedStudentForDetails.syncSealToken || 'ARCHIVE-SECURE-2026-DIALA'}</span>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-200 flex justify-end">
              <button
                onClick={() => setSelectedStudentForDetails(null)}
                className="px-5 py-2 rounded-xl bg-sky-600 text-white font-black text-xs hover:bg-sky-700 shadow-md"
              >
                تم والعودة للأرشيف
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
