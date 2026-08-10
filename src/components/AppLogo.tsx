import React from 'react';

interface AppLogoProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  showText?: boolean;
  className?: string;
}

export const AppLogo: React.FC<AppLogoProps> = ({
  size = 'md',
  showText = true,
  className = ''
}) => {
  // Dimensions mapping
  const sizeClasses = {
    sm: 'w-8 h-8 text-sm',
    md: 'w-10 h-10 text-base',
    lg: 'w-14 h-14 text-xl',
    xl: 'w-24 h-24 text-3xl'
  };

  const capSizes = {
    sm: 'w-5 h-5 -top-2 -left-1',
    md: 'w-7 h-7 -top-2.5 -left-1.5',
    lg: 'w-10 h-10 -top-4 -left-2',
    xl: 'w-16 h-16 -top-6 -left-3'
  };

  return (
    <div className={`flex items-center gap-3 ${className}`}>
      {/* Ultra-Sharp Vector Icon Box (Gradient Squircle + 3D Letter P + Graduation Cap) */}
      <div 
        className={`relative ${sizeClasses[size]} rounded-[24%] bg-gradient-to-b from-blue-600 via-indigo-600 to-purple-900 text-white flex flex-col items-center justify-center font-bold shadow-lg shrink-0 border-2 border-amber-300/90 select-none overflow-visible group`}
        style={{
          boxShadow: '0 6px 16px -2px rgba(124, 58, 237, 0.5), 0 0 0 1px rgba(251, 191, 36, 0.4)'
        }}
      >
        {/* White Letter P in center */}
        <span className="font-black tracking-tighter leading-none font-sans text-white drop-shadow-md transform scale-110 -mt-1">
          P
        </span>

        {/* Graduation Cap perched gracefully on top of P */}
        <div className={`absolute ${capSizes[size]} drop-shadow-xl pointer-events-none z-10`}>
          <svg viewBox="0 0 100 100" className="w-full h-full">
            {/* Diamond top surface of mortarboard */}
            <polygon points="50,15 90,32 50,49 10,32" fill="#f8fafc" stroke="#1e293b" strokeWidth="4" />
            <polygon points="50,18 85,32 50,46 15,32" fill="#ffffff" />
            {/* Skullcap underneath */}
            <path d="M28,38 L28,58 C28,68 72,68 72,58 L72,38" fill="#e2e8f0" stroke="#1e293b" strokeWidth="4" />
            {/* Tassel button */}
            <circle cx="50" cy="32" r="4" fill="#fbbf24" />
            {/* Tassel hanging down to the right */}
            <path d="M50,32 C62,35 72,42 75,58" fill="none" stroke="#fbbf24" strokeWidth="3.5" strokeDasharray="2 1" />
            <polygon points="73,58 77,58 78,72 72,72" fill="#d97706" />
          </svg>
        </div>

        {/* White "Principal" subtext inside the squircle at bottom */}
        <span className="text-[7px] sm:text-[8px] font-black text-white/95 tracking-wide -mt-0.5 leading-none font-sans">
          Principal
        </span>
      </div>

      {/* App Title Text beside icon */}
      {showText && (
        <div className="flex flex-col text-right leading-tight">
          <div className="flex items-center gap-1.5">
            <span className="font-black text-amber-300 text-base md:text-lg tracking-wide drop-shadow-md font-sans">
              The Principal
            </span>
            <span className="text-[10px] font-black px-1.5 py-0.5 rounded bg-amber-400 text-slate-950 shadow-xs">
              المُدير
            </span>
          </div>
          <span className="text-[11px] text-purple-200 font-bold tracking-normal hidden sm:inline">
            نظام الإدارة المدرسية الموحد
          </span>
        </div>
      )}
    </div>
  );
};
