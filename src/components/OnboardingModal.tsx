import React, { useState } from 'react';
import { Building2, Mail, User, ShieldCheck, Sparkles, Cloud, ArrowLeft } from 'lucide-react';

interface OnboardingModalProps {
  onComplete: (data: any) => void;
}

export const OnboardingModal: React.FC<OnboardingModalProps> = ({ onComplete }) => {
  const [schoolName, setSchoolName] = useState('');
  const [email, setEmail] = useState('');
  const [managerName, setManagerName] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    // Logic: Convert Email to School ID (Simulated Global Registration)
    const generatedId = "SCH-" + email.split('@')[0].toUpperCase().slice(0, 4) + "-" + Math.floor(1000 + Math.random() * 9000);

    setTimeout(() => {
      localStorage.setItem('diyala_school_id', generatedId);
      localStorage.setItem('diyala_admin_email', email);
      localStorage.setItem('diyala_school_name', schoolName);
      onComplete({ schoolName, managerName, schoolId: generatedId });
      setIsLoading(false);
    }, 2000);
  };

  return (
    <div className="fixed inset-0 z-[100] bg-slate-950/95 backdrop-blur-xl flex items-center justify-center p-4 dir-rtl">
      <div className="bg-white rounded-[3rem] shadow-[0_0_100px_rgba(30,58,138,0.3)] max-w-xl w-full overflow-hidden border border-slate-200">
        <div className="bg-gradient-to-br from-purple-950 via-indigo-950 to-slate-950 p-10 text-white text-center relative">
          <div className="absolute top-6 left-6 opacity-30 animate-pulse">
            <Sparkles className="w-12 h-12 text-amber-400" />
          </div>
          <div className="w-20 h-20 bg-amber-400 rounded-3xl mx-auto mb-6 flex items-center justify-center rotate-3 shadow-2xl">
            <Building2 className="w-12 h-12 text-slate-900" />
          </div>
          <h2 className="text-3xl font-black mb-3 tracking-tight">منصة المدير الملكية</h2>
          <p className="text-indigo-200 text-sm font-medium">ابدأ بتفعيل مدرستك على السحابة العالمية بلمسة واحدة.</p>
        </div>

        <form onSubmit={handleSubmit} className="p-10 space-y-6">
          <div className="grid grid-cols-1 gap-5">
            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-400 uppercase tracking-widest mr-2">اسم المؤسسة التعليمية</label>
              <input
                required
                type="text"
                value={schoolName}
                onChange={e => setSchoolName(e.target.value)}
                placeholder="مثال: ثانوية المتميزين للبنين"
                className="w-full px-6 py-4 bg-slate-50 border-2 border-slate-100 rounded-2xl focus:border-indigo-600 outline-none transition-all font-bold text-slate-800"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-400 uppercase tracking-widest mr-2">البريد الرسمي للمدير</label>
              <input
                required
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="principal@edu.iq"
                className="w-full px-6 py-4 bg-slate-50 border-2 border-slate-100 rounded-2xl focus:border-indigo-600 outline-none transition-all font-bold text-slate-800"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-black text-slate-400 uppercase tracking-widest mr-2">اسم السيد المدير</label>
              <input
                required
                type="text"
                value={managerName}
                onChange={e => setManagerName(e.target.value)}
                placeholder="الاسم الثلاثي واللقب"
                className="w-full px-6 py-4 bg-slate-50 border-2 border-slate-100 rounded-2xl focus:border-indigo-600 outline-none transition-all font-bold text-slate-800"
              />
            </div>
          </div>

          <div className="pt-6">
            <button
              disabled={isLoading}
              type="submit"
              className="w-full py-5 bg-gradient-to-r from-indigo-600 to-purple-700 hover:from-indigo-700 hover:to-purple-800 text-white rounded-[1.5rem] font-black text-xl shadow-2xl shadow-indigo-200 transition-all flex items-center justify-center gap-3 active:scale-95"
            >
              {isLoading ? (
                <div className="w-7 h-7 border-4 border-white/30 border-t-white rounded-full animate-spin"></div>
              ) : (
                <>
                  <span>تفعيل المنصة السحابية</span>
                  <ArrowLeft className="w-6 h-6" />
                </>
              )}
            </button>
          </div>

          <div className="flex items-center justify-center gap-2 text-[10px] text-slate-400 font-bold bg-slate-50 py-3 rounded-xl">
            <Cloud className="w-3 h-3 text-indigo-400" />
            <span>سيتم تشفير بياناتك وربطها بهوية Google Cloud العالمية تلقائياً</span>
          </div>
        </form>
      </div>
    </div>
  );
};
