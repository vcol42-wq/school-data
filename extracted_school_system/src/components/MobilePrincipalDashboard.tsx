import React, { useState, useEffect } from 'react';
import {
  Smartphone,
  Send,
  Users,
  Calendar,
  ShieldAlert,
  MessageSquare,
  Activity,
  Search,
  Menu,
  X,
  Bell,
  ArrowRight,
  TrendingUp,
  Clock,
  LogOut
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { Student, StaffMember, DayScheduleMap } from '../types';
import { sendDirective } from '../utils/syncService';
import { supabase } from '../utils/supabaseClient';

interface MobilePrincipalDashboardProps {
  students: Student[];
  staffList: StaffMember[];
  schedule: DayScheduleMap;
  schoolName: string;
  onBack: () => void;
}

export const MobilePrincipalDashboard: React.FC<MobilePrincipalDashboardProps> = ({
  students,
  staffList,
  schedule,
  schoolName,
  onBack
}) => {
  const [activeTab, setActiveTab] = useState<'home' | 'directives' | 'history' | 'monitoring'>('home');
  const [directiveTitle, setDirectiveTitle] = useState('');
  const [directiveContent, setDirectiveContent] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [msg, setMsg] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  const [directivesHistory, setDirectivesHistory] = useState<any[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  const schoolId = localStorage.getItem('diyala_school_id') || 'school_01';

  // Fetch Sent Directives History from Supabase
  const fetchDirectivesHistory = async () => {
    setIsLoadingHistory(true);
    try {
      const { data, error } = await supabase
        .from('directives')
        .select('*')
        .eq('school_id', schoolId)
        .order('created_at', { ascending: false });

      if (data) {
        setDirectivesHistory(data);
      }
    } catch (e) {
      console.error('Failed to fetch history', e);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'history') {
      fetchDirectivesHistory();
    }
  }, [activeTab]);

  // Stats
  const activeStudents = students.filter(s => s.status === 'مستمر').length;
  const activeStaff = staffList.filter(s => s.status === 'مستمر').length;

  const handleSendDirective = async () => {
    if (!directiveTitle || !directiveContent) return;
    setIsSending(true);
    const res = await sendDirective(schoolId, directiveTitle, directiveContent, 'all');
    setIsSending(false);
    if (res.success) {
      setMsg({ type: 'success', text: 'تم بث التوجيه بنجاح!' });
      setDirectiveTitle('');
      setDirectiveContent('');
    } else {
      setMsg({ type: 'error', text: res.message });
    }
    setTimeout(() => setMsg(null), 3000);
  };

  return (
    <div className="flex flex-col h-screen bg-slate-50 font-sans dir-rtl overflow-hidden">

      {/* Mobile Top Header */}
      <header className="bg-indigo-600 p-5 pt-8 text-white flex items-center justify-between shadow-lg shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center backdrop-blur-md">
            <Smartphone className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-lg font-black leading-tight">المدير المتنقل</h1>
            <p className="text-[10px] font-bold opacity-70">{schoolName}</p>
          </div>
        </div>
        <button
          onClick={onBack}
          className="p-2 rounded-xl bg-white/10 hover:bg-white/20 transition-all cursor-pointer"
        >
          <LogOut className="w-5 h-5" />
        </button>
      </header>

      {/* Main Scrollable Content */}
      <main className="flex-1 overflow-y-auto p-4 pb-24 space-y-5">

        {activeTab === 'home' && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-5"
          >
            {/* Quick Stats Grid */}
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-white p-5 rounded-3xl border border-slate-100 shadow-sm">
                <Users className="w-6 h-6 text-blue-600 mb-2" />
                <span className="text-[10px] font-black text-slate-400 block uppercase">الطلاب</span>
                <span className="text-xl font-black text-slate-900">{activeStudents}</span>
              </div>
              <div className="bg-white p-5 rounded-3xl border border-slate-100 shadow-sm">
                <Activity className="w-6 h-6 text-emerald-600 mb-2" />
                <span className="text-[10px] font-black text-slate-400 block uppercase">الكادر</span>
                <span className="text-xl font-black text-slate-900">{activeStaff}</span>
              </div>
            </div>

            {/* Current Lesson Info */}
            <div className="bg-gradient-to-br from-indigo-600 to-blue-700 rounded-3xl p-6 text-white shadow-xl relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10">
                <Clock className="w-20 h-20" />
              </div>
              <div className="relative z-10">
                <h3 className="text-sm font-black opacity-80 mb-1">الحصة الحالية (الرابعة)</h3>
                <p className="text-lg font-black">جولة المتابعة الميدانية</p>
                <div className="mt-4 flex items-center gap-2 bg-white/10 p-3 rounded-2xl border border-white/20">
                  <TrendingUp className="w-4 h-4 text-amber-400" />
                  <span className="text-xs font-bold">نسبة الحضور اليومي للمدرسة: 94%</span>
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="space-y-3">
               <button
                onClick={() => setActiveTab('directives')}
                className="w-full p-5 bg-white border border-slate-200 rounded-3xl flex items-center justify-between group active:scale-95 transition-all shadow-sm"
               >
                 <div className="flex items-center gap-4">
                   <div className="p-3 bg-amber-100 text-amber-600 rounded-2xl group-hover:bg-amber-600 group-hover:text-white transition-colors">
                     <Bell className="w-5 h-5" />
                   </div>
                   <div className="text-right">
                     <p className="font-black text-slate-900 text-sm">إرسال تعليمات عاجلة</p>
                     <p className="text-[10px] text-slate-400 font-bold">بث إشعارات لهواتف المعلمين</p>
                   </div>
                 </div>
                 <ArrowRight className="w-5 h-5 text-slate-300" />
               </button>

               <button
                onClick={() => setActiveTab('history')}
                className="w-full p-5 bg-white border border-slate-200 rounded-3xl flex items-center justify-between group active:scale-95 transition-all shadow-sm"
               >
                 <div className="flex items-center gap-4">
                   <div className="p-3 bg-indigo-100 text-indigo-600 rounded-2xl group-hover:bg-indigo-600 group-hover:text-white transition-colors">
                     <MessageSquare className="w-5 h-5" />
                   </div>
                   <div className="text-right">
                     <p className="font-black text-slate-900 text-sm">أرشيف التوجيهات المربوطة</p>
                     <p className="text-[10px] text-slate-400 font-bold">سجل الرسائل والتعليمات السابقة</p>
                   </div>
                 </div>
                 <ArrowRight className="w-5 h-5 text-slate-300" />
               </button>

               <button
                onClick={() => setActiveTab('monitoring')}
                className="w-full p-5 bg-white border border-slate-200 rounded-3xl flex items-center justify-between group active:scale-95 transition-all shadow-sm"
               >
                 <div className="flex items-center gap-4">
                   <div className="p-3 bg-blue-100 text-blue-600 rounded-2xl group-hover:bg-blue-600 group-hover:text-white transition-colors">
                     <Calendar className="w-5 h-5" />
                   </div>
                   <div className="text-right">
                     <p className="font-black text-slate-900 text-sm">مراقبة جدول الحصص</p>
                     <p className="text-[10px] text-slate-400 font-bold">رؤية المدرسين المتواجدين في الصفوف</p>
                   </div>
                 </div>
                 <ArrowRight className="w-5 h-5 text-slate-300" />
               </button>
            </div>
          </motion.div>
        )}

        {activeTab === 'directives' && (
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="space-y-6"
          >
            <div className="bg-white p-6 rounded-[2.5rem] border border-slate-200 shadow-sm space-y-5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 bg-amber-500 rounded-2xl text-white shadow-lg shadow-amber-200">
                    <Send className="w-5 h-5" />
                  </div>
                  <h3 className="text-lg font-black text-slate-900">بث تعليمات إدارية</h3>
                </div>
                <button
                  onClick={() => setActiveTab('history')}
                  className="px-3 py-1.5 bg-indigo-50 text-indigo-600 rounded-xl text-xs font-black hover:bg-indigo-100 transition-all cursor-pointer"
                >
                  عرض السجل 📜
                </button>
              </div>

              <div className="space-y-4">
                <input
                  type="text"
                  value={directiveTitle}
                  onChange={(e) => setDirectiveTitle(e.target.value)}
                  placeholder="عنوان التوجيه (مثال: اجتماع طارئ)"
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl p-4 text-sm font-bold outline-none focus:border-amber-500 focus:bg-white transition-all"
                />
                <textarea
                  value={directiveContent}
                  onChange={(e) => setDirectiveContent(e.target.value)}
                  placeholder="اكتب نص التعليمات هنا..."
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl p-4 text-sm font-bold outline-none focus:border-amber-500 focus:bg-white transition-all h-40 resize-none"
                />
                <button
                  onClick={handleSendDirective}
                  disabled={isSending || !directiveTitle}
                  className="w-full py-4 bg-amber-500 text-slate-900 rounded-2xl font-black shadow-lg hover:bg-amber-600 transition-all flex items-center justify-center gap-2 disabled:opacity-50 cursor-pointer active:scale-95"
                >
                  {isSending ? 'جاري البث...' : 'بث الإشعار فوراً 🚀'}
                </button>
              </div>
            </div>

            {msg && (
              <div className={`p-4 rounded-2xl text-center font-black text-xs ${msg.type === 'success' ? 'bg-emerald-50 text-emerald-600 border border-emerald-100' : 'bg-rose-50 text-rose-600 border border-rose-100'}`}>
                {msg.text}
              </div>
            )}
          </motion.div>
        )}

        {activeTab === 'history' && (
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="space-y-4"
          >
            <div className="flex items-center justify-between px-2">
              <h3 className="text-base font-black text-slate-900 flex items-center gap-2">
                <MessageSquare className="w-5 h-5 text-indigo-600" />
                سجل التوجيهات المرسلة
              </h3>
              <button
                onClick={fetchDirectivesHistory}
                className="text-xs font-black text-indigo-600 bg-indigo-50 px-3 py-1.5 rounded-xl"
              >
                تحديث
              </button>
            </div>

            {isLoadingHistory ? (
              <div className="p-8 text-center text-slate-400 font-bold text-xs">جاري تحميل السجل...</div>
            ) : directivesHistory.length === 0 ? (
              <div className="bg-white p-8 rounded-3xl border border-slate-200 text-center text-slate-400 font-bold text-xs">
                لا توجد توجيهات سابقة محفوظة في السحابة.
              </div>
            ) : (
              <div className="space-y-3">
                {directivesHistory.map((item) => (
                  <div key={item.id} className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-2">
                    <div className="flex items-center justify-between border-b pb-2 border-slate-100">
                      <span className="font-black text-slate-900 text-sm">{item.title}</span>
                      <span className="text-[10px] font-bold text-slate-400">
                        {new Date(item.created_at).toLocaleDateString('ar-IQ')} - {new Date(item.created_at).toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p className="text-xs text-slate-600 font-bold leading-relaxed">{item.content}</p>
                    <div className="flex items-center justify-between pt-1 text-[10px] font-black text-slate-400">
                      <span className="bg-emerald-50 text-emerald-600 px-2.5 py-0.5 rounded-full border border-emerald-100">تم البث للجميع</span>
                      <span>معرف المدرسة: {item.school_id}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </motion.div>
        )}

        {activeTab === 'monitoring' && (
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="space-y-4"
          >
            <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm">
               <h3 className="text-sm font-black text-slate-900 mb-4 flex items-center gap-2">
                 <ShieldAlert className="w-4 h-4 text-rose-500" />
                 مراقبة الميدان (يوم الأحد)
               </h3>
               <div className="space-y-3">
                 {schedule['الأحد']?.slice(0, 5).map((row, idx) => (
                   <div key={idx} className="flex items-center justify-between p-4 bg-slate-50 rounded-2xl border border-slate-100">
                     <div>
                       <span className="text-[10px] font-black text-slate-400 block">{row.grade}</span>
                       <span className="text-sm font-black text-slate-900">{row.section}</span>
                     </div>
                     <div className="text-left">
                       <span className="text-xs font-black text-indigo-600 block">{row.lessons.lesson1.subject}</span>
                       <span className="text-[10px] text-slate-500 font-bold">{row.lessons.lesson1.teacherName}</span>
                     </div>
                   </div>
                 ))}
               </div>
            </div>
          </motion.div>
        )}

      </main>

      {/* Mobile Bottom Navigation */}
      <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-slate-200 px-6 py-4 flex items-center justify-between shadow-[0_-10px_30px_-15px_rgba(0,0,0,0.1)] z-50">
        <button
          onClick={() => setActiveTab('home')}
          className={`flex flex-col items-center gap-1 transition-all ${activeTab === 'home' ? 'text-indigo-600' : 'text-slate-400'}`}
        >
          <Activity className="w-6 h-6" />
          <span className="text-[9px] font-black uppercase">الرئيسية</span>
        </button>
        <button
          onClick={() => setActiveTab('monitoring')}
          className={`flex flex-col items-center gap-1 transition-all ${activeTab === 'monitoring' ? 'text-indigo-600' : 'text-slate-400'}`}
        >
          <Calendar className="w-6 h-6" />
          <span className="text-[9px] font-black uppercase">المتابعة</span>
        </button>
        <button
          onClick={() => setActiveTab('directives')}
          className={`flex flex-col items-center gap-1 transition-all ${activeTab === 'directives' ? 'text-indigo-600' : 'text-slate-400'}`}
        >
          <Bell className="w-6 h-6" />
          <span className="text-[9px] font-black uppercase">توجيهات</span>
        </button>
        <button className="flex flex-col items-center gap-1 text-slate-400">
          <Search className="w-6 h-6" />
          <span className="text-[9px] font-black uppercase">بحث</span>
        </button>
      </nav>

    </div>
  );
};
