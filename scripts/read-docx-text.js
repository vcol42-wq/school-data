import fs from 'fs';
import path from 'path';

const tempDir = process.env.TEMP || '/tmp';
const docXmlPath = path.join(tempDir, 'docx_unpack', 'word', 'document.xml');
const header2Path = path.join(tempDir, 'docx_unpack', 'word', 'header2.xml');
const header1Path = path.join(tempDir, 'docx_unpack', 'word', 'header1.xml');

console.log('Reading unpacked XML files...');

function extractTextFromXml(xmlContent) {
  // Regex to extract text inside <w:t> tags
  const matches = xmlContent.match(/<w:t[^>]*>(.*?)<\/w:t>/g) || [];
  return matches.map(m => m.replace(/<[^>]+>/g, '')).join(' ');
}

function extractParagraphsFromXml(xmlContent) {
  const pMatches = xmlContent.match(/<w:p[^>]*>.*?<\/w:p>/gs) || [];
  return pMatches.map(p => {
    const tMatches = p.match(/<w:t[^>]*>(.*?)<\/w:t>/g) || [];
    return tMatches.map(m => m.replace(/<[^>]+>/g, '')).join('').trim();
  }).filter(line => line.length > 0);
}

if (fs.existsSync(docXmlPath)) {
  const docXml = fs.readFileSync(docXmlPath, 'utf-8');
  console.log('=== DOCUMENT PARAGRAPHS ===');
  const paragraphs = extractParagraphsFromXml(docXml);
  paragraphs.slice(0, 50).forEach((p, idx) => console.log(`P${idx + 1}:`, p));
}

if (fs.existsSync(header2Path)) {
  const h2Xml = fs.readFileSync(header2Path, 'utf-8');
  console.log('=== HEADER 2 TEXT (Watermark / Headers) ===');
  console.log(extractTextFromXml(h2Xml));
  console.log('=== HEADER 2 PARAGRAPHS ===');
  extractParagraphsFromXml(h2Xml).forEach(p => console.log('H2:', p));
}

if (fs.existsSync(header1Path)) {
  const h1Xml = fs.readFileSync(header1Path, 'utf-8');
  console.log('=== HEADER 1 PARAGRAPHS ===');
  extractParagraphsFromXml(h1Xml).forEach(p => console.log('H1:', p));
}
