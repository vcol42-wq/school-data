import React, { useState } from 'react';
import { CloudLiveInspector } from './CloudLiveInspector';
import { PrincipalSyncDashboard } from './PrincipalSyncDashboard';
import { Student, StaffMember, AppConfig, DayScheduleMap } from '../types';
import { Database, Users, ArrowRight, Cloud, QrCode } from 'lucide-react';

interface SyncCenterViewProps {
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  staffList: StaffMember[];
  config: AppConfig;
  scheduleMap: DayScheduleMap;
  onBackToMain: () => void;
}

export const SyncCenterView: React.FC<SyncCenterViewProps> = ({
  students,
  setStudents,
  staffList,
  config,
  scheduleMap,
  onBackToMain
}) => {
  const [activeTab, setActiveTab] = useState<'inspector' | 'teachers'>('inspector');

  return (
    <div className="p-4 md:p-6 max-w-7xl mx-auto space-y-6 dir-rtl">
      
      {/* Top Header Card with Back Button */}
      <div className="bg-white border-4 theme-accent-border rounded-3xl p-5 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-blue-600 text-white flex items-center justify-center shadow-lg shadow-indigo-200 shrink-0">
            <QrCode className="w-7 h-7 text-amber-300" />
          </div>
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-indigo-50 border border-indigo-200 text-indigo-900 text-xs font-black mb-1">
              <Cloud className="w-3.5 h-3.5 text-indigo-600" />
              <span>مركز المزامنة وتوليد الباركود والربط السحابي ⚡</span>
            </div>
            <h1 className="text-xl md:text-2xl font-black text-slate-900">
              مركز الربط السحابي وتوليد أكواد المعلمين (QR Hub)
            </h1>
            <p className="text-xs text-slate-600 font-bold mt-0.5">
              امسح رمز الباركود QR من هواتف المعلمين للربط اللحظي واستعراض الجداول السحابية.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={onBackToMain}
            className="flex items-center gap-2 px-5 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs md:text-sm transition-all shadow-lg hover:shadow-indigo-300 cursor-pointer active:scale-95"
            title="الرجوع إلى الشاشة الرئيسية"
          >
            <ArrowRight className="w-4 h-4" />
            <span>العودة للرئيسية ✕</span>
          </button>
        </div>
      </div>

      {/* Tabs Selector */}
      <div className="flex bg-slate-100 p-2 rounded-2xl border border-slate-200 gap-2 shadow-sm">
        <button
          onClick={() => setActiveTab('inspector')}
          className={`flex-1 py-3 px-4 rounded-xl font-black text-xs md:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer ${
            activeTab === 'inspector'
              ? 'bg-indigo-600 text-white shadow-lg'
              : 'text-slate-600 hover:bg-slate-200'
          }`}
        >
          <Database className="w-4 h-4" />
          <span>مركز فاحص ومزامنة السحابة والباركود (Live Progress & Inspector)</span>
        </button>

        <button
          onClick={() => setActiveTab('teachers')}
          className={`flex-1 py-3 px-4 rounded-xl font-black text-xs md:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer ${
            activeTab === 'teachers'
              ? 'bg-indigo-600 text-white shadow-lg'
              : 'text-slate-600 hover:bg-slate-200'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>المعلمون المسجلون وحالة الاتصال والمزامنة</span>
        </button>
      </div>

      {/* Tab Contents */}
      <div className="min-h-[500px]">
        {activeTab === 'inspector' && (
          <CloudLiveInspector
            students={students}
            staffList={staffList}
            config={config}
            scheduleMap={scheduleMap}
            onBack={onBackToMain}
          />
        )}

        {activeTab === 'teachers' && (
          <PrincipalSyncDashboard
            students={students}
            setStudents={setStudents}
            schedule={scheduleMap}
            onBack={onBackToMain}
          />
        )}
      </div>

    </div>
  );
};
