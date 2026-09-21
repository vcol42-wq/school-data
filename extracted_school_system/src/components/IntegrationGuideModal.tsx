import React from 'react';
import { 
  BookOpen, 
  X, 
  CheckCircle2, 
  Smartphone, 
  Monitor, 
  Cloud, 
  Key, 
  QrCode, 
  Users, 
  Calendar, 
  FileSpreadsheet, 
  Sparkles,
  ArrowLeft,
  ShieldCheck,
  Check
} from 'lucide-react';

interface IntegrationGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
  onOpenLicenseModal?: () => void;
  onOpenAndroidModal?: () => void;
}

export const IntegrationGuideModal: React.FC<IntegrationGuideModalProps> = ({
  isOpen,
  onClose,
  onOpenLicenseModal,
  onOpenAndroidModal
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200 dir-rtl">
      <div className="relative w-full max-w-4xl bg-white border-4 border-indigo-600 rounded-[2.5rem] shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        
        {/* Header */}
        <div className="relative bg-gradient-to-r from-indigo-900 via-blue-900 to-indigo-950 p-6 text-white flex items-center justify-between border-b-4 border-amber-400">
          <div className="flex items-center gap-4">
            <div className="w-13 h-13 rounded-2xl bg-white/15 backdrop-blur-md flex items-center justify-center border border-white/20 shadow-xl">
              <BookOpen className="w-7 h-7 text-amber-300" />
            </div>
            <div>
              <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-emerald-500/25 text-emerald-200 border border-emerald-400/30 text-xs font-black mb-1">
                <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                <span>دورة العمل الشاملة</span>
              </div>
              <h2 className="text-xl md:text-2xl font-black text-white">
                دليل التشغيل والربط السحابي المتكامل (Desktop + Android)
              </h2>
              <p className="text-xs text-blue-200 font-bold">
                كيف تعمل المنظومة كحلقة وصل ذكية بين حاسوب إدارة المدرسة وهواتف الكادر والطلبة
              </p>
            </div>
          </div>

          <button 
            onClick={onClose}
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-colors cursor-pointer"
            title="إغلاق"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto space-y-8 flex-1 text-right">

          {/* Intro Card */}
          <div className="bg-gradient-to-r from-indigo-50 to-blue-50 border-2 border-indigo-200 p-5 rounded-3xl flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-indigo-600 text-white rounded-2xl shadow-md">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-sm font-black text-indigo-950">نموذج تشغيل عصري ومرن (Freemium):</h3>
                <p className="text-xs text-slate-600 font-bold mt-0.5">
                  جميع ميزات سطح المكتب (شيتات الإكسل، الدرجات، السجلات، طباعة الجدول) <span className="text-emerald-700 font-black">مجانية ومتاحة بالكامل 100%</span>. يتم تفعيل الاشتراك فقط عند الرغبة في فتح الربط السحابي ومزامنة هواتف الأساتذة والطلبة.
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              {onOpenLicenseModal && (
                <button
                  onClick={() => { onClose(); onOpenLicenseModal(); }}
                  className="px-4 py-2.5 bg-amber-500 hover:bg-amber-400 text-slate-950 rounded-xl text-xs font-black shadow-md cursor-pointer transition-all active:scale-95 border border-amber-300"
                >
                  تفعيل الربط السحابي ⚡
                </button>
              )}
              {onOpenAndroidModal && (
                <button
                  onClick={() => { onClose(); onOpenAndroidModal(); }}
                  className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-black shadow-md cursor-pointer transition-all active:scale-95"
                >
                  تطبيق أندرويد 📱
                </button>
              )}
            </div>
          </div>

          {/* 4-Step Visual Lifecycle */}
          <div className="space-y-6">
            <h3 className="text-base font-black text-slate-900 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-indigo-600" />
              <span>مراحل دورة العمل المتكاملة (خطوة بخطوة):</span>
            </h3>

            {/* Step 1 */}
            <div className="relative bg-white border-2 border-slate-200 hover:border-indigo-400 p-5 rounded-3xl shadow-sm transition-all flex flex-col md:flex-row items-start gap-4">
              <div className="w-12 h-12 rounded-2xl bg-indigo-100 text-indigo-700 flex items-center justify-center text-lg font-black shrink-0 border border-indigo-200">
                1
              </div>
              <div className="flex-1 space-y-1.5">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-black text-slate-900 flex items-center gap-2">
                    <Monitor className="w-4 h-4 text-indigo-600" />
                    <span>إعداد بيانات المدرسة محلياً على الحاسوب (مجاناً بالكامل)</span>
                  </h4>
                  <span className="px-2.5 py-0.5 bg-emerald-100 text-emerald-800 rounded-full text-[10px] font-black border border-emerald-200">
                    متاح دائماً
                  </span>
                </div>
                <p className="text-xs text-slate-600 font-bold leading-relaxed">
                  يقوم مدير المدرسة أو المعاون باستيراد شيت إكسل الوزاري المعتمد الذي يحتوي على أسماء الطلبة وسجلاتهم، ثم إدخال أسماء الكادر التعليمي وإسناد الحصص، وتشغيل محرك التوليد الآلي الذكي لجدول الحصص الأسبوعي وضبط الشواغر.
                </p>
                <div className="flex flex-wrap gap-2 pt-1 text-[11px] font-bold text-slate-500">
                  <span className="px-2 py-0.5 bg-slate-100 rounded-lg">✓ استيراد وتصدير الإكسل</span>
                  <span className="px-2 py-0.5 bg-slate-100 rounded-lg">✓ توليد الجدول المدرسي</span>
                  <span className="px-2 py-0.5 bg-slate-100 rounded-lg">✓ طباعة السجلات والشيت</span>
                </div>
              </div>
            </div>

            {/* Step 2 */}
            <div className="relative bg-white border-2 border-amber-200 hover:border-amber-400 p-5 rounded-3xl shadow-sm transition-all flex flex-col md:flex-row items-start gap-4">
              <div className="w-12 h-12 rounded-2xl bg-amber-100 text-amber-800 flex items-center justify-center text-lg font-black shrink-0 border border-amber-300">
                2
              </div>
              <div className="flex-1 space-y-1.5">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-black text-slate-900 flex items-center gap-2">
                    <Cloud className="w-4 h-4 text-amber-600" />
                    <span>تفعيل الربط السحابي وفتح الباركود والرموز الثلاثية</span>
                  </h4>
                  <span className="px-2.5 py-0.5 bg-amber-100 text-amber-900 rounded-full text-[10px] font-black border border-amber-300">
                    تفعيل لمرة واحدة
                  </span>
                </div>
                <p className="text-xs text-slate-600 font-bold leading-relaxed">
                  عند رغبة المدرسة بربط هواتف الأساتذة بالمنظومة، يتم تفعيل ترخيص الربط السحابي لفتح أكواد الاقتران الثلاثية (كود المعلم، كود الطالب، كود المدير) وإظهار باركود الـ QR، ويتم رفع وتصدير كامل بيانات المدرسة والجدول إلى السحابة بضغطة زر واحدة.
                </p>
                <div className="flex flex-wrap gap-2 pt-1 text-[11px] font-bold text-slate-500">
                  <span className="px-2 py-0.5 bg-amber-50 text-amber-900 rounded-lg">✓ فتح أكواد الربط الثلاثية</span>
                  <span className="px-2 py-0.5 bg-amber-50 text-amber-900 rounded-lg">✓ رفع الجداول والقوائم للسحابة</span>
                  <span className="px-2 py-0.5 bg-amber-50 text-amber-900 rounded-lg">✓ مصادقة أمان Supabase RLS</span>
                </div>
              </div>
            </div>

            {/* Step 3 */}
            <div className="relative bg-white border-2 border-slate-200 hover:border-blue-400 p-5 rounded-3xl shadow-sm transition-all flex flex-col md:flex-row items-start gap-4">
              <div className="w-12 h-12 rounded-2xl bg-blue-100 text-blue-700 flex items-center justify-center text-lg font-black shrink-0 border border-blue-200">
                3
              </div>
              <div className="flex-1 space-y-1.5">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-black text-slate-900 flex items-center gap-2">
                    <Smartphone className="w-4 h-4 text-blue-600" />
                    <span>تثبيت تطبيق أندرويد على هواتف الأساتذة والطلبة</span>
                  </h4>
                  <span className="px-2.5 py-0.5 bg-blue-100 text-blue-800 rounded-full text-[10px] font-black border border-blue-200">
                    Google Play
                  </span>
                </div>
                <p className="text-xs text-slate-600 font-bold leading-relaxed">
                  تقوم إدارة المدرسة بمشاركة رابط تطبيق Google Play مع كروب الأساتذة عبر الواتساب. يقوم كل أستاذ بتحميل التطبيق على هاتفه المحمول بكل بساطة دون الحاجة لإنشاء حسابات معقدة.
                </p>
                <div className="flex flex-wrap gap-2 pt-1 text-[11px] font-bold text-slate-500">
                  <span className="px-2 py-0.5 bg-slate-100 rounded-lg">✓ متاح على متجر Google Play</span>
                  <span className="px-2 py-0.5 bg-slate-100 rounded-lg">✓ خفيف وسريع ومتوافق مع جميع الهواتف</span>
                </div>
              </div>
            </div>

            {/* Step 4 */}
            <div className="relative bg-white border-2 border-emerald-200 hover:border-emerald-400 p-5 rounded-3xl shadow-sm transition-all flex flex-col md:flex-row items-start gap-4">
              <div className="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-800 flex items-center justify-center text-lg font-black shrink-0 border border-emerald-300">
                4
              </div>
              <div className="flex-1 space-y-1.5">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-black text-slate-900 flex items-center gap-2">
                    <QrCode className="w-4 h-4 text-emerald-600" />
                    <span>مسح الباركود، رصد الدرجات والغيابات، والمزامنة الحية</span>
                  </h4>
                  <span className="px-2.5 py-0.5 bg-emerald-100 text-emerald-800 rounded-full text-[10px] font-black border border-emerald-300">
                    لحظي وفوري
                  </span>
                </div>
                <p className="text-xs text-slate-600 font-bold leading-relaxed">
                  يقوم كل أستاذ بمسح بطاقة الباركود الخاصة به (أو إدخال كود المدرسة والـ PIN المخصص له). فوراً تظهر حصصه ومواده وقوائم طلابه؛ فيرصد درجات الأشهر والغياب اليومي مباشرة من داخل الصف الدراسي، وتنعكس كافة البيانات على حاسوب الإدارة تلقائياً دون أي جهد ورقي!
                </p>
                <div className="flex flex-wrap gap-2 pt-1 text-[11px] font-bold text-slate-500">
                  <span className="px-2 py-0.5 bg-emerald-50 text-emerald-900 rounded-lg">✓ رصد درجات الأشهر ونصف السنة</span>
                  <span className="px-2 py-0.5 bg-emerald-50 text-emerald-900 rounded-lg">✓ تسجيل الغيابات وحصص الاحتياط</span>
                  <span className="px-2 py-0.5 bg-emerald-50 text-emerald-900 rounded-lg">✓ سحب فوري للدرجات داخل شيت الحاسوب</span>
                </div>
              </div>
            </div>

          </div>

        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs font-bold text-slate-600">
          <span className="text-[11px] text-slate-500">
            منظومة The Principal — البيئة المدرسية الذكية الموحدة للمدارس العراقية والعربية.
          </span>
          <button
            onClick={onClose}
            className="px-6 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-black transition-colors cursor-pointer"
          >
            إغلاق الدليل
          </button>
        </div>

      </div>
    </div>
  );
};
