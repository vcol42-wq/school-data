process.noAsar = true;
const { build, Platform } = require('electron-builder');
const path = require('path');

async function run() {
  console.log('[BUILDER] Starting electron-builder packaging (NSIS Setup Installer)...');
  try {
    const result = await build({
      targets: Platform.WINDOWS.createTarget(['nsis']),
      projectDir: path.resolve(__dirname, '..'),
      prepackaged: path.resolve(__dirname, '..', 'dist_electron', 'win-unpacked')
    });
    console.log('[BUILDER] Packaging completed successfully! Built artifacts:');
    result.forEach(art => console.log(' -> ' + art));
  } catch (err) {
    console.error('[BUILDER ERROR]', err);
    process.exit(1);
  }
}

run();
