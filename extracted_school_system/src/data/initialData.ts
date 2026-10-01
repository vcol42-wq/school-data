import { AppConfig, DayScheduleMap, Student, StaffMember, OfficialDocument } from '../types';

export const defaultAppConfig: AppConfig = {
  schoolName: 'المدرسة الذكية',
  schoolId: '',
  pairingCode: '',
  studentPairingCode: '',
  principalPairingCode: '',
  managerName: 'وليد الحسون',
  directorateName: 'المديرية العامة لتربية ديالى',
  sectionName: 'قسم التعليم الثانوي والملاك',
  passcode: '1234',
  developerCode: '9999',
  lessonDurationMinutes: 45,
  breakDurationMinutes: 10,
  schoolStartHour: '08:00',
  enableBellSound: true,
  enableScreensaver: false,
  adminEmail: 'principal@diyala.edu.iq',
  geminiApiKey: '',
  splashImageUrl: '',
  screensaverImageUrl: ''
};

export const defaultDayScheduleMap: DayScheduleMap = {
  'الأحد': [],
  'الإثنين': [],
  'الثلاثاء': [],
  'الأربعاء': [],
  'الخميس': []
};

export const defaultStudents: Student[] = [];

export const defaultStaff: StaffMember[] = [
  {
    id: 'stf-01',
    firstName: 'أحمد',
    secondName: 'شاكر',
    thirdName: 'محمود',
    specialization: 'اللغة العربية',
    degree: 'بكالوريوس',
    role: 'معلم جامعي',
    phoneNumber: '07701234567',
    weeklyQuotas: 20
  },
  {
    id: 'stf-02',
    firstName: 'محمد',
    secondName: 'جاسم',
    thirdName: 'خلف',
    specialization: 'الرياضيات',
    degree: 'بكالوريوس',
    role: 'مدرس',
    phoneNumber: '07801234567',
    weeklyQuotas: 18
  },
  {
    id: 'stf-03',
    firstName: 'خالد',
    secondName: 'إبراهيم',
    thirdName: 'عبد',
    specialization: 'اللغة الإنكليزية',
    degree: 'ماجستير',
    role: 'مدرس أقدم',
    phoneNumber: '07711223344',
    weeklyQuotas: 16
  }
];

export const defaultDocuments: OfficialDocument[] = [];
