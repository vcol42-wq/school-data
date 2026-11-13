const fs = require('fs');
const path = require('path');

const files = [
  'src/components/CloudLiveInspector.tsx',
  'src/components/LicenseModal.tsx',
  'src/components/MainLauncher.tsx',
  'src/components/PrincipalSyncDashboard.tsx',
  'src/components/TopHeader.tsx'
];

const root = path.resolve(__dirname, '..');

for (const rel of files) {
  const full = path.join(root, rel);
  if (fs.existsSync(full)) {
    let content = fs.readFileSync(full, 'utf8');
    content = content.replace(/v6\.5\.0/g, 'v7.0.0');
    content = content.replace(/v6\.5\.1/g, 'v7.0.0');
    content = content.replace(/v6\.5/g, 'v7.0');
    fs.writeFileSync(full, content, 'utf8');
    console.log('Updated version in:', rel);
  }
}
