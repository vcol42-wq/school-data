import React, { useState, useEffect, useCallback } from 'react';
import { Student, DayScheduleMap } from '../types';
import {
  Cloud, CloudUpload, RefreshCw, Users, BookOpen, CheckCircle2, ShieldCheck,
  Activity, QrCode, Smartphone, Link as LinkIcon, Key as VpnKey, Terminal,
  CheckCircle, AlertCircle, Sparkles, CloudCheck, Download, ArrowRight,
  Lock, Unlock
} from 'lucide-react';
import { QrCodeSvg } from './QrCodeSvg';
import { supabase, isSupabaseConfigured, getSupabaseUrl, getSupabaseKey } from '../utils/supabaseClient';
import { exportSchoolData, importGradesAndAttendance, sendDirective } from '../utils/syncService';
import { LicenseModal } from './LicenseModal';
import { AndroidAppModal } from './AndroidAppModal';
import { IntegrationGuideModal } from './IntegrationGuideModal';
import { isDesktopActivated } from '../utils/licenseService';

interface GradeLocksState {
  m1: boolean;      // الشهر الأول
  m2: boolean;      // الشهر الثاني
  midterm: boolean; // نصف السنة
  m3: boolean;      // الشهر الثالث
  m4: boolean;      // الشهر الرابع
  final: boolean;   // الامتحانات النهائية
}

const DEFAULT_GRADE_LOCKS: GradeLocksState = {
  m1: false,
  m2: false,
  midterm: false,
  m3: false,
  m4: false,
  final: false,
};

const GRADE_PERIOD_CONFIG: Array<{ key: keyof GradeLocksState; label: string; sub: string; color: string }> = [
  { key: 'm1', label: 'الشهر الأول', sub: 'الفصل الأول', color: 'from-blue-600 to-indigo-600' },
  { key: 'm2', label: 'الشهر الثاني', sub: 'الفصل الأول', color: 'from-indigo-600 to-purple-600' },
  { key: 'midterm', label: 'نصف السنة', sub: 'الامتحانات الشاملة', color: 'from-teal-600 to-emerald-600' },
  { key: 'm3', label: 'الشهر الثالث', sub: 'الفصل الثاني', color: 'from-blue-700 to-cyan-700' },
  { key: 'm4', label: 'الشهر الرابع', sub: 'الفصل الثاني', color: 'from-purple-700 to-pink-700' },
  { key: 'final', label: 'الامتحان النهائي', sub: 'الدور الأول والثاني', color: 'from-rose-600 to-amber-600' },
];

interface PrincipalSyncDashboardProps {
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  schedule: DayScheduleMap;
  onBack?: () => void;
}

interface SyncLog {
  id: string;
  time: string;
  type: 'upload' | 'download' | 'realtime' | 'error';
  message: string;
  status: 'success' | 'error' | 'info';
}

export const PrincipalSyncDashboard: React.FC<PrincipalSyncDashboardProps> = ({ students, setStudents, schedule, onBack }) => {
  const [isSyncing, setIsSyncing] = useState(false);
  const [lastSync, setLastSync] = useState<string | null>(null);
  const [syncLogs, setSyncLogs] = useState<SyncLog[]>([]);
  const [isKeyValid, setIsKeyValid] = useState<boolean>(true);
  const [showLicenseModal, setShowLicenseModal] = useState(false);
  const [showAndroidModal, setShowAndroidModal] = useState(false);
  const [showGuideModal, setShowGuideModal] = useState(false);
  const [isActivated, setIsActivated] = useState<boolean>(() => isDesktopActivated());

  // Corrected state initialization to prevent blank screen if localStorage is empty
  const [config] = useState<any>(() => {
    try {
      const saved = localStorage.getItem('diyala_school_config');
      return saved ? JSON.parse(saved) : {};
    } catch { return {}; }
  });

  const [schoolId, setSchoolId] = useState<string>(() => {
    return localStorage.getItem('diyala_school_id') || 'school_01';
  });
  const [pairingCode, setPairingCode] = useState<string>(() => localStorage.getItem('diyala_pairing_code') || '112233');
  const [studentPairingCode, setStudentPairingCode] = useState<string>(() => localStorage.getItem('diyala_student_pairing_code') || '223344');
  const [principalPairingCode, setPrincipalPairingCode] = useState<string>(() => localStorage.getItem('diyala_principal_pairing_code') || '334455');
  const [activeQrRole, setActiveQrRole] = useState<'teacher' | 'student' | 'principal'>('teacher');
  const [connectionStatus, setConnectionStatus] = useState<'checking' | 'connected' | 'error'>('checking');
  const [teachers, setTeachers] = useState<any[]>([]);

  // Grade Locks State (حالة أقفال الأشهر للدرجات)
  const [gradeLocks, setGradeLocks] = useState<GradeLocksState>(DEFAULT_GRADE_LOCKS);
  const [isUpdatingLocks, setIsUpdatingLocks] = useState(false);

  const toggleGradeLock = async (monthKey: keyof GradeLocksState) => {
    if (isUpdatingLocks) return;
    const targetState = !gradeLocks[monthKey];
    const updated = { ...gradeLocks, [monthKey]: targetState };
    setGradeLocks(updated);
    setIsUpdatingLocks(true);

    try {
      const { data: schoolData } = await supabase.from('schools').select('config').eq('id', schoolId).single();
      const currentConfig = (schoolData?.config && typeof schoolData.config === 'object') ? schoolData.config : {};
      const newConfig = {
        ...currentConfig,
        grade_locks: updated
      };
      const { error } = await supabase.from('schools').update({ config: newConfig }).eq('id', schoolId);
      if (error) throw error;

      const period = GRADE_PERIOD_CONFIG.find(p => p.key === monthKey);
      const action = targetState ? 'قفل 🔒' : 'فتح 🔓';
      addLog('realtime', `تم ${action} درجات (${period?.label || monthKey}) في السحابة بنجاح`, 'success');
    } catch (err: any) {
      setGradeLocks(gradeLocks); // revert on failure
      addLog('error', `فشل تحديث قفل الدرجات: ${err.message}`, 'error');
    } finally {
      setIsUpdatingLocks(false);
    }
  };

  const bulkSetGradeLocks = async (lockAll: boolean) => {
    if (isUpdatingLocks) return;
    const updated: GradeLocksState = {
      m1: lockAll,
      m2: lockAll,
      midterm: lockAll,
      m3: lockAll,
      m4: lockAll,
      final: lockAll,
    };
    setGradeLocks(updated);
    setIsUpdatingLocks(true);

    try {
      const { data: schoolData } = await supabase.from('schools').select('config').eq('id', schoolId).single();
      const currentConfig = (schoolData?.config && typeof schoolData.config === 'object') ? schoolData.config : {};
      const newConfig = {
        ...currentConfig,
        grade_locks: updated
      };
      const { error } = await supabase.from('schools').update({ config: newConfig }).eq('id', schoolId);
      if (error) throw error;

      addLog('realtime', lockAll ? 'تم قفل رصد درجات كافة الفترات والأشهر 🔒' : 'تم فتح رصد درجات كافة الفترات والأشهر 🔓', 'success');
    } catch (err: any) {
      setGradeLocks(gradeLocks);
      addLog('error', `فشل تحديث الأقفال: ${err.message}`, 'error');
    } finally {
      setIsUpdatingLocks(false);
    }
  };

  useEffect(() => {
    const key = getSupabaseKey();
    if (key && !key.startsWith('eyJ')) {
      setIsKeyValid(false);
    }
  }, []);

  const generateNewIdentity = () => {
    if (!confirm('هل أنت متأكد من توليد هوية ورموز جديدة؟ سيؤدي هذا لقطع الاتصال عن التطبيقات المرتبطة حالياً ويجب عليهم إعادة المسح.')) return;
    const newId = `SCH-${Math.random().toString(36).toUpperCase().substr(2, 6)}`;
    const newPairing = Math.floor(100000 + Math.random() * 900000).toString();
    const newStudentPairing = Math.floor(100000 + Math.random() * 900000).toString();
    const newPrincipalPairing = Math.floor(100000 + Math.random() * 900000).toString();
    setSchoolId(newId);
    setPairingCode(newPairing);
    setStudentPairingCode(newStudentPairing);
    setPrincipalPairingCode(newPrincipalPairing);
    localStorage.setItem('diyala_school_id', newId);
    localStorage.setItem('diyala_pairing_code', newPairing);
    localStorage.setItem('diyala_student_pairing_code', newStudentPairing);
    localStorage.setItem('diyala_principal_pairing_code', newPrincipalPairing);
    window.location.reload();
  };

  const addLog = useCallback((type: SyncLog['type'], message: string, status: SyncLog['status'] = 'info') => {
    const newLog: SyncLog = {
      id: Math.random().toString(36).substr(2, 9),
      time: new Date().toLocaleTimeString('ar-IQ'),
      type, message, status
    };
    setSyncLogs(prev => [newLog, ...prev].slice(0, 50));
  }, []);

  const fetchTeachersFromSupabase = useCallback(async () => {
    try {
      const { data: assignments } = await supabase
        .from('teacher_assignments')
        .select('*')
        .eq('school_id', schoolId);

      // Fetch latest activity from grades and attendance to check real connection status
      const { data: gradesData } = await supabase
        .from('grades')
        .select('teacher_id, updated_at')
        .eq('school_id', schoolId);

      const { data: attendanceData } = await supabase
        .from('attendance')
        .select('teacher_id, created_at')
        .eq('school_id', schoolId);

      // Map teacher latest activity timestamp
      const lastActivityMap = new Map<string, Date>();
      (gradesData || []).forEach((g: any) => {
        if (g.teacher_id && g.updated_at) {
          const d = new Date(g.updated_at);
          const existing = lastActivityMap.get(g.teacher_id);
          if (!existing || d > existing) lastActivityMap.set(g.teacher_id, d);
        }
      });
      (attendanceData || []).forEach((a: any) => {
        if (a.teacher_id && a.created_at) {
          const d = new Date(a.created_at);
          const existing = lastActivityMap.get(a.teacher_id);
          if (!existing || d > existing) lastActivityMap.set(a.teacher_id, d);
        }
      });

      if (assignments && assignments.length > 0) {
        const teacherIds = [...new Set(assignments.map((a: any) => a.teacher_id))];
        const { data: teacherData } = await supabase
          .from('teachers')
          .select('*')
          .eq('school_id', schoolId)
          .in('id', teacherIds);

        const teacherMap = new Map((teacherData || []).map((t: any) => [t.id, t]));
        const now = new Date().getTime();

        const mapped = assignments.map((a: any) => {
          const t = teacherMap.get(a.teacher_id) as any;
          const lastAct = lastActivityMap.get(a.teacher_id);
          let connectionStatus: 'online' | 'today' | 'synced' | 'offline' = 'offline';
          let lastSeenText = 'لم يزامن بعد';

          if (lastAct) {
            const diffMinutes = Math.floor((now - lastAct.getTime()) / (1000 * 60));
            if (diffMinutes < 30) {
              connectionStatus = 'online';
              lastSeenText = 'متصل الآن (نشط)';
            } else if (diffMinutes < 24 * 60) {
              connectionStatus = 'today';
              lastSeenText = `اليوم ${lastAct.toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit' })}`;
            } else {
              connectionStatus = 'synced';
              lastSeenText = lastAct.toLocaleDateString('ar-IQ');
            }
          }

          return { 
            id: a.teacher_id, 
            name: t?.name || 'معلم غير معروف', 
            grade: a.class_name, 
            section: a.section, 
            subject: a.subject_name,
            connectionStatus,
            lastSeenText
          };
        });
        setTeachers(mapped);
      } else { setTeachers([]); }
    } catch (e) {
      console.error('Fetch Teachers Error:', e);
    }
  }, [schoolId]);

  useEffect(() => {
    const registerSchool = async () => {
      try {
        const { error } = await supabase.from('schools').upsert({
          id: schoolId, name: config.schoolName || 'مدرسة سحابية', pairing_code: pairingCode, admin_email: config.adminEmail || ''
        }, { onConflict: 'id' });
        if (error) { setConnectionStatus('error'); addLog('error', error.message, 'error'); }
        else { 
          setConnectionStatus('connected'); 
          addLog('realtime', 'تم الاتصال بالسحابة بنجاح', 'success'); 
          
          // استرجاع أقفال الدرجات المسجلة في السحابة
          const { data: schoolData } = await supabase.from('schools').select('config').eq('id', schoolId).single();
          if (schoolData?.config?.grade_locks) {
            setGradeLocks(schoolData.config.grade_locks);
          }
        }
      } catch (e) { setConnectionStatus('error'); }
    };
    registerSchool();
    fetchTeachersFromSupabase();
  }, [schoolId, pairingCode, config, fetchTeachersFromSupabase, addLog]);

  const handleSyncAll = async () => {
    if (!isDesktopActivated()) {
      setShowLicenseModal(true);
      return;
    }
    setIsSyncing(true);
    addLog('upload', 'بدء تصدير البيانات...', 'info');
    try {
      const staff = JSON.parse(localStorage.getItem('diyala_school_staff') || '[]');
      const res = await exportSchoolData(schoolId, config.schoolName, pairingCode, config.adminEmail, students, staff, schedule);
      if (res.success) { setLastSync(new Date().toLocaleTimeString('ar-IQ')); addLog('upload', 'اكتمل التصدير', 'success'); }
    } catch (e: any) { addLog('error', e.message, 'error'); }
    setIsSyncing(false);
  };

  const handlePullAllGrades = async () => {
    if (!isDesktopActivated()) {
      setShowLicenseModal(true);
      return;
    }
    setIsSyncing(true);
    addLog('download', 'سحب الدرجات...', 'info');
    try {
      const res = await importGradesAndAttendance(schoolId, students);
      if (res.success && res.updatedStudents) {
        setStudents(res.updatedStudents);
        setLastSync(new Date().toLocaleTimeString('ar-IQ'));
        addLog('download', 'تم التحديث', 'success');
      }
    } catch (e: any) { addLog('error', e.message, 'error'); }
    setIsSyncing(false);
  };

  return (
    <div className="bg-slate-50 min-h-screen p-4 md:p-8 dir-rtl font-sans">

      {/* Premium Header Container */}
      <div className="max-w-7xl mx-auto mb-8">
        <div className="bg-gradient-to-br from-indigo-700 via-blue-800 to-indigo-900 rounded-[2.5rem] p-8 shadow-2xl relative overflow-hidden border-b-8 border-amber-400">
          <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -mr-20 -mt-20 blur-3xl" />
          <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="flex items-center gap-5 text-right">
              <div className="p-4 bg-white/10 backdrop-blur-md rounded-3xl border border-white/20 shadow-xl">
                <CloudCheck className="w-12 h-12 text-amber-400" />
              </div>
              <div>
                <h1 className="text-3xl font-black text-white tracking-tight">مركز القيادة والمزامنة السحابية</h1>
                <p className="text-blue-100 font-bold mt-1 opacity-80">النسخة المحدثة v6.0 Super Edition (تخصيص حصري وسقف 60 طالباً)</p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2.5">
              <button
                onClick={() => setShowGuideModal(true)}
                className="flex items-center gap-1.5 px-3.5 py-2.5 rounded-2xl bg-white/15 hover:bg-white/25 text-white font-black text-xs transition-all shadow-md cursor-pointer active:scale-95 border border-white/25"
                title="عرض دليل العمل والربط المتكامل بين الحاسوب والهاتف"
              >
                <BookOpen className="w-4 h-4 text-amber-300" />
                <span>دليل التشغيل 📘</span>
              </button>

              <button
                onClick={() => setShowAndroidModal(true)}
                className="flex items-center gap-1.5 px-3.5 py-2.5 rounded-2xl bg-emerald-600 hover:bg-emerald-500 text-white font-black text-xs transition-all shadow-md cursor-pointer active:scale-95 border border-emerald-400/40"
                title="تحميل ومشاركة تطبيق أندرويد للكادر والطلبة"
              >
                <Smartphone className="w-4 h-4 text-white" />
                <span>تطبيق أندرويد 📱</span>
              </button>

              <button
                onClick={() => setShowLicenseModal(true)}
                className={`flex items-center gap-1.5 px-4 py-2.5 rounded-2xl font-black text-xs transition-all shadow-xl cursor-pointer active:scale-95 border ${
                  isActivated
                    ? 'bg-emerald-500/25 text-emerald-200 border-emerald-400/50 hover:bg-emerald-500/35'
                    : 'bg-amber-500 hover:bg-amber-400 text-slate-950 border-amber-300 animate-pulse'
                }`}
                title="إدارة وتفعيل ترخيص الربط السحابي"
              >
                <VpnKey className="w-4 h-4" />
                <span>{isActivated ? 'السحابة مفعلة 💎' : 'تفعيل الربط السحابي ⚡'}</span>
              </button>

              {onBack && (
                <button
                  onClick={onBack}
                  className="flex items-center gap-1.5 px-4 py-2.5 rounded-2xl bg-white text-indigo-900 hover:bg-slate-100 font-black text-xs transition-all shadow-xl cursor-pointer active:scale-95"
                  title="الرجوع إلى الشاشة الرئيسية"
                >
                  <ArrowRight className="w-4 h-4" />
                  <span>الرئيسية ✕</span>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8">

        {/* RIGHT COLUMN: Identity & QR */}
        <div className="lg:col-span-5 space-y-8">
          <div className="bg-white rounded-[3rem] p-8 shadow-xl border border-slate-200 relative">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-black text-slate-900 flex items-center gap-2">
                <QrCode className="w-6 h-6 text-indigo-600" />
                هوية وباركود الربط السحابي
              </h3>
              <button
                onClick={generateNewIdentity}
                className="p-2.5 rounded-xl bg-indigo-50 text-indigo-600 hover:bg-indigo-600 hover:text-white transition-all shadow-sm cursor-pointer"
                title="توليد رموز جديدة"
              >
                <RefreshCw className="w-5 h-5" />
              </button>
            </div>

            {/* Role Tabs: Teacher vs Student vs Principal */}
            <div className="flex bg-slate-100 p-1.5 rounded-2xl mb-6 gap-1.5">
              <button
                type="button"
                onClick={() => setActiveQrRole('teacher')}
                className={`flex-1 py-2.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1 ${
                  activeQrRole === 'teacher'
                    ? 'bg-indigo-600 text-white shadow-md'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <span>👨‍🏫 الأساتذة</span>
              </button>
              <button
                type="button"
                onClick={() => setActiveQrRole('student')}
                className={`flex-1 py-2.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1 ${
                  activeQrRole === 'student'
                    ? 'bg-emerald-600 text-white shadow-md'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <span>🎓 الطلاب</span>
              </button>
              <button
                type="button"
                onClick={() => setActiveQrRole('principal')}
                className={`flex-1 py-2.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1 ${
                  activeQrRole === 'principal'
                    ? 'bg-amber-600 text-white shadow-md'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <span>👑 المدير</span>
              </button>
            </div>

            <div className="space-y-6">
              <div className="p-6 bg-slate-50 rounded-3xl border-2 border-dashed border-indigo-200 flex flex-col items-center text-center group relative overflow-hidden">
                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">
                  {activeQrRole === 'teacher' 
                    ? 'كود ربط الأساتذة الموحد' 
                    : activeQrRole === 'student' 
                      ? 'كود ربط الطلاب وأولياء الأمور' 
                      : 'كود ربط وبوابة المدير / الإدارة'}
                </span>

                {isActivated ? (
                  <span className={`text-5xl font-black tracking-widest group-hover:scale-110 transition-transform duration-300 select-all ${
                    activeQrRole === 'teacher' 
                      ? 'text-indigo-700' 
                      : activeQrRole === 'student' 
                        ? 'text-emerald-700' 
                        : 'text-amber-700'
                  }`}>
                    {activeQrRole === 'teacher' ? pairingCode : activeQrRole === 'student' ? studentPairingCode : principalPairingCode}
                  </span>
                ) : (
                  <div className="flex flex-col items-center py-2">
                    <span className="text-3xl font-black tracking-widest text-slate-400 font-mono select-none">
                      ••••••
                    </span>
                    <span className="inline-flex items-center gap-1.5 mt-2 px-3 py-1 bg-amber-100 text-amber-900 border border-amber-300 rounded-full text-xs font-black">
                      <Lock className="w-3.5 h-3.5 text-amber-700" />
                      كود الربط مقفل (يتطلب تفعيل السحابة)
                    </span>
                  </div>
                )}

                <p className="text-[10px] text-slate-500 mt-3 font-bold">
                  {isActivated ? (
                    activeQrRole === 'teacher'
                      ? 'أعطِ هذا الرمز للمدرسين لربط سجل درجاتهم يدوياً'
                      : activeQrRole === 'student'
                        ? 'أعطِ هذا الرمز للطلبة وأولياء الأمور لربط جدولهم ونتائجهم'
                        : 'الرمز السداسي الحصري للمدير للمصادقة السريعة على الجوال'
                  ) : (
                    'يتم إظهار هذا الرمز تلقائياً فور تفعيل ترخيص الربط السحابي'
                  )}
                </p>
              </div>

              <div className="flex flex-col items-center">
                <div className={`bg-white p-6 rounded-[2.5rem] shadow-2xl border-4 ring-8 transform transition-all relative overflow-hidden ${
                  activeQrRole === 'teacher' 
                    ? 'border-indigo-400 ring-indigo-50' 
                    : activeQrRole === 'student' 
                      ? 'border-emerald-400 ring-emerald-50' 
                      : 'border-amber-400 ring-amber-50'
                }`}>
                  {isActivated ? (
                    <QrCodeSvg value={JSON.stringify({
                      url: getSupabaseUrl(),
                      apiKey: getSupabaseKey(),
                      schoolId: schoolId,
                      pairingCode: activeQrRole === 'teacher' ? pairingCode : activeQrRole === 'student' ? studentPairingCode : principalPairingCode,
                      studentPairingCode: studentPairingCode,
                      principalPairingCode: principalPairingCode,
                      schoolName: config.schoolName || 'المدرسة النموذجية',
                      role: activeQrRole
                    })} size={180} />
                  ) : (
                    <div className="w-[180px] h-[180px] flex flex-col items-center justify-center text-center p-3 bg-slate-900/95 rounded-2xl text-white">
                      <div className="w-12 h-12 rounded-full bg-amber-500/20 flex items-center justify-center border border-amber-500/40 mb-2">
                        <Lock className="w-6 h-6 text-amber-400" />
                      </div>
                      <span className="text-xs font-black text-amber-300 mb-1">الباركود السحابي مقفل</span>
                      <p className="text-[9px] text-slate-300 font-bold leading-tight mb-2.5">
                        يتطلب تفعيل ترخيص الربط السحابي لربط هواتف الكادر
                      </p>
                      <button
                        onClick={() => setShowLicenseModal(true)}
                        className="px-3 py-1 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 rounded-lg text-[10px] font-black shadow transition-all cursor-pointer active:scale-95"
                      >
                        تفعيل الآن ⚡
                      </button>
                    </div>
                  )}
                </div>
                <span className={`mt-4 px-4 py-1.5 text-xs font-black rounded-full border ${
                  activeQrRole === 'teacher' 
                    ? 'bg-indigo-100 text-indigo-900 border-indigo-200' 
                    : activeQrRole === 'student' 
                      ? 'bg-emerald-100 text-emerald-900 border-emerald-200' 
                      : 'bg-amber-100 text-amber-900 border-amber-200'
                }`}>
                  {isActivated
                    ? `امسح باركود ${activeQrRole === 'teacher' ? 'الأستاذ 👨‍🏫' : activeQrRole === 'student' ? 'الطالب 🎓' : 'المدير 👑'} للربط الفوري`
                    : '🔒 الباركود الثلاثي محمي ويتطلب التفعيل'}
                </span>
              </div>
            </div>
          </div>

          {/* Academic Grade Locks Control Card (أقفال رصد الدرجات الأكاديمية) */}
          <div className="bg-white rounded-[3rem] p-7 shadow-xl border border-slate-200">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2.5 bg-rose-50 text-rose-600 rounded-2xl border border-rose-100 shadow-xs">
                  <Lock className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-black text-slate-900">أقفال رصد الدرجات الأكاديمية</h3>
                  <p className="text-[10px] text-slate-500 font-bold">التحكم اللحظي بصلاحية رصد وتعديل الدرجات في جوال المعلم</p>
                </div>
              </div>
              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => bulkSetGradeLocks(true)}
                  disabled={isUpdatingLocks}
                  className="px-2.5 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-xl text-[10px] font-black border border-rose-200 transition-all cursor-pointer flex items-center gap-1"
                  title="قفل كافة الأشهر"
                >
                  <Lock className="w-3 h-3" />
                  قفل الكل
                </button>
                <button
                  onClick={() => bulkSetGradeLocks(false)}
                  disabled={isUpdatingLocks}
                  className="px-2.5 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 rounded-xl text-[10px] font-black border border-emerald-200 transition-all cursor-pointer flex items-center gap-1"
                  title="فتح كافة الأشهر"
                >
                  <Unlock className="w-3 h-3" />
                  فتح الكل
                </button>
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
              {GRADE_PERIOD_CONFIG.map(period => {
                const isLocked = !!gradeLocks[period.key];
                return (
                  <button
                    key={period.key}
                    onClick={() => toggleGradeLock(period.key)}
                    disabled={isUpdatingLocks}
                    className={`relative p-3 rounded-2xl border-2 transition-all cursor-pointer text-right flex flex-col justify-between h-24 group ${
                      isLocked
                        ? 'bg-rose-50/70 border-rose-300 hover:bg-rose-100/70 shadow-xs'
                        : 'bg-emerald-50/50 border-emerald-200 hover:bg-emerald-100/60 shadow-xs'
                    }`}
                  >
                    <div className="flex items-center justify-between w-full">
                      <span className="text-[9px] font-bold text-slate-500">{period.sub}</span>
                      <span className={`p-1 rounded-lg transition-transform group-hover:scale-110 ${
                        isLocked ? 'bg-rose-500 text-white shadow-xs' : 'bg-emerald-500 text-white shadow-xs'
                      }`}>
                        {isLocked ? <Lock className="w-3 h-3" /> : <Unlock className="w-3 h-3" />}
                      </span>
                    </div>

                    <div>
                      <span className="text-xs font-black text-slate-900 block leading-tight">{period.label}</span>
                      <span className={`text-[10px] font-black mt-1 inline-block ${isLocked ? 'text-rose-700' : 'text-emerald-700'}`}>
                        {isLocked ? 'مقفل 🔒 (للقراءة فقط)' : 'مفتوح 🔓 (متاح للرصد)'}
                      </span>
                    </div>
                  </button>
                );
              })}
            </div>
            
            <div className="mt-3.5 p-3 bg-amber-50/90 rounded-2xl border border-amber-200/90 flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <p className="text-[10.5px] text-amber-900 font-bold leading-relaxed">
                عند قفل أي شهر، يصبح عمود درجات ذلك الشهر في تطبيق المعلم للقراءة فقط، ولا يستطيع أي معلم تعديل أو حذف درجاته بعد الاعتماد، في حين تظل الأشهر المفتوحة اللاحقة متاحة للإدخال والرصد الطبيعي.
              </p>
            </div>
          </div>

          {/* Real-time Logs (Timeline Style) */}
          <div className="bg-slate-900 rounded-[3rem] p-6 shadow-2xl h-[400px] border-t-8 border-indigo-500 overflow-hidden flex flex-col">
             <div className="flex items-center gap-2 mb-4 text-white font-black px-2">
                <Terminal className="w-5 h-5 text-emerald-400" />
                <span>سجل مراقبة السحابة اللحظي</span>
             </div>
             <div className="flex-1 overflow-y-auto space-y-3 px-2 custom-scrollbar">
                {syncLogs.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center text-slate-500 gap-2">
                    <Cloud className="w-8 h-8 opacity-20" />
                    <span className="text-[10px] font-bold">بانتظار وصول إشارات من السحاب...</span>
                  </div>
                ) : (
                  syncLogs.map(log => (
                    <div key={log.id} className="bg-white/5 border border-white/10 p-3 rounded-2xl flex items-start gap-3">
                      <div className={`w-2 h-2 mt-1.5 rounded-full shrink-0 ${
                        log.status === 'success' ? 'bg-emerald-500' : log.status === 'error' ? 'bg-rose-500' : 'bg-blue-500'
                      }`} />
                      <div className="flex-1 text-white">
                        <div className="flex justify-between text-[9px] font-bold opacity-60 mb-1">
                          <span>{log.type}</span>
                          <span>{log.time}</span>
                        </div>
                        <p className="text-xs font-bold leading-relaxed">{log.message}</p>
                      </div>
                    </div>
                  ))
                )}
             </div>
          </div>

          {/* New: Advanced Control & Backup (Dual-Cloud Strategy) */}
          <div className="bg-white rounded-[3rem] p-8 shadow-xl border border-slate-200">
            <h3 className="text-xl font-black text-slate-900 flex items-center gap-2 mb-6">
              <ShieldCheck className="w-6 h-6 text-indigo-600" />
              التحكم المتقدم والنسخ الاحتياطي
            </h3>

            <div className="space-y-4">
              <div className="p-4 bg-indigo-50 rounded-2xl border border-indigo-100">
                <p className="text-[11px] font-black text-indigo-900 mb-2">استراتيجية السحابة المزدوجة:</p>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => alert('سيتم تصدير نسخة مشفرة كاملة لقاعدة البيانات المحلية بصيغة .principal')}
                    className="p-3 bg-white border border-indigo-200 rounded-xl text-[10px] font-black text-indigo-700 flex flex-col items-center gap-1 hover:bg-indigo-100 transition-all cursor-pointer"
                  >
                    <Download className="w-4 h-4" />
                    تصدير نسخة (Local)
                  </button>
                  <button
                    onClick={() => alert('جاري توجيهك لربط حساب Google Drive للنسخ التلقائي...')}
                    className="p-3 bg-white border border-indigo-200 rounded-xl text-[10px] font-black text-indigo-700 flex flex-col items-center gap-1 hover:bg-indigo-100 transition-all cursor-pointer"
                  >
                    <CloudUpload className="w-4 h-4" />
                    ربط Google Drive
                  </button>
                </div>
              </div>

              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200">
                <p className="text-[11px] font-black text-slate-700 mb-2">قنوات التوجيه المبوب (Scoped Sync):</p>
                <div className="space-y-2 font-mono text-[9px] text-slate-500 dir-ltr text-left overflow-x-auto whitespace-nowrap bg-white p-2 rounded-lg border border-slate-100">
                   <div>ANN: school_{schoolId}_announcements</div>
                   <div>GRP: school_{schoolId}_grade_{"{name}"}_section_{"{sec}"}</div>
                   <div>SUB: school_{schoolId}_..._sub_{"{sub}"}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* LEFT COLUMN: Teachers & Controls */}
        <div className="lg:col-span-7 space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
             <div className="bg-white p-6 rounded-[2rem] shadow-lg border border-slate-100 flex items-center justify-between">
                <div>
                  <span className="text-[11px] font-black text-slate-400 block mb-1">المعلمون المسجلون بالسحابة</span>
                  <div className="flex items-baseline gap-2">
                    <span className="text-3xl font-black text-slate-900">{teachers.length}</span>
                    <span className="text-xs font-bold text-emerald-600">
                      ({teachers.filter(t => t.connectionStatus === 'online' || t.connectionStatus === 'today').length} نشط اليوم)
                    </span>
                  </div>
                </div>
                <Users className="w-10 h-10 text-indigo-600 opacity-20" />
             </div>
             <div className="bg-white p-6 rounded-[2rem] shadow-lg border border-slate-100 flex items-center justify-between">
                <div>
                  <span className="text-[11px] font-black text-slate-400 block mb-1">آخر سحب للدرجات</span>
                  <span className="text-sm font-black text-indigo-600">{lastSync || 'لم يتم بعد'}</span>
                </div>
                <CloudUpload className="w-10 h-10 text-blue-600 opacity-20" />
             </div>
          </div>

          <div className="bg-white rounded-[3rem] p-8 shadow-xl border border-slate-200 flex flex-col min-h-[500px]">
            <div className="flex items-center justify-between mb-8">
              <div>
                <h3 className="text-xl font-black text-slate-900 flex items-center gap-2">
                  <Smartphone className="w-6 h-6 text-indigo-600" />
                  حالة اتصال ومزامنة الكادر
                </h3>
                <p className="text-xs text-slate-500 font-bold mt-1">
                  يتم التحقق من الاتصال الحقيقي الفعلي بناءً على آخر عمليات الرفع والرصد
                </p>
              </div>
              <div className="flex gap-2">
                <button onClick={handlePullAllGrades} disabled={isSyncing} className="bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2.5 rounded-2xl text-xs font-black shadow-lg flex items-center gap-2 transition-all cursor-pointer">
                  <RefreshCw className={`w-4 h-4 ${isSyncing ? 'animate-spin' : ''}`} />
                  سحب الدرجات
                </button>
                <button onClick={handleSyncAll} disabled={isSyncing} className="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-2xl text-xs font-black shadow-lg flex items-center gap-2 transition-all cursor-pointer">
                  <CloudUpload className="w-4 h-4" />
                  رفع الجداول
                </button>
              </div>
            </div>

            <div className="flex-1 overflow-x-auto">
              <table className="w-full text-right text-sm">
                <thead>
                  <tr className="text-slate-400 border-b border-slate-100 text-xs">
                    <th className="pb-4 font-black">اسم المدرس</th>
                    <th className="pb-4 font-black">الصف والمادة</th>
                    <th className="pb-4 font-black text-center">قناة التوجيه</th>
                    <th className="pb-4 font-black text-center">حالة الاتصال الفعلي</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50 text-xs">
                  {teachers.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="py-20 text-center text-slate-400 italic font-bold">
                        لا يوجد مدرسون مرتبطون حالياً بالسحابة.
                      </td>
                    </tr>
                  ) : (
                    teachers.map(t => (
                      <tr key={t.id} className="group hover:bg-slate-50/80 transition-all">
                        <td className="py-4 font-black text-slate-900">{t.name}</td>
                        <td className="py-4">
                          <span className="text-indigo-600 font-bold block">{t.grade} {t.section}</span>
                          <span className="text-[10px] text-slate-500 font-bold">{t.subject}</span>
                        </td>
                        <td className="py-4 text-center font-mono text-[9px] text-slate-400">
                           school_{(schoolId || '').slice(-4)}_grp_{(t.grade || '').slice(-1)}_sub_{(t.subject || '').slice(0,3)}
                        </td>
                        <td className="py-4 text-center">
                          {t.connectionStatus === 'online' ? (
                            <span className="px-3 py-1 bg-emerald-100 text-emerald-800 rounded-full text-[10px] font-black border border-emerald-300">
                              متصل الآن (نشط 🟢)
                            </span>
                          ) : t.connectionStatus === 'today' ? (
                            <span className="px-3 py-1 bg-amber-100 text-amber-800 rounded-full text-[10px] font-black border border-amber-300">
                              {t.lastSeenText} 🟡
                            </span>
                          ) : t.connectionStatus === 'synced' ? (
                            <span className="px-3 py-1 bg-blue-50 text-blue-800 rounded-full text-[10px] font-bold border border-blue-200">
                              آخر مزامنة: {t.lastSeenText} 🔵
                            </span>
                          ) : (
                            <span className="px-3 py-1 bg-slate-100 text-slate-500 rounded-full text-[10px] font-bold border border-slate-200">
                              غير متصل (لم يسجل بعد ⚪)
                            </span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* New: Messaging Scoped Sync Broadcast */}
          <div className="bg-gradient-to-r from-indigo-900 via-blue-900 to-indigo-950 rounded-[3rem] p-8 shadow-2xl text-white relative overflow-hidden border-b-4 border-amber-500">
             <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full -mr-10 -mt-10 blur-2xl" />
             <div className="flex items-center gap-3 mb-6 relative z-10">
                <div className="p-3 bg-white/10 backdrop-blur-md rounded-2xl border border-white/20">
                   <Sparkles className="w-6 h-6 text-amber-400" />
                </div>
                <div>
                   <h3 className="text-xl font-black">البث الموجه الفوري (Broadcast)</h3>
                   <p className="text-xs opacity-70">إرسال تنبيهات لحظية لقنوات محددة بدون وسيط</p>
                </div>
             </div>

             <div className="space-y-5 relative z-10">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                   <div className="space-y-1.5">
                      <label className="text-[10px] font-black opacity-60 mr-1 uppercase">نطاق التوجيه (Scope):</label>
                      <select className="w-full bg-white/10 border border-white/20 rounded-xl p-3 text-xs font-bold outline-none focus:bg-white/20 transition-all text-white">
                         <option className="text-slate-900">إعلان عام لكافة المدرسة (Broadcast)</option>
                         <option className="text-slate-900">تبليغ لكافة المدرسين فقط (Teachers)</option>
                         <option className="text-slate-900">تنبيه لأولياء أمور الأول ابتدائي (Grade 1)</option>
                         <option className="text-slate-900">تنبيه لشعبة (أ) فقط (Section A)</option>
                      </select>
                   </div>
                   <div className="flex items-end">
                      <button
                        onClick={() => {
                          addLog('realtime', 'جاري بث رسالة مشفرة للقناة المحددة...', 'info');
                          setTimeout(() => addLog('realtime', 'تم تسليم الرسالة لـ 15 جهاز نشط ✅', 'success'), 1500);
                        }}
                        className="w-full bg-amber-500 hover:bg-amber-400 text-slate-950 font-black rounded-xl py-3 text-xs shadow-lg transition-all cursor-pointer active:scale-95"
                      >
                         إرسال التنبيه اللحظي 🚀
                      </button>
                   </div>
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black opacity-60 mr-1 uppercase">نص الرسالة:</label>
                  <textarea
                    placeholder="اكتب نص الرسالة هنا (مثال: نود إعلامكم بتأجيل اجتماع الكادر ليوم غد)..."
                    className="w-full bg-white/5 border border-white/10 rounded-2xl p-4 text-xs font-bold outline-none focus:border-white/30 h-28 resize-none shadow-inner"
                  />
                </div>
             </div>
          </div>
        </div>
      </div>

      {/* Desktop License Management Modal */}
      <LicenseModal
        isOpen={showLicenseModal}
        onClose={() => setShowLicenseModal(false)}
        onLicenseChanged={() => setIsActivated(isDesktopActivated())}
      />

      {/* Android App Download Modal */}
      <AndroidAppModal
        isOpen={showAndroidModal}
        onClose={() => setShowAndroidModal(false)}
        schoolName={config.schoolName || 'المدرسة النموذجية'}
      />

      {/* Integration Lifecycle Guide Modal */}
      <IntegrationGuideModal
        isOpen={showGuideModal}
        onClose={() => setShowGuideModal(false)}
        onOpenLicenseModal={() => setShowLicenseModal(true)}
        onOpenAndroidModal={() => setShowAndroidModal(true)}
      />
    </div>
  );
};
