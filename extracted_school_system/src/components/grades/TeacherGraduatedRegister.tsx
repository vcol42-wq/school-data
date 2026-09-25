import React, { useState } from 'react';
import { Student, StudentMark } from '../../types';
import { 
  GraduationCap, 
  PlusCircle, 
  CheckCircle, 
  HelpCircle, 
  Printer, 
  Maximize2, 
  BookOpen,
  Sparkles
} from 'lucide-react';
import { normalizeArabic } from '../../utils/syncService';
import { calculateTermAverage, calculateYearlyEffort, calculateFinalGradeScore } from '../../utils/gradeCalculations';

interface TeacherGraduatedRegisterProps {
  students: Student[];
  onUpdateStudentMark: (studentId: string, subject: string, updater: (prevMark: StudentMark) => StudentMark) => void;
  onSelectStudent: (student: Student) => void;
  subjectsList: string[];
}

export const TeacherGraduatedRegister: React.FC<TeacherGraduatedRegisterProps> = ({
  students,
  onUpdateStudentMark,
  onSelectStudent,
  subjectsList
}) => {
  const [selectedSubject, setSelectedSubject] = useState<string>(() => subjectsList[0] || 'الرياضيات');
  const [showThirdMonthTerm1, setShowThirdMonthTerm1] = useState<boolean>(false);
  const [showThirdMonthTerm2, setShowThirdMonthTerm2] = useState<boolean>(false);

  const getStudentMark = (std: Student, subject: string): StudentMark => {
    const norm = normalizeArabic(subject);
    const m = (std.marksHistory || []).find(x => normalizeArabic(x.subject) === norm);
    return m || {
      year: std.registrationYear || '2025-2026',
      subject,
      m1MonthAvg: 0,
      m2MonthAvg: 0,
      m1Month3: 0,
      term1Avg: 0,
      midtermFinalGrade: 0,
      m3MonthAvg: 0,
      m4MonthAvg: 0,
      m2Month3: 0,
      term2Avg: 0,
      annualAverage: 0,
      finalWrittenD1: 0,
      finalWrittenD2: null,
      finalGrade: 0
    };
  };

  const handleFieldChange = (
    studentId: string,
    field: keyof StudentMark,
    val: number | null
  ) => {
    onUpdateStudentMark(studentId, selectedSubject, prev => {
      const next: StudentMark = { ...prev, [field]: val };

      // 1. إعادة حساب معدل الفصل الأول
      const m1 = next.m1MonthAvg || 0;
      const m2 = next.m2MonthAvg || 0;
      const m1_3 = showThirdMonthTerm1 ? (next.m1Month3 || 0) : 0;
      next.term1Avg = calculateTermAverage(m1, m2, m1_3 > 0 ? m1_3 : undefined);

      // 2. إعادة حساب معدل الفصل الثاني
      const m3 = next.m3MonthAvg || 0;
      const m4 = next.m4MonthAvg || 0;
      const m2_3 = showThirdMonthTerm2 ? (next.m2Month3 || 0) : 0;
      next.term2Avg = calculateTermAverage(m3, m4, m2_3 > 0 ? m2_3 : undefined);

      // 3. إعادة حساب السعي السنوي بالمعادلة الوزارية (ف1 + نصف سنة + ف2) / 3
      const t1 = next.term1Avg || 0;
      const mid = next.midtermFinalGrade || 0;
      const t2 = next.term2Avg || 0;
      next.annualAverage = calculateYearlyEffort(t1, mid, t2);

      // 4. إعادة حساب الدرجة النهائية
      const exam = (next.finalWrittenD2 && next.finalWrittenD2 > 0) ? next.finalWrittenD2 : (next.finalWrittenD1 || 0);
      next.finalGrade = calculateFinalGradeScore(next.annualAverage || 0, exam);

      return next;
    });
  };

  return (
    <div className="space-y-4">

      {/* 1. Subject Selector Bar & Options */}
      <div className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-3 text-xs">
        
        {/* Subject dropdown or pills */}
        <div className="flex items-center gap-2">
          <BookOpen className="w-4 h-4 text-indigo-600" />
          <span className="font-black text-slate-800">اختر مادة سجل المدرج:</span>
          <select
            value={selectedSubject}
            onChange={e => setSelectedSubject(e.target.value)}
            className="p-2 rounded-xl border border-slate-300 font-bold text-slate-900 bg-slate-50 focus:border-indigo-500 outline-none shadow-2xs"
          >
            {subjectsList.map(s => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>

        {/* Third Month Toggles */}
        <div className="flex items-center gap-3 flex-wrap">
          <label className="flex items-center gap-2 cursor-pointer bg-blue-50/70 hover:bg-blue-100 text-blue-900 px-3 py-1.5 rounded-xl border border-blue-200 transition-colors">
            <input
              type="checkbox"
              checked={showThirdMonthTerm1}
              onChange={e => setShowThirdMonthTerm1(e.target.checked)}
              className="w-4 h-4 accent-indigo-600 rounded cursor-pointer"
            />
            <span className="font-black text-[11px]">+ شهر ثالث اختياري (الفصل الأول)</span>
          </label>

          <label className="flex items-center gap-2 cursor-pointer bg-purple-50/70 hover:bg-purple-100 text-purple-900 px-3 py-1.5 rounded-xl border border-purple-200 transition-colors">
            <input
              type="checkbox"
              checked={showThirdMonthTerm2}
              onChange={e => setShowThirdMonthTerm2(e.target.checked)}
              className="w-4 h-4 accent-purple-600 rounded cursor-pointer"
            />
            <span className="font-black text-[11px]">+ شهر ثالث اختياري (الفصل الثاني)</span>
          </label>
        </div>

      </div>

      {/* 2. جدول سجل المدرج الرسمي المعتمد */}
      <div className="bg-white rounded-2xl border-2 border-slate-300 shadow-xl overflow-hidden">
        <div className="overflow-x-auto custom-scrollbar">
          <table className="w-full text-center border-collapse text-xs min-w-[1000px]">
            <thead>
              {/* الصف الأول من الترويسة */}
              <tr className="bg-gradient-to-r from-indigo-900 via-indigo-800 to-slate-900 text-white font-black text-[11px]">
                <th rowSpan={2} className="py-2 px-1 border-r border-indigo-700 w-10 text-center">ت</th>
                <th rowSpan={2} className="py-2 px-1 border-r border-indigo-700 w-16 text-center">القيد</th>
                <th rowSpan={2} className="py-2 px-3 border-r border-indigo-700 text-right min-w-[200px]">اسم الطالب الرباعي (انقر للتوسعة)</th>
                
                {/* الفصل الأول */}
                <th colSpan={showThirdMonthTerm1 ? 4 : 3} className="py-1 px-1 border-r border-indigo-700 bg-blue-900/60 text-blue-200">
                  الفصل الأول
                </th>

                {/* نصف السنة */}
                <th rowSpan={2} className="py-2 px-2 border-r border-indigo-700 bg-emerald-900/60 text-emerald-200 w-24">
                  نصف السنة
                </th>

                {/* الفصل الثاني */}
                <th colSpan={showThirdMonthTerm2 ? 4 : 3} className="py-1 px-1 border-r border-indigo-700 bg-purple-900/60 text-purple-200">
                  الفصل الثاني
                </th>

                {/* السعي السنوي */}
                <th rowSpan={2} className="py-2 px-2 border-r border-indigo-700 bg-amber-900/60 text-amber-200 w-24">
                  السعي السنوي
                </th>

                {/* الامتحان النهائي */}
                <th colSpan={2} className="py-1 px-1 border-r border-indigo-700 bg-rose-900/60 text-rose-200">
                  الامتحان النهائي
                </th>

                {/* النتيجة النهائية */}
                <th rowSpan={2} className="py-2 px-2 border-r border-indigo-700 bg-amber-600 text-slate-950 font-black w-24">
                  الدرجة النهائية
                </th>

                <th rowSpan={2} className="py-2 px-2 text-center w-20">
                  النتيجة
                </th>
              </tr>

              {/* الصف الثاني من الترويسة الفرعية */}
              <tr className="bg-indigo-950 text-indigo-200 font-bold text-[10px] border-b-2 border-indigo-400">
                {/* ف1 */}
                <th className="py-1 px-1 border-r border-indigo-800 w-14">شهر 1</th>
                <th className="py-1 px-1 border-r border-indigo-800 w-14">شهر 2</th>
                {showThirdMonthTerm1 && <th className="py-1 px-1 border-r border-indigo-800 w-14 bg-indigo-900 text-amber-300">شهر 3 (اختياري)</th>}
                <th className="py-1 px-1 border-r border-indigo-800 w-18 bg-blue-950 text-blue-300 font-black">معدل ف1</th>

                {/* ف2 */}
                <th className="py-1 px-1 border-r border-indigo-800 w-14">شهر 1</th>
                <th className="py-1 px-1 border-r border-indigo-800 w-14">شهر 2</th>
                {showThirdMonthTerm2 && <th className="py-1 px-1 border-r border-indigo-800 w-14 bg-indigo-900 text-amber-300">شهر 3 (اختياري)</th>}
                <th className="py-1 px-1 border-r border-indigo-800 w-18 bg-purple-950 text-purple-300 font-black">معدل ف2</th>

                {/* نهائي */}
                <th className="py-1 px-1 border-r border-indigo-800 w-14">دور 1</th>
                <th className="py-1 px-1 border-r border-indigo-800 w-14">دور 2</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-200">
              {students.length === 0 ? (
                <tr>
                  <td colSpan={15} className="py-12 text-center text-slate-500 font-bold">
                    لا يوجد طلاب في هذه الشعبة.
                  </td>
                </tr>
              ) : (
                students.map((std, idx) => {
                  const mark = getStudentMark(std, selectedSubject);
                  const isFail = mark.finalGrade && mark.finalGrade > 0 && mark.finalGrade < 50;
                  const isPass = mark.finalGrade && mark.finalGrade >= 50;

                  return (
                    <tr key={std.id} className="hover:bg-indigo-50/40 transition-colors">
                      {/* Seq */}
                      <td className="py-1.5 px-1 font-mono font-bold border-r border-slate-200 text-slate-600 bg-slate-50 text-[11px]">
                        {idx + 1}
                      </td>

                      {/* Record No */}
                      <td className="py-1.5 px-1 font-mono font-bold border-r border-slate-200 text-blue-900 text-[11px]">
                        #{std.recordNumber}
                      </td>

                      {/* Student Name */}
                      <td 
                        onClick={() => onSelectStudent(std)}
                        className="py-1.5 px-3 border-r border-slate-200 text-right font-black text-slate-900 cursor-pointer hover:text-indigo-700 transition-colors"
                        title="انقر لعرض بطاقة درجات الطالب الكاملة"
                      >
                        <div className="flex items-center justify-between">
                          <span>{std.firstName} {std.secondName} {std.thirdName} {std.fourthName || ''} {std.titleName || ''}</span>
                          <span className="text-[10px] text-indigo-600 opacity-60 hover:opacity-100 flex items-center gap-0.5">
                            كشف 🔍
                          </span>
                        </div>
                      </td>

                      {/* Month 1 */}
                      <td className="py-1 px-1 border-r border-slate-200">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          value={mark.m1MonthAvg && mark.m1MonthAvg > 0 ? mark.m1MonthAvg : ''}
                          onChange={e => {
                            const val = parseInt(e.target.value, 10);
                            handleFieldChange(std.id, 'm1MonthAvg', isNaN(val) ? 0 : val);
                          }}
                          placeholder="—"
                          className="w-13 text-center py-1 rounded-md font-mono text-xs border border-slate-200 focus:border-indigo-500 outline-none"
                        />
                      </td>

                      {/* Month 2 */}
                      <td className="py-1 px-1 border-r border-slate-200">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          value={mark.m2MonthAvg && mark.m2MonthAvg > 0 ? mark.m2MonthAvg : ''}
                          onChange={e => {
                            const val = parseInt(e.target.value, 10);
                            handleFieldChange(std.id, 'm2MonthAvg', isNaN(val) ? 0 : val);
                          }}
                          placeholder="—"
                          className="w-13 text-center py-1 rounded-md font-mono text-xs border border-slate-200 focus:border-indigo-500 outline-none"
                        />
                      </td>

                      {/* Optional Month 3 (Term 1) */}
                      {showThirdMonthTerm1 && (
                        <td className="py-1 px-1 border-r border-slate-200 bg-indigo-50/50">
                          <input
                            type="number"
                            min="0"
                            max="100"
                            value={mark.m1Month3 && mark.m1Month3 > 0 ? mark.m1Month3 : ''}
                            onChange={e => {
                              const val = parseInt(e.target.value, 10);
                              handleFieldChange(std.id, 'm1Month3', isNaN(val) ? 0 : val);
                            }}
                            placeholder="—"
                            className="w-13 text-center py-1 rounded-md font-mono text-xs border border-indigo-200 focus:border-indigo-500 bg-white outline-none"
                          />
                        </td>
                      )}

                      {/* Term 1 Average (Auto) */}
                      <td className="py-1.5 px-1 border-r border-slate-200 font-mono font-black text-blue-900 bg-blue-50/60 text-xs">
                        {mark.term1Avg && mark.term1Avg > 0 ? mark.term1Avg : '—'}
                      </td>

                      {/* Midterm Final Grade */}
                      <td className="py-1 px-1 border-r border-slate-200">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          value={mark.midtermFinalGrade && mark.midtermFinalGrade > 0 ? mark.midtermFinalGrade : ''}
                          onChange={e => {
                            const val = parseInt(e.target.value, 10);
                            handleFieldChange(std.id, 'midtermFinalGrade', isNaN(val) ? 0 : val);
                          }}
                          placeholder="—"
                          className="w-14 text-center py-1 rounded-md font-mono font-bold text-xs border border-emerald-300 focus:border-emerald-500 bg-emerald-50/30 outline-none"
                        />
                      </td>

                      {/* Month 3 (Term 2) */}
                      <td className="py-1 px-1 border-r border-slate-200">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          value={mark.m3MonthAvg && mark.m3MonthAvg > 0 ? mark.m3MonthAvg : ''}
                          onChange={e => {
                            const val = parseInt(e.target.value, 10);
                            handleFieldChange(std.id, 'm3MonthAvg', isNaN(val) ? 0 : val);
                          }}
                          placeholder="—"
                          className="w-13 text-center py-1 rounded-md font-mono text-xs border border-slate-200 focus:border-indigo-500 outline-none"
                        />
                      </td>

                      {/* Month 4 (Term 2) */}
                      <td className="py-1 px-1 border-r border-slate-200">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          value={mark.m4MonthAvg && mark.m4MonthAvg > 0 ? mark.m4MonthAvg : ''}
                          onChange={e => {
                            const val = parseInt(e.target.value, 10);
                            handleFieldChange(std.id, 'm4MonthAvg', isNaN(val) ? 0 : val);
                          }}
                          placeholder="—"
                          className="w-13 text-center py-1 rounded-md font-mono text-xs border border-slate-200 focus:border-indigo-500 outline-none"
                        />
                      </td>

                      {/* Optional Month 3 (Term 2) */}
                      {showThirdMonthTerm2 && (
                        <td className="py-1 px-1 border-r border-slate-200 bg-purple-50/50">
                          <input
                            type="number"
                            min="0"
                            max="100"
                            value={mark.m2Month3 && mark.m2Month3 > 0 ? mark.m2Month3 : ''}
                            onChange={e => {
                              const val = parseInt(e.target.value, 10);
                              handleFieldChange(std.id, 'm2Month3', isNaN(val) ? 0 : val);
                            }}
                            placeholder="—"
                            className="w-13 text-center py-1 rounded-md font-mono text-xs border border-purple-200 focus:border-purple-500 bg-white outline-none"
                          />
                        </td>
                      )}

                      {/* Term 2 Average (Auto) */}
                      <td className="py-1.5 px-1 border-r border-slate-200 font-mono font-black text-purple-900 bg-purple-50/60 text-xs">
                        {mark.term2Avg && mark.term2Avg > 0 ? mark.term2Avg : '—'}
                      </td>

                      {/* Annual Average / السعي السنوي (Auto) */}
                      <td className="py-1.5 px-2 border-r border-slate-200 font-mono font-black text-amber-950 bg-amber-100/70 text-xs">
                        {mark.annualAverage && mark.annualAverage > 0 ? mark.annualAverage : '—'}
                      </td>

                      {/* Final D1 */}
                      <td className="py-1 px-1 border-r border-slate-200">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          value={mark.finalWrittenD1 && mark.finalWrittenD1 > 0 ? mark.finalWrittenD1 : ''}
                          onChange={e => {
                            const val = parseInt(e.target.value, 10);
                            handleFieldChange(std.id, 'finalWrittenD1', isNaN(val) ? 0 : val);
                          }}
                          placeholder="—"
                          className="w-13 text-center py-1 rounded-md font-mono text-xs border border-rose-200 focus:border-rose-500 outline-none"
                        />
                      </td>

                      {/* Final D2 */}
                      <td className="py-1 px-1 border-r border-slate-200">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          value={mark.finalWrittenD2 && mark.finalWrittenD2 > 0 ? mark.finalWrittenD2 : ''}
                          onChange={e => {
                            const val = parseInt(e.target.value, 10);
                            handleFieldChange(std.id, 'finalWrittenD2', isNaN(val) ? null : val);
                          }}
                          placeholder="—"
                          className="w-13 text-center py-1 rounded-md font-mono text-xs border border-purple-200 focus:border-purple-500 outline-none"
                        />
                      </td>

                      {/* Final Grade (Auto) */}
                      <td className="py-1.5 px-2 border-r border-slate-200 font-mono">
                        <span className={`inline-block px-2.5 py-1 rounded-lg font-black text-xs ${
                          isFail 
                            ? 'bg-rose-100 text-rose-800 border border-rose-300' 
                            : isPass 
                              ? 'bg-emerald-100 text-emerald-900 border border-emerald-300' 
                              : 'text-slate-400'
                        }`}>
                          {mark.finalGrade && mark.finalGrade > 0 ? mark.finalGrade : '—'}
                        </span>
                      </td>

                      {/* Result */}
                      <td className="py-1.5 px-2 text-center font-bold">
                        {isPass ? (
                          <span className="text-emerald-700 font-black text-xs">ناجح</span>
                        ) : isFail ? (
                          <span className="text-rose-600 font-black text-xs">راسب</span>
                        ) : (
                          <span className="text-slate-400 text-xs">—</span>
                        )}
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
