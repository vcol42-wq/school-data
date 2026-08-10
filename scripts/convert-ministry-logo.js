import { app, nativeImage } from 'electron';
import fs from 'fs';
import path from 'path';

app.whenReady().then(() => {
  try {
    const svgPath = path.resolve('public/iraq_education_logo.svg');
    const pngPath = path.resolve('public/iraq_education_logo.png');

    const image = nativeImage.createFromPath(svgPath);
    const resized = image.resize({ width: 512, height: 512 });
    const pngBuffer = resized.toPNG();
    
    fs.writeFileSync(pngPath, pngBuffer);
    console.log('Successfully created public/iraq_education_logo.png!');
  } catch (err) {
    console.error('Error converting logo:', err);
  } finally {
    app.quit();
  }
});
