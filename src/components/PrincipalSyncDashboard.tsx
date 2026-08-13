import React, { useState, useEffect } from 'react';
import { Student, DayScheduleMap } from '../types';
import { Cloud, CloudUpload, RefreshCw, Users, BookOpen, CheckCircle2, ShieldCheck, Activity, QrCode, Smartphone, Link as LinkIcon, Key as VpnKey } from 'lucide-react';
import { QrCodeSvg } from './QrCodeSvg';

interface PrincipalSyncDashboardProps {
  students: Student[];
  schedule: DayScheduleMap;
}

export const PrincipalSyncDashboard: React.FC<PrincipalSyncDashboardProps> = ({ students, schedule }) => {
  const [isSyncing, setIsSyncing] = useState(false);
  const [lastSync, setLastSync] = useState<string | null>(null);
  const [schoolId, setSchoolId] = useState<string>('999888'); // The Unified Code
  const [serverUrl, setServerUrl] = useState<string>('جاري استخراج الرابط...');
  const [teachers, setTeachers] = useState<any[]>([]);

  useEffect(() => {
    // Fetch Local IP and active teachers
    const fetchInfo = async () => {
      try {
        const qrResp = await fetch('/api/sync/qr-data');
        const qrData = await qrResp.json();
        if (qrData.success) {
          setServerUrl(qrData.url.replace('http://', ''));
          setSchoolId(qrData.pairingCode);
        }

        const teachResp = await fetch('/api/cloud/teachers');
        const teachData = await teachResp.json();
        if (teachData.success) setTeachers(teachData.teachers);
      } catch (e) { console.error(e); }
    };
    fetchInfo();
    const timer = setInterval(fetchInfo, 5000);
    return () => clearInterval(timer);
  }, []);

  const handleSyncAll = async () => {
    setIsSyncing(true);
    try {
      const config = {
        schoolName: localStorage.getItem('diyala_school_name') || 'مدرستي',
        schoolId: schoolId,
        adminEmail: localStorage.getItem('diyala_admin_email')
      };
      const resp = await fetch('/api/cloud/sync-all', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ students, config })
      });
      const data = await resp.json();
      if (data.success) {
        setLastSync(new Date().toLocaleTimeString('ar-IQ'));
        alert('تم رفع قاعدة البيانات المدرسية إلى السحابة بنجاح! يمكن للمدرسين الآن تحميل الأسماء.');
      }
    } catch (e) {
      alert('خطأ في الاتصال بالسحابة');
    }
    setIsSyncing(false);
  };

  return (
    <div className="bg-white rounded-[2.5rem] border border-slate-200 shadow-2xl overflow-hidden dir-rtl">
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 p-8 text-white flex items-center justify-between border-b-4 border-amber-400">
        <div className="flex items-center gap-4">
          <div className="p-3 bg-blue-500/20 rounded-2xl border border-blue-400/30">
            <Cloud className="w-10 h-10 text-blue-400" />
          </div>
          <div>
            <h2 className="text-2xl font-black">مركز المزامنة والربط الفوري</h2>
            <p className="text-sm text-slate-400">نظام ربط هواتف المدرسين بحاسوب الإدارة</p>
          </div>
        </div>
        <div className="bg-white/10 px-6 py-3 rounded-2xl border border-white/10 text-center shadow-inner">
          <span className="block text-[10px] text-blue-300 font-bold uppercase tracking-widest mb-1">Status</span>
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse" />
            <span className="text-sm font-black text-emerald-400">متصل الآن</span>
          </div>
        </div>
      </div>

      <div className="p-8 grid grid-cols-1 lg:grid-cols-12 gap-10">

        {/* Connection Info Section (LEFT) */}
        <div className="lg:col-span-5 bg-slate-950 rounded-[2rem] p-8 text-white text-center space-y-6 shadow-2xl border-2 border-white/5 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-blue-600/10 blur-[50px]" />

          <div className="space-y-1 relative z-10">
            <h3 className="text-lg font-black text-amber-400 flex items-center justify-center gap-2">
               <QrCode className="w-5 h-5" />
               بيانات الربط مع المدرسين
            </h3>
            <p className="text-[11px] text-slate-400 font-medium">اجعل المدرسين يفتحون تطبيق المدرس ويقومون بنقل هذه البيانات</p>
          </div>

          <div className="grid grid-cols-1 gap-5 relative z-10">
            {/* Server URL Display */}
            <div className="bg-white/5 p-6 rounded-2xl border border-white/10 relative group hover:border-blue-500/50 transition-all">
              <div className="absolute -top-3 right-6 px-3 py-1 bg-blue-600 text-[10px] font-black rounded-full shadow-lg flex items-center gap-1.5">
                <LinkIcon className="w-3.5 h-3.5" />
                رابط الاتصال (IP)
              </div>
              <span className="text-3xl font-black font-mono text-white tracking-widest block mt-2">{serverUrl}</span>
            </div>

            {/* Unified Code Display */}
            <div className="bg-white/5 p-6 rounded-2xl border border-white/10 relative group hover:border-emerald-500/50 transition-all">
              <div className="absolute -top-3 right-6 px-3 py-1 bg-emerald-600 text-[10px] font-black rounded-full shadow-lg flex items-center gap-1.5">
                <VpnKey className="w-3.5 h-3.5" />
                الرمز الموحد للمدرسة
              </div>
              <span className="text-6xl font-black font-mono text-emerald-400 tracking-[0.2em] block mt-2">{schoolId}</span>
            </div>
          </div>

          <div className="pt-4 flex flex-col items-center gap-4 relative z-10">
            <div className="bg-white p-4 rounded-3xl shadow-2xl border-4 border-amber-400 transform hover:scale-105 transition-transform">
              <QrCodeSvg value={JSON.stringify({ url: serverUrl, code: schoolId })} size={160} />
            </div>
            <p className="text-[12px] text-slate-400 font-bold leading-relaxed px-6">
               أو قم بمسح <span className="text-amber-400">الباركود</span> أعلاه بواسطة تطبيق المدرس للربط التلقائي.
            </p>
          </div>
        </div>

        {/* Status & Teachers Section (RIGHT) */}
        <div className="lg:col-span-7 space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div className="bg-emerald-50 p-6 rounded-3xl border border-emerald-100 flex items-center justify-between shadow-sm">
              <div className="space-y-1">
                <span className="block text-[10px] text-emerald-600 font-black uppercase tracking-wider">حالة السحابة</span>
                <span className="text-lg font-black text-slate-800">نشطة ومتصلة ✓</span>
              </div>
              <Activity className="w-10 h-10 text-emerald-500 opacity-40" />
            </div>

            <div className="bg-blue-50 p-6 rounded-3xl border border-blue-100 flex items-center justify-between shadow-sm">
              <div className="space-y-1">
                <span className="block text-[10px] text-blue-600 font-black uppercase tracking-wider">آخر مزامنة</span>
                <span className="text-lg font-black text-slate-800">{lastSync || 'بانتظار البدء'}</span>
              </div>
              <CloudUpload className="w-10 h-10 text-blue-500 opacity-40" />
            </div>
          </div>

          <div className="bg-slate-50 p-6 rounded-[2rem] border border-slate-200 shadow-inner flex flex-col h-full min-h-[400px]">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3 text-slate-900 font-black text-base">
                <div className="p-2 bg-purple-100 rounded-xl">
                  <Smartphone className="w-6 h-6 text-purple-600" />
                </div>
                <span>المدرسون المرتبطون حالياً</span>
              </div>
              <button
                onClick={handleSyncAll}
                disabled={isSyncing}
                className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl text-xs font-black shadow-xl shadow-indigo-200 transition-all active:scale-95 flex items-center gap-2"
              >
                {isSyncing ? <RefreshCw className="w-4 h-4 animate-spin" /> : <CloudUpload className="w-4 h-4" />}
                <span>إرسال كافة الأسماء للمدرسين</span>
              </button>
            </div>

            <div className="overflow-x-auto flex-1">
              <table className="w-full text-sm text-right">
                <thead className="text-slate-400 font-bold border-b border-slate-200">
                  <tr>
                    <th className="pb-3 pr-4">اسم المدرس</th>
                    <th className="pb-3">الصف والشعبة</th>
                    <th className="pb-3">المادة</th>
                    <th className="pb-3 text-center">الحالة</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700">
                  {teachers.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="py-20 text-center text-slate-400 italic">
                         لا يوجد مدرسون مرتبطون حالياً. <br/>
                         شارك بيانات الربط (IP والرمز) الموضحة في الجهة اليمنى للبدء.
                      </td>
                    </tr>
                  ) : (
                    teachers.map(t => (
                      <tr key={t.id} className="group hover:bg-white transition-all cursor-default">
                        <td className="py-4 pr-4 font-black text-slate-900">{t.name}</td>
                        <td className="py-4 text-amber-700 font-bold">{t.grade} {t.section}</td>
                        <td className="py-4 text-indigo-700 font-bold">{t.subject}</td>
                        <td className="py-4 text-center">
                          <span className={`px-3 py-1 rounded-full text-[10px] font-black border ${
                            t.status === 'active'
                            ? 'bg-emerald-100 text-emerald-800 border-emerald-200'
                            : 'bg-slate-100 text-slate-600 border-slate-200'
                          }`}>
                            {t.status === 'active' ? 'نشط الآن' : 'غير متصل'}
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>

      </div>

      <div className="p-5 bg-indigo-50 border-t border-indigo-100 flex items-center justify-center gap-3">
        <ShieldCheck className="w-5 h-5 text-indigo-600" />
        <span className="text-[12px] text-indigo-900 font-bold">
          نظام المزامنة الذكي v2.5 - الربط عبر السحابة مع هوية المدير: {localStorage.getItem('diyala_admin_email')}
        </span>
      </div>
    </div>
  );
};
