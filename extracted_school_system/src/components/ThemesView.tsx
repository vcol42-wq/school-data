import React, { useState } from 'react';
import { AppTheme, AppFont } from '../types';
import { Palette, Check, Type, Sparkles } from 'lucide-react';

interface ThemesViewProps {
  currentTheme: AppTheme;
  setTheme: (theme: AppTheme) => void;
  currentFont?: AppFont;
  setFont?: (font: AppFont) => void;
}

export const ThemesView: React.FC<ThemesViewProps> = ({ 
  currentTheme, 
  setTheme,
  currentFont = 'tajawal',
  setFont
}) => {
  const [activeTab, setActiveTab] = useState<'themes' | 'fonts'>('themes');

  const themesList: {
    id: AppTheme;
    name: string;
    description: string;
    colors: string[];
    headerColor: string;
    bgStyle: string;
  }[] = [
    {
      id: 'dark',
      name: 'الوضع الليلي (Dark Mode)',
      description: 'ثيم داكن عميق (#0b0f19) عالي التباين والوضوح، مريح جداً للعين ويمنع إجهاد البصر أثناء العمل الطويل.',
      colors: ['#0b0f19', '#111827', '#38bdf8', '#f8fafc'],
      headerColor: 'bg-slate-950',
      bgStyle: 'bg-slate-900'
    },
    {
      id: 'lunar',
      name: 'الوضع القمري (Lunar Slate)',
      description: 'ثيم سماء ليلية قمرية بلون أزرق-رمادي هادئ وفخم (#0f172a) مع لمسات نيلية وفضية أنيقة وواضحة.',
      colors: ['#0f172a', '#1e293b', '#60a5fa', '#f1f5f9'],
      headerColor: 'bg-slate-900',
      bgStyle: 'bg-slate-800'
    },
    {
      id: 'cream',
      name: 'الوضع الكريمي الدافئ (Warm Cream)',
      description: 'ثيم ورق بردي كريمي مريح وطبيعي (#f5efe6) مع تباين عالي ونصوص قهوائية داكنة واضحة وممتازة للقراءة.',
      colors: ['#f5efe6', '#ffffff', '#b45309', '#1c1917'],
      headerColor: 'bg-amber-950',
      bgStyle: 'bg-amber-100'
    },
    {
      id: 'burgundy',
      name: 'الوضع العنابي الملكي (Royal Burgundy)',
      description: 'ثيم عنابي ملكي فاخر (#4c0519) مع تباين قوي وخلفية وردية عاجية مريحة تعكس هيبة المؤسسات الرسمية.',
      colors: ['#4c0519', '#9f1239', '#fff1f2', '#ffffff'],
      headerColor: 'bg-rose-950',
      bgStyle: 'bg-rose-100'
    }
  ];

  const fontsList: {
    id: AppFont;
    name: string;
    fontClass: string;
    description: string;
    sampleText: string;
  }[] = [
    {
      id: 'tajawal',
      name: 'خط تجوال (Tajawal)',
      fontClass: 'font-tajawal',
      description: 'الخط القياسي الحديث المتناسق في الواجهات والتطبيقات الحكومية.',
      sampleText: 'جمهورية العراق - وزارة التربية - مديرية تربية ديالى'
    },
    {
      id: 'cairo',
      name: 'خط كايرو (Cairo)',
      fontClass: 'font-cairo',
      description: 'خط هندسي واضح ومرن ذو حضور قوي في العناوين والجداول الرسمية.',
      sampleText: 'نظام الإدارة المدرسية المتكامل والجدول الدراسي الموحد'
    },
    {
      id: 'amiri',
      name: 'خط أميري الكلاسيكي (Amiri)',
      fontClass: 'font-amiri',
      description: 'الخط النسخي العربي الأصيل والهيبة للوثائق والشهادات الرسمية.',
      sampleText: 'بسم الله الرحمن الرحيم - وثيقة درجات وانتقال طالب رسمية'
    },
    {
      id: 'alexandria',
      name: 'خط الإسكندرية (Alexandria)',
      fontClass: 'font-alexandria',
      description: 'خط عصري عريض جداً ممتاز للقراءة السريعة والأرقام.',
      sampleText: 'سجل الطلبة والكادر التدريسي - العام الدراسي 2024-2025'
    },
    {
      id: 'noto',
      name: 'خط نوتو الواضح (Noto Sans Arabic)',
      fontClass: 'font-noto',
      description: 'خط قياسي واضح مريح للعين أثناء قراءة النصوص الطويلة.',
      sampleText: 'استمارة الملاك المدرسي الشاملة وإحصاءات المواد الدراسية'
    }
  ];

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
      
      {/* Header */}
      <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-50 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300 text-xs font-bold mb-2">
            <Palette className="w-4 h-4" />
            <span>تخصيص المظهر والثيمات والخطوط</span>
          </div>
          <h2 className="text-2xl font-black text-[var(--theme-text-main)]">
            التحكم الحقيقي في مظهر وثيمات وخطوط البرنامج
          </h2>
          <p className="text-xs text-[var(--theme-text-muted)] mt-1">
            يتم تطبيق التغييرات فورياً ولحظياً على كافة البطاقات والشاشات والخلفيات والجداول والطباعة.
          </p>
        </div>

        {/* Tab Controls (الثيمات | الخطوط) */}
        <div className="flex items-center gap-2 p-1.5 bg-slate-100 dark:bg-slate-800 rounded-2xl border border-slate-200 dark:border-slate-700">
          <button
            onClick={() => setActiveTab('themes')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-black text-xs transition-all cursor-pointer ${
              activeTab === 'themes'
                ? 'bg-amber-500 text-slate-950 shadow-md font-extrabold'
                : 'text-slate-600 dark:text-slate-300 hover:bg-slate-200'
            }`}
          >
            <Palette className="w-4 h-4" />
            <span>ألوان وثيمات الواجهة</span>
          </button>

          <button
            onClick={() => setActiveTab('fonts')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-black text-xs transition-all cursor-pointer ${
              activeTab === 'fonts'
                ? 'bg-amber-500 text-slate-950 shadow-md font-extrabold'
                : 'text-slate-600 dark:text-slate-300 hover:bg-slate-200'
            }`}
          >
            <Type className="w-4 h-4" />
            <span>الخطوط العربية الرسمية</span>
          </button>
        </div>
      </div>

      {/* TAB 1: THEMES PALETTES */}
      {activeTab === 'themes' && (
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
                  className={`mt-4 w-full py-2.5 rounded-xl text-xs font-bold transition-colors ${
                    isSelected
                      ? 'bg-amber-500 text-slate-950 font-black'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 group-hover:bg-amber-500 group-hover:text-slate-950'
                  }`}
                >
                  {isSelected ? 'الثيم المطبق حالياً ✓' : 'تطبيق هذا الثيم'}
                </button>
              </div>
            );
          })}
        </div>
      )}

      {/* TAB 2: ARABIC FONTS */}
      {activeTab === 'fonts' && (
        <div className="space-y-4">
          {fontsList.map((f) => {
            const isSelected = currentFont === f.id;
            return (
              <div
                key={f.id}
                onClick={() => setFont && setFont(f.id)}
                className={`p-5 rounded-2xl border-2 transition-all cursor-pointer bg-[var(--theme-card)] shadow-sm hover:shadow-md flex flex-col md:flex-row items-start md:items-center justify-between gap-4 ${
                  isSelected 
                    ? 'border-amber-500 ring-2 ring-amber-500/20' 
                    : 'border-[var(--theme-card-border)] hover:border-amber-400'
                }`}
              >
                <div className="space-y-2 flex-1">
                  <div className="flex items-center gap-3">
                    <h3 className={`text-lg font-bold text-[var(--theme-text-main)] ${f.fontClass}`}>
                      {f.name}
                    </h3>
                    {isSelected && (
                      <span className="bg-amber-500 text-slate-950 text-xs font-black px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-sm">
                        <Check className="w-3.5 h-3.5" />
                        الخط المطبق حالياً
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-[var(--theme-text-muted)]">{f.description}</p>
                  
                  {/* Live Sample Text */}
                  <div className={`p-4 rounded-xl bg-slate-900 text-amber-300 border-2 border-amber-400 text-lg font-black shadow-inner ${f.fontClass}`}>
                    "{f.sampleText}"
                  </div>
                </div>

                <button
                  className={`px-6 py-2.5 rounded-xl text-xs font-bold transition-all shrink-0 ${
                    isSelected
                      ? 'bg-amber-500 text-slate-950 font-black'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-amber-500 hover:text-slate-950'
                  }`}
                >
                  {isSelected ? 'الخط المفعل ✓' : 'تطبيق هذا الخط'}
                </button>
              </div>
            );
          })}
        </div>
      )}

    </div>
  );
};
