import React from 'react';
import {
  Lightbulb,
  BookOpen,
  Users,
  CheckCircle2,
  Target,
  MessageSquare,
  ShieldCheck,
  Sparkles,
  ArrowRight,
  ClipboardCheck,
  Zap
} from 'lucide-react';
import { motion } from 'motion/react';

interface ManagementTipsViewProps {
  onBack?: () => void;
}

export const ManagementTipsView: React.FC<ManagementTipsViewProps> = ({ onBack }) => {
  const tips = [
    {
      title: "الإدارة الصفية الناجحة",
      icon: Users,
      color: "bg-blue-500",
      items: [
        "ضع قواعد واضحة ومحددة منذ اليوم الأول وناقشها مع الطلاب.",
        "التزم بالثبات والعدل في تطبيق القواعد على جميع الطلاب دون تمييز.",
        "استخدم لغة الجسد ونبرة الصوت المتنوعة لجذب الانتباه والسيطرة.",
        "نظم مقاعد الصف بما يتناسب مع طبيعة النشاط التعليمي (حلقات، صفوف، مجموعات).",
        "احرص على البدء الفوري بالدرس بمجرد دخولك الصف لتقليل الفوضى."
      ]
    },
    {
      title: "طرائق التدريس الفعالة",
      icon: BookOpen,
      color: "bg-emerald-500",
      items: [
        "التعلّم النشط: اجعل الطالب هو المحور من خلال النقاش وحل المشكلات.",
        "التعلم باللعب: استخدم الألعاب التعليمية لتبسيط المفاهيم الصعبة.",
        "استراتيجية العصف الذهني: شجع الطلاب على توليد الأفكار بحرية.",
        "التدريس المتمايز: قدّم المحتوى بطرق مختلفة تناسب ذكاءات الطلاب المتنوعة.",
        "الخرائط الذهنية: استخدم الرسوم التوضيحية لربط المعلومات وتسهيل تذكرها."
      ]
    },
    {
      title: "تحفيز الطلاب وتفاعلهم",
      icon: Sparkles,
      color: "bg-amber-500",
      items: [
        "استخدم التعزيز الإيجابي الفوري (اللفظي والمادي) للإنجازات الصغيرة.",
        "اربط المادة العلمية بالواقع العملي والاهتمامات الشخصية للطلاب.",
        "نوّع في استخدام الوسائل التعليمية (فيديو، صور، تجارب عملية).",
        "اعطِ الطلاب أدواراً قيادية داخل الصف لتعزيز ثقتهم بأنفسهم.",
        "اجعل بيئة الصف آمنة يتقبل فيها الجميع الخطأ كجزء من عملية التعلم."
      ]
    },
    {
      title: "إرشادات تربوية عامة",
      icon: ShieldCheck,
      color: "bg-rose-500",
      items: [
        "كن قدوة حسنة في الالتزام بالمواعيد، الهندام، وأسلوب التعامل.",
        "تواصل بانتظام مع أولياء الأمور لإشراكهم في العملية التعليمية.",
        "اهتم بالجانب النفسي والاجتماعي للطلاب وليس الأكاديمي فقط.",
        "خصّص وقتاً للاستماع لمشاكل الطلاب ومقترحاتهم حول الدرس.",
        "طوّر نفسك مهنياً من خلال الاطلاع على أحدث الأبحاث والوسائل التربوية."
      ]
    }
  ];

  return (
    <div className="p-4 md:p-8 max-w-6xl mx-auto">
      {/* Header Section */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between mb-8 gap-4">
        <div className="flex items-center gap-4">
          {onBack && (
            <button
              onClick={onBack}
              className="p-2.5 rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 transition-all shadow-sm cursor-pointer"
            >
              <ArrowRight className="w-5 h-5" />
            </button>
          )}
          <div>
            <div className="flex items-center gap-2 text-indigo-600 mb-1">
              <Zap className="w-5 h-5 fill-indigo-600" />
              <span className="text-xs font-black uppercase tracking-wider">مرشد المعلم والإدارة</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-black text-slate-900">نصائح الإدارة الصفية وطرائق التدريس</h1>
          </div>
        </div>

        <div className="hidden md:flex items-center gap-3 px-4 py-2 bg-indigo-50 border border-indigo-100 rounded-2xl">
          <Lightbulb className="w-5 h-5 text-indigo-600" />
          <span className="text-sm font-bold text-indigo-700">دليلك المهني نحو بيئة تعليمية متميزة</span>
        </div>
      </div>

      {/* Grid of Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {tips.map((section, idx) => {
          const Icon = section.icon;
          return (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.1 }}
              key={idx}
              className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden flex flex-col"
              style={{
                boxShadow: '0 10px 25px -5px rgba(0,0,0,0.02), 0 8px 10px -6px rgba(0,0,0,0.02)'
              }}
            >
              <div className={`p-5 flex items-center gap-4 ${section.color} text-white`}>
                <div className="p-2.5 bg-white/20 rounded-xl backdrop-blur-sm">
                  <Icon className="w-6 h-6 text-white" />
                </div>
                <h2 className="text-xl font-black">{section.title}</h2>
              </div>

              <div className="p-6 flex-1 bg-gradient-to-b from-white to-slate-50/50">
                <ul className="space-y-4">
                  {section.items.map((item, i) => (
                    <li key={i} className="flex items-start gap-3 group">
                      <div className="mt-1.5 shrink-0">
                        <CheckCircle2 className={`w-4 h-4 ${section.color.replace('bg-', 'text-')} opacity-60 group-hover:opacity-100 transition-opacity`} />
                      </div>
                      <span className="text-slate-700 font-bold text-sm leading-relaxed">{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </motion.div>
          );
        })}
      </div>

      {/* Action Banner */}
      <div className="mt-10 p-6 rounded-[2.5rem] bg-indigo-900 text-white relative overflow-hidden border-4 border-indigo-200/20 shadow-2xl">
        <div className="absolute top-0 right-0 p-8 opacity-10 rotate-12">
          <Target className="w-32 h-32" />
        </div>

        <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="text-center md:text-right">
            <h3 className="text-xl font-black mb-2 flex items-center justify-center md:justify-start gap-2">
              <ClipboardCheck className="w-6 h-6 text-amber-400" />
              رسالة للمربي المتميز
            </h3>
            <p className="text-indigo-100 font-medium max-w-xl text-sm leading-relaxed">
              تذكر دائماً أن تأثير المعلم الجيد لا ينتهي أبداً؛ فأنت لا تدرس مادة علمية فحسب، بل تبني عقولاً وأجيالاً. استخدامك لهذه الاستراتيجيات يحول الصف من مجرد غرفة دراسية إلى بيئة ملهمة للإبداع.
            </p>
          </div>

          <button
            className="px-8 py-3 bg-white text-indigo-900 rounded-2xl font-black shadow-lg hover:bg-indigo-50 transition-all hover:scale-105 active:scale-95 cursor-pointer flex items-center gap-2"
            onClick={() => window.print()}
          >
            <MessageSquare className="w-5 h-5" />
            طباعة الدليل السريع
          </button>
        </div>
      </div>
    </div>
  );
};
