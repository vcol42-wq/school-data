import React, { useState, useEffect } from 'react';
import QRCode from 'qrcode';

interface QrCodeSvgProps {
  value: string;
  size?: number;
  className?: string;
}

export const QrCodeSvg: React.FC<QrCodeSvgProps> = ({ value, size = 150, className = "" }) => {
  const [qrSrc, setQrSrc] = useState<string>('');

  useEffect(() => {
    QRCode.toDataURL(value, {
      width: size,
      margin: 1,
      color: {
        dark: '#000000',
        light: '#ffffff'
      }
    })
      .then(url => setQrSrc(url))
      .catch(err => console.error(err));
  }, [value, size]);

  if (!qrSrc) {
    return (
      <div 
        style={{ width: size, height: size }} 
        className="flex items-center justify-center bg-slate-100 rounded-2xl animate-pulse"
      >
        <span className="text-xs text-slate-400">جاري إنشاء الرمز...</span>
      </div>
    );
  }

  return (
    <div className={`inline-block bg-white p-3 rounded-2xl border-4 border-slate-900 shadow-lg ${className}`}>
      <img 
        src={qrSrc} 
        alt="QR Code" 
        width={size} 
        height={size} 
        className="mx-auto block select-none"
      />
    </div>
  );
};
