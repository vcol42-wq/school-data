import React from 'react';
import { StaffMember, Student, DayScheduleMap } from '../types';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  Tooltip, 
  ResponsiveContainer, 
  PieChart, 
  Pie, 
  Cell 
} from 'recharts';
import { 
  BarChart3, 
  Users, 
  GraduationCap, 
  Layers, 
  CheckCircle2, 
  XCircle, 
  AlertTriangle, 
  Sparkles,
  PieChart as PieIcon
} from 'lucide-react';

interface StatisticsViewProps {
  staffList: StaffMember[];
  students: Student[];
  scheduleMap: DayScheduleMap;
}

export const StatisticsView: React.FC<StatisticsViewProps> = ({
  staffList,
  students,
  scheduleMap
}) => {
  // Business Rule: Long leave or seconded out are "خارج الملاك"
  const onRosterStaff = staffList.filter(s => s.status !== 'مجاز إجازة طويلة' && s.status !== 'منسب خارج المدرسة');
  const offRosterStaff = staffList.filter(s => s.status === 'مجاز إجازة طويلة' || s.status === 'منسب خارج المدرسة');

  // Student Statistics
  const grade1Students = students.filter(s => s.currentGrade.includes('الأول'));
  const grade2Students = students.filter(s => s.currentGrade.includes('الثاني'));
  const grade3Students = students.filter(s => s.currentGrade.includes('الثالث'));

  const totalStudents = students.length;
  const activeStudents = students.filter(s => s.status === 'مستمر').length;
  const transferredStudents = students.filter(s => s.status === 'غادر المدرسة').length;

  // Grade Sections Count
  const grade1Sections = new Set(grade1Students.map(s => s.section)).size || 2;
  const grade2Sections = new Set(grade2Students.map(s => s.section)).size || 2;
  const grade3Sections = new Set(grade3Students.map(s => s.section)).size || 1;
  const totalSections = grade1Sections + grade2Sections + grade3Sections;

  // Subject Census Statistics
  const subjectList = ['اللغة العربية', 'الرياضيات', 'الفيزياء', 'الكيمياء', 'الأحياء', 'التربية الإسلامية', 'الجغرافيا والتاريخ', 'اللغة الإنكليزية', 'الحاسوب'];

  const subjectCensus = subjectList.map(subj => {
    const totalTech = onRosterStaff.filter(s => s.specialization.includes(subj.split(' ')[0]) || subj.includes(s.specialization.split(' ')[0])).length;
    
    // Required teachers estimation (e.g. 2 per subject)
    const required = 2;
    const surplus = totalTech > required ? totalTech - required : 0;
    const vacancy = totalTech < required ? required - totalTech : 0;

    return {
      subject: subj,
      teachersCount: totalTech,
      surplus,
      vacancy
    };
  });

  // Recharts Data Structures
  const studentChartData = [
    { name: 'الصف الأول متوسط', studentsCount: grade1Students.length, sectionsCount: grade1Sections },
    { name: 'الصف الثاني متوسط', studentsCount: grade2Students.length, sectionsCount: grade2Sections },
    { name: 'الصف الثالث متوسط', studentsCount: grade3Students.length, sectionsCount: grade3Sections },
  ];

  const staffStatusData = [
    { name: 'مستمر في الملاك', value: onRosterStaff.length, color: '#10b981' },
    { name: 'مجاز / منسب خارج الملاك', value: offRosterStaff.length, color: '#f59e0b' },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
      
      {/* Title & Banner */}
      <div className="bg-[var(--theme-card)] p-5 rounded-2xl border border-[var(--theme-card-border)] shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300 text-xs font-bold mb-2">
            <BarChart3 className="w-3.5 h-3.5" />
            <span>الإحصاء الشامل واستمارة الملاك</span>
          </div>
          <h2 className="text-xl md:text-2xl font-black text-[var(--theme-text-main)]">
            لوحة الإحصاءات الرسمية والملاك المدرسي
          </h2>
          <p className="text-xs text-[var(--theme-text-muted)] mt-1">
            إحصائيات الملاكات والمواد وفق ضوابط مديرية تربية ديالى والشواغر والفائض
          </p>
        </div>

        {/* Totals Pill Badges */}
        <div className="flex items-center gap-3 bg-slate-100 dark:bg-slate-800 p-3 rounded-xl border border-slate-200 dark:border-slate-700">
          <div className="text-center px-3 border-l border-slate-300 dark:border-slate-700">
            <span className="text-xl font-black text-blue-600 block">{totalStudents}</span>
            <span className="text-[11px] text-slate-500 font-bold">إجمالي الطلاب</span>
          </div>
          <div className="text-center px-3 border-l border-slate-300 dark:border-slate-700">
            <span className="text-xl font-black text-indigo-600 block">{totalSections}</span>
            <span className="text-[11px] text-slate-500 font-bold">إجمالي الشعب</span>
          </div>
          <div className="text-center px-3">
            <span className="text-xl font-black text-emerald-600 block">{onRosterStaff.length}</span>
            <span className="text-[11px] text-slate-500 font-bold">الملاك الفعلي</span>
          </div>
        </div>
      </div>

      {/* Top Stat Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        
        <div className="p-4 rounded-2xl bg-[var(--theme-card)] border border-[var(--theme-card-border)] shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs text-[var(--theme-text-muted)] font-bold block">إجمالي الطلاب المقيدين</span>
            <span className="text-2xl font-black text-blue-600 mt-1 block">{totalStudents} طالب</span>
            <span className="text-[11px] text-emerald-600 font-medium">مستمر: {activeStudents} | غادر: {transferredStudents}</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-900/40 text-blue-600 flex items-center justify-center">
            <GraduationCap className="w-6 h-6" />
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-[var(--theme-card)] border border-[var(--theme-card-border)] shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs text-[var(--theme-text-muted)] font-bold block">المجموع الكلي للشعب</span>
            <span className="text-2xl font-black text-indigo-600 mt-1 block">{totalSections} شعبة</span>
            <span className="text-[11px] text-indigo-500 font-medium">الأول: {grade1Sections} | الثاني: {grade2Sections} | الثالث: {grade3Sections}</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-indigo-50 dark:bg-indigo-900/40 text-indigo-600 flex items-center justify-center">
            <Layers className="w-6 h-6" />
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-[var(--theme-card)] border border-[var(--theme-card-border)] shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs text-[var(--theme-text-muted)] font-bold block">الكادر المحسوب على الملاك</span>
            <span className="text-2xl font-black text-emerald-600 mt-1 block">{onRosterStaff.length} مدرس</span>
            <span className="text-[11px] text-slate-500 font-medium">محسوبين ضمن القوة التدريسية</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-emerald-50 dark:bg-emerald-900/40 text-emerald-600 flex items-center justify-center">
            <CheckCircle2 className="w-6 h-6" />
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-[var(--theme-card)] border border-[var(--theme-card-border)] shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs text-[var(--theme-text-muted)] font-bold block">المجازين والمنسبين خارجيًا</span>
            <span className="text-2xl font-black text-amber-600 mt-1 block">{offRosterStaff.length} منتسب</span>
            <span className="text-[11px] text-rose-500 font-bold">خارج حسابات القوة الملاكية</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-amber-50 dark:bg-amber-900/40 text-amber-600 flex items-center justify-center">
            <AlertTriangle className="w-6 h-6" />
          </div>
        </div>

      </div>

      {/* Section 1: Staff Roster Table (استمارة الملاك الرسمية) */}
      <div className="bg-[var(--theme-card)] rounded-2xl border border-[var(--theme-card-border)] shadow-lg overflow-hidden space-y-3 p-4">
        <div className="flex items-center justify-between border-b pb-3">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-amber-500" />
            <h3 className="text-base font-bold text-[var(--theme-text-main)]">
              استمارة ملاك المدرسين والمشرفين (نموذج وزارة التربية الرسمي)
            </h3>
          </div>
          <span className="text-xs font-bold text-slate-500 bg-slate-100 dark:bg-slate-800 px-3 py-1 rounded-full">
            تحديث فوري
          </span>
        </div>

        <div className="overflow-x-auto no-scrollbar">
          <table className="w-full text-center border-collapse text-xs min-w-[1200px]">
            <thead>
              <tr className="bg-slate-800 text-white font-bold border-b border-slate-700">
                <th className="py-2.5 px-2 border-r border-slate-700 w-10">ت</th>
                <th className="py-2.5 px-3 border-r border-slate-700">الاسم الكامل</th>
                <th className="py-2.5 px-2 border-r border-slate-700">الجنس</th>
                <th className="py-2.5 px-2 border-r border-slate-700">المواليد</th>
                <th className="py-2.5 px-2 border-r border-slate-700">العنوان الوظيفي</th>
                <th className="py-2.5 px-2 border-r border-slate-700">تاريخ المباشرة الأولى</th>
                <th className="py-2.5 px-2 border-r border-slate-700">تاريخ بالمدرسة</th>
                <th className="py-2.5 px-2 border-r border-slate-700">الشهادة</th>
                <th className="py-2.5 px-2 border-r border-slate-700">التخصص</th>
                <th className="py-2.5 px-3 border-r border-slate-700">محل السكن</th>
                <th className="py-2.5 px-2 border-r border-slate-700">حالة المنتسب</th>
                <th className="py-2.5 px-2 border-r border-slate-700">الصفوف المكلف بها</th>
                <th className="py-2.5 px-2 border-r border-slate-700">عدد الشعب</th>
                <th className="py-2.5 px-2">نصاب الأستاذ</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-[var(--theme-card-border)]">
              {staffList.map((stf, idx) => {
                const isOffRoster = stf.status === 'مجاز إجازة طويلة' || stf.status === 'منسب خارج المدرسة';
                return (
                  <tr 
                    key={stf.id} 
                    className={`hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-colors ${
                      isOffRoster ? 'bg-amber-50/50 dark:bg-amber-950/20 text-slate-500' : ''
                    }`}
                  >
                    <td className="py-2.5 px-2 font-mono font-bold border-r border-[var(--theme-card-border)]">{idx + 1}</td>
                    <td className="py-2.5 px-3 font-bold border-r border-[var(--theme-card-border)] text-right">
                      {stf.firstName} {stf.secondName} {stf.thirdName} {stf.titleName}
                    </td>
                    <td className="py-2.5 px-2 border-r border-[var(--theme-card-border)]">ذكر</td>
                    <td className="py-2.5 px-2 font-mono border-r border-[var(--theme-card-border)]">{stf.birthYear}</td>
                    <td className="py-2.5 px-2 border-r border-[var(--theme-card-border)] font-bold text-purple-700">{stf.functionalTitle}</td>
                    <td className="py-2.5 px-2 font-mono border-r border-[var(--theme-card-border)]">{stf.firstDirectYear}</td>
                    <td className="py-2.5 px-2 font-mono border-r border-[var(--theme-card-border)]">{stf.schoolDirectYear}</td>
                    <td className="py-2.5 px-2 border-r border-[var(--theme-card-border)]">{stf.academicDegree}</td>
                    <td className="py-2.5 px-2 border-r border-[var(--theme-card-border)] font-bold text-blue-600">{stf.specialization}</td>
                    <td className="py-2.5 px-3 border-r border-[var(--theme-card-border)] text-right text-[11px]">{stf.residenceDistrict}</td>
                    
                    <td className="py-2.5 px-2 border-r border-[var(--theme-card-border)]">
                      <span className={`px-2 py-0.5 rounded font-bold text-[10px] ${
                        isOffRoster ? 'bg-rose-100 text-rose-800 border border-rose-300' : 'bg-emerald-100 text-emerald-800'
                      }`}>
                        {stf.status} {isOffRoster && '(خارج الملاك)'}
                      </span>
                    </td>

                    <td className="py-2.5 px-2 border-r border-[var(--theme-card-border)] text-[11px]">
                      {stf.classesTaught.join('، ') || 'غير مخصص'}
                    </td>
                    
                    <td className="py-2.5 px-2 border-r border-[var(--theme-card-border)] font-bold">{stf.sectionsTaughtCount}</td>
                    <td className="py-2.5 px-2 font-bold text-amber-600">{stf.teachingQuota} حصة</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Section 2: Subject Census Stats (الشواغر والفائض) */}
      <div className="bg-[var(--theme-card)] rounded-2xl border border-[var(--theme-card-border)] p-5 shadow-sm space-y-4">
        <h3 className="text-base font-bold text-[var(--theme-text-main)] border-b pb-2">
          إحصاء أسماء المواد وعدد المدرسين والمدرسات والشواغر والفائض
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 text-xs">
          {subjectCensus.map((item) => (
            <div key={item.subject} className="p-4 rounded-xl bg-white border-2 border-sky-300 shadow-sm flex flex-col justify-between">
              <div className="flex items-center justify-between border-b border-slate-200 pb-2 mb-2">
                <span className="font-black text-sm text-slate-900">{item.subject}</span>
                <span className="bg-sky-100 text-sky-950 px-2.5 py-0.5 rounded-full font-black border border-sky-300">
                  {item.teachersCount} مدرسين
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 mt-1">
                <div className="p-2 rounded-lg bg-emerald-50 text-emerald-950 border border-emerald-300 text-center font-black">
                  <span className="block text-[10px] font-bold">عدد الفائض</span>
                  <span className="text-base font-black">{item.surplus}</span>
                </div>

                <div className="p-2 rounded-lg bg-rose-50 text-rose-950 border border-rose-300 text-center font-black">
                  <span className="block text-[10px] font-bold">عدد الشواغر</span>
                  <span className="text-base font-black">{item.vacancy}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Visual Analytics with Recharts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Student Density Chart */}
        <div className="bg-[var(--theme-card)] rounded-2xl border border-[var(--theme-card-border)] p-5 shadow-sm space-y-3">
          <h4 className="text-sm font-bold text-[var(--theme-text-main)]">توزيع أعداد الطلاب والشعب بين المراحل الدراسية</h4>
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={studentChartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="studentsCount" fill="#2563eb" name="عدد الطلاب" radius={[6, 6, 0, 0]} />
                <Bar dataKey="sectionsCount" fill="#059669" name="عدد الشعب" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Staff Roster Pie Chart */}
        <div className="bg-[var(--theme-card)] rounded-2xl border border-[var(--theme-card-border)] p-5 shadow-sm space-y-3">
          <h4 className="text-sm font-bold text-[var(--theme-text-main)]">نسبة الملاك الفعلي مقابل الخارج الملاك</h4>
          <div className="h-64 w-full flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={staffStatusData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                  label={({ name, value }) => `${name}: ${value}`}
                >
                  {staffStatusData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

      </div>

    </div>
  );
};
