import React, { useState } from 'react';
import { ActiveView } from '../types';
import { AppLogo } from './AppLogo';
import { 
  Search,
  CalendarDays, 
  GraduationCap, 
  UserMinus,
  Users, 
  BarChart3, 
  Printer, 
  Palette, 
  Type, 
  BellRing, 
  Settings,
  Sparkles,
  LayoutGrid,
  Circle,
  Square,
  ChevronLeft,
  Mic,
  Laptop,
  Smartphone,
  CloudCheck,
  Cloud,
  X
} from 'lucide-react';
import { PrincipalSyncDashboard } from './PrincipalSyncDashboard';

interface MainLauncherProps {
  setActiveView: (view: ActiveView) => void;
  studentsCount: number;
  staffCount: number;
  onOpenVoiceModal?: () => void;
  students: any[];
  scheduleMap: any;
}

export const MainLauncher: React.FC<MainLauncherProps> = ({
  setActiveView,
  studentsCount,
  staffCount,
  onOpenVoiceModal,
  students,
  scheduleMap
}) => {
  const [iconShape, setIconShape] = useState<'squircle' | 'round'>('squircle');
  const [showSyncPanel, setShowSyncDashboard] = useState(false);

  const launcherItems = [
    {
      id: 'teacher_portal' as ActiveView,
      title: 'تطبيق المدرسين الويب',
      subtitle: 'رصد الدرجات بالسحابة',
      icon: Smartphone,
      gradient: 'from-blue-600 via-indigo-600 to-purple-700',
      badge: 'تطبيق الويب 📱',
      badgeBg: 'bg-emerald-400 text-slate-950 font-black animate-pulse'
    },
    {
      id: 'sync_center' as any, // Virtual ID for the sync panel
      title: 'مركز المزامنة السحابي',
      subtitle: 'ربط المدرسين والبيانات',
      icon: Cloud,
      gradient: 'from-indigo-600 to-blue-900',
      badge: 'سحابة ☁️',
      badgeBg: 'bg-amber-400 text-slate-950 font-black'
    },
    {
      id: 'schedule' as ActiveView,
      title: 'الجدول الدراسي',
      subtitle: 'الحصص والتوقيتات',
      icon: CalendarDays,
      gradient: 'from-blue-600 to-indigo-700',
      badge: 'اليومي',
      badgeBg: 'bg-blue-100 text-blue-800'
    },
    {
      id: 'students' as ActiveView,
      title: 'سجل الطلاب الموحد',
      subtitle: `${studentsCount} طالب مقيد`,
      icon: GraduationCap,
      gradient: 'from-emerald-500 to-teal-700',
      badge: `${studentsCount}`,
      badgeBg: 'bg-emerald-100 text-emerald-800'
    },
    {
      id: 'students' as ActiveView,
      title: 'البحث السريع الموحد',
      subtitle: 'البحث بالاسم والقيد والموحدة',
      icon: Search,
      gradient: 'from-sky-500 to-blue-700',
      badge: 'بحث 🔍',
      badgeBg: 'bg-sky-100 text-sky-950 font-black'
    },
    {
      id: 'former_students' as ActiveView,
      title: 'الطلاب السابقون',
      subtitle: 'أرشيف المغادرين والممتنعين',
      icon: UserMinus,
      gradient: 'from-rose-600 to-pink-700',
      badge: 'أرشيف 📂',
      badgeBg: 'bg-rose-100 text-rose-950 font-black'
    },
    {
      id: 'staff' as ActiveView,
      title: 'سجل الكادر',
      subtitle: `${staffCount} منتسب ومدرس`,
      icon: Users,
      gradient: 'from-purple-600 to-violet-800',
      badge: `${staffCount}`,
      badgeBg: 'bg-purple-100 text-purple-800'
    },
    {
      id: 'stats' as ActiveView,
      title: 'الإحصاءات والملاك',
      subtitle: 'الشواغر والفائض',
      icon: BarChart3,
      gradient: 'from-amber-500 to-orange-700',
      badge: 'شامل',
      badgeBg: 'bg-amber-100 text-amber-800'
    },
    {
      id: 'print' as ActiveView,
      title: 'مركز الطباعة',
      subtitle: 'الكتب والوثائق',
      icon: Printer,
      gradient: 'from-cyan-500 to-blue-700',
      badge: 'رسمي A4',
      badgeBg: 'bg-cyan-100 text-cyan-800'
    },
    {
      id: 'alarm' as ActiveView,
      title: 'المنبه والجرس',
      subtitle: 'التوقيت المباشر',
      icon: BellRing,
      gradient: 'from-yellow-500 to-amber-600',
      badge: 'جرس',
      badgeBg: 'bg-yellow-100 text-yellow-800'
    },
    {
      id: 'themes' as ActiveView,
      title: 'الألوان والثيمات',
      subtitle: 'تخصيص الواجهة',
      icon: Palette,
      gradient: 'from-pink-500 to-rose-700',
      badge: 'ثيمات',
      badgeBg: 'bg-pink-100 text-pink-800'
    },
    {
      id: 'fonts' as ActiveView,
      title: 'الخطوط الرسمية',
      subtitle: 'تغيير الخط العربي',
      icon: Type,
      gradient: 'from-red-500 to-rose-800',
      badge: 'خطوط',
      badgeBg: 'bg-rose-100 text-rose-800'
    },
    {
      id: 'settings' as ActiveView,
      title: 'الإعدادات والأمان',
      subtitle: 'بيانات المدرسة',
      icon: Settings,
      gradient: 'from-slate-700 to-slate-900',
      badge: 'حماية',
      badgeBg: 'bg-slate-200 text-slate-800'
    }
  ];

  return (
    <div className="max-w-6xl mx-auto px-4 py-6 md:py-8">
      
      {/* 3D Announcement Banner Container with Ornate Golden Border & Side Shadow */}
      <div 
        className="relative overflow-hidden mb-8 rounded-2xl bg-gradient-to-r from-purple-950 via-indigo-900 to-purple-900 text-white p-5 border-3 border-amber-400 ring-2 ring-purple-950/50 flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
        style={{
          boxShadow: '10px 10px 0px 0px rgba(15, 23, 42, 0.85), 0px 15px 30px -5px rgba(88, 28, 135, 0.45)'
        }}
      >
        {/* Ornate Golden Corner Brackets */}
        <div className="absolute top-1.5 right-1.5 w-4 h-4 border-t-2 border-r-2 border-amber-300 rounded-tr-sm pointer-events-none" />
        <div className="absolute top-1.5 left-1.5 w-4 h-4 border-t-2 border-l-2 border-amber-300 rounded-tl-sm pointer-events-none" />
        <div className="absolute bottom-1.5 right-1.5 w-4 h-4 border-b-2 border-r-2 border-amber-300 rounded-br-sm pointer-events-none" />
        <div className="absolute bottom-1.5 left-1.5 w-4 h-4 border-b-2 border-l-2 border-amber-300 rounded-bl-sm pointer-events-none" />

        <div className="flex items-center gap-3.5 relative z-10">
          <div className="p-3 rounded-2xl bg-amber-400 text-slate-950 shadow-lg border border-amber-200 shrink-0">
            <LayoutGrid className="w-6 h-6 text-slate-950 animate-pulse" />
          </div>
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-amber-400/20 text-amber-300 text-[11px] font-black border border-amber-300/30 mb-1">
              <Sparkles className="w-3.5 h-3.5" />
              <span>تطبيق Principal الإداري الموحد</span>
            </div>
            <h2 className="text-xl md:text-2xl font-black text-amber-300 tracking-wide drop-shadow-md">
              سطح المكتب - لوحة التطبيقات الرسمية
            </h2>
            <p className="text-xs text-purple-200 font-bold mt-0.5">
              اضغط على أيقونة التطبيق المطلوب للذهاب الفوري إلى الصفحة الإدارية المخصصة
            </p>
          </div>
        </div>

        {/* Middle Banner Tools: Voice Assistant, Shape Switcher, Counts */}
        <div className="flex flex-wrap items-center gap-3 bg-purple-900/60 p-2.5 rounded-2xl border border-amber-400/40 relative z-10 shrink-0 self-stretch md:self-auto justify-between">
          {/* Voice Assistant Button */}
          {onOpenVoiceModal && (
            <button
              onClick={onOpenVoiceModal}
              className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-amber-400 text-slate-950 font-black text-xs hover:bg-amber-300 transition-all shadow-md cursor-pointer whitespace-nowrap"
              title="تفعيل الأوامر الصوتية باللغة العربية"
            >
              <Mic className="w-4 h-4 text-slate-950 animate-bounce" />
              <span>المساعد الصوتي</span>
            </button>
          )}

          {/* Shape Switcher */}
          <div className="flex items-center gap-1 bg-black/40 p-1 rounded-xl">
            <button
              onClick={() => setIconShape('squircle')}
              className={`p-1.5 rounded-lg text-xs font-black flex items-center gap-1 transition-all ${
                iconShape === 'squircle' ? 'bg-amber-400 text-slate-950 shadow-sm' : 'text-slate-300 hover:text-white'
              }`}
              title="أيقونات مربعة منحنية"
            >
              <Square className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">مربعة</span>
            </button>
            <button
              onClick={() => setIconShape('round')}
              className={`p-1.5 rounded-lg text-xs font-black flex items-center gap-1 transition-all ${
                iconShape === 'round' ? 'bg-amber-400 text-slate-950 shadow-sm' : 'text-slate-300 hover:text-white'
              }`}
              title="أيقونات مدورة"
            >
              <Circle className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">مدورة</span>
            </button>
          </div>

          {/* Quick Counts Pill */}
          <div className="flex items-center gap-2 text-xs font-black bg-white/10 px-3 py-1.5 rounded-xl text-white border border-white/20">
            <span className="text-amber-300">{studentsCount} طالب</span>
            <span className="text-white/40">|</span>
            <span className="text-amber-300">{staffCount} كادر</span>
          </div>
        </div>
      </div>

      {/* 3D Desktop App Icons Container with Ornate Golden Border & Directional Side Shadow */}
      <div 
        className="relative overflow-hidden bg-white border-3 border-amber-400 ring-4 ring-purple-950/40 rounded-3xl p-6 md:p-8"
        style={{
          boxShadow: '12px 12px 0px 0px rgba(15, 23, 42, 0.85), 0px 20px 40px -5px rgba(88, 28, 135, 0.4)'
        }}
      >
        {/* Ornate Golden Frame Accents */}
        <div className="absolute top-2 right-2 w-6 h-6 border-t-3 border-r-3 border-amber-400 rounded-tr-md pointer-events-none" />
        <div className="absolute top-2 left-2 w-6 h-6 border-t-3 border-l-3 border-amber-400 rounded-tl-md pointer-events-none" />
        <div className="absolute bottom-2 right-2 w-6 h-6 border-b-3 border-r-3 border-amber-400 rounded-br-md pointer-events-none" />
        <div className="absolute bottom-2 left-2 w-6 h-6 border-b-3 border-l-3 border-amber-400 rounded-bl-md pointer-events-none" />

        <div className="flex items-center justify-between mb-6 pb-3 border-b-2 border-sky-300">
          <div className="flex items-center gap-2">
            <LayoutGrid className="w-5 h-5 text-amber-500" />
            <h2 className="text-base md:text-lg font-black text-slate-950">
              أيقونات الوصول السريع للصفحات
            </h2>
          </div>
          <span className="text-xs font-black text-slate-700 bg-amber-100 border border-amber-300 px-3 py-1 rounded-xl shadow-xs">
            اختر أيقونة للفتح الفوري
          </span>
        </div>

        {/* Desktop Launcher Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6 md:gap-8 justify-items-center">
          {launcherItems.map((item) => {
            const IconComponent = item.icon;
            
            return (
              <button
                key={item.id}
                onClick={() => {
                  if (item.id === 'sync_center') {
                    setShowSyncDashboard(true);
                  } else {
                    setActiveView(item.id as ActiveView);
                  }
                }}
                className="group flex flex-col items-center text-center cursor-pointer transition-all duration-200 transform hover:-translate-y-1.5 focus:outline-none focus:ring-2 focus:ring-amber-500/50 rounded-2xl p-2 w-full max-w-[130px]"
              >
                {/* 3D Directional Side Drop Shadow Box + Ornate Frame */}
                <div 
                  className={`relative w-20 h-20 md:w-22 md:h-22 bg-gradient-to-br ${item.gradient} text-white flex items-center justify-center transition-all duration-300 group-hover:scale-105 border-2 border-amber-400/90 ring-2 ring-purple-900/40 ${
                    iconShape === 'squircle' 
                      ? 'rounded-[26%]' 
                      : 'rounded-full'
                  }`}
                  style={{
                    boxShadow: '7px 7px 0px 0px rgba(15, 23, 42, 0.85), 10px 10px 20px -2px rgba(88, 28, 135, 0.4)'
                  }}
                >
                  {/* Ornate corner dot flourishes */}
                  <div className="absolute top-1 left-1 w-1.5 h-1.5 bg-amber-300 rounded-full shadow-xs" />
                  <div className="absolute bottom-1 right-1 w-1.5 h-1.5 bg-amber-300 rounded-full shadow-xs" />

                  {/* Inner gloss highlight */}
                  <div className="absolute inset-0 bg-gradient-to-t from-transparent via-white/10 to-white/30 rounded-[inherit] pointer-events-none" />

                  {/* Icon Symbol */}
                  <IconComponent className="w-9 h-9 md:w-10 md:h-10 text-white drop-shadow-md group-hover:rotate-3 transition-transform duration-300 z-10" />

                  {/* Corner Badge */}
                  <span className={`absolute -top-2 -right-2 px-2 py-0.5 rounded-full text-[10px] font-black shadow-lg border-2 border-white ${item.badgeBg}`}>
                    {item.badge}
                  </span>
                </div>

                {/* App Label under icon */}
                <span className="mt-3 text-xs md:text-sm font-extrabold text-[var(--theme-text-main)] group-hover:text-blue-600 dark:group-hover:text-amber-400 transition-colors leading-tight">
                  {item.title}
                </span>

                {/* Subtitle / Description */}
                <span className="text-[11px] text-[var(--theme-text-muted)] mt-0.5 line-clamp-1 font-medium opacity-85">
                  {item.subtitle}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Footer Info Note */}
      <div className="mt-8 text-center text-xs text-[var(--theme-text-muted)] flex items-center justify-center gap-2">
        <span>يمكنك أيضاً فتح القائمة الجانبية بالضغط على الأسطر الثلاثة (☰) في الشريط الأعلى.</span>
      </div>

      {/* Sync Dashboard Modal */}
      {showSyncPanel && (
        <div className="fixed inset-0 z-[60] bg-slate-900/80 backdrop-blur-sm flex items-center justify-center p-4 md:p-8">
          <div className="max-w-5xl w-full relative">
            <button
              onClick={() => setShowSyncDashboard(false)}
              className="absolute -top-12 left-0 p-2 bg-white/10 hover:bg-white/20 rounded-full text-white transition-all"
            >
              <X className="w-6 h-6" />
            </button>
            <PrincipalSyncDashboard students={students} schedule={scheduleMap} />
          </div>
        </div>
      )}

    </div>
  );
};
