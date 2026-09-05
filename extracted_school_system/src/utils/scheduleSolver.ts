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

// Core curriculum priority weights (lower number = higher preference for early slots 1-4)
const SUBJECT_TIME_PRIORITY: Record<string, number> = {
  'الرياضيات': 1,
  'اللغة العربية': 1,
  'اللغة الانكليزية': 1,
  'اللغة الإنجليزية': 1,
  'الفيزياء': 2,
  'الكيمياء': 2,
  'الأحياء': 2,
  'العلوم': 2,
  'التربية الإسلامية': 3,
  'الاجتماعيات': 3,
  'التاريخ': 3,
  'الجغرافية': 3,
  'الوطنية': 3,
  'الحاسوب': 4,
  'التربية الفنية': 5,
  'التربية الرياضية': 5,
  'النشيد والموسيقى': 5,
};

/**
 * Strict Rule: 
 * 1. Science subjects (Physics, Chemistry, Biology) CANNOT be placed in Lesson 6.
 * 2. Single-period subjects (like Ethics) and dual-period subjects (نصاب فردي أو ثنائي <= 2) CANNOT be placed in Lesson 6.
 * 3. Thursday Lesson 6 is strictly vacant / activity (لا يوضع درس حقيقي في الدرس السادس يوم الخميس).
 * 4. Any vacant slots must strictly be placed in Lesson 6.
 */
export function isForbiddenInPeriod6(subjectName: string, weeklyQuota: number = 2): boolean {
  if (!subjectName) return false;
  const s = subjectName.trim().toLowerCase();
  
  // 1. المواد العلمية (الفيزياء، الكيمياء، الأحياء)
  const isScience = 
    s.includes('فيز') || 
    s.includes('كيم') || 
    s.includes('احيا') || 
    s.includes('phys') || 
    s.includes('chem') || 
    s.includes('bio');

  // 2. الدروس التي نصابها درس واحد أو درسان (أحادي أو ثنائي <= 2 حصص أسبوعياً) مثل الأخلاقية
  const isLowQuota = weeklyQuota <= 2 || s.includes('اخلاق') || s.includes('أخلاق');

  return isScience || isLowQuota;
}

/**
 * Validates whether a generated or manually edited schedule has any teacher collisions.
 */
export function checkScheduleCollisions(scheduleMap: DayScheduleMap): CollisionReport[] {
  const collisions: CollisionReport[] = [];

  DAYS_OF_WEEK.forEach(day => {
    const rows = scheduleMap[day] || [];
    LESSON_KEYS.forEach(lessonKey => {
      const teacherMap = new Map<string, string[]>(); // teacherName -> section list

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

export interface DailySubjectDuplicateReport {
  day: DayOfWeek;
  grade: string;
  section: string;
  subject: string;
  count: number;
}

/**
 * Validates whether any section has the same subject repeated on the same day.
 */
export function checkDailySubjectDuplicates(scheduleMap: DayScheduleMap): DailySubjectDuplicateReport[] {
  const duplicates: DailySubjectDuplicateReport[] = [];

  DAYS_OF_WEEK.forEach(day => {
    const rows = scheduleMap[day] || [];
    rows.forEach(row => {
      const subjectCounts = new Map<string, number>();
      LESSON_KEYS.forEach(lKey => {
        const cell = row.lessons[lKey];
        if (cell && cell.subject && cell.subject !== 'شاغر / نشاط حر' && !cell.isOff) {
          const s = cell.subject.trim();
          subjectCounts.set(s, (subjectCounts.get(s) || 0) + 1);
        }
      });

      subjectCounts.forEach((count, subject) => {
        if (count > 1) {
          duplicates.push({
            day,
            grade: row.grade,
            section: row.section,
            subject,
            count
          });
        }
      });
    });
  });

  return duplicates;
}

/**
 * Simple pseudo-random generator with seed
 */
function pseudoRandom(seed: number) {
  const x = Math.sin(seed++) * 10000;
  return x - Math.floor(x);
}

/**
 * Evaluates fairness and pedagogical balance of a schedule:
 * 1. Severe penalty for repeating the same subject on the same day for the same section (unless weekly quota > 5).
 * 2. High reward for distributing recurring subjects evenly across different days of the week.
 * 3. High reward for rotating period slots (prevent fixing Math/Arabic to period 1 every day).
 */
function calculateFairnessScore(schedule: DayScheduleMap): number {
  let score = 0;
  
  const allSectionKeys = new Set<string>();
  DAYS_OF_WEEK.forEach(day => {
    (schedule[day] || []).forEach(row => {
      allSectionKeys.add(`${row.grade}_${row.section}`);
    });
  });

  allSectionKeys.forEach(secKey => {
    const [grade, section] = secKey.split('_');
    const subjectPeriodsMap = new Map<string, number[]>();
    const subjectDaysMap = new Map<string, Set<string>>();

    DAYS_OF_WEEK.forEach(day => {
      const row = (schedule[day] || []).find(r => r.grade === grade && r.section === section);
      if (row) {
        const dailySubjectCounts = new Map<string, number>();

        LESSON_KEYS.forEach((lKey, pIdx) => {
          const cell = row.lessons[lKey];
          if (cell && cell.subject && cell.subject !== 'شاغر / نشاط حر' && !cell.isOff) {
            const subj = cell.subject.trim();
            // Period tracking
            const list = subjectPeriodsMap.get(subj) || [];
            list.push(pIdx);
            subjectPeriodsMap.set(subj, list);

            // Day tracking
            if (!subjectDaysMap.has(subj)) {
              subjectDaysMap.set(subj, new Set());
            }
            subjectDaysMap.get(subj)!.add(day);

            // Daily repetition tracking
            dailySubjectCounts.set(subj, (dailySubjectCounts.get(subj) || 0) + 1);
          }
        });

        // 1. HARD PENALTY for duplicate subject on the same day for this section
        dailySubjectCounts.forEach((count, subj) => {
          if (count > 1) {
            score -= (count - 1) * 3500; // Strong penalty for repeating same subject in the same day!
          }
        });

        // 2. STRICT RULE FOR PERIOD 6:
        // - No real lesson in Thursday Lesson 6 (الخميس الدرس السادس مخصص حصراً للشواغر / النشاط ولا يوضع فيه درس).
        // - No Physics, Chemistry, Biology, or single/dual-period subjects (<= 2 حصص كالأخلاقية والدروس الثنائية) in Lesson 6.
        const cell6 = row.lessons.lesson6;
        if (cell6 && cell6.subject && cell6.subject !== 'شاغر / نشاط حر' && !cell6.isOff) {
          if (day === 'الخميس') {
            score -= 100000; // Violates rule: Thursday Lesson 6 must NOT have a real curriculum lesson!
          }
          const quota = subjectPeriodsMap.get(cell6.subject.trim())?.length || 2;
          if (isForbiddenInPeriod6(cell6.subject, quota)) {
            score -= 50000; // Severe violation: Science or <= 2 quota subject in Lesson 6!
          }
        }

        // 3. VACANT PERIOD RULE:
        // Vacant periods should strictly be in Lesson 6 at the end of the day, not in Lessons 1-5!
        for (let p = 0; p < 5; p++) {
          const key = LESSON_KEYS[p];
          if (row.lessons[key]?.subject === 'شاغر / نشاط حر') {
            score -= 15000; // Vacant period in the middle/start of the day is heavily penalized!
          }
        }
      }
    });

    // 4. Reward spreading recurring subjects across different days of the week
    subjectDaysMap.forEach((days, subj) => {
      score += days.size * 40; // e.g. 5 days = +200 points
    });

    // 5. Reward diversity of period indices (rotation between early, middle, and late)
    subjectPeriodsMap.forEach((periods) => {
      if (periods.length > 1) {
        const uniquePeriods = new Set(periods);
        score += uniquePeriods.size * 35; // Reward for rotating period index across days

        // Reward balance between early (periods 0, 1, 2) and late (periods 3, 4, 5)
        const earlyCount = periods.filter(p => p < 3).length;
        const lateCount = periods.filter(p => p >= 3).length;
        const balanceDiff = Math.abs(earlyCount - lateCount);
        score += Math.max(0, 10 - balanceDiff) * 15;
      }
    });

    // 6. LESSON 6 SECTION-LEVEL FAIRNESS (توازن الدرس السادس داخل الشعبة):
    // Reward having diverse teachers and subjects in Lesson 6 across the week without repeating
    const secP6Subjects = new Map<string, number>();
    const secP6Teachers = new Map<string, number>();
    DAYS_OF_WEEK.forEach(day => {
      const row = (schedule[day] || []).find(r => r.grade === grade && r.section === section);
      const c6 = row?.lessons.lesson6;
      if (c6 && c6.subject && c6.subject !== 'شاغر / نشاط حر' && !c6.isOff) {
        const s = c6.subject.trim();
        secP6Subjects.set(s, (secP6Subjects.get(s) || 0) + 1);
        if (c6.teacherName && c6.teacherName !== 'شاغر' && c6.teacherName !== 'أ. أستاذ المادة') {
          const t = c6.teacherName.trim();
          secP6Teachers.set(t, (secP6Teachers.get(t) || 0) + 1);
        }
      }
    });

    // Heavy penalty for repeating the same teacher or subject in Lesson 6 for this section
    secP6Subjects.forEach((count) => {
      if (count > 1) {
        score -= (count - 1) * 3000;
      } else {
        score += 80; // Reward unique subject in Lesson 6
      }
    });
    secP6Teachers.forEach((count) => {
      if (count > 1) {
        score -= (count - 1) * 3500;
      } else {
        score += 100; // Reward unique teacher in Lesson 6
      }
    });
  });

  // 7. LESSON 6 SCHOOL-WIDE FAIRNESS (توازن الدرس السادس بين جميع مدرسي المدرسة):
  // Ensure no teacher is disproportionately burdened with Lesson 6 across multiple sections
  const schoolP6TeacherLoads = new Map<string, number>();
  DAYS_OF_WEEK.forEach(day => {
    (schedule[day] || []).forEach(row => {
      const c6 = row.lessons.lesson6;
      if (c6 && c6.teacherName && c6.teacherName !== 'شاغر' && c6.teacherName !== 'أ. أستاذ المادة' && !c6.isOff && c6.subject !== 'شاغر / نشاط حر') {
        const t = c6.teacherName.trim();
        schoolP6TeacherLoads.set(t, (schoolP6TeacherLoads.get(t) || 0) + 1);
      }
    });
  });

  if (schoolP6TeacherLoads.size > 1) {
    const loads = Array.from(schoolP6TeacherLoads.values());
    const maxLoad = Math.max(...loads);
    const minLoad = Math.min(...loads);
    const spread = maxLoad - minLoad;
    if (spread > 1) {
      score -= (spread - 1) * 750; // Penalize uneven distribution among teachers
    }
  }

  return score;
}

/**
 * Intelligent Multi-Pass Constraint-Satisfaction Scheduler with Dynamic Lesson Rotation & Dispersion
 */
export function generateSmartFairSchedule(
  sections: SmartScheduleSection[],
  maxAttempts: number = 400,
  randomSeed: number = Date.now() + Math.random() * 10000
): { success: boolean; scheduleMap: DayScheduleMap; collisions: CollisionReport[]; errorMsg?: string } {
  if (!sections || sections.length === 0) {
    return {
      success: false,
      scheduleMap: createEmptyScheduleMap([]),
      collisions: [],
      errorMsg: 'يرجى إضافة صفوف وشعب أولاً قبل توليد الجدول.'
    };
  }

  // Pre-validate teacher workloads: No teacher can have > 30 lessons across all sections (5 days * 6 lessons = 30)
  const totalTeacherLessons = new Map<string, number>();
  sections.forEach(sec => {
    sec.subjects.forEach(sub => {
      const t = (sub.teacherName || '').trim();
      if (t && t !== 'شاغر') {
        const count = totalTeacherLessons.get(t) || 0;
        totalTeacherLessons.set(t, count + (sub.weeklyLessons || 0));
      }
    });
  });

  for (const [tName, count] of totalTeacherLessons.entries()) {
    if (count > 30) {
      return {
        success: false,
        scheduleMap: createEmptyScheduleMap(sections),
        collisions: [],
        errorMsg: `المعلم [${tName}] مكلف بـ (${count}) حصة أسبوعياً، والحد الأقصى المتاح أسبوعياً هو 30 حصة. يرجى تخفيف نصابه لتجنب التضارب المستحيل.`
      };
    }
  }

  let bestSchedule: DayScheduleMap = createEmptyScheduleMap(sections);
  let minCollisionsCount = Infinity;
  let bestFairnessScore = -Infinity;
  let bestCollisions: CollisionReport[] = [];

  // Attempt randomized heuristic backtracking with rotation preference & daily non-repetition balance
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const seed = randomSeed + attempt * 17.31 + Math.random() * 1000;
    const candidateMap = buildCandidateSchedule(sections, seed);
    const candidateRepaired = repairDailySubjectDuplicates(candidateMap);
    const candidateBalanced = balancePeriod6AcrossSchool(candidateRepaired);
    
    // Validate candidate
    const collisions = checkScheduleCollisions(candidateBalanced);
    const activeCandidate = collisions.length === 0 ? candidateBalanced : (checkScheduleCollisions(candidateRepaired).length === 0 ? candidateRepaired : candidateMap);
    const currentCollisions = checkScheduleCollisions(activeCandidate);

    if (currentCollisions.length === 0) {
      const fairness = calculateFairnessScore(activeCandidate);
      if (fairness > bestFairnessScore || minCollisionsCount > 0) {
        bestFairnessScore = fairness;
        minCollisionsCount = 0;
        bestSchedule = activeCandidate;
        bestCollisions = [];
      }

      // If we find an exceptionally fair collision-free schedule after several attempts, return it
      if (attempt > 30 && bestFairnessScore > 150) {
        return {
          success: true,
          scheduleMap: bestSchedule,
          collisions: []
        };
      }
    } else if (currentCollisions.length < minCollisionsCount) {
      minCollisionsCount = currentCollisions.length;
      bestSchedule = activeCandidate;
      bestCollisions = currentCollisions;
    }
  }

  // If 0 collision schedule was found among attempts, refine and return it
  if (minCollisionsCount === 0) {
    let finalClean = repairDailySubjectDuplicates(bestSchedule);
    finalClean = balancePeriod6AcrossSchool(finalClean);
    if (checkScheduleCollisions(finalClean).length > 0) {
      finalClean = bestSchedule; // safety fallback if balance introduced collision
    }
    return {
      success: true,
      scheduleMap: finalClean,
      collisions: []
    };
  }

  // If not 100% collision-free in random passes, apply deterministic conflict resolution swap pass
  let resolvedMap = resolveConflictsBySwapping(bestSchedule, sections);
  resolvedMap = repairDailySubjectDuplicates(resolvedMap);
  resolvedMap = balancePeriod6AcrossSchool(resolvedMap);
  const finalCollisions = checkScheduleCollisions(resolvedMap);

  return {
    success: finalCollisions.length === 0,
    scheduleMap: resolvedMap,
    collisions: finalCollisions,
    errorMsg: finalCollisions.length > 0 
      ? `تم توليد الجدول مع (${finalCollisions.length}) تضارب بسبب تشابك كبير في أنصبة بعض المعلمين. يمكنك تعديلها يدوياً بسهولة.`
      : undefined
  };
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
 * Internal single-pass heuristic builder with strict period rotation and end-of-week vacant placement
 */
function buildCandidateSchedule(sections: SmartScheduleSection[], seed: number): DayScheduleMap {
  const schedule = createEmptyScheduleMap(sections);

  // Track busy teachers: Map<day, Map<lessonKey, Set<teacherName>>>
  const teacherBusyMap: Record<DayOfWeek, Record<string, Set<string>>> = {
    'الأحد': { lesson1: new Set(), lesson2: new Set(), lesson3: new Set(), lesson4: new Set(), lesson5: new Set(), lesson6: new Set() },
    'الإثنين': { lesson1: new Set(), lesson2: new Set(), lesson3: new Set(), lesson4: new Set(), lesson5: new Set(), lesson6: new Set() },
    'الثلاثاء': { lesson1: new Set(), lesson2: new Set(), lesson3: new Set(), lesson4: new Set(), lesson5: new Set(), lesson6: new Set() },
    'الأربعاء': { lesson1: new Set(), lesson2: new Set(), lesson3: new Set(), lesson4: new Set(), lesson5: new Set(), lesson6: new Set() },
    'الخميس': { lesson1: new Set(), lesson2: new Set(), lesson3: new Set(), lesson4: new Set(), lesson5: new Set(), lesson6: new Set() },
  };

  // School-wide Lesson 6 load tracking to ensure even distribution among teachers
  const schoolWidePeriod6TeacherCount = new Map<string, number>();

  // Priority slots for vacant / free periods
  // Strictly assign vacant slots to the 6th lesson across days starting from end of week (Thursday, Wednesday, Tuesday, Monday, Sunday)
  // This guarantees all 5 days have Lessons 1 to 5 fully booked (25 core lessons), and extra 6th lessons / vacant slots are synchronized on the same days across the entire school!
  const endOfWeekVacantCandidates: { day: DayOfWeek; lessonKey: typeof LESSON_KEYS[number]; periodIndex: number }[] = [
    { day: 'الخميس', lessonKey: 'lesson6', periodIndex: 5 }, // 1st vacant -> Thursday Lesson 6
    { day: 'الأربعاء', lessonKey: 'lesson6', periodIndex: 5 }, // 2nd vacant -> Wednesday Lesson 6
    { day: 'الثلاثاء', lessonKey: 'lesson6', periodIndex: 5 }, // 3rd vacant -> Tuesday Lesson 6
    { day: 'الإثنين', lessonKey: 'lesson6', periodIndex: 5 }, // 4th vacant -> Monday Lesson 6
    { day: 'الأحد', lessonKey: 'lesson6', periodIndex: 5 }, // 5th vacant -> Sunday Lesson 6
    // Fallback only if total quota is less than 25 lessons:
    { day: 'الخميس', lessonKey: 'lesson5', periodIndex: 4 },
    { day: 'الأربعاء', lessonKey: 'lesson5', periodIndex: 4 },
    { day: 'الثلاثاء', lessonKey: 'lesson5', periodIndex: 4 },
    { day: 'الإثنين', lessonKey: 'lesson5', periodIndex: 4 },
    { day: 'الأحد', lessonKey: 'lesson5', periodIndex: 4 },
  ];

  // Shuffle sections order slightly to prevent earlier sections from always taking slot 0
  const shuffledSections = [...sections].sort((a, b) => {
    return pseudoRandom(seed + a.grade.length * 3 + a.section.charCodeAt(0)) - 0.5;
  });

  shuffledSections.forEach((sec, secIdx) => {
    // Collect real curriculum lessons
    let realLessons: { subject: string; teacher: string; priority: number; itemIndex: number }[] = [];
    let counter = 0;
    
    sec.subjects.forEach(sub => {
      const quota = Math.max(0, sub.weeklyLessons || 0);
      const isVacantSub = sub.subjectName.includes('شاغر') || (sub.teacherName && sub.teacherName === 'شاغر');
      if (!isVacantSub) {
        const priority = SUBJECT_TIME_PRIORITY[sub.subjectName.trim()] || 3;
        for (let i = 0; i < quota; i++) {
          realLessons.push({
            subject: sub.subjectName,
            teacher: sub.teacherName || 'أ. أستاذ المادة',
            priority,
            itemIndex: counter++
          });
        }
      }
    });

    if (realLessons.length > 30) {
      realLessons = realLessons.slice(0, 30);
    }

    // Number of vacant periods needed to reach 30 periods per week
    const vacantCount = Math.max(0, 30 - realLessons.length);

    // Row references in schedule
    const sectionRowRefs: Record<DayOfWeek, ClassScheduleRow | undefined> = {
      'الأحد': schedule['الأحد'].find(r => r.grade === sec.grade && r.section === sec.section),
      'الإثنين': schedule['الإثنين'].find(r => r.grade === sec.grade && r.section === sec.section),
      'الثلاثاء': schedule['الثلاثاء'].find(r => r.grade === sec.grade && r.section === sec.section),
      'الأربعاء': schedule['الأربعاء'].find(r => r.grade === sec.grade && r.section === sec.section),
      'الخميس': schedule['الخميس'].find(r => r.grade === sec.grade && r.section === sec.section),
    };

    // 1. Assign vacant periods directly to the end of the week
    const reservedVacantKeys = new Set<string>();
    // If section has any vacant slots (vacantCount >= 1), ensure Thursday Lesson 6 is the first vacant reserved!
    for (let v = 0; v < vacantCount && v < endOfWeekVacantCandidates.length; v++) {
      const vSlot = endOfWeekVacantCandidates[v];
      const row = sectionRowRefs[vSlot.day];
      if (row) {
        row.lessons[vSlot.lessonKey] = {
          subject: 'شاغر / نشاط حر',
          teacherName: 'شاغر',
          isOff: false
        };
        reservedVacantKeys.add(`${vSlot.day}_${vSlot.lessonKey}`);
      }
    }

    // 2. Dynamic shuffle of real lessons with rotation & priority
    realLessons.sort((a, b) => {
      if (a.priority !== b.priority) {
        const pDiff = a.priority - b.priority;
        const noise = (pseudoRandom(seed + a.itemIndex * 7) - 0.5) * 0.4;
        return pDiff + noise;
      }
      return pseudoRandom(seed + a.itemIndex * 13 + secIdx) - 0.5;
    });

    // 3. Build candidate slot pool for real lessons (excluding reserved vacant slots)
    const daySubjectCounts: Record<DayOfWeek, Record<string, number>> = {
      'الأحد': {}, 'الإثنين': {}, 'الثلاثاء': {}, 'الأربعاء': {}, 'الخميس': {}
    };
    const secPeriod6SubjectCount = new Map<string, number>();
    const secPeriod6TeacherCount = new Map<string, number>();
    const subjectAssignedPeriods: Record<string, number[]> = {};
    const subjectQuotas: Record<string, number> = {};
    realLessons.forEach(l => { subjectQuotas[l.subject] = (subjectQuotas[l.subject] || 0) + 1; });

    const availableSlots: { day: DayOfWeek; dayIndex: number; lessonKey: typeof LESSON_KEYS[number]; periodIndex: number }[] = [];
    
    // Stagger days order per section for diverse day allocations
    const dayShift = Math.floor(pseudoRandom(seed + secIdx * 19) * 5);
    const staggeredDays = [...DAYS_OF_WEEK.slice(dayShift), ...DAYS_OF_WEEK.slice(0, dayShift)];

    staggeredDays.forEach((day, dIdx) => {
      const periodRotationOffset = (dIdx * 2 + secIdx) % 6;
      for (let p = 0; p < 6; p++) {
        const periodIndex = (p + periodRotationOffset) % 6;
        const lessonKey = LESSON_KEYS[periodIndex];
        const slotKey = `${day}_${lessonKey}`;
        // If Thursday Lesson 6: strictly disallow adding it as an available slot for real lessons if total quota <= 29
        if (day === 'الخميس' && lessonKey === 'lesson6' && realLessons.length < 30) {
          continue;
        }
        if (!reservedVacantKeys.has(slotKey)) {
          availableSlots.push({ day, dayIndex: dIdx, lessonKey, periodIndex });
        }
      }
    });

    // 4. Allocate each real curriculum lesson to the best rotating slot
    for (const item of realLessons) {
      const isTeacherSpecial = item.teacher && item.teacher !== 'شاغر';
      const prevPeriods = subjectAssignedPeriods[item.subject] || [];

      let bestSlotIndex = -1;
      let lowestPenalty = Infinity;

      for (let sIdx = 0; sIdx < availableSlots.length; sIdx++) {
        const slot = availableSlots[sIdx];
        const row = sectionRowRefs[slot.day];
        if (!row) continue;

        // Check if slot is already occupied
        const currentCell = row.lessons[slot.lessonKey];
        if (currentCell.subject !== '') continue;

        let penalty = 0;

        // Hard Constraint 1: Teacher Conflict
        if (isTeacherSpecial && teacherBusyMap[slot.day][slot.lessonKey].has(item.teacher)) {
          penalty += 15000;
        }

        // Hard Constraint 2: Strictly prevent repeating the same subject on the same day for the same section
        const existingCountToday = daySubjectCounts[slot.day][item.subject] || 0;
        const totalQuota = subjectQuotas[item.subject] || 1;
        const maxAllowedPerDay = Math.ceil(totalQuota / 5);

        if (existingCountToday >= maxAllowedPerDay) {
          // Hard violation: exceeds mathematical maximum for this day!
          penalty += 40000;
        } else if (existingCountToday >= 1) {
          // Soft violation: already has a lesson today, strongly prefer empty days
          penalty += 15000;
        } else {
          // Reward placing on an empty day
          penalty -= 80;
        }

        // Hard Constraint 0: Strict Period 6 Rules
        // Rule A: Thursday Lesson 6 is strictly forbidden for any curriculum lesson (ممنوع وضع أي درس في الدرس السادس يوم الخميس)
        // Rule B: Physics, Chemistry, Biology, and single/dual-quota subjects (<= 2 كالأخلاقية والدروس الثنائية) CANNOT be placed in Lesson 6
        if (slot.periodIndex === 5) {
          if (slot.day === 'الخميس') {
            penalty += 2000000; // Strictly forbidden: No curriculum lesson in Thursday Lesson 6!
          }
          const quota = subjectQuotas[item.subject] || 1;
          if (isForbiddenInPeriod6(item.subject, quota)) {
            penalty += 1000000; // Impossible / strictly forbidden in Lesson 6 (Science or Quota <= 2)!
          } else {
            // FAIR BALANCE IN LESSON 6 (توازن الدرس السادس بين المدرسين والمواد المسموحة):
            // 1. Heavy penalty if this section ALREADY has this subject in Lesson 6 this week
            const secSubjP6Count = secPeriod6SubjectCount.get(item.subject) || 0;
            if (secSubjP6Count > 0) {
              penalty += 7500 * secSubjP6Count;
            }

            // 2. Heavy penalty if this section ALREADY has this teacher in Lesson 6 this week
            if (isTeacherSpecial) {
              const secTeachP6Count = secPeriod6TeacherCount.get(item.teacher) || 0;
              if (secTeachP6Count > 0) {
                penalty += 8500 * secTeachP6Count;
              }

              // 3. School-wide balance: penalize loading the same teacher with multiple Lesson 6 slots across sections
              const schoolWideP6Count = schoolWidePeriod6TeacherCount.get(item.teacher) || 0;
              penalty += schoolWideP6Count * 1800;
            }
          }
        }

        // Critical Fairness Constraint 3: Rotation and dynamic balance across periods (Prevent fixing in Lesson 1!)
        if (prevPeriods.includes(slot.periodIndex)) {
          const samePeriodCount = prevPeriods.filter(p => p === slot.periodIndex).length;
          penalty += 850 * samePeriodCount; // Strongly penalize fixing the subject in the same period
        }

        // Dynamic Balance between beginning (lessons 1, 2, 3) and end (lessons 4, 5, 6) of the day:
        if (prevPeriods.length > 0) {
          const lastPeriod = prevPeriods[prevPeriods.length - 1];
          const lastWasEarly = lastPeriod < 3;
          const currentIsEarly = slot.periodIndex < 3;
          if (lastWasEarly === currentIsEarly) {
            penalty += 140; // Prefer alternating early and later
          } else {
            penalty -= 90; // Reward balance
          }
        }

        // Constraint 4: Pedagogical slot preference with natural rotation
        if (item.priority <= 2) {
          // Allow core subjects (Math, Arabic, English) to take their fair turn in Lesson 6 with slight nudge
          if (slot.periodIndex === 5) penalty += 50;
        } else if (item.priority >= 5) {
          if (slot.periodIndex <= 1) penalty += 60;
        }

        // Slight randomness to break ties
        const tieBreaker = (pseudoRandom(seed + sIdx * 11 + item.itemIndex) - 0.5) * 5;
        const totalPenalty = penalty + tieBreaker;

        if (totalPenalty < lowestPenalty) {
          lowestPenalty = totalPenalty;
          bestSlotIndex = sIdx;
        }
      }

      // Assign to best slot found
      if (bestSlotIndex !== -1) {
        const chosenSlot = availableSlots[bestSlotIndex];
        const row = sectionRowRefs[chosenSlot.day];
        if (row) {
          row.lessons[chosenSlot.lessonKey] = {
            subject: item.subject,
            teacherName: item.teacher,
            isOff: false
          };

          if (isTeacherSpecial) {
            teacherBusyMap[chosenSlot.day][chosenSlot.lessonKey].add(item.teacher);
          }

          daySubjectCounts[chosenSlot.day][item.subject] = (daySubjectCounts[chosenSlot.day][item.subject] || 0) + 1;
          
          if (!subjectAssignedPeriods[item.subject]) {
            subjectAssignedPeriods[item.subject] = [];
          }
          subjectAssignedPeriods[item.subject].push(chosenSlot.periodIndex);

          // Track Lesson 6 assignments for section and school
          if (chosenSlot.periodIndex === 5) {
            secPeriod6SubjectCount.set(item.subject, (secPeriod6SubjectCount.get(item.subject) || 0) + 1);
            if (isTeacherSpecial) {
              secPeriod6TeacherCount.set(item.teacher, (secPeriod6TeacherCount.get(item.teacher) || 0) + 1);
              schoolWidePeriod6TeacherCount.set(item.teacher, (schoolWidePeriod6TeacherCount.get(item.teacher) || 0) + 1);
            }
          }
        }
      } else {
        // Fallback: If heuristic penalty rejected all slots, pick the first unoccupied slot in availableSlots
        for (const slot of availableSlots) {
          const row = sectionRowRefs[slot.day];
          if (row && row.lessons[slot.lessonKey].subject === '') {
            row.lessons[slot.lessonKey] = {
              subject: item.subject,
              teacherName: item.teacher,
              isOff: false
            };
            if (isTeacherSpecial) {
              teacherBusyMap[slot.day][slot.lessonKey].add(item.teacher);
            }
            daySubjectCounts[slot.day][item.subject] = (daySubjectCounts[slot.day][item.subject] || 0) + 1;
            if (slot.periodIndex === 5) {
              secPeriod6SubjectCount.set(item.subject, (secPeriod6SubjectCount.get(item.subject) || 0) + 1);
              if (isTeacherSpecial) {
                secPeriod6TeacherCount.set(item.teacher, (secPeriod6TeacherCount.get(item.teacher) || 0) + 1);
                schoolWidePeriod6TeacherCount.set(item.teacher, (schoolWidePeriod6TeacherCount.get(item.teacher) || 0) + 1);
              }
            }
            break;
          }
        }
      }
    }

    // 5. Fill any unallocated slots as vacant
    DAYS_OF_WEEK.forEach(day => {
      const row = sectionRowRefs[day];
      if (row) {
        LESSON_KEYS.forEach(lKey => {
          if (row.lessons[lKey].subject === '') {
            row.lessons[lKey] = {
              subject: 'شاغر / نشاط حر',
              teacherName: 'شاغر',
              isOff: false
            };
          }
        });
      }
    });
  });

  return schedule;
}

/**
 * Secondary refinement pass to swap conflicting cells into free compatible slots.
 */
function resolveConflictsBySwapping(schedule: DayScheduleMap, sections: SmartScheduleSection[]): DayScheduleMap {
  const result: DayScheduleMap = JSON.parse(JSON.stringify(schedule));
  
  DAYS_OF_WEEK.forEach(day => {
    const rows = result[day] || [];
    LESSON_KEYS.forEach(lessonKey => {
      const seenTeachers = new Map<string, number[]>(); // teacher -> row indexes

      rows.forEach((row, rIdx) => {
        const cell = row.lessons[lessonKey];
        if (cell && cell.teacherName && cell.teacherName !== 'شاغر') {
          if (!seenTeachers.has(cell.teacherName)) {
            seenTeachers.set(cell.teacherName, []);
          }
          seenTeachers.get(cell.teacherName)!.push(rIdx);
        }
      });

      // For any teacher found in multiple rows in this slot, try swapping one row with another lesson in the same day
      seenTeachers.forEach((rowIndices, teacher) => {
        if (rowIndices.length > 1) {
          for (let i = 1; i < rowIndices.length; i++) {
            const rowIndexToSwap = rowIndices[i];
            const targetRow = rows[rowIndexToSwap];
            
            for (const otherLessonKey of LESSON_KEYS) {
              if (otherLessonKey === lessonKey) continue;
              const otherCell = targetRow.lessons[otherLessonKey];
              
              // STRICT RULE: Check Lesson 6 rule for Physics, Chemistry, Biology, Ethics, and Vacant slots
              const currentSubject = targetRow.lessons[lessonKey]?.subject || '';
              const otherSubject = otherCell?.subject || '';
              
              // Thursday Lesson 6 must never receive a real curriculum lesson
              if (day === 'الخميس' && (lessonKey === 'lesson6' || otherLessonKey === 'lesson6')) {
                if (lessonKey === 'lesson6' && otherSubject !== 'شاغر / نشاط حر') continue;
                if (otherLessonKey === 'lesson6' && currentSubject !== 'شاغر / نشاط حر') continue;
              }

              if (otherLessonKey === 'lesson6' && isForbiddenInPeriod6(currentSubject, 2)) continue;
              if (lessonKey === 'lesson6' && isForbiddenInPeriod6(otherSubject, 2)) continue;

              // Do not swap vacant slot into lessons 1-5
              if (currentSubject === 'شاغر / نشاط حر' && otherLessonKey !== 'lesson6') continue;
              if (otherSubject === 'شاغر / نشاط حر' && lessonKey !== 'lesson6') continue;

              const isTeacherFreeInOther = !rows.some(r => r.lessons[otherLessonKey]?.teacherName === teacher);
              const isOtherTeacherFreeInCurrent = !otherCell.teacherName || otherCell.teacherName === 'شاغر' || 
                !rows.some((r, idx) => idx !== rowIndexToSwap && r.lessons[lessonKey]?.teacherName === otherCell.teacherName);

              if (isTeacherFreeInOther && isOtherTeacherFreeInCurrent) {
                // Swap the two cells
                const temp = { ...targetRow.lessons[lessonKey] };
                targetRow.lessons[lessonKey] = { ...otherCell };
                targetRow.lessons[otherLessonKey] = temp;
                break;
              }
            }
          }
        }
      });
    });
  });

  return result;
}

/**
 * Inter-day swap pass to balance subjects across days and eliminate any accidental same-day subject duplicates.
 */
export function repairDailySubjectDuplicates(schedule: DayScheduleMap): DayScheduleMap {
  const result: DayScheduleMap = JSON.parse(JSON.stringify(schedule));
  
  const allSections: { grade: string; section: string }[] = [];
  (result['الأحد'] || []).forEach(row => {
    allSections.push({ grade: row.grade, section: row.section });
  });

  allSections.forEach(sec => {
    // For each day, find subjects with count > 1
    for (const dayA of DAYS_OF_WEEK) {
      const rowA = (result[dayA] || []).find(r => r.grade === sec.grade && r.section === sec.section);
      if (!rowA) continue;

      const subCountsA = new Map<string, typeof LESSON_KEYS[number][]>();
      LESSON_KEYS.forEach(lKey => {
        const cell = rowA.lessons[lKey];
        if (cell && cell.subject && cell.subject !== 'شاغر / نشاط حر' && !cell.isOff) {
          const list = subCountsA.get(cell.subject) || [];
          list.push(lKey);
          subCountsA.set(cell.subject, list);
        }
      });

      subCountsA.forEach((slotsA, subjA) => {
        if (slotsA.length > 1) {
          // We have duplicates of subjA on dayA! Try to move the second one (slotsA[1]) to another dayB that doesn't have subjA
          const slotToMoveA = slotsA[1];
          const cellA = rowA.lessons[slotToMoveA];

          for (const dayB of DAYS_OF_WEEK) {
            if (dayB === dayA) continue;
            const rowB = (result[dayB] || []).find(r => r.grade === sec.grade && r.section === sec.section);
            if (!rowB) continue;

            const hasSubjAInDayB = LESSON_KEYS.some(k => rowB.lessons[k]?.subject === subjA);
            if (hasSubjAInDayB) continue; // dayB already has subjA, skip

            // Find a slot in dayB that can be swapped with slotToMoveA
            for (const slotB of LESSON_KEYS) {
              const cellB = rowB.lessons[slotB];
              if (!cellB || cellB.isOff) continue;

              // STRICT RULE: Check Lesson 6 rule
              // Thursday Lesson 6 must never receive a real curriculum lesson
              if (dayB === 'الخميس' && slotB === 'lesson6' && cellA.subject !== 'شاغر / نشاط حر') continue;
              if (dayA === 'الخميس' && slotToMoveA === 'lesson6' && cellB.subject !== 'شاغر / نشاط حر') continue;

              if (slotB === 'lesson6' && isForbiddenInPeriod6(cellA.subject, 2)) continue;
              if (slotToMoveA === 'lesson6' && isForbiddenInPeriod6(cellB.subject, 2)) continue;

              // Vacant slot must remain in lesson6
              if (cellA.subject === 'شاغر / نشاط حر' && slotB !== 'lesson6') continue;
              if (cellB.subject === 'شاغر / نشاط حر' && slotToMoveA !== 'lesson6') continue;

              const hasSubjBInDayA = cellB.subject !== 'شاغر / نشاط حر' && LESSON_KEYS.some(k => k !== slotToMoveA && rowA.lessons[k]?.subject === cellB.subject);
              if (hasSubjBInDayA) continue; // swapping would create duplicate of subjB in dayA!

              // Check teacher collisions if we place cellA at dayB[slotB] and cellB at dayA[slotToMoveA]
              const isTeacherAFreeInDayBSlotB = !cellA.teacherName || cellA.teacherName === 'شاغر' ||
                !(result[dayB] || []).some(r => r.id !== rowB.id && r.lessons[slotB]?.teacherName === cellA.teacherName);

              const isTeacherBFreeInDayASlotA = !cellB.teacherName || cellB.teacherName === 'شاغر' ||
                !(result[dayA] || []).some(r => r.id !== rowA.id && r.lessons[slotToMoveA]?.teacherName === cellB.teacherName);

              if (isTeacherAFreeInDayBSlotB && isTeacherBFreeInDayASlotA) {
                // Perform the swap!
                const temp = { ...cellA };
                rowA.lessons[slotToMoveA] = { ...cellB };
                rowB.lessons[slotB] = temp;
                break;
              }
            }
          }
        }
      });
    }
  });

  return result;
}

/**
 * Intra-section and school-wide balancing pass specifically for Lesson 6.
 * Ensures Lesson 6 rotates equitably among permitted subjects and teachers without repeated clustering.
 */
export function balancePeriod6AcrossSchool(schedule: DayScheduleMap): DayScheduleMap {
  const result: DayScheduleMap = JSON.parse(JSON.stringify(schedule));

  const allSections: { grade: string; section: string }[] = [];
  (result['الأحد'] || []).forEach(row => {
    allSections.push({ grade: row.grade, section: row.section });
  });

  allSections.forEach(sec => {
    // 1. Check Section-level Lesson 6 distribution across the 5 days
    const p6Occurrences: { day: DayOfWeek; subject: string; teacher: string }[] = [];
    DAYS_OF_WEEK.forEach(day => {
      const row = (result[day] || []).find(r => r.grade === sec.grade && r.section === sec.section);
      const c6 = row?.lessons.lesson6;
      if (c6 && c6.subject && c6.subject !== 'شاغر / نشاط حر' && !c6.isOff) {
        p6Occurrences.push({ day, subject: c6.subject.trim(), teacher: c6.teacherName?.trim() || '' });
      }
    });

    // Find any repeated subject or teacher in Lesson 6 for this section
    const subjectCounts = new Map<string, number>();
    const teacherCounts = new Map<string, number>();
    p6Occurrences.forEach(occ => {
      subjectCounts.set(occ.subject, (subjectCounts.get(occ.subject) || 0) + 1);
      if (occ.teacher && occ.teacher !== 'شاغر' && occ.teacher !== 'أ. أستاذ المادة') {
        teacherCounts.set(occ.teacher, (teacherCounts.get(occ.teacher) || 0) + 1);
      }
    });

    // Try to swap repeated Lesson 6 entries with compatible lessons in earlier periods of that same day (e.g. lesson4 or lesson5)
    DAYS_OF_WEEK.forEach(day => {
      // Thursday Lesson 6 is strictly vacant / activity; do not swap curriculum lessons into it
      if (day === 'الخميس') return;

      const row = (result[day] || []).find(r => r.grade === sec.grade && r.section === sec.section);
      if (!row) return;

      const c6 = row.lessons.lesson6;
      if (!c6 || !c6.subject || c6.subject === 'شاغر / نشاط حر' || c6.isOff) return;

      const currentSubj = c6.subject.trim();
      const currentTeacher = c6.teacherName?.trim() || '';
      const isSubjRepeated = (subjectCounts.get(currentSubj) || 0) > 1;
      const isTeacherRepeated = currentTeacher && (teacherCounts.get(currentTeacher) || 0) > 1;

      if (isSubjRepeated || isTeacherRepeated) {
        // Look for candidate slots in the same day (prefer lesson5 or lesson4) whose subject is permitted in Lesson 6
        const candidateKeys: (typeof LESSON_KEYS[number])[] = ['lesson5', 'lesson4', 'lesson3'];
        for (const candKey of candidateKeys) {
          const candCell = row.lessons[candKey];
          if (!candCell || !candCell.subject || candCell.subject === 'شاغر / نشاط حر' || candCell.isOff) continue;

          const candSubj = candCell.subject.trim();
          const candTeacher = candCell.teacherName?.trim() || '';

          // Must be permitted in Lesson 6
          if (isForbiddenInPeriod6(candSubj, 2)) continue;

          // Candidate subject/teacher should not already be heavily present in Lesson 6 for this section
          if ((subjectCounts.get(candSubj) || 0) >= 1) continue;
          if (candTeacher && (teacherCounts.get(candTeacher) || 0) >= 1) continue;

          // Check collisions: If we swap current Lesson 6 and candKey in row on `day`
          // Does currentTeacher clash with another section in candKey?
          const currentTeacherCollides = currentTeacher && currentTeacher !== 'شاغر' &&
            (result[day] || []).some(r => r.id !== row.id && r.lessons[candKey]?.teacherName === currentTeacher);

          // Does candTeacher clash with another section in lesson6?
          const candTeacherCollides = candTeacher && candTeacher !== 'شاغر' &&
            (result[day] || []).some(r => r.id !== row.id && r.lessons.lesson6?.teacherName === candTeacher);

          if (!currentTeacherCollides && !candTeacherCollides) {
            // Swap safely!
            const temp = { ...c6 };
            row.lessons.lesson6 = { ...candCell };
            row.lessons[candKey] = temp;

            // Update local count trackers
            subjectCounts.set(currentSubj, (subjectCounts.get(currentSubj) || 1) - 1);
            subjectCounts.set(candSubj, (subjectCounts.get(candSubj) || 0) + 1);
            if (currentTeacher) {
              teacherCounts.set(currentTeacher, (teacherCounts.get(currentTeacher) || 1) - 1);
            }
            if (candTeacher) {
              teacherCounts.set(candTeacher, (teacherCounts.get(candTeacher) || 0) + 1);
            }
            break;
          }
        }
      }
    });
  });

  return result;
}
