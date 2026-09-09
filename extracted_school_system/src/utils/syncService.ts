import { getSupabase } from './supabaseClient';
import { Student, StaffMember, StudentMark, DayScheduleMap } from '../types';
import { 
  standardizeGradeName, 
  standardizeSectionName, 
  standardizeSubjectName, 
  isValidSubjectName, 
  isExemptStaff, 
  parseClassTaught 
} from './syncEngine';

// Helper to normalize Arabic characters for comparison
export function normalizeArabic(str: string): string {
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

/**
 * Exports all manager school data (school config, teachers, classes, subjects, assignments, students)
 * directly to Supabase tables. Uses RLS isolation via the 'x-school-id' header.
 */
export async function exportSchoolData(
  schoolId: string,
  schoolName: string,
  pairingCode: string,
  adminEmail: string,
  students: Student[],
  staff: StaffMember[],
  scheduleMap?: DayScheduleMap
): Promise<{ success: boolean; message: string }> {
  try {
    const client = getSupabase(schoolId);

    // 1. Upsert School configuration
    const schoolsPayload = [
      {
        id: schoolId,
        name: schoolName,
        pairing_code: pairingCode,
        admin_email: adminEmail
      }
    ];

    const { error: schoolError } = await client
      .from('schools')
      .upsert(schoolsPayload, { onConflict: 'id', ignoreDuplicates: false });

    if (schoolError) throw new Error(`School Sync Error: ${schoolError.message}`);

    // 1.5. Upsert School Schedule Map
    const finalScheduleMap = (scheduleMap && Object.keys(scheduleMap).length > 0)
      ? scheduleMap
      : { 'الأحد': [], 'الإثنين': [], 'الثلاثاء': [], 'الأربعاء': [], 'الخميس': [] };

    const { error: scheduleError } = await client
      .from('schedules')
      .upsert({
        id: schoolId,
        schedule_map: finalScheduleMap
      }, { onConflict: 'id', ignoreDuplicates: false });

    if (scheduleError) {
      console.warn('Schedule Sync Warning:', scheduleError.message);
    }

    // 2. Export Teachers (Clean old duplicates and use stable IDs)
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
      // Purge previous duplicate teacher records for clean state
      await client.from('teachers').delete().eq('school_id', schoolId);

      const { error: teacherError } = await client
        .from('teachers')
        .upsert(teachersPayload, { onConflict: 'id', ignoreDuplicates: false });

      if (teacherError) throw new Error(`Teachers Sync Error: ${teacherError.message}`);
    }

    // 3. Extract and Export unique Classes (from BOTH students AND scheduleMap with strict standardization)
    const uniqueClassesMap = new Map<string, { school_id: string; name: string; section: string }>();

    // 3.1 From Students Roster
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

    // 3.2 From Weekly Schedule
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
      // Purge old classes before upserting clean standardized classes
      try {
        await client.from('classes').delete().eq('school_id', schoolId);
      } catch (delErr) {
        console.warn('Notice clearing previous classes:', delErr);
      }

      const { error: classError } = await client
        .from('classes')
        .upsert(classesPayload, { onConflict: 'school_id,name,section', ignoreDuplicates: false });

      if (classError) throw new Error(`Classes Sync Error: ${classError.message}`);
    }

    // 4 & 5. Extract Unique Subjects and Teacher Assignments (Prioritizing Assigned Schedule Lessons)
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

    // 1. PRIMARY SOURCE: Live Timetable Schedule Slots (المطابقة المباشرة مع الجدول)
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

      const { error: subjectError } = await client
        .from('subjects')
        .upsert(subjectsPayload, { onConflict: 'school_id,name', ignoreDuplicates: false });

      if (subjectError) throw new Error(`Subjects Sync Error: ${subjectError.message}`);
    }

    const assignmentsPayload = Array.from(assignmentMap.values());

    if (assignmentsPayload.length > 0) {
      // Purge old assignments for this school to avoid stale cross-teacher allocations
      await client.from('teacher_assignments').delete().eq('school_id', schoolId);

      const { error: assignmentError } = await client
        .from('teacher_assignments')
        .upsert(assignmentsPayload, {
          onConflict: 'school_id,teacher_id,class_name,section,subject_name',
          ignoreDuplicates: false
        });

      if (assignmentError) throw new Error(`Assignments Sync Error: ${assignmentError.message}`);
    }

    // 6. Export Students (Ensuring full_name is complete and Alphabetically Sorted)
    const sortedStudents = [...students].sort((a, b) => {
      const nameA = [a.firstName, a.secondName, a.thirdName, a.fourthName, a.titleName].filter(Boolean).join(' ').trim();
      const nameB = [b.firstName, b.secondName, b.thirdName, b.fourthName, b.titleName].filter(Boolean).join(' ').trim();
      return nameA.localeCompare(nameB, 'ar', { sensitivity: 'base' });
    });

    const studentsPayload = sortedStudents.map(std => {
      const computedFullName = [std.firstName, std.secondName, std.thirdName, std.fourthName, std.titleName]
        .filter(Boolean)
        .join(' ')
        .trim() || (std.fullName && std.fullName.trim()) || std.firstName;

      return {
        school_id: schoolId,
        record_number: std.recordNumber || String(Math.floor(1000 + Math.random() * 9000)),
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

    if (studentsPayload.length > 0) {
      const { error: studentError } = await client
        .from('students')
        .upsert(studentsPayload, { onConflict: 'school_id,record_number', ignoreDuplicates: false });

      if (studentError) throw new Error(`Students Sync Error: ${studentError.message}`);
    }

    return { success: true, message: 'تم تصدير كافة الجداول والبيانات إلى السحاب بنجاح!' };
  } catch (error: any) {
    console.error('Export Error:', error);
    return { success: false, message: error.message || 'حدث خطأ غير متوقع أثناء تصدير البيانات.' };
  }
}

/**
 * Imports all student grades and attendance records from Supabase
/**
 * Imports all student grades and attendance records from Supabase,
 * detects any new students added by teachers or cloud,
 * and merges them back into the Manager's local students array state.
 */
export async function importGradesAndAttendance(
  schoolId: string,
  currentStudents: Student[]
): Promise<{ success: boolean; message: string; updatedStudents?: Student[]; newStudentsCount?: number }> {
  try {
    const client = getSupabase(schoolId);

    // 1. Fetch all cloud students matching school_id
    const { data: dbCloudStudents, error: cloudStudentsError } = await client
      .from('students')
      .select('*')
      .eq('school_id', schoolId);

    if (cloudStudentsError) {
      console.warn('Notice fetching cloud students:', cloudStudentsError.message);
    }

    // 2. Fetch all grades matching school_id
    const { data: dbGrades, error: gradesError } = await client
      .from('grades')
      .select('*')
      .eq('school_id', schoolId);

    if (gradesError) throw new Error(`Pull Grades Error: ${gradesError.message}`);

    // 3. Fetch all attendance matching school_id
    const { data: dbAttendance, error: attendanceError } = await client
      .from('attendance')
      .select('*')
      .eq('school_id', schoolId);

    if (attendanceError) throw new Error(`Pull Attendance Error: ${attendanceError.message}`);

    // 4. Identify any students from cloud that are NOT in currentStudents
    // (e.g. added by teacher in mobile app)
    const newStudents: Student[] = [];
    const existingRecs = new Set<string>();
    const existingIds = new Set<string>();
    const existingNameKeys = new Set<string>();

    currentStudents.forEach(s => {
      if (s.recordNumber) existingRecs.add(s.recordNumber.trim());
      if (s.id) existingIds.add(s.id.trim());
      const nKey = `${normalizeArabic(s.fullName || `${s.firstName} ${s.secondName}`)}_${standardizeGradeName(s.currentGrade)}_${standardizeSectionName(s.section)}`;
      existingNameKeys.add(nKey);
    });

    (dbCloudStudents || []).forEach(cs => {
      const csRec = (cs.record_number || '').trim();
      const csId = (cs.id || '').trim();
      const csNameKey = `${normalizeArabic(cs.full_name || `${cs.first_name} ${cs.second_name || ''}`)}_${standardizeGradeName(cs.current_grade)}_${standardizeSectionName(cs.section)}`;

      const exists = (csRec && existingRecs.has(csRec)) ||
                     (csId && existingIds.has(csId)) ||
                     existingNameKeys.has(csNameKey);

      if (!exists) {
        // This student was added by a teacher in the mobile app!
        const fullName = (cs.full_name || `${cs.first_name} ${cs.second_name || ''} ${cs.third_name || ''}`).trim();
        const parts = fullName.split(/\s+/);
        const newStd: Student = {
          id: cs.id || `std_t_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
          recordNumber: cs.record_number || `REC-${Math.floor(1000 + Math.random() * 9000)}`,
          firstName: cs.first_name || parts[0] || 'طالب جديد',
          secondName: cs.second_name || parts[1] || '',
          thirdName: cs.third_name || parts[2] || '',
          fourthName: cs.fourth_name || parts.slice(3).join(' ') || '',
          titleName: cs.title_name || '',
          fullName: fullName,
          currentGrade: standardizeGradeName(cs.current_grade || 'الأول الابتدائي'),
          section: standardizeSectionName(cs.section || 'أ'),
          status: (cs.status as any) || 'مستمر',
          absencesCount: cs.absences_count || 0,
          registrationYear: '2025-2026',
          marksHistory: [],
          addedByTeacher: true,
          teacherName: 'الأستاذ'
        };
        newStudents.push(newStd);
        if (csRec) existingRecs.add(csRec);
        if (csId) existingIds.add(csId);
        existingNameKeys.add(csNameKey);
      }
    });

    // Also check if dbGrades has student_record_number that is neither in currentStudents nor in newStudents
    (dbGrades || []).forEach(g => {
      const gRec = (g.student_record_number || '').trim();
      if (!gRec) return;
      const exists = existingRecs.has(gRec) || existingIds.has(gRec);
      if (!exists) {
        const studentName = g.marks?.studentName || `طالب (${gRec})`;
        const parts = studentName.split(/\s+/);
        const newStd: Student = {
          id: `std_grade_${gRec}`,
          recordNumber: gRec,
          firstName: parts[0] || studentName,
          secondName: parts[1] || '',
          thirdName: parts[2] || '',
          fourthName: parts.slice(3).join(' ') || '',
          titleName: '',
          fullName: studentName,
          currentGrade: standardizeGradeName(g.grade || 'الأول الابتدائي'),
          section: standardizeSectionName(g.section || 'أ'),
          status: 'مستمر',
          absencesCount: 0,
          registrationYear: '2025-2026',
          marksHistory: [],
          addedByTeacher: true,
          teacherName: 'الأستاذ'
        };
        newStudents.push(newStd);
        existingRecs.add(gRec);
      }
    });

    // Merge base students with newly discovered teacher-added students
    const allStudentsToProcess = [...currentStudents, ...newStudents];

    // 5. Map grades & attendance to all students
    const updatedStudents = allStudentsToProcess.map(std => {
      const stdRec = (std.recordNumber || '').trim();
      const stdAltId = `std_${std.id}`.trim();
      const stdGradeNorm = standardizeGradeName(std.currentGrade);
      const stdSecNorm = standardizeSectionName(std.section);
      const stdNormName = normalizeArabic(std.fullName || `${std.firstName} ${std.secondName} ${std.thirdName || ''}`.trim());

      const studentGrades = (dbGrades || []).filter(g => {
        const gRec = (g.student_record_number || '').trim();
        const recMatches = (stdRec && gRec === stdRec) || (stdRec && gRec.includes(stdRec)) || gRec === stdAltId;
        
        // Match by name as fallback
        const gName = g.marks?.studentName || '';
        const nameMatches = gName && normalizeArabic(gName) === stdNormName;

        if (!recMatches && !nameMatches) return false;

        // If cloud grade row has grade & section, ensure they match to prevent cross-over
        if (g.grade && g.section) {
          return standardizeGradeName(g.grade) === stdGradeNorm && standardizeSectionName(g.section) === stdSecNorm;
        }
        return true;
      });

      const studentAbsenceCount = (dbAttendance || []).filter(a => {
        const aRec = (a.student_record_number || '').trim();
        return ((stdRec && aRec === stdRec) || aRec === stdAltId) && a.status === 'absent';
      }).length;

      let updatedMarksHistory = [...(std.marksHistory || [])];

      studentGrades.forEach(gradeRow => {
        const { subject, marks } = gradeRow;
        if (!marks) return;
        const academicYear = std.registrationYear || '2025-2026';
        const stdSubj = normalizeArabic(subject);

        const existingMarkIdx = updatedMarksHistory.findIndex(
          m => normalizeArabic(m.subject) === stdSubj
        );

        const latestRecorded = (marks.finalGrade && marks.finalGrade > 0) ? marks.finalGrade :
          (marks.annualAverage && marks.annualAverage > 0) ? marks.annualAverage :
          (marks.term2Avg && marks.term2Avg > 0) ? marks.term2Avg :
          (marks.m4MonthAvg && marks.m4MonthAvg > 0) ? marks.m4MonthAvg :
          (marks.m3MonthAvg && marks.m3MonthAvg > 0) ? marks.m3MonthAvg :
          (marks.midtermFinalGrade && marks.midtermFinalGrade > 0) ? marks.midtermFinalGrade :
          (marks.term1Avg && marks.term1Avg > 0) ? marks.term1Avg :
          (marks.m2MonthAvg && marks.m2MonthAvg > 0) ? marks.m2MonthAvg :
          (marks.m1MonthAvg && marks.m1MonthAvg > 0) ? marks.m1MonthAvg : 0;

        const newMarkEntry: StudentMark = {
          year: academicYear,
          subject: subject,
          midterm: marks.midtermFinalGrade ?? marks.midtermTotal ?? 0,
          finalExam: marks.finalExamTotal ?? marks.finalWrittenD1 ?? 0,
          finalGrade: marks.finalGrade ?? 0,
          total: latestRecorded,
          ...marks
        };

        if (existingMarkIdx > -1) {
          updatedMarksHistory[existingMarkIdx] = {
            ...updatedMarksHistory[existingMarkIdx],
            ...newMarkEntry,
            year: updatedMarksHistory[existingMarkIdx].year || academicYear
          };
        } else {
          updatedMarksHistory.push(newMarkEntry);
        }
      });

      return {
        ...std,
        marksHistory: updatedMarksHistory,
        absencesCount: (std.absencesCount || 0) + studentAbsenceCount
      };
    });

    // 6. Automatically persist back into localStorage and notify all views
    try {
      localStorage.setItem('diyala_school_students', JSON.stringify(updatedStudents));
      window.dispatchEvent(new Event('school_data_updated'));
    } catch (storageErr) {
      console.warn('LocalStorage save warning:', storageErr);
    }

    const newCount = newStudents.length;
    const msg = newCount > 0
      ? `تم سحب وتحديث درجات وغيابات الطلاب بنجاح (${dbGrades?.length || 0} سجل درجات). تم اكتشاف وإدراج ${newCount} طالب جديد مضاف من الأستاذ 👨‍🏫!`
      : `تم سحب وتحديث درجات وغيابات الطلاب بنجاح (${dbGrades?.length || 0} سجل سحابي)!`;

    return {
      success: true,
      message: msg,
      updatedStudents,
      newStudentsCount: newCount
    };
  } catch (error: any) {
    console.error('Import Error:', error);
    return { success: false, message: error.message || 'حدث خطأ أثناء استيراد البيانات من السحاب.' };
  }
}

/**
 * Restores all school data (students, staff, etc.) from Supabase
 * when a school re-onboards using an existing email or ID.
 */
export async function restoreSchoolData(schoolId: string): Promise<{
  success: boolean;
  message: string;
  config?: any;
  students?: Student[];
  teachers?: StaffMember[];
}> {
  try {
    const client = getSupabase(schoolId);

    // 0. Fetch school metadata
    const { data: schoolMeta, error: metaError } = await client
      .from('schools')
      .select('*')
      .eq('id', schoolId)
      .single();

    if (metaError) throw new Error(`School Metadata Error: ${metaError.message}`);

    // 1. Fetch teachers/staff
    const { data: dbTeachers, error: teachersError } = await client
      .from('teachers')
      .select('*')
      .eq('school_id', schoolId);

    if (teachersError) throw new Error(`Restore Teachers Error: ${teachersError.message}`);

    // 2. Fetch students
    const { data: dbStudents, error: studentsError } = await client
      .from('students')
      .select('*')
      .eq('school_id', schoolId);

    if (studentsError) throw new Error(`Restore Students Error: ${studentsError.message}`);

    // 3. Fetch grades
    const { data: dbGrades, error: gradesError } = await client
      .from('grades')
      .select('*')
      .eq('school_id', schoolId);

    if (gradesError) throw new Error(`Restore Grades Error: ${gradesError.message}`);

    // 4. Fetch attendance
    const { data: dbAttendance, error: attendanceError } = await client
      .from('attendance')
      .select('*')
      .eq('school_id', schoolId);

    if (attendanceError) throw new Error(`Restore Attendance Error: ${attendanceError.message}`);

    // Map teachers to StaffMember
    const teachersList: StaffMember[] = (dbTeachers || []).map((t) => ({
      id: t.id,
      firstName: t.name.split(' ')[0] || t.name,
      secondName: t.name.split(' ')[1] || '',
      thirdName: t.name.split(' ')[2] || '',
      fourthName: '',
      titleName: '',
      motherName: '',
      phoneNumber: t.email || '',
      specialization: t.specialization || 'عام',
      classesTaught: [],
      teachingQuota: 24,
      status: 'مستمر',
      jobTitle: 'مدرس',
      yearsOfService: 10,
      appointmentOrderNo: '',
      firstDirectOrderNo: '',
      residenceDistrict: '',
      nearestLandmark: '',
      residenceCardNumber: '',
      salaryAccountNumber: '',
      sectionsTaughtCount: 0
    }));

    // Map students to Student
    const studentsList: Student[] = (dbStudents || []).map((s, idx) => {
      const studentGrades = (dbGrades || []).filter(g => g.student_record_number === s.record_number);
      const studentAbsenceCount = (dbAttendance || []).filter(
        a => a.student_record_number === s.record_number && a.status === 'absent'
      ).length;

      const marksHistory: StudentMark[] = studentGrades.map(gradeRow => {
        const { subject, marks } = gradeRow;
        const academicYear = '2024-2025';
        return {
          year: academicYear,
          subject: subject,
          midterm: marks.midtermFinalGrade,
          finalExam: marks.finalExamTotal,
          finalGrade: marks.finalGrade,
          total: marks.finalGrade,
          ...marks
        };
      });

      return {
        id: `std-${s.record_number}`,
        sequence: idx + 1,
        recordNumber: s.record_number,
        registerPageNumber: '',
        wasatiPageNumber: '',
        registrationYear: '2024-2025',
        previousYearResult: '',
        currentGrade: s.current_grade,
        section: s.section,
        absencesCount: s.absences_count || studentAbsenceCount,
        status: 'مستمر',
        healthStatus: 'سليم',
        firstName: s.first_name,
        secondName: s.second_name || '',
        thirdName: s.third_name || '',
        fourthName: s.fourth_name || '',
        titleName: s.title_name || '',
        motherName: '',
        nationalCardNumber: '',
        conductScore: 'ممتاز',
        marksHistory
      };
    });

    return {
      success: true,
      message: 'تم استعادة بيانات المدرسة من السحاب بنجاح!',
      config: schoolMeta.config,
      students: studentsList,
      teachers: teachersList
    };
  } catch (error: any) {
    console.error('Restore Error:', error);
    return {
      success: false,
      message: error.message || 'فشل استعادة البيانات من السحاب.'
    };
  }
}

/**
 * Sends a pairing request from a Teacher or Student app to Supabase.
 */
export async function sendPairingRequest(payload: {
  schoolId: string;
  fullName: string;
  role: 'teacher' | 'student';
  grade: string;
  section: string;
  subject?: string;
  pairingCode: string;
}): Promise<{ success: boolean; message: string }> {
  try {
    const client = getSupabase(payload.schoolId);
    const { error } = await client
      .from('join_requests')
      .insert([{
        school_id: payload.schoolId,
        full_name: payload.fullName,
        role: payload.role,
        class_name: payload.grade,
        section: payload.section,
        subject_name: payload.subject || 'عام',
        status: 'pending',
        pairing_code_attempt: payload.pairingCode
      }]);

    if (error) throw error;

    return { success: true, message: 'تم إرسال طلب الانضمام بنجاح! بانتظار موافقة المدير.' };
  } catch (error: any) {
    console.error('Pairing Request Error:', error);
    return { success: false, message: error.message || 'فشل إرسال طلب الربط.' };
  }
}

/**
 * Sends an administrative directive (instruction) to all teachers/staff
 * via Supabase 'directives' table.
 */
export async function sendDirective(
  schoolId: string,
  title: string,
  content: string,
  targetRole: 'teachers' | 'students' | 'all' = 'all'
): Promise<{ success: boolean; message: string }> {
  try {
    const client = getSupabase(schoolId);
    const { error } = await client
      .from('directives')
      .insert([{
        school_id: schoolId,
        title,
        content,
        target_role: targetRole,
        created_at: new Date().toISOString(),
        is_active: true
      }]);

    if (error) throw error;
    return { success: true, message: 'تم إرسال التوجيه الإداري بنجاح لجميع الأجهزة!' };
  } catch (error: any) {
    console.error('Send Directive Error:', error);
    return { success: false, message: 'فشل إرسال التوجيه: ' + error.message };
  }
}

/**
 * Direct Instant Sync for Student roster to Supabase (Used on Excel import)
 */
export async function quickSyncStudentsToSupabase(schoolId: string, students: Student[]): Promise<{ success: boolean; message: string }> {
  if (!students || students.length === 0) return { success: true, message: 'لا توجد بيانات' };
  try {
    const client = getSupabase(schoolId);
    const payload = students.map(std => {
      const computedFullName = [std.firstName, std.secondName, std.thirdName, std.fourthName, std.titleName]
        .filter(Boolean)
        .join(' ')
        .trim() || (std.fullName && std.fullName.trim()) || std.firstName;

      return {
        school_id: schoolId,
        record_number: std.recordNumber || String(Math.floor(1000 + Math.random() * 9000)),
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

    const { error } = await client.from('students').upsert(payload, { onConflict: 'school_id,record_number', ignoreDuplicates: false });
    if (error) throw error;

    return { success: true, message: 'تمت مزامنة بيانات الطلاب مع السحابة بنجاح!' };
  } catch (e: any) {
    console.error('Quick sync error:', e);
    return { success: false, message: e.message };
  }
}

/**
 * Purges or resets school data from Supabase for a new academic year or fresh start.
 */
export async function purgeSchoolDataFromCloud(
  schoolId: string,
  scope: 'all' | 'grades_and_attendance' | 'students'
): Promise<{ success: boolean; message: string }> {
  try {
    const client = getSupabase(schoolId);

    if (scope === 'grades_and_attendance' || scope === 'all') {
      await client.from('grades').delete().eq('school_id', schoolId);
      await client.from('attendance').delete().eq('school_id', schoolId);
      await client.from('daily_assignments').delete().eq('school_id', schoolId);
    }

    if (scope === 'students' || scope === 'all') {
      await client.from('students').delete().eq('school_id', schoolId);
      await client.from('classes').delete().eq('school_id', schoolId);
    }

    if (scope === 'all') {
      await client.from('teacher_assignments').delete().eq('school_id', schoolId);
      await client.from('teachers').delete().eq('school_id', schoolId);
      await client.from('subjects').delete().eq('school_id', schoolId);
      await client.from('directives').delete().eq('school_id', schoolId);
      await client.from('join_requests').delete().eq('school_id', schoolId);
    }

    return { success: true, message: 'تم تصفير البيانات السحابية المحددة بنجاح!' };
  } catch (e: any) {
    console.error('Cloud Purge Error:', e);
    return { success: false, message: e.message || 'فشل تصفير السحابة.' };
  }
}

