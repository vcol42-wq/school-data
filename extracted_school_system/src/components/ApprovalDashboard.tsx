import React, { useEffect, useState } from 'react';
import { supabase } from '../utils/supabaseClient';
import { 
  UserCheck, 
  UserX, 
  Users, 
  BookOpen, 
  Clock, 
  RefreshCw, 
  GraduationCap, 
  ShieldAlert, 
  CheckCircle2, 
  XCircle,
  Inbox,
  Zap,
  Smartphone,
  Check
} from 'lucide-react';

interface JoinRequest {
  id: string;
  role: 'teacher' | 'student';
  full_name: string;
  device_id: string;
  class_name?: string;
  section_name?: string;
  subject_specialty?: string;
  status: 'pending' | 'approved' | 'rejected';
  created_at: string;
}

export const ApprovalDashboard: React.FC<{ schoolId: string }> = ({ schoolId }) => {
  const [requests, setRequests] = useState<JoinRequest[]>([]);
  const [activeTab, setActiveTab] = useState<'connected_students' | 'pending_students' | 'teacher'>('connected_students');
  const [loading, setLoading] = useState<boolean>(false);
  const [actioningId, setActioningId] = useState<string | null>(null);
  const [autoApproveStudents, setAutoApproveStudents] = useState<boolean>(() => {
    return localStorage.getItem('auto_approve_students') !== 'false';
  });

  // Fetch all requests
  const fetchRequests = async () => {
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('join_requests')
        .select('*')
        .or(`school_id.eq.${schoolId},school_id.eq.school_01`)
        .order('created_at', { ascending: false });

      if (!error && data) {
        setRequests(data as JoinRequest[]);
      } else if (error) {
        console.error('Fetch requests error:', error);
      }
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchRequests();

    // Subscribe to real-time changes for join_requests
    const channel = supabase
      .channel(`approval-sync-${schoolId}`)
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'join_requests',
          filter: `school_id=eq.${schoolId}`
        },
        async (payload) => {
          if (payload.eventType === 'INSERT') {
            const newReq = payload.new as JoinRequest;
            // If auto-approve is enabled and it's a student
            if (autoApproveStudents && newReq.role === 'student' && newReq.status === 'pending') {
              try {
                await supabase
                  .from('join_requests')
                  .update({ status: 'approved' })
                  .eq('id', newReq.id);
                newReq.status = 'approved';
              } catch (e) {
                console.error(e);
              }
            }
            setRequests(prev => [newReq, ...prev]);
          } else if (payload.eventType === 'UPDATE' || payload.eventType === 'DELETE') {
            fetchRequests();
          }
        }
      )
      .subscribe();

    return () => {
      supabase.removeChannel(channel);
    };
  }, [schoolId, autoApproveStudents]);

  // Handle approval/rejection decision
  const handleDecision = async (id: string, decision: 'approved' | 'rejected') => {
    setActioningId(id);
    try {
      const { error } = await supabase
        .from('join_requests')
        .update({ status: decision })
        .eq('id', id);

      if (!error) {
        setRequests((prev) => prev.map((req) => req.id === id ? { ...req, status: decision } : req));
      } else {
        alert(`فشل التحديث: ${error.message}`);
      }
    } catch (e: any) {
      alert(`خطأ في الشبكة: ${e.message}`);
    }
    setActioningId(null);
  };

  // Batch approve all pending students
  const handleApproveAllStudents = async () => {
    setLoading(true);
    try {
      const { error } = await supabase
        .from('join_requests')
        .update({ status: 'approved' })
        .eq('school_id', schoolId)
        .eq('role', 'student')
        .eq('status', 'pending');

      if (!error) {
        setRequests(prev => prev.map(r => r.role === 'student' ? { ...r, status: 'approved' } : r));
        alert('تم اعتماد كافة طلبات انضمام الطلاب المعلقة بنجاح!');
      }
    } catch (e: any) {
      alert(`خطأ: ${e.message}`);
    }
    setLoading(false);
  };

  const connectedStudents = requests.filter(r => r.role === 'student' && r.status === 'approved');
  const pendingStudents = requests.filter(r => r.role === 'student' && r.status === 'pending');
  const teacherRequests = requests.filter(r => r.role === 'teacher');

  return (
    <div className="max-w-7xl mx-auto p-8 dir-rtl text-right">
      
      {/* Top Header Card */}
      <div className="bg-white p-8 text-slate-900 rounded-[2.5rem] border-b-4 border-indigo-600 shadow-2xl flex flex-col md:flex-row items-center justify-between gap-6 mb-8 border border-slate-200">
        <div className="flex items-center gap-4">
          <div className="p-3.5 bg-emerald-50 rounded-2xl border border-emerald-200">
            <GraduationCap className="w-10 h-10 text-emerald-600" />
          </div>
          <div>
            <h2 className="text-2xl font-black text-slate-900">مركز الطلاب المتصلين واعتماد الأجهزة</h2>
            <p className="text-sm text-slate-500">مراقبة هواتف الطلاب المتصلة بتطبيق (The Boss) والاعتماد التلقائي الفوري</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              const next = !autoApproveStudents;
              setAutoApproveStudents(next);
              localStorage.setItem('auto_approve_students', next.toString());
            }}
            className={`px-5 py-3 rounded-2xl text-xs font-black transition-all flex items-center gap-2 cursor-pointer border ${
              autoApproveStudents
                ? 'bg-emerald-500 text-white border-emerald-600 shadow-lg shadow-emerald-200'
                : 'bg-slate-100 text-slate-700 border-slate-300'
            }`}
          >
            <Zap className={`w-4 h-4 ${autoApproveStudents ? 'animate-pulse' : ''}`} />
            <span>الاعتماد التلقائي الفوري: {autoApproveStudents ? 'مفعّل ⚡' : 'يدوي'}</span>
          </button>

          <button
            onClick={fetchRequests}
            disabled={loading}
            className="px-5 py-3 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white rounded-2xl text-xs font-black shadow-xl shadow-indigo-200 transition-all active:scale-95 flex items-center gap-2 cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            <span>تحديث</span>
          </button>
        </div>
      </div>

      {/* Tabs list */}
      <div className="flex gap-4 border-b border-slate-200 mb-8">
        <button
          onClick={() => setActiveTab('connected_students')}
          className={`pb-4 px-4 font-black text-sm transition-all flex items-center gap-2 cursor-pointer ${
            activeTab === 'connected_students'
              ? 'border-b-4 border-emerald-600 text-emerald-600'
              : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          <Smartphone className="w-4 h-4" />
          <span>الطلاب المتصلون والمعتمدون</span>
          <span className="bg-emerald-100 text-emerald-900 text-[10px] px-2.5 py-0.5 rounded-full font-black">
            {connectedStudents.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('pending_students')}
          className={`pb-4 px-4 font-black text-sm transition-all flex items-center gap-2 cursor-pointer ${
            activeTab === 'pending_students'
              ? 'border-b-4 border-amber-500 text-amber-600'
              : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          <Clock className="w-4 h-4" />
          <span>الطلبات المعلقة للطلاب</span>
          <span className="bg-amber-100 text-amber-900 text-[10px] px-2.5 py-0.5 rounded-full font-black">
            {pendingStudents.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('teacher')}
          className={`pb-4 px-4 font-black text-sm transition-all flex items-center gap-2 cursor-pointer ${
            activeTab === 'teacher'
              ? 'border-b-4 border-indigo-600 text-indigo-600'
              : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          <BookOpen className="w-4 h-4" />
          <span>طلبات الأساتذة</span>
          <span className="bg-slate-100 text-slate-800 text-[10px] px-2.5 py-0.5 rounded-full font-black">
            {teacherRequests.length}
          </span>
        </button>
      </div>

      {/* Batch Approval Alert (Only for pending students) */}
      {activeTab === 'pending_students' && pendingStudents.length > 0 && (
        <div className="bg-amber-50 border border-amber-200 p-6 rounded-3xl mb-8 flex flex-col md:flex-row items-center justify-between gap-4 shadow-sm">
          <div className="flex items-center gap-3">
            <Users className="w-6 h-6 text-amber-600 shrink-0" />
            <div>
              <h4 className="font-bold text-slate-900 text-sm">يوجد {pendingStudents.length} طلب انضمام بانتظار الاعتماد</h4>
              <p className="text-xs text-slate-500 mt-0.5">يمكنك قبول جميع الطلاب المعلقين فوراً بنقرة واحدة</p>
            </div>
          </div>
          <button
            onClick={handleApproveAllStudents}
            className="px-6 py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-2xl text-xs font-black transition-all active:scale-95 shadow-lg shadow-emerald-500/20 flex items-center gap-2 cursor-pointer"
          >
            <Check className="w-4 h-4" />
            <span>اعتماد كافة الطلاب المعلقين الآن</span>
          </button>
        </div>
      )}

      {loading ? (
        <div className="flex flex-col items-center justify-center py-24 bg-white rounded-[2rem] border border-slate-200 shadow-md">
          <RefreshCw className="w-10 h-10 text-indigo-600 animate-spin mb-4" />
          <p className="text-slate-500 font-bold text-sm">جاري تحميل قائمة الطلاب المتصلين...</p>
        </div>
      ) : activeTab === 'connected_students' ? (
        connectedStudents.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-24 bg-white rounded-[2rem] border border-slate-200 shadow-md text-center">
            <Smartphone className="w-16 h-16 text-slate-300 mb-4 animate-pulse" />
            <p className="text-slate-800 font-black text-lg">لا يوجد أجهزة طلاب متصلة حالياً</p>
            <p className="text-slate-400 text-xs mt-1">عند قيام الطلاب بالربط عبر تطبيق الطالب، ستظهر أجهزتهم هنا فورياً</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {connectedStudents.map((req) => (
              <div 
                key={req.id} 
                className="bg-white rounded-[2rem] border border-slate-200 p-6 shadow-xl flex flex-col justify-between hover:shadow-2xl hover:border-emerald-300 transition-all group"
              >
                <div>
                  <div className="flex justify-between items-start mb-4">
                    <div>
                      <h3 className="font-black text-slate-900 text-lg group-hover:text-emerald-700 transition-colors">
                        {req.full_name}
                      </h3>
                      <span className="text-[10px] text-slate-400 block mt-1 font-mono">ID: {req.device_id.substring(0, 14)}...</span>
                    </div>
                    <span className="px-3 py-1 bg-emerald-50 text-emerald-700 text-[10px] font-black rounded-full border border-emerald-200 flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                      <span>متصل ومعتمد</span>
                    </span>
                  </div>

                  <div className="space-y-2 bg-slate-50 p-4 rounded-2xl border border-slate-100">
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400">الصف الدراسي:</span>
                      <span className="font-bold text-slate-800">{req.class_name || 'غير محدد'}</span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400">الشعبة:</span>
                      <span className="font-bold text-emerald-800">الشعبة {req.section_name || 'أ'}</span>
                    </div>
                    <div className="flex justify-between text-[11px] pt-2 border-t border-slate-200 text-slate-400">
                      <span>تاريخ الاتصال:</span>
                      <span>{new Date(req.created_at).toLocaleDateString('ar-IQ', { day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' })}</span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )
      ) : activeTab === 'pending_students' ? (
        pendingStudents.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-24 bg-white rounded-[2rem] border border-slate-200 shadow-md text-center">
            <CheckCircle2 className="w-16 h-16 text-emerald-400 mb-4" />
            <p className="text-slate-800 font-black text-lg">جميع الطلاب معتمدون ومطابقون</p>
            <p className="text-slate-400 text-xs mt-1">لا توجد طلبات انضمام معلقة حالياً</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {pendingStudents.map((req) => (
              <div 
                key={req.id} 
                className="bg-white rounded-[2rem] border border-slate-200 p-6 shadow-xl flex flex-col justify-between hover:shadow-2xl hover:border-amber-300 transition-all group"
              >
                <div>
                  <div className="flex justify-between items-start mb-4">
                    <div>
                      <h3 className="font-black text-slate-900 text-lg group-hover:text-amber-700 transition-colors">
                        {req.full_name}
                      </h3>
                      <span className="text-[10px] text-slate-400 block mt-1 font-mono">Device: {req.device_id.substring(0, 12)}...</span>
                    </div>
                    <span className="px-3 py-1 bg-amber-50 text-amber-700 text-[10px] font-black rounded-full border border-amber-200 flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      <span>قيد الانتظار</span>
                    </span>
                  </div>

                  <div className="space-y-2 bg-slate-50 p-4 rounded-2xl border border-slate-100">
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400">الصف الدراسي:</span>
                      <span className="font-bold text-slate-800">{req.class_name}</span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400">الشعبة:</span>
                      <span className="font-bold text-amber-800">الشعبة {req.section_name}</span>
                    </div>
                    <div className="flex justify-between text-[11px] pt-2 border-t border-slate-200 text-slate-400">
                      <span>تاريخ التقديم:</span>
                      <span>{new Date(req.created_at).toLocaleDateString('ar-IQ', { day: 'numeric', month: 'long' })}</span>
                    </div>
                  </div>
                </div>

                <div className="flex gap-3 mt-6 pt-4 border-t border-slate-100">
                  <button
                    onClick={() => handleDecision(req.id, 'approved')}
                    disabled={actioningId === req.id}
                    className="flex-1 px-4 py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-2xl text-xs font-black shadow-lg shadow-emerald-500/10 active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    <span>اعتماد ومطابقة</span>
                  </button>
                  <button
                    onClick={() => handleDecision(req.id, 'rejected')}
                    disabled={actioningId === req.id}
                    className="px-4 py-3 bg-rose-50 hover:bg-rose-100 text-rose-600 rounded-2xl text-xs font-black border border-rose-200 active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <XCircle className="w-4 h-4" />
                    <span>رفض</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        teacherRequests.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-24 bg-white rounded-[2rem] border border-slate-200 shadow-md text-center">
            <Inbox className="w-16 h-16 text-slate-300 mb-4" />
            <p className="text-slate-800 font-black text-lg">لا توجد طلبات معلمين</p>
            <p className="text-slate-400 text-xs mt-1">يقوم المعلمون بالربط المباشر عبر رمز الاقتران أو الـ QR</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {teacherRequests.map((req) => (
              <div 
                key={req.id} 
                className="bg-white rounded-[2rem] border border-slate-200 p-6 shadow-xl flex flex-col justify-between hover:shadow-2xl hover:border-indigo-200 transition-all group"
              >
                <div>
                  <div className="flex justify-between items-start mb-4">
                    <div>
                      <h3 className="font-black text-slate-900 text-lg group-hover:text-indigo-600 transition-colors">
                        {req.full_name}
                      </h3>
                      <span className="text-[10px] text-slate-400 block mt-1 font-mono">Device: {req.device_id.substring(0, 12)}...</span>
                    </div>
                    <span className="px-3 py-1 bg-indigo-50 text-indigo-700 text-[10px] font-black rounded-full border border-indigo-200 flex items-center gap-1">
                      {req.status === 'approved' ? 'معتمد' : 'قيد الانتظار'}
                    </span>
                  </div>

                  <div className="space-y-2 bg-slate-50 p-4 rounded-2xl border border-slate-100">
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400">التخصص:</span>
                      <span className="font-bold text-slate-800">{req.subject_specialty || 'عام'}</span>
                    </div>
                  </div>
                </div>

                {req.status === 'pending' && (
                  <div className="flex gap-3 mt-6 pt-4 border-t border-slate-100">
                    <button
                      onClick={() => handleDecision(req.id, 'approved')}
                      disabled={actioningId === req.id}
                      className="flex-1 px-4 py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-2xl text-xs font-black shadow-lg shadow-emerald-500/10 active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                    >
                      <CheckCircle2 className="w-4 h-4" />
                      <span>اعتماد</span>
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )
      )}
    </div>
  );
};
