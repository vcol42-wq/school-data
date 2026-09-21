import React, { useState } from 'react';
import { 
  Smartphone, 
  X, 
  Copy, 
  Check, 
  ExternalLink, 
  MessageCircle, 
  Download, 
  QrCode, 
  ShieldCheck, 
  Sparkles,
  Layers,
  ArrowRight
} from 'lucide-react';
import { QrCodeSvg } from './QrCodeSvg';

interface AndroidAppModalProps {
  isOpen: boolean;
  onClose: () => void;
  schoolName?: string;
}

export const AndroidAppModal: React.FC<AndroidAppModalProps> = ({
  isOpen,
  onClose,
  schoolName = 'مدرستنا الكريمة'
}) => {
  const [copiedLink, setCopiedLink] = useState(false);
  const [activeLinkTab, setActiveLinkTab] = useState<'internal' | 'store'>('internal');

  if (!isOpen) return null;

  // Google Play URLs
  const internalTestUrl = 'https://play.google.com/apps/internaltest/4701715584919434866';
  const publicStoreUrl = 'https://play.google.com/store/apps/details?id=com.theprincipal.student';

  const currentUrl = activeLinkTab === 'internal' ? internalTestUrl : publicStoreUrl;

  const handleCopy = () => {
    navigator.clipboard.writeText(currentUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2500);
  };

  const handleShareWhatsApp = () => {
    const text = encodeURIComponent(
      `السلام عليكم زملائي الأساتذة في ${schoolName} 🌹\n\nيرجى تحميل وتثبيت تطبيق المدرس المعتمد (The Principal) من متجر Google Play عبر الرابط التالي لمتابعة جدول الحصص ورصد الدرجات والغيابات:\n${currentUrl}\n\nبعد التثبيت، يرجى فتح التطبيق ومسح كود الباركود الخاص بك المستلم من الإدارة.`
    );
    window.open(`https://api.whatsapp.com/send?text=${text}`, '_blank');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200 dir-rtl">
      <div className="relative w-full max-w-2xl bg-white border-4 border-indigo-600 rounded-[2.5rem] shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        
        {/* Header */}
        <div className="relative bg-gradient-to-r from-indigo-800 via-blue-800 to-indigo-950 p-6 text-white flex items-center justify-between border-b-4 border-amber-400">
          <div className="flex items-center gap-3.5">
            <div className="w-13 h-13 rounded-2xl bg-white/15 backdrop-blur-md flex items-center justify-center border border-white/20 shadow-xl">
              <Smartphone className="w-7 h-7 text-amber-300" />
            </div>
            <div>
              <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-emerald-500/25 text-emerald-200 border border-emerald-400/30 text-xs font-black mb-1">
                <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                <span>التطبيق المعتمد على Google Play</span>
              </div>
              <h2 className="text-xl font-black text-white">
                تطبيق المعلم والطالب (Android)
              </h2>
              <p className="text-xs text-blue-200 font-bold">
                تحميل التطبيق على هواتف الأساتذة والطلبة للرصد والمتابعة اللحظية
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
        <div className="p-6 overflow-y-auto space-y-6 flex-1 text-right">

          {/* Links Selector Tabs */}
          <div className="flex bg-slate-100 p-1.5 rounded-2xl gap-2 border border-slate-200">
            <button
              onClick={() => setActiveLinkTab('internal')}
              className={`flex-1 py-2.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                activeLinkTab === 'internal'
                  ? 'bg-indigo-600 text-white shadow-md'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <span>🚀 رابط الاختبار والتوزيع الفوري (المعتمد حالياً)</span>
            </button>
            <button
              onClick={() => setActiveLinkTab('store')}
              className={`flex-1 py-2.5 rounded-xl text-xs font-black transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                activeLinkTab === 'store'
                  ? 'bg-indigo-600 text-white shadow-md'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <span>🌐 رابط المتجر العام الدائم (Google Play)</span>
            </button>
          </div>

          {/* QR Code and Quick Share Card */}
          <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center bg-gradient-to-br from-indigo-50/70 via-blue-50/40 to-slate-50 p-6 rounded-3xl border-2 border-indigo-100">
            
            {/* QR Section */}
            <div className="md:col-span-5 flex flex-col items-center justify-center">
              <div className="bg-white p-4 rounded-3xl shadow-xl border-4 border-indigo-300 ring-8 ring-indigo-50">
                <QrCodeSvg value={currentUrl} size={155} />
              </div>
              <span className="text-[10px] font-black text-slate-500 mt-2.5">
                امسح بكاميرا الهاتف للتحميل المباشر 📷
              </span>
            </div>

            {/* URL and Share Actions */}
            <div className="md:col-span-7 space-y-4">
              <div>
                <label className="text-xs font-black text-slate-700 block mb-1.5">
                  رابط التطبيق المباشر (Google Play):
                </label>
                <div className="bg-white p-3 rounded-2xl border-2 border-slate-200 font-mono text-[11px] text-slate-700 break-all select-all flex items-center justify-between">
                  <span>{currentUrl}</span>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                <button
                  onClick={handleCopy}
                  className="py-3 px-3 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-black flex items-center justify-center gap-1.5 transition-all cursor-pointer active:scale-95 shadow-md"
                >
                  {copiedLink ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-amber-300" />}
                  <span>{copiedLink ? 'تم نسخ الرابط' : 'نسخ الرابط'}</span>
                </button>

                <a
                  href={currentUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="py-3 px-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-black flex items-center justify-center gap-1.5 transition-all cursor-pointer active:scale-95 shadow-md"
                >
                  <ExternalLink className="w-4 h-4 text-amber-300" />
                  <span>فتح في المتجر</span>
                </a>
              </div>

              {/* WhatsApp Share Button */}
              <button
                onClick={handleShareWhatsApp}
                className="w-full py-3.5 px-4 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white rounded-xl text-xs font-black flex items-center justify-center gap-2 transition-all cursor-pointer active:scale-95 shadow-lg shadow-emerald-900/20 border border-emerald-400/40"
              >
                <MessageCircle className="w-4 h-4 text-white" />
                <span>مشاركة رابط التحميل لكروب الأساتذة على WhatsApp 💬</span>
              </button>
            </div>
          </div>

          {/* Quick 3-Step Guide for Teachers */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 space-y-3">
            <h4 className="text-xs font-black text-slate-800 flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-indigo-600" />
              <span>خطوات المعلم بعد تثبيت التطبيق:</span>
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                <span className="w-6 h-6 rounded-full bg-indigo-600 text-white font-black flex items-center justify-center text-[10px] mb-2">1</span>
                <p className="font-bold text-slate-800">تثبيت التطبيق</p>
                <p className="text-[10px] text-slate-500 mt-1">تنزيل وتثبيت التطبيق من متجر Google Play عبر الرابط.</p>
              </div>
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                <span className="w-6 h-6 rounded-full bg-indigo-600 text-white font-black flex items-center justify-center text-[10px] mb-2">2</span>
                <p className="font-bold text-slate-800">مسح الباركود الخاص</p>
                <p className="text-[10px] text-slate-500 mt-1">مسح باركود المعلم المطبوع أو إدخال كود المدرسة ورمز الـ PIN.</p>
              </div>
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                <span className="w-6 h-6 rounded-full bg-emerald-600 text-white font-black flex items-center justify-center text-[10px] mb-2">3</span>
                <p className="font-bold text-slate-800">الرصد الفوري</p>
                <p className="text-[10px] text-slate-500 mt-1">يظهر جدول الأستاذ وصفوفه ومواده مباشرة للبدء برصد الدرجات والغياب.</p>
              </div>
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs font-bold text-slate-600">
          <span className="text-[11px] text-slate-500">
            تطبيق أندرويد متوافق مع نظام Android 8.0 فما فوق ويعمل بوضعي الأونلاين والأوفلاين.
          </span>
          <button
            onClick={onClose}
            className="px-5 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-black transition-colors cursor-pointer"
          >
            إغلاق
          </button>
        </div>

      </div>
    </div>
  );
};
