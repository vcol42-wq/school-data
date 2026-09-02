import React, { useState } from 'react';
import { Award, X, CheckCircle2, GraduationCap, AlertCircle, ArrowRight, UserMinus, ShieldAlert } from 'lucide-react';
import { Student } from '../../types';

interface PromotionModalProps {
  isOpen: boolean;
  onClose: () => void;
  students: Student[];
  onExecutePromotion: (stage: 'متوسطة' | 'إعدادية' | 'ابتدائية', schoolType: 'صباحي' | 'مسائي') => void;
}

export const PromotionModal: React.FC<PromotionModalProps> = ({
  isOpen,
  onClose,
  students,
  onExecutePromotion
}) => {
  const [stage, setStage] = useState<'متوسطة' | 'إعدادية' | 'ابتدائية'>('متوسطة');
  const [schoolType, setSchoolType] = useState<'صباحي' | 'مسائي'>('صباحي');

  if (!isOpen) return null;

  // Active students only
  const activeStudents = students.filter(s => ['مستمر', 'active'].includes(s.status));

  // Compute preview stats
  let previewGraduated = 0;
  let previewPromoted = 0;
  let previewSeparated = 0;
  let previewTransferredToEvening = 0;
  let previewRepeating = 0;

  activeStudents.forEach(s => {
    // Check if passed (either from marksHistory or currentResult)
    const hasFailingMarks = s.marksHistory && s.marksHistory.length > 0 && s.marksHistory.some(m => (m.finalGrade || 0) < 50);
    const isPassed = !hasFailingMarks && (s.currentResult === 'ناجح' || !s.previousYearResult?.includes('راسب'));

    const grade = s.currentGrade || '';

    if (isPassed) {
      if (stage === 'متوسطة' && (grade.includes('الثالث') || grade.includes('3'))) {
        previewGraduated++;
      } else if (stage === 'إعدادية' && (grade.includes('السادس') || grade.includes('6'))) {
        previewGraduated++;
      } else if (stage === 'ابتدائية' && (grade.includes('السادس') || grade.includes('6'))) {
        previewGraduated++;
      } else {
        previewPromoted++;
      }
    } else {
      if (schoolType === 'مسائي') {
        previewSeparated++;
      } else {
        // Morning school
        const wasPreviouslyFailing = (s.previousYearResult || '').includes('راسب') || (s.previousYearResult || '').includes('معيد');
        if (wasPreviouslyFailing) {
          previewTransferredToEvening++;
        } else {
          previewRepeating++;
        }
      }
    }
  });

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white border-2 border-amber-400 rounded-3xl p-6 max-w-2xl w-full shadow-2xl space-y-5 my-6 dir-rtl">
        
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b pb-3">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-amber-500 text-white rounded-2xl shadow-md">
              <Award className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-lg font-black text-slate-900">
                نظام الترفيع والترحيل الوزاري التلقائي للعام الدراسي الجديد
              </h3>
              <p className="text-xs text-slate-500 font-bold">
                تطبيق القواعد الوزارية الصارمة للناجحين، الخريجين، المفصولين، والمنقولين للمسائي
              </p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 rounded-xl bg-slate-100 hover:bg-rose-100 text-slate-500 hover:text-rose-600 transition-colors cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Configuration Options */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          
          <div className="p-3.5 bg-amber-50/70 border-2 border-amber-200 rounded-2xl space-y-1.5">
            <label className="block text-xs font-black text-amber-950">المرحلة الدراسية للمدرسة:</label>
            <select
              value={stage}
              onChange={e => setStage(e.target.value as any)}
              className="w-full p-2.5 rounded-xl border border-amber-300 bg-white font-black text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option value="متوسطة">مدرسة متوسطة (الأول، الثاني، الثالث متوسط)</option>
              <option value="إعدادية">مدرسة إعدادية / ثانوية (الرابع، الخامس، السادس)</option>
              <option value="ابتدائية">مدرسة ابتدائية (الأول إلى السادس)</option>
            </select>
          </div>

          <div className="p-3.5 bg-sky-50/70 border-2 border-sky-200 rounded-2xl space-y-1.5">
            <label className="block text-xs font-black text-sky-950">نوع الدوام المدرسي:</label>
            <select
              value={schoolType}
              onChange={e => setSchoolType(e.target.value as any)}
              className="w-full p-2.5 rounded-xl border border-sky-300 bg-white font-black text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="صباحي">مدرسة صباحية (الرسوب سنتين = نقل للمسائي)</option>
              <option value="مسائي">مدرسة مسائية (الرسوب سنة واحدة = فصل نهائي)</option>
            </select>
          </div>

        </div>

        {/* Rules Breakdown */}
        <div className="space-y-2 p-4 bg-slate-50 rounded-2xl border border-slate-200 text-xs leading-relaxed">
          <div className="font-black text-slate-900 text-xs mb-2">📜 ملخص القواعد الوزارية المطبقة:</div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px]">
            
            <div className="flex items-start gap-2 p-2 rounded-xl bg-emerald-50 text-emerald-950 border border-emerald-200">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-black block">الناجحون في الصفوف الانتقالية:</span>
                <span>يُرحلون إلى الصف الأعلى التالي تلقائياً ({previewPromoted} طالب).</span>
              </div>
            </div>

            <div className="flex items-start gap-2 p-2 rounded-xl bg-amber-50 text-amber-950 border border-amber-200">
              <GraduationCap className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-black block">الناجحون في الصفوف المنتهية (الثالث/السادس):</span>
                <span>يُكتب لهم <span className="font-bold underline">"تخرج"</span> ويُنقلون للأرشيف ({previewGraduated} طالب).</span>
              </div>
            </div>

            {schoolType === 'مسائي' ? (
              <div className="flex items-start gap-2 p-2 rounded-xl bg-rose-50 text-rose-950 border border-rose-200 col-span-2">
                <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <div>
                  <span className="font-black block">الراسبون في المدارس المسائية:</span>
                  <span>يُكتب لهم <span className="font-bold underline">"فصل"</span> ويُحذفون من المستمرين للأرشيف ({previewSeparated} طالب).</span>
                </div>
              </div>
            ) : (
              <>
                <div className="flex items-start gap-2 p-2 rounded-xl bg-purple-50 text-purple-950 border border-purple-200">
                  <UserMinus className="w-4 h-4 text-purple-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-black block">الراسبون سنتين متتاليتين:</span>
                    <span>يُكتب لهم <span className="font-bold underline">"نقل إلى المسائي"</span> ويُنقلون للأرشيف ({previewTransferredToEvening} طالب).</span>
                  </div>
                </div>

                <div className="flex items-start gap-2 p-2 rounded-xl bg-slate-100 text-slate-800 border border-slate-300">
                  <AlertCircle className="w-4 h-4 text-slate-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-black block">الراسبون لسنة واحدة (معيد):</span>
                    <span>يبقون في نفس صفهم لإعادة السنة ({previewRepeating} طالب).</span>
                  </div>
                </div>
              </>
            )}

          </div>

          <div className="pt-2 text-[11px] text-slate-500 font-bold">
            🔒 <span className="font-black text-slate-700">الأرشفة التلقائية:</span> جميع الخريجين والمفصولين والمنقولين للمسائي يُحذفون فوراً من قائمة المستمرين وتُحفظ سجلاتهم كاملة في أيقونة الطلاب السابقين (الخزن المحلي).
          </div>
        </div>

        {/* Modal Actions */}
        <div className="flex items-center justify-between pt-3 border-t">
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-800 text-xs font-black transition-colors cursor-pointer"
          >
            إلغاء
          </button>
          
          <button
            onClick={() => {
              if (confirm('هل أنت متأكد من تنفيذ الترفيع والترحيل لجميع الطلاب وتطبيق قواعد التخرج والفصل والأرشفة؟')) {
                onExecutePromotion(stage, schoolType);
                onClose();
              }
            }}
            className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 text-slate-950 text-xs font-black shadow-lg transition-all cursor-pointer"
          >
            <Award className="w-4 h-4" />
            <span>تنفيذ الترحيل والأرشفة للعام الجديد</span>
          </button>
        </div>

      </div>
    </div>
  );
};
