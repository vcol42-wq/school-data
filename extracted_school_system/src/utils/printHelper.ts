export interface PrintOptions {
  title?: string;
  orientation?: 'portrait' | 'landscape';
}

export const printElement = (
  elementIdOrElement: string | HTMLElement,
  options?: PrintOptions
) => {
  const elem = typeof elementIdOrElement === 'string' 
    ? document.getElementById(elementIdOrElement) 
    : elementIdOrElement;

  const title = options?.title || 'مستند مدرسي رسمي';
  const orientation = options?.orientation || 'portrait';

  if (!elem) {
    console.warn('Print target element not found, falling back to window.print()');
    window.print();
    return;
  }

  // Create an invisible iframe for isolated printing
  const existingIframe = document.getElementById('secret-print-iframe');
  if (existingIframe && existingIframe.parentNode) {
    existingIframe.parentNode.removeChild(existingIframe);
  }

  const iframe = document.createElement('iframe');
  iframe.id = 'secret-print-iframe';
  iframe.style.position = 'fixed';
  iframe.style.left = '-9999px';
  iframe.style.top = '-9999px';
  iframe.style.width = '0px';
  iframe.style.height = '0px';
  iframe.style.border = 'none';

  document.body.appendChild(iframe);

  const doc = iframe.contentWindow?.document;
  if (!doc) {
    window.print();
    return;
  }

  // Clone content and remove zoom/transform styles
  const clone = elem.cloneNode(true) as HTMLElement;
  clone.style.transform = 'none';
  clone.style.boxShadow = 'none';
  clone.style.border = '2px solid #0f172a';
  clone.style.padding = '10mm 12mm';
  clone.style.margin = '0 auto';
  clone.style.width = orientation === 'landscape' ? '297mm' : '210mm';
  clone.style.maxWidth = '100%';
  clone.style.minHeight = orientation === 'landscape' ? '210mm' : '297mm';
  clone.style.maxWidth = orientation === 'landscape' ? '297mm' : '210mm';
  clone.style.display = 'flex';
  clone.style.flexDirection = 'column';
  clone.style.justifyContent = 'space-between';
  clone.style.boxSizing = 'border-box';

  // Collect parent style tags & linked CSS stylesheets
  const styleTags = Array.from(document.querySelectorAll('link[rel="stylesheet"], style'))
    .map(node => node.outerHTML)
    .join('\n');

  doc.open();
  doc.write(`
    <!DOCTYPE html>
    <html dir="rtl" lang="ar">
    <head>
      <meta charset="utf-8">
      <title>${title}</title>
      <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Amiri:ital,wght@0,400;0,700;1,400;1,700&family=Tajawal:wght@400;700;900&display=swap">
      ${styleTags}
      <style>
        @page {
          size: A4 ${orientation};
          margin: 0;
        }
        @media print {
          html, body {
            background: #ffffff !important;
            color: #0f172a !important;
            -webkit-print-color-adjust: exact !important;
            print-color-adjust: exact !important;
            width: 100% !important;
            margin: 0 !important;
            padding: 0 !important;
          }
          .no-print, .no-print-modal {
            display: none !important;
          }
          #printable-area-frame, .print-page, .print-page-a4 {
            width: ${orientation === 'landscape' ? '297mm' : '210mm'} !important;
            max-width: ${orientation === 'landscape' ? '297mm' : '210mm'} !important;
            min-height: ${orientation === 'landscape' ? '210mm' : '297mm'} !important;
            margin: 0 auto !important;
            border-radius: 0 !important;
          }
        }
        body {
          font-family: 'Amiri', 'Traditional Arabic', 'Tajawal', serif;
          direction: rtl;
          margin: 0;
          padding: 0;
          background: #ffffff !important;
          color: #0f172a !important;
          box-sizing: border-box;
          -webkit-print-color-adjust: exact !important;
          print-color-adjust: exact !important;
        }
        * {
          box-sizing: border-box;
        }
        img {
          max-width: 100%;
          height: auto;
          object-fit: contain;
        }
        img[alt*="وزارة التربية"], .ministry-logo, .ministry-emblem {
          width: 110px !important;
          height: 110px !important;
          max-width: 110px !important;
          max-height: 110px !important;
          object-fit: contain !important;
        }
        img[alt*="الختم"], .official-seal, .official-stamp {
          width: 80px !important;
          height: 80px !important;
          max-width: 80px !important;
          max-height: 80px !important;
        }
        table {
          width: 100% !important;
          border-collapse: collapse !important;
          page-break-inside: auto;
        }
        tr {
          page-break-inside: avoid;
          page-break-after: auto;
        }
        th, td {
          border: 1px solid #94a3b8 !important;
          color: #0f172a !important;
          padding: 4px 6px !important;
        }
      </style>
    </head>
    <body>
      ${clone.outerHTML}
    </body>
    </html>
  `);
  doc.close();

  setTimeout(() => {
    try {
      iframe.contentWindow?.focus();
      iframe.contentWindow?.print();
    } catch (err) {
      console.error('Print iframe error:', err);
      window.print();
    }
    setTimeout(() => {
      if (document.body.contains(iframe)) {
        document.body.removeChild(iframe);
      }
    }, 2500);
  }, 500);
};
