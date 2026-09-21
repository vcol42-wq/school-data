process.noAsar = true;
const { build, Platform } = require('electron-builder');
const path = require('path');
const fs = require('fs');

async function run() {
  console.log('[STORE BUILDER] Starting Microsoft Store package build (APPX / MSIX)...');
  try {
    const projectDir = path.resolve(__dirname, '..');
    const prepackaged = path.resolve(projectDir, 'dist_electron', 'win-unpacked');

    if (!fs.existsSync(prepackaged)) {
      console.error('[ERROR] Prepackaged directory does not exist:', prepackaged);
      process.exit(1);
    }

    const result = await build({
      targets: Platform.WINDOWS.createTarget(['appx']),
      projectDir: projectDir,
      prepackaged: prepackaged
    });

    console.log('[STORE BUILDER] Successfully built Microsoft Store package(s):');
    result.forEach(art => console.log(' -> ' + art));
  } catch (err) {
    console.error('[STORE BUILDER ERROR]', err);
    process.exit(1);
  }
}

run();
