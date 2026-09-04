import React, { useState } from 'react';
import { ActiveView } from '../types';
import { 
  CalendarDays, 
  GraduationCap, 
  UserMinus,
  Users, 
  BarChart3, 
  Printer, 
  Palette, 
  BellRing, 
  Settings,
  Sparkles,
  LayoutGrid,
  Circle,
  Square,
  Mic,
  Cloud,
  ClipboardList,
  UserCheck,
  Trash2,
  KeyRound
} from 'lucide-react';

interface MainLauncherProps {
  setActiveView: (view: ActiveView) => void;
  studentsCount: number;
  staffCount: number;
  onOpenVoiceModal?: () => void;
  students: any[];
  setStudents: React.Dispatch<React.SetStateAction<any[]>>;
  scheduleMap: any;
  config?: any;
  onResetData?: () => void;
}

export const MainLauncher: React.FC<MainLauncherProps> = ({
  setActiveView,
  studentsCount,
  staffCount,
  onOpenVoiceModal,
  config,
  onResetData
}) => {
  const [iconShape, setIconShape] = useState<'squircle' | 'round'>('squircle');
  const [copiedCode, setCopiedCode] = useState(false);

  const launcherItems = [
    // 1. الجدول والحصص
    {
      id: 'schedule' as ActiveView,
      title: 'جدول الحصص والتوقيتات',
      subtitle: 'الجدول الأسبوعي وتوزيع الحصص',
      icon: CalendarDays,
      gradient: 'from-blue-600 via-indigo-600 to-blue-800',
      badge: 'الجدول 📅',
      badgeBg: 'bg-blue-100 text-blue-900 font-black'
    },
    // 1.1. مولّد الجدول الذكي والعادل
    {
      id: 'smart_schedule' as ActiveView,
      title: 'مولّد الجدول الذكي والعادل',
      subtitle: 'توزيع الحصص آلياً ومنع تضارب المعلمين',
      icon: Sparkles,
      gradient: 'from-purple-700 via-indigo-700 to-purple-900',
      badge: 'توليد ذكي 🪄',
      badgeBg: 'bg-purple-100 text-purple-950 font-black'
    },
    // 2. الشعب والطلاب المستمرون
    {
      id: 'students' as ActiveView,
      title: 'الطلاب المستمرون والشعب',
      subtitle: `${studentsCount} طالب مستمر | بطاقات الشعب والدرجات`,
      icon: GraduationCap,
      gradient: 'from-emerald-500 via-teal-600 to-emerald-800',
      badge: `${studentsCount} طالب`,
      badgeBg: 'bg-emerald-100 text-emerald-950 font-black'
    },
    // 3. سجل الدرجات والتقييمات
    {
      id: 'student_grades' as ActiveView,
      title: 'سجل الدرجات والتقييمات',
      subtitle: 'مراجعة درجات وغيابات المواد المتزامنة',
      icon: ClipboardList,
      gradient: 'from-amber-500 via-orange-600 to-amber-700',
      badge: 'الدرجات 📝',
      badgeBg: 'bg-amber-100 text-amber-950 font-black'
    },
    // 4. سجل ومتابعة الغيابات
    {
      id: 'attendance' as ActiveView,
      title: 'سجل ومتابعة الغيابات',
      subtitle: 'متابعة الحضور اليومي، الإنذارات، وسحب غيابات المدرسين',
      icon: UserCheck,
      gradient: 'from-rose-600 via-rose-700 to-amber-800',
      badge: 'الغيابات والحضور 📋',
      badgeBg: 'bg-rose-100 text-rose-950 font-black'
    },
    // 5. سجل وتوزيع الكادر
    {
      id: 'staff' as ActiveView,
      title: 'سجل وتوزيع الكادر',
      subtitle: `${staffCount} منتسب ومدرس | إسناد المواد وحصص الجدول`,
      icon: Users,
      gradient: 'from-purple-600 via-violet-700 to-indigo-800',
      badge: `${staffCount} كادر`,
      badgeBg: 'bg-purple-100 text-purple-950 font-black'
    },
    // 5.1. أكواد المعلمين وتفويض الشعب والدرجات
    {
      id: 'teacher_authority' as ActiveView,
      title: 'أكواد المعلمين وتفويض الشعب',
      subtitle: 'رموز رفع الدرجات السرية وقفل الشعب والمواد',
      icon: KeyRound,
      gradient: 'from-amber-600 via-orange-600 to-amber-900',
      badge: 'أمان ورفع الدرجات 🔑',
      badgeBg: 'bg-amber-100 text-amber-950 font-black'
    },
    // 6. الإحصاء والملاك الرسمي
    {
      id: 'stats' as ActiveView,
      title: 'الإحصاء والملاك الرسمي',
      subtitle: 'السجل التفصيلي العريض، الشواغر، والفائض',
      icon: BarChart3,
      gradient: 'from-orange-500 via-amber-600 to-orange-700',
      badge: 'الملاك الشامل 📊',
      badgeBg: 'bg-orange-100 text-orange-950 font-black'
    },
    // 7. أرشيف الطلاب السابقين
    {
      id: 'former_students' as ActiveView,
      title: 'أرشيف الطلاب السابقين',
      subtitle: 'أرشيف المنقولين والمغادرين والممتنعين',
      icon: UserMinus,
      gradient: 'from-rose-500 via-pink-600 to-rose-700',
      badge: 'أرشيف 📂',
      badgeBg: 'bg-rose-100 text-rose-950 font-black'
    },
    // 8. مركز الطباعة والوثائق الرسمية
    {
      id: 'print' as ActiveView,
      title: 'مركز الطباعة والوثائق الرسمية',
      subtitle: 'الكتب، الإحصائيات، شعار الوزارة، وقوائم A4',
      icon: Printer,
      gradient: 'from-cyan-600 via-blue-600 to-cyan-800',
      badge: 'A4 رسمي 📄',
      badgeBg: 'bg-cyan-100 text-cyan-950 font-black'
    },
    // 9. المنبه والجرس الذكي
    {
      id: 'alarm' as ActiveView,
      title: 'المنبه والجرس الذكي',
      subtitle: 'التنبيه التلقائي للحصص والفرص',
      icon: BellRing,
      gradient: 'from-yellow-500 via-amber-500 to-yellow-600',
      badge: 'جرس 🔔',
      badgeBg: 'bg-yellow-100 text-yellow-950 font-black'
    },
    // 10. الثيمات والمظهر العام
    {
      id: 'themes' as ActiveView,
      title: 'الثيمات والمظهر العام',
      subtitle: 'تغيير ألوان البطاقات والخلفيات والخطوط الحية',
      icon: Palette,
      gradient: 'from-pink-600 via-purple-600 to-indigo-700',
      badge: 'مظهر 🎨',
      badgeBg: 'bg-pink-100 text-pink-950 font-black'
    },
    // 11. مركز الضبط وإعدادات المدرسة
    {
      id: 'settings' as ActiveView,
      title: 'مركز الضبط وإعدادات المدرسة',
      subtitle: 'بيانات الإدارة، تهيئة السنة الجديدة، والأمان',
      icon: Settings,
      gradient: 'from-slate-700 via-slate-800 to-slate-900',
      badge: 'إدارة ⚙️',
      badgeBg: 'bg-slate-200 text-slate-900 font-black'
    },
    // 12. مركز المزامنة والربط السحابي
    {
      id: 'sync_center' as ActiveView,
      title: 'مركز المزامنة والربط السحابي',
      subtitle: 'الباركود QR، حالة المدرسين، والتحديث اللحظي',
      icon: Cloud,
      gradient: 'from-indigo-600 via-blue-700 to-indigo-950',
      badge: 'QR سحابي ⚡',
      badgeBg: 'bg-amber-400 text-slate-950 font-black ring-2 ring-amber-300'
    }
  ];

  return (
    <div className="max-w-6xl mx-auto px-4 py-6 md:py-8">
      
      {/* Light Announcement Banner Container with Indigo Border */}
      <div 
        className="relative overflow-hidden mb-8 rounded-2xl bg-white text-slate-900 p-5 border-3 border-indigo-600 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 shadow-xl"
      >
        <div className="flex items-center gap-3.5 relative z-10">
          <div className="p-3 rounded-2xl bg-indigo-600 text-white shadow-lg border border-indigo-500 shrink-0">
            <LayoutGrid className="w-6 h-6 text-white animate-pulse" />
          </div>
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-indigo-50 text-indigo-600 text-[11px] font-black border border-indigo-100 mb-1">
              <Sparkles className="w-3.5 h-3.5" />
              <span>منظومة The Principal الموحدة v5.0</span>
            </div>
            <h2 className="text-xl md:text-2xl font-black text-slate-900 tracking-wide">
              لوحة التحكم والإدارة المدرسية الشاملة
            </h2>
            <p className="text-xs text-slate-500 font-bold mt-0.5">
              منصة القيادة المركزية وإدارة السجلات وتوزيع المناهج والمزامنة السحابية اللحظية
            </p>
          </div>
        </div>

        {/* Middle Compact Ribbon: Voice Assistant, Pairing Code, Counts */}
        <div className="flex flex-wrap items-center gap-2 bg-slate-50 p-1.5 rounded-xl border border-slate-200 relative z-10 shrink-0">
          {/* Voice Assistant Button */}
          {onOpenVoiceModal && (
            <button
              onClick={onOpenVoiceModal}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 text-white font-black text-xs hover:bg-indigo-700 transition-all shadow-sm cursor-pointer whitespace-nowrap"
              title="تفعيل الأوامر الصوتية باللغة العربية"
            >
              <Mic className="w-3.5 h-3.5 text-white animate-pulse" />
              <span>المساعد الصوتي AI</span>
            </button>
          )}

          {/* Generated School Pairing Code Badge with 1-click Copy */}
          <div 
            onClick={() => {
              const code = config?.pairingCode || '112233';
              navigator.clipboard.writeText(code);
              setCopiedCode(true);
              setTimeout(() => setCopiedCode(false), 2000);
            }}
            title="انقر لنسخ رمز الاقتران الموحد لربط التطبيقات"
            className="flex items-center gap-1.5 bg-gradient-to-r from-amber-400 to-amber-300 text-slate-950 px-2.5 py-1 rounded-lg border border-amber-500 shadow-xs cursor-pointer hover:scale-102 transition-all"
          >
            <Cloud className="w-3.5 h-3.5 text-indigo-950" />
            <span className="text-[10px] font-black text-slate-800">كود الاقتران:</span>
            <span className="font-mono font-black text-xs tracking-wider text-indigo-950">{config?.pairingCode || '112233'}</span>
            <span className="text-[9px] bg-slate-950 text-amber-300 px-1 py-0.2 rounded font-black">
              {copiedCode ? '✓' : 'نسخ'}
            </span>
          </div>

          {/* Quick Counts Pill */}
          <div className="flex items-center gap-2 text-xs font-black bg-white px-2.5 py-1 rounded-lg text-slate-700 border border-slate-200">
            <span className="text-indigo-600 font-mono">{studentsCount} طالب</span>
            <span className="text-slate-300">|</span>
            <span className="text-indigo-600 font-mono">{staffCount} كادر</span>
          </div>
        </div>
      </div>

      {/* Desktop App Icons Container */}
      <div 
        className="relative overflow-hidden bg-white border-4 theme-accent-border rounded-3xl p-6 md:p-8 shadow-xl transition-colors duration-300"
      >
        <div className="flex items-center justify-between mb-6 pb-3 border-b-2 border-slate-100">
          <div className="flex items-center gap-2">
            <LayoutGrid className="w-5 h-5 text-indigo-600" />
            <h2 className="text-base md:text-lg font-black text-slate-900">
              أقسام النظام الأساسية
            </h2>
          </div>
          <span className="text-xs font-black text-indigo-600 bg-indigo-50 border border-indigo-100 px-3 py-1 rounded-xl shadow-xs">
            اضغط على أي قسم للفتح الفوري 🚀
          </span>
        </div>

        {/* Desktop Launcher Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6 md:gap-8 justify-items-center">
          {launcherItems.map((item) => {
            const IconComponent = item.icon;
            
            return (
              <button
                key={item.id}
                onClick={() => {
                  setActiveView(item.id as ActiveView);
                }}
                className="group flex flex-col items-center text-center cursor-pointer transition-all duration-200 transform hover:-translate-y-1.5 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 rounded-2xl p-2 w-full max-w-[130px]"
              >
                {/* Icon Box */}
                <div 
                  className={`relative w-20 h-20 md:w-22 md:h-22 bg-gradient-to-br ${item.gradient} text-white flex items-center justify-center transition-all duration-300 group-hover:scale-105 border-2 border-white shadow-lg ${
                    iconShape === 'squircle' 
                      ? 'rounded-[26%]' 
                      : 'rounded-full'
                  }`}
                >
                  {/* Inner gloss highlight */}
                  <div className="absolute inset-0 bg-gradient-to-t from-transparent via-white/10 to-white/20 rounded-[inherit] pointer-events-none" />

                  {/* Icon Symbol */}
                  <IconComponent className="w-9 h-9 md:w-10 md:h-10 text-white drop-shadow-sm group-hover:rotate-3 transition-transform duration-300 z-10" />

                  {/* Corner Badge */}
                  <span className={`absolute -top-2 -right-2 px-2 py-0.5 rounded-full text-[10px] font-black shadow-lg border-2 border-white ${item.badgeBg}`}>
                    {item.badge}
                  </span>
                </div>

                {/* App Label under icon */}
                <span className="mt-3 text-xs md:text-sm font-extrabold text-[var(--theme-text-main)] group-hover:text-indigo-600 transition-colors leading-tight">
                  {item.title}
                </span>

                {/* Subtitle / Description */}
                <span className="text-[11px] text-[var(--theme-text-muted)] mt-0.5 line-clamp-1 font-medium opacity-85">
                  {item.subtitle}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Footer Info Note */}
      <div className="mt-8 text-center text-xs text-[var(--theme-text-muted)] flex items-center justify-center gap-2">
        <span>يمكنك أيضاً فتح القائمة الجانبية بالضغط على الأسطر الثلاثة (☰) في الشريط الأعلى.</span>
      </div>

    </div>
  );
};
