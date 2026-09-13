import React from 'react';
import { Student, StudentMark } from '../types';
import { X, Printer, Award, FileText, User, Calendar } from 'lucide-react';

interface StudentTranscriptModalProps {
  student: Student;
  isOpen: boolean;
  onClose: () => void;
}

export const StudentTranscriptModal: React.FC<StudentTranscriptModalProps> = ({ student, isOpen, onClose }) => {
  if (!isOpen) return null;

  const subjects = [
    'اللغة العربية', 'الرياضيات', 'العلوم', 'اللغة الإنجليزية',
    'التربية الإسلامية', 'التربية الفنية', 'التربية الأخلاقية',
    'النشاط البدني', 'حقوق الإنسان', 'الاجتماعيات'
  ];

  const getMark = (subject: string): StudentMark | undefined => {
    return student.marksHistory.find(m => m.subject === subject);
  };

  return (
    <div className="fixed inset-0 z-[60] bg-slate-900/90 backdrop-blur-sm flex items-center justify-center p-4 dir-rtl">
      <div className="bg-white w-full max-w-4xl rounded-[2.5rem] shadow-2xl flex flex-col max-h-[90vh] overflow-hidden border-4 border-indigo-600">

        {/* Header */}
        <div className="p-6 bg-indigo-600 text-white flex items-center justify-between shadow-lg">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-2xl bg-white/20 flex items-center justify-center border-2 border-white/30">
              <Award className="w-10 h-10 text-amber-300" />
            </div>
            <div>
              <h2 className="text-2xl font-black">وثيقة الطالب الرسمية</h2>
              <p className="text-indigo-100 font-bold opacity-80">سجل الدرجات الشامل للعام الدراسي 2024-2025</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 rounded-xl bg-white/10 hover:bg-rose-500 transition-all cursor-pointer"><X className="w-6 h-6" /></button>
        </div>

        {/* Student Info Bar */}
        <div className="p-6 bg-slate-50 border-b-2 border-slate-100 grid grid-cols-1 md:grid-cols-3 gap-4">
           <div className="flex items-center gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
              <User className="w-5 h-5 text-indigo-600" />
              <div className="flex flex-col">
                <span className="text-[10px] font-black text-slate-400">اسم الطالب:</span>
                <span className="text-sm font-black text-slate-900">{student.firstName} {student.secondName} {student.titleName}</span>
              </div>
           </div>
           <div className="flex items-center gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
              <Calendar className="w-5 h-5 text-emerald-600" />
              <div className="flex flex-col">
                <span className="text-[10px] font-black text-slate-400">الصف والشعبة:</span>
                <span className="text-sm font-black text-slate-900">{student.currentGrade} - الشعبة ({student.section})</span>
              </div>
           </div>
           <div className="flex items-center gap-3 bg-white p-3 rounded-2xl border border-slate-200 shadow-sm">
              <FileText className="w-5 h-5 text-amber-600" />
              <div className="flex flex-col">
                <span className="text-[10px] font-black text-slate-400">رقم القيد:</span>
                <span className="text-sm font-black text-slate-900 font-mono">{student.recordNumber}</span>
              </div>
           </div>
        </div>

        {/* Grades Table - VERTICAL ROWS AS REQUESTED */}
        <div className="flex-1 overflow-y-auto p-6 bg-white custom-scrollbar">
           <div className="grid grid-cols-1 gap-2">
              <div className="grid grid-cols-6 gap-2 bg-indigo-900 text-white p-3 rounded-xl text-center text-[11px] font-black mb-2 sticky top-0 shadow-md">
                 <div className="text-right pr-2 col-span-2">المادة الدراسية</div>
                 <div>فصل 1</div>
                 <div>نصف السنة</div>
                 <div>فصل 2</div>
                 <div className="bg-amber-500 text-slate-950 rounded-lg">الدرجة النهائية</div>
              </div>

              {subjects.map((sub, idx) => {
                const mark = getMark(sub);
                const finalVal = mark?.finalGrade ?? null;
                const isFail = finalVal !== null && finalVal > 0 && finalVal < 50;

                return (
                  <div key={idx} className="grid grid-cols-6 gap-2 p-3 rounded-2xl border border-slate-100 hover:bg-slate-50 transition-colors items-center text-center font-bold text-sm">
                     <div className="text-right pr-2 col-span-2 flex items-center gap-2">
                        <div className="w-2 h-2 rounded-full bg-indigo-400" />
                        <span className="text-slate-900">{sub}</span>
                     </div>
                     <div className="text-slate-500 font-mono">{mark?.term1Avg || '-'}</div>
                     <div className="text-slate-500 font-mono">{mark?.midtermFinalGrade || '-'}</div>
                     <div className="text-slate-500 font-mono">{mark?.term2Avg || '-'}</div>
                     <div className={`py-1.5 rounded-xl font-mono font-black border ${
                       isFail 
                         ? 'text-rose-600 bg-rose-50 border-rose-200' 
                         : 'text-slate-900 bg-slate-50 border-slate-200'
                     }`}>
                       {mark?.finalGrade || '-'}
                     </div>
                  </div>
                );
              })}
           </div>

           {/* Results Summary */}
           <div className="mt-8 p-6 bg-slate-900 text-white rounded-[2rem] border-b-8 border-amber-500 flex items-center justify-between">
              <div>
                 <h4 className="text-lg font-black">النتيجة النهائية والتقييم</h4>
                 <p className="text-xs text-slate-400 mt-1 italic">يتم احتساب النتيجة بناءً على السعي السنوي والامتحانات النهائية</p>
              </div>
              <div className="text-center">
                 <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">الحالة العامة</span>
                 <div className="text-3xl font-black text-amber-400">{student.finalYearScore && student.finalYearScore >= 50 ? 'ناجح' : 'قيد المراجعة'}</div>
              </div>
           </div>
        </div>

        {/* Footer Actions */}
        <div className="p-6 bg-slate-50 border-t-2 border-slate-100 flex items-center justify-between shrink-0">
           <button onClick={() => window.print()} className="px-8 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl font-black text-sm shadow-xl flex items-center gap-2 transition-all cursor-pointer">
              <Printer className="w-5 h-5" />
              طباعة الوثيقة الرسمية
           </button>
           <button onClick={onClose} className="px-6 py-3 bg-white border-2 border-slate-200 text-slate-600 rounded-2xl font-bold text-sm hover:bg-slate-100 transition-all cursor-pointer">إغلاق</button>
        </div>

      </div>
    </div>
  );
};
