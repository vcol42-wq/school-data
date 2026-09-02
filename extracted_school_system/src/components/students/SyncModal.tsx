import React from 'react';
import { Cloud, X, CheckCircle2 } from 'lucide-react';

interface SyncModalProps {
  isOpen: boolean;
  onClose: () => void;
  syncPasscode: string;
  setSyncPasscode: (passcode: string) => void;
  isSyncingCloud: boolean;
  syncSealResult: { sealToken: string; date: string } | null;
  onExecute: () => void;
}

export const SyncModal: React.FC<SyncModalProps> = ({
  isOpen,
  onClose,
  syncPasscode,
  setSyncPasscode,
  isSyncingCloud,
  syncSealResult,
  onExecute
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white border-2 border-emerald-500 rounded-3xl p-6 md:p-8 max-w-md w-full shadow-2xl space-y-4 text-slate-900">

        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
          <div className="flex items-center gap-2 text-emerald-800 font-black">
            <Cloud className="w-5 h-5" />
            <span>المزامنة السحابية وقفل الدرجات</span>
          </div>
          <button onClick={onClose} className="text-slate-600 hover:text-slate-900 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-slate-700 leading-relaxed font-bold">
          عند إدخال رمز دخول المدير والمزامنة مع السحابة وتطبيق الأندرويد، تحفظ الدرجات رسمياً ويتم قفل تعديلات الأستاذ نهائياً لمنع تغيير أي نقطة.
        </p>

        <div className="space-y-3">
          <div>
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
              رمز دخول المدير / رمز الحماية:
            </label>
            <input
              type="password"
              value={syncPasscode}
              onChange={e => setSyncPasscode(e.target.value)}
              placeholder="أدخل رمز المدير لتأكيد القفل"
              className="w-full p-2.5 rounded-xl border bg-white text-xs font-mono font-bold"
            />
          </div>

          {syncSealResult && (
            <div className="p-3 bg-emerald-50 dark:bg-emerald-950/40 rounded-xl border border-emerald-300 text-xs text-emerald-900 dark:text-emerald-200 space-y-1">
              <div className="font-bold flex items-center gap-1">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>تمت المزامنة والقفل بختم رسمي!</span>
              </div>
              <div className="font-mono text-[11px]">رمز الختم: {syncSealResult.sealToken}</div>
            </div>
          )}

          <div className="flex gap-2 pt-2">
            <button
              onClick={onClose}
              className="flex-1 py-2.5 rounded-xl bg-slate-200 dark:bg-slate-800 text-xs font-bold"
            >
              إلغاء
            </button>
            <button
              onClick={onExecute}
              disabled={isSyncingCloud}
              className="flex-1 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-black shadow flex items-center justify-center gap-1"
            >
              {isSyncingCloud ? 'جاري المزامنة...' : 'تأكيد المزامنة والقفل'}
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
