export type ActiveView = 
  | 'launcher'
  | 'schedule'
  | 'students'
  | 'former_students'
  | 'staff'
  | 'stats'
  | 'print'
  | 'themes'
  | 'fonts'
  | 'alarm'
  | 'settings'
  | 'desktop_guide'
  | 'teacher_portal';

export type AppTheme = 'vibrant' | 'classic' | 'diyala' | 'emerald' | 'dark' | 'burgundy';

export type AppFont = 'tajawal' | 'cairo' | 'amiri' | 'alexandria' | 'noto';

export type SchoolStage = 'primary' | 'intermediate' | 'preparatory' | 'secondary';

export interface AppConfig {
  schoolName: string;
  managerName: string;
  directorateName: string;
  sectionName: string;
  schoolStage?: SchoolStage;
  passcode: string;
  developerCode: string;
  lessonDurationMinutes: number;
  breakDurationMinutes: number;
  schoolStartHour: string;
  enableBellSound: boolean;
  enableScreensaver?: boolean;
  splashImageUrl?: string;
  screensaverImageUrl?: string;
}

// 1. Schedule Types
export interface ScheduleCell {
  subject: string;
  teacherName: string;
  isOff: boolean;
}

export interface ClassScheduleRow {
  id: string;
  grade: string;
  section: string;
  teacherInCharge: string;
  lessons: {
    lesson1: ScheduleCell;
    break1?: boolean;
    lesson2: ScheduleCell;
    break2?: boolean;
    lesson3: ScheduleCell;
    break3?: boolean;
    lesson4: ScheduleCell;
    break4?: boolean;
    lesson5: ScheduleCell;
    break5?: boolean;
    lesson6: ScheduleCell;
  };
}

export type DayOfWeek = 'الأحد' | 'الإثنين' | 'الثلاثاء' | 'الأربعاء' | 'الخميس';

export interface DayScheduleMap {
  [day: string]: ClassScheduleRow[];
}

// 2. Student Record Types
export interface StudentMark {
  year: string;
  subject: string;
  firstSemester?: number; // الفصل الأول
  midterm?: number;        // نصف السنة
  secondSemester?: number;// الفصل الثاني
  yearlyEffort?: number;  // السعي السنوي
  finalExam?: number;     // امتحان آخر السنة
  secondRound?: number;   // امتحان الدور الثاني
  finalGrade?: number;    // الدرجة النهائية
  
  // تفاصيل سجل اليومي والشهور
  month1Daily?: number;    // مجم ش1
  month1Written?: number;  // تحر ش1
  month1?: number;         // ش1 = مجم + تحر
  
  month2Daily?: number;    // مجم ش2
  month2Written?: number;  // تحر ش2
  month2?: number;         // ش2 = مجم + تحر
  
  midtermDaily?: number;   // يومي نصف السنة (مجم)
  midtermWritten?: number; // تحريري نصف السنة (تحر)

  final: number;
  total: number;
}

export interface StudentNote {
  id: string;
  date: string;
  type: 'وثيقة' | 'فصل' | 'تخرج' | 'نقل' | 'صحي' | 'ملاحظة عامة';
  text: string;
}

export interface Student {
  id: string;
  sequence: number;
  recordNumber: string;
  registerPageNumber: string;
  wasatiPageNumber: string;
  registrationYear: string;
  previousYearResult: string;
  currentGrade: string;
  section: string;
  absencesCount: number;
  finalYearScore?: number;
  isLockedAndSynced?: boolean;
  syncSealToken?: string;
  promotionDestination?: string;
  attestationStage?: 'غير مصادق' | 'الفصل الأول' | 'نصف السنة' | 'الفصل الثاني' | 'أخر السنة' | 'الدور الثاني';
  status: 'مستمر' | 'غادر المدرسة' | 'متخرج' | 'مفصول';
  healthStatus: string;
  firstName: string;
  secondName: string;
  thirdName: string;
  fourthName: string;
  titleName: string;
  motherName: string;
  nationalCardNumber: string;
  conductScore: string;
  marksHistory: StudentMark[];
  notesLog: StudentNote[];
}

// 3. Staff Member Types
export interface StaffMember {
  id: string;
  jobTitle: string;
  firstName: string;
  secondName: string;
  thirdName: string;
  fourthName: string;
  titleName: string;
  motherName: string;
  birthDay: string;
  birthMonth: string;
  birthYear: string;
  nationalCardNumber: string;
  rationCardNumber: string;
  rationCenterNumber: string;
  spouseOccupation: string;
  phoneNumber: string;
  specialization: string;
  firstDirectDay: string;
  firstDirectMonth: string;
  firstDirectYear: string;
  hasMasterDegree: boolean;
  schoolDirectDay: string;
  schoolDirectMonth: string;
  schoolDirectYear: string;
  academicDegree: string;
  yearsOfService: number;
  status: 'مستمر' | 'مجاز إجازة طويلة' | 'منسب إلى المدرسة' | 'منسب خارج المدرسة';
  appointmentOrderNo: string;
  firstDirectOrderNo: string;
  functionalTitle: string;
  residenceDistrict: string;
  nearestLandmark: string;
  residenceCardNumber: string;
  salaryAccountNumber: string;
  classesTaught: string[];
  sectionsTaughtCount: number;
  teachingQuota: number;
  leaveType?: string;
  releaseOrderNoAndDate?: string;
}

// 4. Official Document / Print Center Types
export interface OfficialDocument {
  id: string;
  title: string;
  templateType: 'enrollment' | 'transfer' | 'transcript' | 'staff_service' | 'release' | 'custom';
  refNumber: string;
  date: string;
  recipient: string;
  studentOrStaffName?: string;
  subject: string;
  bodyContent: string;
  managerTitle: string;
  managerName: string;
  watermarkText: string;
}
