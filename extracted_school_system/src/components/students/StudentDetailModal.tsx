import React, { useState, useEffect } from 'react';
import { X, GraduationCap, Save, Edit3, Award, FileText } from 'lucide-react';
import { Student } from '../../types';
import { Portal } from '../common/Portal';

interface StudentDetailModalProps {
  student: Student | null;
  onClose: () => void;
  onUpdate: (updatedStudent: Student) => void;
}

export const StudentDetailModal: React.FC<StudentDetailModalProps> = ({
  student,
  onClose,
  onUpdate
}) => {
  const [draft, setDraft] = useState<Student | null>(student);
  const [saveSuccessMsg, setSaveSuccessMsg] = useState(false);

  useEffect(() => {
    setDraft(student);
  }, [student]);

  if (!student || !draft) return null;

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    onUpdate(draft);
    setSaveSuccessMsg(true);
    setTimeout(() => {
      setSaveSuccessMsg(false);
      onClose();
    }, 800);
  };

  return (
    <Portal>
      <div className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white border-2 border-emerald-500 rounded-3xl p-6 md:p-8 max-w-3xl w-full shadow-2xl space-y-5 my-8 text-slate-900">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-800 flex items-center justify-center font-black">
              <GraduationCap className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-lg font-black text-emerald-950">
                الملف الشامل وتعديل بيانات الطالب
              </h3>
              <p className="text-xs text-slate-600 font-bold">
                {draft.firstName} {draft.secondName} {draft.thirdName} {draft.fourthName || ''} {draft.titleName || ''} (قيد #{draft.recordNumber})
              </p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-slate-100 text-slate-700 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        {saveSuccessMsg && (
          <div className="p-3 bg-emerald-100 border-2 border-emerald-400 text-emerald-950 text-xs font-black rounded-xl text-center animate-bounce">
            تم حفظ وتحديث بيانات الطالب في السجلات بنجاح ✓
          </div>
        )}

        {/* Editable Form */}
        <form onSubmit={handleSave} className="space-y-4 text-xs">
          
          {/* Section 1: Names & Identity */}
          <div className="p-4 rounded-2xl bg-emerald-50/50 border-2 border-emerald-200 space-y-3">
            <h4 className="font-black text-sm text-emerald-950 border-b border-emerald-200 pb-1 flex items-center gap-1.5">
              <Edit3 className="w-4 h-4 text-emerald-700" />
              <span>1. الاسم الرباعي واللقب والهوية الوطنية</span>
            </h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <div>
                <label className="text-slate-800 font-bold block mb-1">الاسم الأول:</label>
                <input
                  type="text"
                  required
                  value={draft.firstName || ''}
                  onChange={e => setDraft({ ...draft, firstName: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">اسم الأب:</label>
                <input
                  type="text"
                  required
                  value={draft.secondName || ''}
                  onChange={e => setDraft({ ...draft, secondName: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">اسم الجد:</label>
                <input
                  type="text"
                  required
                  value={draft.thirdName || ''}
                  onChange={e => setDraft({ ...draft, thirdName: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">والد الجد / اللقب:</label>
                <input
                  type="text"
                  value={draft.fourthName || draft.titleName || ''}
                  onChange={e => setDraft({ ...draft, fourthName: e.target.value, titleName: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-emerald-600 outline-none"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">اسم الأم الثلاثي:</label>
                <input
                  type="text"
                  value={draft.motherName || ''}
                  onChange={e => setDraft({ ...draft, motherName: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-purple-900 font-black focus:border-emerald-600 outline-none"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">رقم البطاقة الموحدة:</label>
                <input
                  type="text"
                  value={draft.nationalCardNumber || ''}
                  onChange={e => setDraft({ ...draft, nationalCardNumber: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-blue-900 font-mono font-black focus:border-emerald-600 outline-none"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">رقم القيد:</label>
                <input
                  type="text"
                  value={draft.recordNumber || ''}
                  onChange={e => setDraft({ ...draft, recordNumber: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-mono font-black focus:border-emerald-600 outline-none"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">رقم الصفحة بالسجل:</label>
                <input
                  type="text"
                  value={draft.registerPageNumber || ''}
                  onChange={e => setDraft({ ...draft, registerPageNumber: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-mono font-black focus:border-emerald-600 outline-none"
                />
              </div>
            </div>
          </div>

          {/* Section 2: Academic Grade, Section, and Contacts */}
          <div className="p-4 rounded-2xl bg-blue-50/50 border-2 border-blue-200 space-y-3">
            <h4 className="font-black text-sm text-blue-950 border-b border-blue-200 pb-1">
              2. الصف، الشعبة، وبيانات التواصل والسكن
            </h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <div>
                <label className="text-slate-800 font-bold block mb-1">الصف الدراسي:</label>
                <input
                  type="text"
                  value={draft.currentGrade || ''}
                  onChange={e => setDraft({ ...draft, currentGrade: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-blue-600 outline-none"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">الشعبة:</label>
                <input
                  type="text"
                  value={draft.section || ''}
                  onChange={e => setDraft({ ...draft, section: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-blue-600 outline-none"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">هاتف ولي الأمر:</label>
                <input
                  type="text"
                  value={draft.guardianPhone || ''}
                  onChange={e => setDraft({ ...draft, guardianPhone: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-mono font-black focus:border-blue-600 outline-none dir-ltr"
                />
              </div>
              <div>
                <label className="text-slate-800 font-bold block mb-1">تقييم السلوك والانضباط:</label>
                <input
                  type="text"
                  value={draft.conductScore || '100'}
                  onChange={e => setDraft({ ...draft, conductScore: e.target.value })}
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-emerald-800 font-black focus:border-blue-600 outline-none"
                />
              </div>
              <div className="col-span-2 md:col-span-4">
                <label className="text-slate-800 font-bold block mb-1">محل السكن والعنوان التفصيلي:</label>
                <input
                  type="text"
                  value={draft.address || ''}
                  onChange={e => setDraft({ ...draft, address: e.target.value })}
                  placeholder="مثال: ديالى - بعقوبة - قرب نقطة دالة"
                  className="w-full p-2 border-2 border-slate-300 rounded-xl bg-white text-slate-950 font-black focus:border-blue-600 outline-none"
                />
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-200">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-black border-2 border-slate-300 cursor-pointer transition-all"
            >
              إلغاء التراجع
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black shadow-lg cursor-pointer transition-all flex items-center gap-1.5"
            >
              <Save className="w-4 h-4" />
              <span>حفظ تعديلات الطالب في السجل 💾</span>
            </button>
          </div>

        </form>

      </div>
    </div>
    </Portal>
  );
};
