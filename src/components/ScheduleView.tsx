import React, { useState, useEffect } from 'react';
import { DayScheduleMap, ClassScheduleRow, DayOfWeek, AppConfig, ScheduleCell } from '../types';
import { 
  Calendar, 
  Clock, 
  Plus, 
  Trash2, 
  Edit3, 
  Check, 
  X, 
  Sparkles, 
  Volume2, 
  Info,
  SlidersHorizontal,
  BellRing,
  Printer
} from 'lucide-react';

interface ScheduleViewProps {
  scheduleMap: DayScheduleMap;
  setScheduleMap: React.Dispatch<React.SetStateAction<DayScheduleMap>>;
  config: AppConfig;
}

export const ScheduleView: React.FC<ScheduleViewProps> = ({
  scheduleMap,
  setScheduleMap,
  config
}) => {
  const [selectedDay, setSelectedDay] = useState<DayOfWeek>('الأحد');
  const [activeSlotIndex, setActiveSlotIndex] = useState<number | null>(null);
  const [editingCell, setEditingCell] = useState<{
    rowId: string;
    lessonKey: keyof ClassScheduleRow['lessons'];
  } | null>(null);

  const [cellForm, setCellForm] = useState<ScheduleCell>({
    subject: '',
    teacherName: '',
    isOff: false
  });

  const [showAddRowModal, setShowAddRowModal] = useState(false);
  const [showSettingsModal, setShowSettingsModal] = useState(false);
  const [newRowGrade, setNewRowGrade] = useState('الصف الأول');
  const [newRowSection, setNewRowSection] = useState('أ');
  const [newRowTeacher, setNewRowTeacher] = useState('أ. أستاذ المادة');

  // Schedule Custom Names State
  const [customLessonNames, setCustomLessonNames] = useState<{ [key: string]: string }>({
    lesson1: 'الدرس الأول',
    lesson2: 'الدرس الثاني',
    lesson3: 'الدرس الثالث',
    lesson4: 'الدرس الرابع',
    lesson5: 'الدرس الخامس',
    lesson6: 'الدرس السادس',
  });

  const daysList: DayOfWeek[] = ['الأحد', 'الإثنين', 'الثلاثاء', 'الأربعاء', 'الخميس'];

  // Current day's rows
  const currentDayRows = scheduleMap[selectedDay] || [];

  // Calculate start and end times for all 6 lesson slots & 5 breaks in 12-Hour format (ص/م)
  const calculateSlotTimings = () => {
    const timings: { type: 'lesson' | 'break'; name: string; start: string; end: string; slotNum?: number }[] = [];
    
    const [startH, startM] = config.schoolStartHour.split(':').map(Number);
    let currentTotalMinutes = (startH || 8) * 60 + (startM || 0);

    const formatTime12 = (totalMin: number) => {
      let h = Math.floor(totalMin / 60);
      const m = totalMin % 60;
      const period = h >= 12 ? 'م' : 'ص';
      h = h % 12;
      if (h === 0) h = 12;
      return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')} ${period}`;
    };

    for (let i = 1; i <= 6; i++) {
      // Lesson
      const lessonStart = formatTime12(currentTotalMinutes);
      currentTotalMinutes += config.lessonDurationMinutes;
      const lessonEnd = formatTime12(currentTotalMinutes);

      timings.push({
        type: 'lesson',
        name: `الدرس ${i}`,
        start: lessonStart,
        end: lessonEnd,
        slotNum: i
      });

      // Break (after lessons 1 to 5)
      if (i < 6) {
        const breakStart = formatTime12(currentTotalMinutes);
        currentTotalMinutes += config.breakDurationMinutes;
        const breakEnd = formatTime12(currentTotalMinutes);

        timings.push({
          type: 'break',
          name: `${config.breakDurationMinutes} د`,
          start: breakStart,
          end: breakEnd
        });
      }
    }

    return timings;
  };

  const slotTimings = calculateSlotTimings();

  // Determine active slot based on system clock
  useEffect(() => {
    const checkActiveSlot = () => {
      const now = new Date();
      const currentMin = now.getHours() * 60 + now.getMinutes();

      let activeIdx: number | null = null;

      slotTimings.forEach((slot, idx) => {
        const [sH, sM] = slot.start.split(':').map(Number);
        const [eH, eM] = slot.end.split(':').map(Number);
        const startTotal = sH * 60 + sM;
        const endTotal = eH * 60 + eM;

        if (currentMin >= startTotal && currentMin < endTotal) {
          activeIdx = idx;
        }
      });

      setActiveSlotIndex(activeIdx);
    };

    checkActiveSlot();
    const interval = setInterval(checkActiveSlot, 5000);
    return () => clearInterval(interval);
  }, [config]);

  // Open Cell Editor
  const handleEditCell = (rowId: string, lessonKey: keyof ClassScheduleRow['lessons']) => {
    const row = currentDayRows.find(r => r.id === rowId);
    if (!row) return;

    const cell = row.lessons[lessonKey] as ScheduleCell;
    if (cell) {
      setCellForm({ ...cell });
      setEditingCell({ rowId, lessonKey });
    }
  };

  // Save Cell Edit
  const handleSaveCell = () => {
    if (!editingCell) return;

    setScheduleMap(prev => {
      const dayRows = prev[selectedDay] ? [...prev[selectedDay]] : [];
      const updatedRows = dayRows.map(row => {
        if (row.id === editingCell.rowId) {
          return {
            ...row,
            lessons: {
              ...row.lessons,
              [editingCell.lessonKey]: { ...cellForm }
            }
          };
        }
        return row;
      });

      return {
        ...prev,
        [selectedDay]: updatedRows
      };
    });

    setEditingCell(null);
  };

  // Add new Class Row
  const handleAddRow = (e: React.FormEvent) => {
    e.preventDefault();
    const newRow: ClassScheduleRow = {
      id: `row-${Date.now()}`,
      grade: newRowGrade,
      section: newRowSection,
      teacherInCharge: newRowTeacher,
      lessons: {
        lesson1: { subject: 'مادة جديدة', teacherName: 'أستاذ المادة', isOff: false },
        lesson2: { subject: 'مادة جديدة', teacherName: 'أستاذ المادة', isOff: false },
        lesson3: { subject: 'مادة جديدة', teacherName: 'أستاذ المادة', isOff: false },
        lesson4: { subject: 'مادة جديدة', teacherName: 'أستاذ المادة', isOff: false },
        lesson5: { subject: 'مادة جديدة', teacherName: 'أستاذ المادة', isOff: false },
        lesson6: { subject: 'مادة جديدة', teacherName: 'أستاذ المادة', isOff: false }
      }
    };

    setScheduleMap(prev => ({
      ...prev,
      [selectedDay]: [...(prev[selectedDay] || []), newRow]
    }));

    setShowAddRowModal(false);
  };

  // Delete Row
  const handleDeleteRow = (rowId: string) => {
    if (confirm('هل أنت تأكد من حذف هذا الصف من الجدول؟')) {
      setScheduleMap(prev => ({
        ...prev,
        [selectedDay]: (prev[selectedDay] || []).filter(r => r.id !== rowId)
      }));
    }
  };

  // Compute Teacher Quota / Load for active day
  const teacherLoadMap: { [teacher: string]: number } = {};
  currentDayRows.forEach(row => {
    Object.values(row.lessons).forEach(val => {
      if (typeof val === 'object' && val && 'teacherName' in val) {
        const cell = val as ScheduleCell;
        if (!cell.isOff && cell.teacherName) {
          teacherLoadMap[cell.teacherName] = (teacherLoadMap[cell.teacherName] || 0) + 1;
        }
      }
    });
  });

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
      
      {/* Title & Day Selector Header */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 bg-[var(--theme-card)] p-5 rounded-2xl border border-[var(--theme-card-border)] shadow-sm no-print">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 text-xs font-bold mb-2">
            <Calendar className="w-3.5 h-3.5" />
            <span>الجدول الدراسي اليومي المنظم</span>
          </div>
          <h2 className="text-xl md:text-2xl font-black text-[var(--theme-text-main)]">
            جدول دروس اليوم - {selectedDay}
          </h2>
          <p className="text-xs text-[var(--theme-text-muted)] mt-1">
            مدة الدرس: {config.lessonDurationMinutes} دقيقة | مدة الفرصة: {config.breakDurationMinutes} دقائق | بداية الدوام: {config.schoolStartHour} صباحاً
          </p>
        </div>

        {/* Days Bar */}
        <div className="flex items-center gap-1.5 overflow-x-auto p-1 bg-slate-100 dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          {daysList.map(day => (
            <button
              key={day}
              onClick={() => setSelectedDay(day)}
              className={`px-4 py-2 rounded-lg text-xs font-bold transition-all whitespace-nowrap cursor-pointer ${
                selectedDay === day
                  ? 'bg-amber-500 text-slate-950 shadow-md font-extrabold scale-105'
                  : 'text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
              }`}
            >
              {day}
            </button>
          ))}
        </div>
      </div>

      {/* Main Timetable Table Container */}
      <div className="bg-[var(--theme-card)] rounded-2xl border border-[var(--theme-card-border)] shadow-lg overflow-hidden print-page">
        <div className="p-4 border-b border-[var(--theme-card-border)] flex items-center justify-between bg-slate-50 dark:bg-slate-800/50">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-amber-500" />
            <span className="text-xs font-bold text-[var(--theme-text-main)]">
              توقيتات الحصص ومؤشر الدرس الحالي النشط
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowSettingsModal(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-900 text-amber-400 text-xs font-bold transition-colors cursor-pointer shadow border border-slate-700"
              title="تعديل ألقاب وأسماء الدروس والأساتذة والتوقيتات"
            >
              <SlidersHorizontal className="w-4 h-4 text-amber-400" />
              <span>ضبط الدروس والأساتذة</span>
            </button>

            <button
              onClick={() => window.print()}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-colors cursor-pointer shadow border border-emerald-500"
              title="طباعة وتصدير الجدول الدراسي بصيغة PDF ورقية A4"
            >
              <Printer className="w-4 h-4 text-amber-300" />
              <span>طباعة / تصدير PDF</span>
            </button>

            <button
              onClick={() => setShowAddRowModal(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold transition-colors cursor-pointer shadow"
            >
              <Plus className="w-4 h-4" />
              <span>إضافة صف / شعبة جديدة</span>
            </button>
          </div>
        </div>

        {/* Responsive Table */}
        <div className="overflow-x-auto no-scrollbar bg-[#e6f4f1] p-2 rounded-2xl shadow-inner">
          <table className="w-full text-center border-separate border-spacing-1 min-w-[950px]">
            <thead>
              {/* Row 1: Timings header */}
              <tr className="bg-gradient-to-r from-sky-800 via-sky-700 to-pink-700 text-white text-xs font-mono">
                <th className="py-2 px-2 rounded-xl bg-sky-900 text-amber-300 font-sans font-black w-40 text-center">
                  توقيت الدرس ←
                </th>
                {slotTimings.map((st, i) => (
                  <th 
                    key={i} 
                    className={`py-2 px-1 rounded-xl font-black text-xs ${
                      activeSlotIndex === i ? 'bg-amber-400 text-slate-950 font-black animate-pulse' : st.type === 'break' ? 'bg-pink-600 text-white' : ''
                    }`}
                  >
                    {st.start} - {st.end}
                  </th>
                ))}
                <th className="py-2 px-1 w-10 font-black rounded-xl text-center">إجراء</th>
              </tr>

              {/* Row 2: Columns Titles with primary vertical distinction */}
              <tr className="text-xs font-black">
                <th className="py-2 px-2 rounded-xl bg-sky-800 text-amber-200">
                  الصفوف والشعب
                </th>
                <th className={`py-2 px-1 rounded-xl bg-sky-200 text-sky-950 font-black ${activeSlotIndex === 0 ? 'bg-amber-400 text-slate-950' : ''}`}>الدرس الأول</th>
                <th className={`py-2 px-1 rounded-xl bg-pink-200 text-pink-950 font-black text-[11px] ${activeSlotIndex === 1 ? 'bg-amber-400 text-slate-950' : ''}`}>{config.breakDurationMinutes} د</th>
                <th className={`py-2 px-1 rounded-xl bg-cyan-200 text-cyan-950 font-black ${activeSlotIndex === 2 ? 'bg-amber-400 text-slate-950' : ''}`}>الدرس الثاني</th>
                <th className={`py-2 px-1 rounded-xl bg-pink-200 text-pink-950 font-black text-[11px] ${activeSlotIndex === 3 ? 'bg-amber-400 text-slate-950' : ''}`}>{config.breakDurationMinutes} د</th>
                <th className={`py-2 px-1 rounded-xl bg-amber-200 text-amber-950 font-black ${activeSlotIndex === 4 ? 'bg-amber-400 text-slate-950' : ''}`}>الدرس الثالث</th>
                <th className={`py-2 px-1 rounded-xl bg-pink-200 text-pink-950 font-black text-[11px] ${activeSlotIndex === 5 ? 'bg-amber-400 text-slate-950' : ''}`}>{config.breakDurationMinutes} د</th>
                <th className={`py-2 px-1 rounded-xl bg-emerald-200 text-emerald-950 font-black ${activeSlotIndex === 6 ? 'bg-amber-400 text-slate-950' : ''}`}>الدرس الرابع</th>
                <th className={`py-2 px-1 rounded-xl bg-pink-200 text-pink-950 font-black text-[11px] ${activeSlotIndex === 7 ? 'bg-amber-400 text-slate-950' : ''}`}>{config.breakDurationMinutes} د</th>
                <th className={`py-2 px-1 rounded-xl bg-indigo-200 text-indigo-950 font-black ${activeSlotIndex === 8 ? 'bg-amber-400 text-slate-950' : ''}`}>الدرس الخامس</th>
                <th className={`py-2 px-1 rounded-xl bg-pink-200 text-pink-950 font-black text-[11px] ${activeSlotIndex === 9 ? 'bg-amber-400 text-slate-950' : ''}`}>{config.breakDurationMinutes} د</th>
                <th className={`py-2 px-1 rounded-xl bg-rose-200 text-rose-950 font-black ${activeSlotIndex === 10 ? 'bg-amber-400 text-slate-950' : ''}`}>الدرس السادس</th>
                <th className="py-2 px-1 rounded-xl bg-slate-300 text-slate-900 text-center">حذف</th>
              </tr>
            </thead>

            <tbody className="text-xs">
              {currentDayRows.length === 0 ? (
                <tr>
                  <td colSpan={13} className="py-8 text-center text-slate-500 font-bold bg-white rounded-xl">
                    لا توجد صفوف مضافة لجدول يوم {selectedDay}. اضغط على "إضافة صف" بالأعلى لإضافة الصفوف.
                  </td>
                </tr>
              ) : (
                currentDayRows.map((row) => (
                  <tr key={row.id}>
                    
                    {/* Class & Section title */}
                    <td className="p-0.5 align-middle bg-[#e6f4f1]">
                      <div className="px-2.5 py-1.5 rounded-xl border-2 border-sky-300 bg-white font-black text-slate-900 text-right shadow-xs min-h-[56px] flex flex-col justify-center">
                        <span className="text-sky-950 font-black text-xs block leading-tight">
                          {row.grade} ({row.section})
                        </span>
                        <span className="text-[11px] text-slate-600 font-bold block mt-0.5">
                          مرشد: {cleanTeacherName(row.teacherInCharge)}
                        </span>
                      </div>
                    </td>

                    {/* Lesson 1 */}
                    <LessonCell
                      cell={row.lessons.lesson1}
                      isActive={activeSlotIndex === 0}
                      columnBgClass="bg-[#e6f4f1]"
                      onClick={() => handleEditCell(row.id, 'lesson1')}
                    />

                    {/* Break 1 */}
                    <BreakCell isActive={activeSlotIndex === 1} durationMinutes={config.breakDurationMinutes} />

                    {/* Lesson 2 */}
                    <LessonCell
                      cell={row.lessons.lesson2}
                      isActive={activeSlotIndex === 2}
                      columnBgClass="bg-[#e6f4f1]"
                      onClick={() => handleEditCell(row.id, 'lesson2')}
                    />

                    {/* Break 2 */}
                    <BreakCell isActive={activeSlotIndex === 3} durationMinutes={config.breakDurationMinutes} />

                    {/* Lesson 3 */}
                    <LessonCell
                      cell={row.lessons.lesson3}
                      isActive={activeSlotIndex === 4}
                      columnBgClass="bg-[#e6f4f1]"
                      onClick={() => handleEditCell(row.id, 'lesson3')}
                    />

                    {/* Break 3 */}
                    <BreakCell isActive={activeSlotIndex === 5} durationMinutes={config.breakDurationMinutes} />

                    {/* Lesson 4 */}
                    <LessonCell
                      cell={row.lessons.lesson4}
                      isActive={activeSlotIndex === 6}
                      columnBgClass="bg-[#e6f4f1]"
                      onClick={() => handleEditCell(row.id, 'lesson4')}
                    />

                    {/* Break 4 */}
                    <BreakCell isActive={activeSlotIndex === 7} durationMinutes={config.breakDurationMinutes} />

                    {/* Lesson 5 */}
                    <LessonCell
                      cell={row.lessons.lesson5}
                      isActive={activeSlotIndex === 8}
                      columnBgClass="bg-[#e6f4f1]"
                      onClick={() => handleEditCell(row.id, 'lesson5')}
                    />

                    {/* Break 5 */}
                    <BreakCell isActive={activeSlotIndex === 9} durationMinutes={config.breakDurationMinutes} />

                    {/* Lesson 6 */}
                    <LessonCell
                      cell={row.lessons.lesson6}
                      isActive={activeSlotIndex === 10}
                      columnBgClass="bg-[#e6f4f1]"
                      onClick={() => handleEditCell(row.id, 'lesson6')}
                    />

                    {/* Row Action */}
                    <td className="p-0.5 align-middle bg-[#e6f4f1]">
                      <div className="px-1.5 py-1.5 rounded-xl border-2 border-slate-300 bg-white flex items-center justify-center gap-1 shadow-xs min-h-[56px]">
                        <button
                          onClick={() => {
                            const newSubject = prompt('تغيير مادة الحصة لجميع الحصص بالصف أو صف محدد؟ أدخل اسم المادة:');
                            if (newSubject) {
                              setScheduleMap(prev => {
                                const dayRows = prev[selectedDay] || [];
                                return {
                                  ...prev,
                                  [selectedDay]: dayRows.map(r => r.id === row.id ? {
                                    ...r,
                                    lessons: {
                                      ...r.lessons,
                                      lesson1: { ...r.lessons.lesson1, subject: newSubject }
                                    }
                                  } : r)
                                };
                              });
                            }
                          }}
                          className="p-1 rounded-lg text-slate-600 hover:bg-slate-200"
                          title="ضبط سريع"
                        >
                          <SlidersHorizontal className="w-3.5 h-3.5" />
                        </button>

                        <button
                          onClick={() => handleDeleteRow(row.id)}
                          className="p-1 rounded-lg text-rose-600 hover:bg-rose-100 transition-colors"
                          title="حذف هذا الصف"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>

                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Teacher Load / Quota Section (نصاب كل مدرس أو معلم - مخفي في الطباعة بطلب المدير) */}
      <div className="bg-white rounded-2xl border-2 border-sky-300 p-5 shadow-lg no-print">
        <div className="flex items-center gap-2 mb-4 border-b border-sky-200 pb-3">
          <Sparkles className="w-5 h-5 text-sky-600" />
          <h3 className="text-base font-black text-slate-900">
            نصاب المدرسين اليومي لمدرسين المدرسة (عدد الحصص اليومية - {selectedDay})
          </h3>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {Object.entries(teacherLoadMap).length === 0 ? (
            <div className="col-span-full text-xs text-slate-500 font-bold">لا توجد حصص مسندة للمدرسين لهذا اليوم.</div>
          ) : (
            Object.entries(teacherLoadMap).map(([teacher, count]) => (
              <div key={teacher} className="p-3 rounded-xl bg-slate-50 border-2 border-sky-300 flex flex-col justify-between shadow-sm">
                <span className="text-xs font-black text-slate-900 truncate">{teacher}</span>
                <div className="flex items-center justify-between mt-2 pt-2 border-t border-slate-200">
                  <span className="text-[11px] text-slate-600 font-bold">النصاب اليومي:</span>
                  <span className="text-sm font-black text-sky-900 bg-sky-100 px-2.5 py-0.5 rounded-full border border-sky-300">
                    {count} حصة
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* FULL WEEKLY SCHEDULE PRINT TEMPLATE (PRINT / PDF EXPORT ONLY) */}
      <div className="hidden print:block space-y-8 w-full dir-rtl">
        {daysList.map((day) => {
          const dayRows = scheduleMap[day] || [];
          return (
            <div key={day} className="page-break-after font-sans p-6 bg-white border-2 border-slate-900 rounded-xl mb-8 min-h-[95vh] flex flex-col justify-between">
              
              {/* Day Header */}
              <div className="flex items-center justify-between border-b-2 border-slate-900 pb-3 mb-4">
                <div className="text-right leading-tight">
                  <h2 className="text-base font-black text-slate-950">الجدول الدراسي الرسمي - {config.schoolName}</h2>
                  <p className="text-xs font-bold text-slate-700">جدول يوم: <span className="text-amber-950 font-black text-sm">{day}</span> | العام الدراسي 2025-2026</p>
                </div>
                <div className="text-left leading-tight text-xs font-bold text-slate-950">
                  <p>جمهورية العراق - وزارة التربية</p>
                  <p>{config.directorateName}</p>
                  <p>إشراف المدير: {config.managerName}</p>
                </div>
              </div>

              {/* Day Timetable Table - Auto-fits A4 landscape width 100% without clipping */}
              <div className="w-full overflow-hidden flex-1">
                <table className="w-full text-center border-collapse border-2 border-slate-900 text-[11px] font-bold">
                  <thead>
                    <tr className="bg-slate-950 text-white font-black text-[10px]">
                      <th className="p-2 border border-slate-700 w-32 bg-sky-950">الصف والشعبة</th>
                      <th className="p-2 border border-slate-700">الدرس الأول</th>
                      <th className="p-1 border border-slate-700 w-10 text-[9px] bg-pink-900">{config.breakDurationMinutes} د</th>
                      <th className="p-2 border border-slate-700">الدرس الثاني</th>
                      <th className="p-1 border border-slate-700 w-10 text-[9px] bg-pink-900">{config.breakDurationMinutes} د</th>
                      <th className="p-2 border border-slate-700">الدرس الثالث</th>
                      <th className="p-1 border border-slate-700 w-10 text-[9px] bg-pink-900">{config.breakDurationMinutes} د</th>
                      <th className="p-2 border border-slate-700">الدرس الرابع</th>
                      <th className="p-1 border border-slate-700 w-10 text-[9px] bg-pink-900">{config.breakDurationMinutes} د</th>
                      <th className="p-2 border border-slate-700">الدرس الخامس</th>
                      <th className="p-1 border border-slate-700 w-10 text-[9px] bg-pink-900">{config.breakDurationMinutes} د</th>
                      <th className="p-2 border border-slate-700">الدرس السادس</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dayRows.length === 0 ? (
                      <tr>
                        <td colSpan={12} className="p-6 text-center text-slate-500 font-bold">
                          لا توجد حصص مسندة ليوم {day}
                        </td>
                      </tr>
                    ) : (
                      dayRows.map((row) => (
                        <tr key={row.id} className="border-b border-slate-800">
                          <td className="p-2 border border-slate-400 font-black bg-slate-100 text-slate-950">
                            <div className="text-xs">{row.grade} ({row.section})</div>
                            <div className="text-[9px] text-slate-700 font-normal">مرشد: {cleanTeacherName(row.teacherInCharge)}</div>
                          </td>
                          
                          <td className="p-1.5 border border-slate-300">
                            {row.lessons.lesson1.isOff ? <span className="text-rose-700 font-bold text-[9px]">شاغرة</span> : (
                              <div>
                                <div className="font-black text-slate-950">{row.lessons.lesson1.subject || '-'}</div>
                                <div className="text-[9px] text-slate-700">{cleanTeacherName(row.lessons.lesson1.teacherName)}</div>
                              </div>
                            )}
                          </td>
                          <td className="p-0.5 border border-slate-300 bg-pink-50"></td>

                          <td className="p-1.5 border border-slate-300">
                            {row.lessons.lesson2.isOff ? <span className="text-rose-700 font-bold text-[9px]">شاغرة</span> : (
                              <div>
                                <div className="font-black text-slate-950">{row.lessons.lesson2.subject || '-'}</div>
                                <div className="text-[9px] text-slate-700">{cleanTeacherName(row.lessons.lesson2.teacherName)}</div>
                              </div>
                            )}
                          </td>
                          <td className="p-0.5 border border-slate-300 bg-pink-50"></td>

                          <td className="p-1.5 border border-slate-300">
                            {row.lessons.lesson3.isOff ? <span className="text-rose-700 font-bold text-[9px]">شاغرة</span> : (
                              <div>
                                <div className="font-black text-slate-950">{row.lessons.lesson3.subject || '-'}</div>
                                <div className="text-[9px] text-slate-700">{cleanTeacherName(row.lessons.lesson3.teacherName)}</div>
                              </div>
                            )}
                          </td>
                          <td className="p-0.5 border border-slate-300 bg-pink-50"></td>

                          <td className="p-1.5 border border-slate-300">
                            {row.lessons.lesson4.isOff ? <span className="text-rose-700 font-bold text-[9px]">شاغرة</span> : (
                              <div>
                                <div className="font-black text-slate-950">{row.lessons.lesson4.subject || '-'}</div>
                                <div className="text-[9px] text-slate-700">{cleanTeacherName(row.lessons.lesson4.teacherName)}</div>
                              </div>
                            )}
                          </td>
                          <td className="p-0.5 border border-slate-300 bg-pink-50"></td>

                          <td className="p-1.5 border border-slate-300">
                            {row.lessons.lesson5.isOff ? <span className="text-rose-700 font-bold text-[9px]">شاغرة</span> : (
                              <div>
                                <div className="font-black text-slate-950">{row.lessons.lesson5.subject || '-'}</div>
                                <div className="text-[9px] text-slate-700">{cleanTeacherName(row.lessons.lesson5.teacherName)}</div>
                              </div>
                            )}
                          </td>
                          <td className="p-0.5 border border-slate-300 bg-pink-50"></td>

                          <td className="p-1.5 border border-slate-300">
                            {row.lessons.lesson6.isOff ? <span className="text-rose-700 font-bold text-[9px]">شاغرة</span> : (
                              <div>
                                <div className="font-black text-slate-950">{row.lessons.lesson6.subject || '-'}</div>
                                <div className="text-[9px] text-slate-700">{cleanTeacherName(row.lessons.lesson6.teacherName)}</div>
                              </div>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              {/* Day Footer Signatures */}
              <div className="pt-4 border-t-2 border-slate-900 flex justify-between items-center text-xs font-black">
                <span>توقيع رئيس لجنة الجدول المدرسي</span>
                <span>توقيع وختم مدير المدرسة: {config.managerName}</span>
              </div>

            </div>
          );
        })}
      </div>

      {/* Modal: Edit Schedule Cell */}
      {editingCell && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white border-2 border-sky-300 rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <h3 className="text-base font-black text-slate-900">تعديل مادة الحصة والأستاذ</h3>
              <button onClick={() => setEditingCell(null)} className="p-1 rounded-lg hover:bg-slate-100 text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-black mb-1 text-slate-900">اسم المادة الدراسية:</label>
                <input
                  type="text"
                  value={cellForm.subject}
                  onChange={e => setCellForm(prev => ({ ...prev, subject: e.target.value }))}
                  placeholder="مثال: جغرافية، رياضيات..."
                  className="w-full px-3.5 py-2.5 rounded-xl border-2 border-sky-300 bg-white text-slate-900 font-bold placeholder-slate-400 focus:outline-none focus:border-sky-500 shadow-sm"
                />
              </div>

              <div>
                <label className="block font-black mb-1 text-slate-900">اسم المدرس / المعلم:</label>
                <input
                  type="text"
                  value={cellForm.teacherName}
                  onChange={e => setCellForm(prev => ({ ...prev, teacherName: e.target.value }))}
                  placeholder="مثال: أ. محمد الجبوري..."
                  className="w-full px-3.5 py-2.5 rounded-xl border-2 border-sky-300 bg-white text-slate-900 font-bold placeholder-slate-400 focus:outline-none focus:border-sky-500 shadow-sm"
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="cellOffToggle"
                  checked={cellForm.isOff}
                  onChange={e => setCellForm(prev => ({ ...prev, isOff: e.target.checked }))}
                  className="w-4 h-4 text-rose-600 rounded"
                />
                <label htmlFor="cellOffToggle" className="font-extrabold text-rose-600 cursor-pointer">
                  تفريغ الحصة (شاغرة / Off)
                </label>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
              <button
                onClick={() => setEditingCell(null)}
                className="px-4 py-2 rounded-xl bg-slate-100 text-slate-800 text-xs font-black border border-slate-300 hover:bg-slate-200"
              >
                إلغاء
              </button>
              <button
                onClick={handleSaveCell}
                className="px-5 py-2 rounded-xl bg-sky-600 text-white text-xs font-black hover:bg-sky-700 shadow-md"
              >
                حفظ التعديلات
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Add Row (New Class) */}
      {showAddRowModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-center justify-center p-4">
          <form onSubmit={handleAddRow} className="bg-white border-2 border-sky-300 rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <h3 className="text-base font-black text-slate-900">إضافة صف / شعبة لجدول {selectedDay}</h3>
              <button type="button" onClick={() => setShowAddRowModal(false)} className="p-1 rounded-lg hover:bg-slate-100 text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-black mb-1 text-slate-900">الصف الدراسـي:</label>
                <select
                  value={newRowGrade}
                  onChange={e => setNewRowGrade(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border-2 border-sky-300 bg-white text-slate-900 font-black focus:outline-none focus:border-sky-500 shadow-sm cursor-pointer"
                >
                  <optgroup label="المرحلة الابتدائية">
                    <option value="الأول ابتدائي">الأول ابتدائي</option>
                    <option value="الثاني ابتدائي">الثاني ابتدائي</option>
                    <option value="الثالث ابتدائي">الثالث ابتدائي</option>
                    <option value="الرابع ابتدائي">الرابع ابتدائي</option>
                    <option value="الخامس ابتدائي">الخامس ابتدائي</option>
                    <option value="السادس ابتدائي">السادس ابتدائي</option>
                  </optgroup>
                  <optgroup label="المرحلة المتوسطة">
                    <option value="الأول متوسط">الأول متوسط</option>
                    <option value="الثاني متوسط">الثاني متوسط</option>
                    <option value="الثالث متوسط">الثالث متوسط</option>
                  </optgroup>
                  <optgroup label="المرحلة الإعدادية والثانوية">
                    <option value="الرابع العلمي">الرابع العلمي</option>
                    <option value="الرابع الأدبي">الرابع الأدبي</option>
                    <option value="الخامس العلمي">الخامس العلمي</option>
                    <option value="الخامس الأدبي">الخامس الأدبي</option>
                    <option value="السادس العلمي (أحياء)">السادس العلمي (أحياء)</option>
                    <option value="السادس العلمي (تطبيقية)">السادس العلمي (تطبيقية)</option>
                    <option value="السادس الأدبي">السادس الأدبي</option>
                  </optgroup>
                </select>
              </div>

              <div>
                <label className="block font-black mb-1 text-slate-900">الشعبة:</label>
                <input
                  type="text"
                  value={newRowSection}
                  onChange={e => setNewRowSection(e.target.value)}
                  placeholder="أ / ب / ج / د..."
                  className="w-full px-3.5 py-2.5 rounded-xl border-2 border-sky-300 bg-white text-slate-900 font-black placeholder-slate-400 focus:outline-none focus:border-sky-500 shadow-sm"
                />
              </div>

              <div>
                <label className="block font-black mb-1 text-slate-900">مرشد الصف المسؤول:</label>
                <input
                  type="text"
                  value={newRowTeacher}
                  onChange={e => setNewRowTeacher(e.target.value)}
                  placeholder="اسم المدرس..."
                  className="w-full px-3.5 py-2.5 rounded-xl border-2 border-sky-300 bg-white text-slate-900 font-black placeholder-slate-400 focus:outline-none focus:border-sky-500 shadow-sm"
                />
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
              <button
                type="button"
                onClick={() => setShowAddRowModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 text-slate-800 text-xs font-black border border-slate-300 hover:bg-slate-200"
              >
                إلغاء
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-xl bg-sky-600 text-white text-xs font-black hover:bg-sky-700 shadow-md"
              >
                إضافة للصفوف
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Modal: Schedule Settings Modal (ضبط وتعديل أسماء الدروس والأساتذة) */}
      {showSettingsModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-2xl p-6 max-w-lg w-full shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <SlidersHorizontal className="w-5 h-5 text-amber-500" />
                <h3 className="text-base font-bold text-[var(--theme-text-main)]">
                  إعدادات وضبط أسماء الدروس والأساتذة بالجدول
                </h3>
              </div>
              <button onClick={() => setShowSettingsModal(false)} className="p-1 rounded-lg hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <p className="font-bold text-slate-700 dark:text-slate-300">
                1. تخصيص أسماء وألقاب الحصص والدروس:
              </p>

              <div className="grid grid-cols-2 gap-2">
                {Object.keys(customLessonNames).map((key, idx) => (
                  <div key={key}>
                    <label className="block text-[11px] font-bold text-slate-500 mb-1">الدرس {idx + 1}:</label>
                    <input
                      type="text"
                      value={customLessonNames[key]}
                      onChange={e => setCustomLessonNames(prev => ({ ...prev, [key]: e.target.value }))}
                      className="w-full p-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 font-bold"
                    />
                  </div>
                ))}
              </div>

              <div className="pt-3 border-t space-y-2">
                <p className="font-bold text-slate-700 dark:text-slate-300">
                  2. إعادة تخصيص الأستاذ لجميع الشعب في اليوم الحالي ({selectedDay}):
                </p>
                <button
                  onClick={() => {
                    const oldT = prompt('أدخل اسم الأستاذ القديم للبحث عنه واستبداله:');
                    if (!oldT) return;
                    const newT = prompt('أدخل اسم الأستاذ الجديد:');
                    if (!newT) return;

                    setScheduleMap(prev => {
                      const dayRows = prev[selectedDay] || [];
                      const updated = dayRows.map(row => {
                        const newLessons = { ...row.lessons };
                        (Object.keys(newLessons) as Array<keyof typeof newLessons>).forEach(k => {
                          if (newLessons[k].teacherName === oldT) {
                            newLessons[k] = { ...newLessons[k], teacherName: newT };
                          }
                        });
                        return { ...row, lessons: newLessons };
                      });
                      return { ...prev, [selectedDay]: updated };
                    });

                    alert(`تم استبدال الأستاذ "${oldT}" بالأستاذ "${newT}" في جدول يوم ${selectedDay} بنجاح!`);
                  }}
                  className="w-full py-2 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 font-black transition-all shadow"
                >
                  استبدال مدرس بمدرس آخر بالجدول الحالي
                </button>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t">
              <button
                onClick={() => setShowSettingsModal(false)}
                className="px-5 py-2 rounded-xl bg-slate-800 text-white text-xs font-bold shadow"
              >
                حفظ وإغلاق الضبط
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

// Subcomponent: Lesson Cell in schedule
const cleanTeacherName = (name?: string) => {
  if (!name) return 'غير مخصص';
  let cleaned = name.replace(/^(أ\.|أستاذ\s*|د\.|م\.|السيد\s*)\s*/gi, '').trim();
  const tokens = cleaned.split(/\s+/).filter(t => t.length > 0);
  if (tokens.length >= 2) {
    return `${tokens[0]} ${tokens[1]}`;
  }
  return cleaned;
};

const LessonCell: React.FC<{
  cell: ScheduleCell;
  isActive: boolean;
  columnBgClass?: string;
  onClick: () => void;
}> = ({ cell, isActive, columnBgClass = 'bg-[#e6f4f1]', onClick }) => {
  const displayTeacher = cleanTeacherName(cell.teacherName);

  return (
    <td 
      onClick={onClick}
      className={`p-0.5 align-middle cursor-pointer transition-all ${columnBgClass}`}
    >
      <div className={`px-1.5 py-1 rounded-xl border-2 transition-all hover:scale-[1.02] relative text-center min-h-[56px] flex flex-col justify-center items-center ${
        isActive 
          ? 'bg-amber-300 text-slate-950 font-black border-amber-500 shadow-md ring-2 ring-amber-400' 
          : cell.isOff 
          ? 'bg-rose-50 text-rose-900 border-rose-300 font-bold shadow-2xs' 
          : 'bg-white text-slate-900 border-slate-300/90 shadow-2xs hover:border-sky-400'
      }`}>
        {isActive && (
          <span className="absolute -top-2 left-1/2 -translate-x-1/2 px-1.5 py-0.2 rounded-full text-[8px] font-black bg-rose-600 text-white shadow-xs whitespace-nowrap animate-pulse">
            الدرس الحالي
          </span>
        )}

        {cell.isOff ? (
          <span className="text-[10px] font-black text-rose-700 bg-rose-200/80 px-1.5 py-0.5 rounded-lg border border-rose-300 inline-block">
            شاغرة (Off)
          </span>
        ) : (
          <div className="space-y-0.5 w-full">
            <span className="font-black text-xs block leading-tight text-slate-900 truncate max-w-full">
              {cell.subject || 'بدون مادة'}
            </span>
            <span className="text-[10px] font-extrabold text-slate-800 bg-slate-100 border border-slate-300 px-1.5 py-0.2 rounded-full inline-block whitespace-nowrap truncate max-w-full">
              {displayTeacher}
            </span>
          </div>
        )}
      </div>
    </td>
  );
};

// Subcomponent: Break Cell in schedule (Slim bar, no repeated 'فرصة' text)
const BreakCell: React.FC<{ isActive: boolean }> = ({ isActive }) => {
  return (
    <td className="p-0.5 align-middle bg-[#e6f4f1] w-5 min-w-[20px] max-w-[24px]">
      <div className={`w-full rounded-xl border-2 transition-all min-h-[56px] flex items-center justify-center ${
        isActive 
          ? 'bg-emerald-500 border-emerald-600 animate-pulse shadow-md' 
          : 'bg-pink-100/90 border-pink-300 shadow-2xs'
      }`}>
        <div className="w-1 h-6 bg-pink-400/60 rounded-full"></div>
      </div>
    </td>
  );
};
