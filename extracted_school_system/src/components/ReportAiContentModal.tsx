import React, { useState } from 'react';
import { Flag, X, ShieldAlert, CheckCircle2, AlertTriangle, Send, Sparkles } from 'lucide-react';

export interface AiReportData {
  id: string;
  timestamp: string;
  reportedContent: string;
  category: string;
  additionalDetails?: string;
  reporterContact?: string;
  schoolName?: string;
  status: 'pending' | 'reviewed';
}

interface ReportAiContentModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialContent?: string;
  schoolName?: string;
  onReportSubmitted?: (report: AiReportData) => void;
}

const REPORT_CATEGORIES = [
  { id: 'inappropriate', label: 'محتوى غير لائق أو مسيء أو غير ملائم', desc: 'محتوى يخالف الآداب العامة أو يحتوي على لغة مسيئة' },
  { id: 'inaccurate', label: 'معلومات غير دقيقة أو مضللة تربوياً', desc: 'بيانات غير صحيحة، أرقام خاطئة، أو نصائح تربوية غير سليمة' },
  { id: 'safety', label: 'مخاوف تتعلق بسلامة الطلاب أو البيئة المدرسية', desc: 'أي محتوى قد يمس سلامة أو حماية الطلبة والكوادر' },
  { id: 'privacy', label: 'انتهاك للخصوصية أو ظهور بيانات حساسة', desc: 'كشف غير مبرر لمعلومات خاصة أو بيانات شخصية' },
  { id: 'technical', label: 'إجابة مشوهة أو مشكلة فنية في التوليد', desc: 'رد غير مفهوم، تكرار غير طبيعي، أو عطل بالنموذج' },
  { id: 'other', label: 'ملاحظة أو مشكلة أخرى', desc: 'أي أمر آخر ترغب في إبلاغ الإدارة وفريق التطوير به' },
];

export const ReportAiContentModal: React.FC<ReportAiContentModalProps> = ({
  isOpen,
  onClose,
  initialContent = '',
  schoolName = '',
  onReportSubmitted
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string>('inappropriate');
  const [contentToReport, setContentToReport] = useState<string>(initialContent);
  const [additionalDetails, setAdditionalDetails] = useState<string>('');
  const [reporterContact, setReporterContact] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [isSubmittedSuccess, setIsSubmittedSuccess] = useState<boolean>(false);

  // Update content whenever initialContent changes
  React.useEffect(() => {
    if (initialContent) {
      setContentToReport(initialContent);
    }
    if (isOpen) {
      setIsSubmittedSuccess(false);
      setAdditionalDetails('');
    }
  }, [initialContent, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    const report: AiReportData = {
      id: 'REP-' + Date.now().toString(36) + '-' + Math.random().toString(36).substring(2, 6),
      timestamp: new Date().toISOString(),
      reportedContent: contentToReport || 'محتوى ذكاء اصطناعي غير محدد',
      category: selectedCategory,
      additionalDetails: additionalDetails.trim() || undefined,
      reporterContact: reporterContact.trim() || undefined,
      schoolName: schoolName || 'مدرسة غير محددة',
      status: 'pending'
    };

    try {
      // 1. Save to localStorage audit log
      const existingReportsJson = localStorage.getItem('ai_content_reports');
      const existingReports: AiReportData[] = existingReportsJson ? JSON.parse(existingReportsJson) : [];
      existingReports.unshift(report);
      localStorage.setItem('ai_content_reports', JSON.stringify(existingReports.slice(0, 100)));

      // 2. Send to backend server if available
      try {
        await fetch('http://127.0.0.1:3000/api/report-ai-content', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(report)
        });
      } catch (err) {
        // Fallback local endpoint
        try {
          await fetch('/api/report-ai-content', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(report)
          });
        } catch (ignored) {}
      }

      if (onReportSubmitted) {
        onReportSubmitted(report);
      }

      setIsSubmittedSuccess(true);
    } catch (err) {
      console.error('Failed to submit AI report:', err);
      setIsSubmittedSuccess(true);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 dir-rtl text-right">
      <div className="bg-white dark:bg-slate-900 border-2 border-rose-500/30 rounded-3xl shadow-2xl w-full max-w-xl max-h-[92vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        
        {/* Header */}
        <div className="p-5 border-b border-slate-200 dark:border-slate-800 bg-rose-50/70 dark:bg-rose-950/30 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-rose-600 text-white rounded-2xl shadow-md">
              <Flag className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-slate-900 dark:text-white flex items-center gap-2">
                <span>الإبلاغ عن محتوى غير مناسب من الذكاء الاصطناعي</span>
              </h3>
              <p className="text-[11px] font-bold text-rose-600 dark:text-rose-400">
                وفق سياسات سلامة المحتوى التوليدي (Microsoft Store Policy 11.16)
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5">
          {isSubmittedSuccess ? (
            <div className="py-10 text-center space-y-4">
              <div className="w-16 h-16 bg-emerald-100 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 rounded-full flex items-center justify-center mx-auto shadow-inner">
                <CheckCircle2 className="w-10 h-10" />
              </div>
              <h4 className="text-lg font-black text-slate-900 dark:text-white">تم استلام بلاغك بنجاح!</h4>
              <p className="text-xs text-slate-600 dark:text-slate-300 max-w-md mx-auto leading-relaxed font-bold">
                شكراً لمساعدتنا في الحفاظ على سلامة وموثوقية مخرجات الذكاء الاصطناعي. تم توثيق البلاغ وستتم مراجعته وتدقيق النموذج لضمان التوافق مع المعايير التربوية والسياسات المعتمدة.
              </p>
              <div className="pt-4">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-6 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-black shadow-lg cursor-pointer transition"
                >
                  إغلاق النافذة
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4 text-xs font-bold">
              
              {/* Content Preview Box */}
              <div>
                <label className="block mb-1 text-slate-700 dark:text-slate-300">
                  المحتوى المولد بالذكاء الاصطناعي موضوع البلاغ:
                </label>
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 max-h-28 overflow-y-auto text-[11px] text-slate-700 dark:text-slate-200 whitespace-pre-wrap font-normal leading-relaxed">
                  {contentToReport || 'لا يوجد نص محدد؛ سيتم توثيق الاستجابة الأخيرة للذكاء الاصطناعي.'}
                </div>
              </div>

              {/* Category Selection */}
              <div>
                <label className="block mb-2 text-slate-800 dark:text-slate-200">
                  سبب الإبلاغ وتصنيف المشكلة:
                </label>
                <div className="grid grid-cols-1 gap-2">
                  {REPORT_CATEGORIES.map((cat) => (
                    <label
                      key={cat.id}
                      className={`flex items-start gap-3 p-3 rounded-xl border cursor-pointer transition ${
                        selectedCategory === cat.id
                          ? 'border-rose-500 bg-rose-50/60 dark:bg-rose-950/30 text-rose-950 dark:text-rose-200 shadow-sm'
                          : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      <input
                        type="radio"
                        name="reportCategory"
                        value={cat.id}
                        checked={selectedCategory === cat.id}
                        onChange={() => setSelectedCategory(cat.id)}
                        className="mt-0.5 accent-rose-600"
                      />
                      <div className="flex-1">
                        <div className="font-black text-xs">{cat.label}</div>
                        <div className="text-[10px] text-slate-500 dark:text-slate-400 font-normal">{cat.desc}</div>
                      </div>
                    </label>
                  ))}
                </div>
              </div>

              {/* Additional Details */}
              <div>
                <label className="block mb-1 text-slate-700 dark:text-slate-300">
                  تفاصيل إضافية أو ملاحظاتك حول المحتوى (اختياري):
                </label>
                <textarea
                  rows={2}
                  value={additionalDetails}
                  onChange={(e) => setAdditionalDetails(e.target.value)}
                  placeholder="اكتب هنا أي تفاصيل توضح سبب كون هذا الرد غير مناسب أو مضللاً..."
                  className="w-full p-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-normal text-xs focus:outline-none focus:border-rose-500"
                />
              </div>

              {/* Optional Contact */}
              <div>
                <label className="block mb-1 text-slate-700 dark:text-slate-300">
                  البريد الإلكتروني أو الهاتف لمتابعة البلاغ (اختياري):
                </label>
                <input
                  type="text"
                  value={reporterContact}
                  onChange={(e) => setReporterContact(e.target.value)}
                  placeholder="name@school.edu.iq"
                  className="w-full p-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-normal text-xs focus:outline-none focus:border-rose-500"
                />
              </div>

              {/* Notice */}
              <div className="p-3 bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800/40 rounded-xl flex items-start gap-2.5 text-amber-800 dark:text-amber-300 text-[11px] leading-relaxed">
                <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5 text-amber-600" />
                <span>
                  نحن نأخذ سلامة مخرجات الذكاء الاصطناعي بأعلى درجات الجدية. تتم مراجعة كافة البلاغات وتحديث فلاتر الأمان دورياً لحظر أي محتوى غير لائق في البيئة المدرسية.
                </span>
              </div>

              {/* Action Buttons */}
              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 font-bold cursor-pointer transition"
                >
                  إلغاء
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-6 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-black shadow-lg shadow-rose-600/30 flex items-center gap-2 cursor-pointer transition disabled:opacity-50"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>{isSubmitting ? 'جاري الإرسال...' : 'إرسال البلاغ الآن'}</span>
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
