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
  School
} from 'lucide-react';
import { refreshSupabaseClient, getSupabase } from '../utils/supabaseClient';
import { 
  restoreSchoolData, 
  searchSchoolsInSupabase, 
  checkDuplicateSchool,
  CloudSchoolSummary 
} from '../utils/syncService';
import { defaultAppConfig } from '../data/initialData';

interface OnboardingModalProps {
  onComplete: (data: any) => void;
}

export const OnboardingModal: React.FC<OnboardingModalProps> = ({ onComplete }) => {
  const [onboardMode, setOnboardMode] = useState<'selection' | 'new' | 'existing'>('selection');
  
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
    setIsLoading(true);
    try {
      const restoreResult = await restoreSchoolData(school.id);
      if (restoreResult.success) {
        const studentCode = school.student_pairing_code || restoreResult.config?.studentPairingCode || '223344';
        const principalCode = school.principal_pairing_code || restoreResult.config?.principalPairingCode || '334455';

        localStorage.setItem('diyala_school_id', school.id);
        localStorage.setItem('diyala_school_pairing_code', school.pairing_code);
        localStorage.setItem('diyala_pairing_code', school.pairing_code);
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
          pairingCode: school.pairing_code,
          studentPairingCode: studentCode,
          principalPairingCode: principalCode,
          adminEmail: school.admin_email,
          restoredStudents: restoreResult.students,
          restoredTeachers: restoreResult.teachers,
          restoredSchedule: restoreResult.schedule
        });

        alert(`✅ تم استعادة بيانات (${school.name}) بنجاح!\nرمز المدرس: ${school.pairing_code} | رمز الطالب: ${studentCode} | رمز المدير: ${principalCode}`);
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

  // Handle Create New School with strict deduplication check
  const handleCreateNew = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      // 1. Check if a school already exists with this Name or Email
      const dup = await checkDuplicateSchool(schoolName, email);
      if (dup) {
        setDuplicateFound(dup);
        setIsLoading(false);
        return;
      }

      // 2. Generate clean unique IDs and 3-role pairing codes
      const emailPrefix = email.trim().split('@')[0].toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 4) || 'SCH';
      const generatedId = `SCH-${emailPrefix}-${Math.floor(1000 + Math.random() * 9000)}`;
      const generatedTeacherCode = Math.floor(100000 + Math.random() * 900000).toString();
      const generatedStudentCode = Math.floor(100000 + Math.random() * 900000).toString();
      const generatedPrincipalCode = Math.floor(100000 + Math.random() * 900000).toString();

      // 3. Register School directly in Supabase
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

      // 4. Save to localStorage
      localStorage.setItem('diyala_school_id', generatedId);
      localStorage.setItem('diyala_school_pairing_code', generatedTeacherCode);
      localStorage.setItem('diyala_pairing_code', generatedTeacherCode);
      localStorage.setItem('diyala_student_pairing_code', generatedStudentCode);
      localStorage.setItem('diyala_principal_pairing_code', generatedPrincipalCode);
      localStorage.setItem('diyala_admin_email', email.trim().toLowerCase());
      localStorage.setItem('diyala_school_name', schoolName.trim());

      refreshSupabaseClient(generatedId);

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

      alert(`🎉 مبروك! تم تأسيس المدرسة بنجاح بدون أي تكرار.\nرمز المدرس: ${generatedTeacherCode}\nرمز الطالب: ${generatedStudentCode}\nرمز المدير: ${generatedPrincipalCode}`);
    } catch (err: any) {
      console.error(err);
      alert('خطأ أثناء إنشاء المدرسة: ' + (err.message || ''));
    } finally {
      setIsLoading(false);
    }
  };

  if (onboardMode === 'selection') {
    return (
      <div className="fixed inset-0 z-[100] bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 dir-rtl text-right font-sans">
        <div className="bg-white rounded-[2.5rem] shadow-2xl max-w-2xl w-full p-8 md:p-10 space-y-8 border border-slate-200">
          <div className="text-center space-y-3">
            <div className="w-20 h-20 bg-indigo-600 rounded-3xl mx-auto flex items-center justify-center shadow-xl shadow-indigo-200">
               <Building2 className="w-10 h-10 text-white" />
            </div>
            <h2 className="text-2xl md:text-3xl font-black text-slate-900">مرحباً بك في منصة The Principal</h2>
            <p className="text-slate-500 font-bold text-sm">اختر طريقة البدء المناسبة لبيئة عملك:</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <button
              onClick={() => { setDuplicateFound(null); setOnboardMode('new'); }}
              className="p-6 md:p-8 bg-slate-50 border-2 border-indigo-500 hover:border-indigo-600 rounded-[2rem] hover:bg-indigo-50/50 transition-all group flex flex-col items-center text-center gap-3 cursor-pointer shadow-sm hover:shadow-md"
            >
              <div className="p-4 bg-indigo-100 rounded-2xl group-hover:scale-110 transition-transform">
                <Sparkles className="w-8 h-8 text-indigo-600" />
              </div>
              <div>
                <span className="block font-black text-lg text-slate-900">تأسيس مدرسة جديدة</span>
                <span className="text-xs text-slate-500 font-semibold mt-1">تسجيل مدرسة لأول مرة بنظام حماية ضد التكرار</span>
              </div>
            </button>

            <button
              onClick={() => { setDuplicateFound(null); setOnboardMode('existing'); }}
              className="p-6 md:p-8 bg-slate-50 border-2 border-emerald-500 hover:border-emerald-600 rounded-[2rem] hover:bg-emerald-50/50 transition-all group flex flex-col items-center text-center gap-3 cursor-pointer shadow-sm hover:shadow-md"
            >
              <div className="p-4 bg-emerald-100 rounded-2xl group-hover:scale-110 transition-transform">
                <Cloud className="w-8 h-8 text-emerald-600" />
              </div>
              <div>
                <span className="block font-black text-lg text-slate-900">استعادة مدرسة من السحابة</span>
                <span className="text-xs text-slate-500 font-semibold mt-1">البحث بالاسم أو الإيميل أو رمز الاقتران</span>
              </div>
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-[100] bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto dir-rtl text-right font-sans">
      <div className="bg-white rounded-[2.5rem] shadow-2xl max-w-2xl w-full overflow-hidden border border-slate-200 my-8">

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

            <button 
              type="submit" 
              disabled={isLoading} 
              className="w-full mt-4 py-4 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl font-black text-base shadow-xl transition-all cursor-pointer flex items-center justify-center gap-2"
            >
              {isLoading ? <Loader2 className="w-6 h-6 animate-spin" /> : <Sparkles className="w-5 h-5" />}
              <span>التحقق والتأسيس السحابي 🚀</span>
            </button>
            <p className="text-center text-[11px] text-slate-400 font-bold">
              سيتم التحقق التلقائي لمنع أي تكرار وتوليد أكواد الاقتران الثلاثية المعتمدة فوراً
            </p>
          </form>
        ) : onboardMode === 'existing' ? (
          <div className="p-6 md:p-8 space-y-6">
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

                    <div className="grid grid-cols-3 gap-2 pt-2 border-t border-slate-200 text-center text-xs">
                      <div className="bg-indigo-50/80 p-2 rounded-lg border border-indigo-100">
                        <span className="block text-[10px] text-indigo-600 font-bold">رمز المدرس</span>
                        <span className="font-mono font-black text-indigo-900">{school.pairing_code}</span>
                      </div>
                      <div className="bg-emerald-50/80 p-2 rounded-lg border border-emerald-100">
                        <span className="block text-[10px] text-emerald-600 font-bold">رمز الطالب</span>
                        <span className="font-mono font-black text-emerald-900">{school.student_pairing_code || '223344'}</span>
                      </div>
                      <div className="bg-amber-50/80 p-2 rounded-lg border border-amber-100">
                        <span className="block text-[10px] text-amber-700 font-bold">رمز المدير</span>
                        <span className="font-mono font-black text-amber-900">{school.principal_pairing_code || '334455'}</span>
                      </div>
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
  );
};
