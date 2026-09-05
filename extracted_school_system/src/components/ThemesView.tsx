import React from 'react';
import { AppTheme, AppFont } from '../types';
import { Check, Type, CheckCircle2, ShieldCheck } from 'lucide-react';

interface ThemesViewProps {
  currentTheme?: AppTheme;
  setTheme?: (theme: AppTheme) => void;
  currentFont?: AppFont;
  setFont?: (font: AppFont) => void;
}

export const ThemesView: React.FC<ThemesViewProps> = ({ 
  currentFont = 'tajawal',
  setFont
}) => {
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
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50 text-blue-700 text-xs font-bold mb-2">
            <ShieldCheck className="w-4 h-4" />
            <span>المظهر القياسي والخطوط الرسمية المعتمدة</span>
          </div>
          <h2 className="text-2xl font-black text-slate-900">
            مظهر البرنامج الموحد والخطوط العربية
          </h2>
          <p className="text-xs text-slate-500 mt-1">
            يعتمد البرنامج المظهر التربوي الرسمي عالي التباين لضمان وضوح كامل للخطوط والجداول بدون أي تشويه بصري.
          </p>
        </div>

        {/* Info Badge */}
        <div className="flex items-center gap-2 px-4 py-2 bg-emerald-50 text-emerald-800 rounded-xl border border-emerald-200 text-xs font-bold">
          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          <span>المظهر القياسي النقي مفعل دائماً (وضوح 100%)</span>
        </div>
      </div>

      {/* Standard Clean Theme Status Card */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-blue-600 text-white flex items-center justify-center font-black text-xl shadow">
              ✓
            </div>
            <div>
              <h3 className="text-lg font-black text-slate-900">المظهر الرسمي المعتمد لوزارة التربية</h3>
              <p className="text-xs text-slate-600 mt-1">
                واجهة متناسقة بنسبة تباين كاملة مع خلفيات بيضاء نقية ونصوص داكنة حادة لمنع أي تداخل أو بهتان في الخطوط مع راحة بصرية فائقة.
              </p>
            </div>
          </div>
          <span className="bg-blue-100 text-blue-800 text-xs font-black px-3 py-1 rounded-full shrink-0">
            المظهر الافتراضي الثابت
          </span>
        </div>
      </div>

      {/* ARABIC FONTS SELECTION */}
      <div className="space-y-4">
        <div className="flex items-center gap-2 px-2">
          <Type className="w-5 h-5 text-blue-600" />
          <h3 className="text-lg font-black text-slate-900">اختر نوع الخط العربي المفضل لواجهة البرنامج:</h3>
        </div>

        {fontsList.map((f) => {
          const isSelected = currentFont === f.id;
          return (
            <div
              key={f.id}
              onClick={() => setFont && setFont(f.id)}
              className={`p-5 rounded-2xl border-2 transition-all cursor-pointer bg-white shadow-sm hover:shadow-md flex flex-col md:flex-row items-start md:items-center justify-between gap-4 ${
                isSelected 
                  ? 'border-blue-600 ring-2 ring-blue-500/20 bg-blue-50/20' 
                  : 'border-slate-200 hover:border-blue-300'
              }`}
            >
              <div className="space-y-2 flex-1">
                <div className="flex items-center gap-3">
                  <h4 className={`text-lg font-bold text-slate-900 ${f.fontClass}`}>
                    {f.name}
                  </h4>
                  {isSelected && (
                    <span className="bg-blue-600 text-white text-xs font-black px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-sm">
                      <Check className="w-3.5 h-3.5" />
                      الخط المطبق حالياً
                    </span>
                  )}
                </div>
                <p className="text-xs text-slate-500">{f.description}</p>
                
                {/* Live Sample Text */}
                <div className={`p-4 rounded-xl bg-slate-100 text-slate-900 border border-slate-300 text-lg font-black ${f.fontClass}`}>
                  "{f.sampleText}"
                </div>
              </div>

              <button
                className={`px-6 py-2.5 rounded-xl text-xs font-bold transition-all shrink-0 ${
                  isSelected
                    ? 'bg-blue-600 text-white font-black shadow'
                    : 'bg-slate-100 text-slate-700 hover:bg-blue-600 hover:text-white'
                }`}
              >
                {isSelected ? 'الخط المفعل ✓' : 'تطبيق هذا الخط'}
              </button>
            </div>
          );
        })}
      </div>

    </div>
  );
};
