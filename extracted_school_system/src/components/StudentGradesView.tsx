import React, { useState } from 'react';
import { Student, AppConfig, StudentMark } from '../types';
import { 
  ClipboardList, 
  Search, 
  Filter, 
  Printer, 
  Sparkles, 
  X, 
  Award, 
  RefreshCw, 
  FileText, 
  CheckCircle2, 
  Save,
  Maximize2,
  Plus,
  Edit3,
  Check,
  GraduationCap,
  Layers,
  ChevronDown,
  Trash2
} from 'lucide-react';
import { importGradesAndAttendance, normalizeArabic } from '../utils/syncService';
import { standardizeSubjectName, standardizeGradeName, standardizeSectionName, sortGradesList, sortSectionsAlphabetically } from '../utils/syncEngine';
import { Portal } from './common/Portal';

interface StudentGradesViewProps {
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  config: AppConfig;
}

export const StudentGradesView: React.FC<StudentGradesViewProps> = ({
  students,
  setStudents,
  config
}) => {
  const uniqueGrades = sortGradesList(Array.from(new Set(students.map(s => s.currentGrade).filter(Boolean))));
  const uniqueSections = sortSectionsAlphabetically(Array.from(new Set(students.map(s => s.section).filter(Boolean))));

  const [selectedGrade, setSelectedGrade] = useState<string>(() => uniqueGrades[0] || 'الصف الأول');
  const [selectedSection, setSelectedSection] = useState<string>(() => uniqueSections[0] || 'أ');
  const [searchQuery, setSearchQuery] = useState('');
  const [isPulling, setIsPulling] = useState<boolean>(false);
  const [pullMsg, setPullMsg] = useState<string>('');

  // 1. Dynamic Subject List (المواد المعتمدة مع إمكانية التعديل والإضافة والحذف)
  const [subjectList, setSubjectList] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem('diyala_school_grade_subjects');
      return saved ? JSON.parse(saved) : [
        'التربية الإسلامية',
        'اللغة العربية',
        'اللغة الإنكليزية',
        'الرياضيات',
        'الاجتماعيات',
        'الكيمياء',
        'الأحياء',
        'الفيزياء',
        'النشاط البدني',
        'التربية الفنية',
        'التربية الأخلاقية',
        'الحاسوب'
      ];
    } catch {
      return [
        'التربية الإسلامية',
        'اللغة العربية',
        'اللغة الإنكليزية',
        'الرياضيات',
        'الاجتماعيات',
        'الكيمياء',
        'الأحياء',
        'الفيزياء',
        'النشاط البدني',
        'التربية الفنية',
        'التربية الأخلاقية',
        'الحاسوب'
      ];
    }
  });

  // Editing Subject Modal
  const [editingSubjectIndex, setEditingSubjectIndex] = useState<number | null>(null);
  const [editingSubjectName, setEditingSubjectName] = useState<string>('');
  const [newSubjectName, setNewSubjectName] = useState<string>('');
  const [showAddSubjectModal, setShowAddSubjectModal] = useState<boolean>(false);

  // Student Full Grade Expansion Modal
  const [expandedStudent, setExpandedStudent] = useState<Student | null>(null);

  // Helper: Get or create StudentMark object
  const getStudentMarkForSubject = (std: Student, subject: string): StudentMark => {
    const currentYear = std.registrationYear || '2025-2026';
    const normSubj = standardizeSubjectName(subject);
    let mark = (std.marksHistory || []).find(m => standardizeSubjectName(m.subject) === normSubj || normalizeArabic(m.subject) === normalizeArabic(subject));
    return mark || {
      year: currentYear,
      subject,
      term1Avg: 0,
      midtermFinalGrade: 0,
      term2Avg: 0,
      annualAverage: 0,
      finalWrittenD1: 0,
      finalWrittenD2: null,
      finalGrade: 0,
      m1MonthAvg: 0,
      m2MonthAvg: 0,
      m3MonthAvg: 0,
      m4MonthAvg: 0
    };
  };

  // Helper: Get latest month mark for student in subject
  const getLatestMonthMark = (std: Student, subject: string): number => {
    const normSubj = standardizeSubjectName(subject);
    const m = (std.marksHistory || []).find(mark => standardizeSubjectName(mark.subject) === normSubj || normalizeArabic(mark.subject) === normalizeArabic(subject));
    if (!m) return 0;
    if (m.finalGrade && m.finalGrade > 0) return m.finalGrade;
    if (m.annualAverage && m.annualAverage > 0) return m.annualAverage;
    if (m.midtermFinalGrade && m.midtermFinalGrade > 0) return m.midtermFinalGrade;
    if (m.m4MonthAvg && m.m4MonthAvg > 0) return m.m4MonthAvg;
    if (m.m3MonthAvg && m.m3MonthAvg > 0) return m.m3MonthAvg;
    if (m.term2Avg && m.term2Avg > 0) return m.term2Avg;
    if (m.m2MonthAvg && m.m2MonthAvg > 0) return m.m2MonthAvg;
    if (m.m1MonthAvg && m.m1MonthAvg > 0) return m.m1MonthAvg;
    if (m.term1Avg && m.term1Avg > 0) return m.term1Avg;
    if (m.finalWrittenD1 && m.finalWrittenD1 > 0) return m.finalWrittenD1;
    if (m.total && m.total > 0) return m.total;
    return 0;
  };

  // Helper: Update single mark in table directly
  const handleUpdateLatestMark = (studentId: string, subject: string, newMark: number) => {
    const currentYear = '2025-2026';
    setStudents(prev => {
      const updated = prev.map(s => {
        if (s.id !== studentId) return s;

        const history = [...(s.marksHistory || [])];
        const normSubj = normalizeArabic(subject);
        let markIndex = history.findIndex(m => normalizeArabic(m.subject) === normSubj);
        let markObj: StudentMark;

        if (markIndex > -1) {
          markObj = { 
            ...history[markIndex], 
            m1MonthAvg: newMark,
            annualAverage: newMark,
            finalGrade: newMark 
          };
          history[markIndex] = markObj;
        } else {
          markObj = {
            year: currentYear,
            subject,
            term1Avg: newMark,
            midtermFinalGrade: newMark,
            term2Avg: newMark,
            annualAverage: newMark,
            finalWrittenD1: newMark,
            finalWrittenD2: null,
            finalGrade: newMark,
            m1MonthAvg: newMark,
            m2MonthAvg: newMark,
            m3MonthAvg: newMark,
            m4MonthAvg: newMark
          };
          history.push(markObj);
        }

        return { ...s, marksHistory: history };
      });
      localStorage.setItem('diyala_school_students', JSON.stringify(updated));
      return updated;
    });
  };


  // Helper: Update detailed field in Expansion Modal
  const handleUpdateDetailedMark = (
    studentId: string,
    subject: string,
    field: keyof StudentMark,
    value: number | null
  ) => {
    const currentYear = '2024-2025';
    setStudents(prev => prev.map(s => {
      if (s.id !== studentId) return s;

      const history = [...(s.marksHistory || [])];
      let markIndex = history.findIndex(m => m.subject === subject && m.year === currentYear);
      let markObj: StudentMark;

      if (markIndex > -1) {
        markObj = { ...history[markIndex], [field]: value };
      } else {
        markObj = {
          year: currentYear,
          subject,
          term1Avg: 0,
          midtermFinalGrade: 0,
          term2Avg: 0,
          annualAverage: 0,
          finalWrittenD1: 0,
          finalWrittenD2: null,
          finalGrade: 0,
          [field]: value
        };
      }

      // Auto calculation logic
      const m1 = markObj.m1MonthAvg || 0;
      const m2 = markObj.m2MonthAvg || 0;
      if (m1 > 0 && m2 > 0) {
        markObj.term1Avg = Math.round((m1 + m2) / 2);
      } else if (m2 > 0) {
        markObj.term1Avg = m2;
      } else {
        markObj.term1Avg = 0;
      }

      const m3 = markObj.m3MonthAvg || 0;
      const m4 = markObj.m4MonthAvg || 0;
      if (m3 > 0 && m4 > 0) {
        markObj.term2Avg = Math.round((m3 + m4) / 2);
      } else if (m4 > 0) {
        markObj.term2Avg = m4;
      } else {
        markObj.term2Avg = 0;
      }

      const t1 = markObj.term1Avg || 0;
      const mid = markObj.midtermFinalGrade || 0;
      const t2 = markObj.term2Avg || 0;
      // يحسب السعي السنوي فقط عند اكتمال الفصلين ونصف السنة
      if (t1 > 0 && mid > 0 && t2 > 0) {
        markObj.annualAverage = Math.round((t1 + mid + t2) / 3);
      } else {
        markObj.annualAverage = 0;
      }

      const annual = markObj.annualAverage || 0;
      const d1 = markObj.finalWrittenD1 || 0;
      const d2 = markObj.finalWrittenD2;
      const finalExam = (d2 !== null && d2 !== undefined && d2 > 0) ? d2 : d1;
      // تحسب الدرجة النهائية فقط عند توفر السعي السنوي والامتحان النهائي
      if (annual > 0 && finalExam > 0) {
        markObj.finalGrade = Math.round((annual + finalExam) / 2);
      } else {
        markObj.finalGrade = 0;
      }

      if (markIndex > -1) {
        history[markIndex] = markObj;
      } else {
        history.push(markObj);
      }

      const updatedStudent = { ...s, marksHistory: history };
      if (expandedStudent && expandedStudent.id === studentId) {
        setExpandedStudent(updatedStudent);
      }

      return updatedStudent;
    }));
  };

  // Helper: Auto Result Calculation
  const calculateStudentResult = (std: Student) => {
    let failingCount = 0;
    let evaluatedCount = 0;

    subjectList.forEach(subj => {
      const mark = getLatestMonthMark(std, subj);
      if (mark > 0) {
        evaluatedCount++;
        if (mark < 50) {
          failingCount++;
        }
      }
    });

    if (evaluatedCount === 0) {
      return { text: 'غير مرصود', status: 'none', count: 0 };
    }

    if (failingCount === 0) {
      return { text: 'ناجح', status: 'pass', count: 0 };
    } else {
      return { text: `${failingCount}`, status: 'fail', count: failingCount };
    }
  };

  // Sync students from localStorage if updated elsewhere
  React.useEffect(() => {
    const handleDataUpdate = () => {
      try {
        const saved = localStorage.getItem('diyala_school_students');
        if (saved && saved !== 'undefined') {
          const list = JSON.parse(saved);
          if (Array.isArray(list) && list.length > 0) {
            setStudents(list);
          }
        }
      } catch (e) {}
    };
    window.addEventListener('school_data_updated', handleDataUpdate);
    window.addEventListener('storage', handleDataUpdate);
    return () => {
      window.removeEventListener('school_data_updated', handleDataUpdate);
      window.removeEventListener('storage', handleDataUpdate);
    };
  }, [setStudents]);

  // Filtered and Alphabetically Sorted list with Smart Arabic & Grade matching
  const filteredStudents = students
    .filter(s => {
      // Include student if status is continuing OR missing/empty (to ensure all added students appear)
      const isContinuing = !s.status || ['active', 'مستمر', 'muted', 'نشط'].includes(s.status);
      if (!isContinuing) return false;

      const query = searchQuery.trim();
      if (query) {
        const full = `${s.firstName} ${s.secondName || ''} ${s.thirdName || ''} ${s.fourthName || ''} ${s.titleName || ''} ${s.fullName || ''}`
          .toLowerCase()
          .replace(/[أإآ]/g, 'ا')
          .replace(/ة/g, 'ه')
          .replace(/ى/g, 'ي')
          .replace(/[\u064B-\u065F\u0670]/g, '')
          .replace(/ـ/g, '');
        const cleanQuery = query
          .toLowerCase()
          .replace(/[أإآ]/g, 'ا')
          .replace(/ة/g, 'ه')
          .replace(/ى/g, 'ي')
          .replace(/[\u064B-\u065F\u0670]/g, '')
          .replace(/ـ/g, '');

        const matchesQuery = full.includes(cleanQuery) || 
                             (s.recordNumber && s.recordNumber.includes(cleanQuery));
        // Global search across grades and sections when query is typed!
        return matchesQuery;
      }

      if (selectedGrade !== 'الكل') {
        const stdG = standardizeGradeName(s.currentGrade);
        const selG = standardizeGradeName(selectedGrade);
        if (stdG !== selG && s.currentGrade !== selectedGrade) return false;
      }

      if (selectedSection !== 'الكل') {
        const stdS = standardizeSectionName(s.section);
        const selS = standardizeSectionName(selectedSection);
        if (stdS !== selS && s.section !== selectedSection) return false;
      }

      return true;
    })
    .sort((a, b) => {
      const nameA = [a.firstName, a.secondName, a.thirdName, a.fourthName, a.titleName].filter(Boolean).join(' ').trim();
      const nameB = [b.firstName, b.secondName, b.thirdName, b.fourthName, b.titleName].filter(Boolean).join(' ').trim();
      return nameA.localeCompare(nameB, 'ar', { sensitivity: 'base' });
    });

  const handlePullGrades = async () => {
    const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
    setIsPulling(true);
    setPullMsg('');
    const res = await importGradesAndAttendance(schoolId, students);
    setIsPulling(false);
    if (res.success && res.updatedStudents) {
      setStudents(res.updatedStudents);
      localStorage.setItem('diyala_school_students', JSON.stringify(res.updatedStudents));
      setPullMsg('تم سحب وتحديث درجات المدرسين من السحابة بنجاح! ✅');
      setTimeout(() => setPullMsg(''), 4000);
    } else {
      setPullMsg('تنبيه: ' + res.message);
    }
  };

  // Delete Subject Column Handler
  const handleDeleteSubject = (subjectToDelete: string) => {
    if (confirm(`هل أنت متأكد من حذف عمود مادة (${subjectToDelete}) من سجل الدرجات؟`)) {
      setSubjectList(prev => {
        const next = prev.filter(s => s !== subjectToDelete);
        localStorage.setItem('diyala_school_grade_subjects', JSON.stringify(next));
        return next;
      });
      setEditingSubjectIndex(null);
    }
  };

  // Add Subject Handler
  const handleAddSubject = () => {
    const trimmed = newSubjectName.trim();
    if (trimmed && !subjectList.includes(trimmed)) {
      setSubjectList(prev => {
        const next = [...prev, trimmed];
        localStorage.setItem('diyala_school_grade_subjects', JSON.stringify(next));
        return next;
      });
      setNewSubjectName('');
      setShowAddSubjectModal(false);
    }
  };

  // Edit Subject Name Handler
  const handleSaveSubjectName = (index: number) => {
    const trimmed = editingSubjectName.trim();
    if (trimmed && index >= 0 && index < subjectList.length) {
      setSubjectList(prev => {
        const next = [...prev];
        next[index] = trimmed;
        localStorage.setItem('diyala_school_grade_subjects', JSON.stringify(next));
        return next;
      });
      setEditingSubjectIndex(null);
      setEditingSubjectName('');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6 dir-rtl">
      
      {/* Top Banner */}
      <div className="bg-white p-5 rounded-2xl border-3 border-amber-500 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-2xl bg-amber-500 text-white shadow-lg">
            <ClipboardList className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-black text-slate-900">
              سجل درجات الطلاب المعتمد ومصفوفة المواد
            </h2>
            <p className="text-xs text-slate-600 font-bold mt-0.5">
              درجة آخر شهر لكل مادة أمام اسم الطالب | حساب النتيجة التلقائي (ناجح / عدد الإكمال) | زر التوسعة الشامل
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setShowAddSubjectModal(true)}
            className="flex items-center gap-1.5 px-3.5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs transition-all shadow-md cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>إضافة مادة دراسية</span>
          </button>

          <button
            onClick={handlePullGrades}
            disabled={isPulling}
            className="flex items-center gap-1.5 px-3.5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs transition-all shadow-md cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isPulling ? 'animate-spin' : ''}`} />
            <span>سحب درجات السحابة ☁️</span>
          </button>

          <button
            onClick={() => window.print()}
            className="flex items-center gap-1.5 px-3.5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs transition-all shadow-md cursor-pointer"
          >
            <Printer className="w-4 h-4 text-amber-400" />
            <span>طباعة A4</span>
          </button>
        </div>
      </div>

      {pullMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-300 text-emerald-900 rounded-xl text-xs font-black text-center flex items-center justify-center gap-2 animate-fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          <span>{pullMsg}</span>
        </div>
      )}

      {/* Dynamic Class & Section & Search Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
          
          {/* Grade Selector */}
          <div>
            <label className="block font-bold text-slate-500 mb-1">الصف الدراسي:</label>
            <select 
              value={selectedGrade} 
              onChange={e => setSelectedGrade(e.target.value)} 
              className="w-full p-2.5 rounded-xl border-2 border-amber-200 font-bold text-slate-900 outline-none focus:border-amber-500 bg-slate-50 text-xs shadow-xs"
            >
              <option value="الكل">جميع الصفوف</option>
              {uniqueGrades.map(g => <option key={g} value={g}>{g}</option>)}
            </select>
          </div>

          {/* Section Selector */}
          <div>
            <label className="block font-bold text-slate-500 mb-1">الشعبة:</label>
            <select 
              value={selectedSection} 
              onChange={e => setSelectedSection(e.target.value)} 
              className="w-full p-2.5 rounded-xl border-2 border-amber-200 font-bold text-slate-900 outline-none focus:border-amber-500 bg-slate-50 text-xs shadow-xs"
            >
              <option value="الكل">جميع الشعب</option>
              {uniqueSections.map(s => <option key={s} value={s}>شعبة ({s})</option>)}
            </select>
          </div>

          {/* Search Input */}
          <div>
            <label className="block font-bold text-slate-500 mb-1">بحث سريع:</label>
            <div className="relative">
              <Search className="w-4 h-4 absolute right-3 top-3 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="ابحث بالاسم أو رقم القيد..."
                className="w-full pr-9 pl-3 py-2 rounded-xl border-2 border-amber-200 font-bold text-slate-900 outline-none focus:border-amber-500 bg-slate-50 text-xs shadow-xs"
              />
            </div>
          </div>

        </div>

        {/* Quick Grade & Section Pills */}
        <div className="flex flex-wrap items-center gap-1.5 pt-2 border-t border-slate-100">
          <button
            type="button"
            onClick={() => { setSelectedGrade('الكل'); setSelectedSection('الكل'); }}
            className={`px-3 py-1 rounded-xl text-xs font-black transition-all cursor-pointer border ${
              selectedGrade === 'الكل' && selectedSection === 'الكل'
                ? 'bg-amber-600 text-white border-amber-500 shadow-sm scale-105'
                : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-300'
            }`}
          >
            🌟 كافة الصفوف والشعب ({students.filter(s => !s.status || ['active', 'مستمر', 'muted', 'نشط'].includes(s.status)).length} طالب)
          </button>
          {uniqueGrades.map(g => (
            <button
              key={g}
              type="button"
              onClick={() => { setSelectedGrade(g); }}
              className={`px-2.5 py-1 rounded-xl text-xs font-bold transition-all cursor-pointer border ${
                selectedGrade === g
                  ? 'bg-amber-100 text-amber-950 border-amber-300 font-black shadow-xs'
                  : 'bg-white hover:bg-slate-100 text-slate-700 border-slate-200'
              }`}
            >
              {g}
            </button>
          ))}
        </div>
      </div>

      {/* Main Grades Matrix Table (جدول المواد المعتمدة ودرجة آخر شهر والنتيجة الآلية) */}
      <div className="data-grid-shell bg-white rounded-2xl border-2 border-slate-300 shadow-xl overflow-hidden">
        
        {/* Quick Top Scroll Strip / Control Indicator */}
        <div className="bg-slate-100 border-b border-slate-200 px-4 py-2 flex items-center justify-between text-xs text-slate-700 font-bold">
          <div className="flex items-center gap-2">
            <span className="bg-amber-100 text-amber-900 px-2.5 py-0.5 rounded-md text-[11px] font-black">
              ↔️ شريط التمرير الجانبي المباشر:
            </span>
            <span className="text-slate-600 hidden sm:inline">
              يمكنك التمرير يميناً ويساراً لتصفح درجات كافة المواد أو استخدام الأزرار:
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={() => {
                const el = document.getElementById('grades-table-scroll-container');
                if (el) el.scrollBy({ left: -300, behavior: 'smooth' });
              }}
              className="px-3 py-1 rounded-lg bg-white hover:bg-slate-200 text-slate-800 border border-slate-300 text-xs font-bold transition-all shadow-xs cursor-pointer flex items-center gap-1 active:scale-95"
            >
              <span>⬅️ تمرير لليسار</span>
            </button>
            <button
              type="button"
              onClick={() => {
                const el = document.getElementById('grades-table-scroll-container');
                if (el) el.scrollBy({ left: 300, behavior: 'smooth' });
              }}
              className="px-3 py-1 rounded-lg bg-white hover:bg-slate-200 text-slate-800 border border-slate-300 text-xs font-bold transition-all shadow-xs cursor-pointer flex items-center gap-1 active:scale-95"
            >
              <span>➡️ تمرير لليمين</span>
            </button>
          </div>
        </div>

        <div id="grades-table-scroll-container" className="overflow-x-auto custom-scrollbar">
          <table className="data-grid w-full text-center border-collapse text-xs min-w-[980px]">
            <thead>
              <tr className="bg-gradient-to-r from-amber-700 via-amber-600 to-orange-700 text-white font-black border-b-2 border-amber-400 text-xs">
                <th className="py-2.5 px-1 border-r border-amber-500 w-8 text-center whitespace-nowrap text-[11px]">ت</th>
                <th className="py-2.5 px-1 border-r border-amber-500 w-14 text-center whitespace-nowrap text-[11px]">القيد</th>
                <th className="py-2.5 px-2.5 border-r border-amber-500 text-right whitespace-nowrap min-w-[150px] text-xs">اسم الطالب الرباعي واللقب</th>
                <th className="py-2.5 px-1 border-r border-amber-500 text-center whitespace-nowrap w-20 text-[11px]">الشعبة</th>
                
                {/* Dynamic Subject Column Headers (Compact Size) */}
                {subjectList.map((subjectName, idx) => {
                  const shortTitle = subjectName
                    .replace(/^التربية\s+/, '')
                    .replace(/^اللغة\s+/, '');

                  return (
                    <th key={subjectName} className="py-2 px-1 border-r border-amber-500 text-center whitespace-nowrap min-w-[62px] max-w-[72px]" title={subjectName}>
                      <div className="flex flex-col items-center justify-center gap-0.5 group">
                        <span className="text-[11px] font-black leading-tight tracking-tight">{shortTitle}</span>
                        <div className="flex items-center gap-0.5 opacity-60 group-hover:opacity-100 transition-opacity">
                          <button
                            onClick={() => {
                              setEditingSubjectIndex(idx);
                              setEditingSubjectName(subjectName);
                            }}
                            className="p-0.5 rounded hover:bg-amber-600/60 cursor-pointer"
                            title={`تعديل اسم مادة (${subjectName})`}
                          >
                            <Edit3 className="w-2.5 h-2.5 text-amber-200" />
                          </button>
                          <button
                            onClick={() => handleDeleteSubject(subjectName)}
                            className="p-0.5 rounded hover:bg-rose-700 text-rose-200 cursor-pointer"
                            title={`حذف عمود (${subjectName})`}
                          >
                            <Trash2 className="w-2.5 h-2.5" />
                          </button>
                        </div>
                      </div>
                    </th>
                  );
                })}

                {/* Auto Result Column */}
                <th className="py-2.5 px-1.5 border-r border-amber-500 text-center whitespace-nowrap bg-amber-900/40 text-amber-200 font-black w-18 text-[11px]">
                  النتيجة
                </th>

                {/* Detailed Expansion Button Column */}
                <th className="py-2.5 px-1.5 text-center whitespace-nowrap w-14 text-[11px]">
                  توسعة 🔍
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-200">
              {filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan={subjectList.length + 6} className="py-12 text-center text-slate-500 font-bold">
                    لا يوجد طلاب مطابقون لخيارات الفلترة الحالية.
                  </td>
                </tr>
              ) : (
                filteredStudents.map((std, i) => {
                  const resultInfo = calculateStudentResult(std);

                  return (
                    <tr key={std.id} className="hover:bg-amber-50/60 transition-colors">
                      {/* 1. Seq */}
                      <td className="py-2 px-1 font-mono font-bold border-r border-slate-200 text-slate-700 bg-slate-50 text-[11px]">
                        {i + 1}
                      </td>

                      {/* 2. Record No */}
                      <td className="py-2 px-1 font-mono font-bold border-r border-slate-200 text-blue-900 text-[11px]">
                        #{std.recordNumber}
                      </td>

                      {/* 3. Full Name */}
                      <td className="py-2 px-2.5 font-black border-r border-slate-200 text-right whitespace-nowrap text-slate-950 text-xs">
                        <div className="flex items-center gap-1.5 justify-start">
                          <span>{std.firstName} {std.secondName} {std.thirdName} {std.fourthName || ''} {std.titleName || ''}</span>
                          {std.addedByTeacher && (
                            <span 
                              className="px-1.5 py-0.5 rounded-full text-[9px] font-black bg-purple-100 text-purple-800 border border-purple-300 shrink-0" 
                              title="تمت إضافة هذا الطالب من قبل مدرس المادة في تطبيق الهاتف"
                            >
                              مضاف من الأستاذ 👨‍🏫
                            </span>
                          )}
                        </div>
                      </td>

                      {/* 4. Grade & Section */}
                      <td className="py-2 px-1 font-bold border-r border-slate-200 whitespace-nowrap text-center">
                        <span className="px-1.5 py-0.5 rounded-md bg-slate-100 text-slate-800 text-[10px]">
                          {std.currentGrade} ({std.section})
                        </span>
                      </td>

                      {/* 5. Marks for each subject (Latest Month - Compact Width) */}
                      {subjectList.map(subj => {
                        const markVal = getLatestMonthMark(std, subj);
                        const isFail = markVal > 0 && markVal < 50;
                        const isPass = markVal >= 50;

                        return (
                          <td key={subj} className="py-1 px-1 border-r border-slate-200 font-mono text-center">
                            <input
                              type="number"
                              min="0"
                              max="100"
                              value={markVal > 0 ? markVal : ''}
                              onChange={(e) => {
                                const val = parseInt(e.target.value, 10);
                                handleUpdateLatestMark(std.id, subj, isNaN(val) ? 0 : val);
                              }}
                              placeholder="—"
                              className={`w-11 text-center py-1 rounded-md font-black text-xs border transition-all focus:outline-none focus:ring-2 focus:ring-amber-400 ${
                                isFail 
                                  ? 'bg-rose-50 text-rose-700 border-rose-300 font-black' 
                                  : isPass 
                                    ? 'bg-emerald-50 text-emerald-800 border-emerald-300' 
                                    : 'bg-white text-slate-600 border-slate-200'
                              }`}
                            />
                          </td>
                        );
                      })}

                      {/* 6. Auto Calculated Result */}
                      <td className="py-2 px-1.5 border-r border-slate-200 text-center whitespace-nowrap">
                        {resultInfo.status === 'pass' && (
                          <span className="inline-block px-2 py-0.5 rounded-full bg-emerald-600 text-white font-black text-[11px] shadow-xs">
                            ناجح
                          </span>
                        )}
                        {resultInfo.status === 'fail' && (
                          <span className="inline-block px-3 py-1 rounded-full bg-rose-600 text-white font-black text-xs shadow-xs" title={`مكمل في ${resultInfo.count} مادة`}>
                            {resultInfo.count}
                          </span>
                        )}
                        {resultInfo.status === 'none' && (
                          <span className="text-slate-400 font-bold text-[11px]">
                            —
                          </span>
                        )}
                      </td>

                      {/* 7. Expansion Button */}
                      <td className="py-2 px-2 text-center whitespace-nowrap">
                        <button
                          onClick={() => setExpandedStudent(std)}
                          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 font-black text-xs shadow-xs transition-all cursor-pointer"
                          title="عرض وتعديل التفصيل الكامل لدرجات الطالب في جميع الأشهر والفصول"
                        >
                          <Maximize2 className="w-3.5 h-3.5" />
                          <span>توسعة</span>
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

      {/* Modal 1: Full Expandable Student Grade Sheet (تفصيل الدرجات الشامل كشهادة النتيجة) */}
      {expandedStudent && (
        <Portal>
          <div 
            onClick={(e) => { if (e.target === e.currentTarget) setExpandedStudent(null); }}
            className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in dir-rtl"
          >
            <div className="bg-white border-2 border-amber-400 rounded-3xl max-w-5xl w-full shadow-2xl flex flex-col max-h-[85vh] md:max-h-[90vh] overflow-hidden">
              
              {/* Modal Header */}
              <div className="p-5 border-b shrink-0 flex items-center justify-between bg-gradient-to-r from-amber-50 to-orange-50">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 bg-amber-500 text-white rounded-2xl shadow-md">
                    <Award className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="text-base font-black text-slate-900">
                      بطاقة وسجل الدرجات التفصيلي للطالب (سجل الأستاذ الشامل)
                    </h3>
                    <p className="text-xs text-slate-600 font-bold">
                      الطالب: <span className="text-amber-800 font-black text-sm">{expandedStudent.firstName} {expandedStudent.secondName} {expandedStudent.thirdName} {expandedStudent.fourthName || ''} {expandedStudent.titleName || ''}</span> | رقم القيد: #{expandedStudent.recordNumber} | الصف: {expandedStudent.currentGrade} ({expandedStudent.section})
                    </p>
                  </div>
                </div>

                <button 
                  onClick={() => setExpandedStudent(null)}
                  className="p-2 rounded-xl bg-white hover:bg-rose-100 text-slate-500 hover:text-rose-600 transition-colors cursor-pointer border shadow-xs"
                  title="إغلاق النافذة"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Scrollable Detailed Grade Breakdown Table */}
              <div className="p-4 overflow-y-auto flex-1">
                <div className="overflow-x-auto border-2 border-slate-200 rounded-2xl shadow-inner">
                  <table className="data-grid w-full text-center border-collapse text-xs min-w-[950px]">
                    <thead className="sticky top-0 bg-slate-900 text-white font-black z-10">
                      <tr className="text-xs">
                        <th className="py-3 px-3 border-r border-slate-700 text-right min-w-[150px]">المادة الدراسية</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-sky-950/40">ش1</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-sky-950/40">ش2</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-blue-900/60 font-black text-blue-200">معدل ف1</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-indigo-900/60 font-black text-amber-300">نصف السنة</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-purple-950/40">ش3</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-purple-950/40">ش4</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-purple-900/60 font-black text-purple-200">معدل ف2</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-amber-900/60 font-black text-amber-200">السعي السنوي</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-slate-800">نهائي د1</th>
                        <th className="py-3 px-2 border-r border-slate-700 bg-slate-800">نهائي د2</th>
                        <th className="py-3 px-3 bg-emerald-900 font-black text-white">الدرجة النهائية</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {subjectList.map(subj => {
                        const m = getStudentMarkForSubject(expandedStudent, subj);

                        return (
                          <tr key={subj} className="hover:bg-amber-50/40 transition-colors">
                            <td className="py-2.5 px-3 border-r border-slate-200 font-black text-right text-slate-900 bg-slate-50">
                              {subj}
                            </td>

                            {/* M1 */}
                            <td className="py-1.5 px-1 border-r border-slate-200">
                              <input 
                                type="number" min="0" max="100"
                                value={m.m1MonthAvg || ''}
                                onChange={(e) => handleUpdateDetailedMark(expandedStudent.id, subj, 'm1MonthAvg', parseInt(e.target.value, 10) || 0)}
                                className="w-12 text-center p-1 rounded border border-slate-300 font-mono text-xs"
                              />
                            </td>

                            {/* M2 */}
                            <td className="py-1.5 px-1 border-r border-slate-200">
                              <input 
                                type="number" min="0" max="100"
                                value={m.m2MonthAvg || ''}
                                onChange={(e) => handleUpdateDetailedMark(expandedStudent.id, subj, 'm2MonthAvg', parseInt(e.target.value, 10) || 0)}
                                className="w-12 text-center p-1 rounded border border-slate-300 font-mono text-xs"
                              />
                            </td>

                            {/* Term 1 Avg */}
                            <td className="py-2.5 px-1.5 border-r border-slate-200 font-mono font-bold text-blue-900 bg-blue-50/50">
                              {m.term1Avg || 0}
                            </td>

                            {/* Midterm Final */}
                            <td className="py-1.5 px-1 border-r border-slate-200 bg-amber-50/40">
                              <input 
                                type="number" min="0" max="100"
                                value={m.midtermFinalGrade || ''}
                                onChange={(e) => handleUpdateDetailedMark(expandedStudent.id, subj, 'midtermFinalGrade', parseInt(e.target.value, 10) || 0)}
                                className="w-12 text-center p-1 rounded border-2 border-amber-300 font-mono font-black text-xs bg-white"
                              />
                            </td>

                            {/* M3 */}
                            <td className="py-1.5 px-1 border-r border-slate-200">
                              <input 
                                type="number" min="0" max="100"
                                value={m.m3MonthAvg || ''}
                                onChange={(e) => handleUpdateDetailedMark(expandedStudent.id, subj, 'm3MonthAvg', parseInt(e.target.value, 10) || 0)}
                                className="w-12 text-center p-1 rounded border border-slate-300 font-mono text-xs"
                              />
                            </td>

                            {/* M4 */}
                            <td className="py-1.5 px-1 border-r border-slate-200">
                              <input 
                                type="number" min="0" max="100"
                                value={m.m4MonthAvg || ''}
                                onChange={(e) => handleUpdateDetailedMark(expandedStudent.id, subj, 'm4MonthAvg', parseInt(e.target.value, 10) || 0)}
                                className="w-12 text-center p-1 rounded border border-slate-300 font-mono text-xs"
                              />
                            </td>

                            {/* Term 2 Avg */}
                            <td className="py-2.5 px-1.5 border-r border-slate-200 font-mono font-bold text-purple-900 bg-purple-50/50">
                              {m.term2Avg || 0}
                            </td>

                            {/* Annual Average */}
                            <td className="py-2.5 px-1.5 border-r border-slate-200 font-mono font-black text-amber-900 bg-amber-100/60">
                              {m.annualAverage || 0}
                            </td>

                            {/* Final D1 */}
                            <td className="py-1.5 px-1 border-r border-slate-200">
                              <input 
                                type="number" min="0" max="100"
                                value={m.finalWrittenD1 || ''}
                                onChange={(e) => handleUpdateDetailedMark(expandedStudent.id, subj, 'finalWrittenD1', parseInt(e.target.value, 10) || 0)}
                                className="w-12 text-center p-1 rounded border border-slate-300 font-mono text-xs"
                              />
                            </td>

                            {/* Final D2 */}
                            <td className="py-1.5 px-1 border-r border-slate-200">
                              <input 
                                type="number" min="0" max="100"
                                value={m.finalWrittenD2 || ''}
                                onChange={(e) => handleUpdateDetailedMark(expandedStudent.id, subj, 'finalWrittenD2', parseInt(e.target.value, 10) || 0)}
                                className="w-12 text-center p-1 rounded border border-slate-300 font-mono text-xs"
                              />
                            </td>

                            {/* Final Grade */}
                            <td className="py-2.5 px-2 font-mono font-black text-emerald-950 bg-emerald-50 text-sm">
                              {m.finalGrade || 0}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Pinned Sticky Modal Footer - Never gets cut off */}
              <div className="p-4 bg-slate-50 border-t shrink-0 flex items-center justify-between">
                <div className="text-xs font-bold text-slate-600">
                  💡 يتم احتساب السعي السنوي والمعدلات تلقائياً فور كتابة أي درجة.
                </div>
                <button
                  onClick={() => setExpandedStudent(null)}
                  className="px-6 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 font-black text-xs shadow-md transition-all cursor-pointer"
                >
                  إغلاق وحفظ التعديلات ✓
                </button>
              </div>

            </div>
          </div>
        </Portal>
      )}

      {/* Modal 2: Edit Subject Name */}
      {editingSubjectIndex !== null && (
        <Portal>
          <div 
            onClick={(e) => { if (e.target === e.currentTarget) setEditingSubjectIndex(null); }}
            className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in dir-rtl"
          >
            <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-4 border-2 border-amber-300">
              <div className="flex items-center justify-between border-b pb-2">
                <h4 className="font-black text-sm text-slate-900">تعديل اسم المادة الدراسية:</h4>
                <button 
                  onClick={() => setEditingSubjectIndex(null)}
                  className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              <input
                type="text"
                value={editingSubjectName}
                onChange={e => setEditingSubjectName(e.target.value)}
                className="w-full p-2.5 rounded-xl border-2 border-amber-300 font-bold text-xs outline-none focus:border-amber-500"
              />
              <div className="flex items-center justify-between pt-2 border-t">
                <button 
                  onClick={() => handleDeleteSubject(subjectList[editingSubjectIndex])} 
                  className="px-3 py-2 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>حذف المادة 🗑️</span>
                </button>
                <div className="flex gap-2">
                  <button onClick={() => setEditingSubjectIndex(null)} className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold cursor-pointer">إلغاء</button>
                  <button onClick={() => handleSaveSubjectName(editingSubjectIndex)} className="px-4 py-2 rounded-xl bg-amber-500 text-slate-950 font-black text-xs cursor-pointer shadow-xs">حفظ الاسم</button>
                </div>
              </div>
            </div>
          </div>
        </Portal>
      )}

      {/* Modal 3: Add New Subject */}
      {showAddSubjectModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-5 max-w-md w-full shadow-2xl space-y-4">
            <h4 className="font-black text-sm text-slate-900">إضافة مادة دراسية جديدة للمصفوفة:</h4>
            <input
              type="text"
              value={newSubjectName}
              onChange={e => setNewSubjectName(e.target.value)}
              placeholder="اكتب اسم المادة (مثال: التربية الوطنية، الكردية...)"
              className="w-full p-2.5 rounded-xl border-2 border-indigo-300 font-bold text-xs"
            />
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowAddSubjectModal(false)} className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold">إلغاء</button>
              <button onClick={handleAddSubject} className="px-4 py-2 rounded-xl bg-indigo-600 text-white font-black text-xs">إضافة المادة</button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
