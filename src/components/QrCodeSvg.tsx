import React from 'react';

interface QrCodeSvgProps {
  value: string;
  size?: number;
  className?: string;
}

export const QrCodeSvg: React.FC<QrCodeSvgProps> = ({ value, size = 150, className = "" }) => {
  // Simple deterministic pseudo-QR generator for visualization
  const grid = Array(21).fill(0).map(() => Array(21).fill(false));

  const addFinder = (r: number, c: number) => {
    for (let i = 0; i < 7; i++) {
      for (let j = 0; j < 7; j++) {
        if (i === 0 || i === 6 || j === 0 || j === 6 || (i >= 2 && i <= 4 && j >= 2 && j <= 4)) {
          grid[r + i][c + j] = true;
        }
      }
    }
  };

  addFinder(0, 0);
  addFinder(0, 14);
  addFinder(14, 0);

  let charIdx = 0;
  for (let r = 0; r < 21; r++) {
    for (let c = 0; c < 21; c++) {
      if ((r < 8 && c < 8) || (r < 8 && c >= 13) || (r >= 13 && c < 8)) continue;
      const seed = (value.charCodeAt(charIdx % value.length) * (r + 3) + c * 11) % 3;
      grid[r][c] = seed === 0 || (r + c) % 2 === 0;
      charIdx++;
    }
  }

  const cellSize = size / 21;

  return (
    <div className={`inline-block bg-white p-3 rounded-2xl border-4 border-slate-900 ${className}`}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="mx-auto block">
        <rect width={size} height={size} fill="#ffffff" />
        {grid.map((row, r) =>
          row.map((cell, c) =>
            cell ? (
              <rect
                key={`${r}-${c}`}
                x={c * cellSize}
                y={r * cellSize}
                width={cellSize + 0.1}
                height={cellSize + 0.1}
                fill="#000000"
              />
            ) : null
          )
        )}
      </svg>
    </div>
  );
};
