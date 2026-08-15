import React, { useState } from 'react';
import { AppConfig } from '../types';
import { 
  Settings, 
  Lock, 
  UserCheck, 
  Building2, 
  Clock, 
  Image as ImageIcon, 
  RotateCcw, 
  Save, 
  Key, 
  ShieldCheck,
  CheckCircle2,
  Sparkles
} from 'lucide-react';

interface SettingsViewProps {
  config: AppConfig;
  setConfig: React.Dispatch<React.SetStateAction<AppConfig>>;
  onResetData: () => void;
  onTriggerScreensaver: () => void;
  onTriggerSplash: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  config,
  setConfig,
  onResetData,
  onTriggerScreensaver,
  onTriggerSplash
}) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [enteredPasscode, setEnteredPasscode] = useState('');
  const [authError, setAuthError] = useState('');

  const [formConfig, setFormConfig] = useState<AppConfig>({ ...config });

  // Handle Security Login
  const handleAuthSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (enteredPasscode === config.passcode || enteredPasscode === config.developerCode) {
      setIsAuthenticated(true);
      setAuthError('');
    } else {
      setAuthError('رمز الدخول غير صحيح، يرجى كتابة رمز المدير أو المبرمج.');
    }
  };

  // Save Settings
  const handleSaveConfig = (e: React.FormEvent) => {
    e.preventDefault();
    setConfig({ ...formConfig });
    alert('تم حفظ كافة إعدادات النظام وتحديث التوقيتات وبيانات المدير والمدرسة بنجاح!');
  };

  if (!isAuthenticated) {
    return (
      <div className="max-w-md mx-auto px-4 py-16">
        <form onSubmit={handleAuthSubmit} className="bg-[var(--theme-card)] p-6 md:p-8 rounded-3xl border border-[var(--theme-card-border)] shadow-2xl space-y-5 text-center">
          <div className="w-16 h-16 rounded-2xl bg-amber-500/20 text-amber-600 mx-auto flex items-center justify-center">
            <Lock className="w-8 h-8" />
          </div>

          <div>
            <h2 className="text-xl font-extrabold text-[var(--theme-text-main)]">
              منطقة إعدادات الإدارة المحمية
            </h2>
            <p className="text-xs text-[var(--theme-text-muted)] mt-1">
              يرجى إدخال رمز دخول المدير (الافتراضي: 1234) أو رمز المبرمج لتعديل الإعدادات.
            </p>
          </div>

          <div className="space-y-2 text-right">
            <label className="block text-xs font-bold text-[var(--theme-text-main)]">رمز الدخول الأمني:</label>
            <input
              type="password"
              value={enteredPasscode}
              onChange={e => setEnteredPasscode(e.target.value)}
              placeholder="****"
              className="w-full text-center tracking-widest text-xl font-mono p-3.5 rounded-xl border-2 border-amber-400 bg-white text-slate-900 font-black placeholder-slate-400 focus:outline-none focus:border-amber-500 shadow-sm"
            />
            {authError && <span className="text-xs text-rose-600 font-bold block">{authError}</span>}
          </div>

          <button
            type="submit"
            className="w-full py-3 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 font-black text-sm shadow-lg transition-all"
          >
            تأكيد الرمز والدخول للإعدادات
          </button>
        </form>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
      
      {/* Title */}
      <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm flex items-center justify-between">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300 text-xs font-bold mb-2">
            <ShieldCheck className="w-4 h-4" />
            <span>لوحة تحكم إعدادات المدير والمبرمج</span>
          </div>
          <h2 className="text-2xl font-black text-[var(--theme-text-main)]">
            إعدادات النظام، التوقيتات، والرموز الأمنية
          </h2>
        </div>

        <button
          onClick={() => setIsAuthenticated(false)}
          className="px-3 py-1.5 rounded-lg bg-rose-50 text-rose-700 text-xs font-bold hover:bg-rose-100"
        >
          قفل الإعدادات 🔒
        </button>
      </div>

      <form onSubmit={handleSaveConfig} className="space-y-6">
        
        {/* Section 1: School & Manager Details */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <UserCheck className="w-5 h-5 text-amber-500" />
            <span>1. بيانات المدرسة والمدير والمديرية</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">اسم مدير المدرسة المحترم:</label>
              <input
                type="text"
                required
                value={formConfig.managerName}
                onChange={e => setFormConfig(p => ({ ...p, managerName: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 font-bold"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">اسم المدرسة الرسمي:</label>
              <input
                type="text"
                required
                value={formConfig.schoolName}
                onChange={e => setFormConfig(p => ({ ...p, schoolName: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 font-bold"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">اسم مديرية التربية:</label>
              <input
                type="text"
                value={formConfig.directorateName}
                onChange={e => setFormConfig(p => ({ ...p, directorateName: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">نوع ومرحلة المدرسة الرسمية:</label>
              <select
                value={formConfig.schoolStage || 'intermediate'}
                onChange={e => setFormConfig(p => ({ ...p, schoolStage: e.target.value as any }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 font-bold text-amber-600"
              >
                <option value="primary">🏫 مدرسة ابتدائية (من الأول إلى السادس الابتدائي)</option>
                <option value="intermediate">🏫 مدرسة متوسطة (الأول إلى الثالث متوسط)</option>
                <option value="preparatory">🏫 مدرسة إعدادية (الرابع، الخامس، السادس العلمي والأدبي)</option>
                <option value="secondary">🏫 مدرسة ثانوية متكاملة (متوسطة + إعدادية)</option>
              </select>
            </div>

            <div>
              <label className="block font-bold mb-1">القسم أو المنطقة التعليمية:</label>
              <input
                type="text"
                value={formConfig.sectionName}
                onChange={e => setFormConfig(p => ({ ...p, sectionName: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900"
              />
            </div>
          </div>
        </div>

        {/* Section 2: Schedule & Bell Timings */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <Clock className="w-5 h-5 text-blue-500" />
            <span>2. ضبط أوقات جدول الحصص والمنبه</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">مدة الدرس الواحد (بالدقائق):</label>
              <input
                type="number"
                min={20}
                max={90}
                value={formConfig.lessonDurationMinutes}
                onChange={e => setFormConfig(p => ({ ...p, lessonDurationMinutes: Number(e.target.value) }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 font-bold text-blue-600"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">مدة الفرصة الاستراحة (بالدقائق):</label>
              <input
                type="number"
                min={5}
                max={40}
                value={formConfig.breakDurationMinutes}
                onChange={e => setFormConfig(p => ({ ...p, breakDurationMinutes: Number(e.target.value) }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 font-bold text-amber-600"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">ساعة بداية الدوام الرسمي:</label>
              <input
                type="time"
                value={formConfig.schoolStartHour}
                onChange={e => setFormConfig(p => ({ ...p, schoolStartHour: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 font-bold text-emerald-600"
              />
            </div>
          </div>
        </div>

        {/* Section 3: Passcodes */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <Key className="w-5 h-5 text-purple-500" />
            <span>3. تغيير رموز الحماية للدخول (رمز المدير ورمز المبرمج)</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">رمز المدير للدخول للإعدادات:</label>
              <input
                type="text"
                value={formConfig.passcode}
                onChange={e => setFormConfig(p => ({ ...p, passcode: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 font-mono font-bold"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">رمز المبرمج الرئيسي (Developer Code):</label>
              <input
                type="text"
                value={formConfig.developerCode}
                onChange={e => setFormConfig(p => ({ ...p, developerCode: e.target.value }))}
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 font-mono font-bold text-purple-600"
              />
            </div>
          </div>
        </div>

        {/* Section 4: Splash Screen & Screensaver photos */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <ImageIcon className="w-5 h-5 text-teal-500" />
            <span>4. صور واجهة الافتتاح وواجهة السكون</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">رابط صورة واجهة الافتتاح (Splash Image):</label>
              <input
                type="text"
                value={formConfig.splashImageUrl || ''}
                onChange={e => setFormConfig(p => ({ ...p, splashImageUrl: e.target.value }))}
                placeholder="https://..."
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-mono"
              />
              <button
                type="button"
                onClick={onTriggerSplash}
                className="mt-2 text-[11px] text-teal-600 font-bold hover:underline"
              >
                معاينة واجهة الافتتاح الآن 🖼️
              </button>
            </div>

            <div>
              <label className="block font-bold mb-1">رابط صورة واجهة السكون (Screensaver Image):</label>
              <input
                type="text"
                value={formConfig.screensaverImageUrl || ''}
                onChange={e => setFormConfig(p => ({ ...p, screensaverImageUrl: e.target.value }))}
                placeholder="https://..."
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-mono"
              />
              <button
                type="button"
                onClick={onTriggerScreensaver}
                className="mt-2 text-[11px] text-amber-600 font-bold hover:underline"
              >
                تشغيل واجهة السكون والتوقف يدويًا 💤
              </button>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
            <div>
              <span className="block text-xs font-bold text-[var(--theme-text-main)]">
                تفعيل شاشة التوقف التلقائية عند خمول النظام (Screensaver):
              </span>
              <span className="text-[11px] text-[var(--theme-text-muted)]">
                (تم إيقافها تلقائياً لتفادي إزعاج العمل أثناء الاستخدام)
              </span>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={formConfig.enableScreensaver || false}
                onChange={e => setFormConfig(p => ({ ...p, enableScreensaver: e.target.checked }))}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-300 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:right-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-amber-500"></div>
            </label>
          </div>
        </div>

        {/* Section 4.5: Administration Email and Gemini Key */}
        <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-4">
          <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-indigo-500" />
            <span>5. إعدادات حساب إدارة المدرسة والذكاء الاصطناعي</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-bold mb-1">البريد الإلكتروني للادارة (Email):</label>
              <input
                type="email"
                value={formConfig.adminEmail || ''}
                onChange={e => setFormConfig(p => ({ ...p, adminEmail: e.target.value }))}
                placeholder="school.admin@gmail.com"
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-mono font-bold text-indigo-600 dark:text-indigo-400"
              />
            </div>

            <div>
              <label className="block font-bold mb-1">مفتاح ذكاء اصطناعي Gemini الخاص بالإدارة (Gemini API Key):</label>
              <input
                type="text"
                value={formConfig.geminiApiKey || ''}
                onChange={e => setFormConfig(p => {
                  const updated = { ...p, geminiApiKey: e.target.value };
                  localStorage.setItem('gemini_api_key', e.target.value);
                  localStorage.setItem('diyala_school_gemini_key', e.target.value);
                  return updated;
                })}
                placeholder="AIzaSy..."
                className="w-full p-2.5 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-mono font-bold text-emerald-600 dark:text-emerald-400"
              />
              <span className="text-[10px] text-slate-500 block mt-1">
                عند إضافة المفتاح، سيتم تشغيل البحث الذكي وتوليد الإحصائيات مباشرة على جهازك.
              </span>
            </div>
          </div>
        </div>

        {/* Form Action Controls */}
        <div className="flex items-center justify-between pt-4 border-t border-[var(--theme-card-border)]">
          <button
            type="button"
            onClick={onResetData}
            className="px-4 py-2.5 rounded-xl bg-rose-50 text-rose-700 text-xs font-bold hover:bg-rose-100 flex items-center gap-1.5"
          >
            <RotateCcw className="w-4 h-4" />
            <span>إعادة ضبط البيانات الأولية</span>
          </button>

          <button
            type="submit"
            className="px-8 py-3 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 text-sm font-black shadow-lg transition-all flex items-center gap-2 cursor-pointer"
          >
            <Save className="w-5 h-5" />
            <span>حفظ كل الإعدادات</span>
          </button>
        </div>

      </form>

    </div>
  );
};
