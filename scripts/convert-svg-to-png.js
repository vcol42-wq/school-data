import { app, BrowserWindow } from 'electron';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

app.whenReady().then(async () => {
  try {
    const rootDir = path.resolve(__dirname, '..');
    const iconSvgPath = path.join(rootDir, 'build', 'icon.svg');
    const ministrySvgPath = path.join(rootDir, 'public', 'iraq_education_logo.svg');

    // Create a hidden window for rendering
    const win = new BrowserWindow({
      width: 512,
      height: 512,
      useContentSize: true,
      show: false,
      frame: false,
      transparent: true,
      webPreferences: {
        backgroundThrottling: false
      }
    });

    // Function to render an SVG and save as PNG
    const renderSvgToPng = async (svgFilePath, size) => {
      win.setSize(size, size);
      await win.loadFile(svgFilePath);
      // Wait a moment for rendering/painting
      await new Promise(resolve => setTimeout(resolve, 500));
      const image = await win.webContents.capturePage();
      return image;
    };

    console.log('Rendering application icon...');
    const appImage = await renderSvgToPng(iconSvgPath, 512);
    
    // Save 512x512 PNG to public/logo.png
    const png512 = appImage.toPNG();
    fs.writeFileSync(path.join(rootDir, 'public', 'logo.png'), png512);

    // Save 256x256 PNG to build/icon.png
    const resizedAppImage = appImage.resize({ width: 256, height: 256 });
    const png256 = resizedAppImage.toPNG();
    fs.writeFileSync(path.join(rootDir, 'build', 'icon.png'), png256);

    // Create a valid Windows ICO file at build/icon.ico
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
    header.writeUInt32LE(png256.length, 14); // Image bytes size
    header.writeUInt32LE(22, 18);            // Image offset

    const icoBuf = Buffer.concat([header, png256]);
    fs.writeFileSync(path.join(rootDir, 'build', 'icon.ico'), icoBuf);

    console.log('Rendering Ministry of Education logo...');
    const ministryImage = await renderSvgToPng(ministrySvgPath, 512);
    const ministryPng = ministryImage.toPNG();
    fs.writeFileSync(path.join(rootDir, 'public', 'iraq_education_logo.png'), ministryPng);

    console.log('All icons generated successfully!');
  } catch (err) {
    console.error('Error generating icons:', err);
  } finally {
    app.quit();
  }
});
