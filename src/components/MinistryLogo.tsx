import React from 'react';

interface MinistryLogoProps {
  className?: string;
  style?: React.CSSProperties;
}

export const MinistryLogo: React.FC<MinistryLogoProps> = ({ className = "w-20 h-20", style }) => {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      viewBox="0 0 500 500" 
      className={className} 
      style={style}
    >
      <defs>
        <linearGradient id="goldOuterLogo" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#fef08a"/>
          <stop offset="30%" stopColor="#eab308"/>
          <stop offset="70%" stopColor="#ca8a04"/>
          <stop offset="100%" stopColor="#854d0e"/>
        </linearGradient>
        <linearGradient id="greenInnerLogo" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#4ade80"/>
          <stop offset="50%" stopColor="#16a34a"/>
          <stop offset="100%" stopColor="#14532d"/>
        </linearGradient>
        <filter id="shadowLogo" x="-10%" y="-10%" width="120%" height="120%">
          <feDropShadow dx="2" dy="4" stdDeviation="4" floodColor="#000000" floodOpacity="0.4"/>
        </filter>
      </defs>

      {/* Outer Black Ring */}
      <circle cx="250" cy="250" r="245" fill="#000000"/>

      {/* Golden Ring Base */}
      <circle cx="250" cy="250" r="240" fill="url(#goldOuterLogo)" filter="url(#shadowLogo)"/>
      <circle cx="250" cy="250" r="236" fill="none" stroke="#78350f" strokeWidth="2"/>

      {/* Top Banner Arc for REPUBLIC OF IRAQ */}
      <path id="topArcLogo" d="M 60 250 A 190 190 0 0 1 440 250" fill="none"/>
      {/* Bottom Banner Arc for MINISTRY OF EDUCATION */}
      <path id="bottomArcLogo" d="M 440 250 A 190 190 0 0 1 60 250" fill="none"/>

      {/* Inner Green Circle Base */}
      <circle cx="250" cy="250" r="185" fill="url(#greenInnerLogo)" stroke="#854d0e" strokeWidth="6"/>
      <circle cx="250" cy="250" r="180" fill="none" stroke="#fef08a" strokeWidth="2" strokeDasharray="6 4"/>

      {/* Top Text: جمهورية العراق / REPUBLIC OF IRAQ */}
      <text fontFamily="Arial, sans-serif" fontWeight="900" fill="#000000" fontSize="26" textAnchor="middle">
        <textPath href="#topArcLogo" startOffset="50%">جمهورية العراق • REPUBLIC OF IRAQ</textPath>
      </text>

      {/* Bottom Text: وزارة التربية / MINISTRY OF EDUCATION */}
      <text fontFamily="Arial, sans-serif" fontWeight="900" fill="#000000" fontSize="24" textAnchor="middle">
        <textPath href="#bottomArcLogo" startOffset="50%">وزارة التربية • MINISTRY OF EDUCATION</textPath>
      </text>

      {/* Central Palm Tree (النخلة المباركة) */}
      <g transform="translate(250, 160) scale(1.1)">
        <path d="M -8 110 L -4 10 Q 0 -10 4 10 L 8 110 Z" fill="#854d0e" stroke="#fef08a" strokeWidth="1.5"/>
        <path d="M -8 110 Q 0 120 8 110 L 12 130 Q 0 140 -12 130 Z" fill="#ca8a04"/>

        <path d="M 0 0 Q -40 -30 -80 -20 Q -40 10 0 0" fill="#15803d" stroke="#fef08a" strokeWidth="1"/>
        <path d="M 0 0 Q 40 -30 80 -20 Q 40 10 0 0" fill="#15803d" stroke="#fef08a" strokeWidth="1"/>
        <path d="M 0 0 Q -60 -50 -100 -50 Q -50 -10 0 0" fill="#166534" stroke="#fef08a" strokeWidth="1"/>
        <path d="M 0 0 Q 60 -50 100 -50 Q 50 -10 0 0" fill="#166534" stroke="#fef08a" strokeWidth="1"/>
        <path d="M 0 0 Q -30 -60 -50 -90 Q -20 -40 0 0" fill="#22c55e" stroke="#fef08a" strokeWidth="1"/>
        <path d="M 0 0 Q 30 -60 50 -90 Q 20 -40 0 0" fill="#22c55e" stroke="#fef08a" strokeWidth="1"/>
        <path d="M 0 0 Q 0 -70 0 -100 Q 10 -50 0 0" fill="#4ade80" stroke="#fef08a" strokeWidth="1"/>
      </g>

      {/* Arabic Calligraphy "اقرأ" & Open Book */}
      <g transform="translate(250, 275) scale(1.1)">
        <path d="M -70 30 Q -35 15 0 35 Q 35 15 70 30 L 60 50 Q 30 35 0 50 Q -30 35 -60 50 Z" fill="#fef08a" stroke="#854d0e" strokeWidth="2"/>
        <text x="0" y="5" fontFamily="'Amiri', 'Tajawal', serif" fontWeight="900" fontSize="62" fill="#fef08a" textAnchor="middle" stroke="#78350f" strokeWidth="2" filter="url(#shadowLogo)">اقرأ</text>
      </g>
    </svg>
  );
};
