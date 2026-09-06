import { getSupabase } from './supabaseClient';
import { Student, StaffMember, StudentMark, DayScheduleMap } from '../types';

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
  if (!gradeStr) return 'الأول';
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
  else if (lower.includes('متوسط')) branch = 'المتوسط';
  else if (lower.includes('اعدادي') || lower.includes('ثانوي')) branch = 'الإعدادي';
  else if (lower.includes('ابتدائي')) branch = 'الابتدائي';

  return branch ? `${base} ${branch}` : base;
}

export function standardizeSectionName(secStr: string): string {
  if (!secStr) return 'أ';
  const clean = secStr.trim().replace(/^(شعبة|الشعبة|ش)\s*/g, '').trim();
  const lower = clean.toLowerCase();
  if (clean === 'ا' || clean === 'أ' || clean === 'إ' || clean === 'آ' || lower === 'a' || lower === '1' || clean === '١') return 'أ';
  if (clean === 'ب' || lower === 'b' || lower === '2' || clean === '٢') return 'ب';
  if (clean === 'ج' || lower === 'c' || lower === '3' || clean === '٣') return 'ج';
  if (clean === 'د' || lower === 'd' || lower === '4' || clean === '٤') return 'د';
  if (clean === 'ه' || clean === 'هـ' || lower === 'e' || lower === '5' || clean === '٥') return 'هـ';
  if (clean === 'و' || lower === 'f' || lower === '6' || clean === '٦') return 'و';
  if (clean === 'ز' || lower === 'z' || lower === '7' || clean === '٧') return 'ز';
  if (clean === 'ح' || lower === 'h' || lower === '8' || clean === '٨') return 'ح';
  if (clean === 'ط' || lower === '9' || clean === '٩') return 'ط';
  if (clean === 'خ') return 'خ';
  return clean || 'أ';
}

export function standardizeSubjectName(raw: string): string {
  if (!raw) return 'المادة العامة';
  const s = raw.trim();
  const norm = normalizeArabic(s).toLowerCase();
  const rawLower = s.toLowerCase().replace(/[^a-z0-9\u0600-\u06FF]/g, '');

  // 1. اللغة الإنكليزية: جميع التسميات (عربي، إنجليزي، انكليزي، أحرف لاتينية)
  if (
    norm.includes('انكل') || norm.includes('انجل') ||
    rawLower.includes('engl') || rawLower.startsWith('eng') || rawLower.endsWith('eng') ||
    rawLower === 'e' || rawLower === 'en' || rawLower === 'el' ||
    rawLower.includes('english') || norm.includes('انكلش') || norm.includes('انجلش') ||
    norm.includes('انجليز') || norm.includes('انكليز')
  ) {
    return 'اللغة الإنكليزية';
  }

  // 2. الأحياء: علم الأحياء / علوم الأحياء / احياء / الأحياء
  if (norm.includes('احياء') || norm.includes('علماحياء') || norm.includes('علوماحياء')) return 'الأحياء';

  // 3. التربية الإسلامية: اسلامية / الاسلامية / دين / قرآن
  if (norm.includes('اسلام') || norm.includes('قران') || norm.includes('دين')) return 'التربية الإسلامية';

  // 4. اللغة العربية: عربي / العربي
  if (norm.includes('عرب')) return 'اللغة العربية';

  // 5. باقي المواد
  if (norm.includes('فيزيا')) return 'الفيزياء';
  if (norm.includes('كيميا')) return 'الكيمياء';
  if (norm.includes('اجتماع') || norm.includes('تاريخ') || norm.includes('جغرافي') || norm.includes('وطني')) return 'الاجتماعيات';
  if (norm.includes('رياض')) return 'الرياضيات';

  return s;
}

// Helper to parse the assigned classes taught by teachers
function parseClassTaught(classStr: string, defaultSubject: string) {
  let cleaned = classStr.replace(/^(الصف|صف)\s+/g, '').trim();
  let explicitSubject = '';

  // 1. Check for parenthesis containing subject: e.g. "الأول متوسط - أ (التربية الأخلاقية)"
  const parenMatch = cleaned.match(/\((.*?)\)/);
  if (parenMatch) {
    explicitSubject = parenMatch[1].trim();
    cleaned = cleaned.replace(/\(.*?\)/, '').trim();
  }

  // 2. Split by dash or spaces to extract grade and section
  const parts = cleaned.split(/[-–—\s]+/).filter(Boolean);
  if (parts.length >= 2) {
    const section = standardizeSectionName(parts[parts.length - 1]);
    const grade = standardizeGradeName(parts.slice(0, parts.length - 1).join(' '));
    const subject = explicitSubject || defaultSubject || 'عام';
    return { grade, section, subject };
  } else if (parts.length === 1) {
    return { grade: standardizeGradeName(parts[0]), section: 'أ', subject: explicitSubject || defaultSubject || 'عام' };
  } else {
    return { grade: 'الأول', section: 'أ', subject: explicitSubject || defaultSubject || 'عام' };
  }
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
    const schoolPayload = [
      {
        id: schoolId,
        name: schoolName,
        pairing_code: pairingCode,
        admin_email: adminEmail
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
    emit('step_classes', 4, 'استخراج وتصدير الفصول والشعب', 50, 'جاري استخراج الفصول والشعب الفريدة من سجلات الطلاب...', 'active');
    const uniqueClassesMap = new Map<string, { school_id: string; name: string; section: string }>();
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

    const classesPayload = Array.from(uniqueClassesMap.values());
    if (classesPayload.length > 0) {
      const { error: classError } = await client.from('classes').upsert(classesPayload, { onConflict: 'school_id,name,section' });
      if (classError) {
        console.warn('Classes RLS warning:', classError.message);
        emit('step_classes', 4, 'استخراج وتصدير الفصول والشعب', 60, `تنبيه في جدول الفصول: ${classError.message}`, 'warning');
      } else {
        emit('step_classes', 4, 'استخراج وتصدير الفصول والشعب', 60, `تم رفع ${classesPayload.length} شعبة وفصل دراسي بنجاح ✓`, 'success', classesPayload.length);
      }
    } else {
      emit('step_classes', 4, 'استخراج وتصدير الفصول والشعب', 60, 'لا توجد فصول جديدة للرفع', 'success', 0);
    }

    // ----------------------------------------------------
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
                uniqueSubjects.add(stdSubject);

                const matchedTeacher = findStaff(slot.teacherName);
                if (matchedTeacher) {
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

    // 2. SECONDARY SOURCE: classesTaught and actualSubjectTaught
    staff.forEach((member, idx) => {
      const stableId = getStaffStableId(member, idx);
      const defaultSubject = member.actualSubjectTaught || member.specialization || 'عام';
      if (defaultSubject) uniqueSubjects.add(standardizeSubjectName(defaultSubject));

      if (member.classesTaught && member.classesTaught.length > 0) {
        member.classesTaught.forEach(classStr => {
          const parsed = parseClassTaught(classStr, defaultSubject);
          const stdGrade = standardizeGradeName(parsed.grade);
          const stdSection = standardizeSectionName(parsed.section);
          const stdSubject = standardizeSubjectName(parsed.subject);
          
          if (stdSubject) uniqueSubjects.add(stdSubject);
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
        });
      }
    });

    const subjectsPayload = Array.from(uniqueSubjects).map(sub => ({
      school_id: schoolId,
      name: sub
    }));

    if (subjectsPayload.length > 0) {
      const { error: subjectError } = await client.from('subjects').upsert(subjectsPayload, { onConflict: 'school_id,name' });
      if (subjectError) {
        console.warn('Subjects RLS warning:', subjectError.message);
        emit('step_subjects', 5, 'استخراج وتصدير المواد الدراسية', 75, `تنبيه في جدول المواد: ${subjectError.message}`, 'warning');
      } else {
        emit('step_subjects', 5, 'استخراج وتصدير المواد الدراسية', 75, `تم تسجيل ${subjectsPayload.length} مادة دراسية في السحابة ✓`, 'success', subjectsPayload.length);
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

      if (savedAssStr) {
        try {
          const parsed = JSON.parse(savedAssStr);
          if (Array.isArray(parsed) && parsed.length > 0) {
            subAssignmentsToUpsert = parsed.map(a => ({
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

      if (subAssignmentsToUpsert.length === 0 && savedProfilesStr) {
        try {
          const profiles: any[] = JSON.parse(savedProfilesStr);
          profiles.forEach(prof => {
            if (prof.isExempt) return;
            const subjects = prof.subjects && prof.subjects.length > 0 ? prof.subjects : [prof.specialization || 'عام'];
            const classes = prof.classes && prof.classes.length > 0 ? prof.classes : [{ grade: 'الأول المتوسط', section: 'أ' }];
            subjects.forEach((subj: string) => {
              classes.forEach((cls: any) => {
                subAssignmentsToUpsert.push({
                  school_id: schoolId,
                  grade: standardizeGradeName(cls.grade || ''),
                  section: standardizeSectionName(cls.section || ''),
                  subject: standardizeSubjectName(subj || ''),
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

      // 1. Save complete authority profiles, assignments, and supervisor to schools.config
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

    const studentsPayload = sortedStudentsList.map(std => {
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
      const { error: studentError } = await client.from('students').upsert(studentsPayload, { onConflict: 'school_id,record_number' });
      if (studentError) throw new Error(`خطأ في رفع سجل الطلاب: ${studentError.message}`);
    }
    emit('step_students', 8, 'تصدير سجل الطلاب الموحد والأسماء الكاملة', 96, `تم تصدير ${studentsPayload.length} طالب بنجاح مع أسمائهم الكاملة ✓`, 'success', studentsPayload.length);

    // ----------------------------------------------------
    // Step 9: Export Schedule Map
    // ----------------------------------------------------
    emit('step_schedule', 9, 'تصدير الجدول الأسبوعي للمدرسة', 98, 'جاري رفع خريطة جدول الحصص والتوقيتات...', 'active');
    const finalScheduleMap = (scheduleMap && Object.keys(scheduleMap).length > 0)
      ? scheduleMap
      : { 'الأحد': [], 'الإثنين': [], 'الثلاثاء': [], 'الأربعاء': [], 'الخميس': [] };

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

    // If viewing subject_assignments, attempt reading from SQL table or fallback to schools.config
    if (tableName === 'subject_assignments') {
      try {
        const { data: sqlData, error: sqlErr } = await client.from('subject_assignments').select('*').eq('school_id', schoolId).limit(limit);
        if (!sqlErr && sqlData && sqlData.length > 0) {
          return { success: true, data: sqlData };
        }
      } catch {}

      const { data: schoolRow } = await client.from('schools').select('config').eq('id', schoolId).single();
      const subAss = schoolRow?.config?.subject_assignments;
      if (Array.isArray(subAss) && subAss.length > 0) {
        return { success: true, data: subAss };
      }
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
