import fs from 'fs';
import path from 'path';

const pngPath = path.resolve('build/icon.png');
const icoPath = path.resolve('build/icon.ico');

if (fs.existsSync(pngPath)) {
  const pngBuf = fs.readFileSync(pngPath);
  
  // Valid 22-byte ICO header wrapper for 256x256 PNG
  const header = Buffer.alloc(22);
  header.writeUInt16LE(0, 0);  // Reserved
  header.writeUInt16LE(1, 2);  // Type = 1 (ICO)
  header.writeUInt16LE(1, 4);  // 1 image

  header.writeUInt8(0, 6);     // Width = 0 (256px)
  header.writeUInt8(0, 7);     // Height = 0 (256px)
  header.writeUInt8(0, 8);     // Colors
  header.writeUInt8(0, 9);     // Reserved
  header.writeUInt16LE(1, 10); // Color Planes
  header.writeUInt16LE(32, 12);// Bits per pixel
  header.writeUInt32LE(pngBuf.length, 14); // Image bytes size
  header.writeUInt32LE(22, 18);            // Image offset

  const icoBuf = Buffer.concat([header, pngBuf]);
  fs.writeFileSync(icoPath, icoBuf);
  console.log('Valid Windows ICO file created successfully at build/icon.ico!');
}
