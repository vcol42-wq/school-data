import React, { useState } from 'react';
import { Student } from '../../types';
import { Portal } from '../common/Portal';
import { 
  X, 
  Split, 
  Users, 
  CheckCircle2, 
  ArrowRight, 
  Sparkles, 
  SortAsc, 
  Shuffle, 
  Layers,
  HelpCircle
} from 'lucide-react';

interface SectionDividerModalProps {
  isOpen: boolean;
  onClose: () => void;
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
}

export const SectionDividerModal: React.FC<SectionDividerModalProps> = ({
  isOpen,
  onClose,
  students,
  setStudents
}) => {
  if (!isOpen) return null;

  const uniqueGrades = Array.from(new Set(students.map(s => s.currentGrade).filter(Boolean)));
  const [selectedGrade, setSelectedGrade] = useState<string>(uniqueGrades[0] || 'الصف الأول');
  const [selectedSections, setSelectedSections] = useState<string[]>(['أ', 'ب', 'ج']);
  const [newSectionInput, setNewSectionInput] = useState<string>('');
  const [divisionMethod, setDivisionMethod] = useState<'alphabetical' | 'round_robin' | 'random'>('alphabetical');
  const [previewResult, setPreviewResult] = useState<Record<string, Student[]> | null>(null);
  const [successMsg, setSuccessMsg] = useState<string>('');

  // Get active continuing students in the selected grade
  const targetStudents = students.filter(
    s => s.currentGrade === selectedGrade && ['active', 'مستمر', 'muted'].includes(s.status)
  );

  const handleToggleSection = (sec: string) => {
    if (selectedSections.includes(sec)) {
      if (selectedSections.length <= 1) {
        alert('يجب الإبقاء على شعبة واحدة على الأقل.');
        return;
      }
      setSelectedSections(prev => prev.filter(s => s !== sec));
    } else {
      setSelectedSections(prev => [...prev, sec]);
    }
  };

  const handleAddCustomSection = () => {
    const trimmed = newSectionInput.trim();
    if (!trimmed) return;
    if (selectedSections.includes(trimmed)) {
      alert('هذه الشعبة موجودة مسبقاً.');
      return;
    }
    setSelectedSections(prev => [...prev, trimmed]);
    setNewSectionInput('');
  };

  const MAX_STUDENTS_PER_SECTION = 60;
  const minSectionsRequired = Math.ceil(targetStudents.length / MAX_STUDENTS_PER_SECTION);

  // Generate preview of division
  const handleCalculateDivision = () => {
    if (targetStudents.length === 0) {
      alert(`لا يوجد طلاب مسجلون في ${selectedGrade}.`);
      return;
    }

    if (targetStudents.length > selectedSections.length * MAX_STUDENTS_PER_SECTION) {
      alert(`تنبيه النظام المعتمد: الحد الأعلى لسعة الشعبة هو ${MAX_STUDENTS_PER_SECTION} طالباً. الصف يحتوي على ${targetStudents.length} طالباً، لذا تحتاج إلى ${minSectionsRequired} شعب على الأقل لتفادي تجاوز السعة المحددة.`);
    }

    let sorted = [...targetStudents];

    if (divisionMethod === 'alphabetical') {
      sorted.sort((a, b) => {
        const nameA = `${a.firstName} ${a.secondName || ''} ${a.thirdName || ''}`;
        const nameB = `${b.firstName} ${b.secondName || ''} ${b.thirdName || ''}`;
        return nameA.localeCompare(nameB, 'ar');
      });
    } else if (divisionMethod === 'random') {
      sorted.sort(() => Math.random() - 0.5);
    }

    const result: Record<string, Student[]> = {};
    selectedSections.forEach(sec => {
      result[sec] = [];
    });

    if (divisionMethod === 'alphabetical') {
      // Chunk evenly
      const countPerSec = Math.ceil(sorted.length / selectedSections.length);
      selectedSections.forEach((sec, idx) => {
        const start = idx * countPerSec;
        const end = start + countPerSec;
        result[sec] = sorted.slice(start, end);
      });
    } else {
      // Round robin / interleaved
      sorted.forEach((std, idx) => {
        const sec = selectedSections[idx % selectedSections.length];
        result[sec].push(std);
      });
    }

    setPreviewResult(result);
  };

  // Apply division to global students state
  const handleApplyDivision = () => {
    if (!previewResult) return;

    // Check if any section exceeds max limit
    const exceeded = Object.entries(previewResult).some(([_, list]) => (list as Student[]).length > MAX_STUDENTS_PER_SECTION);
    if (exceeded) {
      if (!confirm(`تنبيه: إحدى الشعب تتجاوز الحد الأقصى المقرر (${MAX_STUDENTS_PER_SECTION} طالباً). هل أنت متأكد من المتابعة رغم التحذير؟`)) {
        return;
      }
    }

    const studentToSectionMap = new Map<string, string>();
    Object.entries(previewResult).forEach(([sec, list]) => {
      (list as Student[]).forEach(std => {
        studentToSectionMap.set(std.id, sec);
      });
    });

    setStudents(prev => prev.map(s => {
      if (studentToSectionMap.has(s.id)) {
        return {
          ...s,
          section: studentToSectionMap.get(s.id)!
        };
      }
      return s;
    }));

    setSuccessMsg(`تم تقسيم ${targetStudents.length} طالباً في (${selectedGrade}) على ${selectedSections.length} شعب بنجاح (سقف الشعبة: ${MAX_STUDENTS_PER_SECTION} طالب)! 🎉`);
    setTimeout(() => {
      setSuccessMsg('');
      onClose();
    }, 2000);
  };

  return (
    <Portal>
      <div 
        onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-sm animate-fade-in dir-rtl"
      >
        <div className="bg-white w-full max-w-4xl max-h-[90vh] rounded-3xl border-4 border-indigo-600 shadow-2xl flex flex-col overflow-hidden">
        
        {/* Header */}
        <div className="p-5 bg-gradient-to-r from-indigo-900 via-indigo-800 to-blue-900 text-white flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-amber-400 text-slate-950 shadow-md">
              <Split className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-lg font-black">أداة تقسيم وتوزيع الطلاب على الشعب الذكية</h3>
              <p className="text-xs text-indigo-200">توزيع متوازن وأبجدي عادل لطلاب الصف الدراسي بنقرة واحدة</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white cursor-pointer transition-all"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Success Alert */}
        {successMsg && (
          <div className="p-3.5 bg-emerald-100 border-b border-emerald-300 text-emerald-950 font-black text-xs text-center flex items-center justify-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            <span>{successMsg}</span>
          </div>
        )}

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1 text-xs">
          
          {/* Step 1: Choose Grade */}
          <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 space-y-2">
            <div className="flex items-center justify-between">
              <label className="font-black text-slate-800 flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-indigo-600 text-white flex items-center justify-center text-[10px]">1</span>
                <span>اختر الصف الدراسي المراد تقسيمه:</span>
              </label>
              <span className="font-black text-indigo-700 bg-indigo-50 border border-indigo-200 px-3 py-1 rounded-xl">
                إجمالي طلاب الصف: {targetStudents.length} طالب
              </span>
            </div>

            <select
              value={selectedGrade}
              onChange={e => {
                setSelectedGrade(e.target.value);
                setPreviewResult(null);
              }}
              className="w-full p-2.5 rounded-xl border-2 border-indigo-200 font-black text-slate-900 bg-white outline-none focus:border-indigo-600"
            >
              {uniqueGrades.map(g => (
                <option key={g} value={g}>{g}</option>
              ))}
            </select>
          </div>

          {/* Step 2: Choose Target Sections */}
          <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 space-y-3">
            <label className="font-black text-slate-800 flex items-center gap-1.5">
              <span className="w-5 h-5 rounded-full bg-indigo-600 text-white flex items-center justify-center text-[10px]">2</span>
              <span>حدد الشعب المستهدفة للتوزيع (اضغط لتفعيل أو تعطيل):</span>
            </label>

            <div className="flex flex-wrap items-center gap-2">
              {['أ', 'ب', 'ج', 'د', 'هـ', 'و', 'ز', 'ح'].map(sec => {
                const isSelected = selectedSections.includes(sec);
                return (
                  <button
                    key={sec}
                    type="button"
                    onClick={() => {
                      handleToggleSection(sec);
                      setPreviewResult(null);
                    }}
                    className={`px-4 py-2 rounded-xl font-black text-xs transition-all cursor-pointer border ${
                      isSelected 
                        ? 'bg-indigo-600 text-white border-indigo-600 shadow-md scale-105' 
                        : 'bg-white text-slate-600 border-slate-300 hover:bg-slate-100'
                    }`}
                  >
                    شعبة ({sec}) {isSelected ? '✓' : ''}
                  </button>
                );
              })}
            </div>

            {/* Add custom section */}
            <div className="flex items-center gap-2 pt-2">
              <input
                type="text"
                value={newSectionInput}
                onChange={e => setNewSectionInput(e.target.value)}
                placeholder="إضافة اسم شعبة مخصصة (مثال: أ1 أو ز)..."
                className="p-2 rounded-xl border border-slate-300 bg-white font-bold text-xs flex-1 outline-none"
              />
              <button
                type="button"
                onClick={handleAddCustomSection}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-xl font-black text-xs cursor-pointer transition-all"
              >
                + إضافة شعبة
              </button>
            </div>
          </div>

          {/* Step 3: Division Method */}
          <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 space-y-3">
            <label className="font-black text-slate-800 flex items-center gap-1.5">
              <span className="w-5 h-5 rounded-full bg-indigo-600 text-white flex items-center justify-center text-[10px]">3</span>
              <span>اختر معيار وطريقة التقسيم:</span>
            </label>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <button
                type="button"
                onClick={() => { setDivisionMethod('alphabetical'); setPreviewResult(null); }}
                className={`p-3.5 rounded-2xl border text-right transition-all cursor-pointer flex flex-col justify-between ${
                  divisionMethod === 'alphabetical'
                    ? 'bg-indigo-50 border-indigo-500 ring-2 ring-indigo-200 text-indigo-950 font-black'
                    : 'bg-white border-slate-200 text-slate-700 font-bold hover:bg-slate-100'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-black">أبجدي متسلسل (أ ⬅️ ي)</span>
                  <SortAsc className="w-4 h-4 text-indigo-600" />
                </div>
                <p className="text-[11px] text-slate-500 font-normal">
                  فرز الأسماء هجائياً ثم تقسيمها لكتل متساوية على الشعب
                </p>
              </button>

              <button
                type="button"
                onClick={() => { setDivisionMethod('round_robin'); setPreviewResult(null); }}
                className={`p-3.5 rounded-2xl border text-right transition-all cursor-pointer flex flex-col justify-between ${
                  divisionMethod === 'round_robin'
                    ? 'bg-indigo-50 border-indigo-500 ring-2 ring-indigo-200 text-indigo-950 font-black'
                    : 'bg-white border-slate-200 text-slate-700 font-bold hover:bg-slate-100'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-black">تدويري متناوب (أ، ب، ج...)</span>
                  <Layers className="w-4 h-4 text-blue-600" />
                </div>
                <p className="text-[11px] text-slate-500 font-normal">
                  توزيع طالب تلو الآخر بالتناوب لضمان تنوع الحروف في كل شعبة
                </p>
              </button>

              <button
                type="button"
                onClick={() => { setDivisionMethod('random'); setPreviewResult(null); }}
                className={`p-3.5 rounded-2xl border text-right transition-all cursor-pointer flex flex-col justify-between ${
                  divisionMethod === 'random'
                    ? 'bg-indigo-50 border-indigo-500 ring-2 ring-indigo-200 text-indigo-950 font-black'
                    : 'bg-white border-slate-200 text-slate-700 font-bold hover:bg-slate-100'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-black">عشوائي متكافئ</span>
                  <Shuffle className="w-4 h-4 text-emerald-600" />
                </div>
                <p className="text-[11px] text-slate-500 font-normal">
                  خلط عشوائي وتوزيع أعداد متساوية تماماً بين الشعب
                </p>
              </button>
            </div>
          </div>

          {/* Action: Generate Preview */}
          <div className="text-center pt-2">
            <button
              type="button"
              onClick={handleCalculateDivision}
              className="px-8 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-black text-sm transition-all shadow-lg hover:scale-105 active:scale-95 cursor-pointer flex items-center gap-2 mx-auto"
            >
              <Sparkles className="w-5 h-5 text-amber-300" />
              <span>معاينة نتيجة التقسيم قبل الحفظ 🔍</span>
            </button>
          </div>

          {/* Step 4: Preview Result Cards */}
          {previewResult && (
            <div className="space-y-4 pt-4 border-t-2 border-indigo-100 animate-fade-in">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-black text-slate-900">
                  نتيجة التوزيع المقترحة ({selectedGrade}):
                </h4>
                <span className="text-xs text-slate-500 font-bold">
                  متوسط الشعبة: {Math.round(targetStudents.length / selectedSections.length)} طالب
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {Object.entries(previewResult).map(([sec, rawList]) => {
                  const list = rawList as Student[];
                  return (
                  <div key={sec} className="p-4 rounded-2xl bg-white border-2 border-indigo-200 shadow-sm space-y-3">
                    <div className="flex items-center justify-between border-b pb-2">
                      <span className="font-black text-sm text-indigo-700">شعبة ({sec})</span>
                      <span className={`px-2.5 py-0.5 rounded-full font-mono font-black text-xs ${
                        list.length > MAX_STUDENTS_PER_SECTION 
                          ? 'bg-red-100 text-red-700 border border-red-300' 
                          : 'bg-indigo-100 text-indigo-900'
                      }`}>
                        {list.length} / {MAX_STUDENTS_PER_SECTION} طالب {list.length > MAX_STUDENTS_PER_SECTION ? '⚠️' : '✓'}
                      </span>
                    </div>

                    <div className="max-h-48 overflow-y-auto space-y-1 font-medium text-[11px] text-slate-700 pr-1">
                      {list.map((std, i) => (
                        <div key={std.id} className="flex items-center justify-between py-1 border-b border-slate-100">
                          <span className="font-mono text-slate-400 text-[10px] ml-1">{i + 1}.</span>
                          <span className="truncate flex-1 text-right">{std.firstName} {std.secondName} {std.thirdName}</span>
                          <span className="text-[10px] text-slate-400 font-mono">#{std.recordNumber}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
              </div>
            </div>
          )}

        </div>

        {/* Modal Footer */}
        <div className="p-4 bg-slate-50 border-t flex items-center justify-between shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-700 font-black text-xs cursor-pointer transition-all"
          >
            إلغاء
          </button>

          {previewResult && (
            <button
              type="button"
              onClick={handleApplyDivision}
              className="px-8 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs shadow-lg transition-all cursor-pointer flex items-center gap-2"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>اعتماد وحفظ توزيع الشعب في السجلات ✓</span>
            </button>
          )}
        </div>

      </div>
    </div>
  </Portal>
  );
};
