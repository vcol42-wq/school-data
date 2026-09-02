import React, { useState, useEffect } from 'react';
import {
  CloudDownload,
  CloudUpload,
  Calendar,
  ShieldCheck,
  AlertCircle,
  CheckCircle2,
  Loader2,
  ArrowRight,
  Database,
  Sparkles,
  Share2
} from 'lucide-react';
import { DayScheduleMap, Student, StaffMember } from '../types';
import { motion } from 'motion/react';
import { getSupabase } from '../utils/supabaseClient';
import { exportSchoolData } from '../utils/syncService';

interface CloudScheduleViewProps {
  currentSchedule: DayScheduleMap;
  onImport: (newSchedule: DayScheduleMap) => void;
  onBack: () => void;
  students: Student[];
  staffList: StaffMember[];
}

export const CloudScheduleView: React.FC<CloudScheduleViewProps> = ({
  currentSchedule,
  onImport,
  onBack,
  students,
  staffList
}) => {
  const [isSyncing, setIsSyncing] = useState(false);
  const [cloudSchedule, setCloudSchedule] = useState<DayScheduleMap | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const configSaved = JSON.parse(localStorage.getItem('diyala_school_config') || '{}');
  const schoolId = configSaved.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
  const pairingCode = configSaved.pairingCode || localStorage.getItem('diyala_pairing_code') || '112233';
  const schoolName = configSaved.schoolName || 'مدرستنا';
  const adminEmail = configSaved.adminEmail || '';

  // Function to fetch schedule from Supabase
  const fetchCloudSchedule = async () => {
    setIsSyncing(true);
    setError(null);
    setSuccessMsg(null);

    try {
      const client = getSupabase(schoolId);
      const { data, error: fetchError } = await client
        .from('schedules')
        .select('schedule_map')
        .eq('id', schoolId)
        .single();

      if (fetchError) throw fetchError;

      if (data && data.schedule_map) {
        setCloudSchedule(data.schedule_map as DayScheduleMap);
        setSuccessMsg("تم جلب النسخة السحابية بنجاح!");
      } else {
        setError("لا يوجد جدول مخزن لهذه المدرسة في السحابة حالياً.");
      }
    } catch (e: any) {
      setError("فشل في استرداد البيانات: " + (e.message || "تأكد من الاتصال بالإنترنت."));
    } finally {
      setIsSyncing(false);
    }
  };

  // Function to export current schedule to Supabase
  const handleExport = async () => {
    setIsSyncing(true);
    setError(null);
    setSuccessMsg(null);

    try {
      const res = await exportSchoolData(
        schoolId,
        schoolName,
        pairingCode,
        adminEmail,
        students,
        staffList,
        currentSchedule
      );

      if (res.success) {
        setSuccessMsg("تم تصدير الجدول إلى سحابة Supabase بنجاح! يمكن للمدرسين والطلاب الآن سحب التحديث.");
      } else {
        setError(res.message);
      }
    } catch (e: any) {
      setError("خطأ أثناء التصدير: " + e.message);
    } finally {
      setIsSyncing(false);
    }
  };


  const handleImport = () => {
    if (cloudSchedule) {
      if (confirm("هل أنت متأكد من استيراد هذا الجدول؟ سيتم استبدال الجدول الحالي لديك.")) {
        onImport(cloudSchedule);
        setSuccessMsg("تم تحديث الجدول المحلي بنجاح!");
        setTimeout(() => onBack(), 2000);
      }
    }
  };

  return (
    <div className="p-4 md:p-8 max-w-6xl mx-auto space-y-8 dir-rtl font-sans">

      {/* Dynamic Header */}
      <div className="bg-gradient-to-l from-indigo-700 to-blue-800 p-8 rounded-[2.5rem] text-white shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 p-12 opacity-10 rotate-12">
          <Database className="w-40 h-40" />
        </div>
        <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-5">
            <div className="p-4 bg-white/20 backdrop-blur-md rounded-3xl border border-white/20 shadow-xl">
              <CloudUpload className="w-10 h-10 text-amber-400" />
            </div>
            <div>
              <h1 className="text-3xl font-black">مزامنة الجدول السحابي (Supabase)</h1>
              <p className="text-blue-100 font-bold opacity-80 mt-1">تصدير الجدول للمدرسين واستيراد النسخ الاحتياطية</p>
            </div>
          </div>
          <button
            onClick={onBack}
            className="px-6 py-3 bg-white/10 hover:bg-white/20 rounded-2xl border border-white/30 transition-all cursor-pointer flex items-center gap-2 font-black text-sm"
          >
            <ArrowRight className="w-5 h-5" />
            <span>العودة للرئيسية</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">

        {/* EXPORT CARD */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          className="bg-white rounded-[3rem] border border-slate-200 p-8 shadow-xl space-y-6 flex flex-col"
        >
          <div className="flex items-center gap-4 mb-2">
            <div className="p-3 bg-indigo-100 text-indigo-600 rounded-2xl">
              <Share2 className="w-6 h-6" />
            </div>
            <h2 className="text-xl font-black text-slate-900">تصدير الجدول للسحابة</h2>
          </div>
          <p className="text-sm text-slate-500 font-bold leading-relaxed">
            عند الضغط على تصدير، سيتم رفع النسخة الحالية من الجدول الدراسي إلى خادم Supabase. سيتمكن المدرسون المرتبطون والطلاب من رؤية التحديثات فوراً في تطبيقاتهم.
          </p>
          <div className="flex-1 bg-slate-50 rounded-3xl p-6 border-2 border-dashed border-slate-200 flex flex-col items-center justify-center gap-4">
            <div className="text-center">
              <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block mb-1">المدرسة المرتبطة</span>
              <span className="text-lg font-black text-indigo-600">{schoolName}</span>
            </div>
            <div className="flex items-center gap-2 px-4 py-1.5 bg-white rounded-full border border-slate-200 text-xs font-black text-slate-500 shadow-sm">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
              <span>مؤمن عبر Supabase RLS</span>
            </div>
          </div>
          <button
            onClick={handleExport}
            disabled={isSyncing}
            className="w-full py-4 bg-indigo-600 text-white rounded-2xl font-black shadow-xl hover:bg-indigo-700 transition-all hover:scale-105 active:scale-95 disabled:opacity-50 cursor-pointer flex items-center justify-center gap-3"
          >
            {isSyncing ? <Loader2 className="w-6 h-6 animate-spin" /> : <CloudUpload className="w-6 h-6" />}
            <span>رفع وتصدير الجدول الآن</span>
          </button>
        </motion.div>

        {/* IMPORT CARD */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          className="bg-white rounded-[3rem] border border-slate-200 p-8 shadow-xl space-y-6 flex flex-col"
        >
          <div className="flex items-center gap-4 mb-2">
            <div className="p-3 bg-blue-100 text-blue-600 rounded-2xl">
              <CloudDownload className="w-6 h-6" />
            </div>
            <h2 className="text-xl font-black text-slate-900">استيراد الجدول من السحابة</h2>
          </div>
          <p className="text-sm text-slate-500 font-bold leading-relaxed">
            هل تريد استعادة الجدول من السحابة؟ استخدم هذه الميزة إذا قمت بتغيير الجهاز أو تريد العودة إلى آخر نسخة تم رفعها بنجاح.
          </p>

          <div className="flex-1 flex flex-col gap-4">
            {cloudSchedule ? (
              <div className="bg-blue-50 border border-blue-200 rounded-3xl p-5 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-black text-blue-700 flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5" /> تم العثور على نسخة
                  </span>
                  <button onClick={() => setCloudSchedule(null)} className="text-[10px] font-black text-slate-400 hover:text-rose-500">إلغاء</button>
                </div>
                <div className="flex items-center justify-between bg-white p-4 rounded-2xl shadow-sm border border-blue-100">
                   <div>
                      <p className="text-xs font-black text-slate-900">جدول يوم الأحد</p>
                      <p className="text-[10px] text-slate-500 font-bold">{cloudSchedule['الأحد']?.length || 0} صفوف دراسية</p>
                   </div>
                   <button
                    onClick={handleImport}
                    className="px-4 py-2 bg-blue-600 text-white rounded-xl text-xs font-black shadow-md hover:bg-blue-700 transition-all cursor-pointer"
                   >
                     استيراد وتحديث
                   </button>
                </div>
              </div>
            ) : (
              <div className="flex-1 bg-slate-50 rounded-3xl p-6 border-2 border-dashed border-slate-200 flex flex-col items-center justify-center text-center gap-3">
                <CloudDownload className="w-12 h-12 text-slate-300" />
                <p className="text-xs font-bold text-slate-400">اضغط على الزر أدناه لفحص النسخة السحابية المتاحة</p>
              </div>
            )}
          </div>

          <button
            onClick={fetchCloudSchedule}
            disabled={isSyncing}
            className="w-full py-4 bg-slate-800 text-white rounded-2xl font-black shadow-xl hover:bg-slate-900 transition-all hover:scale-105 active:scale-95 disabled:opacity-50 cursor-pointer flex items-center justify-center gap-3"
          >
            {isSyncing ? <Loader2 className="w-6 h-6 animate-spin" /> : <CloudDownload className="w-6 h-6" />}
            <span>فحص وجلب النسخة السحابية</span>
          </button>
        </motion.div>

      </div>

      {/* Messages */}
      {error && (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="p-5 bg-rose-50 border-2 border-rose-200 rounded-[2rem] flex items-center gap-4 text-rose-700 shadow-lg">
          <AlertCircle className="w-6 h-6 shrink-0" />
          <p className="text-sm font-black">{error}</p>
        </motion.div>
      )}

      {successMsg && (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="p-5 bg-emerald-50 border-2 border-emerald-200 rounded-[2rem] flex items-center gap-4 text-emerald-700 shadow-lg">
          <CheckCircle2 className="w-6 h-6 shrink-0" />
          <p className="text-sm font-black">{successMsg}</p>
        </motion.div>
      )}

      {/* Footer Pro Tip */}
      <div className="bg-indigo-50 p-6 rounded-[2.5rem] border border-indigo-100 flex items-center gap-4 shadow-sm">
        <Sparkles className="w-8 h-8 text-amber-500 shrink-0" />
        <p className="text-xs font-bold text-indigo-900 leading-relaxed">
          تلميح إداري: المزامنة السحابية عبر Supabase تضمن أن يكون المدرس والطالب على اطلاع دائم بأي تغيير في الجدول الدراسي لحظة بلحظة، دون الحاجة لطباعة نسخ ورقية جديدة عند كل تغيير.
        </p>
      </div>

    </div>
  );
};
