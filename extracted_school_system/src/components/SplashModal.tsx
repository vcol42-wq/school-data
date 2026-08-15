import React, { useEffect } from 'react';
import { AppConfig } from '../types';
import { Sparkles } from 'lucide-react';
import { AppLogo } from './AppLogo';

interface SplashModalProps {
  config: AppConfig;
  onClose: () => void;
}

export const SplashModal: React.FC<SplashModalProps> = ({ config, onClose }) => {
  // Auto dismiss splash after 3.5 seconds
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, 3500);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div 
      onClick={onClose}
      className="fixed inset-0 z-50 bg-slate-900/40 flex items-center justify-center p-6 cursor-pointer animate-fadeIn"
    >
      <div className="relative max-w-2xl w-full text-center space-y-6 p-8 md:p-10 rounded-3xl bg-white border-4 border-amber-400 shadow-2xl overflow-hidden">
        
        {/* Background Image Layer */}
        {config.splashImageUrl && (
          <div 
            className="absolute inset-0 opacity-15 bg-cover bg-center pointer-events-none"
            style={{ backgroundImage: `url(${config.splashImageUrl})` }}
          ></div>
        )}

        <div className="relative z-10 space-y-4">
          <div className="flex justify-center mb-2">
            <AppLogo size="xl" showText={false} />
          </div>

          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-amber-100 text-amber-950 text-xs font-black border border-amber-300">
            <Sparkles className="w-4 h-4 text-amber-600" />
            <span>جمهورية العراق - وزارة التربية</span>
          </div>

          <h1 className="text-3xl md:text-4xl font-black text-slate-950 leading-tight">
            {config.schoolName}
          </h1>

          <div className="text-sm space-y-1.5 text-slate-900 font-bold">
            <p className="font-black text-sky-900 text-base">{config.directorateName}</p>
            <p className="text-slate-900">إشراف السيد مدير المدرسة المحترم: <span className="text-amber-900 font-black">{config.managerName}</span></p>
          </div>

          <div className="pt-4 border-t border-slate-200 text-xs font-black text-emerald-700 animate-pulse">
            جاري فتح المنظومة المدرسية الموحدة... انقر في أي مكان للدخول المباشر.
          </div>
        </div>

      </div>
    </div>
  );
};
