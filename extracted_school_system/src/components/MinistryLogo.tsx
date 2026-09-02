import React from 'react';

interface MinistryLogoProps {
  className?: string;
  style?: React.CSSProperties;
}

export const MinistryLogo: React.FC<MinistryLogoProps> = ({ className = "w-24 h-24", style }) => {
  return (
    <img 
      src="./iraq_education_logo.svg"
      alt="شعار وزارة التربية العراقية الذهبي"
      className={`ministry-logo ${className} object-contain`}
      style={{ maxWidth: '140px', maxHeight: '140px', objectFit: 'contain', ...style }}
      onError={(e) => {
        const target = e.currentTarget as HTMLImageElement;
        if (!target.src.endsWith('iraq_education_logo.png')) {
          target.src = './iraq_education_logo.png';
        }
      }}
    />
  );
};
