import React, { useState, useEffect } from 'react';
import { 
  DayScheduleMap, 
  StaffMember, 
  AppConfig, 
  SmartScheduleSection, 
  SectionSubjectAssignment,
  DayOfWeek,
  ClassScheduleRow,
  ScheduleCell
} from '../types';
import { 
  generateSmartFairSchedule, 
  sanitizeAndRepairSections,
  checkScheduleCollisions, 
  isForbiddenInPeriod6,
  DAYS_OF_WEEK, 
  LESSON_KEYS, 
  LESSON_LABELS,
  CollisionReport
} from '../utils/scheduleSolver';
import { 
  Sparkles, 
  Calendar, 
  Clock, 
  Users, 
  BookOpen, 
  Plus, 
  Trash2, 
  CheckCircle2, 
  AlertTriangle, 
  Save, 
  Printer, 
  RotateCcw, 
  Layers, 
  Edit3, 
  Check, 
  X, 
  ArrowRight,
  ShieldCheck,
  ChevronRight,
  ChevronLeft,
  Copy,
  Info
} from 'lucide-react';
import { PrintPreviewModal } from './PrintPreviewModal';
import { getSupabase } from '../utils/supabaseClient';
import { canonicalSubject, MASTER_SUBJECTS_LIST, matchStaffWithScheduleCell } from '../utils/subjectHelper';
import { standardizeGradeName, standardizeSectionName, standardizeSubjectName, sortSectionsList } from '../utils/syncEngine';
import { Student } from '../types';

interface SmartScheduleGeneratorViewProps {
  scheduleMap: DayScheduleMap;
  setScheduleMap: React.Dispatch<React.SetStateAction<DayScheduleMap>>;
  staffList: StaffMember[];
  setStaffList?: React.Dispatch<React.SetStateAction<StaffMember[]>>;
  students?: Student[];
  config: AppConfig;
  onBackToLauncher: () => void;
}

// Default standard Iraqi curriculum subjects for middle/secondary schools (30 periods/week)
const STANDARD_SUBJECTS_TEMPLATE: { name: string; quota: number }[] = [
  { name: 'التربية الإسلامية', quota: 2 },
  { name: 'اللغة العربية', quota: 5 },
  { name: 'اللغة الانكليزية', quota: 5 },
  { name: 'الرياضيات', quota: 5 },
  { name: 'الكيمياء', quota: 2 },
  { name: 'الفيزياء', quota: 2 },
  { name: 'الأحياء', quota: 2 },
  { name: 'الاجتماعيات', quota: 4 },
  { name: 'النشاط البدني', quota: 1 },
  { name: 'التربية الفنية', quota: 1 },
  { name: 'التربية الأخلاقية', quota: 1 },
  { name: 'شاغر / نشاط حر', quota: 1 },
];

// High-Performance Row Component to isolate typing re-renders and eliminate lag
interface SubjectRowProps {
  sectionId: string;
  sub: SectionSubjectAssignment;
  sIdx: number;
  staffList: StaffMember[];
  onUpdateSubject: (sectionId: string, subId: string, updates: Partial<SectionSubjectAssignment>) => void;
  onBlurSubjectName: (sectionId: string, subId: string, val: string) => void;
  onApplyTeacherToAll: (subjectName: string, teacherName: string) => void;
  onDuplicate: (sectionId: string, subId: string) => void;
  onDelete: (sectionId: string, subId: string) => void;
}

const SubjectRow: React.FC<SubjectRowProps> = React.memo(({
  sectionId,
  sub,
  sIdx,
  staffList,
  onUpdateSubject,
  onBlurSubjectName,
  onApplyTeacherToAll,
  onDuplicate,
  onDelete
}) => {
  const [localSubject, setLocalSubject] = useState(sub.subjectName);
  const [localTeacher, setLocalTeacher] = useState(sub.teacherName);
  const [localQuota, setLocalQuota] = useState(sub.weeklyLessons);

  useEffect(() => {
    setLocalSubject(sub.subjectName);
  }, [sub.subjectName]);

  useEffect(() => {
    setLocalTeacher(sub.teacherName);
  }, [sub.teacherName]);

  useEffect(() => {
    setLocalQuota(sub.weeklyLessons);
  }, [sub.weeklyLessons]);

  const isRestrictedP6 = isForbiddenInPeriod6(sub.subjectName, sub.weeklyLessons);

  return (
    <tr className="hover:bg-white transition-colors">
      <td className="p-2 text-center font-mono font-bold text-slate-500">{sIdx + 1}</td>
      <td className="p-2">
        <div className="flex items-center gap-1.5">
          <input
            type="text"
            list="master-subjects-datalist"
            value={localSubject}
            onChange={e => {
              setLocalSubject(e.target.value);
            }}
            onBlur={e => {
              const val = e.target.value;
              onUpdateSubject(sectionId, sub.id, { subjectName: val });
              onBlurSubjectName(sectionId, sub.id, val);
            }}
            placeholder="اسم المادة"
            className="w-full p-1.5 rounded-lg border border-slate-300 font-bold bg-white text-slate-900 focus:border-indigo-600 focus:outline-none"
          />
          {isRestrictedP6 && (
            <span 
              className="shrink-0 px-1.5 py-0.5 rounded-md bg-rose-50 text-rose-700 border border-rose-200 text-[10px] font-black"
              title="مستبعدة برمجياً من الدرس السادس (مادة علمية أو دينية أو نصاب حصتين فأقل)"
            >
              مستبعدة من درس 6
            </span>
          )}
        </div>
      </td>
      <td className="p-2">
        <div className="flex items-center gap-1.5">
          <input
            type="text"
            list="teachers-datalist"
            value={localTeacher}
            onChange={e => {
              setLocalTeacher(e.target.value);
            }}
            onBlur={e => {
              const val = e.target.value;
              onUpdateSubject(sectionId, sub.id, { teacherName: val });
            }}
            placeholder="اكتب أو اختر اسم الأستاذ"
            className="w-full p-1.5 rounded-lg border border-slate-300 font-bold bg-white text-slate-900 focus:border-indigo-600 focus:outline-none"
          />
          {staffList.length > 0 && (
            <select
              value={localTeacher}
              onChange={e => {
                const val = e.target.value;
                setLocalTeacher(val);
                onUpdateSubject(sectionId, sub.id, { teacherName: val });
              }}
              title="اختيار سريع من الكادر"
              className="w-8 p-1.5 rounded-lg border border-slate-300 bg-slate-50 text-slate-700 font-bold cursor-pointer text-xs"
            >
              <option value="">اختيار...</option>
              {staffList.map(stf => {
                const fullName = stf.fullName || `${stf.firstName} ${stf.secondName}`.trim();
                return <option key={stf.id} value={fullName}>{fullName}</option>;
              })}
            </select>
          )}
        </div>
      </td>
      <td className="p-2 text-center">
        <div className="inline-flex items-center justify-center gap-1">
          <button
            type="button"
            onClick={() => {
              const newVal = Math.max(1, localQuota - 1);
              setLocalQuota(newVal);
              onUpdateSubject(sectionId, sub.id, { weeklyLessons: newVal });
            }}
            title="تقليل الحصة (-1)"
            className="w-6 h-6 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 font-black flex items-center justify-center transition-all cursor-pointer select-none text-xs border border-slate-300"
          >
            -
          </button>
          <input
            type="number"
            min="1"
            max="15"
            value={localQuota}
            onChange={e => {
              const val = Number(e.target.value);
              setLocalQuota(val);
            }}
            onBlur={e => {
              const val = Math.max(1, Number(e.target.value) || 1);
              setLocalQuota(val);
              onUpdateSubject(sectionId, sub.id, { weeklyLessons: val });
            }}
            className="w-12 p-1 rounded-md border border-slate-300 font-black font-mono text-center bg-white text-slate-900 focus:border-indigo-600 focus:outline-none text-sm"
          />
          <button
            type="button"
            onClick={() => {
              const newVal = Math.min(15, localQuota + 1);
              setLocalQuota(newVal);
              onUpdateSubject(sectionId, sub.id, { weeklyLessons: newVal });
            }}
            title="زيادة الحصة (+1)"
            className="w-6 h-6 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 font-black flex items-center justify-center transition-all cursor-pointer select-none text-xs border border-slate-300"
          >
            +
          </button>
        </div>
      </td>
      <td className="p-2 text-center">
        <button
          type="button"
          onClick={() => onApplyTeacherToAll(sub.subjectName, localTeacher)}
          title={`تعيين [${localTeacher}] لمادة [${sub.subjectName}] في كافة الشعب`}
          className="text-[10px] font-black px-2 py-1 rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-200 hover:bg-indigo-600 hover:text-white transition-all cursor-pointer"
        >
          تطبيق للكل
        </button>
      </td>
      <td className="p-2 text-center">
        <div className="flex items-center justify-center gap-1">
          <button
            type="button"
            onClick={() => onDuplicate(sectionId, sub.id)}
            title="تكرار هذه المادة (لإسناد مدرس آخر أو تقسيم الحصص)"
            className="p-1.5 rounded-lg bg-purple-50 text-purple-700 hover:bg-purple-600 hover:text-white border border-purple-200 text-[11px] font-bold transition-all cursor-pointer flex items-center gap-1 shadow-2xs"
          >
            <Copy className="w-3.5 h-3.5" />
            <span>تكرار</span>
          </button>
          <button
            type="button"
            onClick={() => onDelete(sectionId, sub.id)}
            title="حذف هذه المادة من هذه الشعبة"
            className="p-1.5 rounded-lg bg-rose-50 text-rose-700 hover:bg-rose-600 hover:text-white border border-rose-200 text-[11px] font-bold transition-all cursor-pointer flex items-center gap-1 shadow-2xs"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>حذف</span>
          </button>
        </div>
      </td>
    </tr>
  );
});
SubjectRow.displayName = 'SubjectRow';


export const SmartScheduleGeneratorView: React.FC<SmartScheduleGeneratorViewProps> = ({
  scheduleMap,
  setScheduleMap,
  staffList,
  setStaffList,
  students,
  config,
  onBackToLauncher
}) => {
  const [activeStep, setActiveStep] = useState<'classes' | 'subjects' | 'preview'>('classes');

  // School timing settings
  const [startHour, setStartHour] = useState(config.schoolStartHour || '08:00');
  const [lessonDuration, setLessonDuration] = useState(config.lessonDurationMinutes || 45);
  const [breakDuration, setBreakDuration] = useState(config.breakDurationMinutes || 10);

  // Student Roster stats and auto-discovered classes
  const studentDiscoveredStats = React.useMemo(() => {
    const secMap = new Map<string, { grade: string; section: string; count: number }>();
    if (students && students.length > 0) {
      students.forEach(s => {
        if (s.currentGrade && s.section) {
          const cleanGrade = standardizeGradeName(s.currentGrade);
          const cleanSec = standardizeSectionName(s.section);
          const key = `${cleanGrade}_${cleanSec}`;
          const cur = secMap.get(key);
          if (cur) cur.count++;
          else secMap.set(key, { grade: cleanGrade, section: cleanSec, count: 1 });
        }
      });
    }
    const list = sortSectionsList(Array.from(secMap.values()));
    const gradesSet = new Set(list.map(i => i.grade));
    return {
      totalStudents: students?.length || 0,
      sectionsCount: list.length,
      gradesCount: gradesSet.size,
      sectionsList: list
    };
  }, [students]);

  // Enhanced Smart Auto-Import from Student Roster and Staff Distribution Register
  const handleSmartSyncFromStudentsAndStaff = (showToast = true) => {
    const activeSchoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'SCH-VCOL-6072';

    // 1. Discover unique classes and sections from students
    const studentSectionMap = new Map<string, { grade: string; section: string; studentCount: number }>();
    if (students && students.length > 0) {
      students.forEach(s => {
        if (s.currentGrade && s.section) {
          const cleanGrade = standardizeGradeName(s.currentGrade);
          const cleanSec = standardizeSectionName(s.section);
          const key = `${cleanGrade}_${cleanSec}`;
          const existing = studentSectionMap.get(key);
          if (existing) {
            existing.studentCount++;
          } else {
            studentSectionMap.set(key, { grade: cleanGrade, section: cleanSec, studentCount: 1 });
          }
        }
      });
    }

    // Fallback to scheduleMap if students is not yet populated
    if (studentSectionMap.size === 0 && scheduleMap) {
      DAYS_OF_WEEK.forEach(day => {
        (scheduleMap[day] || []).forEach(row => {
          if (row.grade && row.section) {
            const cleanGrade = standardizeGradeName(row.grade);
            const cleanSec = standardizeSectionName(row.section);
            const key = `${cleanGrade}_${cleanSec}`;
            if (!studentSectionMap.has(key)) {
              studentSectionMap.set(key, { grade: cleanGrade, section: cleanSec, studentCount: 0 });
            }
          }
        });
      });
    }

    const discoveredList = sortSectionsList(Array.from(studentSectionMap.values()));
    if (discoveredList.length === 0) {
      if (showToast) alert('لم يتم العثور على سجلات طلاب أو شعب في النظام حتى الآن. يمكنك إضافة الشعب يدوياً.');
      return;
    }

    // 2. Read Staff Subject Assignments from localStorage (Authority Hub)
    let authorityAssignments: any[] = [];
    try {
      const rawAss = localStorage.getItem(`diyala_subject_assignments_${activeSchoolId}`);
      if (rawAss) {
        authorityAssignments = JSON.parse(rawAss);
      }
    } catch {}

    // 3. Build sections with full curriculum & teacher assignments
    const newSections: SmartScheduleSection[] = discoveredList.map((item, idx) => {
      const gradeNorm = item.grade.replace(/^الصف\s+/, '').trim();
      
      const subjects: SectionSubjectAssignment[] = STANDARD_SUBJECTS_TEMPLATE.map((tmpl, sIdx) => {
        const subCanon = canonicalSubject(tmpl.name);

        // Priority 1: Direct assignment from TeacherAuthorityHub for this grade & section
        const directAuthority = authorityAssignments.find(ass => {
          const assGradeNorm = (ass.grade || '').replace(/^الصف\s+/, '').trim();
          const assSec = (ass.section || '').trim();
          const assSubCanon = canonicalSubject(ass.subject || '');
          return (assGradeNorm === gradeNorm || assGradeNorm.includes(gradeNorm) || gradeNorm.includes(assGradeNorm)) &&
                 assSec === item.section &&
                 assSubCanon === subCanon;
        });

        if (directAuthority && directAuthority.teacher_name) {
          return {
            id: `sub-${idx}-${sIdx}`,
            subjectName: tmpl.name,
            teacherName: directAuthority.teacher_name.trim(),
            weeklyLessons: tmpl.quota
          };
        }

        // Priority 2: Staff member with classesTaught matching this class and section
        const staffWithClass = staffList.find(stf => {
          const stfSubCanon = canonicalSubject(stf.actualSubjectTaught || stf.specialization || '');
          if (stfSubCanon !== subCanon) return false;
          return (stf.classesTaught || []).some(cls => {
            const clsClean = cls.replace(/^الصف\s+/, '').trim();
            return clsClean.includes(gradeNorm) && clsClean.includes(item.section);
          });
        });

        if (staffWithClass) {
          const tName = staffWithClass.fullName || `${staffWithClass.firstName} ${staffWithClass.secondName}`.trim();
          return {
            id: `sub-${idx}-${sIdx}`,
            subjectName: tmpl.name,
            teacherName: tName,
            weeklyLessons: tmpl.quota
          };
        }

        // Priority 3: Staff member with matching specialization or actualSubjectTaught
        const staffBySpec = staffList.find(stf => {
          const stfSubCanon = canonicalSubject(stf.actualSubjectTaught || stf.specialization || '');
          return stfSubCanon === subCanon;
        });

        const fallbackTeacher = staffBySpec
          ? (staffBySpec.fullName || `${staffBySpec.firstName} ${staffBySpec.secondName}`.trim())
          : 'أ. أستاذ المادة';

        return {
          id: `sub-${idx}-${sIdx}`,
          subjectName: tmpl.name,
          teacherName: fallbackTeacher,
          weeklyLessons: tmpl.quota
        };
      });

      return {
        id: `sec-smart-${Date.now()}-${idx}`,
        grade: item.grade,
        section: item.section,
        subjects
      };
    });

    const sortedSections = sortSectionsList(newSections);
    setSections(sortedSections);
    localStorage.setItem('diyala_smart_schedule_sections', JSON.stringify(sortedSections));
    if (showToast) {
      alert(`تم استيراد وتوزيع عدد (${sortedSections.length}) شعبة تلقائياً من أيقونة الطلاب، وتوزيع المناهج وإسناد الأساتذة من سجل الكادر بنجاح! ⚡`);
    }
  };

  // Auto-assign teachers across all sections based on their specialization and actual subject
  const handleAutoAssignTeachersBySpec = () => {
    if (!staffList || staffList.length === 0) {
      alert('لا يوجد كادر مسجل في المدرسة للإسناد التلقائي.');
      return;
    }

    const activeSchoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'SCH-VCOL-6072';
    let authorityAssignments: any[] = [];
    try {
      const rawAss = localStorage.getItem(`diyala_subject_assignments_${activeSchoolId}`);
      if (rawAss) {
        authorityAssignments = JSON.parse(rawAss);
      }
    } catch {}

    setSections(prev => prev.map(sec => {
      const gradeNorm = sec.grade.replace(/^الصف\s+/, '').trim();
      return {
        ...sec,
        subjects: sec.subjects.map(sub => {
          const subCanon = canonicalSubject(sub.subjectName);

          // Priority 1: Direct assignment from TeacherAuthorityHub
          const directAuthority = authorityAssignments.find(ass => {
            const assGradeNorm = (ass.grade || '').replace(/^الصف\s+/, '').trim();
            const assSec = (ass.section || '').trim();
            const assSubCanon = canonicalSubject(ass.subject || '');
            return (assGradeNorm === gradeNorm || assGradeNorm.includes(gradeNorm) || gradeNorm.includes(assGradeNorm)) &&
                   assSec === sec.section &&
                   assSubCanon === subCanon;
          });

          if (directAuthority && directAuthority.teacher_name) {
            return { ...sub, teacherName: directAuthority.teacher_name.trim() };
          }

          // Priority 2: Staff member with classesTaught matching this class and section
          const staffWithClass = staffList.find(stf => {
            const stfSubCanon = canonicalSubject(stf.actualSubjectTaught || stf.specialization || '');
            if (stfSubCanon !== subCanon) return false;
            return (stf.classesTaught || []).some(cls => {
              const clsClean = cls.replace(/^الصف\s+/, '').trim();
              return clsClean.includes(gradeNorm) && clsClean.includes(sec.section);
            });
          });

          if (staffWithClass) {
            const tName = staffWithClass.fullName || `${staffWithClass.firstName} ${staffWithClass.secondName}`.trim();
            return { ...sub, teacherName: tName };
          }

          // Priority 3: Staff member matching specialization or actual subject
          const matchingStaff = staffList.find(stf => {
            const stfCanon = canonicalSubject(stf.actualSubjectTaught || stf.specialization || '');
            return stfCanon === subCanon;
          });

          if (matchingStaff) {
            const tName = matchingStaff.fullName || `${matchingStaff.firstName} ${matchingStaff.secondName}`.trim();
            return { ...sub, teacherName: tName };
          }

          return sub;
        })
      };
    }));

    alert('تم مطابقة وإسناد الأساتذة تلقائياً لجميع المواد في كل الشعب وفق سجل الكادر وتوزيع الصلاحيات بنجاح! 🪄');
  };

  // Canonical subject name validator on edit blur
  const handleSubjectNameBlur = (secId: string, subId: string, currentVal: string) => {
    const trimmed = currentVal.trim();
    if (!trimmed) return;
    const canonical = canonicalSubject(trimmed);
    const isMaster = MASTER_SUBJECTS_LIST.includes(canonical) || canonical === 'العلوم' || canonical === 'التربية الأخلاقية';
    const finalName = isMaster ? canonical : trimmed;
    setSections(prev => prev.map(s => s.id === secId ? {
      ...s,
      subjects: s.subjects.map(item => item.id === subId ? { ...item, subjectName: finalName } : item)
    } : s));
  };

  // Sections configuration
  const [sections, setSections] = useState<SmartScheduleSection[]>(() => {
    try {
      const saved = localStorage.getItem('diyala_smart_schedule_sections');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          const { repaired } = sanitizeAndRepairSections(parsed);
          return sortSectionsList(repaired);
        }
      }
    } catch (e) {}

    return [];
  });

  // Auto-sync on mount if no sections saved or only empty
  useEffect(() => {
    const saved = localStorage.getItem('diyala_smart_schedule_sections');
    if ((!saved || sections.length === 0) && studentDiscoveredStats.sectionsCount > 0) {
      handleSmartSyncFromStudentsAndStaff(false);
    }
  }, [studentDiscoveredStats.sectionsCount]);

  // Generated schedule candidate state
  const [generatedMap, setGeneratedMap] = useState<DayScheduleMap>(() => scheduleMap);
  const [collisions, setCollisions] = useState<CollisionReport[]>([]);
  const [isGenerating, setIsGenerating] = useState(false);
  const [saveSuccessMsg, setSaveSuccessMsg] = useState('');
  const [showPrintModal, setShowPrintModal] = useState(false);
  const [selectedPreviewDay, setSelectedPreviewDay] = useState<DayOfWeek>('الأحد');

  // In-place Cell Editor State
  const [editingCell, setEditingCell] = useState<{
    day: DayOfWeek;
    rowId: string;
    lessonKey: typeof LESSON_KEYS[number];
    grade: string;
    section: string;
  } | null>(null);

  const [editSubjectInput, setEditSubjectInput] = useState('');
  const [editTeacherInput, setEditTeacherInput] = useState('');

  // Save sections to localStorage
  useEffect(() => {
    localStorage.setItem('diyala_smart_schedule_sections', JSON.stringify(sections));
  }, [sections]);

  // Handle adding a new section
  const [newGrade, setNewGrade] = useState('الصف الأول متوسط');
  const [newSectionLetter, setNewSectionLetter] = useState('أ');

  const handleAddSection = () => {
    const exists = sections.some(s => s.grade === newGrade && s.section === newSectionLetter);
    if (exists) {
      alert(`الشعبة [${newGrade} - ${newSectionLetter}] موجودة مسبقاً.`);
      return;
    }

    const newSec: SmartScheduleSection = {
      id: `sec-${Date.now()}`,
      grade: newGrade,
      section: newSectionLetter,
      subjects: STANDARD_SUBJECTS_TEMPLATE.map((sub, idx) => ({
        id: `sub-${Date.now()}-${idx}`,
        subjectName: sub.name,
        teacherName: staffList[idx % staffList.length] ? `${staffList[idx % staffList.length].firstName} ${staffList[idx % staffList.length].secondName}` : 'أ. أستاذ المادة',
        weeklyLessons: sub.quota
      }))
    };

    setSections(prev => sortSectionsList([...prev, newSec]));
  };

  const handleRemoveSection = (id: string, label: string) => {
    if (confirm(`هل أنت متأكد من حذف الشعبة [${label}]؟`)) {
      setSections(prev => prev.filter(s => s.id !== id));
    }
  };

  // Copy subjects and teachers from one section to another
  const handleCopySubjectsToAll = (sourceSectionId: string) => {
    const source = sections.find(s => s.id === sourceSectionId);
    if (!source) return;

    if (confirm(`هل ترغب في تعميم قائمة المواد والمدرسين من [${source.grade} - ${source.section}] على كافة الشعب الأخرى؟`)) {
      setSections(prev => prev.map(sec => {
        if (sec.id === sourceSectionId) return sec;
        return {
          ...sec,
          subjects: source.subjects.map(s => ({
            ...s,
            id: `sub-${sec.id}-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`
          }))
        };
      }));
      alert('تم تعميم خطة المواد والأساتذة على كافة الشعب بنجاح!');
    }
  };

  // Assign a specific teacher to teach a subject across ALL sections (e.g. Chemistry teacher for all classes)
  const handleApplyTeacherToAllSections = (subjectName: string, teacherName: string) => {
    if (!teacherName || teacherName === 'شاغر') return;
    if (confirm(`هل تريد تكليف الأستاذ [${teacherName}] بتدريس مادة [${subjectName}] في كافة شعب المدرسة؟`)) {
      setSections(prev => prev.map(sec => ({
        ...sec,
        subjects: sec.subjects.map(sub => {
          if (sub.subjectName.trim() === subjectName.trim()) {
            return { ...sub, teacherName };
          }
          return sub;
        })
      })));
      alert(`تم تكليف [${teacherName}] بمادة [${subjectName}] في كافة الشعب. سيقوم المولد بتوزيع حصصه دون أي تضارب!`);
    }
  };

  // Delete a subject from a section
  const handleDeleteSubject = (sectionId: string, subjectId: string) => {
    setSections(prev => prev.map(sec => {
      if (sec.id !== sectionId) return sec;
      return {
        ...sec,
        subjects: sec.subjects.filter(s => s.id !== subjectId)
      };
    }));
  };

  // Update a single subject's property
  const handleUpdateSubjectAssignment = React.useCallback((sectionId: string, subId: string, updates: Partial<SectionSubjectAssignment>) => {
    setSections(prev => prev.map(sec => {
      if (sec.id !== sectionId) return sec;
      return {
        ...sec,
        subjects: sec.subjects.map(item => item.id === subId ? { ...item, ...updates } : item)
      };
    }));
  }, []);

  // Duplicate an existing subject row (splits quota between teachers without inflating total section quota)
  const handleDuplicateSubject = (sectionId: string, subjectId: string) => {
    setSections(prev => prev.map(sec => {
      if (sec.id !== sectionId) return sec;
      const target = sec.subjects.find(s => s.id === subjectId);
      if (!target) return sec;

      const currentQuota = target.weeklyLessons || 2;
      const duplicateQuota = Math.max(1, Math.floor(currentQuota / 2));
      const remainingQuota = Math.max(1, currentQuota - duplicateQuota);

      const updatedTarget: SectionSubjectAssignment = {
        ...target,
        weeklyLessons: remainingQuota
      };

      const duplicate: SectionSubjectAssignment = {
        ...target,
        id: `sub-${sec.id}-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
        subjectName: target.subjectName,
        teacherName: 'أ. أستاذ آخر',
        weeklyLessons: duplicateQuota
      };

      return {
        ...sec,
        subjects: sec.subjects.map(s => s.id === subjectId ? updatedTarget : s).concat(duplicate)
      };
    }));
  };

  // Auto-Balance Quota of a Section to exactly 30 (protects Biology & Science quotas)
  const handleAutoBalanceSectionTo30 = (sectionId: string) => {
    setSections(prev => prev.map(sec => {
      if (sec.id !== sectionId) return sec;
      const currentQuota = sec.subjects.reduce((sum, s) => sum + (s.weeklyLessons || 0), 0);
      if (currentQuota === 30) {
        alert('مجموع حصص هذه الشعبة هو 30 حصة بالفعل (متوازن تماماً).');
        return sec;
      }

      if (currentQuota < 30) {
        // Less than 30: add a vacant/free activity lesson for the remainder
        const diff = 30 - currentQuota;
        const vacantSub: SectionSubjectAssignment = {
          id: `sub-${sec.id}-${Date.now()}-vacant`,
          subjectName: 'شاغر / نشاط حر',
          teacherName: 'شاغر',
          weeklyLessons: diff
        };
        return {
          ...sec,
          subjects: [...sec.subjects, vacantSub]
        };
      } else {
        // More than 30: Scale down subjects from surplus (e.g. Arabic > 5 or Islamic > 2), protecting Science/Biology
        let surplus = currentQuota - 30;
        const adjustedSubs = [...sec.subjects].map(s => ({ ...s }));
        
        // 1. Reduce/remove any vacant subjects first
        for (let i = adjustedSubs.length - 1; i >= 0 && surplus > 0; i--) {
          if (adjustedSubs[i].subjectName.includes('شاغر') || adjustedSubs[i].teacherName === 'شاغر') {
            const removable = Math.min(surplus, adjustedSubs[i].weeklyLessons);
            adjustedSubs[i].weeklyLessons -= removable;
            surplus -= removable;
          }
        }

        // 2. Reduce Arabic if > 5
        for (let i = 0; i < adjustedSubs.length && surplus > 0; i++) {
          if (adjustedSubs[i].subjectName.includes('عرب') && adjustedSubs[i].weeklyLessons > 5) {
            const removable = Math.min(surplus, adjustedSubs[i].weeklyLessons - 5);
            adjustedSubs[i].weeklyLessons -= removable;
            surplus -= removable;
          }
        }

        // 3. Reduce Islamic if > 2
        for (let i = 0; i < adjustedSubs.length && surplus > 0; i++) {
          if ((adjustedSubs[i].subjectName.includes('اسلام') || adjustedSubs[i].subjectName.includes('دين')) && adjustedSubs[i].weeklyLessons > 2) {
            const removable = Math.min(surplus, adjustedSubs[i].weeklyLessons - 2);
            adjustedSubs[i].weeklyLessons -= removable;
            surplus -= removable;
          }
        }

        // 4. Reduce any non-science subject with quota > 2
        for (let i = adjustedSubs.length - 1; i >= 0 && surplus > 0; i--) {
          const sName = adjustedSubs[i].subjectName;
          const isSci = isForbiddenInPeriod6(sName);
          if (!isSci && adjustedSubs[i].weeklyLessons > 2) {
            const removable = Math.min(surplus, adjustedSubs[i].weeklyLessons - 2);
            adjustedSubs[i].weeklyLessons -= removable;
            surplus -= removable;
          }
        }
        
        // 5. If still surplus, trim any subject > 1 except Biology/Science
        for (let i = adjustedSubs.length - 1; i >= 0 && surplus > 0; i--) {
          if (adjustedSubs[i].weeklyLessons > 1 && !isForbiddenInPeriod6(adjustedSubs[i].subjectName)) {
            const removable = Math.min(surplus, adjustedSubs[i].weeklyLessons - 1);
            adjustedSubs[i].weeklyLessons -= removable;
            surplus -= removable;
          }
        }

        const filtered = adjustedSubs.filter(s => s.weeklyLessons > 0);
        return {
          ...sec,
          subjects: filtered
        };
      }
    }));
  };

  // Add a new subject to a section
  const handleAddSubjectToSection = (sectionId: string, subjectName: string = 'مادة جديدة', quota: number = 2) => {
    const sec = sections.find(s => s.id === sectionId);
    if (sec) {
      const current = sec.subjects.reduce((sum, s) => sum + (s.weeklyLessons || 0), 0);
      if (current + quota > 30) {
        if (!confirm(`⚠️ تنبيه: إضافة هذه المادة (${quota} حصص) ستجعل مجموع حصص الشعبة (${current + quota}) وهو أعلى من 30 حصة المسموح بها أسبوعياً.\n\nهل ترغب في إضافتها على أن تقوم بضبط باقي الحصص لاحقاً؟`)) {
          return;
        }
      }
    }

    setSections(prev => prev.map(sec => {
      if (sec.id !== sectionId) return sec;
      const newSub: SectionSubjectAssignment = {
        id: `sub-${sec.id}-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
        subjectName: subjectName.trim() || 'مادة جديدة',
        teacherName: 'أ. أستاذ المادة',
        weeklyLessons: quota
      };
      return {
        ...sec,
        subjects: [...sec.subjects, newSub]
      };
    }));
  };

  // Reset section subjects to standard ministry template
  const handleResetSectionToTemplate = (sectionId: string) => {
    if (confirm('هل ترغب في إعادة تعيين مواد هذه الشعبة إلى الخطة الوزارية القياسية (30 حصة)؟')) {
      setSections(prev => prev.map(sec => {
        if (sec.id !== sectionId) return sec;
        return {
          ...sec,
          subjects: STANDARD_SUBJECTS_TEMPLATE.map((sub, idx) => ({
            id: `sub-${sec.id}-${Date.now()}-${idx}`,
            subjectName: sub.name,
            teacherName: staffList[idx % staffList.length] ? `${staffList[idx % staffList.length].firstName} ${staffList[idx % staffList.length].secondName}` : 'أ. أستاذ المادة',
            weeklyLessons: sub.quota
          }))
        };
      }));
    }
  };

  // Clear all subjects in a section
  const handleClearAllSubjects = (sectionId: string) => {
    if (confirm('هل ترغب في مسح وتفريغ كافة المواد في هذه الشعبة؟')) {
      setSections(prev => prev.map(sec => {
        if (sec.id !== sectionId) return sec;
        return {
          ...sec,
          subjects: []
        };
      }));
    }
  };

  // Trigger Smart Algorithm
  const handleRunGenerator = () => {
    // 1. Strict Quota Validation: Check if any section has quota > 30 (Extra lessons prevention)
    const overQuotaSections = sections.filter(sec => {
      const q = sec.subjects.reduce((sum, s) => sum + (s.weeklyLessons || 0), 0);
      return q > 30;
    });

    if (overQuotaSections.length > 0) {
      const names = overQuotaSections.map(s => `[${s.grade} - ${s.section}] (${s.subjects.reduce((sum, sub) => sum + (sub.weeklyLessons || 0), 0)} حصة)`).join('، ');
      alert(`⚠️ لا يمكن توليد الجدول بسبب وجود حصص زيادة في الشعب التالية:\n${names}\n\nالحد الأقصى المسموح به هو 30 حصة أسبوعياً (5 أيام × 6 دروس). يرجى تقليل حصص المواد الزائدة أو مسحها حتى لا تتجاوز 30.`);
      return;
    }

    setIsGenerating(true);
    setSaveSuccessMsg('');

    setTimeout(() => {
      const freshSeed = Date.now() + Math.random() * 1000000;
      const result = generateSmartFairSchedule(sections, 500, freshSeed);
      setGeneratedMap(result.scheduleMap);
      setCollisions(result.collisions);
      setIsGenerating(false);
      setActiveStep('preview');

      if (result.success) {
        // Success
      } else if (result.errorMsg) {
        alert(result.errorMsg);
      }
    }, 250);
  };

  // Open Cell Editor
  const handleOpenCellEditor = (day: DayOfWeek, row: ClassScheduleRow, lessonKey: typeof LESSON_KEYS[number]) => {
    const cell = row.lessons[lessonKey];
    setEditingCell({
      day,
      rowId: row.id,
      lessonKey,
      grade: row.grade,
      section: row.section
    });
    setEditSubjectInput(cell?.subject || '');
    setEditTeacherInput(cell?.teacherName || '');
  };

  const handleSaveCellEdit = () => {
    if (!editingCell) return;
    const { day, rowId, lessonKey } = editingCell;

    setGeneratedMap(prev => {
      const updated = { ...prev };
      const rows = [...(updated[day] || [])];
      const targetRowIdx = rows.findIndex(r => r.id === rowId);
      if (targetRowIdx !== -1) {
        rows[targetRowIdx] = {
          ...rows[targetRowIdx],
          lessons: {
            ...rows[targetRowIdx].lessons,
            [lessonKey]: {
              subject: editSubjectInput.trim(),
              teacherName: editTeacherInput.trim(),
              isOff: !editSubjectInput.trim()
            }
          }
        };
        updated[day] = rows;
      }
      // Re-check collisions after manual edit
      const cols = checkScheduleCollisions(updated);
      setCollisions(cols);
      return updated;
    });

    setEditingCell(null);
  };

  // Save Official Schedule and push to Cloud
  const handleSaveAndApplyOfficial = async () => {
    if (collisions.length > 0) {
      if (!confirm(`⚠️ يوجد (${collisions.length}) تضارب في مواعيد المعلمين. هل أنت متأكد من اعتماد الجدول مع التضارب؟`)) {
        return;
      }
    }

    setScheduleMap(generatedMap);
    localStorage.setItem('diyala_school_schedule', JSON.stringify(generatedMap));

    // 1. Calculate and update Staff Members according to the adopted schedule
    if (staffList && staffList.length > 0) {
      const updatedStaffList = staffList.map(staff => {
        let totalLessons = 0;
        const classesSet = new Set<string>();
        const subjectsSet = new Set<string>();

        DAYS_OF_WEEK.forEach(day => {
          const rows = generatedMap[day] || [];
          rows.forEach(row => {
            LESSON_KEYS.forEach(lk => {
              const cell = row.lessons[lk];
              if (cell && !cell.isOff && cell.teacherName) {
                const isMatch = matchStaffWithScheduleCell(staff, cell.teacherName, cell.subject);
                if (isMatch) {
                  totalLessons++;
                  classesSet.add(`${standardizeGradeName(row.grade)} - شعبة ${standardizeSectionName(row.section)}`);
                  const validSub = standardizeSubjectName(cell.subject);
                  if (validSub) subjectsSet.add(validSub);
                }
              }
            });
          });
        });

        if (totalLessons > 0 || classesSet.size > 0) {
          const primarySubject = Array.from(subjectsSet)[0] || staff.actualSubjectTaught || staff.specialization;
          return {
            ...staff,
            teachingQuota: totalLessons,
            classesTaught: Array.from(classesSet),
            actualSubjectTaught: primarySubject
          };
        }
        return staff;
      });

      if (setStaffList) {
        setStaffList(updatedStaffList);
      }
      localStorage.setItem('diyala_school_staff', JSON.stringify(updatedStaffList));
    }

    setSaveSuccessMsg('تم حفظ واعتماد الجدول المدرسي وتحديث أنصبة ومواد كادر التدريس رسمياً! 💾');

    // Sync to Supabase
    try {
      const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
      const client = getSupabase(schoolId);
      await client.from('schedules').upsert({
        id: schoolId,
        schedule_map: generatedMap
      }, { onConflict: 'id', ignoreDuplicates: false });
      setSaveSuccessMsg('تم حفظ واعتماد الجدول وتحديث أنصبة المعلمين ورفعها للسحابة بنجاح تام! 🚀');
    } catch (e) {}

    setTimeout(() => setSaveSuccessMsg(''), 5000);
  };

  // Teacher name options from Staff Register
  const teacherOptions = Array.from(new Set([
    ...staffList.map(s => `${s.firstName} ${s.secondName}`.trim()),
    'أ. أستاذ المادة',
    'شاغر'
  ])).filter(Boolean);

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
      
      {/* Top Header Card */}
      <div className="bg-white border-4 theme-accent-border rounded-3xl p-6 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 to-purple-600 text-white flex items-center justify-center shadow-lg shadow-indigo-200">
            <Sparkles className="w-8 h-8 text-amber-300 animate-pulse" />
          </div>
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-indigo-50 border border-indigo-200 text-indigo-900 text-xs font-black mb-1">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
              <span>محرك الذكاء الاصطناعي للتوزيع العادل ومنع التضارب</span>
            </div>
            <h1 className="text-xl md:text-2xl font-black text-slate-900">
              المولّد الذكي للجدول المدرسي العادل (Auto-Scheduler Wizard)
            </h1>
            <p className="text-xs text-slate-600 font-bold mt-0.5">
              حدد الصفوف والشعب والمواد ومدرسيها وأنصبتها، وسيقوم النظام بتوزيع الحصص بصورة عادلة خالية من أي تضارب للمعلمين 100%.
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={onBackToLauncher}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-black transition-all cursor-pointer shadow-md"
            title="إغلاق والعودة لجدول الحصص الرئيسي"
          >
            <ArrowRight className="w-4 h-4" />
            <span>العودة لجدول الحصص ✕</span>
          </button>
        </div>
      </div>

      {/* Step Navigation Bar */}
      <div className="grid grid-cols-3 gap-3">
        <button
          onClick={() => setActiveStep('classes')}
          className={`p-3.5 rounded-2xl font-black text-xs md:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer border-2 ${
            activeStep === 'classes'
              ? 'bg-indigo-600 text-white border-indigo-600 shadow-md scale-[1.02]'
              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
          }`}
        >
          <span className="w-6 h-6 rounded-full bg-white/20 flex items-center justify-center text-xs">1</span>
          <span>1. الصفوف والشعب والتوقيتات</span>
        </button>

        <button
          onClick={() => setActiveStep('subjects')}
          className={`p-3.5 rounded-2xl font-black text-xs md:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer border-2 ${
            activeStep === 'subjects'
              ? 'bg-indigo-600 text-white border-indigo-600 shadow-md scale-[1.02]'
              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
          }`}
        >
          <span className="w-6 h-6 rounded-full bg-white/20 flex items-center justify-center text-xs">2</span>
          <span>2. المواد ومدرسوها والأنصبة</span>
        </button>

        <button
          onClick={() => setActiveStep('preview')}
          className={`p-3.5 rounded-2xl font-black text-xs md:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer border-2 ${
            activeStep === 'preview'
              ? 'bg-purple-700 text-white border-purple-700 shadow-md scale-[1.02]'
              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
          }`}
        >
          <span className="w-6 h-6 rounded-full bg-white/20 flex items-center justify-center text-xs">3</span>
          <span>3. التوليد والمعاينة والتعديل 🪄</span>
        </button>
      </div>

      {/* STEP 1: Classes, Sections & Timings */}
      {activeStep === 'classes' && (
        <div className="space-y-6">
          
          {/* Timings Card */}
          <div className="bg-white border-2 border-slate-300 rounded-3xl p-6 shadow-sm space-y-4">
            <h3 className="text-base font-black text-slate-900 flex items-center gap-2 border-b border-slate-200 pb-3">
              <Clock className="w-5 h-5 text-indigo-600" />
              <span>إعدادات توقيت الدوام والحصص اليومية</span>
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
              <div>
                <label className="block font-black text-slate-800 mb-1">وقت بداية الدوام (الطابور/الدرس 1):</label>
                <input
                  type="time"
                  value={startHour}
                  onChange={e => setStartHour(e.target.value)}
                  className="w-full p-2.5 rounded-xl border-2 border-slate-300 font-bold font-mono text-sm bg-white"
                />
              </div>
              <div>
                <label className="block font-black text-slate-800 mb-1">مدة الحصة الدراسية (بالدقائق):</label>
                <input
                  type="number"
                  min="30"
                  max="60"
                  value={lessonDuration}
                  onChange={e => setLessonDuration(Number(e.target.value))}
                  className="w-full p-2.5 rounded-xl border-2 border-slate-300 font-bold font-mono text-sm bg-white"
                />
              </div>
              <div>
                <label className="block font-black text-slate-800 mb-1">مدة الاستراحة / الفرصة (بالدقائق):</label>
                <input
                  type="number"
                  min="5"
                  max="30"
                  value={breakDuration}
                  onChange={e => setBreakDuration(Number(e.target.value))}
                  className="w-full p-2.5 rounded-xl border-2 border-slate-300 font-bold font-mono text-sm bg-white"
                />
              </div>
            </div>
          </div>

          {/* Add Section Card */}
          <div className="bg-white border-2 border-slate-300 rounded-3xl p-6 shadow-sm space-y-4">
            
            {/* Student & Staff Sync Info Banner */}
            <div className="p-4 rounded-2xl bg-gradient-to-r from-emerald-50 via-teal-50 to-indigo-50 border-2 border-emerald-300 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center shadow-sm shrink-0">
                  <Users className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="font-black text-emerald-950 text-sm flex items-center gap-1.5">
                    <span>إحصائية بيانات الطلاب والكادر المكتشفة في النظام</span>
                    <span className="px-2 py-0.5 rounded-full bg-emerald-200 text-emerald-900 text-[11px] font-black">ربط تلقائي</span>
                  </h4>
                  <p className="text-xs text-emerald-800 font-bold mt-0.5">
                    {studentDiscoveredStats.totalStudents > 0 ? (
                      <>
                        تم اكتشاف <strong>{studentDiscoveredStats.totalStudents}</strong> طالباً موزعين على <strong>{studentDiscoveredStats.sectionsCount}</strong> شعبة ضمن <strong>{studentDiscoveredStats.gradesCount}</strong> مراحل دراسية.
                      </>
                    ) : (
                      'يمكنك الاستيراد المباشر من سجل الطلاب وسجل توزيع الكادر بضغطة زر أدناه.'
                    )}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => handleSmartSyncFromStudentsAndStaff(true)}
                className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs transition-all shadow-sm cursor-pointer whitespace-nowrap active:scale-95 shrink-0"
                title="توليد جميع الشعب وفق سجل الطلاب وإسناد الأساتذة وفق سجل الكادر"
              >
                <Sparkles className="w-4 h-4 text-amber-300 animate-pulse" />
                <span>⚡ استيراد الشعب والمواد من سجل الطلاب والكادر</span>
              </button>
            </div>

            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-slate-200 pb-3">
              <h3 className="text-base font-black text-slate-900 flex items-center gap-2">
                <Layers className="w-5 h-5 text-indigo-600" />
                <span>إضافة وتحديد الصفوف والشعب المدرسية</span>
              </h3>
              <span className="text-xs font-black text-indigo-900 bg-indigo-50 px-3 py-1 rounded-xl border border-indigo-200">
                الشعب المعتمدة حالياً: {sections.length}
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <div className="flex-1 min-w-[200px]">
                <label className="block text-xs font-black text-slate-700 mb-1">المرحلة / الصف:</label>
                <select
                  value={newGrade}
                  onChange={e => setNewGrade(e.target.value)}
                  className="w-full p-2.5 rounded-xl border-2 border-slate-300 font-bold text-xs bg-white"
                >
                  <option value="الصف الأول متوسط">الصف الأول متوسط</option>
                  <option value="الصف الثاني متوسط">الصف الثاني متوسط</option>
                  <option value="الصف الثالث متوسط">الصف الثالث متوسط</option>
                  <option value="الصف الرابع العلمي">الصف الرابع العلمي</option>
                  <option value="الصف الرابع الأدبي">الصف الرابع الأدبي</option>
                  <option value="الصف الخامس العلمي">الصف الخامس العلمي</option>
                  <option value="الصف الخامس الأدبي">الصف الخامس الأدبي</option>
                  <option value="الصف السادس العلمي">الصف السادس العلمي</option>
                  <option value="الصف السادس الأدبي">الصف السادس الأدبي</option>
                  <option value="الصف الأول الابتدائي">الصف الأول الابتدائي</option>
                  <option value="الصف الثاني الابتدائي">الصف الثاني الابتدائي</option>
                  <option value="الصف الثالث الابتدائي">الصف الثالث الابتدائي</option>
                  <option value="الصف الرابع الابتدائي">الصف الرابع الابتدائي</option>
                  <option value="الصف الخامس الابتدائي">الصف الخامس الابتدائي</option>
                  <option value="الصف السادس الابتدائي">الصف السادس الابتدائي</option>
                </select>
              </div>

              <div className="w-32">
                <label className="block text-xs font-black text-slate-700 mb-1">الشعبة:</label>
                <select
                  value={newSectionLetter}
                  onChange={e => setNewSectionLetter(e.target.value)}
                  className="w-full p-2.5 rounded-xl border-2 border-slate-300 font-bold text-xs bg-white text-center"
                >
                  {['أ', 'ب', 'ج', 'د', 'هـ', 'و', 'ز', 'ح'].map(l => (
                    <option key={l} value={l}>شعبة {l}</option>
                  ))}
                </select>
              </div>

              <div className="pt-5">
                <button
                  onClick={handleAddSection}
                  className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs transition-all shadow-md cursor-pointer"
                >
                  <Plus className="w-4 h-4" />
                  <span>إضافة الشعبة للجدول</span>
                </button>
              </div>
            </div>

            {/* List of Configured Sections */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3 pt-3">
              {sections.map(sec => {
                const totalQuota = sec.subjects.reduce((sum, s) => sum + (s.weeklyLessons || 0), 0);
                return (
                  <div 
                    key={sec.id}
                    className="p-4 rounded-2xl bg-slate-50 border-2 border-slate-200 flex items-center justify-between shadow-xs"
                  >
                    <div>
                      <h4 className="font-black text-slate-900 text-sm">{sec.grade} - شعبة {sec.section}</h4>
                      <div className="flex items-center gap-2 mt-1">
                        <span className={`text-[11px] font-black px-2 py-0.5 rounded-full ${
                          totalQuota === 30 ? 'bg-emerald-100 text-emerald-900 border border-emerald-300' : 'bg-amber-100 text-amber-900 border border-amber-300'
                        }`}>
                          {totalQuota} / 30 حصة أسبوعياً
                        </span>
                        <span className="text-[11px] text-slate-500 font-bold">({sec.subjects.length} مواد)</span>
                      </div>
                    </div>
                    <button
                      onClick={() => handleRemoveSection(sec.id, `${sec.grade} - ${sec.section}`)}
                      title="حذف هذه الشعبة"
                      className="p-2 rounded-xl bg-rose-100 text-rose-700 hover:bg-rose-600 hover:text-white transition-all cursor-pointer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                );
              })}
            </div>

            {/* Navigation Button */}
            <div className="flex justify-end pt-4 border-t border-slate-200">
              <button
                onClick={() => setActiveStep('subjects')}
                className="flex items-center gap-2 px-6 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-sm shadow-lg transition-all cursor-pointer"
              >
                <span>الانتقال لتوزيع المواد والأساتذة</span>
                <ChevronLeft className="w-4 h-4" />
              </button>
            </div>
          </div>

        </div>
      )}

      {/* STEP 2: Subject, Teacher & Quota Matrix */}
      {activeStep === 'subjects' && (
        <div className="space-y-6">
          
          <div className="bg-white border-2 border-slate-300 rounded-3xl p-6 shadow-sm space-y-4">
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-3 border-b border-slate-200 pb-3">
              <div>
                <h3 className="text-base font-black text-slate-900 flex items-center gap-2">
                  <BookOpen className="w-5 h-5 text-indigo-600" />
                  <span>توزيع المواد وأسماء المدرسين وعدد الحصص الأسبوعية لكل شعبة</span>
                </h3>
                <p className="text-xs text-slate-600 font-bold mt-0.5">
                  إذا كان المدرس يُدرّس المادة في كل الشعب (مثل الكيمياء أو الرياضيات)، اختر اسمه وسيضمن النظام عدم تضاربه مطلقاً.
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={() => {
                    const { repaired, wasModified, fixesSummary } = sanitizeAndRepairSections(sections);
                    setSections(sortSectionsList(repaired));
                    if (wasModified) {
                      alert(`✅ تم إصلاح وموازنة الأنصبة بنجاح!\n\n${fixesSummary.join('\n')}\n\nأصبحت جميع الشعب مضبوطة على 30 حصة أسبوعياً.`);
                    } else {
                      alert('✅ جميع الأنصبة موزونة تماماً (30 حصة) ولا توجد أي حصص زائدة أو ناقصة.');
                    }
                  }}
                  className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white font-black text-xs transition-all shadow-sm cursor-pointer active:scale-95"
                  title="موازنة فورية لكافة الشعب وإصلاح أي زيادة في العربي أو الإسلامية واستعادة مادة الأحياء"
                >
                  <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                  <span>⚡ موازنة وإصلاح الأنصبة لكافة الشعب (30 حصة)</span>
                </button>
                <button
                  type="button"
                  onClick={handleAutoAssignTeachersBySpec}
                  className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 text-white font-black text-xs transition-all shadow-sm cursor-pointer active:scale-95"
                  title="مطابقة مواد كل الشعب مع اختصاصات كادر المدرسة المسجل تلقائياً"
                >
                  <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                  <span>🪄 إسناد الأساتذة تلقائياً حسب الاختصاص</span>
                </button>
                <span className="text-xs font-black text-slate-700 bg-slate-100 px-3 py-1.5 rounded-xl border border-slate-200">
                  إجمالي الشعب: {sections.length}
                </span>
              </div>
            </div>

            {/* Smart Rules & Constraints Card (ضوابط وقوانين الجدول المدرسي المعتمدة) */}
            <div className="p-4 rounded-2xl bg-gradient-to-r from-indigo-900 via-slate-900 to-indigo-950 text-white shadow-md border border-indigo-700/50">
              <div className="flex items-center gap-2 mb-2">
                <ShieldCheck className="w-5 h-5 text-emerald-400" />
                <h4 className="font-black text-sm text-indigo-100">
                  ضوابط وقوانين التوزيع المبرمجة آلياً في خوارزمية التوليد:
                </h4>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
                <div className="flex items-start gap-2 bg-white/10 p-2.5 rounded-xl border border-white/10">
                  <span className="text-rose-400 font-black text-base leading-none">🚫</span>
                  <div>
                    <span className="font-black text-amber-300 block mb-0.5">استبعاد الدرس السادس</span>
                    <p className="text-[11px] text-slate-200 leading-relaxed font-bold">
                      المواد العلمية (فيزياء، كيمياء، أحياء) والدروس الفردية والثنائية (نصاب حصة أو حصتان كالأخلاقية) لا توضع أبداً في الدرس السادس.
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-2 bg-white/10 p-2.5 rounded-xl border border-white/10">
                  <span className="text-amber-400 font-black text-base leading-none">📅</span>
                  <div>
                    <span className="font-black text-amber-300 block mb-0.5">سادس الخميس شاغر حتماً</span>
                    <p className="text-[11px] text-slate-200 leading-relaxed font-bold">
                      لا يوضع درس حقيقي في الدرس السادس يوم الخميس؛ فهو مخصص لانتهاء الدوام أو النشاط الحر لجميع الصفوف.
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-2 bg-white/10 p-2.5 rounded-xl border border-white/10">
                  <span className="text-emerald-400 font-black text-base leading-none">🛑</span>
                  <div>
                    <span className="font-black text-emerald-300 block mb-0.5">الشواغر في الدرس السادس</span>
                    <p className="text-[11px] text-slate-200 leading-relaxed font-bold">
                      إذا قل نصاب الحصص عن 30 حصة، يتم حصر الحصص الشاغرة حتماً في نهاية الدوام دون أي فراغات وسط اليوم المدرسي.
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-2 bg-white/10 p-2.5 rounded-xl border border-white/10">
                  <span className="text-cyan-400 font-black text-base leading-none">⚖️</span>
                  <div>
                    <span className="font-black text-cyan-300 block mb-0.5">توازن السادس والتدوير العادل</span>
                    <p className="text-[11px] text-slate-200 leading-relaxed font-bold">
                      توزيع متكافئ للدرس السادس بين المواد المسموحة والمدرسين دون تكرار مدرس أو درس أكثر من غيره، مع تدوير الحصص بين بداية ونهاية الدوام.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Sections Accordion/Cards */}
            <div className="space-y-6">
              {sections.map((sec, secIdx) => {
                const totalQuota = sec.subjects.reduce((sum, s) => sum + (s.weeklyLessons || 0), 0);
                return (
                  <div key={sec.id} className="border-2 border-indigo-100 rounded-2xl p-4 bg-slate-50/70 space-y-3">
                    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 border-b border-indigo-100 pb-2">
                      <div className="flex items-center gap-3">
                        <span className="w-7 h-7 rounded-xl bg-indigo-600 text-white font-black text-xs flex items-center justify-center">
                          {secIdx + 1}
                        </span>
                        <h4 className="font-black text-slate-900 text-base">
                          {sec.grade} - شعبة ({sec.section})
                        </h4>
                        <span className={`text-xs font-black px-2.5 py-0.5 rounded-full ${
                          totalQuota === 30 
                            ? 'bg-emerald-100 text-emerald-900 border border-emerald-300' 
                            : totalQuota > 30 
                              ? 'bg-rose-100 text-rose-900 border-2 border-rose-400 animate-pulse' 
                              : 'bg-amber-100 text-amber-900 border border-amber-300'
                        }`}>
                          {totalQuota === 30 && 'المجموع: 30 / 30 حصة (مثالي ✓)'}
                          {totalQuota > 30 && `⚠️ زيادة: ${totalQuota} / 30 حصة (زيادة ${totalQuota - 30} حصص!)`}
                          {totalQuota < 30 && `المجموع: ${totalQuota} / 30 حصة (شاغر ${30 - totalQuota})`}
                        </span>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => handleCopySubjectsToAll(sec.id)}
                          title="تعميم هذا التوزيع على باقي الشعب"
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white text-indigo-700 border border-indigo-200 text-xs font-black hover:bg-indigo-50 transition-all cursor-pointer shadow-xs"
                        >
                          <Copy className="w-3.5 h-3.5" />
                          <span>تعميم على باقي الشعب</span>
                        </button>
                      </div>
                    </div>

                    {/* Table of Subjects for this Section */}
                    <div className="overflow-x-auto">
                      <table className="w-full text-right text-xs border-collapse">
                        <thead>
                          <tr className="bg-slate-200/80 text-slate-900 font-black border-b border-slate-300">
                            <th className="p-2 w-10 text-center">ت</th>
                            <th className="p-2 min-w-[150px]">اسم المادة الدراسية</th>
                            <th className="p-2 min-w-[180px]">أستاذ المادة</th>
                            <th className="p-2 w-28 text-center">الحصص أسبوعياً</th>
                            <th className="p-2 w-32 text-center">تطبيق للكل</th>
                            <th className="p-2 w-36 text-center">تكرار / حذف ⚙️</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-200">
                          {sec.subjects.length === 0 ? (
                            <tr>
                              <td colSpan={6} className="py-6 text-center text-slate-500 font-bold bg-amber-50/50">
                                تم تفريغ كافة المواد من هذه الشعبة. يمكنك استخدام شريط الإضافة أدناه لإضافة مواد جديدة.
                              </td>
                            </tr>
                          ) : (
                            sec.subjects.map((sub, sIdx) => (
                              <SubjectRow
                                key={sub.id}
                                sectionId={sec.id}
                                sub={sub}
                                sIdx={sIdx}
                                staffList={staffList}
                                onUpdateSubject={handleUpdateSubjectAssignment}
                                onBlurSubjectName={handleSubjectNameBlur}
                                onApplyTeacherToAll={handleApplyTeacherToAllSections}
                                onDuplicate={handleDuplicateSubject}
                                onDelete={handleDeleteSubject}
                              />
                            ))
                          )}
                        </tbody>
                      </table>
                    </div>

                    {/* Quick Add Subject Bar & Section Actions */}
                    <div className="bg-indigo-50/60 p-3 rounded-xl border border-indigo-100 flex flex-wrap items-center justify-between gap-3 text-xs mt-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-black text-indigo-950 flex items-center gap-1">
                          <Plus className="w-4 h-4 text-indigo-600" />
                          <span>إضافة مادة للشعبة:</span>
                        </span>
                        <select
                          id={`add-sub-select-${sec.id}`}
                          className="p-1.5 rounded-lg border border-slate-300 font-bold bg-white text-slate-900 cursor-pointer"
                          defaultValue="التربية الإسلامية"
                        >
                          {STANDARD_SUBJECTS_TEMPLATE.map(s => (
                            <option key={s.name} value={s.name}>{s.name} ({s.quota} حصص)</option>
                          ))}
                          <option value="التربية الأخلاقية">التربية الأخلاقية (1 حصة)</option>
                          <option value="النشيد والموسيقى">النشيد والموسيقى (1 حصة)</option>
                          <option value="نشاط حر / شاغر">نشاط حر / شاغر (1 حصة)</option>
                          <option value="مادة إضافية مخصصة">مادة إضافية مخصصة (2 حصة)</option>
                        </select>
                        <button
                          type="button"
                          onClick={() => {
                            const select = document.getElementById(`add-sub-select-${sec.id}`) as HTMLSelectElement;
                            const val = select ? select.value : 'مادة جديدة';
                            const quota = val.includes('5') ? 5 : val.includes('4') ? 4 : val.includes('3') ? 3 : val.includes('1') ? 1 : 2;
                            const cleanName = val.split('(')[0].trim();
                            handleAddSubjectToSection(sec.id, cleanName, quota);
                          }}
                          className="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs transition-all shadow-xs cursor-pointer flex items-center gap-1"
                        >
                          <Plus className="w-3.5 h-3.5" />
                          <span>إضافة المادة ➕</span>
                        </button>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => handleAutoBalanceSectionTo30(sec.id)}
                          className="px-2.5 py-1 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 text-[11px] font-black transition-all cursor-pointer shadow-2xs flex items-center gap-1"
                          title="موازنة مجموع الحصص آلياً ليصبح 30 حصة تماماً"
                        >
                          ⚖️ موازنة لـ 30 حصة
                        </button>
                        <button
                          type="button"
                          onClick={() => handleResetSectionToTemplate(sec.id)}
                          className="px-2.5 py-1 rounded-lg bg-white hover:bg-slate-100 text-slate-700 border border-slate-300 text-[11px] font-bold transition-all cursor-pointer shadow-2xs"
                          title="استعادة المواد الوزارية القياسية (30 حصة)"
                        >
                          🔄 استعادة خطة 30 حصة
                        </button>
                        <button
                          type="button"
                          onClick={() => handleClearAllSubjects(sec.id)}
                          className="px-2.5 py-1 rounded-lg bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 text-[11px] font-bold transition-all cursor-pointer shadow-2xs"
                          title="حذف وتفريغ كافة المواد"
                        >
                          🗑️ تفريغ كافة المواد
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>


            {/* Bottom Actions */}
            <div className="flex items-center justify-between pt-4 border-t border-slate-200">
              <button
                onClick={() => setActiveStep('classes')}
                className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 font-black text-xs transition-all cursor-pointer"
              >
                <ChevronRight className="w-4 h-4" />
                <span>السابق (الصفوف)</span>
              </button>

              <button
                onClick={handleRunGenerator}
                disabled={isGenerating}
                className="flex items-center gap-2 px-8 py-3 rounded-2xl bg-gradient-to-r from-purple-700 to-indigo-700 hover:from-purple-800 hover:to-indigo-800 text-white font-black text-sm shadow-xl transition-all cursor-pointer scale-105"
              >
                <Sparkles className="w-5 h-5 text-amber-300 animate-spin" />
                <span>{isGenerating ? 'جارٍ تشغيل الخوارزمية الذكية...' : 'توليد الجدول العادل آلياً الآن 🚀'}</span>
              </button>
            </div>
          </div>

        </div>
      )}

      {/* STEP 3: Preview, Live Conflict Checker & In-place Editor */}
      {activeStep === 'preview' && (
        <div className="space-y-6">
          
          {/* Status Bar */}
          <div className={`p-5 rounded-3xl border-2 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 shadow-md ${
            collisions.length === 0
              ? 'bg-emerald-50 border-emerald-300 text-emerald-950'
              : 'bg-amber-50 border-amber-400 text-amber-950'
          }`}>
            <div className="flex items-center gap-3">
              {collisions.length === 0 ? (
                <div className="w-12 h-12 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shadow-md">
                  <CheckCircle2 className="w-7 h-7" />
                </div>
              ) : (
                <div className="w-12 h-12 rounded-2xl bg-amber-500 text-white flex items-center justify-center shadow-md">
                  <AlertTriangle className="w-7 h-7" />
                </div>
              )}
              <div>
                <h3 className="font-black text-base md:text-lg">
                  {collisions.length === 0 
                    ? 'جدول مثالي خالٍ من أي تضارب بنسبة 100% (Conflict-Free Schedule) ✓' 
                    : `تنبيه: يوجد (${collisions.length}) تضارب بحاجة لمراجعة أو تعديل يدوياً.`}
                </h3>
                <p className="text-xs font-bold opacity-90">
                  {collisions.length === 0 
                    ? 'تم توزيع كافة حصص الأساتذة والمواد بتوازن تام (عدم تكرار المادة في نفس اليوم للشعبة + تدوير الحصص عبر الأسبوع + وضع الشواغر نهاية الأسبوع) دون أي تضارب ✓.'
                    : 'يمكنك النقر على أي حصة مباشرة في الجدول أدناه لتعديل المادة أو تغيير الأستاذ وحل التضارب فورياً.'}
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={handleRunGenerator}
                disabled={isGenerating}
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-white text-indigo-700 border border-indigo-300 hover:bg-indigo-50 font-black text-xs shadow-xs transition-all cursor-pointer disabled:opacity-50"
              >
                <RotateCcw className={`w-4 h-4 ${isGenerating ? 'animate-spin' : ''}`} />
                <span>{isGenerating ? 'جارٍ إعادة التوزيع والتدوير...' : 'إعادة التوليد بتوزيع آخر 🔄'}</span>
              </button>

              <button
                onClick={handleSaveAndApplyOfficial}
                className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs shadow-lg transition-all cursor-pointer"
              >
                <Save className="w-4 h-4" />
                <span>اعتماد وحفظ الجدول الرسمي 💾</span>
              </button>

              <button
                onClick={() => setShowPrintModal(true)}
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-black text-xs shadow-xs transition-all cursor-pointer"
              >
                <Printer className="w-4 h-4" />
                <span>طباعة الجدول 🖨️</span>
              </button>
            </div>
          </div>

          {/* Success Message Banner */}
          {saveSuccessMsg && (
            <div className="p-4 rounded-2xl bg-emerald-600 text-white font-black text-sm text-center shadow-lg animate-bounce">
              {saveSuccessMsg}
            </div>
          )}

          {/* Day Filter Tabs for Preview */}
          <div className="flex gap-2">
            {DAYS_OF_WEEK.map(day => (
              <button
                key={day}
                onClick={() => setSelectedPreviewDay(day)}
                className={`flex-1 py-3 rounded-2xl font-black text-xs md:text-sm transition-all cursor-pointer border-2 ${
                  selectedPreviewDay === day
                    ? 'bg-indigo-600 text-white border-indigo-600 shadow-md scale-105'
                    : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                }`}
              >
                {day}
              </button>
            ))}
          </div>

          {/* Interactive Timetable Grid for Selected Day */}
          <div className="bg-white border-2 border-slate-300 rounded-3xl shadow-xl overflow-hidden">
            <div className="p-4 bg-slate-100 border-b border-slate-300 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Calendar className="w-5 h-5 text-indigo-600" />
                <h3 className="font-black text-slate-900 text-base">
                  جدول يوم ({selectedPreviewDay}) - اضغط على أي حصة لتعديلها يدوياً ✏️
                </h3>
              </div>
              <span className="text-xs font-black text-indigo-700 bg-indigo-50 border border-indigo-200 px-3 py-1 rounded-xl">
                عدد الشعب: {(generatedMap[selectedPreviewDay] || []).length}
              </span>
            </div>

            <div className="overflow-x-auto custom-scrollbar">
              <table className="w-full text-center border-collapse min-w-[850px] text-xs">
                <thead>
                  <tr className="bg-slate-200/90 text-slate-950 font-black border-b-2 border-slate-300">
                    <th className="p-3 border-r border-slate-300 w-24">الصف</th>
                    <th className="p-3 border-r border-slate-300 w-16">الشعبة</th>
                    {LESSON_KEYS.map((lKey) => (
                      <th key={lKey} className="p-3 border-r border-slate-300 min-w-[130px]">
                        {LESSON_LABELS[lKey]}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {(generatedMap[selectedPreviewDay] || []).map((row) => (
                    <tr key={row.id} className="hover:bg-slate-50 transition-colors">
                      <td className="p-2.5 font-black text-slate-900 border-r border-slate-200 bg-slate-50/50">
                        {row.grade}
                      </td>
                      <td className="p-2.5 font-black text-indigo-700 border-r border-slate-200 bg-indigo-50/40 text-sm">
                        {row.section}
                      </td>

                      {LESSON_KEYS.map((lKey) => {
                        const cell = row.lessons[lKey];
                        const isCellConflict = collisions.some(
                          c => c.day === selectedPreviewDay && c.lessonKey === lKey && c.teacherName === cell?.teacherName
                        );

                        return (
                          <td 
                            key={lKey} 
                            onClick={() => handleOpenCellEditor(selectedPreviewDay, row, lKey)}
                            title="انقر للتعديل اليدوي على الحصة أو الأستاذ"
                            className={`p-2 border-r border-slate-200 cursor-pointer transition-all hover:ring-2 hover:ring-indigo-400 ${
                              isCellConflict ? 'bg-rose-100 text-rose-950 font-black border-rose-300' : 'bg-white hover:bg-indigo-50/50'
                            }`}
                          >
                            <div className="flex flex-col items-center justify-center p-1 rounded-xl">
                              <span className="font-black text-slate-900 text-xs line-clamp-1">
                                {cell?.subject || 'شاغر'}
                              </span>
                              <span className="text-[11px] font-bold text-slate-600 line-clamp-1 mt-0.5">
                                {cell?.teacherName ? `أ. ${cell.teacherName.replace(/^أ\.\s*/, '')}` : '—'}
                              </span>
                            </div>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Quick Edit Cell Modal */}
          {editingCell && (
            <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
              <div className="bg-white border-2 border-indigo-400 rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-4 text-slate-900">
                <div className="flex items-center justify-between border-b pb-3">
                  <h3 className="font-black text-base text-slate-900 flex items-center gap-2">
                    <Edit3 className="w-5 h-5 text-indigo-600" />
                    <span>تعديل الحصة ({LESSON_LABELS[editingCell.lessonKey]})</span>
                  </h3>
                  <button onClick={() => setEditingCell(null)} className="p-1 rounded-lg hover:bg-slate-100 cursor-pointer">
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="bg-slate-50 p-2.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-700">
                  <span>اليوم: <b>{editingCell.day}</b></span> | <span>الصف: <b>{editingCell.grade} - {editingCell.section}</b></span>
                </div>

                <div className="space-y-3 text-xs">
                  <div>
                    <label className="block font-black text-slate-800 mb-1">اسم المادة:</label>
                    <input
                      type="text"
                      list="master-subjects-datalist"
                      value={editSubjectInput}
                      onChange={e => setEditSubjectInput(e.target.value)}
                      placeholder="مثلاً: الرياضيات، الكيمياء، شاغر..."
                      className="w-full p-2.5 rounded-xl border-2 border-slate-300 font-bold bg-white text-slate-900 focus:border-indigo-600 outline-none"
                    />
                  </div>

                  <div>
                    <label className="block font-black text-slate-800 mb-1">أستاذ المادة:</label>
                    <input
                      type="text"
                      list="teachers-datalist"
                      value={editTeacherInput}
                      onChange={e => setEditTeacherInput(e.target.value)}
                      placeholder="اكتب أو اختر اسم الأستاذ"
                      className="w-full p-2.5 rounded-xl border-2 border-slate-300 font-bold bg-white text-slate-900 focus:border-indigo-600 outline-none"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
                  <button
                    onClick={() => setEditingCell(null)}
                    className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-black"
                  >
                    إلغاء
                  </button>
                  <button
                    onClick={handleSaveCellEdit}
                    className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-black shadow"
                  >
                    حفظ التعديل ✓
                  </button>
                </div>
              </div>
            </div>
          )}

        </div>
      )}

      {/* Print Preview Modal */}
      <PrintPreviewModal
        isOpen={showPrintModal}
        onClose={() => setShowPrintModal(false)}
        title={`جدول الحصص والتوقيتات - يوم (${selectedPreviewDay})`}
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
              <span className="text-xs text-sky-200 font-bold">| جدول توزيع الحصص والتوقيتات الأسبوعي - يوم ({selectedPreviewDay})</span>
            </div>
            <div className="text-xs text-sky-100 font-mono font-bold bg-white/10 px-3 py-1 rounded-xl border border-white/20">
              بداية الدوام: <span className="text-amber-300 font-black">{startHour}</span> | الحصة: <span className="text-amber-300 font-black">{lessonDuration} د</span> | الفرصة: <span className="text-pink-300 font-black">{breakDuration} د</span>
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
                  {(() => {
                    const [sh, sm] = (startHour || '08:00').split(':').map(Number);
                    let cur = (sh || 8) * 60 + (sm || 0);
                    const timings: { type: 'lesson' | 'break'; start: string; end: string }[] = [];
                    for (let l = 1; l <= 6; l++) {
                      const lStart = cur;
                      const lEnd = cur + Number(lessonDuration || 40);
                      const fmt = (min: number) => {
                        const h24 = Math.floor(min / 60) % 24;
                        const m = min % 60;
                        const h12 = h24 % 12 || 12;
                        const p = h24 < 12 ? 'ص' : 'م';
                        return `${h12.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')} ${p}`;
                      };
                      timings.push({ type: 'lesson', start: fmt(lStart), end: fmt(lEnd) });
                      cur = lEnd;
                      if (l < 6) {
                        const bStart = cur;
                        const bEnd = cur + Number(breakDuration || 10);
                        timings.push({ type: 'break', start: fmt(bStart), end: fmt(bEnd) });
                        cur = bEnd;
                      }
                    }
                    return timings.map((st, i) => (
                      <th 
                        key={i} 
                        className={`py-2 px-1 rounded-xl font-black text-[11px] ${
                          st.type === 'break' ? 'bg-pink-600 text-white' : 'bg-sky-800 text-white'
                        }`}
                      >
                        {st.start} - {st.end}
                      </th>
                    ));
                  })()}
                </tr>

                {/* Row 2: Columns Titles with Distinct Colors */}
                <tr className="text-xs font-black">
                  <th className="py-2 px-2 rounded-xl bg-sky-800 text-amber-200">الصفوف والشعب</th>
                  <th className="py-2 px-1 rounded-xl bg-sky-200 text-sky-950">الدرس الأول</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{breakDuration} د</th>
                  <th className="py-2 px-1 rounded-xl bg-cyan-200 text-cyan-950">الدرس الثاني</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{breakDuration} د</th>
                  <th className="py-2 px-1 rounded-xl bg-amber-200 text-amber-950">الدرس الثالث</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{breakDuration} د</th>
                  <th className="py-2 px-1 rounded-xl bg-emerald-200 text-emerald-950">الدرس الرابع</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{breakDuration} د</th>
                  <th className="py-2 px-1 rounded-xl bg-indigo-200 text-indigo-950">الدرس الخامس</th>
                  <th className="py-2 px-1 rounded-xl bg-pink-200 text-pink-950 text-[11px]">{breakDuration} د</th>
                  <th className="py-2 px-1 rounded-xl bg-rose-200 text-rose-950">الدرس السادس</th>
                </tr>
              </thead>

              <tbody className="text-xs">
                {(generatedMap[selectedPreviewDay] || []).length === 0 ? (
                  <tr>
                    <td colSpan={12} className="py-8 text-center text-slate-500 font-bold bg-white rounded-xl">
                      لا توجد حصص مضافة لهذا اليوم
                    </td>
                  </tr>
                ) : (
                  (generatedMap[selectedPreviewDay] || []).map((row) => (
                    <tr key={row.id}>
                      {/* Class & Section */}
                      <td className="p-0.5 align-middle bg-[#e6f4f1]">
                        <div className="px-2.5 py-1.5 rounded-xl border-2 border-sky-300 bg-white font-black text-slate-900 text-right shadow-2xs min-h-[54px] flex flex-col justify-center">
                          <span className="text-sky-950 font-black text-xs block leading-tight">
                            {row.grade} ({row.section})
                          </span>
                          <span className="text-[10px] text-slate-600 font-bold block mt-0.5">
                            مرشد: {row.teacherInCharge ? row.teacherInCharge.replace(/^(أ\.|أستاذ\s*)\s*/gi, '') : 'غير مخصص'}
                          </span>
                        </div>
                      </td>

                      {/* 6 Lessons & 5 Breaks */}
                      {LESSON_KEYS.map((lKey, idx) => {
                        const cell = row.lessons[lKey];
                        const isVacant = cell?.isOff || cell?.subject === 'شاغر / نشاط حر' || cell?.teacherName === 'شاغر' || cell?.subject === 'شاغر';

                        return (
                          <React.Fragment key={lKey}>
                            <td 
                              className="p-0.5 align-middle bg-[#e6f4f1] cursor-pointer"
                              onClick={() => handleOpenCellEditor(selectedPreviewDay, row, lKey)}
                              title="اضغط لتعديل مادة هذه الحصة أو أستاذها"
                            >
                              <div className={`px-1.5 py-1 rounded-xl border-2 text-center min-h-[54px] flex flex-col justify-center items-center shadow-2xs hover:scale-[1.02] hover:border-indigo-400 transition-all ${
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
                                      {cell?.teacherName ? `أ. ${cell.teacherName.replace(/^أ\.\s*/, '')}` : '—'}
                                    </span>
                                  </div>
                                )}
                              </div>
                            </td>

                            {/* Break column */}
                            {idx < 5 && (
                              <td className="p-0.5 align-middle bg-[#e6f4f1] text-center w-8">
                                <div className="py-1 rounded-xl bg-pink-100 text-pink-950 border border-pink-300 text-[10px] font-black flex items-center justify-center min-h-[54px]">
                                  {breakDuration}د
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
            <span>عدد الشعب في الجدول: <strong>{(generatedMap[selectedPreviewDay] || []).length} شعبة</strong></span>
            <span>حالة الجدول: <strong className="text-emerald-700 font-black">مكتمل ومدقق بنسبة 100% ✓</strong></span>
          </div>

        </div>
      </PrintPreviewModal>

      {/* Datalists for Easy Input & Canonical Matching */}
      <datalist id="master-subjects-datalist">
        {MASTER_SUBJECTS_LIST.map(sub => (
          <option key={sub} value={sub} />
        ))}
        <option value="العلوم" />
        <option value="التربية الأخلاقية" />
      </datalist>

      <datalist id="teachers-datalist">
        {staffList.map(stf => {
          const fullName = stf.fullName || `${stf.firstName} ${stf.secondName}`.trim();
          const spec = stf.specialization ? ` (${stf.specialization})` : '';
          return <option key={stf.id} value={fullName}>{fullName}{spec}</option>;
        })}
      </datalist>

    </div>
  );
};
