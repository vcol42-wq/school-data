import fs from 'fs';
import path from 'path';

const tempDir = process.env.TEMP || '/tmp';
const docXmlPath = path.join(tempDir, 'docx_unpack', 'word', 'document.xml');

if (fs.existsSync(docXmlPath)) {
  const docXml = fs.readFileSync(docXmlPath, 'utf-8');
  
  // Extract all paragraphs
  const pMatches = docXml.match(/<w:p[^>]*>.*?<\/w:p>/gs) || [];
  console.log(`TOTAL PARAGRAPHS: ${pMatches.length}`);

  pMatches.forEach((p, idx) => {
    const tMatches = p.match(/<w:t[^>]*>(.*?)<\/w:t>/g) || [];
    const text = tMatches.map(m => m.replace(/<[^>]+>/g, '')).join('').trim();
    if (text.length > 0) {
      console.log(`[P${idx + 1}]`, text);
    }
  });
}
