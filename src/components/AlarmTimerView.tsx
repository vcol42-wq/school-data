import React, { useState, useEffect } from 'react';
import { AppConfig } from '../types';
import { bellAudio } from '../utils/audio';
import { BellRing, Volume2, Clock, Play, AlertCircle, CheckCircle } from 'lucide-react';

interface AlarmTimerViewProps {
  config: AppConfig;
  setConfig: React.Dispatch<React.SetStateAction<AppConfig>>;
}

export const AlarmTimerView: React.FC<AlarmTimerViewProps> = ({ config, setConfig }) => {
  const [now, setNow] = useState(new Date());

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Compute Active Lesson
  const [startH, startM] = config.schoolStartHour.split(':').map(Number);
  const schoolStartSec = (startH || 8) * 3600 + (startM || 0) * 60;
  const currentSec = now.getHours() * 3600 + now.getMinutes() * 60 + now.getSeconds();

  const lessonSec = config.lessonDurationMinutes * 60;
  const breakSec = config.breakDurationMinutes * 60;
  const slotSec = lessonSec + breakSec;

  const elapsed = currentSec - schoolStartSec;
  const slotIndex = Math.floor(elapsed / slotSec);

  let currentTitle = 'خارج أوقات الدوام';
  let remainingSec = 0;
  let totalDurationSec = lessonSec;
  let isBreak = false;
  let isWarning = false;

  if (currentSec < schoolStartSec) {
    currentTitle = 'قبل بداية الدوام الرسمي';
    remainingSec = schoolStartSec - currentSec;
    totalDurationSec = 3600;
  } else if (slotIndex >= 6) {
    currentTitle = 'انتهى الدوام الرسمي اليوم';
    remainingSec = 0;
  } else {
    const timeInSlot = elapsed % slotSec;
    if (timeInSlot < lessonSec) {
      currentTitle = `الدرس ${slotIndex + 1}`;
      remainingSec = lessonSec - timeInSlot;
      totalDurationSec = lessonSec;
      isWarning = remainingSec <= 60;
    } else {
      currentTitle = `الفرصة ${slotIndex + 1}`;
      remainingSec = slotSec - timeInSlot;
      totalDurationSec = breakSec;
      isBreak = true;
      isWarning = remainingSec <= 60;
    }
  }

  const formatMinSec = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const percent = totalDurationSec > 0 ? Math.max(0, Math.min(100, ((totalDurationSec - remainingSec) / totalDurationSec) * 100)) : 0;

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 space-y-6">
      
      {/* Title */}
      <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm text-center">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300 text-xs font-bold mb-2">
          <BellRing className="w-4 h-4" />
          <span>منبه الجرس وجداول الحصص المباشرة</span>
        </div>
        <h2 className="text-2xl font-black text-[var(--theme-text-main)]">
          شاشة المنبه والعد التنازلي المباشر للدرس والفرصة
        </h2>
        <p className="text-xs text-[var(--theme-text-muted)] mt-1">
          يتغير لون المنبه تلقائيًا عند اقتراب نهاية الدرس بدقيقة واحدة مع إطلاق صوت جرس المدرسة المعتمد.
        </p>
      </div>

      {/* Main Digital Clock & Timer Display Card */}
      <div className={`p-8 rounded-3xl border-4 shadow-2xl transition-all text-center space-y-6 ${
        isWarning 
          ? 'bg-gradient-to-b from-rose-900 via-rose-950 to-slate-950 text-white border-rose-500 animate-pulse' 
          : isBreak 
          ? 'bg-gradient-to-b from-amber-900 via-slate-900 to-slate-950 text-white border-amber-500' 
          : 'bg-gradient-to-b from-blue-900 via-indigo-950 to-slate-950 text-white border-blue-500'
      }`}>
        
        {/* Status Badge */}
        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/10 backdrop-blur-md border border-white/20 text-sm font-extrabold text-amber-300">
          <BellRing className={`w-4 h-4 ${isWarning ? 'animate-bounce text-amber-200' : ''}`} />
          <span>الحالة الحالية: {currentTitle}</span>
        </div>

        {/* Huge Countdown Display */}
        <div className="space-y-1">
          <span className="text-xs font-medium text-slate-300 block">الوقت المتبقي لانتهاء الحصّة / الفرصة:</span>
          <div className="text-6xl md:text-7xl font-mono font-black tracking-widest text-amber-300 dir-ltr drop-shadow-lg">
            {formatMinSec(remainingSec)}
          </div>
        </div>

        {/* Progress Bar */}
        <div className="max-w-md mx-auto space-y-1">
          <div className="h-3 w-full bg-white/20 rounded-full overflow-hidden p-0.5">
            <div 
              className={`h-full rounded-full transition-all duration-1000 ${
                isWarning ? 'bg-rose-400' : isBreak ? 'bg-amber-400' : 'bg-blue-400'
              }`}
              style={{ width: `${percent}%` }}
            ></div>
          </div>
          <div className="flex justify-between text-[10px] text-slate-300 font-mono">
            <span>البداية</span>
            <span>{Math.round(percent)}% مكتمل</span>
            <span>النهاية</span>
          </div>
        </div>

        {/* Sound Controls */}
        <div className="pt-4 border-t border-white/10 flex flex-wrap items-center justify-center gap-4">
          <button
            onClick={() => bellAudio.playBellRing()}
            className="flex items-center gap-2 px-5 py-2.5 rounded-2xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-xs transition-all shadow-lg cursor-pointer"
          >
            <Volume2 className="w-4 h-4" />
            <span>تجربة رنين جرس المدرسة الان</span>
          </button>

          <button
            onClick={() => setConfig(p => ({ ...p, enableBellSound: !p.enableBellSound }))}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-2xl font-bold text-xs transition-all border cursor-pointer ${
              config.enableBellSound 
                ? 'bg-emerald-600/80 text-white border-emerald-400' 
                : 'bg-white/10 text-slate-300 border-white/20'
            }`}
          >
            <span>جرس المدرسة الصوتي: {config.enableBellSound ? 'مفعل 🔔' : 'مكتوم 🔕'}</span>
          </button>
        </div>

      </div>

    </div>
  );
};
