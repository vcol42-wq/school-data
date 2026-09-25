import React, { useState } from 'react';
import { Student, StudentMark } from '../../types';
import { 
  Building2, 
  Sparkles, 
  Printer, 
  Award, 
  CheckCircle2, 
  AlertCircle, 
  FileSpreadsheet,
  Maximize2,
  TrendingUp,
  Percent
} from 'lucide-react';
import { normalizeArabic } from '../../utils/syncService';
import { applyMinisterialGrace } from '../../utils/gradeCalculations';

interface AdminMasterRegisterProps {
  students: Student[];
  subjectsList: string[];
  onSelectStudent: (student: Student) => void;
  selectedGrade: string;
  selectedSection: string;
}

export type AdminViewMetric = 'annual' | 'final' | 'midterm' | 'term1' | 'term2';

export const AdminMasterRegister: React.FC<AdminMasterRegisterProps> = ({
  students,
  subjectsList,
  onSelectStudent,
  selectedGrade,
  selectedSection
}) => {
  const [metric, setMetric] = useState<AdminViewMetric>('annual');
  const [applyGrace, setApplyGrace] = useState<boolean>(false);

  const getStudentSubjectScore = (std: Student, subject: string): number => {
    const norm = normalizeArabic(subject);
    const m = (std.marksHistory || []).find(x => normalizeArabic(x.subject) === norm);
    if (!m) return 0;

    if (metric === 'annual') return m.annualAverage || 0;
    if (metric === 'final') return m.finalGrade || m.annualAverage || 0;
    if (metric === 'midterm') return m.midtermFinalGrade || 0;
    if (metric === 'term1') return m.term1Avg || 0;
    if (metric === 'term2') return m.term2Avg || 0;
    return 0;
  };

  const getMetricLabel = () => {
    if (metric === 'annual') return 'السعي السنوي';
    if (metric === 'final') return 'الدرجة النهائية';
    if (metric === 'midterm') return 'درجة نصف السنة';
    if (metric === 'term1') return 'معدل الفصل الأول';
    if (metric === 'term2') return 'معدل الفصل الثاني';
    return '';
  };

  return (
    <div className="space-y-4">
      
      {/* 1. Header & Metric Selector Bar */}
      <div className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-3 text-xs">
        
        {/* Metric Picker */}
        <div className="flex items-center gap-2 flex-wrap">
          <Building2 className="w-4 h-4 text-amber-600 ml-1" />
          <span className="font-black text-slate-800">الدرجة المعروضة بسجل الإدارة:</span>

          <div className="inline-flex rounded-xl bg-slate-100 p-0.5 border border-slate-200">
            <button
              type="button"
              onClick={() => setMetric('annual')}
              className={`px-3 py-1.5 rounded-lg font-black transition-all cursor-pointer ${
                metric === 'annual'
                  ? 'bg-amber-600 text-white shadow-xs'
                  : 'text-slate-700 hover:text-slate-900'
              }`}
            >
              🌟 السعي السنوي
            </button>
            <button
              type="button"
              onClick={() => setMetric('midterm')}
              className={`px-3 py-1.5 rounded-lg font-black transition-all cursor-pointer ${
                metric === 'midterm'
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'text-slate-700 hover:text-slate-900'
              }`}
            >
              نصف السنة
            </button>
            <button
              type="button"
              onClick={() => setMetric('final')}
              className={`px-3 py-1.5 rounded-lg font-black transition-all cursor-pointer ${
                metric === 'final'
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'text-slate-700 hover:text-slate-900'
              }`}
            >
              الدرجة النهائية
            </button>
            <button
              type="button"
              onClick={() => setMetric('term1')}
              className={`px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer ${
                metric === 'term1'
                  ? 'bg-blue-600 text-white shadow-xs'
                  : 'text-slate-700 hover:text-slate-900'
              }`}
            >
              معدل ف1
            </button>
            <button
              type="button"
              onClick={() => setMetric('term2')}
              className={`px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer ${
                metric === 'term2'
                  ? 'bg-purple-600 text-white shadow-xs'
                  : 'text-slate-700 hover:text-slate-900'
              }`}
            >
              معدل ف2
            </button>
          </div>
        </div>

        {/* Grace Decision Toggle & Print Button */}
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 cursor-pointer select-none bg-amber-50 hover:bg-amber-100 text-amber-950 px-3 py-1.5 rounded-xl border border-amber-300 transition-colors shadow-2xs">
            <input
              type="checkbox"
              checked={applyGrace}
              onChange={e => setApplyGrace(e.target.checked)}
              className="w-4 h-4 accent-amber-600 rounded cursor-pointer"
            />
            <span className="font-black text-[11px] flex items-center gap-1">
              <Sparkles className="w-3.5 h-3.5 text-amber-600" />
              تطبيق درجات القرار الـ 5 (محاكاة وزارية)
            </span>
          </label>

          <button
            type="button"
            onClick={() => window.print()}
            className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl font-bold text-xs flex items-center gap-1.5 transition-all shadow-xs cursor-pointer active:scale-95"
          >
            <Printer className="w-3.5 h-3.5 text-amber-400" />
            <span>طباعة الشيت الإداري</span>
          </button>
        </div>

      </div>

      {/* 2. جدول الشيت الإداري المركزي الموحد */}
      <div className="bg-white rounded-2xl border-2 border-slate-300 shadow-xl overflow-hidden">
        <div className="overflow-x-auto custom-scrollbar">
          <table className="w-full text-center border-collapse text-xs min-w-[1050px]">
            <thead>
              <tr className="bg-gradient-to-r from-slate-950 via-slate-900 to-indigo-950 text-white font-black text-[11px] border-b-2 border-amber-500">
                <th className="py-2.5 px-1 border-r border-slate-800 w-10 text-center">ت</th>
                <th className="py-2.5 px-1 border-r border-slate-800 w-16 text-center">القيد</th>
                <th className="py-2.5 px-3 border-r border-slate-800 text-right min-w-[200px] sticky right-0 bg-slate-950 z-10">
                  اسم الطالب الرباعي (انقر للتوسعة 🔍)
                </th>

                {/* أعمدة المواد الدراسية */}
                {subjectsList.map(subj => {
                  const shortName = subj.replace(/^التربية\s+/, '').replace(/^اللغة\s+/, '');
                  return (
                    <th key={subj} className="py-2 px-1 border-r border-slate-800 min-w-[65px] max-w-[75px]" title={subj}>
                      <span className="text-[11px] font-black">{shortName}</span>
                    </th>
                  );
                })}

                {/* المؤشرات الإدارية التراكمية */}
                <th className="py-2.5 px-2 border-r border-slate-800 w-18 bg-amber-950/70 text-amber-200">
                  المجموع
                </th>
                <th className="py-2.5 px-2 border-r border-slate-800 w-18 bg-indigo-950/70 text-indigo-200">
                  المعدل
                </th>
                <th className="py-2.5 px-2 border-r border-slate-800 w-18 bg-rose-950/70 text-rose-200">
                  الرسوب
                </th>
                <th className="py-2.5 px-3 border-r border-slate-800 w-24 bg-amber-600 text-slate-950 font-black">
                  الحالة الرسمية
                </th>
                <th className="py-2.5 px-2 text-center w-14">
                  كشف
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-200">
              {students.length === 0 ? (
                <tr>
                  <td colSpan={subjectsList.length + 8} className="py-12 text-center text-slate-500 font-bold">
                    لا يوجد طلاب في هذه الشعبة.
                  </td>
                </tr>
              ) : (
                students.map((std, idx) => {
                  // تحضير قائمة الدرجات للتحليل
                  const rawList = subjectsList.map(subj => ({
                    subject: subj,
                    score: getStudentSubjectScore(std, subj)
                  }));

                  // تطبيق القرار إذا كان مفعلاً
                  const graceAnalysis = applyMinisterialGrace(rawList, 5);

                  const evaluatedScores = rawList.map(item => {
                    if (applyGrace) {
                      const graceItem = graceAnalysis.items.find(g => g.subject === item.subject);
                      return {
                        subject: item.subject,
                        score: graceItem ? graceItem.finalScoreWithGrace : item.score,
                        usedGrace: graceItem ? graceItem.graceUsed : 0
                      };
                    }
                    return {
                      subject: item.subject,
                      score: item.score,
                      usedGrace: 0
                    };
                  });

                  const validOnly = evaluatedScores.filter(s => s.score > 0);
                  const totalSum = validOnly.reduce((acc, curr) => acc + curr.score, 0);
                  const avg = validOnly.length > 0 ? Math.round(totalSum / validOnly.length) : 0;
                  const fails = evaluatedScores.filter(s => s.score > 0 && s.score < 50).length;

                  const officialStatus = validOnly.length === 0 ? 'غير مرصود' :
                    fails === 0 ? 'ناجح' :
                    fails <= 2 ? `مكمل (${fails})` : `راسب (${fails})`;

                  return (
                    <tr key={std.id} className="hover:bg-amber-50/50 transition-colors">
                      {/* Seq */}
                      <td className="py-2 px-1 font-mono font-bold border-r border-slate-200 text-slate-600 bg-slate-50 text-[11px]">
                        {idx + 1}
                      </td>

                      {/* Record No */}
                      <td className="py-2 px-1 font-mono font-bold border-r border-slate-200 text-blue-900 text-[11px]">
                        #{std.recordNumber}
                      </td>

                      {/* Name */}
                      <td
                        onClick={() => onSelectStudent(std)}
                        className="py-2 px-3 border-r border-slate-200 text-right font-black text-slate-900 cursor-pointer hover:text-amber-700 transition-colors sticky right-0 bg-white"
                        title="انقر لعرض بطاقة درجات الطالب الكاملة 360°"
                      >
                        <div className="flex items-center justify-between">
                          <span>{std.firstName} {std.secondName} {std.thirdName} {std.fourthName || ''} {std.titleName || ''}</span>
                          <span className="text-[10px] text-amber-600 opacity-60 hover:opacity-100">
                            🔍
                          </span>
                        </div>
                      </td>

                      {/* Subjects Grades */}
                      {evaluatedScores.map(subjItem => {
                        const score = subjItem.score;
                        const isFail = score > 0 && score < 50;
                        const hasGrace = subjItem.usedGrace > 0;

                        return (
                          <td key={subjItem.subject} className="py-1.5 px-1 border-r border-slate-200 font-mono text-center">
                            <span className={`inline-block px-1.5 py-0.5 rounded-md font-bold text-xs ${
                              isFail 
                                ? 'bg-rose-100 text-rose-800 border border-rose-300' 
                                : hasGrace 
                                  ? 'bg-purple-100 text-purple-900 border border-purple-300 font-black' 
                                  : score >= 50 
                                    ? 'text-slate-800' 
                                    : 'text-slate-400'
                            }`} title={hasGrace ? `تمت إضافة ${subjItem.usedGrace} درجات قرار` : ''}>
                              {score > 0 ? score : '—'}
                              {hasGrace && <span className="text-[8px] text-purple-700 block -mt-0.5 font-bold">ق</span>}
                            </span>
                          </td>
                        );
                      })}

                      {/* Total */}
                      <td className="py-2 px-1 border-r border-slate-200 font-mono font-bold text-slate-900 bg-slate-50/50">
                        {totalSum > 0 ? totalSum : '—'}
                      </td>

                      {/* Average */}
                      <td className="py-2 px-1 border-r border-slate-200 font-mono font-black text-indigo-900 bg-indigo-50/40">
                        {avg > 0 ? `${avg}%` : '—'}
                      </td>

                      {/* Fails Count */}
                      <td className="py-2 px-1 border-r border-slate-200 font-mono text-center">
                        {fails > 0 ? (
                          <span className="px-2 py-0.5 rounded-full bg-rose-600 text-white font-black text-[11px]">
                            {fails}
                          </span>
                        ) : validOnly.length > 0 ? (
                          <span className="text-emerald-600 font-bold text-[11px]">0</span>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>

                      {/* Official Status */}
                      <td className="py-2 px-2 border-r border-slate-200 text-center whitespace-nowrap">
                        <span className={`inline-block px-3 py-1 rounded-xl font-black text-xs shadow-2xs ${
                          officialStatus === 'ناجح' ? 'bg-emerald-600 text-white' :
                          officialStatus.startsWith('مكمل') ? 'bg-amber-500 text-slate-950 font-black' :
                          officialStatus.startsWith('راسب') ? 'bg-rose-600 text-white' :
                          'bg-slate-100 text-slate-500'
                        }`}>
                          {officialStatus}
                        </span>
                      </td>

                      {/* Open Modal Button */}
                      <td className="py-2 px-2 text-center">
                        <button
                          type="button"
                          onClick={() => onSelectStudent(std)}
                          className="p-1.5 rounded-lg bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 cursor-pointer transition-colors"
                          title="عرض بطاقة درجات الطالب الكاملة"
                        >
                          <Maximize2 className="w-3.5 h-3.5" />
                        </button>
                      </td>

                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  );
};
