const fs = require('fs');
const path = require('path');
const pkgPath = path.resolve(__dirname, '..', 'package.json');
const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));

pkg.version = '7.0.0';
pkg.description = 'نظام الإدارة المدرسية المتكامل - الإصدار المحدث v7.0.0 Super Edition';
pkg.build.productName = 'The Principal v7.0.0 Super Edition';
pkg.build.appx.artifactName = 'The_Principal_v7.0.0_MicrosoftStore.appx';
pkg.build.nsis.shortcutName = 'The Principal v7.0.0';
pkg.build.nsis.artifactName = 'The_Principal_Setup_v7.0.0.exe';
pkg.build.portable.artifactName = 'The_Principal_Portable_v7.0.0.exe';

fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2), 'utf8');
console.log('Successfully updated package.json to version:', pkg.version);
