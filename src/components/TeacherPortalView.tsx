import React, { useState, useEffect } from 'react';
import { Student, StaffMember, AppConfig } from '../types';
import { 
  Cloud, 
  CloudCheck, 
  Mic, 
  MicOff, 
  Camera, 
  Upload, 
  CheckCircle2, 
  Sparkles, 
  BookOpen, 
  User, 
  Layers, 
  LogOut, 
  Send, 
  FileText, 
  ScanLine, 
  RefreshCw, 
  X, 
  AlertCircle, 
  Sliders, 
  Globe, 
  ShieldCheck,
  KeyRound,
  Lock,
  Unlock,
  Copy,
  Check,
  Notebook,
  Building2,
  Bell,
  Printer,
  Eye,
  EyeOff,
  QrCode
} from 'lucide-react';

// Barcode SVG Generator Component
const BarcodeSvg: React.FC<{ value: string; height?: number; className?: string }> = ({ value, height = 48, className = "" }) => {
  const digits = value.split('');
  let bars: boolean[] = [true, false, true, true, false, true]; // Start pattern
  
  digits.forEach((d) => {
    const num = parseInt(d, 10) || 0;
    const pattern = [
      [1, 2, 1, 1, 2, 1], [2, 1, 1, 2, 1, 1], [1, 1, 2, 1, 2, 1], [2, 2, 1, 1, 1, 1], [1, 2, 2, 1, 1, 1],
      [1, 1, 1, 2, 2, 1], [1, 1, 2, 2, 1, 1], [2, 1, 1, 1, 2, 1], [1, 2, 1, 1, 1, 2], [2, 1, 2, 1, 1, 1]
    ][num % 10];
    
    pattern.forEach((w, idx) => {
      const isBar = idx % 2 === 0;
      for (let i = 0; i < w; i++) {
        bars.push(isBar);
      }
    });
  });
  
  bars.push(true, false, true, true, false, true, true, true); // End pattern

  const barWidth = 2.5;
  const totalWidth = bars.length * barWidth;

  return (
    <div className={`inline-block bg-white p-2 rounded-xl border border-slate-300 ${className}`}>
      <svg width={totalWidth} height={height} viewBox={`0 0 ${totalWidth} ${height}`} className="mx-auto block">
        <rect width={totalWidth} height={height} fill="#ffffff" />
        {bars.map((isBar, i) =>
          isBar ? (
            <rect key={i} x={i * barWidth} y={0} width={barWidth} height={height} fill="#000000" />
          ) : null
        )}
      </svg>
    </div>
  );
};

// QR Code SVG Generator Component
const QrCodeSvg: React.FC<{ value: string; size?: number; className?: string }> = ({ value, size = 90, className = "" }) => {
  const grid = Array(15).fill(0).map(() => Array(15).fill(false));
  
  const addFinder = (r: number, c: number) => {
    for (let i = 0; i < 5; i++) {
      for (let j = 0; j < 5; j++) {
        if (i === 0 || i === 4 || j === 0 || j === 4 || (i >= 1 && i <= 3 && j >= 1 && j <= 3 && !(i === 2 && j === 2))) {
          grid[r + i][c + j] = true;
        } else if (i === 2 && j === 2) {
          grid[r + i][c + j] = true;
        }
      }
    }
  };

  addFinder(0, 0);
  addFinder(0, 10);
  addFinder(10, 0);

  let charIdx = 0;
  for (let r = 0; r < 15; r++) {
    for (let c = 0; c < 15; c++) {
      if ((r < 5 && c < 5) || (r < 5 && c >= 10) || (r >= 10 && c < 5)) continue;
      const seed = (value.charCodeAt(charIdx % value.length) * (r + 3) + c * 11) % 3;
      grid[r][c] = seed === 0 || (r + c) % 2 === 0;
      charIdx++;
    }
  }

  const cellSize = size / 15;

  return (
    <div className={`inline-block bg-white p-2 rounded-xl border border-slate-300 ${className}`}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="mx-auto block">
        <rect width={size} height={size} fill="#ffffff" />
        {grid.map((row, r) =>
          row.map((cell, c) =>
            cell ? (
              <rect
                key={`${r}-${c}`}
                x={c * cellSize}
                y={r * cellSize}
                width={cellSize + 0.3}
                height={cellSize + 0.3}
                fill="#000000"
              />
            ) : null
          )
        )}
      </svg>
    </div>
  );
};

interface TeacherPortalViewProps {
  students: Student[];
  setStudents: React.Dispatch<React.SetStateAction<Student[]>>;
  staffList: StaffMember[];
  config: AppConfig;
  onBackToMain: () => void;
}

export const TeacherPortalView: React.FC<TeacherPortalViewProps> = ({
  students,
  setStudents,
  staffList,
  config,
  onBackToMain
}) => {
  // Connection & Auth State
  const [cloudUrl, setCloudUrl] = useState('https://diyala-school-cloud.iq/api/v1/sync');
  const [teacherName, setTeacherName] = useState('');
  const [selectedSubjects, setSelectedSubjects] = useState<string[]>(['اللغة العربية']);
  const [selectedGrades, setSelectedGrades] = useState<string[]>(['الأول الابتدائي']);
  const [selectedSections, setSelectedSections] = useState<string[]>(['أ']);
  const [currentSubject, setCurrentSubject] = useState('اللغة العربية');
  const [currentGrade, setCurrentGrade] = useState('الأول الابتدائي');
  const [currentSection, setCurrentSection] = useState('أ');
  const [isSessionActive, setIsSessionActive] = useState(false);

  // OTP Verification System
  const [generatedOtpCode, setGeneratedOtpCode] = useState<string>('849203');
  const [inputOtpCode, setInputOtpCode] = useState<string>('');
  const [isVerifiedByPrincipal, setIsVerifiedByPrincipal] = useState<boolean>(false);
  const [showOtpModal, setShowOtpModal] = useState<boolean>(false);
  const [otpError, setOtpError] = useState<string | null>(null);
  const [copiedOtp, setCopiedOtp] = useState<boolean>(false);

  // Entry Method State ('manual' | 'camera' | 'image' | 'voice')
  const [entryMode, setEntryMode] = useState<'manual' | 'camera' | 'image' | 'voice'>('manual');

  // Local state for modified grades in this session (StudentId -> Score)
  const [sessionScores, setSessionScores] = useState<Record<string, number>>({});

  // Voice State
  const [isListening, setIsListening] = useState(false);
  const [voiceTranscript, setVoiceTranscript] = useState('');
  const [voiceFeedback, setVoiceFeedback] = useState('اضغط على المايك ثم انطق اسم الطالب والدرجة (مثال: "عباس حسن 85")');

  // OCR State
  const [ocrImagePreview, setOcrImagePreview] = useState<string | null>(null);
  const [isOcrScanning, setIsOcrScanning] = useState(false);
  const [ocrExtractedData, setOcrExtractedData] = useState<Array<{ name: string; mark: number }>>([]);

  // Review & Sync Modal State
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [isSyncingCloud, setIsSyncingCloud] = useState(false);
  const [syncSuccessToast, setSyncSuccessToast] = useState<string | null>(null);

  // Printing Modals State
  const [showPrintGradeSheetModal, setShowPrintGradeSheetModal] = useState(false);
  const [showPrintOtpSlipModal, setShowPrintOtpSlipModal] = useState(false);
  const [otpPrintMode, setOtpPrintMode] = useState<'both' | 'barcode_only' | 'digits_only'>('both');

  // Available subjects list
  const availableSubjects = [
    'اللغة العربية', 'التربية الإسلامية', 'الرياضيات', 'العلوم', 
    'اللغة الإنجليزية', 'الاجتماعيات', 'التربية الفنية', 'التربية الرياضية'
  ];

  const availableGrades = [
    'الأول الابتدائي', 'الثاني الابتدائي', 'الثالث الابتدائي', 
    'الرابع الابتدائي', 'الخامس الابتدائي', 'السادس الابتدائي'
  ];

  const availableSections = ['أ', 'ب', 'ج', 'د', 'هـ'];

  // Generate random 6-digit OTP code whenever teacher selects name/subject/grade
  const refreshOtpCode = () => {
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    setGeneratedOtpCode(code);
    setInputOtpCode('');
  };

  useEffect(() => {
    refreshOtpCode();
  }, []);

  // Speech Output Helper
  const speakArabic = (text: string) => {
    try {
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'ar-IQ';
        utterance.rate = 0.95;
        window.speechSynthesis.speak(utterance);
      }
    } catch (e) {
      console.log('Speech error:', e);
    }
  };

  // Start Session handler
  const handleStartSession = (verifiedDirectly: boolean = false) => {
    if (!teacherName.trim()) {
      alert('يرجى تحديد أو كتابة اسم المدرس أولاً');
      return;
    }
    if (selectedSubjects.length === 0 || selectedGrades.length === 0 || selectedSections.length === 0) {
      alert('يرجى تحديد مادة وصف وشعبة واحدة على الأقل');
      return;
    }

    setCurrentSubject(selectedSubjects[0]);
    setCurrentGrade(selectedGrades[0]);
    setCurrentSection(selectedSections[0]);

    // Initialize local session scores with existing student scores
    const initialScores: Record<string, number> = {};
    students.forEach(s => {
      if (s.finalYearScore !== undefined) {
        initialScores[s.id] = s.finalYearScore;
      }
    });
    setSessionScores(initialScores);

    if (verifiedDirectly) {
      setIsVerifiedByPrincipal(true);
      speakArabic(`تم التوثيق برمز المدير، أهلاً بك أستاذ ${teacherName}، تم تفعيل المزامنة السحابية المباشرة`);
    } else if (inputOtpCode.trim() === generatedOtpCode) {
      setIsVerifiedByPrincipal(true);
      speakArabic(`تم مطابقة الرمز بنجاح! أهلاً بك أستاذ ${teacherName}، تم تفعيل المزامنة السحابية الرسمية`);
    } else {
      setIsVerifiedByPrincipal(false);
      speakArabic(`تم فتح دفتر الدرجات الخاص للأستاذ ${teacherName}، يمكنك رصد الدرجات وحفظها محلياً`);
    }

    setIsSessionActive(true);
    setShowOtpModal(false);
  };

  // Verify OTP submission handler
  const handleVerifyOtpSubmit = () => {
    setOtpError(null);
    if (inputOtpCode.trim() === generatedOtpCode) {
      setIsVerifiedByPrincipal(true);
      setSyncSuccessToast('تم اعتماد التوثيق بنجاح برمز المدير المعتمد! أصبحت المزامنة السحابية المباشرة نشطة.');
      speakArabic('تم إدخال الرمز الصحيح، أصبحت المزامنة مع سحابة المدرسة نشطة الآن');
      setShowOtpModal(false);
      setTimeout(() => setSyncSuccessToast(null), 4000);
    } else {
      setOtpError(`الرمز المدخل (${inputOtpCode}) غير صحيح! الرمز المولد هو (${generatedOtpCode}). يمكنك المتابعة كدفتر خاص أو إعادة المحاولة.`);
      speakArabic('رمز التوثيق غير صحيح، يرجى التأكد من الرمز المرسل من المدير');
    }
  };

  // Filter students for current grade & section
  const filteredStudents = students.filter(
    s => s.currentGrade === currentGrade && s.section === currentSection
  );

  // Update Score Helper
  const handleScoreChange = (studentId: string, score: number) => {
    const validScore = Math.min(100, Math.max(0, score || 0));
    setSessionScores(prev => ({ ...prev, [studentId]: validScore }));
  };

  // Handle Voice Listening
  const toggleVoiceListening = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert('التعرف الصوتي المباشر يتطلب متصفح Chrome أو Edge');
      return;
    }

    if (isListening) {
      setIsListening(false);
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.lang = 'ar-IQ';
      recognition.continuous = false;
      recognition.interimResults = true;

      recognition.onstart = () => {
        setIsListening(true);
        setVoiceFeedback('جاري الاستماع الآن... انطق اسم الطالب والدرجة');
      };

      recognition.onresult = (e: any) => {
        let transcriptStr = '';
        for (let i = e.resultIndex; i < e.results.length; ++i) {
          transcriptStr += e.results[i][0].transcript;
        }
        setVoiceTranscript(transcriptStr);

        if (e.results[0].isFinal) {
          processVoiceScoreInput(transcriptStr);
        }
      };

      recognition.onerror = () => {
        setIsListening(false);
        setVoiceFeedback('لم يتم التقاط الصوت بوضوح، اضغط المايك للمحاولة مجدداً');
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognition.start();
    } catch (err) {
      console.error(err);
      setIsListening(false);
    }
  };

  // Process Voice Command
  const processVoiceScoreInput = (text: string) => {
    const numbers = text.match(/\d+/g);
    const score = numbers ? parseInt(numbers[0], 10) : null;

    if (score === null || score < 0 || score > 100) {
      setVoiceFeedback(`سمعنا: "${text}"، لكن لم نحدد درجة بين 0 و 100`);
      speakArabic('يرجى نطق الدرجة بشكل أرقام صحيحة');
      return;
    }

    const matchedStudent = filteredStudents.find(s => 
      text.toLowerCase().includes(s.firstName.toLowerCase())
    );

    if (matchedStudent) {
      handleScoreChange(matchedStudent.id, score);
      const msg = `تم تسجيل درجة (${score}) للطالب ${matchedStudent.firstName}`;
      setVoiceFeedback(msg);
      speakArabic(`تم تسجيل درجة ${score} للطالب ${matchedStudent.firstName}`);
    } else {
      setVoiceFeedback(`سمعنا: "${text}"، الدرجة ${score}، لكن لم نجد اسماً مطابقاً في هذا الصف`);
      speakArabic(`لم أجد طالباً بهذا الاسم في شعبة ${currentSection}`);
    }
  };

  // Handle Image File selection for OCR
  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (ev) => {
        setOcrImagePreview(ev.target?.result as string);
        runSimulatedOcrScan();
      };
      reader.readAsDataURL(file);
    }
  };

  // OCR Scan Simulation / Extraction
  const runSimulatedOcrScan = () => {
    setIsOcrScanning(true);
    setTimeout(() => {
      const extracted = filteredStudents.map(s => ({
        name: s.firstName,
        mark: Math.floor(Math.random() * 35) + 65
      }));
      setOcrExtractedData(extracted);
      setIsOcrScanning(false);
      speakArabic(`تم تحليل ورقة الدرجات بنجاح واستخراج درجات ${extracted.length} طلاب`);
    }, 1800);
  };

  // Apply OCR Grades to Session
  const applyOcrGradesToSession = () => {
    ocrExtractedData.forEach(item => {
      const std = filteredStudents.find(s => s.firstName.includes(item.name));
      if (std) {
        handleScoreChange(std.id, item.mark);
      }
    });
    setSyncSuccessToast(`تم تطبيق درجات ${ocrExtractedData.length} طالب بنجاح من ورقة السجل`);
    setTimeout(() => setSyncSuccessToast(null), 4000);
    setEntryMode('manual');
  };

  // Handle Final Review & Send to Cloud
  const handleExecuteSendToCloud = () => {
    if (!isVerifiedByPrincipal) {
      setShowReviewModal(false);
      setShowOtpModal(true);
      return;
    }

    setIsSyncingCloud(true);
    setTimeout(() => {
      setStudents(prev => prev.map(s => {
        const updatedScore = sessionScores[s.id];
        if (updatedScore !== undefined && s.currentGrade === currentGrade && s.section === currentSection) {
          return {
            ...s,
            finalYearScore: updatedScore,
            previousYearResult: updatedScore >= 50 ? `ناجح (${updatedScore})` : `راسب (${updatedScore})`
          };
        }
        return s;
      }));

      setIsSyncingCloud(false);
      setShowReviewModal(false);
      setSyncSuccessToast(`تمت المزامنة بنجاح مع سحابة المدرسة (${config.schoolName}) وتم اعتماد الدرجات رسمياً!`);
      speakArabic('تمت المزامنة وحفظ الدرجات بنجاح في سحابة المدرسة');
      setTimeout(() => setSyncSuccessToast(null), 5000);
    }, 1500);
  };

  return (
    <div className="space-y-6 dir-rtl min-h-[80vh]">
      
      {/* Toast Banner */}
      {syncSuccessToast && (
        <div className="fixed top-5 left-1/2 -translate-x-1/2 z-50 bg-emerald-600 text-white px-6 py-3.5 rounded-2xl shadow-2xl font-black text-sm flex items-center gap-2 animate-bounce">
          <CheckCircle2 className="w-5 h-5 text-amber-300" />
          <span>{syncSuccessToast}</span>
        </div>
      )}

      {/* HEADER BAR FOR TEACHER PORTAL */}
      <div className="bg-gradient-to-r from-blue-900 via-indigo-900 to-slate-900 text-white p-5 rounded-3xl shadow-xl flex flex-wrap items-center justify-between gap-4 border border-blue-500/30">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-amber-500 text-slate-950 flex items-center justify-center font-black shadow-lg">
            <BookOpen className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-xl font-black">بوابة المدرسين الإلكترونية (Teacher Web Portal)</h2>
              {isSessionActive && (
                isVerifiedByPrincipal ? (
                  <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/20 border border-emerald-400/40 text-emerald-300 text-[11px] font-bold flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                    <span>موثق رسمياً برمز المدير (سحابة نشطة)</span>
                  </span>
                ) : (
                  <span className="px-2.5 py-0.5 rounded-full bg-amber-500/20 border border-amber-400/40 text-amber-300 text-[11px] font-bold flex items-center gap-1">
                    <Notebook className="w-3.5 h-3.5 text-amber-400" />
                    <span>دفتر درجات خاص (غير موثق بالسحابة)</span>
                  </span>
                )
              )}
            </div>
            <p className="text-xs text-blue-200 mt-0.5">
              منظومة إدخال ورصد الدرجات للمدرسين وتوثيق الدخول مع إدارة {config.schoolName}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {isSessionActive && !isVerifiedByPrincipal && (
            <button
              onClick={() => setShowOtpModal(true)}
              className="px-3.5 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-xs flex items-center gap-1.5 transition-all shadow-md cursor-pointer animate-pulse"
            >
              <KeyRound className="w-4 h-4" />
              <span>إدخال رمز المدير للتفعيل السحابي</span>
            </button>
          )}

          {isSessionActive && (
            <button
              onClick={() => {
                if (confirm('هل تريد تسجيل الخروج وتبديل المدرس أو الصف؟')) {
                  setIsSessionActive(false);
                  refreshOtpCode();
                }
              }}
              className="px-3.5 py-2 rounded-xl bg-rose-500/20 hover:bg-rose-500/30 border border-rose-400/30 text-rose-200 text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer"
            >
              <LogOut className="w-4 h-4" />
              <span>تسجيل الخروج / تبديل الصف</span>
            </button>
          )}

          <button
            onClick={onBackToMain}
            className="px-3.5 py-2 rounded-xl bg-white/10 hover:bg-white/20 border border-white/20 text-white text-xs font-bold flex items-center gap-1 transition-all cursor-pointer"
          >
            <span>العودة للرئيسية</span>
          </button>
        </div>
      </div>

      {/* STEP 1: INITIAL LOGIN & CLOUD CONNECTION FORM */}
      {!isSessionActive ? (
        <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-3xl p-6 md:p-8 shadow-xl space-y-6 max-w-4xl mx-auto">
          <div className="text-center space-y-2 border-b pb-4">
            <div className="inline-flex p-3 rounded-2xl bg-blue-100 dark:bg-blue-900/40 text-blue-600 dark:text-blue-300 mb-1">
              <Globe className="w-8 h-8 animate-spin-slow" />
            </div>
            <h3 className="text-lg font-black text-slate-900 dark:text-white">
              ربط وتوثيق جلسة المدرس مع إدارة المدرسة
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-md mx-auto">
              عند تحديد اسم المدرس والصف يتم إرسال رمز أمان لتطبيق المدير، وعند أدخاله يتفعل ربط السحابة، أو المتابعة كدفتر درجات خاص
            </p>
          </div>

          {/* SIMULATED PRINCIPAL APP DISPATCH NOTIFICATION PANEL */}
          <div className="bg-gradient-to-r from-amber-500/10 via-purple-500/10 to-indigo-500/10 border-2 border-amber-500/30 rounded-3xl p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-amber-700 dark:text-amber-300 font-black text-xs">
                <Bell className="w-4 h-4 animate-bounce text-amber-500" />
                <span>إشعار وارد لتطبيق المدير / الإدارة (School Principal Dispatch):</span>
              </div>
              <span className="px-2.5 py-0.5 rounded-full bg-amber-500 text-slate-950 font-mono font-black text-[10px]">
                رمز التوثيق الموجه للأستاذ
              </span>
            </div>

            <div className="p-3 bg-amber-50/80 dark:bg-amber-950/40 rounded-2xl border border-amber-200 dark:border-amber-800 space-y-1.5 text-xs">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 text-slate-800 dark:text-slate-200 font-bold">
                  <span>اسم المدرس المستهدف بالطلب:</span>
                  <span className="text-sm font-black text-blue-700 dark:text-blue-300 bg-blue-100 dark:bg-blue-900/60 px-2.5 py-0.5 rounded-lg border border-blue-300">
                    الأستاذ / {teacherName || 'لم يُدخل الاسم بعد'}
                  </span>
                </div>
                <div className="text-[11px] text-slate-600 dark:text-slate-300 font-medium">
                  المادة: <strong className="text-emerald-700 dark:text-emerald-300">{selectedSubjects.join('، ')}</strong> | الصف: <strong className="text-amber-700 dark:text-amber-300">{selectedGrades.join('، ')} (شعبة {selectedSections.join('، ')})</strong>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 p-3 bg-white dark:bg-slate-900 rounded-2xl border shadow-inner">
              <div className="flex items-center gap-3">
                <div className="p-2.5 bg-amber-500/20 text-amber-700 dark:text-amber-300 rounded-xl">
                  <KeyRound className="w-6 h-6" />
                </div>
                <div>
                  <span className="block text-[10px] text-slate-500 font-bold">رمز التوثيق المعتمد والخاص بالأستاذ:</span>
                  <span className="text-2xl font-black font-mono tracking-widest text-emerald-600 dark:text-emerald-400">
                    {generatedOtpCode}
                  </span>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={() => setShowPrintOtpSlipModal(true)}
                  className="px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs flex items-center gap-1.5 cursor-pointer shadow-md"
                >
                  <Printer className="w-3.5 h-3.5" />
                  <span>طباعة بطاقة الرمز للأستاذ</span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    navigator.clipboard.writeText(generatedOtpCode);
                    setCopiedOtp(true);
                    setTimeout(() => setCopiedOtp(false), 2000);
                  }}
                  className="px-3 py-1.5 rounded-xl bg-slate-200 dark:bg-slate-800 text-xs font-bold flex items-center gap-1 cursor-pointer hover:bg-slate-300"
                >
                  {copiedOtp ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedOtp ? 'تم النسخ' : 'نسخ الرمز'}</span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setInputOtpCode(generatedOtpCode);
                    handleStartSession(true);
                  }}
                  className="px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs flex items-center gap-1 cursor-pointer shadow-md"
                >
                  <ShieldCheck className="w-4 h-4" />
                  <span>موافقة المدير واعتماد الدخول مباشرة</span>
                </button>
              </div>
            </div>
          </div>

          {/* FORM FIELDS */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Cloud API URL */}
            <div className="md:col-span-2 space-y-1">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <Cloud className="w-4 h-4 text-blue-500" />
                <span>رابط سحابة المدرسة (Cloud Sync Link):</span>
              </label>
              <input 
                type="text" 
                value={cloudUrl}
                onChange={e => setCloudUrl(e.target.value)}
                placeholder="https://diyala-school-cloud.iq/api/v1/sync"
                className="w-full p-3 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-mono text-slate-800 dark:text-slate-200"
              />
            </div>

            {/* Teacher Name Selection or Entry */}
            <div className="space-y-1">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <User className="w-4 h-4 text-purple-500" />
                <span>اسم المدرس / الأستاذ:</span>
              </label>
              <select
                value={teacherName}
                onChange={e => {
                  setTeacherName(e.target.value);
                  refreshOtpCode();
                }}
                className="w-full p-3 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-bold"
              >
                <option value="">-- اختر اسم المدرس من الكادر --</option>
                {staffList.map(st => (
                  <option key={st.id} value={st.fullName}>{st.fullName} ({st.specialization})</option>
                ))}
              </select>
              {!teacherName && (
                <input 
                  type="text" 
                  placeholder="أو اكتب اسم المدرس هنا..."
                  onChange={e => {
                    setTeacherName(e.target.value);
                    refreshOtpCode();
                  }}
                  className="w-full p-2.5 mt-1 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-bold"
                />
              )}
            </div>

            {/* Select Main Subjects (Multi-select) */}
            <div className="space-y-1">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <BookOpen className="w-4 h-4 text-emerald-500" />
                <span>المواد المشمولة بالتدرس:</span>
              </label>
              <div className="flex flex-wrap gap-1.5 p-2 bg-slate-50 dark:bg-slate-900 rounded-xl border max-h-32 overflow-y-auto">
                {availableSubjects.map(sub => {
                  const isSelected = selectedSubjects.includes(sub);
                  return (
                    <button
                      key={sub}
                      type="button"
                      onClick={() => {
                        if (isSelected) {
                          if (selectedSubjects.length > 1) setSelectedSubjects(prev => prev.filter(s => s !== sub));
                        } else {
                          setSelectedSubjects(prev => [...prev, sub]);
                        }
                        refreshOtpCode();
                      }}
                      className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                        isSelected 
                          ? 'bg-emerald-600 text-white shadow' 
                          : 'bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      {sub} {isSelected && '✓'}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Select Grades */}
            <div className="space-y-1">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <Layers className="w-4 h-4 text-amber-500" />
                <span>الصف الدراسي المشمول:</span>
              </label>
              <div className="flex flex-wrap gap-1.5 p-2 bg-slate-50 dark:bg-slate-900 rounded-xl border">
                {availableGrades.map(grd => {
                  const isSelected = selectedGrades.includes(grd);
                  return (
                    <button
                      key={grd}
                      type="button"
                      onClick={() => {
                        if (isSelected) {
                          if (selectedGrades.length > 1) setSelectedGrades(prev => prev.filter(g => g !== grd));
                        } else {
                          setSelectedGrades(prev => [...prev, grd]);
                        }
                        refreshOtpCode();
                      }}
                      className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                        isSelected 
                          ? 'bg-amber-500 text-slate-950 font-black shadow' 
                          : 'bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      {grd} {isSelected && '✓'}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Select Sections */}
            <div className="space-y-1">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <Sliders className="w-4 h-4 text-indigo-500" />
                <span>الشعبة المشمولة:</span>
              </label>
              <div className="flex flex-wrap gap-2 p-2 bg-slate-50 dark:bg-slate-900 rounded-xl border">
                {availableSections.map(sec => {
                  const isSelected = selectedSections.includes(sec);
                  return (
                    <button
                      key={sec}
                      type="button"
                      onClick={() => {
                        if (isSelected) {
                          if (selectedSections.length > 1) setSelectedSections(prev => prev.filter(s => s !== sec));
                        } else {
                          setSelectedSections(prev => [...prev, sec]);
                        }
                        refreshOtpCode();
                      }}
                      className={`w-9 h-9 rounded-xl font-black text-xs transition-all cursor-pointer ${
                        isSelected 
                          ? 'bg-indigo-600 text-white shadow ring-2 ring-indigo-400' 
                          : 'bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      شعبة {sec}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Verification Code Input Field */}
            <div className="md:col-span-2 space-y-1 bg-amber-50 dark:bg-amber-950/30 p-4 rounded-2xl border border-amber-300 dark:border-amber-800">
              <label className="block text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                <KeyRound className="w-4 h-4 text-amber-600" />
                <span>أدخل رمز الاعتماد المزود من المدير (اختياري للتفعيل السحابي المباشر):</span>
              </label>
              <div className="flex gap-2">
                <input 
                  type="text" 
                  value={inputOtpCode}
                  onChange={e => setInputOtpCode(e.target.value)}
                  placeholder="مثال: 849203"
                  className="flex-1 p-3 rounded-xl border bg-white dark:bg-slate-900 text-center font-mono font-black text-lg tracking-widest text-slate-900 dark:text-white"
                />
                <button
                  type="button"
                  onClick={() => {
                    if (inputOtpCode.trim() === generatedOtpCode) {
                      handleStartSession(true);
                    } else {
                      alert(`رمز التوثيق غير صحيح! الرمز المولد من تطبيق المدير هو (${generatedOtpCode})`);
                    }
                  }}
                  className="px-5 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs flex items-center gap-1.5 cursor-pointer shadow-md"
                >
                  <ShieldCheck className="w-4 h-4" />
                  <span>تأكيد الرمز</span>
                </button>
              </div>
            </div>
          </div>

          {/* TWO DUAL LAUNCH OPTIONS */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
            <button
              onClick={() => handleStartSession(false)}
              className="py-4 rounded-2xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-xs shadow-lg flex items-center justify-center gap-2 cursor-pointer transition-all hover:scale-[1.01]"
            >
              <Notebook className="w-5 h-5 text-slate-900" />
              <span>الدخول بـ (دفتر درجات خاص للأستاذ) بدون رمز</span>
            </button>

            <button
              onClick={() => handleStartSession(true)}
              className="py-4 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white font-black text-xs shadow-lg flex items-center justify-center gap-2 cursor-pointer transition-all hover:scale-[1.01]"
            >
              <ShieldCheck className="w-5 h-5 text-amber-300" />
              <span>دخول موثق ومُعتمد بالسحابة (برمز المدير)</span>
            </button>
          </div>
        </div>
      ) : (

        /* STEP 2: ACTIVE TEACHER MARKS RECORDING PORTAL */
        <div className="space-y-6">

          {/* ACTIVE SESSION BAR & QUICK FILTERS */}
          <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] p-4 rounded-2xl shadow flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-purple-100 dark:bg-purple-950 text-purple-700 dark:text-purple-300 font-bold text-xs flex items-center gap-1.5">
                <User className="w-4 h-4" />
                <span>المدرس: {teacherName}</span>
              </div>

              {/* Subject Selector */}
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-bold text-slate-500">المادة:</span>
                <select
                  value={currentSubject}
                  onChange={e => setCurrentSubject(e.target.value)}
                  className="p-1.5 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-black text-emerald-700 dark:text-emerald-300"
                >
                  {selectedSubjects.map(sub => (
                    <option key={sub} value={sub}>{sub}</option>
                  ))}
                </select>
              </div>

              {/* Grade Selector */}
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-bold text-slate-500">الصف:</span>
                <select
                  value={currentGrade}
                  onChange={e => setCurrentGrade(e.target.value)}
                  className="p-1.5 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-black text-amber-700 dark:text-amber-300"
                >
                  {selectedGrades.map(grd => (
                    <option key={grd} value={grd}>{grd}</option>
                  ))}
                </select>
              </div>

              {/* Section Selector */}
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-bold text-slate-500">الشعبة:</span>
                <select
                  value={currentSection}
                  onChange={e => setCurrentSection(e.target.value)}
                  className="p-1.5 rounded-xl border bg-slate-50 dark:bg-slate-900 text-xs font-black text-indigo-700 dark:text-indigo-300"
                >
                  {selectedSections.map(sec => (
                    <option key={sec} value={sec}>شعبة {sec}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setShowPrintGradeSheetModal(true)}
                className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-900 text-white font-bold text-xs shadow flex items-center gap-1.5 cursor-pointer border border-slate-700"
                title="طباعة سجل ودفتر الدرجات الخاص بالأستاذ"
              >
                <Printer className="w-4 h-4 text-amber-400" />
                <span>طباعة دفتر الدرجات</span>
              </button>

              {/* SEND ALL TO CLOUD BUTTON */}
              <button
                onClick={() => {
                  if (!isVerifiedByPrincipal) {
                    setShowOtpModal(true);
                  } else {
                    setShowReviewModal(true);
                  }
                }}
                className={`px-5 py-2.5 rounded-xl font-black text-xs shadow-lg flex items-center gap-2 cursor-pointer transition-all ${
                  isVerifiedByPrincipal 
                    ? 'bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white' 
                    : 'bg-amber-500 hover:bg-amber-400 text-slate-950 animate-bounce'
                }`}
              >
                {isVerifiedByPrincipal ? <Send className="w-4 h-4" /> : <Lock className="w-4 h-4" />}
                <span>{isVerifiedByPrincipal ? 'مزامنة وإرسال الدرجات لسحابة المدرسة' : 'تفعيل المزامنة برمز المدير'}</span>
              </button>
            </div>
          </div>

          {/* MODE NOTICE BANNER */}
          {!isVerifiedByPrincipal && (
            <div className="bg-amber-50 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-800 p-4 rounded-2xl flex flex-wrap items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-2.5 text-amber-900 dark:text-amber-200">
                <Notebook className="w-5 h-5 text-amber-600 shrink-0" />
                <div>
                  <p className="font-black">أنت تعمل حالياً في وضع: (دفتر الدرجات الخاص للأستاذ)</p>
                  <p className="text-[11px] text-amber-700 dark:text-amber-300 mt-0.5">
                    يمكنك استخدام الصوت، الكاميرا، أو الإدخال اليدوي لرصد وحفظ درجاتك محلياً. للمزامنة المباشرة مع سحابة المدرسة يُرجى إدخال رمز المدير المعتمد.
                  </p>
                </div>
              </div>

              <button
                onClick={() => setShowOtpModal(true)}
                className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black flex items-center gap-1.5 shrink-0 shadow-md cursor-pointer"
              >
                <KeyRound className="w-4 h-4" />
                <span>أدخل رمز المدير لتفعيل السحابة</span>
              </button>
            </div>
          )}

          {/* ENTRY MODE TABS (4 WAYS TO INPUT MARKS) */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            
            {/* Mode 1: Manual */}
            <button
              onClick={() => setEntryMode('manual')}
              className={`p-3.5 rounded-2xl border font-black text-xs flex items-center justify-center gap-2 transition-all cursor-pointer ${
                entryMode === 'manual'
                  ? 'bg-blue-600 text-white border-blue-600 shadow-lg scale-[1.02]'
                  : 'bg-[var(--theme-card)] border-[var(--theme-card-border)] text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <FileText className="w-4 h-4" />
              <span>1. إدخال يدوي تفاعلي</span>
            </button>

            {/* Mode 2: Camera Live OCR */}
            <button
              onClick={() => setEntryMode('camera')}
              className={`p-3.5 rounded-2xl border font-black text-xs flex items-center justify-center gap-2 transition-all cursor-pointer ${
                entryMode === 'camera'
                  ? 'bg-purple-600 text-white border-purple-600 shadow-lg scale-[1.02]'
                  : 'bg-[var(--theme-card)] border-[var(--theme-card-border)] text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <Camera className="w-4 h-4" />
              <span>2. التقاط الكاميرا الفورية</span>
            </button>

            {/* Mode 3: Image File OCR */}
            <button
              onClick={() => setEntryMode('image')}
              className={`p-3.5 rounded-2xl border font-black text-xs flex items-center justify-center gap-2 transition-all cursor-pointer ${
                entryMode === 'image'
                  ? 'bg-amber-500 text-slate-950 border-amber-500 shadow-lg scale-[1.02]'
                  : 'bg-[var(--theme-card)] border-[var(--theme-card-border)] text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <Upload className="w-4 h-4" />
              <span>3. رفع صورة السجل</span>
            </button>

            {/* Mode 4: Voice Commands Entry */}
            <button
              onClick={() => setEntryMode('voice')}
              className={`p-3.5 rounded-2xl border font-black text-xs flex items-center justify-center gap-2 transition-all cursor-pointer ${
                entryMode === 'voice'
                  ? 'bg-rose-600 text-white border-rose-600 shadow-lg scale-[1.02]'
                  : 'bg-[var(--theme-card)] border-[var(--theme-card-border)] text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <Mic className="w-4 h-4 animate-pulse" />
              <span>4. الإدخال بالصوت المباشر</span>
            </button>

          </div>

          {/* MODE 1: MANUAL TABLE ENTRY */}
          {entryMode === 'manual' && (
            <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-3xl p-5 shadow-xl space-y-4">
              <div className="flex items-center justify-between border-b pb-3">
                <h3 className="font-black text-sm text-slate-900 dark:text-white flex items-center gap-2">
                  <FileText className="w-4 h-4 text-blue-600" />
                  <span>دفتر درجات الأستاذ ({currentGrade} - شعبة {currentSection} - مادة {currentSubject})</span>
                </h3>
                <span className="text-xs text-slate-500 font-bold">
                  عدد الطلاب: {filteredStudents.length} طالب
                </span>
              </div>

              {filteredStudents.length === 0 ? (
                <div className="p-8 text-center text-slate-500 text-xs font-bold space-y-2">
                  <AlertCircle className="w-8 h-8 text-amber-500 mx-auto" />
                  <p>لا يوجد طلاب مسجلون في {currentGrade} - شعبة {currentSection}</p>
                </div>
              ) : (
                <div className="overflow-x-auto border rounded-2xl">
                  <table className="w-full text-xs text-right">
                    <thead className="bg-slate-100 dark:bg-slate-800/80 text-slate-700 dark:text-slate-300 font-bold border-b">
                      <tr>
                        <th className="p-3">#</th>
                        <th className="p-3">اسم الطالب الثلاثي</th>
                        <th className="p-3">رقم القيد</th>
                        <th className="p-3 text-center">درجة المادة (/100)</th>
                        <th className="p-3 text-center">النتيجة والتقدير</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y text-slate-800 dark:text-slate-200">
                      {filteredStudents.map((std, idx) => {
                        const score = sessionScores[std.id] !== undefined ? sessionScores[std.id] : (std.finalYearScore || 0);
                        const isPass = score >= 50;

                        return (
                          <tr key={std.id} className="hover:bg-slate-50 dark:hover:bg-slate-900/50 transition-colors">
                            <td className="p-3 font-mono font-bold text-slate-400">{idx + 1}</td>
                            <td className="p-3 font-black text-slate-900 dark:text-white">{std.firstName}</td>
                            <td className="p-3 font-mono text-slate-500">{std.recordNumber}</td>
                            <td className="p-3 text-center">
                              <input
                                type="number"
                                min={0}
                                max={100}
                                value={score}
                                onChange={e => handleScoreChange(std.id, Number(e.target.value))}
                                className={`w-20 px-2 py-1.5 text-center font-black font-mono rounded-xl border text-xs ${
                                  isPass 
                                    ? 'bg-emerald-50 border-emerald-300 text-emerald-900 dark:bg-emerald-950/60 dark:text-emerald-200' 
                                    : 'bg-rose-50 border-rose-300 text-rose-900 dark:bg-rose-950/60 dark:text-rose-200'
                                }`}
                              />
                            </td>
                            <td className="p-3 text-center">
                              <span className={`px-2.5 py-1 rounded-lg font-bold text-[11px] ${
                                isPass 
                                  ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300' 
                                  : 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-300'
                              }`}>
                                {isPass ? 'ناجح' : 'راسب'}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* MODE 2 & 3: CAMERA OCR OR UPLOAD IMAGE SCAN */}
          {(entryMode === 'camera' || entryMode === 'image') && (
            <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-3xl p-6 shadow-xl space-y-5 max-w-2xl mx-auto">
              <div className="text-center space-y-1">
                <h3 className="font-black text-sm text-slate-900 dark:text-white flex items-center justify-center gap-2">
                  <ScanLine className="w-5 h-5 text-purple-600" />
                  <span>مسح وقراءة ورقة الدرجات المطبوعة أو المكتوبة (Gemini Vision)</span>
                </h3>
                <p className="text-xs text-slate-500">
                  {entryMode === 'camera' ? 'التقط صورة مباشرة من كاميرا الهاتف لورقة الدرجات' : 'قم برفع صورة ورقة الدرجات من الجهاز'}
                </p>
              </div>

              <div className="border-2 border-dashed border-purple-300 dark:border-purple-800 rounded-3xl p-6 text-center bg-purple-50/50 dark:bg-purple-950/20 hover:bg-purple-50 transition-all">
                <input 
                  type="file" 
                  accept="image/*" 
                  capture={entryMode === 'camera' ? 'environment' : undefined}
                  onChange={handleImageUpload}
                  className="hidden" 
                  id="teacher-sheet-input"
                />
                <label htmlFor="teacher-sheet-input" className="cursor-pointer flex flex-col items-center gap-3">
                  {ocrImagePreview ? (
                    <img src={ocrImagePreview} alt="معاينة الورقة" className="max-h-56 rounded-2xl object-contain shadow-lg border" />
                  ) : (
                    <>
                      <div className="w-16 h-16 rounded-2xl bg-purple-600 text-white flex items-center justify-center shadow-lg">
                        {entryMode === 'camera' ? <Camera className="w-8 h-8" /> : <Upload className="w-8 h-8" />}
                      </div>
                      <span className="text-xs font-black text-slate-700 dark:text-slate-300">
                        اضغط لالتقاط أو اختيار صورة السجل
                      </span>
                    </>
                  )}
                </label>
              </div>

              {isOcrScanning && (
                <div className="p-4 rounded-2xl bg-purple-900 text-white text-xs font-bold flex items-center justify-center gap-2 animate-pulse">
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>جاري تحليل ورقة الدرجات بالذكاء الاصطناعي واستخراج الأسماء...</span>
                </div>
              )}

              {ocrExtractedData.length > 0 && !isOcrScanning && (
                <div className="space-y-3 pt-3 border-t">
                  <h4 className="text-xs font-black text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                    <Sparkles className="w-4 h-4 text-amber-500" />
                    <span>النتائج المستخرجة للطلاب ({ocrExtractedData.length} طالب):</span>
                  </h4>

                  <div className="max-h-48 overflow-y-auto border rounded-xl p-2 text-xs divide-y bg-slate-50 dark:bg-slate-900">
                    {ocrExtractedData.map((item, i) => (
                      <div key={i} className="py-2 flex items-center justify-between">
                        <span className="font-bold">{item.name}</span>
                        <span className="font-black font-mono text-emerald-600">الدرجة: {item.mark}</span>
                      </div>
                    ))}
                  </div>

                  <button
                    onClick={applyOcrGradesToSession}
                    className="w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-black text-xs shadow-lg cursor-pointer"
                  >
                    تطبيق وتحديث الدرجات في دفتر الأستاذ
                  </button>
                </div>
              )}
            </div>
          )}

          {/* MODE 4: VOICE COMMANDS MARKS ENTRY */}
          {entryMode === 'voice' && (
            <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-3xl p-6 shadow-xl space-y-6 max-w-xl mx-auto text-center">
              <div className="space-y-1">
                <h3 className="font-black text-sm text-slate-900 dark:text-white flex items-center justify-center gap-2">
                  <Mic className="w-5 h-5 text-rose-600 animate-pulse" />
                  <span>رصد الدرجات بالأوامر الصوتية المباشرة</span>
                </h3>
                <p className="text-xs text-slate-500">
                  انطق اسم الطالب والدرجة مباشرة (مثال: "عباس حسن 85" أو "محمد علي 92")
                </p>
              </div>

              {/* MIC BUTTON */}
              <button
                onClick={toggleVoiceListening}
                className={`w-28 h-28 mx-auto rounded-full flex items-center justify-center transition-all duration-300 shadow-2xl cursor-pointer ${
                  isListening 
                    ? 'bg-rose-600 text-white scale-110 ring-8 ring-rose-500/30 animate-pulse' 
                    : 'bg-slate-900 text-amber-400 hover:scale-105 ring-4 ring-amber-400/20'
                }`}
              >
                {isListening ? <Mic className="w-12 h-12" /> : <MicOff className="w-12 h-12" />}
              </button>

              <div className="p-4 rounded-2xl bg-slate-900 text-white text-xs font-bold space-y-1 border border-slate-800">
                <p className="text-amber-400 font-mono">{voiceTranscript || 'في انتظار نطق الاسم والدرجة...'}</p>
                <p className="text-slate-300 text-[11px]">{voiceFeedback}</p>
              </div>
            </div>
          )}

        </div>
      )}

      {/* OTP VERIFICATION MODAL */}
      {showOtpModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 dir-rtl">
          <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-5">
            
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400 font-black">
                <KeyRound className="w-6 h-6" />
                <span>إدخال رمز التوثيق المزود من المدير</span>
              </div>
              <button onClick={() => setShowOtpModal(false)} className="text-slate-500">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs text-slate-700 dark:text-slate-300">
              <p className="leading-relaxed">
                لربط الجلسة بسحابة المدرسة الرسمية، أدخل الرمز المرسل إلى تطبيق المدير (الرمز المولد المتاح في النظام: <strong className="font-mono text-emerald-600 font-black">{generatedOtpCode}</strong>).
              </p>

              {otpError && (
                <div className="p-3 bg-rose-50 dark:bg-rose-950/50 border border-rose-300 text-rose-800 dark:text-rose-200 rounded-xl text-xs font-bold flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <span>{otpError}</span>
                </div>
              )}

              <div className="space-y-1">
                <label className="block text-xs font-bold text-slate-800 dark:text-slate-200">
                  رمز الاعتماد المكون من 6 أرقام:
                </label>
                <input 
                  type="text"
                  value={inputOtpCode}
                  onChange={e => setInputOtpCode(e.target.value)}
                  placeholder="مثال: 849203"
                  className="w-full p-3 rounded-2xl border bg-slate-50 dark:bg-slate-900 text-center font-mono font-black text-xl tracking-widest text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <div className="flex flex-col gap-2 pt-2">
              <button
                onClick={handleVerifyOtpSubmit}
                className="w-full py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs shadow-xl flex items-center justify-center gap-2 cursor-pointer"
              >
                <ShieldCheck className="w-4 h-4" />
                <span>تأكيد الرمز وتفعيل المزامنة السحابية</span>
              </button>

              <button
                onClick={() => {
                  setIsVerifiedByPrincipal(false);
                  setShowOtpModal(false);
                }}
                className="w-full py-2.5 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-bold text-xs cursor-pointer"
              >
                الاستمرار بدون رمز (دفتر درجات خاص للأستاذ)
              </button>
            </div>

          </div>
        </div>
      )}

      {/* FINAL REVIEW & SYNC MODAL */}
      {showReviewModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 dir-rtl">
          <div className="bg-[var(--theme-card)] border border-[var(--theme-card-border)] rounded-3xl p-6 max-w-lg w-full shadow-2xl space-y-5">
            
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400 font-black">
                <ShieldCheck className="w-6 h-6" />
                <span>تدقيق وتأكيد إرسال الدرجات لسحابة المدرسة</span>
              </div>
              <button onClick={() => setShowReviewModal(false)} className="text-slate-500">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs text-slate-700 dark:text-slate-300">
              <p className="leading-relaxed">
                سيتم إرسال الدرجات المرصودة رسمياً لـ <strong className="text-slate-900 dark:text-white">{currentGrade} - شعبة {currentSection} ({currentSubject})</strong> بإمضاء الأستاذ <strong className="text-blue-600">{teacherName}</strong> إلى سحابة المدرسة.
              </p>

              <div className="p-3 bg-slate-100 dark:bg-slate-900 rounded-2xl space-y-1 font-bold">
                <div className="flex justify-between">
                  <span>عدد الطلاب المشمولين:</span>
                  <span className="font-mono text-blue-600">{filteredStudents.length} طالب</span>
                </div>
                <div className="flex justify-between">
                  <span>الرابط السحابي المستهدف:</span>
                  <span className="font-mono text-[10px] text-slate-500">{cloudUrl}</span>
                </div>
              </div>
            </div>

            <div className="flex gap-2 pt-2">
              <button
                onClick={() => setShowReviewModal(false)}
                className="flex-1 py-3 rounded-2xl bg-slate-200 dark:bg-slate-800 text-xs font-bold cursor-pointer"
              >
                إلغاء ومراجعة
              </button>

              <button
                onClick={handleExecuteSendToCloud}
                disabled={isSyncingCloud}
                className="flex-1 py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs shadow-xl flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50"
              >
                {isSyncingCloud ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>جاري الإرسال والمزامنة...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>تأكيد وإرسال للجهة الإدارية</span>
                  </>
                )}
              </button>
            </div>

          </div>
        </div>
      )}

      {/* PRINT MODAL 1: OTP SECURITY AUTHORIZATION SLIP FOR TEACHER */}
      {showPrintOtpSlipModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white text-slate-900 rounded-3xl p-8 max-w-xl w-full shadow-2xl space-y-6 my-8 print-page relative border-4 border-amber-500/40 dir-rtl">
            
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
                <span className="text-[10px] font-black tracking-widest uppercase text-amber-700 block">بطاقة رمز توثيق المدرس</span>
              </div>

              <div className="text-left text-xs font-mono space-y-1">
                <p>رقم الإشعار: OTP-{Math.floor(1000 + Math.random() * 9000)}</p>
                <p>التاريخ: {new Date().toLocaleDateString('ar-IQ')}</p>
              </div>
            </div>

            {/* Print Privacy Mode Selector - Hidden on Print */}
            <div className="p-3 bg-slate-100 rounded-2xl border border-slate-300 no-print space-y-2">
              <div className="flex items-center justify-between text-xs font-bold text-slate-800">
                <span className="flex items-center gap-1.5 text-amber-700">
                  <ShieldCheck className="w-4 h-4" />
                  <span>نمط الخصوصية والطباعة للمستند:</span>
                </span>
                <span className="text-[10px] text-slate-500 font-normal">اختر طريقة عرض الرمز عند الطباعة أو الإرسال</span>
              </div>

              <div className="grid grid-cols-3 gap-1.5 text-[11px] font-bold">
                <button
                  type="button"
                  onClick={() => setOtpPrintMode('barcode_only')}
                  className={`py-2 px-2 rounded-xl flex items-center justify-center gap-1 cursor-pointer transition-all ${
                    otpPrintMode === 'barcode_only'
                      ? 'bg-amber-600 text-white shadow-md'
                      : 'bg-white text-slate-700 hover:bg-slate-200 border border-slate-300'
                  }`}
                  title="حجب الرقم الصريح وطباعة الباركود والـ QR كود فقط لحماية السرية"
                >
                  <EyeOff className="w-3.5 h-3.5 text-amber-200" />
                  <span>حجب الرقم (باركود و QR)</span>
                </button>

                <button
                  type="button"
                  onClick={() => setOtpPrintMode('both')}
                  className={`py-2 px-2 rounded-xl flex items-center justify-center gap-1 cursor-pointer transition-all ${
                    otpPrintMode === 'both'
                      ? 'bg-amber-600 text-white shadow-md'
                      : 'bg-white text-slate-700 hover:bg-slate-200 border border-slate-300'
                  }`}
                  title="عرض الباركود والـ QR كود مع الأرقام الصريحة معا"
                >
                  <ScanLine className="w-3.5 h-3.5" />
                  <span>إظهار الكل (باركود + أرقام)</span>
                </button>

                <button
                  type="button"
                  onClick={() => setOtpPrintMode('digits_only')}
                  className={`py-2 px-2 rounded-xl flex items-center justify-center gap-1 cursor-pointer transition-all ${
                    otpPrintMode === 'digits_only'
                      ? 'bg-amber-600 text-white shadow-md'
                      : 'bg-white text-slate-700 hover:bg-slate-200 border border-slate-300'
                  }`}
                  title="عرض الأرقام الصريحة فقط"
                >
                  <Eye className="w-3.5 h-3.5" />
                  <span>أرقام صريحة فقط</span>
                </button>
              </div>
            </div>

            {/* Slip Content */}
            <div className="space-y-4 text-xs">
              <div className="p-4 bg-amber-50 rounded-2xl border border-amber-200 text-center space-y-2">
                <h3 className="text-sm font-black text-amber-900">بطاقة منح رمز التوثيق والاعتماد السحابي للأستاذ</h3>
                <p className="text-slate-700">تستخدم هذه البطاقة لتأكيد ربط تطبيق المدرس بسحابة المدرسة الرسمية</p>
              </div>

              <div className="p-4 bg-slate-100 rounded-2xl border border-slate-300 space-y-2 font-bold text-xs">
                <div className="flex justify-between border-b pb-1">
                  <span className="text-slate-600">اسم الأستاذ / المدرس المستهدف:</span>
                  <span className="text-blue-700 font-black text-sm">{teacherName || 'الأستاذ المعتمد'}</span>
                </div>
                <div className="flex justify-between border-b pb-1">
                  <span className="text-slate-600">المادة الدراسية:</span>
                  <span className="text-emerald-700">{selectedSubjects.join('، ')}</span>
                </div>
                <div className="flex justify-between border-b pb-1">
                  <span className="text-slate-600">الصف والشعبة المخصصة:</span>
                  <span className="text-amber-800">{selectedGrades.join('، ')} (شعبة {selectedSections.join('، ')})</span>
                </div>
              </div>

              {/* OTP CODE & BARCODE BOX */}
              <div className="p-5 bg-slate-900 text-white rounded-3xl text-center space-y-3 shadow-xl border-2 border-amber-400">
                <div className="flex items-center justify-center gap-1.5 text-amber-400 font-bold text-xs">
                  <KeyRound className="w-4 h-4" />
                  <span>
                    {otpPrintMode === 'barcode_only'
                      ? 'رمز التوثيق المشفّر بالباركود (حجب المعلومات للسرية)'
                      : 'رمز الأمان والتوثيق المعتمد (OTP Security Code):'}
                  </span>
                </div>

                {/* Plain Numbers (Shown in 'both' or 'digits_only') */}
                {(otpPrintMode === 'both' || otpPrintMode === 'digits_only') && (
                  <div className="py-1">
                    <span className="text-4xl font-black font-mono tracking-widest text-emerald-400 block">
                      {generatedOtpCode}
                    </span>
                  </div>
                )}

                {/* Masked Box Notice (Shown in 'barcode_only') */}
                {otpPrintMode === 'barcode_only' && (
                  <div className="py-2.5 px-4 bg-slate-950/80 rounded-2xl border border-amber-500/30 text-center space-y-1">
                    <div className="inline-flex items-center gap-1.5 text-amber-300 font-bold text-xs">
                      <ShieldCheck className="w-4 h-4 text-emerald-400" />
                      <span>تم حجب الأرقام الصريحة لحماية السرية عند التسليم الورقي</span>
                    </div>
                    <p className="text-[10px] text-slate-400">
                      يتم مسح الباركود أو الـ QR كود أدناه مباشرة بواسطة كاميرا تطبيق المدرس أو القارئ الضوئي
                    </p>
                  </div>
                )}

                {/* Barcode & QR Code Section (Shown in 'both' or 'barcode_only') */}
                {(otpPrintMode === 'both' || otpPrintMode === 'barcode_only') && (
                  <div className="pt-2 flex flex-col sm:flex-row items-center justify-around gap-4 bg-white/5 p-3 rounded-2xl border border-white/10">
                    <div className="text-center space-y-1">
                      <span className="text-[10px] text-slate-300 block font-bold">الرمز الشريطي (Barcode):</span>
                      <BarcodeSvg value={generatedOtpCode} height={46} className="shadow-md" />
                    </div>

                    <div className="text-center space-y-1">
                      <span className="text-[10px] text-slate-300 block font-bold">رمز القراءة السريعة (QR Code):</span>
                      <QrCodeSvg value={`OTP:${generatedOtpCode}:${teacherName}`} size={80} className="shadow-md" />
                    </div>
                  </div>
                )}

                <span className="text-[10px] text-slate-400 block pt-1">
                  {otpPrintMode === 'barcode_only'
                    ? 'يتعرف الماسح الضوئي وتطبيق المدرس آلياً على رمز الباركود والـ QR'
                    : 'صالح للاستخدام لربط وتوثيق جلسة إدخال الدرجات'}
                </span>
              </div>
            </div>

            {/* Signature Section */}
            <div className="pt-6 flex justify-between items-end text-xs border-t">
              <div className="text-center space-y-1">
                <p className="font-bold text-slate-700">توقيع المدرس المستلم:</p>
                <p className="font-black text-blue-800">{teacherName || 'الأستاذ'}</p>
                <div className="w-28 h-10 border border-dashed border-slate-300 rounded-lg"></div>
              </div>

              <div className="text-center space-y-1">
                <p className="font-bold text-slate-700">توقيع مدير المدرسة والختم:</p>
                <p className="font-black text-slate-900">{config.managerName}</p>
                <div className="w-28 h-10 border border-dashed border-slate-300 rounded-lg flex items-center justify-center text-[10px] text-slate-400">
                  ختم المدرسة
                </div>
              </div>
            </div>

            {/* Action Bar (Hidden on print) */}
            <div className="flex gap-2 pt-4 border-t no-print">
              <button
                onClick={() => setShowPrintOtpSlipModal(false)}
                className="flex-1 py-3 rounded-2xl bg-slate-200 text-slate-800 text-xs font-bold cursor-pointer"
              >
                إغلاق النافذة
              </button>

              <button
                onClick={() => window.print()}
                className="flex-1 py-3 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-black shadow-xl flex items-center justify-center gap-2 cursor-pointer"
              >
                <Printer className="w-4 h-4" />
                <span>طباعة بطاقة الرمز (A4 / Voucher)</span>
              </button>
            </div>

          </div>
        </div>
      )}

      {/* PRINT MODAL 2: TEACHER CLASS GRADE RECORD SHEET */}
      {showPrintGradeSheetModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white text-slate-900 rounded-3xl p-8 max-w-3xl w-full shadow-2xl space-y-6 my-8 print-page relative border-2 border-slate-800 dir-rtl">
            
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
                <h2 className="text-sm font-black tracking-wide text-slate-900">سجل رصد الدرجات الرسمي (دفتر المدرس)</h2>
              </div>

              <div className="text-left text-xs font-mono space-y-1">
                <p>العام الدراسي: 2024-2025</p>
                <p>التاريخ: {new Date().toLocaleDateString('ar-IQ')}</p>
              </div>
            </div>

            {/* Meta Info Bar */}
            <div className="p-3 bg-slate-100 rounded-2xl border border-slate-300 grid grid-cols-3 gap-2 text-xs font-bold">
              <div>
                <span className="text-slate-500 block text-[10px]">مدرس المادة:</span>
                <span className="text-blue-700">{teacherName || 'الأستاذ المعتمد'}</span>
              </div>
              <div>
                <span className="text-slate-500 block text-[10px]">المادة الدراسية:</span>
                <span className="text-emerald-700">{currentSubject}</span>
              </div>
              <div>
                <span className="text-slate-500 block text-[10px]">الصف والشعبة:</span>
                <span className="text-amber-800">{currentGrade} (شعبة {currentSection})</span>
              </div>
            </div>

            {/* Students Table */}
            <div className="border border-slate-400 rounded-xl overflow-hidden">
              <table className="w-full text-center border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-200 border-b border-slate-400 font-black">
                    <th className="py-2.5 px-2 border-r border-slate-400">ت</th>
                    <th className="py-2.5 px-2 border-r border-slate-400">رقم القيد</th>
                    <th className="py-2.5 px-2 border-r border-slate-400 text-right">اسم الطالب الرباعي واللقب</th>
                    <th className="py-2.5 px-2 border-r border-slate-400">الدرجة (/100)</th>
                    <th className="py-2.5 px-2 border-r border-slate-400">النتيجة والتقدير</th>
                    <th className="py-2.5 px-2">توقيع المدرس</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-300">
                  {filteredStudents.map((std, idx) => {
                    const score = sessionScores[std.id] !== undefined ? sessionScores[std.id] : (std.finalYearScore || 85);
                    const isPassed = score >= 50;
                    return (
                      <tr key={std.id} className="hover:bg-slate-50">
                        <td className="py-2 px-2 border-r border-slate-300 font-mono font-bold">{idx + 1}</td>
                        <td className="py-2 px-2 border-r border-slate-300 font-mono">{std.recordNumber}</td>
                        <td className="py-2 px-2 border-r border-slate-300 text-right font-bold text-slate-900">
                          {std.firstName} {std.secondName} {std.thirdName} {std.fourthName} {std.titleName}
                        </td>
                        <td className="py-2 px-2 border-r border-slate-300 font-black font-mono text-sm text-blue-700">
                          {score}
                        </td>
                        <td className="py-2 px-2 border-r border-slate-300 font-bold">
                          {isPassed ? (
                            <span className="text-emerald-700">ناجح</span>
                          ) : (
                            <span className="text-rose-700">راسب</span>
                          )}
                        </td>
                        <td className="py-2 px-2"></td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Signatures */}
            <div className="pt-6 flex justify-between items-end text-xs border-t">
              <div className="text-center space-y-1">
                <p className="font-bold text-slate-700">توقيع ومصادقة مدرس المادة:</p>
                <p className="font-black text-blue-800">{teacherName || 'الأستاذ'}</p>
                <div className="w-32 h-10 border border-dashed border-slate-300 rounded-lg"></div>
              </div>

              <div className="text-center space-y-1">
                <p className="font-bold text-slate-700">مصادقة مدير المدرسة والختم:</p>
                <p className="font-black text-slate-900">{config.managerName}</p>
                <div className="w-32 h-10 border border-dashed border-slate-300 rounded-lg flex items-center justify-center text-[10px] text-slate-400">
                  ختم المدرسة الرسمي
                </div>
              </div>
            </div>

            {/* Action Bar (Hidden on print) */}
            <div className="flex gap-2 pt-4 border-t no-print">
              <button
                onClick={() => setShowPrintGradeSheetModal(false)}
                className="flex-1 py-3 rounded-2xl bg-slate-200 text-slate-800 text-xs font-bold cursor-pointer"
              >
                إغلاق النافذة
              </button>

              <button
                onClick={() => window.print()}
                className="flex-1 py-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black shadow-xl flex items-center justify-center gap-2 cursor-pointer"
              >
                <Printer className="w-4 h-4" />
                <span>طباعة سجل درجات الصف (A4)</span>
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
};
