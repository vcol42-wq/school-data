import React from 'react';
import { AppFont } from '../types';
import { Type, Check } from 'lucide-react';

interface FontsViewProps {
  currentFont: AppFont;
  setFont: (font: AppFont) => void;
}

export const FontsView: React.FC<FontsViewProps> = ({ currentFont, setFont }) => {
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
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
      
      <div className="bg-[var(--theme-card)] p-6 rounded-2xl border border-[var(--theme-card-border)] shadow-sm">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-rose-50 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300 text-xs font-bold mb-2">
          <Type className="w-4 h-4" />
          <span>تخصيص الخطوط الرسمية</span>
        </div>
        <h2 className="text-2xl font-black text-[var(--theme-text-main)]">
          اختيار نوع الخط المطبق في كافة أجزاء النظام
        </h2>
        <p className="text-xs text-[var(--theme-text-muted)] mt-1">
          اختر نوع الخط العربي ليتغير مظهر العناوين والجداول والكتب المدرسية حقيقيًا عبر كافة الشاشات.
        </p>
      </div>

      <div className="space-y-4">
        {fontsList.map((f) => {
          const isSelected = currentFont === f.id;
          return (
            <div
              key={f.id}
              onClick={() => setFont(f.id)}
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
                
                {/* Live Sample Text in target font */}
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
                {isSelected ? 'مفعل' : 'تطبيق الخط'}
              </button>
            </div>
          );
        })}
      </div>

    </div>
  );
};
