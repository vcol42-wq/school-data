import fs from 'fs';

const imgPath = 'c:/the boss/extracted_school_system/src/assets/official_seal.png';
const outPath = 'c:/the boss/extracted_school_system/src/assets/officialSealDataUri.ts';

const buf = fs.readFileSync(imgPath);
const b64 = buf.toString('base64');
const content = `export const OFFICIAL_SEAL_DATA_URI = "data:image/png;base64,${b64}";\n`;

fs.writeFileSync(outPath, content, 'utf8');
console.log('Successfully updated OFFICIAL_SEAL_DATA_URI with new gold seal logo!');
