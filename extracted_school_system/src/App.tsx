/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { 
  ActiveView, 
  AppTheme, 
  AppFont, 
  AppConfig, 
  DayScheduleMap, 
  Student, 
  StaffMember, 
  OfficialDocument 
} from './types';

import { 
  defaultAppConfig, 
  defaultDayScheduleMap, 
  defaultStudents, 
  defaultStaff, 
  defaultDocuments 
} from './data/initialData';

import { TopHeader } from './components/TopHeader';
import { MainLauncher } from './components/MainLauncher';
import { ScheduleView } from './components/ScheduleView';
import { StudentRegisterView } from './components/StudentRegisterView';
import { StudentGradesView } from './components/StudentGradesView';
import { StudentAttendanceView } from './components/StudentAttendanceView';
import { FormerStudentsView } from './components/FormerStudentsView';
import { StaffRegisterView } from './components/StaffRegisterView';
import { StatisticsView } from './components/StatisticsView';
import { PrintingCenterView } from './components/PrintingCenterView';
import { ThemesView } from './components/ThemesView';
import { FontsView } from './components/FontsView';
import { AlarmTimerView } from './components/AlarmTimerView';
import { SettingsView } from './components/SettingsView';
import { DesktopGuideView } from './components/DesktopGuideView';
import { SmartScheduleGeneratorView } from './components/SmartScheduleGeneratorView';
import { TeacherPortalView } from './components/TeacherPortalView';
import { VoiceAssistantModal } from './components/VoiceAssistantModal';
import { SplashModal } from './components/SplashModal';
import { ScreensaverModal } from './components/ScreensaverModal';
import { OnboardingModal } from './components/OnboardingModal';
import { ApprovalDashboard } from './components/ApprovalDashboard';
import { SyncCenterView } from './components/SyncCenterView';
import { ManagementTipsView } from './components/ManagementTipsView';
import { CloudScheduleView } from './components/CloudScheduleView';
import { MobilePrincipalDashboard } from './components/MobilePrincipalDashboard';
import { TeacherAuthorityHub } from './components/TeacherAuthorityHub';
import { Sparkles } from 'lucide-react';
import { exportSchoolData } from './utils/syncService';

export default function App() {
  // Navigation & Theme
  const [activeView, setActiveView] = useState<ActiveView>('launcher');
  const [theme, setTheme] = useState<AppTheme>(() => {
    const savedTheme = localStorage.getItem('diyala_school_theme');
    const supportedThemes: AppTheme[] = ['cream', 'burgundy', 'sky', 'emerald', 'night', 'moon'];
    return supportedThemes.includes(savedTheme as AppTheme) ? savedTheme as AppTheme : 'cream';
  });
  const [font, setFont] = useState<AppFont>(() => {
    return (localStorage.getItem('diyala_school_font') as AppFont) || 'tajawal';
  });

  // Data Persistence with Safety Checks
  const [config, setConfig] = useState<AppConfig>(() => {
    try {
      const saved = localStorage.getItem('diyala_school_config');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (
          parsed.schoolName === 'م.كعب بن مالك المسائية للبنين' || 
          parsed.schoolName === 'م. كعب بن مالك المسائية للبنين' || 
          parsed.schoolId === 'SCH-VCOL-6072' ||
          parsed.schoolId === 'SCH-1001'
        ) {
          localStorage.setItem('diyala_school_config', JSON.stringify(defaultAppConfig));
          return defaultAppConfig;
        }
        return { ...defaultAppConfig, ...parsed };
      }
      return defaultAppConfig;
    } catch (e) {
      console.error('Failed to parse config:', e);
      return defaultAppConfig;
    }
  });

  const [scheduleMap, setScheduleMap] = useState<DayScheduleMap>(() => {
    try {
      const saved = localStorage.getItem('diyala_school_schedule');
      return (saved && saved !== 'undefined') ? JSON.parse(saved) : { 'الأحد': [], 'الإثنين': [], 'الثلاثاء': [], 'الأربعاء': [], 'الخميس': [] };
    } catch (e) {
      return { 'الأحد': [], 'الإثنين': [], 'الثلاثاء': [], 'الأربعاء': [], 'الخميس': [] };
    }
  });

  const [students, setStudents] = useState<Student[]>(() => {
    try {
      const saved = localStorage.getItem('diyala_school_students');
      if (saved && saved !== 'undefined') {
        const list = JSON.parse(saved);
        if (Array.isArray(list)) {
          return list;
        }
      }
      return defaultStudents;
    } catch (e) {
      return defaultStudents;
    }
  });

  const [staffList, setStaffList] = useState<StaffMember[]>(() => {
    try {
      const saved = localStorage.getItem('diyala_school_staff');
      if (saved && saved !== 'undefined') {
        const list = JSON.parse(saved);
        if (Array.isArray(list)) {
          return list;
        }
      }
      return defaultStaff;
    } catch (e) {
      return defaultStaff;
    }
  });

  const [documents, setDocuments] = useState<OfficialDocument[]>(() => {
    try {
      const saved = localStorage.getItem('diyala_school_documents');
      return (saved && saved !== 'undefined') ? JSON.parse(saved) : defaultDocuments;
    } catch (e) {
      return defaultDocuments;
    }
  });

  // Modal Overlays
  const [showSplash, setShowSplash] = useState(false);
  const [showScreensaver, setShowScreensaver] = useState(false);
  const [showVoiceModal, setShowVoiceModal] = useState(false);

  // Auto-migrate older configs missing schoolId or pairingCode to clean school identity
  useEffect(() => {
    const targetSchoolId = config.schoolId || `SCH-${Math.floor(1000 + Math.random() * 9000)}`;
    const targetPairingCode = config.pairingCode || Math.floor(100000 + Math.random() * 900000).toString();
    const targetSchoolName = config.schoolName || 'مدرستي النموذجية';
    const targetAdminEmail = config.adminEmail || '';

    localStorage.setItem('diyala_school_id', targetSchoolId);
    localStorage.setItem('diyala_pairing_code', targetPairingCode);

    if (!config.schoolId || !config.pairingCode || !config.schoolName) {
      setConfig(prev => ({
        ...prev,
        schoolId: targetSchoolId,
        pairingCode: targetPairingCode,
        schoolName: targetSchoolName,
        adminEmail: targetAdminEmail
      }));
    }
  }, [config.schoolName, config.schoolId, config.pairingCode]);

  // Automatic Background Cloud Sync for School, Students, Staff, and Classes
  useEffect(() => {
    if (!config.schoolId || !config.schoolName) return;

    const activeSchoolId = config.schoolId;
    const activeSchoolName = config.schoolName;
    const activePairingCode = config.pairingCode || '112233';
    const activeEmail = config.adminEmail || '';

    const timer = setTimeout(() => {
      exportSchoolData(
        activeSchoolId,
        activeSchoolName,
        activePairingCode,
        activeEmail,
        students,
        staffList,
        scheduleMap
      ).then(res => {
        if (res.success) {
          console.log('✓ Auto Cloud Sync Completed:', res.message);
        }
      }).catch(e => console.warn('Auto Cloud Sync warning:', e));
    }, 1500);

    return () => clearTimeout(timer);
  }, [config.schoolId, config.schoolName, config.pairingCode, config.adminEmail, students, staffList, scheduleMap]);

  // Sync Font to LocalStorage & Body
  useEffect(() => {
    localStorage.setItem('diyala_school_font', font);
    document.body.className = `font-${font} min-h-screen`;
  }, [font]);

  useEffect(() => {
    localStorage.setItem('diyala_school_config', JSON.stringify(config));
  }, [config]);

  useEffect(() => {
    localStorage.setItem('diyala_school_schedule', JSON.stringify(scheduleMap));
  }, [scheduleMap]);

  useEffect(() => {
    localStorage.setItem('diyala_school_students', JSON.stringify(students));
  }, [students]);

  useEffect(() => {
    localStorage.setItem('diyala_school_staff', JSON.stringify(staffList));
  }, [staffList]);

  useEffect(() => {
    localStorage.setItem('diyala_school_documents', JSON.stringify(documents));
  }, [documents]);

  // Theme & Font Synchronization
  useEffect(() => {
    localStorage.setItem('diyala_school_theme', theme);
    localStorage.setItem('diyala_school_font', font);
    document.documentElement.className = `theme-${theme} font-${font}`;
    document.body.className = `theme-${theme} font-${font}`;
  }, [theme, font]);

  // Reset to empty fresh state from scratch
  const handleResetData = () => {
    if (confirm('هل أنت متأكد من تصفير كافة البيانات ومسح السجلات للبدء من الصفر تماماً؟')) {
      localStorage.clear();
      setConfig(defaultAppConfig);
      setScheduleMap(defaultDayScheduleMap);
      setStudents([]);
      setStaffList([]);
      setDocuments([]);
      localStorage.setItem('diyala_school_students', JSON.stringify([]));
      localStorage.setItem('diyala_school_staff', JSON.stringify([]));
      localStorage.setItem('diyala_school_documents', JSON.stringify([]));
      localStorage.setItem('diyala_school_config', JSON.stringify(defaultAppConfig));
      alert('تم تصفير كافة البيانات بنجاح! النظام الآن فارغ ونظيف وجاهز لاستقبال بيانات مدرستكم من الصفر.');
    }
  };

  // Screensaver Idle Inactivity Timer (5 Minutes of no activity)
  useEffect(() => {
    if (config.enableScreensaver === false) return;

    let idleTimer: NodeJS.Timeout;
    const FIVE_MINUTES_MS = 5 * 60 * 1000;

    const resetIdleTimer = () => {
      clearTimeout(idleTimer);
      idleTimer = setTimeout(() => {
        setShowScreensaver(true);
      }, FIVE_MINUTES_MS);
    };

    // Event listeners to detect activity
    window.addEventListener('mousemove', resetIdleTimer);
    window.addEventListener('keydown', resetIdleTimer);
    window.addEventListener('touchstart', resetIdleTimer);
    window.addEventListener('click', resetIdleTimer);

    resetIdleTimer();

    return () => {
      clearTimeout(idleTimer);
      window.removeEventListener('mousemove', resetIdleTimer);
      window.removeEventListener('keydown', resetIdleTimer);
      window.removeEventListener('touchstart', resetIdleTimer);
      window.removeEventListener('click', resetIdleTimer);
    };
  }, [config.enableScreensaver]);

  // Icon Shape state
  const [iconShape, setIconShape] = useState<'squircle' | 'round'>('squircle');

  return (
    <div className={`h-screen flex flex-col theme-${theme} font-${font} transition-colors duration-300 overflow-hidden bg-[var(--theme-bg)] text-[var(--theme-text-main)]`}>
      
      {/* Fixed Header Bar */}
      <TopHeader
        config={config}
        activeView={activeView}
        setActiveView={setActiveView}
        scheduleMap={scheduleMap}
        onOpenPasscode={() => setActiveView('settings')}
        onOpenVoiceModal={() => setShowVoiceModal(true)}
        studentsCount={students.length}
        staffCount={staffList.length}
        iconShape={iconShape}
        setIconShape={setIconShape}
      />

      {/* Scrollable Main View Area - FORCED SCROLL FIX */}
      <div className="flex-1 overflow-y-auto custom-scrollbar relative">
        <main className="pb-24 max-w-7xl mx-auto w-full">
          {!config.schoolId ? (
          <div className="flex-1 flex items-center justify-center p-4">
             <OnboardingModal
                onComplete={(data) => {
                  const { restoredStudents, restoredTeachers, ...onboardConfig } = data;
                  setConfig(prev => ({ ...prev, ...onboardConfig }));
                  if (restoredStudents) setStudents(restoredStudents);
                  if (restoredTeachers) setStaffList(restoredTeachers);
                }}
              />
          </div>
        ) : (
          <>
            {activeView === 'launcher' && (
              <MainLauncher
                setActiveView={setActiveView}
                studentsCount={students.length}
                staffCount={staffList.length}
                onOpenVoiceModal={() => setShowVoiceModal(true)}
                students={students}
                setStudents={setStudents}
                scheduleMap={scheduleMap}
                config={config}
                onResetData={handleResetData}
              />
            )}

            {activeView === 'schedule' && (
              <ScheduleView
                scheduleMap={scheduleMap}
                setScheduleMap={setScheduleMap}
                config={config}
                staffList={staffList}
                setStaffList={setStaffList}
                students={students}
                onOpenSmartGenerator={() => setActiveView('smart_schedule')}
              />
            )}

            {activeView === 'smart_schedule' && (
              <SmartScheduleGeneratorView
                scheduleMap={scheduleMap}
                setScheduleMap={setScheduleMap}
                staffList={staffList}
                setStaffList={setStaffList}
                students={students}
                config={config}
                onBackToLauncher={() => setActiveView('launcher')}
              />
            )}

            {activeView === 'students' && (
              <StudentRegisterView
                students={students}
                setStudents={setStudents}
                config={config}
              />
            )}

            {activeView === 'student_grades' && (
              <StudentGradesView
                students={students}
                setStudents={setStudents}
                config={config}
              />
            )}

            {activeView === 'attendance' && (
              <StudentAttendanceView
                students={students}
                setStudents={setStudents}
                config={config}
                onBackToMain={() => setActiveView('launcher')}
              />
            )}

            {activeView === 'former_students' && (
              <FormerStudentsView
                students={students}
                setStudents={setStudents}
                config={config}
              />
            )}

            {activeView === 'staff' && (
              <StaffRegisterView
                staffList={staffList}
                setStaffList={setStaffList}
                config={config}
                scheduleMap={scheduleMap}
              />
            )}

            {activeView === 'teacher_authority' && (
              <TeacherAuthorityHub
                staffList={staffList}
                setStaffList={setStaffList}
                config={config}
                scheduleMap={scheduleMap}
                onBackToMain={() => setActiveView('launcher')}
              />
            )}

            {activeView === 'stats' && (
              <StatisticsView
                staffList={staffList}
                students={students}
                scheduleMap={scheduleMap}
              />
            )}

            {activeView === 'print' && (
              <PrintingCenterView
                documents={documents}
                setDocuments={setDocuments}
                config={config}
              />
            )}

            {activeView === 'themes' && (
              <ThemesView
                currentTheme={theme}
                setTheme={setTheme}
                currentFont={font}
                setFont={setFont}
              />
            )}

            {activeView === 'fonts' && (
              <FontsView
                currentFont={font}
                setFont={setFont}
              />
            )}

            {activeView === 'alarm' && (
              <AlarmTimerView
                config={config}
                setConfig={setConfig}
              />
            )}

            {activeView === 'desktop_guide' && (
              <DesktopGuideView
                config={config}
              />
            )}

            {activeView === 'teacher_portal' && (
              <TeacherPortalView
                students={students}
                setStudents={setStudents}
                staffList={staffList}
                config={config}
                onBackToMain={() => setActiveView('launcher')}
              />
            )}

            {activeView === 'settings' && (
              <SettingsView
                config={config}
                setConfig={setConfig}
                students={students}
                setStudents={setStudents}
                staffList={staffList}
                setStaffList={setStaffList}
                onResetData={handleResetData}
                onTriggerScreensaver={() => setShowScreensaver(true)}
                onTriggerSplash={() => setShowSplash(true)}
              />
            )}

            {activeView === 'approval_dashboard' && (
              <ApprovalDashboard schoolId={config.schoolId || 'school_01'} />
            )}

            {activeView === 'sync_center' && (
              <SyncCenterView
                students={students}
                setStudents={setStudents}
                staffList={staffList}
                config={config}
                scheduleMap={scheduleMap}
                onBackToMain={() => setActiveView('launcher')}
              />
            )}

            {activeView === 'management_tips' && (
              <ManagementTipsView
                onBack={() => setActiveView('launcher')}
              />
            )}

            {activeView === 'cloud_schedule' && (
              <CloudScheduleView
                currentSchedule={scheduleMap}
                onImport={(newSchedule) => setScheduleMap(newSchedule)}
                onBack={() => setActiveView('launcher')}
                students={students}
                staffList={staffList}
              />
            )}

            {activeView === 'mobile_dashboard' && (
              <MobilePrincipalDashboard
                students={students}
                staffList={staffList}
                schedule={scheduleMap}
                schoolName={config.schoolName || 'مدرستنا'}
                onBack={() => setActiveView('launcher')}
              />
            )}
          </>
        )}
        </main>
      </div>

      {/* Voice Assistant Modal */}
      <VoiceAssistantModal
        isOpen={showVoiceModal}
        onClose={() => setShowVoiceModal(false)}
        students={students}
        setStudents={setStudents}
        staffList={staffList}
        setStaffList={setStaffList}
        setActiveView={setActiveView}
        setTheme={setTheme}
        config={config}
      />

      {/* Splash Screen Overlay */}
      {showSplash && (
        <SplashModal
          config={config}
          onClose={() => setShowSplash(false)}
        />
      )}

      {/* Screensaver Overlay */}
      {showScreensaver && (
        <ScreensaverModal
          config={config}
          scheduleMap={scheduleMap}
          onUnlock={() => setShowScreensaver(false)}
        />
      )}

      {/* Floating AI Assistant Button */}
      <button
        onClick={() => setShowVoiceModal(true)}
        className="fixed bottom-20 right-6 z-50 w-14 h-14 bg-indigo-600 text-white rounded-full shadow-2xl flex items-center justify-center hover:bg-indigo-700 hover:scale-110 transition-all cursor-pointer group border-4 border-white"
        title="مساعد الإدارة الذكي (AI Assistant)"
      >
        <Sparkles className="w-7 h-7 group-hover:animate-pulse" />
      </button>

    </div>
  );
}
