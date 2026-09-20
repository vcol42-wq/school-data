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
  CreditCard, 
  Send,
  PlusCircle
} from 'lucide-react';
import { 
  getMachineFingerprint, 
  getLocalLicense, 
  activateDesktopLicense, 
  createNewLicenseKey, 
  LicenseRecord 
} from '../utils/licenseService';

interface LicenseModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLicenseChanged?: () => void;
}

export const LicenseModal: React.FC<LicenseModalProps> = ({
  isOpen,
  onClose,
  onLicenseChanged
}) => {
  const [activeTab, setActiveTab] = useState<'activate' | 'owner'>('activate');
  const [licenseKeyInput, setLicenseKeyInput] = useState('');
  const [schoolNameInput, setSchoolNameInput] = useState('');
  const [statusMsg, setStatusMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [currentLicense, setCurrentLicense] = useState<LicenseRecord | null>(null);
  const [copiedKey, setCopiedKey] = useState(false);
  const [fingerprint, setFingerprint] = useState('');

  // Owner generator state
  const [newSchoolName, setNewSchoolName] = useState('');
  const [newSchoolPhone, setNewSchoolPhone] = useState('');
  const [newPaymentNotes, setNewPaymentNotes] = useState('زين كاش - تفعيل دائم');
  const [generatedKey, setGeneratedKey] = useState<string | null>(null);

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

  const handleGenerateByOwner = async () => {
    if (!newSchoolName.trim()) {
      setStatusMsg({ type: 'error', text: 'يرجى كتابة اسم المدرسة أولاً.' });
      return;
    }
    setIsLoading(true);
    setStatusMsg(null);

    const res = await createNewLicenseKey(newSchoolName, newSchoolPhone, newPaymentNotes);
    setIsLoading(false);

    if (res.success && res.licenseKey) {
      setGeneratedKey(res.licenseKey);
      setStatusMsg({ type: 'success', text: res.message });
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
          <button 
            onClick={onClose}
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab switcher */}
        <div className="flex bg-slate-950/50 p-1 border-b border-slate-800">
          <button
            onClick={() => { setActiveTab('activate'); setStatusMsg(null); }}
            className={`flex-1 py-2.5 text-xs font-bold rounded-xl transition-all ${
              activeTab === 'activate' 
                ? 'bg-amber-600 text-white shadow-md' 
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            تفعيل الحاسوب والتحقق 💻
          </button>
          <button
            onClick={() => { setActiveTab('owner'); setStatusMsg(null); }}
            className={`flex-1 py-2.5 text-xs font-bold rounded-xl transition-all ${
              activeTab === 'owner' 
                ? 'bg-emerald-600 text-white shadow-md' 
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            لوحة إصدار التراخيص (للمالك) 👑
          </button>
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

          {activeTab === 'activate' ? (
            <>
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

                  {/* Payment instructions Iraq */}
                  <div className="bg-gradient-to-br from-slate-800/70 to-slate-900 border border-slate-700/80 rounded-2xl p-4 space-y-3">
                    <div className="flex items-center gap-2 text-xs font-bold text-amber-300">
                      <CreditCard className="w-4 h-4" />
                      <span>طرق التحويل والسداد المحلي داخل العراق 🇮🇶:</span>
                    </div>
                    <ul className="text-xs text-slate-300 space-y-1 list-disc list-inside">
                      <li>محفظة <strong>زين كاش (ZainCash)</strong></li>
                      <li>بطاقة <strong>ماستر كارد / كي كارد / مصرف الرافدين</strong></li>
                      <li>التحويل المباشر أو النقدي</li>
                    </ul>
                    <button
                      onClick={openWhatsApp}
                      className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-black flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/30 transition-all"
                    >
                      <MessageCircle className="w-4 h-4" />
                      <span>طلب كود التفعيل عبر واتساب 💬</span>
                    </button>
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
            </>
          ) : (
            /* Owner Tab */
            <div className="space-y-4">
              <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl text-xs text-amber-300 font-medium">
                👑 هذه اللوحة مخصصة للمالك لإصدار أكواد تفعيل جديدة للمدارس بعد استلام الحوالة، ليتم تفعيل حاسوبهم لمرة واحدة.
              </div>

              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1">اسم المدرسة المستفيدة:</label>
                  <input
                    type="text"
                    value={newSchoolName}
                    onChange={(e) => setNewSchoolName(e.target.value)}
                    placeholder="مثال: ثانوية المتفوقين للبنين"
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-600"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1">رقم هاتف المشتري / المدير:</label>
                  <input
                    type="text"
                    value={newSchoolPhone}
                    onChange={(e) => setNewSchoolPhone(e.target.value)}
                    placeholder="078XXXXXXXX"
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-600"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1">ملاحظات وطريقة السداد:</label>
                  <input
                    type="text"
                    value={newPaymentNotes}
                    onChange={(e) => setNewPaymentNotes(e.target.value)}
                    placeholder="زين كاش - مدفوع"
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-600"
                  />
                </div>

                <button
                  onClick={handleGenerateByOwner}
                  disabled={isLoading}
                  className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-black flex items-center justify-center gap-2 transition-all disabled:opacity-50"
                >
                  <PlusCircle className="w-4 h-4" />
                  <span>{isLoading ? 'جاري التوليد...' : 'توليد وحفظ كود ترخيص جديد ⚡'}</span>
                </button>
              </div>

              {/* Display Generated Key */}
              {generatedKey && (
                <div className="p-4 bg-emerald-950/60 border border-emerald-500/50 rounded-2xl space-y-3">
                  <div className="text-xs font-bold text-emerald-300">تم توليد كود التفعيل بنجاح للمدرسة:</div>
                  <div className="font-mono text-center text-lg font-black text-amber-300 bg-black/50 p-2.5 rounded-xl border border-emerald-500/30">
                    {generatedKey}
                  </div>
                  <button
                    onClick={() => {
                      const msg = `مرحباً إدارة (${newSchoolName})، تم تفعيل ترخيصكم الدائم لمنظومة The Principal للحاسوب بنجاح.\nكود التفعيل الخاص بكم هو:\n${generatedKey}\n\nنتمنى لكم عاماً دراسياً موفقاً!`;
                      copyToClipboard(msg);
                    }}
                    className="w-full py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-bold flex items-center justify-center gap-2"
                  >
                    {copiedKey ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                    <span>نسخ كود التفعيل مع رسالة التهنئة للواتساب</span>
                  </button>
                </div>
              )}
            </div>
          )}

        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-950/80 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <div className="flex items-center gap-2">
            <Laptop className="w-4 h-4 text-slate-500" />
            <span>The Principal Desktop v6.0 Super Edition</span>
          </div>
          <button 
            onClick={onClose}
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl font-bold transition-colors"
          >
            إغلاق
          </button>
        </div>

      </div>
    </div>
  );
};
