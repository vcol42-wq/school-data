import fs from 'fs';
import path from 'path';

const icoPath = path.resolve('build/icon.ico');
if (fs.existsSync(icoPath)) {
  fs.unlinkSync(icoPath);
  console.log('Removed invalid icon.ico file from build directory!');
}
