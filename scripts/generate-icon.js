import fs from 'fs';
import path from 'path';

// Ensure 'build' directory exists
const buildDir = path.resolve('build');
if (!fs.existsSync(buildDir)) {
  fs.mkdirSync(buildDir, { recursive: true });
}

// Generate SVG icon
const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
  <defs>
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#4c1d95"/>
      <stop offset="50%" stop-color="#1e1b4b"/>
      <stop offset="100%" stop-color="#0f172a"/>
    </linearGradient>
    <linearGradient id="goldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#fef08a"/>
      <stop offset="50%" stop-color="#f59e0b"/>
      <stop offset="100%" stop-color="#b45309"/>
    </linearGradient>
    <filter id="dropShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="4" dy="8" stdDeviation="6" flood-color="#000000" flood-opacity="0.6"/>
    </filter>
  </defs>

  <!-- Squircle Base -->
  <rect x="24" y="24" width="464" height="464" rx="100" fill="url(#bgGrad)" stroke="url(#goldGrad)" stroke-width="16" filter="url(#dropShadow)"/>

  <!-- Ornate Golden Inner Frame -->
  <rect x="48" y="48" width="416" height="416" rx="80" fill="none" stroke="#f59e0b" stroke-width="4" stroke-dasharray="12 8" opacity="0.8"/>

  <!-- Tasselled Graduation Cap at Top -->
  <g transform="translate(256, 140) scale(1.4)">
    <polygon points="0,-45 70,-10 0,25 -70,-10" fill="url(#goldGrad)" stroke="#78350f" stroke-width="3"/>
    <polygon points="-40,-5 0,15 40,-5 0,-25" fill="#fef08a"/>
    <path d="M 45,-2 L 55,35" stroke="#fef08a" stroke-width="4" stroke-linecap="round"/>
    <circle cx="55" cy="40" r="7" fill="#fbbf24"/>
  </g>

  <!-- Large 3D White "P" Letter -->
  <text x="256" y="360" font-family="Arial, sans-serif" font-size="220" font-weight="900" text-anchor="middle" fill="#ffffff" filter="url(#dropShadow)">P</text>

  <!-- Bottom Gold Badge Text -->
  <rect x="112" y="400" width="288" height="48" rx="24" fill="url(#goldGrad)"/>
  <text x="256" y="432" font-family="Arial, sans-serif" font-size="26" font-weight="900" text-anchor="middle" fill="#0f172a">THE PRINCIPAL</text>
</svg>`;

const svgPath = path.join(buildDir, 'icon.svg');
fs.writeFileSync(svgPath, svgContent, 'utf-8');
console.log('Generated build/icon.svg successfully!');
