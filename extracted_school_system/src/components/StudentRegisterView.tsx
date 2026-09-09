import React, { useState } from 'react';
import { Student, AppConfig, StudentMark } from '../types';
import { parseStudentsFromRawInput, parseExcelFileForStudents } from '../utils/parser';
import { quickSyncStudentsToSupabase } from '../utils/syncService';
import {
  GraduationCap, 
  Search, 
  Filter, 
  Plus,
  UserPlus, 
  Printer,
  FileSpreadsheet,
  Mic,
  X,
  Users,
  Archive,
  Award,
  Sparkles,
  Split
} from 'lucide-react';

// Import our new sub-components
import { AddStudentModal } from './students/AddStudentModal';
import { PromotionModal } from './students/PromotionModal';
import { StudentRosterTable } from './students/StudentRosterTable';
import { StudentDetailModal } from './students/StudentDetailModal';
import { PrintReportModal } from './students/PrintReportModal';
import { ImportModal } from './students/ImportModal';
import { OcrModal } from './students/OcrModal';
import { SyncModal } from './students/SyncModal';
import { StudentTranscriptModal } from './StudentTranscriptModal';
import { SectionDividerModal } from './students/SectionDividerModal';

interface StudentRegisterViewProps {
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  config: AppConfig;
}

export const StudentRegisterView: React.FC<StudentRegisterViewProps> = ({
  students,
  setStudents,
  config
}) => {
  // Navigation Tabs: 'active' (مستمرون) vs 'archive' (أرشيف المتخرجين)
  const [activeTab, setActiveTab] = useState<'active' | 'archive'>('active');

  // Search & Filter States
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedGrade, setSelectedGrade] = useState('الكل');
  const [selectedStatus, setSelectedStatus] = useState('الكل');
  const [healthFilter, setHealthFilter] = useState('الكل');
  const [selectedSection, setSelectedSection] = useState('الكل');
  const [selectedSubject, setSelectedSubject] = useState('اللغة العربية');

  // Modals Visibility States
  const [showAddStudentModal, setShowAddStudentModal] = useState(false);
  const [showPromotionModal, setShowPromotionModal] = useState(false);
  const [showSectionDividerModal, setShowSectionDividerModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [showOcrModal, setShowOcrModal] = useState(false);
  const [showSyncModal, setShowSyncModal] = useState(false);
  const [showAuditModal, setShowAuditModal] = useState(false);
  const [showPrintRosterModal, setShowPrintRosterModal] = useState(false);
  const [showTeacherSyncModal, setShowTeacherSyncModal] = useState(false);

  // Data States for Modals
  const [selectedStudentForDetail, setSelectedStudentForDetail] = useState<Student | null>(null);
  const [selectedStudentForPrint, setSelectedStudentForPrint] = useState<Student | null>(null);
  const [newStudent, setNewStudent] = useState<Partial<Student>>({
    firstName: '', secondName: '', thirdName: '', fourthName: '', titleName: '',
    motherName: '', nationalCardNumber: '', currentGrade: 'الصف الأول', section: 'أ',
    recordNumber: '', status: 'مستمر', healthStatus: 'سليم', absencesCount: 0
  });
  const [importRawText, setImportRawText] = useState('');
  const [auditPendingList, setAuditPendingList] = useState<any[] | null>(null);
  const [ocrPreviewUrl, setOcrPreviewUrl] = useState<string | null>(null);
  const [ocrImageFile, setOcrImageFile] = useState<File | null>(null);
  const [isOcrScanning, setIsOcrScanning] = useState(false);
  const [ocrExtractedStudents, setOcrExtractedStudents] = useState<any[]>([]);
  const [syncPasscode, setSyncPasscode] = useState('');
  const [isSyncingCloud, setIsSyncingCloud] = useState(false);
  const [syncSealResult, setSyncSealResult] = useState<any>(null);
  const [selectedAuditStage, setSelectedAuditStage] = useState<any>('نصف السنة');
  const [auditPasscode, setAuditPasscode] = useState('');
  const [teacherSyncProgress, setTeacherSyncProgress] = useState(0);
  const [promotionReport, setPromotionReport] = useState<any>(null);

  // Helper: get or initialize a StudentMark object
  const getStudentMarkForSubject = (std: Student, subject: string): StudentMark => {
    const currentYear = std.registrationYear || '2024-2025';
    let mark = std.marksHistory.find(m => m.subject === subject && m.year === currentYear);
    return mark || {
      year: currentYear, subject,
      m1Daily: [0, 0, 0, 0, 0], m1Written: 0, m1MonthAvg: 0,
      m2Daily: [0, 0, 0, 0, 0], m2Written: 0, m2MonthAvg: 0,
      term1Avg: 0, midtermOral: [0], midtermWritten: 0, midtermFinalGrade: 0,
      m3Daily: [0, 0, 0, 0, 0], m3Written: 0, m3MonthAvg: 0,
      m4Daily: [0, 0, 0, 0, 0], m4Written: 0, m4MonthAvg: 0,
      term2Avg: 0, annualAverage: 0, finalWrittenD1: 0, finalWrittenD2: null,
      finalGrade: 0, total: 0
    };
  };

// Smart Arabic text normalizer for accurate search matching
function normalizeForSearch(str: string): string {
  if (!str) return '';
  return str
    .toLowerCase()
    .replace(/[أإآ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/ى/g, 'ي')
    .replace(/[\u064B-\u065F\u0670]/g, '') // remove tashkeel
    .replace(/ـ/g, '') // remove tatweel
    .replace(/\s+/g, ' ')
    .trim();
}

  // Logic: Handle Add Student
  const handleAddStudentSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleanGrade = (newStudent.currentGrade && newStudent.currentGrade !== 'الصف الأول') 
      ? newStudent.currentGrade 
      : (students[0]?.currentGrade || 'الأول المتوسط');

    const cleanFullName = [
      newStudent.firstName,
      newStudent.secondName,
      newStudent.thirdName,
      newStudent.fourthName,
      newStudent.titleName
    ].filter(Boolean).join(' ').trim();

    const studentToAdd: Student = {
      ...newStudent as Student,
      id: `std-${Date.now()}`,
      sequence: students.length + 1,
      currentGrade: cleanGrade,
      section: newStudent.section || 'أ',
      fullName: cleanFullName,
      status: 'مستمر',
      marksHistory: [],
      notesLog: []
    };

    const updated = [...students, studentToAdd];
    setStudents(updated);
    localStorage.setItem('diyala_school_students', JSON.stringify(updated));
    window.dispatchEvent(new Event('school_data_updated'));
    const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
    quickSyncStudentsToSupabase(schoolId, updated);

    setShowAddStudentModal(false);
    setNewStudent({
      firstName: '', secondName: '', thirdName: '', fourthName: '', titleName: '',
      motherName: '', nationalCardNumber: '', currentGrade: cleanGrade, section: 'أ',
      recordNumber: '', status: 'مستمر', healthStatus: 'سليم', absencesCount: 0
    });
    alert('تمت إضافة الطالب بنجاح وحفظه في سجل المدرسة وسجل الدرجات والسحابة! ✓');
  };

  // Logic: Real AI OCR Scanner
  const handleScanPaperSheet = async () => {
    if (!ocrPreviewUrl) return;
    setIsOcrScanning(true);
    try {
      const apiKey = localStorage.getItem('gemini_api_key') || '';
      const response = await fetch('/api/ocr-score-sheet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ imageBase64: ocrPreviewUrl, userApiKey: apiKey })
      });
      const result = await response.json();
      if (result.success) {
        setOcrExtractedStudents(result.data.students);
      }
    } catch (err) { alert('خطأ في الاتصال بالذكاء الاصطناعي'); }
    finally { setIsOcrScanning(false); }
  };

  const handleApplyOcrGrades = () => {
    // Merge logic...
    setShowOcrModal(false);
  };

  const handleExecutePromotion = (stage: 'متوسطة' | 'إعدادية' | 'ابتدائية', schoolType: 'صباحي' | 'مسائي') => {
    const getNextGrade = (curr: string): { next: string; isGrad: boolean } => {
      const g = curr || '';
      if (stage === 'متوسطة') {
        if (g.includes('الأول') || g.includes('1')) return { next: 'الصف الثاني', isGrad: false };
        if (g.includes('الثاني') || g.includes('2')) return { next: 'الصف الثالث', isGrad: false };
        if (g.includes('الثالث') || g.includes('3')) return { next: 'تخرج', isGrad: true };
      } else if (stage === 'إعدادية') {
        if (g.includes('الرابع') || g.includes('4')) return { next: 'الصف الخامس', isGrad: false };
        if (g.includes('الخامس') || g.includes('5')) return { next: 'الصف السادس', isGrad: false };
        if (g.includes('السادس') || g.includes('6')) return { next: 'تخرج', isGrad: true };
      } else if (stage === 'ابتدائية') {
        if (g.includes('الأول') || g.includes('1')) return { next: 'الصف الثاني', isGrad: false };
        if (g.includes('الثاني') || g.includes('2')) return { next: 'الصف الثالث', isGrad: false };
        if (g.includes('الثالث') || g.includes('3')) return { next: 'الصف الرابع', isGrad: false };
        if (g.includes('الرابع') || g.includes('4')) return { next: 'الصف الخامس', isGrad: false };
        if (g.includes('الخامس') || g.includes('5')) return { next: 'الصف السادس', isGrad: false };
        if (g.includes('السادس') || g.includes('6')) return { next: 'تخرج', isGrad: true };
      }
      return { next: curr, isGrad: false };
    };

    let countPromoted = 0;
    let countGraduated = 0;
    let countSeparated = 0;
    let countTransferred = 0;

    const updatedStudents = students.map(s => {
      if (!['مستمر', 'active'].includes(s.status)) return s;

      const hasFailingMarks = s.marksHistory && s.marksHistory.length > 0 && s.marksHistory.some(m => (m.finalGrade || 0) < 50);
      const isPassed = !hasFailingMarks && (s.currentResult === 'ناجح' || !s.previousYearResult?.includes('راسب'));

      if (isPassed) {
        const { next, isGrad } = getNextGrade(s.currentGrade);
        if (isGrad) {
          countGraduated++;
          return {
            ...s,
            status: 'متخرج' as const,
            promotionDestination: 'تخرج',
            previousYearResult: 'ناجح (تخرج)',
            currentResult: 'تخرج'
          };
        } else {
          countPromoted++;
          return {
            ...s,
            currentGrade: next,
            status: 'مستمر' as const,
            promotionDestination: `رُحّل إلى ${next}`,
            previousYearResult: 'ناجح',
            currentResult: 'مستمر'
          };
        }
      } else {
        // Failed
        if (schoolType === 'مسائي') {
          countSeparated++;
          return {
            ...s,
            status: 'مفصول' as const,
            promotionDestination: 'فصل',
            previousYearResult: 'راسب (فصل)',
            currentResult: 'مفصول'
          };
        } else {
          const wasPreviouslyFailing = (s.previousYearResult || '').includes('راسب') || (s.previousYearResult || '').includes('معيد');
          if (wasPreviouslyFailing) {
            countTransferred++;
            return {
              ...s,
              status: 'غادر المدرسة' as const,
              promotionDestination: 'نقل إلى المسائي',
              previousYearResult: 'راسب سنتين (نقل مسائي)',
              currentResult: 'نقل مسائي'
            };
          } else {
            return {
              ...s,
              status: 'مستمر' as const,
              promotionDestination: 'معيد في نفس الصف',
              previousYearResult: 'راسب (معيد)',
              currentResult: 'معيد'
            };
          }
        }
      }
    });

    setStudents(updatedStudents);
    localStorage.setItem('diyala_school_students', JSON.stringify(updatedStudents));
    window.dispatchEvent(new Event('school_data_updated'));
    const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
    quickSyncStudentsToSupabase(schoolId, updatedStudents);

    alert(`تم الترحيل وتطبيق القواعد الوزارية بنجاح:
- رُحّل إلى الصف الأعلى: ${countPromoted} طالب
- تخرج (أرشيف): ${countGraduated} طالب
- فصل (أرشيف): ${countSeparated} طالب
- نقل إلى المسائي (أرشيف): ${countTransferred} طالب`);
  };

  const handleUpdateStatus = (id: string, newStatus: Student['status']) => {
    const updated = students.map(s => {
      if (s.id === id) {
        return { ...s, status: newStatus };
      }
      return s;
    });
    setStudents(updated);
    localStorage.setItem('diyala_school_students', JSON.stringify(updated));
    window.dispatchEvent(new Event('school_data_updated'));
    const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
    quickSyncStudentsToSupabase(schoolId, updated);
  };

  const handleDeleteStudent = (id: string, name: string) => {
    if (confirm(`هل أنت متأكد من حذف الطالب [${name}] نهائياً من السجل؟`)) {
      const updated = students.filter(s => s.id !== id);
      setStudents(updated);
      localStorage.setItem('diyala_school_students', JSON.stringify(updated));
      window.dispatchEvent(new Event('school_data_updated'));
      const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
      quickSyncStudentsToSupabase(schoolId, updated);
    }
  };

  const handleRestoreStudent = (id: string) => {
    handleUpdateStatus(id, 'active');
  };

  // Filter logic
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);
  const [showTranscript, setShowTranscript] = useState(false);
  const [sortOption, setSortOption] = useState<'name' | 'record' | 'marks'>('name');

  const uniqueGrades = ['الكل', ...Array.from(new Set(students.map(s => s.currentGrade).filter(Boolean)))];
  const uniqueSections = ['الكل', ...Array.from(new Set(students.map(s => s.section).filter(Boolean)))];

  // Extract all distinct Grade + Section pairs for the top Section Cards
  const sectionCards = React.useMemo(() => {
    const map = new Map<string, { grade: string; section: string; count: number }>();
    students.forEach(s => {
      if (s.currentGrade && s.section) {
        const key = `${s.currentGrade} - شعبة ${s.section}`;
        const existing = map.get(key);
        if (existing) {
          existing.count++;
        } else {
          map.set(key, { grade: s.currentGrade, section: s.section, count: 1 });
        }
      }
    });
    return Array.from(map.entries()).map(([key, data]) => ({
      key,
      ...data
    }));
  }, [students]);

  // Filtered and Sorted Students with Smart Arabic Search
  const filteredStudents = React.useMemo(() => {
    const query = normalizeForSearch(searchQuery);
    const queryWords = query ? query.split(' ').filter(Boolean) : [];

    const list = students.filter(s => {
      const isContinuing = ['active', 'مستمر', 'muted'].includes(s.status);
      if (activeTab === 'active' && !isContinuing) return false;
      if (activeTab === 'archive' && isContinuing) return false;

      // When searching by query, search globally across the current active/archive tab
      if (queryWords.length > 0) {
        const full = normalizeForSearch(`${s.firstName} ${s.secondName || ''} ${s.thirdName || ''} ${s.fourthName || ''} ${s.titleName || ''} ${s.fullName || ''}`);
        const rec = (s.recordNumber || '').trim();
        const nat = (s.nationalCardNumber || '').trim();

        const matchesQuery = queryWords.every(w => full.includes(w)) || rec.includes(query) || nat.includes(query);
        return matchesQuery;
      }

      // If no query, filter by selected grade and section
      if (selectedGrade !== 'الكل' && s.currentGrade !== selectedGrade) return false;
      if (selectedSection !== 'الكل' && s.section !== selectedSection) return false;

      return true;
    });

    return list.sort((a, b) => {
      if (sortOption === 'record') {
        const rA = parseInt(a.recordNumber, 10) || 0;
        const rB = parseInt(b.recordNumber, 10) || 0;
        return rA - rB;
      }
      if (sortOption === 'marks') {
        const mA = a.marksHistory?.reduce((acc, m) => acc + (m.finalGrade || m.total || 0), 0) || 0;
        const mB = b.marksHistory?.reduce((acc, m) => acc + (m.finalGrade || m.total || 0), 0) || 0;
        return mB - mA;
      }
      const nameA = [a.firstName, a.secondName, a.thirdName, a.fourthName, a.titleName].filter(Boolean).join(' ').trim();
      const nameB = [b.firstName, b.secondName, b.thirdName, b.fourthName, b.titleName].filter(Boolean).join(' ').trim();
      return nameA.localeCompare(nameB, 'ar', { sensitivity: 'base' });
    });
  }, [students, activeTab, selectedGrade, selectedSection, searchQuery, sortOption]);

  const handleExcelUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const imported = await parseExcelFileForStudents(file, students.length + 1);
      if (imported.length > 0) {
        const updatedList = [...students, ...imported];
        setStudents(updatedList);
        
        // Auto Sync to Cloud immediately
        const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
        await quickSyncStudentsToSupabase(schoolId, updatedList);
        
        alert(`تم استيراد ${imported.length} طالب بنجاح ومزامنتهم فورياً مع السحابة (Supabase)!`);
        setShowImportModal(false);
      } else {
        alert('لم يتم العثور على سجلات صالحة في ملف الأكسل. يرجى التأكد من احتواء الملف على أعمدة الأسماء.');
      }
    } catch (err: any) {
      alert(`خطأ في معالجة ملف الأكسل: ${err.message}`);
    }
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setShowImportModal(false);
    setOcrImageFile(file);
    setOcrPreviewUrl(URL.createObjectURL(file));
    setShowOcrModal(true);
  };

  const handleImportRawText = async () => {
    if (!importRawText.trim()) {
      alert('يرجى لصق قائمة الأسماء أولاً.');
      return;
    }
    const imported = parseStudentsFromRawInput(importRawText, students.length + 1);
    if (imported.length > 0) {
      const updatedList = [...students, ...imported];
      setStudents(updatedList);
      
      const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
      await quickSyncStudentsToSupabase(schoolId, updatedList);
      
      alert(`تم استيراد ${imported.length} طالب بنجاح ومزامنتهم مع السحابة!`);
      setImportRawText('');
      setShowImportModal(false);
    } else {
      alert('لم يتم التعرف على بنية الأسماء. يرجى التأكد من وضع كل اسم في سطر منفصل.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6 dir-rtl">
      
      {/* Header with Add Student Button */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-4 bg-white p-5 rounded-2xl border-3 border-indigo-600 shadow-xl">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-2xl bg-indigo-600 text-white shadow-lg">
            <GraduationCap className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-black text-slate-900">سجل الطلاب الموحد</h2>
            <p className="text-xs text-slate-500 font-bold">إدارة بيانات الطلبة، الأرشفة، والترفيع السنوي</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setShowAddStudentModal(true)}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs transition-all shadow-lg cursor-pointer"
          >
            <UserPlus className="w-4 h-4" />
            <span>إضافة طالب جديد</span>
          </button>

          <button
            onClick={() => setShowSectionDividerModal(true)}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs transition-all shadow-lg cursor-pointer"
            title="تقسيم وتوزيع طلاب الصف آلياً على الشعب (أ، ب، ج، د)"
          >
            <Split className="w-4 h-4" />
            <span>تقسيم وتوزيع الشعب</span>
          </button>

          <button
            onClick={() => setShowPromotionModal(true)}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-900 font-black text-xs transition-all shadow-lg cursor-pointer"
          >
            <Award className="w-4 h-4" />
            <span>الترفيع والترحيل</span>
          </button>

          <button
            onClick={() => setShowImportModal(true)}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-900 text-white font-bold text-xs transition-all shadow-lg cursor-pointer"
          >
            <FileSpreadsheet className="w-4 h-4" />
            <span>استيراد بيانات</span>
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2">
        <button
          onClick={() => setActiveTab('active')}
          className={`flex-1 py-3 rounded-xl font-black text-sm flex items-center justify-center gap-2 transition-all cursor-pointer ${
            activeTab === 'active' ? 'bg-indigo-600 text-white shadow-lg' : 'bg-white text-slate-600 border border-slate-200'
          }`}
        >
          <Users className="w-5 h-5" />
          <span>الطلاب المستمرون ({students.filter(s => s.status === 'مستمر').length})</span>
        </button>
        <button
          onClick={() => setActiveTab('archive')}
          className={`flex-1 py-3 rounded-xl font-black text-sm flex items-center justify-center gap-2 transition-all cursor-pointer ${
            activeTab === 'archive' ? 'bg-amber-600 text-white shadow-lg' : 'bg-white text-slate-600 border border-slate-200'
          }`}
        >
          <Archive className="w-5 h-5" />
          <span>أرشيف الطلاب ({students.filter(s => s.status !== 'مستمر').length})</span>
        </button>
      </div>

      {/* Search and Filters */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
        <div className="flex items-center gap-3">
          <div className="relative flex-1">
            <Search className="w-5 h-5 absolute right-3 top-2.5 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="ابحث بالاسم الرباعي، اللقب، رقم القيد، أو البطاقة الموحدة..."
              className="w-full pr-10 pl-4 py-2.5 rounded-xl border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 outline-none font-bold text-sm"
            />
          </div>
          {(selectedGrade !== 'الكل' || selectedSection !== 'الكل' || searchQuery) && (
            <button 
              onClick={() => { setSelectedGrade('الكل'); setSelectedSection('الكل'); setSearchQuery(''); }}
              className="px-3 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-xl text-xs font-black flex items-center gap-1 cursor-pointer transition-colors"
            >
              <X className="w-4 h-4" />
              <span>إعادة ضبط</span>
            </button>
          )}
        </div>

        {/* Section Icons Grid (أيقونة لكل شعبة + أيقونة الكل) */}
        <div className="pt-3 border-t border-slate-100 space-y-2">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
            <div className="flex items-center gap-1.5 text-xs font-black text-slate-900">
              <Sparkles className="w-4 h-4 text-amber-500" />
              <span>توزيع الشعب الدراسية (اضغط على الشعبة للفرز المباشر):</span>
            </div>
            
            {/* Sorting Controls */}
            <div className="flex items-center gap-1.5 text-xs font-bold bg-slate-50 p-1 rounded-xl border border-slate-200">
              <span className="text-slate-500 text-[11px] px-1">الفرز:</span>
              <button
                onClick={() => setSortOption('name')}
                className={`px-2.5 py-1 rounded-lg transition-all cursor-pointer ${
                  sortOption === 'name' ? 'bg-indigo-600 text-white font-black shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                أبجدي (أ - ي)
              </button>
              <button
                onClick={() => setSortOption('record')}
                className={`px-2.5 py-1 rounded-lg transition-all cursor-pointer ${
                  sortOption === 'record' ? 'bg-indigo-600 text-white font-black shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                رقم القيد
              </button>
              <button
                onClick={() => setSortOption('marks')}
                className={`px-2.5 py-1 rounded-lg transition-all cursor-pointer ${
                  sortOption === 'marks' ? 'bg-indigo-600 text-white font-black shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                الدرجات
              </button>
            </div>
          </div>

          {/* Gallery of Section Badges */}
          <div className="flex flex-wrap items-center gap-2 pt-1">
            {/* 1. All Sections Badge */}
            <button
              onClick={() => { setSelectedGrade('الكل'); setSelectedSection('الكل'); }}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-xl font-black text-xs transition-all shadow-xs cursor-pointer border-2 ${
                selectedGrade === 'الكل' && selectedSection === 'الكل'
                  ? 'bg-gradient-to-r from-indigo-700 to-blue-700 text-white border-indigo-400 shadow-md scale-105'
                  : 'bg-slate-50 hover:bg-slate-100 text-slate-800 border-slate-200'
              }`}
            >
              <Users className="w-4 h-4 text-amber-400" />
              <span>الكل (كافة الشعب)</span>
              <span className={`px-2 py-0.5 rounded-full text-[11px] font-mono ${
                selectedGrade === 'الكل' && selectedSection === 'الكل' ? 'bg-white/20 text-white' : 'bg-indigo-100 text-indigo-900'
              }`}>
                {students.filter(s => s.status === 'مستمر' || activeTab === 'archive').length}
              </span>
            </button>

            {/* 2. Distinct Grade + Section Badges */}
            {sectionCards.map(sec => {
              const isSelected = selectedGrade === sec.grade && selectedSection === sec.section;
              return (
                <button
                  key={sec.key}
                  onClick={() => { setSelectedGrade(sec.grade); setSelectedSection(sec.section); }}
                  className={`flex items-center gap-2 px-3.5 py-2 rounded-xl font-black text-xs transition-all shadow-xs cursor-pointer border-2 ${
                    isSelected
                      ? 'bg-gradient-to-r from-emerald-600 to-teal-700 text-white border-emerald-400 shadow-md scale-105'
                      : 'bg-white hover:bg-slate-50 text-slate-800 border-slate-300'
                  }`}
                >
                  <GraduationCap className={`w-4 h-4 ${isSelected ? 'text-amber-300' : 'text-emerald-600'}`} />
                  <span>{sec.grade} ({sec.section})</span>
                  <span className={`px-2 py-0.5 rounded-full text-[11px] font-mono ${
                    isSelected ? 'bg-white/20 text-white' : 'bg-emerald-50 text-emerald-900 border border-emerald-200'
                  }`}>
                    {sec.count} طالب
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Main Table */}
      <StudentRosterTable
        activeTab={activeTab}
        filteredStudents={filteredStudents}
        onSelectDetail={setSelectedStudentForDetail}
        onSelectPrint={setSelectedStudentForPrint}
        onRestore={handleRestoreStudent}
        onUpdateStatus={handleUpdateStatus}
        onSelectTranscript={(s) => { setSelectedStudent(s); setShowTranscript(true); }}
        onUpdateStudent={(u) => {
          const updated = students.map(s => s.id === u.id ? u : s);
          setStudents(updated);
          localStorage.setItem('diyala_school_students', JSON.stringify(updated));
          window.dispatchEvent(new Event('school_data_updated'));
          const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
          quickSyncStudentsToSupabase(schoolId, updated);
        }}
        onDeleteStudent={handleDeleteStudent}
      />

      {/* Modals */}
      <AddStudentModal
        isOpen={showAddStudentModal}
        onClose={() => setShowAddStudentModal(false)}
        newStudent={newStudent}
        setNewStudent={setNewStudent}
        onSubmit={handleAddStudentSubmit}
      />

      <StudentDetailModal
        student={selectedStudentForDetail}
        onClose={() => setSelectedStudentForDetail(null)}
        onUpdate={(u) => {
          const updated = students.map(s => s.id === u.id ? u : s);
          setStudents(updated);
          localStorage.setItem('diyala_school_students', JSON.stringify(updated));
          setSelectedStudentForDetail(u);
          const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
          quickSyncStudentsToSupabase(schoolId, updated);
        }}
      />

      <PrintReportModal
        student={selectedStudentForPrint}
        config={config}
        onClose={() => setSelectedStudentForPrint(null)}
      />

      <OcrModal
        isOpen={showOcrModal}
        onClose={() => setShowOcrModal(false)}
        ocrPreviewUrl={ocrPreviewUrl}
        setOcrPreviewUrl={setOcrPreviewUrl}
        setOcrImageFile={setOcrImageFile}
        isOcrScanning={isOcrScanning}
        ocrExtractedStudents={ocrExtractedStudents}
        onScan={handleScanPaperSheet}
        onApply={handleApplyOcrGrades}
      />
      <ImportModal
        isOpen={showImportModal}
        onClose={() => setShowImportModal(false)}
        importRawText={importRawText}
        setImportRawText={setImportRawText}
        onExcelUpload={handleExcelUpload}
        onImageUpload={handleImageUpload}
        onImportText={handleImportRawText}
      />

      {/* Student Transcript Modal (v4.0 NEW) */}
      {selectedStudent && (
        <StudentTranscriptModal
          student={selectedStudent}
          isOpen={showTranscript}
          onClose={() => { setShowTranscript(false); setSelectedStudent(null); }}
        />
      )}

      {/* Promotion Modal */}
      <PromotionModal
        isOpen={showPromotionModal}
        onClose={() => setShowPromotionModal(false)}
        students={students}
        onExecutePromotion={handleExecutePromotion}
      />

      {/* Section Divider Modal */}
      <SectionDividerModal
        isOpen={showSectionDividerModal}
        onClose={() => setShowSectionDividerModal(false)}
        students={students}
        setStudents={setStudents}
      />
    </div>
  );
};
