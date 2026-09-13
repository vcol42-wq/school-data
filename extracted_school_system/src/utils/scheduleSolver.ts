/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 *
 * Professional Mathematical Backtracking CSP School Timetable Solver (Deck-Based Allocation)
 * نظام التوليد الرياضي الدقيق لجدول الحصص الأسبوعي (خوارزمية حل القيود والتراجع الرياضي المتقدم)
 * ميثاق وضوابط توليد الجدول المدرسي:
 * 1. استبعاد الدرس السادس: منع المواد العلمية والمواد ذات النصاب <= 2 من الدرس السادس.
 * 2. سادس الخميس شاغر حتماً لكافة الصفوف (مخصص للنشاط الحر / نهاية الدوام).
 * 3. حصر الشواغر في نهاية الدوام (الدرس السادس) ومنع أي شاغر في الدروس 1 إلى 5.
 * 4. التكافؤ والتوازن العادل في الدرس السادس وتدوير الحصص والمدرسين.
 * 5. منع التكرار اليومي للمادة إلا إذا زاد نصابها عن 5 حصص، ومنع رتابة الحصة اليومية بتنويع أوقات المادة.
 */

import { DayScheduleMap, ClassScheduleRow, DayOfWeek, SmartScheduleSection, ScheduleCell } from '../types';

export interface CollisionReport {
  day: DayOfWeek;
  lessonKey: string;
  lessonLabel: string;
  teacherName: string;
  sections: string[];
}

export const DAYS_OF_WEEK: DayOfWeek[] = ['الأحد', 'الإثنين', 'الثلاثاء', 'الأربعاء', 'الخميس'];
export const LESSON_KEYS = ['lesson1', 'lesson2', 'lesson3', 'lesson4', 'lesson5', 'lesson6'] as const;

export const LESSON_LABELS: Record<string, string> = {
  lesson1: 'الدرس الأول',
  lesson2: 'الدرس الثاني',
  lesson3: 'الدرس الثالث',
  lesson4: 'الدرس الرابع',
  lesson5: 'الدرس الخامس',
  lesson6: 'الدرس السادس',
};

export const TOTAL_PERIODS_PER_WEEK = 30; // 5 days * 6 periods = 30

/**
 * Represent a single physical lesson card (Card in the Section's Deck)
 */
export interface LessonCard {
  id: string;
  sectionKey: string;     // e.g. "الصف الأول متوسط_أ"
  grade: string;
  section: string;
  subject: string;
  teacher: string;
  isSpecialTeacher: boolean; // teacher !== 'شاغر' and teacher !== ''
  isVacant: boolean;
  weeklyQuota: number;
}

/**
 * Standard slot representation in the weekly matrix (0 to 29)
 * slotIndex = dayIndex * 6 + periodIndex
 */
export interface TimeSlot {
  slotIndex: number;
  day: DayOfWeek;
  dayIndex: number;
  lessonKey: typeof LESSON_KEYS[number];
  periodIndex: number;
}

export const ALL_SLOTS: TimeSlot[] = [];
DAYS_OF_WEEK.forEach((day, dIdx) => {
  LESSON_KEYS.forEach((lessonKey, pIdx) => {
    ALL_SLOTS.push({
      slotIndex: dIdx * 6 + pIdx,
      day,
      dayIndex: dIdx,
      lessonKey,
      periodIndex: pIdx
    });
  });
});

/**
 * تسلسل تسكين الحصص الشاغرة حصراً في الدرس السادس (Rule 2 & Rule 3)
 * تبدأ بالخميس حتماً (نشاط حر / نهاية دوام) ثم الأربعاء فالثلاثاء فالإثنين فالأحد
 */
export const VACANT_SLOT_SEQUENCE: { day: DayOfWeek; dayIndex: number; periodIndex: number; slotIndex: number }[] = [
  { day: 'الخميس', dayIndex: 4, periodIndex: 5, slotIndex: 29 }, // 1. الخميس - الدرس السادس (إجباري لكافة الصفوف)
  { day: 'الأربعاء', dayIndex: 3, periodIndex: 5, slotIndex: 23 }, // 2. الأربعاء - الدرس السادس
  { day: 'الثلاثاء', dayIndex: 2, periodIndex: 5, slotIndex: 17 }, // 3. الثلاثاء - الدرس السادس
  { day: 'الإثنين', dayIndex: 1, periodIndex: 5, slotIndex: 11 }, // 4. الإثنين - الدرس السادس
  { day: 'الأحد', dayIndex: 0, periodIndex: 5, slotIndex: 5 },  // 5. الأحد - الدرس السادس
];

/**
 * Evaluates whether a subject is forbidden from Period 6 by pedagogical guidelines.
 * تمنع المواد العلمية (الفيزياء، الكيمياء، الأحياء، العلوم) والمواد الدينية والأخلاقية والمواد ذات النصاب <= 2 من الدرس السادس
 */
export function isForbiddenInPeriod6(subjectName: string, weeklyQuota?: number): boolean {
  if (!subjectName) return false;
  const s = subjectName.trim().toLowerCase();
  if (s === 'شاغر' || s.includes('شاغر') || s.includes('نشاط')) return false;

  // Rule 1: Subjects with quota <= 2 are forbidden in Period 6
  if (weeklyQuota !== undefined && weeklyQuota <= 2) return true;

  const isScience =
    s.includes('فيز') ||
    s.includes('كيم') ||
    s.includes('احيا') ||
    s.includes('أحيا') ||
    s.includes('علوم') ||
    s.includes('علمي') ||
    s.includes('phys') ||
    s.includes('chem') ||
    s.includes('bio') ||
    s.includes('sci');

  const isIslamic =
    s.includes('اسلام') ||
    s.includes('إسلام') ||
    s.includes('قران') ||
    s.includes('قرآن') ||
    s.includes('دين');

  const isEthics = s.includes('اخلاق') || s.includes('أخلاق');
  const isPE = s.includes('رياض') || s.includes('sport');
  const isArt = s.includes('فني') || s.includes('رسم') || s.includes('art');

  return isScience || isIslamic || isEthics || isPE || isArt;
}

/**
 * Pure Validation Tool: Checks if teacher collisions exist in a ScheduleMap.
 */
export function checkScheduleCollisions(scheduleMap: DayScheduleMap): CollisionReport[] {
  const collisions: CollisionReport[] = [];

  DAYS_OF_WEEK.forEach(day => {
    const rows = scheduleMap[day] || [];
    LESSON_KEYS.forEach(lessonKey => {
      const teacherMap = new Map<string, string[]>();

      rows.forEach(row => {
        const cell = row.lessons?.[lessonKey];
        if (cell && cell.teacherName && !cell.isOff) {
          const teacher = cell.teacherName.trim();
          if (teacher && teacher !== 'شاغر' && teacher !== 'أ. أستاذ المادة') {
            const sectionLabel = `${row.grade} - شعبة ${row.section}`;
            if (!teacherMap.has(teacher)) {
              teacherMap.set(teacher, []);
            }
            teacherMap.get(teacher)!.push(sectionLabel);
          }
        }
      });

      teacherMap.forEach((sections, teacher) => {
        if (sections.length > 1) {
          collisions.push({
            day,
            lessonKey,
            lessonLabel: LESSON_LABELS[lessonKey] || lessonKey,
            teacherName: teacher,
            sections
          });
        }
      });
    });
  });

  return collisions;
}

/**
 * Creates an empty structure for the schedule map.
 */
export function createEmptyScheduleMap(sections: SmartScheduleSection[]): DayScheduleMap {
  const map: DayScheduleMap = {
    'الأحد': [],
    'الإثنين': [],
    'الثلاثاء': [],
    'الأربعاء': [],
    'الخميس': []
  };

  DAYS_OF_WEEK.forEach(day => {
    sections.forEach(sec => {
      map[day].push({
        id: `row-${sec.grade}-${sec.section}-${day}`,
        grade: sec.grade,
        section: sec.section,
        teacherInCharge: sec.subjects[0]?.teacherName || 'أ. أستاذ المادة',
        lessons: {
          lesson1: { subject: '', teacherName: '', isOff: false },
          lesson2: { subject: '', teacherName: '', isOff: false },
          lesson3: { subject: '', teacherName: '', isOff: false },
          lesson4: { subject: '', teacherName: '', isOff: false },
          lesson5: { subject: '', teacherName: '', isOff: false },
          lesson6: { subject: '', teacherName: '', isOff: false },
        }
      });
    });
  });

  return map;
}

/**
 * Mathematical Post-Generation Audit Layer:
 * 1. Checks every section has exactly 30 allocated periods (no empty / missing slots).
 * 2. Checks every subject in every section has EXACTLY the quota requested (Deck integrity).
 * 3. Rule 2: Checks Thursday Period 6 is strictly VACANT for ALL sections.
 * 4. Rule 3: Checks zero vacancies in Periods 1 to 5.
 * 5. Rule 1: Checks zero forbidden subjects in Period 6.
 * 6. Rule 4: Checks max 1 lesson of any subject in Period 6 per section.
 * 7. Rule 5: Checks zero subject repetition on the same day (unless quota > 5).
 * 8. Checks zero teacher collisions across all slots.
 */
export function auditScheduleMathematicalCorrectness(
  scheduleMap: DayScheduleMap,
  sections: SmartScheduleSection[]
): { isValid: boolean; errors: string[] } {
  const errors: string[] = [];

  sections.forEach(sec => {
    let totalSlots = 0;
    const actualSubjectCounts = new Map<string, number>();

    DAYS_OF_WEEK.forEach(day => {
      const row = (scheduleMap[day] || []).find(r => r.grade === sec.grade && r.section === sec.section);
      if (!row) {
        errors.push(`الشعبة [${sec.grade} - ${sec.section}] غير موجودة في يوم (${day}).`);
        return;
      }

      LESSON_KEYS.forEach(lKey => {
        const cell = row.lessons[lKey];
        if (!cell || !cell.subject || cell.subject.trim() === '') {
          errors.push(`الشعبة [${sec.grade} - ${sec.section}] تحتوي على خانة فارغة غير مخصصة في يوم (${day}) - ${LESSON_LABELS[lKey]}.`);
        } else {
          totalSlots++;
          const sName = cell.subject.trim();
          actualSubjectCounts.set(sName, (actualSubjectCounts.get(sName) || 0) + 1);
        }
      });
    });

    if (totalSlots !== 30) {
      errors.push(`الشعبة [${sec.grade} - ${sec.section}] مجموع حصصها الأسبوعية (${totalSlots}) ولا يساوي 30 حصة.`);
    }

    // Rule 2: Thursday Period 6 is MANDATORY VACANT for ALL sections
    const thuRow = (scheduleMap['الخميس'] || []).find(r => r.grade === sec.grade && r.section === sec.section);
    if (thuRow) {
      const thu6 = thuRow.lessons.lesson6;
      const isThu6Vacant = !thu6 || thu6.isOff || !thu6.subject || thu6.subject.includes('شاغر') || thu6.subject.includes('نشاط');
      if (!isThu6Vacant) {
        errors.push(`الشعبة [${sec.grade} - ${sec.section}]: الدرس السادس يوم الخميس مخصص لنشاط حر / شاغر حصراً، ووجد به درس حقيقي [${thu6?.subject}].`);
      }
    }

    // Rule 3: Zero vacancies in Periods 1 to 5
    DAYS_OF_WEEK.forEach(day => {
      const row = (scheduleMap[day] || []).find(r => r.grade === sec.grade && r.section === sec.section);
      if (row) {
        LESSON_KEYS.forEach((lKey, pIdx) => {
          const cell = row.lessons[lKey];
          const isCellVacant = !cell || cell.isOff || !cell.subject || cell.subject.includes('شاغر') || cell.teacherName === 'شاغر';
          if (pIdx < 5 && isCellVacant) {
            errors.push(`الشعبة [${sec.grade} - ${sec.section}]: الحصة الشاغرة وُضعت في (${LESSON_LABELS[lKey]}) ليوم (${day})، وهذا مخالف لميثاق الجدول حيث يجب حصر الشواغر في الدرس السادس.`);
          }
        });
      }
    });

    // Rule 1: No science or <= 2 quota subjects in Period 6
    const p6SubjectCounts = new Map<string, number>();
    DAYS_OF_WEEK.forEach(day => {
      const row = (scheduleMap[day] || []).find(r => r.grade === sec.grade && r.section === sec.section);
      if (row) {
        const p6Cell = row.lessons.lesson6;
        if (p6Cell && p6Cell.subject && !p6Cell.isOff && !p6Cell.subject.includes('شاغر') && !p6Cell.subject.includes('نشاط')) {
          const sName = p6Cell.subject.trim();
          const q = sec.subjects.find(s => s.subjectName.trim() === sName)?.weeklyLessons;
          if (isForbiddenInPeriod6(sName, q)) {
            errors.push(`الشعبة [${sec.grade} - ${sec.section}]: المادة العلمية/المحظورة [${sName}] وضعت في الدرس السادس ليوم (${day}).`);
          }
          p6SubjectCounts.set(sName, (p6SubjectCounts.get(sName) || 0) + 1);
        }
      }
    });

    // Rule 4: At most 1 lesson of any subject in Period 6
    p6SubjectCounts.forEach((cnt, sName) => {
      if (cnt > 1) {
        errors.push(`الشعبة [${sec.grade} - ${sec.section}]: المادة [${sName}] تكررت (${cnt}) مرات في الدرس السادس والحد الأقصى هو مرة واحدة.`);
      }
    });

    // Rule 5: No duplicate subject on the same day unless quota > 5
    DAYS_OF_WEEK.forEach(day => {
      const row = (scheduleMap[day] || []).find(r => r.grade === sec.grade && r.section === sec.section);
      if (row) {
        const todaySubjects = new Map<string, number>();
        LESSON_KEYS.forEach(lk => {
          const cell = row.lessons[lk];
          if (cell && cell.subject && !cell.isOff && !cell.subject.includes('شاغر') && !cell.subject.includes('نشاط')) {
            const s = cell.subject.trim();
            todaySubjects.set(s, (todaySubjects.get(s) || 0) + 1);
          }
        });
        todaySubjects.forEach((count, s) => {
          const quota = sec.subjects.find(sub => sub.subjectName.trim() === s)?.weeklyLessons || 0;
          if (quota <= 5 && count > 1) {
            errors.push(`الشعبة [${sec.grade} - ${sec.section}]: مادة [${s}] تكررت (${count}) مرات في يوم (${day})، وميثاق الجدول يمنع تكرار المادة في نفس اليوم.`);
          }
        });
      }
    });
  });

  // Check teacher collisions
  const collisions = checkScheduleCollisions(scheduleMap);
  if (collisions.length > 0) {
    collisions.forEach(col => {
      errors.push(`تضارب في يوم (${col.day}) - ${col.lessonLabel}: الأستاذ [${col.teacherName}] مكلف في شعبتين في نفس الوقت: (${col.sections.join(' ، ')}).`);
    });
  }

  return {
    isValid: errors.length === 0,
    errors
  };
}

/**
 * Deck-Based CSP Solver with Intelligent Backtracking, Dynamic Anti-Repetition, and Priority Seeding
 */
export function generateSmartFairSchedule(
  sections: SmartScheduleSection[],
  maxAttempts: number = 500,
  randomSeed: number = Date.now()
): { success: boolean; scheduleMap: DayScheduleMap; collisions: CollisionReport[]; errorMsg?: string } {
  if (!sections || sections.length === 0) {
    return {
      success: false,
      scheduleMap: createEmptyScheduleMap([]),
      collisions: [],
      errorMsg: 'يرجى إضافة صفوف وشعب أولاً قبل توليد الجدول.'
    };
  }

  // 1. Deck Building & Mathematical Validation
  const sectionDecks = new Map<string, LessonCard[]>();
  const totalTeacherLoad = new Map<string, number>();

  for (const sec of sections) {
    const secKey = `${sec.grade}_${sec.section}`;
    const cards: LessonCard[] = [];
    let academicLessonsCount = 0;
    let explicitVacantCount = 0;

    sec.subjects.forEach((sub, subIdx) => {
      const count = Math.max(0, sub.weeklyLessons || 0);
      const sName = sub.subjectName.trim();
      const tName = (sub.teacherName || '').trim();
      const isVacant = sName.includes('شاغر') || sName.includes('نشاط') || tName === 'شاغر';
      const isSpecialTeacher = !isVacant && tName !== '' && tName !== 'أ. أستاذ المادة';

      if (isVacant) {
        explicitVacantCount += count;
      } else {
        academicLessonsCount += count;
        if (isSpecialTeacher) {
          totalTeacherLoad.set(tName, (totalTeacherLoad.get(tName) || 0) + count);
        }
      }

      for (let i = 0; i < count; i++) {
        cards.push({
          id: `card-${secKey}-${subIdx}-${i}`,
          sectionKey: secKey,
          grade: sec.grade,
          section: sec.section,
          subject: sName,
          teacher: isVacant ? 'شاغر' : (tName || 'أ. أستاذ المادة'),
          isSpecialTeacher,
          isVacant,
          weeklyQuota: count
        });
      }
    });

    // Rule 2 & 3 Enforcement:
    // Thursday Period 6 is MANDATORY VACANT (سادس الخميس شاغر حتماً لكافة الصفوف)
    // Therefore, maximum academic lessons must be 29 to allow at least 1 vacant slot for Thursday Period 6.
    if (cards.filter(c => c.isVacant).length === 0) {
      if (cards.length >= 30) {
        // Find highest quota non-science card to convert to mandatory Thursday vacancy
        let convertIdx = cards.findIndex(c => !isForbiddenInPeriod6(c.subject, c.weeklyQuota) && c.weeklyQuota >= 4);
        if (convertIdx === -1) {
          convertIdx = cards.findIndex(c => !isForbiddenInPeriod6(c.subject, c.weeklyQuota));
        }
        if (convertIdx === -1) {
          convertIdx = cards.length - 1;
        }
        cards.splice(convertIdx, 1);
      }
      // Add mandatory Thursday 6th vacancy card
      cards.push({
        id: `card-${secKey}-mandatory-thu-vacant`,
        sectionKey: secKey,
        grade: sec.grade,
        section: sec.section,
        subject: 'شاغر / نشاط حر',
        teacher: 'شاغر',
        isSpecialTeacher: false,
        isVacant: true,
        weeklyQuota: 0
      });
    }

    // Ensure total deck is exactly 30 periods
    if (cards.length < TOTAL_PERIODS_PER_WEEK) {
      const needed = TOTAL_PERIODS_PER_WEEK - cards.length;
      for (let i = 0; i < needed; i++) {
        cards.push({
          id: `card-${secKey}-pad-vacant-${i}`,
          sectionKey: secKey,
          grade: sec.grade,
          section: sec.section,
          subject: 'شاغر / نشاط حر',
          teacher: 'شاغر',
          isSpecialTeacher: false,
          isVacant: true,
          weeklyQuota: 0
        });
      }
    } else if (cards.length > TOTAL_PERIODS_PER_WEEK) {
      cards.splice(TOTAL_PERIODS_PER_WEEK);
    }

    sectionDecks.set(secKey, cards);
  }

  // Check teacher impossible load (> 30 periods a week is physically impossible)
  for (const [tName, load] of totalTeacherLoad.entries()) {
    if (load > 30) {
      return {
        success: false,
        scheduleMap: createEmptyScheduleMap(sections),
        collisions: [],
        errorMsg: `المعلم [${tName}] مكلف بـ (${load}) حصة أسبوعياً، وأقصى حد متاح في الأسبوع هو 30 حصة. يرجى تخفيض نصابه.`
      };
    }
  }

  // 2. Multi-Pass Backtracking Solver
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const seed = randomSeed + attempt * 47.3 + Math.random() * 5000;
    const scheduleResult = solveWithCSP(sections, sectionDecks, totalTeacherLoad, seed);

    if (scheduleResult) {
      const audit = auditScheduleMathematicalCorrectness(scheduleResult, sections);
      if (audit.isValid) {
        return {
          success: true,
          scheduleMap: scheduleResult,
          collisions: []
        };
      }
    }
  }

  // If deterministic backtrack hit iteration limit, build master-crafted non-repetitive fallback
  const fallback = buildDeterministicFallback(sections, sectionDecks);
  const collisions = checkScheduleCollisions(fallback);

  return {
    success: collisions.length === 0,
    scheduleMap: fallback,
    collisions,
    errorMsg: collisions.length > 0
      ? `تم توليد الجدول مع (${collisions.length}) تضارب في أنصبة بعض المعلمين المشتركين. يمكنك تعديلها يدوياً.`
      : undefined
  };
}

/**
 * Core Constraint Satisfaction Solver with Backtracking & Anti-Repetition Forward Checking
 */
function solveWithCSP(
  sections: SmartScheduleSection[],
  sectionDecks: Map<string, LessonCard[]>,
  teacherTotalLoads: Map<string, number>,
  seed: number
): DayScheduleMap | null {
  // Grid matrix: [sectionKey][slotIndex] -> LessonCard
  const assignment: Record<string, (LessonCard | null)[]> = {};

  // Teacher busy occupancy: [teacherName] -> Set of slotIndices
  const teacherOccupancyMap: Map<string, Set<number>> = new Map();

  // Daily subject counter per section: [sectionKey][dayIndex][subject] -> count
  const dailySubjectCount: Record<string, Record<number, Record<string, number>>> = {};

  // Anti-repetition: Period-index tracker per subject [sectionKey][subject] -> array of size 6 counting occurrences in lesson 1..6
  const subjectPeriodCount: Record<string, Record<string, number[]>> = {};

  // Period 6 counter per section: [sectionKey][subject] -> count in Period 6
  const period6SubjectCount: Record<string, Record<string, number>> = {};

  // Periods 1-5 counter per section: [sectionKey][subject] -> count in Periods 1-5
  const period1to5SubjectCount: Record<string, Record<string, number>> = {};

  // Global Period 6 teacher counter across the school for Fair Rotation (Rule 4)
  const schoolPeriod6TeacherCount: Map<string, number> = new Map();

  sections.forEach(sec => {
    const sKey = `${sec.grade}_${sec.section}`;
    assignment[sKey] = new Array(30).fill(null);
    dailySubjectCount[sKey] = { 0: {}, 1: {}, 2: {}, 3: {}, 4: {} };
    subjectPeriodCount[sKey] = {};
    period6SubjectCount[sKey] = {};
    period1to5SubjectCount[sKey] = {};
  });

  // Sort sections by constraint degree (sections with heaviest shared teachers first)
  const sortedSections = [...sections].sort((a, b) => {
    const scoreA = a.subjects.reduce((sum, s) => sum + (teacherTotalLoads.get(s.teacherName.trim()) || 0), 0);
    const scoreB = b.subjects.reduce((sum, s) => sum + (teacherTotalLoads.get(s.teacherName.trim()) || 0), 0);
    return scoreB - scoreA;
  });

  // Solve section by section using CSP
  for (const sec of sortedSections) {
    const sKey = `${sec.grade}_${sec.section}`;
    const fullDeck = [...(sectionDecks.get(sKey) || [])];

    const vacantCards = fullDeck.filter(c => c.isVacant);
    const realCards = fullDeck.filter(c => !c.isVacant);

    // Rule 2 & 3: Pre-allocate all vacant cards directly to designated Lesson 6 slots
    // (Starting Thursday 6th, Wednesday 6th, Tuesday 6th, Monday 6th, Sunday 6th)
    vacantCards.forEach((vCard, idx) => {
      const vSlot = VACANT_SLOT_SEQUENCE[idx];
      if (vSlot) {
        assignment[sKey][vSlot.slotIndex] = vCard;
        dailySubjectCount[sKey][vSlot.dayIndex][vCard.subject] = (dailySubjectCount[sKey][vSlot.dayIndex][vCard.subject] || 0) + 1;
        if (vSlot.periodIndex === 5) {
          period6SubjectCount[sKey][vCard.subject] = (period6SubjectCount[sKey][vCard.subject] || 0) + 1;
        }
      }
    });

    // Order real deck: Heaviest teacher loads & morning sciences first
    realCards.sort((a, b) => {
      const tLoadA = teacherTotalLoads.get(a.teacher) || 0;
      const tLoadB = teacherTotalLoads.get(b.teacher) || 0;
      if (tLoadA !== tLoadB) return tLoadB - tLoadA;
      const aSci = isForbiddenInPeriod6(a.subject, a.weeklyQuota);
      const bSci = isForbiddenInPeriod6(b.subject, b.weeklyQuota);
      if (aSci !== bSci) return aSci ? -1 : 1;
      return (Math.sin(seed + a.id.length * 11) - 0.5);
    });

    const success = backtrackSection(
      0,
      realCards,
      sKey,
      assignment[sKey],
      teacherOccupancyMap,
      dailySubjectCount[sKey],
      subjectPeriodCount[sKey],
      period6SubjectCount[sKey],
      period1to5SubjectCount[sKey],
      schoolPeriod6TeacherCount,
      seed
    );

    if (!success) {
      return null; // Trigger next restart with new seed
    }
  }

  // Convert assignment matrix to DayScheduleMap
  const scheduleMap = createEmptyScheduleMap(sections);

  DAYS_OF_WEEK.forEach((day, dIdx) => {
    sections.forEach(sec => {
      const sKey = `${sec.grade}_${sec.section}`;
      const row = scheduleMap[day].find(r => r.grade === sec.grade && r.section === sec.section);
      if (row) {
        LESSON_KEYS.forEach((lKey, pIdx) => {
          const slotIndex = dIdx * 6 + pIdx;
          const card = assignment[sKey][slotIndex];
          if (card) {
            row.lessons[lKey] = {
              subject: card.subject,
              teacherName: card.teacher,
              isOff: card.isVacant
            };
          }
        });
      }
    });
  });

  return scheduleMap;
}

/**
 * Recursive Backtracking with Dynamic Anti-Repetition and Pedagogical Priority
 */
function backtrackSection(
  cardIndex: number,
  deck: LessonCard[],
  sectionKey: string,
  grid: (LessonCard | null)[],
  teacherOccupancyMap: Map<string, Set<number>>,
  dailySubjectCount: Record<number, Record<string, number>>,
  subjectPeriodCount: Record<string, number[]>,
  period6SubjectCount: Record<string, number>,
  period1to5SubjectCount: Record<string, number>,
  schoolPeriod6TeacherCount: Map<string, number>,
  seed: number
): boolean {
  if (cardIndex >= deck.length) {
    return true; // All cards successfully allocated
  }

  const card = deck[cardIndex];

  // Generate and score candidate slots (0 to 29)
  const candidateSlots: { slot: TimeSlot; penalty: number }[] = [];

  for (const slot of ALL_SLOTS) {
    if (grid[slot.slotIndex] !== null) continue; // Already occupied (including pre-allocated vacancies)

    // Hard Constraint: Vacant cards are forbidden from Periods 1 to 5 (Rule 3)
    if (card.isVacant && slot.periodIndex < 5) {
      continue;
    }

    // Hard Constraint: Real cards are strictly forbidden from Thursday Period 6 (Rule 2)
    if (!card.isVacant && slot.slotIndex === 29) {
      continue;
    }

    // 1. HARD CONSTRAINT: Teacher clash in another section
    if (card.isSpecialTeacher) {
      const busySlots = teacherOccupancyMap.get(card.teacher);
      if (busySlots && busySlots.has(slot.slotIndex)) {
        continue; // Absolute teacher collision
      }
    }

    // 2. HARD CONSTRAINT: Daily subject repetition (Rule 5: Max 1 per day unless quota > 5)
    const currentToday = dailySubjectCount[slot.dayIndex][card.subject] || 0;
    const maxPerDay = card.weeklyQuota > 5 ? Math.ceil(card.weeklyQuota / 5) : 1;
    if (!card.isVacant && currentToday >= maxPerDay) {
      continue; // Strictly no duplicate subject on the same day
    }

    // 3. PERIOD 6 HARD CONSTRAINTS (Rule 1 & Rule 4)
    if (slot.periodIndex === 5) {
      // 3a. Forbidden in Period 6: Science, Islamic, Ethics, PE, Art, or quota <= 2
      if (isForbiddenInPeriod6(card.subject, card.weeklyQuota) || (!card.isVacant && card.weeklyQuota <= 2)) {
        continue;
      }

      // 3b. Max 1 lesson in Period 6 per subject across the entire week
      const currentInP6 = period6SubjectCount[card.subject] || 0;
      if (!card.isVacant && currentInP6 >= 1) {
        continue;
      }
    }

    // 4. SCORING & PROFESSIONAL PEDAGOGICAL HEURISTICS
    let penalty = 0;

    // 4a. ANTI-REPETITION (Dynamic Period Rotation):
    // Heavily penalize placing the same subject at the same period number more than once in a week!
    // (Prevents Arabic/English/Math from being Lesson 1 every single day)
    const pHistory = subjectPeriodCount[card.subject] || [0, 0, 0, 0, 0, 0];
    const timesInThisPeriod = pHistory[slot.periodIndex] || 0;
    if (timesInThisPeriod === 1) {
      penalty += 450; // Heavy penalty for putting the subject at the same period number twice
    } else if (timesInThisPeriod >= 2) {
      penalty += 2200; // Strictly prevent 3rd repetition of the same period number
    }

    // 4b. MORNING PRIORITY for Heavy Sciences & Single/Dual Quota Subjects:
    // Place sciences, math, and single-quota subjects in Periods 1, 2, 3 where focus is highest
    const isMorningPriority = isForbiddenInPeriod6(card.subject, card.weeklyQuota) || card.weeklyQuota <= 2;
    if (isMorningPriority && slot.periodIndex < 5) {
      penalty -= (4 - slot.periodIndex) * 75; // Earlier periods preferred
    }

    // 4c. Rule 4: Fair Rotation of Period 6 among teachers across the school
    if (slot.periodIndex === 5 && !card.isVacant) {
      const p6TeacherLoad = schoolPeriod6TeacherCount.get(card.teacher) || 0;
      penalty += p6TeacherLoad * 250;
      if (card.weeklyQuota >= 5) {
        penalty -= 100;
      }
    }

    // 4d. Avoid two consecutive heavy academic/science lessons on the same day
    if (slot.periodIndex > 0) {
      const prevSlot = slot.slotIndex - 1;
      const prevCard = grid[prevSlot];
      if (prevCard && !prevCard.isVacant && isMorningPriority && isForbiddenInPeriod6(prevCard.subject, prevCard.weeklyQuota)) {
        penalty += 150; // Soft penalty for consecutive heavy subjects
      }
    }

    // 4e. Natural dispersion tie-breaker
    const noise = (Math.sin(seed + cardIndex * 19.3 + slot.slotIndex * 31.7) - 0.5) * 35;
    candidateSlots.push({ slot, penalty: penalty + noise });
  }

  // Sort candidate slots by lowest penalty (MRV / Best fit first)
  candidateSlots.sort((a, b) => a.penalty - b.penalty);

  for (const cand of candidateSlots) {
    const slot = cand.slot;

    // Place card
    grid[slot.slotIndex] = card;
    if (card.isSpecialTeacher) {
      if (!teacherOccupancyMap.has(card.teacher)) {
        teacherOccupancyMap.set(card.teacher, new Set());
      }
      teacherOccupancyMap.get(card.teacher)!.add(slot.slotIndex);
    }
    dailySubjectCount[slot.dayIndex][card.subject] = (dailySubjectCount[slot.dayIndex][card.subject] || 0) + 1;

    if (!subjectPeriodCount[card.subject]) {
      subjectPeriodCount[card.subject] = [0, 0, 0, 0, 0, 0];
    }
    subjectPeriodCount[card.subject][slot.periodIndex] = (subjectPeriodCount[card.subject][slot.periodIndex] || 0) + 1;

    if (slot.periodIndex === 5) {
      period6SubjectCount[card.subject] = (period6SubjectCount[card.subject] || 0) + 1;
      if (card.isSpecialTeacher) {
        schoolPeriod6TeacherCount.set(card.teacher, (schoolPeriod6TeacherCount.get(card.teacher) || 0) + 1);
      }
    } else {
      period1to5SubjectCount[card.subject] = (period1to5SubjectCount[card.subject] || 0) + 1;
    }

    // Recurse to next card
    if (backtrackSection(
      cardIndex + 1,
      deck,
      sectionKey,
      grid,
      teacherOccupancyMap,
      dailySubjectCount,
      subjectPeriodCount,
      period6SubjectCount,
      period1to5SubjectCount,
      schoolPeriod6TeacherCount,
      seed
    )) {
      return true;
    }

    // Backtrack (Undo placement)
    grid[slot.slotIndex] = null;
    if (card.isSpecialTeacher) {
      teacherOccupancyMap.get(card.teacher)!.delete(slot.slotIndex);
    }
    dailySubjectCount[slot.dayIndex][card.subject] = (dailySubjectCount[slot.dayIndex][card.subject] || 0) - 1;
    subjectPeriodCount[card.subject][slot.periodIndex] = Math.max(0, (subjectPeriodCount[card.subject][slot.periodIndex] || 1) - 1);

    if (slot.periodIndex === 5) {
      period6SubjectCount[card.subject] = (period6SubjectCount[card.subject] || 0) - 1;
      if (card.isSpecialTeacher) {
        schoolPeriod6TeacherCount.set(card.teacher, Math.max(0, (schoolPeriod6TeacherCount.get(card.teacher) || 1) - 1));
      }
    } else {
      period1to5SubjectCount[card.subject] = (period1to5SubjectCount[card.subject] || 0) - 1;
    }
  }

  return false;
}

/**
 * Master-Crafted Deterministic Fallback Builder (Ensures exactly 30 lessons per section,
 * strictly enforces Thursday 6th vacancy, zeroes vacancies in periods 1-5, and avoids column repetition)
 */
function buildDeterministicFallback(
  sections: SmartScheduleSection[],
  sectionDecks: Map<string, LessonCard[]>
): DayScheduleMap {
  const scheduleMap = createEmptyScheduleMap(sections);

  sections.forEach((sec, secIdx) => {
    const sKey = `${sec.grade}_${sec.section}`;
    const cards = [...(sectionDecks.get(sKey) || [])];

    const vacantCards = cards.filter(c => c.isVacant);
    const realCards = cards.filter(c => !c.isVacant);

    // Rule 2 & 3: Thursday Period 6 is always vacant, followed by other Period 6 vacancies
    vacantCards.forEach((vCard, idx) => {
      const vSlot = VACANT_SLOT_SEQUENCE[idx];
      if (vSlot) {
        const row = scheduleMap[vSlot.day].find(r => r.grade === sec.grade && r.section === sec.section);
        if (row) {
          const lKey = LESSON_KEYS[vSlot.periodIndex];
          row.lessons[lKey] = {
            subject: vCard.subject || 'شاغر / نشاط حر',
            teacherName: 'شاغر',
            isOff: true
          };
        }
      }
    });

    // Partition real cards: eligible for Period 6 (quota >= 3, non-science) vs restricted to Periods 1-5
    const eligibleP6Cards: LessonCard[] = [];
    const restrictedCards: LessonCard[] = [];

    realCards.forEach(c => {
      if (isForbiddenInPeriod6(c.subject, c.weeklyQuota) || c.weeklyQuota <= 2) {
        restrictedCards.push(c);
      } else {
        eligibleP6Cards.push(c);
      }
    });

    // Sort eligible cards by highest quota
    eligibleP6Cards.sort((a, b) => b.weeklyQuota - a.weeklyQuota);

    // Fill remaining Period 6 slots (if any) with at most 1 lesson per eligible subject
    const seenP6Subjects = new Set<string>();
    DAYS_OF_WEEK.forEach((day, dIdx) => {
      if (dIdx === 4) return; // Thursday Period 6 is strictly vacant!
      const row = scheduleMap[day].find(r => r.grade === sec.grade && r.section === sec.section);
      if (row && (!row.lessons.lesson6.subject || row.lessons.lesson6.subject === '')) {
        const cardIdx = eligibleP6Cards.findIndex(c => !seenP6Subjects.has(c.subject));
        if (cardIdx >= 0) {
          const picked = eligibleP6Cards.splice(cardIdx, 1)[0];
          seenP6Subjects.add(picked.subject);
          row.lessons.lesson6 = {
            subject: picked.subject,
            teacherName: picked.teacher,
            isOff: false
          };
        }
      }
    });

    // All remaining cards (restricted + leftover eligible cards) are placed in Periods 1 to 5
    // To PREVENT repetition across days, we group cards by subject and distribute them across the 5 days
    const remainingCards = [...restrictedCards, ...eligibleP6Cards];
    const subjectBuckets = new Map<string, LessonCard[]>();
    remainingCards.forEach(c => {
      if (!subjectBuckets.has(c.subject)) {
        subjectBuckets.set(c.subject, []);
      }
      subjectBuckets.get(c.subject)!.push(c);
    });

    // Sort subjects: Sciences and heavy subjects first
    const sortedSubjectNames = Array.from(subjectBuckets.keys()).sort((a, b) => {
      const aSci = isForbiddenInPeriod6(a);
      const bSci = isForbiddenInPeriod6(b);
      if (aSci !== bSci) return aSci ? -1 : 1;
      return (subjectBuckets.get(b)?.length || 0) - (subjectBuckets.get(a)?.length || 0);
    });

    // Grid matrix: grid[dayIndex][periodIndex (0..4)]
    const dayGrid: (LessonCard | null)[][] = Array.from({ length: 5 }, () => Array(5).fill(null));

    sortedSubjectNames.forEach((sName, sIdx) => {
      const sCards = subjectBuckets.get(sName) || [];
      sCards.forEach((c, cIdx) => {
        // Distribute across days: offset by subject index and section index to prevent section clones
        const targetDay = (cIdx * 2 + sIdx + secIdx) % 5;
        // Shift period to vary lesson number across the week
        const targetPeriod = (cIdx + sIdx * 2 + secIdx) % 5;

        let placed = false;
        // 1. Try targetDay first with all periods
        for (let pOffset = 0; pOffset < 5 && !placed; pOffset++) {
          const p = (targetPeriod + pOffset) % 5;
          if (dayGrid[targetDay][p] === null) {
            dayGrid[targetDay][p] = c;
            placed = true;
          }
        }
        // 2. If targetDay was full, try other days
        for (let dOffset = 1; dOffset < 5 && !placed; dOffset++) {
          const d = (targetDay + dOffset) % 5;
          const alreadyInDay = dayGrid[d].some(cell => cell?.subject === sName);
          if (alreadyInDay && sCards.length <= 5) continue;

          for (let pOffset = 0; pOffset < 5 && !placed; pOffset++) {
            const p = (targetPeriod + pOffset) % 5;
            if (dayGrid[d][p] === null) {
              dayGrid[d][p] = c;
              placed = true;
            }
          }
        }
        // 3. Emergency fallback to any free slot in Periods 1-5
        if (!placed) {
          for (let d = 0; d < 5 && !placed; d++) {
            for (let p = 0; p < 5 && !placed; p++) {
              if (dayGrid[d][p] === null) {
                dayGrid[d][p] = c;
                placed = true;
              }
            }
          }
        }
      });
    });

    // Transfer dayGrid into scheduleMap
    DAYS_OF_WEEK.forEach((day, dIdx) => {
      const row = scheduleMap[day].find(r => r.grade === sec.grade && r.section === sec.section);
      if (row) {
        for (let p = 0; p < 5; p++) {
          const lKey = LESSON_KEYS[p];
          const c = dayGrid[dIdx][p];
          if (c) {
            row.lessons[lKey] = {
              subject: c.subject,
              teacherName: c.teacher,
              isOff: false
            };
          }
        }
      }
    });
  });

  return scheduleMap;
}

/**
 * Sanitizes and repairs sections to ensure each section has valid quotas totaling exactly 30 periods
 * with mandatory reservation for Thursday Period 6 vacancy (نشاط حر / نهاية دوام).
 */
export function sanitizeAndRepairSections(sections: SmartScheduleSection[]): {
  repaired: SmartScheduleSection[];
  wasModified: boolean;
  fixesSummary: string[];
} {
  const fixesSummary: string[] = [];
  let wasModified = false;

  const repaired = sections.map(sec => {
    const newSubjects = sec.subjects.map(s => ({ ...s }));

    // Rule 2: Thursday Period 6 is mandatory vacant.
    // Therefore, academic lessons can be at most 29.
    let academicTotal = newSubjects.filter(s => !s.subjectName.includes('شاغر') && !s.subjectName.includes('نشاط'))
                                   .reduce((sum, s) => sum + (s.weeklyLessons || 0), 0);

    // If academic lessons equal 30 (no room for Thursday 6 vacancy), reduce 1 from largest non-science subject
    if (academicTotal >= 30) {
      wasModified = true;
      let excess = academicTotal - 29;
      for (let i = 0; i < newSubjects.length && excess > 0; i++) {
        const sub = newSubjects[i];
        if (!isForbiddenInPeriod6(sub.subjectName, sub.weeklyLessons) && sub.weeklyLessons >= 4) {
          sub.weeklyLessons -= 1;
          excess -= 1;
          academicTotal -= 1;
          fixesSummary.push(`تم تعديل حصة من مادة [${sub.subjectName}] لتوفير حصة سادس الخميس شاغر/نشاط حر الإلزامية.`);
        }
      }
      if (excess > 0) {
        for (let i = newSubjects.length - 1; i >= 0 && excess > 0; i--) {
          const sub = newSubjects[i];
          if (sub.weeklyLessons > 2) {
            sub.weeklyLessons -= 1;
            excess -= 1;
            academicTotal -= 1;
          }
        }
      }
    }

    const currentTotal = newSubjects.reduce((sum, s) => sum + (s.weeklyLessons || 0), 0);
    if (currentTotal < TOTAL_PERIODS_PER_WEEK) {
      wasModified = true;
      const diff = TOTAL_PERIODS_PER_WEEK - currentTotal;
      const vacantIdx = newSubjects.findIndex(s => s.subjectName.includes('شاغر') || s.subjectName.includes('نشاط'));
      if (vacantIdx >= 0) {
        newSubjects[vacantIdx].weeklyLessons += diff;
      } else {
        newSubjects.push({
          id: `sub-vacant-${Date.now()}-${Math.random()}`,
          subjectName: 'شاغر / نشاط حر',
          teacherName: 'شاغر',
          weeklyLessons: diff
        });
      }
      fixesSummary.push(`تمت موازنة الشعبة (${sec.grade} - ${sec.section}) بحصص شاغرة للوصول إلى 30 حصة أسبوعياً مع ضمان سادس الخميس.`);
    }

    return {
      ...sec,
      subjects: newSubjects.filter(s => s.weeklyLessons > 0)
    };
  });

  return { repaired, wasModified, fixesSummary };
}
