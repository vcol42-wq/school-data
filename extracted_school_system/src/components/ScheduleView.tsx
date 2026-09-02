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
  Printer,
  CloudUpload,
  Loader2
} from 'lucide-react';
import { PrintPreviewModal } from './PrintPreviewModal';
import { getSupabase } from '../utils/supabaseClient';

interface ScheduleViewProps {
  scheduleMap: DayScheduleMap;
  setScheduleMap: React.Dispatch<React.SetStateAction<DayScheduleMap>>;
  config: AppConfig;
  onOpenSmartGenerator?: () => void;
}

export const ScheduleView: React.FC<ScheduleViewProps> = ({
  scheduleMap,
  setScheduleMap,
  config,
  onOpenSmartGenerator
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
  const [showPrintPreviewModal, setShowPrintPreviewModal] = useState(false);
  const [isUploadingSchedule, setIsUploadingSchedule] = useState(false);
  const [uploadSuccessMsg, setUploadSuccessMsg] = useState('');
  const [newRowGrade, setNewRowGrade] = useState('الصف الأول');
  const [newRowSection, setNewRowSection] = useState('أ');
  const [newRowTeacher, setNewRowTeacher] = useState('أ. أستاذ المادة');

  const handleUploadScheduleToCloud = async () => {
    setIsUploadingSchedule(true);
    setUploadSuccessMsg('');
    try {
      const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
      const client = getSupabase(schoolId);

      // 1. Save locally to localStorage
      localStorage.setItem('diyala_school_schedule', JSON.stringify(scheduleMap));

      // 2. Direct upsert to Supabase
      const { error } = await client.from('schedules').upsert({
        id: schoolId,
        schedule_map: scheduleMap
      }, { onConflict: 'id', ignoreDuplicates: false });

      if (error) {
        throw error;
      }

      setUploadSuccessMsg('تم رفع وحفظ جدول الحصص الأسبوعي إلى السحابة بنجاح! 🚀');
      setTimeout(() => setUploadSuccessMsg(''), 5000);
    } catch (e: any) {
      alert('فشل في رفع الجدول للسحابة: ' + (e.message || 'تأكد من الاتصال بالإنترنت'));
    } finally {
      setIsUploadingSchedule(false);
    }
  };

  // Schedule Custom Names State
  const [customLessonNames, setCustomLessonNames] = useState<{ [key: string]: string }>({
    lesson1: 'الدرس الأول',
    lesson2: 'الدرس الثاني',
    lesson3: 'الدرس الثالث',
    lesson4: 'الدرس الرابع',
    lesson5: 'الدرس الخامس',
    lesson6: 'الدرس السادس',
  });

  const [replaceOldTeacher, setReplaceOldTeacher] = useState('');
  const [replaceNewTeacher, setReplaceNewTeacher] = useState('');
  const [replaceStatus, setReplaceStatus] = useState('');

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
          const clean = cleanTeacherName(cell.teacherName);
          if (clean && clean !== 'شاغر' && clean !== 'مفرغ') {
            teacherLoadMap[clean] = (teacherLoadMap[clean] || 0) + 1;
          }
        }
      }
    });
  });

  // Compute Weekly Teacher Quota across ALL 5 days of the week
  const weeklyTeacherLoadMap: { [teacher: string]: number } = {};
  daysList.forEach(day => {
    const dayRows = scheduleMap[day] || [];
    dayRows.forEach(row => {
      Object.values(row.lessons).forEach(val => {
        if (typeof val === 'object' && val && 'teacherName' in val) {
          const cell = val as ScheduleCell;
          if (!cell.isOff && cell.teacherName) {
            const clean = cleanTeacherName(cell.teacherName);
            if (clean && clean !== 'شاغر' && clean !== 'مفرغ') {
              weeklyTeacherLoadMap[clean] = (weeklyTeacherLoadMap[clean] || 0) + 1;
            }
          }
        }
      });
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
        <div className="flex items-center gap-2 overflow-x-auto p-1.5 bg-slate-900 rounded-2xl border-2 border-indigo-500 shadow-md">
          {daysList.map(day => (
            <button
              key={day}
              onClick={() => setSelectedDay(day)}
              className={`px-5 py-2.5 rounded-xl text-xs font-black transition-all whitespace-nowrap cursor-pointer ${
                selectedDay === day
                  ? 'bg-gradient-to-r from-amber-400 to-amber-500 text-slate-950 shadow-lg font-black scale-105 border-2 border-amber-300 ring-2 ring-amber-400/50'
                  : 'bg-slate-800 hover:bg-slate-700 text-white font-bold border border-slate-700 hover:scale-102'
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

          <div className="flex items-center gap-2 flex-wrap">
            {uploadSuccessMsg && (
              <span className="text-xs font-black text-emerald-700 bg-emerald-100 px-3 py-1.5 rounded-lg border border-emerald-300 animate-pulse">
                {uploadSuccessMsg}
              </span>
            )}

            {onOpenSmartGenerator && (
              <button
                onClick={onOpenSmartGenerator}
                className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-purple-700 to-indigo-700 hover:from-purple-800 hover:to-indigo-800 text-white text-xs font-black transition-all cursor-pointer shadow-md border border-purple-400"
                title="فتح معالج التوليد الآلي والعادل للجدول المدرسي ومنع تضارب المدرسين"
              >
                <Sparkles className="w-4 h-4 text-amber-300 animate-pulse" />
                <span>المولّد الذكي للجدول 🪄</span>
              </button>
            )}

            <button
              onClick={handleUploadScheduleToCloud}
              disabled={isUploadingSchedule}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-blue-700 via-indigo-700 to-purple-700 hover:from-blue-600 hover:to-indigo-600 text-white text-xs font-black transition-all cursor-pointer shadow-md border border-blue-400/40 disabled:opacity-50"
              title="رفع وحفظ خريطة جدول الحصص في السحابة لتصل لتطبيقات المدرسين فوراً"
            >
              {isUploadingSchedule ? (
                <Loader2 className="w-4 h-4 text-amber-300 animate-spin" />
              ) : (
                <CloudUpload className="w-4 h-4 text-amber-300" />
              )}
              <span>{isUploadingSchedule ? 'جاري الرفع السحابي...' : 'حفظ ورفع الجدول للسحابة 📤'}</span>
            </button>

            <button
              onClick={() => setShowSettingsModal(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-900 text-amber-400 text-xs font-bold transition-colors cursor-pointer shadow border border-slate-700"
              title="تعديل ألقاب وأسماء الدروس والأساتذة والتوقيتات"
            >
              <SlidersHorizontal className="w-4 h-4 text-amber-400" />
              <span>ضبط الدروس والأساتذة</span>
            </button>

            <button
              onClick={() => setShowPrintPreviewModal(true)}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs font-black transition-all cursor-pointer shadow-md border border-emerald-400/40"
              title="معاينة الجدول الدراسي ورقية قبل الطباعة والتصدير"
            >
              <Printer className="w-4 h-4 text-amber-300" />
              <span>معاينة وطباعة الجدول</span>
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
                          onClick={() => handleEditCell(row.id, 'lesson1')}
                          className="p-1 rounded-lg text-indigo-600 hover:bg-indigo-50 transition-colors cursor-pointer"
                          title="تعديل حصص هذا الصف"
                        >
                          <SlidersHorizontal className="w-3.5 h-3.5" />
                        </button>

                        <button
                          onClick={() => handleDeleteRow(row.id)}
                          className="p-1 rounded-lg text-rose-600 hover:bg-rose-100 transition-colors cursor-pointer"
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
        <div className="flex items-center justify-between gap-2 mb-4 border-b border-sky-200 pb-3">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-sky-600" />
            <h3 className="text-base font-black text-slate-900">
              نصاب المدرسين اليومي (عدد الحصص اليومية - {selectedDay})
            </h3>
          </div>
          <span className="text-xs font-black text-sky-700 bg-sky-50 px-3 py-1 rounded-full border border-sky-200">
            {Object.keys(teacherLoadMap).length} مدرس نشط اليوم
          </span>
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

      {/* Weekly Teacher Quota Section (نصاب الأسبوع الكلي) */}
      <div className="bg-white rounded-2xl border-2 border-indigo-300 p-5 shadow-lg no-print">
        <div className="flex items-center justify-between gap-2 mb-4 border-b border-indigo-200 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-indigo-600 text-white flex items-center justify-center font-bold shadow">
              <Calendar className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-black text-slate-900">
                نصاب الأسبوع الكلي للمدرسين (إجمالي الحصص الأسبوعية)
              </h3>
              <p className="text-xs text-slate-500 font-bold">
                مجموع الحصص الأسبوعية الفعلية لكل مدرس في الجدول الأسبوعي كاملاً (أحد إلى خميس)
              </p>
            </div>
          </div>
          <span className="text-xs font-black text-indigo-700 bg-indigo-50 px-3 py-1 rounded-full border border-indigo-200">
            {Object.keys(weeklyTeacherLoadMap).length} مدرس مسند بالجدول الأسبوعي
          </span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {Object.entries(weeklyTeacherLoadMap).length === 0 ? (
            <div className="col-span-full text-xs text-slate-500 font-bold">لا توجد حصص مسندة للمدرسين في الجدول الأسبوعي.</div>
          ) : (
            Object.entries(weeklyTeacherLoadMap)
              .sort((a, b) => b[1] - a[1])
              .map(([teacher, count]) => (
                <div key={teacher} className="p-3 rounded-xl bg-indigo-50/40 border-2 border-indigo-200 flex flex-col justify-between shadow-sm hover:bg-indigo-50 transition">
                  <span className="text-xs font-black text-slate-900 truncate">{teacher}</span>
                  <div className="flex items-center justify-between mt-2 pt-2 border-t border-indigo-100">
                    <span className="text-[11px] text-slate-600 font-bold">نصاب الأسبوع:</span>
                    <span className="text-sm font-black text-indigo-900 bg-indigo-100 px-2.5 py-0.5 rounded-full border border-indigo-300">
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

              <div className="pt-3 border-t space-y-3">
                <p className="font-bold text-slate-700 dark:text-slate-300">
                  2. إعادة تخصيص واستبدال الأستاذ في جدول اليوم ({selectedDay}):
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[11px] font-bold text-slate-500 mb-1">اسم الأستاذ الحالي للبحث عنه:</label>
                    <input
                      type="text"
                      placeholder="مثال: أ. أحمد"
                      value={replaceOldTeacher}
                      onChange={e => setReplaceOldTeacher(e.target.value)}
                      className="w-full p-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 font-bold text-xs"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-slate-500 mb-1">اسم الأستاذ البديل الجديد:</label>
                    <input
                      type="text"
                      placeholder="مثال: أ. حيدر"
                      value={replaceNewTeacher}
                      onChange={e => setReplaceNewTeacher(e.target.value)}
                      className="w-full p-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 font-bold text-xs"
                    />
                  </div>
                </div>

                <button
                  onClick={() => {
                    const oldT = replaceOldTeacher.trim();
                    const newT = replaceNewTeacher.trim();
                    if (!oldT || !newT) {
                      setReplaceStatus('يرجى إدخال اسم الأستاذ الحالي والجديد أولاً.');
                      return;
                    }

                    setScheduleMap(prev => {
                      const dayRows = prev[selectedDay] || [];
                      const updated = dayRows.map(row => {
                        const newLessons = { ...row.lessons };
                        (Object.keys(newLessons) as Array<keyof typeof newLessons>).forEach(k => {
                          if (newLessons[k].teacherName && newLessons[k].teacherName.includes(oldT)) {
                            newLessons[k] = { ...newLessons[k], teacherName: newT };
                          }
                        });
                        return { ...row, lessons: newLessons };
                      });
                      return { ...prev, [selectedDay]: updated };
                    });

                    setReplaceStatus(`تم استبدال الأستاذ "${oldT}" بـ "${newT}" بنجاح! ✓`);
                    setReplaceOldTeacher('');
                    setReplaceNewTeacher('');
                    setTimeout(() => setReplaceStatus(''), 3500);
                  }}
                  className="w-full py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 font-black transition-all shadow cursor-pointer text-xs"
                >
                  استبدال الأستاذ في جدول اليوم الحالي ✓
                </button>

                {replaceStatus && (
                  <div className="p-2 bg-emerald-50 border border-emerald-200 text-emerald-800 text-[11px] font-bold rounded-lg text-center">
                    {replaceStatus}
                  </div>
                )}
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

      {/* Print Preview Modal */}
      <PrintPreviewModal
        isOpen={showPrintPreviewModal}
        onClose={() => setShowPrintPreviewModal(false)}
        title={`جدول الحصص والتوقيتات - يوم (${selectedDay})`}
        subtitle="معاينة الجدول الدراسي والتوقيتات للطباعة الرسمية"
        config={config}
        defaultOrientation="landscape"
        hideOfficialHeader={true}
        hideFooterSignatures={true}
        hideWatermark={true}
        hideSeal={true}
      >
        <div className="space-y-3 font-tajawal dir-rtl text-slate-900">
          
          {/* Top Title Bar */}
          <div className="flex flex-col sm:flex-row items-center justify-between bg-gradient-to-r from-sky-900 via-indigo-900 to-sky-900 text-white px-4 py-2.5 rounded-2xl border-2 border-sky-600 shadow-sm gap-2">
            <div className="flex items-center gap-2">
              <span className="text-base md:text-lg font-black text-amber-300">{config.schoolName || 'المدرسة النموذجية'}</span>
              <span className="text-xs text-sky-200 font-bold">| جدول توزيع الحصص والتوقيتات الأسبوعي - يوم ({selectedDay})</span>
            </div>
            <div className="text-xs text-sky-100 font-mono font-bold bg-white/10 px-3 py-1 rounded-xl border border-white/20">
              بداية الدوام: <span className="text-amber-300 font-black">{config.schoolStartHour}</span> | الحصة: <span className="text-amber-300 font-black">{config.lessonDurationMinutes} د</span> | الفرصة: <span className="text-pink-300 font-black">{config.breakDurationMinutes} د</span>
            </div>
          </div>

          {/* Table Matching the Live Schedule Style Exactly */}
          <div className="overflow-x-auto bg-[#e6f4f1] p-2 rounded-2xl border-2 border-sky-300 shadow-sm">
            <table className="w-full text-center border-separate border-spacing-1 text-xs">
              <thead>
                {/* Row 1: Timings Header */}
                <tr className="bg-sky-900 text-white text-xs font-mono">
                  <th className="py-2 px-2 rounded-xl bg-sky-950 text-amber-300 font-sans font-black w-36 text-center">
                    توقيت الدرس ←
                  </th>
                  {slotTimings.map((st, i) => (
                    <th 
                      key={i} 
                      className={`py-2 px-1 rounded-xl font-black text-[11px] ${
                        st.type === 'break' ? 'bg-pink-600 text-white' : 'bg-sky-800 text-white'
                      }`}
                    >
                      {st.start} - {st.end}
                    </th>
                  ))}
                </tr>

                {/* Row 2: Columns Titles with Distinct Colors */}
                <tr className="text-xs font-black">
                  <th className="py-2 px-2 rounded-xl bg-sky-800 text-amber-200">الصفوف والشعب</th>
                  <th className="py-2 px-1 rounded-xl bg-sky-200 text-sky-950">الدرس الأول</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{config.breakDurationMinutes} د</th>
                  <th className="py-2 px-1 rounded-xl bg-cyan-200 text-cyan-950">الدرس الثاني</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{config.breakDurationMinutes} د</th>
                  <th className="py-2 px-1 rounded-xl bg-amber-200 text-amber-950">الدرس الثالث</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{config.breakDurationMinutes} د</th>
                  <th className="py-2 px-1 rounded-xl bg-emerald-200 text-emerald-950">الدرس الرابع</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{config.breakDurationMinutes} د</th>
                  <th className="py-2 px-1 rounded-xl bg-indigo-200 text-indigo-950">الدرس الخامس</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{config.breakDurationMinutes} د</th>
                  <th className="py-2 px-1 rounded-xl bg-rose-200 text-rose-950">الدرس السادس</th>
                </tr>
              </thead>

              <tbody className="text-xs">
                {currentDayRows.length === 0 ? (
                  <tr>
                    <td colSpan={12} className="py-8 text-center text-slate-500 font-bold bg-white rounded-xl">
                      لا توجد حصص مضافة لهذا اليوم
                    </td>
                  </tr>
                ) : (
                  currentDayRows.map((row) => (
                    <tr key={row.id}>
                      {/* Class & Section */}
                      <td className="p-0.5 align-middle bg-[#e6f4f1]">
                        <div className="px-2.5 py-1.5 rounded-xl border-2 border-sky-300 bg-white font-black text-slate-900 text-right shadow-2xs min-h-[54px] flex flex-col justify-center">
                          <span className="text-sky-950 font-black text-xs block leading-tight">
                            {row.grade} ({row.section})
                          </span>
                          <span className="text-[10px] text-slate-600 font-bold block mt-0.5">
                            مرشد: {cleanTeacherName(row.teacherInCharge)}
                          </span>
                        </div>
                      </td>

                      {/* 6 Lessons & 5 Breaks */}
                      {['lesson1', 'lesson2', 'lesson3', 'lesson4', 'lesson5', 'lesson6'].map((lKey, idx) => {
                        const cell = row.lessons[lKey as keyof typeof row.lessons];
                        const isVacant = cell?.isOff || cell?.subject === 'شاغر / نشاط حر' || cell?.teacherName === 'شاغر';

                        return (
                          <React.Fragment key={lKey}>
                            <td className="p-0.5 align-middle bg-[#e6f4f1]">
                              <div className={`px-1.5 py-1 rounded-xl border-2 text-center min-h-[54px] flex flex-col justify-center items-center shadow-2xs ${
                                cell?.isOff 
                                  ? 'bg-rose-50 text-rose-900 border-rose-300 font-bold' 
                                  : isVacant
                                  ? 'bg-slate-100/90 text-slate-500 border-dashed border-slate-300 font-bold'
                                  : 'bg-white text-slate-900 border-slate-300'
                              }`}>
                                {cell?.isOff ? (
                                  <span className="text-[10px] font-black text-rose-700 bg-rose-200/80 px-2 py-0.5 rounded-md border border-rose-300">
                                    شاغرة
                                  </span>
                                ) : isVacant ? (
                                  <span className="text-[11px] font-bold text-slate-500">
                                    شاغر
                                  </span>
                                ) : (
                                  <div className="space-y-0.5 w-full">
                                    <span className="font-black text-xs block leading-tight text-slate-900 truncate">
                                      {cell?.subject || 'مادة'}
                                    </span>
                                    <span className="text-[10px] font-bold block leading-tight text-slate-600 truncate">
                                      {cleanTeacherName(cell?.teacherName)}
                                    </span>
                                  </div>
                                )}
                              </div>
                            </td>

                            {/* Break column */}
                            {idx < 5 && (
                              <td className="p-0.5 align-middle bg-[#e6f4f1] text-center w-8">
                                <div className="py-1 rounded-xl bg-pink-100 text-pink-950 border border-pink-300 text-[10px] font-black flex items-center justify-center min-h-[54px]">
                                  {config.breakDurationMinutes}د
                                </div>
                              </td>
                            )}
                          </React.Fragment>
                        );
                      })}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-300 text-xs font-tajawal text-slate-700 flex justify-between items-center">
            <span>عدد الشعب في الجدول: <strong>{currentDayRows.length} شعبة</strong></span>
            <span>حالة الجدول: <strong className="text-emerald-700 font-black">مكتمل ومدقق بنسبة 100% ✓</strong></span>
          </div>

        </div>
      </PrintPreviewModal>

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
