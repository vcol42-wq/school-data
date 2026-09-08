import React from 'react';
import { X, UserPlus } from 'lucide-react';
import { Student } from '../../types';
import { Portal } from '../common/Portal';

interface AddStudentModalProps {
  isOpen: boolean;
  onClose: () => void;
  newStudent: Partial<Student>;
  setNewStudent: React.Dispatch<React.SetStateAction<Partial<Student>>>;
  onSubmit: (e: React.FormEvent) => void;
}

export const AddStudentModal: React.FC<AddStudentModalProps> = ({
  isOpen,
  onClose,
  newStudent,
  setNewStudent,
  onSubmit
}) => {
  if (!isOpen) return null;

  return (
    <Portal>
      <div className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4">
      <form 
        onSubmit={onSubmit} 
        className="modal-surface bg-white border-2 border-emerald-500 rounded-3xl p-6 md:p-8 max-w-xl w-full shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto text-slate-900"
      >
        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center font-black">
              <UserPlus className="w-5 h-5" />
            </div>
            <h3 className="text-base font-black text-emerald-950">إضافة طالب جديد لسجل المدرسة</h3>
          </div>
          <button type="button" onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100 text-slate-700 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="grid grid-cols-2 gap-3 text-xs">
          <div>
            <label className="block font-black text-slate-800 mb-1">الاسم الأول:</label>
            <input
              type="text"
              required
              value={newStudent.firstName || ''}
              onChange={e => setNewStudent(p => ({ ...p, firstName: e.target.value }))}
              placeholder="مثال: علي"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
            />
          </div>
          <div>
            <label className="block font-black text-slate-800 mb-1">اسم الأب:</label>
            <input
              type="text"
              required
              value={newStudent.secondName || ''}
              onChange={e => setNewStudent(p => ({ ...p, secondName: e.target.value }))}
              placeholder="مثال: محمد"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
            />
          </div>
          <div>
            <label className="block font-black text-slate-800 mb-1">اسم الجد:</label>
            <input
              type="text"
              required
              value={newStudent.thirdName || ''}
              onChange={e => setNewStudent(p => ({ ...p, thirdName: e.target.value }))}
              placeholder="مثال: حسن"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
            />
          </div>
          <div>
            <label className="block font-black text-slate-800 mb-1">والد الجد واللقب:</label>
            <input
              type="text"
              value={newStudent.titleName || ''}
              onChange={e => setNewStudent(p => ({ ...p, titleName: e.target.value, fourthName: e.target.value }))}
              placeholder="مثال: الكرخي"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">اسم الأم الثلاثي:</label>
            <input
              type="text"
              value={newStudent.motherName || ''}
              onChange={e => setNewStudent(p => ({ ...p, motherName: e.target.value }))}
              placeholder="اسم الأم الثلاثي"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-purple-900 font-black focus:border-emerald-600 outline-none"
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">رقم البطاقة الموحدة:</label>
            <input
              type="text"
              value={newStudent.nationalCardNumber || ''}
              onChange={e => setNewStudent(p => ({ ...p, nationalCardNumber: e.target.value }))}
              placeholder="رقم البطاقة الوطنية"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-blue-900 font-mono font-black focus:border-emerald-600 outline-none"
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">الصف الحالي:</label>
            <select
              value={newStudent.currentGrade || 'الأول المتوسط'}
              onChange={e => setNewStudent(p => ({ ...p, currentGrade: e.target.value }))}
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
            >
              <optgroup label="المرحلة الابتدائية">
                <option value="الأول الابتدائي">الأول الابتدائي</option>
                <option value="الثاني الابتدائي">الثاني الابتدائي</option>
                <option value="الثالث الابتدائي">الثالث الابتدائي</option>
                <option value="الرابع الابتدائي">الرابع الابتدائي</option>
                <option value="الخامس الابتدائي">الخامس الابتدائي</option>
                <option value="السادس الابتدائي">السادس الابتدائي</option>
              </optgroup>
              <optgroup label="المرحلة المتوسطة">
                <option value="الأول المتوسط">الأول المتوسط</option>
                <option value="الثاني المتوسط">الثاني المتوسط</option>
                <option value="الثالث المتوسط">الثالث المتوسط</option>
              </optgroup>
              <optgroup label="الفرع العلمي">
                <option value="الرابع العلمي">الرابع العلمي</option>
                <option value="الخامس العلمي">الخامس العلمي</option>
                <option value="السادس العلمي">السادس العلمي</option>
              </optgroup>
              <optgroup label="الفرع الأدبي">
                <option value="الرابع الأدبي">الرابع الأدبي</option>
                <option value="الخامس الأدبي">الخامس الأدبي</option>
                <option value="السادس الأدبي">السادس الأدبي</option>
              </optgroup>
              <optgroup label="الفرع المهني والصناعي والتجاري">
                <option value="الرابع الصناعي">الرابع الصناعي</option>
                <option value="الخامس الصناعي">الخامس الصناعي</option>
                <option value="السادس الصناعي">السادس الصناعي</option>
                <option value="الرابع التجاري">الرابع التجاري</option>
                <option value="الخامس التجاري">الخامس التجاري</option>
                <option value="السادس التجاري">السادس التجاري</option>
              </optgroup>
            </select>
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">الشعبة:</label>
            <select
              value={newStudent.section || 'أ'}
              onChange={e => setNewStudent(p => ({ ...p, section: e.target.value }))}
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
            >
              {['أ', 'ب', 'ج', 'د', 'هـ', 'و', 'ز', 'ح'].map(s => (
                <option key={s} value={s}>شعبة ({s})</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">رقم القيد:</label>
            <input
              type="text"
              value={newStudent.recordNumber || ''}
              onChange={e => setNewStudent(p => ({ ...p, recordNumber: e.target.value }))}
              placeholder="رقم القيد العام"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-mono font-black focus:border-emerald-600 outline-none"
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">رقم الصفحة بالسجل:</label>
            <input
              type="text"
              value={newStudent.registerPageNumber || ''}
              onChange={e => setNewStudent(p => ({ ...p, registerPageNumber: e.target.value }))}
              placeholder="رقم الصفحة"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-mono font-black focus:border-emerald-600 outline-none"
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">رقم هاتف ولي الأمر:</label>
            <input
              type="text"
              value={newStudent.guardianPhone || ''}
              onChange={e => setNewStudent(p => ({ ...p, guardianPhone: e.target.value }))}
              placeholder="مثال: 07701234567"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-mono font-black focus:border-emerald-600 outline-none dir-ltr"
            />
          </div>

          <div>
            <label className="block font-black text-slate-800 mb-1">محل السكن والعنوان:</label>
            <input
              type="text"
              value={newStudent.address || ''}
              onChange={e => setNewStudent(p => ({ ...p, address: e.target.value }))}
              placeholder="مثال: ديالى - بعقوبة"
              className="w-full p-2.5 rounded-xl border-2 border-slate-300 bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
            />
          </div>
        </div>

        <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-200">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 border-2 border-slate-300 text-xs font-black transition-all cursor-pointer"
          >
            إلغاء التراجع
          </button>
          <button
            type="submit"
            className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black shadow-lg transition-all cursor-pointer flex items-center gap-1.5"
          >
            <span>إضافة الطالب للسجل ➕</span>
          </button>
        </div>
      </form>
    </div>
    </Portal>
  );
};
