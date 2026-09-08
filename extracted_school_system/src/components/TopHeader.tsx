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
  Cloud,
  ClipboardList,
  QrCode,
  Copy,
  Check
} from 'lucide-react';
import { QrCodeSvg } from './QrCodeSvg';
import { getSupabaseUrl, getSupabaseKey } from '../utils/supabaseClient';

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
  const [showPairingModal, setShowPairingModal] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);
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
    { id: 'launcher' as ActiveView, label: 'الشاشة الرئيسية (Home)', icon: Home, color: 'bg-amber-500' },
    { id: 'schedule' as ActiveView, label: '1. جدول الحصص والتوقيتات', icon: CalendarDays, color: 'bg-blue-600' },
    { id: 'students' as ActiveView, label: '2. الطلاب المستمرون والشعب', icon: GraduationCap, color: 'bg-emerald-600' },
    { id: 'student_grades' as ActiveView, label: '3. سجل الدرجات والتقييمات', icon: ClipboardList, color: 'bg-orange-600' },
    { id: 'attendance' as ActiveView, label: '4. سجل ومتابعة الغيابات', icon: UserCheck, color: 'bg-rose-600' },
    { id: 'staff' as ActiveView, label: '5. سجل وتوزيع الكادر', icon: Users, color: 'bg-purple-600' },
    { id: 'stats' as ActiveView, label: '6. الإحصاء والملاك الرسمي', icon: BarChart3, color: 'bg-amber-600' },
    { id: 'former_students' as ActiveView, label: '7. أرشيف الطلاب السابقين', icon: UserMinus, color: 'bg-rose-600' },
    { id: 'print' as ActiveView, label: '8. مركز الطباعة والوثائق الرسمية', icon: Printer, color: 'bg-cyan-600' },
    { id: 'alarm' as ActiveView, label: '9. المنبه والجرس الذكي', icon: BellRing, color: 'bg-yellow-600' },
    { id: 'themes' as ActiveView, label: '10. الثيمات والمظهر العام', icon: Palette, color: 'bg-pink-600' },
    { id: 'settings' as ActiveView, label: '11. إعدادات المدرسة وإدارة السنة', icon: Settings, color: 'bg-slate-700' },
    { id: 'sync_center' as ActiveView, label: '12. مركز المزامنة والربط السحابي (QR)', icon: Cloud, color: 'bg-indigo-600' },
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
          className="theme-shell-header max-w-7xl mx-auto rounded-2xl bg-white text-slate-900 border-4 theme-accent-border relative overflow-hidden pointer-events-auto shadow-2xl transition-colors duration-300"
          style={{
            boxShadow: '0px 8px 0px 0px rgba(15, 23, 42, 0.1), 0px 12px 24px -2px rgba(88, 28, 135, 0.1)'
          }}
        >
          {/* Top Ornate Pattern Background Accent Line */}
          <div className="absolute top-0 inset-x-0 h-1 bg-gradient-to-r from-indigo-500 via-indigo-300 to-indigo-500" />
          <div className="px-4 py-2 flex items-center justify-between gap-4 relative z-10">
            
            {/* Right Section: Hamburger Menu + Dedicated Home Button + School & Manager Info */}
            <div className="flex items-center gap-2 sm:gap-3 shrink-0">
              {/* Hamburger Menu Toggle (3 lines) */}
              <button
                onClick={() => setIsSidebarOpen(true)}
                aria-label="فتح القائمة الجانبية"
                title="القائمة الجانبية للتنقل"
                className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-indigo-600 border border-slate-200 flex items-center justify-center shrink-0 shadow-md cursor-pointer transition-all active:scale-95"
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
                    ? 'bg-indigo-600 text-white border-indigo-500 font-black shadow-indigo-500/20'
                    : 'bg-indigo-50 hover:bg-indigo-100 text-indigo-600 border-indigo-200 hover:text-indigo-800'
                }`}
              >
                <Home className="w-4 h-4 text-indigo-600" />
                <span className="hidden xs:inline font-bold">الرئيسية</span>
              </button>

              {/* School Name & Manager Info */}
              <div className="header-school-identity flex flex-col text-right leading-tight">
                <span className="font-black text-xs md:text-sm text-indigo-900 drop-shadow-sm">
                  {config.schoolName}
                </span>
                <span className="text-[11px] font-bold text-slate-500 mt-0.5">
                  إشراف المدير: <span className="text-slate-900 font-black">{config.managerName}</span>
                </span>
              </div>
            </div>

            {/* Center Section: App Brand (The Principal Logo & Title) */}
            <div 
              onClick={() => setActiveView('launcher')}
              className="header-brand cursor-pointer hover:opacity-95 transition-opacity px-2 py-0.5 rounded-2xl bg-slate-50 border border-slate-200 shadow-inner"
            >
              <AppLogo size="md" showText={true} />
            </div>

            {/* Left Section: Live Date & Time Widget */}
            <div className="flex items-center gap-2 shrink-0">
              <div className="header-clock hidden sm:flex items-center gap-2 bg-slate-50 border border-slate-200 px-3.5 py-1.5 rounded-xl text-xs font-black text-slate-700 shadow-inner">
                <Clock className="w-4 h-4 text-indigo-600 animate-pulse shrink-0" />
                <span>{dayName}، {now.toLocaleDateString('ar-IQ', { day: 'numeric', month: 'long', year: 'numeric' })}</span>
                <span className="text-slate-300">|</span>
                <span className="font-mono dir-ltr text-slate-900 text-xs font-black">{now.toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
              </div>
              <div className={`header-lesson-status hidden md:flex items-center gap-2 px-3 py-1.5 rounded-xl border text-[10px] font-black ${activeLessonInfo.isWarning ? 'header-lesson-warning' : ''}`}>
                <BellRing className="w-3.5 h-3.5" />
                <span>{activeLessonInfo.lessonName}</span>
                <span className="font-mono dir-ltr">{formatRemaining(activeLessonInfo.remainingSeconds)}</span>
              </div>

              {/* Quick Mobile Pairing Button with Generated Code Display */}
              <button
                onClick={() => setShowPairingModal(true)}
                title={`رمز الاقتران المولد: ${config.pairingCode || '112233'} - انقر لعرض الباركود QR`}
                className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white font-black text-xs transition-all border border-emerald-400/50 shrink-0 cursor-pointer shadow-md flex items-center gap-1.5 active:scale-95"
              >
                <QrCode className="w-4 h-4 text-amber-300" />
                <span className="hidden sm:inline">كود الاقتران:</span>
                <span className="font-mono bg-emerald-950/60 px-2 py-0.5 rounded-md text-amber-300 font-black tracking-widest text-[11px] border border-amber-400/30">
                  {config.pairingCode || '112233'}
                </span>
              </button>

              {/* Quick AI Assistant Access Button */}
              <button
                onClick={onOpenVoiceModal}
                title="مساعد الذكاء الاصطناعي (AI Assistant)"
                className="p-2 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 transition-all border-2 border-white shrink-0 cursor-pointer shadow-lg animate-pulse"
              >
                <Sparkles className="w-4.5 h-4.5" />
              </button>

              {/* Lock / Settings Trigger */}
              <button
                onClick={onOpenPasscode}
                title="رمز الحماية والإعدادات"
                className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 transition-colors border border-slate-200 shrink-0 cursor-pointer"
              >
                <Lock className="w-4.5 h-4.5" />
              </button>
            </div>

          </div>
        </header>
      </div>

      {/* Quick QR & Simple Code Pairing Modal */}
      {showPairingModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 dir-rtl">
          <div className="bg-white border-4 border-indigo-600 rounded-[2.5rem] p-8 max-w-md w-full shadow-2xl relative text-right flex flex-col items-center">
            <button 
              onClick={() => setShowPairingModal(false)}
              className="absolute left-6 top-6 p-2 rounded-full bg-slate-100 text-slate-500 hover:text-slate-900 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="p-3 bg-emerald-100 text-emerald-800 rounded-2xl mb-4">
              <Smartphone className="w-8 h-8" />
            </div>

            <h3 className="text-xl font-black text-slate-900 mb-1">الربط السريع للتطبيقات</h3>
            <p className="text-xs text-slate-500 mb-6 text-center font-bold">
              افتح تطبيق المعلم، الطالب، أو المدير وامسح هذا الباركود أو ادخل الرمز البسيط:
            </p>

            {/* Giant QR Code */}
            <div className="bg-white p-4 rounded-3xl shadow-xl border-4 border-amber-400 mb-6">
              <QrCodeSvg
                value={JSON.stringify({
                  url: getSupabaseUrl(),
                  apiKey: getSupabaseKey(),
                  schoolId: config.schoolId || 'SCH-MAIN-001',
                  pairingCode: config.pairingCode || '112233',
                  schoolName: config.schoolName || 'المدرسة النموذجية'
                })}
                size={180}
              />
            </div>

            {/* 6-Digit Simple Pairing Code */}
            <div className="w-full bg-slate-50 border-2 border-dashed border-indigo-300 rounded-2xl p-4 flex items-center justify-between mb-4">
              <div>
                <span className="text-[10px] font-black text-slate-400 block uppercase">الرمز السري الموحد للمدرسة:</span>
                <span className="text-3xl font-black text-indigo-700 tracking-widest">{config.pairingCode || '112233'}</span>
              </div>
              <button
                onClick={() => {
                  navigator.clipboard.writeText(config.pairingCode || '112233');
                  setCopiedCode(true);
                  setTimeout(() => setCopiedCode(false), 2000);
                }}
                className="p-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold flex items-center gap-1 cursor-pointer transition-all shadow-md"
              >
                {copiedCode ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                <span>{copiedCode ? 'تم النسخ' : 'نسخ الرمز'}</span>
              </button>
            </div>

            <p className="text-[11px] text-emerald-700 font-bold bg-emerald-50 px-4 py-2 rounded-xl border border-emerald-200 text-center w-full mb-4">
              ⚡ تم تفعيل الاتصال الفوري المباشر مع سحابة المدرسة لجميع الهواتف!
            </p>

            {/* Clear Close / Back Button */}
            <button
              onClick={() => setShowPairingModal(false)}
              className="w-full py-3 rounded-2xl bg-slate-900 hover:bg-slate-800 text-white font-black text-sm transition-all shadow-lg cursor-pointer flex items-center justify-center gap-2"
            >
              <span>إغلاق والعودة للرئيسية ✕</span>
            </button>
          </div>
        </div>
      )}

      {/* Side Drawer Overlay (الشريط الجانبي) */}
      {isSidebarOpen && (
        <div className="fixed inset-0 z-50 flex">
          {/* Backdrop Blur */}
          <div 
            className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm transition-opacity animate-fade-in"
            onClick={() => setIsSidebarOpen(false)}
          />

          {/* Slide-out Sidebar Drawer */}
          <div className="relative w-80 max-w-[85vw] bg-white text-slate-900 shadow-2xl flex flex-col h-full border-l border-slate-200 z-10 transform transition-transform duration-300 animate-slide-in-right">
            
            {/* Sidebar Header */}
            <div className="p-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <AppLogo size="md" showText={true} />
              </div>
              <button
                onClick={() => setIsSidebarOpen(false)}
                className="p-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-900 transition-colors"
                aria-label="إغلاق القائمة"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* School Info Header in Sidebar */}
            <div className="p-4 bg-indigo-50 border-b border-indigo-100">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-lg bg-indigo-600 text-white flex items-center justify-center shrink-0">
                  <Building2 className="w-5 h-5" />
                </div>
                <div className="overflow-hidden">
                  <h3 className="text-sm font-bold text-slate-900 truncate">{config.schoolName}</h3>
                  <p className="text-xs text-indigo-600 truncate">{config.directorateName}</p>
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
                        ? 'bg-indigo-600 text-white font-bold shadow-md'
                        : 'text-slate-700 hover:bg-slate-100 hover:text-slate-900'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div className={`w-8 h-8 rounded-lg ${isActive ? 'bg-white/20' : item.color} text-white flex items-center justify-center shrink-0 shadow-sm`}>
                        <IconComponent className="w-4 h-4" />
                      </div>
                      <span className="text-sm">{item.label}</span>
                    </div>

                    <ChevronLeft className={`w-4 h-4 text-slate-400 group-hover:text-slate-900 group-hover:-translate-x-1 transition-transform ${isActive ? 'text-white' : ''}`} />
                  </button>
                );
              })}
            </div>

            {/* Sidebar Footer */}
            <div className="p-4 bg-slate-50 border-t border-slate-200 text-xs text-slate-500 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-indigo-500" />
                <span>إصدار التطبيق v2.5</span>
              </div>
              <span className="text-slate-400 font-mono">Principal</span>
            </div>

          </div>
        </div>
      )}
    </>
  );
};
