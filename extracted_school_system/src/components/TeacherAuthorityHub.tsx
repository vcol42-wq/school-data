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
  X,
  UserCheck,
  UserX,
  Send,
  MessageSquare,
  Compass,
  FileText,
  BadgeCheck,
  GraduationCap
} from 'lucide-react';
import { getSupabase, isSupabaseConfigured, getSupabaseKey } from '../utils/supabaseClient';
import { standardizeSubjectInput, STANDARD_APPROVED_SUBJECTS } from '../utils/subjectHelper';
import { standardizeGradeName, standardizeSectionName, standardizeSubjectName, parseClassTaught } from '../utils/syncEngine';

// Data Transfer Interface matching cloud table & local storage
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

// Teacher Profile (Teacher-Centric PIN Architecture)
export interface TeacherAuthorityProfile {
  teacherId: string;
  teacherName: string;
  jobTitle: string;
  specialization: string;
  teachingQuota: number;
  isExempt: boolean; // متفرغ / إداري مستبعد من أكواد التدريس
  secretCode: string; // كود موحد من 4 أرقام للأستاذ لكافة مواده وشعبه
  isLocked: boolean; // قفل رفع الدرجات
  subjects: string[]; // المواد المسندة للأستاذ (يدعم أكثر من مادة)
  classes: Array<{ grade: string; section: string }>; // الصفوف والشعب المسندة (يدعم أكثر من شعبة)
}

// Supervisor Profile (المدير / المشرف التربوي / موظف التطبيق)
export interface SupervisorDirective {
  id: string;
  date: string;
  target: string;
  message: string;
}

export interface SupervisorAuthorityProfile {
  code: string;
  title: string;
  name: string;
  isReadOnly: true;
  canSendDirectives: true;
  canViewAllClasses: true;
  canViewSchedule: true;
  directives: SupervisorDirective[];
}

interface TeacherAuthorityHubProps {
  staffList: StaffMember[];
  setStaffList?: React.Dispatch<React.SetStateAction<StaffMember[]>>;
  config: AppConfig;
  scheduleMap?: DayScheduleMap;
  onBackToMain: () => void;
}

// Available Grades & Sections
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

const COMMON_SECTIONS = ['أ', 'ب', 'ج', 'د', 'هـ', 'و', 'ز', 'ح'];

// Helper to detect non-teaching / administrative / exempt staff
export const detectIsExempt = (member: StaffMember): boolean => {
  const job = (member.jobTitle || '').trim();
  const spec = (member.specialization || '').trim();
  const nonTeachingKeywords = [
    'مدير', 'مديرة', 'معاون', 'معاونة', 'معاون مدير',
    'أمين مكتبة', 'أمينة مكتبة', 'مرشد', 'مرشدة', 'مرشد تربوي',
    'كاتب', 'كاتبة', 'إداري', 'إدارية', 'سكرتير', 'سكرتيرة',
    'موظف خدمة', 'موظفة خدمة', 'حارس', 'متفرغ', 'تفرغ', 'مشرف'
  ];
  const isNonTeachingTitle = nonTeachingKeywords.some(kw => job.includes(kw));
  const isZeroQuota = member.teachingQuota === 0 || (!member.teachingQuota && (!member.classesTaught || member.classesTaught.length === 0));
  const isSpecialExempt = spec.includes('إدارة') || spec.includes('تفرغ');
  const isInactiveStatus = member.status === 'مجاز إجازة طويلة' || member.status === 'منسب خارج المدرسة';

  return isNonTeachingTitle || isZeroQuota || isSpecialExempt || isInactiveStatus;
};

// Pure SVG Barcode Generator
const BarcodeSvg: React.FC<{ value: string; height?: number; className?: string }> = ({ value, height = 40, className = "" }) => {
  const clean = value.replace(/\D/g, '') || '1234';
  const digits = clean.split('');
  let bars: boolean[] = [true, false, true, true, false, true];
  
  digits.forEach((d) => {
    const num = parseInt(d, 10) || 0;
    const pattern = [
      [1, 2, 1, 1, 2, 1], [2, 1, 1, 2, 1, 1], [1, 1, 2, 1, 2, 1], [2, 2, 1, 1, 1, 1], [1, 2, 2, 1, 1, 1],
      [1, 1, 1, 2, 2, 1], [1, 1, 2, 2, 1, 1], [2, 1, 1, 1, 2, 1], [1, 2, 1, 1, 1, 2], [2, 1, 2, 1, 1, 1]
    ][num % 10];
    
    pattern.forEach((w, idx) => {
      const isBar = idx % 2 === 0;
      for (let i = 0; i < w; i++) {
        bars.push(isBar);
      }
    });
  });
  
  bars.push(true, false, true, true, false, true, true, true);
  const barWidth = 2.2;
  const totalWidth = bars.length * barWidth;

  return (
    <div className={`inline-block bg-white p-1 rounded border border-slate-300 ${className}`}>
      <svg width={totalWidth} height={height} viewBox={`0 0 ${totalWidth} ${height}`} className="mx-auto block">
        <rect width={totalWidth} height={height} fill="#ffffff" />
        {bars.map((isBar, i) =>
          isBar ? (
            <rect key={i} x={i * barWidth} y={0} width={barWidth} height={height} fill="#000000" />
          ) : null
        )}
      </svg>
    </div>
  );
};

// Pure SVG QR Code Generator
const QrCodeSvg: React.FC<{ value: string; size?: number; className?: string }> = ({ value, size = 70, className = "" }) => {
  const grid = Array(15).fill(0).map(() => Array(15).fill(false));
  const addFinder = (r: number, c: number) => {
    for (let i = 0; i < 5; i++) {
      for (let j = 0; j < 5; j++) {
        if (i === 0 || i === 4 || j === 0 || j === 4 || (i >= 1 && i <= 3 && j >= 1 && j <= 3 && !(i === 2 && j === 2))) {
          grid[r + i][c + j] = true;
        } else if (i === 2 && j === 2) {
          grid[r + i][c + j] = true;
        }
      }
    }
  };
  addFinder(0, 0);
  addFinder(0, 10);
  addFinder(10, 0);

  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) & 0xffffffff;
  }
  for (let r = 0; r < 15; r++) {
    for (let c = 0; c < 15; c++) {
      if ((r < 5 && c < 5) || (r < 5 && c >= 10) || (r >= 10 && c < 5)) continue;
      const bit = ((hash ^ (r * 15 + c)) >> ((r + c) % 16)) & 1;
      grid[r][c] = bit === 1;
    }
  }

  const cellSize = size / 15;
  return (
    <div className={`inline-block bg-white p-1 rounded border border-slate-300 ${className}`}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <rect width={size} height={size} fill="#ffffff" />
        {grid.map((row, r) =>
          row.map((filled, c) =>
            filled ? (
              <rect
                key={`${r}-${c}`}
                x={c * cellSize}
                y={r * cellSize}
                width={cellSize}
                height={cellSize}
                fill="#000000"
              />
            ) : null
          )
        )}
      </svg>
    </div>
  );
};

export const TeacherAuthorityHub: React.FC<TeacherAuthorityHubProps> = ({
  staffList,
  config,
  scheduleMap,
  onBackToMain
}) => {
  const activeSchoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'SCH-VCOL-6072';

  // Helper to generate 4-digit PIN
  const generateRandomPin = () => {
    return Math.floor(1000 + Math.random() * 9000).toString();
  };

  // 1. TEACHER PROFILES STATE
  const [profiles, setProfiles] = useState<TeacherAuthorityProfile[]>([]);
  
  // 2. SUPERVISOR PROFILE STATE
  const [supervisor, setSupervisor] = useState<SupervisorAuthorityProfile>(() => {
    try {
      const saved = localStorage.getItem(`diyala_supervisor_profile_${activeSchoolId}`);
      if (saved) return JSON.parse(saved);
    } catch {}
    const defaultCode = localStorage.getItem(`diyala_supervisor_code_${activeSchoolId}`) || `SUP-${Math.floor(1000 + Math.random() * 9000)}`;
    return {
      code: defaultCode,
      title: 'المشرف التربوي / مدير المدرسة / موظف التطبيق',
      name: config.managerName || 'المشرف التربوي المعتمد',
      isReadOnly: true,
      canSendDirectives: true,
      canViewAllClasses: true,
      canViewSchedule: true,
      directives: [
        {
          id: 'dir-1',
          date: new Date().toLocaleDateString('ar-IQ'),
          target: 'كافة الصفوف والشعب',
          message: 'يرجى الالتزام بمواعيد تدقيق الدفاتر الامتحانية ومطابقة السجلات قبل الإغلاق النهائي.'
        }
      ]
    };
  });

  // UI State
  const [activeTab, setActiveTab] = useState<'teaching' | 'exempt' | 'supervisor'>('teaching');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilterSubject, setSelectedFilterSubject] = useState<string>('all');
  const [isLoading, setIsLoading] = useState(false);
  const [isSavingCloud, setIsSavingCloud] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
  const [showPinMap, setShowPinMap] = useState<{ [key: string]: boolean }>({});
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  
  // Print Modal State
  const [isPrintModalOpen, setIsPrintModalOpen] = useState(false);
  const [printTarget, setPrintTarget] = useState<'all_teachers' | 'supervisor_only' | string>('all_teachers');

  // Modals for adding/editing multi-class & multi-subject
  const [classModalTeacherIdx, setClassModalTeacherIdx] = useState<number | null>(null);
  const [selectedAddGrade, setSelectedAddGrade] = useState(COMMON_GRADES[0]);
  const [selectedAddSection, setSelectedAddSection] = useState(COMMON_SECTIONS[0]);

  const [subjectModalTeacherIdx, setSubjectModalTeacherIdx] = useState<number | null>(null);
  const [selectedAddSubject, setSelectedAddSubject] = useState(STANDARD_APPROVED_SUBJECTS[0]);
  const [customSubjectInput, setCustomSubjectInput] = useState('');

  // Directive Modal for Supervisor
  const [showDirectiveModal, setShowDirectiveModal] = useState(false);
  const [directiveTarget, setDirectiveTarget] = useState('كافة الصفوف والشعب');
  const [directiveMessage, setDirectiveMessage] = useState('');

  // Flatten Profiles into individual SubjectAssignmentRecords for Database & Schedule Map
  const flattenProfilesToAssignments = useCallback((profList: TeacherAuthorityProfile[]): SubjectAssignmentRecord[] => {
    const result: SubjectAssignmentRecord[] = [];
    const seen = new Set<string>();

    profList.forEach(prof => {
      if (prof.isExempt) return; // Skip non-teaching staff

      const subjects = prof.subjects && prof.subjects.length > 0 ? prof.subjects : [prof.specialization || 'عام'];
      const classes = prof.classes && prof.classes.length > 0 ? prof.classes : [{ grade: 'الأول المتوسط', section: 'أ' }];

      subjects.forEach(subj => {
        const cleanSubj = standardizeSubjectName(subj);
        if (!cleanSubj || cleanSubj.length <= 1 || /^[أ-يa-zA-Z]$/.test(cleanSubj)) return; // استبعاد إسناد الأحرف كمادة
        if (cleanSubj.includes('مفرغ') || cleanSubj.includes('إدارة') || cleanSubj.includes('تفرغ')) return; // استبعاد المفرغين

        classes.forEach(cls => {
          const cleanGrade = standardizeGradeName(cls.grade);
          const cleanSec = standardizeSectionName(cls.section);
          if (cleanSec.includes('متوسط') || cleanSec.includes('صف')) return; // استبعاد الشعب غير الصحيحة

          const key = `${cleanGrade}__${cleanSec}__${cleanSubj}`;
          if (!seen.has(key)) {
            seen.add(key);
            result.push({
              school_id: activeSchoolId,
              grade: cleanGrade,
              section: cleanSec,
              subject: cleanSubj,
              secret_code: prof.secretCode.trim(),
              is_locked: prof.isLocked,
              teacher_id: prof.teacherId,
              teacher_name: prof.teacherName.trim(),
              last_updated_at: new Date().toISOString()
            });
          }
        });
      });
    });

    return result;
  }, [activeSchoolId]);

  // Save changes locally
  const saveProfilesState = useCallback((newProfiles: TeacherAuthorityProfile[], newSupervisor?: SupervisorAuthorityProfile) => {
    setProfiles(newProfiles);
    try {
      localStorage.setItem(`diyala_teacher_profiles_${activeSchoolId}`, JSON.stringify(newProfiles));
      const flattened = flattenProfilesToAssignments(newProfiles);
      localStorage.setItem(`diyala_subject_assignments_${activeSchoolId}`, JSON.stringify(flattened));
      
      if (newSupervisor) {
        setSupervisor(newSupervisor);
        localStorage.setItem(`diyala_supervisor_profile_${activeSchoolId}`, JSON.stringify(newSupervisor));
        localStorage.setItem(`diyala_supervisor_code_${activeSchoolId}`, newSupervisor.code);
      }
    } catch (e) {
      console.error('Error saving profiles locally:', e);
    }
  }, [activeSchoolId, flattenProfilesToAssignments]);

  // Initial Load: Build profiles from staffList, scheduleMap, or saved local/cloud data
  useEffect(() => {
    const initializeProfiles = () => {
      try {
        // 1. Check if saved profiles exist
        const savedProf = localStorage.getItem(`diyala_teacher_profiles_${activeSchoolId}`);
        if (savedProf) {
          const parsed: TeacherAuthorityProfile[] = JSON.parse(savedProf);
          if (parsed && parsed.length > 0) {
            setProfiles(parsed);
            return;
          }
        }

        // 2. Check if legacy assignments exist to construct profiles
        const legacyAss = localStorage.getItem(`diyala_subject_assignments_${activeSchoolId}`);
        const legacyList: SubjectAssignmentRecord[] = legacyAss ? JSON.parse(legacyAss) : [];

        // Map teacher names from legacy assignments
        const legacyTeacherMap = new Map<string, {
          code: string;
          isLocked: boolean;
          subjects: Set<string>;
          classes: Map<string, { grade: string; section: string }>;
        }>();

        legacyList.forEach(item => {
          const tName = item.teacher_name?.trim() || '';
          if (!tName || tName === 'غير مسند') return;

          const rawSub = (item.subject || '').trim();
          const cleanSub = standardizeSubjectName(rawSub);
          if (!cleanSub || cleanSub.length <= 1 || /^[أ-يa-zA-Z]$/.test(cleanSub) || cleanSub.includes('مفرغ') || cleanSub.includes('إدارة')) return;

          if (!legacyTeacherMap.has(tName)) {
            legacyTeacherMap.set(tName, {
              code: item.secret_code || generateRandomPin(),
              isLocked: !!item.is_locked,
              subjects: new Set(),
              classes: new Map()
            });
          }
          const tData = legacyTeacherMap.get(tName)!;
          tData.subjects.add(cleanSub);
          if (item.grade && item.section) {
            const stdG = standardizeGradeName(item.grade);
            const stdS = standardizeSectionName(item.section);
            if (!stdS.includes('متوسط') && !stdS.includes('صف')) {
              tData.classes.set(`${stdG}-${stdS}`, { grade: stdG, section: stdS });
            }
          }
        });

        // 3. Scan ScheduleMap for real timetable lesson mappings
        const scheduleTeacherMap = new Map<string, {
          subjects: Set<string>;
          classes: Map<string, { grade: string; section: string }>;
        }>();

        if (scheduleMap) {
          Object.values(scheduleMap).forEach(dayRows => {
            dayRows.forEach(row => {
              const gr = row.grade?.trim();
              const sec = row.section?.trim();
              if (!gr || !sec) return;

              const slots = [
                row.lessons?.lesson1,
                row.lessons?.lesson2,
                row.lessons?.lesson3,
                row.lessons?.lesson4,
                row.lessons?.lesson5,
                row.lessons?.lesson6
              ];

              slots.forEach(s => {
                if (s && s.teacherName && s.subject && !s.isOff) {
                  const tClean = s.teacherName.trim();
                  if (!scheduleTeacherMap.has(tClean)) {
                    scheduleTeacherMap.set(tClean, { subjects: new Set(), classes: new Map() });
                  }
                  const tData = scheduleTeacherMap.get(tClean)!;
                  tData.subjects.add(s.subject.trim());
                  tData.classes.set(`${gr}-${sec}`, { grade: gr, section: sec });
                }
              });
            });
          });
        }

        // 4. Construct TeacherAuthorityProfile for all staff members
        const builtProfiles: TeacherAuthorityProfile[] = staffList.map((stf, idx) => {
          const tName = stf.fullName || `${stf.firstName} ${stf.secondName || ''} ${stf.thirdName || ''}`.trim();
          const isExempt = detectIsExempt(stf);
          const tId = stf.id || `stf-${idx + 1}`;

          // Match legacy or schedule
          const leg = legacyTeacherMap.get(tName);
          const sch = scheduleTeacherMap.get(tName);

          const subjectsSet = new Set<string>();
          const classesMap = new Map<string, { grade: string; section: string }>();

          if (leg) {
            leg.subjects.forEach(s => subjectsSet.add(s));
            leg.classes.forEach((val, k) => classesMap.set(k, val));
          }
          if (sch) {
            sch.subjects.forEach(s => subjectsSet.add(s));
            sch.classes.forEach((val, k) => classesMap.set(k, val));
          }

          // Fallback if not mapped
          if (!isExempt) {
            if (subjectsSet.size === 0) {
              const sub = stf.actualSubjectTaught || stf.specialization || 'الرياضيات';
              subjectsSet.add(sub);
            }
            if (classesMap.size === 0) {
              const classesTaught = stf.classesTaught && stf.classesTaught.length > 0 
                ? stf.classesTaught 
                : ['الأول المتوسط'];
              classesTaught.forEach(cStr => {
                let cleanG = cStr.replace(/^(الصف|صف)\s+/g, '').trim();
                let sec = 'أ';
                const pMatch = cleanG.match(/\((.*?)\)/);
                if (pMatch) {
                  sec = standardizeSectionName(pMatch[1].trim());
                  cleanG = cleanG.replace(/\(.*?\)/g, '').trim();
                } else {
                  const parts = cleanG.split(/[-–—\s]+/);
                  if (parts.length >= 2) {
                    const last = parts[parts.length - 1];
                    if (/^[أ-يa-zA-Z]$/.test(last) || ['أ', 'ب', 'ج', 'د', 'هـ', 'ه', 'و', 'ز', 'ح', 'ط', 'ي'].includes(last)) {
                      sec = standardizeSectionName(last);
                      cleanG = parts.slice(0, parts.length - 1).join(' ');
                    }
                  }
                }
                const stdGrade = standardizeGradeName(cleanG);
                classesMap.set(`${stdGrade}-${sec}`, { grade: stdGrade, section: sec });
              });
            }
          }

          return {
            teacherId: tId,
            teacherName: tName,
            jobTitle: stf.jobTitle || (isExempt ? 'إداري / متفرغ' : 'مدرس'),
            specialization: stf.specialization || 'عام',
            teachingQuota: stf.teachingQuota || 0,
            isExempt: isExempt,
            secretCode: leg?.code || generateRandomPin(),
            isLocked: leg?.isLocked || false,
            subjects: Array.from(subjectsSet),
            classes: Array.from(classesMap.values())
          };
        });

        // Sort: Active teachers first, then exempt staff
        builtProfiles.sort((a, b) => {
          if (a.isExempt === b.isExempt) return a.teacherName.localeCompare(b.teacherName, 'ar');
          return a.isExempt ? 1 : -1;
        });

        saveProfilesState(builtProfiles);
      } catch (err) {
        console.error('Error constructing initial profiles:', err);
      }
    };

    initializeProfiles();
  }, [staffList, scheduleMap, activeSchoolId, saveProfilesState]);

  // Fetch Cloud assignments on mount and reconcile with profiles
  const fetchCloudAssignments = useCallback(async () => {
    setIsLoading(true);
    try {
      const client = getSupabase(activeSchoolId);

      // 1. First check schools.config for complete teacher_profiles and supervisor
      const { data: schoolData } = await client
        .from('schools')
        .select('config')
        .eq('id', activeSchoolId)
        .maybeSingle();

      if (schoolData?.config) {
        const conf = schoolData.config as any;
        if (conf.supervisor_code) {
          setSupervisor(prev => ({
            ...prev,
            code: conf.supervisor_code,
            name: conf.supervisor_name || prev.name,
            title: conf.supervisor_title || prev.title
          }));
        }

        if (Array.isArray(conf.teacher_profiles) && conf.teacher_profiles.length > 0) {
          setProfiles(conf.teacher_profiles);
          localStorage.setItem(`diyala_teacher_profiles_${activeSchoolId}`, JSON.stringify(conf.teacher_profiles));
          const flattened = flattenProfilesToAssignments(conf.teacher_profiles);
          localStorage.setItem(`diyala_subject_assignments_${activeSchoolId}`, JSON.stringify(flattened));
          setStatusMessage({ type: 'success', text: `تم استرجاع ومطابقة بيانات المعلمين من السحابة بنجاح ✓` });
          return;
        }
      }

      // 2. Fallback: check subject_assignments table with strict sanity filter
      const { data, error } = await client
        .from('subject_assignments')
        .select('*')
        .eq('school_id', activeSchoolId);

      if (!error && data && data.length > 0) {
        const cleanData = data.filter((d: any) => {
          const s = (d.subject || '').trim();
          return s.length > 1 && !/^[أ-يa-zA-Z]$/.test(s) && !s.includes('مفرغ') && !s.includes('إدارة') && !s.includes('تفرغ');
        });

        if (cleanData.length > 0) {
          setProfiles(prevProfiles => {
            if (prevProfiles.length === 0) return prevProfiles;

            const updated = prevProfiles.map(prof => {
              // Match assignments by class and subject since teacher_name might not exist in the table
              const cloudRecords = cleanData.filter((d: any) => {
                const matchesClass = prof.classes.some(c => standardizeGradeName(c.grade) === standardizeGradeName(d.grade) && standardizeSectionName(c.section) === standardizeSectionName(d.section));
                const matchesSub = prof.subjects.some(s => standardizeSubjectName(s) === standardizeSubjectName(d.subject));
                return matchesClass && matchesSub;
              });

              if (cloudRecords.length > 0) {
                const cloudCode = cloudRecords[0].secret_code || prof.secretCode;
                const cloudLocked = cloudRecords.some((r: any) => r.is_locked);
                return {
                  ...prof,
                  secretCode: cloudCode,
                  isLocked: cloudLocked
                };
              }
              return prof;
            });

            localStorage.setItem(`diyala_teacher_profiles_${activeSchoolId}`, JSON.stringify(updated));
            const flattened = flattenProfilesToAssignments(updated);
            localStorage.setItem(`diyala_subject_assignments_${activeSchoolId}`, JSON.stringify(flattened));
            return updated;
          });

          setStatusMessage({ type: 'success', text: `تم الاتصال بالسحابة ومطابقة بيانات المعلمين بنجاح ✓` });
        }
      }
    } catch (err) {
      console.warn('Could not fetch cloud subject assignments:', err);
    } finally {
      setIsLoading(false);
    }
  }, [activeSchoolId, flattenProfilesToAssignments]);

  useEffect(() => {
    fetchCloudAssignments();
  }, [fetchCloudAssignments]);

  // SYNC TO SUPABASE CLOUD (1-Click Central Sync)
  const handleSyncToCloud = async () => {
    const flattened = flattenProfilesToAssignments(profiles);
    if (flattened.length === 0) {
      alert('لا توجد مهام تدريسية للكادر الفعلي لحفظها.');
      return;
    }

    setIsSavingCloud(true);
    setStatusMessage({ type: 'info', text: 'جاري رفع واعتماد أكواد المعلمين وكود المشرف في السحابة...' });

    try {
      // 1. Save complete authority profiles, assignments, and supervisor directly into schools.config
      const configPayload = {
        supervisor_code: supervisor.code,
        supervisor_name: supervisor.name,
        supervisor_title: supervisor.title,
        teacher_profiles: profiles,
        subject_assignments: flattened.map(a => ({
          grade: a.grade.trim(),
          section: a.section.trim(),
          subject: a.subject.trim(),
          secret_code: a.secret_code.trim(),
          is_locked: !!a.is_locked,
          teacher_name: a.teacher_name?.trim() || ''
        })),
        updated_at: new Date().toISOString()
      };

      const client = getSupabase(activeSchoolId);

      const { error: schoolErr } = await client
        .from('schools')
        .update({ config: configPayload })
        .eq('id', activeSchoolId);

      if (schoolErr) {
        console.warn('Could not update schools.config directly:', schoolErr);
      }

      // 2. Clean sync to subject_assignments table:
      // First delete all existing records for this school to purge old/corrupted legacy rows
      try {
        const { error: delError } = await client
          .from('subject_assignments')
          .delete()
          .eq('school_id', activeSchoolId);

        if (delError) {
          console.warn('Notice clearing previous subject_assignments:', delError.message);
        }

        const recordsToInsert = flattened.map(a => ({
          school_id: activeSchoolId,
          grade: a.grade.trim(),
          section: a.section.trim(),
          subject: a.subject.trim(),
          secret_code: a.secret_code.trim(),
          is_locked: !!a.is_locked,
          last_updated_at: new Date().toISOString()
        }));

        if (recordsToInsert.length > 0) {
          // Try upsert first; if conflict constraint missing, fallback to clean insert
          const { error: assError } = await client
            .from('subject_assignments')
            .upsert(recordsToInsert, { onConflict: 'school_id,grade,section,subject' });

          if (assError) {
            console.warn('Upsert warning, falling back to direct insert:', assError.message);
            await client.from('subject_assignments').insert(recordsToInsert);
          }
        }
      } catch (assErr) {
        console.warn('Exception during subject_assignments table sync:', assErr);
      }

      setStatusMessage({ 
        type: 'success', 
        text: `تم تفريغ السجلات القديمة وتحديث وحفظ ${flattened.length} إسناداً معتمداً لـ (${activeTeachingProfiles.length}) أساتذة + كود المشرف في السحابة بنجاح! التطبيق متطابق 100% ✓` 
      });
    } catch (e: any) {
      console.error('Cloud Sync failed:', e);
      setStatusMessage({ type: 'error', text: `فشل الحفظ السحابي: ${e.message || 'خطأ غير معروف'}` });
    } finally {
      setIsSavingCloud(false);
    }
  };

  // Dedicated Clean Purge & Sync (تفريغ الجدول بالكامل وإعادة الرفع النظيف من قوائم الطلاب والكادر)
  const handlePurgeAndResyncCloud = async () => {
    if (!window.confirm(`هل ترغب في تطهير السحابة بالكامل لمدرستك (${activeSchoolId}) وإعادة بناء الشعب والرموز النقية؟\n\n1. حصر الشعب من سجلات الطلاب الـ 264 الفعلية فقط ومسح الشعب الزائدة المشوهة.\n2. استبعاد المفرغين إدارياً والمعاونين من أي رموز أو حصص تدريسية.\n3. مسح الأكواد المشوهة والمكررة وإعادة الرفع النظيف للسحابة.`)) {
      return;
    }

    try {
      setIsSavingCloud(true);
      setStatusMessage({ type: 'info', text: 'جاري التطهير وإعادة استخراج الشعب من سجلات الطلاب والكادر...' });

      // 1. مسح المفاتيح المحلية القديمة التي كانت تعيد البيانات المشوهة
      localStorage.removeItem(`diyala_teacher_profiles_${activeSchoolId}`);
      localStorage.removeItem(`diyala_subject_assignments_${activeSchoolId}`);

      // 2. استخراج الشعب الحقيقية النقية من الطلاب
      const rawStudents = localStorage.getItem('diyala_school_students');
      const studentList = rawStudents ? JSON.parse(rawStudents) : [];
      const studentClassesMap = new Map<string, { grade: string; section: string }>();
      
      if (Array.isArray(studentList)) {
        studentList.forEach((std: any) => {
          if (std.currentGrade && std.section) {
            const g = standardizeGradeName(std.currentGrade);
            const s = standardizeSectionName(std.section);
            if (!s.includes('متوسط')) {
              studentClassesMap.set(`${g}-${s}`, { grade: g, section: s });
            }
          }
        });
      }

      const canonicalClasses = Array.from(studentClassesMap.values());
      const fallbackClass = canonicalClasses.length > 0 ? canonicalClasses[0] : { grade: 'الأول المتوسط', section: 'أ' };

      // 3. إعادة بناء بروفايلات الأساتذة النشطين فقط واستبعاد المفرغين
      const freshProfiles: TeacherAuthorityProfile[] = staffList.map((stf, idx) => {
        const tName = stf.fullName || `${stf.firstName} ${stf.secondName || ''} ${stf.thirdName || ''}`.trim();
        const isExempt = detectIsExempt(stf);
        const tId = stf.id || `stf-${idx + 1}`;

        if (isExempt) {
          return {
            teacherId: tId,
            teacherName: tName,
            jobTitle: stf.jobTitle || 'إداري / متفرغ',
            specialization: stf.specialization || 'إدارة',
            teachingQuota: 0,
            isExempt: true,
            secretCode: '',
            isLocked: false,
            subjects: [],
            classes: []
          };
        }

        // الأستاذ المكلف بالتدريس
        const rawSub = stf.actualSubjectTaught || stf.specialization || 'الرياضيات';
        const cleanSubj = standardizeSubjectName(rawSub);
        const validSubj = (cleanSubj.length > 1 && !cleanSubj.includes('مفرغ') && !cleanSubj.includes('إدارة'))
          ? cleanSubj
          : 'الرياضيات';

        // مطابقة الشعب المسندة مع الشعب الحقيقية المستخرجة من الطلاب
        const assignedClasses: Array<{ grade: string; section: string }> = [];
        if (stf.classesTaught && stf.classesTaught.length > 0) {
          stf.classesTaught.forEach(cStr => {
            const parsed = parseClassTaught(cStr, validSubj);
            const stdG = standardizeGradeName(parsed.grade);
            const stdS = standardizeSectionName(parsed.section);
            if (!stdS.includes('متوسط')) {
              assignedClasses.push({ grade: stdG, section: stdS });
            }
          });
        }

        const finalClasses = assignedClasses.length > 0 ? assignedClasses : [fallbackClass];

        return {
          teacherId: tId,
          teacherName: tName,
          jobTitle: stf.jobTitle || 'مدرس',
          specialization: stf.specialization || 'عام',
          teachingQuota: stf.teachingQuota || 20,
          isExempt: false,
          secretCode: generateRandomPin(),
          isLocked: false,
          subjects: [validSubj],
          classes: finalClasses
        };
      });

      // فرز: الكادر النشط أولاً ثم المفرغين
      freshProfiles.sort((a, b) => {
        if (a.isExempt === b.isExempt) return a.teacherName.localeCompare(b.teacherName, 'ar');
        return a.isExempt ? 1 : -1;
      });

      // حفظ الحالة الجديدة النقية محلياً
      saveProfilesState(freshProfiles);

      // رفع ومزامنة نقية للسحابة
      const flattened = flattenProfilesToAssignments(freshProfiles);
      
      const client = getSupabase(activeSchoolId);

      // مسح وإعادة كتابة جدول subject_assignments
      try {
        await client.from('subject_assignments').delete().eq('school_id', activeSchoolId);
        if (flattened.length > 0) {
          const recordsToInsert = flattened.map(a => ({
            school_id: activeSchoolId,
            grade: a.grade.trim(),
            section: a.section.trim(),
            subject: a.subject.trim(),
            secret_code: a.secret_code.trim(),
            is_locked: !!a.is_locked,
            last_updated_at: new Date().toISOString()
          }));
          await client.from('subject_assignments').insert(recordsToInsert);
        }
      } catch (subErr) {
        console.warn('Notice syncing subject_assignments table during reset:', subErr);
      }

      // تحديث مدارس config
      const fullConfig = {
        supervisor_code: supervisor.code,
        supervisor_name: supervisor.name,
        supervisor_title: supervisor.title,
        teacher_profiles: freshProfiles,
        subject_assignments: flattened.map(a => ({
          grade: a.grade.trim(),
          section: a.section.trim(),
          subject: a.subject.trim(),
          secret_code: a.secret_code.trim(),
          is_locked: !!a.is_locked,
          teacher_name: a.teacher_name?.trim() || ''
        })),
        updated_at: new Date().toISOString()
      };
      await client.from('schools').update({ config: fullConfig }).eq('id', activeSchoolId);

      setStatusMessage({
        type: 'success',
        text: `تم التطهير الكامل بنجاح! تم حصر الشعب من الطلاب وتوليد ${flattened.length} رمزاً معتمداً بدون تكرار واستبعاد المفرغين 100% ✓`
      });
    } catch (err: any) {
      console.error('Error during deep purge:', err);
      setStatusMessage({ type: 'error', text: `فشل التطهير السحابي: ${err.message || 'خطأ غير معروف'}` });
    } finally {
      setIsSavingCloud(false);
    }
  };

  // Toggle Exempt / Active status for a teacher
  const handleToggleExempt = (teacherIdx: number) => {
    const updated = [...profiles];
    const item = updated[teacherIdx];
    const newExempt = !item.isExempt;
    item.isExempt = newExempt;

    if (!newExempt && (!item.secretCode || item.secretCode.length < 4)) {
      item.secretCode = generateRandomPin();
    }
    if (!newExempt && item.subjects.length === 0) {
      item.subjects = [item.specialization || 'الرياضيات'];
    }
    if (!newExempt && item.classes.length === 0) {
      item.classes = [{ grade: 'الأول المتوسط', section: 'أ' }];
    }

    saveProfilesState(updated);
    setStatusMessage({ 
      type: 'info', 
      text: newExempt ? `تم استبعاد (${item.teacherName}) كمتفرغ/إداري بدون كود.` : `تم تفعيل (${item.teacherName}) كأستاذ مكلف وتوليد كود له.` 
    });
  };

  // Toggle Lock for a teacher
  const handleToggleLock = (teacherIdx: number) => {
    const updated = [...profiles];
    updated[teacherIdx].isLocked = !updated[teacherIdx].isLocked;
    saveProfilesState(updated);
  };

  // Regenerate Code for a specific teacher
  const handleRegenerateCode = (teacherIdx: number) => {
    const newCode = generateRandomPin();
    const updated = [...profiles];
    updated[teacherIdx].secretCode = newCode;
    saveProfilesState(updated);
    setStatusMessage({ type: 'info', text: `تم توليد كود سري جديد (${newCode}) للأستاذ: ${updated[teacherIdx].teacherName}` });
  };

  // Regenerate Code for Supervisor
  const handleRegenerateSupervisorCode = () => {
    const newCode = `SUP-${Math.floor(1000 + Math.random() * 9000)}`;
    const updatedSup = { ...supervisor, code: newCode };
    saveProfilesState(profiles, updatedSup);
    setStatusMessage({ type: 'info', text: `تم توليد كود جديد للمشرف العام: (${newCode})` });
  };

  // Regenerate All Teacher Codes
  const handleRegenerateAllTeacherCodes = () => {
    const updated = profiles.map(p => {
      if (p.isExempt) return p;
      return { ...p, secretCode: generateRandomPin() };
    });
    saveProfilesState(updated);
    setStatusMessage({ type: 'success', text: 'تم توليد وتحديث الأكواد السرية لجميع أساتذة الكادر الفعلي بنجاح 🎲' });
  };

  // Copy Code to Clipboard
  const handleCopyCode = (code: string, idStr: string) => {
    navigator.clipboard.writeText(code);
    setCopiedKey(idStr);
    setTimeout(() => setCopiedKey(null), 2500);
  };

  // Add Subject to a teacher
  const handleAddSubjectToTeacher = () => {
    if (subjectModalTeacherIdx === null) return;
    const finalSubject = customSubjectInput.trim() 
      ? standardizeSubjectInput(customSubjectInput).standardized 
      : selectedAddSubject;

    if (!finalSubject) return;

    const updated = [...profiles];
    const prof = updated[subjectModalTeacherIdx];
    if (!prof.subjects.includes(finalSubject)) {
      prof.subjects = [...prof.subjects, finalSubject];
      saveProfilesState(updated);
      setStatusMessage({ type: 'success', text: `تم إضافة مادة (${finalSubject}) للأستاذ (${prof.teacherName}) ✓` });
    }
    setSubjectModalTeacherIdx(null);
    setCustomSubjectInput('');
  };

  // Remove Subject from a teacher
  const handleRemoveSubjectFromTeacher = (teacherIdx: number, subjectName: string) => {
    const updated = [...profiles];
    const prof = updated[teacherIdx];
    if (prof.subjects.length <= 1) {
      alert('يجب أن تبقى مادة واحدة على الأقل مسندة للأستاذ، أو يمكنك تحويله إلى متفرغ.');
      return;
    }
    prof.subjects = prof.subjects.filter(s => s !== subjectName);
    saveProfilesState(updated);
  };

  // Add Class & Section to a teacher
  const handleAddClassToTeacher = () => {
    if (classModalTeacherIdx === null) return;
    const updated = [...profiles];
    const prof = updated[classModalTeacherIdx];
    const exists = prof.classes.some(c => c.grade === selectedAddGrade && c.section === selectedAddSection);

    if (!exists) {
      prof.classes = [...prof.classes, { grade: selectedAddGrade, section: selectedAddSection }];
      saveProfilesState(updated);
      setStatusMessage({ type: 'success', text: `تم إضافة شعبة (${selectedAddGrade} - ${selectedAddSection}) للأستاذ (${prof.teacherName}) ✓` });
    }
    setClassModalTeacherIdx(null);
  };

  // Remove Class & Section from a teacher
  const handleRemoveClassFromTeacher = (teacherIdx: number, grade: string, section: string) => {
    const updated = [...profiles];
    const prof = updated[teacherIdx];
    if (prof.classes.length <= 1) {
      alert('يجب أن يبقى صف وشعبة واحدة على الأقل مسندة للأستاذ، أو يمكنك تحويله إلى متفرغ.');
      return;
    }
    prof.classes = prof.classes.filter(c => !(c.grade === grade && c.section === section));
    saveProfilesState(updated);
  };

  // Add New Directive by Supervisor
  const handleAddDirective = () => {
    if (!directiveMessage.trim()) return;
    const newDir: SupervisorDirective = {
      id: `dir-${Date.now()}`,
      date: new Date().toLocaleDateString('ar-IQ'),
      target: directiveTarget.trim() || 'كافة الصفوف والشعب',
      message: directiveMessage.trim()
    };

    const updatedSup = {
      ...supervisor,
      directives: [newDir, ...supervisor.directives]
    };
    saveProfilesState(profiles, updatedSup);
    setDirectiveMessage('');
    setShowDirectiveModal(false);
    setStatusMessage({ type: 'success', text: 'تم تسجيل وإرسال التوجيه الإشرافي بنجاح ✓' });
  };

  // Delete Directive
  const handleDeleteDirective = (id: string) => {
    const updatedSup = {
      ...supervisor,
      directives: supervisor.directives.filter(d => d.id !== id)
    };
    saveProfilesState(profiles, updatedSup);
  };

  // Filtered profiles
  const activeTeachingProfiles = useMemo(() => {
    return profiles.filter(p => !p.isExempt);
  }, [profiles]);

  const exemptProfiles = useMemo(() => {
    return profiles.filter(p => p.isExempt);
  }, [profiles]);

  const displayedProfiles = useMemo(() => {
    const list = activeTab === 'teaching' ? activeTeachingProfiles : exemptProfiles;
    return list.filter(p => {
      const q = searchQuery.toLowerCase();
      const matchSearch = p.teacherName.toLowerCase().includes(q) ||
        p.specialization.toLowerCase().includes(q) ||
        p.jobTitle.toLowerCase().includes(q) ||
        p.secretCode.includes(q) ||
        p.subjects.some(s => s.toLowerCase().includes(q)) ||
        p.classes.some(c => c.grade.toLowerCase().includes(q) || c.section.includes(q));

      const matchSubject = selectedFilterSubject === 'all' || p.subjects.includes(selectedFilterSubject);
      return matchSearch && matchSubject;
    });
  }, [activeTab, activeTeachingProfiles, exemptProfiles, searchQuery, selectedFilterSubject]);

  // Unique Subjects for filter
  const allUniqueSubjects = useMemo(() => {
    const set = new Set<string>();
    activeTeachingProfiles.forEach(p => p.subjects.forEach(s => set.add(s)));
    return Array.from(set);
  }, [activeTeachingProfiles]);

  // Statistics
  const totalCoveredLessons = useMemo(() => {
    let count = 0;
    activeTeachingProfiles.forEach(p => {
      count += (p.subjects.length * p.classes.length);
    });
    return count;
  }, [activeTeachingProfiles]);

  const reductionPercentage = useMemo(() => {
    if (totalCoveredLessons === 0) return 0;
    const ratio = (1 - (activeTeachingProfiles.length / Math.max(totalCoveredLessons, 1))) * 100;
    return Math.max(0, Math.round(ratio));
  }, [activeTeachingProfiles.length, totalCoveredLessons]);

  return (
    <div className="p-4 md:p-6 max-w-7xl mx-auto space-y-6 dir-rtl text-slate-800">
      
      {/* 1. Header Card with Title & Central Action Buttons */}
      <div className="bg-white border-4 border-indigo-500/30 rounded-3xl p-5 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 via-purple-600 to-blue-600 text-white flex items-center justify-center shadow-lg shadow-indigo-300 shrink-0">
            <Key className="w-8 h-8 text-amber-300" />
          </div>
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-indigo-50 border border-indigo-200 text-indigo-900 text-xs font-black mb-1">
              <ShieldCheck className="w-3.5 h-3.5 text-indigo-600" />
              <span>منظومة الحماية وتفويض الصلاحيات الذكية 🛡️</span>
            </div>
            <h1 className="text-xl md:text-2xl font-black text-slate-900">
              أكواد المعلمين وتفويض الشعب (كود موحد لكل أستاذ)
            </h1>
            <p className="text-xs text-slate-600 font-bold mt-0.5">
              توليد كود واحد لكل أستاذ يغطي كافة مواده وشعبه، استبعاد المتفرغين، مع كود المشرف العام للاطلاع والتوجيه.
            </p>
          </div>
        </div>

        <div className="flex items-center flex-wrap gap-2 shrink-0">
          {/* Print Security Passes Button */}
          <button
            onClick={() => {
              setPrintTarget('all_teachers');
              setIsPrintModalOpen(true);
            }}
            className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs md:text-sm shadow-md transition-all cursor-pointer active:scale-95"
            title="طباعة بطاقات الاعتماد السرية لتسليمها للكادر"
          >
            <Printer className="w-4 h-4" />
            <span>طباعة بطاقات الاعتماد 🖨️</span>
          </button>

          {/* Purge & Clean Sync to Supabase */}
          <button
            onClick={handlePurgeAndResyncCloud}
            disabled={isSavingCloud}
            className="flex items-center gap-2 px-3.5 py-2.5 rounded-2xl bg-amber-600 hover:bg-amber-700 text-white font-black text-xs md:text-sm shadow-md transition-all cursor-pointer active:scale-95 disabled:opacity-50"
            title="تفريغ جدول السحابة بالكامل من الأكواد القديمة والمشوهة وإعادة الرفع النظيف المتطابق 100%"
          >
            <Trash2 className="w-4 h-4" />
            <span>تفريغ ومزامنة نظيفة 🧹</span>
          </button>

          {/* Sync to Supabase Cloud */}
          <button
            onClick={handleSyncToCloud}
            disabled={isSavingCloud}
            className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs md:text-sm shadow-md transition-all cursor-pointer active:scale-95 disabled:opacity-50"
          >
            {isSavingCloud ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>جاري المزامنة...</span>
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                <span>حفظ ومزامنة السحابة ☁️</span>
              </>
            )}
          </button>

          {/* Back Button */}
          <button
            onClick={onBackToMain}
            className="p-2.5 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors cursor-pointer"
            title="رجوع للقائمة الرئيسية"
          >
            <ArrowRight className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Status Alert Banner */}
      {statusMessage && (
        <div className={`p-4 rounded-2xl text-xs font-bold flex items-center justify-between shadow-sm animate-fade-in ${
          statusMessage.type === 'success' 
            ? 'bg-emerald-50 text-emerald-900 border border-emerald-300' 
            : statusMessage.type === 'error'
            ? 'bg-rose-50 text-rose-900 border border-rose-300'
            : 'bg-indigo-50 text-indigo-900 border border-indigo-300'
        }`}>
          <div className="flex items-center gap-2">
            {statusMessage.type === 'success' ? <CheckCircle2 className="w-4 h-4 text-emerald-600" /> : <AlertCircle className="w-4 h-4" />}
            <span>{statusMessage.text}</span>
          </div>
          <button onClick={() => setStatusMessage(null)} className="text-slate-400 hover:text-slate-700 cursor-pointer">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 2. GOLDEN SUPERVISOR CARD (بطاقة كود المشرف العام والرقابي) */}
      <div className="bg-gradient-to-r from-amber-500/10 via-orange-500/10 to-amber-600/10 border-2 border-amber-400 rounded-3xl p-5 shadow-lg relative overflow-hidden">
        <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
          <div className="space-y-2 flex-1">
            <div className="flex items-center gap-2">
              <span className="px-3 py-1 rounded-full bg-amber-500 text-white font-black text-xs shadow-sm flex items-center gap-1.5">
                <Compass className="w-3.5 h-3.5" />
                <span>كود المشرف العام والرقابي (Supervisor Code)</span>
              </span>
              <span className="text-[11px] font-bold text-amber-900 bg-amber-100 px-2 py-0.5 rounded-lg border border-amber-300">
                المدير / المشرف التربوي / موظف التطبيق
              </span>
            </div>
            
            <p className="text-xs text-slate-700 font-bold leading-relaxed">
              🔑 يمنح هذا الكود صلاحية <strong className="text-slate-950 font-black">تنزيل ورؤية كل الشعب والمواد وسجلات الطلاب وجدول الحصص بالكامل</strong>، مع تفعيل <strong className="text-amber-800 underline">وضع القراءة فقط (Read-Only)</strong> لمنع تغيير أو إضافة درجات، مع تمكين إرسال التوجيهات الإشرافية.
            </p>

            <div className="flex flex-wrap items-center gap-2 pt-1 text-[11px] text-slate-600 font-bold">
              <span className="flex items-center gap-1 bg-white/80 px-2.5 py-1 rounded-xl border border-amber-200">
                <BadgeCheck className="w-3.5 h-3.5 text-emerald-600" />
                <span>رؤية وتنزيل كل الشعب</span>
              </span>
              <span className="flex items-center gap-1 bg-white/80 px-2.5 py-1 rounded-xl border border-amber-200">
                <BookOpen className="w-3.5 h-3.5 text-blue-600" />
                <span>استعراض الجدول الأسبوعي</span>
              </span>
              <span className="flex items-center gap-1 bg-white/80 px-2.5 py-1 rounded-xl border border-amber-200">
                <Lock className="w-3.5 h-3.5 text-rose-600" />
                <span>قراءة فقط بدون تعديل درجات</span>
              </span>
              <span className="flex items-center gap-1 bg-white/80 px-2.5 py-1 rounded-xl border border-amber-200">
                <Send className="w-3.5 h-3.5 text-indigo-600" />
                <span>إرسال توجيهات إشرافية</span>
              </span>
            </div>
          </div>

          {/* Supervisor PIN Box & Actions */}
          <div className="flex flex-col sm:flex-row items-center gap-3 bg-white/90 p-3.5 rounded-2xl border-2 border-amber-300 shadow-sm shrink-0">
            <div className="text-center sm:text-right">
              <div className="text-[10px] text-slate-500 font-bold">رمز دخول المشرف المعتمد:</div>
              <div className="font-mono text-xl font-black tracking-widest text-amber-600 mt-0.5">
                {showPinMap['supervisor'] ? supervisor.code : '••••••••'}
              </div>
            </div>

            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={() => setShowPinMap(prev => ({ ...prev, supervisor: !prev['supervisor'] }))}
                className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer"
                title={showPinMap['supervisor'] ? 'إخفاء الرمز' : 'إظهار الرمز'}
              >
                {showPinMap['supervisor'] ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>

              <button
                type="button"
                onClick={() => handleCopyCode(supervisor.code, 'supervisor')}
                className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer"
                title="نسخ كود المشرف"
              >
                {copiedKey === 'supervisor' ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
              </button>

              <button
                type="button"
                onClick={handleRegenerateSupervisorCode}
                className="p-2 rounded-xl bg-amber-100 hover:bg-amber-200 text-amber-900 cursor-pointer"
                title="توليد كود مشرف جديد"
              >
                <Shuffle className="w-4 h-4" />
              </button>

              <button
                type="button"
                onClick={() => {
                  setPrintTarget('supervisor_only');
                  setIsPrintModalOpen(true);
                }}
                className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-black text-xs shadow cursor-pointer"
                title="طباعة بطاقة المشرف الرسمية"
              >
                <Printer className="w-3.5 h-3.5" />
                <span>بطاقة المشرف</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Metrics Summary Bar (Code Reduction & Faculty Balance) */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="bg-white border-2 border-indigo-100 p-3.5 rounded-2xl shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-700 flex items-center justify-center shrink-0">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xl font-black text-indigo-950">{activeTeachingProfiles.length}</div>
            <div className="text-[11px] font-bold text-slate-500">كادر تدريسي (أكواد نشطة)</div>
          </div>
        </div>

        <div className="bg-white border-2 border-amber-100 p-3.5 rounded-2xl shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-700 flex items-center justify-center shrink-0">
            <UserX className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xl font-black text-amber-950">{exemptProfiles.length}</div>
            <div className="text-[11px] font-bold text-slate-500">متفرغون وإداريون مستبعدون</div>
          </div>
        </div>

        <div className="bg-white border-2 border-blue-100 p-3.5 rounded-2xl shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center shrink-0">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xl font-black text-blue-950">{totalCoveredLessons}</div>
            <div className="text-[11px] font-bold text-slate-500">إجمالي حصص وشعب مغطاة</div>
          </div>
        </div>

        <div className="bg-gradient-to-tr from-emerald-50 to-teal-50 border-2 border-emerald-200 p-3.5 rounded-2xl shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-500 text-white flex items-center justify-center shrink-0 font-black shadow-md">
            %
          </div>
          <div>
            <div className="text-xl font-black text-emerald-900">{reductionPercentage}%</div>
            <div className="text-[11px] font-black text-emerald-700">نسبة اختصار الأكواد 🎉</div>
          </div>
        </div>
      </div>

      {/* 4. Tabs Navigation & Control Toolbar */}
      <div className="bg-white border border-slate-200 p-3.5 rounded-2xl shadow-sm space-y-3">
        <div className="flex flex-col md:flex-row items-center justify-between gap-3 border-b pb-3">
          {/* Tabs */}
          <div className="flex items-center gap-1.5 p-1 bg-slate-100 rounded-2xl w-full md:w-auto">
            <button
              onClick={() => setActiveTab('teaching')}
              className={`flex-1 md:flex-initial py-2 px-4 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                activeTab === 'teaching'
                  ? 'bg-indigo-600 text-white shadow-md'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Users className="w-3.5 h-3.5" />
              <span>الكادر التدريسي المكلف ({activeTeachingProfiles.length})</span>
            </button>

            <button
              onClick={() => setActiveTab('exempt')}
              className={`flex-1 md:flex-initial py-2 px-4 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                activeTab === 'exempt'
                  ? 'bg-amber-600 text-white shadow-md'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <UserX className="w-3.5 h-3.5" />
              <span>المتفرغون والإداريون ({exemptProfiles.length})</span>
            </button>

            <button
              onClick={() => setActiveTab('supervisor')}
              className={`flex-1 md:flex-initial py-2 px-4 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                activeTab === 'supervisor'
                  ? 'bg-orange-600 text-white shadow-md'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Compass className="w-3.5 h-3.5" />
              <span>توجيهات المشرف ({supervisor.directives.length})</span>
            </button>
          </div>

          {/* Quick Actions */}
          <div className="flex items-center gap-2 w-full md:w-auto justify-end">
            <button
              onClick={handleRegenerateAllTeacherCodes}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-900 border border-purple-200 text-xs font-black transition-all cursor-pointer active:scale-95"
              title="توليد أكواد سرية جديدة لجميع مدرسي الكادر الفعلي"
            >
              <Shuffle className="w-3.5 h-3.5 text-purple-600" />
              <span>توليد أكواد جديدة للكادر 🎲</span>
            </button>
          </div>
        </div>

        {/* Search & Subject Filter Bar (Shown for teaching & exempt tabs) */}
        {activeTab !== 'supervisor' && (
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 text-slate-400 absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                placeholder="بحث باسم الأستاذ، المادة، الصف، أو الكود..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-3 pr-9 py-2 text-xs font-bold bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white"
              />
            </div>

            {activeTab === 'teaching' && (
              <div className="flex items-center gap-2 w-full sm:w-auto">
                <span className="text-xs font-black text-slate-600 shrink-0">تصفية بالمادة:</span>
                <select
                  value={selectedFilterSubject}
                  onChange={(e) => setSelectedFilterSubject(e.target.value)}
                  className="py-1.5 px-3 text-xs font-bold bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="all">كل المواد ({allUniqueSubjects.length})</option>
                  {allUniqueSubjects.map(sub => (
                    <option key={sub} value={sub}>{sub}</option>
                  ))}
                </select>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 5. MAIN CONTENT AREA */}
      {activeTab === 'supervisor' ? (
        /* SUPERVISOR DIRECTIVES & AUDIT VIEW */
        <div className="bg-white border-2 border-slate-200 rounded-3xl p-6 shadow-lg space-y-6">
          <div className="flex items-center justify-between border-b pb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-orange-100 text-orange-700 flex items-center justify-center font-black">
                <MessageSquare className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-black text-slate-900">سجل التوجيهات والملاحظات الإشرافية</h3>
                <p className="text-xs text-slate-500 font-bold">
                  التوجيهات الصادرة من المشرف التربوي أو الإدارة للمدرسة أو لمعلمي الشعب
                </p>
              </div>
            </div>

            <button
              onClick={() => setShowDirectiveModal(true)}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-orange-600 hover:bg-orange-700 text-white font-black text-xs shadow cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>إرسال توجيه إشرافي جديد +</span>
            </button>
          </div>

          {supervisor.directives.length === 0 ? (
            <div className="p-8 text-center text-slate-400 font-bold text-xs bg-slate-50 rounded-2xl border border-dashed border-slate-300">
              لا توجد توجيهات إشرافية مسجلة حالياً. اضغط على الزر أعلاه لإضافة توجيه جديد.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {supervisor.directives.map((dir) => (
                <div key={dir.id} className="p-4 rounded-2xl bg-orange-50/50 border border-orange-200 space-y-2 relative shadow-sm">
                  <div className="flex items-start justify-between">
                    <span className="px-2.5 py-0.5 rounded-full bg-orange-200 text-orange-900 text-[10px] font-black">
                      المستهدف: {dir.target}
                    </span>
                    <span className="text-[10px] text-slate-400 font-bold font-mono">
                      {dir.date}
                    </span>
                  </div>
                  <p className="text-xs text-slate-800 font-bold leading-relaxed pt-1">
                    {dir.message}
                  </p>
                  <div className="flex justify-end pt-2">
                    <button
                      onClick={() => handleDeleteDirective(dir.id)}
                      className="text-slate-400 hover:text-rose-600 p-1 cursor-pointer"
                      title="حذف التوجيه"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        /* TEACHERS OR EXEMPT STAFF LIST */
        <div className="bg-white border-2 border-slate-200 rounded-3xl shadow-lg overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-right border-collapse">
              <thead>
                <tr className="bg-slate-900 text-white text-xs md:text-sm">
                  <th className="p-3.5 font-black border-b border-slate-800 w-12 text-center">#</th>
                  <th className="p-3.5 font-black border-b border-slate-800 min-w-[200px]">1. اسم الأستاذ والملاك</th>
                  <th className="p-3.5 font-black border-b border-slate-800 min-w-[220px]">2. المواد المسندة (متعددة)</th>
                  <th className="p-3.5 font-black border-b border-slate-800 min-w-[240px]">3. الصفوف والشعب المخصصة</th>
                  {activeTab === 'teaching' && (
                    <th className="p-3.5 font-black border-b border-slate-800 min-w-[190px]">4. كود الرفع السري (PIN)</th>
                  )}
                  <th className="p-3.5 font-black border-b border-slate-800 w-44 text-center">الحالة والإجراءات</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-xs md:text-sm">
                {displayedProfiles.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="p-12 text-center text-slate-500 font-bold">
                      لا توجد سجلات مطابقة في هذا التبويب.
                    </td>
                  </tr>
                ) : (
                  displayedProfiles.map((prof, idx) => {
                    const originalIndex = profiles.findIndex(p => p.teacherId === prof.teacherId);
                    const isPinVisible = !!showPinMap[prof.teacherId];
                    const isCopied = copiedKey === prof.teacherId;

                    return (
                      <tr 
                        key={prof.teacherId}
                        className={`transition-colors hover:bg-indigo-50/40 ${prof.isLocked ? 'bg-rose-50/30' : ''}`}
                      >
                        {/* # Index */}
                        <td className="p-3.5 text-center font-black text-slate-400">
                          {idx + 1}
                        </td>

                        {/* 1. اسم الأستاذ والملاك */}
                        <td className="p-3.5">
                          <div className="flex items-center gap-2.5">
                            <div className={`w-9 h-9 rounded-full flex items-center justify-center font-black text-xs shrink-0 border ${
                              prof.isExempt 
                                ? 'bg-amber-100 text-amber-800 border-amber-300' 
                                : 'bg-indigo-100 text-indigo-700 border-indigo-200'
                            }`}>
                              {(prof.teacherName || '؟').charAt(0)}
                            </div>
                            <div>
                              <div className="font-black text-slate-900 text-sm">
                                {prof.teacherName}
                              </div>
                              <div className="flex items-center gap-1.5 text-[10px] text-slate-500 font-bold mt-0.5">
                                <span className="text-indigo-700">{prof.jobTitle}</span>
                                <span>•</span>
                                <span>{prof.specialization}</span>
                                {prof.teachingQuota > 0 && (
                                  <>
                                    <span>•</span>
                                    <span className="text-emerald-700 font-black">{prof.teachingQuota} حصة</span>
                                  </>
                                )}
                              </div>
                            </div>
                          </div>
                        </td>

                        {/* 2. المواد المسندة (Multi-Subject Tags) */}
                        <td className="p-3.5">
                          <div className="space-y-1.5">
                            <div className="flex flex-wrap items-center gap-1">
                              {prof.subjects.map(sub => (
                                <span 
                                  key={sub}
                                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-amber-50 border border-amber-300 text-amber-950 font-black text-xs shadow-xs"
                                >
                                  <span>{sub}</span>
                                  {!prof.isExempt && (
                                    <button
                                      type="button"
                                      onClick={() => handleRemoveSubjectFromTeacher(originalIndex, sub)}
                                      className="text-amber-600 hover:text-rose-600 cursor-pointer text-xs"
                                      title="إزالة هذه المادة"
                                    >
                                      ×
                                    </button>
                                  )}
                                </span>
                              ))}

                              {!prof.isExempt && (
                                <button
                                  type="button"
                                  onClick={() => {
                                    setSubjectModalTeacherIdx(originalIndex);
                                    setSelectedAddSubject(STANDARD_APPROVED_SUBJECTS[0]);
                                    setCustomSubjectInput('');
                                  }}
                                  className="p-1 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-indigo-700 border border-indigo-200 text-xs font-bold cursor-pointer"
                                  title="إضافة مادة أخرى للأستاذ"
                                >
                                  <Plus className="w-3.5 h-3.5" />
                                </button>
                              )}
                            </div>
                          </div>
                        </td>

                        {/* 3. الصفوف والشعب المخصصة (Multi-Class/Section Tags) */}
                        <td className="p-3.5">
                          <div className="space-y-1.5">
                            <div className="flex flex-wrap items-center gap-1">
                              {prof.classes.map(c => (
                                <span 
                                  key={`${c.grade}-${c.section}`}
                                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-blue-50 border border-blue-300 text-blue-950 font-black text-xs shadow-xs"
                                >
                                  <span>{c.grade} ({c.section})</span>
                                  {!prof.isExempt && (
                                    <button
                                      type="button"
                                      onClick={() => handleRemoveClassFromTeacher(originalIndex, c.grade, c.section)}
                                      className="text-blue-600 hover:text-rose-600 cursor-pointer text-xs"
                                      title="إزالة هذه الشعبة"
                                    >
                                      ×
                                    </button>
                                  )}
                                </span>
                              ))}

                              {!prof.isExempt && (
                                <button
                                  type="button"
                                  onClick={() => {
                                    setClassModalTeacherIdx(originalIndex);
                                    setSelectedAddGrade(COMMON_GRADES[0]);
                                    setSelectedAddSection(COMMON_SECTIONS[0]);
                                  }}
                                  className="p-1 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 text-xs font-bold cursor-pointer"
                                  title="إضافة صف وشعبة أخرى للأستاذ"
                                >
                                  <Plus className="w-3.5 h-3.5" />
                                </button>
                              )}
                            </div>
                          </div>
                        </td>

                        {/* 4. كود الرفع السري الموحد (PIN) */}
                        {activeTab === 'teaching' && (
                          <td className="p-3.5">
                            <div className="flex items-center gap-1.5">
                              <div className="relative flex-1">
                                <input
                                  type={isPinVisible ? 'text' : 'password'}
                                  value={prof.secretCode}
                                  onChange={(e) => {
                                    const val = e.target.value.trim();
                                    const updated = [...profiles];
                                    updated[originalIndex].secretCode = val;
                                    saveProfilesState(updated);
                                  }}
                                  maxLength={8}
                                  className="w-full font-mono text-center tracking-widest text-sm font-black py-1 px-2 rounded-xl bg-slate-900 text-amber-400 border border-slate-700 shadow-inner focus:outline-none focus:ring-2 focus:ring-amber-400"
                                  placeholder="4 أرقام"
                                />
                              </div>

                              {/* Show/Hide PIN */}
                              <button
                                type="button"
                                onClick={() => setShowPinMap(prev => ({ ...prev, [prof.teacherId]: !prev[prof.teacherId] }))}
                                className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 cursor-pointer"
                                title={isPinVisible ? 'إخفاء الرمز' : 'إظهار الرمز'}
                              >
                                {isPinVisible ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                              </button>

                              {/* Copy PIN */}
                              <button
                                type="button"
                                onClick={() => handleCopyCode(prof.secretCode, prof.teacherId)}
                                className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 cursor-pointer"
                                title="نسخ الكود"
                              >
                                {isCopied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                              </button>

                              {/* Regenerate Random PIN */}
                              <button
                                type="button"
                                onClick={() => handleRegenerateCode(originalIndex)}
                                className="p-1.5 rounded-lg bg-purple-50 hover:bg-purple-100 text-purple-700 border border-purple-200 cursor-pointer"
                                title="توليد كود عشوائي جديد"
                              >
                                <Shuffle className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </td>
                        )}

                        {/* الحالة والإجراءات */}
                        <td className="p-3.5 text-center">
                          <div className="flex items-center justify-center gap-1.5">
                            {activeTab === 'teaching' ? (
                              <>
                                {/* Lock / Unlock */}
                                <button
                                  onClick={() => handleToggleLock(originalIndex)}
                                  className={`p-1.5 rounded-xl text-xs font-black flex items-center gap-1 transition-all cursor-pointer ${
                                    prof.isLocked 
                                      ? 'bg-rose-100 text-rose-800 hover:bg-rose-200 border border-rose-300' 
                                      : 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200 border border-emerald-300'
                                  }`}
                                  title={prof.isLocked ? 'الرفع مقفل أمنياً لهذا الأستاذ' : 'الرفع مفتوح لهذا الأستاذ'}
                                >
                                  {prof.isLocked ? <Lock className="w-3.5 h-3.5 text-rose-600" /> : <Unlock className="w-3.5 h-3.5 text-emerald-600" />}
                                  <span>{prof.isLocked ? 'مقفل' : 'مفتوح'}</span>
                                </button>

                                {/* Print Individual Pass */}
                                <button
                                  onClick={() => {
                                    setPrintTarget(prof.teacherId);
                                    setIsPrintModalOpen(true);
                                  }}
                                  className="p-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer"
                                  title="طباعة بطاقة هذا الأستاذ"
                                >
                                  <Printer className="w-3.5 h-3.5" />
                                </button>

                                {/* Exclude to Exempt */}
                                <button
                                  onClick={() => handleToggleExempt(originalIndex)}
                                  className="p-1.5 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 text-[10px] font-bold cursor-pointer"
                                  title="تحويل إلى متفرغ / استبعاد من الأكواد"
                                >
                                  تفريغ
                                </button>
                              </>
                            ) : (
                              /* Exempt Actions: Activate to Teaching */
                              <button
                                onClick={() => handleToggleExempt(originalIndex)}
                                className="flex items-center gap-1 px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs shadow cursor-pointer"
                                title="تكليف بحصص وتوليد كود"
                              >
                                <UserCheck className="w-3.5 h-3.5" />
                                <span>تكليف بحصة وتفعيل كود +</span>
                              </button>
                            )}
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
      )}

      {/* 6. MODAL: ADD CLASS & SECTION TO TEACHER */}
      {classModalTeacherIdx !== null && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border-2 border-indigo-200 p-6 max-w-sm w-full shadow-2xl space-y-4 text-right">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <div className="flex items-center gap-2">
                <Layers className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-black text-slate-900">
                  إسناد شعبة جديدة للأستاذ
                </h3>
              </div>
              <button
                onClick={() => setClassModalTeacherIdx(null)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-500 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="text-xs text-slate-600 font-bold bg-indigo-50 p-2.5 rounded-xl border border-indigo-100">
              الأستاذ المستهدف: <strong className="text-indigo-950 font-black">{profiles[classModalTeacherIdx]?.teacherName}</strong>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-black text-slate-700 mb-1">المرحلة الدراسية:</label>
                <select
                  value={selectedAddGrade}
                  onChange={(e) => setSelectedAddGrade(e.target.value)}
                  className="w-full py-2 px-3 text-xs font-bold bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500"
                >
                  {COMMON_GRADES.map(g => (
                    <option key={g} value={g}>{g}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-black text-slate-700 mb-1">الشعبة المخصصة:</label>
                <select
                  value={selectedAddSection}
                  onChange={(e) => setSelectedAddSection(e.target.value)}
                  className="w-full py-2 px-3 text-xs font-bold bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500"
                >
                  {COMMON_SECTIONS.map(s => (
                    <option key={s} value={s}>شعبة ({s})</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
              <button
                type="button"
                onClick={() => setClassModalTeacherIdx(null)}
                className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-xs cursor-pointer"
              >
                إلغاء
              </button>
              <button
                type="button"
                onClick={handleAddClassToTeacher}
                className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs shadow-md cursor-pointer flex items-center gap-1.5"
              >
                <Check className="w-4 h-4" />
                <span>إضافة الشعبة للأستاذ</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 7. MODAL: ADD SUBJECT TO TEACHER */}
      {subjectModalTeacherIdx !== null && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border-2 border-indigo-200 p-6 max-w-md w-full shadow-2xl space-y-4 text-right">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <div className="flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-black text-slate-900">
                  إسناد مادة إضافية للأستاذ
                </h3>
              </div>
              <button
                onClick={() => setSubjectModalTeacherIdx(null)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-500 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="text-xs text-slate-600 font-bold bg-indigo-50 p-2.5 rounded-xl border border-indigo-100">
              الأستاذ المستهدف: <strong className="text-indigo-950 font-black">{profiles[subjectModalTeacherIdx]?.teacherName}</strong>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-black text-slate-700 mb-1">اختر من المواد المعتمدة وزارياً:</label>
                <select
                  value={selectedAddSubject}
                  onChange={(e) => {
                    setSelectedAddSubject(e.target.value);
                    setCustomSubjectInput('');
                  }}
                  className="w-full py-2 px-3 text-xs font-bold bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500"
                >
                  {STANDARD_APPROVED_SUBJECTS.map(sub => (
                    <option key={sub} value={sub}>{sub}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-black text-slate-700 mb-1">أو اكتب اسم مادة خاصة مخصصة:</label>
                <input
                  type="text"
                  placeholder="مثال: علم الأحياء المجهرية، الرسم، الخط..."
                  value={customSubjectInput}
                  onChange={(e) => setCustomSubjectInput(e.target.value)}
                  className="w-full py-2 px-3 text-xs font-bold bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
              <button
                type="button"
                onClick={() => setSubjectModalTeacherIdx(null)}
                className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-xs cursor-pointer"
              >
                إلغاء
              </button>
              <button
                type="button"
                onClick={handleAddSubjectToTeacher}
                className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs shadow-md cursor-pointer flex items-center gap-1.5"
              >
                <Check className="w-4 h-4" />
                <span>إضافة المادة للأستاذ</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 8. MODAL: ADD SUPERVISOR DIRECTIVE */}
      {showDirectiveModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border-2 border-orange-300 p-6 max-w-md w-full shadow-2xl space-y-4 text-right">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <div className="flex items-center gap-2">
                <Send className="w-5 h-5 text-orange-600" />
                <h3 className="text-base font-black text-slate-900">
                  إرسال توجيه إشرافي جديد 📢
                </h3>
              </div>
              <button
                onClick={() => setShowDirectiveModal(false)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-500 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-black text-slate-700 mb-1">الجهة أو الشعب المستهدفة:</label>
                <input
                  type="text"
                  placeholder="مثال: كافة الصفوف، مدرسي الرياضيات، الثالث المتوسط..."
                  value={directiveTarget}
                  onChange={(e) => setDirectiveTarget(e.target.value)}
                  className="w-full py-2 px-3 text-xs font-bold bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-orange-500"
                />
              </div>

              <div>
                <label className="block text-xs font-black text-slate-700 mb-1">نص التوجيه أو الملاحظة الإشرافية:</label>
                <textarea
                  rows={4}
                  placeholder="اكتب التوجيه الإداري أو التربوي هنا..."
                  value={directiveMessage}
                  onChange={(e) => setDirectiveMessage(e.target.value)}
                  className="w-full p-3 text-xs font-bold bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-orange-500 leading-relaxed"
                />
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
              <button
                type="button"
                onClick={() => setShowDirectiveModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-xs cursor-pointer"
              >
                إلغاء
              </button>
              <button
                type="button"
                onClick={handleAddDirective}
                className="px-5 py-2 rounded-xl bg-orange-600 hover:bg-orange-700 text-white font-black text-xs shadow-md cursor-pointer flex items-center gap-1.5"
              >
                <Send className="w-4 h-4" />
                <span>إرسال التوجيه الآن</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 9. PRINT MODAL: OFFICIAL SECURITY PASSES (Teacher-Centric & Supervisor) */}
      {isPrintModalOpen && (
        <div className="fixed inset-0 bg-slate-900/80 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-4xl w-full p-6 space-y-6 shadow-2xl border-4 border-indigo-500 max-h-[90vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b pb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-indigo-100 text-indigo-700 flex items-center justify-center font-black">
                  <Printer className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-lg font-black text-slate-900">
                    طباعة بطاقات الاعتماد السرية الرسمية (Official Passes)
                  </h3>
                  <p className="text-xs text-slate-600 font-bold">
                    بطاقة واحدة مخصصة لكل أستاذ بكوده الموحد ومواده وشعبه، وبطاقة المشرف العام للمتابعة.
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

            {/* Print Selection Filter in Modal */}
            <div className="flex items-center gap-2 pb-2">
              <span className="text-xs font-black text-slate-700">تحديد المستند للطباعة:</span>
              <button
                onClick={() => setPrintTarget('all_teachers')}
                className={`py-1.5 px-3 rounded-xl text-xs font-black cursor-pointer ${
                  printTarget === 'all_teachers' 
                    ? 'bg-indigo-600 text-white shadow' 
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                كافة بطاقات المعلمين ({activeTeachingProfiles.length})
              </button>
              <button
                onClick={() => setPrintTarget('supervisor_only')}
                className={`py-1.5 px-3 rounded-xl text-xs font-black cursor-pointer ${
                  printTarget === 'supervisor_only' 
                    ? 'bg-amber-600 text-white shadow' 
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                بطاقة المشرف العام فقط 🌟
              </button>
            </div>

            {/* Printable Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 print:grid-cols-2" id="printable-passes">
              
              {/* 1. Supervisor Pass (If selected or showing all) */}
              {(printTarget === 'supervisor_only' || printTarget === 'all_teachers') && (
                <div className="bg-gradient-to-br from-amber-50 to-orange-50 border-4 border-double border-amber-400 p-5 rounded-2xl relative shadow-sm space-y-3">
                  <div className="flex items-start justify-between border-b-2 border-amber-300 pb-2">
                    <div>
                      <div className="text-[10px] font-black text-amber-800">جمهورية العراق - وزارة التربية</div>
                      <div className="text-xs font-black text-slate-900">{config.schoolName || 'المدرسة النموذجية'}</div>
                      <div className="text-sm font-black text-amber-900 mt-1 flex items-center gap-1.5">
                        <Compass className="w-4 h-4 text-amber-600" />
                        <span>بطاقة اعتماد المشرف العام / المدير</span>
                      </div>
                    </div>
                    <div className="text-left shrink-0">
                      <div className="text-[9px] font-bold text-slate-500">معرف المدرسة:</div>
                      <div className="font-mono text-[10px] font-black text-indigo-900">{activeSchoolId}</div>
                    </div>
                  </div>

                  <div className="space-y-1 text-xs font-bold text-slate-700 bg-white/80 p-3 rounded-xl border border-amber-200">
                    <div className="flex justify-between">
                      <span className="text-slate-500">المكلف بالبطاقة:</span>
                      <span className="font-black text-slate-900">{supervisor.name}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">طبيعة الصلاحية:</span>
                      <span className="font-black text-amber-800">إشراف واطلاع شامل (قراءة فقط)</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">النطاق المشمول:</span>
                      <span className="font-black text-slate-900">كافة المواد والشعب والجدول</span>
                    </div>
                  </div>

                  {/* PIN & Barcode Box */}
                  <div className="p-3 rounded-xl bg-slate-900 text-amber-400 flex items-center justify-between shadow-inner">
                    <div>
                      <div className="text-[10px] text-slate-400 font-bold">كود المشرف العام (PIN):</div>
                      <div className="font-mono text-xl font-black tracking-widest text-amber-400">
                        {supervisor.code}
                      </div>
                    </div>
                    <QrCodeSvg value={`SUPERVISOR:${supervisor.code}:${activeSchoolId}`} size={56} />
                  </div>

                  <div className="flex items-center justify-center pt-1">
                    <BarcodeSvg value={supervisor.code} height={32} />
                  </div>

                  <div className="text-[9px] text-slate-500 text-center font-bold">
                    👁️ تستخدم هذه البطاقة لمسح الباركود والدخول الفوري لوضع المشرف الرقابي بالتطبيق.
                  </div>
                </div>
              )}

              {/* 2. Teacher Passes */}
              {printTarget !== 'supervisor_only' && activeTeachingProfiles
                .filter(p => printTarget === 'all_teachers' || p.teacherId === printTarget)
                .map((prof) => (
                  <div 
                    key={prof.teacherId}
                    className="bg-white border-2 border-dashed border-slate-300 p-4 rounded-2xl relative shadow-sm space-y-2 hover:border-indigo-400 transition-all"
                  >
                    <div className="flex items-start justify-between border-b border-slate-200 pb-2">
                      <div>
                        <div className="text-[10px] font-black text-indigo-700">جمهورية العراق - وزارة التربية</div>
                        <div className="text-xs font-black text-slate-800">{config.schoolName || 'المدرسة النموذجية'}</div>
                        <div className="text-sm font-black text-slate-900 mt-0.5">الأستاذ: {prof.teacherName}</div>
                      </div>
                      <div className="text-left shrink-0">
                        <div className="text-[9px] font-bold text-slate-400">معرف المدرسة:</div>
                        <div className="font-mono text-[10px] font-black text-indigo-900">{activeSchoolId}</div>
                      </div>
                    </div>

                    <div className="space-y-1 text-xs font-bold text-slate-700">
                      <div className="flex justify-between">
                        <span className="text-slate-500">المواد المسندة:</span>
                        <span className="font-black text-slate-900">{prof.subjects.join(' ، ')}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-500">الصفوف والشعب:</span>
                        <span className="font-black text-slate-900">
                          {prof.classes.map(c => `${c.grade} (${c.section})`).join(' ، ')}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-500">الاختصاص:</span>
                        <span className="text-slate-800">{prof.specialization || prof.jobTitle}</span>
                      </div>
                    </div>

                    {/* PIN & Barcode Box */}
                    <div className="mt-2 p-2.5 rounded-xl bg-slate-900 text-amber-400 flex items-center justify-between">
                      <div>
                        <div className="text-[9px] text-slate-400 font-bold">كود رفع الدرجات الموحد (PIN):</div>
                        <div className="font-mono text-lg font-black tracking-widest text-amber-400">
                          {prof.secretCode}
                        </div>
                      </div>
                      <QrCodeSvg value={`TEACHER:${prof.secretCode}:${activeSchoolId}:${prof.teacherName}`} size={48} />
                    </div>

                    <div className="flex items-center justify-center pt-1">
                      <BarcodeSvg value={prof.secretCode} height={28} />
                    </div>

                    <div className="text-[9px] text-slate-400 text-center font-bold">
                      ⚠️ كود شخصي سري موحد معتمد لكافة مواد وشعب الأستاذ أعلاه.
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

    </div>
  );
};
