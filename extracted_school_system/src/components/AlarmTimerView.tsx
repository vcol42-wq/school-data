import React, { useState, useEffect } from 'react';
import { AppConfig } from '../types';
import { bellAudio, BellTone } from '../utils/audio';
import { 
  BellRing, 
  Volume2, 
  Clock, 
  Play, 
  AlertCircle, 
  CheckCircle, 
  Sliders, 
  Music, 
  Flame, 
  Save, 
  Timer, 
  Settings2,
  VolumeX,
  Volume1
} from 'lucide-react';

interface AlarmTimerViewProps {
  config: AppConfig;
  setConfig: React.Dispatch<React.SetStateAction<AppConfig>>;
}

export const AlarmTimerView: React.FC<AlarmTimerViewProps> = ({ config, setConfig }) => {
  const [now, setNow] = useState(new Date());
  const [selectedTone, setSelectedTone] = useState<BellTone>('classic_brass');
  const [volume, setVolume] = useState<number>(80);
  const [isRingingLive, setIsRingingLive] = useState(false);
  const [saveFeedback, setSaveFeedback] = useState(false);

  // Form State for Schedule Timings
  const [startHour, setStartHour] = useState(config.schoolStartHour || '08:00');
  const [lessonMinutes, setLessonMinutes] = useState(config.lessonDurationMinutes || 45);
  const [breakMinutes, setBreakMinutes] = useState(config.breakDurationMinutes || 10);
  const [enableBellSound, setEnableBellSound] = useState(config.enableBellSound !== false);

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Compute Active Lesson
  const [startH, startM] = (config.schoolStartHour || '08:00').split(':').map(Number);
  const schoolStartSec = (startH || 8) * 3600 + (startM || 0) * 60;
  const currentSec = now.getHours() * 3600 + now.getMinutes() * 60 + now.getSeconds();

  const lessonSec = (config.lessonDurationMinutes || 45) * 60;
  const breakSec = (config.breakDurationMinutes || 10) * 60;
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

  // Available Tones
  const tonesList: { id: BellTone; name: string; desc: string; icon: string }[] = [
    { id: 'classic_brass', name: 'الجرس النحاسي التقليدي (Classic Brass)', desc: 'رنات نحاسية ثلاثية كلاسيكية مع صدى طبيعي عميق', icon: '🔔' },
    { id: 'electronic_melody', name: 'النغمة اللحنية (Westminster Chime)', desc: 'نغمة موسيقية لحنية هادئة متدرجة الأوتار', icon: '🎵' },
    { id: 'digital_pulse', name: 'النبضات الرقمية الحديثة (Digital Pulse)', desc: 'نغمات إلكترونية عصرية سريعة وتنبيهية واضحة', icon: '⚡' },
    { id: 'gentle_chime', name: 'الرنين الهادئ الناعم (Gentle Chime)', desc: 'أوتار هادئة ومريحة لبيئات التعلم الابتدائية', icon: '✨' },
    { id: 'siren_alert', name: 'نغمة الإنذار القوية (Emergency Alert)', desc: 'صوت إنذار تصاعدي للحالات الطارئة والانصراف الفوري', icon: '🚨' }
  ];

  // Test Tone
  const handleTestTone = (tone: BellTone) => {
    bellAudio.setVolume(volume / 100);
    bellAudio.playTone(tone);
  };

  // Ring Bell Now (Manual trigger)
  const handleManualRing = () => {
    setIsRingingLive(true);
    bellAudio.setVolume(volume / 100);
    bellAudio.playTone(selectedTone);
    setTimeout(() => setIsRingingLive(false), 3000);
  };

  // Save Config
  const handleSaveSettings = (e: React.FormEvent) => {
    e.preventDefault();
    const updated = {
      ...config,
      schoolStartHour: startHour,
      lessonDurationMinutes: Number(lessonMinutes),
      breakDurationMinutes: Number(breakMinutes),
      enableBellSound: enableBellSound
    };
    setConfig(updated);
    localStorage.setItem('diyala_school_config', JSON.stringify(updated));
    setSaveFeedback(true);
    setTimeout(() => setSaveFeedback(false), 3000);
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-6 space-y-6">
      
      {/* Title & Banner */}
      <div className="bg-white p-6 rounded-3xl border-2 border-slate-300 shadow-md flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-amber-100 text-amber-950 border border-amber-300 text-xs font-black mb-2 shadow-xs">
            <BellRing className="w-4 h-4 text-amber-600 animate-bounce" />
            <span>لوحة التحكم في المنبه والجرس المدرسي الذكي</span>
          </div>
          <h2 className="text-xl md:text-2xl font-black text-slate-900">
            إعدادات الجرس المدرسي وضبط النغمات وأوقات الحصص
          </h2>
          <p className="text-xs text-slate-600 font-bold mt-1">
            اختر نغمة الجرس المناسبة لمدرستك، اضبط توقيتات الدوام، واختبر الرنين الفوري بنقرة واحدة.
          </p>
        </div>

        {/* Manual Instant Ring Button */}
        <button
          onClick={handleManualRing}
          disabled={isRingingLive}
          className={`flex items-center gap-2 px-6 py-3.5 rounded-2xl text-xs font-black transition-all shadow-xl cursor-pointer ${
            isRingingLive 
              ? 'bg-rose-600 text-white animate-pulse scale-105' 
              : 'bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 text-slate-950 hover:scale-105'
          }`}
        >
          <BellRing className={`w-5 h-5 ${isRingingLive ? 'animate-spin' : ''}`} />
          <span>{isRingingLive ? '🔔 جاري رن الجرس الآن...' : '🔔 رن الجرس فوراً (يدوياً)'}</span>
        </button>
      </div>

      {/* Grid: 1. Live Countdown Status Card | 2. Sound Settings & Ringtones */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Col 1: Live Status & Countdown Card */}
        <div className="lg:col-span-1 space-y-4">
          <div className={`p-6 rounded-3xl border-3 shadow-xl transition-all text-center space-y-4 ${
            isWarning 
              ? 'bg-gradient-to-b from-rose-900 via-rose-950 to-slate-950 text-white border-rose-500 animate-pulse' 
              : isBreak 
              ? 'bg-gradient-to-b from-amber-900 via-slate-900 to-slate-950 text-white border-amber-500' 
              : 'bg-gradient-to-b from-blue-900 via-indigo-950 to-slate-950 text-white border-blue-500'
          }`}>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/20 text-xs font-black text-amber-300">
              <Clock className="w-3.5 h-3.5" />
              <span>{currentTitle}</span>
            </div>

            <div className="font-mono font-black text-4xl sm:text-5xl tracking-widest text-amber-300 drop-shadow-md">
              {formatMinSec(remainingSec)}
            </div>

            <div className="text-[11px] text-slate-300 font-bold">
              الوقت المتبقي لانتهاء الفعالية الحالية
            </div>

            {/* Progress Bar */}
            <div className="w-full bg-white/20 h-2.5 rounded-full overflow-hidden">
              <div 
                className="bg-amber-400 h-full transition-all duration-1000 rounded-full shadow"
                style={{ width: `${percent}%` }}
              />
            </div>

            <div className="pt-2 border-t border-white/10 flex justify-between text-[10px] text-slate-300 font-mono font-bold">
              <span>الساعة الآن: {now.toLocaleTimeString('ar-IQ')}</span>
              <span>بداية الدوام: {config.schoolStartHour || '08:00'}</span>
            </div>
          </div>

          {/* Volume Control Card */}
          <div className="bg-white p-5 rounded-3xl border-2 border-slate-300 shadow-sm space-y-3">
            <div className="flex items-center justify-between text-xs font-black text-slate-800">
              <span className="flex items-center gap-1.5">
                <Volume2 className="w-4 h-4 text-indigo-600" />
                <span>مستوى صوت الجرس:</span>
              </span>
              <span className="font-mono text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-lg">{volume}%</span>
            </div>

            <input
              type="range"
              min="0"
              max="100"
              value={volume}
              onChange={(e) => {
                const v = Number(e.target.value);
                setVolume(v);
                bellAudio.setVolume(v / 100);
              }}
              className="w-full accent-indigo-600 cursor-pointer"
            />

            <div className="flex justify-between text-[10px] text-slate-400 font-bold">
              <span className="flex items-center gap-1"><VolumeX className="w-3 h-3" /> كتم</span>
              <span className="flex items-center gap-1"><Volume1 className="w-3 h-3" /> متوسط</span>
              <span className="flex items-center gap-1"><Volume2 className="w-3 h-3" /> أقصى صوت</span>
            </div>
          </div>
        </div>

        {/* Col 2 & 3: Tones Selector & Timings Form */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* Section: Select Tone */}
          <div className="bg-white p-6 rounded-3xl border-2 border-slate-300 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <div className="flex items-center gap-2">
                <Music className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-black text-slate-900">
                  اختيار نغمة الجرس المدرسي (Tones Library)
                </h3>
              </div>
              <span className="text-xs text-slate-500 font-bold">اضغط للاستماع والتحديد</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {tonesList.map((t) => {
                const isSelected = selectedTone === t.id;
                return (
                  <div
                    key={t.id}
                    onClick={() => {
                      setSelectedTone(t.id);
                      handleTestTone(t.id);
                    }}
                    className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex flex-col justify-between gap-2 shadow-xs ${
                      isSelected
                        ? 'border-amber-500 bg-amber-50/70 ring-2 ring-amber-400/30'
                        : 'border-slate-200 bg-slate-50/50 hover:bg-slate-100 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2">
                        <span className="text-2xl">{t.icon}</span>
                        <div>
                          <h4 className="text-xs font-black text-slate-900">{t.name}</h4>
                          <p className="text-[10px] text-slate-600 font-bold mt-0.5 leading-snug">{t.desc}</p>
                        </div>
                      </div>
                      {isSelected && (
                        <span className="text-[10px] bg-amber-500 text-slate-950 font-black px-2 py-0.5 rounded-full shrink-0 shadow-xs">
                          المحددة ✓
                        </span>
                      )}
                    </div>

                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleTestTone(t.id);
                      }}
                      className="mt-2 w-full py-1.5 px-3 rounded-xl bg-white hover:bg-indigo-50 border border-slate-300 hover:border-indigo-400 text-slate-800 hover:text-indigo-900 font-black text-[11px] flex items-center justify-center gap-1.5 transition-all shadow-xs cursor-pointer"
                    >
                      <Play className="w-3 h-3 fill-indigo-600 text-indigo-600" />
                      <span>تجربة النغمة الآن 🔊</span>
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Section: School Daily Timings Schedule Form */}
          <form onSubmit={handleSaveSettings} className="bg-white p-6 rounded-3xl border-2 border-slate-300 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <div className="flex items-center gap-2">
                <Settings2 className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-black text-slate-900">
                  ضبط أوقات الحصص والدوام المدرسي
                </h3>
              </div>
              {saveFeedback && (
                <span className="text-xs bg-emerald-100 text-emerald-900 border border-emerald-300 font-black px-3 py-1 rounded-xl animate-fade-in flex items-center gap-1">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                  تم حفظ الإعدادات بنجاح!
                </span>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs font-bold">
              <div>
                <label className="block text-slate-700 font-black mb-1.5">وقت بداية الدوام الرسمي:</label>
                <input
                  type="time"
                  value={startHour}
                  onChange={(e) => setStartHour(e.target.value)}
                  className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-900 font-mono font-black text-sm focus:border-indigo-600 focus:outline-none shadow-sm"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-black mb-1.5">مدة الحصة الدراسية (بالدقائق):</label>
                <input
                  type="number"
                  min="20"
                  max="90"
                  value={lessonMinutes}
                  onChange={(e) => setLessonMinutes(Number(e.target.value))}
                  className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-900 font-mono font-black text-sm focus:border-indigo-600 focus:outline-none shadow-sm"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-black mb-1.5">مدة الاستراحة / الفرصة (بالدقائق):</label>
                <input
                  type="number"
                  min="5"
                  max="60"
                  value={breakMinutes}
                  onChange={(e) => setBreakMinutes(Number(e.target.value))}
                  className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-900 font-mono font-black text-sm focus:border-indigo-600 focus:outline-none shadow-sm"
                />
              </div>
            </div>

            {/* Checkbox Options */}
            <div className="pt-3 border-t border-slate-200 flex flex-wrap items-center justify-between gap-4">
              <label className="flex items-center gap-2 cursor-pointer text-xs font-black text-slate-800">
                <input
                  type="checkbox"
                  checked={enableBellSound}
                  onChange={(e) => setEnableBellSound(e.target.checked)}
                  className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500"
                />
                <span>تفعيل الرنين التلقائي للجرس عند بداية ونهاية كل حصة</span>
              </label>

              <button
                type="submit"
                className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs shadow-md transition-all flex items-center gap-1.5 cursor-pointer"
              >
                <Save className="w-4 h-4" />
                <span>حفظ وضبط الميقاتية</span>
              </button>
            </div>
          </form>

        </div>

      </div>

    </div>
  );
};
