/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 *
 * Mathematical Backtracking CSP School Timetable Solver (Deck-Based Allocation)
 * نظام التوليد الرياضي الدقيق لجدول الحصص الأسبوعي (خوارزمية حل القيود والتراجع الرياضي)
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
 * Evaluates whether a subject is forbidden from Period 6 by pedagogical guidelines.
 * تمنع المواد العلمية (الفيزياء، الكيمياء، الأحياء، العلوم) والمواد الدينية والأخلاقية من الدرس السادس
 */
export function isForbiddenInPeriod6(subjectName: string): boolean {
  if (!subjectName) return false;
  const s = subjectName.trim().toLowerCase();
  if (s === 'شاغر' || s.includes('شاغر') || s.includes('نشاط')) return false;

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

  return isScience || isIslamic || isEthics;
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
 * 3. Checks zero teacher collisions across all slots.
 */
export function auditScheduleMathematicalCorrectness(
  scheduleMap: DayScheduleMap,
  sections: SmartScheduleSection[]
): { isValid: boolean; errors: string[] } {
  const errors: string[] = [];

  sections.forEach(sec => {
    // 1. Check total slots
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

    // 2. Check each requested subject count against expected deck
    sec.subjects.forEach(sub => {
      const sName = sub.subjectName.trim();
      const expected = sub.weeklyLessons || 0;
      const actual = actualSubjectCounts.get(sName) || 0;
      if (actual !== expected) {
        errors.push(`الشعبة [${sec.grade} - ${sec.section}]: مادة [${sName}] نصابها المطلوب (${expected}) ولكن تم تسكين (${actual}) حصص.`);
      }
    });

    // 3. Check Period 6 pedagogical constraints: 0 science in lesson 6, max 1 of any subject in lesson 6
    const p6SubjectCounts = new Map<string, number>();
    DAYS_OF_WEEK.forEach(day => {
      const row = (scheduleMap[day] || []).find(r => r.grade === sec.grade && r.section === sec.section);
      if (row) {
        const p6Cell = row.lessons.lesson6;
        if (p6Cell && p6Cell.subject && !p6Cell.isOff && !p6Cell.subject.includes('شاغر') && !p6Cell.subject.includes('نشاط')) {
          const sName = p6Cell.subject.trim();
          if (isForbiddenInPeriod6(sName)) {
            errors.push(`الشعبة [${sec.grade} - ${sec.section}]: المادة العلمية/المحظورة [${sName}] وضعت في الدرس السادس ليوم (${day}).`);
          }
          p6SubjectCounts.set(sName, (p6SubjectCounts.get(sName) || 0) + 1);
        }
      }
    });

    p6SubjectCounts.forEach((cnt, sName) => {
      if (cnt > 1) {
        errors.push(`الشعبة [${sec.grade} - ${sec.section}]: المادة [${sName}] تكررت (${cnt}) مرات في الدرس السادس والحد الأقصى هو مرة واحدة.`);
      }
    });
  });

  // 4. Check teacher collisions
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
 * Deck-Based CSP Solver with Backtracking & MRV (Minimum Remaining Values)
 */
export function generateSmartFairSchedule(
  sections: SmartScheduleSection[],
  maxAttempts: number = 250,
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
    let secQuota = 0;

    sec.subjects.forEach((sub, subIdx) => {
      const count = Math.max(0, sub.weeklyLessons || 0);
      secQuota += count;
      const sName = sub.subjectName.trim();
      const tName = (sub.teacherName || '').trim();
      const isVacant = sName.includes('شاغر') || tName === 'شاغر';
      const isSpecialTeacher = !isVacant && tName !== '' && tName !== 'أ. أستاذ المادة';

      if (isSpecialTeacher) {
        totalTeacherLoad.set(tName, (totalTeacherLoad.get(tName) || 0) + count);
      }

      for (let i = 0; i < count; i++) {
        cards.push({
          id: `card-${secKey}-${subIdx}-${i}`,
          sectionKey: secKey,
          grade: sec.grade,
          section: sec.section,
          subject: sName,
          teacher: tName || 'أ. أستاذ المادة',
          isSpecialTeacher,
          isVacant,
          weeklyQuota: count
        });
      }
    });

    // Enforce Mathematical Equality: Each section MUST have exactly 30 periods
    if (secQuota !== TOTAL_PERIODS_PER_WEEK) {
      if (secQuota < TOTAL_PERIODS_PER_WEEK) {
        // Auto-pad missing slots with clean Vacant Activity cards to ensure exact mathematical 30
        const needed = TOTAL_PERIODS_PER_WEEK - secQuota;
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
      } else {
        return {
          success: false,
          scheduleMap: createEmptyScheduleMap(sections),
          collisions: [],
          errorMsg: `الشعبة [${sec.grade} - ${sec.section}] تحتوي على (${secQuota}) حصة، والحد الأقصى المسموح هو 30 حصة أسبوعياً (5 أيام × 6 دروس). يرجى تقليل (${secQuota - 30}) حصص.`
        };
      }
    }

    sectionDecks.set(secKey, cards);
  }

  // Check teacher impossible load (> 30 hours a week is physically impossible)
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
  let bestSchedule: DayScheduleMap = createEmptyScheduleMap(sections);
  let bestCollisions: CollisionReport[] = [];

  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const seed = randomSeed + attempt * 31.7 + Math.random() * 1000;
    const scheduleResult = solveWithCSP(sections, sectionDecks, totalTeacherLoad, seed);

    if (scheduleResult) {
      // 3. Post-Generation Audit Verification
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

  // If deterministic backtrack hit recursion depth limit, return best effort with full reports
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
 * Core Constraint Satisfaction Solver with Backtracking & Forward Checking
 */
function solveWithCSP(
  sections: SmartScheduleSection[],
  sectionDecks: Map<string, LessonCard[]>,
  teacherTotalLoads: Map<string, number>,
  seed: number
): DayScheduleMap | null {
  // Grid matrix: [sectionKey][slotIndex] -> LessonCard
  const assignment: Record<string, (LessonCard | null)[]> = {};

  // Teacher busy occupancy: [slotIndex][teacherName] -> boolean
  const teacherOccupancyMap: Map<string, Set<number>> = new Map();

  // Daily subject counter per section: [sectionKey][dayIndex][subject] -> count
  const dailySubjectCount: Record<string, Record<number, Record<string, number>>> = {};

  // Period 6 counter per section: [sectionKey][subject] -> count in Period 6
  const period6SubjectCount: Record<string, Record<string, number>> = {};

  // Periods 1-5 counter per section: [sectionKey][subject] -> count in Periods 1-5
  const period1to5SubjectCount: Record<string, Record<string, number>> = {};

  sections.forEach(sec => {
    const sKey = `${sec.grade}_${sec.section}`;
    assignment[sKey] = new Array(30).fill(null);
    dailySubjectCount[sKey] = { 0: {}, 1: {}, 2: {}, 3: {}, 4: {} };
    period6SubjectCount[sKey] = {};
    period1to5SubjectCount[sKey] = {};
  });

  // Sort sections by constraint degree (sections with most shared teachers first)
  const sortedSections = [...sections].sort((a, b) => {
    const scoreA = a.subjects.reduce((sum, s) => sum + (teacherTotalLoads.get(s.teacherName.trim()) || 0), 0);
    const scoreB = b.subjects.reduce((sum, s) => sum + (teacherTotalLoads.get(s.teacherName.trim()) || 0), 0);
    return scoreB - scoreA;
  });

  // Solve section by section using CSP
  for (const sec of sortedSections) {
    const sKey = `${sec.grade}_${sec.section}`;
    const deck = [...(sectionDecks.get(sKey) || [])];

    // Order deck: Heaviest teacher loads & science & non-vacant first
    deck.sort((a, b) => {
      if (a.isVacant !== b.isVacant) return a.isVacant ? 1 : -1;
      const tLoadA = teacherTotalLoads.get(a.teacher) || 0;
      const tLoadB = teacherTotalLoads.get(b.teacher) || 0;
      if (tLoadA !== tLoadB) return tLoadB - tLoadA;
      return (Math.sin(seed + a.id.length * 7) - 0.5);
    });

    const success = backtrackSection(
      0,
      deck,
      sKey,
      assignment[sKey],
      teacherOccupancyMap,
      dailySubjectCount[sKey],
      period6SubjectCount[sKey],
      period1to5SubjectCount[sKey],
      seed
    );

    if (!success) {
      return null; // Trigger next restart/seed
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
              isOff: false
            };
          }
        });
      }
    });
  });

  return scheduleMap;
}

/**
 * Recursive Backtracking for a Single Section
 */
function backtrackSection(
  cardIndex: number,
  deck: LessonCard[],
  sectionKey: string,
  grid: (LessonCard | null)[],
  teacherOccupancyMap: Map<string, Set<number>>,
  dailySubjectCount: Record<number, Record<string, number>>,
  period6SubjectCount: Record<string, number>,
  period1to5SubjectCount: Record<string, number>,
  seed: number
): boolean {
  if (cardIndex >= deck.length) {
    return true; // All cards for this section successfully placed!
  }

  const card = deck[cardIndex];

  // Generate and score candidate slots (0 to 29)
  const candidateSlots: { slot: TimeSlot; penalty: number }[] = [];

  for (const slot of ALL_SLOTS) {
    if (grid[slot.slotIndex] !== null) continue; // Already occupied

    // 1. HARD CONSTRAINT: Teacher clash in another section
    if (card.isSpecialTeacher) {
      const busySlots = teacherOccupancyMap.get(card.teacher);
      if (busySlots && busySlots.has(slot.slotIndex)) {
        continue; // Absolute collision - prune
      }
    }

    // 2. HARD CONSTRAINT: Daily subject repetition (Max 1 of the same subject per day unless quota > 5)
    const currentToday = dailySubjectCount[slot.dayIndex][card.subject] || 0;
    const maxPerDay = card.weeklyQuota > 5 ? Math.ceil(card.weeklyQuota / 5) : 1;
    if (!card.isVacant && currentToday >= maxPerDay) {
      continue; // Strictly no duplicate subject on the same day
    }

    // 3. HARD CONSTRAINT: Thursday Lesson 6 (سادس الخميس شاغر حتماً للنشاط الحر أو نهاية الدوام)
    if (slot.day === 'الخميس' && slot.periodIndex === 5) {
      if (!card.isVacant && deck.some(c => c.isVacant)) {
        continue;
      }
    }

    // 4. PERIOD 6 RULES & EXACT QUOTA ALLOCATION:
    if (slot.periodIndex === 5) {
      // 4a. Forbidden subjects: Science (physics, chem, bio, general science), Islamic, Ethics, or quota <= 2
      if (isForbiddenInPeriod6(card.subject) || (!card.isVacant && card.weeklyQuota <= 2)) {
        continue; // Strictly forbidden in Period 6
      }

      // 4b. Max 1 lesson in Period 6 per subject (مولد الجدول يختار درس واحد فقط في الدرس السادس)
      const currentInP6 = period6SubjectCount[card.subject] || 0;
      if (!card.isVacant && currentInP6 >= 1) {
        continue; // No subject can have more than 1 lesson in Period 6
      }

      // 4c. Thursday Period 6 should not take core academic subjects if other days are available
      if (slot.day === 'الخميس' && !card.isVacant && (card.weeklyQuota === 5 || card.weeklyQuota === 4)) {
        if (deck.some(c => c.isVacant)) {
          continue;
        }
      }
    } else {
      // PERIODS 1 TO 5 (الدروس من 1 إلى 5):
      const currentIn1to5 = period1to5SubjectCount[card.subject] || 0;

      // 4d. For Quota 5 subjects: EXACTLY 4 lessons in Periods 1-5!
      // (يختار درس واحد في السادس ويبقى أربعة ضمن الخمس دروس)
      if (!card.isVacant && card.weeklyQuota === 5 && currentIn1to5 >= 4) {
        continue; // Must save the 5th lesson for Period 6!
      }

      // 4e. For Quota 4 subjects: EXACTLY 3 lessons in Periods 1-5!
      // (ويختار من 4 ليكون ثلاثة ضمن الخمس دروس)
      if (!card.isVacant && card.weeklyQuota === 4 && !isForbiddenInPeriod6(card.subject) && currentIn1to5 >= 3) {
        continue; // Must save the 4th lesson for Period 6!
      }
    }

    // 5. Scoring & Heuristic Penalties
    let penalty = 0;
    if (card.isVacant) {
      if (slot.periodIndex !== 5) {
        penalty += 15000; // Never put vacant in lessons 1-5 unless no other choice
      }
      // Thursday lesson 6 is highest preference for vacant
      if (slot.day === 'الخميس' && slot.periodIndex === 5) {
        penalty -= 1000;
      }
    } else {
      if (slot.periodIndex === 5) {
        // Priority for non-science quota 5 and 4 subjects in Period 6 (Sun, Mon, Tue, Wed)
        if (card.weeklyQuota === 5 && slot.day !== 'الخميس') {
          const curP6 = period6SubjectCount[card.subject] || 0;
          if (curP6 === 0) penalty -= 600; // Strongly guide 1 card to Period 6
        } else if (card.weeklyQuota === 4 && !isForbiddenInPeriod6(card.subject) && slot.day !== 'الخميس') {
          const curP6 = period6SubjectCount[card.subject] || 0;
          if (curP6 === 0) penalty -= 500; // Strongly guide 1 card to Period 6
        } else {
          penalty += 200;
        }
      } else {
        // In Periods 1-5: give prime morning slots to heavy sciences and core subjects
        if (isForbiddenInPeriod6(card.subject)) {
          penalty -= 200;
        }
      }
    }

    // 6. Natural dispersion: Add tie-breaking noise for rotation
    const noise = (Math.sin(seed + cardIndex * 13 + slot.slotIndex * 17) - 0.5) * 40;
    candidateSlots.push({ slot, penalty: penalty + noise });
  }

  // Sort candidate slots by lowest penalty (MRV heuristic)
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
    if (slot.periodIndex === 5) {
      period6SubjectCount[card.subject] = (period6SubjectCount[card.subject] || 0) + 1;
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
      period6SubjectCount,
      period1to5SubjectCount,
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
    if (slot.periodIndex === 5) {
      period6SubjectCount[card.subject] = (period6SubjectCount[card.subject] || 0) - 1;
    } else {
      period1to5SubjectCount[card.subject] = (period1to5SubjectCount[card.subject] || 0) - 1;
    }
  }

  return false; // Backtrack to previous card
}

/**
 * Deterministic Fallback Builder (Ensures exactly 30 lessons per section without omissions)
 */
function buildDeterministicFallback(
  sections: SmartScheduleSection[],
  sectionDecks: Map<string, LessonCard[]>
): DayScheduleMap {
  const scheduleMap = createEmptyScheduleMap(sections);

  sections.forEach(sec => {
    const sKey = `${sec.grade}_${sec.section}`;
    const cards = [...(sectionDecks.get(sKey) || [])];

    // Partition cards: Period 6 candidates (quota 5, quota 4, vacant) vs Periods 1-5 cards (sciences, etc.)
    const period6Cards: LessonCard[] = [];
    const periods1to5Cards: LessonCard[] = [];

    const seenP6Subjects = new Set<string>();

    // 1. Pick vacant for Thursday period 6
    const vacantIdx = cards.findIndex(c => c.isVacant);
    if (vacantIdx >= 0) {
      period6Cards.push(cards.splice(vacantIdx, 1)[0]);
    }

    // 2. Pick 1 card from each quota-5 subject for Period 6
    for (let i = cards.length - 1; i >= 0 && period6Cards.length < 5; i--) {
      const c = cards[i];
      if (c.weeklyQuota === 5 && !seenP6Subjects.has(c.subject) && !isForbiddenInPeriod6(c.subject)) {
        period6Cards.push(cards.splice(i, 1)[0]);
        seenP6Subjects.add(c.subject);
      }
    }

    // 3. Pick 1 card from quota-4 subjects for Period 6
    for (let i = cards.length - 1; i >= 0 && period6Cards.length < 5; i--) {
      const c = cards[i];
      if (c.weeklyQuota === 4 && !seenP6Subjects.has(c.subject) && !isForbiddenInPeriod6(c.subject)) {
        period6Cards.push(cards.splice(i, 1)[0]);
        seenP6Subjects.add(c.subject);
      }
    }

    // Fill remaining Period 6 up to 5 if needed with non-forbidden subjects
    while (period6Cards.length < 5 && cards.length > 0) {
      const candidateIdx = cards.findIndex(c => !isForbiddenInPeriod6(c.subject) && !seenP6Subjects.has(c.subject));
      if (candidateIdx >= 0) {
        period6Cards.push(cards.splice(candidateIdx, 1)[0]);
        seenP6Subjects.add(cards[candidateIdx]?.subject || '');
      } else {
        period6Cards.push(cards.pop()!);
      }
    }

    // All remaining cards are for Periods 1 to 5
    periods1to5Cards.push(...cards);

    // Populate schedule slots
    DAYS_OF_WEEK.forEach((day, dIdx) => {
      const row = scheduleMap[day].find(r => r.grade === sec.grade && r.section === sec.section);
      if (row) {
        // Fill Periods 1 to 5
        for (let p = 0; p < 5; p++) {
          const lKey = LESSON_KEYS[p];
          if (periods1to5Cards.length > 0) {
            const c = periods1to5Cards.shift()!;
            row.lessons[lKey] = {
              subject: c.subject,
              teacherName: c.teacher,
              isOff: c.isVacant
            };
          }
        }

        // Fill Period 6
        const p6Key = LESSON_KEYS[5];
        if (period6Cards.length > 0) {
          const c = period6Cards.shift()!;
          row.lessons[p6Key] = {
            subject: c.subject,
            teacherName: c.teacher,
            isOff: c.isVacant
          };
        }
      }
    });
  });

  return scheduleMap;
}

/**
 * Sanitizes and repairs sections to ensure each section has valid quotas totaling exactly 30 periods.
 */
export function sanitizeAndRepairSections(sections: SmartScheduleSection[]): {
  repaired: SmartScheduleSection[];
  wasModified: boolean;
  fixesSummary: string[];
} {
  const fixesSummary: string[] = [];
  let wasModified = false;

  const repaired = sections.map(sec => {
    let currentTotal = sec.subjects.reduce((sum, s) => sum + (s.weeklyLessons || 0), 0);
    const newSubjects = sec.subjects.map(s => ({ ...s }));

    if (currentTotal !== TOTAL_PERIODS_PER_WEEK) {
      wasModified = true;
      if (currentTotal < TOTAL_PERIODS_PER_WEEK) {
        // Add vacant or fill quota up to 30
        const diff = TOTAL_PERIODS_PER_WEEK - currentTotal;
        const vacantIdx = newSubjects.findIndex(s => s.subjectName === 'شاغر');
        if (vacantIdx >= 0) {
          newSubjects[vacantIdx].weeklyLessons += diff;
        } else {
          newSubjects.push({
            id: `sub-vacant-${Date.now()}-${Math.random()}`,
            subjectName: 'شاغر',
            teacherName: 'شاغر',
            weeklyLessons: diff
          });
        }
        fixesSummary.push(`تمت موازنة الشعبة (${sec.grade} - ${sec.section}) بإضافة ${diff} حصص شاغرة للوصول إلى 30 حصة.`);
      } else {
        // Exceeds 30: trim vacant or reduce largest quotas
        let excess = currentTotal - TOTAL_PERIODS_PER_WEEK;
        for (let i = newSubjects.length - 1; i >= 0 && excess > 0; i--) {
          if (newSubjects[i].subjectName === 'شاغر') {
            const reduce = Math.min(newSubjects[i].weeklyLessons, excess);
            newSubjects[i].weeklyLessons -= reduce;
            excess -= reduce;
          }
        }
        if (excess > 0) {
          for (let i = newSubjects.length - 1; i >= 0 && excess > 0; i--) {
            if (newSubjects[i].weeklyLessons > 1) {
              const reduce = Math.min(newSubjects[i].weeklyLessons - 1, excess);
              newSubjects[i].weeklyLessons -= reduce;
              excess -= reduce;
            }
          }
        }
        fixesSummary.push(`تم تقليص الزيادة في الشعبة (${sec.grade} - ${sec.section}) لتصبح 30 حصة أسبوعياً.`);
      }
    }

    return {
      ...sec,
      subjects: newSubjects.filter(s => s.weeklyLessons > 0)
    };
  });

  return { repaired, wasModified, fixesSummary };
}
