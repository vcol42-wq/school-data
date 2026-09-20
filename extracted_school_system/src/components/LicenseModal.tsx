import React, { useState, useEffect } from 'react';
import { 
  Key, 
  ShieldCheck, 
  Copy, 
  Check, 
  Sparkles, 
  X, 
  Laptop, 
  MessageCircle, 
  CreditCard 
} from 'lucide-react';
import { 
  getMachineFingerprint, 
  getLocalLicense, 
  activateDesktopLicense, 
  LicenseRecord 
} from '../utils/licenseService';

interface LicenseModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLicenseChanged?: () => void;
  forceLock?: boolean;
}

export const LicenseModal: React.FC<LicenseModalProps> = ({
  isOpen,
  onClose,
  onLicenseChanged,
  forceLock = false
}) => {
  const [licenseKeyInput, setLicenseKeyInput] = useState('');
  const [schoolNameInput, setSchoolNameInput] = useState('');
  const [statusMsg, setStatusMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [currentLicense, setCurrentLicense] = useState<LicenseRecord | null>(null);
  const [copiedKey, setCopiedKey] = useState(false);
  const [fingerprint, setFingerprint] = useState('');

  useEffect(() => {
    if (isOpen) {
      setFingerprint(getMachineFingerprint());
      setCurrentLicense(getLocalLicense());
      setStatusMsg(null);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleActivate = async () => {
    if (!licenseKeyInput.trim()) {
      setStatusMsg({ type: 'error', text: 'يرجى إدخال كود التفعيل أولاً.' });
      return;
    }
    setIsLoading(true);
    setStatusMsg(null);

    const res = await activateDesktopLicense(licenseKeyInput, schoolNameInput);
    setIsLoading(false);

    if (res.success) {
      setStatusMsg({ type: 'success', text: res.message });
      setCurrentLicense(res.license || getLocalLicense());
      if (onLicenseChanged) onLicenseChanged();
    } else {
      setStatusMsg({ type: 'error', text: res.message });
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(true);
    setTimeout(() => setCopiedKey(false), 2500);
  };

  const openWhatsApp = () => {
    const text = encodeURIComponent(
      `السلام عليكم، أرغب في تفعيل ترخيص منظومة The Principal للحاسوب لمرة واحدة لمدرستنا.\nمعرّف الحاسوب الخاص بنا: ${fingerprint}`
    );
    window.open(`https://api.whatsapp.com/send?text=${text}`, '_blank');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl bg-slate-900 border border-slate-700/80 rounded-3xl shadow-2xl overflow-hidden text-slate-100 flex flex-col max-h-[90vh]">
        
        {/* Header */}
        <div className="relative bg-gradient-to-r from-amber-600 via-amber-700 to-emerald-800 p-6 flex items-center justify-between border-b border-amber-500/30">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-white/10 backdrop-blur-md flex items-center justify-center border border-white/20 shadow-inner">
              <Key className="w-6 h-6 text-amber-300" />
            </div>
            <div>
              <h2 className="text-xl font-black text-white flex items-center gap-2">
                تفعيل منظومة The Principal للحاسوب
                <Sparkles className="w-4 h-4 text-amber-300" />
              </h2>
              <p className="text-xs text-amber-200/90 font-medium">
                ترخيص تفعيل لمرة واحدة مدى الحياة (Lifetime One-Time Activation)
              </p>
            </div>
          </div>
          {!forceLock && (
            <button 
              onClick={onClose}
              className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-colors cursor-pointer"
              title="إغلاق"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Body content */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1 text-right" dir="rtl">
          
          {/* Status Message */}
          {statusMsg && (
            <div className={`p-4 rounded-2xl border text-xs font-bold flex items-center gap-3 ${
              statusMsg.type === 'success'
                ? 'bg-emerald-950/60 border-emerald-500/50 text-emerald-300'
                : 'bg-rose-950/60 border-rose-500/50 text-rose-300'
            }`}>
              {statusMsg.type === 'success' ? <ShieldCheck className="w-5 h-5 shrink-0" /> : <X className="w-5 h-5 shrink-0" />}
              <span>{statusMsg.text}</span>
            </div>
          )}

          {/* If Activated Already */}
          {currentLicense?.is_activated ? (
            <div className="bg-emerald-950/40 border border-emerald-500/40 rounded-2xl p-5 space-y-3">
              <div className="flex items-center justify-between">
                <span className="px-3 py-1 bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 rounded-full text-xs font-black">
                  ✓ نسخة مفعلة بالكامل مدى الحياة
                </span>
                <ShieldCheck className="w-6 h-6 text-emerald-400" />
              </div>
              <div className="text-sm space-y-1 text-slate-300">
                <p><strong className="text-white">المدرسة المرخصة:</strong> {currentLicense.school_name || 'مدرستنا الكريمة'}</p>
                <p><strong className="text-white">كود الترخيص:</strong> <code className="font-mono text-emerald-400 bg-black/40 px-2 py-0.5 rounded">{currentLicense.license_key}</code></p>
                <p className="text-xs text-slate-400"><strong className="text-slate-300">بصمة الحاسوب:</strong> {currentLicense.machine_fingerprint || fingerprint}</p>
              </div>
            </div>
          ) : (
            <>
              {/* Machine Fingerprint Card */}
              <div className="bg-slate-800/60 border border-slate-700/70 rounded-2xl p-4 flex items-center justify-between">
                <div>
                  <div className="text-xs text-slate-400 font-medium">معرّف الحاسوب الخاص بكم (Hardware ID):</div>
                  <div className="font-mono text-amber-300 text-sm font-bold tracking-wider">{fingerprint}</div>
                </div>
                <button
                  onClick={() => copyToClipboard(fingerprint)}
                  className="px-3 py-1.5 bg-slate-700 hover:bg-slate-600 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors"
                >
                  {copiedKey ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>نسخ</span>
                </button>
              </div>

              {/* Payment instructions Iraq & Official Channels */}
              <div className="bg-gradient-to-br from-slate-900 to-slate-950 border border-slate-700/90 rounded-2xl p-5 space-y-4 shadow-xl">
                <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                  <div className="flex items-center gap-2.5 text-sm font-black text-amber-300">
                    <CreditCard className="w-5 h-5 text-amber-400" />
                    <span>طرق الدفع المحلي واستلام كود التفعيل 🇮🇶:</span>
                  </div>
                  <span className="text-[11px] font-black px-2.5 py-1 bg-amber-500/10 text-amber-400 border border-amber-500/30 rounded-lg">
                    تفعيل لمرة واحدة مدى الحياة
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                  <div className="bg-slate-800/70 border border-slate-700/80 rounded-xl p-3 text-center space-y-1">
                    <div className="text-xs font-black text-white">زين كاش (ZainCash)</div>
                    <div className="text-[10px] text-amber-300/90 font-medium">تحويل فوري عبر المحفظة</div>
                  </div>
                  <div className="bg-slate-800/70 border border-slate-700/80 rounded-xl p-3 text-center space-y-1">
                    <div className="text-xs font-black text-white">كي كارد / ماستر كارد</div>
                    <div className="text-[10px] text-teal-300/90 font-medium">سداد إلكتروني مباشر</div>
                  </div>
                  <div className="bg-slate-800/70 border border-slate-700/80 rounded-xl p-3 text-center space-y-1">
                    <div className="text-xs font-black text-white">مصرف الرافدين</div>
                    <div className="text-[10px] text-sky-300/90 font-medium">إيداع وحوالة مصرفية</div>
                  </div>
                </div>

                <div className="bg-amber-950/20 border border-amber-500/30 rounded-xl p-3 text-xs text-amber-200/90 leading-relaxed">
                  💡 <strong>طريقة التفعيل السريعة:</strong> انقر على زر الواتساب بالأسفل لإرسال بصمة جهازك واسم مدرستك، وسيصلك كود التفعيل المعتمد فوراً لإدخاله في الخانة المخصصة أدناه.
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                  <button
                    onClick={openWhatsApp}
                    className="py-3 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white rounded-xl text-xs font-black flex items-center justify-center gap-2 shadow-lg shadow-emerald-950/50 transition-all cursor-pointer active:scale-95"
                  >
                    <MessageCircle className="w-4 h-4" />
                    <span>طلب كود التفعيل الفوري (WhatsApp) 💬</span>
                  </button>

                  <a
                    href="https://whatsapp.com/channel/0029Vb9C7bs0QeaggKCbuI0J"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="py-3 bg-slate-800/90 hover:bg-slate-700 border border-emerald-500/40 text-emerald-300 rounded-xl text-xs font-bold flex items-center justify-center gap-2 transition-all cursor-pointer"
                  >
                    <span>قناة التحديثات الرسمية 📢</span>
                  </a>
                </div>

                <div className="text-[11px] text-slate-400 text-center pt-2 border-t border-slate-800/80 flex items-center justify-center gap-2">
                  <span>البريد الإلكتروني الرسمي للدعم:</span>
                  <a href="mailto:vcol42@gmail.com" className="text-amber-300 hover:underline font-mono font-bold">vcol42@gmail.com</a>
                </div>
              </div>

              {/* Key Entry Form */}
              <div className="space-y-3">
                <label className="block text-xs font-bold text-slate-300">
                  أدخل كود التفعيل المستلم (مثال: BOSS-8492-3321):
                </label>
                <input
                  type="text"
                  value={licenseKeyInput}
                  onChange={(e) => setLicenseKeyInput(e.target.value.toUpperCase())}
                  placeholder="BOSS-XXXX-XXXX-XXXX"
                  className="w-full px-4 py-3 bg-slate-950 border border-slate-700 rounded-xl font-mono text-center text-sm font-bold text-amber-300 placeholder-slate-600 focus:outline-none focus:border-amber-500"
                />

                <input
                  type="text"
                  value={schoolNameInput}
                  onChange={(e) => setSchoolNameInput(e.target.value)}
                  placeholder="اسم المدرسة (اختياري لتثبيته في الترخيص)"
                  className="w-full px-4 py-2.5 bg-slate-950/60 border border-slate-800 rounded-xl text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-amber-500"
                />

                <button
                  onClick={handleActivate}
                  disabled={isLoading}
                  className="w-full py-3 bg-gradient-to-r from-amber-600 to-amber-700 hover:from-amber-500 hover:to-amber-600 text-white rounded-xl text-xs font-black shadow-lg shadow-amber-900/30 transition-all disabled:opacity-50"
                >
                  {isLoading ? 'جاري التحقق والتفعيل...' : 'تفعيل المنظومة الآن ⚡'}
                </button>
              </div>
            </>
          )}

        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-950/80 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <div className="flex items-center gap-2">
            <Laptop className="w-4 h-4 text-amber-400" />
            <span className="font-bold text-slate-300">The Principal Desktop v6.0 Super Edition</span>
          </div>
          {!forceLock && (
            <button 
              onClick={onClose}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl font-bold transition-colors cursor-pointer"
            >
              إغلاق
            </button>
          )}
          {forceLock && (
            <span className="text-[11px] text-amber-400 font-bold">
              🔒 يتطلب إدخال كود التفعيل لفتح المنظومة
            </span>
          )}
        </div>

      </div>
    </div>
  );
};
