import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { StaffMember, AppConfig, DayScheduleMap } from '../types';
import { 
  ShieldCheck, 
  Key, 
  Lock, 
  Unlock, 
  RefreshCw, 
  Printer, 
  Copy, 
  Check, 
  Search, 
  Sparkles, 
  Users, 
  BookOpen, 
  ArrowRight, 
  Plus, 
  Trash2, 
  CheckCircle2, 
  AlertCircle, 
  Eye, 
  EyeOff, 
  QrCode,
  Layers,
  Save,
  FileSpreadsheet,
  Shuffle,
  Edit3,
  X
} from 'lucide-react';
import { supabase, isSupabaseConfigured, getSupabaseKey } from '../utils/supabaseClient';
import { standardizeSubjectInput, STANDARD_APPROVED_SUBJECTS } from '../utils/subjectHelper';

export interface SubjectAssignmentRecord {
  id?: number;
  school_id: string;
  grade: string;
  section: string;
  subject: string;
  secret_code: string;
  is_locked: boolean;
  teacher_id?: string;
  teacher_name?: string;
  last_updated_at?: string;
}

interface TeacherAuthorityHubProps {
  staffList: StaffMember[];
  setStaffList?: React.Dispatch<React.SetStateAction<StaffMember[]>>;
  config: AppConfig;
  scheduleMap?: DayScheduleMap;
  onBackToMain: () => void;
}

// Available Grades and Sections for quick assignments
const COMMON_GRADES = [
  'الأول المتوسط',
  'الثاني المتوسط',
  'الثالث المتوسط',
  'الرابع الإعدادي',
  'الخامس الإعدادي',
  'السادس الإعدادي',
  'الأول الابتدائي',
  'الثاني الابتدائي',
  'الثالث الابتدائي',
  'الرابع الابتدائي',
  'الخامس الابتدائي',
  'السادس الابتدائي'
];

const COMMON_SECTIONS = ['أ', 'ب', 'ج', 'د', 'هـ'];

export const TeacherAuthorityHub: React.FC<TeacherAuthorityHubProps> = ({
  staffList,
  config,
  scheduleMap,
  onBackToMain
}) => {
  const activeSchoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'SCH-VCOL-6072';

  const [assignments, setAssignments] = useState<SubjectAssignmentRecord[]>(() => {
    try {
      const saved = localStorage.getItem(`diyala_subject_assignments_${activeSchoolId}`);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilterSubject, setSelectedFilterSubject] = useState<string>('all');
  const [isLoading, setIsLoading] = useState(false);
  const [isSavingCloud, setIsSavingCloud] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
  const [showPinMap, setShowPinMap] = useState<{ [key: string]: boolean }>({});
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [isPrintModalOpen, setIsPrintModalOpen] = useState(false);

  // Subject Edit & Add New Modal State
  const [editingSubjectModal, setEditingSubjectModal] = useState<{
    index: number;
    currentSubject: string;
    teacherName: string;
  } | null>(null);
  const [subjectInputValue, setSubjectInputValue] = useState('');

  // Extract any custom subjects outside standard list currently used
  const customSubjectsList = useMemo(() => {
    const custom = new Set<string>();
    assignments.forEach(a => {
      const std = standardizeSubjectInput(a.subject);
      if (!std.isApproved && a.subject && a.subject !== 'عام') {
        custom.add(a.subject);
      }
    });
    return Array.from(custom);
  }, [assignments]);

  // Quick helper to generate a 4 to 6 digit secure code
  const generateRandomPin = () => {
    return Math.floor(1000 + Math.random() * 9000).toString();
  };

  // Helper to extract assignments from scheduleMap and staffList
  const buildInitialAssignments = useCallback(() => {
    const list: SubjectAssignmentRecord[] = [];
    const seen = new Set<string>();

    // 1. Gather from scheduleMap if available
    if (scheduleMap) {
      Object.entries(scheduleMap).forEach(([_day, rows]) => {
        rows.forEach(row => {
          const grade = row.grade || '';
          const section = row.section || '';
          const lessons = [
            row.lessons?.lesson1,
            row.lessons?.lesson2,
            row.lessons?.lesson3,
            row.lessons?.lesson4,
            row.lessons?.lesson5,
            row.lessons?.lesson6
          ];

          lessons.forEach(l => {
            if (l && l.subject && l.teacherName && !l.isOff) {
              const key = `${grade}__${section}__${l.subject.trim()}`;
              if (!seen.has(key)) {
                seen.add(key);
                list.push({
                  school_id: activeSchoolId,
                  grade: grade.trim(),
                  section: section.trim(),
                  subject: l.subject.trim(),
                  secret_code: generateRandomPin(),
                  is_locked: false,
                  teacher_name: l.teacherName.trim()
                });
              }
            }
          });
        });
      });
    }

    // 2. Add remaining teachers from staffList if not yet mapped
    staffList.forEach(stf => {
      const isTeacher = ['مدرس', 'معلم'].includes(stf.jobTitle) || (stf.teachingQuota && stf.teachingQuota > 0);
      if (!isTeacher) return;

      const tName = stf.fullName || `${stf.firstName} ${stf.secondName}`.trim();
      const subject = stf.actualSubjectTaught || stf.specialization || 'عام';
      const classes = (stf.classesTaught && stf.classesTaught.length > 0) ? stf.classesTaught : ['الأول المتوسط'];

      classes.forEach(cls => {
        ['أ'].forEach(sec => {
          const key = `${cls}__${sec}__${subject}`;
          if (!seen.has(key)) {
            seen.add(key);
            list.push({
              school_id: activeSchoolId,
              grade: cls.trim(),
              section: sec.trim(),
              subject: subject.trim(),
              secret_code: generateRandomPin(),
              is_locked: false,
              teacher_id: stf.id,
              teacher_name: tName
            });
          }
        });
      });
    });

    return list;
  }, [scheduleMap, staffList, activeSchoolId]);

  // Fetch from Supabase on mount
  const fetchCloudAssignments = useCallback(async () => {
    setIsLoading(true);
    try {
      const { data, error } = await supabase
        .from('subject_assignments')
        .select('*')
        .eq('school_id', activeSchoolId);

      if (!error && data && data.length > 0) {
        // Link teachers to loaded cloud assignments
        const enriched = data.map((item: any) => {
          let foundName = item.teacher_name;
          if (!foundName) {
            const matchedStaff = staffList.find(s => {
              const sName = s.fullName || `${s.firstName} ${s.secondName}`;
              const sSub = s.actualSubjectTaught || s.specialization;
              return (sSub && sSub.trim() === item.subject.trim()) || (s.classesTaught && s.classesTaught.includes(item.grade));
            });
            if (matchedStaff) {
              foundName = matchedStaff.fullName || `${matchedStaff.firstName} ${matchedStaff.secondName}`;
            }
          }
          return {
            id: item.id,
            school_id: item.school_id || activeSchoolId,
            grade: item.grade,
            section: item.section,
            subject: item.subject,
            secret_code: item.secret_code,
            is_locked: item.is_locked ?? false,
            teacher_name: foundName || 'غير مسند',
            last_updated_at: item.last_updated_at
          };
        });

        setAssignments(enriched);
        localStorage.setItem(`diyala_subject_assignments_${activeSchoolId}`, JSON.stringify(enriched));
        setStatusMessage({ type: 'success', text: `تم جلب ${enriched.length} سجل معتمد من السحابة بنجاح ✓` });
      } else {
        // If empty in cloud, populate from local structure
        setAssignments(prev => {
          if (prev.length > 0) return prev;
          const initial = buildInitialAssignments();
          localStorage.setItem(`diyala_subject_assignments_${activeSchoolId}`, JSON.stringify(initial));
          return initial;
        });
      }
    } catch (err: any) {
      console.error('Error fetching subject assignments:', err);
      setStatusMessage({ type: 'error', text: 'تعذر الاتصال بالسحابة لجلب الأكواد. يتم العمل محلياً.' });
    } finally {
      setIsLoading(false);
    }
  }, [activeSchoolId, buildInitialAssignments, staffList]);

  useEffect(() => {
    fetchCloudAssignments();
  }, [fetchCloudAssignments]);

  // Save to LocalStorage whenever assignments change
  const updateAssignmentsState = (newAssignments: SubjectAssignmentRecord[]) => {
    setAssignments(newAssignments);
    localStorage.setItem(`diyala_subject_assignments_${activeSchoolId}`, JSON.stringify(newAssignments));
  };

  // 1-Click Sync to Supabase Cloud
  const handleSyncToCloud = async () => {
    if (assignments.length === 0) {
      alert('لا توجد سجلات لحفظها.');
      return;
    }

    setIsSavingCloud(true);
    setStatusMessage({ type: 'info', text: 'جاري رفع واعتماد أكواد المواد والشعب في السحابة...' });

    try {
      const recordsToUpsert = assignments.map(a => ({
        school_id: activeSchoolId,
        grade: a.grade.trim(),
        section: a.section.trim(),
        subject: a.subject.trim(),
        secret_code: a.secret_code.trim(),
        is_locked: a.is_locked,
        last_updated_at: new Date().toISOString()
      }));

      // Upsert into subject_assignments on unique (school_id, grade, section, subject)
      const { error } = await supabase
        .from('subject_assignments')
        .upsert(recordsToUpsert, {
          onConflict: 'school_id,grade,section,subject'
        });

      if (error) {
        console.error('Cloud upsert error:', error);
        throw error;
      }

      setStatusMessage({ 
        type: 'success', 
        text: `تم حفظ ومزامنة ${recordsToUpsert.length} كود اعتماد سري في السحابة بنجاح! التطبيق جاهز الآن للتحقق الأمني ✓` 
      });

      // Refetch to ensure IDs match
      fetchCloudAssignments();
    } catch (e: any) {
      console.error('Sync failed:', e);
      setStatusMessage({ type: 'error', text: `فشل الحفظ في السحابة: ${e.message || 'خطأ غير معروف'}` });
    } finally {
      setIsSavingCloud(false);
    }
  };

  // Toggle Lock for a subject/section
  const handleToggleLock = (index: number) => {
    const updated = [...assignments];
    updated[index].is_locked = !updated[index].is_locked;
    updateAssignmentsState(updated);
  };

  // Regenerate Code for a specific row
  const handleRegenerateCode = (index: number) => {
    const newCode = generateRandomPin();
    const updated = [...assignments];
    updated[index].secret_code = newCode;
    updateAssignmentsState(updated);
    setStatusMessage({ type: 'info', text: `تم توليد كود جديد (${newCode}) لـ ${updated[index].subject} - ${updated[index].grade}` });
  };

  // Regenerate All Missing Codes
  const handleGenerateAllMissing = () => {
    const updated = assignments.map(a => {
      if (!a.secret_code || a.secret_code.length < 4) {
        return { ...a, secret_code: generateRandomPin() };
      }
      return a;
    });
    updateAssignmentsState(updated);
    setStatusMessage({ type: 'success', text: 'تم استكمال وتوليد الأكواد لجميع المواد والشعب بنجاح ✓' });
  };

  // Change Secret Code manually
  const handleCodeChange = (index: number, val: string) => {
    const updated = [...assignments];
    updated[index].secret_code = val.trim();
    updateAssignmentsState(updated);
  };

  // Change Teacher Assignment
  const handleTeacherChange = (index: number, teacherName: string) => {
    const updated = [...assignments];
    updated[index].teacher_name = teacherName;
    updateAssignmentsState(updated);
  };

  // Add New Row
  const handleAddAssignment = () => {
    const defaultTeacher = staffList.find(s => ['مدرس', 'معلم'].includes(s.jobTitle));
    const newRow: SubjectAssignmentRecord = {
      school_id: activeSchoolId,
      grade: 'الأول المتوسط',
      section: 'أ',
      subject: defaultTeacher?.actualSubjectTaught || defaultTeacher?.specialization || 'الرياضيات',
      secret_code: generateRandomPin(),
      is_locked: false,
      teacher_name: defaultTeacher?.fullName || (defaultTeacher ? `${defaultTeacher.firstName} ${defaultTeacher.secondName}` : 'أستاذ جديد')
    };
    updateAssignmentsState([newRow, ...assignments]);
  };

  // Delete Row
  const handleDeleteRow = (index: number) => {
    if (!confirm('هل أنت متأكد من حذف هذا الإسناد والكود؟')) return;
    const updated = assignments.filter((_, i) => i !== index);
    updateAssignmentsState(updated);
  };

  // Copy Code to Clipboard
  const handleCopyCode = (code: string, idStr: string) => {
    navigator.clipboard.writeText(code);
    setCopiedKey(idStr);
    setTimeout(() => setCopiedKey(null), 2500);
  };

  // Grouped by Teacher for aggregated viewing & printing
  const groupedByTeacher = useMemo(() => {
    const map = new Map<string, SubjectAssignmentRecord[]>();
    assignments.forEach(a => {
      const name = a.teacher_name || 'غير محدد';
      if (!map.has(name)) map.set(name, []);
      map.get(name)!.push(a);
    });
    return Array.from(map.entries()).map(([teacherName, items]) => ({
      teacherName,
      items,
      subject: items[0]?.subject || 'عام',
      sectionsSummary: items.map(i => `${i.grade} (${i.section})`).join(' ، ')
    }));
  }, [assignments]);

  // Filtered List
  const filteredAssignments = useMemo(() => {
    return assignments.filter(a => {
      const matchesSearch = 
        (a.teacher_name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        a.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
        a.grade.toLowerCase().includes(searchQuery.toLowerCase()) ||
        a.secret_code.includes(searchQuery);
      
      const matchesSubject = selectedFilterSubject === 'all' || a.subject === selectedFilterSubject;
      return matchesSearch && matchesSubject;
    });
  }, [assignments, searchQuery, selectedFilterSubject]);

  // Unique Subjects List for filter
  const uniqueSubjects = useMemo(() => {
    return Array.from(new Set(assignments.map(a => a.subject))).filter(Boolean);
  }, [assignments]);

  // Statistics
  const totalAssignments = assignments.length;
  const lockedCount = assignments.filter(a => a.is_locked).length;
  const openCount = totalAssignments - lockedCount;
  const uniqueTeachersCount = new Set(assignments.map(a => a.teacher_name).filter(Boolean)).size;

  return (
    <div className="p-4 md:p-6 max-w-7xl mx-auto space-y-6 dir-rtl text-slate-800">
      
      {/* 1. Header Card with Navigation and Quick Summary */}
      <div className="bg-white border-4 border-indigo-500/30 rounded-3xl p-5 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 via-purple-600 to-blue-600 text-white flex items-center justify-center shadow-lg shadow-indigo-300 shrink-0">
            <Key className="w-8 h-8 text-amber-300" />
          </div>
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-indigo-50 border border-indigo-200 text-indigo-900 text-xs font-black mb-1">
              <ShieldCheck className="w-3.5 h-3.5 text-indigo-600" />
              <span>منظومة الحماية وتفويض الصلاحيات المدرسية 🛡️</span>
            </div>
            <h1 className="text-xl md:text-2xl font-black text-slate-900">
              أكواد المعلمين وتفويض الشعب والدرجات
            </h1>
            <p className="text-xs text-slate-600 font-bold mt-0.5">
              تحديد كود الرفع السري لكل مدرس ومادته وشعبه لمنع التداخل وقفل الشعب عند انتهاء الامتحانات.
            </p>
          </div>
        </div>

        <div className="flex items-center flex-wrap gap-2 shrink-0">
          {/* Print Security Badges Button */}
          <button
            onClick={() => setIsPrintModalOpen(true)}
            className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs md:text-sm shadow-md transition-all cursor-pointer active:scale-95"
            title="طباعة بطاقات الاعتماد الرسمية للمعلمين"
          >
            <Printer className="w-4 h-4" />
            <span>طباعة بطاقات الأكواد 🖨️</span>
          </button>

          {/* Sync to Cloud Button */}
          <button
            onClick={handleSyncToCloud}
            disabled={isSavingCloud}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-2xl text-white font-black text-xs md:text-sm shadow-lg transition-all cursor-pointer active:scale-95 ${
              isSavingCloud ? 'bg-slate-400 cursor-not-allowed' : 'bg-gradient-to-r from-indigo-600 to-blue-600 hover:from-indigo-700 hover:to-blue-700 shadow-indigo-200'
            }`}
            title="مزامنة واعتماد الأكواد في سحابة Supabase فوراً"
          >
            <RefreshCw className={`w-4 h-4 ${isSavingCloud ? 'animate-spin' : ''}`} />
            <span>{isSavingCloud ? 'جاري الرفع...' : 'حفظ ومزامنة السحابة ⚡'}</span>
          </button>

          {/* Back Button */}
          <button
            onClick={onBackToMain}
            className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-xs md:text-sm transition-all cursor-pointer active:scale-95 border border-slate-300"
          >
            <ArrowRight className="w-4 h-4" />
            <span>العودة للرئيسية ✕</span>
          </button>
        </div>
      </div>

      {/* Status Notification Toast */}
      {statusMessage && (
        <div className={`p-4 rounded-2xl flex items-center justify-between gap-3 shadow-md border ${
          statusMessage.type === 'success' 
            ? 'bg-emerald-50 border-emerald-300 text-emerald-900' 
            : statusMessage.type === 'error'
            ? 'bg-rose-50 border-rose-300 text-rose-900'
            : 'bg-blue-50 border-blue-300 text-blue-900'
        }`}>
          <div className="flex items-center gap-2.5 font-black text-sm">
            {statusMessage.type === 'success' ? <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" /> : <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />}
            <span>{statusMessage.text}</span>
          </div>
          <button 
            onClick={() => setStatusMessage(null)}
            className="text-xs font-black underline hover:opacity-80 cursor-pointer"
          >
            إغلاق ✕
          </button>
        </div>
      )}

      {/* 2. Key Metrics Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="bg-white border-2 border-indigo-100 p-3.5 rounded-2xl shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-700 flex items-center justify-center shrink-0">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xl font-black text-indigo-950">{uniqueTeachersCount}</div>
            <div className="text-[11px] font-bold text-slate-500">معلم ومدرس مفوّض</div>
          </div>
        </div>

        <div className="bg-white border-2 border-blue-100 p-3.5 rounded-2xl shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center shrink-0">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xl font-black text-blue-950">{totalAssignments}</div>
            <div className="text-[11px] font-bold text-slate-500">مادة وشعبة مسندة</div>
          </div>
        </div>

        <div className="bg-white border-2 border-emerald-100 p-3.5 rounded-2xl shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center shrink-0">
            <Unlock className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xl font-black text-emerald-950">{openCount}</div>
            <div className="text-[11px] font-bold text-slate-500">شعب مفتوحة للرفع</div>
          </div>
        </div>

        <div className="bg-white border-2 border-rose-100 p-3.5 rounded-2xl shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-700 flex items-center justify-center shrink-0">
            <Lock className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xl font-black text-rose-950">{lockedCount}</div>
            <div className="text-[11px] font-bold text-slate-500">شعب مقفلة أمنياً</div>
          </div>
        </div>
      </div>

      {/* 3. Filter & Control Toolbar */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Search */}
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute right-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            placeholder="بحث باسم الأستاذ، المادة، الصف، أو الكود..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-3 pr-10 py-2 text-xs md:text-sm font-bold bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white"
          />
        </div>

        {/* Subject Filter */}
        <div className="flex items-center gap-2 w-full md:w-auto">
          <span className="text-xs font-black text-slate-600 shrink-0">تصفية بالمادة:</span>
          <select
            value={selectedFilterSubject}
            onChange={(e) => setSelectedFilterSubject(e.target.value)}
            className="py-2 px-3 text-xs md:text-sm font-bold bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">كل المواد ({uniqueSubjects.length})</option>
            {uniqueSubjects.map(sub => (
              <option key={sub} value={sub}>{sub}</option>
            ))}
          </select>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2 w-full md:w-auto justify-end">
          <button
            onClick={handleGenerateAllMissing}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-900 border border-purple-200 text-xs font-black transition-all cursor-pointer active:scale-95"
            title="توليد أكواد سرية جديدة لمن ليس لديه كود"
          >
            <Shuffle className="w-3.5 h-3.5 text-purple-600" />
            <span>توليد للأكواد الفارغة 🎲</span>
          </button>

          <button
            onClick={handleAddAssignment}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-black shadow-md transition-all cursor-pointer active:scale-95"
          >
            <Plus className="w-4 h-4" />
            <span>إضافة مادة وشعبة جديدة +</span>
          </button>
        </div>
      </div>

      {/* 4. MAIN TABLE (The 4 Clean Columns Requested by User) */}
      <div className="bg-white border-2 border-slate-200 rounded-3xl shadow-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right border-collapse">
            <thead>
              <tr className="bg-slate-900 text-white text-xs md:text-sm">
                <th className="p-4 font-black border-b border-slate-800 w-12 text-center">#</th>
                <th className="p-4 font-black border-b border-slate-800 min-w-[200px]">1. اسم الأستاذ</th>
                <th className="p-4 font-black border-b border-slate-800 min-w-[160px]">2. المادة التي يدرسها</th>
                <th className="p-4 font-black border-b border-slate-800 min-w-[220px]">3. الصفوف والشعب المخصصة</th>
                <th className="p-4 font-black border-b border-slate-800 min-w-[240px]">4. كود رفع الدرجات للشعب (PIN)</th>
                <th className="p-4 font-black border-b border-slate-800 w-36 text-center">الحالة والإجراءات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-xs md:text-sm">
              {filteredAssignments.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-12 text-center text-slate-500 font-bold">
                    لا توجد سجلات مطابقة للبحث أو التصفية الحالية.
                  </td>
                </tr>
              ) : (
                filteredAssignments.map((row, idx) => {
                  const isPinVisible = !!showPinMap[`${row.grade}-${row.section}-${row.subject}`];
                  const rowIdStr = `${row.grade}-${row.section}-${row.subject}`;
                  const isCopied = copiedKey === rowIdStr;

                  return (
                    <tr 
                      key={`${row.grade}-${row.section}-${row.subject}-${idx}`}
                      className={`transition-colors hover:bg-indigo-50/40 ${row.is_locked ? 'bg-rose-50/30' : ''}`}
                    >
                      {/* # Index */}
                      <td className="p-4 text-center font-black text-slate-400">
                        {idx + 1}
                      </td>

                      {/* 1. اسم الأستاذ */}
                      <td className="p-4">
                        <div className="flex items-center gap-2.5">
                          <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 font-black text-xs flex items-center justify-center shrink-0 border border-indigo-200">
                            {(row.teacher_name || '؟').charAt(0)}
                          </div>
                          <div className="flex-1">
                            <select
                              value={row.teacher_name || ''}
                              onChange={(e) => handleTeacherChange(idx, e.target.value)}
                              className="font-black text-slate-900 bg-transparent hover:bg-slate-100 p-1 rounded-lg border border-transparent hover:border-slate-300 focus:outline-none focus:bg-white focus:border-indigo-500 w-full"
                            >
                              <option value="غير مسند">-- اختر أستاذ المادة --</option>
                              {staffList.map(stf => {
                                const fullName = stf.fullName || `${stf.firstName} ${stf.secondName}`;
                                return (
                                  <option key={stf.id} value={fullName}>
                                    {fullName} ({stf.specialization || stf.jobTitle})
                                  </option>
                                );
                              })}
                            </select>
                            <div className="text-[10px] text-slate-500 font-bold px-1">
                              ملاك المدرسة المعتمد
                            </div>
                          </div>
                        </div>
                      </td>

                      {/* 2. المادة التي يدرسها */}
                      <td className="p-4">
                        <div className="flex items-center gap-1.5">
                          <select
                            value={row.subject}
                            onChange={(e) => {
                              const val = e.target.value;
                              if (val === '__custom_new__') {
                                setEditingSubjectModal({
                                  index: idx,
                                  currentSubject: row.subject,
                                  teacherName: row.teacher_name || ''
                                });
                                setSubjectInputValue(row.subject);
                              } else {
                                const updated = [...assignments];
                                updated[idx].subject = val;
                                updateAssignmentsState(updated);
                              }
                            }}
                            className="font-black text-slate-900 bg-amber-50/80 hover:bg-amber-100 border border-amber-300 rounded-xl px-2.5 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-amber-500 cursor-pointer max-w-[170px]"
                            title="تعديل المادة أو اختيار مادة معتمدة أو مضافة"
                          >
                            <optgroup label="الدروس المعتمدة المثبتة">
                              {STANDARD_APPROVED_SUBJECTS.map(sub => (
                                <option key={sub} value={sub}>{sub}</option>
                              ))}
                            </optgroup>
                            {customSubjectsList.filter(s => !STANDARD_APPROVED_SUBJECTS.includes(s)).length > 0 && (
                              <optgroup label="المواد الجديدة المستحدثة">
                                {customSubjectsList.filter(s => !STANDARD_APPROVED_SUBJECTS.includes(s)).map(sub => (
                                  <option key={sub} value={sub}>{sub} ✨</option>
                                ))}
                              </optgroup>
                            )}
                            <option value="__custom_new__">➕ إضافة / كتابة مادة جديدة...</option>
                          </select>

                          <button
                            type="button"
                            onClick={() => {
                              setEditingSubjectModal({
                                index: idx,
                                currentSubject: row.subject,
                                teacherName: row.teacher_name || ''
                              });
                              setSubjectInputValue(row.subject);
                            }}
                            title="تعديل أو كتابة اسم مادة جديدة"
                            className="p-1.5 rounded-lg text-amber-800 hover:bg-amber-200/70 transition-colors cursor-pointer"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>

                      {/* 3. الصفوف والشعب المخصصة */}
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          {/* Grade Selector */}
                          <select
                            value={row.grade}
                            onChange={(e) => {
                              const updated = [...assignments];
                              updated[idx].grade = e.target.value;
                              updateAssignmentsState(updated);
                            }}
                            className="bg-slate-50 border border-slate-300 text-slate-800 text-xs font-bold rounded-lg px-2 py-1.5 focus:ring-1 focus:ring-indigo-500"
                          >
                            {COMMON_GRADES.map(g => (
                              <option key={g} value={g}>{g}</option>
                            ))}
                          </select>

                          {/* Section Selector */}
                          <select
                            value={row.section}
                            onChange={(e) => {
                              const updated = [...assignments];
                              updated[idx].section = e.target.value;
                              updateAssignmentsState(updated);
                            }}
                            className="bg-slate-50 border border-slate-300 text-slate-800 text-xs font-black rounded-lg px-2 py-1.5 focus:ring-1 focus:ring-indigo-500"
                          >
                            {COMMON_SECTIONS.map(s => (
                              <option key={s} value={s}>شعبة ({s})</option>
                            ))}
                          </select>
                        </div>
                      </td>

                      {/* 4. كود رفع الدرجات للشعب (PIN) */}
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          <div className="relative flex-1">
                            <input
                              type={isPinVisible ? 'text' : 'password'}
                              value={row.secret_code}
                              onChange={(e) => handleCodeChange(idx, e.target.value)}
                              maxLength={8}
                              className="w-full font-mono text-center tracking-widest text-sm font-black py-1.5 px-3 rounded-xl bg-slate-900 text-amber-400 border border-slate-700 shadow-inner focus:outline-none focus:ring-2 focus:ring-amber-400"
                              placeholder="4-6 أرقام"
                            />
                          </div>

                          {/* Show/Hide PIN Button */}
                          <button
                            type="button"
                            onClick={() => setShowPinMap(prev => ({ ...prev, [rowIdStr]: !prev[rowIdStr] }))}
                            className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 cursor-pointer"
                            title={isPinVisible ? 'إخفاء الرمز' : 'إظهار الرمز'}
                          >
                            {isPinVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                          </button>

                          {/* Copy PIN */}
                          <button
                            type="button"
                            onClick={() => handleCopyCode(row.secret_code, rowIdStr)}
                            className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 cursor-pointer"
                            title="نسخ الكود"
                          >
                            {isCopied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
                          </button>

                          {/* Regenerate Random PIN */}
                          <button
                            type="button"
                            onClick={() => handleRegenerateCode(idx)}
                            className="p-1.5 rounded-lg bg-purple-50 hover:bg-purple-100 text-purple-700 border border-purple-200 cursor-pointer"
                            title="توليد كود عشوائي جديد"
                          >
                            <Shuffle className="w-4 h-4" />
                          </button>
                        </div>
                      </td>

                      {/* الحالة والإجراءات */}
                      <td className="p-4 text-center">
                        <div className="flex items-center justify-center gap-2">
                          {/* Toggle Lock */}
                          <button
                            onClick={() => handleToggleLock(idx)}
                            className={`p-2 rounded-xl text-xs font-black flex items-center gap-1 transition-all cursor-pointer ${
                              row.is_locked 
                                ? 'bg-rose-100 text-rose-800 hover:bg-rose-200 border border-rose-300' 
                                : 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200 border border-emerald-300'
                            }`}
                            title={row.is_locked ? 'الشعبة مقفلة أمنياً من الرفع - انقر للفتح' : 'الشعبة مفتوحة للرفع - انقر للقفل'}
                          >
                            {row.is_locked ? <Lock className="w-4 h-4 text-rose-600" /> : <Unlock className="w-4 h-4 text-emerald-600" />}
                            <span>{row.is_locked ? 'مقفل' : 'مفتوح'}</span>
                          </button>

                          {/* Delete */}
                          <button
                            onClick={() => handleDeleteRow(idx)}
                            className="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-all cursor-pointer"
                            title="حذف هذا الإسناد"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 5. PRINT MODAL (Official Teacher Security Passes / Slips) */}
      {isPrintModalOpen && (
        <div className="fixed inset-0 bg-slate-900/70 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-4xl w-full p-6 space-y-6 shadow-2xl border-4 border-indigo-500 max-h-[90vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b pb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-indigo-100 text-indigo-700 flex items-center justify-center font-black">
                  <Printer className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-lg font-black text-slate-900">
                    طباعة بطاقات الاعتماد السرية للمعلمين (Official Security Passes)
                  </h3>
                  <p className="text-xs text-slate-600 font-bold">
                    بطاقات مقسمة جاهزة للطباعة والتسليم اليدوي لكل معلم متضمنة رمزه السري ومواده المخصصة.
                  </p>
                </div>
              </div>
              <button
                onClick={() => setIsPrintModalOpen(false)}
                className="text-slate-400 hover:text-slate-700 font-black text-xl p-2 cursor-pointer"
              >
                ✕
              </button>
            </div>

            {/* Printable Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 print:grid-cols-2" id="printable-badges">
              {groupedByTeacher.map((gt, i) => (
                <div 
                  key={i}
                  className="bg-white border-2 border-dashed border-slate-300 p-4 rounded-2xl relative shadow-sm hover:border-indigo-400 transition-all"
                >
                  <div className="flex items-start justify-between gap-3 border-b border-slate-200 pb-2 mb-2">
                    <div>
                      <div className="text-[10px] font-black text-indigo-700">جمهورية العراق - وزارة التربية</div>
                      <div className="text-xs font-black text-slate-800">{config.schoolName || 'المدرسة النموذجية'}</div>
                      <div className="text-sm font-black text-slate-900 mt-1">الأستاذ: {gt.teacherName}</div>
                    </div>
                    <div className="text-left shrink-0">
                      <div className="text-[9px] font-bold text-slate-400">معرف المدرسة:</div>
                      <div className="font-mono text-[10px] font-black text-indigo-900">{activeSchoolId}</div>
                    </div>
                  </div>

                  {/* Badges Info */}
                  <div className="space-y-1.5 text-xs font-bold text-slate-700">
                    <div className="flex justify-between">
                      <span className="text-slate-500">المادة المسندة:</span>
                      <span className="font-black text-slate-900">{gt.subject}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">الصفوف والشعب:</span>
                      <span className="font-black text-slate-900">{gt.sectionsSummary}</span>
                    </div>
                  </div>

                  {/* Secret Codes Box */}
                  <div className="mt-3 p-2.5 rounded-xl bg-slate-900 text-amber-400 flex items-center justify-between">
                    <div>
                      <div className="text-[10px] text-slate-400 font-bold">كود رفع الدرجات السري (PIN):</div>
                      <div className="font-mono text-base font-black tracking-widest">
                        {gt.items.map(it => it.secret_code).join(' | ')}
                      </div>
                    </div>
                    <Key className="w-5 h-5 text-amber-400" />
                  </div>

                  <div className="mt-2 text-[9px] text-slate-400 text-center font-bold">
                    ⚠️ هذا الرمز شخصي وسري ومخصص لاعتماد ورفع درجات هذه الشعب فقط.
                  </div>
                </div>
              ))}
            </div>

            {/* Modal Actions */}
            <div className="flex justify-end gap-3 pt-4 border-t">
              <button
                onClick={() => window.print()}
                className="flex items-center gap-2 px-6 py-2.5 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs md:text-sm shadow-lg cursor-pointer"
              >
                <Printer className="w-4 h-4" />
                <span>إرسال إلى الطابعة الآن 🖨️</span>
              </button>
              <button
                onClick={() => setIsPrintModalOpen(false)}
                className="px-5 py-2.5 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-xs cursor-pointer"
              >
                إغلاق النافذة
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Edit or Add New Subject */}
      {editingSubjectModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border-2 border-indigo-200 p-6 max-w-md w-full shadow-2xl space-y-4 text-right">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <div className="flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-black text-slate-900">
                  تعديل المادة أو إضافة مادة جديدة 📚
                </h3>
              </div>
              <button
                onClick={() => setEditingSubjectModal(null)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-500 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="text-xs text-slate-600 font-bold bg-indigo-50/60 p-2.5 rounded-xl border border-indigo-100">
              الأستاذ: <strong className="text-indigo-950">{editingSubjectModal.teacherName || 'أستاذ المادة'}</strong>
            </div>

            <div className="space-y-2">
              <label className="block text-xs font-black text-slate-800">
                اسم المادة الدراسية:
              </label>
              <input
                type="text"
                list="approved-subjects-list"
                value={subjectInputValue}
                onChange={e => setSubjectInputValue(e.target.value)}
                placeholder="اكتب اسم المادة (مثال: التربية الإسلامية، علم الأرض، ذكاء اصطناعي...)"
                className="w-full p-2.5 rounded-xl border-2 border-slate-300 font-black text-sm text-slate-900 focus:border-indigo-600 focus:outline-none"
                autoFocus
              />
              <datalist id="approved-subjects-list">
                {STANDARD_APPROVED_SUBJECTS.map(sub => (
                  <option key={sub} value={sub} />
                ))}
              </datalist>
            </div>

            {/* Live Normalization / Feedback Badge */}
            {subjectInputValue.trim() && (() => {
              const res = standardizeSubjectInput(subjectInputValue);
              return res.isApproved ? (
                <div className="p-3 rounded-2xl bg-emerald-50 border border-emerald-300 text-emerald-900 text-xs font-bold flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>
                    مادة معتمدة وزارياً: تم الضبط التلقائي لإملاء الدروس المعتمدة: <strong>({res.standardized})</strong> ✓
                  </span>
                </div>
              ) : (
                <div className="p-3 rounded-2xl bg-purple-50 border border-purple-300 text-purple-900 text-xs font-bold flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-purple-600 shrink-0" />
                  <span>
                    مادة دراسية جديدة (خارج الدروس المعتمدة): سيتم إضافتها وتثبيتها كما كُتبت: <strong>({res.standardized})</strong> ✨
                  </span>
                </div>
              );
            })()}

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
              <button
                type="button"
                onClick={() => setEditingSubjectModal(null)}
                className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-xs cursor-pointer"
              >
                إلغاء
              </button>
              <button
                type="button"
                onClick={() => {
                  if (!subjectInputValue.trim()) return;
                  const res = standardizeSubjectInput(subjectInputValue);
                  const updated = [...assignments];
                  if (updated[editingSubjectModal.index]) {
                    updated[editingSubjectModal.index].subject = res.standardized;
                    updateAssignmentsState(updated);
                  }
                  setEditingSubjectModal(null);
                }}
                className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs shadow-md cursor-pointer flex items-center gap-1.5"
              >
                <Check className="w-4 h-4" />
                <span>اعتماد وحفظ المادة ✓</span>
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
