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

  const title = options?.title || 'مستند مدرسي رسمية';
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
  clone.style.border = 'none';
  clone.style.margin = '0 auto';
  clone.style.width = '100%';
  clone.style.minHeight = 'auto';

  doc.open();
  doc.write(`
    <!DOCTYPE html>
    <html dir="rtl" lang="ar">
    <head>
      <meta charset="utf-8">
      <title>${title}</title>
      <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Amiri:ital,wght@0,400;0,700;1,400;1,700&family=Tajawal:wght@400;700;900&display=swap">
      <style>
        @page {
          size: A4 ${orientation};
          margin: 8mm;
        }
        @media print {
          html, body {
            background: #ffffff !important;
            color: #0f172a !important;
            -webkit-print-color-adjust: exact !important;
            print-color-adjust: exact !important;
          }
          .no-print {
            display: none !important;
          }
        }
        body {
          font-family: 'Amiri', 'Traditional Arabic', serif;
          direction: rtl;
          margin: 0;
          padding: 0;
          background: #ffffff !important;
          color: #0f172a !important;
          box-sizing: border-box;
        }
        * {
          box-sizing: border-box;
        }
        table {
          width: 100%;
          border-collapse: collapse;
        }
        th, td {
          border-color: #cbd5e1 !important;
          color: #0f172a !important;
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
    }, 2000);
  }, 400);
};
