import React, { useState, useMemo, useCallback } from 'react';
import { StaffMember, AppConfig, DayScheduleMap } from '../types';
import { parseExcelFileForStaff } from '../utils/parser';
import { Portal } from './common/Portal';
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
  AlertCircle,
  BookOpen,
  Clock,
  ChevronDown,
  Trash2,
  Edit3,
  Save,
  Sparkles,
  Check
} from 'lucide-react';
import { standardizeSubjectInput, STANDARD_APPROVED_SUBJECTS } from '../utils/subjectHelper';
import { printElement } from '../utils/printHelper';

interface StaffRegisterViewProps {
  staffList: StaffMember[];
  setStaffList: React.Dispatch<React.SetStateAction<StaffMember[]>>;
  config: AppConfig;
  scheduleMap?: DayScheduleMap;
}

export const JOB_TITLE_OPTIONS = [
  'مدرس',
  'معلم',
  'معاون مدير',
  'مدير',
  'مرشد تربوي',
  'أمين مكتبة',
  'كاتب',
  'موظف خدمة',
  'مشرف'
];

import { canonicalSubject, matchStaffWithScheduleCell, MASTER_SUBJECTS_LIST } from '../utils/subjectHelper';
import { getSupabase } from '../utils/supabaseClient';
import { isExemptStaff, parseClassTaught, standardizeGradeName, standardizeSectionName } from '../utils/syncEngine';

export const GRADE_OPTIONS = [
  'الأول',
  'الثاني',
  'الثالث',
  'الرابع',
  'الخامس',
  'السادس'
];

export const SECTION_OPTIONS = ['أ', 'ب', 'ج', 'د', 'هـ', 'و'];

export const ALL_SUBJECTS = [
  'مفرغ إدارياً / إدارة',
  'التربية الإسلامية',
  'اللغة العربية',
  'اللغة الإنكليزية',
  'الرياضيات',
  'الاجتماعيات',
  'الأحياء',
  'الكيمياء',
  'الفيزياء',
  'الحاسوب',
  'النشاط البدني',
  'التربية الفنية',
  'التربية الأخلاقية',
  'العلوم'
];

// Component: Isolated AddStaffModal to prevent parent re-renders while typing
interface AddStaffModalProps {
  onClose: () => void;
  onAdd: (newMember: StaffMember) => void;
}

const AddStaffModal: React.FC<AddStaffModalProps> = ({ onClose, onAdd }) => {
  const [formData, setFormData] = useState({
    jobTitle: 'مدرس',
    status: 'مستمر في الملاك' as StaffMember['status'],
    firstName: '',
    secondName: '',
    thirdName: '',
    titleName: '',
    specialization: 'اللغة العربية',
    classesTaughtList: ['الأول أ'] as string[],
    modalGrade: 'الأول',
    phoneNumber: '',
    nationalCardNumber: '',
    academicDegree: 'بكالوريوس',
    teachingQuota: 18
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.firstName.trim() || !formData.secondName.trim()) {
      alert('يرجى إدخال الاسم الأول واسم الأب والجد على الأقل');
      return;
    }

    const isZero = ['مدير', 'معاون مدير', 'مرشد تربوي', 'أمين مكتبة', 'كاتب', 'موظف خدمة', 'مشرف'].includes(formData.jobTitle) || formData.teachingQuota === 0;
    const finalQuota = isZero ? 0 : formData.teachingQuota;
    const spec = canonicalSubject(formData.specialization || 'اللغة العربية');
    const actualSub = isZero ? 'مفرغ إدارياً / إدارة' : spec;
    const classParts = formData.classesTaughtList;

    const newMember: StaffMember = {
      id: `stf-manual-${Date.now()}`,
      jobTitle: formData.jobTitle,
      firstName: formData.firstName.trim(),
      secondName: formData.secondName.trim(),
      thirdName: formData.thirdName.trim(),
      fourthName: '',
      titleName: formData.titleName.trim(),
      motherName: 'زينب كاظم',
      birthDay: '01',
      birthMonth: '01',
      birthYear: '1985',
      nationalCardNumber: formData.nationalCardNumber.trim() || `${Date.now()}`,
      rationCardNumber: '',
      rationCenterNumber: '304',
      spouseOccupation: 'ربة بيت',
      phoneNumber: formData.phoneNumber.trim(),
      specialization: spec,
      actualSubjectTaught: actualSub,
      firstDirectDay: '01',
      firstDirectMonth: '10',
      firstDirectYear: '2010',
      hasMasterDegree: false,
      schoolDirectDay: '01',
      schoolDirectMonth: '10',
      schoolDirectYear: '2018',
      academicDegree: formData.academicDegree.trim() || 'بكالوريوس',
      yearsOfService: 10,
      status: formData.status,
      appointmentOrderNo: '',
      firstDirectOrderNo: '',
      functionalTitle: 'مدرس أول',
      residenceDistrict: 'بعقوبة - المركز',
      nearestLandmark: '',
      residenceCardNumber: '',
      salaryAccountNumber: '',
      classesTaught: isZero ? [] : (classParts.length > 0 ? classParts : ['الصف الأول']),
      sectionsTaughtCount: isZero ? 0 : 3,
      teachingQuota: finalQuota
    };

    onAdd(newMember);
  };

  return (
    <Portal>
      <div 
        onClick={onClose}
        className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm overflow-y-auto p-4 sm:p-6 flex justify-center items-start"
      >
      <form 
        onClick={e => e.stopPropagation()}
        onSubmit={handleSubmit} 
        className="bg-white border-2 border-purple-400 rounded-3xl p-6 md:p-8 max-w-2xl w-full shadow-2xl space-y-4 my-auto max-h-[90vh] flex flex-col text-slate-900"
      >
        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3 flex-shrink-0">
          <h3 className="text-base font-black text-purple-950 flex items-center gap-2">
            <Plus className="w-5 h-5 text-purple-600" />
            <span>إضافة منتسب جديد بكادر المدرسة</span>
          </h3>
          <button 
            type="button" 
            onClick={onClose} 
            className="p-1 rounded-lg hover:bg-slate-100 text-slate-700 cursor-pointer"
            title="إغلاق"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs overflow-y-auto flex-1 p-1">
          <div>
            <label className="block font-black text-slate-800 mb-1">الوظيفة بالمدرسة:</label>
            <select 
              value={formData.jobTitle || 'مدرس'} 
              onChange={e => {
                const title = e.target.value;
                const isZero = ['مدير', 'معاون مدير', 'مرشد تربوي', 'أمين مكتبة', 'كاتب', 'موظف خدمة', 'مشرف'].includes(title);
                setFormData(p => ({ 
                  ...p, 
                  jobTitle: title,
                  teachingQuota: isZero ? 0 : (p.teachingQuota === 0 ? 18 : p.teachingQuota)
                }));
              }} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold focus:border-purple-600 focus:outline-none cursor-pointer"
            >
              {JOB_TITLE_OPTIONS.map(title => (
                <option key={title} value={title}>{title}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">الموقف / حالة الملاك:</label>
            <select 
              value={formData.status || 'مستمر في الملاك'} 
              onChange={e => setFormData(p => ({ ...p, status: e.target.value as StaffMember['status'] }))} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold focus:border-purple-600 focus:outline-none cursor-pointer"
            >
              <option value="مستمر في الملاك">مستمر في الملاك</option>
              <option value="مجاز إجازة طويلة">مجاز إجازة طويلة</option>
              <option value="منسب إلى المدرسة">منسب إلى المدرسة</option>
              <option value="منسب خارج المدرسة">منسب خارج المدرسة</option>
            </select>
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">الاسم الأول (مطلوب):</label>
            <input 
              type="text" 
              required 
              placeholder="مثال: أحمد"
              value={formData.firstName} 
              onChange={e => setFormData(p => ({ ...p, firstName: e.target.value }))} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold focus:border-purple-600 focus:outline-none" 
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">اسم الأب والجد (مطلوب):</label>
            <input 
              type="text" 
              required 
              placeholder="مثال: محمد علي"
              value={formData.secondName} 
              onChange={e => setFormData(p => ({ ...p, secondName: e.target.value }))} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold focus:border-purple-600 focus:outline-none" 
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">اسم والد الجد (الرابع):</label>
            <input 
              type="text" 
              placeholder="مثال: حسن"
              value={formData.thirdName} 
              onChange={e => setFormData(p => ({ ...p, thirdName: e.target.value }))} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold focus:border-purple-600 focus:outline-none" 
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">اللقب / العشيرة:</label>
            <input 
              type="text" 
              placeholder="مثال: الجبوري"
              value={formData.titleName} 
              onChange={e => setFormData(p => ({ ...p, titleName: e.target.value }))} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold focus:border-purple-600 focus:outline-none" 
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">الاختصاص الأكاديمي / المادة:</label>
            <select 
              value={formData.specialization || 'اللغة العربية'} 
              onChange={e => setFormData(p => ({ ...p, specialization: e.target.value }))} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold focus:border-purple-600 focus:outline-none cursor-pointer"
            >
              {ALL_SUBJECTS.map(subj => (
                <option key={subj} value={subj}>{subj}</option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <label className="block font-black text-slate-800 text-xs">الصفوف والشعب المكلف بها:</label>
            
            {/* Badges List */}
            <div className="flex flex-wrap gap-1 min-h-[36px] p-2 bg-slate-50 border border-slate-300 rounded-xl">
              {formData.classesTaughtList.length === 0 ? (
                <span className="text-xs text-slate-400 font-bold">لم يتم تحديد صفوف أو شعب بعد</span>
              ) : (
                formData.classesTaughtList.map(tag => (
                  <span key={tag} className="inline-flex items-center gap-1 px-2 py-0.5 bg-indigo-100 text-indigo-950 border border-indigo-300 rounded-lg text-xs font-bold shadow-2xs">
                    <span>{tag}</span>
                    <button
                      type="button"
                      onClick={() => setFormData(p => ({ ...p, classesTaughtList: p.classesTaughtList.filter(t => t !== tag) }))}
                      className="text-slate-400 hover:text-rose-600 font-black cursor-pointer"
                    >
                      ×
                    </button>
                  </span>
                ))
              )}
            </div>

            {/* Quick Add Grade + Section Toolbar */}
            <div className="flex items-center gap-2 pt-1">
              <select
                value={formData.modalGrade}
                onChange={e => setFormData(p => ({ ...p, modalGrade: e.target.value }))}
                className="p-1.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold text-xs"
                title="اختر الصف"
              >
                {GRADE_OPTIONS.map(g => (
                  <option key={g} value={g}>الصف {g}</option>
                ))}
              </select>

              <div className="flex items-center gap-1 flex-wrap">
                {SECTION_OPTIONS.map(sec => {
                  const tag = `${formData.modalGrade} ${sec}`;
                  const isSelected = formData.classesTaughtList.includes(tag);
                  return (
                    <button
                      key={sec}
                      type="button"
                      onClick={() => {
                        setFormData(p => ({
                          ...p,
                          classesTaughtList: isSelected
                            ? p.classesTaughtList.filter(t => t !== tag)
                            : [...p.classesTaughtList, tag]
                        }));
                      }}
                      className={`w-7 h-7 rounded-lg font-bold text-xs flex items-center justify-center transition-all cursor-pointer ${
                        isSelected
                          ? 'bg-indigo-700 text-white font-black shadow-xs'
                          : 'bg-white hover:bg-indigo-50 text-indigo-950 border border-slate-300'
                      }`}
                      title={isSelected ? `إلغاء الشعبة (${tag})` : `إضافة الشعبة (${tag})`}
                    >
                      {sec}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">رقم الهاتف:</label>
            <input 
              type="text" 
              placeholder="0770xxxxxxx"
              value={formData.phoneNumber} 
              onChange={e => setFormData(p => ({ ...p, phoneNumber: e.target.value }))} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold font-mono focus:border-purple-600 focus:outline-none" 
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">رقم البطاقة الوطنية:</label>
            <input 
              type="text" 
              placeholder="19xxxxxxxxxx"
              value={formData.nationalCardNumber} 
              onChange={e => setFormData(p => ({ ...p, nationalCardNumber: e.target.value }))} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold font-mono focus:border-purple-600 focus:outline-none" 
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">الشهادة والتحصيل الدراسي:</label>
            <input 
              type="text" 
              placeholder="بكالوريوس / ماجستير"
              value={formData.academicDegree} 
              onChange={e => setFormData(p => ({ ...p, academicDegree: e.target.value }))} 
              className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold focus:border-purple-600 focus:outline-none" 
            />
          </div>

          <div className="sm:col-span-2">
            <label className="block font-black text-slate-800 mb-1">نصاب الحصص الأسبوعي:</label>
            <div className="flex items-center gap-2">
              <input 
                type="number" 
                min="0" 
                max="40" 
                value={formData.teachingQuota !== undefined ? formData.teachingQuota : 18} 
                onChange={e => setFormData(p => ({ ...p, teachingQuota: Math.max(0, parseInt(e.target.value, 10) || 0) }))} 
                className="w-full p-2.5 border-2 border-slate-300 rounded-xl bg-white text-blue-900 font-black text-base focus:border-purple-600 focus:outline-none" 
              />
              <button 
                type="button" 
                onClick={() => setFormData(p => ({ ...p, teachingQuota: 0 }))} 
                className="px-4 py-2.5 rounded-xl bg-purple-100 hover:bg-purple-200 text-purple-950 border border-purple-300 text-xs font-black whitespace-nowrap cursor-pointer"
                title="تفريغ إداري (0 حصة)"
              >
                تفريغ إداري (0)
              </button>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200 flex-shrink-0">
          <button 
            type="button" 
            onClick={onClose} 
            className="px-5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 border-2 border-slate-300 text-xs font-black transition-all cursor-pointer"
          >
            إلغاء التراجع
          </button>
          <button 
            type="submit" 
            className="px-6 py-2.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-black shadow-md transition-all cursor-pointer flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" />
            <span>إضافة وتثبيت المنتسب بالسجل ➕</span>
          </button>
        </div>
      </form>
    </div>
    </Portal>
  );
};

// Component: Isolated EditStaffModal to prevent parent re-renders while typing and eliminate lag
interface EditStaffModalProps {
  staff: StaffMember;
  onClose: () => void;
  onSave: (updatedMember: StaffMember) => void;
}

const EditStaffModal: React.FC<EditStaffModalProps> = ({ staff, onClose, onSave }) => {
  const [formData, setFormData] = useState<StaffMember>(() => ({ ...staff }));
  const [editGrade, setEditGrade] = useState('الأول');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <Portal>
      <div 
        onClick={onClose}
        className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm overflow-y-auto p-4 sm:p-6 flex justify-center items-start"
      >
      <div 
        onClick={e => e.stopPropagation()}
        className="bg-white border-2 border-purple-400 rounded-3xl p-6 md:p-8 max-w-3xl w-full shadow-2xl space-y-5 my-auto max-h-[92vh] flex flex-col text-slate-900"
      >
        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3 flex-shrink-0">
          <div className="flex items-center gap-2">
            <Briefcase className="w-6 h-6 text-purple-700" />
            <div>
              <h3 className="text-lg font-black text-purple-950">
                تعديل وسجل الخدمة والبيانات الرسمية للمنتسب
              </h3>
              <p className="text-xs text-slate-600 font-bold">
                {formData.firstName} {formData.secondName} {formData.thirdName} {formData.fourthName} {formData.titleName}
              </p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100 text-slate-700 cursor-pointer" title="إغلاق">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Structured Iraqi Administrative Fields */}
        <form 
          onSubmit={handleSubmit}
          className="space-y-4 text-xs overflow-y-auto flex-1 pr-1 pl-1"
        >
          
          {/* Section 1: Names & Identity */}
          <div className="p-4 rounded-2xl bg-purple-50/50 border-2 border-purple-200 space-y-3">
            <h4 className="font-black text-sm text-purple-950 border-b border-purple-200 pb-1 flex items-center gap-1.5">
              <Edit3 className="w-4 h-4 text-purple-700" />
              <span>1. البيانات الشخصية والهوية الوطنية (قابلة للتعديل)</span>
            </h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <div>
                <label className="text-slate-800 font-bold block mb-1">الاسم الأول:</label>
                <input type="text" required value={formData.firstName || ''} onChange={e => setFormData(p => ({ ...p, firstName: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-purple-600 outline-none" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">اسم الأب:</label>
                <input type="text" required value={formData.secondName || ''} onChange={e => setFormData(p => ({ ...p, secondName: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-purple-600 outline-none" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">اسم الجد:</label>
                <input type="text" required value={formData.thirdName || ''} onChange={e => setFormData(p => ({ ...p, thirdName: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-purple-600 outline-none" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">والد الجد / اللقب:</label>
                <input type="text" value={formData.titleName || formData.fourthName || ''} onChange={e => setFormData(p => ({ ...p, titleName: e.target.value, fourthName: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-purple-600 outline-none" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">اسم الأم الثلاثي:</label>
                <input type="text" value={formData.motherName || ''} onChange={e => setFormData(p => ({ ...p, motherName: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-purple-900 font-black focus:border-purple-600 outline-none" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">المواليد (يوم/شهر/سنة):</label>
                <div className="grid grid-cols-3 gap-1">
                  <input type="text" placeholder="يوم" value={formData.birthDay || ''} onChange={e => setFormData(p => ({ ...p, birthDay: e.target.value }))} className="p-1 text-center border-2 border-slate-300 rounded-lg bg-white text-slate-950 font-bold" />
                  <input type="text" placeholder="شهر" value={formData.birthMonth || ''} onChange={e => setFormData(p => ({ ...p, birthMonth: e.target.value }))} className="p-1 text-center border-2 border-slate-300 rounded-lg bg-white text-slate-950 font-bold" />
                  <input type="text" placeholder="سنة" value={formData.birthYear || ''} onChange={e => setFormData(p => ({ ...p, birthYear: e.target.value }))} className="p-1 text-center border-2 border-slate-300 rounded-lg bg-white text-slate-950 font-bold" />
                </div>
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">رقم البطاقة الوطنية:</label>
                <input type="text" value={formData.nationalCardNumber || ''} onChange={e => setFormData(p => ({ ...p, nationalCardNumber: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-blue-900 font-mono font-black focus:border-purple-600 outline-none" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">رقم الهاتف:</label>
                <input type="text" value={formData.phoneNumber || ''} onChange={e => setFormData(p => ({ ...p, phoneNumber: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-mono font-black focus:border-purple-600 outline-none dir-ltr" />
              </div>
            </div>
          </div>

          {/* Section 2: Ration & Family */}
          <div className="p-4 rounded-2xl bg-blue-50/50 border-2 border-blue-200 space-y-3">
            <h4 className="font-black text-sm text-blue-950 border-b border-blue-200 pb-1">
              2. البطاقة التموينية والسكن وعنوان الراتب
            </h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <div>
                <label className="text-slate-800 font-bold block mb-1">رقم البطاقة التموينية:</label>
                <input type="text" value={formData.rationCardNumber || ''} onChange={e => setFormData(p => ({ ...p, rationCardNumber: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">رقم مركز التموين:</label>
                <input type="text" value={formData.rationCenterNumber || ''} onChange={e => setFormData(p => ({ ...p, rationCenterNumber: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">مهنة الزوج / الزوجة:</label>
                <input type="text" value={formData.spouseOccupation || ''} onChange={e => setFormData(p => ({ ...p, spouseOccupation: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">محل السكن (قضاء/ناحية):</label>
                <input type="text" value={formData.residenceDistrict || ''} onChange={e => setFormData(p => ({ ...p, residenceDistrict: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">أقرب نقطة دالة:</label>
                <input type="text" value={formData.nearestLandmark || ''} onChange={e => setFormData(p => ({ ...p, nearestLandmark: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">رقم بطاقة السكن:</label>
                <input type="text" value={formData.residenceCardNumber || ''} onChange={e => setFormData(p => ({ ...p, residenceCardNumber: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div className="col-span-2">
                <label className="text-slate-800 font-bold block mb-1">رقم الحساب المالي (IBAN / الراتب):</label>
                <input type="text" value={formData.salaryAccountNumber || ''} onChange={e => setFormData(p => ({ ...p, salaryAccountNumber: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-emerald-800 font-mono font-black dir-ltr" />
              </div>
            </div>
          </div>

          {/* Section 3: Official Orders & Service Dates */}
          <div className="p-4 rounded-2xl bg-emerald-50/50 border-2 border-emerald-200 space-y-3">
            <h4 className="font-black text-sm text-emerald-950 border-b border-emerald-200 pb-1">
              3. الخدمة والأوامر الإدارية والشهادة
            </h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <div>
                <label className="text-slate-800 font-bold block mb-1">تاريخ المباشرة الأولى:</label>
                <input type="text" value={`${formData.firstDirectYear || '2020'}/${formData.firstDirectMonth || '01'}/${formData.firstDirectDay || '01'}`} onChange={e => {
                  const parts = e.target.value.split('/');
                  setFormData(p => ({ ...p, firstDirectYear: parts[0] || '', firstDirectMonth: parts[1] || '', firstDirectDay: parts[2] || '' }));
                }} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-mono font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">المباشرة بالمدرسة الحالية:</label>
                <input type="text" value={`${formData.schoolDirectYear || '2024'}/${formData.schoolDirectMonth || '09'}/${formData.schoolDirectDay || '01'}`} onChange={e => {
                  const parts = e.target.value.split('/');
                  setFormData(p => ({ ...p, schoolDirectYear: parts[0] || '', schoolDirectMonth: parts[1] || '', schoolDirectDay: parts[2] || '' }));
                }} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-mono font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">أمر التعيين الوزاري:</label>
                <input type="text" value={formData.appointmentOrderNo || ''} onChange={e => setFormData(p => ({ ...p, appointmentOrderNo: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">الشهادة الأكاديمية:</label>
                <input type="text" value={formData.academicDegree || ''} onChange={e => setFormData(p => ({ ...p, academicDegree: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">الوظيفة بالمدرسة:</label>
                <input type="text" value={formData.jobTitle || 'مدرس'} onChange={e => setFormData(p => ({ ...p, jobTitle: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">الاختصاص الدقيق:</label>
                <input type="text" value={formData.specialization || ''} onChange={e => setFormData(p => ({ ...p, specialization: e.target.value }))} className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">المادة التي يدرسها فعلياً:</label>
                <input 
                  type="text" 
                  list="staff-modal-approved-subjects"
                  value={formData.actualSubjectTaught || ''} 
                  onChange={e => setFormData(p => ({ ...p, actualSubjectTaught: e.target.value }))}
                  onBlur={e => {
                    const res = standardizeSubjectInput(e.target.value);
                    setFormData(p => ({ ...p, actualSubjectTaught: res.standardized }));
                  }}
                  placeholder="اختر أو اكتب اسم المادة..."
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black" 
                />
                <datalist id="staff-modal-approved-subjects">
                  {STANDARD_APPROVED_SUBJECTS.map(s => (
                    <option key={s} value={s} />
                  ))}
                </datalist>
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">الموقف / حالة الملاك:</label>
                <select 
                  value={formData.status || 'مستمر في الملاك'} 
                  onChange={e => setFormData(p => ({ ...p, status: e.target.value as StaffMember['status'] }))} 
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black cursor-pointer"
                >
                  <option value="مستمر في الملاك">مستمر في الملاك</option>
                  <option value="مجاز إجازة طويلة">مجاز إجازة طويلة</option>
                  <option value="منسب إلى المدرسة">منسب إلى المدرسة</option>
                  <option value="منسب خارج المدرسة">منسب خارج المدرسة</option>
                </select>
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">نصاب الحصص الأسبوعي:</label>
                <div className="flex items-center gap-1.5">
                  <input 
                    type="number" 
                    min="0" 
                    max="40" 
                    value={formData.teachingQuota !== undefined ? formData.teachingQuota : 0} 
                    onChange={e => setFormData(p => ({ ...p, teachingQuota: Math.max(0, parseInt(e.target.value, 10) || 0) }))} 
                    className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-blue-900 font-black" 
                  />
                  <button
                    type="button"
                    onClick={() => setFormData(p => ({ ...p, teachingQuota: 0, actualSubjectTaught: 'مفرغ إدارياً / إدارة' }))}
                    className="px-3 py-2 rounded-xl bg-purple-100 hover:bg-purple-200 text-purple-950 border border-purple-300 text-xs font-black whitespace-nowrap cursor-pointer"
                    title="تفريغ إداري (0 حصة)"
                  >
                    تفريغ (0)
                  </button>
                </div>
              </div>
              <div className="col-span-2 md:col-span-4 space-y-2">
                <label className="text-slate-800 font-bold block mb-1">الصفوف والشعب المكلف بتدريسها:</label>
                
                {/* Badges */}
                <div className="flex flex-wrap gap-1 min-h-[36px] p-2 bg-slate-50 border border-slate-300 rounded-xl">
                  {(!formData.classesTaught || formData.classesTaught.length === 0) ? (
                    <span className="text-xs text-slate-400 font-bold">لم تُسند أي شعب بعد</span>
                  ) : (
                    formData.classesTaught.map((item, idx) => (
                      <span key={idx} className="inline-flex items-center gap-1 px-2.5 py-1 bg-indigo-100 text-indigo-950 border border-indigo-300 rounded-lg text-xs font-bold shadow-2xs">
                        <span>{item}</span>
                        <button
                          type="button"
                          onClick={() => setFormData(p => ({ ...p, classesTaught: (p.classesTaught || []).filter(t => t !== item) }))}
                          className="text-slate-400 hover:text-rose-600 font-black cursor-pointer"
                        >
                          ×
                        </button>
                      </span>
                    ))
                  )}
                </div>

                {/* Grade & Section Quick Toolbar */}
                <div className="flex items-center gap-2 pt-1">
                  <select
                    value={editGrade}
                    onChange={e => setEditGrade(e.target.value)}
                    className="p-1.5 border-2 border-slate-300 rounded-xl bg-white text-slate-900 font-bold text-xs"
                    title="اختر الصف"
                  >
                    {GRADE_OPTIONS.map(g => (
                      <option key={g} value={g}>الصف {g}</option>
                    ))}
                  </select>

                  <div className="flex items-center gap-1 flex-wrap">
                    {SECTION_OPTIONS.map(sec => {
                      const tag = `${editGrade} ${sec}`;
                      const isSelected = (formData.classesTaught || []).includes(tag);
                      return (
                        <button
                          key={sec}
                          type="button"
                          onClick={() => {
                            setFormData(p => ({
                              ...p,
                              classesTaught: isSelected
                                ? (p.classesTaught || []).filter(t => t !== tag)
                                : [...(p.classesTaught || []), tag]
                            }));
                          }}
                          className={`w-7 h-7 rounded-lg font-bold text-xs flex items-center justify-center transition-all cursor-pointer ${
                            isSelected
                              ? 'bg-indigo-700 text-white font-black shadow-xs'
                              : 'bg-white hover:bg-indigo-50 text-indigo-950 border border-slate-300'
                          }`}
                          title={isSelected ? `إلغاء الشعبة (${tag})` : `إضافة الشعبة (${tag})`}
                        >
                          {sec}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Direct Text Input for convenience */}
                <input 
                  type="text" 
                  value={Array.isArray(formData.classesTaught) ? formData.classesTaught.join('، ') : (formData.classesTaught || '')} 
                  onChange={e => {
                    const parts = e.target.value.split(/[،,]/).map(c => c.trim()).filter(Boolean);
                    setFormData(p => ({ 
                      ...p, 
                      classesTaught: parts.length > 0 ? parts : (e.target.value.trim() ? [e.target.value.trim()] : []) 
                    }));
                  }} 
                  placeholder="أو اكتب مباشرة مفصولاً بفارزة: مثال: الأول أ، الثاني ب" 
                  className="w-full p-2 border border-slate-300 rounded-xl bg-white text-slate-800 text-xs font-bold" 
                />
              </div>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-200 flex-shrink-0">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-black border-2 border-slate-300 cursor-pointer transition-all"
            >
              إلغاء التراجع
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-black shadow-lg cursor-pointer transition-all flex items-center gap-1.5"
            >
              <Save className="w-4 h-4" />
              <span>حفظ تعديلات المنتسب بالسجل 💾</span>
            </button>
          </div>

        </form>
      </div>
    </div>
    </Portal>
  );
};

export const StaffRegisterView: React.FC<StaffRegisterViewProps> = ({
  staffList,
  setStaffList,
  config,
  scheduleMap
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSpecialization, setSelectedSpecialization] = useState('الكل');
  const [selectedStatus, setSelectedStatus] = useState('الكل');

  const [selectedStaffForDetail, setSelectedStaffForDetail] = useState<StaffMember | null>(null);
  const [showPrintStaffModal, setShowPrintStaffModal] = useState(false);
  const [showAddStaffModal, setShowAddStaffModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [customSubjectStaffId, setCustomSubjectStaffId] = useState<string | null>(null);
  const [customSubjectInput, setCustomSubjectInput] = useState('');
  const [rowActiveGrade, setRowActiveGrade] = useState<Record<string, string>>({});

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

  // Helper: Determine if staff is administrative / zero quota role
  const isAdministrativeOrZero = (staff: StaffMember) => {
    return isExemptStaff(staff);
  };

  // Memoized calculation of schedule metrics per staff (prevents re-render lag)
  const staffScheduleInfo = useMemo(() => {
    const infoMap: Record<string, { lessons: number; classes: string }> = {};

    staffList.forEach(staff => {
      const isZero = isAdministrativeOrZero(staff);
      let count = 0;
      const classSet = new Set<string>();

      if (scheduleMap && staff.teachingQuota !== 0) {
        Object.values(scheduleMap).forEach((dayRows: any) => {
          if (Array.isArray(dayRows)) {
            dayRows.forEach(row => {
              if (row && row.lessons) {
                Object.values(row.lessons).forEach((cell: any) => {
                  if (cell && typeof cell === 'object' && cell.teacherName && !cell.isOff) {
                    if (matchStaffWithScheduleCell(staff, cell.teacherName, cell.subject)) {
                      count++;
                      if (row.grade) {
                        classSet.add(`${row.grade} (${row.section || 'أ'})`.trim());
                      }
                    }
                  }
                });
              }
            });
          }
        });
      }

      const assignedStr = classSet.size > 0 
        ? Array.from(classSet).join('، ') 
        : ((Array.isArray(staff.classesTaught) && staff.classesTaught.length > 0)
            ? staff.classesTaught.join('، ') 
            : (isZero ? 'مفرغ إدارياً' : ''));

      infoMap[staff.id] = {
        lessons: staff.teachingQuota === 0 ? 0 : (count > 0 ? count : (staff.teachingQuota !== undefined ? staff.teachingQuota : (isZero ? 0 : 18))),
        classes: assignedStr
      };
    });

    return infoMap;
  }, [staffList, scheduleMap]);

  // Handlers for immediate inline update
  const handleJobTitleChange = (staffId: string, newJobTitle: string) => {
    const isZeroRole = ['مدير', 'معاون مدير', 'مرشد تربوي', 'أمين مكتبة', 'كاتب', 'موظف خدمة', 'مشرف'].includes(newJobTitle);
    setStaffList(prev => {
      const updated = prev.map(s => {
        if (s.id !== staffId) return s;
        return {
          ...s,
          jobTitle: newJobTitle,
          teachingQuota: isZeroRole ? 0 : (s.teachingQuota === 0 ? 18 : s.teachingQuota),
          actualSubjectTaught: isZeroRole && (!s.actualSubjectTaught || s.actualSubjectTaught === 'اللغة العربية') ? 'مفرغ إدارياً / إدارة' : s.actualSubjectTaught
        };
      });
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });
  };

  const handleQuotaChange = (staffId: string, newQuota: number) => {
    const q = Math.max(0, newQuota);
    setStaffList(prev => {
      const updated = prev.map(s => s.id === staffId ? { ...s, teachingQuota: q } : s);
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });
  };

  const handleActualSubjectChange = (staffId: string, newSubject: string) => {
    if (newSubject === '__custom_new__') {
      const stf = staffList.find(s => s.id === staffId);
      setCustomSubjectStaffId(staffId);
      setCustomSubjectInput(stf?.actualSubjectTaught || '');
      return;
    }
    const res = standardizeSubjectInput(newSubject);
    setStaffList(prev => {
      const updated = prev.map(s => s.id === staffId ? { ...s, actualSubjectTaught: res.standardized } : s);
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });
  };

  const handleClassesTaughtChange = (staffId: string, newClassesText: string) => {
    const parts = newClassesText.split(/[،,]/).map(c => c.trim()).filter(Boolean);
    setStaffList(prev => {
      const updated = prev.map(s => {
        if (s.id !== staffId) return s;
        return {
          ...s,
          classesTaught: parts.length > 0 ? parts : (newClassesText.trim() ? [newClassesText.trim()] : [])
        };
      });
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });
  };

  const handleAddGrade = (staffId: string, grade: string) => {
    setStaffList(prev => {
      const updated = prev.map(s => {
        if (s.id !== staffId) return s;
        const current = Array.isArray(s.classesTaught) ? [...s.classesTaught] : [];
        const normGrade = grade.replace(/^(الصف|صف)\s+/, '').trim();
        const hasGrade = current.some(c => c.includes(normGrade));
        if (!hasGrade) {
          current.push(`${grade} أ`);
        }
        return { ...s, classesTaught: current };
      });
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });
    setRowActiveGrade(prev => ({ ...prev, [staffId]: grade }));
  };

  const handleRemoveGrade = (staffId: string, grade: string) => {
    const normGrade = grade.replace(/^(الصف|صف)\s+/, '').trim();
    setStaffList(prev => {
      const updated = prev.map(s => {
        if (s.id !== staffId) return s;
        const current = Array.isArray(s.classesTaught) ? s.classesTaught : [];
        const filtered = current.filter(c => !c.includes(normGrade));
        return { ...s, classesTaught: filtered };
      });
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });
  };

  const handleToggleClassSection = (staffId: string, grade: string, section: string) => {
    const targetTag = `${grade} ${section}`;
    const normGrade = grade.replace(/^(الصف|صف)\s+/, '').trim();

    setStaffList(prev => {
      const updated = prev.map(s => {
        if (s.id !== staffId) return s;
        const current = Array.isArray(s.classesTaught) ? [...s.classesTaught] : [];
        const existingIdx = current.findIndex(cStr => {
          const parsed = parseClassTaught(cStr, '');
          const pNorm = parsed.grade.replace(/^(الصف|صف)\s+/, '').trim();
          const matchG = parsed.grade === grade || pNorm.includes(normGrade) || normGrade.includes(pNorm) || cStr.includes(normGrade);
          const matchS = parsed.section === section || cStr.includes(section);
          return matchG && matchS;
        });

        if (existingIdx !== -1) {
          current.splice(existingIdx, 1);
        } else {
          current.push(targetTag);
        }

        return { ...s, classesTaught: current };
      });
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });
  };

  const handleRemoveClassSection = (staffId: string, targetTag: string) => {
    setStaffList(prev => {
      const updated = prev.map(s => {
        if (s.id !== staffId) return s;
        const current = Array.isArray(s.classesTaught) ? s.classesTaught : [];
        const filtered = current.filter(c => c !== targetTag && c.trim() !== targetTag.trim());
        return { ...s, classesTaught: filtered };
      });
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });
  };

  const handleStatusChange = (staffId: string, newStatus: StaffMember['status']) => {
    setStaffList(prev => {
      const updated = prev.map(s => s.id === staffId ? { ...s, status: newStatus } : s);
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });

    try {
      const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
      const client = getSupabase(schoolId);
      client.from('teachers').update({ status: newStatus }).eq('id', staffId).then(() => {});
    } catch (e) {}
  };

  const handleAddNewStaff = (newMember: StaffMember) => {
    setStaffList(prev => {
      const updated = [newMember, ...prev];
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });

    try {
      const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
      const client = getSupabase(schoolId);
      client.from('teachers').insert({
        id: newMember.id,
        school_id: schoolId,
        name: `${newMember.firstName} ${newMember.secondName} ${newMember.thirdName} ${newMember.fourthName} ${newMember.titleName}`.trim(),
        specialization: newMember.specialization,
        quota: newMember.teachingQuota,
        status: newMember.status,
        raw_data: newMember
      }).then(() => {});
    } catch (e) {}

    setShowAddStaffModal(false);
  };

  const handleSaveStaffDetail = (updatedMember: StaffMember) => {
    setStaffList(prev => {
      const updated = prev.map(s => s.id === updatedMember.id ? updatedMember : s);
      localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
      return updated;
    });

    try {
      const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
      const client = getSupabase(schoolId);
      const fullName = `${updatedMember.firstName || ''} ${updatedMember.secondName || ''} ${updatedMember.thirdName || ''} ${updatedMember.fourthName || ''} ${updatedMember.titleName || ''}`.trim();
      client.from('teachers').upsert({
        id: updatedMember.id,
        school_id: schoolId,
        name: fullName,
        specialization: updatedMember.specialization,
        quota: updatedMember.teachingQuota,
        status: updatedMember.status,
        raw_data: updatedMember
      }, { onConflict: 'id' }).then(() => {});
    } catch (e) {}

    setSelectedStaffForDetail(null);
  };

  const handleDeleteStaff = (staffId: string, staffName: string) => {
    if (confirm(`هل أنت متأكد من حذف المنتسب [${staffName}] نهائياً من سجل الكادر؟`)) {
      setStaffList(prev => {
        const updated = prev.filter(s => s.id !== staffId);
        localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
        return updated;
      });

      try {
        const schoolId = config.schoolId || localStorage.getItem('diyala_school_id') || 'school_01';
        const client = getSupabase(schoolId);
        client.from('teachers').delete().eq('id', staffId).then(() => {});
      } catch (e) {}
    }
  };

  const handlePurgeAllStaff = () => {
    if (confirm('⚠️ تحذير: هل أنت متأكد من مسح وتصفير كافة كادر التدريس بالكامل؟')) {
      setStaffList([]);
      localStorage.setItem('diyala_school_staff', JSON.stringify([]));
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
      
      {/* Title & Top Bar */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 bg-[var(--theme-card)] p-5 rounded-2xl border border-[var(--theme-card-border)] shadow-sm">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-50 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300 text-xs font-bold mb-2">
            <Users className="w-3.5 h-3.5" />
            <span>سجل وتوزيع الكادر التدريسي</span>
          </div>
          <h2 className="text-xl md:text-2xl font-black text-[var(--theme-text-main)]">
            سجل الكادر التعليمي وتوزيع الحصص الأسبوعية
          </h2>
          <p className="text-xs text-[var(--theme-text-muted)] mt-1">
            إجمالي الكادر: {staffList.length} منتسب | الحصص مستخرجة ومحسوبة من الجدول الأسبوعي حصراً
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

      {/* Horizontal Scroll Quick Bar & Table View */}
      <div className="data-grid-shell bg-white rounded-2xl border-2 border-slate-300 shadow-lg overflow-hidden">
        
        {/* Quick Top Scroll Strip / Control Indicator */}
        <div className="bg-slate-100 border-b border-slate-200 px-4 py-2 flex items-center justify-between text-xs text-slate-700 font-bold">
          <div className="flex items-center gap-2">
            <span className="bg-indigo-100 text-indigo-900 px-2.5 py-0.5 rounded-md text-[11px] font-black">
              ↔️ شريط التمرير الجانبي المباشر:
            </span>
            <span className="text-slate-600 hidden sm:inline">
              يمكنك التمرير يميناً ويساراً مباشرة من أي مكان أو استخدام الأزرار:
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={() => {
                const el = document.getElementById('staff-table-scroll-container');
                if (el) el.scrollBy({ left: -300, behavior: 'smooth' });
              }}
              className="px-3 py-1 rounded-lg bg-white hover:bg-slate-200 text-slate-800 border border-slate-300 text-xs font-bold transition-all shadow-xs cursor-pointer"
            >
              ⬅️ تمرير لليسار
            </button>
            <button
              type="button"
              onClick={() => {
                const el = document.getElementById('staff-table-scroll-container');
                if (el) el.scrollBy({ left: 300, behavior: 'smooth' });
              }}
              className="px-3 py-1 rounded-lg bg-white hover:bg-slate-200 text-slate-800 border border-slate-300 text-xs font-bold transition-all shadow-xs cursor-pointer"
            >
              ➡️ تمرير لليمين
            </button>
          </div>
        </div>

        {/* Scrollable Container with sticky headers and pinned scrollbar */}
        <div 
          id="staff-table-scroll-container" 
          className="overflow-x-auto overflow-y-auto max-h-[calc(100vh-280px)] scrollbar-thin scrollbar-thumb-sky-500"
        >
          <table className="data-grid w-full text-center border-collapse min-w-[1250px] text-xs">
            <thead className="sticky top-0 z-20 shadow-md">
              <tr className="bg-gradient-to-r from-sky-800 via-indigo-800 to-purple-800 text-white font-black border-b-2 border-indigo-400 text-xs">
                <th className="py-3.5 px-2.5 border-r border-indigo-600 w-10 text-center whitespace-nowrap">ت</th>
                <th className="py-3.5 px-3 border-r border-indigo-600 text-right whitespace-nowrap">اسم الأستاذ الرباعي واللقب</th>
                <th className="py-3.5 px-3 border-r border-indigo-600 text-center whitespace-nowrap">الموقف / الحالة</th>
                <th className="py-3.5 px-3 border-r border-indigo-600 text-center whitespace-nowrap">الاختصاص الأكاديمي</th>
                <th className="py-3.5 px-3 border-r border-indigo-600 text-center whitespace-nowrap">المادة التي يدرّسها</th>
                <th className="py-3.5 px-3 border-r border-indigo-600 text-center whitespace-nowrap min-w-[150px]">الصفوف المكلف بها 🎓</th>
                <th className="py-3.5 px-3 border-r border-indigo-600 text-center whitespace-nowrap min-w-[260px]">الشعب المكلف بها 🏷️ (أ، ب، ...)</th>
                <th className="py-3.5 px-3 border-r border-indigo-600 text-center whitespace-nowrap">عدد الحصص (من الجدول الأسبوعي)</th>
                <th className="py-3.5 px-3 border-r border-indigo-600 text-center whitespace-nowrap">الوظيفة في المدرسة</th>
                <th className="py-3.5 px-3 border-r border-indigo-600 text-center whitespace-nowrap">السجل</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-200 text-xs">
              {filteredStaff.length === 0 ? (
                <tr>
                  <td colSpan={10} className="py-8 text-center text-slate-500 font-bold">
                    لا توجد منتسبون مطابقون لخيارات الفلترة أو البحث الحالية.
                  </td>
                </tr>
              ) : (
                filteredStaff.map((staff, idx) => {
                  const scheduleInfo = staffScheduleInfo[staff.id] || { lessons: staff.teachingQuota ?? 18, classes: '' };
                  const calculatedLessons = scheduleInfo.lessons;
                  const teacherClasses = Array.isArray(staff.classesTaught) ? staff.classesTaught : [];
                  const assignedGrades: string[] = Array.from(new Set<string>(teacherClasses.map(c => {
                    const parsed = parseClassTaught(c, '');
                    return parsed.grade.replace(/^(الصف|صف)\s+/, '').trim() || c.split(' ')[0] || c;
                  }))).filter(Boolean);
                  const curActiveGrade = rowActiveGrade[staff.id] || (assignedGrades[0] || 'الأول');
                  const actualSubject = staff.actualSubjectTaught || staff.specialization || 'اللغة العربية';
                  const isSubjectMismatch = staff.specialization && !staff.specialization.includes(actualSubject) && !actualSubject.includes(staff.specialization);

                  return (
                    <tr key={staff.id} className="hover:bg-sky-50/80 transition-colors">
                      
                      {/* 1. Seq */}
                      <td className="py-3 px-2 border-r border-slate-200 text-center font-mono font-bold text-slate-700">
                        {idx + 1}
                      </td>

                      {/* 2. Full 4-Part Name & Title */}
                      <td className="py-3 px-3 border-r border-slate-200 text-right whitespace-nowrap">
                        <div className="font-black text-slate-900 text-sm">
                          أ. {[staff.firstName, staff.secondName, staff.thirdName, staff.fourthName, staff.titleName].filter(Boolean).join(' ')}
                        </div>
                        <div className="text-[10px] text-slate-500 font-mono">
                          {staff.phoneNumber || staff.nationalCardNumber || 'بدون هاتف'}
                        </div>
                      </td>

                      {/* 3. Status (الموقف / الحالة: مستمر، مجاز، منسب) */}
                      <td className="py-2.5 px-2 border-r border-slate-200 text-center whitespace-nowrap">
                        <select
                          value={staff.status || 'مستمر في الملاك'}
                          onChange={(e) => handleStatusChange(staff.id, e.target.value as StaffMember['status'])}
                          className={`px-2.5 py-1.5 rounded-xl font-black text-[11px] border-2 shadow-xs cursor-pointer focus:outline-none transition-all ${
                            staff.status === 'مجاز إجازة طويلة'
                              ? 'bg-amber-100 text-amber-950 border-amber-400'
                              : staff.status === 'منسب إلى المدرسة'
                              ? 'bg-sky-100 text-sky-950 border-sky-400'
                              : staff.status === 'منسب خارج المدرسة'
                              ? 'bg-purple-100 text-purple-950 border-purple-400'
                              : 'bg-emerald-100 text-emerald-950 border-emerald-400'
                          }`}
                          title="تعديل الموقف وحالة الملاك (مستمر، مجاز، منسب)"
                        >
                          <option value="مستمر في الملاك">مستمر في الملاك</option>
                          <option value="مجاز إجازة طويلة">مجاز إجازة طويلة</option>
                          <option value="منسب إلى المدرسة">منسب إلى المدرسة</option>
                          <option value="منسب خارج المدرسة">منسب خارج المدرسة</option>
                        </select>
                      </td>

                      {/* 4. Specialization */}
                      <td className="py-3 px-3 border-r border-slate-200 text-center whitespace-nowrap">
                        <span className="inline-block px-2.5 py-1 rounded-full bg-slate-100 text-slate-800 border border-slate-300 font-black text-xs">
                          {staff.specialization || 'عام'}
                        </span>
                      </td>

                      {/* 5. Actual Subject Taught (Dropdown) */}
                      <td className="py-2.5 px-3 border-r border-slate-200 text-center whitespace-nowrap">
                        <div className="flex items-center justify-center gap-1">
                          <select
                            value={actualSubject}
                            onChange={(e) => handleActualSubjectChange(staff.id, e.target.value)}
                            className={`px-2.5 py-1.5 rounded-lg font-bold text-xs border-2 shadow-xs cursor-pointer focus:outline-none transition-all ${
                              isSubjectMismatch 
                                ? 'bg-amber-50 text-amber-950 border-amber-400' 
                                : 'bg-sky-50 text-sky-950 border-sky-300'
                            }`}
                            title={isSubjectMismatch ? 'تنبيه: المادة المدرّسة تختلف عن الاختصاص الأصلي' : 'المادة المسندة للمدرس'}
                          >
                            <optgroup label="الدروس الوزارية المعتمدة">
                              {STANDARD_APPROVED_SUBJECTS.map(subj => (
                                <option key={subj} value={subj}>{subj}</option>
                              ))}
                            </optgroup>
                            <option value="مفرغ إدارياً / إدارة">مفرغ إدارياً / إدارة</option>
                            {!STANDARD_APPROVED_SUBJECTS.includes(actualSubject) && actualSubject !== 'مفرغ إدارياً / إدارة' && (
                              <optgroup label="مادة جديدة مستحدثة">
                                <option value={actualSubject}>{actualSubject} ✨</option>
                              </optgroup>
                            )}
                            <option value="__custom_new__">➕ إضافة / كتابة مادة جديدة...</option>
                          </select>
                          <button
                            type="button"
                            onClick={() => {
                              setCustomSubjectStaffId(staff.id);
                              setCustomSubjectInput(actualSubject);
                            }}
                            title="تعديل أو كتابة اسم مادة جديدة"
                            className="p-1 rounded text-sky-700 hover:bg-sky-100 transition-colors cursor-pointer"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                          </button>
                          {isSubjectMismatch && (
                            <span className="text-[10px] font-bold text-amber-700 bg-amber-100 px-1 py-0.5 rounded border border-amber-300" title="مخالف للاختصاص">
                              مغاير
                            </span>
                          )}
                        </div>
                      </td>

                      {/* 6. Classes Taught (Dropdown from 1st to 6th) */}
                      <td className="py-2 px-2 border-r border-slate-200 text-center whitespace-nowrap min-w-[150px]">
                        {isExemptStaff(staff) ? (
                          <span className="px-2.5 py-1 rounded-xl bg-purple-100 text-purple-950 border border-purple-300 font-bold text-[11px] inline-flex items-center gap-1 shadow-2xs">
                            <span>مفرغ إدارياً</span>
                          </span>
                        ) : (
                          <div className="flex flex-col items-center gap-1.5">
                            {assignedGrades.length > 0 && (
                              <div className="flex flex-wrap items-center justify-center gap-1 max-w-[190px]">
                                {assignedGrades.map(g => (
                                  <span key={g} className="px-2 py-0.5 rounded-lg bg-indigo-50 border border-indigo-200 text-indigo-900 font-bold text-[11px] flex items-center gap-1 shadow-2xs">
                                    <span>الصف {g}</span>
                                    <button
                                      type="button"
                                      onClick={() => handleRemoveGrade(staff.id, g)}
                                      className="text-slate-400 hover:text-rose-600 font-black text-xs cursor-pointer transition-colors"
                                      title={`حذف كافة شعب الصف ${g}`}
                                    >
                                      ×
                                    </button>
                                  </span>
                                ))}
                              </div>
                            )}
                            <select
                              value=""
                              onChange={(e) => {
                                if (e.target.value) {
                                  handleAddGrade(staff.id, e.target.value);
                                }
                              }}
                              className="px-2 py-1 rounded-lg border-2 border-indigo-200 bg-white text-indigo-950 font-bold text-xs cursor-pointer hover:border-indigo-400 focus:outline-none focus:border-indigo-600 transition-all shadow-2xs"
                              title="اختر صفاً لإضافته للمدرس"
                            >
                              <option value="">➕ إضافة صف...</option>
                              {GRADE_OPTIONS.map(grade => (
                                <option key={grade} value={grade}>الصف {grade}</option>
                              ))}
                            </select>
                          </div>
                        )}
                      </td>

                      {/* 7. Sections Taught (Badges + Multi-Section Quick Selector) */}
                      <td className="py-2 px-2 border-r border-slate-200 text-center whitespace-nowrap min-w-[260px]">
                        {isExemptStaff(staff) ? (
                          <span className="px-2.5 py-1 rounded-xl bg-purple-50 text-purple-900 border border-purple-200 font-bold text-[11px] inline-flex items-center gap-1 shadow-2xs">
                            <span>معفى من الشعب (0 حصة)</span>
                          </span>
                        ) : (
                          <div className="flex flex-col items-center gap-1.5">
                            {/* Assigned Class+Section Badges */}
                            <div className="flex flex-wrap items-center justify-center gap-1 max-w-[280px]">
                              {teacherClasses.length === 0 ? (
                                <span className="text-[11px] text-slate-400 font-bold">لم تُسند شعب بعد</span>
                              ) : (
                                teacherClasses.map((item, cIdx) => (
                                  <span
                                    key={cIdx}
                                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-sky-50 text-sky-950 border border-sky-300 font-bold text-xs shadow-2xs group"
                                  >
                                    <span>{item}</span>
                                    <button
                                      type="button"
                                      onClick={() => handleRemoveClassSection(staff.id, item)}
                                      className="text-slate-400 group-hover:text-rose-600 font-black text-xs cursor-pointer transition-colors"
                                      title={`إلغاء تكليف شعبة (${item})`}
                                    >
                                      ×
                                    </button>
                                  </span>
                                ))
                              )}
                            </div>

                            {/* Section Quick Toggle Bar */}
                            <div className="flex items-center justify-center gap-1 bg-slate-50 p-1 rounded-xl border border-slate-200 shadow-2xs">
                              <select
                                value={curActiveGrade}
                                onChange={(e) => setRowActiveGrade(prev => ({ ...prev, [staff.id]: e.target.value }))}
                                className="px-1.5 py-0.5 rounded-lg border border-slate-300 bg-white text-slate-900 font-bold text-[11px] focus:outline-none cursor-pointer"
                                title="اختر الصف لتحديد شعبه"
                              >
                                {GRADE_OPTIONS.map(g => (
                                  <option key={g} value={g}>{g}</option>
                                ))}
                              </select>

                              <div className="flex items-center gap-0.5">
                                {SECTION_OPTIONS.map(sec => {
                                  const isAssigned = teacherClasses.some(cStr => {
                                    const parsed = parseClassTaught(cStr, '');
                                    const pNorm = parsed.grade.replace(/^(الصف|صف)\s+/, '').trim();
                                    const curNorm = curActiveGrade.replace(/^(الصف|صف)\s+/, '').trim();
                                    const matchG = parsed.grade === curActiveGrade || pNorm.includes(curNorm) || curNorm.includes(pNorm) || cStr.includes(curNorm);
                                    const matchS = parsed.section === sec || cStr.includes(sec);
                                    return matchG && matchS;
                                  });

                                  return (
                                    <button
                                      key={sec}
                                      type="button"
                                      onClick={() => handleToggleClassSection(staff.id, curActiveGrade, sec)}
                                      className={`w-6 h-6 rounded-md font-bold text-xs flex items-center justify-center transition-all cursor-pointer shadow-2xs ${
                                        isAssigned
                                          ? 'bg-indigo-700 text-white font-black ring-1 ring-indigo-800 shadow-sm'
                                          : 'bg-white hover:bg-indigo-50 text-indigo-950 border border-slate-300'
                                      }`}
                                      title={isAssigned ? `إلغاء تكليف شعبة (${curActiveGrade} ${sec})` : `تكليف شعبة (${curActiveGrade} ${sec})`}
                                    >
                                      {sec}
                                    </button>
                                  );
                                })}
                              </div>
                            </div>
                          </div>
                        )}
                      </td>

                      {/* 7. Quota (From Weekly Schedule or Direct Override) */}
                      <td className="py-2 px-2 border-r border-slate-200 text-center whitespace-nowrap">
                        <div className="flex items-center justify-center gap-1.5">
                          <input
                            type="number"
                            min="0"
                            max="40"
                            value={staff.teachingQuota !== undefined ? staff.teachingQuota : calculatedLessons}
                            onChange={(e) => handleQuotaChange(staff.id, parseInt(e.target.value, 10) || 0)}
                            className={`w-14 text-center py-1 rounded-lg font-mono font-black text-xs border-2 shadow-xs transition-all ${
                              (staff.teachingQuota === 0 || (staff.teachingQuota === undefined && calculatedLessons === 0))
                                ? 'bg-purple-100 text-purple-950 border-purple-400 font-black'
                                : 'bg-emerald-50 text-emerald-950 border-emerald-300'
                            }`}
                            title="تعديل عدد الحصص الأسبوعية (اكتب 0 للمدير أو المعاون المفرغ)"
                          />
                          <button
                            type="button"
                            onClick={() => handleQuotaChange(staff.id, staff.teachingQuota === 0 ? 18 : 0)}
                            className={`px-2 py-1 rounded-lg text-[10px] font-black transition-all cursor-pointer border ${
                              staff.teachingQuota === 0
                                ? 'bg-purple-700 text-white border-purple-800'
                                : 'bg-slate-100 hover:bg-purple-100 text-slate-700 border-slate-300'
                            }`}
                            title="تفريغ إداري (0 حصة) بنقرة واحدة"
                          >
                            {staff.teachingQuota === 0 ? 'مفرغ 0' : 'تفريغ 0'}
                          </button>
                        </div>
                      </td>

                      {/* 8. Job Title (Dropdown) */}
                      <td className="py-2.5 px-3 border-r border-slate-200 text-center whitespace-nowrap">
                        <select
                          value={staff.jobTitle || 'مدرس'}
                          onChange={(e) => handleJobTitleChange(staff.id, e.target.value)}
                          className="px-2.5 py-1.5 rounded-lg bg-purple-50 text-purple-950 border-2 border-purple-300 font-bold text-xs shadow-xs cursor-pointer focus:outline-none focus:border-purple-500"
                        >
                          {JOB_TITLE_OPTIONS.map(title => (
                            <option key={title} value={title}>{title}</option>
                          ))}
                        </select>
                      </td>

                      {/* 9. Action Buttons */}
                      <td className="py-2 px-3 text-center whitespace-nowrap">
                        <div className="flex items-center justify-center gap-1.5">
                          <button
                            onClick={() => setSelectedStaffForDetail(staff)}
                            className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs shadow-xs transition-all cursor-pointer"
                            title="تعديل وفتح سجل التفاصيل الكاملة للأستاذ"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                            <span>تعديل ✏️</span>
                          </button>
                          <button
                            onClick={() => handleDeleteStaff(staff.id, `${staff.firstName} ${staff.secondName}`)}
                            className="p-1.5 rounded-xl bg-rose-50 text-rose-700 hover:bg-rose-600 hover:text-white border border-rose-300 font-bold text-xs shadow-xs transition-all cursor-pointer"
                            title="حذف المنتسب نهائياً من السجل"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>

                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal 1: Full Expandable Staff Detail (Isolated Component) */}
      {selectedStaffForDetail && (
        <EditStaffModal
          key={selectedStaffForDetail.id}
          staff={selectedStaffForDetail}
          onClose={() => setSelectedStaffForDetail(null)}
          onSave={handleSaveStaffDetail}
        />
      )}

      {/* Modal 2: Import Staff from Excel / Image / Text */}
      {showImportModal && (
        <Portal>
          <div className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-white border-2 border-purple-400 rounded-3xl p-6 md:p-8 max-w-lg w-full shadow-2xl space-y-4 text-slate-900">
              <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
                <h3 className="text-base font-black text-purple-950">
                  استيراد بيانات الكادر التدريسي (أكسل / صورة / وورد)
                </h3>
                <button onClick={() => setShowImportModal(false)} className="p-1 rounded-lg hover:bg-slate-100 text-slate-700 cursor-pointer">
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Option 1: Excel */}
              <div className="p-4 rounded-2xl bg-purple-50/60 border-2 border-purple-200 space-y-2 text-xs">
                <span className="font-black text-purple-950 block">
                  الخيار الأول: اختيار ملف أكسل (Excel):
                </span>
                <input
                  type="file"
                  accept=".xlsx, .xls, .csv, .doc, .docx, .pdf, .png, .jpg, .jpeg, .txt"
                  onChange={handleExcelUpload}
                  className="w-full text-xs text-slate-800 font-bold file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-black file:bg-purple-700 file:text-white"
                />
              </div>

              {/* Option 2: Image Photo Reader */}
              <div className="p-4 rounded-2xl bg-amber-50/60 border-2 border-amber-200 space-y-2 text-xs">
                <span className="font-black text-amber-950 block flex items-center gap-1">
                  <Upload className="w-4 h-4 text-amber-600" />
                  الخيار الثاني: قراءة واستيراد من صورة مستند/سجل (Photo Scanner):
                </span>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleImageUpload}
                  className="w-full text-xs text-slate-800 font-bold file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-black file:bg-amber-600 file:text-white"
                />
              </div>

              {/* Option 3: Raw Text */}
              <div className="space-y-2 text-xs">
                <span className="font-black text-slate-800 block">
                  الخيار الثالث: لصق نص من ملف وورد أو نص مباشر:
                </span>
                <textarea
                  value={importRawText}
                  onChange={e => setImportRawText(e.target.value)}
                  rows={3}
                  placeholder="لصق أسماء المدرسين واختصاصاتهم..."
                  className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-900 font-bold text-xs focus:border-purple-600 outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
                <button
                  onClick={() => setShowImportModal(false)}
                  className="px-5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-black border-2 border-slate-300 cursor-pointer"
                >
                  إلغاء التراجع
                </button>
              </div>
            </div>
          </div>
        </Portal>
      )}

      {/* Modal 2.1: Staff Pre-Import Audit Modal */}
      {auditPendingList && (
        <Portal>
          <div className="fixed inset-0 z-50 bg-slate-900/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white border-2 border-purple-400 rounded-3xl p-6 md:p-8 max-w-3xl w-full shadow-2xl space-y-4 my-8 text-slate-900">
              <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-6 h-6 text-purple-700" />
                  <div>
                    <h3 className="text-base font-black text-purple-950">
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
        </Portal>
      )}

      {/* Modal 3: Add Staff Member (Isolated Component) */}
      {showAddStaffModal && (
        <AddStaffModal 
          onClose={() => setShowAddStaffModal(false)} 
          onAdd={handleAddNewStaff} 
        />
      )}

      {/* Printable Staff Roster Modal */}
      {showPrintStaffModal && (
        <Portal>
          <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
          <div id="staff-roster-printable-area" className="bg-white text-slate-900 rounded-3xl p-8 max-w-4xl w-full shadow-2xl space-y-6 my-8 print-page relative border-2 border-slate-800 dir-rtl">
            
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
              <table className="data-grid w-full text-center border-collapse text-xs">
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
                onClick={() => printElement('staff-roster-printable-area', { title: 'سجل الكادر الموحد', orientation: 'landscape' })}
                className="flex-1 py-3 rounded-2xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-black shadow-xl flex items-center justify-center gap-2 cursor-pointer"
              >
                <Printer className="w-4 h-4" />
                <span>طباعة سجل الكادر الموحد (A4)</span>
              </button>
            </div>

          </div>
        </div>
        </Portal>
      )}

      {/* Modal: Add or Edit Custom Subject for Staff */}
      {customSubjectStaffId && (
        <Portal>
          <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border-2 border-sky-300 p-6 max-w-md w-full shadow-2xl space-y-4 text-right">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <div className="flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-sky-600" />
                <h3 className="text-base font-black text-slate-900">
                  تعديل أو إضافة مادة تدريس للأستاذ 📚
                </h3>
              </div>
              <button
                onClick={() => setCustomSubjectStaffId(null)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-500 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-2">
              <label className="block text-xs font-black text-slate-800">
                اسم المادة الدراسية:
              </label>
              <input
                type="text"
                list="staff-custom-approved-subjects"
                value={customSubjectInput}
                onChange={e => setCustomSubjectInput(e.target.value)}
                placeholder="اكتب اسم المادة (مثال: التربية الإسلامية، علم الأرض، ذكاء اصطناعي...)"
                className="w-full p-2.5 rounded-xl border-2 border-slate-300 font-black text-sm text-slate-900 focus:border-sky-600 focus:outline-none"
                autoFocus
              />
              <datalist id="staff-custom-approved-subjects">
                {STANDARD_APPROVED_SUBJECTS.map(sub => (
                  <option key={sub} value={sub} />
                ))}
              </datalist>
            </div>

            {/* Live Normalization / Feedback Badge */}
            {customSubjectInput.trim() && (() => {
              const res = standardizeSubjectInput(customSubjectInput);
              return res.isApproved ? (
                <div className="p-3 rounded-2xl bg-emerald-50 border border-emerald-300 text-emerald-900 text-xs font-bold flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>
                    مادة معتمدة وزارياً: تم الضبط التلقائي لإملاء الدروس المعتمدة: <strong>({res.standardized})</strong> ✓
                  </span>
                </div>
              ) : (
                <div className="p-3 rounded-2xl bg-purple-50 border border-purple-300 text-purple-900 text-xs font-bold flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-purple-600 shrink-0" />
                  <span>
                    مادة دراسية جديدة (خارج الدروس المعتمدة): سيتم إضافتها وتثبيتها كما كُتبت: <strong>({res.standardized})</strong> ✨
                  </span>
                </div>
              );
            })()}

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
              <button
                type="button"
                onClick={() => setCustomSubjectStaffId(null)}
                className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-xs cursor-pointer"
              >
                إلغاء
              </button>
              <button
                type="button"
                onClick={() => {
                  if (!customSubjectInput.trim()) return;
                  const res = standardizeSubjectInput(customSubjectInput);
                  setStaffList(prev => {
                    const updated = prev.map(s => s.id === customSubjectStaffId ? { ...s, actualSubjectTaught: res.standardized } : s);
                    localStorage.setItem('diyala_school_staff', JSON.stringify(updated));
                    return updated;
                  });
                  setCustomSubjectStaffId(null);
                }}
                className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-black text-xs shadow-md cursor-pointer flex items-center gap-1.5"
              >
                <Check className="w-4 h-4" />
                <span>حفظ واعتماد المادة ✓</span>
              </button>
            </div>
          </div>
        </div>
        </Portal>
      )}

    </div>
  );
};
