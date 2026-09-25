import React, { useState } from 'react';
import { Student, StudentMark } from '../../types';
import { 
  X, 
  Printer, 
  Award, 
  Calendar, 
  User, 
  FileText, 
  CheckCircle, 
  AlertTriangle, 
  Sparkles,
  TrendingUp,
  Percent,
  Layers,
  ChevronDown
} from 'lucide-react';
import { normalizeArabic } from '../../utils/syncService';
import { isOralAndWrittenSubject, applyMinisterialGrace } from '../../utils/gradeCalculations';

interface StudentGradeProfileModalProps {
  student: Student | null;
  isOpen: boolean;
  onClose: () => void;
  subjectsList: string[];
  schoolName?: string;
  onSaveMark?: (subject: string, updatedMark: Partial<StudentMark>) => void;
}

export const StudentGradeProfileModal: React.FC<StudentGradeProfileModalProps> = ({
  student,
  isOpen,
  onClose,
  subjectsList,
  schoolName = 'ثانوية المتميزين للبنين',
  onSaveMark
}) => {
  const [viewMode, setViewMode] = useState<'detailed' | 'summary'>('detailed');
  const [enableGraceDecision, setEnableGraceDecision] = useState<boolean>(false);

  if (!isOpen || !student) return null;

  const marksHistory = student.marksHistory || [];

  const getMarkForSubject = (subj: string): StudentMark => {
    const norm = normalizeArabic(subj);
    const found = marksHistory.find(m => normalizeArabic(m.subject) === norm);
    return found || {
      year: student.registrationYear || '2025-2026',
      subject: subj,
      term1Avg: 0,
      midtermFinalGrade: 0,
      term2Avg: 0,
      annualAverage: 0,
      finalWrittenD1: 0,
      finalGrade: 0
    };
  };

  // تجهيز مصفوفة الدرجات للتقييم الإجمالي ودرجات القرار
  const evaluationList = subjectsList.map(subj => {
    const mark = getMarkForSubject(subj);
    // الدرجة المعتمدة للتقييم: النهائية > السعي > نصف السنة > معدل ف1
    const score = (mark.finalGrade && mark.finalGrade > 0) ? mark.finalGrade :
      (mark.annualAverage && mark.annualAverage > 0) ? mark.annualAverage :
      (mark.midtermFinalGrade && mark.midtermFinalGrade > 0) ? mark.midtermFinalGrade :
      (mark.term1Avg && mark.term1Avg > 0) ? mark.term1Avg : 0;

    return { subject: subj, score };
  });

  const validScores = evaluationList.filter(e => e.score > 0);
  const totalMarks = validScores.reduce((acc, curr) => acc + curr.score, 0);
  const generalAverage = validScores.length > 0 ? Math.round(totalMarks / validScores.length) : 0;

  // فحص درجات القرار
  const graceAnalysis = applyMinisterialGrace(evaluationList, 5);

  const getFailingCount = () => {
    if (enableGraceDecision) {
      return graceAnalysis.newFails;
    }
    return evaluationList.filter(e => e.score > 0 && e.score < 50).length;
  };

  const failingCount = getFailingCount();
  const studentStatus = validScores.length === 0 ? 'غير مرصود' :
    failingCount === 0 ? 'ناجح' :
    failingCount <= 2 ? `مكمل (${failingCount})` : `راسب (${failingCount})`;

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-[70] bg-slate-950/85 backdrop-blur-md flex items-center justify-center p-3 sm:p-5 dir-rtl overflow-y-auto">
      <div className="bg-white w-full max-w-6xl rounded-3xl shadow-2xl flex flex-col max-h-[94vh] overflow-hidden border-2 border-amber-500/40 animate-in fade-in zoom-in-95 duration-200">

        {/* 1. Header Bar */}
        <div className="p-5 bg-gradient-to-r from-amber-700 via-amber-600 to-orange-700 text-white flex items-center justify-between shadow-md shrink-0">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-white/15 flex items-center justify-center border-2 border-white/30 shadow-inner">
              <Award className="w-8 h-8 text-amber-300" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl sm:text-2xl font-black tracking-tight">بطاقة الدرجات التفصيلية الشاملة</h2>
                <span className="px-2.5 py-0.5 rounded-full text-[11px] font-black bg-amber-400 text-slate-950 shadow-xs">
                  كشف 360°
                </span>
              </div>
              <p className="text-amber-100/90 text-xs sm:text-sm font-bold mt-0.5">
                {schoolName} • العام الدراسي {student.registrationYear || '2025-2026'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              className="px-4 py-2 bg-white/20 hover:bg-white/30 text-white rounded-xl font-bold text-xs flex items-center gap-1.5 transition-all cursor-pointer shadow-xs active:scale-95"
              title="طباعة كشف درجات رسمي A4"
            >
              <Printer className="w-4 h-4 text-amber-200" />
              <span className="hidden sm:inline">طباعة الكشف</span>
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded-xl bg-white/15 hover:bg-rose-600 text-white transition-all cursor-pointer"
              title="إغلاق النافذة"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* 2. Student Info & Quick KPI Ribbon */}
        <div className="p-4 bg-slate-50 border-b border-slate-200 grid grid-cols-2 md:grid-cols-5 gap-3 shrink-0 text-xs">
          <div className="bg-white p-2.5 rounded-xl border border-slate-200 shadow-2xs flex items-center gap-2.5">
            <User className="w-4 h-4 text-amber-600 shrink-0" />
            <div className="truncate">
              <span className="text-[10px] font-black text-slate-400 block">اسم الطالب الرباعي:</span>
              <span className="font-black text-slate-900 text-xs truncate block">
                {student.firstName} {student.secondName} {student.thirdName} {student.fourthName || ''} {student.titleName || ''}
              </span>
            </div>
          </div>

          <div className="bg-white p-2.5 rounded-xl border border-slate-200 shadow-2xs flex items-center gap-2.5">
            <Calendar className="w-4 h-4 text-blue-600 shrink-0" />
            <div>
              <span className="text-[10px] font-black text-slate-400 block">الصف والشعبة:</span>
              <span className="font-black text-slate-900 text-xs">
                {student.currentGrade} - ({student.section})
              </span>
            </div>
          </div>

          <div className="bg-white p-2.5 rounded-xl border border-slate-200 shadow-2xs flex items-center gap-2.5">
            <FileText className="w-4 h-4 text-purple-600 shrink-0" />
            <div>
              <span className="text-[10px] font-black text-slate-400 block">رقم القيد المدرسي:</span>
              <span className="font-black text-slate-900 font-mono text-xs">#{student.recordNumber}</span>
            </div>
          </div>

          <div className="bg-white p-2.5 rounded-xl border border-slate-200 shadow-2xs flex items-center gap-2.5">
            <TrendingUp className="w-4 h-4 text-emerald-600 shrink-0" />
            <div>
              <span className="text-[10px] font-black text-slate-400 block">المعدل العام التراكمي:</span>
              <span className="font-black text-emerald-700 font-mono text-xs">
                {generalAverage > 0 ? `${generalAverage}%` : '—'}
              </span>
            </div>
          </div>

          <div className={`p-2.5 rounded-xl border shadow-2xs flex items-center gap-2.5 ${
            studentStatus === 'ناجح' ? 'bg-emerald-50 border-emerald-200 text-emerald-900' :
            studentStatus.startsWith('مكمل') ? 'bg-amber-50 border-amber-200 text-amber-900' :
            studentStatus.startsWith('راسب') ? 'bg-rose-50 border-rose-200 text-rose-900' :
            'bg-slate-100 border-slate-200 text-slate-700'
          }`}>
            <Award className="w-4 h-4 shrink-0" />
            <div>
              <span className="text-[10px] font-black opacity-60 block">النتيجة التقديرية:</span>
              <span className="font-black text-xs">{studentStatus}</span>
            </div>
          </div>
        </div>

        {/* 3. Controls Bar: View Mode & Decision Simulation */}
        <div className="px-5 py-2.5 bg-slate-100 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3 text-xs shrink-0">
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-600">نمط عرض السجل:</span>
            <div className="inline-flex rounded-xl bg-slate-200 p-0.5 border border-slate-300">
              <button
                type="button"
                onClick={() => setViewMode('detailed')}
                className={`px-3 py-1 rounded-lg font-black transition-all cursor-pointer ${
                  viewMode === 'detailed' 
                    ? 'bg-amber-600 text-white shadow-xs' 
                    : 'text-slate-700 hover:text-slate-900'
                }`}
              >
                📊 التفصيل الكامل (يومي + أشهر + فصول + سعي)
              </button>
              <button
                type="button"
                onClick={() => setViewMode('summary')}
                className={`px-3 py-1 rounded-lg font-black transition-all cursor-pointer ${
                  viewMode === 'summary' 
                    ? 'bg-amber-600 text-white shadow-xs' 
                    : 'text-slate-700 hover:text-slate-900'
                }`}
              >
                📋 الشهادة الرسمية المختصرة
              </button>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 cursor-pointer select-none bg-white px-3 py-1 rounded-xl border border-slate-300 hover:border-amber-400 transition-colors shadow-2xs">
              <input
                type="checkbox"
                checked={enableGraceDecision}
                onChange={e => setEnableGraceDecision(e.target.checked)}
                className="w-4 h-4 accent-amber-600 rounded cursor-pointer"
              />
              <span className="font-black text-slate-800 text-[11px] flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                تطبيق محاكي درجات القرار (5 درجات)
              </span>
            </label>

            {enableGraceDecision && (
              <span className="text-[11px] font-bold text-amber-800 bg-amber-100 px-2 py-0.5 rounded-lg border border-amber-200">
                المستخدم من القرار: {graceAnalysis.totalGraceUsed} من 5 درجات
              </span>
            )}
          </div>
        </div>

        {/* 4. Table Area: Subjects in Column 1, Progressive Grades Across Columns */}
        <div className="flex-1 overflow-x-auto overflow-y-auto p-4 custom-scrollbar bg-slate-50/50">
          <table className="w-full text-center border-collapse text-xs bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden min-w-[900px]">
            <thead>
              {viewMode === 'detailed' ? (
                <>
                  <tr className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 text-white font-black text-[11px]">
                    <th rowSpan={2} className="py-2.5 px-3 border-r border-slate-700 text-right min-w-[160px] sticky right-0 bg-slate-900 z-10">
                      المادة الدراسية
                    </th>
                    <th colSpan={4} className="py-1 px-1 border-r border-slate-700 bg-blue-900/60 text-blue-200">
                      الفصل الأول
                    </th>
                    <th colSpan={2} className="py-1 px-1 border-r border-slate-700 bg-emerald-900/60 text-emerald-200">
                      نصف السنة
                    </th>
                    <th colSpan={4} className="py-1 px-1 border-r border-slate-700 bg-purple-900/60 text-purple-200">
                      الفصل الثاني
                    </th>
                    <th rowSpan={2} className="py-2.5 px-2 border-r border-slate-700 bg-amber-900/60 text-amber-200 min-w-[70px]">
                      السعي السنوي
                    </th>
                    <th colSpan={3} className="py-1 px-1 border-r border-slate-700 bg-rose-900/60 text-rose-200">
                      الامتحان النهائي
                    </th>
                    <th rowSpan={2} className="py-2.5 px-2 border-r border-slate-700 bg-amber-600 text-slate-950 min-w-[75px] font-black">
                      الدرجة النهائية
                    </th>
                    <th rowSpan={2} className="py-2.5 px-2 text-center min-w-[65px] font-black">
                      الحالة
                    </th>
                  </tr>
                  <tr className="bg-slate-800 text-slate-200 font-bold text-[10px] border-b-2 border-slate-300">
                    {/* ف1 */}
                    <th className="py-1.5 px-1 border-r border-slate-700">يومي 1</th>
                    <th className="py-1.5 px-1 border-r border-slate-700">ش 1</th>
                    <th className="py-1.5 px-1 border-r border-slate-700">ش 2</th>
                    <th className="py-1.5 px-1 border-r border-slate-700 bg-blue-950 text-blue-300 font-black">معدل ف1</th>
                    {/* نصف السنة */}
                    <th className="py-1.5 px-1 border-r border-slate-700">شفهي</th>
                    <th className="py-1.5 px-1 border-r border-slate-700 bg-emerald-950 text-emerald-300 font-black">الدرجة</th>
                    {/* ف2 */}
                    <th className="py-1.5 px-1 border-r border-slate-700">يومي 2</th>
                    <th className="py-1.5 px-1 border-r border-slate-700">ش 3</th>
                    <th className="py-1.5 px-1 border-r border-slate-700">ش 4</th>
                    <th className="py-1.5 px-1 border-r border-slate-700 bg-purple-950 text-purple-300 font-black">معدل ف2</th>
                    {/* النهائي */}
                    <th className="py-1.5 px-1 border-r border-slate-700">شفهي</th>
                    <th className="py-1.5 px-1 border-r border-slate-700">دور 1</th>
                    <th className="py-1.5 px-1 border-r border-slate-700">دور 2</th>
                  </tr>
                </>
              ) : (
                <tr className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 text-white font-black text-xs border-b-2 border-slate-300">
                  <th className="py-3 px-3 border-r border-slate-700 text-right min-w-[180px]">المادة الدراسية</th>
                  <th className="py-3 px-2 border-r border-slate-700">معدل الفصل الأول</th>
                  <th className="py-3 px-2 border-r border-slate-700">درجة نصف السنة</th>
                  <th className="py-3 px-2 border-r border-slate-700">معدل الفصل الثاني</th>
                  <th className="py-3 px-2 border-r border-slate-700 bg-amber-900/40 text-amber-200">السعي السنوي</th>
                  <th className="py-3 px-2 border-r border-slate-700">الامتحان النهائي</th>
                  <th className="py-3 px-2 border-r border-slate-700 bg-amber-600 text-slate-950 font-black">الدرجة النهائية</th>
                  {enableGraceDecision && <th className="py-3 px-2 border-r border-slate-700 bg-purple-900/60 text-purple-200">بعد القرار</th>}
                  <th className="py-3 px-2 text-center">النتيجة</th>
                </tr>
              )}
            </thead>

            <tbody className="divide-y divide-slate-200">
              {subjectsList.map((subjectName, idx) => {
                const mark = getMarkForSubject(subjectName);
                const isOral = isOralAndWrittenSubject(subjectName);
                const graceItem = graceAnalysis.items.find(g => g.subject === subjectName);
                const hasGrace = enableGraceDecision && graceItem && graceItem.graceUsed > 0;
                
                // حساب الدرجة النهائية المعروضة
                const rawFinal = mark.finalGrade && mark.finalGrade > 0 ? mark.finalGrade : mark.annualAverage || 0;
                const effectiveFinal = hasGrace ? graceItem.finalScoreWithGrace : rawFinal;
                const isFailed = effectiveFinal > 0 && effectiveFinal < 50;

                // يومي مجمع
                const m1DailyAvg = mark.m1Daily && mark.m1Daily.length > 0 
                  ? Math.round(mark.m1Daily.filter(n => n > 0).reduce((a, b) => a + b, 0) / (mark.m1Daily.filter(n => n > 0).length || 1)) 
                  : 0;
                const m2DailyAvg = mark.m3Daily && mark.m3Daily.length > 0 
                  ? Math.round(mark.m3Daily.filter(n => n > 0).reduce((a, b) => a + b, 0) / (mark.m3Daily.filter(n => n > 0).length || 1)) 
                  : 0;
                const midtermOralScore = mark.midtermOral && mark.midtermOral.length > 0 ? mark.midtermOral[0] : 0;
                const finalOralScore = mark.finalOral && mark.finalOral.length > 0 ? mark.finalOral[0] : 0;

                return (
                  <tr key={idx} className="hover:bg-amber-50/50 transition-colors">
                    {/* Subject Name (Column 1) */}
                    <td className="py-2.5 px-3 border-r border-slate-200 text-right font-black text-slate-900 sticky right-0 bg-white">
                      <div className="flex items-center gap-2">
                        <div className={`w-2.5 h-2.5 rounded-full ${isOral ? 'bg-amber-500' : 'bg-blue-500'}`} />
                        <span>{subjectName}</span>
                        {isOral && (
                          <span className="text-[9px] font-black px-1.5 py-0.2 rounded-md bg-amber-100 text-amber-800 border border-amber-200">
                            شفهي
                          </span>
                        )}
                      </div>
                    </td>

                    {viewMode === 'detailed' ? (
                      <>
                        {/* الفصل الأول */}
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-600 bg-slate-50/50">
                          {m1DailyAvg > 0 ? m1DailyAvg : '—'}
                        </td>
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-800 font-bold">
                          {mark.m1MonthAvg || '—'}
                        </td>
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-800 font-bold">
                          {mark.m2MonthAvg || '—'}
                        </td>
                        <td className="py-2 px-1 border-r border-slate-200 font-mono font-black text-blue-900 bg-blue-50/50">
                          {mark.term1Avg || '—'}
                        </td>

                        {/* نصف السنة */}
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-600 bg-slate-50/50">
                          {isOral && midtermOralScore > 0 ? midtermOralScore : '—'}
                        </td>
                        <td className="py-2 px-1 border-r border-slate-200 font-mono font-black text-emerald-900 bg-emerald-50/50">
                          {mark.midtermFinalGrade || '—'}
                        </td>

                        {/* الفصل الثاني */}
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-600 bg-slate-50/50">
                          {m2DailyAvg > 0 ? m2DailyAvg : '—'}
                        </td>
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-800 font-bold">
                          {mark.m3MonthAvg || '—'}
                        </td>
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-800 font-bold">
                          {mark.m4MonthAvg || '—'}
                        </td>
                        <td className="py-2 px-1 border-r border-slate-200 font-mono font-black text-purple-900 bg-purple-50/50">
                          {mark.term2Avg || '—'}
                        </td>

                        {/* السعي السنوي */}
                        <td className="py-2 px-1 border-r border-slate-200 font-mono font-black text-amber-950 bg-amber-50">
                          {mark.annualAverage || '—'}
                        </td>

                        {/* الامتحان النهائي */}
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-600 bg-slate-50/50">
                          {isOral && finalOralScore > 0 ? finalOralScore : '—'}
                        </td>
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-800 font-bold">
                          {mark.finalWrittenD1 || '—'}
                        </td>
                        <td className="py-2 px-1 border-r border-slate-200 font-mono text-slate-800">
                          {mark.finalWrittenD2 || '—'}
                        </td>

                        {/* الدرجة النهائية */}
                        <td className="py-2 px-1 border-r border-slate-200 font-mono">
                          <div className={`py-1 rounded-lg font-black text-xs ${
                            isFailed 
                              ? 'bg-rose-100 text-rose-800 border border-rose-200' 
                              : effectiveFinal >= 50 
                                ? 'bg-emerald-100 text-emerald-900 border border-emerald-200' 
                                : 'text-slate-400'
                          }`}>
                            {effectiveFinal > 0 ? effectiveFinal : '—'}
                            {hasGrace && (
                              <span className="text-[9px] text-amber-700 block font-bold">
                                (+{graceItem.graceUsed} قرار)
                              </span>
                            )}
                          </div>
                        </td>

                        {/* النتيجة */}
                        <td className="py-2 px-1 text-center font-bold">
                          {effectiveFinal >= 50 ? (
                            <span className="text-emerald-700 text-[11px] font-black">ناجح</span>
                          ) : effectiveFinal > 0 ? (
                            <span className="text-rose-600 text-[11px] font-black">راسب</span>
                          ) : (
                            <span className="text-slate-400 text-[10px]">مستمر</span>
                          )}
                        </td>
                      </>
                    ) : (
                      <>
                        <td className="py-2.5 px-2 border-r border-slate-200 font-mono font-bold text-slate-700">
                          {mark.term1Avg || '—'}
                        </td>
                        <td className="py-2.5 px-2 border-r border-slate-200 font-mono font-bold text-slate-700">
                          {mark.midtermFinalGrade || '—'}
                        </td>
                        <td className="py-2.5 px-2 border-r border-slate-200 font-mono font-bold text-slate-700">
                          {mark.term2Avg || '—'}
                        </td>
                        <td className="py-2.5 px-2 border-r border-slate-200 font-mono font-black text-amber-950 bg-amber-50">
                          {mark.annualAverage || '—'}
                        </td>
                        <td className="py-2.5 px-2 border-r border-slate-200 font-mono font-bold text-slate-700">
                          {mark.finalWrittenD2 || mark.finalWrittenD1 || '—'}
                        </td>
                        <td className="py-2.5 px-2 border-r border-slate-200 font-mono">
                          <span className={`px-2.5 py-1 rounded-lg font-black text-xs ${
                            isFailed 
                              ? 'bg-rose-100 text-rose-800' 
                              : effectiveFinal >= 50 
                                ? 'bg-emerald-100 text-emerald-900' 
                                : 'text-slate-400'
                          }`}>
                            {effectiveFinal > 0 ? effectiveFinal : '—'}
                          </span>
                        </td>
                        {enableGraceDecision && (
                          <td className="py-2.5 px-2 border-r border-slate-200 font-mono text-purple-900 font-black">
                            {hasGrace ? (
                              <span className="bg-purple-100 text-purple-900 px-2 py-0.5 rounded-md text-xs">
                                50 (+{graceItem.graceUsed})
                              </span>
                            ) : (
                              '—'
                            )}
                          </td>
                        )}
                        <td className="py-2.5 px-2 text-center font-bold">
                          {effectiveFinal >= 50 ? (
                            <span className="text-emerald-700 font-black text-xs">ناجح</span>
                          ) : effectiveFinal > 0 ? (
                            <span className="text-rose-600 font-black text-xs">راسب</span>
                          ) : (
                            <span className="text-slate-400 text-xs">—</span>
                          )}
                        </td>
                      </>
                    )}

                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* 5. Official Iraqi A4 Print Footer & Seal */}
        <div className="p-4 bg-slate-50 border-t border-slate-200 flex flex-wrap items-center justify-between gap-4 text-xs shrink-0">
          <div className="flex items-center gap-4 text-slate-500 font-bold">
            <span>مدير المدرسة: ............................</span>
            <span>معاون شؤون الطلبة: ............................</span>
            <span>الختم الرسمي: [ .................... ]</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              className="px-6 py-2.5 bg-gradient-to-r from-amber-600 to-orange-600 hover:from-amber-700 hover:to-orange-700 text-white rounded-xl font-black text-xs shadow-md flex items-center gap-2 transition-all cursor-pointer active:scale-95"
            >
              <Printer className="w-4 h-4" />
              <span>طباعة كشف الدرجات الرسمي A4</span>
            </button>
            <button
              onClick={onClose}
              className="px-5 py-2.5 bg-white border border-slate-300 hover:bg-slate-100 text-slate-700 rounded-xl font-bold text-xs transition-all cursor-pointer"
            >
              إغلاق النافذة
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
