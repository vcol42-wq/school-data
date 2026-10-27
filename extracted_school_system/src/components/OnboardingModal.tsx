import React, { useState } from 'react';
import { 
  Building2, 
  Mail, 
  User, 
  Sparkles, 
  Cloud, 
  ArrowLeft, 
  Search,
  Key,
  ShieldAlert,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  RefreshCw,
  School,
  Copy,
  Check,
  MessageCircle,
  ShieldCheck,
  ExternalLink
} from 'lucide-react';
import { refreshSupabaseClient, getSupabase } from '../utils/supabaseClient';
import { 
  restoreSchoolData, 
  searchSchoolsInSupabase, 
  checkDuplicateSchool,
  CloudSchoolSummary 
} from '../utils/syncService';
import { 
  getMachineFingerprint, 
  activateDesktopLicense, 
  isDesktopActivated 
} from '../utils/licenseService';
import { defaultAppConfig } from '../data/initialData';

interface OnboardingModalProps {
  onComplete: (data: any) => void;
}

export const OnboardingModal: React.FC<OnboardingModalProps> = ({ onComplete }) => {
  const [onboardMode, setOnboardMode] = useState<'selection' | 'new' | 'existing'>('selection');
  
  // License Activation State
  const [licenseKeyInput, setLicenseKeyInput] = useState('');
  const [isMachineActivated, setIsMachineActivated] = useState(() => isDesktopActivated());
  const [copiedFp, setCopiedFp] = useState(false);
  const fingerprint = getMachineFingerprint();

  // New School Fields
  const [schoolName, setSchoolName] = useState('');
  const [email, setEmail] = useState('');
  const [managerName, setManagerName] = useState('');
  const [directorateName, setDirectorateName] = useState('مديرية تربية ديالى');
  const [sectionName, setSectionName] = useState('قسم التربية والتعليم - بعقوبة المركز');
  const [schoolStage, setSchoolStage] = useState<'primary' | 'intermediate' | 'secondary'>('intermediate');
  const [passcode, setPasscode] = useState('1234');
  
  // Existing School Search
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<CloudSchoolSummary[]>([]);
  const [hasSearched, setHasSearched] = useState(false);
  
  // States
  const [isLoading, setIsLoading] = useState(false);
  const [duplicateFound, setDuplicateFound] = useState<CloudSchoolSummary | null>(null);

  const copyFingerprint = () => {
    navigator.clipboard.writeText(fingerprint);
    setCopiedFp(true);
    setTimeout(() => setCopiedFp(false), 2500);
  };

  const openWhatsAppForLicense = () => {
    const school = schoolName.trim() || 'مدرستنا الكريمة';
    const text = encodeURIComponent(
      `السلام عليكم، أرغب في تفعيل ترخيص منظومة The Principal المكتبية.\nمعرّف الحاسوب الخاص بنا (Hardware ID): ${fingerprint}\nاسم المدرسة: ${school}`
    );
    window.open(`https://api.whatsapp.com/send?text=${text}`, '_blank');
  };

  // Search Schools in Cloud by Name, Email, or Pairing Code
  const handleSearch = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!searchQuery.trim()) return;

    setIsLoading(true);
    setHasSearched(true);
    try {
      const results = await searchSchoolsInSupabase(searchQuery);
      setSearchResults(results);
    } catch (err) {
      console.error(err);
      alert('حدث خطأ أثناء البحث في السحابة.');
    } finally {
      setIsLoading(false);
    }
  };

  // Restore Existing School
  const handleRestoreSchool = async (school: CloudSchoolSummary) => {
    // 0. Enforce STRICT activation check before restoration - ALWAYS require valid key for this school
    let keyToVerify = licenseKeyInput.trim();
    if (!keyToVerify) {
      const inputKey = prompt(
        `🔒 يلزم إدخال كود التفعيل المعتمد للمدرسة (${school.name}) للمتابعة واستعادة البيانات:\nمعرّف هذا الحاسوب (Hardware ID): ${fingerprint}\n\nأدخل كود تفعيل المنظومة (License Key):`
      );
      if (!inputKey || !inputKey.trim()) {
        alert('تم إلغاء عملية الاستعادة: لا يمكن استعادة بيانات المدرسة بدون كود التفعيل والترخيص المعتمد.');
        return;
      }
      keyToVerify = inputKey.trim().toUpperCase();
    }

    setIsLoading(true);
    const licRes = await activateDesktopLicense(keyToVerify, school.name);
    if (!licRes.success) {
      setIsLoading(false);
      alert(`❌ فشل التحقق من الترخيص: ${licRes.message}\nلا يمكنك استرجاع بيانات هذه المدرسة إلا باستخدام كود التفعيل المعتمد والمخصص لها.`);
      return;
    }
    setIsMachineActivated(true);

    try {
      const restoreResult = await restoreSchoolData(school.id);
      if (restoreResult.success) {
        const teacherCode = restoreResult.config?.pairingCode || restoreResult.schoolMeta?.pairing_code || '112233';
        const studentCode = restoreResult.config?.studentPairingCode || '223344';
        const principalCode = restoreResult.config?.principalPairingCode || '334455';

        localStorage.setItem('diyala_school_id', school.id);
        localStorage.setItem('diyala_school_pairing_code', teacherCode);
        localStorage.setItem('diyala_pairing_code', teacherCode);
        localStorage.setItem('diyala_student_pairing_code', studentCode);
        localStorage.setItem('diyala_principal_pairing_code', principalCode);
        localStorage.setItem('diyala_admin_email', school.admin_email);
        localStorage.setItem('diyala_school_name', school.name);

        refreshSupabaseClient(school.id);

        onComplete({
          ...defaultAppConfig,
          ...(restoreResult.config || {}),
          schoolName: school.name,
          schoolId: school.id,
          pairingCode: teacherCode,
          studentPairingCode: studentCode,
          principalPairingCode: principalCode,
          adminEmail: school.admin_email,
          restoredStudents: restoreResult.students,
          restoredTeachers: restoreResult.teachers,
          restoredSchedule: restoreResult.schedule
        });

        alert(`✅ تم استعادة وتثبيت مدرسة (${school.name}) بنجاح.`);
      } else {
        alert('⚠️ فشل في استعادة البيانات: ' + restoreResult.message);
      }
    } catch (err: any) {
      console.error(err);
      alert('خطأ في استعادة بيانات المدرسة: ' + (err.message || ''));
    } finally {
      setIsLoading(false);
    }
  };

  // Handle Create New School (Supports full local operation + optional cloud registration if licensed)
  const handleCreateNew = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      // 0. Check if user provided a license key or is already activated
      let isLicenseValid = isMachineActivated;
      if (!isLicenseValid && licenseKeyInput.trim()) {
        const licRes = await activateDesktopLicense(licenseKeyInput.trim().toUpperCase(), schoolName.trim());
        if (licRes.success) {
          isLicenseValid = true;
          setIsMachineActivated(true);
        } else {
          alert(`❌ تنبيه: كود الترخيص المدخل غير صحيح: ${licRes.message}\nسيتم إكمال التأسيس في وضع العمل المحلي.`);
        }
      }

      // 1. Check if a school already exists with this Name or Email (only if attempting cloud registration)
      if (isLicenseValid) {
        const dup = await checkDuplicateSchool(schoolName, email);
        if (dup) {
          setDuplicateFound(dup);
          setIsLoading(false);
          return;
        }
      }

      // 2. Generate clean IDs and pairing codes
      const emailPrefix = email.trim().split('@')[0].toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 4) || 'SCH';
      const randNum = Math.floor(1000 + Math.random() * 9000);
      const generatedId = isLicenseValid ? `SCH-${emailPrefix}-${randNum}` : `SCH-LOCAL-${randNum}`;
      
      let generatedPrincipalCode = '';
      let generatedTeacherCode = '';
      let generatedStudentCode = '';

      if (isLicenseValid) {
        // ONLY generate and register cloud codes if licensed!
        const randSuffix = Math.random().toString(36).substring(2, 4).toUpperCase();
        generatedPrincipalCode = `ADM-${Math.floor(1000 + Math.random() * 9000)}-${randSuffix}`;
        generatedTeacherCode = `TCH-${Math.floor(1000 + Math.random() * 9000)}`;
        generatedStudentCode = Math.floor(1000 + Math.random() * 9000).toString();

        // Register School directly in Supabase Cloud
        const client = getSupabase(generatedId);
        const { error: regError } = await client.from('schools').insert([{
          id: generatedId,
          name: schoolName.trim(),
          pairing_code: generatedTeacherCode,
          admin_email: email.trim().toLowerCase(),
          config: {
            managerName,
            directorateName,
            sectionName,
            schoolStage,
            passcode,
            schoolStartHour: '08:00',
            lessonDurationMinutes: 45,
            breakDurationMinutes: 10,
            studentPairingCode: generatedStudentCode,
            principalPairingCode: generatedPrincipalCode
          }
        }]);

        if (regError) {
          console.warn('School register warning:', regError.message);
        }
      }

      // 3. Save to localStorage
      localStorage.setItem('diyala_school_id', generatedId);
      localStorage.setItem('diyala_school_pairing_code', generatedTeacherCode);
      localStorage.setItem('diyala_pairing_code', generatedTeacherCode);
      localStorage.setItem('diyala_student_pairing_code', generatedStudentCode);
      localStorage.setItem('diyala_principal_pairing_code', generatedPrincipalCode);
      localStorage.setItem('diyala_admin_email', email.trim().toLowerCase());
      localStorage.setItem('diyala_school_name', schoolName.trim());

      if (isLicenseValid) {
        refreshSupabaseClient(generatedId);
      }

      onComplete({
        ...defaultAppConfig,
        schoolName: schoolName.trim(),
        managerName,
        directorateName,
        sectionName,
        schoolStage,
        passcode,
        schoolId: generatedId,
        pairingCode: generatedTeacherCode,
        studentPairingCode: generatedStudentCode,
        principalPairingCode: generatedPrincipalCode,
        adminEmail: email.trim().toLowerCase()
      });

      if (isLicenseValid) {
        alert(`🎉 مبروك! تم تأسيس المدرسة بنجاح وتفعيل الربط السحابي.\nرمز المدرس: ${generatedTeacherCode}\nرمز الطالب: ${generatedStudentCode}\nرمز المدير: ${generatedPrincipalCode}`);
      } else {
        alert(`🎉 مرحباً بك! تم تأسيس المدرسة بنجاح.\nالنظام يعمل الآن محلياً بكامل طاقته ومميزاته (إدارة الطلاب، الكادر، الجدول المدرسي، سجلات الدرجات، الطباعة).\n\n💡 ملاحظة: الربط والمزامنة السحابية مع هواتف الأساتذة والطلبة تتطلب تفعيل ترخيص المنظومة بأي وقت.`);
      }
    } catch (err: any) {
      console.error(err);
      alert('خطأ أثناء إنشاء المدرسة: ' + (err.message || ''));
    } finally {
      setIsLoading(false);
    }
  };

  if (onboardMode === 'selection') {
    return (
      <div className="fixed inset-0 z-[100] bg-slate-900/60 backdrop-blur-sm overflow-y-auto p-4 dir-rtl text-right font-sans">
        <div className="min-h-full flex items-center justify-center py-6">
          <div className="bg-white rounded-[2.5rem] shadow-2xl max-w-2xl w-full p-6 sm:p-8 md:p-10 space-y-6 sm:space-y-8 border border-slate-200 my-auto">
            <div className="text-center space-y-3">
              <div className="w-16 h-16 sm:w-20 sm:h-20 bg-indigo-600 rounded-3xl mx-auto flex items-center justify-center shadow-xl shadow-indigo-200">
                 <Building2 className="w-8 h-8 sm:w-10 sm:h-10 text-white" />
              </div>
              <h2 className="text-xl sm:text-2xl md:text-3xl font-black text-slate-900">مرحباً بك في منصة The Principal</h2>
              <p className="text-slate-500 font-bold text-xs sm:text-sm">اختر طريقة البدء المناسبة لبيئة عملك:</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-5">
              <button
                onClick={() => { setDuplicateFound(null); setOnboardMode('new'); }}
                className="p-5 sm:p-6 md:p-8 bg-slate-50 border-2 border-indigo-500 hover:border-indigo-600 rounded-[2rem] hover:bg-indigo-50/50 transition-all group flex flex-col items-center text-center gap-3 cursor-pointer shadow-sm hover:shadow-md"
              >
                <div className="p-3 sm:p-4 bg-indigo-100 rounded-2xl group-hover:scale-110 transition-transform">
                  <Sparkles className="w-7 h-7 sm:w-8 sm:h-8 text-indigo-600" />
                </div>
                <div>
                  <span className="block font-black text-base sm:text-lg text-slate-900">تأسيس مدرسة جديدة</span>
                  <span className="text-xs text-slate-500 font-semibold mt-1">تسجيل مدرسة لأول مرة بنظام حماية ضد التكرار</span>
                </div>
              </button>

              <button
                onClick={() => { setDuplicateFound(null); setOnboardMode('existing'); }}
                className="p-5 sm:p-6 md:p-8 bg-slate-50 border-2 border-emerald-500 hover:border-emerald-600 rounded-[2rem] hover:bg-emerald-50/50 transition-all group flex flex-col items-center text-center gap-3 cursor-pointer shadow-sm hover:shadow-md"
              >
                <div className="p-3 sm:p-4 bg-emerald-100 rounded-2xl group-hover:scale-110 transition-transform">
                  <Cloud className="w-7 h-7 sm:w-8 sm:h-8 text-emerald-600" />
                </div>
                <div>
                  <span className="block font-black text-base sm:text-lg text-slate-900">استعادة مدرسة من السحابة</span>
                  <span className="text-xs text-slate-500 font-semibold mt-1">البحث بالاسم أو الإيميل أو رمز الاقتران</span>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-[100] bg-slate-900/60 backdrop-blur-sm overflow-y-auto p-4 dir-rtl text-right font-sans">
      <div className="min-h-full flex items-center justify-center py-6">
        <div className="bg-white rounded-[2.5rem] shadow-2xl max-w-2xl w-full overflow-hidden border border-slate-200 my-auto">

        {/* Modal Header */}
        <div className="bg-slate-50 px-6 py-5 text-slate-900 flex items-center justify-between border-b border-slate-200">
           <button 
             onClick={() => { setDuplicateFound(null); setOnboardMode('selection'); }} 
             className="p-2 rounded-xl bg-white border border-slate-200 text-slate-500 hover:text-indigo-600 cursor-pointer transition-colors shadow-sm"
           >
              <ArrowLeft className="w-5 h-5 rotate-180" />
           </button>
           <div className="text-center flex-1">
              <h2 className="text-lg md:text-xl font-black text-slate-800">
                {onboardMode === 'new' ? 'تأسيس مدرسة جديدة (نظام منع التكرار)' : 'استعادة مدرسة سابقة من السحابة'}
              </h2>
              <p className="text-xs text-slate-500 font-bold mt-0.5">
                {onboardMode === 'new' ? 'أدخل تفاصيل المدرسة للتحقق والتأسيس السحابي' : 'ابحث بالاسم أو البريد أو رمز الاقتران لاستعادة بياناتك'}
              </p>
           </div>
           <div className="w-9" />
        </div>

        {/* Duplicate Warning Dialog Overlay */}
        {duplicateFound && (
          <div className="p-6 bg-amber-50 border-b border-amber-200 space-y-4">
            <div className="flex items-start gap-3">
              <div className="p-2.5 bg-amber-200 rounded-xl text-amber-800 shrink-0">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <h4 className="font-black text-amber-900 text-base">⚠️ تنبيه: تم العثور على مدرسة مسجلة مسبقاً!</h4>
                <p className="text-xs text-amber-800 font-bold mt-1">
                  توجد مدرسة بنفس هذا الاسم أو البريد مسجلة في السحابة. لمنع التكرار والحفاظ على تكامل البيانات، ننصح باستعادة هذه المدرسة مباشرة:
                </p>
                <div className="mt-3 p-3 bg-white rounded-xl border border-amber-300 text-xs font-bold text-slate-800 space-y-1">
                  <div>🏫 <span className="text-slate-500">اسم المدرسة:</span> {duplicateFound.name}</div>
                  <div>🆔 <span className="text-slate-500">المعرف السحابي:</span> {duplicateFound.id}</div>
                  <div>📧 <span className="text-slate-500">البريد:</span> {duplicateFound.admin_email}</div>
                  <div>👨‍🏫 <span className="text-slate-500">رمز المدرس:</span> <span className="font-mono text-indigo-600">{duplicateFound.pairing_code}</span></div>
                  {duplicateFound.student_pairing_code && (
                    <div>🎓 <span className="text-slate-500">رمز الطالب:</span> <span className="font-mono text-emerald-600">{duplicateFound.student_pairing_code}</span></div>
                  )}
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                disabled={isLoading}
                onClick={() => handleRestoreSchool(duplicateFound)}
                className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-black text-sm shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer"
              >
                {isLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : <CheckCircle2 className="w-5 h-5" />}
                <span>نعم، استعادة هذه المدرسة والربط بها فوراً ✅</span>
              </button>
              <button
                type="button"
                onClick={() => setDuplicateFound(null)}
                className="px-4 py-3 bg-slate-200 hover:bg-slate-300 text-slate-700 rounded-xl font-bold text-xs cursor-pointer transition-colors"
              >
                تعديل البيانات
              </button>
            </div>
          </div>
        )}

        {/* Form Content */}
        {onboardMode === 'new' && !duplicateFound ? (
          <form onSubmit={handleCreateNew} className="p-6 md:p-8 space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-bold">
               <div className="space-y-1 md:col-span-2">
                  <label className="text-slate-600">اسم المدرسة الرسمي:</label>
                  <input 
                    required 
                    type="text" 
                    value={schoolName} 
                    onChange={e => setSchoolName(e.target.value)} 
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:border-indigo-600 outline-none text-slate-900 font-bold" 
                    placeholder="مثال: متوسطة كعب بن مالك المسائية للبنين" 
                  />
               </div>
               <div className="space-y-1">
                  <label className="text-slate-600">اسم المدير:</label>
                  <input 
                    required 
                    type="text" 
                    value={managerName} 
                    onChange={e => setManagerName(e.target.value)} 
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:border-indigo-600 outline-none text-slate-900" 
                    placeholder="الاسم الثلاثي واللقب" 
                  />
               </div>
               <div className="space-y-1">
                  <label className="text-slate-600">البريد الإلكتروني الرسمي:</label>
                  <input 
                    required 
                    type="email" 
                    value={email} 
                    onChange={e => setEmail(e.target.value)} 
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:border-indigo-600 outline-none font-mono text-slate-900" 
                    placeholder="principal@school.edu.iq" 
                  />
               </div>
               <div className="space-y-1">
                  <label className="text-slate-600">رمز دخول الإدارة (4 أرقام):</label>
                  <input 
                    required 
                    type="text" 
                    maxLength={4} 
                    value={passcode} 
                    onChange={e => setPasscode(e.target.value.replace(/\D/g,''))} 
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:border-indigo-600 outline-none text-center font-mono tracking-widest text-lg font-black" 
                    placeholder="1234" 
                  />
               </div>
               <div className="space-y-1">
                  <label className="text-slate-600">المرحلة الدراسية:</label>
                  <select 
                    value={schoolStage} 
                    onChange={e => setSchoolStage(e.target.value as any)} 
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl outline-none font-bold text-slate-900"
                  >
                    <option value="primary">ابتدائية</option>
                    <option value="intermediate">متوسطة</option>
                    <option value="secondary">إعدادية / ثانوية</option>
                  </select>
               </div>
            </div>

            {/* License Activation Section */}
            {isMachineActivated ? (
              <div className="p-3.5 bg-emerald-50 border-2 border-emerald-400 rounded-2xl flex items-center justify-between text-xs font-black text-emerald-900">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-emerald-600" />
                  <span>✓ هذا الحاسوب مفعل ومرخص رسمياً بمنظومة The Principal</span>
                </div>
                <span className="font-mono text-[11px] text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-lg">
                  {fingerprint}
                </span>
              </div>
            ) : (
              <div className="p-4 bg-gradient-to-br from-slate-900 via-slate-950 to-indigo-950 border-2 border-amber-400/80 rounded-2xl text-white space-y-3.5 shadow-lg">
                <div className="flex items-center justify-between border-b border-amber-400/30 pb-2.5">
                  <div className="flex items-center gap-2">
                    <div className="p-1.5 bg-amber-400/20 text-amber-300 rounded-lg">
                      <Key className="w-4 h-4" />
                    </div>
                    <div>
                      <h4 className="text-xs font-black text-amber-300">ترخيص الربط السحابي (اختياري للعمل المحلي)</h4>
                      <p className="text-[10px] text-slate-300">البرنامج يعمل محلياً بكامل طاقته - أدخل الكود فقط إذا كنت ترغب بالربط السحابي الآن</p>
                    </div>
                  </div>
                  <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 text-[10px] font-black">
                    اختياري الآن
                  </span>
                </div>

                {/* Hardware ID Display */}
                <div className="p-2.5 bg-slate-800/80 rounded-xl border border-slate-700 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 text-xs">
                  <div>
                    <span className="text-[10px] text-slate-400 block font-bold">معرّف هذا الحاسوب (Hardware ID):</span>
                    <span className="font-mono text-amber-300 font-black tracking-wider text-xs">{fingerprint}</span>
                  </div>
                  <div className="flex items-center gap-1.5 self-end sm:self-auto">
                    <button
                      type="button"
                      onClick={copyFingerprint}
                      className="px-2.5 py-1.5 bg-slate-700 hover:bg-slate-600 border border-slate-600 rounded-lg text-[10px] font-black text-slate-200 flex items-center gap-1 cursor-pointer transition-colors"
                      title="نسخ معرّف الحاسوب"
                    >
                      {copiedFp ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5 text-amber-300" />}
                      <span>{copiedFp ? 'تم النسخ' : 'نسخ المعرّف'}</span>
                    </button>
                    <button
                      type="button"
                      onClick={openWhatsAppForLicense}
                      className="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-[10px] font-black flex items-center gap-1 cursor-pointer transition-colors shadow-xs"
                      title="طلب كود التفعيل عبر الواتساب"
                    >
                      <MessageCircle className="w-3.5 h-3.5" />
                      <span>طلب كود التفعيل 💬</span>
                    </button>
                  </div>
                </div>

                {/* License Key Input */}
                <div className="space-y-1">
                  <label className="text-[11px] font-black text-amber-200 block">
                    كود تفعيل الربط السحابي (اتركه فارغاً للبدء محلياً فوراً):
                  </label>
                  <input
                    type="text"
                    value={licenseKeyInput}
                    onChange={e => setLicenseKeyInput(e.target.value.toUpperCase())}
                    placeholder="BOSS-L1-XXXX-XXXX-XX (اختياري للعمل المحلي)"
                    className="w-full px-3.5 py-2.5 bg-slate-950 border-2 border-amber-400/60 focus:border-amber-400 rounded-xl font-mono text-center text-sm font-black text-amber-300 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-amber-400"
                  />
                  <p className="text-[10px] text-slate-400 font-bold">
                    ملاحظة: يمكنك إكمال التأسيس بدون كود والعمل محلياً بكامل الميزات، وتفعيل الربط السحابي لاحقاً بأي وقت.
                  </p>
                </div>
              </div>
            )}

            <button 
              type="submit" 
              disabled={isLoading} 
              className="w-full mt-4 py-4 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl font-black text-base shadow-xl transition-all cursor-pointer flex items-center justify-center gap-2"
            >
              {isLoading ? <Loader2 className="w-6 h-6 animate-spin" /> : <Sparkles className="w-5 h-5" />}
              <span>تأسيس المدرسة والبدء بالعمل 🚀</span>
            </button>
            <p className="text-center text-[11px] text-slate-400 font-bold">
              يعمل البرنامج محلياً بكامل طاقته ومميزاته، مع إمكانية تفعيل الربط السحابي لاحقاً
            </p>
          </form>
        ) : onboardMode === 'existing' ? (
          <div className="p-6 md:p-8 space-y-6">
            {/* If machine not activated, display activation helper */}
            {!isMachineActivated && (
              <div className="p-3.5 bg-gradient-to-r from-slate-900 to-indigo-950 border-2 border-amber-400/80 rounded-2xl text-white space-y-2.5 text-xs shadow-md">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Key className="w-4 h-4 text-amber-300" />
                    <span className="font-black text-amber-300">ترخيص وتفعيل المنظومة (إلزامي للاستعادة):</span>
                  </div>
                  <span className="font-mono text-amber-200 text-[11px] bg-slate-800 px-2.5 py-0.5 rounded border border-slate-700">
                    {fingerprint}
                  </span>
                </div>
                <p className="text-[11px] text-slate-300 font-bold leading-relaxed">
                  أدخل كود تفعيل المنظومة هنا لتثبيته تلقائياً عند استعادة بيانات المدرسة:
                </p>
                <div className="flex flex-col sm:flex-row items-center gap-2">
                  <input
                    type="text"
                    value={licenseKeyInput}
                    onChange={e => setLicenseKeyInput(e.target.value.toUpperCase())}
                    placeholder="BOSS-L1-XXXX-XXXX-XX"
                    className="w-full sm:flex-1 px-3 py-2 bg-black/60 border border-amber-400 rounded-xl font-mono text-amber-300 text-xs placeholder:text-slate-500 focus:outline-none"
                  />
                  <div className="flex items-center gap-1.5 self-end sm:self-auto shrink-0">
                    <button
                      type="button"
                      onClick={copyFingerprint}
                      className="px-2.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-[10px] font-black flex items-center gap-1 cursor-pointer border border-slate-700"
                    >
                      {copiedFp ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5 text-amber-300" />}
                      <span>{copiedFp ? 'تم' : 'نسخ ID'}</span>
                    </button>
                    <button
                      type="button"
                      onClick={openWhatsAppForLicense}
                      className="px-2.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-[10px] font-black flex items-center gap-1 cursor-pointer shadow-xs"
                    >
                      <MessageCircle className="w-3.5 h-3.5" />
                      <span>طلب الكود 💬</span>
                    </button>
                  </div>
                </div>
              </div>
            )}

            <form onSubmit={handleSearch} className="space-y-3">
              <label className="text-xs font-black text-slate-700">
                ابحث باسم المدرسة، البريد الإلكتروني، أو رمز الاقتران (المدرس/الطالب/المدير):
              </label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={e => setSearchQuery(e.target.value)}
                    placeholder="مثال: كعب بن مالك أو 922769 أو البريد..."
                    className="w-full px-4 py-3.5 bg-slate-50 border border-slate-200 rounded-xl focus:border-emerald-600 outline-none font-bold text-sm text-slate-900"
                  />
                </div>
                <button
                  type="submit"
                  disabled={isLoading || !searchQuery.trim()}
                  className="px-6 py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-black text-sm shadow-md transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
                >
                  {isLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Search className="w-5 h-5" />}
                  <span>بحث</span>
                </button>
              </div>
            </form>

            {/* Results Section */}
            <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
              {searchResults.length > 0 ? (
                searchResults.map(school => (
                  <div 
                    key={school.id} 
                    className="p-4 bg-slate-50 hover:bg-emerald-50/50 border border-slate-200 hover:border-emerald-300 rounded-2xl transition-all space-y-3"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <h4 className="font-black text-slate-900 text-base">{school.name}</h4>
                        <div className="text-xs text-slate-500 font-semibold mt-0.5">
                          معرف المدرسة: <span className="font-mono text-slate-700 font-bold">{school.id}</span>
                          {school.admin_email && <span> • {school.admin_email}</span>}
                        </div>
                      </div>
                      <button
                        type="button"
                        disabled={isLoading}
                        onClick={() => handleRestoreSchool(school)}
                        className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-black text-xs shadow transition-all flex items-center gap-1.5 cursor-pointer shrink-0"
                      >
                        <Cloud className="w-4 h-4" />
                        <span>استعادة هذه المدرسة ⚡</span>
                      </button>
                    </div>

                    <div className="flex items-center justify-between pt-2 border-t border-slate-200 text-xs px-1 text-slate-500 font-bold">
                      <span className="flex items-center gap-1.5 text-amber-700 bg-amber-50 px-2.5 py-1 rounded-lg border border-amber-200">
                        <Lock className="w-3.5 h-3.5" />
                        <span>الرموز والبيانات مشفرة ومحمية بكود التفعيل 🔒</span>
                      </span>
                      <span className="text-[11px] text-slate-400">يلزم إدخال كود الترخيص للاستعادة</span>
                    </div>
                  </div>
                ))
              ) : hasSearched && !isLoading ? (
                <div className="p-8 text-center bg-slate-50 rounded-2xl border border-slate-200">
                  <p className="text-sm font-bold text-slate-500">لم يتم العثور على مدرسة مطابقة في السحابة.</p>
                  <p className="text-xs text-slate-400 mt-1">تأكد من كتابة الاسم أو البريد أو رمز الاقتران بشكل صحيح.</p>
                </div>
              ) : null}
            </div>

            <p className="text-center text-[10px] text-slate-400 font-bold">
              استعادة المدرسة تسحب تلقائياً الطلاب والأساتذة والجدول والدرجات من السحابة دون الحاجة لأي إدخال يدوي
            </p>
          </div>
        ) : null}

      </div>
      </div>
    </div>
  );
};
