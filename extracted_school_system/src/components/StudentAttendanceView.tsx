import React, { useState, useMemo } from 'react';
import { Student, AppConfig } from '../types';
import { 
  UserCheck, 
  Search, 
  Printer, 
  CloudDownload, 
  AlertTriangle, 
  ShieldAlert, 
  CheckCircle, 
  Users, 
  Plus, 
  Minus, 
  FileText, 
  RefreshCw,
  Calendar,
  Filter,
  X
} from 'lucide-react';
import { importGradesAndAttendance } from '../utils/syncService';
import { sortGradesList, sortSectionsAlphabetically } from '../utils/syncEngine';
import { Portal } from './common/Portal';

interface StudentAttendanceViewProps {
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  config: AppConfig;
  onBackToMain?: () => void;
}

export const StudentAttendanceView: React.FC<StudentAttendanceViewProps> = ({
  students,
  setStudents,
  config,
  onBackToMain
}) => {
  const [selectedGrade, setSelectedGrade] = useState<string>('الكل');
  const [selectedSection, setSelectedSection] = useState<string>('الكل');
  const [selectedFilterRisk, setSelectedFilterRisk] = useState<'all' | 'zero' | 'warning' | 'danger'>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isPulling, setIsPulling] = useState<boolean>(false);
  const [pullMsg, setPullMsg] = useState<string>('');
  
  // Warning Letter Modal state
  const [warningModalStudent, setWarningModalStudent] = useState<Student | null>(null);

  // Extract unique grades & sections
  const uniqueGrades = sortGradesList(['الكل', ...Array.from(new Set(students.map(s => s.currentGrade).filter(Boolean)))]);
  const uniqueSections = sortSectionsAlphabetically(['الكل', ...Array.from(new Set(students.map(s => s.section).filter(Boolean)))]);

  // Helper to determine risk level
  const getAbsenceStatus = (count: number) => {
    if (count === 0) return { label: 'منتظم (100%)', badge: 'bg-emerald-100 text-emerald-800 border-emerald-300', level: 'zero' };
    if (count <= 3) return { label: 'تنبيه شفهي', badge: 'bg-yellow-100 text-yellow-800 border-yellow-300', level: 'warning' };
    if (count <= 6) return { label: 'إنذار أول', badge: 'bg-orange-100 text-orange-800 border-orange-300', level: 'warning' };
    if (count <= 10) return { label: 'إنذار نهائي', badge: 'bg-rose-100 text-rose-800 border-rose-300', level: 'danger' };
    return { label: 'تجاوز الحد القانوني (فصل)', badge: 'bg-red-200 text-red-900 border-red-500 font-black', level: 'danger' };
  };

  // Filtered and Alphabetically Sorted Students
  const filteredStudents = useMemo(() => {
    return students
      .filter(s => {
        if (selectedGrade !== 'الكل' && s.currentGrade !== selectedGrade) return false;
        if (selectedSection !== 'الكل' && s.section !== selectedSection) return false;
        
        const count = s.absencesCount || 0;
        if (selectedFilterRisk === 'zero' && count > 0) return false;
        if (selectedFilterRisk === 'warning' && (count < 1 || count > 6)) return false;
        if (selectedFilterRisk === 'danger' && count <= 6) return false;

        const fullName = `${s.firstName} ${s.secondName || ''} ${s.thirdName || ''} ${s.fourthName || ''} ${s.titleName || ''}`.toLowerCase();
        const q = searchQuery.toLowerCase().trim();
        if (q) {
          return fullName.includes(q) || (s.recordNumber && s.recordNumber.includes(q));
        }
        return true;
      })
      .sort((a, b) => {
        const nameA = [a.firstName, a.secondName, a.thirdName, a.fourthName, a.titleName].filter(Boolean).join(' ').trim();
        const nameB = [b.firstName, b.secondName, b.thirdName, b.fourthName, b.titleName].filter(Boolean).join(' ').trim();
        return nameA.localeCompare(nameB, 'ar', { sensitivity: 'base' });
      });
  }, [students, selectedGrade, selectedSection, selectedFilterRisk, searchQuery]);

  // Statistics
  const totalStudentsCount = students.length;
  const zeroAbsencesCount = students.filter(s => (s.absencesCount || 0) === 0).length;
  const warningAbsencesCount = students.filter(s => (s.absencesCount || 0) >= 1 && (s.absencesCount || 0) <= 6).length;
  const dangerAbsencesCount = students.filter(s => (s.absencesCount || 0) > 6).length;
  const totalAbsencesSum = students.reduce((acc, s) => acc + (s.absencesCount || 0), 0);

  // Update Absences directly
  const handleAdjustAbsence = (studentId: string, delta: number) => {
    const updated = students.map(s => {
      if (s.id === studentId) {
        const current = s.absencesCount || 0;
        const next = Math.max(0, current + delta);
        return { ...s, absencesCount: next };
      }
      return s;
    });
    setStudents(updated);
    localStorage.setItem('diyala_school_students', JSON.stringify(updated));
  };

  // Pull Absences from Cloud
  const handlePullCloudAbsences = async () => {
    const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
    setIsPulling(true);
    setPullMsg('جاري جلب سجلات الغياب من السحابة...');
    try {
      const res = await importGradesAndAttendance(schoolId, students);
      const updated = res.updatedStudents || (res as any).studentsWithMarks;
      if (res.success && updated) {
        setStudents(updated);
        localStorage.setItem('diyala_school_students', JSON.stringify(updated));
        setPullMsg(res.message || 'تم تحديث سجل الغيابات بنجاح ✓');
      } else {
        setPullMsg(res.message || 'لم يتم العثور على تحديثات جديدة.');
      }
    } catch (e: any) {
      setPullMsg(`خطأ: ${e.message}`);
    } finally {
      setIsPulling(false);
    }
  };

  // Print Roster
  const handlePrintAbsenceRoster = () => {
    window.print();
  };

  return (
    <div className="min-h-screen bg-slate-100 p-4 md:p-6 font-tajawal text-slate-800" dir="rtl">
      {/* Header Bar */}
      <div className="bg-white rounded-2xl shadow-md border border-slate-200 p-5 mb-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-rose-600 to-amber-500 text-white flex items-center justify-center shadow-md shadow-rose-500/20">
              <UserCheck className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-800">سجل ومتابعة غيابات الطلاب</h1>
              <p className="text-xs text-slate-500 mt-0.5">
                متابعة الحضور اليومي، إحصاء أيام الغياب، وتوجيه الإنذارات الرسمية للطلبة
              </p>
            </div>
          </div>

          <div className="flex items-center flex-wrap gap-2">
            <button
              onClick={handlePullCloudAbsences}
              disabled={isPulling}
              className="flex items-center gap-2 px-4 py-2.5 bg-emerald-700 hover:bg-emerald-800 text-white rounded-xl font-bold text-xs shadow transition active:scale-95 disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${isPulling ? 'animate-spin' : ''}`} />
              <span>{isPulling ? 'جاري السحب...' : 'سحب الغيابات من تطبيق المدرس 🔄'}</span>
            </button>

            <button
              onClick={handlePrintAbsenceRoster}
              className="flex items-center gap-2 px-4 py-2.5 bg-indigo-700 hover:bg-indigo-800 text-white rounded-xl font-bold text-xs shadow transition active:scale-95"
            >
              <Printer className="w-4 h-4" />
              <span>طباعة كشف الغيابات 🖨️</span>
            </button>

            {onBackToMain && (
              <button
                onClick={onBackToMain}
                className="px-4 py-2.5 bg-slate-200 hover:bg-slate-300 text-slate-700 rounded-xl font-bold text-xs transition"
              >
                الرئيسية 🏠
              </button>
            )}
          </div>
        </div>

        {pullMsg && (
          <div className="mt-3 p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-bold text-center">
            {pullMsg}
          </div>
        )}
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-700 flex items-center justify-center">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-slate-500 font-bold">إجمالي الطلاب</div>
            <div className="text-xl font-black text-slate-800">{totalStudentsCount}</div>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
            <CheckCircle className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-emerald-600 font-bold">انتظام كامل (0 غياب)</div>
            <div className="text-xl font-black text-emerald-800">{zeroAbsencesCount}</div>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-amber-600 font-bold">تنبيه وإنذار (1-6)</div>
            <div className="text-xl font-black text-amber-800">{warningAbsencesCount}</div>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-100 text-rose-700 flex items-center justify-center">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-rose-600 font-bold">مهدد بالفصل (7+ غياب)</div>
            <div className="text-xl font-black text-rose-800">{dangerAbsencesCount}</div>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-4 mb-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Grade */}
          <div>
            <label className="block text-xs font-bold text-slate-600 mb-1">الصف الدراسي</label>
            <select
              value={selectedGrade}
              onChange={e => setSelectedGrade(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-rose-500"
            >
              {uniqueGrades.map(g => (
                <option key={g} value={g}>{g}</option>
              ))}
            </select>
          </div>

          {/* Section */}
          <div>
            <label className="block text-xs font-bold text-slate-600 mb-1">الشعبة</label>
            <select
              value={selectedSection}
              onChange={e => setSelectedSection(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-rose-500"
            >
              {uniqueSections.map(s => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>

          {/* Risk Level Filter */}
          <div>
            <label className="block text-xs font-bold text-slate-600 mb-1">مستوى الغياب</label>
            <select
              value={selectedFilterRisk}
              onChange={e => setSelectedFilterRisk(e.target.value as any)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-rose-500"
            >
              <option value="all">كافة الطلاب ({students.length})</option>
              <option value="zero">منتظمون 0 غياب ({zeroAbsencesCount})</option>
              <option value="warning">تنبيه وإنذار 1-6 غيابات ({warningAbsencesCount})</option>
              <option value="danger">خطر الفصل 7+ غيابات ({dangerAbsencesCount})</option>
            </select>
          </div>

          {/* Search */}
          <div>
            <label className="block text-xs font-bold text-slate-600 mb-1">بحث بالاسم أو القيد</label>
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute right-3 top-2.5" />
              <input
                type="text"
                placeholder="ابحث عن اسم الطالب..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl pr-9 pl-3 py-2 text-xs font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Main Table */}
      <div className="bg-white rounded-2xl border-2 border-slate-300 shadow-md overflow-hidden">
        <div className="overflow-x-auto custom-scrollbar">
          <table className="w-full text-center border-collapse min-w-[900px] text-xs">
            <thead>
              <tr className="bg-slate-100 text-slate-900 font-black border-b-2 border-slate-300 text-xs">
                <th className="py-3.5 px-2 border-r border-slate-300 w-12 text-center text-slate-950 font-black">ت</th>
                <th className="py-3.5 px-2 border-r border-slate-300 w-24 text-center text-slate-950 font-black">رقم القيد</th>
                <th className="py-3.5 px-3 border-r border-slate-300 text-right min-w-[200px] text-slate-950 font-black">اسم الطالب الرباعي واللقب</th>
                <th className="py-3.5 px-2 border-r border-slate-300 w-24 text-center text-slate-950 font-black">الصف</th>
                <th className="py-3.5 px-2 border-r border-slate-300 w-20 text-center text-slate-950 font-black">الشعبة</th>
                <th className="py-3.5 px-3 border-r border-slate-300 w-32 text-center bg-rose-50 text-rose-950 font-black">أيام/حصص الغياب</th>
                <th className="py-3.5 px-3 border-r border-slate-300 min-w-[160px] text-center text-slate-950 font-black">الموقف والانضباط</th>
                <th className="py-3.5 px-2 border-r border-slate-300 w-32 text-center text-slate-950 font-black">تعديل سريع</th>
                <th className="py-3.5 px-2 text-center w-28 text-slate-950 font-black">إجراءات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-10 text-slate-400 text-sm font-bold">
                    لا يوجد طلاب مطابقين لمعايير البحث والفلترة الحالية
                  </td>
                </tr>
              ) : (
                filteredStudents.map((std, idx) => {
                  const count = std.absencesCount || 0;
                  const statusInfo = getAbsenceStatus(count);
                  const fullName = [std.firstName, std.secondName, std.thirdName, std.fourthName, std.titleName].filter(Boolean).join(' ');

                  return (
                    <tr key={std.id} className="hover:bg-slate-50 transition">
                      <td className="py-2.5 px-2 border-r border-slate-200 font-mono font-bold text-slate-600">{idx + 1}</td>
                      <td className="py-2.5 px-2 border-r border-slate-200 font-mono text-slate-700">{std.recordNumber || '-'}</td>
                      <td className="py-2.5 px-3 border-r border-slate-200 text-right font-bold text-slate-900">{fullName}</td>
                      <td className="py-2.5 px-2 border-r border-slate-200 text-slate-700">{std.currentGrade}</td>
                      <td className="py-2.5 px-2 border-r border-slate-200 text-slate-700">{std.section}</td>
                      <td className="py-2.5 px-3 border-r border-slate-200 font-mono font-black text-sm">
                        <span className={`px-3 py-1 rounded-lg ${count === 0 ? 'text-emerald-700 bg-emerald-50' : count > 6 ? 'text-rose-700 bg-rose-50' : 'text-amber-700 bg-amber-50'}`}>
                          {count} {count === 1 ? 'يوم' : count === 2 ? 'يومان' : 'أيام'}
                        </span>
                      </td>
                      <td className="py-2.5 px-3 border-r border-slate-200">
                        <span className={`inline-block px-2.5 py-1 rounded-full text-xs font-bold border ${statusInfo.badge}`}>
                          {statusInfo.label}
                        </span>
                      </td>
                      <td className="py-2.5 px-2 border-r border-slate-200">
                        <div className="flex items-center justify-center gap-1">
                          <button
                            onClick={() => handleAdjustAbsence(std.id, 1)}
                            title="إضافة يوم غياب"
                            className="w-7 h-7 bg-rose-100 hover:bg-rose-200 text-rose-700 rounded-lg flex items-center justify-center font-bold transition active:scale-95"
                          >
                            <Plus className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleAdjustAbsence(std.id, -1)}
                            disabled={count === 0}
                            title="إنقاص يوم غياب"
                            className="w-7 h-7 bg-emerald-100 hover:bg-emerald-200 text-emerald-700 rounded-lg flex items-center justify-center font-bold transition active:scale-95 disabled:opacity-30"
                          >
                            <Minus className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                      <td className="py-2.5 px-2">
                        <button
                          onClick={() => setWarningModalStudent(std)}
                          className="px-2 py-1 bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-300 rounded-lg font-bold text-xs transition"
                        >
                          إنذار غياب 📄
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Official Warning Letter Modal */}
      {warningModalStudent && (
        <Portal>
          <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl relative" dir="rtl">
              <button
                onClick={() => setWarningModalStudent(null)}
                className="absolute left-4 top-4 text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>

              <div className="border-4 border-double border-slate-700 p-6 rounded-xl text-center bg-amber-50/20">
                <div className="flex justify-between items-center text-xs font-bold text-slate-600 mb-4 border-b border-slate-300 pb-2">
                  <div>
                    <p>جمهورية العراق</p>
                    <p>وزارة التربية</p>
                    <p>{config.directorateName || 'المديرية العامة للتربية'}</p>
                  </div>
                  <div>
                    <h2 className="text-base font-black text-rose-800">
                      {warningModalStudent.absencesCount && warningModalStudent.absencesCount > 6 ? 'إنذار نهائي وتنبيه بالفصل' : 'إنذار غياب رسمي'}
                    </h2>
                    <p>الرقم: م / غ / {warningModalStudent.recordNumber || '---'}</p>
                  </div>
                  <div>
                    <p>{config.schoolName || 'المدرسة'}</p>
                    <p>التاريخ: {new Date().toLocaleDateString('ar-IQ')}</p>
                  </div>
                </div>

                <div className="my-6 text-right text-sm leading-relaxed font-bold text-slate-800">
                  <p className="mb-2">إلى ولي أمر الطالب / الطالبة: <span className="text-rose-700 font-black text-base">{[warningModalStudent.firstName, warningModalStudent.secondName, warningModalStudent.thirdName, warningModalStudent.titleName].filter(Boolean).join(' ')}</span> المحترم</p>
                  <p className="mb-2">الصف: <span className="text-indigo-700">{warningModalStudent.currentGrade}</span> | الشعبة: <span className="text-indigo-700">{warningModalStudent.section}</span> | رقم القيد: <span className="text-indigo-700">{warningModalStudent.recordNumber}</span></p>
                  <p className="mt-4 leading-7">
                    نود إعلامكم بأن الطالب المذكور أعلاه قد بلغ مجموع غياباته بدون عذر مشروع <span className="text-rose-800 text-lg font-black underline">({warningModalStudent.absencesCount || 0}) يوماً</span> خلال العام الدراسي الحالي.
                    يرجى الحضور إلى إدارة المدرسة خلال مدة أقصاها (٣) أيام لبيان أسباب الغياب وتلافي صدور قرار الفصل بموجب التعليمات والأنظمة الوزارية النافذة.
                  </p>
                </div>

                <div className="flex justify-between items-center mt-10 pt-4 border-t border-slate-300 text-xs font-bold text-slate-700">
                  <div>
                    <p>المرشد التربوي / المعاون</p>
                    <p className="mt-6">..........................................</p>
                  </div>
                  <div>
                    <p>مدير المدرسة</p>
                    <p className="text-sm font-black text-slate-900 mt-1">{config.managerName || 'مدير المدرسة'}</p>
                    <p className="mt-4">الختم والتوقيع الرسمي</p>
                  </div>
                </div>
              </div>

              <div className="flex justify-end gap-2 mt-4">
                <button
                  onClick={() => window.print()}
                  className="px-5 py-2.5 bg-rose-700 hover:bg-rose-800 text-white font-bold text-xs rounded-xl shadow flex items-center gap-2"
                >
                  <Printer className="w-4 h-4" />
                  <span>طباعة هذا الإنذار فوراً</span>
                </button>
                <button
                  onClick={() => setWarningModalStudent(null)}
                  className="px-4 py-2.5 bg-slate-200 hover:bg-slate-300 text-slate-700 font-bold text-xs rounded-xl"
                >
                  إغلاق
                </button>
              </div>
            </div>
          </div>
        </Portal>
      )}
    </div>
  );
};
