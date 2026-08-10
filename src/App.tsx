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
import { FormerStudentsView } from './components/FormerStudentsView';
import { StaffRegisterView } from './components/StaffRegisterView';
import { StatisticsView } from './components/StatisticsView';
import { PrintingCenterView } from './components/PrintingCenterView';
import { ThemesView } from './components/ThemesView';
import { FontsView } from './components/FontsView';
import { AlarmTimerView } from './components/AlarmTimerView';
import { SettingsView } from './components/SettingsView';
import { DesktopGuideView } from './components/DesktopGuideView';
import { TeacherPortalView } from './components/TeacherPortalView';
import { VoiceAssistantModal } from './components/VoiceAssistantModal';
import { SplashModal } from './components/SplashModal';
import { ScreensaverModal } from './components/ScreensaverModal';

export default function App() {
  // Navigation & Theme
  const [activeView, setActiveView] = useState<ActiveView>('launcher');
  const [theme, setTheme] = useState<AppTheme>(() => {
    return (localStorage.getItem('diyala_school_theme') as AppTheme) || 'vibrant';
  });
  const [font, setFont] = useState<AppFont>(() => {
    return (localStorage.getItem('diyala_school_font') as AppFont) || 'tajawal';
  });

  // Data Persistence
  const [config, setConfig] = useState<AppConfig>(() => {
    const saved = localStorage.getItem('diyala_school_config');
    return saved ? JSON.parse(saved) : defaultAppConfig;
  });

  const [scheduleMap, setScheduleMap] = useState<DayScheduleMap>(() => {
    const saved = localStorage.getItem('diyala_school_schedule');
    return saved ? JSON.parse(saved) : defaultDayScheduleMap;
  });

  const [students, setStudents] = useState<Student[]>(() => {
    const saved = localStorage.getItem('diyala_school_students');
    return saved ? JSON.parse(saved) : defaultStudents;
  });

  const [staffList, setStaffList] = useState<StaffMember[]>(() => {
    const saved = localStorage.getItem('diyala_school_staff');
    return saved ? JSON.parse(saved) : defaultStaff;
  });

  const [documents, setDocuments] = useState<OfficialDocument[]>(() => {
    const saved = localStorage.getItem('diyala_school_documents');
    return saved ? JSON.parse(saved) : defaultDocuments;
  });

  // Modal Overlays
  const [showSplash, setShowSplash] = useState(true);
  const [showScreensaver, setShowScreensaver] = useState(false);
  const [showVoiceModal, setShowVoiceModal] = useState(false);

  // Sync to LocalStorage
  useEffect(() => {
    localStorage.setItem('diyala_school_theme', theme);
    document.body.className = `theme-${theme} font-${font} bg-[var(--theme-bg)] text-[var(--theme-text-main)] min-h-screen transition-colors duration-300`;
  }, [theme, font]);

  useEffect(() => {
    localStorage.setItem('diyala_school_font', font);
    document.body.className = `theme-${theme} font-${font} bg-[var(--theme-bg)] text-[var(--theme-text-main)] min-h-screen transition-colors duration-300`;
  }, [theme, font]);

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

  // Reset to default sample data
  const handleResetData = () => {
    if (confirm('هل أنت تأكد من إعادة ضبط كافة البيانات إلى الحالة الافتراضية؟')) {
      setConfig(defaultAppConfig);
      setScheduleMap(defaultDayScheduleMap);
      setStudents(defaultStudents);
      setStaffList(defaultStaff);
      setDocuments(defaultDocuments);
      localStorage.clear();
      alert('تمت إعادة ضبط البيانات بنجاح!');
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
    <div className="min-h-screen flex flex-col font-sans transition-colors duration-300">
      
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

      {/* Main View Area */}
      <main className="flex-1 pb-16">
        {activeView === 'launcher' && (
          <MainLauncher
            setActiveView={setActiveView}
            studentsCount={students.length}
            staffCount={staffList.length}
            onOpenVoiceModal={() => setShowVoiceModal(true)}
          />
        )}

        {activeView === 'schedule' && (
          <ScheduleView
            scheduleMap={scheduleMap}
            setScheduleMap={setScheduleMap}
            config={config}
          />
        )}

        {activeView === 'students' && (
          <StudentRegisterView
            students={students}
            setStudents={setStudents}
            config={config}
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
            onResetData={handleResetData}
            onTriggerScreensaver={() => setShowScreensaver(true)}
            onTriggerSplash={() => setShowSplash(true)}
          />
        )}
      </main>

      {/* Voice Assistant Speech Control Modal */}
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
          onUnlock={() => setShowScreensaver(false)}
        />
      )}

    </div>
  );
}
