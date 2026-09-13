import React, { useState, useEffect } from 'react';
import { 
  Megaphone, 
  Send, 
  X, 
  Trash2, 
  RefreshCw, 
  Users, 
  GraduationCap, 
  Globe, 
  CheckCircle2, 
  AlertCircle,
  Clock,
  Sparkles
} from 'lucide-react';
import { sendDirective, getDirectivesList, deleteDirective } from '../utils/syncService';

interface DirectivesModalProps {
  isOpen: boolean;
  onClose: () => void;
  schoolId: string;
}

export const DirectivesModal: React.FC<DirectivesModalProps> = ({
  isOpen,
  onClose,
  schoolId
}) => {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [targetRole, setTargetRole] = useState<'all' | 'teachers' | 'students'>('all');
  const [isSending, setIsSending] = useState(false);
  const [statusMsg, setStatusMsg] = useState<{ success: boolean; text: string } | null>(null);

  const [directivesList, setDirectivesList] = useState<any[]>([]);
  const [isLoadingList, setIsLoadingList] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const fetchHistory = async () => {
    setIsLoadingList(true);
    try {
      const data = await getDirectivesList(schoolId);
      setDirectivesList(data);
    } catch (e) {
      console.error(e);
    } finally {
      setIsLoadingList(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchHistory();
      setStatusMsg(null);
    }
  }, [isOpen]);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setStatusMsg({ success: false, text: 'يرجى كتابة عنوان التوجيه ونص الرسالة' });
      return;
    }

    setIsSending(true);
    setStatusMsg(null);

    const res = await sendDirective(schoolId, title, content, targetRole);
    setIsSending(false);

    if (res.success) {
      setStatusMsg({ success: true, text: res.message });
      setTitle('');
      setContent('');
      fetchHistory();
      setTimeout(() => setStatusMsg(null), 4000);
    } else {
      setStatusMsg({ success: false, text: res.message });
    }
  };

  const handleDelete = async (directiveId: string) => {
    if (!confirm('هل تريد حذف هذا التوجيه من السحابة وهواتف الكادر والطلاب؟')) return;
    setDeletingId(directiveId);
    const ok = await deleteDirective(schoolId, directiveId);
    setDeletingId(null);
    if (ok) {
      setDirectivesList(prev => prev.filter(d => d.id !== directiveId));
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-2xl overflow-hidden flex flex-col max-h-[90vh]">
        
        {/* Header */}
        <div className="bg-gradient-to-r from-amber-600 via-orange-600 to-amber-700 text-white p-5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-white/20 backdrop-blur-md shadow-inner">
              <Megaphone className="w-6 h-6 text-white animate-bounce" />
            </div>
            <div>
              <h3 className="text-xl font-black tracking-tight">بث التوجيهات والتعاميم الإدارية 📢</h3>
              <p className="text-xs text-amber-100 font-medium mt-0.5">
                إرسال تعليمات وتنبيهات المدير المباشرة لهواتف الكادر والطلاب عبر السحابة
              </p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          
          {/* Status Message Alert */}
          {statusMsg && (
            <div className={`p-4 rounded-xl flex items-center gap-3 text-sm font-bold animate-in zoom-in-95 ${
              statusMsg.success 
                ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' 
                : 'bg-rose-50 text-rose-800 border border-rose-200'
            }`}>
              {statusMsg.success ? (
                <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
              ) : (
                <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
              )}
              <span>{statusMsg.text}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSend} className="space-y-4 bg-slate-50 p-4 rounded-xl border border-slate-200">
            <div className="flex items-center justify-between">
              <span className="text-sm font-black text-slate-800 flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-amber-500" />
                إنشاء توجيه أو تعميم جديد
              </span>

              {/* Target Audience Selector */}
              <div className="flex items-center bg-white p-1 rounded-lg border border-slate-200 text-xs font-bold">
                <button
                  type="button"
                  onClick={() => setTargetRole('all')}
                  className={`px-3 py-1 rounded-md transition-all flex items-center gap-1.5 ${
                    targetRole === 'all' 
                      ? 'bg-amber-600 text-white shadow-sm' 
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  <Globe className="w-3.5 h-3.5" />
                  <span>الكل</span>
                </button>
                <button
                  type="button"
                  onClick={() => setTargetRole('teachers')}
                  className={`px-3 py-1 rounded-md transition-all flex items-center gap-1.5 ${
                    targetRole === 'teachers' 
                      ? 'bg-indigo-600 text-white shadow-sm' 
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  <Users className="w-3.5 h-3.5" />
                  <span>الكادر</span>
                </button>
                <button
                  type="button"
                  onClick={() => setTargetRole('students')}
                  className={`px-3 py-1 rounded-md transition-all flex items-center gap-1.5 ${
                    targetRole === 'students' 
                      ? 'bg-emerald-600 text-white shadow-sm' 
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  <GraduationCap className="w-3.5 h-3.5" />
                  <span>الطلاب</span>
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                عنوان التوجيه / الموضوع:
              </label>
              <input
                type="text"
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="مثال: اجتماع طارئ للهيئة التدريسية / مواعيد الامتحانات الشهرية"
                className="w-full px-3.5 py-2.5 rounded-lg border border-slate-300 text-sm font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500 bg-white"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                نص التوجيه والتعليمات:
              </label>
              <textarea
                value={content}
                onChange={e => setContent(e.target.value)}
                placeholder="اكتب التعليمات أو التوجيه الصادر من إدارة المدرسة هنا..."
                rows={3}
                className="w-full px-3.5 py-2.5 rounded-lg border border-slate-300 text-sm font-medium text-slate-900 focus:outline-none focus:ring-2 focus:ring-amber-500 bg-white"
                required
              />
            </div>

            <div className="flex items-center justify-between pt-1">
              <span className="text-[11px] text-slate-500 font-semibold">
                * سيتم إرسال إشعار فوري برنين واهتزاز على هواتف الفئة المحددة فوراً.
              </span>
              <button
                type="submit"
                disabled={isSending}
                className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-amber-600 to-orange-600 hover:from-amber-700 hover:to-orange-700 text-white font-black text-sm shadow-md hover:shadow-lg transition-all flex items-center gap-2 disabled:opacity-50"
              >
                {isSending ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>جاري البث...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>بث التوجيه الآن 🚀</span>
                  </>
                )}
              </button>
            </div>
          </form>

          {/* History List */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-black text-slate-800 flex items-center gap-2">
                <Clock className="w-4 h-4 text-slate-500" />
                <span>سجل التوجيهات والتعاميم الصادرة ({directivesList.length})</span>
              </h4>
              <button
                type="button"
                onClick={fetchHistory}
                disabled={isLoadingList}
                className="text-xs text-amber-700 hover:text-amber-800 font-bold flex items-center gap-1 transition-colors"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isLoadingList ? 'animate-spin' : ''}`} />
                <span>تحديث السجل</span>
              </button>
            </div>

            {isLoadingList ? (
              <div className="p-8 text-center text-slate-400 text-xs font-bold">
                جاري جلب التعاميم من السحابة...
              </div>
            ) : directivesList.length === 0 ? (
              <div className="p-8 text-center text-slate-400 text-xs font-bold bg-slate-50 rounded-xl border border-dashed border-slate-200">
                لا توجد تعاميم إدارية مرسلة حتى الآن. يمكنك بث أول توجيه أعلاه!
              </div>
            ) : (
              <div className="space-y-2.5 max-h-60 overflow-y-auto pr-1">
                {directivesList.map(item => (
                  <div 
                    key={item.id}
                    className="p-3.5 rounded-xl border border-slate-200 bg-white shadow-sm flex items-start justify-between gap-3 hover:border-amber-300 transition-colors"
                  >
                    <div className="space-y-1 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-xs font-black text-slate-900">{item.title}</span>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          item.target_role === 'teachers' 
                            ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                            : item.target_role === 'students'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-amber-50 text-amber-700 border border-amber-200'
                        }`}>
                          {item.target_role === 'teachers' ? '👨‍🏫 الكادر فقط' : item.target_role === 'students' ? '🎓 الطلاب فقط' : '🌐 موجه للجميع'}
                        </span>
                        {item.created_at && (
                          <span className="text-[10px] text-slate-400 font-medium">
                            {new Date(item.created_at).toLocaleDateString('ar-IQ')} {new Date(item.created_at).toLocaleTimeString('ar-IQ', { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-600 font-medium leading-relaxed">
                        {item.content}
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => handleDelete(item.id)}
                      disabled={deletingId === item.id}
                      title="حذف من السحابة"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors shrink-0"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

        </div>

        {/* Footer */}
        <div className="bg-slate-50 px-6 py-3.5 border-t border-slate-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-800 text-xs font-bold transition-colors"
          >
            إغلاق
          </button>
        </div>

      </div>
    </div>
  );
};
