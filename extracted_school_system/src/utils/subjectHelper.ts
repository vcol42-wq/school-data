import { StaffMember } from '../types';

/**
 * Smart Arabic Text Normalization
 */
export const normalizeText = (text: string): string => {
  if (!text) return '';
  return text
    .trim()
    .toLowerCase()
    .replace(/[ًٌٍَُِّْـ]/g, '') // remove tashkeel
    .replace(/[أإآ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/ى/g, 'ي')
    .replace(/ئ/g, 'ي')
    .replace(/ؤ/g, 'و');
};

/**
 * Smart Ministry Canonical Subject Mapping
 * Handles typos & variations:
 * - الأحياء (علوم الأحياء، احياء، حياة، بايو، biology...)
 * - الكيمياء (كمياء، كيميا، كميا، chemistry...)
 * - الاجتماعيات (تاريخ، جغرافيا، جغرافية، وطنية، دراسات اجتماعية...)
 * - اللغة العربية (عربي، عربية، قواعد، أدب، نصوص، لغة عربية...)
 * - اللغة الإنكليزية (انكليزي، انجليزي، انكليزية، انجليزية، english...)
 * - التربية الإسلامية (اسلامية، اسلاميه، دين، قرآن، عقيدة...)
 * - الفيزياء (فيزياء، فيزيا، physics...)
 * - الرياضيات (رياضيات، رياضي، جبر، هندسة، حساب، math...)
 * - الحاسوب (حاسوب، حاسبات، كمبيوتر، برمجة، تقنية معلومات...)
 * - التربية الرياضية (رياضة، رياضه، ألعاب، sport...)
 * - التربية الفنية (فنية، فنيه، رسم، فن، art...)
 * - التربية الأخلاقية (اخلاقية، اخلاق، تربية اخلاقية...)
 * - العلوم (علوم، علوم عامة...)
 * - مفرغ إدارياً / إدارة (مدير، معاون، مرشد، كاتب، أمين مكتبة، خدمة...)
 */
export const canonicalSubject = (subjectOrSpec: string): string => {
  const norm = normalizeText(subjectOrSpec);
  if (!norm) return 'عام';

  // 1. مفرغ إدارياً / إدارة مدرسية
  if (
    norm.includes('مفرغ') || 
    norm.includes('تفريغ') || 
    norm.includes('اداره') || 
    norm.includes('مدير') || 
    norm.includes('معاون') || 
    norm.includes('مرشد') || 
    norm.includes('مكتبه') || 
    norm.includes('خدمه') || 
    norm.includes('كاتب')
  ) {
    return 'مفرغ إدارياً / إدارة';
  }

  // 2. الأحياء (احياء / علوم الأحياء / علوم الحياة / حياة / بايو)
  if (
    norm.includes('احيا') || 
    norm.includes('بايو') || 
    norm.includes('حياه') || 
    norm.includes('حياة') || 
    norm.includes('علم الاحيا') || 
    norm.includes('علوم الاحيا') ||
    norm.includes('bio')
  ) {
    return 'الأحياء';
  }

  // 3. الكيمياء (كمياء / كيميا / كميا / كيمياء / chem)
  if (
    norm.includes('كيم') || 
    norm.includes('كمي') || 
    norm.includes('chem')
  ) {
    return 'الكيمياء';
  }

  // 4. الاجتماعيات (الجغرافيا + التاريخ + الوطنية + الاجتماع)
  if (
    norm.includes('اجتماع') || 
    norm.includes('جغراف') || 
    norm.includes('تاريخ') || 
    norm.includes('وطني') ||
    norm.includes('فلسف') ||
    norm.includes('اقتصاد') ||
    norm.includes('دراسات اجتماعي')
  ) {
    return 'الاجتماعيات';
  }

  // 5. الفيزياء (فيزياء / فيزيا / phys)
  if (
    norm.includes('فيز') || 
    norm.includes('phys')
  ) {
    return 'الفيزياء';
  }

  // 6. الرياضيات (رياضيات / رياضي / حساب / جبر / هندسة / math)
  if (
    norm.includes('رياضي') && !norm.includes('تربيه رياض') && !norm.includes('العاب') || 
    norm.includes('حساب') || 
    norm.includes('جبر') || 
    norm.includes('هندس') || 
    norm.includes('تفاضل') || 
    norm.includes('تكامل') || 
    norm.includes('math')
  ) {
    return 'الرياضيات';
  }

  // 7. اللغة الإنكليزية (انكليزي / انجليزي / انكليزية / انجليزية / english)
  if (
    norm.includes('انكل') || 
    norm.includes('انجل') || 
    norm.includes('انقل') || 
    norm.includes('english') || 
    norm === 'eng'
  ) {
    return 'اللغة الإنكليزية';
  }

  // 8. اللغة العربية (عربي / عربية / لغة عربية / قواعد / أدب / نصوص / بلاغة / قراءة)
  if (
    norm.includes('عرب') || 
    norm.includes('قواعد') || 
    norm.includes('ادب') || 
    norm.includes('نصوص') || 
    norm.includes('بلاغ') || 
    norm.includes('املا') || 
    norm.includes('انشا') || 
    norm.includes('قراءه') || 
    norm.includes('مطالع')
  ) {
    return 'اللغة العربية';
  }

  // 9. التربية الإسلامية (إسلامية / دين / قرآن / عقيدة / فقه)
  if (
    norm.includes('اسلام') || 
    norm.includes('دين') || 
    norm.includes('قران') || 
    norm.includes('عقيد') || 
    norm.includes('فقه') || 
    norm.includes('شريع')
  ) {
    return 'التربية الإسلامية';
  }

  // 10. الحاسوب (حاسوب / حاسبات / كمبيوتر / برمجة / تقنية معلومات)
  if (
    norm.includes('حاس') || 
    norm.includes('كمبيوتر') || 
    norm.includes('كومبيوتر') || 
    norm.includes('برمج') || 
    norm.includes('معلومات') || 
    norm === 'it' || 
    norm.includes('computer')
  ) {
    return 'الحاسوب';
  }

  // 11. التربية الرياضية (رياضة / ألعاب / العاب رياضية)
  if (
    norm === 'رياضه' || 
    norm.includes('تربيه رياض') || 
    norm.includes('العاب') || 
    norm.includes('sport') || 
    norm === 'pe' || 
    norm.includes('بدني')
  ) {
    return 'التربية الرياضية';
  }

  // 12. التربية الفنية (فنية / فن / رسم)
  if (
    norm.includes('فني') || 
    norm.includes('رسم') || 
    norm.includes('فنون') || 
    norm.includes('art')
  ) {
    return 'التربية الفنية';
  }

  // 13. التربية الأخلاقية (أخلاقية / أخلاق)
  if (
    norm.includes('اخلاق')
  ) {
    return 'التربية الأخلاقية';
  }

  // 14. العلوم العامة (علوم / علوم عامة)
  if (
    norm === 'علوم' || 
    norm.includes('علوم عام') || 
    norm.includes('science')
  ) {
    return 'العلوم';
  }

  return subjectOrSpec.trim();
};

/**
 * Standard Ministry Master Subjects List for High Schools & Intermediate Schools
 */
export const MASTER_SUBJECTS_LIST = [
  'التربية الإسلامية',
  'اللغة العربية',
  'اللغة الإنكليزية',
  'الرياضيات',
  'الاجتماعيات',
  'الأحياء',
  'الكيمياء',
  'الفيزياء',
  'الحاسوب',
  'التربية الرياضية',
  'التربية الفنية',
  'التربية الأخلاقية'
];

/**
 * Smart Matcher between Staff Member and Schedule Cell (Teacher Name & Subject)
 */
export const matchStaffWithScheduleCell = (
  staff: StaffMember, 
  cellTeacherRaw: string, 
  cellSubjectRaw?: string
): boolean => {
  if (!cellTeacherRaw) return false;

  const cleanCellTeacher = cellTeacherRaw.replace(/^أ\.\s*|^استاذ\s*|^معلم\s*/, '').trim();
  const normCell = normalizeText(cleanCellTeacher);
  if (!normCell || normCell === 'شاغر' || normCell === 'مفرغ') return false;

  const normFirst = normalizeText(staff.firstName);
  const normSecond = normalizeText(staff.secondName);
  const normThird = normalizeText(staff.thirdName);
  const normFourth = normalizeText(staff.fourthName || '');
  const normTitle = normalizeText(staff.titleName || '');

  const fullName1 = `${normFirst} ${normSecond} ${normThird} ${normFourth} ${normTitle}`.trim();
  const fullName2 = `${normFirst} ${normSecond} ${normThird}`.trim();
  const shortName = `${normFirst} ${normSecond}`.trim();
  const titleName = `${normFirst} ${normTitle}`.trim();

  // 1. Direct exact or substring match with names
  if (
    normCell === fullName1 ||
    normCell === fullName2 ||
    normCell === shortName ||
    normCell === titleName ||
    fullName1.includes(normCell) ||
    normCell.includes(shortName)
  ) {
    return true;
  }

  // 2. If cell only has first name, check if subject matches specialization
  if (normCell === normFirst && cellSubjectRaw) {
    const canonicalCellSubj = canonicalSubject(cellSubjectRaw);
    const canonicalStaffSubj = canonicalSubject(staff.actualSubjectTaught || staff.specialization);
    if (canonicalCellSubj === canonicalStaffSubj) {
      return true;
    }
  }

  return false;
};
