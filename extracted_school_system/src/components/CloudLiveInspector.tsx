import React, { useState, useEffect } from 'react';
import { 
  Cloud, 
  RefreshCw, 
  CheckCircle2, 
  AlertCircle, 
  UploadCloud, 
  Database, 
  Users, 
  GraduationCap, 
  BookOpen, 
  Calendar, 
  ClipboardList, 
  Eye, 
  X, 
  QrCode, 
  Copy, 
  Sparkles,
  Layers,
  ArrowRight,
  ShieldCheck,
  Zap,
  Server,
  Trash2,
  Edit2,
  Check,
  AlertTriangle,
  Key
} from 'lucide-react';
import QRCode from 'qrcode';
import { Portal } from './common/Portal';
import { Student, StaffMember, AppConfig, DayScheduleMap } from '../types';
import { 
  exportSchoolDataWithProgress, 
  deepCleanSchoolCloudData,
  fetchCloudTableStats, 
  fetchCloudTableRows, 
  deleteCloudRow,
  updateCloudRow,
  clearCloudTable,
  SyncStepInfo, 
  CloudTableStats 
} from '../utils/syncEngine';

interface CloudLiveInspectorProps {
  students: Student[];
  staffList: StaffMember[];
  config: AppConfig;
  scheduleMap?: DayScheduleMap;
  onBack?: () => void;
}

export const CloudLiveInspector: React.FC<CloudLiveInspectorProps> = ({
  students,
  staffList,
  config,
  scheduleMap,
  onBack
}) => {
  const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
  const schoolName = config.schoolName || 'المدرسة النموذجية';
  const pairingCode = config.pairingCode || localStorage.getItem('diyala_pairing_code') || '112233';
  const studentPairingCode = config.studentPairingCode || localStorage.getItem('diyala_student_pairing_code') || '223344';
  const principalPairingCode = config.principalPairingCode || localStorage.getItem('diyala_principal_pairing_code') || '334455';
  const adminEmail = config.adminEmail || '';

  // 3-Role QR selector state
  const [activeQrRole, setActiveQrRole] = useState<'teacher' | 'student' | 'principal'>('teacher');

  // Sync Progress States
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncPercent, setSyncPercent] = useState(0);
  const [syncSteps, setSyncSteps] = useState<SyncStepInfo[]>([]);
  const [syncFinalResult, setSyncFinalResult] = useState<{ success: boolean; message: string } | null>(null);

  // Cloud Stats States
  const [cloudStats, setCloudStats] = useState<CloudTableStats | null>(null);
  const [isLoadingStats, setIsLoadingStats] = useState(false);

  // Modal Table Explorer States
  const [previewTableName, setPreviewTableName] = useState<string | null>(null);
  const [previewTableTitle, setPreviewTableTitle] = useState<string>('');
  const [previewRows, setPreviewRows] = useState<any[]>([]);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [actionMsg, setActionMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Row Editing States
  const [editingRowIdx, setEditingRowIdx] = useState<number | null>(null);
  const [editingRowData, setEditingRowData] = useState<any>({});

  // QR Code State
  const [qrDataUrl, setQrDataUrl] = useState<string>('');
  const [copied, setCopied] = useState(false);

  // Initial Load
  useEffect(() => {
    loadCloudStats();
  }, [schoolId]);

  // Regenerate QR whenever active role changes
  useEffect(() => {
    generateSmartQr(activeQrRole);
  }, [activeQrRole, schoolId, pairingCode, studentPairingCode, principalPairingCode]);

  const loadCloudStats = async () => {
    setIsLoadingStats(true);
    try {
      const stats = await fetchCloudTableStats(schoolId);
      setCloudStats(stats);
    } catch (e) {
      console.error('Failed to load stats:', e);
    } finally {
      setIsLoadingStats(false);
    }
  };

  const generateSmartQr = async (role: 'teacher' | 'student' | 'principal' = activeQrRole) => {
    const supabaseUrl = localStorage.getItem('diyala_supabase_url') || 'https://pexehlvkpdhmpukjydwd.supabase.co';
    const supabaseKey = localStorage.getItem('diyala_supabase_key') || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY';

    const targetCode = role === 'teacher' ? pairingCode : role === 'student' ? studentPairingCode : principalPairingCode;

    const payload = JSON.stringify({
      url: supabaseUrl,
      apiKey: supabaseKey,
      schoolId: schoolId,
      pairingCode: targetCode,
      studentPairingCode: studentPairingCode,
      principalPairingCode: principalPairingCode,
      schoolName: schoolName,
      role: role
    });

    try {
      const colorDark = role === 'teacher' ? '#312e81' : role === 'student' ? '#064e3b' : '#78350f';
      const url = await QRCode.toDataURL(payload, {
        width: 320,
        margin: 2,
        color: { dark: colorDark, light: '#ffffff' }
      });
      setQrDataUrl(url);
    } catch (err) {
      console.error('QR error:', err);
    }
  };

  const copyCurrentCode = () => {
    const codeToCopy = activeQrRole === 'teacher' ? pairingCode : activeQrRole === 'student' ? studentPairingCode : principalPairingCode;
    navigator.clipboard.writeText(codeToCopy);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleStartExport = async () => {
    setIsSyncing(true);
    setSyncPercent(0);
    setSyncSteps([]);
    setSyncFinalResult(null);

    const result = await exportSchoolDataWithProgress(
      schoolId,
      schoolName,
      pairingCode,
      adminEmail,
      students,
      staffList,
      scheduleMap,
      (step) => {
        setSyncPercent(step.percent);
        setSyncSteps(prev => {
          const idx = prev.findIndex(s => s.id === step.id);
          if (idx > -1) {
            const updated = [...prev];
            updated[idx] = step;
            return updated;
          }
          return [...prev, step];
        });
      }
    );

    setIsSyncing(false);
    setSyncFinalResult(result);
    // Reload cloud stats to reflect the freshly uploaded data
    await loadCloudStats();
  };

  const handleDeepPurgeAndSync = async () => {
    if (!confirm('⚠️ تنبيه هام للتطهير السحابي الشامل:\n\nسيقوم هذا الإجراء بمسح أي شعب مكررة، ومسح المواد الوهمية ذات الأحرف المفردة (أ، ب، ج، د)، ومسح الحصص المزدوجة من السحابة تماماً، ثم إعادة رفع ومزامنة البيانات النقية المعتمدة وفق المعايير الوزارية 100%.\n\nهل أنت متأكد وترغب بالمتابعة؟')) return;

    setIsSyncing(true);
    setSyncPercent(5);
    setSyncSteps([]);
    setSyncFinalResult(null);

    // 1. Purge dirty cloud tables
    const cleanRes = await deepCleanSchoolCloudData(schoolId);
    if (!cleanRes.success) {
      alert(`تنبيه: ${cleanRes.message}`);
    }

    // 2. Freshly export all standardized data
    await handleStartExport();
  };

  const handleOpenTablePreview = async (tableName: string, title: string) => {
    setPreviewTableName(tableName);
    setPreviewTableTitle(title);
    setIsLoadingPreview(true);
    setPreviewRows([]);
    setEditingRowIdx(null);
    setActionMsg(null);

    const res = await fetchCloudTableRows(schoolId, tableName);
    setIsLoadingPreview(false);
    if (res.success) {
      setPreviewRows(res.data);
    } else {
      setActionMsg({ type: 'error', text: `تعذر جلب البيانات: ${res.error}` });
    }
  };

  const handleDeleteRow = async (row: any, idx: number) => {
    if (!previewTableName) return;
    if (!confirm('هل أنت متأكد من حذف هذا السجل نهائياً من السحابة؟')) return;

    const matchKey = row.id ? 'id' : (row.record_number ? 'record_number' : (row.name ? 'name' : Object.keys(row)[0]));
    const matchVal = row[matchKey];

    const res = await deleteCloudRow(schoolId, previewTableName, matchKey, matchVal);
    if (res.success) {
      setPreviewRows(prev => prev.filter((_, i) => i !== idx));
      setActionMsg({ type: 'success', text: 'تم حذف السجل من السحابة بنجاح! 🗑️' });
      await loadCloudStats();
      setTimeout(() => setActionMsg(null), 3000);
    } else {
      setActionMsg({ type: 'error', text: res.message });
    }
  };

  const handleStartEditRow = (row: any, idx: number) => {
    setEditingRowIdx(idx);
    setEditingRowData({ ...row });
  };

  const handleSaveEditRow = async (originalRow: any, idx: number) => {
    if (!previewTableName) return;

    const matchKey = originalRow.id ? 'id' : (originalRow.record_number ? 'record_number' : (originalRow.name ? 'name' : Object.keys(originalRow)[0]));
    const matchVal = originalRow[matchKey];

    const res = await updateCloudRow(schoolId, previewTableName, matchKey, matchVal, editingRowData);
    if (res.success) {
      setPreviewRows(prev => {
        const updated = [...prev];
        updated[idx] = editingRowData;
        return updated;
      });
      setEditingRowIdx(null);
      setActionMsg({ type: 'success', text: 'تم حفظ التعديل في السحابة بنجاح! 💾' });
      setTimeout(() => setActionMsg(null), 3000);
    } else {
      setActionMsg({ type: 'error', text: res.message });
    }
  };

  const handleClearEntireTable = async () => {
    if (!previewTableName) return;
    if (previewTableName === 'schools') {
      alert('لا يمكن مسح سجل هوية المدرسة الأساسي.');
      return;
    }
    if (!confirm(`تحذير: هل أنت متأكد من مسح وتفريغ جدول (${previewTableTitle}) بالكامل من السحابة لهذه المدرسة؟`)) return;

    const res = await clearCloudTable(schoolId, previewTableName);
    if (res.success) {
      setPreviewRows([]);
      setActionMsg({ type: 'success', text: res.message });
      await loadCloudStats();
      setTimeout(() => setActionMsg(null), 3000);
    } else {
      setActionMsg({ type: 'error', text: res.message });
    }
  };

  const handleClearTableDirectly = async (tableName: string, title: string) => {
    if (tableName === 'schools') {
      alert('لا يمكن مسح سجل هوية المدرسة الأساسي.');
      return;
    }
    if (!confirm(`⚠️ تأكيد التصفير السحابي المباشر:\n\nهل أنت متأكد من رغبتك في تصفير ومسح كافة سجلات جدول (${title}) لهذه المدرسة من السحابة تماماً؟\n\nلن تتأثر بياناتك المحلية في الحاسوب، لكن سيتم تفريغ الجدول السحابي فوراً.`)) return;

    setIsLoadingStats(true);
    const res = await clearCloudTable(schoolId, tableName);
    setIsLoadingStats(false);
    if (res.success) {
      alert(`✅ ${res.message}`);
      await loadCloudStats();
    } else {
      alert(`❌ تنبيه: ${res.message}`);
    }
  };

  const copyPairingCode = () => {
    navigator.clipboard.writeText(pairingCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const statCards = [
    {
      id: 'students',
      title: 'الطلاب في السحابة',
      count: cloudStats?.studentsCount ?? 0,
      icon: GraduationCap,
      color: 'from-emerald-500 to-teal-600',
      tableName: 'students'
    },
    {
      id: 'teachers',
      title: 'المعلمون في السحابة',
      count: cloudStats?.teachersCount ?? 0,
      icon: Users,
      color: 'from-purple-500 to-indigo-600',
      tableName: 'teachers'
    },
    {
      id: 'classes',
      title: 'الصفوف والشعب',
      count: cloudStats?.classesCount ?? 0,
      icon: Layers,
      color: 'from-blue-500 to-indigo-600',
      tableName: 'classes'
    },
    {
      id: 'subjects',
      title: 'المواد المقررة',
      count: cloudStats?.subjectsCount ?? 0,
      icon: BookOpen,
      color: 'from-amber-500 to-orange-600',
      tableName: 'subjects'
    },
    {
      id: 'teacher_assignments',
      title: 'إسناد وتوزيع الحصص',
      count: cloudStats?.assignmentsCount ?? 0,
      icon: ClipboardList,
      color: 'from-pink-500 to-rose-600',
      tableName: 'teacher_assignments'
    },
    {
      id: 'subject_assignments',
      title: 'الرموز وتفويض الكادر',
      count: cloudStats?.secretCodesCount ?? 0,
      icon: Key,
      color: 'from-amber-600 to-amber-800',
      tableName: 'subject_assignments'
    },
    {
      id: 'grades',
      title: 'الدرجات المرصودة',
      count: cloudStats?.gradesCount ?? 0,
      icon: ShieldCheck,
      color: 'from-cyan-500 to-blue-600',
      tableName: 'grades'
    },
    {
      id: 'attendance',
      title: 'الغيابات المسجلة',
      count: cloudStats?.attendanceCount ?? 0,
      icon: Calendar,
      color: 'from-violet-500 to-purple-600',
      tableName: 'attendance'
    },
    {
      id: 'schedules',
      title: 'الجدول الأسبوعي السحابي',
      count: cloudStats?.schedulesCount ?? 0,
      icon: Calendar,
      color: 'from-amber-600 to-yellow-600',
      tableName: 'schedules'
    },
    {
      id: 'schools',
      title: 'هوية المدرسة',
      count: cloudStats?.schoolsCount ?? 0,
      icon: Server,
      color: 'from-slate-600 to-slate-800',
      tableName: 'schools'
    }
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6 dir-rtl">
      
      {/* Top Banner & Navigation */}
      <div className="bg-gradient-to-r from-indigo-900 via-indigo-800 to-blue-900 text-white p-6 rounded-3xl shadow-2xl border-4 border-indigo-500 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/5 rounded-full blur-3xl pointer-events-none" />
        
        <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6 relative z-10">
          <div className="flex items-center gap-4">
            {onBack && (
              <button 
                onClick={onBack}
                className="p-2.5 rounded-2xl bg-white/10 hover:bg-white/20 text-white transition-all cursor-pointer"
                title="الرجوع للرئيسية"
              >
                <ArrowRight className="w-6 h-6" />
              </button>
            )}
            <div className="p-3.5 rounded-2xl bg-amber-400 text-indigo-950 shadow-xl font-black">
              <Cloud className="w-8 h-8" />
            </div>
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-black border border-emerald-400/30 mb-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
                <span>نظام المزامنة والربط الشفاف Live v5.0</span>
              </div>
              <h1 className="text-2xl font-black tracking-wide">
                مركز المزامنة وفاحص السحابة المباشر
              </h1>
              <p className="text-xs text-indigo-200 mt-1 font-medium">
                رؤية لحظية شاملة لجميع الجداول السحابية مع إمكانية التعديل والحذف وشريط تقدم حي
              </p>
            </div>
          </div>

          {/* Quick Action Button */}
          <div className="flex flex-wrap items-center gap-3 w-full lg:w-auto">
            {onBack && (
              <button 
                onClick={onBack}
                className="flex items-center gap-2 px-5 py-3.5 rounded-2xl bg-white text-indigo-950 font-black text-xs md:text-sm hover:bg-slate-100 transition-all shadow-xl cursor-pointer active:scale-95"
                title="الرجوع للرئيسية"
              >
                <ArrowRight className="w-4 h-4" />
                <span>العودة للرئيسية ✕</span>
              </button>
            )}

            <button
              onClick={handleDeepPurgeAndSync}
              disabled={isSyncing}
              className="flex-1 lg:flex-none flex items-center justify-center gap-2.5 px-5 py-3.5 rounded-2xl bg-gradient-to-r from-rose-600 to-amber-600 hover:from-rose-500 hover:to-amber-500 text-white font-black text-sm transition-all shadow-xl hover:scale-105 active:scale-95 cursor-pointer disabled:opacity-50 border border-white/20"
              title="تطهير ومسح أي تكرارات أو أحرف مفردة من السحابة وإعادة رفع البيانات النقية"
            >
              <Sparkles className="w-5 h-5 text-yellow-300" />
              <span>تطهير جذري ومزامنة نقية 🧼</span>
            </button>

            <button
              onClick={handleStartExport}
              disabled={isSyncing}
              className="flex-1 lg:flex-none flex items-center justify-center gap-2.5 px-6 py-3.5 rounded-2xl bg-amber-400 hover:bg-amber-300 text-slate-950 font-black text-sm transition-all shadow-xl hover:scale-105 active:scale-95 cursor-pointer disabled:opacity-50"
            >
              <UploadCloud className={`w-5 h-5 ${isSyncing ? 'animate-bounce' : ''}`} />
              <span>{isSyncing ? 'جاري الرفع خطوة بخطوة...' : 'بدء الرفع السحابي الشامل ⚡'}</span>
            </button>

            <button
              onClick={loadCloudStats}
              disabled={isLoadingStats}
              className="p-3 rounded-2xl bg-white/10 hover:bg-white/20 text-white font-black text-xs transition-all border border-white/10 cursor-pointer"
              title="تحديث إحصاءات السحابة الحية"
            >
              <RefreshCw className={`w-5 h-5 ${isLoadingStats ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </div>

      {/* Grid: Live Progress Track & Smart QR Side-by-Side */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left 2 Cols: Step-by-Step Live Progress Track */}
        <div className="lg:col-span-2 bg-white p-6 rounded-3xl border-3 border-indigo-600 shadow-xl space-y-6">
          <div className="flex items-center justify-between border-b pb-4">
            <div className="flex items-center gap-2.5">
              <Zap className="w-6 h-6 text-amber-500" />
              <div>
                <h3 className="text-base font-black text-slate-900">مسار التقدم اللحظي للرفع (Live Progress)</h3>
                <p className="text-xs text-slate-500 font-bold">تتبع كل جدول وسجل أثناء رفعه إلى السحابة</p>
              </div>
            </div>
            <div className="text-left">
              <span className="text-2xl font-black text-indigo-600 font-mono">{syncPercent}%</span>
            </div>
          </div>

          {/* Master Progress Bar */}
          <div className="relative w-full h-4 bg-slate-100 rounded-full overflow-hidden border border-slate-200 shadow-inner">
            <div 
              className="h-full bg-gradient-to-r from-indigo-500 via-blue-500 to-emerald-500 transition-all duration-500 rounded-full"
              style={{ width: `${syncPercent}%` }}
            />
          </div>

          {/* Live Steps List */}
          <div className="space-y-3">
            {syncSteps.length === 0 && !isSyncing && (
              <div className="py-10 text-center text-slate-400 font-bold text-xs border-2 border-dashed border-slate-200 rounded-2xl">
                اضغط على زر <span className="text-indigo-600 font-black">"بدء الرفع السحابي الشامل"</span> أعلاه لتشغيل المسار ورؤية شريط التقدم اللحظي.
              </div>
            )}

            {syncSteps.map((step) => (
              <div 
                key={step.id} 
                className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between gap-3 ${
                  step.status === 'success' ? 'bg-emerald-50/80 border-emerald-300 text-emerald-950' :
                  step.status === 'warning' ? 'bg-amber-50 border-amber-300 text-amber-950' :
                  step.status === 'active' ? 'bg-indigo-50 border-indigo-300 text-indigo-950 ring-2 ring-indigo-200 animate-pulse' :
                  step.status === 'error' ? 'bg-rose-50 border-rose-300 text-rose-950' :
                  'bg-slate-50 border-slate-200 text-slate-600'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="shrink-0">
                    {step.status === 'success' && <CheckCircle2 className="w-5 h-5 text-emerald-600" />}
                    {step.status === 'warning' && <AlertTriangle className="w-5 h-5 text-amber-600" />}
                    {step.status === 'active' && <RefreshCw className="w-5 h-5 text-indigo-600 animate-spin" />}
                    {step.status === 'error' && <AlertCircle className="w-5 h-5 text-rose-600" />}
                    {step.status === 'pending' && <span className="w-5 h-5 rounded-full border-2 border-slate-300 block" />}
                  </div>
                  <div>
                    <div className="text-xs font-black flex items-center gap-2">
                      <span>الخطوة {step.stepIndex}: {step.title}</span>
                      {step.count !== undefined && step.count > 0 && (
                        <span className="px-2 py-0.5 rounded-full bg-white text-[10px] font-black text-slate-800 border shadow-xs">
                          {step.count} سجل
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-slate-600 font-medium mt-0.5">{step.details}</p>
                  </div>
                </div>

                <div className="shrink-0 font-mono font-black text-xs">
                  {step.percent}%
                </div>
              </div>
            ))}
          </div>

          {/* Final Result Notification */}
          {syncFinalResult && (
            <div className={`p-4 rounded-2xl border-2 font-black text-xs flex items-center gap-3 ${
              syncFinalResult.success ? 'bg-emerald-100 border-emerald-400 text-emerald-950' : 'bg-rose-100 border-rose-400 text-rose-950'
            }`}>
              {syncFinalResult.success ? <CheckCircle2 className="w-6 h-6 text-emerald-700 shrink-0" /> : <AlertCircle className="w-6 h-6 text-rose-700 shrink-0" />}
              <span>{syncFinalResult.message}</span>
            </div>
          )}
        </div>

        {/* Right 1 Col: Smart QR Hub (Teacher / Student / Principal) */}
        <div className="bg-white p-5 rounded-3xl border-3 border-indigo-600 shadow-xl flex flex-col items-center text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-black border border-indigo-100">
            <QrCode className="w-4 h-4 text-indigo-600" />
            <span>باركود وأكواد الربط الثلاثي ⚡</span>
          </div>

          {/* 3-Role Switching Tabs */}
          <div className="flex w-full bg-slate-100 p-1.5 rounded-2xl gap-1">
            <button
              type="button"
              onClick={() => setActiveQrRole('teacher')}
              className={`flex-1 py-1.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1 ${
                activeQrRole === 'teacher'
                  ? 'bg-indigo-600 text-white shadow-md'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <span>👨‍🏫 الأستاذ</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveQrRole('student')}
              className={`flex-1 py-1.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1 ${
                activeQrRole === 'student'
                  ? 'bg-emerald-600 text-white shadow-md'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <span>🎓 الطالب</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveQrRole('principal')}
              className={`flex-1 py-1.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1 ${
                activeQrRole === 'principal'
                  ? 'bg-amber-600 text-white shadow-md'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <span>👑 المدير</span>
            </button>
          </div>

          <p className="text-[11px] text-slate-500 font-bold">
            {activeQrRole === 'teacher' && 'امسح الباركود من تطبيق الأستاذ للربط اللحظي بسجل الدرجات والجدول'}
            {activeQrRole === 'student' && 'امسح الباركود من تطبيق الطالب لعرض الجدول ونتائج الدرجات'}
            {activeQrRole === 'principal' && 'امسح الباركود من تطبيق المدير للإدارة الشاملة والمصادقة الحية'}
          </p>

          {/* QR Image Box with dynamic border color */}
          <div className={`p-3 bg-white rounded-2xl border-3 shadow-lg relative group transition-all ${
            activeQrRole === 'teacher' ? 'border-indigo-400' : activeQrRole === 'student' ? 'border-emerald-400' : 'border-amber-400'
          }`}>
            {qrDataUrl ? (
              <img src={qrDataUrl} alt={`QR Code ${activeQrRole}`} className="w-44 h-44 object-contain rounded-xl" />
            ) : (
              <div className="w-44 h-44 bg-slate-100 rounded-xl flex items-center justify-center text-slate-400 text-xs font-bold">
                جاري توليد الباركود...
              </div>
            )}
          </div>

          {/* Pairing Code Card */}
          <div className="w-full bg-slate-50 p-3 rounded-2xl border border-slate-200 space-y-1.5">
            <div className="text-[11px] font-bold text-slate-500">
              {activeQrRole === 'teacher' && 'كود اقتران الأستاذ (6 أرقام):'}
              {activeQrRole === 'student' && 'كود اقتران الطالب (6 أرقام):'}
              {activeQrRole === 'principal' && 'كود اقتران المدير (6 أرقام):'}
            </div>
            <div className="flex items-center justify-center gap-2">
              <span className={`font-mono text-2xl font-black tracking-widest bg-white px-3 py-1 rounded-xl border shadow-xs ${
                activeQrRole === 'teacher' ? 'text-indigo-700' : activeQrRole === 'student' ? 'text-emerald-700' : 'text-amber-700'
              }`}>
                {activeQrRole === 'teacher' ? pairingCode : activeQrRole === 'student' ? studentPairingCode : principalPairingCode}
              </span>
              <button
                type="button"
                onClick={copyCurrentCode}
                className={`p-2 rounded-xl text-white font-bold transition-all cursor-pointer shadow-md ${
                  activeQrRole === 'teacher' ? 'bg-indigo-600 hover:bg-indigo-700' : activeQrRole === 'student' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-amber-600 hover:bg-amber-700'
                }`}
                title="نسخ الرمز"
              >
                {copied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
              </button>
            </div>
            {copied && <span className="text-[10px] font-black text-emerald-600 block">تم نسخ الرمز! ✓</span>}
          </div>

          {/* Quick 3-Codes Summary Pill */}
          <div className="w-full grid grid-cols-3 gap-1 pt-1 text-[10px] font-black">
            <div 
              onClick={() => setActiveQrRole('teacher')}
              className={`p-1.5 rounded-lg border cursor-pointer transition-all ${activeQrRole === 'teacher' ? 'bg-indigo-50 border-indigo-300 text-indigo-800' : 'bg-slate-50 text-slate-600'}`}
            >
              <div>مدرس</div>
              <div className="font-mono text-xs">{pairingCode}</div>
            </div>
            <div 
              onClick={() => setActiveQrRole('student')}
              className={`p-1.5 rounded-lg border cursor-pointer transition-all ${activeQrRole === 'student' ? 'bg-emerald-50 border-emerald-300 text-emerald-800' : 'bg-slate-50 text-slate-600'}`}
            >
              <div>طالب</div>
              <div className="font-mono text-xs">{studentPairingCode}</div>
            </div>
            <div 
              onClick={() => setActiveQrRole('principal')}
              className={`p-1.5 rounded-lg border cursor-pointer transition-all ${activeQrRole === 'principal' ? 'bg-amber-50 border-amber-300 text-amber-800' : 'bg-slate-50 text-slate-600'}`}
            >
              <div>مدير</div>
              <div className="font-mono text-xs">{principalPairingCode}</div>
            </div>
          </div>

          <div className="text-[11px] text-slate-400 font-medium">
            المعرف: <span className="font-mono font-bold text-slate-600">{schoolId}</span>
          </div>
        </div>

      </div>

      {/* Live Cloud Data Inspector Grid */}
      <div className="bg-white p-6 rounded-3xl border-3 border-indigo-600 shadow-xl space-y-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b pb-4">
          <div className="flex items-center gap-2.5">
            <Database className="w-6 h-6 text-indigo-600" />
            <div>
              <h3 className="text-base font-black text-slate-900">مستكشف بيانات السحابة الحية (Cloud Live Inspector)</h3>
              <p className="text-xs text-slate-500 font-bold">
                قراءة حية ومباشرة من قاعدة بيانات Supabase | آخر فحص: {cloudStats?.lastChecked || 'الآن'}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => {
                const el = document.getElementById('cloud-purge-hub-section');
                if (el) el.scrollIntoView({ behavior: 'smooth' });
              }}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-300 text-xs font-black transition-all cursor-pointer shadow-xs active:scale-95"
              title="الانتقال الفوري إلى لوحة التصفير والتفريغ السحابي"
            >
              <Trash2 className="w-3.5 h-3.5 text-rose-600" />
              <span>لوحة التصفير والمسح السحابي 🧹</span>
            </button>

            <button
              onClick={loadCloudStats}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-black transition-all border cursor-pointer"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoadingStats ? 'animate-spin' : ''}`} />
              <span>تحديث العدادات</span>
            </button>
          </div>
        </div>

        {/* 8 Stats Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {statCards.map((card) => {
            const Icon = card.icon;
            return (
              <div 
                key={card.id}
                className="p-4 rounded-2xl bg-slate-50 border border-slate-200 hover:border-indigo-400 transition-all flex flex-col justify-between space-y-3 group shadow-xs hover:shadow-md"
              >
                <div className="flex items-center justify-between">
                  <div className={`p-2.5 rounded-xl bg-gradient-to-br ${card.color} text-white shadow-md`}>
                    <Icon className="w-5 h-5" />
                  </div>
                  <span className="text-2xl font-black font-mono text-slate-900 group-hover:text-indigo-600 transition-colors">
                    {card.count}
                  </span>
                </div>

                <div>
                  <h4 className="text-xs font-black text-slate-800">{card.title}</h4>
                  <p className="text-[10px] text-slate-400 font-mono mt-0.5">table: {card.tableName}</p>
                </div>

                <div className="flex items-center gap-1.5 pt-1">
                  <button
                    onClick={() => handleOpenTablePreview(card.tableName, card.title)}
                    className="flex-1 py-1.5 rounded-xl bg-white hover:bg-indigo-600 hover:text-white text-indigo-700 text-xs font-black border border-indigo-200 flex items-center justify-center gap-1 transition-all cursor-pointer shadow-xs"
                    title="معاينة وتعديل سجلات الجدول"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    <span>معاينة 👁️</span>
                  </button>
                  {card.tableName !== 'schools' && (
                    <button
                      onClick={() => handleClearTableDirectly(card.tableName, card.title)}
                      className="px-2.5 py-1.5 rounded-xl bg-rose-100 hover:bg-rose-600 hover:text-white text-rose-800 text-xs font-black border border-rose-300 flex items-center justify-center gap-1 transition-all cursor-pointer shadow-xs active:scale-95"
                      title={`تصفير وتفريغ جدول (${card.title}) من السحابة`}
                    >
                      <Trash2 className="w-3.5 h-3.5 text-rose-600 group-hover:text-white" />
                      <span>تفريغ / مسح 🗑️</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Cloud Purge & Reset Control Hub */}
      <div id="cloud-purge-hub-section" className="bg-gradient-to-r from-rose-950 via-slate-900 to-indigo-950 p-6 rounded-3xl border-3 border-rose-500 shadow-xl text-white space-y-4 scroll-mt-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-white/10 pb-3">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-rose-500 rounded-2xl shadow-md">
              <Trash2 className="w-6 h-6 text-white" />
            </div>
            <div>
              <h3 className="text-base font-black text-rose-300">
                لوحة تصفير وإعادة تعيين بيانات السحابة (Cloud Purge & Reset Hub)
              </h3>
              <p className="text-xs text-slate-300 font-bold">
                أزرار فورية لتصفير معلومات معينة في السحابة لبدء دورة امتحانية أو سنة دراسية جديدة
              </p>
            </div>
          </div>
          <span className="text-[11px] font-mono px-3 py-1 bg-white/10 rounded-full text-rose-200 border border-rose-400/30">
            School ID: {schoolId}
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 pt-1">
          <button
            onClick={() => handleClearTableDirectly('grades', 'الدرجات المرصودة')}
            className="flex items-center justify-between p-3.5 rounded-2xl bg-rose-900/40 hover:bg-rose-800/80 border border-rose-500/50 hover:border-rose-400 text-right transition-all cursor-pointer shadow-xs group"
          >
            <div className="flex items-center gap-2.5">
              <ShieldCheck className="w-5 h-5 text-rose-400 group-hover:scale-110 transition-transform" />
              <div>
                <div className="text-xs font-black text-white">تصفير سجل الدرجات السحابي</div>
                <div className="text-[10px] text-slate-400">مسح كافة درجات الشهور والفصول والنهائي</div>
              </div>
            </div>
            <Trash2 className="w-4 h-4 text-rose-400" />
          </button>

          <button
            onClick={() => handleClearTableDirectly('attendance', 'الغيابات المسجلة')}
            className="flex items-center justify-between p-3.5 rounded-2xl bg-purple-900/40 hover:bg-purple-800/80 border border-purple-500/50 hover:border-purple-400 text-right transition-all cursor-pointer shadow-xs group"
          >
            <div className="flex items-center gap-2.5">
              <Calendar className="w-5 h-5 text-purple-400 group-hover:scale-110 transition-transform" />
              <div>
                <div className="text-xs font-black text-white">تصفير سجل الغيابات السحابي</div>
                <div className="text-[10px] text-slate-400">مسح سجلات الحضور والغياب اليومية</div>
              </div>
            </div>
            <Trash2 className="w-4 h-4 text-purple-400" />
          </button>

          <button
            onClick={() => handleClearTableDirectly('schedules', 'الجدول الأسبوعي')}
            className="flex items-center justify-between p-3.5 rounded-2xl bg-amber-900/40 hover:bg-amber-800/80 border border-amber-500/50 hover:border-amber-400 text-right transition-all cursor-pointer shadow-xs group"
          >
            <div className="flex items-center gap-2.5">
              <Calendar className="w-5 h-5 text-amber-400 group-hover:scale-110 transition-transform" />
              <div>
                <div className="text-xs font-black text-white">تصفير جدول الحصص الأسبوعي</div>
                <div className="text-[10px] text-slate-400">تفريغ خريطة الجدول السحابي لتوليد جدول جديد</div>
              </div>
            </div>
            <Trash2 className="w-4 h-4 text-amber-400" />
          </button>

          <button
            onClick={() => handleClearTableDirectly('students', 'الطلاب المسجلون')}
            className="flex items-center justify-between p-3.5 rounded-2xl bg-emerald-900/40 hover:bg-emerald-800/80 border border-emerald-500/50 hover:border-emerald-400 text-right transition-all cursor-pointer shadow-xs group"
          >
            <div className="flex items-center gap-2.5">
              <GraduationCap className="w-5 h-5 text-emerald-400 group-hover:scale-110 transition-transform" />
              <div>
                <div className="text-xs font-black text-white">تصفير سجل الطلاب السحابي</div>
                <div className="text-[10px] text-slate-400">مسح قوائم الطلاب السحابية للعام الجديد</div>
              </div>
            </div>
            <Trash2 className="w-4 h-4 text-emerald-400" />
          </button>

          <button
            onClick={() => handleClearTableDirectly('subject_assignments', 'الرموز وتفويض الكادر')}
            className="flex items-center justify-between p-3.5 rounded-2xl bg-indigo-900/40 hover:bg-indigo-800/80 border border-indigo-500/50 hover:border-indigo-400 text-right transition-all cursor-pointer shadow-xs group"
          >
            <div className="flex items-center gap-2.5">
              <Key className="w-5 h-5 text-indigo-400 group-hover:scale-110 transition-transform" />
              <div>
                <div className="text-xs font-black text-white">تصفير الرموز السرية والتفويض</div>
                <div className="text-[10px] text-slate-400">إلغاء أكواد الربط لتوليد تفويضات جديدة للكادر</div>
              </div>
            </div>
            <Trash2 className="w-4 h-4 text-indigo-400" />
          </button>

          <button
            onClick={handleDeepPurgeAndSync}
            disabled={isSyncing}
            className="flex items-center justify-between p-3.5 rounded-2xl bg-gradient-to-r from-rose-600 to-amber-600 hover:from-rose-500 hover:to-amber-500 border border-amber-300/40 text-right transition-all cursor-pointer shadow-md group disabled:opacity-50"
          >
            <div className="flex items-center gap-2.5">
              <Sparkles className="w-5 h-5 text-yellow-300 group-hover:rotate-12 transition-transform" />
              <div>
                <div className="text-xs font-black text-white">تطهير جذري شامل ومزامنة نقية 🧼</div>
                <div className="text-[10px] text-amber-100">مسح المكررات والحروف ثم إعادة رفع البيانات</div>
              </div>
            </div>
            <RefreshCw className={`w-4 h-4 text-white ${isSyncing ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Cloud Data Preview Modal with Editing & Deletion */}
      {previewTableName && (
        <Portal>
          <div 
            onClick={(e) => { if (e.target === e.currentTarget) setPreviewTableName(null); }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-sm animate-fade-in"
          >
            <div className="bg-white w-full max-w-6xl max-h-[90vh] rounded-3xl border-4 border-indigo-600 shadow-2xl flex flex-col overflow-hidden">
              
              {/* Modal Header */}
              <div className="p-5 bg-gradient-to-r from-indigo-900 to-blue-900 text-white flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shrink-0">
                <div className="flex items-center gap-3">
                  <Database className="w-6 h-6 text-amber-400 shrink-0" />
                  <div>
                    <h3 className="text-base font-black">
                      مستكشف وتعديل السجلات الحية: {previewTableTitle}
                    </h3>
                    <p className="text-xs text-indigo-200 font-mono">
                      جدول: {previewTableName} | إجمالي المعروض: {previewRows.length} سجل
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-stretch sm:self-auto justify-between sm:justify-end">
                  {previewTableName !== 'schools' && (
                    <button
                      onClick={handleClearEntireTable}
                      className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-black text-xs transition-all cursor-pointer shadow-sm"
                      title="تفريغ هذا الجدول بالكامل من السحابة"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      <span>تفريغ هذا الجدول بالكامل 🗑️</span>
                    </button>
                  )}

                  <button
                    onClick={() => setPreviewTableName(null)}
                    className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white cursor-pointer transition-all"
                    title="إغلاق النافذة"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              </div>

            {/* Notification message */}
            {actionMsg && (
              <div className={`p-3 text-xs font-black text-center flex items-center justify-center gap-2 ${
                actionMsg.type === 'success' ? 'bg-emerald-100 text-emerald-950 border-b border-emerald-300' : 'bg-rose-100 text-rose-950 border-b border-rose-300'
              }`}>
                {actionMsg.type === 'success' ? <CheckCircle2 className="w-4 h-4 text-emerald-600" /> : <AlertCircle className="w-4 h-4 text-rose-600" />}
                <span>{actionMsg.text}</span>
              </div>
            )}

            {/* Modal Body: Table Content */}
            <div className="p-6 overflow-y-auto flex-1 text-xs">
              {isLoadingPreview ? (
                <div className="py-16 text-center text-slate-500 font-black flex flex-col items-center gap-3">
                  <RefreshCw className="w-8 h-8 text-indigo-600 animate-spin" />
                  <span>جاري جلب السجلات الحية مباشرة من خادم Supabase...</span>
                </div>
              ) : previewRows.length === 0 ? (
                <div className="py-16 text-center text-slate-400 font-bold text-sm border-2 border-dashed border-slate-200 rounded-2xl space-y-3">
                  <p>لا توجد بيانات مخزنة حالياً في جدول <span className="font-mono text-indigo-600 font-bold">{previewTableName}</span> لهذه المدرسة.</p>
                  <p className="text-xs text-slate-500">اضغط على "بدء الرفع السحابي الشامل" لرفع البيانات، أو اضغط أدناه لتأكيد تفريغ ومسح أي سجلات سحابية.</p>
                  {previewTableName !== 'schools' && (
                    <div className="pt-2">
                      <button
                        onClick={handleClearEntireTable}
                        className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-black text-xs inline-flex items-center gap-2 cursor-pointer shadow-md active:scale-95"
                      >
                        <Trash2 className="w-4 h-4" />
                        <span>تأكيد تفريغ ومسح هذا الجدول من السحابة 🗑️</span>
                      </button>
                    </div>
                  )}
                </div>
              ) : (
                <div className="overflow-x-auto border rounded-2xl shadow-sm">
                  <table className="w-full text-center border-collapse">
                    <thead>
                      <tr className="bg-slate-100 font-black text-slate-800 border-b text-xs">
                        <th className="py-3 px-3 border-l w-12 text-center">ت</th>
                        {Object.keys(previewRows[0] || {}).map((key) => (
                          <th key={key} className="py-3 px-3 border-l text-slate-700 whitespace-nowrap font-mono text-[11px]">
                            {key}
                          </th>
                        ))}
                        <th className="py-3 px-3 text-center whitespace-nowrap w-28 bg-slate-200/60">
                          الإجراءات
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y text-slate-900 font-medium text-xs">
                      {previewRows.map((row, idx) => {
                        const isEditing = editingRowIdx === idx;
                        return (
                          <tr key={idx} className="hover:bg-indigo-50/50 transition-colors">
                            <td className="py-2 px-3 border-l bg-slate-50 font-mono font-bold text-center">{idx + 1}</td>
                            
                            {Object.keys(row).map((key) => {
                              const val = isEditing ? editingRowData[key] : row[key];
                              return (
                                <td key={key} className="py-2 px-3 border-l text-right whitespace-nowrap max-w-[220px] truncate font-mono text-[11px]">
                                  {isEditing && key !== 'id' && key !== 'school_id' && key !== 'created_at' && typeof val !== 'object' ? (
                                    <input
                                      type="text"
                                      value={val ?? ''}
                                      onChange={(e) => setEditingRowData({ ...editingRowData, [key]: e.target.value })}
                                      className="w-full px-2 py-1 rounded border border-indigo-400 bg-white font-mono text-xs focus:ring-1 focus:ring-indigo-500"
                                    />
                                  ) : (
                                    typeof val === 'object' && val !== null 
                                      ? JSON.stringify(val) 
                                      : String(val ?? '')
                                  )}
                                </td>
                              );
                            })}

                            {/* Actions Column (Edit & Delete) */}
                            <td className="py-2 px-3 text-center whitespace-nowrap bg-slate-50/50">
                              <div className="flex items-center justify-center gap-1.5">
                                {isEditing ? (
                                  <>
                                    <button
                                      onClick={() => handleSaveEditRow(row, idx)}
                                      className="p-1.5 rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 transition-all cursor-pointer shadow-xs"
                                      title="حفظ التعديل في السحابة"
                                    >
                                      <Check className="w-3.5 h-3.5" />
                                    </button>
                                    <button
                                      onClick={() => setEditingRowIdx(null)}
                                      className="p-1.5 rounded-lg bg-slate-300 text-slate-700 hover:bg-slate-400 transition-all cursor-pointer"
                                      title="إلغاء التعديل"
                                    >
                                      <X className="w-3.5 h-3.5" />
                                    </button>
                                  </>
                                ) : (
                                  <>
                                    <button
                                      onClick={() => handleStartEditRow(row, idx)}
                                      className="p-1.5 rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-600 hover:text-white transition-all cursor-pointer shadow-xs border border-indigo-200"
                                      title="تعديل هذا السجل في السحابة"
                                    >
                                      <Edit2 className="w-3.5 h-3.5" />
                                    </button>
                                    <button
                                      onClick={() => handleDeleteRow(row, idx)}
                                      className="p-1.5 rounded-lg bg-rose-50 text-rose-700 hover:bg-rose-600 hover:text-white transition-all cursor-pointer shadow-xs border border-rose-200"
                                      title="حذف هذا السجل نهائياً من السحابة"
                                    >
                                      <Trash2 className="w-3.5 h-3.5" />
                                    </button>
                                  </>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Modal Footer with Clear & Close Buttons */}
            <div className="p-4 bg-slate-50 border-t flex flex-col sm:flex-row items-center justify-between gap-3 shrink-0">
              <span className="text-xs text-slate-500 font-bold">
                يمكنك تعديل أي قيمة وحفظها مباشرة، أو تفريغ الجدول بالكامل من السحابة بنقرة واحدة.
              </span>
              <div className="flex items-center gap-2 self-stretch sm:self-auto justify-end">
                {previewTableName !== 'schools' && (
                  <button
                    onClick={handleClearEntireTable}
                    className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-black text-xs flex items-center gap-1.5 transition-all cursor-pointer shadow-md active:scale-95"
                    title="تفريغ هذا الجدول بالكامل من السحابة"
                  >
                    <Trash2 className="w-4 h-4" />
                    <span>تفريغ ومسح جدول ({previewTableTitle}) 🗑️</span>
                  </button>
                )}
                <button
                  onClick={() => setPreviewTableName(null)}
                  className="px-5 py-2 rounded-xl bg-slate-800 hover:bg-slate-900 text-white font-black text-xs cursor-pointer transition-all active:scale-95"
                >
                  إغلاق النافذة ✕
                </button>
              </div>
            </div>

          </div>
        </div>
      </Portal>
      )}

    </div>
  );
};
