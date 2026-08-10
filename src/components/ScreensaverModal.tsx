import React, { useState, useEffect } from 'react';
import { AppConfig } from '../types';
import { Lock, Clock, Sparkles, BellRing, ArrowLeft, Volume2, ShieldCheck, User, Calendar, BookOpen, Layers } from 'lucide-react';
import { AppLogo } from './AppLogo';

interface ScreensaverModalProps {
  config: AppConfig;
  onUnlock: () => void;
}

// HD Scenic Nature Background Photos (Unsplash curated nature collection)
const NATURE_BACKGROUNDS = [
  'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1920&q=80', // Mountain valley lake
  'https://images.unsplash.com/photo-1511497584788-876761c144ee?auto=format&fit=crop&w=1920&q=80', // Pine forest mist
  'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=1920&q=80', // Green mountain sunset
  'https://images.unsplash.com/photo-1426604966848-d7adac402bff?auto=format&fit=crop&w=1920&q=80', // Waterfall landscape
  'https://images.unsplash.com/photo-1472214103451-9374bd1c798e?auto=format&fit=crop&w=1920&q=80', // Peaceful autumn hills
];

export const ScreensaverModal: React.FC<ScreensaverModalProps> = ({ config, onUnlock }) => {
  const [now, setNow] = useState(new Date());
  const [bgIndex, setBgIndex] = useState(0);
  const [hasPlayedChime, setHasPlayedChime] = useState(false);

  // Live clock tick
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Background image rotation every 15 seconds
  useEffect(() => {
    const bgTimer = setInterval(() => {
      setBgIndex(prev => (prev + 1) % NATURE_BACKGROUNDS.length);
    }, 15000);
    return () => clearInterval(bgTimer);
  }, []);

  // Keyboard / Touch listener to unlock screensaver smoothly without affecting state
  useEffect(() => {
    const handleKey = () => onUnlock();
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [onUnlock]);

  // Helper: Play gentle Web Audio chime sound when lesson is ending
  const playAlertChime = () => {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      
      const playTone = (freq: number, start: number, duration: number) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, ctx.currentTime + start);
        gain.gain.setValueAtTime(0.2, ctx.currentTime + start);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + start + duration);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(ctx.currentTime + start);
        osc.stop(ctx.currentTime + start + duration);
      };

      playTone(523.25, 0, 0.6); // C5
      playTone(659.25, 0.3, 0.8); // E5
      playTone(783.99, 0.6, 1.2); // G5
    } catch (e) {
      console.log('Audio Context restricted:', e);
    }
  };

  // Calculate Lesson Timetable & Current Remaining Time
  const calculateLessonSchedule = () => {
    const startHourParts = (config.schoolStartHour || '08:00').split(':');
    const startHour = parseInt(startHourParts[0] || '8', 10);
    const startMinute = parseInt(startHourParts[1] || '0', 10);

    const lessonMinutes = config.lessonDurationMinutes || 45;
    const breakMinutes = config.breakDurationMinutes || 10;

    const todayStart = new Date(now);
    todayStart.setHours(startHour, startMinute, 0, 0);

    const lessonNames = ['الدرس الأول', 'الدرس الثاني', 'الدرس الثالث', 'الدرس الرابع', 'الدرس الخامس', 'الدرس السادس'];

    let cursor = new Date(todayStart);

    for (let i = 0; i < lessonNames.length; i++) {
      const lessonStart = new Date(cursor);
      const lessonEnd = new Date(lessonStart.getTime() + lessonMinutes * 60 * 1000);

      // Check if current time is inside this lesson
      if (now >= lessonStart && now < lessonEnd) {
        const diffMs = lessonEnd.getTime() - now.getTime();
        const remainingMin = Math.floor(diffMs / (1000 * 60));
        const remainingSec = Math.floor((diffMs % (1000 * 60)) / 1000);
        
        const nextLessonName = i + 1 < lessonNames.length ? lessonNames[i + 1] : 'انتهاء الدوام الرسمي';
        const nextLessonStartStr = i + 1 < lessonNames.length 
          ? new Date(lessonEnd.getTime() + breakMinutes * 60 * 1000).toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit' })
          : 'الساعة 01:30 ظهراً';

        return {
          type: 'lesson' as const,
          title: lessonNames[i],
          remainingMin,
          remainingSec,
          remainingTotalSec: Math.floor(diffMs / 1000),
          endTimeStr: lessonEnd.toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit' }),
          nextTitle: nextLessonName,
          nextStartStr: nextLessonStartStr,
          isEndingSoon: remainingMin < 5
        };
      }

      // Move cursor to break after lesson
      cursor = new Date(lessonEnd);
      const breakEnd = new Date(cursor.getTime() + breakMinutes * 60 * 1000);

      if (now >= lessonEnd && now < breakEnd) {
        const diffMs = breakEnd.getTime() - now.getTime();
        const remainingMin = Math.floor(diffMs / (1000 * 60));
        const remainingSec = Math.floor((diffMs % (1000 * 60)) / 1000);

        const nextLessonName = i + 1 < lessonNames.length ? lessonNames[i + 1] : 'انتهاء الدوام';
        const nextStartStr = breakEnd.toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit' });

        return {
          type: 'break' as const,
          title: `الفرصة المدرسية (${i + 1})`,
          remainingMin,
          remainingSec,
          remainingTotalSec: Math.floor(diffMs / 1000),
          endTimeStr: breakEnd.toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit' }),
          nextTitle: nextLessonName,
          nextStartStr,
          isEndingSoon: remainingMin < 2
        };
      }

      cursor = new Date(breakEnd);
    }

    // If outside official hours
    if (now < todayStart) {
      return {
        type: 'before_school' as const,
        title: 'قبل بداية الدوام الرسمي',
        remainingMin: 0,
        remainingSec: 0,
        remainingTotalSec: 0,
        endTimeStr: '--:--',
        nextTitle: 'الدرس الأول',
        nextStartStr: todayStart.toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit' }),
        isEndingSoon: false
      };
    }

    return {
      type: 'after_school' as const,
      title: 'انتهى الدوام المدرسي اليومي',
      remainingMin: 0,
      remainingSec: 0,
      remainingTotalSec: 0,
      endTimeStr: '--:--',
      nextTitle: 'الدوام القادم (8:00 صباحاً)',
      nextStartStr: 'غداً صباحاً',
      isEndingSoon: false
    };
  };

  const scheduleInfo = calculateLessonSchedule();

  // Trigger alert sound once when entering warning state (< 3 minutes)
  useEffect(() => {
    if (scheduleInfo.isEndingSoon && !hasPlayedChime && config.enableBellSound) {
      playAlertChime();
      setHasPlayedChime(true);
    }
  }, [scheduleInfo.isEndingSoon, hasPlayedChime, config.enableBellSound]);

  return (
    <div 
      onClick={onUnlock}
      className="fixed inset-0 z-50 bg-slate-950 text-white flex flex-col items-center justify-between p-4 md:p-8 cursor-pointer select-none overflow-hidden transition-all duration-700"
    >
      {/* Dynamic Rotating Nature Background Image */}
      {NATURE_BACKGROUNDS.map((bgUrl, idx) => (
        <div 
          key={bgUrl}
          className={`absolute inset-0 bg-cover bg-center transition-opacity duration-1000 ease-in-out pointer-events-none scale-105 filter brightness-50 contrast-110 ${
            idx === bgIndex ? 'opacity-100' : 'opacity-0'
          }`}
          style={{ backgroundImage: `url(${bgUrl})` }}
        ></div>
      ))}

      {/* Dark Gradient Overlay */}
      <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/70 to-slate-950/80 pointer-events-none"></div>

      {/* TOP HEADER BAR */}
      <div className="relative z-10 w-full max-w-6xl flex items-center justify-between text-xs md:text-sm font-bold text-slate-200 bg-white/10 backdrop-blur-md px-6 py-3 rounded-2xl border border-white/15 shadow-xl">
        <div className="flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-amber-400 animate-pulse" />
          <span>{config.directorateName}</span>
        </div>

        <div className="flex items-center gap-3">
          <AppLogo size="sm" showText={false} />
          <span className="font-black text-white text-sm hidden sm:inline">{config.schoolName}</span>
        </div>

        <div className="flex items-center gap-1.5 text-amber-300 font-mono text-xs">
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          <span>النظام نشط</span>
        </div>
      </div>

      {/* CENTER IMPRESSIVE DISPLAY CARD */}
      <div className="relative z-10 my-auto w-full max-w-3xl text-center space-y-6">
        
        {/* Main Card */}
        <div className={`p-8 rounded-3xl backdrop-blur-2xl border shadow-2xl space-y-6 transition-all duration-500 ${
          scheduleInfo.isEndingSoon 
            ? 'bg-gradient-to-b from-amber-950/80 via-slate-950/90 to-rose-950/80 border-amber-500/60 shadow-amber-500/20 ring-4 ring-amber-500/30' 
            : 'bg-slate-900/80 border-white/20 shadow-black/60'
        }`}>

          {/* School Name & Manager Header */}
          <div className="space-y-2 border-b border-white/10 pb-5">
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-amber-400/20 text-amber-300 border border-amber-400/30 font-bold text-xs">
              <User className="w-3.5 h-3.5" />
              <span>إدارة السيد المدير: {config.managerName}</span>
            </div>

            <h1 className="text-3xl md:text-5xl font-black text-white tracking-tight drop-shadow-lg">
              {config.schoolName}
            </h1>
            <p className="text-xs text-slate-300 font-medium">{config.sectionName}</p>
          </div>

          {/* Clock & Full Date */}
          <div className="space-y-2 py-2">
            <div className="text-6xl md:text-8xl font-black font-mono tracking-wider text-amber-300 dir-ltr drop-shadow-2xl font-sans">
              {now.toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
            </div>

            <div className="flex items-center justify-center gap-2 text-sm md:text-base font-bold text-slate-200">
              <Calendar className="w-4 h-4 text-amber-400" />
              <span>{now.toLocaleDateString('ar-IQ', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</span>
            </div>
          </div>

          {/* LESSON COUNTDOWN & STATUS SECTION */}
          <div className="pt-2">
            <div className={`p-5 rounded-2xl border transition-all ${
              scheduleInfo.isEndingSoon 
                ? 'bg-amber-500/20 border-amber-400/50 text-amber-100 animate-pulse' 
                : 'bg-white/10 border-white/15 text-white'
            }`}>
              
              <div className="flex flex-col md:flex-row items-center justify-between gap-4">
                
                {/* Current Lesson Title */}
                <div className="text-right space-y-1">
                  <div className="flex items-center gap-2">
                    {scheduleInfo.isEndingSoon ? (
                      <BellRing className="w-5 h-5 text-amber-400 animate-bounce" />
                    ) : (
                      <BookOpen className="w-5 h-5 text-amber-400" />
                    )}
                    <span className="text-xs font-bold text-amber-300">الوضع الحالي بالجدول:</span>
                  </div>
                  <h3 className="text-lg md:text-xl font-black text-white">
                    {scheduleInfo.title}
                  </h3>
                  {scheduleInfo.type !== 'after_school' && scheduleInfo.type !== 'before_school' && (
                    <p className="text-[11px] text-slate-300">ينتهي الموعد في الساعة: {scheduleInfo.endTimeStr}</p>
                  )}
                </div>

                {/* Countdown Timer Badge */}
                {scheduleInfo.type !== 'after_school' && scheduleInfo.type !== 'before_school' && (
                  <div className="bg-slate-950/80 px-6 py-3 rounded-2xl border border-amber-400/30 text-center min-w-[180px]">
                    <span className="block text-[10px] font-bold text-amber-300/80 mb-0.5">الزمن المتبقي:</span>
                    <span className="text-2xl md:text-3xl font-black font-mono text-amber-400 dir-ltr">
                      {String(scheduleInfo.remainingMin).padStart(2, '0')}:{String(scheduleInfo.remainingSec).padStart(2, '0')}
                    </span>
                  </div>
                )}

                {/* Next Lesson Info */}
                <div className="text-left md:text-left space-y-1 border-t md:border-t-0 md:border-r border-white/15 pt-3 md:pt-0 md:pr-6">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-amber-300/90 justify-end md:justify-start">
                    <Layers className="w-4 h-4 text-amber-400" />
                    <span>الدرس التالي:</span>
                  </div>
                  <p className="text-sm font-black text-white">{scheduleInfo.nextTitle}</p>
                  <p className="text-[11px] text-slate-300">موعد البدء: {scheduleInfo.nextStartStr}</p>
                </div>

              </div>

              {/* Alert Warning Text Banner */}
              {scheduleInfo.isEndingSoon && (
                <div className="mt-3 pt-2 border-t border-amber-400/30 text-xs font-black text-amber-200 flex items-center justify-center gap-2">
                  <BellRing className="w-4 h-4 text-amber-300 animate-spin" />
                  <span>تنبيه: اقترب انتهاء الدرس الحالي! يرجى الاستعداد للدرس التالي ({scheduleInfo.nextTitle})</span>
                </div>
              )}

            </div>
          </div>

        </div>

      </div>

      {/* FOOTER UNLOCK HINT */}
      <div className="relative z-10 flex items-center gap-2 text-xs md:text-sm font-bold text-slate-200 bg-white/15 px-6 py-3 rounded-full border border-white/20 backdrop-blur-xl shadow-xl hover:bg-white/25 transition-all">
        <Lock className="w-4 h-4 text-amber-400" />
        <span>انقر بالماوس أو انقر الشاشة لمتابعة العمل من المكان الحالي</span>
      </div>

    </div>
  );
};
