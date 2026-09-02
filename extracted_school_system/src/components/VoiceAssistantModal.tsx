import React, { useState, useEffect, useRef } from 'react';
import { Student, StaffMember, ActiveView, AppTheme, AppConfig } from '../types';
import { supabase } from '../utils/supabaseClient';
import { 
  Mic, MicOff, Sparkles, X, Loader2, Send, Home, User, Zap, Radio, MessageSquare, CheckCircle2,
  Volume2, AlertCircle
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
  isOpen, onClose, students, staffList, setActiveView, setTheme, config
}) => {
  const [isRecording, setIsRecording] = useState(false);
  const [textInputQuery, setTextInputQuery] = useState('');
  const [messages, setMessages] = useState<Array<{ role: 'user' | 'assistant'; text: string; id: string }>>([
    { role: 'assistant', text: 'أهلاً بك حضرة المدير. أنا مساعدك الإداري الذكي. يمكنك سؤالي عن إحصائيات الطلاب، الكادر، الغيابات، أو توجيه الأوامر بالصوت أو الكتابة.', id: '1' }
  ]);
  const [isAiProcessing, setIsAiProcessing] = useState(false);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
    }
  }, [messages, isAiProcessing]);

  // Fetch API key from Supabase if not in localStorage
  const getEffectiveApiKey = async (): Promise<string> => {
    let key = localStorage.getItem('gemini_api_key') || localStorage.getItem('diyala_school_gemini_key') || config?.geminiApiKey || '';
    if (!key && config?.schoolId) {
      try {
        const { data } = await supabase.from('schools').select('config').eq('id', config.schoolId).single();
        if (data?.config?.gemini_api_key) {
          key = data.config.gemini_api_key;
          localStorage.setItem('gemini_api_key', key);
        }
      } catch (e) {
        console.warn('Could not fetch cloud gemini key', e);
      }
    }
    return key;
  };

  // Local rule-based intelligent fallback engine
  const executeLocalSmartAssistant = (promptText: string): string => {
    const p = promptText.trim().toLowerCase();
    const totalStudents = students.length;
    const activeStudents = students.filter(s => s.status === 'مستمر').length;
    const totalStaff = staffList.length;
    const totalAbsences = students.reduce((acc, s) => acc + (s.absencesCount || 0), 0);

    if (p.includes('إحصائ') || p.includes('احصائ') || p.includes('كم طالب') || p.includes('عدد الطلاب')) {
      return `📊 **تقرير إحصائي شامل:**\n• إجمالي الطلاب المسجلين: **${totalStudents}** طالب\n• الطلاب المستمرون بالدوام: **${activeStudents}**\n• إجمالي الكادر التعليمي: **${totalStaff}** معلم وموظف\n• مجموع الغيابات المسجلة: **${totalAbsences}** غياب.`;
    }
    if (p.includes('منخفض') || p.includes('راسب') || p.includes('ضعيف') || p.includes('مستوى')) {
      const struggling = students.filter(s => (s.absencesCount || 0) > 5);
      if (struggling.length > 0) {
        return `⚠️ **تنبيه المتابعة:** يوجد **${struggling.length}** طلاب لديهم غيابات مرتفعة (أكثر من 5 غيابات). يرجى مراجعة سجل الغيابات لاتخاذ الإجراءات الإدارية.`;
      }
      return `✅ **مؤشر مطمئن:** لا يوجد طلاب لديهم إنذارات غياب حرجة حالياً، المستوى العام منتظم.`;
    }
    if (p.includes('نصيح') || p.includes('اقتراح') || p.includes('توجيه')) {
      return `💡 **نصيحة إدارية لليوم:** يُنصح بمتابعة سجل حضور الحصة الأولى والثانية عبر تطبيق المعلم، والتأكد من مطابقة السجلات الورقية مع السحابة دورياً.`;
    }
    if (p.includes('اعدادات') || p.includes('إعدادات') || p.includes('ضبط')) {
      setActiveView('settings');
      return `⚙️ تم فتح شاشة الإعدادات لك.`;
    }
    if (p.includes('طلاب') || p.includes('سجل')) {
      setActiveView('students');
      return `🎓 تم الانتقال إلى سجل أسماء ومعلومات الطلاب.`;
    }
    if (p.includes('كادر') || p.includes('معلم') || p.includes('مدرس')) {
      setActiveView('staff');
      return `👥 تم فتح سجل الكادر والمنتسبين.`;
    }
    if (p.includes('اتصال') || p.includes('مزامنة') || p.includes('سحاب')) {
      setActiveView('sync_center');
      return `☁️ تم الانتقال إلى مركز المزامنة السحابية.`;
    }

    return `تم استلام طلبك: "${promptText}". المدرسة تضم حالياً ${totalStudents} طالب و ${totalStaff} كادر. يمكنك استخدام الأزرار السريعة بالأسفل للتحليلات الإدارية.`;
  };

  const processAI = async (query?: string, audioBase64?: string) => {
    setIsAiProcessing(true);
    const userPrompt = query || 'رسالة صوتية';
    setMessages(prev => [...prev, { role: 'user', text: query ? query : '🎤 تم إرسال تسجيل صوتي...', id: Date.now().toString() }]);

    try {
      const apiKey = await getEffectiveApiKey();

      // 1. If API key exists, try Gemini 2.0 / 1.5 Direct REST Call
      if (apiKey && apiKey.length > 10) {
        const sysInstruction = `أنت المساعد الإداري الذكي لنظام إدارة المدارس. اسم المدرسة: ${config?.schoolName || 'المدرسة'}. إجمالي الطلاب: ${students.length}. إجمالي الكادر: ${staffList.length}. أجب بلغة عربية فصيحة، واضحة وموجزة.`;
        
        let contents: any[] = [];
        if (audioBase64) {
          contents = [
            {
              role: 'user',
              parts: [
                { inline_data: { mime_type: 'audio/webm', data: audioBase64 } },
                { text: 'أجب على هذا الطلب الصوتي الموجه من مدير المدرسة باحترافية.' }
              ]
            }
          ];
        } else {
          contents = [
            {
              role: 'user',
              parts: [{ text: `${sysInstruction}\n\nسؤال المدير: ${query}` }]
            }
          ];
        }

        const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${apiKey}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ contents })
        });

        if (res.ok) {
          const json = await res.json();
          const candidate = json.candidates?.[0]?.content?.parts?.[0]?.text;
          if (candidate) {
            setMessages(prev => [...prev, { role: 'assistant', text: candidate, id: Date.now().toString() }]);
            setIsAiProcessing(false);
            return;
          }
        }
      }

      // 2. Fallback to Local Server on port 3000 if available
      try {
        const localRes = await fetch('http://127.0.0.1:3000/api/ai-assistant', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            query, audioBase64,
            students, staff: staffList,
            schoolName: config?.schoolName,
            userApiKey: apiKey,
            config
          })
        });
        if (localRes.ok) {
          const localData = await localRes.json();
          if (localData.responseText) {
            setMessages(prev => [...prev, { role: 'assistant', text: localData.responseText, id: Date.now().toString() }]);
            if (localData.action === 'NAVIGATE' && localData.targetView) setActiveView(localData.targetView);
            setIsAiProcessing(false);
            return;
          }
        }
      } catch (err) {
        // Local server not running, continue to smart local engine
      }

      // 3. Fallback to Local Smart Assistant Engine (100% Reliable & Works Offline)
      const localReply = executeLocalSmartAssistant(userPrompt);
      setMessages(prev => [...prev, { role: 'assistant', text: localReply, id: Date.now().toString() }]);

    } catch (e: any) {
      const fallbackReply = executeLocalSmartAssistant(userPrompt);
      setMessages(prev => [...prev, { role: 'assistant', text: fallbackReply, id: Date.now().toString() }]);
    } finally {
      setIsAiProcessing(false);
    }
  };

  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      audioChunksRef.current = [];
      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) audioChunksRef.current.push(e.data);
      };
      recorder.onstop = async () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        const reader = new FileReader();
        reader.readAsDataURL(audioBlob);
        reader.onloadend = () => {
          const base64 = (reader.result as string).split(',')[1];
          processAI(undefined, base64);
        };
        stream.getTracks().forEach(track => track.stop());
      };
      recorder.start();
      mediaRecorderRef.current = recorder;
      setIsRecording(true);
    } catch (e) {
      alert('يرجى السماح بالوصول للمايكروفون لبدء التسجيل الصوتي.');
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-xl flex items-center justify-center p-4 dir-rtl">
      <div className="bg-white border-4 border-indigo-600 rounded-[3rem] shadow-2xl w-full max-w-2xl h-[85vh] flex flex-col overflow-hidden relative text-right">
        
        {/* Top Bar */}
        <div className="p-5 border-b-2 border-slate-100 bg-indigo-50 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-indigo-600 rounded-2xl shadow-lg">
              <Sparkles className="w-6 h-6 text-white animate-pulse" />
            </div>
            <div>
              <h2 className="text-xl font-black text-indigo-950">مساعد المدير الصوتي والذكي</h2>
              <p className="text-[10px] font-bold text-indigo-600 uppercase">Gemini 2.0 Pro / Ultra Hybrid v5.1</p>
            </div>
          </div>
          <div className="flex gap-2">
            <button 
              onClick={() => { setActiveView('launcher'); onClose(); }} 
              className="p-2 rounded-xl bg-white border border-slate-200 text-indigo-600 cursor-pointer hover:bg-indigo-50"
            >
              <Home className="w-5 h-5" />
            </button>
            <button 
              onClick={onClose} 
              className="p-2 rounded-xl bg-rose-50 text-rose-600 cursor-pointer hover:bg-rose-100"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Chat Area */}
        <div ref={scrollRef} className="flex-1 overflow-y-auto p-6 space-y-6 bg-slate-50/50">
          {messages.map((m) => (
            <div key={m.id} className={`flex ${m.role === 'user' ? 'justify-start' : 'justify-end'} animate-in fade-in slide-in-from-bottom-2`}>
              <div className={`max-w-[85%] p-5 rounded-[2rem] shadow-md border ${
                m.role === 'user' 
                  ? 'bg-white border-slate-200 text-slate-900 rounded-br-none' 
                  : 'bg-gradient-to-br from-indigo-600 to-blue-700 border-indigo-500 text-white rounded-bl-none'
              }`}>
                <div className="flex items-center gap-2 mb-1.5 opacity-70">
                  {m.role === 'user' ? <User className="w-3.5 h-3.5" /> : <Sparkles className="w-3.5 h-3.5" />}
                  <span className="text-[10px] font-black uppercase tracking-widest">{m.role === 'user' ? 'المدير' : 'المساعد الذكي'}</span>
                </div>
                <p className="text-sm md:text-base font-bold leading-relaxed whitespace-pre-wrap">{m.text}</p>
              </div>
            </div>
          ))}

          {isAiProcessing && (
            <div className="flex justify-end">
              <div className="bg-indigo-100 p-4 rounded-3xl flex items-center gap-3 animate-pulse border border-indigo-200">
                <Loader2 className="w-5 h-5 text-indigo-600 animate-spin" />
                <span className="text-xs font-black text-indigo-700">جاري التحليل والمعالجة الذكية...</span>
              </div>
            </div>
          )}
        </div>

        {/* Quick Action Chips */}
        <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 flex gap-2 overflow-x-auto no-scrollbar shrink-0">
          <button
            onClick={() => processAI("أعطني إحصائية سريعة وشاملة عن المدرسة")}
            className="px-4 py-2 bg-white border border-slate-200 rounded-full text-[11px] font-black text-slate-700 hover:border-indigo-500 hover:text-indigo-600 transition-all shrink-0 cursor-pointer shadow-sm"
          >
            📊 إحصائيات عامة
          </button>
          <button
            onClick={() => processAI("هل هناك طلاب لديهم إنذارات غياب أو مستوى منخفض؟")}
            className="px-4 py-2 bg-white border border-slate-200 rounded-full text-[11px] font-black text-slate-700 hover:border-indigo-500 hover:text-indigo-600 transition-all shrink-0 cursor-pointer shadow-sm"
          >
            ⚠️ متابعة الغيابات
          </button>
          <button
            onClick={() => processAI("اقترح عليّ نصيحة إدارية لليوم")}
            className="px-4 py-2 bg-white border border-slate-200 rounded-full text-[11px] font-black text-slate-700 hover:border-indigo-500 hover:text-indigo-600 transition-all shrink-0 cursor-pointer shadow-sm"
          >
            💡 نصيحة إدارية
          </button>
          <button
            onClick={() => { setActiveView('approval_dashboard'); onClose(); }}
            className="px-4 py-2 bg-emerald-50 border border-emerald-200 rounded-full text-[11px] font-black text-emerald-800 hover:bg-emerald-100 transition-all shrink-0 cursor-pointer shadow-sm"
          >
            📱 الطلاب المتصلون
          </button>
        </div>

        {/* Bottom Input Area with Hold-to-Talk and Direct Send */}
        <div className="p-6 bg-white border-t-2 border-slate-100 shrink-0">
          <div className="flex items-center gap-3 mb-4">
            <button 
              onMouseDown={startRecording} 
              onMouseUp={stopRecording}
              onTouchStart={startRecording}
              onTouchEnd={stopRecording}
              title="اضغط مطولاً للتحدث بالصوت"
              className={`w-16 h-16 rounded-2xl flex items-center justify-center transition-all shadow-xl cursor-pointer shrink-0 ${
                isRecording 
                  ? 'bg-rose-600 text-white animate-pulse scale-105 ring-4 ring-rose-200' 
                  : 'bg-amber-400 text-slate-950 hover:bg-amber-500 hover:scale-105'
              }`}
            >
              {isRecording ? <Mic className="w-8 h-8" /> : <MicOff className="w-8 h-8" />}
            </button>

            <form 
              onSubmit={(e) => { 
                e.preventDefault(); 
                if (textInputQuery.trim()) { 
                  processAI(textInputQuery); 
                  setTextInputQuery(''); 
                } 
              }} 
              className="flex-1 relative"
            >
              <input 
                type="text" 
                value={textInputQuery} 
                onChange={(e) => setTextInputQuery(e.target.value)} 
                placeholder="اكتب سؤالك أو اضغط مطولاً على المايك الصوتي..." 
                className="w-full bg-slate-100 border-2 border-slate-200 rounded-[1.5rem] py-4 pr-5 pl-20 text-sm font-bold text-slate-900 focus:outline-none focus:border-indigo-500 focus:bg-white transition-all shadow-inner"
              />
              <button 
                type="submit" 
                disabled={isAiProcessing || !textInputQuery.trim()} 
                className="absolute left-2 top-2 bottom-2 px-5 bg-indigo-600 disabled:bg-slate-300 text-white rounded-xl font-black text-xs hover:bg-indigo-700 transition-all cursor-pointer shadow-md"
              >
                إرسال
              </button>
            </form>
          </div>

          <div className="flex justify-center gap-6 text-[10px] font-black text-slate-400 uppercase tracking-tighter">
            <span className="flex items-center gap-1.5"><Radio className={`w-3.5 h-3.5 ${isRecording ? 'text-rose-500' : ''}`} /> {isRecording ? 'جاري التسجيل الصوتي...' : 'صوتي (اضغط للتحدث)'}</span>
            <span className="flex items-center gap-1.5"><MessageSquare className="w-3.5 h-3.5" /> كتابي</span>
            <span className="flex items-center gap-1.5 text-indigo-600"><CheckCircle2 className="w-3.5 h-3.5" /> استجابة هجينة فورية</span>
          </div>
        </div>
      </div>
    </div>
  );
};
