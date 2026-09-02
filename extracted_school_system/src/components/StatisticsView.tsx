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
import { canonicalSubject } from '../utils/subjectHelper';

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

  // Subject Census Statistics using Smart Canonical Subject Normalization
  const subjectList = [
    'التربية الإسلامية',
    'اللغة العربية',
    'اللغة الإنكليزية',
    'الرياضيات',
    'الاجتماعيات',
    'الأحياء',
    'الكيمياء',
    'الفيزياء',
    'الحاسوب',
    'التربية الرياضية',
    'التربية الفنية'
  ];

  const subjectCensus = subjectList.map(subj => {
    const canonicalTarget = canonicalSubject(subj);
    const totalTech = onRosterStaff.filter(s => {
      const specCanon = canonicalSubject(s.specialization);
      const actCanon = s.actualSubjectTaught ? canonicalSubject(s.actualSubjectTaught) : specCanon;
      return specCanon === canonicalTarget || actCanon === canonicalTarget;
    }).length;
    
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
        <div className="flex items-center gap-3 bg-white p-3.5 rounded-2xl border-2 border-slate-300 shadow-sm">
          <div className="text-center px-4 border-l-2 border-slate-200">
            <span className="text-2xl font-black text-blue-700 block font-mono">{totalStudents}</span>
            <span className="text-xs text-slate-800 font-extrabold">إجمالي الطلاب</span>
          </div>
          <div className="text-center px-4 border-l-2 border-slate-200">
            <span className="text-2xl font-black text-indigo-700 block font-mono">{totalSections}</span>
            <span className="text-xs text-slate-800 font-extrabold">إجمالي الشعب</span>
          </div>
          <div className="text-center px-4">
            <span className="text-2xl font-black text-emerald-700 block font-mono">{onRosterStaff.length}</span>
            <span className="text-xs text-slate-800 font-extrabold">الملاك الفعلي</span>
          </div>
        </div>
      </div>

      {/* Top Stat Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        
        <div className="p-5 rounded-2xl bg-white border-2 border-blue-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-700 font-extrabold block">إجمالي الطلاب المقيدين</span>
            <span className="text-3xl font-black text-blue-800 mt-1 block font-mono">{totalStudents} طالب</span>
            <span className="text-[11px] text-emerald-700 font-bold">مستمر: {activeStudents} | غادر: {transferredStudents}</span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-blue-100 text-blue-700 flex items-center justify-center">
            <GraduationCap className="w-6 h-6" />
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-white border-2 border-indigo-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-700 font-extrabold block">المجموع الكلي للشعب</span>
            <span className="text-3xl font-black text-indigo-800 mt-1 block font-mono">{totalSections} شعبة</span>
            <span className="text-[11px] text-indigo-700 font-bold">الأول: {grade1Sections} | الثاني: {grade2Sections} | الثالث: {grade3Sections}</span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-indigo-100 text-indigo-700 flex items-center justify-center">
            <Layers className="w-6 h-6" />
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-white border-2 border-emerald-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-700 font-extrabold block">الكادر المحسوب على الملاك</span>
            <span className="text-3xl font-black text-emerald-800 mt-1 block font-mono">{onRosterStaff.length} مدرس</span>
            <span className="text-[11px] text-slate-600 font-bold">محسوبين ضمن القوة التدريسية</span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
            <CheckCircle2 className="w-6 h-6" />
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-white border-2 border-amber-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-700 font-extrabold block">المجازين والمنسبين خارجيًا</span>
            <span className="text-3xl font-black text-amber-800 mt-1 block font-mono">{offRosterStaff.length} منتسب</span>
            <span className="text-[11px] text-rose-700 font-bold">خارج حسابات القوة الملاكية</span>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center">
            <AlertTriangle className="w-6 h-6" />
          </div>
        </div>

      </div>

      {/* Section 1: Staff Roster Table (استمارة الملاك والسجل التفصيلي العريض) */}
      <div className="bg-[var(--theme-card)] rounded-2xl border border-[var(--theme-card-border)] shadow-lg overflow-hidden space-y-3 p-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 border-b pb-3">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-amber-500" />
            <div>
              <h3 className="text-base font-bold text-[var(--theme-text-main)]">
                السجل الموحد للملاك والمعلومات المفصلة للمدرسين والمعلمين
              </h3>
              <p className="text-xs text-[var(--theme-text-muted)]">
                شريط عريض يمتد أفقياً ليتسع لكافة البيانات الإدارية والمدنية والمالية مع الاسم الكامل واللقب
              </p>
            </div>
          </div>
          <span className="text-xs font-bold text-indigo-700 bg-indigo-50 border border-indigo-200 px-3 py-1 rounded-full">
            شريط أفقي عريض ↔️
          </span>
        </div>

        <div className="overflow-x-auto custom-scrollbar border rounded-xl border-slate-200 shadow-inner">
          <table className="w-full text-center border-collapse text-xs min-w-[1800px]">
            <thead>
              <tr className="bg-slate-900 text-white font-black border-b-2 border-slate-700 text-xs">
                <th className="py-3 px-2 border-r border-slate-700 w-10">ت</th>
                <th className="py-3 px-3 border-r border-slate-700 text-right whitespace-nowrap min-w-[200px]">اسم الأستاذ الكامل واللقب</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">الوظيفة / العنوان الوظيفي</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">الشهادة الأكاديمية</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">الاختصاص الدقيق</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">المادة التي يدرّسها</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">الصفوف والشعب</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">نصاب الحصص</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">الرقم الوطني الموحد</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">رقم وتاريخ أمر التعيين</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">المباشرة الأولى</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">المباشرة بالمدرسة</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">الخدمة الكلية</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">بطاقة السكن ومحل السكن</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">البطاقة والمركز التمويني</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">رقم الهاتف</th>
                <th className="py-3 px-3 border-r border-slate-700 whitespace-nowrap">الرقم الحسابي / الماستر</th>
                <th className="py-3 px-3 whitespace-nowrap">الحالة في الملاك</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-200">
              {staffList.map((stf, idx) => {
                const isOffRoster = stf.status === 'مجاز إجازة طويلة' || stf.status === 'منسب خارج المدرسة';
                const fullName = [stf.firstName, stf.secondName, stf.thirdName, stf.fourthName, stf.titleName].filter(Boolean).join(' ');
                
                return (
                  <tr 
                    key={stf.id} 
                    className={`hover:bg-sky-50/80 transition-colors ${
                      isOffRoster ? 'bg-amber-50/60 text-slate-500' : 'bg-white'
                    }`}
                  >
                    {/* 1. Seq */}
                    <td className="py-3 px-2 font-mono font-bold border-r border-slate-200">{idx + 1}</td>
                    
                    {/* 2. Full Name */}
                    <td className="py-3 px-3 font-black border-r border-slate-200 text-right whitespace-nowrap text-slate-900">
                      أ. {fullName}
                    </td>

                    {/* 3. Job Title */}
                    <td className="py-3 px-3 border-r border-slate-200 font-bold text-purple-700 whitespace-nowrap">
                      {stf.jobTitle || stf.functionalTitle || 'مدرس'}
                    </td>

                    {/* 4. Academic Degree */}
                    <td className="py-3 px-3 border-r border-slate-200 whitespace-nowrap">
                      {stf.academicDegree || 'بكالوريوس'}
                    </td>

                    {/* 5. Specialization */}
                    <td className="py-3 px-3 border-r border-slate-200 font-bold text-sky-700 whitespace-nowrap">
                      {stf.specialization || 'عام'}
                    </td>

                    {/* 6. Actual Subject */}
                    <td className="py-3 px-3 border-r border-slate-200 font-bold text-indigo-700 whitespace-nowrap">
                      {stf.actualSubjectTaught || stf.specialization || 'اللغة العربية'}
                    </td>

                    {/* 7. Classes & Sections */}
                    <td className="py-3 px-3 border-r border-slate-200 whitespace-nowrap">
                      {Array.isArray(stf.classesTaught) ? stf.classesTaught.join('، ') : 'الصف الأول أ'}
                    </td>

                    {/* 8. Quota */}
                    <td className="py-3 px-3 border-r border-slate-200 font-black text-emerald-700 whitespace-nowrap">
                      {stf.teachingQuota || 18} حصة
                    </td>

                    {/* 9. National ID */}
                    <td className="py-3 px-3 border-r border-slate-200 font-mono font-bold text-slate-700 whitespace-nowrap">
                      {stf.nationalCardNumber || '—'}
                    </td>

                    {/* 10. Appointment Order */}
                    <td className="py-3 px-3 border-r border-slate-200 font-mono text-[11px] whitespace-nowrap">
                      {stf.appointmentOrderNo || '—'}
                    </td>

                    {/* 11. First Direct Date */}
                    <td className="py-3 px-3 border-r border-slate-200 font-mono text-[11px] whitespace-nowrap">
                      {stf.firstDirectYear}/{stf.firstDirectMonth}/{stf.firstDirectDay}
                    </td>

                    {/* 12. School Direct Date */}
                    <td className="py-3 px-3 border-r border-slate-200 font-mono text-[11px] whitespace-nowrap">
                      {stf.schoolDirectYear}/{stf.schoolDirectMonth}/{stf.schoolDirectDay}
                    </td>

                    {/* 13. Service Years */}
                    <td className="py-3 px-3 border-r border-slate-200 font-bold whitespace-nowrap">
                      {stf.yearsOfService || 0} سنة
                    </td>

                    {/* 14. Residence */}
                    <td className="py-3 px-3 border-r border-slate-200 text-right text-[11px] whitespace-nowrap">
                      {stf.residenceDistrict || '—'} ({stf.residenceCardNumber || 'بدون بطاقة'})
                    </td>

                    {/* 15. Ration Card */}
                    <td className="py-3 px-3 border-r border-slate-200 font-mono text-[11px] whitespace-nowrap">
                      {stf.rationCardNumber || '—'} / م {stf.rationCenterNumber || '304'}
                    </td>

                    {/* 16. Phone */}
                    <td className="py-3 px-3 border-r border-slate-200 font-mono font-bold text-indigo-900 whitespace-nowrap" dir="ltr">
                      {stf.phoneNumber || '—'}
                    </td>

                    {/* 17. Salary Account */}
                    <td className="py-3 px-3 border-r border-slate-200 font-mono text-[10px] whitespace-nowrap" dir="ltr">
                      {stf.salaryAccountNumber || '—'}
                    </td>

                    {/* 18. Status */}
                    <td className="py-3 px-3 whitespace-nowrap">
                      <span className={`px-2.5 py-1 rounded-full font-bold text-[11px] ${
                        isOffRoster ? 'bg-rose-100 text-rose-800 border border-rose-300' : 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                      }`}>
                        {stf.status} {isOffRoster && '(خارج الملاك)'}
                      </span>
                    </td>

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
