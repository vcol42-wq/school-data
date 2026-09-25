const { app, BrowserWindow } = require('electron');
const path = require('path');
const fs = require('fs');

app.whenReady().then(async () => {
  let win = null;
  try {
    const rootDir = path.resolve(__dirname, '..');
    const buildAppxDir = path.join(rootDir, 'build', 'appx');
    if (!fs.existsSync(buildAppxDir)) {
      fs.mkdirSync(buildAppxDir, { recursive: true });
    }

    const logoPath = path.join(rootDir, 'public', 'logo.png');
    const logoBase64 = fs.readFileSync(logoPath).toString('base64');
    const logoDataUrl = `data:image/png;base64,${logoBase64}`;

    win = new BrowserWindow({
      width: 620,
      height: 620,
      useContentSize: true,
      show: false,
      frame: false,
      transparent: false,
      backgroundColor: '#0F172A',
      webPreferences: {
        backgroundThrottling: false,
        offscreen: true
      }
    });

    const assets = [
      {
        name: 'Square44x44Logo.png',
        w: 44,
        h: 44,
        html: `
          <div style="width:44px;height:44px;display:flex;align-items:center;justify-content:center;background:#0F172A;">
            <img src="${logoDataUrl}" style="width:36px;height:36px;object-fit:contain;">
          </div>
        `
      },
      {
        name: 'StoreLogo.png',
        w: 50,
        h: 50,
        html: `
          <div style="width:50px;height:50px;display:flex;align-items:center;justify-content:center;background:#0F172A;">
            <img src="${logoDataUrl}" style="width:40px;height:40px;object-fit:contain;">
          </div>
        `
      },
      {
        name: 'SmallTile.png',
        w: 71,
        h: 71,
        html: `
          <div style="width:71px;height:71px;display:flex;align-items:center;justify-content:center;background:#0F172A;">
            <img src="${logoDataUrl}" style="width:56px;height:56px;object-fit:contain;">
          </div>
        `
      },
      {
        name: 'Square150x150Logo.png',
        w: 300,
        h: 300,
        html: `
          <div style="width:300px;height:300px;display:flex;flex-direction:column;align-items:center;justify-content:center;background:linear-gradient(135deg, #090D16 0%, #0F172A 50%, #1E293B 100%);position:relative;font-family:'Segoe UI', Tahoma, sans-serif;">
            <div style="position:absolute;width:220px;height:220px;background:radial-gradient(circle, rgba(56,189,248,0.2) 0%, transparent 70%);"></div>
            <img src="${logoDataUrl}" style="width:190px;height:190px;object-fit:contain;z-index:2;filter:drop-shadow(0 10px 20px rgba(0,0,0,0.6));">
            <div style="margin-top:12px;color:#F8FAFC;font-weight:700;font-size:16px;letter-spacing:0.5px;z-index:2;text-shadow:0 2px 4px rgba(0,0,0,0.8);">The Principal</div>
          </div>
        `
      },
      {
        name: 'Wide310x150Logo.png',
        w: 620,
        h: 300,
        html: `
          <div style="width:620px;height:300px;display:flex;align-items:center;background:linear-gradient(135deg, #090D16 0%, #0F172A 40%, #1E293B 100%);padding:0 40px;box-sizing:border-box;position:relative;font-family:'Segoe UI', Tahoma, sans-serif;border-left:8px solid #38BDF8;">
            <div style="position:absolute;left:40px;width:240px;height:240px;background:radial-gradient(circle, rgba(56,189,248,0.2) 0%, transparent 70%);"></div>
            <div style="width:190px;height:190px;display:flex;align-items:center;justify-content:center;z-index:2;flex-shrink:0;">
              <img src="${logoDataUrl}" style="width:180px;height:180px;object-fit:contain;filter:drop-shadow(0 10px 20px rgba(0,0,0,0.6));">
            </div>
            <div style="margin-left:36px;display:flex;flex-direction:column;justify-content:center;z-index:2;">
              <div style="display:inline-block;align-self:flex-start;background:rgba(56,189,248,0.15);color:#38BDF8;border:1px solid rgba(56,189,248,0.4);padding:4px 14px;border-radius:999px;font-size:13px;font-weight:700;margin-bottom:10px;">نظام الإدارة المدرسية الموحد</div>
              <div style="font-size:32px;font-weight:800;color:#FFFFFF;margin-bottom:6px;letter-spacing:-0.5px;text-shadow:0 2px 6px rgba(0,0,0,0.5);">The Principal</div>
              <div style="font-size:20px;font-weight:600;color:#94A3B8;direction:rtl;">الإدارة المدرسية الذكية</div>
            </div>
          </div>
        `
      },
      {
        name: 'LargeTile.png',
        w: 620,
        h: 620,
        html: `
          <div style="width:620px;height:620px;display:flex;flex-direction:column;align-items:center;justify-content:center;background:linear-gradient(135deg, #090D16 0%, #0F172A 50%, #1E293B 100%);position:relative;font-family:'Segoe UI', Tahoma, sans-serif;">
            <div style="position:absolute;width:450px;height:450px;background:radial-gradient(circle, rgba(56,189,248,0.22) 0%, transparent 70%);"></div>
            <img src="${logoDataUrl}" style="width:340px;height:340px;object-fit:contain;z-index:2;filter:drop-shadow(0 14px 28px rgba(0,0,0,0.6));">
            <div style="margin-top:24px;color:#FFFFFF;font-weight:800;font-size:36px;z-index:2;letter-spacing:-0.5px;text-shadow:0 2px 6px rgba(0,0,0,0.6);">The Principal</div>
            <div style="margin-top:8px;color:#38BDF8;font-weight:600;font-size:22px;direction:rtl;z-index:2;">الإدارة المدرسية الذكية</div>
          </div>
        `
      },
      {
        name: 'SplashScreen.png',
        w: 620,
        h: 300,
        html: `
          <div style="width:620px;height:300px;display:flex;flex-direction:column;align-items:center;justify-content:center;background:#0F172A;position:relative;font-family:'Segoe UI', Tahoma, sans-serif;">
            <div style="position:absolute;width:300px;height:300px;background:radial-gradient(circle, rgba(56,189,248,0.2) 0%, transparent 70%);"></div>
            <img src="${logoDataUrl}" style="width:130px;height:130px;object-fit:contain;z-index:2;filter:drop-shadow(0 10px 20px rgba(0,0,0,0.6));">
            <div style="margin-top:14px;font-size:22px;font-weight:800;color:#FFFFFF;z-index:2;">The Principal</div>
            <div style="margin-top:4px;font-size:14px;font-weight:600;color:#38BDF8;direction:rtl;z-index:2;">الإدارة المدرسية الذكية</div>
          </div>
        `
      }
    ];

    for (const item of assets) {
      win.setSize(item.w, item.h);
      const fullHtml = `<!DOCTYPE html><html><head><meta charset="utf-8"><style>* { box-sizing: border-box; margin: 0; padding: 0; } body { overflow: hidden; background: #0F172A; }</style></head><body>${item.html}</body></html>`;
      await win.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(fullHtml)}`);
      await new Promise(r => setTimeout(r, 350));
      const img = await win.webContents.capturePage({ x: 0, y: 0, width: item.w, height: item.h });
      const buf = img.toPNG();
      const outPath = path.join(buildAppxDir, item.name);
      fs.writeFileSync(outPath, buf);
      console.log(`[GENERATED] ${item.name} (${item.w}x${item.h}) -> ${buf.length} bytes`);
    }

    console.log('ALL APPX ASSETS CREATED SUCCESSFULLY IN build/appx!');
  } catch (err) {
    console.error('Error in asset generation:', err);
  } finally {
    if (win) win.destroy();
    app.quit();
  }
});
