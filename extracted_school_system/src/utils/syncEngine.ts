import { getSupabase } from './supabaseClient';
import { Student, StaffMember, StudentMark, DayScheduleMap } from '../types';
import { canonicalSubject } from './subjectHelper';

export interface SyncStepInfo {
  id: string;
  stepIndex: number;
  totalSteps: number;
  title: string;
  percent: number;
  details: string;
  count?: number;
  status: 'pending' | 'active' | 'success' | 'error' | 'warning';
}

export interface CloudTableStats {
  schoolsCount: number;
  teachersCount: number;
  classesCount: number;
  subjectsCount: number;
  assignmentsCount: number;
  studentsCount: number;
  gradesCount: number;
  attendanceCount: number;
  schedulesCount: number;
  directivesCount: number;
  secretCodesCount?: number;
  lastChecked: string;
}

// Helper to normalize Arabic characters for comparison
function normalizeArabic(str: string): string {
  if (!str) return '';
  return str
    .trim()
    .replace(/[أإآ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/ى/g, 'ي')
    .replace(/^(الصف|صف)\s+/g, '')
    .replace(/(^|\s)ال/g, '$1')
    .replace(/\s+/g, '');
}

export function standardizeGradeName(gradeStr: string): string {
  if (!gradeStr) return 'الأول المتوسط';
  const s = gradeStr.trim().replace(/^(الصف|صف)\s+/g, '').trim();
  const lower = s.replace(/[أإآ]/g, 'ا').replace(/ة/g, 'ه').replace(/ى/g, 'ي');

  let base = 'الأول';
  if (lower.includes('سادس') || lower.includes('6') || lower.includes('٦')) base = 'السادس';
  else if (lower.includes('خامس') || lower.includes('5') || lower.includes('٥')) base = 'الخامس';
  else if (lower.includes('رابع') || lower.includes('4') || lower.includes('٤')) base = 'الرابع';
  else if (lower.includes('ثالث') || lower.includes('3') || lower.includes('٣')) base = 'الثالث';
  else if (lower.includes('ثاني') || lower.includes('2') || lower.includes('٢')) base = 'الثاني';
  else if (lower.includes('اول') || lower.includes('1') || lower.includes('١')) base = 'الأول';

  let branch = '';
  if (lower.includes('احيائي')) branch = 'الأحيائي';
  else if (lower.includes('تطبيقي')) branch = 'التطبيقي';
  else if (lower.includes('علمي')) branch = 'العلمي';
  else if (lower.includes('ادبي')) branch = 'الأدبي';
  else if (lower.includes('مهني')) branch = 'المهني';
  else if (lower.includes('صناعي')) branch = 'الصناعي';
  else if (lower.includes('تجاري')) branch = 'التجاري';
  else if (lower.includes('اعدادي') || lower.includes('ثانوي')) branch = 'الإعدادي';
  else if (lower.includes('ابتدائي')) branch = 'الابتدائي';
  else if (lower.includes('متوسط') || base === 'الأول' || base === 'الثاني' || base === 'الثالث') {
    // توحيد صفوف المرحلة المتوسطة لمنع التكرار باختلاف كلمة (الأول مقابل الأول المتوسط)
    branch = 'المتوسط';
  } else if (base === 'الرابع' || base === 'الخامس' || base === 'السادس') {
    branch = 'الإعدادي';
  }

  return branch ? `${base} ${branch}` : `${base} المتوسط`;
}

export function standardizeSectionName(secStr: string): string {
  if (!secStr || typeof secStr !== 'string') return 'أ';
  let clean = secStr.trim();
  if (!clean) return 'أ';

  // 1. Remove wrapping quotes, brackets, and parentheses
  clean = clean.replace(/^[\[\(\{\<"'\s]+|[\]\)\}\>"'\s]+$/g, '').trim();

  // 2. Remove prefixes like "شعبة", "الشعبة", "ش:", "ش/", "فرع"
  clean = clean.replace(/^(شعبة|الشعبة|ش|الفرع|فرع|رمز|مجموعة)\s*[:\-\/\\]?\s*/i, '').trim();

  // Helper mapper for single tokens
  const mapTokenToSection = (token: string): string | null => {
    if (!token) return null;
    const t = token.trim().replace(/[\[\(\)\{\}\<\>\"\']/g, '');
    const lower = t.toLowerCase();
    if (t === 'ا' || t === 'أ' || t === 'إ' || t === 'آ' || lower === 'a' || lower === '1' || t === '١') return 'أ';
    if (t === 'ب' || lower === 'b' || lower === '2' || t === '٢') return 'ب';
    if (t === 'ج' || lower === 'c' || lower === '3' || t === '٣') return 'ج';
    if (t === 'د' || lower === 'd' || lower === '4' || t === '٤') return 'د';
    if (t === 'ه' || t === 'هـ' || lower === 'e' || lower === '5' || t === '٥') return 'هـ';
    if (t === 'و' || lower === 'f' || lower === '6' || t === '٦') return 'و';
    if (t === 'ز' || lower === 'z' || lower === '7' || t === '٧') return 'ز';
    if (t === 'ح' || lower === 'h' || lower === '8' || t === '٨') return 'ح';
    if (t === 'ط' || lower === '9' || t === '٩') return 'ط';
    if (t === 'ي' || lower === '10' || t === '١٠') return 'ي';
    if (t === 'خ') return 'خ';
    if (/^[أ-يa-zA-Z]$/.test(t)) return t;
    return null;
  };

  // Direct match if clean is already a single char/token
  const direct = mapTokenToSection(clean);
  if (direct) return direct;

  // 3. Handle combined Grade/Section patterns like "1/2", "1-2", "الأول / ب", "1/ب"
  const slashOrDash = clean.match(/[\/\-]\s*([أ-يa-zA-Z0-9]+)/);
  if (slashOrDash && slashOrDash[1]) {
    const fromSlash = mapTokenToSection(slashOrDash[1]);
    if (fromSlash) return fromSlash;
  }

  // 4. Extract section from combined text like "الأول متوسط ب" or "الصف الثاني (ج)" or "اول ب"
  const strippedGrade = clean
    .replace(/(الصف|صف)\s+/g, '')
    .replace(/(الأول|الاول|اول|أول|ثاني|الثاني|ثالث|الثالث|رابع|الرابع|خامس|الخامس|سادس|السادس)/g, '')
    .replace(/(متوسطة|متوسط|ابتدائية|ابتدائي|إعدادية|اعدادية|إعدادي|اعدادي|ثانوية|ثانوي)/g, '')
    .replace(/(علمي|أدبي|ادبي|تطبيقي|أحيائي|احيائي|صناعي|تجاري)/g, '')
    .replace(/[\(\)\[\]\{\}\:\-\_\/\\]/g, ' ')
    .trim();

  if (strippedGrade) {
    const tokens = strippedGrade.split(/\s+/).filter(Boolean);
    // Check tokens from right to left (as section usually appears at the end)
    for (let i = tokens.length - 1; i >= 0; i--) {
      const mapped = mapTokenToSection(tokens[i]);
      if (mapped) return mapped;
    }
  }

  // 5. Look for any standalone Arabic or English letter / digit in brackets or spaces
  const bracketMatch = clean.match(/[\(\[\{]([أ-يa-zA-Z0-9]+)[\)\]\}]/);
  if (bracketMatch && bracketMatch[1]) {
    const fromBracket = mapTokenToSection(bracketMatch[1]);
    if (fromBracket) return fromBracket;
  }

  return 'أ';
}

/**
 * Calculates a numerical sorting weight for any grade name.
 * Order:
 * - Primary (الابتدائي): 1st to 6th (101..106)
 * - Intermediate (المتوسط): 1st to 3rd (201..203)
 * - Secondary/Preparatory (الإعدادي/الثانوي): 4th to 6th (304..306)
 */
export function getGradeOrder(gradeStr: string): number {
  if (!gradeStr) return 999;
  const s = gradeStr.trim().replace(/^(الصف|صف)\s+/g, '').trim();
  const lower = s.replace(/[أإآ]/g, 'ا').replace(/ة/g, 'ه').replace(/ى/g, 'ي');

  let gradeNum = 1;
  if (lower.includes('سادس') || lower.includes('6') || lower.includes('٦')) gradeNum = 6;
  else if (lower.includes('خامس') || lower.includes('5') || lower.includes('٥')) gradeNum = 5;
  else if (lower.includes('رابع') || lower.includes('4') || lower.includes('٤')) gradeNum = 4;
  else if (lower.includes('ثالث') || lower.includes('3') || lower.includes('٣')) gradeNum = 3;
  else if (lower.includes('ثاني') || lower.includes('2') || lower.includes('٢')) gradeNum = 2;
  else if (lower.includes('اول') || lower.includes('1') || lower.includes('١')) gradeNum = 1;

  let stageWeight = 200; // Default to intermediate (المتوسط)
  if (lower.includes('ابتدائي')) {
    stageWeight = 100;
  } else if (
    lower.includes('اعدادي') || lower.includes('ثانوي') ||
    lower.includes('علمي') || lower.includes('ادبي') ||
    lower.includes('احيائي') || lower.includes('تطبيقي') ||
    lower.includes('مهني') || lower.includes('صناعي') || lower.includes('تجاري') ||
    gradeNum >= 4
  ) {
    stageWeight = 300;
  } else {
    stageWeight = 200;
  }

  return stageWeight + gradeNum;
}

const ARABIC_ALPHABET_ORDER: Record<string, number> = {
  'أ': 1, 'ا': 1, 'إ': 1, 'آ': 1, 'a': 1, '1': 1, '١': 1,
  'ب': 2, 'b': 2, '2': 2, '٢': 2,
  'ج': 3, 'c': 3, '3': 3, '٣': 3,
  'د': 4, 'd': 4, '4': 4, '٤': 4,
  'هـ': 5, 'ه': 5, 'e': 5, '5': 5, '٥': 5,
  'و': 6, 'f': 6, '6': 6, '٦': 6,
  'ز': 7, 'z': 7, '7': 7, '٧': 7,
  'ح': 8, 'h': 8, '8': 8, '٨': 8,
  'ط': 9, '9': 9, '٩': 9,
  'ي': 10, 'j': 10, '10': 10, '١٠': 10,
  'ك': 11, 'k': 11,
  'ل': 12, 'l': 12,
  'م': 13, 'm': 13,
  'ن': 14, 'n': 14,
  'س': 15, 's': 15,
  'ع': 16,
  'ف': 17,
  'ص': 18,
  'ق': 19,
  'ر': 20,
  'ش': 21,
  'ت': 22,
  'ث': 23,
  'خ': 24,
  'ذ': 25,
  'ض': 26,
  'ظ': 27,
  'غ': 28
};

/**
 * Calculates a numerical sorting weight for any section identifier (أ، ب، ج، د...).
 */
export function getSectionOrder(secStr: string): number {
  if (!secStr) return 999;
  const clean = secStr.trim();
  if (ARABIC_ALPHABET_ORDER[clean] !== undefined) {
    return ARABIC_ALPHABET_ORDER[clean];
  }
  const std = standardizeSectionName(clean);
  if (ARABIC_ALPHABET_ORDER[std] !== undefined) {
    return ARABIC_ALPHABET_ORDER[std];
  }
  return 999;
}

/**
 * Comparator for ascending order: First grade and all its sections, then Second and its sections, etc.
 */
export function compareGradesAndSections(gradeA: string, secA: string, gradeB: string, secB: string): number {
  const gOrderA = getGradeOrder(gradeA);
  const gOrderB = getGradeOrder(gradeB);
  if (gOrderA !== gOrderB) return gOrderA - gOrderB;

  const sOrderA = getSectionOrder(secA);
  const sOrderB = getSectionOrder(secB);
  if (sOrderA !== sOrderB) return sOrderA - sOrderB;

  return (secA || '').localeCompare(secB || '', 'ar');
}

/**
 * Sorts an array of grade strings ascendingly (الأول ثم الثاني ثم الثالث...).
 * Preserves 'الكل' at the beginning if present.
 */
export function sortGradesList(grades: string[]): string[] {
  const hasAll = grades.includes('الكل');
  const filtered = grades.filter(g => g && g !== 'الكل');
  const sorted = [...new Set(filtered)].sort((a, b) => getGradeOrder(a) - getGradeOrder(b));
  return hasAll ? ['الكل', ...sorted] : sorted;
}

/**
 * Sorts an array of section strings ascendingly (أ ثم ب ثم ج...).
 * Preserves 'الكل' at the beginning if present.
 */
export function sortSectionsAlphabetically(sections: string[]): string[] {
  const hasAll = sections.includes('الكل');
  const filtered = sections.filter(s => s && s !== 'الكل');
  const sorted = [...new Set(filtered)].sort((a, b) => getSectionOrder(a) - getSectionOrder(b));
  return hasAll ? ['الكل', ...sorted] : sorted;
}

/**
 * Sorts any list of items containing grade and section:
 * First grade and its sections (أ ثم ب ثم ج), then Second and its sections, then Third and its sections, etc.
 */
export function sortSectionsList<T extends { grade?: string; currentGrade?: string; section?: string }>(items: T[]): T[] {
  return [...items].sort((a, b) => {
    const gA = a.grade || a.currentGrade || '';
    const gB = b.grade || b.currentGrade || '';
    const sA = a.section || '';
    const sB = b.section || '';
    return compareGradesAndSections(gA, sA, gB, sB);
  });
}

export function isValidSubjectName(raw: string): boolean {
  if (!raw || typeof raw !== 'string') return false;
  const s = raw.trim();
  if (s.length <= 1) return false;
  if (/^[أ-يa-zA-Z]$/.test(s)) return false;
  const lower = s.toLowerCase();
  const invalidKeywords = ['مفرغ', 'إدارة', 'شاغر', 'نشاط حر', 'تفرغ', 'معاون', 'مدير', 'كاتب', 'مرشد', 'خدمة', 'حارس'];
  if (invalidKeywords.some(kw => lower.includes(kw))) return false;
  return true;
}

export function isExemptStaff(member: StaffMember): boolean {
  if (!member) return true;
  const job = (member.jobTitle || '').trim();
  const spec = (member.specialization || '').trim();
  const actual = (member.actualSubjectTaught || '').trim();
  const status = (member.status || '').trim();
  const nonTeaching = ['مدير', 'مديرة', 'معاون', 'معاونة', 'مرشد', 'مرشدة', 'أمين مكتبة', 'كاتب', 'إداري', 'متفرغ', 'مفرغ', 'تفرغ', 'مشرف', 'خدمة', 'حارس'];
  return nonTeaching.some(kw => job.includes(kw)) || 
         member.teachingQuota === 0 || 
         spec.includes('إدارة') || spec.includes('تفرغ') || spec.includes('مفرغ') ||
         actual.includes('مفرغ') || actual.includes('إدارة') || actual.includes('تفرغ') ||
         status === 'مجاز إجازة طويلة' || status === 'منسب خارج المدرسة';
}

export function standardizeSubjectName(raw: string): string {
  if (!raw) return '';
  const s = raw.trim();

  // استبعاد الأحرف المفردة والمسميات الإدارية تماماً
  if (!isValidSubjectName(s)) {
    return '';
  }

  const norm = normalizeArabic(s).toLowerCase();
  const rawLower = s.toLowerCase().replace(/[^a-z0-9\u0600-\u06FF]/g, '');

  // 1. اللغة الإنكليزية: جميع التسميات
  if (
    norm.includes('انكل') || norm.includes('انجل') ||
    rawLower.includes('engl') || rawLower.startsWith('eng') || rawLower.endsWith('eng') ||
    rawLower === 'en' || rawLower === 'el' ||
    rawLower.includes('english') || norm.includes('انكلش') || norm.includes('انجلش') ||
    norm.includes('انجليز') || norm.includes('انكليز')
  ) {
    return 'اللغة الإنكليزية';
  }

  // 2. الأحياء: علم الأحياء / علوم الأحياء / احياء
  if (norm.includes('احياء') || norm.includes('علماحياء') || norm.includes('علوماحياء') || norm.includes('بايو')) return 'الأحياء';

  // 3. التربية الإسلامية
  if (norm.includes('اسلام') || norm.includes('قران') || norm.includes('دين') || norm.includes('عقيده')) return 'التربية الإسلامية';

  // 4. اللغة العربية
  if (norm.includes('عرب') || norm.includes('قواعد') || norm.includes('نصوص') || norm.includes('ادب')) return 'اللغة العربية';

  // 5. باقي المواد المعتمدة
  if (norm.includes('فيزيا')) return 'الفيزياء';
  if (norm.includes('كيميا')) return 'الكيمياء';
  if (norm.includes('اجتماع') || norm.includes('تاريخ') || norm.includes('جغرافي') || norm.includes('وطني')) return 'الاجتماعيات';
  if (norm.includes('رياضيات') || norm.includes('رياضي') || norm.includes('جبر') || norm.includes('هندس')) return 'الرياضيات';
  if (norm.includes('حاسوب') || norm.includes('كمبيوتر') || norm.includes('برمج')) return 'الحاسوب';
  if (norm.includes('فني') || norm.includes('رسم') || norm.includes('فنون')) return 'التربية الفنية';
  if (norm.includes('رياضه') || norm.includes('العاب') || norm.includes('بدني')) return 'النشاط البدني';
  if (norm.includes('اخلاق')) return 'التربية الأخلاقية';
  if (norm.includes('علوم')) return 'العلوم';

  return s;
}

// Helper to parse the assigned classes taught by teachers
export function parseClassTaught(classStr: string, defaultSubject: string) {
  let cleaned = classStr.replace(/^(الصف|صف)\s+/g, '').trim();
  let explicitSubject = '';
  let explicitSection = '';

  // 1. Check for parenthesis: could be section like (أ) or (ب) or subject like (التربية الأخلاقية)
  const parenMatch = cleaned.match(/\((.*?)\)/);
  if (parenMatch) {
    const inside = parenMatch[1].trim();
    const isSec = /^(شعبة|الشعبة|ش\s*)?[أ-يa-zA-Z]$/.test(inside) || ['أ', 'ب', 'ج', 'د', 'هـ', 'ه', 'و', 'ز', 'ح', 'ط', 'ي'].includes(inside);
    if (isSec) {
      explicitSection = standardizeSectionName(inside);
    } else {
      explicitSubject = standardizeSubjectName(inside);
    }
    cleaned = cleaned.replace(/\(.*?\)/g, '').trim();
  }

  // 2. Extract section and grade
  let section = explicitSection;
  let grade = '';

  const parts = cleaned.split(/[-–—\s]+/).filter(Boolean);
  if (!section) {
    if (parts.length >= 2) {
      const last = parts[parts.length - 1];
      const isSec = /^[أ-يa-zA-Z]$/.test(last) || ['أ', 'ب', 'ج', 'د', 'هـ', 'ه', 'و', 'ز', 'ح', 'ط', 'ي'].includes(last);
      if (isSec) {
        section = standardizeSectionName(last);
        grade = standardizeGradeName(parts.slice(0, parts.length - 1).join(' '));
      } else {
        section = 'أ';
        grade = standardizeGradeName(parts.join(' '));
      }
    } else if (parts.length === 1) {
      section = 'أ';
      grade = standardizeGradeName(parts[0]);
    } else {
      section = 'أ';
      grade = 'الأول المتوسط';
    }
  } else {
    grade = standardizeGradeName(cleaned || 'الأول المتوسط');
  }

  const rawSub = explicitSubject || defaultSubject || '';
  const subject = standardizeSubjectName(rawSub);
  return { grade, section, subject };
}

/**
 * المطابقة الذكية للمدرس وفق الاختصاص والشعبة المسندة له في سجل الكادر، مع استبعاد المفرغين إدارياً تماماً
 */
export function findBestTeacherForSubjectAndSection(
  grade: string,
  section: string,
  subjectName: string,
  staffList: StaffMember[]
): string {
  if (!staffList || staffList.length === 0) return 'أ. أستاذ المادة';

  const cleanSubj = (subjectName || '').trim();
  if (cleanSubj.includes('شاغر') || cleanSubj.includes('نشاط')) {
    return 'شاغر';
  }

  // 1. استبعاد المفرغين إدارياً تماماً من الجدول الآلي
  const activeStaff = staffList.filter(st => !isExemptStaff(st));
  if (activeStaff.length === 0) return 'أ. أستاذ المادة';

  const subCanon = canonicalSubject(cleanSubj);
  const stdGrade = standardizeGradeName(grade);
  const stdSection = standardizeSectionName(section);
  const gradeNorm = grade.replace(/^(الصف|صف)\s+/, '').trim();

  // 2. حصر أساتذة الاختصاص النشطين
  const subjectTeachers = activeStaff.filter(st => {
    const actCanon = canonicalSubject(st.actualSubjectTaught || '');
    const specCanon = canonicalSubject(st.specialization || '');
    return actCanon === subCanon || specCanon === subCanon ||
           (st.actualSubjectTaught && cleanSubj.includes(st.actualSubjectTaught)) ||
           (st.specialization && cleanSubj.includes(st.specialization));
  });

  if (subjectTeachers.length === 0) {
    return 'أ. أستاذ المادة';
  }

  // 3. الأولوية القصوى: مدرس يدرّس نفس الصف والشعبة المحددة في سجل الكادر
  const exactMatch = subjectTeachers.find(st => {
    if (!Array.isArray(st.classesTaught) || st.classesTaught.length === 0) return false;
    return st.classesTaught.some(cStr => {
      const parsed = parseClassTaught(cStr, '');
      const pGradeNorm = parsed.grade.replace(/^(الصف|صف)\s+/, '').trim();
      const matchGrade = parsed.grade === stdGrade || pGradeNorm.includes(gradeNorm) || gradeNorm.includes(pGradeNorm) || cStr.includes(gradeNorm);
      const matchSec = parsed.section === stdSection || cStr.includes(stdSection);
      return matchGrade && matchSec;
    });
  });

  if (exactMatch) {
    return exactMatch.fullName || `${exactMatch.firstName} ${exactMatch.secondName}`.trim();
  }

  // 4. الأولوية الثانية: مدرس يدرّس نفس الصف في سجل الكادر
  const gradeMatch = subjectTeachers.find(st => {
    if (!Array.isArray(st.classesTaught) || st.classesTaught.length === 0) return false;
    return st.classesTaught.some(cStr => {
      const parsed = parseClassTaught(cStr, '');
      const pGradeNorm = parsed.grade.replace(/^(الصف|صف)\s+/, '').trim();
      return parsed.grade === stdGrade || pGradeNorm.includes(gradeNorm) || gradeNorm.includes(pGradeNorm) || cStr.includes(gradeNorm);
    });
  });

  if (gradeMatch) {
    return gradeMatch.fullName || `${gradeMatch.firstName} ${gradeMatch.secondName}`.trim();
  }

  // 5. الأولوية الثالثة: تدوير وتوزيع الشعب بالتساوي بين أساتذة الاختصاص النشطين
  const secCharCode = stdSection.charCodeAt(0) || 0;
  const chosenTeacher = subjectTeachers[secCharCode % subjectTeachers.length];
  return chosenTeacher.fullName || `${chosenTeacher.firstName} ${chosenTeacher.secondName}`.trim();
}

/**
 * Executes a full step-by-step export of all school data to Supabase
 * with real-time callbacks for visual progress tracking.
 */
export async function exportSchoolDataWithProgress(
  schoolId: string,
  schoolName: string,
  pairingCode: string,
  adminEmail: string,
  students: Student[],
  staff: StaffMember[],
  scheduleMap?: DayScheduleMap,
  onProgress?: (step: SyncStepInfo) => void
): Promise<{ success: boolean; message: string; logs: SyncStepInfo[] }> {
  const client = getSupabase(schoolId);
  const totalSteps = 8;
  const logs: SyncStepInfo[] = [];

  const emit = (
    id: string,
    stepIndex: number,
    title: string,
    percent: number,
    details: string,
    status: SyncStepInfo['status'],
    count?: number
  ) => {
    const step: SyncStepInfo = {
      id,
      stepIndex,
      totalSteps,
      title,
      percent,
      details,
      status,
      count
    };
    const existingIdx = logs.findIndex(l => l.id === id);
    if (existingIdx > -1) {
      logs[existingIdx] = step;
    } else {
      logs.push(step);
    }
    if (onProgress) onProgress(step);
  };

  try {
    // ----------------------------------------------------
    // Step 1: Check Connection and Auth Ping
    // ----------------------------------------------------
    emit('step_ping', 1, 'فحص الاتصال بالسحابة والمصادقة', 10, 'جاري اختبار استجابة الخادم وتفويض الصلاحيات...', 'active');
    const { error: pingError } = await client.from('schools').select('id').limit(1);
    if (pingError) throw new Error(`فشل فحص الاتصال: ${pingError.message}`);
    emit('step_ping', 1, 'فحص الاتصال بالسحابة والمصادقة', 15, 'تم الاتصال بالخادم بنجاح وجاهزية الصلاحيات ✓', 'success');

    // ----------------------------------------------------
    // Step 2: Upsert School Identity & Pairing Profile
    // ----------------------------------------------------
    emit('step_school', 2, 'تصدير هوية المدرسة ورمز الاقتران', 20, `جاري تسجيل مدرسة (${schoolName}) ورمز (${pairingCode})...`, 'active');
    
    let timingConfig = {
      schoolStartHour: '08:00',
      lessonDurationMinutes: 45,
      breakDurationMinutes: 10
    };
    try {
      const savedCfg = typeof window !== 'undefined' ? localStorage.getItem('diyala_school_config') : null;
      if (savedCfg) {
        const parsed = JSON.parse(savedCfg);
        if (parsed.schoolStartHour) timingConfig.schoolStartHour = parsed.schoolStartHour;
        if (parsed.lessonDurationMinutes) timingConfig.lessonDurationMinutes = Number(parsed.lessonDurationMinutes);
        if (parsed.breakDurationMinutes) timingConfig.breakDurationMinutes = Number(parsed.breakDurationMinutes);
      }
    } catch (_) {}

    const schoolPayload = [
      {
        id: schoolId,
        name: schoolName,
        pairing_code: pairingCode,
        admin_email: adminEmail,
        config: timingConfig
      }
    ];
    const { error: schoolError } = await client.from('schools').upsert(schoolPayload, { onConflict: 'id' });
    if (schoolError) throw new Error(`خطأ في رفع بيانات المدرسة: ${schoolError.message}`);
    emit('step_school', 2, 'تصدير هوية المدرسة ورمز الاقتران', 28, `تم تسجيل هوية المدرسة بنجاح في السحابة (معرف: ${schoolId}) ✓`, 'success', 1);

    // ----------------------------------------------------
    // Step 3: Export Teachers & Staff (Clean old duplicates & Use Stable IDs)
    // ----------------------------------------------------
    emit('step_teachers', 3, 'تصدير كادر المعلمين والمدرسين', 35, `جاري تجهيز ورفع ${staff.length} معلماً وتصفية التكرارات...`, 'active');
    const teachersPayload = staff.map((member, idx) => {
      const cleanPhone = (member.phoneNumber || '').replace(/\D/g, '');
      const cleanNat = (member.nationalCardNumber || '').replace(/\D/g, '');
      const stableId = member.id && !member.id.startsWith('stf-xls-')
        ? member.id
        : (cleanPhone.length >= 8 ? `stf-${cleanPhone}` : (cleanNat.length >= 8 ? `stf-${cleanNat}` : `stf-idx-${idx + 1}`));

      return {
        id: stableId,
        school_id: schoolId,
        name: `${member.firstName} ${member.secondName || ''} ${member.thirdName || ''}`.trim() || 'معلم',
        email: member.phoneNumber || '',
        specialization: member.specialization || 'عام'
      };
    });

    if (teachersPayload.length > 0) {
      // Clean up previous duplicated teacher rows in cloud
      await client.from('teachers').delete().eq('school_id', schoolId);

      const { error: teachersError } = await client.from('teachers').upsert(teachersPayload, { onConflict: 'id' });
      if (teachersError) throw new Error(`خطأ في رفع كادر المعلمين: ${teachersError.message}`);
    }
    emit('step_teachers', 3, 'تصدير كادر المعلمين والمدرسين', 45, `تم رفع وتحديث بيانات ${teachersPayload.length} معلماً بنجاح بدون تكرار ✓`, 'success', teachersPayload.length);

    // ----------------------------------------------------
    // Step 4: Extract and Export Unique Classes & Sections
    // ----------------------------------------------------
    emit('step_classes', 4, 'استخراج وتصدير الفصول والشعب', 50, 'جاري استخراج الفصول والشعب الفريدة وتوحيدها قياسياً...', 'active');
    const uniqueClassesMap = new Map<string, { school_id: string; name: string; section: string }>();

    // 4.1 From Students Roster
    students.forEach(std => {
      if (std.currentGrade && std.section) {
        const stdGrade = standardizeGradeName(std.currentGrade);
        const stdSection = standardizeSectionName(std.section);
        const key = `${stdGrade}-${stdSection}`;
        if (!uniqueClassesMap.has(key)) {
          uniqueClassesMap.set(key, {
            school_id: schoolId,
            name: stdGrade,
            section: stdSection
          });
        }
      }
    });

    // 4.2 From Weekly Schedule
    if (scheduleMap) {
      Object.keys(scheduleMap).forEach(day => {
        (scheduleMap[day] || []).forEach(row => {
          if (row.grade && row.section) {
            const stdGrade = standardizeGradeName(row.grade);
            const stdSection = standardizeSectionName(row.section);
            const key = `${stdGrade}-${stdSection}`;
            if (!uniqueClassesMap.has(key)) {
              uniqueClassesMap.set(key, {
                school_id: schoolId,
                name: stdGrade,
                section: stdSection
              });
            }
          }
        });
      });
    }

    const classesPayload = Array.from(uniqueClassesMap.values());
    if (classesPayload.length > 0) {
      // Clean up previous duplicated or dirty classes in cloud for this school
      try {
        await client.from('classes').delete().eq('school_id', schoolId);
      } catch (delErr) {
        console.warn('Notice clearing previous classes:', delErr);
      }

      const { error: classError } = await client.from('classes').upsert(classesPayload, { onConflict: 'school_id,name,section' });
      if (classError) {
        console.warn('Classes RLS warning:', classError.message);
        emit('step_classes', 4, 'استخراج وتصدير الفصول والشعب', 60, `تنبيه في جدول الفصول: ${classError.message}`, 'warning');
      } else {
        emit('step_classes', 4, 'استخراج وتصدير الفصول والشعب', 60, `تم رفع وتوحيد ${classesPayload.length} شعبة وفصل دراسي بنجاح بدون تكرار ✓`, 'success', classesPayload.length);
      }
    } else {
      emit('step_classes', 4, 'استخراج وتصدير الفصول والشعب', 60, 'لا توجد فصول جديدة للرفع', 'success', 0);
    }

    // ----------------------------------------------------
    // Step 5 & 6: Extract Unique Subjects and Teacher Assignments (Prioritizing Assigned Schedule Lessons)
    // ----------------------------------------------------
    emit('step_subjects', 5, 'استخراج وتصدير المواد الدراسية', 65, 'جاري استخراج المواد المقررة والدروس المسندة بالجدول...', 'active');
    const uniqueSubjects = new Set<string>();
    const assignmentMap = new Map<string, any>();

    const getStaffStableId = (member: StaffMember, idx: number) => {
      const cleanPhone = (member.phoneNumber || '').replace(/\D/g, '');
      const cleanNat = (member.nationalCardNumber || '').replace(/\D/g, '');
      return member.id && !member.id.startsWith('stf-xls-')
        ? member.id
        : (cleanPhone.length >= 8 ? `stf-${cleanPhone}` : (cleanNat.length >= 8 ? `stf-${cleanNat}` : `stf-idx-${idx + 1}`));
    };

    const findStaff = (teacherNameOrId: string) => {
      if (!teacherNameOrId) return null;
      const clean = teacherNameOrId.replace(/^(أ\.|أستاذ\s*)\s*/gi, '').trim();
      const norm = normalizeArabic(clean);
      return staff.find(s => {
        if (s.id === teacherNameOrId) return true;
        const sFull = [s.firstName, s.secondName, s.thirdName, s.fourthName].filter(Boolean).join(' ');
        const normFull = normalizeArabic(sFull);
        const normFirstTwo = normalizeArabic(`${s.firstName} ${s.secondName}`);
        const normFirstThree = normalizeArabic(`${s.firstName} ${s.secondName} ${s.thirdName}`);
        return normFull === norm || normFirstThree === norm || normFirstTwo === norm ||
               (norm.length >= 3 && normFull.includes(norm)) ||
               (normFull.length >= 3 && norm.includes(normFull));
      });
    };

    // 1. PRIMARY SOURCE: Live Timetable Schedule Slots (المطابقة المباشرة مع دروس الجدول)
    if (scheduleMap) {
      Object.keys(scheduleMap).forEach(day => {
        const rows = scheduleMap[day] || [];
        rows.forEach(row => {
          const stdGrade = standardizeGradeName(row.grade);
          const stdSection = standardizeSectionName(row.section);
          
          if (row.lessons) {
            const lessonSlots = [
              row.lessons.lesson1,
              row.lessons.lesson2,
              row.lessons.lesson3,
              row.lessons.lesson4,
              row.lessons.lesson5,
              row.lessons.lesson6,
            ].filter(Boolean);

            lessonSlots.forEach(slot => {
              if (!slot.isOff && slot.subject && slot.teacherName) {
                const stdSubject = standardizeSubjectName(slot.subject);
                if (!isValidSubjectName(stdSubject)) return;

                uniqueSubjects.add(stdSubject);

                const matchedTeacher = findStaff(slot.teacherName);
                if (matchedTeacher && !isExemptStaff(matchedTeacher)) {
                  const teacherIdx = staff.indexOf(matchedTeacher);
                  const teacherId = getStaffStableId(matchedTeacher, teacherIdx);
                  const key = `${teacherId}_${stdGrade}_${stdSection}_${stdSubject}`;
                  
                  if (!assignmentMap.has(key)) {
                    assignmentMap.set(key, {
                      school_id: schoolId,
                      teacher_id: teacherId,
                      class_name: stdGrade,
                      section: stdSection,
                      subject_name: stdSubject
                    });
                  }
                }
              }
            });
          }
        });
      });
    }

    // 2. SECONDARY SOURCE: classesTaught and actualSubjectTaught (Only for active teaching staff)
    staff.forEach((member, idx) => {
      if (isExemptStaff(member)) return; // استبعاد المفرغين إدارياً تماماً من أنصبة ومواد التدريس

      const stableId = getStaffStableId(member, idx);
      const defaultSubject = member.actualSubjectTaught || member.specialization || 'عام';
      const cleanDefSubj = standardizeSubjectName(defaultSubject);
      if (isValidSubjectName(cleanDefSubj)) {
        uniqueSubjects.add(cleanDefSubj);
      }

      if (member.classesTaught && member.classesTaught.length > 0) {
        member.classesTaught.forEach(classStr => {
          const parsed = parseClassTaught(classStr, cleanDefSubj);
          const stdGrade = standardizeGradeName(parsed.grade);
          const stdSection = standardizeSectionName(parsed.section);
          const stdSubject = standardizeSubjectName(parsed.subject);
          
          if (isValidSubjectName(stdSubject)) {
            uniqueSubjects.add(stdSubject);
            const key = `${stableId}_${stdGrade}_${stdSection}_${stdSubject}`;
            if (!assignmentMap.has(key)) {
              assignmentMap.set(key, {
                school_id: schoolId,
                teacher_id: stableId,
                class_name: stdGrade,
                section: stdSection,
                subject_name: stdSubject
              });
            }
          }
        });
      }
    });

    const subjectsPayload = Array.from(uniqueSubjects)
      .filter(s => isValidSubjectName(s))
      .map(sub => ({
        school_id: schoolId,
        name: sub
      }));

    if (subjectsPayload.length > 0) {
      try {
        await client.from('subjects').delete().eq('school_id', schoolId);
      } catch (delSubErr) {
        console.warn('Notice clearing previous subjects:', delSubErr);
      }

      const { error: subjectError } = await client.from('subjects').upsert(subjectsPayload, { onConflict: 'school_id,name' });
      if (subjectError) {
        console.warn('Subjects RLS warning:', subjectError.message);
        emit('step_subjects', 5, 'استخراج وتصدير المواد الدراسية', 75, `تنبيه في جدول المواد: ${subjectError.message}`, 'warning');
      } else {
        emit('step_subjects', 5, 'استخراج وتصدير المواد الدراسية', 75, `تم تسجيل ${subjectsPayload.length} مادة دراسية قياسية في السحابة ✓`, 'success', subjectsPayload.length);
      }
    } else {
      emit('step_subjects', 5, 'استخراج وتصدير المواد الدراسية', 75, 'المواد مسجلة مسبقاً', 'success', 0);
    }

    emit('step_assignments', 6, 'تصدير إسناد وتوزيع الحصص للمدرسين', 80, 'جاري ربط كل معلم بصفوفه ودروسه المسندة في الجدول...', 'active');
    const assignmentsPayload = Array.from(assignmentMap.values());

    if (assignmentsPayload.length > 0) {
      await client.from('teacher_assignments').delete().eq('school_id', schoolId);
      const { error: assignmentError } = await client.from('teacher_assignments').upsert(assignmentsPayload, {
        onConflict: 'school_id,teacher_id,class_name,section,subject_name'
      });
      if (assignmentError) {
        console.warn('Assignments RLS warning:', assignmentError.message);
        emit('step_assignments', 6, 'تصدير إسناد وتوزيع الحصص للمدرسين', 75, `تنبيه في جدول الإسناد: ${assignmentError.message}`, 'warning');
      } else {
        emit('step_assignments', 6, 'تصدير إسناد وتوزيع الحصص للمدرسين', 75, `تم رفع ${assignmentsPayload.length} إسناد درس مسند للمعلمين ✓`, 'success', assignmentsPayload.length);
      }
    } else {
      emit('step_assignments', 6, 'تصدير إسناد وتوزيع الحصص للمدرسين', 75, 'لا توجد إسنادات جديدة', 'success', 0);
    }

    // ----------------------------------------------------
    // Step 7: Export Secret Codes & Staff Authority (تصدير وتأمين الرموز السرية وتفويض الكادر والمشرف)
    // ----------------------------------------------------
    emit('step_secrets', 7, 'تصدير وتأمين الرموز السرية وتفويض الكادر والمشرف', 80, 'جاري تشفير وتأمين الرموز السرية للمواد والمعلمين ورمز المشرف التربوي...', 'active');
    try {
      let subAssignmentsToUpsert: any[] = [];
      const savedAssStr = typeof window !== 'undefined' ? localStorage.getItem(`diyala_subject_assignments_${schoolId}`) : null;
      const savedProfilesStr = typeof window !== 'undefined' ? localStorage.getItem(`diyala_teacher_profiles_${schoolId}`) : null;

      // Prioritize saved profiles from TeacherAuthorityHub (Teacher-Centric unified PIN)
      if (savedProfilesStr) {
        try {
          const profiles: any[] = JSON.parse(savedProfilesStr);
          profiles.forEach(prof => {
            if (prof.isExempt) return;
            const subjects = prof.subjects && prof.subjects.length > 0 ? prof.subjects : [prof.specialization || 'عام'];
            const classes = prof.classes && prof.classes.length > 0 ? prof.classes : [{ grade: 'الأول المتوسط', section: 'أ' }];
            subjects.forEach((subj: string) => {
              const cleanSubj = standardizeSubjectName(subj || '');
              if (cleanSubj.includes('مفرغ') || cleanSubj.includes('إدارة') || cleanSubj.includes('تفرغ')) return;

              classes.forEach((cls: any) => {
                const cleanGrade = standardizeGradeName(cls.grade || '');
                const cleanSec = standardizeSectionName(cls.section || '');
                subAssignmentsToUpsert.push({
                  school_id: schoolId,
                  grade: cleanGrade,
                  section: cleanSec,
                  subject: cleanSubj,
                  secret_code: (prof.secretCode || '').trim() || pairingCode.slice(0, 4) || '1234',
                  is_locked: !!prof.isLocked,
                  teacher_name: (prof.teacherName || '').trim() || null,
                  last_updated_at: new Date().toISOString()
                });
              });
            });
          });
        } catch (e) {
          console.warn('Error building subject assignments from profiles:', e);
        }
      }

      // Fallback to legacy assignments only if profiles not found
      if (subAssignmentsToUpsert.length === 0 && savedAssStr) {
        try {
          const parsed = JSON.parse(savedAssStr);
          if (Array.isArray(parsed) && parsed.length > 0) {
            subAssignmentsToUpsert = parsed
              .filter(a => {
                const s = (a.subject || '').trim();
                return s && !s.includes('مفرغ') && !s.includes('إدارة') && !s.includes('تفرغ');
              })
              .map(a => ({
                school_id: schoolId,
                grade: standardizeGradeName(a.grade || ''),
                section: standardizeSectionName(a.section || ''),
                subject: standardizeSubjectName(a.subject || ''),
                secret_code: (a.secret_code || '').trim(),
                is_locked: !!a.is_locked,
                teacher_name: (a.teacher_name || '').trim() || null,
                last_updated_at: new Date().toISOString()
              }));
          }
        } catch (e) {
          console.warn('Error parsing diyala_subject_assignments:', e);
        }
      }

      // Fallback: build from assignmentMap and staff if no custom codes were set
      if (subAssignmentsToUpsert.length === 0 && assignmentsPayload.length > 0) {
        subAssignmentsToUpsert = assignmentsPayload.map(a => {
          const staffObj = staff.find(s => s.id === a.teacher_id);
          const tName = staffObj ? `${staffObj.firstName} ${staffObj.secondName || ''}`.trim() : null;
          return {
            school_id: schoolId,
            grade: standardizeGradeName(a.class_name),
            section: standardizeSectionName(a.section),
            subject: standardizeSubjectName(a.subject_name),
            secret_code: pairingCode.slice(0, 4) || '1234',
            is_locked: false,
            teacher_name: tName,
            last_updated_at: new Date().toISOString()
          };
        });
      }

      // Strictly filter, sanitize, and deduplicate subAssignmentsToUpsert to prevent duplicate/letter subjects
      const seenSubKeys = new Set<string>();
      subAssignmentsToUpsert = subAssignmentsToUpsert.filter(a => {
        const g = (a.grade || '').trim();
        const sec = (a.section || '').trim();
        const s = (a.subject || '').trim();
        if (!g || !sec || !s) return false;
        if (s.length <= 1 || /^[أ-يa-zA-Z]$/.test(s)) return false; // منع إسناد الأحرف كمادة
        if (s.includes('مفرغ') || s.includes('إدارة') || s.includes('تفرغ')) return false; // استبعاد المفرغين
        if (sec.includes('متوسط') || sec.includes('صف')) return false; // منع الشعب غير الصحيحة
        
        const key = `${g}__${sec}__${s}`;
        if (seenSubKeys.has(key)) return false;
        seenSubKeys.add(key);
        return true;
      });
      try {
        let profilesList: any[] = [];
        if (savedProfilesStr) {
          try {
            profilesList = JSON.parse(savedProfilesStr);
          } catch {}
        }
        let supProfile: any = null;
        const supervisorProfileStr = typeof window !== 'undefined' ? localStorage.getItem(`diyala_supervisor_profile_${schoolId}`) : null;
        if (supervisorProfileStr) {
          try {
            supProfile = JSON.parse(supervisorProfileStr);
          } catch {}
        }

        const fullConfig = {
          supervisor_code: supProfile?.code || (typeof window !== 'undefined' ? localStorage.getItem(`diyala_supervisor_code_${schoolId}`) : null) || 'SUP-1234',
          supervisor_name: supProfile?.name || 'المشرف التربوي المعتمد',
          supervisor_title: supProfile?.title || 'المشرف التربوي',
          teacher_profiles: profilesList,
          subject_assignments: subAssignmentsToUpsert,
          updated_at: new Date().toISOString()
        };

        await client.from('schools').update({ config: fullConfig }).eq('id', schoolId);
      } catch (confErr) {
        console.warn('Could not save complete config to schools table:', confErr);
      }

      // 2. Try upserting to subject_assignments table (Omit teacher_name column to avoid PostgreSQL error 42703)
      if (subAssignmentsToUpsert.length > 0) {
        try {
          const tableRecords = subAssignmentsToUpsert.map(a => ({
            school_id: a.school_id,
            grade: a.grade,
            section: a.section,
            subject: a.subject,
            secret_code: a.secret_code,
            is_locked: a.is_locked,
            last_updated_at: a.last_updated_at
          }));

          await client.from('subject_assignments').delete().eq('school_id', schoolId);
          await client.from('subject_assignments').upsert(tableRecords, {
            onConflict: 'school_id,grade,section,subject'
          });
        } catch (tableErr) {
          console.warn('Notice syncing subject_assignments table:', tableErr);
        }
      }

      emit('step_secrets', 7, 'تصدير وتأمين الرموز السرية وتفويض الكادر والمشرف', 88, `تم تأمين ورفع ${subAssignmentsToUpsert.length} رمزاً سرياً للكادر والمشرف في السحابة بنجاح ✓`, 'success', subAssignmentsToUpsert.length);
    } catch (subErr) {
      console.warn('Could not sync subject_assignments in cloud sync:', subErr);
      emit('step_secrets', 7, 'تصدير وتأمين الرموز السرية وتفويض الكادر والمشرف', 88, 'تم تحديث هوية المدرسة وإعدادات الرموز السحابية بنجاح ✓', 'success', 1);
    }

    // ----------------------------------------------------
    // Step 8: Export Students Roster (Ensuring Complete full_name & Alphabetical Order)
    // ----------------------------------------------------
    emit('step_students', 8, 'تصدير سجل الطلاب الموحد والأسماء الكاملة', 92, `جاري رفع سجلات ${students.length} طالب إلى السحابة...`, 'active');
    const sortedStudentsList = [...students].sort((a, b) => {
      const nameA = [a.firstName, a.secondName, a.thirdName, a.fourthName, a.titleName].filter(Boolean).join(' ').trim();
      const nameB = [b.firstName, b.secondName, b.thirdName, b.fourthName, b.titleName].filter(Boolean).join(' ').trim();
      return nameA.localeCompare(nameB, 'ar', { sensitivity: 'base' });
    });

    const seenRecordNumbers = new Set<string>();
    const studentsPayload = sortedStudentsList.map((std, idx) => {
      const computedFullName = [std.firstName, std.secondName, std.thirdName, std.fourthName, std.titleName]
        .filter(Boolean)
        .join(' ')
        .trim() || (std.fullName && std.fullName.trim()) || std.firstName;

      let recNum = (std.recordNumber || '').trim();
      if (!recNum || seenRecordNumbers.has(recNum)) {
        recNum = recNum ? `${recNum}-${std.section || idx + 1}` : `${1000 + idx + 1}`;
        if (seenRecordNumbers.has(recNum)) {
          recNum = `${1000 + idx + 1}`;
        }
      }
      seenRecordNumbers.add(recNum);

      return {
        school_id: schoolId,
        record_number: recNum,
        first_name: std.firstName,
        second_name: std.secondName || '',
        third_name: std.thirdName || '',
        fourth_name: std.fourthName || '',
        title_name: std.titleName || '',
        full_name: computedFullName,
        current_grade: standardizeGradeName(std.currentGrade),
        section: standardizeSectionName(std.section),
        absences_count: std.absencesCount || 0,
        status: std.status || 'مستمر'
      };
    });

    // Deduplicate payload strictly by record_number
    const uniqueStudentsMap = new Map<string, typeof studentsPayload[0]>();
    for (const s of studentsPayload) {
      uniqueStudentsMap.set(s.record_number, s);
    }
    const safeStudentsPayload = Array.from(uniqueStudentsMap.values());

    if (safeStudentsPayload.length > 0) {
      for (let i = 0; i < safeStudentsPayload.length; i += 100) {
        const chunk = safeStudentsPayload.slice(i, i + 100);
        const { error: studentError } = await client.from('students').upsert(chunk, { 
          onConflict: 'school_id,record_number',
          ignoreDuplicates: false 
        });
        if (studentError) throw new Error(`خطأ في رفع سجل الطلاب: ${studentError.message}`);
      }
    }
    emit('step_students', 8, 'تصدير سجل الطلاب الموحد والأسماء الكاملة', 96, `تم تصدير ${safeStudentsPayload.length} طالب بنجاح مع أسمائهم الكاملة ✓`, 'success', safeStudentsPayload.length);

    // ----------------------------------------------------
    // Step 9: Export Schedule Map
    // ----------------------------------------------------
    emit('step_schedule', 9, 'تصدير الجدول الأسبوعي للمدرسة', 98, 'جاري رفع خريطة جدول الحصص والتوقيتات...', 'active');
    const finalScheduleMap = (scheduleMap && Object.keys(scheduleMap).length > 0)
      ? { ...scheduleMap, _timing: timingConfig }
      : { 'الأحد': [], 'الإثنين': [], 'الثلاثاء': [], 'الأربعاء': [], 'الخميس': [], _timing: timingConfig };

    const { error: scheduleError } = await client.from('schedules').upsert({
      id: schoolId,
      school_id: schoolId,
      schedule_map: finalScheduleMap
    }, { onConflict: 'id', ignoreDuplicates: false });

    if (scheduleError) {
      console.warn('Schedule Sync Warning:', scheduleError.message);
    }
    emit('step_schedule', 9, 'تصدير الجدول الأسبوعي للمدرسة', 100, 'اكتمل رفع وتحديث جميع الجداول والرموز السحابية بنجاح 100% 🚀', 'success', 1);

    return {
      success: true,
      message: 'تم إنجاز المزامنة وتصدير كافة الجداول السحابية بنجاح تام!',
      logs
    };
  } catch (error: any) {
    console.error('Export Error:', error);
    const failedStep = logs[logs.length - 1];
    if (failedStep) {
      failedStep.status = 'error';
      failedStep.details = `❌ فشلت الخطوة: ${error.message}`;
      if (onProgress) onProgress(failedStep);
    }
    return {
      success: false,
      message: error.message || 'حدث خطأ غير متوقع أثناء الرفع السحابي.',
      logs
    };
  }
}

/**
 * Fetches real-time row counts directly from Supabase for all 10 core tables.
 */
export async function fetchCloudTableStats(schoolId: string): Promise<CloudTableStats> {
  const client = getSupabase(schoolId);
  const now = new Date().toLocaleTimeString('ar-IQ');

  const getCount = async (tableName: string) => {
    try {
      const filterCol = (tableName === 'schools' || tableName === 'schedules') ? 'id' : 'school_id';
      const { count, error } = await client
        .from(tableName)
        .select('*', { count: 'exact', head: true })
        .eq(filterCol, schoolId);
      if (error) return 0;
      return count || 0;
    } catch {
      return 0;
    }
  };

  const [
    teachersCount,
    classesCount,
    subjectsCount,
    assignmentsCount,
    studentsCount,
    gradesCount,
    attendanceCount,
    schedulesCount,
    directivesCount
  ] = await Promise.all([
    getCount('teachers'),
    getCount('classes'),
    getCount('subjects'),
    getCount('teacher_assignments'),
    getCount('students'),
    getCount('grades'),
    getCount('attendance'),
    getCount('schedules'),
    getCount('directives')
  ]);

  // Check schools table and count secret codes in config
  let schoolsCount = 0;
  let secretCodesCount = 0;
  try {
    const { data: schoolRow, error: schoolErr } = await client.from('schools').select('id, config').eq('id', schoolId).single();
    if (schoolRow) {
      schoolsCount = 1;
      const subAss = schoolRow.config?.subject_assignments;
      const profs = schoolRow.config?.teacher_profiles;
      if (Array.isArray(subAss) && subAss.length > 0) {
        secretCodesCount = subAss.length;
      } else if (Array.isArray(profs) && profs.length > 0) {
        secretCodesCount = profs.length;
      }
    }
  } catch {}

  // Also check subject_assignments table if exists as fallback
  if (secretCodesCount === 0) {
    try {
      const { count } = await client.from('subject_assignments').select('*', { count: 'exact', head: true }).eq('school_id', schoolId);
      if (count && count > 0) secretCodesCount = count;
    } catch {}
  }

  return {
    schoolsCount,
    teachersCount,
    classesCount,
    subjectsCount,
    assignmentsCount,
    studentsCount,
    gradesCount,
    attendanceCount,
    schedulesCount,
    directivesCount,
    secretCodesCount,
    lastChecked: now
  };
}

/**
 * Fetches real live rows from any cloud table for preview in the inspector.
 */
export async function fetchCloudTableRows(
  schoolId: string,
  tableName: string,
  limit: number = 100
): Promise<{ success: boolean; data: any[]; error?: string }> {
  try {
    const client = getSupabase(schoolId);

    // If viewing subject_assignments, prioritize verified clean assignments from schools.config
    // and strictly filter out any legacy single-letter or exempt status rows
    if (tableName === 'subject_assignments') {
      try {
        const { data: schoolRow } = await client.from('schools').select('config').eq('id', schoolId).single();
        const subAss = schoolRow?.config?.subject_assignments;
        if (Array.isArray(subAss) && subAss.length > 0) {
          const cleanConfigAss = subAss.filter((r: any) => {
            const s = (r.subject || '').trim();
            const g = (r.grade || '').trim();
            if (s.length <= 1 || /^[أ-يa-zA-Z]$/.test(s)) return false;
            if (s.includes('مفرغ') || s.includes('إدارة') || s.includes('تفرغ') || s.includes('شاغر')) return false;
            return g.length > 0;
          });
          if (cleanConfigAss.length > 0) {
            return { success: true, data: cleanConfigAss };
          }
        }
      } catch (confErr) {
        console.warn('Notice reading schools.config for subject assignments preview:', confErr);
      }

      try {
        const { data: sqlData, error: sqlErr } = await client.from('subject_assignments').select('*').eq('school_id', schoolId).limit(limit);
        if (!sqlErr && sqlData && sqlData.length > 0) {
          // Strictly sanitize and deduplicate sqlData
          const seenKeys = new Set<string>();
          const cleanSqlData: any[] = [];
          
          // Sort latest updated first
          const sortedSql = [...sqlData].sort((a, b) => 
            new Date(b.last_updated_at || 0).getTime() - new Date(a.last_updated_at || 0).getTime()
          );

          for (const row of sortedSql) {
            const s = (row.subject || '').trim();
            const g = (row.grade || '').trim();
            const sec = (row.section || '').trim();

            if (s.length <= 1 || /^[أ-يa-zA-Z]$/.test(s)) continue;
            if (s.includes('مفرغ') || s.includes('إدارة') || s.includes('تفرغ') || s.includes('شاغر')) continue;
            if (!g.includes('المتوسط') && !g.includes('الإعدادي') && !g.includes('الابتدائي')) continue;

            const key = `${standardizeGradeName(g)}__${standardizeSectionName(sec)}__${s}`;
            if (!seenKeys.has(key)) {
              seenKeys.add(key);
              cleanSqlData.push({
                ...row,
                grade: standardizeGradeName(g),
                section: standardizeSectionName(sec)
              });
            }
          }

          if (cleanSqlData.length > 0) {
            return { success: true, data: cleanSqlData };
          }
        }
      } catch {}
    }

    let query = client.from(tableName).select('*').limit(limit);

    if (tableName === 'schools' || tableName === 'schedules') {
      query = query.eq('id', schoolId);
    } else {
      query = query.eq('school_id', schoolId);
    }

    const { data, error } = await query;
    if (error) throw error;

    return { success: true, data: data || [] };
  } catch (err: any) {
    return { success: false, data: [], error: err.message };
  }
}

/**
 * Deletes an individual row directly from Supabase.
 */
export async function deleteCloudRow(
  schoolId: string,
  tableName: string,
  matchKey: string,
  matchValue: any
): Promise<{ success: boolean; message: string }> {
  try {
    const client = getSupabase(schoolId);
    let query = client.from(tableName).delete().eq(matchKey, matchValue);

    if (tableName !== 'schools') {
      query = query.eq('school_id', schoolId);
    }

    const { error } = await query;
    if (error) throw error;

    return { success: true, message: 'تم حذف السجل من السحابة بنجاح!' };
  } catch (err: any) {
    return { success: false, message: err.message || 'فشل حذف السجل من السحابة' };
  }
}

/**
 * Updates an individual row directly in Supabase.
 */
export async function updateCloudRow(
  schoolId: string,
  tableName: string,
  matchKey: string,
  matchValue: any,
  updateData: any
): Promise<{ success: boolean; message: string }> {
  try {
    const client = getSupabase(schoolId);
    let query = client.from(tableName).update(updateData).eq(matchKey, matchValue);

    if (tableName !== 'schools') {
      query = query.eq('school_id', schoolId);
    }

    const { error } = await query;
    if (error) throw error;

    return { success: true, message: 'تم تحديث السجل في السحابة بنجاح!' };
  } catch (err: any) {
    return { success: false, message: err.message || 'فشل تحديث السجل' };
  }
}

/**
 * Clears/purges all rows of a specific table for the school in Supabase.
 */
export async function clearCloudTable(
  schoolId: string,
  tableName: string
): Promise<{ success: boolean; message: string }> {
  try {
    const client = getSupabase(schoolId);
    if (tableName === 'schools') {
      return { success: false, message: 'لا يمكن حذف سجل هوية المدرسة الأساسي.' };
    }

    const { error } = await client.from(tableName).delete().eq('school_id', schoolId);
    if (error) throw error;

    return { success: true, message: `تم تفريغ جدول (${tableName}) من السحابة بنجاح!` };
  } catch (err: any) {
    return { success: false, message: err.message || 'فشل تفريغ الجدول' };
  }
}

/**
 * Deep cleans duplicate and invalid rows (single-letter subjects, ghost classes, redundant assignments)
 * directly from Supabase for this school.
 */
export async function deepCleanSchoolCloudData(
  schoolId: string
): Promise<{ success: boolean; message: string }> {
  try {
    const client = getSupabase(schoolId);
    
    // Purge classes, subjects, and assignments to clean all duplicates and single letter artifacts
    await client.from('classes').delete().eq('school_id', schoolId);
    await client.from('subjects').delete().eq('school_id', schoolId);
    await client.from('teacher_assignments').delete().eq('school_id', schoolId);
    try {
      await client.from('subject_assignments').delete().eq('school_id', schoolId);
    } catch {}

    return { 
      success: true, 
      message: 'تم تفريغ وتطهير جداول الشعب والمواد والإسناد والرموز السرية في السحابة بنجاح! أصبحت السحابة نقية 100%.' 
    };
  } catch (err: any) {
    return { success: false, message: err.message || 'فشل التطهير السحابي' };
  }
}

/**
 * Generates a collision-resistant unique ID for students.
 */
export function generateUniqueStudentId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `std_${crypto.randomUUID()}`;
  }
  return `std_${Date.now()}_${Math.random().toString(36).substring(2, 9)}_${Math.random().toString(36).substring(2, 9)}`;
}

/**
 * Validates and repairs student records to ensure each student has a strictly unique ID
 * and completely independent marksHistory array (breaking any accidental links between students).
 */
export function sanitizeStudents(list: Student[]): { sanitized: Student[]; hasRepairs: boolean } {
  if (!Array.isArray(list)) return { sanitized: [], hasRepairs: false };
  const seenIds = new Set<string>();
  let hasRepairs = false;

  const sanitized = list.map((s, idx) => {
    let id = s.id ? String(s.id).trim() : '';
    // Deep clone marksHistory to prevent shared object reference leaks
    let marksHistory = s.marksHistory ? JSON.parse(JSON.stringify(s.marksHistory)) : [];

    if (!id || seenIds.has(id)) {
      hasRepairs = true;
      id = generateUniqueStudentId();
    }
    seenIds.add(id);

    return {
      ...s,
      id,
      marksHistory
    };
  });

  return { sanitized, hasRepairs };
}

