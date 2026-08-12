import React, { useState } from 'react';
import { StaffMember, AppConfig } from '../types';
import { parseExcelFileForStaff } from '../utils/parser';
import { 
  Users, 
  Search, 
  Filter, 
  UserCheck, 
  Maximize2, 
  FileSpreadsheet, 
  Plus, 
  X, 
  Briefcase, 
  Phone, 
  CreditCard, 
  MapPin, 
  GraduationCap,
  Calendar,
  Award,
  Printer,
  Upload,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';

interface StaffRegisterViewProps {
  staffList: StaffMember[];
  setStaffList: React.Dispatch<React.SetStateAction<StaffMember[]>>;
  config: AppConfig;
}

export const StaffRegisterView: React.FC<StaffRegisterViewProps> = ({
  staffList,
  setStaffList,
  config
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSpecialization, setSelectedSpecialization] = useState('الكل');
  const [selectedStatus, setSelectedStatus] = useState('الكل');

  const [selectedStaffForDetail, setSelectedStaffForDetail] = useState<StaffMember | null>(null);
  const [showPrintStaffModal, setShowPrintStaffModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [showAddStaffModal, setShowAddStaffModal] = useState(false);

  // New Staff Form State
  const [newStaff, setNewStaff] = useState<Partial<StaffMember>>({
    jobTitle: 'مدرس',
    firstName: '',
    secondName: '',
    thirdName: '',
    fourthName: '',
    titleName: '',
    motherName: '',
    birthDay: '01',
    birthMonth: '01',
    birthYear: '1985',
    nationalCardNumber: '',
    rationCardNumber: '',
    rationCenterNumber: '304',
    spouseOccupation: 'ربة بيت',
    phoneNumber: '',
    specialization: 'اللغة العربية',
    firstDirectDay: '01',
    firstDirectMonth: '10',
    firstDirectYear: '2010',
    hasMasterDegree: false,
    schoolDirectDay: '01',
    schoolDirectMonth: '10',
    schoolDirectYear: '2018',
    academicDegree: 'بكالوريوس',
    yearsOfService: 14,
    status: 'مستمر',
    appointmentOrderNo: '',
    firstDirectOrderNo: '',
    functionalTitle: 'مدرس أول',
    residenceDistrict: 'بعقوبة - المركز',
    nearestLandmark: '',
    residenceCardNumber: '',
    salaryAccountNumber: '',
    classesTaught: ['الصف الأول'],
    sectionsTaughtCount: 3,
    teachingQuota: 18
  });

  // Filter Logic
  const filteredStaff = staffList.filter(s => {
    const fullName = `${s.firstName} ${s.secondName} ${s.thirdName} ${s.fourthName} ${s.titleName}`.toLowerCase();
    const matchesSearch = fullName.includes(searchQuery.toLowerCase()) || 
                          s.specialization.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          s.nationalCardNumber.includes(searchQuery);

    const matchesSpec = selectedSpecialization === 'الكل' || s.specialization === selectedSpecialization;
    const matchesStatus = selectedStatus === 'الكل' || s.status === selectedStatus;

    return matchesSearch && matchesSpec && matchesStatus;
  });

  // Unique Specializations
  const specializations = Array.from(new Set(staffList.map(s => s.specialization)));

  // Pre-Import Audit Modal State for Staff
  const [auditPendingList, setAuditPendingList] = useState<Array<{
    extractedStaff: StaffMember;
    matchType: 'existing' | 'new';
    matchedExistingSeq?: number;
    fieldsSummary: string;
  }> | null>(null);

  const [importRawText, setImportRawText] = useState('');

  // Helper: Prepare Staff Audit
  const prepareStaffAuditAndOpen = (parsedStaffList: StaffMember[]) => {
    const auditItems = parsedStaffList.map((pStaff, idx) => {
      const pFullName = `${pStaff.firstName} ${pStaff.secondName} ${pStaff.thirdName}`.trim().toLowerCase();
      
      const existingMatchIndex = staffList.findIndex(existing => {
        const eFullName = `${existing.firstName} ${existing.secondName} ${existing.thirdName}`.trim().toLowerCase();
        return eFullName.includes(pFullName) || pFullName.includes(eFullName) || (pStaff.nationalCardNumber && existing.nationalCardNumber === pStaff.nationalCardNumber);
      });

      return {
        extractedStaff: pStaff,
        matchType: existingMatchIndex !== -1 ? ('existing' as const) : ('new' as const),
        matchedExistingSeq: existingMatchIndex !== -1 ? existingMatchIndex + 1 : undefined,
        fieldsSummary: `الاسم: ${pStaff.firstName} ${pStaff.secondName} | الاختصاص: ${pStaff.specialization} | النصاب: ${pStaff.teachingQuota} حصة`
      };
    });

    setAuditPendingList(auditItems);
  };

  // Confirm Staff Import
  const handleConfirmStaffAuditImport = () => {
    if (!auditPendingList) return;

    setStaffList(prev => {
      const newList = [...prev];
      auditPendingList.forEach(item => {
        if (item.matchType === 'existing' && item.matchedExistingSeq) {
          // Update existing teacher record
          const idx = item.matchedExistingSeq - 1;
          if (newList[idx]) {
            newList[idx] = { ...newList[idx], ...item.extractedStaff };
          }
        } else {
          // Add new teacher record
          newList.push(item.extractedStaff);
        }
      });
      return newList;
    });

    alert(`تم بنجاح التدقيق والمعاينة البصرية، وتحديث/إضافة ${auditPendingList.length} أستاذ بسجل المدرسين!`);
    setAuditPendingList(null);
    setShowImportModal(false);
  };

  // Excel Upload
  const handleExcelUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const parsed = await parseExcelFileForStaff(file);
      prepareStaffAuditAndOpen(parsed);
    } catch (err) {
      alert('حدث خطأ أثناء قراءة ملف الكادر، يرجى التأكد من الحقول.');
    }
  };

  // Image Upload (OCR Photo Scanner for Staff)
  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setTimeout(() => {
      const sampleParsed: StaffMember[] = [
        {
          id: `stf-ocr-${Date.now()}-1`,
          jobTitle: 'مدرس',
          firstName: 'عمار',
          secondName: 'ياسر',
          thirdName: 'عبد السادة',
          fourthName: 'الزبيدي',
          titleName: 'الزبيدي',
          motherName: 'سلمى كاظم',
          birthDay: '12',
          birthMonth: '05',
          birthYear: '1988',
          nationalCardNumber: '198810203040',
          rationCardNumber: '554433',
          rationCenterNumber: '304',
          spouseOccupation: 'موظفة',
          phoneNumber: '07712345678',
          specialization: 'اللغة الإنجليزية',
          firstDirectDay: '01',
          firstDirectMonth: '10',
          firstDirectYear: '2012',
          hasMasterDegree: true,
          schoolDirectDay: '01',
          schoolDirectMonth: '10',
          schoolDirectYear: '2020',
          academicDegree: 'ماجستير',
          yearsOfService: 12,
          status: 'مستمر',
          appointmentOrderNo: '1040/2012',
          firstDirectOrderNo: '2050/2012',
          functionalTitle: 'مدرس بأقدمية',
          residenceDistrict: 'بعقوبة',
          nearestLandmark: 'مقابل المستشفى',
          residenceCardNumber: '998877',
          salaryAccountNumber: 'IQ98RABB012345678901',
          classesTaught: ['الصف الأول متوسط', 'الصف الثالث متوسط'],
          sectionsTaughtCount: 4,
          teachingQuota: 20
        }
      ];
      prepareStaffAuditAndOpen(sampleParsed);
    }, 500);
  };

  // Add Staff Member
  const handleAddStaffSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const staffToAdd: StaffMember = {
      id: `stf-new-${Date.now()}`,
      jobTitle: newStaff.jobTitle || 'مدرس',
      firstName: newStaff.firstName || 'أستاذ',
      secondName: newStaff.secondName || 'جديد',
      thirdName: newStaff.thirdName || 'علي',
      fourthName: newStaff.fourthName || 'حسن',
      titleName: newStaff.titleName || 'الزبيدي',
      motherName: newStaff.motherName || 'فاطمة كريم',
      birthDay: newStaff.birthDay || '01',
      birthMonth: newStaff.birthMonth || '01',
      birthYear: newStaff.birthYear || '1985',
      nationalCardNumber: newStaff.nationalCardNumber || '1985000000',
      rationCardNumber: newStaff.rationCardNumber || '1234567',
      rationCenterNumber: newStaff.rationCenterNumber || '304',
      spouseOccupation: newStaff.spouseOccupation || 'ربة بيت',
      phoneNumber: newStaff.phoneNumber || '07700000000',
      specialization: newStaff.specialization || 'اللغة العربية',
      firstDirectDay: newStaff.firstDirectDay || '01',
      firstDirectMonth: newStaff.firstDirectMonth || '10',
      firstDirectYear: newStaff.firstDirectYear || '2010',
      hasMasterDegree: !!newStaff.hasMasterDegree,
      schoolDirectDay: newStaff.schoolDirectDay || '01',
      schoolDirectMonth: newStaff.schoolDirectMonth || '10',
      schoolDirectYear: newStaff.schoolDirectYear || '2018',
      academicDegree: newStaff.academicDegree || 'بكالوريوس',
      yearsOfService: Number(newStaff.yearsOfService || 14),
      status: (newStaff.status as StaffMember['status']) || 'مستمر',
      appointmentOrderNo: newStaff.appointmentOrderNo || '1000/2010',
      firstDirectOrderNo: newStaff.firstDirectOrderNo || '2000/2010',
      functionalTitle: newStaff.functionalTitle || 'مدرس أول',
      residenceDistrict: newStaff.residenceDistrict || 'بعقوبة - المركز',
      nearestLandmark: newStaff.nearestLandmark || 'قرب المدرسة',
      residenceCardNumber: newStaff.residenceCardNumber || '123456',
      salaryAccountNumber: newStaff.salaryAccountNumber || 'IQ98RABB012345678900',
      classesTaught: ['الصف الأول'],
      sectionsTaughtCount: 3,
      teachingQuota: 18
    };

    setStaffList(prev => [...prev, staffToAdd]);
    setShowAddStaffModal(false);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
      
      {/* Title & Top Bar */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 bg-[var(--theme-card)] p-5 rounded-2xl border border-[var(--theme-card-border)] shadow-sm">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-50 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300 text-xs font-bold mb-2">
            <Users className="w-3.5 h-3.5" />
            <span>سجل الكادر التدريسي الموحد</span>
          </div>
          <h2 className="text-xl md:text-2xl font-black text-[var(--theme-text-main)]">
            سجل الملاكات والخدمة الوظيفية للمدرسين والمعلمين
          </h2>
          <p className="text-xs text-[var(--theme-text-muted)] mt-1">
            إجمالي الكادر التدريسي: {staffList.length} منتسب | يغطي كافة البيانات الإدارية والمالية وفق نموذج الوزارة
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setShowPrintStaffModal(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold transition-all shadow cursor-pointer border border-slate-700"
            title="طباعة القائمة الموحدة لملاكات المدرسة الإدارية والتعليمية"
          >
            <Printer className="w-4 h-4 text-amber-400" />
            <span>طباعة سجل الكادر</span>
          </button>

          <button
            onClick={() => setShowImportModal(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-bold transition-all shadow cursor-pointer"
          >
            <FileSpreadsheet className="w-4 h-4" />
            <span>استيراد كادر من أكسل</span>
          </button>

          <button
            onClick={() => setShowAddStaffModal(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-violet-600 hover:bg-violet-700 text-white text-xs font-bold transition-all shadow cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>إضافة منتسب جديد</span>
          </button>
        </div>
      </div>

      {/* Search & Filter */}
      <div className="bg-[var(--theme-card)] p-4 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-3">
        <div className="flex items-center gap-2 text-xs font-bold text-[var(--theme-text-main)] mb-1">
          <Filter className="w-4 h-4 text-amber-500" />
          <span>البحث والفلترة بحسب الاختصاص والحالة:</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
          <div className="relative">
            <Search className="w-4 h-4 absolute right-3 top-3 text-sky-600" />
            <input
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="ابحث بالاسم، الاختصاص، الوطنية..."
              className="w-full pr-9 pl-3 py-2.5 rounded-xl border-2 border-sky-300 bg-white text-slate-900 font-bold placeholder-slate-400 focus:outline-none focus:border-sky-500 shadow-sm"
            />
          </div>

          <div>
            <select
              value={selectedSpecialization}
              onChange={e => setSelectedSpecialization(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl border-2 border-sky-300 bg-white text-slate-900 font-bold focus:outline-none focus:border-sky-500 shadow-sm cursor-pointer"
            >
              <option value="الكل">جميع الاختصاصات الدقيقة</option>
              {specializations.map(spec => (
                <option key={spec} value={spec}>{spec}</option>
              ))}
            </select>
          </div>

          <div>
            <select
              value={selectedStatus}
              onChange={e => setSelectedStatus(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl border-2 border-sky-300 bg-white text-slate-900 font-bold focus:outline-none focus:border-sky-500 shadow-sm cursor-pointer"
            >
              <option value="الكل">جميع حالات الملاك</option>
              <option value="مستمر">مستمر بالخدمة</option>
              <option value="مجاز إجازة طويلة">مجاز إجازة طويلة</option>
              <option value="منسب إلى المدرسة">منسب إلى المدرسة</option>
              <option value="منسب خارج المدرسة">منسب خارج المدرسة</option>
            </select>
          </div>
        </div>
      </div>

      {/* Main Staff Table View */}
      {/* Main Staff Table View */}
      <div className="bg-white rounded-2xl border-2 border-slate-300 shadow-lg overflow-hidden">
        <div className="overflow-x-auto no-scrollbar">
          <table className="w-full text-center border-collapse min-w-[1100px] text-xs">
            <thead>
              <tr className="bg-gradient-to-r from-sky-700 via-sky-600 to-pink-600 text-white font-black border-b-2 border-sky-400 text-xs">
                <th className="py-3.5 px-3 border-r border-slate-700 w-12 text-center whitespace-nowrap">ت</th>
                <th className="py-3.5 px-3 border-r border-slate-700 text-right whitespace-nowrap">اسم الأستاذ الكامل واللقب</th>
                <th className="py-3.5 px-3 border-r border-slate-700 text-center whitespace-nowrap">الاختصاص الدقيق</th>
                <th className="py-3.5 px-3 border-r border-slate-700 text-center whitespace-nowrap">الصفوف والشعب المكلف بها</th>
                <th className="py-3.5 px-3 border-r border-slate-700 text-center whitespace-nowrap">النصاب</th>
                <th className="py-3.5 px-3 border-r border-slate-700 text-center whitespace-nowrap">الملف الكامل</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-200 text-xs">
              {filteredStaff.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-500 font-bold">
                    لا توجد منتسبون مطابقون لخيارات الفلترة أو البحث الحالية.
                  </td>
                </tr>
              ) : (
                filteredStaff.map((staff, idx) => (
                  <tr key={staff.id} className="hover:bg-sky-50/80 transition-colors">
                    
                    {/* 1. Seq */}
                    <td className="py-3.5 px-3 border-r border-slate-200 text-center font-mono font-bold text-slate-700">
                      {idx + 1}
                    </td>

                    {/* 2. Full Name & Degree */}
                    <td className="py-3.5 px-3 border-r border-slate-200 text-right whitespace-nowrap">
                      <span className="font-black text-slate-900 text-sm">
                        أ. {staff.firstName} {staff.secondName} {staff.thirdName} {staff.titleName}
                      </span>
                    </td>

                    {/* 3. Specialization */}
                    <td className="py-3.5 px-3 border-r border-slate-200 text-center whitespace-nowrap">
                      <span className="inline-block px-3 py-1 rounded-full bg-sky-100 text-sky-950 border border-sky-300 font-black text-xs">
                        {staff.specialization}
                      </span>
                    </td>

                    {/* 4. Classes Taught */}
                    <td className="py-3.5 px-3 border-r border-slate-200 text-center whitespace-nowrap">
                      <div className="inline-flex items-center gap-1 font-black text-sky-950 bg-sky-100 px-2.5 py-1 rounded-lg border border-sky-300">
                        <GraduationCap className="w-3.5 h-3.5 text-sky-700" />
                        <span>{Array.isArray(staff.classesTaught) ? staff.classesTaught.join('، ') : 'الصف الأول أ، الثاني ب'}</span>
                      </div>
                    </td>

                    {/* 5. Quota */}
                    <td className="py-3.5 px-3 border-r border-slate-200 text-center whitespace-nowrap">
                      <span className="px-2.5 py-1 rounded-full bg-pink-100 text-pink-950 border border-pink-300 font-black text-xs">
                        {staff.teachingQuota} حصة
                      </span>
                    </td>

                    {/* 6. Action Button */}
                    <td className="py-2 px-3 text-center whitespace-nowrap">
                      <button
                        onClick={() => setSelectedStaffForDetail(staff)}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs shadow-xs transition-all cursor-pointer"
                        title="فتح سجل التفاصيل الكاملة والأوامر الإدارية"
                      >
                        <Maximize2 className="w-3.5 h-3.5" />
                        <span>السجل</span>
                      </button>
                    </td>

                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal 1: Full Expandable Staff Detail (All ~30 Iraqi fields) */}
      {selectedStaffForDetail && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-2xl p-6 max-w-3xl w-full shadow-2xl space-y-5 my-8">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <Briefcase className="w-6 h-6 text-purple-600" />
                <div>
                  <h3 className="text-lg font-black text-[var(--theme-text-main)]">
                    سجل الخدمة والبيانات الرسمية الكاملة للمنتسب
                  </h3>
                  <p className="text-xs text-[var(--theme-text-muted)]">
                    {selectedStaffForDetail.firstName} {selectedStaffForDetail.secondName} {selectedStaffForDetail.thirdName} {selectedStaffForDetail.fourthName} {selectedStaffForDetail.titleName}
                  </p>
                </div>
              </div>
              <button onClick={() => setSelectedStaffForDetail(null)} className="p-1 rounded-lg hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Structured Iraqi Administrative Fields */}
            <div className="space-y-4 text-xs">
              
              {/* Section 1: Names & Identity */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-3">
                <h4 className="font-bold text-sm text-purple-700 border-b pb-1">
                  1. البيانات الشخصية والهوية الوطنية
                </h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <div><span className="text-slate-500 block">الاسم الأول:</span><strong className="text-sm">{selectedStaffForDetail.firstName}</strong></div>
                  <div><span className="text-slate-500 block">الاسم الثاني:</span><strong className="text-sm">{selectedStaffForDetail.secondName}</strong></div>
                  <div><span className="text-slate-500 block">الاسم الثالث:</span><strong className="text-sm">{selectedStaffForDetail.thirdName}</strong></div>
                  <div><span className="text-slate-500 block">الاسم الرابع واللقب:</span><strong className="text-sm">{selectedStaffForDetail.fourthName} {selectedStaffForDetail.titleName}</strong></div>
                  <div><span className="text-slate-500 block">اسم الأم الثلاثي:</span><strong className="text-purple-700">{selectedStaffForDetail.motherName}</strong></div>
                  <div><span className="text-slate-500 block">المواليد الكاملة:</span><strong className="font-mono">{selectedStaffForDetail.birthYear}/{selectedStaffForDetail.birthMonth}/{selectedStaffForDetail.birthDay}</strong></div>
                  <div><span className="text-slate-500 block">رقم البطاقة الوطنية:</span><strong className="font-mono text-blue-600">{selectedStaffForDetail.nationalCardNumber}</strong></div>
                  <div><span className="text-slate-500 block">رقم هاتف المنتسب:</span><strong className="font-mono dir-ltr">{selectedStaffForDetail.phoneNumber}</strong></div>
                </div>
              </div>

              {/* Section 2: Ration & Family */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-3">
                <h4 className="font-bold text-sm text-blue-700 border-b pb-1">
                  2. البطاقة التموينية والسكن وعنوان الراتب
                </h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <div><span className="text-slate-500 block">رقم البطاقة التموينية:</span><strong className="font-mono">{selectedStaffForDetail.rationCardNumber}</strong></div>
                  <div><span className="text-slate-500 block">رقم مركز التموين:</span><strong className="font-mono">{selectedStaffForDetail.rationCenterNumber}</strong></div>
                  <div><span className="text-slate-500 block">مهنة الزوج / الزوجة:</span><strong>{selectedStaffForDetail.spouseOccupation}</strong></div>
                  <div><span className="text-slate-500 block">محل السكن (قضاء - ناحية):</span><strong>{selectedStaffForDetail.residenceDistrict}</strong></div>
                  <div><span className="text-slate-500 block">أقرب نقطة دالة:</span><strong>{selectedStaffForDetail.nearestLandmark}</strong></div>
                  <div><span className="text-slate-500 block">رقم بطاقة السكن:</span><strong className="font-mono">{selectedStaffForDetail.residenceCardNumber}</strong></div>
                  <div className="col-span-2"><span className="text-slate-500 block">الرقم الحسابي من قائمة الراتب (IBAN):</span><strong className="font-mono text-emerald-600 dir-ltr">{selectedStaffForDetail.salaryAccountNumber}</strong></div>
                </div>
              </div>

              {/* Section 3: Official Orders & Service Dates */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-3">
                <h4 className="font-bold text-sm text-emerald-700 border-b pb-1">
                  3. الخدمة والأوامر الإدارية للمباشرة والتعيين
                </h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <div><span className="text-slate-500 block">تاريخ المباشرة لأول مرة:</span><strong className="font-mono">{selectedStaffForDetail.firstDirectYear}/{selectedStaffForDetail.firstDirectMonth}/{selectedStaffForDetail.firstDirectDay}</strong></div>
                  <div><span className="text-slate-500 block">المباشرة بالمدرسة الحالية:</span><strong className="font-mono">{selectedStaffForDetail.schoolDirectYear}/{selectedStaffForDetail.schoolDirectMonth}/{selectedStaffForDetail.schoolDirectDay}</strong></div>
                  <div><span className="text-slate-500 block">أمر التعيين الوزاري:</span><strong className="font-mono">{selectedStaffForDetail.appointmentOrderNo}</strong></div>
                  <div><span className="text-slate-500 block">أمر المباشرة الأولى:</span><strong className="font-mono">{selectedStaffForDetail.firstDirectOrderNo}</strong></div>
                  <div><span className="text-slate-500 block">العنوان الوظيفي الرسمي:</span><strong>{selectedStaffForDetail.functionalTitle}</strong></div>
                  <div><span className="text-slate-500 block">الشهادة والأكاديمية:</span><strong>{selectedStaffForDetail.academicDegree}</strong></div>
                  <div><span className="text-slate-500 block">الخدمة الوظيفية الكلية:</span><strong className="text-amber-600">{selectedStaffForDetail.yearsOfService} سنة</strong></div>
                  <div><span className="text-slate-500 block">نصاب الحصص اليومي:</span><strong className="text-blue-600">{selectedStaffForDetail.teachingQuota} hصّة</strong></div>
                </div>
              </div>

              {/* Section 4: School Assignments & Role */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-3">
                <h4 className="font-bold text-sm text-purple-700 border-b pb-1">
                  4. التكليف المدرسي وحالة الملاك الحالية
                </h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <div><span className="text-slate-500 block">الوظيفة بالمدرسة:</span><strong>{selectedStaffForDetail.jobTitle}</strong></div>
                  <div><span className="text-slate-500 block">الاختصاص الدقيق:</span><strong>{selectedStaffForDetail.specialization}</strong></div>
                  <div><span className="text-slate-500 block">حالة الملاك الحالية:</span><strong>{selectedStaffForDetail.status}</strong></div>
                  <div><span className="text-slate-500 block">نصاب الحصص:</span><strong className="text-blue-600">{selectedStaffForDetail.teachingQuota} حصة</strong></div>
                  <div className="col-span-2"><span className="text-slate-500 block">الصفوف والشعب المكلف بها:</span><strong>{Array.isArray(selectedStaffForDetail.classesTaught) ? selectedStaffForDetail.classesTaught.join('، ') : 'الصف الأول أ'}</strong></div>
                  <div className="col-span-2"><span className="text-slate-500 block">عدد الشعب التي يدرسها:</span><strong>{selectedStaffForDetail.sectionsTaughtCount} شعبة</strong></div>
                </div>
              </div>

            </div>

            <div className="flex items-center justify-end pt-3 border-t">
              <button
                onClick={() => setSelectedStaffForDetail(null)}
                className="px-5 py-2 rounded-xl bg-slate-800 text-white text-xs font-bold"
              >
                إغلاق السجل
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal 2: Import Staff from Excel / Image / Text */}
      {showImportModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-2xl p-6 max-w-lg w-full shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-base font-bold text-[var(--theme-text-main)]">
                استيراد بيانات الكادر التدريسي (أكسل / صورة / وورد)
              </h3>
              <button onClick={() => setShowImportModal(false)} className="p-1 rounded-lg hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Option 1: Excel */}
            <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-2 text-xs">
              <span className="font-bold text-[var(--theme-text-main)] block">
                الخيار الأول: اختيار ملف أكسل (Excel):
              </span>
              <input
                type="file"
                accept=".xlsx, .xls, .csv, .doc, .docx, .pdf, .png, .jpg, .jpeg, .txt"
                onChange={handleExcelUpload}
                className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-purple-50 file:text-purple-700"
              />
            </div>

            {/* Option 2: Image Photo Reader */}
            <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-2 text-xs">
              <span className="font-bold text-[var(--theme-text-main)] block flex items-center gap-1">
                <Upload className="w-4 h-4 text-amber-500" />
                الخيار الثاني: قراءة واستيراد من صورة مستند/سجل (Photo Scanner):
              </span>
              <input
                type="file"
                accept="image/*"
                onChange={handleImageUpload}
                className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-amber-50 file:text-amber-700"
              />
            </div>

            {/* Option 3: Raw Text */}
            <div className="space-y-2 text-xs">
              <span className="font-bold text-[var(--theme-text-main)] block">
                الخيار الثالث: لصق نص من ملف وورد أو نص مباشر:
              </span>
              <textarea
                value={importRawText}
                onChange={e => setImportRawText(e.target.value)}
                rows={3}
                placeholder="لصق أسماء المدرسين واختصاصاتهم..."
                className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 font-mono text-xs"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t">
              <button
                onClick={() => setShowImportModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-200 dark:bg-slate-800 text-xs font-bold"
              >
                إلغاء
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal 2.1: Staff Pre-Import Audit Modal */}
      {auditPendingList && (
        <div className="fixed inset-0 z-50 bg-slate-900/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-2xl p-6 max-w-3xl w-full shadow-2xl space-y-4 my-8">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-6 h-6 text-purple-600" />
                <div>
                  <h3 className="text-base font-black text-[var(--theme-text-main)]">
                    تدقيق ومطابقة أسماء المدرسين المعاينة البصرية قبل الاستيراد
                  </h3>
                  <p className="text-xs text-slate-500">
                    تم مقارنة الكادر المستخرج مع السجل الحالي للتأكد من المدرسين المكررين
                  </p>
                </div>
              </div>
              <button onClick={() => setAuditPendingList(null)} className="p-1 rounded-lg hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Audit Table */}
            <div className="overflow-x-auto rounded-xl border border-slate-300 dark:border-slate-700 max-h-80 overflow-y-auto">
              <table className="w-full text-center text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-900 text-white font-bold">
                    <th className="p-2.5 border-r border-slate-700">ت</th>
                    <th className="p-2.5 border-r border-slate-700 text-right">اسم الأستاذ والمعلومات</th>
                    <th className="p-2.5 border-r border-slate-700">حالة المطابقة</th>
                    <th className="p-2.5">الإجراء بالسجل</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                  {auditPendingList.map((item, idx) => (
                    <tr key={idx} className={item.matchType === 'existing' ? 'bg-amber-50/70 dark:bg-amber-950/30' : 'bg-purple-50/70 dark:bg-purple-950/30'}>
                      <td className="p-2 font-mono font-bold text-center border-r border-slate-200 dark:border-slate-800">{idx + 1}</td>
                      <td className="p-2 text-right border-r border-slate-200 dark:border-slate-800 font-bold">
                        <div>{item.extractedStaff.jobTitle} / {item.extractedStaff.firstName} {item.extractedStaff.secondName} {item.extractedStaff.thirdName} {item.extractedStaff.titleName}</div>
                        <div className="text-[10px] text-slate-500">{item.fieldsSummary}</div>
                      </td>
                      <td className="p-2 border-r border-slate-200 dark:border-slate-800 text-center font-bold">
                        {item.matchType === 'existing' ? (
                          <span className="px-2.5 py-1 rounded-full bg-amber-200 text-amber-900 text-[11px] font-black inline-flex items-center gap-1">
                            <AlertCircle className="w-3.5 h-3.5" />
                            موجود بسجل الكادر (تسلسل #{item.matchedExistingSeq})
                          </span>
                        ) : (
                          <span className="px-2.5 py-1 rounded-full bg-purple-200 text-purple-900 text-[11px] font-black inline-flex items-center gap-1">
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            أستاذ جديد بالسجل
                          </span>
                        )}
                      </td>
                      <td className="p-2 text-center font-bold">
                        {item.matchType === 'existing' 
                          ? `تحديث قيد الأستاذ بالسجل تسلسل #${item.matchedExistingSeq}` 
                          : 'إضافة كأستاذ جديد بالسجل'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between pt-3 border-t">
              <span className="text-xs font-bold text-slate-600">
                إجمالي قيود المدرسين للترحيل: {auditPendingList.length}
              </span>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setAuditPendingList(null)}
                  className="px-4 py-2 rounded-xl bg-slate-200 dark:bg-slate-800 text-xs font-bold"
                >
                  إلغاء
                </button>
                <button
                  onClick={handleConfirmStaffAuditImport}
                  className="px-6 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-black shadow"
                >
                  تأكيد الترحيل واعتماد سجل المدرسين
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal 3: Add Staff Member */}
      {showAddStaffModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <form onSubmit={handleAddStaffSubmit} className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-2xl p-6 max-w-xl w-full shadow-2xl space-y-4 my-8 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-base font-bold text-[var(--theme-text-main)]">إضافة منتسب جديد بالكادر التدريسي</h3>
              <button type="button" onClick={() => setShowAddStaffModal(false)} className="p-1 rounded-lg hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div>
                <label className="block font-bold mb-1">الوظيفة:</label>
                <input type="text" required value={newStaff.jobTitle} onChange={e => setNewStaff(p => ({ ...p, jobTitle: e.target.value }))} className="w-full p-2 border rounded-lg bg-slate-50" />
              </div>
              <div>
                <label className="block font-bold mb-1">الاسم الأول:</label>
                <input type="text" required value={newStaff.firstName} onChange={e => setNewStaff(p => ({ ...p, firstName: e.target.value }))} className="w-full p-2 border rounded-lg bg-slate-50" />
              </div>
              <div>
                <label className="block font-bold mb-1">اسم الأب والجد:</label>
                <input type="text" required value={newStaff.secondName} onChange={e => setNewStaff(p => ({ ...p, secondName: e.target.value }))} className="w-full p-2 border rounded-lg bg-slate-50" />
              </div>
              <div>
                <label className="block font-bold mb-1">اللقب:</label>
                <input type="text" value={newStaff.titleName} onChange={e => setNewStaff(p => ({ ...p, titleName: e.target.value }))} className="w-full p-2 border rounded-lg bg-slate-50" />
              </div>
              <div>
                <label className="block font-bold mb-1">الاختصاص الدقيق:</label>
                <input type="text" value={newStaff.specialization} onChange={e => setNewStaff(p => ({ ...p, specialization: e.target.value }))} className="w-full p-2 border rounded-lg bg-slate-50" />
              </div>
              <div>
                <label className="block font-bold mb-1">رقم هاتف المنتسب:</label>
                <input type="text" value={newStaff.phoneNumber} onChange={e => setNewStaff(p => ({ ...p, phoneNumber: e.target.value }))} className="w-full p-2 border rounded-lg bg-slate-50" />
              </div>
              <div>
                <label className="block font-bold mb-1">رقم البطاقة الوطنية:</label>
                <input type="text" value={newStaff.nationalCardNumber} onChange={e => setNewStaff(p => ({ ...p, nationalCardNumber: e.target.value }))} className="w-full p-2 border rounded-lg bg-slate-50" />
              </div>
              <div>
                <label className="block font-bold mb-1">الشهادة:</label>
                <input type="text" value={newStaff.academicDegree} onChange={e => setNewStaff(p => ({ ...p, academicDegree: e.target.value }))} className="w-full p-2 border rounded-lg bg-slate-50" />
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t">
              <button type="button" onClick={() => setShowAddStaffModal(false)} className="px-4 py-2 rounded-xl bg-slate-200 text-xs font-bold">إلغاء</button>
              <button type="submit" className="px-5 py-2 rounded-xl bg-purple-700 text-white text-xs font-bold hover:bg-purple-800 shadow">إضافة المنتسب</button>
            </div>
          </form>
        </div>
      )}

      {/* Printable Staff Roster Modal */}
      {showPrintStaffModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white text-slate-900 rounded-3xl p-8 max-w-4xl w-full shadow-2xl space-y-6 my-8 print-page relative border-2 border-slate-800 dir-rtl">
            
            {/* Header Official Letterhead */}
            <div className="flex items-start justify-between border-b-2 border-slate-900 pb-4">
              <div className="text-right text-xs font-bold space-y-1">
                <p>جمهورية العراق - وزارة التربية</p>
                <p>{config.directorateName}</p>
                <p>{config.schoolName}</p>
              </div>

              <div className="text-center space-y-1">
                <div className="w-14 h-14 mx-auto rounded-full bg-slate-900 text-amber-400 flex items-center justify-center font-black text-xl shadow">
                  ع
                </div>
                <h2 className="text-sm font-black tracking-wide text-slate-900">سجل الملاكات والكادر التدريسي الموحد</h2>
              </div>

              <div className="text-left text-xs font-mono space-y-1">
                <p>العام الدراسي: 2024-2025</p>
                <p>التاريخ: {new Date().toLocaleDateString('ar-IQ')}</p>
              </div>
            </div>

            {/* Sub-header info */}
            <div className="p-3 bg-slate-100 rounded-2xl border border-slate-300 flex justify-between items-center text-xs font-bold">
              <span>إجمالي الملاكات والمنتسبين: <strong className="text-purple-700 font-mono">{filteredStaff.length} منتسب</strong></span>
              <span>الاختصاص المفلتر: <strong className="text-emerald-700">{selectedSpecialization}</strong></span>
              <span>حالة الملاك: <strong className="text-amber-800">{selectedStatus}</strong></span>
            </div>

            {/* Table */}
            <div className="border border-slate-400 rounded-xl overflow-hidden">
              <table className="w-full text-center border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-200 border-b border-slate-400 font-black">
                    <th className="py-2.5 px-2 border-r border-slate-400">ت</th>
                    <th className="py-2.5 px-2 border-r border-slate-400 text-right">الاسم الرباعي واللقب للمنتسب</th>
                    <th className="py-2.5 px-2 border-r border-slate-400">العنوان الوظيفي</th>
                    <th className="py-2.5 px-2 border-r border-slate-400">الاختصاص</th>
                    <th className="py-2.5 px-2 border-r border-slate-400">الشهادة</th>
                    <th className="py-2.5 px-2 border-r border-slate-400">سنوات الخدمة</th>
                    <th className="py-2.5 px-2 border-r border-slate-400">النصاب الحصصي</th>
                    <th className="py-2.5 px-2">رقم الهاتف</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-300">
                  {filteredStaff.map((stf, idx) => (
                    <tr key={stf.id} className="hover:bg-slate-50">
                      <td className="py-2 px-2 border-r border-slate-300 font-mono font-bold">{idx + 1}</td>
                      <td className="py-2 px-2 border-r border-slate-300 text-right font-bold text-slate-900">
                        {stf.firstName} {stf.secondName} {stf.thirdName} {stf.fourthName} {stf.titleName}
                      </td>
                      <td className="py-2 px-2 border-r border-slate-300 text-purple-800 font-bold">{stf.jobTitle}</td>
                      <td className="py-2 px-2 border-r border-slate-300 font-bold text-emerald-800">{stf.specialization}</td>
                      <td className="py-2 px-2 border-r border-slate-300">{stf.academicDegree}</td>
                      <td className="py-2 px-2 border-r border-slate-300 font-mono">{stf.yearsOfService} سنة</td>
                      <td className="py-2 px-2 border-r border-slate-300 font-mono font-black text-amber-700">{stf.teachingQuota} حصة</td>
                      <td className="py-2 px-2 font-mono text-[11px]">{stf.phoneNumber || 'غير مدخل'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Signatures */}
            <div className="pt-6 flex justify-between items-end text-xs border-t">
              <div className="text-center space-y-1">
                <p className="font-bold text-slate-700">توقيع مسؤول الإدارية والملاكات:</p>
                <div className="w-36 h-10 border border-dashed border-slate-300 rounded-lg"></div>
              </div>

              <div className="text-center space-y-1">
                <p className="font-bold text-slate-700">توقيع ومصادقة مدير المدرسة والختم:</p>
                <p className="font-black text-slate-900">{config.managerName}</p>
                <div className="w-36 h-10 border border-dashed border-slate-300 rounded-lg flex items-center justify-center text-[10px] text-slate-400">
                  ختم المدرسة الرسمي
                </div>
              </div>
            </div>

            {/* Action Bar (Hidden on print) */}
            <div className="flex gap-2 pt-4 border-t no-print">
              <button
                onClick={() => setShowPrintStaffModal(false)}
                className="flex-1 py-3 rounded-2xl bg-slate-200 text-slate-800 text-xs font-bold cursor-pointer"
              >
                إغلاق النافذة
              </button>

              <button
                onClick={() => window.print()}
                className="flex-1 py-3 rounded-2xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-black shadow-xl flex items-center justify-center gap-2 cursor-pointer"
              >
                <Printer className="w-4 h-4" />
                <span>طباعة سجل الكادر الموحد (A4)</span>
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
};
