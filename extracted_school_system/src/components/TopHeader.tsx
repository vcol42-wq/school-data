import React, { useState, useEffect } from 'react';
import { AppConfig, ActiveView, DayScheduleMap } from '../types';
import { bellAudio } from '../utils/audio';
import { AppLogo } from './AppLogo';
import { 
  Menu,
  Home,
  X,
  Building2, 
  UserCheck, 
  Clock, 
  BellRing, 
  Lock, 
  Grid, 
  Palette, 
  Type,
  CalendarDays,
  GraduationCap,
  UserMinus,
  Users,
  BarChart3,
  Printer,
  Settings,
  ChevronLeft,
  Sparkles,
  Laptop,
  Mic,
  Smartphone,
  Square,
  Circle
} from 'lucide-react';

interface TopHeaderProps {
  config: AppConfig;
  activeView: ActiveView;
  setActiveView: (view: ActiveView) => void;
  scheduleMap: DayScheduleMap;
  onOpenPasscode: () => void;
  onOpenVoiceModal?: () => void;
  studentsCount?: number;
  staffCount?: number;
  iconShape?: 'squircle' | 'round';
  setIconShape?: (shape: 'squircle' | 'round') => void;
}

export const TopHeader: React.FC<TopHeaderProps> = ({
  config,
  activeView,
  setActiveView,
  scheduleMap,
  onOpenPasscode,
  onOpenVoiceModal,
  studentsCount = 5,
  staffCount = 6,
  iconShape = 'squircle',
  setIconShape
}) => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [now, setNow] = useState<Date>(new Date());
  const [activeLessonInfo, setActiveLessonInfo] = useState<{
    lessonName: string;
    remainingSeconds: number;
    status: 'in_lesson' | 'in_break' | 'before_school' | 'after_school';
    isWarning: boolean;
  }>({
    lessonName: 'خارج أوقات الدوام',
    remainingSeconds: 0,
    status: 'after_school',
    isWarning: false
  });

  // Live Clock update
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Compute active lesson timing based on current system time
  useEffect(() => {
    const hours = now.getHours();
    const minutes = now.getMinutes();
    const seconds = now.getSeconds();

    const [startH, startM] = config.schoolStartHour.split(':').map(Number);
    const schoolStartInSec = (startH || 8) * 3600 + (startM || 0) * 60;
    const currentSec = hours * 3600 + minutes * 60 + seconds;

    const lessonSec = config.lessonDurationMinutes * 60;
    const breakSec = config.breakDurationMinutes * 60;
    const slotSec = lessonSec + breakSec;

    if (currentSec < schoolStartInSec) {
      const diff = schoolStartInSec - currentSec;
      setActiveLessonInfo({
        lessonName: 'قبل بداية الدوام',
        remainingSeconds: diff,
        status: 'before_school',
        isWarning: false
      });
      return;
    }

    const elapsed = currentSec - schoolStartInSec;
    const slotIndex = Math.floor(elapsed / slotSec);

    if (slotIndex >= 6) {
      setActiveLessonInfo({
        lessonName: 'انتهى الدوام الرسمي',
        remainingSeconds: 0,
        status: 'after_school',
        isWarning: false
      });
      return;
    }

    const timeInSlot = elapsed % slotSec;

    if (timeInSlot < lessonSec) {
      const rem = lessonSec - timeInSlot;
      const isWarn = rem <= 60;
      
      if (isWarn && rem === 60 && config.enableBellSound) {
        bellAudio.playWarningChime();
      } else if (rem === 1 && config.enableBellSound) {
        bellAudio.playBellRing();
      }

      setActiveLessonInfo({
        lessonName: `الدرس ${slotIndex + 1}`,
        remainingSeconds: rem,
        status: 'in_lesson',
        isWarning: isWarn
      });
    } else {
      const rem = slotSec - timeInSlot;
      const isWarn = rem <= 60;

      if (isWarn && rem === 60 && config.enableBellSound) {
        bellAudio.playWarningChime();
      } else if (rem === 1 && config.enableBellSound) {
        bellAudio.playBellRing();
      }

      setActiveLessonInfo({
        lessonName: `الفرصة ${slotIndex + 1}`,
        remainingSeconds: rem,
        status: 'in_break',
        isWarning: isWarn
      });
    }
  }, [now, config]);

  const formatRemaining = (totalSec: number) => {
    const mins = Math.floor(totalSec / 60);
    const secs = totalSec % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const daysArabic = ['الأحد', 'الإثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة', 'السبت'];
  const dayName = daysArabic[now.getDay()];

  // Navigation Items for Sidebar
  const navMenuItems = [
    { id: 'launcher' as ActiveView, label: 'الشاشة الرئيسية (الرئيسية Home)', icon: Home, color: 'bg-amber-500' },
    { id: 'schedule' as ActiveView, label: 'الجدول الدراسي التفاعلي', icon: CalendarDays, color: 'bg-blue-600' },
    { id: 'students' as ActiveView, label: 'سجل ومتابعة الطلاب', icon: GraduationCap, color: 'bg-emerald-600' },
    { id: 'former_students' as ActiveView, label: 'أرشيف الطلاب السابقين (المغادرين)', icon: UserMinus, color: 'bg-rose-600' },
    { id: 'staff' as ActiveView, label: 'سجل الكادر التدريسي', icon: Users, color: 'bg-purple-600' },
    { id: 'stats' as ActiveView, label: 'الإحصاءات والملاك الرسمي', icon: BarChart3, color: 'bg-orange-600' },
    { id: 'print' as ActiveView, label: 'مركز طباعة الوثائق', icon: Printer, color: 'bg-cyan-600' },
    { id: 'alarm' as ActiveView, label: 'المنبه والجرس المباشر', icon: BellRing, color: 'bg-yellow-600' },
    { id: 'desktop_guide' as ActiveView, label: 'تحويل إلى تطبيق سطح مكتب (.EXE)', icon: Laptop, color: 'bg-indigo-600' },
    { id: 'themes' as ActiveView, label: 'تخصيص ألوان الثيم', icon: Palette, color: 'bg-pink-600' },
    { id: 'fonts' as ActiveView, label: 'تغيير الخطوط العربية', icon: Type, color: 'bg-rose-600' },
    { id: 'settings' as ActiveView, label: 'إعدادات المدرسة والأمان', icon: Settings, color: 'bg-slate-700' },
  ];

  const handleNavClick = (view: ActiveView) => {
    setActiveView(view);
    setIsSidebarOpen(false);
  };

  return (
    <>
      {/* Royal Purple Top Header Bar with Bounded Side Borders & Ornate Frame */}
      <div className="sticky top-0 z-40 w-full px-2 sm:px-4 py-1.5 pointer-events-none">
        <header 
          className="max-w-7xl mx-auto rounded-2xl bg-gradient-to-r from-purple-950 via-indigo-950 to-purple-900 text-white border-4 border-amber-400 relative overflow-hidden pointer-events-auto shadow-2xl"
          style={{
            boxShadow: '0px 8px 0px 0px rgba(15, 23, 42, 0.9), 0px 12px 24px -2px rgba(88, 28, 135, 0.6)'
          }}
        >
          {/* Top Ornate Pattern Background Accent Line */}
          <div className="absolute top-0 inset-x-0 h-1 bg-gradient-to-r from-amber-500 via-amber-300 to-amber-500" />
          <div className="px-4 py-2 flex items-center justify-between gap-4 relative z-10">
            
            {/* Right Section: Hamburger Menu + Dedicated Home Button + School & Manager Info */}
            <div className="flex items-center gap-2 sm:gap-3 shrink-0">
              {/* Hamburger Menu Toggle (3 lines) */}
              <button
                onClick={() => setIsSidebarOpen(true)}
                aria-label="فتح القائمة الجانبية"
                title="القائمة الجانبية للتنقل"
                className="p-2 rounded-xl bg-purple-900/60 hover:bg-purple-800 text-amber-300 border border-amber-400/30 flex items-center justify-center shrink-0 shadow-md cursor-pointer transition-all active:scale-95"
              >
                <Menu className="w-5 h-5" />
              </button>

              {/* Dedicated Home Icon Button - Always Visible on All Pages */}
              <button
                onClick={() => setActiveView('launcher')}
                aria-label="الذهاب إلى الشاشة الرئيسية (Home)"
                title="الذهاب إلى الشاشة الرئيسية (Home)"
                className={`p-2 px-3 rounded-xl flex items-center gap-1.5 font-black text-xs shrink-0 shadow-md cursor-pointer transition-all active:scale-95 border ${
                  activeView === 'launcher' 
                    ? 'bg-amber-400 text-slate-950 border-amber-300 font-black shadow-amber-500/20' 
                    : 'bg-amber-500/20 hover:bg-amber-500/35 text-amber-300 border-amber-400/40 hover:text-white'
                }`}
              >
                <Home className="w-4 h-4 text-amber-300" />
                <span className="hidden xs:inline font-bold">الرئيسية</span>
              </button>

              {/* School Name & Manager Info */}
              <div className="flex flex-col text-right leading-tight">
                <span className="font-black text-xs md:text-sm text-amber-300 drop-shadow-sm">
                  {config.schoolName}
                </span>
                <span className="text-[11px] font-bold text-purple-200/90 mt-0.5">
                  إشراف المدير: <span className="text-white font-black">{config.managerName}</span>
                </span>
              </div>
            </div>

            {/* Center Section: App Brand (The Principal Logo & Title) */}
            <div 
              onClick={() => setActiveView('launcher')}
              className="cursor-pointer hover:opacity-95 transition-opacity px-2 py-0.5 rounded-2xl bg-purple-900/40 border border-amber-400/30 shadow-inner"
            >
              <AppLogo size="md" showText={true} />
            </div>

            {/* Left Section: Live Date & Time Widget */}
            <div className="flex items-center gap-2 shrink-0">
              <div className="hidden sm:flex items-center gap-2 bg-purple-900/60 border border-amber-400/30 px-3.5 py-1.5 rounded-xl text-xs font-black text-amber-200 shadow-inner">
                <Clock className="w-4 h-4 text-amber-300 animate-pulse shrink-0" />
                <span>{dayName}، {now.toLocaleDateString('ar-IQ', { day: 'numeric', month: 'long', year: 'numeric' })}</span>
                <span className="text-purple-400/60">|</span>
                <span className="font-mono dir-ltr text-white text-xs font-black">{now.toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
              </div>

              {/* Lock / Settings Trigger */}
              <button
                onClick={onOpenPasscode}
                title="رمز الحماية والإعدادات"
                className="p-2 rounded-xl bg-amber-500/20 hover:bg-amber-500/35 text-amber-300 transition-colors border border-amber-400/30 shrink-0 cursor-pointer"
              >
                <Lock className="w-4.5 h-4.5" />
              </button>
            </div>

          </div>
        </header>
      </div>

      {/* Side Drawer Overlay (الشريط الجانبي) */}
      {isSidebarOpen && (
        <div className="fixed inset-0 z-50 flex">
          {/* Backdrop Blur */}
          <div 
            className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm transition-opacity animate-fade-in"
            onClick={() => setIsSidebarOpen(false)}
          />

          {/* Slide-out Sidebar Drawer */}
          <div className="relative w-80 max-w-[85vw] bg-slate-900 text-white shadow-2xl flex flex-col h-full border-l border-slate-800 z-10 transform transition-transform duration-300 animate-slide-in-right">
            
            {/* Sidebar Header */}
            <div className="p-4 bg-slate-950/80 border-b border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <AppLogo size="md" showText={true} />
              </div>
              <button
                onClick={() => setIsSidebarOpen(false)}
                className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors"
                aria-label="إغلاق القائمة"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* School Info Header in Sidebar */}
            <div className="p-4 bg-gradient-to-r from-blue-900/40 via-indigo-900/30 to-slate-900 border-b border-slate-800">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-lg bg-amber-500/20 border border-amber-400/30 text-amber-400 flex items-center justify-center shrink-0">
                  <Building2 className="w-5 h-5" />
                </div>
                <div className="overflow-hidden">
                  <h3 className="text-sm font-bold text-white truncate">{config.schoolName}</h3>
                  <p className="text-xs text-amber-300/90 truncate">{config.directorateName}</p>
                </div>
              </div>
            </div>

            {/* Navigation Menu List */}
            <div className="flex-1 overflow-y-auto p-3 space-y-1">
              <div className="px-3 py-1.5 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                صفحات وتطبيقات النظام
              </div>

              {navMenuItems.map((item) => {
                const IconComponent = item.icon;
                const isActive = activeView === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => handleNavClick(item.id)}
                    className={`w-full flex items-center justify-between p-3 rounded-xl text-right transition-all group ${
                      isActive 
                        ? 'bg-blue-600 text-white font-bold shadow-md' 
                        : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div className={`w-8 h-8 rounded-lg ${isActive ? 'bg-white/20' : item.color} text-white flex items-center justify-center shrink-0 shadow-sm`}>
                        <IconComponent className="w-4 h-4" />
                      </div>
                      <span className="text-sm">{item.label}</span>
                    </div>

                    <ChevronLeft className={`w-4 h-4 text-slate-400 group-hover:text-white group-hover:-translate-x-1 transition-transform ${isActive ? 'text-white' : ''}`} />
                  </button>
                );
              })}
            </div>

            {/* Sidebar Footer */}
            <div className="p-4 bg-slate-950/90 border-t border-slate-800 text-xs text-slate-400 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-amber-400" />
                <span>إصدار التطبيق v2.5</span>
              </div>
              <span className="text-slate-500 font-mono">Principal</span>
            </div>

          </div>
        </div>
      )}
    </>
  );
};
