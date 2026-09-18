import { AppConfig, DayScheduleMap, Student, StaffMember, OfficialDocument } from '../types';

export const defaultAppConfig: AppConfig = {
  schoolName: 'مدرستي النموذجية',
  schoolId: `SCH-${Math.floor(1000 + Math.random() * 9000)}`,
  pairingCode: Math.floor(100000 + Math.random() * 900000).toString(),
  studentPairingCode: Math.floor(100000 + Math.random() * 900000).toString(),
  principalPairingCode: Math.floor(100000 + Math.random() * 900000).toString(),
  managerName: '',
  directorateName: '',
  sectionName: '',
  passcode: '',
  developerCode: '9999',
  lessonDurationMinutes: 45,
  breakDurationMinutes: 10,
  schoolStartHour: '08:00',
  enableBellSound: true,
  enableScreensaver: false,
  adminEmail: '',
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
export const defaultStaff: StaffMember[] = [];
export const defaultDocuments: OfficialDocument[] = [];

