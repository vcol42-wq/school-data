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
      }
    });

    // 2. Reward spreading recurring subjects across different days of the week
    subjectDaysMap.forEach((days, subj) => {
      score += days.size * 40; // e.g. 5 days = +200 points
    });

    // 3. Reward diversity of period indices (rotation)
    subjectPeriodsMap.forEach((periods) => {
      if (periods.length > 1) {
        const uniquePeriods = new Set(periods);
        score += uniquePeriods.size * 25; // Reward for distributing across different lesson numbers
      }
    });
  });

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
    const collisions = checkScheduleCollisions(candidateRepaired);
    const activeCandidate = collisions.length === 0 ? candidateRepaired : candidateMap;
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
    const finalClean = repairDailySubjectDuplicates(bestSchedule);
    return {
      success: true,
      scheduleMap: finalClean,
      collisions: []
    };
  }

  // If not 100% collision-free in random passes, apply deterministic conflict resolution swap pass
  let resolvedMap = resolveConflictsBySwapping(bestSchedule, sections);
  resolvedMap = repairDailySubjectDuplicates(resolvedMap);
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

        // Critical Fairness Constraint 3: Rotation across periods (Prevent fixing in Lesson 1!)
        if (prevPeriods.includes(slot.periodIndex)) {
          const samePeriodCount = prevPeriods.filter(p => p === slot.periodIndex).length;
          penalty += 450 * samePeriodCount;
        }

        // Constraint 4: Pedagogical slot preference with natural rotation
        if (item.priority <= 2) {
          if (slot.periodIndex === 5) penalty += 50;
        } else if (item.priority >= 5) {
          if (slot.periodIndex <= 1) penalty += 40;
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
