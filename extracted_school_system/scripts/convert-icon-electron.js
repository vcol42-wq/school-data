import { app, nativeImage } from 'electron';
import fs from 'fs';
import path from 'path';

app.whenReady().then(() => {
  try {
    const svgPath = path.resolve('build/icon.svg');
    const pngPath = path.resolve('build/icon.png');
    const icoPath = path.resolve('build/icon.ico');

    const image = nativeImage.createFromPath(svgPath);
    const resized = image.resize({ width: 256, height: 256 });
    const pngBuffer = resized.toPNG();
    
    fs.writeFileSync(pngPath, pngBuffer);
    fs.writeFileSync(icoPath, pngBuffer);

    console.log('Successfully created 256x256 high-res build/icon.png and build/icon.ico!');
  } catch (err) {
    console.error('Error converting icon:', err);
  } finally {
    app.quit();
  }
});
