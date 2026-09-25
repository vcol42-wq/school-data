// ==============================================================================
// Grade Calculation Engine for Iraqi Ministry of Education Curriculums
// ==============================================================================

import { StudentMark } from '../types';
import { normalizeArabic } from './syncService';

/**
 * فحص هل المادة ذات تقييم (شفهي + تحريري) وفق التعليمات الوزارية
 * اللغات (عربي، إنكليزي، كردي، فرنسي) والتربية الإسلامية
 */
export const isOralAndWrittenSubject = (subjectName: string): boolean => {
  const norm = normalizeArabic(subjectName || '').toLowerCase();
  return (
    norm.includes('اسلامي') ||
    norm.includes('اسلامية') ||
    norm.includes('عرب') ||
    norm.includes('عربي') ||
    norm.includes('عربية') ||
    norm.includes('انكليز') ||
    norm.includes('انجليز') ||
    norm.includes('انكليزية') ||
    norm.includes('انجليزية') ||
    norm.includes('كرد') ||
    norm.includes('فرنس') ||
    norm.includes('لغة')
  );
};

/**
 * حساب درجة الشهر من اليومي والتحريري والشفهي
 * نمط الجمع: مجموع اليومي + التحريري + الشفهي (أو حسب توزيع المدرس)
 * نمط التقسيم: احتساب متوسط الامتحانات اليومية + التحريري / الشفهي
 */
export const calculateMonthScore = (
  dailyMarks: number[],
  writtenScore: number,
  oralScore: number = 0,
  mode: 'sum' | 'average' = 'average',
  isOral: boolean = false
): number => {
  const validDaily = (dailyMarks || []).filter(n => typeof n === 'number' && !isNaN(n) && n > 0);
  
  if (mode === 'average') {
    // طريقة التقسيم: معدل اليومي (من 100 مثلاً أو بنسب محددة)
    const dailyAvg = validDaily.length > 0 ? validDaily.reduce((a, b) => a + b, 0) / validDaily.length : 0;
    
    if (isOral) {
      // شفهي + تحريري + يومي
      // إذا أدخل المدرس التحريري والشفهي واليومي كدرجات:
      if (writtenScore > 0 || oralScore > 0 || dailyAvg > 0) {
        // إذا كان المدرس يعتمد صيغة 20 شفهي + 20 يومي + 60 تحريري أو صيغة المعدل المباشر:
        // نأخذ المجموع المباشر إذا كان مجموع القيم <= 100
        const directSum = Math.round(dailyAvg + writtenScore + oralScore);
        if (directSum > 0 && directSum <= 100 && (writtenScore < 70 && oralScore < 40)) {
          return directSum;
        }
        // أو متوسط الدرجات
        const count = (dailyAvg > 0 ? 1 : 0) + (writtenScore > 0 ? 1 : 0) + (oralScore > 0 ? 1 : 0);
        return count > 0 ? Math.round((dailyAvg + writtenScore + oralScore) / count) : 0;
      }
      return 0;
    } else {
      // مادة تحريرية ونشاط
      if (writtenScore > 0 && dailyAvg > 0) {
        return Math.round((dailyAvg + writtenScore) / 2);
      }
      return Math.round(writtenScore || dailyAvg || 0);
    }
  } else {
    // طريقة الجمع المباشر
    const dailySum = validDaily.reduce((a, b) => a + b, 0);
    const total = Math.round(dailySum + writtenScore + (isOral ? oralScore : 0));
    return Math.min(100, Math.max(0, total));
  }
};

/**
 * حساب معدل الفصل (فصل أول أو فصل ثاني)
 * شهر 1 + شهر 2 [+ شهر 3 اختياري]
 */
export const calculateTermAverage = (
  month1: number,
  month2: number,
  month3?: number | null
): number => {
  const m1 = month1 || 0;
  const m2 = month2 || 0;
  const m3 = (typeof month3 === 'number' && month3 > 0) ? month3 : 0;

  if (m3 > 0 && m1 > 0 && m2 > 0) {
    return Math.round((m1 + m2 + m3) / 3);
  }
  if (m1 > 0 && m2 > 0) {
    return Math.round((m1 + m2) / 2);
  }
  if (m2 > 0) return m2;
  if (m1 > 0) return m1;
  if (m3 > 0) return m3;
  return 0;
};

/**
 * حساب درجة امتحان نصف السنة (أو آخر السنة)
 * شفهي + تحريري للمواد الشفوية، أو تحريري خالص
 */
export const calculateExamScore = (
  written: number,
  oral: number = 0,
  isOral: boolean = false
): number => {
  const w = written || 0;
  const o = oral || 0;
  if (isOral) {
    if (w > 0 && o > 0) {
      // إذا كانت الدرجتان مقسمتين (مثلاً 20 شفهي + 80 تحريري أو 50 + 50)
      if (w + o <= 100) {
        return Math.round(w + o);
      }
      return Math.round((w + o) / 2);
    }
    return Math.round(w || o || 0);
  }
  return Math.round(w);
};

/**
 * حساب السعي السنوي بالمعادلة الوزارية العراقية:
 * السعي السنوي = (معدل الفصل الأول + درجة نصف السنة + معدل الفصل الثاني) ÷ 3
 */
export const calculateYearlyEffort = (
  term1Avg: number,
  midtermGrade: number,
  term2Avg: number
): number => {
  const t1 = term1Avg || 0;
  const mid = midtermGrade || 0;
  const t2 = term2Avg || 0;

  if (t1 > 0 && mid > 0 && t2 > 0) {
    return Math.round((t1 + mid + t2) / 3);
  }
  // إذا لم يكتمل الفصل الثاني بعد، يمكن احتساب معدل ف1 ونصف السنة كتقييم مؤقت
  return 0;
};

/**
 * حساب الدرجة النهائية العامة:
 * السعي السنوي + درجة الامتحان النهائي (دور أول أو دور ثاني) ÷ 2
 */
export const calculateFinalGradeScore = (
  annualAverage: number,
  finalExamScore: number
): number => {
  const annual = annualAverage || 0;
  const finalEx = finalExamScore || 0;

  if (annual > 0 && finalEx > 0) {
    return Math.round((annual + finalEx) / 2);
  }
  return 0;
};

/**
 * تطبيق محاكي درجات القرار الوزاري (5 درجات كحد أقصى)
 * تقوم الخوارزمية باختيار المواد الراسبة القريبة من النجاح (45 إلى 49)
 * لمنحها درجات القرار المناسبة لتحويلها إلى 50، لتقليل مواد الإكمال أو النجاح.
 */
export interface DecisionResultItem {
  subject: string;
  originalScore: number;
  graceUsed: number;
  finalScoreWithGrace: number;
  becamePassed: boolean;
}

export const applyMinisterialGrace = (
  subjectScores: { subject: string; score: number }[],
  maxGracePool: number = 5
): {
  items: DecisionResultItem[];
  totalGraceUsed: number;
  remainingGrace: number;
  initialFails: number;
  newFails: number;
} => {
  // تصفية المواد الراسبة القابلة للقرار (الدرجة بين 45 و 49)
  const sortedEligible = subjectScores
    .map(s => ({
      subject: s.subject,
      score: Math.round(s.score || 0),
      needed: 50 - Math.round(s.score || 0)
    }))
    .filter(item => item.score >= 45 && item.score < 50)
    // نرتب بالأقرب للنجاح أولاً (من يحتاج درجات أقل 49 ثم 48...)
    .sort((a, b) => a.needed - b.needed);

  let currentGrace = maxGracePool;
  const graceMap = new Map<string, number>();

  for (const item of sortedEligible) {
    if (currentGrace >= item.needed) {
      graceMap.set(item.subject, item.needed);
      currentGrace -= item.needed;
    }
  }

  let initialFails = 0;
  let newFails = 0;

  const items: DecisionResultItem[] = subjectScores.map(s => {
    const orig = Math.round(s.score || 0);
    const grace = graceMap.get(s.subject) || 0;
    const finalScore = orig + grace;
    const wasFail = orig > 0 && orig < 50;
    const isNowFail = finalScore > 0 && finalScore < 50;

    if (wasFail) initialFails++;
    if (isNowFail) newFails++;

    return {
      subject: s.subject,
      originalScore: orig,
      graceUsed: grace,
      finalScoreWithGrace: finalScore,
      becamePassed: wasFail && !isNowFail
    };
  });

  return {
    items,
    totalGraceUsed: maxGracePool - currentGrace,
    remainingGrace: currentGrace,
    initialFails,
    newFails
  };
};
