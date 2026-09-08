import React from 'react';
import { Maximize2, Printer, RotateCcw, FileText, Award, Edit2, Phone, MapPin, Building2, BookOpen, Trash2 } from 'lucide-react';
import { Student } from '../../types';

interface StudentRosterTableProps {
  activeTab: 'active' | 'archive';
  filteredStudents: Student[];
  onSelectDetail: (student: Student) => void;
  onSelectPrint: (student: Student) => void;
  onRestore: (studentId: string) => void;
  onUpdateStatus: (studentId: string, status: Student['status']) => void;
  onSelectTranscript: (student: Student) => void;
  onUpdateStudent?: (student: Student) => void;
  onDeleteStudent?: (studentId: string, name: string) => void;
}

export const StudentRosterTable: React.FC<StudentRosterTableProps> = ({
  activeTab,
  filteredStudents,
  onSelectDetail,
  onSelectPrint,
  onRestore,
  onUpdateStatus,
  onSelectTranscript,
  onUpdateStudent,
  onDeleteStudent
}) => {
  return (
    <div className="data-grid-shell bg-white rounded-2xl border-2 border-slate-400 shadow-xl overflow-hidden">
      <div className="overflow-x-auto custom-scrollbar">
        {activeTab === 'active' ? (
          <table className="data-grid w-full text-center border-collapse text-xs">
            <thead>
              <tr className="bg-slate-900 text-white font-black border-b-3 border-amber-400 text-xs">
                <th className="py-3.5 px-2 border-r border-slate-700 w-10 text-center whitespace-nowrap text-amber-300">ت</th>
                <th className="py-3.5 px-2 border-r border-slate-700 w-20 text-center whitespace-nowrap">رقم القيد</th>
                <th className="py-3.5 px-2 border-r border-slate-700 w-24 text-center whitespace-nowrap bg-slate-800 text-amber-300">رقم الصفحة</th>
                <th className="py-3.5 px-2 border-r border-slate-700 w-24 text-center whitespace-nowrap">الصف</th>
                <th className="py-3.5 px-2 border-r border-slate-700 w-20 text-center whitespace-nowrap">الشعبة</th>
                <th className="py-3.5 px-3 border-r border-slate-700 text-right whitespace-nowrap min-w-[200px] text-white">اسم الطالب الرباعي واللقب</th>
                <th className="py-3.5 px-3 border-r border-slate-700 text-right whitespace-nowrap min-w-[150px] text-purple-200">اسم الأم الثلاثي</th>
                <th className="py-3.5 px-3 border-r border-slate-700 text-center whitespace-nowrap min-w-[100px] bg-slate-800 text-amber-200">النتيجة الحالية</th>
                <th className="py-3.5 px-2 border-r border-slate-700 text-center whitespace-nowrap min-w-[100px]">الدرجات</th>
                <th className="py-3.5 px-2 border-r border-slate-700 text-center whitespace-nowrap min-w-[90px] text-rose-300">الغيابات</th>
                <th className="py-3.5 px-3 text-center whitespace-nowrap min-w-[140px] text-amber-300">معلومات الطالب والطباعة</th>
              </tr>
            </thead>
            <tbody className="divide-y-2 divide-slate-300 text-xs font-bold">
              {filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan={11} className="py-12 text-center text-slate-500 font-bold">
                    لا يوجد طلاب مستمرون مطابقون لخيارات البحث أو الفلترة.
                  </td>
                </tr>
              ) : (
                filteredStudents.map((std, idx) => {
                  const marksCount = std.marksHistory ? std.marksHistory.length : 0;
                  const currentRes = std.currentResult || (std.marksHistory && std.marksHistory.length > 0 ? (std.marksHistory.every(m => (m.finalGrade || 0) >= 50) ? 'ناجح' : 'مكمل') : 'مستمر');

                  return (
                    <tr key={std.id} className="hover:bg-amber-50/70 transition-colors border-b border-slate-300">
                      <td className="py-3 px-2 font-mono font-black text-slate-950 border-r-2 border-slate-300 text-center whitespace-nowrap bg-slate-100">
                        {idx + 1}
                      </td>
                      <td className="py-1 px-1 border-r-2 border-slate-300 font-mono font-black text-center whitespace-nowrap bg-blue-50/40">
                        <input
                          type="text"
                          value={std.recordNumber || ''}
                          onChange={(e) => onUpdateStudent && onUpdateStudent({ ...std, recordNumber: e.target.value })}
                          className="w-16 text-center py-1 px-0.5 rounded-lg font-mono font-black text-xs text-blue-950 bg-white border border-blue-300 focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-xs"
                          title="تعديل رقم القيد المباشر"
                        />
                      </td>
                      <td className="py-1 px-1 border-r-2 border-slate-300 font-mono text-center whitespace-nowrap bg-emerald-50/60 font-black">
                        <input
                          type="text"
                          value={std.registerPageNumber || ''}
                          onChange={(e) => onUpdateStudent && onUpdateStudent({ ...std, registerPageNumber: e.target.value })}
                          placeholder="ص"
                          className="w-14 text-center py-1 px-0.5 rounded-lg font-mono font-black text-xs text-emerald-950 bg-white border border-emerald-300 focus:outline-none focus:ring-2 focus:ring-emerald-500 shadow-xs"
                          title="تعديل رقم الصفحة المباشر"
                        />
                      </td>
                      <td className="py-3 px-2 border-r-2 border-slate-300 font-black text-center whitespace-nowrap text-slate-850">
                        {std.currentGrade}
                      </td>
                      <td className="py-3 px-2 border-r-2 border-slate-300 font-black text-center whitespace-nowrap text-indigo-950 bg-indigo-50/40">
                        {std.section}
                      </td>
                      <td
                        onClick={() => onSelectDetail(std)}
                        className="py-3 px-3 border-r-2 border-slate-300 font-black text-slate-950 text-right whitespace-nowrap text-xs cursor-pointer hover:text-blue-700 hover:bg-blue-50 transition-all"
                        title="انقر لعرض ملف ومعلومات الطالب المفصلة"
                      >
                        {std.firstName} {std.secondName} {std.thirdName} {std.fourthName || ''} {std.titleName || ''}
                      </td>
                      <td className="py-3 px-3 border-r-2 border-slate-300 text-right whitespace-nowrap text-purple-950 font-bold bg-purple-50/30">
                        {std.motherName || '---'}
                      </td>
                      <td className="py-3 px-2 border-r-2 border-slate-300 text-center whitespace-nowrap">
                        <span className={`px-3 py-1 rounded-full font-black text-[11px] shadow-xs ${
                          currentRes === 'ناجح' 
                            ? 'bg-emerald-600 text-white' 
                            : currentRes === 'مكمل' 
                              ? 'bg-amber-500 text-slate-950' 
                              : 'bg-slate-200 text-slate-800'
                        }`}>
                          {currentRes}
                        </span>
                      </td>
                      <td className="py-2.5 px-2 border-r-2 border-slate-300 text-center whitespace-nowrap">
                        <button
                          onClick={() => onSelectTranscript(std)}
                          className={`inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg font-black text-xs transition-all shadow-xs cursor-pointer ${
                            marksCount > 0 
                              ? 'bg-emerald-100 text-emerald-950 border border-emerald-400 hover:bg-emerald-200' 
                              : 'bg-amber-50 text-amber-900 border border-amber-400 hover:bg-amber-100'
                          }`}
                          title="عرض سجل الدرجات"
                        >
                          <Award className="w-3.5 h-3.5 text-emerald-700" />
                          <span>{marksCount > 0 ? `${marksCount} مادة` : 'الدرجات'}</span>
                        </button>
                      </td>
                      <td className="py-3 px-2 border-r-2 border-slate-300 text-center whitespace-nowrap">
                        <span className={`px-2.5 py-1 rounded-full font-black text-[11px] border ${
                          (std.absencesCount || 0) === 0 
                            ? 'bg-emerald-50 text-emerald-800 border-emerald-300' 
                            : (std.absencesCount || 0) > 6 
                              ? 'bg-rose-100 text-rose-800 border-rose-300 font-black' 
                              : 'bg-amber-100 text-amber-800 border-amber-300'
                        }`}>
                          {std.absencesCount || 0} غياب
                        </span>
                      </td>
                      <td className="py-2 px-2 text-center whitespace-nowrap">
                        <div className="flex items-center justify-center gap-1.5">
                          <button
                            onClick={() => onSelectDetail(std)}
                            title="عرض وتعديل معلومات الطالب المفصلة والشاملة"
                            className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-black text-[11px] transition-all cursor-pointer shadow-xs"
                          >
                            <Maximize2 className="w-3.5 h-3.5" />
                            <span>تفاصيل</span>
                          </button>
                          <button
                            onClick={() => onSelectPrint(std)}
                            title="طباعة وثيقة وسجل الطالب"
                            className="p-1.5 rounded-xl bg-emerald-50 text-emerald-800 hover:bg-emerald-600 hover:text-white border border-emerald-300 transition-all cursor-pointer shadow-xs"
                          >
                            <Printer className="w-3.5 h-3.5" />
                          </button>
                          {onDeleteStudent && (
                            <button
                              onClick={() => onDeleteStudent(std.id, `${std.firstName} ${std.secondName}`)}
                              title="حذف الطالب نهائياً من السجل"
                              className="p-1.5 rounded-xl bg-rose-50 text-rose-700 hover:bg-rose-600 hover:text-white border border-rose-300 transition-all cursor-pointer shadow-xs"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>
                      </td>

                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        ) : (
          /* Former Students Archive Table */
          <table className="data-grid w-full text-center border-collapse min-w-[1300px] text-xs">
            <thead>
              <tr className="bg-gradient-to-r from-rose-800 via-rose-700 to-amber-700 text-white font-black border-b-2 border-rose-400 text-xs">
                <th className="py-3.5 px-3 border-r border-rose-600 w-12 text-center whitespace-nowrap">ت</th>
                <th className="py-3.5 px-3 border-r border-rose-600 text-right whitespace-nowrap">اسم الطالب الكامل واللقب</th>
                <th className="py-3.5 px-3 border-r border-rose-600 text-center whitespace-nowrap">رقم القيد</th>
                <th className="py-3.5 px-3 border-r border-rose-600 text-center whitespace-nowrap">رقم الصفحة بالقيد</th>
                <th className="py-3.5 px-3 border-r border-rose-600 text-center whitespace-nowrap">الصف والشعبة السابقة</th>
                <th className="py-3.5 px-3 border-r border-rose-600 text-center whitespace-nowrap">نتيجة الطالب</th>
                <th className="py-3.5 px-3 border-r border-rose-600 text-center whitespace-nowrap">حالة الطالب بالسجل (سبب الأرشفة)</th>
                <th className="py-3.5 px-3 border-r border-rose-600 text-center whitespace-nowrap">المدرسة المنقول إليها</th>
                <th className="py-3.5 px-3 w-36 text-center whitespace-nowrap">الإجراءات والتحكم</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-xs font-medium">
              {filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-500 font-bold">
                    أرشيف الطلاب فارغ أو لا توجد نتائج مطابقة للبحث.
                  </td>
                </tr>
              ) : (
                filteredStudents.map((std, idx) => {
                  return (
                    <tr key={std.id} className="hover:bg-rose-50/60 transition-colors">
                      <td className="py-3 px-3 font-mono font-black text-slate-950 border-r border-slate-200 text-center whitespace-nowrap bg-slate-50">
                        {idx + 1}
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200 font-black text-slate-950 text-right whitespace-nowrap text-xs">
                        {std.firstName} {std.secondName} {std.thirdName} {std.fourthName || ''} {std.titleName || ''}
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200 font-mono font-black text-blue-900 text-center whitespace-nowrap">
                        #{std.recordNumber}
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200 font-mono text-center whitespace-nowrap text-amber-900 font-bold">
                        ص {std.registerPageNumber || '1'}
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200 font-black text-slate-800 text-center whitespace-nowrap">
                        {std.currentGrade} ({std.section})
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200 text-center whitespace-nowrap">
                        <span className="px-2.5 py-0.5 rounded font-black text-[11px] bg-slate-100 text-slate-800">
                          {std.currentResult || std.previousYearResult || '—'}
                        </span>
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200 text-center whitespace-nowrap">
                        <span className={`px-2.5 py-0.5 rounded-full font-black text-[11px] border shadow-xs ${
                          std.status === 'متخرج' || std.promotionDestination === 'تخرج'
                            ? 'bg-amber-100 text-amber-950 border-amber-300' 
                            : std.status === 'مفصول' || std.promotionDestination === 'فصل'
                              ? 'bg-rose-100 text-rose-950 border-rose-300'
                              : 'bg-indigo-100 text-indigo-950 border-indigo-300'
                        }`}>
                          {std.promotionDestination || std.status}
                        </span>
                      </td>
                      <td className="py-3 px-3 border-r border-slate-200 text-center whitespace-nowrap text-slate-700 font-bold">
                        {std.previousSchool || '—'}
                      </td>
                      <td className="py-2 px-3">
                        <div className="flex items-center justify-center gap-1.5">
                          <button
                            onClick={() => onRestore(std.id)}
                            title="إعادة القيد من الأرشيف إلى الطلاب المستمرين بالدراسة"
                            className="p-1.5 rounded-lg bg-amber-100 text-amber-900 hover:bg-amber-600 hover:text-white transition-all cursor-pointer shadow-xs"
                          >
                            <RotateCcw className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => onSelectDetail(std)}
                            title="عرض وتعديل ملف الطالب الشامل"
                            className="p-1.5 rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-600 hover:text-white transition-all cursor-pointer shadow-xs"
                          >
                            <Maximize2 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => onSelectPrint(std)}
                            title="طباعة وثيقة وسجل الطالب"
                            className="p-1.5 rounded-lg bg-emerald-50 text-emerald-700 hover:bg-emerald-600 hover:text-white transition-all cursor-pointer shadow-xs"
                          >
                            <Printer className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
