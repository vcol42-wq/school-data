import React, { useState, useEffect, useRef } from 'react';
import { Student, StaffMember, ActiveView, AppTheme, AppConfig } from '../types';
import { 
  Mic, 
  MicOff, 
  Volume2, 
  Sparkles, 
  CheckCircle2, 
  Search, 
  UserPlus, 
  Users, 
  X, 
  BookOpen, 
  AlertCircle,
  Award,
  Radio,
  Zap,
  HelpCircle,
  Palette,
  ArrowRight,
  Loader2,
  Send,
  Home,
  Printer,
  CalendarDays,
  GraduationCap,
  BarChart3,
  Settings
} from 'lucide-react';

interface VoiceAssistantModalProps {
  isOpen: boolean;
  onClose: () => void;
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  staffList: StaffMember[];
  setStaffList: React.Dispatch<React.SetStateAction<StaffMember[]>>;
  setActiveView: (view: ActiveView) => void;
  setTheme?: (theme: AppTheme) => void;
  config?: AppConfig;
  onSelectStudent?: (student: Student) => void;
}

export const VoiceAssistantModal: React.FC<VoiceAssistantModalProps> = ({
  isOpen,
  onClose,
  students,
  setStudents,
  staffList,
  setStaffList,
  setActiveView,
  setTheme,
  config,
  onSelectStudent
}) => {
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [interimTranscript, setInterimTranscript] = useState('');
  const [textInputQuery, setTextInputQuery] = useState('');
  const [statusMessage, setStatusMessage] = useState('اضغط على المايك للتحدث، أو اكتب سؤالك بالأسفل للبحث بالذكاء الاصطناعي...');
  const [actionLog, setActionLog] = useState<Array<{ id: string; text: string; type: 'info' | 'success' | 'error' }>>([]);
  const [isAiProcessing, setIsAiProcessing] = useState(false);
  const [apiModelInfo, setApiModelInfo] = useState('Gemini 2.5 Flash Auto-Detected');
  
  const recognitionRef = useRef<any>(null);

  // Auto detect API key status
  useEffect(() => {
    const savedKey = localStorage.getItem('gemini_api_key') || localStorage.getItem('diyala_school_gemini_key');
    if (savedKey) {
      setApiModelInfo(`مربوط بمفتاح المستخدم المخصص (Gemini 2.5 Flash)`);
    } else {
      setApiModelInfo(`مربوط بالمفتاح الآلي للنظام (Gemini 2.5 Flash)`);
    }
  }, []);

  // Helper: Speak Arabic feedback
  const speakFeedback = (text: string) => {
    try {
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel(); // Stop prior speech
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'ar-IQ';
        utterance.rate = 0.95;
        window.speechSynthesis.speak(utterance);
      }
    } catch (e) {
      console.log('Text to Speech error:', e);
    }
  };

  // Helper: Add Log Message
  const addLog = (text: string, type: 'info' | 'success' | 'error' = 'info') => {
    setActionLog(prev => [{ id: String(Date.now()), text, type }, ...prev.slice(0, 8)]);
  };

  // Process Arabic Voice & Text AI Command Logic
  const processVoiceCommand = async (cmdText: string) => {
    const text = cmdText.trim();
    if (!text) return;

    const lowerText = text.toLowerCase();
    addLog(`الأمر المسموع / المكتوب: "${text}"`, 'info');

    // --- INSTANT ZERO-LATENCY LOCAL INTENT PATTERNS (0ms Execution) ---

    // 1. Theme Change Commands ("غير الثيم إلى ديالي", "ثيم كلاسيكي", "غير الألوان")
    if (setTheme && (lowerText.includes('ثيم') || lowerText.includes('اللون') || lowerText.includes('ألوان') || lowerText.includes('الوان'))) {
      let targetTheme: AppTheme | null = null;
      let themeNameArabic = '';

      if (lowerText.includes('ديالي') || lowerText.includes('تربية')) {
        targetTheme = 'diyala';
        themeNameArabic = 'تربية ديالى (أزرق سماوي)';
      } else if (lowerText.includes('كلاسيكي') || lowerText.includes('رسمي')) {
        targetTheme = 'classic';
        themeNameArabic = 'الكلاسيكي الملكي';
      } else if (lowerText.includes('حيوية') || lowerText.includes('حيوي')) {
        targetTheme = 'vibrant';
        themeNameArabic = 'الحيوي الجذاب';
      } else if (lowerText.includes('زمردي') || lowerText.includes('اخضر') || lowerText.includes('أخضر')) {
        targetTheme = 'emerald';
        themeNameArabic = 'الزمردي الأخضر';
      } else if (lowerText.includes('داكن') || lowerText.includes('اسود') || lowerText.includes('أمسيات')) {
        targetTheme = 'dark';
        themeNameArabic = 'الداكن الليلي';
      } else if (lowerText.includes('عنابي') || lowerText.includes('احمر') || lowerText.includes('أحمر')) {
        targetTheme = 'burgundy';
        themeNameArabic = 'العنابي المخملي';
      }

      if (targetTheme) {
        setTheme(targetTheme);
        const msg = `تم تغيير ثيم النظام فوراً إلى ثيم (${themeNameArabic})`;
        setStatusMessage(msg);
        addLog(msg, 'success');
        speakFeedback(`تم تغيير ثيم النظام إلى ${themeNameArabic}`);
        return;
      }
    }

    // 2. Navigation Commands (Instant Page Openers)
    if (lowerText.includes('جدول') || lowerText.includes('الدروس')) {
      setActiveView('schedule');
      setStatusMessage('تم الانتقال فوراً إلى جدول الدروس الأسبوعي');
      speakFeedback('تم فتح جدول الدروس');
      return;
    }

    if (lowerText.includes('كادر') || lowerText.includes('معلمين') || lowerText.includes('الموظفين') || lowerText.includes('إضافة كادر')) {
      setActiveView('staff');
      setStatusMessage('تم الانتقال فوراً إلى سجل الكادر والموظفين');
      speakFeedback('تم فتح سجل الكادر الموحد');
      return;
    }

    if (lowerText.includes('أرشيف') || lowerText.includes('ارشيف') || lowerText.includes('المغادرين') || lowerText.includes('سابقين')) {
      setActiveView('former_students');
      setStatusMessage('تم الانتقال فوراً إلى أرشيف الطلاب المغادرين');
      speakFeedback('تم فتح أرشيف الطلاب المغادرين');
      return;
    }

    if (lowerText.includes('طلاب') || lowerText.includes('الطلاب') || lowerText.includes('إضافة طالب')) {
      setActiveView('students');
      setStatusMessage('تم الانتقال فوراً إلى سجل الطلاب العام');
      speakFeedback('تم فتح سجل الطلاب');
      return;
    }

    if (lowerText.includes('إحصائيات') || lowerText.includes('احصائيات') || lowerText.includes('الملاك')) {
      setActiveView('stats');
      setStatusMessage('تم فتح قسم الإحصاءات والملاك الرسمي');
      speakFeedback('تم فتح الإحصائيات');
      return;
    }

    if (lowerText.includes('طباعة') || lowerText.includes('وثائق') || lowerText.includes('شهادات')) {
      setActiveView('print');
      setStatusMessage('تم فتح مركز طباعة الوثائق الرسمية');
      speakFeedback('تم فتح مركز الطباعة');
      return;
    }

    if (lowerText.includes('إعدادات') || lowerText.includes('اعدادات')) {
      setActiveView('settings');
      setStatusMessage('تم فتح إعدادات المدرسة والنظام');
      speakFeedback('تم فتح الإعدادات');
      return;
    }

    if (lowerText.includes('منبه') || lowerText.includes('جرس')) {
      setActiveView('alarm');
      setStatusMessage('تم فتح نظام المنبه والجرس التلقائي');
      speakFeedback('تم فتح المنبه والجرس');
      return;
    }

    if (lowerText.includes('خطوط') || lowerText.includes('الخط')) {
      setActiveView('fonts');
      setStatusMessage('تم فتح تخصيص الخطوط الرسمية');
      speakFeedback('تم فتح تخصيص الخطوط');
      return;
    }

    if (lowerText.includes('ثيمات') || lowerText.includes('الألوان')) {
      setActiveView('themes');
      setStatusMessage('تم فتح تخصيص ثيمات النظام');
      speakFeedback('تم فتح ثيمات النظام');
      return;
    }

    if (lowerText.includes('رئيسية') || lowerText.includes('الرئيسية') || lowerText.includes('سطح المكتب') || lowerText.includes('هوم')) {
      setActiveView('launcher');
      setStatusMessage('تم العودة إلى الشاشة الرئيسية (سطح المكتب)');
      speakFeedback('تم فتح الشاشة الرئيسية');
      return;
    }

    // 3. Search Commands for Students & Staff
    if (lowerText.includes('ابحث') || lowerText.includes('بحث') || lowerText.includes('جد') || lowerText.includes('من هو')) {
      const nameQuery = lowerText
        .replace(/ابحث عن/g, '')
        .replace(/بحث عن/g, '')
        .replace(/ابحث/g, '')
        .replace(/بحث/g, '')
        .replace(/عن/g, '')
        .trim();

      if (nameQuery) {
        const foundStudent = students.find(s => 
          s.firstName.toLowerCase().includes(nameQuery) ||
          s.recordNumber.includes(nameQuery)
        );

        if (foundStudent) {
          setActiveView('students');
          if (onSelectStudent) onSelectStudent(foundStudent);
          
          const msg = `تم العثور على الطالب ${foundStudent.firstName}، الصف ${foundStudent.currentGrade}، الشعبة ${foundStudent.section}، الدرجة: ${foundStudent.finalYearScore || 'لم تحدد'}`;
          setStatusMessage(msg);
          addLog(msg, 'success');
          speakFeedback(`تم العثور على الطالب ${foundStudent.firstName}`);
          return;
        }

        // Search in Staff
        const foundStaff = staffList.find(s => s.fullName.toLowerCase().includes(nameQuery));
        if (foundStaff) {
          setActiveView('staff');
          const msg = `تم العثور على الموظف ${foundStaff.fullName}، الاختصاص: ${foundStaff.specialization}، الصفة: ${foundStaff.role}`;
          setStatusMessage(msg);
          addLog(msg, 'success');
          speakFeedback(`تم العثور على الموظف ${foundStaff.fullName}`);
          return;
        }
      }
    }

    // 4. Grade Edit Commands
    if (lowerText.includes('درجة') || lowerText.includes('الدرجة') || lowerText.includes('درجه')) {
      const numbers = lowerText.match(/\d+/g);
      const score = numbers ? parseInt(numbers[0], 10) : null;

      if (score !== null && score >= 0 && score <= 100) {
        const cleanWords = lowerText
          .replace(/أضف|اضف|تعديل|غير|درجة|الدرجة|درجه|في|إلى|الي|للطالب|طالب/g, ' ')
          .replace(/\d+/g, ' ')
          .trim()
          .split(/\s+/)
          .filter(w => w.length > 2);

        let targetStudent = students.find(s => cleanWords.some(w => s.firstName.toLowerCase().includes(w)));

        if (targetStudent) {
          if (targetStudent.isLockedAndSynced) {
            const lockMsg = `عذراً، درجات الطالب ${targetStudent.firstName} مقفولة سحابياً رسمياً ولا يمكن التعديل عليها.`;
            setStatusMessage(lockMsg);
            addLog(lockMsg, 'error');
            speakFeedback(`درجات الطالب ${targetStudent.firstName} مقفولة سحابياً`);
            return;
          }

          setStudents(prev => prev.map(s => s.id === targetStudent!.id ? {
            ...s,
            finalYearScore: score,
            previousYearResult: score >= 50 ? `ناجح (${score})` : `راسب (${score})`
          } : s));

          setActiveView('students');
          const successMsg = `تم بنجاح إضافة وتعديل درجة الطالب ${targetStudent.firstName} إلى (${score})`;
          setStatusMessage(successMsg);
          addLog(successMsg, 'success');
          speakFeedback(`تم تسجيل درجة الطالب ${targetStudent.firstName} بنجاح`);
          return;
        }
      }
    }

    // --- STEP 2: GEMINI 2.5 FLASH DEEP AI RESPONSE FOR COMPLEX QUESTIONS ---
    try {
      setIsAiProcessing(true);
      setStatusMessage('✨ جاري الاستجابة والبحث بالذكاء الاصطناعي (Gemini 2.5 Flash)...');

      const userSavedApiKey = localStorage.getItem('gemini_api_key') || localStorage.getItem('diyala_school_gemini_key') || '';

      const res = await fetch('/api/ai-assistant', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: text,
          studentsCount: students.length,
          staffCount: staffList.length,
          schoolName: config?.schoolName || 'م. كعب بن مالك المسائية للبنين',
          userApiKey: userSavedApiKey
        })
      });

      const data = await res.json();
      setIsAiProcessing(false);

      if (data.success) {
        const aiResponse = data.responseText || 'تم تنفيذ الطلب بالذكاء الاصطناعي.';
        setStatusMessage(`✨ الذكاء الاصطناعي: ${aiResponse}`);
        addLog(`إجابة الذكاء الاصطناعي: ${aiResponse}`, 'success');
        speakFeedback(aiResponse);

        if (data.action === 'NAVIGATE' && data.targetView) {
          setActiveView(data.targetView);
        } else if (data.action === 'CHANGE_THEME' && data.targetTheme && setTheme) {
          setTheme(data.targetTheme);
        }
      } else {
        const fallbackMsg = `إجابة سريعة: تم استلام الاستفسار عن "${text}". يمكن التنقل بالسجلات أو البحث عن الأسماء مباشرة.`;
        setStatusMessage(fallbackMsg);
        addLog(fallbackMsg, 'info');
      }
    } catch (err: any) {
      setIsAiProcessing(false);
      console.log('AI Endpoint Error:', err);
      const fallbackMsg = `تم المعالجة: "${text}". ينصح بتجربة أوامر سريعة مثل "افتح مركز الطباعة" أو "غير الثيم لـ ديالي".`;
      setStatusMessage(fallbackMsg);
      addLog(fallbackMsg, 'info');
    }
  };

  // Auto-start listening on modal open
  useEffect(() => {
    if (isOpen) {
      startListening();
    } else {
      stopListening();
    }
  }, [isOpen]);

  // Initialize Web Speech Recognition with Fallback
  const startListening = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setStatusMessage('ملاحظة: يمكنك استخدام خانة البحث الكتابي بالأسفل للبحث الفوري بالذكاء الاصطناعي!');
      setIsListening(false);
      return;
    }

    try {
      if (recognitionRef.current) {
        try { recognitionRef.current.stop(); } catch(e){}
      }

      const recognition = new SpeechRecognition();
      recognition.lang = 'ar-IQ'; // Iraqi Arabic dialect
      recognition.continuous = true;
      recognition.interimResults = true;

      recognition.onstart = () => {
        setIsListening(true);
        setStatusMessage('جاري الاستماع الآن بذكاء فائق... تحدث بصوت واضح باللغة العربية');
      };

      recognition.onresult = (event: any) => {
        let currentInterim = '';
        let finalTranscriptStr = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTranscriptStr += event.results[i][0].transcript;
          } else {
            currentInterim += event.results[i][0].transcript;
          }
        }

        setInterimTranscript(currentInterim);

        if (finalTranscriptStr) {
          setTranscript(finalTranscriptStr);
          processVoiceCommand(finalTranscriptStr);
        }
      };

      recognition.onerror = (event: any) => {
        console.log('Speech recognition error:', event.error);
        setIsListening(false);
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognition;
      recognition.start();

    } catch (e: any) {
      console.log('Failed to start speech recognition:', e);
      setIsListening(false);
    }
  };

  const stopListening = () => {
    if (recognitionRef.current) {
      try { recognitionRef.current.stop(); } catch(e){}
      setIsListening(false);
    }
  };

  const handleManualQuerySubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!textInputQuery.trim()) return;
    processVoiceCommand(textInputQuery);
    setTextInputQuery('');
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 transition-all">
      <div className="bg-white text-slate-950 border-4 border-amber-400 rounded-3xl p-6 max-w-xl w-full shadow-2xl space-y-5 relative overflow-hidden font-sans">
        
        {/* Glow Header */}
        <div className="flex items-center justify-between border-b border-amber-300 pb-3">
          <div className="flex flex-col">
            <div className="flex items-center gap-2 text-amber-900 font-black">
              <Sparkles className="w-6 h-6 animate-pulse text-amber-600 shrink-0" />
              <span className="text-base md:text-lg font-black text-amber-900">المساعد الصوتي والبحث بالذكاء الاصطناعي</span>
            </div>
            <span className="text-[11px] text-slate-800 font-extrabold mt-0.5 flex items-center gap-1">
              <Zap className="w-3.5 h-3.5 text-amber-600" />
              <span className="text-amber-900 font-black">{apiModelInfo}</span>
            </span>
          </div>
          <button 
            type="button"
            onClick={onClose} 
            className="p-2 rounded-xl bg-slate-100 hover:bg-rose-100 text-slate-700 hover:text-rose-700 transition-colors border border-slate-300 cursor-pointer"
          >
            <X className="w-5 h-5 stroke-[3]" />
          </button>
        </div>

        {/* AI SMART TEXT SEARCH INPUT BOX */}
        <form onSubmit={handleManualQuerySubmit} className="relative">
          <div className="flex items-center gap-2 bg-slate-900 text-white p-2 rounded-2xl border-2 border-amber-400 shadow-md">
            <Search className="w-5 h-5 text-amber-400 shrink-0 ml-2" />
            <input
              type="text"
              value={textInputQuery}
              onChange={(e) => setTextInputQuery(e.target.value)}
              placeholder="اكتب سؤالك، ابحث عن اسم، غير ثيم، أو اطلب فتح صفحة..."
              className="bg-transparent text-sm font-black text-white placeholder-slate-400 focus:outline-none flex-1 font-sans"
            />
            <button
              type="submit"
              disabled={isAiProcessing}
              className="bg-amber-400 hover:bg-amber-300 text-slate-950 font-black px-4 py-2 rounded-xl flex items-center gap-1 text-xs shadow-md transition-all active:scale-95 cursor-pointer"
            >
              {isAiProcessing ? (
                <Loader2 className="w-4 h-4 animate-spin text-slate-950" />
              ) : (
                <>
                  <span className="font-black">بحث ذكي</span>
                  <Send className="w-3.5 h-3.5" />
                </>
              )}
            </button>
          </div>
        </form>

        {/* MICROPHONE ACTIVE VISUALIZER BUTTON */}
        <div className="flex flex-col items-center justify-center space-y-3 py-2">
          
          <button
            type="button"
            onClick={isListening ? stopListening : startListening}
            className={`w-24 h-24 rounded-full flex items-center justify-center transition-all duration-300 shadow-2xl cursor-pointer relative ${
              isListening 
                ? 'bg-rose-600 text-white shadow-rose-600/50 scale-110 ring-8 ring-rose-500/30 animate-pulse' 
                : 'bg-gradient-to-tr from-amber-500 via-amber-400 to-amber-500 text-slate-950 hover:scale-105 shadow-amber-500/40 ring-4 ring-amber-400/20'
            }`}
          >
            {isListening ? (
              <Mic className="w-10 h-10 animate-bounce text-white" />
            ) : (
              <MicOff className="w-10 h-10 text-slate-950" />
            )}
            
            {isListening && (
              <span className="absolute -top-1 -right-1 flex h-4 w-4">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-4 w-4 bg-rose-500"></span>
              </span>
            )}
          </button>

          <span className="text-xs font-black text-slate-950">
            {isListening ? 'جاري الاستماع الفوري... (اضغط للإيقاف)' : 'اضغط على المايك ثم تحدث بأي أمر أو سؤال باللغة العربية'}
          </span>

        </div>

        {/* TRANSCRIPT & STATUS BOX - HIGH CONTRAST PITCH BLACK TEXT ON AMBER PASTEL */}
        <div className="p-4 rounded-2xl bg-amber-50 text-slate-950 space-y-2 border-2 border-amber-300 shadow-sm">
          <div className="flex items-center justify-between text-xs font-black border-b border-amber-200 pb-2 text-amber-950">
            <span className="flex items-center gap-1.5 text-amber-950 font-black">
              <Radio className={`w-4 h-4 ${isListening ? 'text-rose-600 animate-ping' : 'text-amber-700'}`} />
              <span>حالة الاستجابة المباشرة:</span>
            </span>
            <span className="text-emerald-800 font-black font-mono text-xs">استجابة فائقة السرعة Low-Latency</span>
          </div>

          <p className="text-sm md:text-base font-black text-slate-950 min-h-[40px] flex items-center leading-relaxed">
            {interimTranscript || transcript || statusMessage}
          </p>
        </div>

        {/* SUGGESTED VOICE COMMAND CARDS - HIGH CONTRAST PASTEL CARDS */}
        <div className="space-y-2">
          <h4 className="text-xs font-black text-slate-900 flex items-center gap-1">
            <HelpCircle className="w-4 h-4 text-amber-600" />
            <span>أمثلة للأوامر الصوتية والبحث الذكي:</span>
          </h4>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs font-bold">
            <div className="p-3 rounded-2xl bg-amber-100/90 border-2 border-amber-300 text-slate-950 shadow-xs">
              <strong className="text-amber-950 font-black block mb-1 text-xs">🔍 البحث عن طالب أو كادر:</strong>
              <span className="text-slate-950 font-extrabold text-[11px] block">"ابحث عن عباس حسن محمد"</span>
            </div>

            <div className="p-3 rounded-2xl bg-emerald-100/90 border-2 border-emerald-300 text-slate-950 shadow-xs">
              <strong className="text-emerald-950 font-black block mb-1 text-xs">✏️ رصد وإضافة الدرجات:</strong>
              <span className="text-slate-950 font-extrabold text-[11px] block">"أضف درجة العربي 85 لعباس"</span>
            </div>

            <div className="p-3 rounded-2xl bg-sky-100/90 border-2 border-sky-300 text-slate-950 shadow-xs">
              <strong className="text-sky-950 font-black block mb-1 text-xs">📂 التنقل المباشر بالسجلات:</strong>
              <span className="text-slate-950 font-extrabold text-[11px] block">"افتح جدول الدروس" أو "سجل الكادر"</span>
            </div>

            <div className="p-3 rounded-2xl bg-purple-100/90 border-2 border-purple-300 text-slate-950 shadow-xs">
              <strong className="text-purple-950 font-black block mb-1 text-xs">📊 الإحصائيات والمركز:</strong>
              <span className="text-slate-950 font-extrabold text-[11px] block">"افتح الإحصائيات المدرسية"</span>
            </div>
          </div>
        </div>

        {/* QUICK ACTION CHIPS */}
        <div className="flex flex-wrap gap-1.5 text-xs font-bold pt-1 border-t border-slate-200">
          <button 
            type="button"
            onClick={() => processVoiceCommand('غير الثيم إلى ديالي')}
            className="p-1.5 px-3 rounded-xl bg-slate-900 text-amber-300 hover:bg-amber-400 hover:text-slate-950 transition-all cursor-pointer flex items-center gap-1 font-black shadow-xs border border-slate-800"
          >
            <Palette className="w-3.5 h-3.5 text-amber-400" />
            <span>ثيم ديالي</span>
          </button>

          <button 
            type="button"
            onClick={() => processVoiceCommand('غير الثيم إلى كلاسيكي')}
            className="p-1.5 px-3 rounded-xl bg-slate-900 text-amber-300 hover:bg-amber-400 hover:text-slate-950 transition-all cursor-pointer flex items-center gap-1 font-black shadow-xs border border-slate-800"
          >
            <Palette className="w-3.5 h-3.5 text-amber-400" />
            <span>ثيم كلاسيكي</span>
          </button>

          <button 
            type="button"
            onClick={() => processVoiceCommand('افتح مركز الطباعة')}
            className="p-1.5 px-3 rounded-xl bg-slate-900 text-amber-300 hover:bg-amber-400 hover:text-slate-950 transition-all cursor-pointer flex items-center gap-1 font-black shadow-xs border border-slate-800"
          >
            <Printer className="w-3.5 h-3.5 text-amber-400" />
            <span>مركز الطباعة</span>
          </button>

          <button 
            type="button"
            onClick={() => processVoiceCommand('افتح سجل الكادر')}
            className="p-1.5 px-3 rounded-xl bg-slate-900 text-amber-300 hover:bg-amber-400 hover:text-slate-950 transition-all cursor-pointer flex items-center gap-1 font-black shadow-xs border border-slate-800"
          >
            <Users className="w-3.5 h-3.5 text-amber-400" />
            <span>سجل الكادر</span>
          </button>

          <button 
            type="button"
            onClick={() => processVoiceCommand('افتح جدول الدروس')}
            className="p-1.5 px-3 rounded-xl bg-slate-900 text-amber-300 hover:bg-amber-400 hover:text-slate-950 transition-all cursor-pointer flex items-center gap-1 font-black shadow-xs border border-slate-800"
          >
            <CalendarDays className="w-3.5 h-3.5 text-amber-400" />
            <span>جدول الدروس</span>
          </button>

          <button 
            type="button"
            onClick={() => processVoiceCommand('افتح الإحصائيات')}
            className="p-1.5 px-3 rounded-xl bg-slate-900 text-amber-300 hover:bg-amber-400 hover:text-slate-950 transition-all cursor-pointer flex items-center gap-1 font-black shadow-xs border border-slate-800"
          >
            <BarChart3 className="w-3.5 h-3.5 text-amber-400" />
            <span>الإحصائيات</span>
          </button>
        </div>

        {/* LOG OF ACTIONS */}
        {actionLog.length > 0 && (
          <div className="space-y-1 pt-2 border-t border-slate-200">
            <span className="text-[10px] font-black text-slate-800">سجل الأوامر المنفذة مؤخراً:</span>
            <div className="max-h-20 overflow-y-auto text-[11px] space-y-1 dir-rtl font-bold">
              {actionLog.map(log => (
                <div 
                  key={log.id} 
                  className={`p-1.5 rounded-lg flex items-center justify-between font-extrabold ${
                    log.type === 'success' 
                      ? 'bg-emerald-100 text-emerald-950 border border-emerald-300' 
                      : log.type === 'error'
                      ? 'bg-rose-100 text-rose-950 border border-rose-300'
                      : 'bg-slate-100 text-slate-950 border border-slate-300'
                  }`}
                >
                  <span>{log.text}</span>
                  <CheckCircle2 className="w-3.5 h-3.5 shrink-0 text-emerald-700" />
                </div>
              ))}
            </div>
          </div>
        )}

      </div>
    </div>
  );
};
