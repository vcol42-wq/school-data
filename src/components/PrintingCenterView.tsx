import React, { useState, useRef } from 'react';
import { OfficialDocument, AppConfig } from '../types';
import { 
  Printer, 
  FileText, 
  Plus, 
  Archive, 
  Edit3, 
  Save, 
  X, 
  CheckCircle2, 
  Sparkles,
  Building2,
  FileCheck,
  Award,
  AlertTriangle,
  FileSpreadsheet,
  Stethoscope,
  UserCheck,
  ShieldAlert,
  FilePlus2,
  Undo2,
  Redo2,
  Bold,
  Italic,
  Underline as UnderlineIcon,
  AlignRight,
  AlignCenter,
  AlignLeft,
  AlignJustify,
  Tag,
  Palette,
  Type,
  Code
} from 'lucide-react';
import { OFFICIAL_SEAL_DATA_URI } from '../assets/officialSealDataUri';
import { PrintPreviewModal } from './PrintPreviewModal';
import { printElement } from '../utils/printHelper';

interface PrintingCenterViewProps {
  documents: OfficialDocument[];
  setDocuments: React.Dispatch<React.SetStateAction<OfficialDocument[]>>;
  config: AppConfig;
}


interface ErrorBoundaryProps {
  children: React.ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

class PrintingCenterErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  declare props: ErrorBoundaryProps;

  constructor(props: ErrorBoundaryProps) {
    super(props);
  }

  state: ErrorBoundaryState = {
    hasError: false,
    error: null
  };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('PrintingCenter error caught:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="max-w-4xl mx-auto my-12 p-8 bg-amber-50 border-2 border-amber-400 rounded-2xl text-center space-y-4 font-amiri shadow-xl">
          <div className="w-16 h-16 bg-amber-100 text-amber-700 rounded-full flex items-center justify-center mx-auto text-2xl font-bold">
            ⚠️
          </div>
          <h2 className="text-2xl font-black text-amber-900">نظام حماية مركز الطباعة والوثائق الرسمية</h2>
          <p className="text-sm text-slate-700 font-bold max-w-lg mx-auto">
            تم رصد حالة استثنائية في البيانات المخزنة مؤقتاً لقوالب الطباعة ({this.state.error?.message || 'خطأ في التنسيق'}).
          </p>
          <div className="pt-4 flex flex-wrap justify-center gap-3">
            <button
              onClick={() => {
                localStorage.removeItem('diyala_school_documents');
                window.location.reload();
              }}
              className="px-6 py-3 bg-amber-600 hover:bg-amber-700 text-white font-black rounded-xl transition-all shadow-md cursor-pointer text-sm"
            >
              🔄 إعادة تحديث واسترجاع القوالب الرسمية الأصلية
            </button>
          </div>
        </div>
      );
    }
    return (this as any).props.children;
  }
}

const PrintingCenterViewInner: React.FC<PrintingCenterViewProps> = ({
  documents,
  setDocuments,
  config
}) => {
  const [selectedDoc, setSelectedDoc] = useState<OfficialDocument>((documents && (documents || []).length > 0 && documents[0]) ? documents[0] : {
    id: 'doc-default',
    title: 'كتاب إنذار غيابات طالب',
    templateType: 'custom',
    refNumber: '',
    date: new Date().toISOString().split('T')[0],
    recipient: 'إلى ولي أمر الطالب المحترم: {اسم_الطالب}',
    subject: 'غيابات الطالب {اسم_الطالب}',
    bodyContent: 'نود إعلامكم بأن مجموع غيابات الطالب <b>{اسم_الطالب}</b> في الصف <b>{الصف_والشعبة}</b> بلغ <b>({عدد_الغيابات})</b> يوماً لغاية تاريخ اليوم، وقد تم خصم درجات المواظبة على الدوام.<br/><br/>نحيطكم علماً بأنه في حال استمرار الطالب في الغياب، فسيعتبر راسباً في صفه لهذا العام عملاً بأحكام المادة (45) المعدلة من نظام المدارس الثانوية.<br/><br/>نرجو تحمل المسؤولية اتجاه أبنائكم مع التقدير.',
    managerTitle: 'مدير المدرسة',
    managerName: (config?.managerName || 'مدير المدرسة'),
    watermarkText: `إدارة: ${(config?.schoolName || 'م. كعب بن مالك المسائية للبنين')}`
  });

  const [isEditing, setIsEditing] = useState(true);
  const [activeTab, setActiveTab] = useState<'editor' | 'archive' | 'prohibitions'>('editor');
  const [showPrintPreview, setShowPrintPreview] = useState(false);
  
  // Sample Data for Merge Tags Testing
  const [sampleStudent, setSampleStudent] = useState({
    name: 'أحمد علي حسين الكرخي',
    grade: 'الثالث متوسط (أ)',
    absences: '12',
    teacher: 'الأستاذ محمد جاسم',
    recordNo: '1042',
    status: 'مستمر بالدوام الرسمي'
  });

  const [editorMode, setEditorMode] = useState<'wysiwyg' | 'raw_html'>('wysiwyg');
  const editorRef = useRef<HTMLDivElement>(null);

  // Exact Templates from "ارشيف طباعة.docx"
  const docxTemplates: Array<{
    id: string;
    title: string;
    icon: string;
    recipient: string;
    subject: string;
    body: string;
    hasTable?: boolean;
    copiesTo?: string[];
  }> = [
    {
      id: 'absent_single',
      title: 'إنذار غيابات طالب فردي',
      icon: '📜',
      recipient: 'إلى ولي أمر الطالب المحترم: {اسم_الطالب}',
      subject: 'إنذار غيابات',
      body: 'نود إعلامكم بأن مجموع غيابات الطالب <b>{اسم_الطالب}</b> في الصف <b>{الصف_والشعبة}</b> بلغ <b>({عدد_الغيابات})</b> يوماً لغاية اليوم، وقد تم خصم درجات المواظبة على الدوام.<br/><br/>نحيطكم علماً بأنه في حال استمرار الطالب في الغياب، فسيعتبر راسباً في صفه لهذا العام عملاً بالمادة (45) المعدلة من نظام المدارس الثانوية.<br/><br/>نرجو تحمل المسؤولية اتجاه ابنائكم مع التقدير.',
      copiesTo: ['ملف الطالب', 'لوحة الإعلانات', 'المدير']
    },
    {
      id: 'absent_table',
      title: 'إنذار غيابات مجمع لأولياء الأمور',
      icon: '📋',
      recipient: 'إلى أولياء الأمور الكرام.....',
      subject: 'غيابات الطلبة المجمعة',
      body: 'نود إعلامكم بغيابات أبنائكم المدرجة أسماؤهم وعدد غياباتهم في أدناه ونحيطكم علماً بأنه في حال استمرار الغياب، فسيعتبر الطالب راسباً في صفه لهذا العام.<br/><br/>يرجى من أولياء الأمور الكرام متابعة أبنائهم وحثهم على الدوام الرسمي..',
      hasTable: true,
      copiesTo: ['سجل الانضباط', 'ملف الغيابات الموحد']
    },
    {
      id: 'failure_order',
      title: 'أمر مدرسي (رسوب بسبب الغياب)',
      icon: '🚨',
      recipient: 'إلى أولياء الأمور الكرام.....',
      subject: 'أمر مدرسي (رسوب بسبب الغياب)',
      body: 'نود إعلامكم بأن غيابات أبنائكم المدرجة أسماؤهم في أدناه قد تجاوزت الحد المسموح به قانونياً، لذلك يعتبر كل منهم راسباً في صفه لهذا العام، وذلك عملاً بالمادة (45) المعدلة من نظام المدارس الثانوية. ويثبت ذلك في القيد العام للمدرسة، للعلم مع التقدير...',
      hasTable: true,
      copiesTo: ['القيد العام', 'السجل الوسطي للمدرسة', 'شعبة الامتحانات']
    },
    {
      id: 'leave_request',
      title: 'طلب إجازة اعتيادية لكادر',
      icon: '✏️',
      recipient: 'السيد مدير المدرسة المحترم ..',
      subject: 'طلب إجازة اعتيادية',
      body: 'تحية طيبة ..<br/>يرجى التفضل بمنحي إجازة اعتيادية لمدة (...................) ابتداءً من يوم (...................) الموافق (    /    / 2026م) ولغاية يوم (...................) الموافق (    /    / 2026م) وذلك بسبب (....................................................................).<br/><br/>علماً إني قد كلفت السيد (.....................................) للقيام بمهام أعمالي في فترة إجازتي.<br/>هذا ولكم الأمر مع التقدير........<br/><br/>اسم وتوقيع البديل إن وجد: .......................................<br/>اسم وتوقيع مقدم الطلب: .......................................',
      copiesTo: ['ملف المنتسب الإداري']
    },
    {
      id: 'staff_direct',
      title: 'كتاب مباشرة أستاذ / معلم',
      icon: '🏢',
      recipient: `إلى / ${(config?.directorateName || 'مديرية تربية ديالى')} / قسم الموارد البشرية`,
      subject: 'مباشرة كادر تدريسي',
      body: 'تحية طيبة ...<br/>إشارة إلى كتابكم المرقم (...................) في (    /    / 2026م) باشر السيد / السيدة <b>{اسم_المعلم}</b> اختصاص (............................) في يوم (................) الموافق (    /    / 2026م) (قبل الظهيرة / بعد الظهيرة).<br/><br/>للتفضل بالعلم واتخاذ اللازم مع التقدير......',
      copiesTo: ['قسم الموارد البشرية', 'سجل المباشرات']
    },
    {
      id: 'cutoff_relation',
      title: 'كتاب قطع علاقة طالب',
      icon: '✂️',
      recipient: 'إلى / الجهة المعنية',
      subject: 'قطع علاقة طالب',
      body: 'نؤيد لكم أن الطالب <b>{اسم_الطالب}</b> أحد طلاب مدرستنا في الصف <b>{الصف_والشعبة}</b> للعام الدراسي (2025-2026) وقطع علاقته بالمدرسة بتاريخ (    /    / 2026م).<br/><br/>للعلم مع التقدير.....',
      copiesTo: ['أرشيف الطالب', 'شعبة شؤون الطلبة']
    },
    {
      id: 'health_center',
      title: 'كتاب إحالة وفحص للمركز الصحي',
      icon: '🏥',
      recipient: 'إلى / المركز الصحي المحترم',
      subject: 'فحص ومعالجة طالب',
      body: 'يرجى فحص ومعالجة الطالب <b>{اسم_الطالب}</b> أحد طلاب مدرستنا في الصف <b>{الصف_والشعبة}</b> وإعلامنا بالنتيجة مع التقدير..<br/><br/>ملاحظات الطبيب الفاحص:<br/>........................................................................................................................................................................',
      copiesTo: ['سجل وحدة الصحة المدرسية']
    },
    {
      id: 'student_status',
      title: 'استمارة كتاب واقع حال طالب',
      icon: '📄',
      recipient: 'إلى / الجهة الموجه إليها',
      subject: 'واقع حال طالب',
      body: 'تحية طيبة ....<br/>نبين لكم أدناه واقع حال الطالب المطلوب:<br/>١- الاسم الثلاثي واللقب: <b>{اسم_الطالب}</b><br/>٢- الصف والشعبة: <b>{الصف_والشعبة}</b><br/>٣- المواليد الكاملة: ........................................................<br/>٤- سنوات الرسوب: ........................................................\n٥- سنوات التأجيل إن وجدت: ........................................................\n٦- موقف الطالب حالياً: <b>{موقف_الطالب}</b><br/>٧- الملاحظات الإدارية: ........................................................',
      copiesTo: ['أرشيف واقع حال الطلبة']
    },
    {
      id: 'accept_student',
      title: 'كتاب لا مانع من قبول طالب واستدعاء وثيقة',
      icon: '✅',
      recipient: 'إلى إدارة المدرسة المحترمة //',
      subject: 'قبول طالب وتزويد بالوثيقة',
      body: 'تحية طيبة ...<br/>لا مانع لدينا من قبول الطالب <b>{اسم_الطالب}</b> في مدرستنا في الصف <b>{الصف_والشعبة}</b> للعام الدراسي (2025-2026).<br/><br/>يرجى تفضلكم بتزويدنا بالوثيقة والبطاقة المدرسية الخاصة بالطالب مع فائق التقدير ....',
      copiesTo: ['سجل المقبولين الجدد']
    },
    {
      id: 'doc_validity',
      title: 'كتاب صحة صدور وثيقة طالب',
      icon: '📜',
      recipient: `إلى ${(config?.directorateName || 'مديرية تربية ديالى')} / قسم الامتحانات`,
      subject: 'صحة صدور وثيقة',
      body: 'تحية طيبة ..<br/>نؤيد لكم صحة صدور الوثيقة المرقمة (..................) وذي العدد (...................) في (    /    / 2026م) العائدة للطالب <b>{اسم_الطالب}</b> في الصف <b>{الصف_والشعبة}</b> والصادرة من مدرستنا وتتحمل إدارة المدرسة صحة المعلومات الواردة فيها.<br/><br/>للعلم مع التقدير.....',
      copiesTo: ['قسم الامتحانات', 'أرشيف صحة الصدور']
    },
    {
      id: 'sick_leave',
      title: 'كتاب إجازة مرضية لكادر مدرسة',
      icon: '🩺',
      recipient: `إلى / ${(config?.directorateName || 'مديرية تربية ديالى')} / قسم الموارد البشرية`,
      subject: 'إجازة مرضية لكادر تدريسي',
      body: 'تحية طيبة ...<br/>إشارة إلى كتاب المستشفى العام المرقم (...................) في (    /    / 2026م) منح السيد / السيدة <b>{اسم_المعلم}</b> إجازة مرضية لمدة (...................) اعتباراً من تاريخ (    /    / 2026م).<br/><br/>للتفضل بالعلم واتخاذ اللازم مع التقدير......',
      copiesTo: ['قسم الموارد البشرية', 'سجل الإجازات المرضية']
    }
  ];

  // Replace Merge Tags with Actual Live Data
  const replacePlaceholders = (text: string) => {
    if (!text) return '';
    return text
      .replace(/\{اسم_الطالب\}/g, sampleStudent.name)
      .replace(/\{الصف_والشعبة\}/g, sampleStudent.grade)
      .replace(/\{عدد_الغيابات\}/g, sampleStudent.absences)
      .replace(/\{اسم_المعلم\}/g, sampleStudent.teacher)
      .replace(/\{اسم_المدرسة\}/g, (config?.schoolName || 'م. كعب بن مالك المسائية للبنين'))
      .replace(/\{اسم_المدير\}/g, (config?.managerName || 'مدير المدرسة'))
      .replace(/\{التاريخ\}/g, selectedDoc?.date || new Date().toISOString().split('T')[0])
      .replace(/\{العدد\}/g, selectedDoc?.refNumber || '.......')
      .replace(/\{رقم_القيد\}/g, sampleStudent.recordNo)
      .replace(/\{موقف_الطالب\}/g, sampleStudent.status);
  };

  // Rich Text Formatting Executer
  const executeFormat = (command: string, value: string | undefined = undefined) => {
    if (editorRef.current) {
      editorRef.current.focus();
    }
    document.execCommand(command, false, value);
    if (editorRef.current) {
      const html = editorRef.current.innerHTML;
      setSelectedDoc(prev => ({ ...prev, bodyContent: html }));
    }
  };

  // Insert Merge Tag Chip Into Active Text Area or Cursor Position
  const insertMergeTag = (tag: string) => {
    if (isEditing && editorRef.current && editorMode === 'wysiwyg') {
      editorRef.current.focus();
      const sel = window.getSelection();
      if (sel && sel.rangeCount > 0) {
        const range = sel.getRangeAt(0);
        range.deleteContents();
        const textNode = document.createTextNode(` ${tag} `);
        range.insertNode(textNode);
        range.setStartAfter(textNode);
        range.setEndAfter(textNode);
        sel.removeAllRanges();
        sel.addRange(range);
        setSelectedDoc(prev => ({
          ...prev,
          bodyContent: editorRef.current?.innerHTML || prev.bodyContent
        }));
        return;
      }
    }
    // Fallback append to state
    setSelectedDoc(prev => ({
      ...prev,
      bodyContent: prev.bodyContent + ` ${tag} `
    }));
  };

  // Insert HTML Table snippet into template
  const insertTable = () => {
    const tableHTML = `
      <table style="width:100%; border-collapse:collapse; margin:10px 0; border:1px solid #1e293b;">
        <thead>
          <tr style="background-color:#0f172a; color:#ffffff;">
            <th style="padding:6px; border:1px solid #334155;">ت</th>
            <th style="padding:6px; border:1px solid #334155; text-align:right;">اسم الطالب</th>
            <th style="padding:6px; border:1px solid #334155;">الصف والشعبة</th>
            <th style="padding:6px; border:1px solid #334155;">الملاحظات</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td style="padding:6px; border:1px solid #cbd5e1; text-align:center;">1</td>
            <td style="padding:6px; border:1px solid #cbd5e1;">{اسم_الطالب}</td>
            <td style="padding:6px; border:1px solid #cbd5e1; text-align:center;">{الصف_والشعبة}</td>
            <td style="padding:6px; border:1px solid #cbd5e1; text-align:center;">غياب {عدد_الغيابات} أيام</td>
          </tr>
        </tbody>
      </table>
    `;
    executeFormat('insertHTML', tableHTML);
  };

  // Create New Custom Template
  const handleCreateNewTemplate = () => {
    const newDoc: OfficialDocument = {
      id: `custom-template-${Date.now()}`,
      title: 'قالب رسمي مخصص جديد',
      templateType: 'custom',
      refNumber: '',
      date: new Date().toISOString().split('T')[0],
      recipient: 'إلى / ولي أمر الطالب المحترم: {اسم_الطالب}',
      subject: 'عنوان موضوع القالب الجديد',
      bodyContent: 'أدخل نص ومضمون القالب هنا واكتسِ بالحرية التامة للتنسيق واستخدام رموز الاستبدال مثل <b>{اسم_الطالب}</b> و <b>{الصف_والشعبة}</b>...',
      managerTitle: 'مدير المدرسة',
      managerName: (config?.managerName || 'مدير المدرسة'),
      watermarkText: `إدارة: ${(config?.schoolName || 'م. كعب بن مالك المسائية للبنين')}`
    };
    setSelectedDoc(newDoc);
    setDocuments(prev => [newDoc, ...prev]);
    setIsEditing(true);
  };

  // Select Quick Template
  const handleSelectDocxTemplate = (tpl: typeof docxTemplates[0]) => {
    setSelectedDoc({
      id: `doc-${Date.now()}`,
      title: tpl.title,
      templateType: tpl.id as any,
      refNumber: '',
      date: new Date().toISOString().split('T')[0],
      recipient: tpl.recipient,
      subject: tpl.subject,
      bodyContent: tpl.body,
      managerTitle: 'مدير المدرسة',
      managerName: (config?.managerName || 'مدير المدرسة'),
      watermarkText: `إدارة: ${(config?.schoolName || 'م. كعب بن مالك المسائية للبنين')}`
    });
  };

  // Save to Archive
  const handleSaveToArchive = () => {
    const existingIdx = (documents || []).findIndex(d => d.id === selectedDoc.id);
    if (existingIdx >= 0) {
      setDocuments(prev => prev.map((d, i) => i === existingIdx ? selectedDoc : d));
    } else {
      setDocuments(prev => [selectedDoc, ...prev]);
    }
    alert('تم بنجاح حفظ نص وتنسيق القالب في قاعدة بيانات الأرشيف (HTML Rich Text System)!');
  };

  // External Print File Path State stored in LocalStorage
  const [externalPrintPath, setExternalPrintPath] = useState<string>(() => {
    return localStorage.getItem('external_print_file_path') || '';
  });

  // Change External Print File Path Handler
  const handleChangeExternalPrintPath = async () => {
    try {
      if (window.require) {
        const { ipcRenderer } = window.require('electron');
        const selected = await ipcRenderer.invoke('select-external-print-file');
        if (selected) {
          setExternalPrintPath(selected);
          localStorage.setItem('external_print_file_path', selected);
        }
      } else {
        alert('ميزة اختيار ملف الطباعة الخارجي متاحة داخل برنامج سطح المكتب Electron');
      }
    } catch (err) {
      console.error(err);
    }
  };

  // Handle External Print Launcher Button
  const handleExternalPrintLaunch = async () => {
    try {
      if (window.require) {
        const { ipcRenderer } = window.require('electron');
        if (!externalPrintPath) {
          const selected = await ipcRenderer.invoke('select-external-print-file');
          if (selected) {
            setExternalPrintPath(selected);
            localStorage.setItem('external_print_file_path', selected);
            await ipcRenderer.invoke('open-external-print-file', selected);
          }
        } else {
          const opened = await ipcRenderer.invoke('open-external-print-file', externalPrintPath);
          if (!opened) {
            // Path broken or moved, re-select
            const selected = await ipcRenderer.invoke('select-external-print-file');
            if (selected) {
              setExternalPrintPath(selected);
              localStorage.setItem('external_print_file_path', selected);
              await ipcRenderer.invoke('open-external-print-file', selected);
            }
          }
        }
      } else {
        alert('ميزة فتح ملف الطباعة الخارجي متاحة داخل برنامج سطح المكتب Electron');
      }
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="printing-center-root font-amiri max-w-7xl mx-auto px-4 py-6 space-y-6">
      
      {/* Top Action Header */}
      <div className="bg-[var(--theme-card)] p-5 rounded-2xl border border-[var(--theme-card-border)] shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4 no-print">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-50 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-300 text-xs font-bold mb-2">
            <Sparkles className="w-3.5 h-3.5 text-amber-500" />
            <span>نظام محرر القوالب القابلة للتحرير بالكامل (Rich Text / HTML Engine)</span>
          </div>
          <h2 className="text-xl md:text-2xl font-black text-[var(--theme-text-main)]">
            مركز طباعة القوالب المرن (Word Rich Text Editor + رموز الاستبدال)
          </h2>
          <p className="text-xs text-[var(--theme-text-muted)] mt-1">
            اكتب وعدل وسنسق القالب كلياً بتنسيقات Word المتقدمة وإدراج رموز الاستبدال والطباعة المباشرة
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* External Print Launcher Button */}
          <div className="relative group">
            <button
              onClick={handleExternalPrintLaunch}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-black transition-all shadow cursor-pointer border border-purple-600"
              title={externalPrintPath ? `مسار الملف المحدد: ${externalPrintPath}` : 'حدد ملف طباعة خارجي لتشغيله مباشرة'}
            >
              <Printer className="w-4 h-4 text-amber-300" />
              <span>{externalPrintPath ? '📂 طباعة خارجي (الملف المحفوظ)' : '📂 طباعة من مصدر خارجي'}</span>
            </button>
            {externalPrintPath && (
              <button
                onClick={handleChangeExternalPrintPath}
                className="mr-1 px-2 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-amber-300 text-[10px] font-bold border border-slate-700 cursor-pointer"
                title="تغيير مسار ملف الطباعة الخارجي"
              >
                تغيير المسار ⚙️
              </button>
            )}
          </div>

          <button
            onClick={handleCreateNewTemplate}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black transition-all shadow cursor-pointer border border-emerald-500"
          >
            <FilePlus2 className="w-4 h-4 text-amber-300" />
            <span>إنشاء قالب جديد ➕</span>
          </button>

          <button
            onClick={() => setIsEditing(!isEditing)}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-black transition-all shadow cursor-pointer border ${
              isEditing ? 'bg-amber-400 text-slate-950 border-amber-500' : 'bg-slate-800 text-white border-slate-700'
            }`}
          >
            <Edit3 className="w-4 h-4" />
            <span>{isEditing ? 'معاينة ورقة A4 النهائية' : 'تعديل وتنسيق القالب'}</span>
          </button>

          <button
            onClick={handleSaveToArchive}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-amber-500 hover:bg-amber-600 text-slate-950 text-xs font-black transition-all shadow cursor-pointer border border-amber-400"
          >
            <Save className="w-4 h-4" />
            <span>حفظ القالب بالأرشيف</span>
          </button>

          <button
            onClick={() => setShowPrintPreview(true)}
            className="flex items-center gap-1.5 px-5 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-blue-600 hover:from-emerald-500 hover:to-blue-500 text-white text-xs font-black transition-all shadow-lg cursor-pointer border border-emerald-400/40"
          >
            <Printer className="w-4 h-4 text-amber-300" />
            <span>معاينة وطباعة الوثيقة</span>
          </button>
        </div>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        
        {/* Left Sidebar: Placeholders & Quick Templates & Archive Log */}
        <div className="space-y-4 no-print">
          
          {/* Quick Merge Tags / Placeholders Selector */}
          <div className="bg-gradient-to-r from-sky-900 to-indigo-950 p-4 rounded-2xl text-white shadow-md space-y-3 border border-sky-700">
            <h3 className="text-xs font-black flex items-center gap-2 text-amber-300">
              <Tag className="w-4 h-4" />
              <span>رموز الاستبدال التلقائية (Merge Tags):</span>
            </h3>
            <p className="text-[10px] text-sky-200 leading-normal">
              اضغط على أي رمز لإدراجه تلقائياً في موضع الكتابة بالقالب:
            </p>

            <div className="flex flex-wrap gap-1.5 text-[11px] font-bold">
              {[
                { tag: '{اسم_الطالب}', label: 'اسم الطالب' },
                { tag: '{الصف_والشعبة}', label: 'الصف والشعبة' },
                { tag: '{عدد_الغيابات}', label: 'عدد الغيابات' },
                { tag: '{اسم_المعلم}', label: 'اسم المعلم' },
                { tag: '{اسم_المدرسة}', label: 'اسم المدرسة' },
                { tag: '{اسم_المدير}', label: 'اسم المدير' },
                { tag: '{التاريخ}', label: 'التاريخ' },
                { tag: '{العدد}', label: 'العدد' },
                { tag: '{رقم_القيد}', label: 'رقم القيد' },
                { tag: '{موقف_الطالب}', label: 'موقف الطالب' }
              ].map(item => (
                <button
                  key={item.tag}
                  onClick={() => insertMergeTag(item.tag)}
                  className="px-2 py-1 rounded-lg bg-sky-800/80 hover:bg-amber-400 hover:text-slate-950 text-sky-100 transition-all border border-sky-600 cursor-pointer"
                  title={`إدراج ${item.label}`}
                >
                  + {item.tag}
                </button>
              ))}
            </div>

            {/* Live Student Sample Data Tester Inputs */}
            <div className="pt-2 border-t border-sky-800 space-y-2 text-[10px]">
              <span className="font-black text-amber-300 block">اختبار استبدال البيانات التلقائي:</span>
              <div className="grid grid-cols-2 gap-1.5">
                <input
                  type="text"
                  placeholder="اسم الطالب"
                  value={sampleStudent.name}
                  onChange={e => setSampleStudent(p => ({ ...p, name: e.target.value }))}
                  className="p-1.5 rounded bg-sky-950 border border-sky-700 text-white font-bold text-[10px]"
                />
                <input
                  type="text"
                  placeholder="الصف"
                  value={sampleStudent.grade}
                  onChange={e => setSampleStudent(p => ({ ...p, grade: e.target.value }))}
                  className="p-1.5 rounded bg-sky-950 border border-sky-700 text-white font-bold text-[10px]"
                />
                <input
                  type="text"
                  placeholder="عدد الغيابات"
                  value={sampleStudent.absences}
                  onChange={e => setSampleStudent(p => ({ ...p, absences: e.target.value }))}
                  className="p-1.5 rounded bg-sky-950 border border-sky-700 text-white font-bold text-[10px]"
                />
                <input
                  type="text"
                  placeholder="المعلم"
                  value={sampleStudent.teacher}
                  onChange={e => setSampleStudent(p => ({ ...p, teacher: e.target.value }))}
                  className="p-1.5 rounded bg-sky-950 border border-sky-700 text-white font-bold text-[10px]"
                />
                <input
                  type="text"
                  placeholder="رقم القيد"
                  value={sampleStudent.recordNo}
                  onChange={e => setSampleStudent(p => ({ ...p, recordNo: e.target.value }))}
                  className="p-1.5 rounded bg-sky-950 border border-sky-700 text-white font-bold text-[10px]"
                />
                <input
                  type="text"
                  placeholder="موقف الطالب"
                  value={sampleStudent.status}
                  onChange={e => setSampleStudent(p => ({ ...p, status: e.target.value }))}
                  className="p-1.5 rounded bg-sky-950 border border-sky-700 text-white font-bold text-[10px]"
                />
              </div>
            </div>
          </div>

          {/* Quick 11 Templates Selector */}
          <div className="bg-[var(--theme-card)] p-4 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-3">
            <h3 className="text-sm font-black text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
              <FileCheck className="w-4 h-4 text-amber-500" />
              <span>قائمة القوالب الجاهزة:</span>
            </h3>

            <div className="space-y-1.5 max-h-72 overflow-y-auto no-scrollbar text-xs font-bold">
              {docxTemplates.map((tpl) => (
                <button
                  key={tpl.id}
                  type="button"
                  onClick={() => handleSelectDocxTemplate(tpl)}
                  className={`w-full text-right p-3 rounded-xl transition-all block border-2 ${
                    selectedDoc?.templateType === tpl.id
                      ? 'bg-amber-400 text-slate-950 border-amber-500 font-black shadow-md'
                      : 'bg-white text-slate-900 border-slate-200 hover:bg-amber-100 hover:border-amber-400 hover:text-amber-950 font-bold'
                  }`}
                >
                  <span className="ml-1.5">{tpl.icon}</span>
                  <span>{tpl.title}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Archived Letters & Templates Log */}
          <div className="bg-[var(--theme-card)] p-4 rounded-2xl border border-[var(--theme-card-border)] shadow-sm space-y-3">
            <h3 className="text-sm font-black text-[var(--theme-text-main)] border-b pb-2 flex items-center gap-2">
              <Archive className="w-4 h-4 text-blue-500" />
              <span>سجل القوالب المحفوظة بالكامل:</span>
            </h3>

            <div className="space-y-2 max-h-48 overflow-y-auto no-scrollbar text-xs">
              {(documents || []).length === 0 ? (
                <div className="p-3 text-center text-slate-500 text-[11px] font-bold">لا توجد قوالب محفوظة.</div>
              ) : (
                (documents || []).map((doc) => (
                  <div
                    key={doc.id}
                    onClick={() => setSelectedDoc(doc)}
                    className={`p-3 rounded-xl border-2 cursor-pointer transition-all ${
                      selectedDoc?.id === doc.id
                        ? 'bg-blue-600 text-white border-blue-700 font-black shadow-md'
                        : 'bg-white text-slate-900 border-slate-200 hover:bg-amber-100 hover:border-amber-400 hover:text-amber-950 font-bold'
                    }`}
                  >
                    <span className="block font-bold">{doc.title}</span>
                    <span className="text-[10px] text-slate-500 block mt-0.5 font-mono">{doc.date}</span>
                  </div>
                ))
              )}
            </div>
          </div>

        </div>

        {/* Right Main Content: Rich Text WORD-Like HTML Editor & A4 Document Canvas */}
        <div className="lg:col-span-3 space-y-4">
          
          {/* WORD-LIKE RICH TEXT FORMATTING TOOLBAR */}
          {isEditing && (
            <div className="rich-text-toolbar bg-slate-950 text-white p-4 rounded-2xl border-4 border-amber-400 shadow-2xl space-y-3 no-print font-sans">
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-700 pb-2">
                <div className="flex items-center gap-2">
                  <Type className="w-5 h-5 text-amber-400 shrink-0" />
                  <span className="font-black text-xs md:text-sm text-amber-300">
                    شريط أدوات تنسيق القالب (Word Rich Text Toolbar):
                  </span>
                </div>

                {/* Editor Mode Switcher: WYSIWYG Visual vs Raw HTML */}
                <div className="flex items-center gap-1 bg-slate-900 p-1 rounded-xl border border-amber-400/40 text-[11px] font-bold">
                  <button
                    type="button"
                    onClick={() => setEditorMode('wysiwyg')}
                    className={`px-3 py-1.5 rounded-lg transition-all cursor-pointer ${
                      editorMode === 'wysiwyg' ? 'bg-amber-400 text-slate-950 font-black shadow-md' : 'text-slate-200 hover:text-white'
                    }`}
                  >
                    محرر بصري (Word)
                  </button>
                  <button
                    type="button"
                    onClick={() => setEditorMode('raw_html')}
                    className={`px-3 py-1.5 rounded-lg transition-all cursor-pointer ${
                      editorMode === 'raw_html' ? 'bg-amber-400 text-slate-950 font-black shadow-md' : 'text-slate-200 hover:text-white'
                    }`}
                  >
                    كود HTML
                  </button>
                </div>
              </div>

              {/* Controls Toolbar Options */}
              {editorMode === 'wysiwyg' && (
                <div className="flex flex-wrap items-center gap-2 text-xs">
                  
                  {/* Undo & Redo Curved Arrows */}
                  <div className="flex items-center gap-1.5 bg-slate-900 p-1.5 rounded-xl border border-slate-700">
                    <button
                      type="button"
                      onClick={() => executeFormat('undo')}
                      className="px-2.5 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-400 hover:text-slate-950 text-amber-300 cursor-pointer transition-all active:scale-95 flex items-center gap-1 font-bold border border-amber-400/40"
                      title="تراجع (Undo ↩)"
                    >
                      <Undo2 className="w-4 h-4 text-amber-300" />
                      <span className="text-xs font-black">تراجع</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => executeFormat('redo')}
                      className="px-2.5 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-400 hover:text-slate-950 text-amber-300 cursor-pointer transition-all active:scale-95 flex items-center gap-1 font-bold border border-amber-400/40"
                      title="تقدم / إعادة (Redo ↪)"
                    >
                      <Redo2 className="w-4 h-4 text-amber-300" />
                      <span className="text-xs font-black">تقدم</span>
                    </button>
                  </div>
                  
                  {/* Text Styling (Bold, Italic, Underline, Strike) */}
                  <div className="flex items-center gap-1.5 bg-slate-900 p-1.5 rounded-xl border border-slate-700">
                    <button
                      type="button"
                      onClick={() => executeFormat('bold')}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-amber-400 hover:text-slate-950 text-white font-black cursor-pointer transition-all border border-slate-600 flex items-center gap-1"
                      title="عريض (Bold B)"
                    >
                      <Bold className="w-4 h-4 text-amber-400" />
                      <span className="font-black text-xs">عريض B</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => executeFormat('italic')}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-amber-400 hover:text-slate-950 text-white font-black cursor-pointer transition-all border border-slate-600 flex items-center gap-1"
                      title="مائل (Italic I)"
                    >
                      <Italic className="w-4 h-4 text-amber-400" />
                      <span className="font-black text-xs italic">مائل I</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => executeFormat('underline')}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-amber-400 hover:text-slate-950 text-white font-black cursor-pointer transition-all border border-slate-600 flex items-center gap-1"
                      title="تحته خط (Underline U)"
                    >
                      <UnderlineIcon className="w-4 h-4 text-amber-400" />
                      <span className="font-black text-xs underline">خط U</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => executeFormat('strikeThrough')}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-amber-400 hover:text-slate-950 text-white font-black cursor-pointer line-through border border-slate-600 text-xs"
                      title="شطب النص"
                    >
                      شطب S
                    </button>
                  </div>

                  {/* Alignment Controls */}
                  <div className="flex items-center gap-1.5 bg-slate-900 p-1.5 rounded-xl border border-slate-700">
                    <button
                      type="button"
                      onClick={() => executeFormat('justifyRight')}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-amber-400 hover:text-slate-950 text-white font-black cursor-pointer transition-all border border-slate-600 flex items-center gap-1"
                      title="محاذاة لليمين"
                    >
                      <AlignRight className="w-4 h-4 text-amber-400" />
                      <span className="text-xs font-bold">يمين</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => executeFormat('justifyCenter')}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-amber-400 hover:text-slate-950 text-white font-black cursor-pointer transition-all border border-slate-600 flex items-center gap-1"
                      title="محاذاة للمنتصف (توسيط)"
                    >
                      <AlignCenter className="w-4 h-4 text-amber-400" />
                      <span className="text-xs font-bold">توسيط</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => executeFormat('justifyLeft')}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-amber-400 hover:text-slate-950 text-white font-black cursor-pointer transition-all border border-slate-600 flex items-center gap-1"
                      title="محاذاة لليسار"
                    >
                      <AlignLeft className="w-4 h-4 text-amber-400" />
                      <span className="text-xs font-bold">يسار</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => executeFormat('justifyFull')}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-amber-400 hover:text-slate-950 text-white font-black cursor-pointer transition-all border border-slate-600 flex items-center gap-1"
                      title="ضبط النص (Justify)"
                    >
                      <AlignJustify className="w-4 h-4 text-amber-400" />
                      <span className="text-xs font-bold">ضبط</span>
                    </button>
                  </div>

                  {/* Font Size Selector */}
                  <div className="flex items-center gap-1.5 bg-slate-900 p-1.5 rounded-xl border border-slate-700">
                    <span className="text-xs font-black text-amber-400">الحجم:</span>
                    <select
                      onChange={(e) => executeFormat('fontSize', e.target.value)}
                      className="bg-slate-900 text-amber-300 text-xs p-1.5 rounded-lg font-black border border-slate-700 focus:outline-none cursor-pointer"
                    >
                      <option value="3" className="bg-slate-900 text-white">عادي (14px)</option>
                      <option value="4" className="bg-slate-900 text-white">متوسط (18px)</option>
                      <option value="5" className="bg-slate-900 text-white">كبير (24px)</option>
                      <option value="6" className="bg-slate-900 text-white">كبير جداً (32px)</option>
                    </select>
                  </div>

                  {/* Text Color Selection */}
                  <div className="flex items-center gap-1.5 bg-slate-900 p-1.5 rounded-xl border border-slate-700">
                    <Palette className="w-4 h-4 text-amber-400 shrink-0" />
                    {[
                      { color: '#000000', label: 'أسود' },
                      { color: '#78350f', label: 'ذهبي' },
                      { color: '#1e3a8a', label: 'كحلي' },
                      { color: '#991b1b', label: 'أحمر' },
                    ].map((c) => (
                      <button
                        key={c.color}
                        type="button"
                        onClick={() => executeFormat('foreColor', c.color)}
                        className="w-5 h-5 rounded-full border-2 border-amber-400 cursor-pointer hover:scale-110 transition-transform"
                        style={{ backgroundColor: c.color }}
                        title={c.label}
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* EXACT A4 DOCUMENT CANVAS WITH RENDERED HTML & PLACEHOLDERS */}
          <div id="printing-center-document-canvas" className="bg-white text-slate-900 p-8 md:p-10 rounded-2xl shadow-2xl border-4 border-amber-900/30 relative overflow-hidden min-h-[29.7cm] w-full max-w-[21cm] mx-auto flex flex-col justify-between print-page print-page-a4 font-amiri">
            
            {/* Inner Decorative Double Border */}
            <div className="absolute inset-2.5 border-2 border-double border-amber-900/40 pointer-events-none rounded-xl"></div>
            
            {/* Corner Bracket Accents */}
            <div className="absolute top-4 right-4 w-6 h-6 border-t-2 border-r-2 border-amber-900/60 pointer-events-none"></div>
            <div className="absolute top-4 left-4 w-6 h-6 border-t-2 border-l-2 border-amber-900/60 pointer-events-none"></div>
            <div className="absolute bottom-4 right-4 w-6 h-6 border-b-2 border-r-2 border-amber-900/60 pointer-events-none"></div>
            <div className="absolute bottom-4 left-4 w-6 h-6 border-b-2 border-l-2 border-amber-900/60 pointer-events-none"></div>

            {/* Central Ministry Watermark */}
            <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none opacity-15 select-none z-0">
              <img 
                src={OFFICIAL_SEAL_DATA_URI} 
                alt="العلامة المائية لوزارة التربية" 
                className="w-96 h-96 object-contain drop-shadow-2xl" 
              />
            </div>

            {/* Top Official Header Grid - 3 Equal Column Grid for Perfect Centering */}
            <div className="relative z-10 grid grid-cols-3 items-center border-b-2 border-slate-900 pb-3 font-amiri">
              
              {/* Right Side: Directorate, Section & School Name Header */}
              <div className="text-right text-sm md:text-base font-bold leading-relaxed space-y-0.5 text-slate-950 font-amiri">
                <p className="font-bold text-sm md:text-base">{(config?.directorateName || 'مديرية تربية ديالى')}</p>
                <p className="font-bold text-sm md:text-base">{(config?.sectionName || 'قسم تربية المقدادية')}</p>
                <p className="font-extrabold text-base md:text-lg text-slate-950 pt-0.5">
                  {((config?.schoolName || 'م. كعب بن مالك المسائية للبنين')).replace(/متوسطة/g, 'م.')}
                </p>
              </div>

              {/* Center: Ministry Official Seal Crest - Perfectly Centered in Column 2 */}
              <div className="flex flex-col items-center justify-center text-center">
                <img 
                  src={OFFICIAL_SEAL_DATA_URI} 
                  alt="شعار وزارة التربية" 
                  className="w-20 h-20 md:w-22 md:h-22 object-contain drop-shadow-md" 
                />
              </div>

              {/* Left Side: Ref Number & Date */}
              <div className="text-left text-xs md:text-sm font-bold leading-relaxed space-y-0.5 text-slate-950 font-amiri">
                <p 
                  contentEditable={isEditing}
                  suppressContentEditableWarning
                  onBlur={(e) => {
                    const text = e.currentTarget.innerText.replace(/^العدد\s*:\s*/, '');
                    setSelectedDoc(prev => ({ ...prev, refNumber: text }));
                  }}
                  className={`p-0.5 rounded inline-block ${isEditing ? 'hover:bg-amber-100 focus:bg-amber-100 focus:outline-amber-500' : ''}`}
                >
                  العدد: {selectedDoc?.refNumber || '.......'}
                </p>
                <p>التاريخ: {selectedDoc?.date ? `${selectedDoc?.date.split('-')[2] || ''} / ${selectedDoc?.date.split('-')[1] || ''} / ${selectedDoc?.date.split('-')[0] || ''}م` : '   /   / 2026م'}</p>
              </div>

            </div>

            {/* Main Content Body - Visual Editable OR Rendered HTML */}
            <div className="relative z-10 my-6 space-y-5 px-4 flex-1">
              
              {/* Recipient Line - Inline Editable */}
              <h3 
                contentEditable={isEditing}
                suppressContentEditableWarning
                onBlur={(e) => setSelectedDoc(prev => ({ ...prev, recipient: e.currentTarget.innerText }))}
                className={`text-base md:text-lg font-black font-amiri text-slate-950 p-1 rounded transition-all ${
                  isEditing ? 'hover:bg-amber-100/60 focus:bg-amber-100 focus:outline-amber-500' : ''
                }`}
              >
                {replacePlaceholders(selectedDoc?.recipient)}
              </h3>

              {/* Subject Title (م/ العنوان) - Inline Editable */}
              <div className="text-center py-2">
                <h4 
                  contentEditable={isEditing}
                  suppressContentEditableWarning
                  onBlur={(e) => {
                    const text = e.currentTarget.innerText.replace(/^م\s*\/\s*/, '');
                    setSelectedDoc(prev => ({ ...prev, subject: text }));
                  }}
                  className={`inline-block text-base md:text-lg font-black font-amiri border-b-2 border-slate-950 pb-1 px-8 text-slate-950 p-1 rounded transition-all ${
                    isEditing ? 'hover:bg-amber-100/60 focus:bg-amber-100 focus:outline-amber-500' : ''
                  }`}
                >
                  م / {replacePlaceholders(selectedDoc?.subject)}
                </h4>
              </div>

              {/* Content Body Section: Full Live Direct Editable Document Body */}
              <div className="space-y-1">
                {isEditing && (
                  <span className="text-[11px] text-amber-900 font-extrabold bg-amber-100 px-2 py-0.5 rounded border border-amber-300 block w-fit no-print">
                    ✏️ اضغط وانقر هنا مباشرة على أي كلمة داخل الورقة للتعديل أو التنسيق الفوري (Direct Live Editing)
                  </span>
                )}
                <div
                  ref={editorRef}
                  contentEditable={isEditing}
                  suppressContentEditableWarning
                  onBlur={(e) => {
                    const html = e.currentTarget.innerHTML;
                    setSelectedDoc(prev => ({ ...prev, bodyContent: html }));
                  }}
                  className={`text-sm md:text-base leading-relaxed font-bold font-amiri text-slate-950 text-justify indent-6 prose max-w-none min-h-[240px] p-4 rounded-xl transition-all outline-none ${
                    isEditing 
                      ? 'border-2 border-dashed border-amber-400 bg-amber-50/50 hover:bg-amber-50/80 focus:ring-2 focus:ring-amber-500' 
                      : ''
                  }`}
                  dangerouslySetInnerHTML={{ __html: replacePlaceholders(selectedDoc?.bodyContent) }}
                />
              </div>

              {/* Optional Table for Group Notices */}
              {(selectedDoc.templateType === 'absent_table' || selectedDoc.templateType === 'failure_order') && !isEditing ? (
                <div className="mt-4 border-2 border-slate-900 rounded-xl overflow-hidden text-xs">
                  <table className="w-full text-center border-collapse">
                    <thead>
                      <tr className="bg-slate-950 text-white font-black">
                        <th className="p-2 border-r border-slate-700">ت</th>
                        <th className="p-2 border-r border-slate-700 text-right">اسم الطالب الرباعي</th>
                        <th className="p-2 border-r border-slate-700">الصف والشعبة</th>
                        <th className="p-2">عدد أيام الغياب</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-300 font-bold">
                      <tr><td className="p-2 border-r">1</td><td className="p-2 border-r text-right">{sampleStudent.name}</td><td className="p-2 border-r">{sampleStudent.grade}</td><td className="p-2">{sampleStudent.absences} يوماً</td></tr>
                      <tr><td className="p-2 border-r">2</td><td className="p-2 border-r text-right">مطفأ / طالب نموذج 2</td><td className="p-2 border-r">الأول متوسط (ب)</td><td className="p-2">15 يوماً</td></tr>
                      <tr><td className="p-2 border-r">3</td><td className="p-2 border-r text-right">مطفأ / طالب نموذج 3</td><td className="p-2 border-r">الثاني متوسط (ج)</td><td className="p-2">18 يوماً</td></tr>
                    </tbody>
                  </table>
                </div>
              ) : null}

            </div>

            {/* Bottom Margins: Signature & Copies To ABOVE the Two Bottom Lines */}
            <div className="relative z-10 pt-4 space-y-4">
              
              {/* Manager Name (Right) & Copies To (Left) */}
              <div className="flex justify-between items-start text-xs font-black px-2">
                {/* Left: Copies To & Attachments */}
                <div className="text-right space-y-0.5 text-slate-900">
                  <p className="font-black text-slate-950 underline">نسخة إلى:</p>
                  <p>- ملف الطالب / المعلم الإداري</p>
                  <p>- لوحة الإعلانات الرسمية</p>
                  <p>- أرشيف الإدارة الموحد</p>
                  <p className="pt-1.5 font-black text-amber-950 underline">المرفقات: لا يوجد</p>
                </div>

                {/* Right: Manager Name & Title with EMPTY SPACE BELOW for Signature */}
                <div className="text-center space-y-1">
                  <p className="font-black text-sm">{selectedDoc?.managerTitle}</p>
                  <p className="font-black text-base text-amber-950">{(config?.managerName || 'مدير المدرسة')}</p>
                  <div className="w-44 h-14 my-1"></div>
                </div>
              </div>

              {/* TWO LINES AT THE VERY BOTTOM OF THE PAGE */}
              <div className="space-y-1 pt-2 border-t-2 border-slate-900">
                <div className="text-center text-xs font-mono text-slate-600 overflow-hidden whitespace-nowrap select-none">
                  ...................................................................................................................................................................................
                </div>
                <div className="text-center text-xs font-mono text-slate-600 overflow-hidden whitespace-nowrap select-none">
                  ...................................................................................................................................................................................
                </div>
              </div>

            </div>

          </div>

        </div>

      </div>

      {/* Print Preview Modal */}
      <PrintPreviewModal
        isOpen={showPrintPreview}
        onClose={() => setShowPrintPreview(false)}
        title={`معاينة القالب الورقي الرسمية - ${selectedDoc?.title || 'وثيقة رسمية'}`}
        subtitle="تفقّد الهوامش والتاريخ والتنسيق الإداري قبل طباعة الكراسة أو المستند"
        config={config}
        documentRef={selectedDoc?.refNumber || '1042'}
        documentDate={selectedDoc?.date || new Date().toISOString().split('T')[0]}
      >
        <div className="space-y-6 font-amiri dir-rtl px-4 py-2">
          
          <div className="text-right">
            <h3 className="text-lg font-black text-slate-950">
              {replacePlaceholders(selectedDoc?.recipient || '')}
            </h3>
          </div>

          <div className="text-center py-2">
            <h4 className="inline-block text-lg font-black border-b-2 border-slate-900 pb-1 px-6 text-slate-950">
              م / {replacePlaceholders(selectedDoc?.subject || '')}
            </h4>
          </div>

          <div 
            className="text-base text-slate-900 leading-relaxed font-amiri min-h-[200px]"
            dangerouslySetInnerHTML={{ __html: replacePlaceholders(selectedDoc?.bodyContent || '') }}
          />

          <div className="pt-8 flex justify-between items-end">
            <div className="text-xs text-slate-600 font-tajawal">
              {selectedDoc?.copiesTo && selectedDoc.copiesTo.length > 0 && (
                <div>
                  <p className="font-bold text-slate-800 mb-1">نسخة منه إلى:</p>
                  <ul className="list-disc list-inside space-y-0.5">
                    {selectedDoc.copiesTo.map((c, i) => (
                      <li key={i}>{c}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            <div className="text-center">
              <p className="text-sm font-bold text-slate-800">{selectedDoc?.managerTitle || 'مدير المدرسة'}</p>
              <p className="text-base font-black text-slate-950">{selectedDoc?.managerName || config.managerName}</p>
            </div>
          </div>

        </div>
      </PrintPreviewModal>

    </div>
  );
};



export const PrintingCenterView: React.FC<PrintingCenterViewProps> = (props) => {
  return (
    <PrintingCenterErrorBoundary>
      <PrintingCenterViewInner {...props} />
    </PrintingCenterErrorBoundary>
  );
};
