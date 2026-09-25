import React, { useState, useEffect } from 'react';
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
  Trash2,
  Calendar,
  Building2,
  BookOpen
} from 'lucide-react';
import { importGradesAndAttendance, normalizeArabic } from '../utils/syncService';
import { 
  standardizeSubjectName, 
  standardizeGradeName, 
  standardizeSectionName, 
  sortGradesList, 
  sortSectionsAlphabetically, 
  sanitizeStudents 
} from '../utils/syncEngine';
import { Portal } from './common/Portal';
import { DailyGradeRegister } from './grades/DailyGradeRegister';
import { TeacherGraduatedRegister } from './grades/TeacherGraduatedRegister';
import { AdminMasterRegister } from './grades/AdminMasterRegister';
import { StudentGradeProfileModal } from './grades/StudentGradeProfileModal';

interface StudentGradesViewProps {
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  config: AppConfig;
}

export type MasterRegisterTab = 'daily' | 'teacher' | 'admin';

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
  
  // 1. التبويب الرئيسي للسجلات الثلاثية
  const [activeTab, setActiveTab] = useState<MasterRegisterTab>('daily');

  // 2. بطاقة درجات الطالب الشاملة 360° المنبثقة
  const [selectedStudentForProfile, setSelectedStudentForProfile] = useState<Student | null>(null);

  // 3. حالة سحب الدرجات السحابية
  const [isPulling, setIsPulling] = useState<boolean>(false);
  const [pullMsg, setPullMsg] = useState<string>('');

  // 4. قائمة المواد الدراسية المعتمدة مع إمكانية التخصيص
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

  // نافذة إضافة / تعديل مادة
  const [showAddSubjectModal, setShowAddSubjectModal] = useState<boolean>(false);
  const [editingSubjectIndex, setEditingSubjectIndex] = useState<number | null>(null);
  const [editingSubjectName, setEditingSubjectName] = useState<string>('');
  const [newSubjectName, setNewSubjectName] = useState<string>('');

  // تحديث وحفظ قائمة المواد
  const saveSubjects = (newList: string[]) => {
    setSubjectList(newList);
    try {
      localStorage.setItem('diyala_school_grade_subjects', JSON.stringify(newList));
    } catch {}
  };

  const handleAddSubject = () => {
    const trimmed = newSubjectName.trim();
    if (!trimmed) return;
    if (subjectList.includes(trimmed)) {
      alert('المادة موجودة بالفعل مسبقاً');
      return;
    }
    const updated = [...subjectList, trimmed];
    saveSubjects(updated);
    setNewSubjectName('');
    setShowAddSubjectModal(false);
  };

  const handleUpdateSubjectName = (index: number) => {
    const trimmed = editingSubjectName.trim();
    if (!trimmed) return;
    const oldName = subjectList[index];
    const updated = [...subjectList];
    updated[index] = trimmed;
    saveSubjects(updated);

    // تحديث درجات الطلاب الحالية للاسم الجديد
    setStudents(prev => {
      const cloned = prev.map(std => {
        const history = (std.marksHistory || []).map(m => {
          if (m.subject === oldName) {
            return { ...m, subject: trimmed };
          }
          return m;
        });
        return { ...std, marksHistory: history };
      });
      try {
        localStorage.setItem('diyala_school_students', JSON.stringify(cloned));
      } catch {}
      return cloned;
    });

    setEditingSubjectIndex(null);
    setEditingSubjectName('');
  };

  const handleDeleteSubject = (subjToDelete: string) => {
    if (!confirm(`هل أنت متأكد من حذف مادة (${subjToDelete})؟`)) return;
    const updated = subjectList.filter(s => s !== subjToDelete);
    saveSubjects(updated);
  };

  // دالة تحديث درجة طالب موحدة وشاملة
  const handleUpdateStudentMark = (
    studentId: string,
    subject: string,
    updater: (prevMark: StudentMark) => StudentMark
  ) => {
    const currentYear = '2025-2026';
    setStudents(prev => {
      const targetIndex = prev.findIndex(s => s.id === studentId);
      if (targetIndex === -1) return prev;

      const updated = [...prev];
      const targetStudent = { ...updated[targetIndex] };
      const history = [...(targetStudent.marksHistory || [])];
      const normSubj = normalizeArabic(subject);
      let markIndex = history.findIndex(m => normalizeArabic(m.subject) === normSubj);

      let currentMark: StudentMark = markIndex > -1 ? history[markIndex] : {
        year: targetStudent.registrationYear || currentYear,
        subject,
        term1Avg: 0,
        midtermFinalGrade: 0,
        term2Avg: 0,
        annualAverage: 0,
        finalWrittenD1: 0,
        finalGrade: 0
      };

      const nextMark = updater(currentMark);

      if (markIndex > -1) {
        history[markIndex] = nextMark;
      } else {
        history.push(nextMark);
      }

      targetStudent.marksHistory = history;
      updated[targetIndex] = targetStudent;

      if (selectedStudentForProfile && selectedStudentForProfile.id === studentId) {
        setSelectedStudentForProfile(targetStudent);
      }

      try {
        localStorage.setItem('diyala_school_students', JSON.stringify(updated));
      } catch {}
      return updated;
    });
  };

  // سحب الدرجات المرفوعة من تطبيق الأستاذ
  const handlePullCloudGrades = async () => {
    const schoolId = config.schoolId || 'SCH-VCOL-6072';
    setIsPulling(true);
    setPullMsg('جارٍ جلب وتحديث درجات الطلاب من سحابة تطبيق الأستاذ...');
    try {
      const result = await importGradesAndAttendance(schoolId, students, config.customSupabaseUrl, config.customSupabaseKey);
      if (result.success && result.students) {
        setStudents(result.students);
        setPullMsg(`✅ تم تحديث ${result.gradesCount || 0} سجلاً للدرجات بنجاح من تطبيق الأستاذ!`);
      } else {
        setPullMsg(`⚠️ تعذر جلب الدرجات: ${result.error || 'تحقق من اتصال الإنترنت'}`);
      }
    } catch (err: any) {
      setPullMsg(`❌ خطأ في الاتصال: ${err.message || err}`);
    } finally {
      setIsPulling(false);
      setTimeout(() => setPullMsg(''), 6000);
    }
  };

  // تصفية الطلاب
  const filteredStudents = students
    .filter(s => {
      const isContinuing = !s.status || ['active', 'مستمر', 'muted', 'نشط'].includes(s.status);
      if (!isContinuing) return false;

      if (selectedGrade !== 'الكل') {
        const stdGrade = standardizeGradeName(s.currentGrade || '');
        const selGrade = standardizeGradeName(selectedGrade);
        if (stdGrade !== selGrade) return false;
      }

      if (selectedSection !== 'الكل') {
        const stdSec = standardizeSectionName(s.section || '');
        const selSec = standardizeSectionName(selectedSection);
        if (stdSec !== selSec) return false;
      }

      if (searchQuery.trim()) {
        const q = normalizeArabic(searchQuery.trim().toLowerCase());
        const name = normalizeArabic((s.fullName || `${s.firstName} ${s.secondName} ${s.thirdName || ''} ${s.titleName || ''}`).toLowerCase());
        const rec = (s.recordNumber || '').toLowerCase();
        if (!name.includes(q) && !rec.includes(q)) return false;
      }

      return true;
    })
    .sort((a, b) => {
      const nameA = `${a.firstName || ''} ${a.secondName || ''} ${a.thirdName || ''}`.trim();
      const nameB = `${b.firstName || ''} ${b.secondName || ''} ${b.thirdName || ''}`.trim();
      return nameA.localeCompare(nameB, 'ar');
    });

  return (
    <div className="space-y-5 pb-12 dir-rtl">
      
      {/* 1. Master Header Banner with Quick Actions */}
      <div className="bg-gradient-to-r from-amber-700 via-amber-600 to-orange-700 p-5 rounded-3xl text-white shadow-xl flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-white/15 flex items-center justify-center border-2 border-white/30 shadow-inner">
            <ClipboardList className="w-8 h-8 text-amber-200" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl sm:text-2xl font-black tracking-tight">سجلات الدرجات المدرسية الشاملة</h1>
              <span className="px-2.5 py-0.5 rounded-full text-[11px] font-black bg-white/20 text-white border border-white/30">
                الإصدار الثلاثي المتزامن
              </span>
            </div>
            <p className="text-amber-100 text-xs sm:text-sm font-bold mt-0.5">
              سجل اليومي والنشاط • سجل المدرس (المدرج) • سجل الإدارة العام • بطاقة درجات الطالب 360°
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {/* Cloud Pull Button */}
          <button
            type="button"
            onClick={handlePullCloudGrades}
            disabled={isPulling}
            className={`px-4 py-2.5 rounded-xl font-black text-xs shadow-md transition-all flex items-center gap-2 cursor-pointer ${
              isPulling 
                ? 'bg-amber-800 text-amber-200 cursor-not-allowed' 
                : 'bg-white text-slate-900 hover:bg-amber-50 active:scale-95'
            }`}
            title="سحب ومزامنة الدرجات المدخلة من قبل المدرسين عبر تطبيق الأندرويد"
          >
            <RefreshCw className={`w-4 h-4 text-amber-600 ${isPulling ? 'animate-spin' : ''}`} />
            <span>{isPulling ? 'جارٍ السحب...' : 'سحب درجات تطبيق الأستاذ ☁️'}</span>
          </button>

          {/* Add Subject Button */}
          <button
            type="button"
            onClick={() => setShowAddSubjectModal(true)}
            className="px-4 py-2.5 rounded-xl font-black text-xs bg-amber-500 hover:bg-amber-400 text-slate-950 transition-all flex items-center gap-1.5 shadow-md active:scale-95 cursor-pointer"
            title="إضافة مادة دراسية جديدة للسجل"
          >
            <Plus className="w-4 h-4" />
            <span>إضافة مادة</span>
          </button>
        </div>
      </div>

      {/* Cloud Pull Status Notice */}
      {pullMsg && (
        <div className={`p-3.5 rounded-2xl text-xs font-bold flex items-center gap-2 border shadow-xs animate-in fade-in duration-200 ${
          pullMsg.startsWith('✅') 
            ? 'bg-emerald-50 text-emerald-900 border-emerald-300' 
            : pullMsg.startsWith('❌') 
              ? 'bg-rose-50 text-rose-900 border-rose-300' 
              : 'bg-amber-50 text-amber-900 border-amber-300'
        }`}>
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>{pullMsg}</span>
        </div>
      )}

      {/* 2. The 3 Master Register Tabs */}
      <div className="bg-white p-2 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-2">
        <button
          type="button"
          onClick={() => setActiveTab('daily')}
          className={`flex-1 py-3 px-4 rounded-xl font-black text-xs sm:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer ${
            activeTab === 'daily'
              ? 'bg-gradient-to-r from-amber-600 to-orange-600 text-white shadow-md scale-101'
              : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
          }`}
        >
          <BookOpen className="w-4 h-4" />
          <span>1. سجل اليومي والنشاط</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('teacher')}
          className={`flex-1 py-3 px-4 rounded-xl font-black text-xs sm:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer ${
            activeTab === 'teacher'
              ? 'bg-gradient-to-r from-indigo-700 to-indigo-600 text-white shadow-md scale-101'
              : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
          }`}
        >
          <GraduationCap className="w-4 h-4" />
          <span>2. سجل المدرس (المدرج)</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('admin')}
          className={`flex-1 py-3 px-4 rounded-xl font-black text-xs sm:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer ${
            activeTab === 'admin'
              ? 'bg-gradient-to-r from-slate-900 to-slate-800 text-white shadow-md scale-101'
              : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
          }`}
        >
          <Building2 className="w-4 h-4 text-amber-400" />
          <span>3. سجل الإدارة العام</span>
        </button>
      </div>

      {/* 3. Class & Section & Search Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
          
          {/* Grade Selector */}
          <div>
            <label className="block font-bold text-slate-500 mb-1">الصف الدراسي:</label>
            <select 
              value={selectedGrade} 
              onChange={e => setSelectedGrade(e.target.value)} 
              className="w-full p-2.5 rounded-xl border border-slate-300 font-bold text-slate-900 outline-none focus:border-amber-500 bg-slate-50 text-xs shadow-2xs"
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
              className="w-full p-2.5 rounded-xl border border-slate-300 font-bold text-slate-900 outline-none focus:border-amber-500 bg-slate-50 text-xs shadow-2xs"
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
                placeholder="ابحث باسم الطالب أو رقم القيد..."
                className="w-full pr-9 pl-3 py-2 rounded-xl border border-slate-300 font-bold text-slate-900 outline-none focus:border-amber-500 bg-slate-50 text-xs shadow-2xs"
              />
            </div>
          </div>

        </div>

        {/* Quick Grade Pills */}
        <div className="flex flex-wrap items-center gap-1.5 pt-2 border-t border-slate-100">
          <button
            type="button"
            onClick={() => { setSelectedGrade('الكل'); setSelectedSection('الكل'); }}
            className={`px-3 py-1 rounded-xl text-xs font-black transition-all cursor-pointer border ${
              selectedGrade === 'الكل' && selectedSection === 'الكل'
                ? 'bg-amber-600 text-white border-amber-500 shadow-2xs'
                : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200'
            }`}
          >
            كافة الصفوف ({students.length} طالب)
          </button>
          {uniqueGrades.map(g => (
            <button
              key={g}
              type="button"
              onClick={() => setSelectedGrade(g)}
              className={`px-2.5 py-1 rounded-xl text-xs font-bold transition-all cursor-pointer border ${
                selectedGrade === g
                  ? 'bg-amber-100 text-amber-950 border-amber-300 font-black shadow-2xs'
                  : 'bg-white hover:bg-slate-100 text-slate-700 border-slate-200'
              }`}
            >
              {g}
            </button>
          ))}
        </div>
      </div>

      {/* 4. Active Register Component */}
      {activeTab === 'daily' && (
        <DailyGradeRegister
          students={filteredStudents}
          subjectsList={subjectList}
          onUpdateStudentMark={handleUpdateStudentMark}
          onSelectStudent={std => setSelectedStudentForProfile(std)}
        />
      )}

      {activeTab === 'teacher' && (
        <TeacherGraduatedRegister
          students={filteredStudents}
          subjectsList={subjectList}
          onUpdateStudentMark={handleUpdateStudentMark}
          onSelectStudent={std => setSelectedStudentForProfile(std)}
        />
      )}

      {activeTab === 'admin' && (
        <AdminMasterRegister
          students={filteredStudents}
          subjectsList={subjectList}
          onSelectStudent={std => setSelectedStudentForProfile(std)}
          selectedGrade={selectedGrade}
          selectedSection={selectedSection}
        />
      )}

      {/* 5. Student 360° Academic Transcript Modal */}
      {selectedStudentForProfile && (
        <StudentGradeProfileModal
          student={selectedStudentForProfile}
          isOpen={Boolean(selectedStudentForProfile)}
          onClose={() => setSelectedStudentForProfile(null)}
          subjectsList={subjectList}
          schoolName={config.schoolName || 'ثانوية المتميزين للبنين'}
        />
      )}

      {/* 6. Modal: Add New Subject */}
      {showAddSubjectModal && (
        <Portal>
          <div className="fixed inset-0 z-[80] bg-slate-900/80 backdrop-blur-xs flex items-center justify-center p-4 dir-rtl">
            <div className="bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border-2 border-amber-500 space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-slate-100">
                <h3 className="text-lg font-black text-slate-900">إضافة مادة دراسية جديدة</h3>
                <button
                  onClick={() => setShowAddSubjectModal(false)}
                  className="p-1 rounded-lg hover:bg-slate-100 text-slate-400"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 mb-1.5">اسم المادة الجديدة:</label>
                <input
                  type="text"
                  value={newSubjectName}
                  onChange={e => setNewSubjectName(e.target.value)}
                  placeholder="مثال: علم الأرض، الفرنسية..."
                  className="w-full p-3 rounded-xl border border-slate-300 font-bold text-slate-900 text-sm outline-none focus:border-amber-500"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddSubjectModal(false)}
                  className="px-4 py-2 rounded-xl text-slate-600 font-bold text-xs hover:bg-slate-100 cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  type="button"
                  onClick={handleAddSubject}
                  className="px-5 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-black text-xs shadow-md cursor-pointer"
                >
                  حفظ المادة
                </button>
              </div>
            </div>
          </div>
        </Portal>
      )}

    </div>
  );
};
