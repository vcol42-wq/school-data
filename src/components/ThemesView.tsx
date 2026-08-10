import React from 'react';
import { AppTheme } from '../types';
import { Palette, Check, Sparkles } from 'lucide-react';

interface ThemesViewProps {
  currentTheme: AppTheme;
  setTheme: (theme: AppTheme) => void;
}

export const ThemesView: React.FC<ThemesViewProps> = ({ currentTheme, setTheme }) => {
  const themesList: {
    id: AppTheme;
    name: string;
    description: string;
    colors: string[];
    headerColor: string;
    bgStyle: string;
  }[] = [
    {
      id: 'vibrant',
      name: 'الثيم الحيوي الملون (Vibrant Palette)',
      description: 'ثيم أزرق مشرق وحيوي (#2563EB) مع تباين عالي وبطاقات بيضاء ناصعة ولمسات ملونة مبهجة.',
      colors: ['#2563eb', '#f97316', '#ffffff', '#111827'],
      headerColor: 'bg-blue-600',
      bgStyle: 'bg-gray-100'
    },
    {
      id: 'classic',
      name: 'الكلاسيكي الحكومي (الرسمي الملكي)',
      description: 'ثيم أزرق ملكي متزن مع لمسات ذهبية يعكس الطابع الإداري الوزاري الرسمي.',
      colors: ['#1e3a8a', '#b45309', '#ffffff', '#0f172a'],
      headerColor: 'bg-blue-900',
      bgStyle: 'bg-slate-100'
    },
    {
      id: 'diyala',
      name: 'الأزرق الحديث (ديالى التقني)',
      description: 'ثيم سماوي تقني مريح للعين مع تباين عالي لقراءة الجداول والملاكات بسهولة.',
      colors: ['#0284c7', '#0369a1', '#e0f2fe', '#0c4a6e'],
      headerColor: 'bg-sky-700',
      bgStyle: 'bg-sky-50'
    },
    {
      id: 'emerald',
      name: 'الزمردي التعليمي (الأكاديمي الراقي)',
      description: 'ثيم زمردي دافئ مستوحى من بيئات التعلم والتميز الأكاديمي.',
      colors: ['#059669', '#d97706', '#f0fdf4', '#064e3b'],
      headerColor: 'bg-emerald-800',
      bgStyle: 'bg-emerald-50'
    },
    {
      id: 'dark',
      name: 'الداكن التنفيذي (Dark Luxury Executive)',
      description: 'ثيم دافور داكن فاخر مريح للرؤية الليلية ويمنع إجهاد العين أثناء العمل الطويل.',
      colors: ['#3b82f6', '#f59e0b', '#1e293b', '#0f172a'],
      headerColor: 'bg-slate-950',
      bgStyle: 'bg-slate-900'
    },
    {
      id: 'burgundy',
      name: 'الدافئ الأكاديمي (العنابي الكلاسيكي)',
      description: 'ثيم عنابي ملكي فاخر يمنح الواجهة هيبة الجامعات والمؤسسات التعليمية العريقة.',
      colors: ['#9f1239', '#d97706', '#fff1f2', '#4c0519'],
      headerColor: 'bg-rose-950',
      bgStyle: 'bg-rose-50'
    }
  ];

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
      
      <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-50 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300 text-xs font-bold mb-2">
          <Palette className="w-4 h-4" />
          <span>تخصيص الثيم والألوان الشاملة</span>
        </div>
        <h2 className="text-2xl font-black text-[var(--theme-text-main)]">
          اختيار الثيم والنمط البصري للنظام
        </h2>
        <p className="text-xs text-[var(--theme-text-muted)] mt-1">
          عند اختيار أي ثيم سيتم تطبيقه فوراً على كافة أجزاء وقوائم وأشرطة البرنامج بشكل ملحوظ وحقيقي.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {themesList.map((t) => {
          const isSelected = currentTheme === t.id;
          return (
            <div
              key={t.id}
              onClick={() => setTheme(t.id)}
              className={`group cursor-pointer rounded-2xl p-5 border-2 transition-all duration-300 relative bg-[var(--theme-card)] shadow-md hover:shadow-xl transform hover:-translate-y-1 ${
                isSelected
                  ? 'border-amber-500 ring-4 ring-amber-500/20'
                  : 'border-[var(--theme-card-border)] hover:border-amber-400'
              }`}
            >
              {isSelected && (
                <div className="absolute top-3 left-3 bg-amber-500 text-slate-950 p-1 rounded-full shadow">
                  <Check className="w-4 h-4" />
                </div>
              )}

              {/* Theme Mock Palette Preview */}
              <div className={`h-24 rounded-xl ${t.bgStyle} border p-3 flex flex-col justify-between mb-4 overflow-hidden relative shadow-inner`}>
                <div className={`h-6 rounded-lg ${t.headerColor} w-full flex items-center px-2 justify-between text-[10px] text-white font-bold`}>
                  <span>شريط التطبيق</span>
                  <div className="w-2 h-2 rounded-full bg-amber-400"></div>
                </div>

                <div className="flex gap-2">
                  {t.colors.map((c, i) => (
                    <div key={i} className="w-6 h-6 rounded-full border border-black/10 shadow-sm" style={{ backgroundColor: c }}></div>
                  ))}
                </div>
              </div>

              <h3 className="text-base font-black text-slate-900 dark:text-amber-300 group-hover:text-amber-500 transition-colors">
                {t.name}
              </h3>
              <p className="text-xs font-bold text-slate-700 dark:text-slate-200 mt-1.5 leading-relaxed">
                {t.description}
              </p>

              <button
                className={`mt-4 w-full py-2 rounded-xl text-xs font-bold transition-colors ${
                  isSelected
                    ? 'bg-amber-500 text-slate-950 font-black'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 group-hover:bg-amber-500 group-hover:text-slate-950'
                }`}
              >
                {isSelected ? 'الثيم المطبق حالياً' : 'تطبيق هذا الثيم'}
              </button>
            </div>
          );
        })}
      </div>

    </div>
  );
};
