import React from 'react';

export interface GradeRegisterCoverPageProps {
  registerTitle: string;
  subject?: string;
  grade: string;
  section: string;
  teacherName?: string;
  schoolName?: string;
  directorate?: string;
  academicYear?: string;
}

export const GradeRegisterCoverPage: React.FC<GradeRegisterCoverPageProps> = ({
  registerTitle,
  subject,
  grade,
  section,
  teacherName,
  schoolName = 'ثانوية المتميزين للبنين',
  directorate = 'المديرية العامة للتربية',
  academicYear = '2025 – 2026 م'
}) => {
  const displayTeacher = teacherName || 'مدرس المادة';
  const displayGradeSection = grade && section && section !== 'الكل' 
    ? `${grade} - شعبة (${section})` 
    : grade || 'كافة الصفوف';

  return (
    <div className="print-cover-page bg-[#fafaf9] p-8 rounded-3xl border-4 border-double border-indigo-900 shadow-lg text-center font-arabsq relative overflow-hidden my-4 max-w-4xl mx-auto">
      {/* Decorative Ornate Corners */}
      <span className="absolute top-3 right-4 text-2xl text-amber-600 select-none">❖</span>
      <span className="absolute top-3 left-4 text-2xl text-amber-600 select-none">❖</span>
      <span className="absolute bottom-3 right-4 text-2xl text-amber-600 select-none">❖</span>
      <span className="absolute bottom-3 left-4 text-2xl text-amber-600 select-none">❖</span>

      {/* Top Header */}
      <div className="space-y-1 mb-6">
        <h1 className="text-2xl font-black text-indigo-950 tracking-wide">جمهورية العراق</h1>
        <h2 className="text-sm font-bold text-slate-600">وزارة التربية • {directorate}</h2>
        <h3 className="text-lg font-black text-slate-900 pt-1">{schoolName}</h3>
      </div>

      {/* Ornate Divider */}
      <div className="flex items-center justify-center w-3/4 mx-auto my-4">
        <div className="flex-1 h-0.5 bg-gradient-to-r from-transparent via-amber-600 to-transparent"></div>
        <div className="px-3 text-amber-700 font-bold text-sm select-none">✦ ❖ ✦</div>
        <div className="flex-1 h-0.5 bg-gradient-to-r from-transparent via-amber-600 to-transparent"></div>
      </div>

      {/* Main Register Title Box */}
      <div className="my-6 space-y-2">
        <span className="inline-block bg-indigo-900 text-white text-xs font-black px-4 py-1 rounded-full shadow-sm">
          وثيقة وسجل درجات رسمي معتمد
        </span>
        <h1 className="text-3xl sm:text-4xl font-black text-indigo-950 py-2">
          {registerTitle}
        </h1>
        <p className="text-amber-800 font-bold text-sm">
          للعام الدراسي ( {academicYear} )
        </p>
      </div>

      {/* 3 Key Identification Plaques */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 my-8 w-11/12 mx-auto">
        {/* Subject */}
        {subject && (
          <div className="bg-white p-4 rounded-2xl border-2 border-slate-200 border-t-4 border-t-amber-600 shadow-sm flex flex-col justify-center items-center">
            <span className="text-xs font-bold text-slate-500 mb-1">المادة الدراسية</span>
            <span className="text-xl font-black text-amber-700">{subject}</span>
          </div>
        )}

        {/* Class & Section */}
        <div className="bg-white p-4 rounded-2xl border-2 border-slate-200 border-t-4 border-t-indigo-700 shadow-sm flex flex-col justify-center items-center">
          <span className="text-xs font-bold text-slate-500 mb-1">الصف والشعبة</span>
          <span className="text-base font-black text-slate-900">{displayGradeSection}</span>
        </div>

        {/* Teacher */}
        <div className="bg-white p-4 rounded-2xl border-2 border-slate-200 border-t-4 border-t-emerald-700 shadow-sm flex flex-col justify-center items-center">
          <span className="text-xs font-bold text-slate-500 mb-1">مدرس المادة المحترم</span>
          <span className="text-base font-black text-emerald-950">{displayTeacher}</span>
        </div>
      </div>

      {/* Official Signatures & Seal */}
      <div className="flex items-end justify-between w-11/12 mx-auto pt-8 border-t border-slate-200/80 text-xs font-bold text-slate-700">
        <div className="text-center w-1/3">
          <p className="text-slate-500 mb-1">أستاذ المادة</p>
          <p className="font-black text-slate-900 text-sm mb-4">{displayTeacher}</p>
          <p className="text-slate-400">التوقيع: .....................</p>
        </div>

        <div className="w-20 h-20 rounded-full border-2 border-dashed border-amber-600 flex flex-col items-center justify-center text-amber-800 bg-amber-50/50 shadow-inner">
          <span className="text-[10px] font-black">الختم الرسمي</span>
          <span className="text-[9px]">للمدرسة</span>
        </div>

        <div className="text-center w-1/3">
          <p className="text-slate-500 mb-1">مدير المدرسة</p>
          <p className="font-black text-slate-900 text-sm mb-4">الإدارة المدرسية</p>
          <p className="text-slate-400">التوقيع: .....................</p>
        </div>
      </div>
    </div>
  );
};
