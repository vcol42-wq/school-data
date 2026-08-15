import fs from 'fs';
import path from 'path';

const srcImgPath = path.resolve('c:/the boss/images/ttt.jpg');
const targetPngPath = path.resolve('public/official_seal.png');
const targetJpgPath = path.resolve('public/official_seal.jpg');
const dataUriTsPath = path.resolve('src/assets/officialSealDataUri.ts');

if (fs.existsSync(srcImgPath)) {
  const buffer = fs.readFileSync(srcImgPath);
  fs.writeFileSync(targetPngPath, buffer);
  fs.writeFileSync(targetJpgPath, buffer);

  const base64Str = buffer.toString('base64');
  const mimeType = 'image/jpeg';
  const dataUri = `data:${mimeType};base64,${base64Str}`;

  const tsContent = `export const OFFICIAL_SEAL_DATA_URI = ${JSON.stringify(dataUri)};\n`;
  
  const assetsDir = path.resolve('src/assets');
  if (!fs.existsSync(assetsDir)) {
    fs.mkdirSync(assetsDir, { recursive: true });
  }
  
  fs.writeFileSync(dataUriTsPath, tsContent, 'utf-8');
  console.log('Successfully created OFFICIAL_SEAL_DATA_URI in src/assets/officialSealDataUri.ts');
} else {
  console.log('srcImgPath not found:', srcImgPath);
}
