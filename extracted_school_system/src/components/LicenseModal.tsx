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
  ExternalLink
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
  const [copiedMsg, setCopiedMsg] = useState(false);
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

  const getWhatsAppMessageText = () => {
    const school = schoolNameInput.trim() ? schoolNameInput.trim() : 'مدرستنا الكريمة';
    return `السلام عليكم، أرغب في تفعيل ترخيص الربط السحابي لربط هواتف الأساتذة والطلبة لمنظومة The Principal.\nمعرّف الحاسوب الخاص بنا (Hardware ID): ${fingerprint}\nاسم المدرسة: ${school}`;
  };

  const openWhatsApp = () => {
    const text = encodeURIComponent(getWhatsAppMessageText());
    window.open(`https://api.whatsapp.com/send?text=${text}`, '_blank');
  };

  const copyWhatsAppMessage = () => {
    navigator.clipboard.writeText(getWhatsAppMessageText());
    setCopiedMsg(true);
    setTimeout(() => setCopiedMsg(false), 2500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/90 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl bg-slate-900 border-2 border-slate-700 rounded-3xl shadow-2xl overflow-hidden text-white flex flex-col max-h-[92vh]">
        
        {/* Header */}
        <div className="relative bg-gradient-to-r from-indigo-700 via-blue-800 to-indigo-950 p-6 flex items-center justify-between border-b border-amber-400/40">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-white/15 backdrop-blur-md flex items-center justify-center border border-white/30 shadow-inner">
              <Key className="w-6 h-6 text-amber-300" />
            </div>
            <div>
              <h2 className="text-xl font-black text-white flex items-center gap-2">
                تفعيل ترخيص الربط والمزامنة السحابية
                <Sparkles className="w-4 h-4 text-amber-300" />
              </h2>
              <p className="text-xs text-amber-100 font-bold">
                فتح الباركود والرموز الثلاثية ومزامنة هواتف الأساتذة والطلبة مدى الحياة
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
            <div className={`p-4 rounded-2xl border-2 text-sm font-black flex items-center gap-3 ${
              statusMsg.type === 'success'
                ? 'bg-emerald-950/90 border-emerald-400 text-emerald-200'
                : 'bg-rose-950/90 border-rose-400 text-rose-200'
            }`}>
              {statusMsg.type === 'success' ? <ShieldCheck className="w-6 h-6 shrink-0 text-emerald-300" /> : <X className="w-6 h-6 shrink-0 text-rose-300" />}
              <span>{statusMsg.text}</span>
            </div>
          )}

          {/* Freemium clarification banner */}
          <div className="p-3.5 bg-indigo-950/70 border border-indigo-400/40 rounded-2xl text-xs font-bold text-indigo-200 flex items-center gap-3">
            <span className="p-1.5 bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 rounded-lg shrink-0 font-black text-[10px]">
              مجاني 100%
            </span>
            <p className="leading-relaxed">
              كافة ميزات سطح المكتب (شيتات الإكسل، الشيت الإلكتروني، توليد الجدول المدرسي، الطباعة) متاحة ومجانية بالكامل. هذا التفعيل يفتح الربط السحابي ومزامنة هواتف الأساتذة والطلبة.
            </p>
          </div>

          {/* If Activated Already */}
          {currentLicense?.is_activated ? (
            <div className="bg-emerald-950/50 border-2 border-emerald-400/60 rounded-2xl p-5 space-y-3">
              <div className="flex items-center justify-between">
                <span className="px-3.5 py-1.5 bg-emerald-500/25 text-emerald-200 border border-emerald-400/40 rounded-full text-xs font-black">
                  ✓ نسخة مفعلة بالكامل مدى الحياة
                </span>
                <ShieldCheck className="w-7 h-7 text-emerald-400" />
              </div>
              <div className="text-sm space-y-2 text-slate-100 font-bold">
                <p><span className="text-amber-300">المدرسة المرخصة:</span> {currentLicense.school_name || 'مدرستنا الكريمة'}</p>
                <p><span className="text-amber-300">كود الترخيص:</span> <code className="font-mono text-emerald-300 bg-black/60 px-3 py-1 rounded-lg border border-emerald-500/40">{currentLicense.license_key}</code></p>
                <p className="text-xs text-slate-300"><span className="text-slate-400">بصمة الحاسوب:</span> {currentLicense.machine_fingerprint || fingerprint}</p>
              </div>
            </div>
          ) : (
            <>
              {/* Machine Fingerprint Card */}
              <div className="bg-slate-800/90 border-2 border-slate-600 rounded-2xl p-4 flex items-center justify-between">
                <div>
                  <div className="text-xs text-slate-200 font-black mb-1">معرّف الحاسوب الخاص بكم (Hardware ID):</div>
                  <div className="font-mono text-amber-300 text-base font-black tracking-widest">{fingerprint}</div>
                </div>
                <button
                  onClick={() => copyToClipboard(fingerprint)}
                  className="px-4 py-2 bg-slate-700 hover:bg-slate-600 border border-slate-500 text-white rounded-xl text-xs font-black flex items-center gap-2 transition-all cursor-pointer active:scale-95"
                >
                  {copiedKey ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-amber-300" />}
                  <span>{copiedKey ? 'تم النسخ' : 'نسخ المعرّف'}</span>
                </button>
              </div>

              {/* Exclusive WhatsApp Activation Section - No payment methods */}
              <div className="bg-gradient-to-br from-slate-900 via-slate-950 to-emerald-950/40 border-2 border-emerald-500/50 rounded-2xl p-5 space-y-4 shadow-xl">
                <div className="flex items-center justify-between border-b border-emerald-500/30 pb-3">
                  <div className="flex items-center gap-2.5 text-base font-black text-emerald-300">
                    <MessageCircle className="w-6 h-6 text-emerald-400" />
                    <span>طلب كود التفعيل عبر الواتساب (WhatsApp) حصراً:</span>
                  </div>
                  <span className="text-xs font-black px-3 py-1 bg-emerald-500/20 text-emerald-200 border border-emerald-500/40 rounded-lg">
                    تواصل فوري
                  </span>
                </div>

                <p className="text-xs text-slate-200 font-bold leading-relaxed">
                  يتم استلام كود التفعيل حصراً عبر التواصل المباشر مع الدعم الفني على الواتساب. انقر على الزر الأخضر أدناه لإرسال بصمة حاسوبك فوراً:
                </p>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
                  <button
                    onClick={openWhatsApp}
                    className="py-3.5 px-4 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white rounded-xl text-xs font-black flex items-center justify-center gap-2.5 shadow-lg shadow-emerald-950/60 transition-all cursor-pointer active:scale-95 border border-emerald-400/40"
                  >
                    <MessageCircle className="w-5 h-5 text-emerald-100" />
                    <span>إرسال طلب التفعيل عبر واتساب (WhatsApp) 💬</span>
                  </button>

                  <button
                    onClick={copyWhatsAppMessage}
                    className="py-3.5 px-4 bg-slate-800 hover:bg-slate-700 border border-slate-600 text-slate-100 rounded-xl text-xs font-black flex items-center justify-center gap-2 transition-all cursor-pointer active:scale-95"
                  >
                    {copiedMsg ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-amber-300" />}
                    <span>{copiedMsg ? 'تم نسخ نص الرسالة' : 'نسخ رسالة الطلب يدوياً'}</span>
                  </button>
                </div>

                <div className="pt-2 border-t border-slate-800 flex items-center justify-between text-xs text-slate-300 font-medium">
                  <span>قناة التحديثات الرسمية للمنظومة:</span>
                  <a
                    href="https://whatsapp.com/channel/0029Vb9C7bs0QeaggKCbuI0J"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-emerald-400 hover:text-emerald-300 flex items-center gap-1 font-bold underline"
                  >
                    <span>متابعة القناة الرسمية</span>
                    <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                </div>
              </div>

              {/* Key Entry Form with High-Contrast Colors */}
              <div className="space-y-4 pt-2">
                <div>
                  <label className="block text-sm font-black text-white mb-2">
                    أدخل كود التفعيل المستلم من الواتساب:
                  </label>
                  <input
                    type="text"
                    value={licenseKeyInput}
                    onChange={(e) => setLicenseKeyInput(e.target.value.toUpperCase())}
                    placeholder="XXXX-XXXX-XXXX-XX"
                    className="w-full px-4 py-3.5 bg-slate-950 border-2 border-amber-400/80 rounded-2xl font-mono text-center text-base font-black text-amber-300 placeholder:text-slate-500 focus:outline-none focus:border-amber-300 focus:ring-2 focus:ring-amber-400/50 shadow-inner"
                  />
                </div>

                <div>
                  <label className="block text-xs font-black text-slate-200 mb-1.5">
                    اسم المدرسة (لتثبيته على شهادة الترخيص):
                  </label>
                  <input
                    type="text"
                    value={schoolNameInput}
                    onChange={(e) => setSchoolNameInput(e.target.value)}
                    placeholder="اكتب اسم مدرستكم هنا"
                    className="w-full px-4 py-3 bg-slate-950 border border-slate-600 rounded-2xl text-xs font-bold text-white placeholder:text-slate-500 focus:outline-none focus:border-emerald-400 focus:ring-2 focus:ring-emerald-400/40"
                  />
                </div>

                <button
                  onClick={handleActivate}
                  disabled={isLoading}
                  className="w-full py-4 bg-gradient-to-r from-amber-500 via-amber-600 to-amber-500 hover:from-amber-400 hover:to-amber-500 text-slate-950 rounded-2xl text-sm font-black shadow-xl shadow-amber-950/60 transition-all cursor-pointer active:scale-98 disabled:opacity-50 tracking-wide"
                >
                  {isLoading ? 'جاري التحقق والتفعيل...' : 'تفعيل المنظومة الآن ⚡'}
                </button>
              </div>
            </>
          )}

        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-950 border-t border-slate-800 flex items-center justify-between text-xs text-slate-300 font-bold">
          <div className="flex items-center gap-2">
            <Laptop className="w-4 h-4 text-amber-400" />
            <span className="text-white">The Principal Desktop v6.0 Super Edition</span>
          </div>
          {!forceLock && (
            <button 
              onClick={onClose}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-xl font-bold transition-colors cursor-pointer border border-slate-700"
            >
              إغلاق
            </button>
          )}
          {forceLock && (
            <span className="text-xs text-amber-300 font-black">
              🔒 يتطلب إدخال كود التفعيل المعتمد لفتح المنظومة
            </span>
          )}
        </div>

      </div>
    </div>
  );
};
