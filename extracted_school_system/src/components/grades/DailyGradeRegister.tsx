import React, { useState } from 'react';
import { Student, StudentMark } from '../../types';
import { 
  BookOpen, 
  Sparkles, 
  HelpCircle, 
  Layers, 
  Save, 
  CheckCircle2, 
  Calculator, 
  Sliders,
  ChevronRight,
  ChevronLeft,
  Calendar,
  Maximize2
} from 'lucide-react';
import { normalizeArabic } from '../../utils/syncService';
import { isOralAndWrittenSubject, calculateMonthScore, calculateExamScore } from '../../utils/gradeCalculations';

interface DailyGradeRegisterProps {
  students: Student[];
  onUpdateStudentMark: (studentId: string, subject: string, updater: (prevMark: StudentMark) => StudentMark) => void;
  onSelectStudent: (student: Student) => void;
  subjectsList: string[];
}

export type SubStage = 'm1' | 'm2' | 'midterm' | 'm3' | 'm4' | 'final';

export const DailyGradeRegister: React.FC<DailyGradeRegisterProps> = ({
  students,
  onUpdateStudentMark,
  onSelectStudent,
  subjectsList
}) => {
  const [selectedSubject, setSelectedSubject] = useState<string>(() => subjectsList[0] || 'اللغة العربية');
  const [subStage, setSubStage] = useState<SubStage>('m1');
  
  // طريقة حساب اليومي لكل مادة (تخزن في localStorage أو بحالة المكون)
  const [calcModes, setCalcModes] = useState<{ [subject: string]: 'sum' | 'average' }>(() => {
    try {
      const saved = localStorage.getItem('diyala_daily_calc_modes');
      return saved ? JSON.parse(saved) : {};
    } catch {
      return {};
    }
  });

  const isOral = isOralAndWrittenSubject(selectedSubject);
  const currentCalcMode = calcModes[selectedSubject] || 'average';

  const toggleCalcMode = (mode: 'sum' | 'average') => {
    const updated = { ...calcModes, [selectedSubject]: mode };
    setCalcModes(updated);
    try {
      localStorage.setItem('diyala_daily_calc_modes', JSON.stringify(updated));
    } catch {}
  };

  const getStudentMark = (std: Student, subject: string): StudentMark => {
    const norm = normalizeArabic(subject);
    const m = (std.marksHistory || []).find(x => normalizeArabic(x.subject) === norm);
    return m || {
      year: std.registrationYear || '2025-2026',
      subject,
      m1Daily: [0, 0, 0, 0, 0],
      m1Written: 0,
      m1MonthAvg: 0,
      m2Daily: [0, 0, 0, 0, 0],
      m2Written: 0,
      m2MonthAvg: 0,
      midtermOral: [0],
      midtermScore: 0,
      midtermFinalGrade: 0,
      m3Daily: [0, 0, 0, 0, 0],
      m3Written: 0,
      m3MonthAvg: 0,
      m4Daily: [0, 0, 0, 0, 0],
      m4Written: 0,
      m4MonthAvg: 0,
      finalOral: [0],
      finalWrittenD1: 0,
      finalWrittenD2: null,
      finalGrade: 0
    };
  };

  // معالجة تغيير درجة يومية محددة
  const handleDailyChange = (studentId: string, dailyIndex: number, val: number) => {
    onUpdateStudentMark(studentId, selectedSubject, prev => {
      const next = { ...prev };
      let list: number[] = [];
      if (subStage === 'm1') {
        list = [...(next.m1Daily || [0, 0, 0, 0, 0])];
        while (list.length < 5) list.push(0);
        list[dailyIndex] = val;
        next.m1Daily = list;
        next.m1MonthAvg = calculateMonthScore(list, next.m1Written || 0, isOral ? (next.m1Daily?.[4] || 0) : 0, currentCalcMode, isOral);
      } else if (subStage === 'm2') {
        list = [...(next.m2Daily || [0, 0, 0, 0, 0])];
        while (list.length < 5) list.push(0);
        list[dailyIndex] = val;
        next.m2Daily = list;
        next.m2MonthAvg = calculateMonthScore(list, next.m2Written || 0, isOral ? (next.m2Daily?.[4] || 0) : 0, currentCalcMode, isOral);
      } else if (subStage === 'm3') {
        list = [...(next.m3Daily || [0, 0, 0, 0, 0])];
        while (list.length < 5) list.push(0);
        list[dailyIndex] = val;
        next.m3Daily = list;
        next.m3MonthAvg = calculateMonthScore(list, next.m3Written || 0, isOral ? (next.m3Daily?.[4] || 0) : 0, currentCalcMode, isOral);
      } else if (subStage === 'm4') {
        list = [...(next.m4Daily || [0, 0, 0, 0, 0])];
        while (list.length < 5) list.push(0);
        list[dailyIndex] = val;
        next.m4Daily = list;
        next.m4MonthAvg = calculateMonthScore(list, next.m4Written || 0, isOral ? (next.m4Daily?.[4] || 0) : 0, currentCalcMode, isOral);
      }
      return next;
    });
  };

  // معالجة تغيير الدرجة التحريرية أو الشفوية
  const handleScoreChange = (studentId: string, field: 'written' | 'oral' | 'finalD2', val: number) => {
    onUpdateStudentMark(studentId, selectedSubject, prev => {
      const next = { ...prev };
      if (subStage === 'm1') {
        if (field === 'written') next.m1Written = val;
        next.m1MonthAvg = calculateMonthScore(next.m1Daily || [], next.m1Written || 0, 0, currentCalcMode, isOral);
      } else if (subStage === 'm2') {
        if (field === 'written') next.m2Written = val;
        next.m2MonthAvg = calculateMonthScore(next.m2Daily || [], next.m2Written || 0, 0, currentCalcMode, isOral);
      } else if (subStage === 'midterm') {
        if (field === 'written') next.midtermScore = val;
        if (field === 'oral') next.midtermOral = [val];
        next.midtermFinalGrade = calculateExamScore(next.midtermScore || 0, next.midtermOral?.[0] || 0, isOral);
      } else if (subStage === 'm3') {
        if (field === 'written') next.m3Written = val;
        next.m3MonthAvg = calculateMonthScore(next.m3Daily || [], next.m3Written || 0, 0, currentCalcMode, isOral);
      } else if (subStage === 'm4') {
        if (field === 'written') next.m4Written = val;
        next.m4MonthAvg = calculateMonthScore(next.m4Daily || [], next.m4Written || 0, 0, currentCalcMode, isOral);
      } else if (subStage === 'final') {
        if (field === 'oral') next.finalOral = [val];
        if (field === 'written') next.finalWrittenD1 = val;
        if (field === 'finalD2') next.finalWrittenD2 = val;
        const examScore = calculateExamScore(next.finalWrittenD2 || next.finalWrittenD1 || 0, next.finalOral?.[0] || 0, isOral);
        next.finalExamTotal = examScore;
      }
      return next;
    });
  };

  return (
    <div className="space-y-4">
      
      {/* 1. الشريط العلوي: أسماء الدروس وتمييز الدروس بطبيعتها (شفهي + تحريري / تحريري) */}
      <div className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="flex items-center justify-between text-xs pb-1 border-b border-slate-100">
          <div className="flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-amber-600" />
            <span className="font-black text-slate-800">اختر المادة الدراسية لتسجيل اليومي والنشاط:</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="flex items-center gap-1 text-[11px] font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200">
              <span className="w-2 h-2 rounded-full bg-amber-500" />
              مادة لغوية / إسلامية (تحتاج شفهي)
            </span>
            <span className="flex items-center gap-1 text-[11px] font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded-md border border-blue-200">
              <span className="w-2 h-2 rounded-full bg-blue-500" />
              مادة علمية / عامة (تحريري ونشاط)
            </span>
          </div>
        </div>

        {/* Subjects Horizontal Ribbon */}
        <div className="flex flex-wrap items-center gap-2">
          {subjectsList.map(subj => {
            const oral = isOralAndWrittenSubject(subj);
            const isSelected = selectedSubject === subj;

            return (
              <button
                key={subj}
                type="button"
                onClick={() => setSelectedSubject(subj)}
                className={`px-3 py-1.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center gap-1.5 border shadow-2xs ${
                  isSelected
                    ? 'bg-gradient-to-r from-amber-600 to-orange-600 text-white border-amber-500 shadow-sm scale-102 ring-2 ring-amber-300'
                    : oral
                      ? 'bg-amber-50/70 hover:bg-amber-100 text-amber-950 border-amber-200'
                      : 'bg-slate-50 hover:bg-slate-100 text-slate-800 border-slate-200'
                }`}
              >
                <span>{subj}</span>
                <span className={`text-[9px] px-1.5 py-0.2 rounded-full font-bold ${
                  isSelected 
                    ? 'bg-white/20 text-white' 
                    : oral ? 'bg-amber-200 text-amber-900' : 'bg-slate-200 text-slate-700'
                }`}>
                  {oral ? 'شفهي+تحريري' : 'تحريري'}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. شريط ضبط طريقة حساب اليومي والمراحل الداخلية (سجل داخله سجلات) */}
      <div className="bg-white p-3 rounded-2xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-3 text-xs">
        
        {/* Sub-stages: الفصل الأول (ش1، ش2) -> نصف السنة -> الفصل الثاني (ش3، ش4) -> آخر السنة */}
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="font-bold text-slate-500 ml-1">المحطة:</span>
          
          <button
            type="button"
            onClick={() => setSubStage('m1')}
            className={`px-3 py-1.5 rounded-xl font-black text-xs transition-all cursor-pointer border ${
              subStage === 'm1' 
                ? 'bg-blue-600 text-white border-blue-700 shadow-xs' 
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200 border-slate-200'
            }`}
          >
            ف1: الشهر الأول
          </button>

          <button
            type="button"
            onClick={() => setSubStage('m2')}
            className={`px-3 py-1.5 rounded-xl font-black text-xs transition-all cursor-pointer border ${
              subStage === 'm2' 
                ? 'bg-blue-600 text-white border-blue-700 shadow-xs' 
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200 border-slate-200'
            }`}
          >
            ف1: الشهر الثاني
          </button>

          <button
            type="button"
            onClick={() => setSubStage('midterm')}
            className={`px-3.5 py-1.5 rounded-xl font-black text-xs transition-all cursor-pointer border ${
              subStage === 'midterm' 
                ? 'bg-emerald-600 text-white border-emerald-700 shadow-xs' 
                : 'bg-emerald-50 text-emerald-900 hover:bg-emerald-100 border-emerald-200'
            }`}
          >
            🌟 امتحان نصف السنة {isOral && '(شفهي+تحريري)'}
          </button>

          <button
            type="button"
            onClick={() => setSubStage('m3')}
            className={`px-3 py-1.5 rounded-xl font-black text-xs transition-all cursor-pointer border ${
              subStage === 'm3' 
                ? 'bg-purple-600 text-white border-purple-700 shadow-xs' 
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200 border-slate-200'
            }`}
          >
            ف2: الشهر الثالث
          </button>

          <button
            type="button"
            onClick={() => setSubStage('m4')}
            className={`px-3 py-1.5 rounded-xl font-black text-xs transition-all cursor-pointer border ${
              subStage === 'm4' 
                ? 'bg-purple-600 text-white border-purple-700 shadow-xs' 
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200 border-slate-200'
            }`}
          >
            ف2: الشهر الرابع
          </button>

          <button
            type="button"
            onClick={() => setSubStage('final')}
            className={`px-3.5 py-1.5 rounded-xl font-black text-xs transition-all cursor-pointer border ${
              subStage === 'final' 
                ? 'bg-rose-600 text-white border-rose-700 shadow-xs' 
                : 'bg-rose-50 text-rose-900 hover:bg-rose-100 border-rose-200'
            }`}
          >
            🏆 الامتحان النهائي {isOral && '(شفهي+د1/د2)'}
          </button>
        </div>

        {/* طريقة حساب اليومي: جمع أو تقسيم */}
        <div className="flex items-center gap-2 bg-slate-50 p-1.5 rounded-xl border border-slate-200">
          <span className="font-bold text-slate-600 flex items-center gap-1 text-[11px]">
            <Calculator className="w-3.5 h-3.5 text-amber-600" />
            طريقة حساب اليومي:
          </span>
          <div className="inline-flex rounded-lg bg-slate-200 p-0.5 border border-slate-300">
            <button
              type="button"
              onClick={() => toggleCalcMode('average')}
              className={`px-2.5 py-0.5 rounded-md font-black text-[11px] transition-all cursor-pointer ${
                currentCalcMode === 'average'
                  ? 'bg-amber-600 text-white shadow-2xs'
                  : 'text-slate-700 hover:text-slate-900'
              }`}
              title="احتساب معدل الامتحانات اليومية تلقائياً"
            >
              ➗ التقسيم (المعدل)
            </button>
            <button
              type="button"
              onClick={() => toggleCalcMode('sum')}
              className={`px-2.5 py-0.5 rounded-md font-black text-[11px] transition-all cursor-pointer ${
                currentCalcMode === 'sum'
                  ? 'bg-amber-600 text-white shadow-2xs'
                  : 'text-slate-700 hover:text-slate-900'
              }`}
              title="جمع درجات اليومي والنشاط المباشر"
            >
              ➕ الجمع المباشر
            </button>
          </div>
        </div>

      </div>

      {/* 3. جدول الرصد والإدخال اليومي */}
      <div className="bg-white rounded-2xl border-2 border-slate-300 shadow-xl overflow-hidden">
        <div className="overflow-x-auto custom-scrollbar">
          <table className="w-full text-center border-collapse text-xs min-w-[850px]">
            <thead>
              <tr className="bg-gradient-to-r from-amber-700 via-amber-600 to-orange-700 text-white font-black text-[11px] border-b-2 border-amber-400">
                <th className="py-2.5 px-1 border-r border-amber-500 w-10 text-center">ت</th>
                <th className="py-2.5 px-2 border-r border-amber-500 w-16 text-center">القيد</th>
                <th className="py-2.5 px-3 border-r border-amber-500 text-right min-w-[200px]">اسم الطالب الرباعي واللقب (انقر للتوسعة)</th>
                
                {/* أعمدة حسب المحطة المحددة */}
                {['m1', 'm2', 'm3', 'm4'].includes(subStage) && (
                  <>
                    <th className="py-2 px-1 border-r border-amber-500 w-14">يومي 1</th>
                    <th className="py-2 px-1 border-r border-amber-500 w-14">يومي 2</th>
                    <th className="py-2 px-1 border-r border-amber-500 w-14">يومي 3</th>
                    <th className="py-2 px-1 border-r border-amber-500 w-14">يومي 4</th>
                    <th className="py-2 px-1 border-r border-amber-500 w-14">يومي 5</th>
                    {isOral && <th className="py-2 px-1 border-r border-amber-500 w-16 bg-amber-800/50">شفهي</th>}
                    <th className="py-2 px-1 border-r border-amber-500 w-16 bg-blue-900/50">تحريري</th>
                    <th className="py-2 px-2 border-r border-amber-500 w-24 bg-amber-950 text-amber-300 font-black">
                      درجة الشهر ({currentCalcMode === 'average' ? 'تقسيم' : 'جمع'})
                    </th>
                  </>
                )}

                {subStage === 'midterm' && (
                  <>
                    {isOral && <th className="py-2 px-2 border-r border-amber-500 w-24 bg-amber-800/60">امتحان شفهي</th>}
                    <th className="py-2 px-2 border-r border-amber-500 w-24 bg-blue-900/60">امتحان تحريري</th>
                    <th className="py-2 px-2 border-r border-amber-500 w-28 bg-emerald-950 text-emerald-300 font-black">
                      درجة نصف السنة
                    </th>
                  </>
                )}

                {subStage === 'final' && (
                  <>
                    {isOral && <th className="py-2 px-2 border-r border-amber-500 w-24 bg-amber-800/60">شفهي نهائي</th>}
                    <th className="py-2 px-2 border-r border-amber-500 w-24 bg-rose-900/60">تحريري دور 1</th>
                    <th className="py-2 px-2 border-r border-amber-500 w-24 bg-rose-900/40">تحريري دور 2</th>
                    <th className="py-2 px-2 border-r border-amber-500 w-28 bg-slate-950 text-amber-400 font-black">
                      درجة الامتحان
                    </th>
                  </>
                )}

                <th className="py-2.5 px-2 text-center w-16">بطاقة الطالب</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-200">
              {students.length === 0 ? (
                <tr>
                  <td colSpan={12} className="py-12 text-center text-slate-500 font-bold">
                    لا يوجد طلاب في هذه الشعبة.
                  </td>
                </tr>
              ) : (
                students.map((std, idx) => {
                  const mark = getStudentMark(std, selectedSubject);
                  
                  // استخراج القيم الحالية للمحطة
                  let dailyArray: number[] = [0, 0, 0, 0, 0];
                  let writtenVal: number = 0;
                  let oralVal: number = 0;
                  let monthVal: number = 0;

                  if (subStage === 'm1') {
                    dailyArray = mark.m1Daily || [0, 0, 0, 0, 0];
                    writtenVal = mark.m1Written || 0;
                    monthVal = mark.m1MonthAvg || 0;
                  } else if (subStage === 'm2') {
                    dailyArray = mark.m2Daily || [0, 0, 0, 0, 0];
                    writtenVal = mark.m2Written || 0;
                    monthVal = mark.m2MonthAvg || 0;
                  } else if (subStage === 'midterm') {
                    writtenVal = mark.midtermScore || 0;
                    oralVal = mark.midtermOral?.[0] || 0;
                    monthVal = mark.midtermFinalGrade || 0;
                  } else if (subStage === 'm3') {
                    dailyArray = mark.m3Daily || [0, 0, 0, 0, 0];
                    writtenVal = mark.m3Written || 0;
                    monthVal = mark.m3MonthAvg || 0;
                  } else if (subStage === 'm4') {
                    dailyArray = mark.m4Daily || [0, 0, 0, 0, 0];
                    writtenVal = mark.m4Written || 0;
                    monthVal = mark.m4MonthAvg || 0;
                  } else if (subStage === 'final') {
                    oralVal = mark.finalOral?.[0] || 0;
                    writtenVal = mark.finalWrittenD1 || 0;
                    monthVal = mark.finalExamTotal || 0;
                  }

                  return (
                    <tr key={std.id} className="hover:bg-amber-50/50 transition-colors">
                      {/* Seq */}
                      <td className="py-1.5 px-1 font-mono font-bold border-r border-slate-200 text-slate-600 bg-slate-50 text-[11px]">
                        {idx + 1}
                      </td>

                      {/* Record No */}
                      <td className="py-1.5 px-1 font-mono font-bold border-r border-slate-200 text-blue-900 text-[11px]">
                        #{std.recordNumber}
                      </td>

                      {/* Student Name (Clickable to view full transcript) */}
                      <td 
                        onClick={() => onSelectStudent(std)}
                        className="py-1.5 px-3 border-r border-slate-200 text-right font-black text-slate-900 cursor-pointer hover:text-amber-700 transition-colors"
                        title="انقر لعرض بطاقة درجات الطالب الكاملة"
                      >
                        <div className="flex items-center justify-between">
                          <span>{std.firstName} {std.secondName} {std.thirdName} {std.fourthName || ''} {std.titleName || ''}</span>
                          <span className="text-[10px] text-amber-600 opacity-60 hover:opacity-100 flex items-center gap-0.5">
                            كشف 🔍
                          </span>
                        </div>
                      </td>

                      {/* Daily 1..5 for Monthly stages */}
                      {['m1', 'm2', 'm3', 'm4'].includes(subStage) && (
                        <>
                          {[0, 1, 2, 3, 4].map(dIdx => (
                            <td key={dIdx} className="py-1 px-1 border-r border-slate-200">
                              <input
                                type="number"
                                min="0"
                                max="100"
                                value={dailyArray[dIdx] > 0 ? dailyArray[dIdx] : ''}
                                onChange={e => {
                                  const val = parseInt(e.target.value, 10);
                                  handleDailyChange(std.id, dIdx, isNaN(val) ? 0 : val);
                                }}
                                placeholder="—"
                                className="w-12 text-center py-1 rounded-md font-mono text-xs border border-slate-200 focus:border-amber-500 focus:bg-amber-50 outline-none"
                              />
                            </td>
                          ))}

                          {isOral && (
                            <td className="py-1 px-1 border-r border-slate-200 bg-amber-50/40">
                              <input
                                type="number"
                                min="0"
                                max="100"
                                value={oralVal > 0 ? oralVal : ''}
                                onChange={e => {
                                  const val = parseInt(e.target.value, 10);
                                  handleScoreChange(std.id, 'oral', isNaN(val) ? 0 : val);
                                }}
                                placeholder="—"
                                className="w-13 text-center py-1 rounded-md font-mono font-bold text-xs border border-amber-200 focus:border-amber-500 bg-white outline-none"
                              />
                            </td>
                          )}

                          <td className="py-1 px-1 border-r border-slate-200 bg-blue-50/30">
                            <input
                              type="number"
                              min="0"
                              max="100"
                              value={writtenVal > 0 ? writtenVal : ''}
                              onChange={e => {
                                const val = parseInt(e.target.value, 10);
                                handleScoreChange(std.id, 'written', isNaN(val) ? 0 : val);
                              }}
                              placeholder="—"
                              className="w-13 text-center py-1 rounded-md font-mono font-bold text-xs border border-blue-200 focus:border-blue-500 bg-white outline-none"
                            />
                          </td>

                          {/* Calculated Month Total */}
                          <td className="py-1.5 px-2 border-r border-slate-200 font-mono font-black text-amber-950 bg-amber-50/80 text-xs">
                            <span className={`inline-block px-2.5 py-0.5 rounded-lg border ${
                              monthVal >= 50 
                                ? 'bg-emerald-100 text-emerald-900 border-emerald-300' 
                                : monthVal > 0 
                                  ? 'bg-rose-100 text-rose-900 border-rose-300' 
                                  : 'text-slate-400 border-transparent'
                            }`}>
                              {monthVal > 0 ? monthVal : '—'}
                            </span>
                          </td>
                        </>
                      )}

                      {/* Midterm Stage */}
                      {subStage === 'midterm' && (
                        <>
                          {isOral && (
                            <td className="py-1 px-2 border-r border-slate-200">
                              <input
                                type="number"
                                min="0"
                                max="100"
                                value={oralVal > 0 ? oralVal : ''}
                                onChange={e => {
                                  const val = parseInt(e.target.value, 10);
                                  handleScoreChange(std.id, 'oral', isNaN(val) ? 0 : val);
                                }}
                                placeholder="—"
                                className="w-16 text-center py-1 rounded-md font-mono font-bold text-xs border border-amber-300 focus:border-amber-500 bg-white outline-none"
                              />
                            </td>
                          )}

                          <td className="py-1 px-2 border-r border-slate-200">
                            <input
                              type="number"
                              min="0"
                              max="100"
                              value={writtenVal > 0 ? writtenVal : ''}
                              onChange={e => {
                                const val = parseInt(e.target.value, 10);
                                handleScoreChange(std.id, 'written', isNaN(val) ? 0 : val);
                              }}
                              placeholder="—"
                              className="w-16 text-center py-1 rounded-md font-mono font-bold text-xs border border-blue-300 focus:border-blue-500 bg-white outline-none"
                            />
                          </td>

                          <td className="py-1.5 px-2 border-r border-slate-200 font-mono font-black text-emerald-950 bg-emerald-50">
                            <span className={`inline-block px-3 py-1 rounded-lg border text-xs font-black ${
                              monthVal >= 50 
                                ? 'bg-emerald-200 text-emerald-950 border-emerald-400' 
                                : monthVal > 0 
                                  ? 'bg-rose-100 text-rose-900 border-rose-300' 
                                  : 'text-slate-400 border-transparent'
                            }`}>
                              {monthVal > 0 ? monthVal : '—'}
                            </span>
                          </td>
                        </>
                      )}

                      {/* Final Exam Stage */}
                      {subStage === 'final' && (
                        <>
                          {isOral && (
                            <td className="py-1 px-2 border-r border-slate-200">
                              <input
                                type="number"
                                min="0"
                                max="100"
                                value={oralVal > 0 ? oralVal : ''}
                                onChange={e => {
                                  const val = parseInt(e.target.value, 10);
                                  handleScoreChange(std.id, 'oral', isNaN(val) ? 0 : val);
                                }}
                                placeholder="—"
                                className="w-16 text-center py-1 rounded-md font-mono font-bold text-xs border border-amber-300 focus:border-amber-500 bg-white outline-none"
                              />
                            </td>
                          )}

                          <td className="py-1 px-2 border-r border-slate-200">
                            <input
                              type="number"
                              min="0"
                              max="100"
                              value={writtenVal > 0 ? writtenVal : ''}
                              onChange={e => {
                                const val = parseInt(e.target.value, 10);
                                handleScoreChange(std.id, 'written', isNaN(val) ? 0 : val);
                              }}
                              placeholder="—"
                              className="w-16 text-center py-1 rounded-md font-mono font-bold text-xs border border-rose-300 focus:border-rose-500 bg-white outline-none"
                            />
                          </td>

                          <td className="py-1 px-2 border-r border-slate-200">
                            <input
                              type="number"
                              min="0"
                              max="100"
                              value={mark.finalWrittenD2 && mark.finalWrittenD2 > 0 ? mark.finalWrittenD2 : ''}
                              onChange={e => {
                                const val = parseInt(e.target.value, 10);
                                handleScoreChange(std.id, 'finalD2', isNaN(val) ? 0 : val);
                              }}
                              placeholder="—"
                              className="w-16 text-center py-1 rounded-md font-mono font-bold text-xs border border-purple-300 focus:border-purple-500 bg-white outline-none"
                            />
                          </td>

                          <td className="py-1.5 px-2 border-r border-slate-200 font-mono font-black text-slate-900 bg-slate-50">
                            <span className={`inline-block px-3 py-1 rounded-lg border text-xs font-black ${
                              monthVal >= 50 
                                ? 'bg-emerald-200 text-emerald-950 border-emerald-400' 
                                : monthVal > 0 
                                  ? 'bg-rose-100 text-rose-900 border-rose-300' 
                                  : 'text-slate-400 border-transparent'
                            }`}>
                              {monthVal > 0 ? monthVal : '—'}
                            </span>
                          </td>
                        </>
                      )}

                      {/* Action to open Modal */}
                      <td className="py-1.5 px-2 text-center">
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
