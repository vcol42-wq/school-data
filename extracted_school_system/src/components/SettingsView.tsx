import React, { useState } from 'react';
import { AppConfig, Student, StaffMember } from '../types';
import { 
  Settings, 
  Lock, 
  UserCheck, 
  Building2, 
  Clock, 
  Image as ImageIcon, 
  RotateCcw, 
  Save, 
  Key, 
  ShieldCheck,
  CheckCircle2,
  Sparkles,
  Trash2,
  Users,
  Download,
  Upload,
  CalendarPlus,
  CloudLightning,
  Cloud,
  Search,
  AlertTriangle,
  Loader2
} from 'lucide-react';
import { refreshSupabaseClient, getSupabase } from '../utils/supabaseClient';
import { purgeSchoolDataFromCloud, searchSchoolsInSupabase, restoreSchoolData, CloudSchoolSummary } from '../utils/syncService';

interface SettingsViewProps {
  config: AppConfig;
  setConfig: React.Dispatch<React.SetStateAction<AppConfig>>;
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  staffList: StaffMember[];
  setStaffList: React.Dispatch<React.SetStateAction<StaffMember[]>>;
  onResetData: () => void;
  onTriggerScreensaver: () => void;
  onTriggerSplash: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  config,
  setConfig,
  students,
  setStudents,
  staffList,
  setStaffList,
  onResetData,
  onTriggerScreensaver,
  onTriggerSplash
}) => {
  const [isAuthenticated, setIsAuthenticated] = useState(true);
  const [enteredPasscode, setEnteredPasscode] = useState('');
  const [authError, setAuthError] = useState('');

  const [formConfig, setFormConfig] = useState<AppConfig>({ ...config });

  const [selectedGradePurge, setSelectedGradePurge] = useState('الصف الأول');
  const [selectedFilterType, setSelectedFilterType] = useState<'all' | 'passed' | 'failed'>('all');

  const getStudentAverage = (std: Student) => {
    const currentYear = std.registrationYear || '2024-2025';
    const marks = (std.marksHistory || []).filter(m => m.year === currentYear);
    if (marks.length === 0) return 0;
    const sum = marks.reduce((acc, m) => acc + (m.finalGrade || m.total || 0), 0);
    return sum / marks.length;
  };

  const handleSelectivePurge = () => {
    let filterName = '';
    if (selectedFilterType === 'all') filterName = 'كل طلاب هذا الصف';
    if (selectedFilterType === 'passed') filterName = 'الطلاب الناجحين فقط (معدل >= 50)';
    if (selectedFilterType === 'failed') filterName = 'الطلاب الراسبين فقط (معدل < 50)';

    if (!confirm(`تحذير: هل أنت متأكد من تنفيذ المسح والفلترة للطلاب في [${selectedGradePurge}] لفلتر [${filterName}]؟ سيتم حذفهم نهائياً من قاعدة البيانات.`)) {
      return;
    }

    setStudents(prev => {
      return prev.filter(std => {
        const matchesGrade = std.currentGrade === selectedGradePurge;
        if (!matchesGrade) return true; // Keep other grades

        const avg = getStudentAverage(std);
        const isPass = avg >= 50;

        if (selectedFilterType === 'all') {
          return false; // delete all of this grade
        }
        if (selectedFilterType === 'passed') {
          return !isPass; // Keep failed, delete passed
        }
        if (selectedFilterType === 'failed') {
          return isPass; // Keep passed, delete failed
        }
        return true;
      });
    });

    alert('تم تنفيذ الحذف والفلترة بنجاح!');
  };

  const handlePurgeAllStudents = () => {
    if (confirm('⚠️ تحذير خطير جداً: هل أنت متأكد من تصفير وحذف قائمة جميع الطلاب المستمرين بالكامل؟ لا يمكن التراجع عن هذا الإجراء.')) {
      setStudents([]);
      alert('تم حذف وتصفير قائمة الطلاب بالكامل بنجاح!');
    }
  };

  const handlePurgeAllStaff = () => {
    if (confirm('⚠️ تحذير خطير جداً: هل أنت متأكد من تصفير وحذف جميع كادر التدريس بالكامل؟ لا يمكن التراجع عن هذا الإجراء.')) {
      setStaffList([]);
      alert('تم حذف وتصفير كادر التدريس بالكامل بنجاح!');
    }
  };

  const handlePurgeAllAbsences = () => {
    if (confirm('هل أنت متأكد من تصفير كافة غيابات الطلاب وإرجاع عداد الغياب إلى صفر لجميع الطلبة؟')) {
      setStudents(prev => prev.map(s => ({ ...s, absencesCount: 0 })));
      alert('تم تصفير غيابات جميع الطلاب بنجاح!');
    }
  };

  const handleNewAcademicYear = async () => {
    if (!confirm('🎓 هل تريد بدء سنة دراسية جديدة؟\n\n- سيتم تصفير الغيابات لكافة الطلبة (العودة إلى 0).\n- سيتم تصفير الدرجات القديمة في السحابة لتجهيز رصد السنة الجديدة.\n- سيتم الاحتفاظ بكافة أسماء وبيانات الطلاب وكادر المدرسين.')) {
      return;
    }
    setStudents(prev => prev.map(s => ({ ...s, absencesCount: 0 })));
    const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
    const res = await purgeSchoolDataFromCloud(schoolId, 'grades_and_attendance');
    if (res.success) {
      alert('✅ تم تهيئة النظام وبدء السنة الدراسية الجديدة بنجاح وتصفير السجلات السحابية القديمة!');
    } else {
      alert('تم التصفير محلياً مع تنبيه في السحابة: ' + res.message);
    }
  };

  const handlePurgeCloudData = async () => {
    if (!confirm('☁️ هل أنت متأكد من تصفير وتفريغ قاعدة بيانات المدرسة في السحابة؟ سيتم مسح بيانات المدرسة القديمة من السحابة لتجهيز تصدير نظيف جديد.')) {
      return;
    }
    const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
    const res = await purgeSchoolDataFromCloud(schoolId, 'all');
    if (res.success) {
      alert('✅ تم تفريغ وتصفير بيانات السحابة بنجاح!');
    } else {
      alert('خطأ في تصفير السحابة: ' + res.message);
    }
  };

  const handleExportBackup = () => {
    const backupData = {
      version: '5.0',
      exportDate: new Date().toISOString(),
      config,
      students,
      staffList,
      schedule: JSON.parse(localStorage.getItem('diyala_school_schedule') || '{}')
    };
    const blob = new Blob([JSON.stringify(backupData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `backup_${config.schoolName || 'school'}_${new Date().toISOString().slice(0,10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleImportBackup = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const parsed = JSON.parse(event.target?.result as string);
        if (parsed.students) setStudents(parsed.students);
        if (parsed.staffList) setStaffList(parsed.staffList);
        if (parsed.config) setConfig(parsed.config);
        if (parsed.schedule) localStorage.setItem('diyala_school_schedule', JSON.stringify(parsed.schedule));
        alert('✅ تم استرجاع النسخة الاحتياطية بنجاح!');
      } catch (err: any) {
        alert('فشل قراءة ملف النسخة الاحتياطية: ' + err.message);
      }
    };
    reader.readAsText(file);
  };

  const handlePurgeToRawSystem = () => {
    if (confirm('🚨 تحذير نهائي: هل أنت متأكد من مسح كافة تهيئات النظام والبيانات وإرجاع التطبيق للحالة الخام بالكامل؟ سيتم إغلاق التطبيق وإعادة إدخال معلومات المدرسة والمدير والرمز السري من جديد.')) {
      setStudents([]);
      setStaffList([]);
      setConfig({
        schoolName: '',
        managerName: '',
        directorateName: '',
        sectionName: '',
        passcode: '',
        developerCode: '9999',
        lessonDurationMinutes: 45,
        breakDurationMinutes: 10,
        schoolStartHour: '08:00',
        enableBellSound: true,
        enableScreensaver: true,
        splashImageUrl: 'https://images.unsplash.com/photo-1580582932707-520aed937b7b?auto=format&fit=crop&w=1200&q=80',
        screensaverImageUrl: 'https://images.unsplash.com/photo-1509062522246-3755977927d7?auto=format&fit=crop&w=1200&q=80'
      });
      localStorage.clear();
      alert('تم إرجاع النظام للحالة الخام بنجاح! يرجى إعادة تشغيل التطبيق للبدء من جديد.');
      window.location.reload();
    }
  };

  // Handle Security Login
  const handleAuthSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (enteredPasscode === config.passcode || enteredPasscode === config.developerCode) {
      setIsAuthenticated(true);
      setAuthError('');
    } else {
      setAuthError('رمز الدخول غير صحيح، يرجى كتابة رمز المدير أو المبرمج.');
    }
  };

  // Cloud Search State
  const [showCloudSearchModal, setShowCloudSearchModal] = useState(false);
  const [cloudSearchQuery, setCloudSearchQuery] = useState('');
  const [cloudSearchResults, setCloudSearchResults] = useState<CloudSchoolSummary[]>([]);
  const [isCloudSearching, setIsCloudSearching] = useState(false);
  const [hasCloudSearched, setHasCloudSearched] = useState(false);
  const [isRestoringSchool, setIsRestoringSchool] = useState(false);

  // Search Schools in Cloud
  const handleCloudSearch = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!cloudSearchQuery.trim()) return;
    setIsCloudSearching(true);
    setHasCloudSearched(true);
    try {
      const res = await searchSchoolsInSupabase(cloudSearchQuery);
      setCloudSearchResults(res);
    } catch (err) {
      console.error(err);
      alert('خطأ أثناء البحث في السحابة');
    } finally {
      setIsCloudSearching(false);
    }
  };

  // Restore Selected School from Cloud
  const handleRestoreFromCloud = async (school: CloudSchoolSummary) => {
    if (!confirm(`هل أنت متأكد من استعادة بيانات (${school.name}) وربط هذا التطبيق بها؟ سيتم سحب الطلاب والأساتذة والجدول المرتبط بهذه المدرسة.`)) return;
    setIsRestoringSchool(true);
    try {
      const res = await restoreSchoolData(school.id);
      if (res.success) {
        const studentCode = school.student_pairing_code || res.config?.studentPairingCode || '223344';
        const principalCode = school.principal_pairing_code || res.config?.principalPairingCode || '334455';

        const newConfig = {
          ...config,
          ...(res.config || {}),
          schoolName: school.name,
          schoolId: school.id,
          pairingCode: school.pairing_code,
          studentPairingCode: studentCode,
          principalPairingCode: principalCode,
          adminEmail: school.admin_email
        };

        setConfig(newConfig);
        setFormConfig(newConfig);
        if (res.students) setStudents(res.students);
        if (res.teachers) setStaffList(res.teachers);
        if (res.schedule) localStorage.setItem('diyala_school_schedule', JSON.stringify(res.schedule));

        localStorage.setItem('diyala_school_id', school.id);
        localStorage.setItem('diyala_school_pairing_code', school.pairing_code);
        localStorage.setItem('diyala_pairing_code', school.pairing_code);
        localStorage.setItem('diyala_student_pairing_code', studentCode);
        localStorage.setItem('diyala_principal_pairing_code', principalCode);
        localStorage.setItem('diyala_admin_email', school.admin_email);
        localStorage.setItem('diyala_school_name', school.name);

        refreshSupabaseClient(school.id);
        setShowCloudSearchModal(false);
        alert(`✅ تم استعادة بيانات (${school.name}) بنجاح!\nرمز المدرس: ${school.pairing_code}\nرمز الطالب: ${studentCode}\nرمز المدير: ${principalCode}`);
      } else {
        alert('⚠️ فشلت الاستعادة: ' + res.message);
      }
    } catch (e: any) {
      alert('خطأ أثناء استعادة المدرسة: ' + (e.message || ''));
    } finally {
      setIsRestoringSchool(false);
    }
  };

  // Save Settings Safely (without destroying data or regenerating IDs)
  const handleSaveConfig = (e: React.FormEvent) => {
    e.preventDefault();

    setConfig({ ...formConfig });
    localStorage.setItem('diyala_school_name', formConfig.schoolName || '');
    localStorage.setItem('diyala_admin_email', formConfig.adminEmail || '');
    localStorage.setItem('diyala_pairing_code', formConfig.pairingCode || '112233');
    localStorage.setItem('diyala_student_pairing_code', formConfig.studentPairingCode || '223344');
    localStorage.setItem('diyala_principal_pairing_code', formConfig.principalPairingCode || '334455');
    
    try {
      const schoolId = formConfig.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
      const client = getSupabase(schoolId);
      client.from('schools').upsert({
        id: schoolId,
        name: formConfig.schoolName,
        pairing_code: formConfig.pairingCode || '112233',
        admin_email: formConfig.adminEmail || '',
        config: {
          managerName: formConfig.managerName || '',
          directorateName: formConfig.directorateName || '',
          sectionName: formConfig.sectionName || '',
          schoolStage: formConfig.schoolStage || 'intermediate',
          schoolStartHour: formConfig.schoolStartHour || '08:00',
          lessonDurationMinutes: Number(formConfig.lessonDurationMinutes) || 45,
          breakDurationMinutes: Number(formConfig.breakDurationMinutes) || 10,
          studentPairingCode: formConfig.studentPairingCode || '223344',
          principalPairingCode: formConfig.principalPairingCode || '334455'
        }
      }, { onConflict: 'id' }).then(() => {});
    } catch (_) {}

    alert('تم حفظ كافة إعدادات النظام وتحديث التوقيتات وأكواد الاقتران الثلاثية بنجاح!');
  };

  if (!isAuthenticated) {
    return (
      <div className="max-w-md mx-auto px-4 py-16">
        <form onSubmit={handleAuthSubmit} className="bg-[var(--theme-card)] p-6 md:p-8 rounded-3xl border border-[var(--theme-card-border)] shadow-2xl space-y-5 text-center">
          <div className="w-16 h-16 rounded-2xl bg-amber-500/20 text-amber-600 mx-auto flex items-center justify-center">
            <Lock className="w-8 h-8" />
          </div>

          <div>
            <h2 className="text-xl font-extrabold text-[var(--theme-text-main)]">
              منطقة إعدادات الإدارة المحمية
            </h2>
            <p className="text-xs text-[var(--theme-text-muted)] mt-1">
              يرجى إدخال رمز دخول المدير (الافتراضي: 1234) أو رمز المبرمج لتعديل الإعدادات.
            </p>
          </div>

          <div className="space-y-2 text-right">
            <label className="block text-xs font-bold text-[var(--theme-text-main)]">رمز الدخول الأمني:</label>
            <input
              type="password"
              value={enteredPasscode}
              onChange={e => setEnteredPasscode(e.target.value)}
              placeholder="****"
              className="w-full text-center tracking-widest text-xl font-mono p-3.5 rounded-xl border-2 border-amber-400 bg-white text-slate-900 font-black placeholder-slate-400 focus:outline-none focus:border-amber-500 shadow-sm"
            />
            {authError && <span className="text-xs text-rose-600 font-bold block">{authError}</span>}
          </div>

          <button
            type="submit"
            className="w-full py-3 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 font-black text-sm shadow-lg transition-all"
          >
            تأكيد الرمز والدخول للإعدادات
          </button>
        </form>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
      
      {/* Title */}
      <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm flex items-center justify-between">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300 text-xs font-bold mb-2">
            <ShieldCheck className="w-4 h-4" />
            <span>لوحة تحكم إعدادات المدير والمبرمج</span>
          </div>
          <h2 className="text-2xl font-black text-[var(--theme-text-main)]">
            إعدادات النظام، التوقيتات، والرموز الأمنية
          </h2>
        </div>

        <button
          onClick={() => setIsAuthenticated(false)}
          className="px-3 py-1.5 rounded-lg bg-rose-50 text-rose-700 text-xs font-bold hover:bg-rose-100"
        >
          قفل الإعدادات 🔒
        </button>
      </div>

      {/* Cloud School Status & Direct Cloud Restore Bar */}
      <div className="p-4 rounded-2xl bg-gradient-to-r from-indigo-50 to-emerald-50 dark:from-indigo-950/30 dark:to-emerald-950/30 border border-indigo-200 dark:border-indigo-800 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-indigo-600 text-white rounded-xl shadow-md">
            <Building2 className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-black text-slate-800 dark:text-slate-100">
              {formConfig.schoolName ? `المدرسة النشطة: ${formConfig.schoolName}` : 'لم يتم تحديد مدرسة بعد'}
            </div>
            <div className="text-[11px] text-slate-500 dark:text-slate-400 font-mono mt-0.5">
              معرف المدرسة: <span className="font-bold text-indigo-600 dark:text-indigo-400">{formConfig.schoolId || 'غير محدد'}</span>
              {formConfig.adminEmail && <span> • {formConfig.adminEmail}</span>}
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={() => { setCloudSearchQuery(''); setCloudSearchResults([]); setHasCloudSearched(false); setShowCloudSearchModal(true); }}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-black shadow-md transition-all flex items-center gap-2 cursor-pointer"
        >
          <Search className="w-4 h-4" />
          <span>استعادة / تبديل المدرسة من السحابة (بالاسم/البريد/الرمز) ☁️</span>
        </button>
      </div>

      <form onSubmit={handleSaveConfig} className="space-y-6">
        
        {/* Section 1: School & Manager Details */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <UserCheck className="w-5 h-5 text-amber-500" />
            <span>1. بيانات المدرسة والمدير والمديرية</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">اسم مدير المدرسة المحترم:</label>
              <input
                type="text"
                required
                value={formConfig.managerName}
                onChange={e => setFormConfig(p => ({ ...p, managerName: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-white font-bold text-slate-900"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">اسم المدرسة الرسمي:</label>
              <input
                type="text"
                required
                value={formConfig.schoolName}
                onChange={e => setFormConfig(p => ({ ...p, schoolName: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-white font-bold text-slate-900"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">اسم مديرية التربية:</label>
              <input
                type="text"
                value={formConfig.directorateName}
                onChange={e => setFormConfig(p => ({ ...p, directorateName: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-white text-slate-900 font-bold"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">نوع ومرحلة المدرسة الرسميّة:</label>
              <select
                value={formConfig.schoolStage || 'intermediate'}
                onChange={e => setFormConfig(p => ({ ...p, schoolStage: e.target.value as any }))}
                className="w-full p-2.5 rounded-xl border bg-white font-bold text-indigo-600"
              >
                <option value="primary">🏫 مدرسة ابتدائية (من الأول إلى السادس الابتدائي)</option>
                <option value="intermediate">🏫 مدرسة متوسطة (الأول إلى الثالث متوسط)</option>
                <option value="preparatory">🏫 مدرسة إعدادية (الرابع، الخامس، السادس العلمي والأدبي)</option>
                <option value="secondary">🏫 مدرسة ثانوية متكاملة (متوسطة + إعدادية)</option>
              </select>
            </div>

            <div>
              <label className="block font-bold mb-1">القسم أو المنطقة التعليمية:</label>
              <input
                type="text"
                value={formConfig.sectionName}
                onChange={e => setFormConfig(p => ({ ...p, sectionName: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-white text-slate-900 font-bold"
              />
            </div>

            {/* 3-Role Pairing Codes Section */}
            <div className="col-span-full pt-4 mt-2 border-t border-slate-200 dark:border-slate-800">
              <h4 className="text-xs font-black text-slate-800 dark:text-slate-200 mb-3 flex items-center gap-1.5">
                <span>🔐 أكواد الاقتران والربط السحابي الثلاثية (للتطبيق الموحد)</span>
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                {/* Teacher Code */}
                <div className="p-3 bg-indigo-50/60 dark:bg-indigo-950/20 border border-indigo-200 rounded-xl space-y-1.5">
                  <span className="block text-[11px] font-black text-indigo-700 dark:text-indigo-300">
                    👨‍🏫 كود المدرس والمشرف:
                  </span>
                  <input
                    type="text"
                    value={formConfig.pairingCode || '112233'}
                    onChange={e => setFormConfig(p => ({ ...p, pairingCode: e.target.value }))}
                    className="w-full p-2 rounded-lg border bg-white text-center font-mono font-black text-sm text-indigo-700 tracking-widest"
                  />
                </div>

                {/* Student Code */}
                <div className="p-3 bg-emerald-50/60 dark:bg-emerald-950/20 border border-emerald-200 rounded-xl space-y-1.5">
                  <span className="block text-[11px] font-black text-emerald-700 dark:text-emerald-300">
                    🎓 كود الطالب وولي الأمر:
                  </span>
                  <input
                    type="text"
                    value={formConfig.studentPairingCode || '223344'}
                    onChange={e => setFormConfig(p => ({ ...p, studentPairingCode: e.target.value }))}
                    className="w-full p-2 rounded-lg border bg-white text-center font-mono font-black text-sm text-emerald-700 tracking-widest"
                  />
                </div>

                {/* Principal Code */}
                <div className="p-3 bg-amber-50/60 dark:bg-amber-950/20 border border-amber-200 rounded-xl space-y-1.5">
                  <span className="block text-[11px] font-black text-amber-800 dark:text-amber-300">
                    👑 كود وبوابة المدير:
                  </span>
                  <input
                    type="text"
                    value={formConfig.principalPairingCode || '334455'}
                    onChange={e => setFormConfig(p => ({ ...p, principalPairingCode: e.target.value }))}
                    className="w-full p-2 rounded-lg border bg-white text-center font-mono font-black text-sm text-amber-700 tracking-widest"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Section 2: Schedule & Bell Timings */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <Clock className="w-5 h-5 text-blue-500" />
            <span>2. ضبط أوقات جدول الحصص والمنبه</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">مدة الدرس الواحد (بالدقائق):</label>
              <input
                type="number"
                min={20}
                max={90}
                value={formConfig.lessonDurationMinutes}
                onChange={e => setFormConfig(p => ({ ...p, lessonDurationMinutes: Number(e.target.value) }))}
                className="w-full p-2.5 rounded-xl border bg-white font-bold text-blue-600"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">مدة الفرصة الاستراحة (بالدقائق):</label>
              <input
                type="number"
                min={5}
                max={40}
                value={formConfig.breakDurationMinutes}
                onChange={e => setFormConfig(p => ({ ...p, breakDurationMinutes: Number(e.target.value) }))}
                className="w-full p-2.5 rounded-xl border bg-white font-bold text-amber-600"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">ساعة بداية الدوام الرسمي:</label>
              <input
                type="time"
                value={formConfig.schoolStartHour}
                onChange={e => setFormConfig(p => ({ ...p, schoolStartHour: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-white font-bold text-emerald-600"
              />
            </div>
          </div>
        </div>

        {/* Section 3: Passcodes */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <Key className="w-5 h-5 text-purple-500" />
            <span>3. تغيير رموز الحماية للدخول (رمز المدير ورمز المبرمج)</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">رمز المدير للدخول للإعدادات:</label>
              <input
                type="text"
                value={formConfig.passcode}
                onChange={e => setFormConfig(p => ({ ...p, passcode: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-white font-mono font-bold"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">رمز المبرمج الرئيسي (Developer Code):</label>
              <input
                type="text"
                value={formConfig.developerCode}
                onChange={e => setFormConfig(p => ({ ...p, developerCode: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-white font-mono font-bold text-purple-600"
              />
            </div>
          </div>
        </div>

        {/* Section 4: Splash Screen & Screensaver photos */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <ImageIcon className="w-5 h-5 text-teal-500" />
            <span>4. صور واجهة الافتتاح وواجهة السكون</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">رابط صورة واجهة الافتتاح (Splash Image):</label>
              <input
                type="text"
                value={formConfig.splashImageUrl || ''}
                onChange={e => setFormConfig(p => ({ ...p, splashImageUrl: e.target.value }))}
                placeholder="https://..."
                className="w-full p-2.5 rounded-xl border bg-white text-xs font-mono"
              />
              <button
                type="button"
                onClick={onTriggerSplash}
                className="mt-2 text-[11px] text-teal-600 font-bold hover:underline"
              >
                معاينة واجهة الافتتاح الآن 🖼️
              </button>
            </div>

            <div>
              <label className="block font-bold mb-1">رابط صورة واجهة السكون (Screensaver Image):</label>
              <input
                type="text"
                value={formConfig.screensaverImageUrl || ''}
                onChange={e => setFormConfig(p => ({ ...p, screensaverImageUrl: e.target.value }))}
                placeholder="https://..."
                className="w-full p-2.5 rounded-xl border bg-white text-xs font-mono"
              />
              <button
                type="button"
                onClick={onTriggerScreensaver}
                className="mt-2 text-[11px] text-amber-600 font-bold hover:underline"
              >
                تشغيل واجهة السكون والتوقف يدويًا 💤
              </button>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
            <div>
              <span className="block text-xs font-bold text-[var(--theme-text-main)]">
                تفعيل شاشة التوقف التلقائية عند خمول النظام (Screensaver):
              </span>
              <span className="text-[11px] text-[var(--theme-text-muted)]">
                (تم إيقافها تلقائياً لتفادي إزعاج العمل أثناء الاستخدام)
              </span>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={formConfig.enableScreensaver || false}
                onChange={e => setFormConfig(p => ({ ...p, enableScreensaver: e.target.checked }))}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-300 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:right-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-amber-500"></div>
            </label>
          </div>
        </div>

        {/* Section 4.5: Administration Email and Gemini Key */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-indigo-500" />
            <span>5. إعدادات حساب إدارة المدرسة والذكاء الاصطناعي</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">البريد الإلكتروني للادارة (Email):</label>
              <input
                type="email"
                value={formConfig.adminEmail || ''}
                onChange={e => setFormConfig(p => ({ ...p, adminEmail: e.target.value }))}
                placeholder="school.admin@gmail.com"
                className="w-full p-2.5 rounded-xl border bg-white text-xs font-mono font-bold text-indigo-600 dark:text-indigo-400"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">حالة تفعيل ميزات الذكاء الاصطناعي (Gemini AI):</label>
              <div className="flex items-center gap-2 mb-2">
                <div className={`px-3 py-1.5 rounded-lg font-black text-[10px] flex items-center gap-1.5 ${
                  formConfig.adminEmail ? 'bg-emerald-100 text-emerald-800 border border-emerald-200' : 'bg-rose-100 text-rose-800'
                }`}>
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>{formConfig.adminEmail ? 'نشط تلقائياً (مرتبط بالبريد الإلكتروني) ✅' : 'غير نشط (يرجى إدخال البريد الإلكتروني)'}</span>
                </div>
              </div>
              <input
                type="password"
                value={formConfig.geminiApiKey || ''}
                onChange={e => setFormConfig(p => {
                  const updated = { ...p, geminiApiKey: e.target.value };
                  localStorage.setItem('gemini_api_key', e.target.value);
                  localStorage.setItem('diyala_school_gemini_key', e.target.value);
                  return updated;
                })}
                placeholder="أدخل مفتاح خاص (اختياري) أو اترك فارغاً للاعتماد على الربط الآلي"
                className="w-full p-2.5 rounded-xl border bg-white text-xs font-mono font-bold text-emerald-600 dark:text-emerald-400"
              />
              <span className="text-[10px] text-slate-500 block mt-1">
                عند إدخال البريد الإلكتروني الرسمي، يتم تفعيل البحث الذكي وتوليد الإحصائيات آلياً دون الحاجة لمفاتيح تقنية.
              </span>
            </div>
          </div>
        </div>

        {/* Section 6: Smart Data Purging Center */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border-2 border-rose-300 dark:border-rose-900/50 shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b border-rose-100 dark:border-rose-950 pb-2 flex items-center gap-2">
            <Trash2 className="w-5 h-5 text-rose-500" />
            <span>6. مركز تصفير البيانات وإدارة السنة الدراسية والنسخ الاحتياطي</span>
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs font-bold text-right">
            
            {/* 1. New Academic Year Setup */}
            <div className="p-4 rounded-2xl bg-indigo-50/50 dark:bg-indigo-950/20 border border-indigo-200 space-y-3 flex flex-col justify-between">
              <div>
                <div className="flex items-center gap-1.5 text-indigo-700 dark:text-indigo-300 border-b border-indigo-200 pb-1 mb-2">
                  <CalendarPlus className="w-4 h-4" />
                  <span>بدء سنة دراسية جديدة 🎓</span>
                </div>
                <p className="text-[11px] text-slate-600 dark:text-slate-400 font-normal leading-relaxed">
                  تصفير عداد الغيابات وتفريغ الدرجات القديمة في السحابة مع الحفاظ الكامل على سجلات الطلاب وكادر التدريس.
                </p>
              </div>

              <button
                type="button"
                onClick={handleNewAcademicYear}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-black transition-all cursor-pointer text-center shadow-md flex items-center justify-center gap-1.5"
              >
                <CalendarPlus className="w-4 h-4" />
                <span>تهيئة السنة الجديدة الآن</span>
              </button>
            </div>

            {/* 2. Backup & Restore */}
            <div className="p-4 rounded-2xl bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-200 space-y-3 flex flex-col justify-between">
              <div>
                <div className="flex items-center gap-1.5 text-emerald-700 dark:text-emerald-300 border-b border-emerald-200 pb-1 mb-2">
                  <Download className="w-4 h-4" />
                  <span>النسخ الاحتياطي والاستعادة 💾</span>
                </div>
                <p className="text-[11px] text-slate-600 dark:text-slate-400 font-normal leading-relaxed">
                  حفظ نسخة شاملة من كامل بيانات المدرسة (الطلاب، الكادر، الجدول، الإعدادات) كملف JSON واسترجاعها بأي وقت.
                </p>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={handleExportBackup}
                  className="py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold flex items-center justify-center gap-1"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>تصدير نسخة</span>
                </button>
                <label className="py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-xl font-bold flex items-center justify-center gap-1 cursor-pointer">
                  <Upload className="w-3.5 h-3.5" />
                  <span>استرجاع</span>
                  <input type="file" accept=".json" onChange={handleImportBackup} className="hidden" />
                </label>
              </div>
            </div>

            {/* 3. Cloud Wipe */}
            <div className="p-4 rounded-2xl bg-amber-50/50 dark:bg-amber-950/20 border border-amber-200 space-y-3 flex flex-col justify-between">
              <div>
                <div className="flex items-center gap-1.5 text-amber-700 dark:text-amber-300 border-b border-amber-200 pb-1 mb-2">
                  <CloudLightning className="w-4 h-4" />
                  <span>تصفير وتفريغ السحابة ☁️</span>
                </div>
                <p className="text-[11px] text-slate-600 dark:text-slate-400 font-normal leading-relaxed">
                  مسح كافة سجلات المدرسة المرفوعة على Supabase للبدء من جديد برفع نظيف وجديد كلياً.
                </p>
              </div>

              <button
                type="button"
                onClick={handlePurgeCloudData}
                className="w-full py-2.5 bg-amber-600 hover:bg-amber-700 text-white rounded-xl font-black transition-all cursor-pointer text-center shadow-md flex items-center justify-center gap-1.5"
              >
                <CloudLightning className="w-4 h-4" />
                <span>تصفير سحابة المدرسة</span>
              </button>
            </div>

          </div>

          {/* Selective & Bulk Purge Row */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-bold text-right pt-3 border-t border-rose-100 dark:border-rose-950">
            
            {/* Selective Purging */}
            <div className="p-4 rounded-2xl bg-rose-50/50 dark:bg-rose-950/10 border border-rose-200 space-y-3">
              <span className="block text-rose-800 dark:text-rose-300 border-b pb-1">مسح وحذف الطلاب بفلاتر محددة (انتقائي)</span>
              
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block mb-1 text-slate-500">اختر الصف الدراسي:</label>
                  <select
                    value={selectedGradePurge}
                    onChange={e => setSelectedGradePurge(e.target.value)}
                    className="w-full p-2 border rounded bg-white text-slate-900 font-bold"
                  >
                    <option value="الصف الأول">الصف الأول</option>
                    <option value="الصف الثاني">الصف الثاني</option>
                    <option value="الصف الثالث">الصف الثالث</option>
                    <option value="الصف الرابع">الصف الرابع</option>
                    <option value="الصف الخامس">الصف الخامس</option>
                    <option value="الصف السادس">الصف السادس</option>
                  </select>
                </div>

                <div>
                  <label className="block mb-1 text-slate-500">فلتر الحذف المطلوب:</label>
                  <select
                    value={selectedFilterType}
                    onChange={e => setSelectedFilterType(e.target.value as any)}
                    className="w-full p-2 border rounded bg-white text-slate-900 font-bold"
                  >
                    <option value="all">كل طلاب هذا الصف</option>
                    <option value="passed">{'الطلاب الناجحين فقط (معدل >= 50)'}</option>
                    <option value="failed">{'الطلاب الراسبين فقط (معدل < 50)'}</option>
                  </select>
                </div>
              </div>

              <button
                type="button"
                onClick={handleSelectivePurge}
                className="w-full py-2.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg font-black transition-all cursor-pointer text-center border-none shadow-sm"
              >
                تنفيذ المسح الانتقائي للطلاب 🗑️
              </button>
            </div>

            {/* Bulk Purging */}
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-slate-200 space-y-3 flex flex-col justify-between">
              <span className="block text-slate-700 dark:text-slate-300 border-b pb-1">حذف وتصفير كلي للنظام المحلي</span>
              
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={handlePurgeAllStudents}
                  className="py-2 bg-slate-900 hover:bg-slate-800 text-white border border-slate-700 rounded-xl font-bold cursor-pointer text-center"
                >
                  حذف كافة الطلاب
                </button>
                <button
                  type="button"
                  onClick={handlePurgeAllStaff}
                  className="py-2 bg-slate-900 hover:bg-slate-800 text-white border border-slate-700 rounded-xl font-bold cursor-pointer text-center"
                >
                  حذف كادر التدريس
                </button>
                <button
                  type="button"
                  onClick={handlePurgeAllAbsences}
                  className="py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl font-bold cursor-pointer text-center col-span-2"
                >
                  تصفير غيابات جميع الطلاب
                </button>
              </div>

              <button
                type="button"
                onClick={handlePurgeToRawSystem}
                className="w-full py-2 bg-rose-700 hover:bg-rose-800 text-white rounded-xl font-black cursor-pointer text-center border-2 border-rose-500"
              >
                إعادة ضبط المصنع بالكامل (تصفير خام للأبد) 🚨
              </button>
            </div>

          </div>
        </div>

        {/* Official Support & Channels Card */}
        <div className="bg-gradient-to-r from-emerald-950/40 via-slate-900 to-indigo-950/40 p-6 rounded-2xl border border-emerald-500/30 space-y-4">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <div>
              <h3 className="text-base font-black text-white flex items-center gap-2">
                قنوات التواصل والدعم الفني الرسمي 💬
              </h3>
              <p className="text-xs text-slate-300 font-medium mt-0.5">
                تواصل مباشر لمتابعة التحديثات، الدعم الفني، وطلب تراخيص التفعيل لمرة واحدة
              </p>
            </div>
            <span className="px-3 py-1 bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 rounded-full text-xs font-bold">
              دعم معتمد ومستمر ✓
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
            <a
              href="https://whatsapp.com/channel/0029Vb9C7bs0QeaggKCbuI0J"
              target="_blank"
              rel="noopener noreferrer"
              className="p-3 bg-emerald-900/40 hover:bg-emerald-800/50 border border-emerald-500/40 rounded-xl text-xs font-bold text-emerald-200 flex items-center justify-center gap-2 transition-all"
            >
              <span>الانضمام لقناة المنظومة على WhatsApp 📢</span>
            </a>

            <a
              href="mailto:vcol42@gmail.com"
              className="p-3 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-xl text-xs font-bold text-slate-200 flex items-center justify-center gap-2 transition-all font-mono"
            >
              <span>البريد الإلكتروني المعتمد: vcol42@gmail.com ✉️</span>
            </a>
          </div>
        </div>

        {/* Form Action Controls */}
        <div className="flex items-center justify-between pt-4 border-t border-[var(--theme-card-border)]">
          <button
            type="button"
            onClick={onResetData}
            className="px-4 py-2.5 rounded-xl bg-rose-50 text-rose-700 text-xs font-bold hover:bg-rose-100 flex items-center gap-1.5"
          >
            <RotateCcw className="w-4 h-4" />
            <span>إعادة ضبط البيانات الأولية</span>
          </button>

          <button
            type="submit"
            className="px-8 py-3 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 text-sm font-black shadow-lg transition-all flex items-center gap-2 cursor-pointer"
          >
            <Save className="w-5 h-5" />
            <span>حفظ كل الإعدادات</span>
          </button>
        </div>

      </form>

      {/* Cloud Search & Restore Modal */}
      {showCloudSearchModal && (
        <div className="fixed inset-0 z-[120] bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto dir-rtl font-sans">
          <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] shadow-2xl max-w-2xl w-full overflow-hidden border border-slate-200 dark:border-slate-800 my-8">
            <div className="bg-slate-50 dark:bg-slate-800/80 px-6 py-5 flex items-center justify-between border-b border-slate-200 dark:border-slate-700">
              <div>
                <h3 className="text-lg font-black text-slate-900 dark:text-white">البحث عن مدرسة في السحابة واستعادتها</h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 font-bold mt-0.5">
                  ابحث باسم المدرسة، أو البريد الإلكتروني، أو رمز الاقتران (المدرس/الطالب/المدير)
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowCloudSearchModal(false)}
                className="p-2 rounded-xl bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 text-xs font-black cursor-pointer"
              >
                إغلاق ✕
              </button>
            </div>

            <div className="p-6 space-y-5">
              <form onSubmit={handleCloudSearch} className="flex gap-2">
                <input
                  type="text"
                  value={cloudSearchQuery}
                  onChange={e => setCloudSearchQuery(e.target.value)}
                  placeholder="مثال: كعب بن مالك أو 922769 أو البريد..."
                  className="flex-1 px-4 py-3 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:border-indigo-600 outline-none font-bold text-sm text-slate-900 dark:text-white"
                />
                <button
                  type="submit"
                  disabled={isCloudSearching || !cloudSearchQuery.trim()}
                  className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-black text-sm shadow transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
                >
                  {isCloudSearching ? <Loader2 className="w-5 h-5 animate-spin" /> : <Search className="w-5 h-5" />}
                  <span>بحث</span>
                </button>
              </form>

              <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
                {cloudSearchResults.length > 0 ? (
                  cloudSearchResults.map(school => (
                    <div 
                      key={school.id}
                      className="p-4 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 hover:border-indigo-400 rounded-2xl transition-all space-y-3"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <h4 className="font-black text-slate-900 dark:text-white text-base">{school.name}</h4>
                          <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold mt-0.5">
                            معرف المدرسة: <span className="font-mono text-indigo-600 font-bold">{school.id}</span>
                            {school.admin_email && <span> • {school.admin_email}</span>}
                          </div>
                        </div>

                        <button
                          type="button"
                          disabled={isRestoringSchool}
                          onClick={() => handleRestoreFromCloud(school)}
                          className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-black text-xs shadow transition-all flex items-center gap-1.5 cursor-pointer shrink-0"
                        >
                          {isRestoringSchool ? <Loader2 className="w-4 h-4 animate-spin" /> : <Cloud className="w-4 h-4" />}
                          <span>استعادة هذه المدرسة ⚡</span>
                        </button>
                      </div>

                      <div className="grid grid-cols-3 gap-2 pt-2 border-t border-slate-200 dark:border-slate-700 text-center text-xs">
                        <div className="bg-indigo-50 dark:bg-indigo-950/40 p-2 rounded-lg border border-indigo-100 dark:border-indigo-900/40">
                          <span className="block text-[10px] text-indigo-600 dark:text-indigo-400 font-bold">رمز المدرس</span>
                          <span className="font-mono font-black text-indigo-900 dark:text-indigo-200">{school.pairing_code}</span>
                        </div>
                        <div className="bg-emerald-50 dark:bg-emerald-950/40 p-2 rounded-lg border border-emerald-100 dark:border-emerald-900/40">
                          <span className="block text-[10px] text-emerald-600 dark:text-emerald-400 font-bold">رمز الطالب</span>
                          <span className="font-mono font-black text-emerald-900 dark:text-emerald-200">{school.student_pairing_code || '223344'}</span>
                        </div>
                        <div className="bg-amber-50 dark:bg-amber-950/40 p-2 rounded-lg border border-amber-100 dark:border-amber-900/40">
                          <span className="block text-[10px] text-amber-700 dark:text-amber-400 font-bold">رمز المدير</span>
                          <span className="font-mono font-black text-amber-900 dark:text-amber-200">{school.principal_pairing_code || '334455'}</span>
                        </div>
                      </div>
                    </div>
                  ))
                ) : hasCloudSearched && !isCloudSearching ? (
                  <div className="p-8 text-center bg-slate-50 dark:bg-slate-800 rounded-2xl border border-slate-200 dark:border-slate-700">
                    <p className="text-sm font-bold text-slate-500 dark:text-slate-400">لم يتم العثور على أي مدرسة مطابقة.</p>
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
