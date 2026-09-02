import React, { useState } from 'react';
import { 
  Building2, 
  Mail, 
  User, 
  ShieldCheck, 
  Sparkles, 
  Cloud, 
  ArrowLeft, 
  Landmark, 
  Compass, 
  Key,
  GraduationCap,
  Loader2
} from 'lucide-react';
import { supabase, isSupabaseConfigured, refreshSupabaseClient } from '../utils/supabaseClient';
import { restoreSchoolData } from '../utils/syncService';
import { defaultAppConfig } from '../data/initialData';

interface OnboardingModalProps {
  onComplete: (data: any) => void;
}

export const OnboardingModal: React.FC<OnboardingModalProps> = ({ onComplete }) => {
  const [onboardMode, setOnboardMode] = useState<'selection' | 'new' | 'existing'>('selection');
  const [schoolName, setSchoolName] = useState('');
  const [email, setEmail] = useState('');
  const [managerName, setManagerName] = useState('');
  const [directorateName, setDirectorateName] = useState('مديرية تربية ديالى');
  const [sectionName, setSectionName] = useState('قسم التربية والتعليم - بعقوبة المركز');
  const [schoolStage, setSchoolStage] = useState<'primary' | 'intermediate' | 'secondary'>('intermediate');
  const [passcode, setPasscode] = useState('1234');
  const [isLoading, setIsLoading] = useState(false);

  const handleExistingLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    
    if (isSupabaseConfigured()) {
      try {
        const { data: schools, error } = await supabase
          .from('schools')
          .select('*')
          .eq('admin_email', email.trim().toLowerCase());

        if (!error && schools && schools.length > 0) {
          const cloudSchool = schools[0];
          const restoreResult = await restoreSchoolData(cloudSchool.id);
          if (restoreResult.success) {
            localStorage.setItem('diyala_school_id', cloudSchool.id);
            localStorage.setItem('diyala_school_pairing_code', cloudSchool.pairing_code);
            localStorage.setItem('diyala_admin_email', email);
            localStorage.setItem('diyala_school_name', cloudSchool.name);
            refreshSupabaseClient();

            onComplete({
              ...defaultAppConfig,
              ...(restoreResult.config || {}),
              schoolName: cloudSchool.name,
              schoolId: cloudSchool.id,
              pairingCode: cloudSchool.pairing_code,
              adminEmail: email,
              restoredStudents: restoreResult.students,
              restoredTeachers: restoreResult.teachers
            });
            alert('✅ تم تسجيل الدخول واستعادة بيانات المدرسة بنجاح!');
            return;
          } else {
            alert('⚠️ فشل في استعادة البيانات: ' + restoreResult.message);
          }
        } else {
          alert('❌ عذراً، لا توجد مدرسة مسجلة بهذا البريد الإلكتروني.');
        }
      } catch (err) {
        console.error(err);
        alert('خطأ في الاتصال بالسحابة');
      }
    }
    setIsLoading(false);
  };

  const handleCreateNew = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    const generatedId = "SCH-" + email.trim().split('@')[0].toUpperCase().slice(0, 4) + "-" + Math.floor(1000 + Math.random() * 9000);
    const generatedPairingCode = Math.floor(100000 + Math.random() * 900000).toString();

    setTimeout(() => {
      localStorage.setItem('diyala_school_id', generatedId);
      localStorage.setItem('diyala_school_pairing_code', generatedPairingCode);
      localStorage.setItem('diyala_admin_email', email);
      localStorage.setItem('diyala_school_name', schoolName);
      refreshSupabaseClient();

      onComplete({ 
        ...defaultAppConfig,
        schoolName, managerName, directorateName, sectionName, schoolStage, passcode,
        schoolId: generatedId, pairingCode: generatedPairingCode, adminEmail: email
      });
      setIsLoading(false);
      alert('🎉 مبروك! تم إنشاء مدرستك السحابية الجديدة وتوليد كود الربط.');
    }, 1500);
  };

  if (onboardMode === 'selection') {
    return (
      <div className="fixed inset-0 z-[100] bg-slate-100 flex items-center justify-center p-4 dir-rtl text-right">
        <div className="bg-white rounded-[3rem] shadow-2xl max-w-2xl w-full p-10 space-y-8 border border-slate-200">
          <div className="text-center space-y-3">
            <div className="w-20 h-20 bg-indigo-600 rounded-3xl mx-auto flex items-center justify-center shadow-xl rotate-3">
               <Building2 className="w-10 h-10 text-white" />
            </div>
            <h2 className="text-3xl font-black text-slate-900">مرحباً بك في منصة The Principal</h2>
            <p className="text-slate-500 font-bold text-sm">يرجى اختيار نوع البدء للمتابعة:</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <button
              onClick={() => setOnboardMode('new')}
              className="p-8 bg-white border-4 border-indigo-600 rounded-[2.5rem] hover:bg-indigo-50 transition-all group flex flex-col items-center text-center gap-4 cursor-pointer"
            >
              <div className="p-4 bg-indigo-100 rounded-2xl group-hover:scale-110 transition-transform">
                <Sparkles className="w-8 h-8 text-indigo-600" />
              </div>
              <div>
                <span className="block font-black text-xl text-slate-900">إنشاء مدرسة جديدة</span>
                <span className="text-xs text-slate-500 font-bold mt-1">للمدراء الذين يستخدمون النظام لأول مرة</span>
              </div>
            </button>

            <button
              onClick={() => setOnboardMode('existing')}
              className="p-8 bg-white border-4 border-emerald-600 rounded-[2.5rem] hover:bg-emerald-50 transition-all group flex flex-col items-center text-center gap-4 cursor-pointer"
            >
              <div className="p-4 bg-emerald-100 rounded-2xl group-hover:scale-110 transition-transform">
                <Cloud className="w-8 h-8 text-emerald-600" />
              </div>
              <div>
                <span className="block font-black text-xl text-slate-900">استعادة مدرسة سابقة</span>
                <span className="text-xs text-slate-500 font-bold mt-1">تسجيل دخول واستيراد البيانات من السحابة</span>
              </div>
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-[100] bg-slate-100 flex items-center justify-center p-4 overflow-y-auto dir-rtl text-right font-tajawal">
      <div className="bg-white rounded-[2.5rem] shadow-2xl max-w-2xl w-full overflow-hidden border border-slate-300 my-8">

        <div className="bg-slate-50 p-6 text-slate-900 flex items-center justify-between border-b border-slate-200">
           <button onClick={() => setOnboardMode('selection')} className="p-2 rounded-xl bg-white border border-slate-200 text-slate-500 hover:text-indigo-600 cursor-pointer transition-colors">
              <ArrowLeft className="w-5 h-5 rotate-180" />
           </button>
           <div className="text-center flex-1">
              <h2 className="text-xl font-black">{onboardMode === 'new' ? 'تأسيس مدرسة جديدة' : 'استعادة مدرسة من السحاب'}</h2>
              <p className="text-[10px] text-slate-500 font-bold">يرجى إكمال البيانات المطلوبة بدقة</p>
           </div>
           <div className="w-9" />
        </div>

        {onboardMode === 'new' ? (
          <form onSubmit={handleCreateNew} className="p-8 space-y-5">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-bold">
               <div className="space-y-1">
                  <label className="mr-1 text-slate-500">اسم المدرسة:</label>
                  <input required type="text" value={schoolName} onChange={e => setSchoolName(e.target.value)} className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:border-indigo-600 outline-none" placeholder="مثال: ثانوية النيل" />
               </div>
               <div className="space-y-1">
                  <label className="mr-1 text-slate-500">اسم المدير:</label>
                  <input required type="text" value={managerName} onChange={e => setManagerName(e.target.value)} className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:border-indigo-600 outline-none" placeholder="الاسم الثلاثي واللقب" />
               </div>
               <div className="space-y-1 md:col-span-2">
                  <label className="mr-1 text-slate-500">البريد الإلكتروني الرسمي:</label>
                  <input required type="email" value={email} onChange={e => setEmail(e.target.value)} className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:border-indigo-600 outline-none font-mono" placeholder="principal@example.com" />
               </div>
               <div className="space-y-1">
                  <label className="mr-1 text-slate-500">رمز دخول الإدارة (4 أرقام):</label>
                  <input required type="text" maxLength={4} value={passcode} onChange={e => setPasscode(e.target.value.replace(/\D/g,''))} className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:border-indigo-600 outline-none text-center font-mono tracking-widest text-lg" placeholder="1234" />
               </div>
               <div className="space-y-1">
                  <label className="mr-1 text-slate-500">المرحلة الدراسية:</label>
                  <select value={schoolStage} onChange={e => setSchoolStage(e.target.value as any)} className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl outline-none font-bold">
                    <option value="primary">ابتدائية</option>
                    <option value="intermediate">متوسطة</option>
                    <option value="secondary">إعدادية / ثانوية</option>
                  </select>
               </div>
            </div>
            <button type="submit" disabled={isLoading} className="w-full py-4 bg-indigo-600 text-white rounded-2xl font-black text-lg shadow-xl hover:bg-indigo-700 transition-all cursor-pointer">
              {isLoading ? <Loader2 className="w-6 h-6 animate-spin mx-auto" /> : 'تفعيل المدرسة والبدء بالعمل 🚀'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleExistingLogin} className="p-8 space-y-6">
            <div className="p-6 bg-emerald-50 rounded-3xl border border-emerald-100 flex items-center gap-4">
               <div className="p-3 bg-white rounded-2xl shadow-sm"><Mail className="w-6 h-6 text-emerald-600" /></div>
               <div>
                  <h4 className="font-black text-emerald-900 text-sm">تسجيل الدخول للمدرسة</h4>
                  <p className="text-[11px] text-emerald-700 font-bold">أدخل البريد المستخدم عند التسجيل لأول مرة لاستعادة بياناتك</p>
               </div>
            </div>
            <div className="space-y-2 font-bold text-xs">
               <label className="mr-1 text-slate-500">البريد الإلكتروني للإدارة:</label>
               <input required type="email" value={email} onChange={e => setEmail(e.target.value)} className="w-full px-4 py-4 bg-slate-50 border border-slate-200 rounded-2xl focus:border-emerald-600 outline-none font-mono text-lg" placeholder="email@example.com" />
            </div>
            <button type="submit" disabled={isLoading} className="w-full py-4 bg-emerald-600 text-white rounded-2xl font-black text-lg shadow-xl hover:bg-emerald-700 transition-all cursor-pointer">
               {isLoading ? <Loader2 className="w-6 h-6 animate-spin mx-auto" /> : 'مزامنة واستعادة البيانات الآن ☁️'}
            </button>
            <p className="text-center text-[10px] text-slate-400 font-bold">سيتم سحب قائمة الطلاب والأساتذة والدرجات من السحابة اللحظية فوراً</p>
          </form>
        )}

      </div>
    </div>
  );
};
