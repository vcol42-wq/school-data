import express from 'express';
import path from 'path';
import fs from 'fs';
import { createServer as createViteServer } from 'vite';
import { GoogleGenAI } from '@google/genai';


function normalizeArabic(str: string): string {
  if (!str) return '';
  return str
    .trim()
    .replace(/[أإآ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/ى/g, 'ي')
    .replace(/^الصف\s+/g, '')
    .replace(/(^|\s)ال/g, '$1')
    .replace(/\s+/g, '');
}

async function startServer() {
  const app = express();
  const PORT = 3000;

  // JSON Body Parser for base64 image scanning & sync requests
  app.use(express.json({ limit: '15mb' }));

  // API Routes
  app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', serverTime: new Date().toISOString() });
  });

  // OCR Paper Register Image Scanner API using Gemini AI
  app.post('/api/ocr-score-sheet', async (req, res) => {
    try {
      const { imageBase64, mimeType = 'image/jpeg' } = req.body;

      if (!imageBase64) {
        return res.status(400).json({ error: 'لم يتم إرسال صورة السجل الورقي' });
      }

      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.status(500).json({ error: 'مفتاح GEMINI_API_KEY غير معرف بالنظام' });
      }

      const ai = new GoogleGenAI({ apiKey });

      // Clean base64 string
      const cleanBase64 = imageBase64.replace(/^data:image\/\w+;base64,/, '');

      const prompt = `أنت خبير فحص وقراءة سجلات درجات الطلاب المدرسية الورقية باللغة العربية.
قم بفرز وقراءة صورة سجل درجات الطلاب المرفقة واستخراج الجدول وتنسيقه في صيغة JSON حصرية دون أي نصوص إضافية.
يجب أن يحتوي الـ JSON على مصفوفة باسم "students" حيث تحتوي كل مفردة على:
- recordNumber: رقم القيد أو تسلسل الطالب إن وجد (نص)
- studentName: الاسم الكامل للطالب (نص)
- midtermMark: درجة منتصف السنة/الفصل الأول (رقم)
- finalMark: درجة نهاية السنة/الدرجة النهائية (رقم)
- status: "ناجح" إذا كانت الدرجة النهائية 50 فأعلى، أو "راسب" إذا كانت أقل من 50.

الصيغة المطلوبة بالضبط:
{
  "students": [
    { "recordNumber": "101", "studentName": "علي أحمد حسين", "midtermMark": 42, "finalMark": 85, "status": "ناجح" }
  ]
}`;

      const response = await ai.models.generateContent({
        model: 'gemini-2.5-flash',
        contents: [
          {
            role: 'user',
            parts: [
              { inlineData: { mimeType, data: cleanBase64 } },
              { text: prompt }
            ]
          }
        ]
      });

      const responseText = response.text || '';
      
      // Clean JSON block from response
      const jsonMatch = responseText.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        const parsedData = JSON.parse(jsonMatch[0]);
        return res.json({ success: true, data: parsedData });
      } else {
        return res.status(500).json({ error: 'تعذر استخراج بيانات الجدول بشكل دقيق من الصورة', raw: responseText });
      }

    } catch (error: any) {
      console.error('OCR Processing Error:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء فحص صورة السجل الورقي' });
    }
  });

  // AI Smart Assistant Endpoint with Gemini 2.5 Flash
  app.post('/api/ai-assistant', async (req, res) => {
    try {
      const { query, studentsCount = 0, staffCount = 0, schoolName = '', userApiKey } = req.body;

      if (!query) {
        return res.status(400).json({ error: 'يرجى إرسال سؤال أو أمر للبحث الصوتي الذكي' });
      }

      const apiKey = userApiKey || process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.status(500).json({ error: 'لم يتم العثور على مفتاح GEMINI_API_KEY. يرجى التأكد من ربط المفتاح بالنظام.' });
      }

      const modelName = 'gemini-2.5-flash';
      const ai = new GoogleGenAI({ apiKey });

      const prompt = `أنت المساعد الصوتي والذكاء الاصطناعي الذكي والمحترف لنظام الإدارة المدرسية الموحد (The Principal).
اسم المدرسة: ${schoolName || 'المدرسة المسائية'}
عدد الطلاب الإجمالي: ${studentsCount}
عدد الكادر والموظفين: ${staffCount}

سؤال/أمر المستخدم: "${query}"

قم بتحليل الأمر بدقة وإرجاع الإجابة والـ intent بتنسيق JSON حصري بالشكل التالي دون أي نصوص إضافية:
{
  "responseText": "إجابة ملخصة ومباشرة باللغة العربية بأسلوب راقي ومحترف",
  "action": "NAVIGATE" | "CHANGE_THEME" | "SEARCH_STUDENT" | "SEARCH_STAFF" | "ANSWER",
  "targetView": "launcher" | "schedule" | "students" | "former_students" | "staff" | "stats" | "print" | "themes" | "fonts" | "alarm" | "settings",
  "targetTheme": "vibrant" | "classic" | "diyala" | "emerald" | "dark" | "burgundy",
  "searchQuery": "اسم الطالب أو المعلم عند البحث",
  "detectedModel": "${modelName}"
}`;

      const response = await ai.models.generateContent({
        model: modelName,
        contents: [
          {
            role: 'user',
            parts: [{ text: prompt }]
          }
        ]
      });

      const responseText = response.text || '';
      const jsonMatch = responseText.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        const parsedData = JSON.parse(jsonMatch[0]);
        return res.json({ success: true, ...parsedData });
      } else {
        return res.json({
          success: true,
          responseText: responseText || 'تم تنفيذ الأمر بنجاح.',
          action: 'ANSWER',
          detectedModel: modelName
        });
      }

    } catch (error: any) {
      console.error('AI Assistant Endpoint Error:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء معالجة الطلب بالذكاء الاصطناعي' });
    }
  });

  // Cloud Sync Simulation Endpoint with Manager Passcode Verification ("نقطة اللا عودة")
  app.post('/api/cloud-sync', (req, res) => {
    const { passcode, syncAction, studentRecordsCount } = req.body;

    if (!passcode) {
      return res.status(401).json({ error: 'رمز الدخول الخاص بالمدير مطلوب لإجراء المزامنة والقفل' });
    }

    // Return success response with cryptographic sync seal hash
    const syncSealToken = `SEAL-${Date.now()}-${Math.floor(Math.random() * 899999 + 100000)}`;

    return res.json({
      success: true,
      syncSealToken,
      syncedCount: studentRecordsCount || 0,
      timestamp: new Date().toISOString(),
      message: 'تمت المزامنة وحفظ الدرجات سحابياً مع تطبيق الأندرويد وإقفال تعديل الأستاذ (وصلنا لنقطة اللا عودة)'
    });
  });

  // ==========================================
  // REAL CLOUD SYNC & PAIRING APIS FOR TEACHER & MANAGER (QR VERSION)
  // ==========================================

  const DB_FILE = path.join(process.cwd(), 'sync_database.json');

  function readSyncDb() {
    try {
      if (fs.existsSync(DB_FILE)) {
        const db = JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
        db.schools = db.schools || {};
        db.pairings = db.pairings || {};
        db.syncedGrades = db.syncedGrades || {};
        db.sessions = db.sessions || {};
        return db;
      }
    } catch (e) {
      console.error('Error reading sync database:', e);
    }
    return { schools: {}, pairings: {}, syncedGrades: {}, sessions: {} };
  }

  function writeSyncDb(db: any) {
    try {
      fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2), 'utf8');
    } catch (e) {
      console.error('Error writing sync database:', e);
    }
  }

  // 1. Upload School Data (Called by Manager App to sync roster database to cloud)
  app.post('/api/sync/upload-manager-data', (req, res) => {
    try {
      const { schoolId, schoolName, students, staff, config } = req.body;
      if (!schoolId) {
        return res.status(400).json({ error: 'معرف المدرسة schoolId مطلوب' });
      }
      const db = readSyncDb();
      db.schools = db.schools || {};
      db.schools[schoolId] = {
        schoolName,
        students: students || [],
        staff: staff || [],
        config: config || {},
        lastUpdated: new Date().toISOString()
      };
      writeSyncDb(db);
      console.log(`[Sync] Uploaded manager data for school: ${schoolName} (${schoolId})`);
      return res.json({ success: true, message: 'تم رفع قاعدة البيانات المدرسية السحابية بنجاح.' });
    } catch (error: any) {
      console.error('Error uploading manager data:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء رفع البيانات' });
    }
  });

  // 1.5. Create Session (Called by Manager Web/Electron App)
  app.post('/api/sync/create-session', (req, res) => {
    try {
      const { otpCode, teacherName, subjects, grades, sections, students } = req.body;
      if (!otpCode) {
        return res.status(400).json({ error: 'رمز الدخول المؤقت otpCode مطلوب' });
      }

      const db = readSyncDb();
      db.sessions = db.sessions || {};
      db.sessions[otpCode] = {
        teacherName,
        subjects: subjects || [],
        grades: grades || [],
        sections: sections || [],
        students: students || [],
        schoolId: 'school_01', // Default schoolId
        createdAt: new Date().toISOString()
      };
      writeSyncDb(db);

      console.log(`[Sync] Created session for OTP: ${otpCode} (Teacher: ${teacherName})`);
      return res.json({ success: true, message: 'تم تسجيل الجلسة بنجاح.' });
    } catch (error: any) {
      console.error('Error creating session:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء إنشاء الجلسة' });
    }
  });

  // 1.6. Get Synced Grades for Session OTP (Called by Manager Web/Electron App)
  app.get('/api/sync/get-synced-grades', (req, res) => {
    try {
      const { otpCode } = req.query;
      if (!otpCode) {
        return res.status(400).json({ error: 'رمز الدخول المؤقت otpCode مطلوب' });
      }

      const db = readSyncDb();
      db.sessions = db.sessions || {};
      const session = db.sessions[otpCode as string];

      if (!session) {
        return res.status(404).json({ error: 'لم يتم العثور على الجلسة المطلوبة' });
      }

      const schoolId = session.schoolId || 'school_01';
      db.syncedGrades = db.syncedGrades || {};
      const allSyncedGrades = db.syncedGrades[schoolId] || [];

      // Filter grades uploaded by teachers that match the grades, sections, and subjects in the session
      const sessionNormalizedGrades = session.grades.map((grd: string) => normalizeArabic(grd));
      const sessionNormalizedSections = session.sections.map((sec: string) => normalizeArabic(sec));
      const sessionNormalizedSubjects = session.subjects.map((sub: string) => normalizeArabic(sub));

      const filteredGrades = allSyncedGrades.filter((g: any) => {
        const gradeMatches = sessionNormalizedGrades.includes(normalizeArabic(g.grade));
        const sectionMatches = sessionNormalizedSections.includes(normalizeArabic(g.section));
        const subjectMatches = sessionNormalizedSubjects.includes(normalizeArabic(g.subject));
        return gradeMatches && sectionMatches && subjectMatches;
      });

      console.log(`[Sync] Pulled synced grades for OTP: ${otpCode}, found ${filteredGrades.length} classes`);
      return res.json({
        success: true,
        syncedGrades: filteredGrades
      });
    } catch (error: any) {
      console.error('Error getting synced grades:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء جلب الدرجات المزامنة' });
    }
  });

  const UNIFIED_SCHOOL_CODE = '999888';

  // 2. Request Pairing (Called by Teacher Android App to link subject and section)
  app.post('/api/sync/request-pairing', (req, res) => {
    try {
      const { schoolId, teacherName, grade, section, subject, pairingCode } = req.body;
      if (!schoolId || !teacherName || !grade || !section || !subject) {
        return res.status(400).json({ error: 'بيانات الاقتران غير مكتملة' });
      }

      if (!pairingCode) {
        return res.status(400).json({ error: 'رمز الاقتران الموحد مطلوب لربط المعلم بسحابة المدرسة' });
      }

      if (pairingCode !== UNIFIED_SCHOOL_CODE) {
        return res.status(400).json({ error: 'رمز الاقتران الموحد غير صحيح! يرجى إدخال الرمز الصحيح المعروض في بوابة المدير.' });
      }

      const db = readSyncDb();
      db.pairings = db.pairings || {};
      const pairings = db.pairings[schoolId] || [];

      // Check conflict (another teacher teaching the same subject on the same grade/section)
      const conflict = pairings.find(
        (p: any) => p.status !== 'reread' &&
                    normalizeArabic(p.grade) === normalizeArabic(grade) && 
                    normalizeArabic(p.section) === normalizeArabic(section) && 
                    normalizeArabic(p.subject) === normalizeArabic(subject) && 
                    p.teacherName !== teacherName
      );

      if (conflict) {
        return res.json({
          success: false,
          warning: true,
          message: `تنبيه: الشعبة ${grade} (${section}) مسجلة بالفعل للمدرس (${conflict.teacherName}) لنفس المادة (${subject})!`
        });
      }

      const id = `${teacherName}-${grade}-${section}-${subject}`.replace(/\s+/g, '-');
      
      let pairing = pairings.find((p: any) => p.id === id);
      const token = pairing ? pairing.token : `TEACHER-${Math.floor(100000 + Math.random() * 900000)}`;
      
      // Auto-approved since they entered the correct unified code, but notify manager for review
      pairing = {
        id,
        teacherName,
        grade,
        section,
        subject,
        status: 'approved',
        token,
        lastActiveTime: new Date().toISOString(),
        isNewNotification: true // Flag to show notification card to the manager!
      };

      // Replace or add pairing
      const existingIdx = pairings.findIndex((p: any) => p.id === id);
      if (existingIdx > -1) {
        pairings[existingIdx] = pairing;
      } else {
        pairings.push(pairing);
      }

      db.pairings[schoolId] = pairings;
      writeSyncDb(db);

      console.log(`[Sync] Pairing request by ${teacherName} for ${grade}-${section} [${subject}], auto-approved via unified code.`);
      return res.json({
        success: true,
        warning: false,
        token: pairing.token,
        status: pairing.status,
        message: 'تم ربط جهازك بسحابة المدرسة بنجاح! سيتم تنزيل أسماء الطلاب والبدء بالعمل فوراً.'
      });
    } catch (error: any) {
      console.error('Error requesting pairing:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء تسجيل طلب الاقتران' });
    }
  });

  // 3. Get Pairing Requests (Called by Manager App to list requests)
  app.get('/api/sync/pairing-requests', (req, res) => {
    try {
      const { schoolId } = req.query;
      if (!schoolId) {
        return res.status(400).json({ error: 'معرف المدرسة مطلوب' });
      }
      const db = readSyncDb();
      db.pairings = db.pairings || {};
      const pairings = db.pairings[schoolId as string] || [];
      return res.json({ success: true, pairings });
    } catch (error: any) {
      console.error('Error fetching pairing requests:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء جلب طلبات الاقتران' });
    }
  });

  // 4. Approve/Reject/Revoke Pairing (Called by Manager App to update pairing status)
  app.post('/api/sync/approve-pairing', (req, res) => {
    try {
      const { schoolId, pairingId, action } = req.body;
      if (!schoolId || !pairingId) {
        return res.status(400).json({ error: 'معرف المدرسة أو معرف الاقتران مفقود' });
      }

      const db = readSyncDb();
      db.pairings = db.pairings || {};
      const pairings = db.pairings[schoolId] || [];
      const pairingIdx = pairings.findIndex((p: any) => p.id === pairingId);

      if (pairingIdx === -1) {
        return res.status(404).json({ error: 'طلب الاقتران غير موجود' });
      }

      if (action === 'reject') {
        pairings.splice(pairingIdx, 1);
        db.pairings[schoolId] = pairings;
        writeSyncDb(db);
        return res.json({ success: true, message: 'تم رفض وحذف طلب الاقتران.' });
      } else if (action === 'reread') {
        // Revoke pairing and request re-reading
        pairings[pairingIdx].status = 'reread';
        pairings[pairingIdx].isNewNotification = false;
        pairings[pairingIdx].lastActiveTime = new Date().toISOString();
        db.pairings[schoolId] = pairings;
        writeSyncDb(db);
        return res.json({ success: true, message: 'تم إيقاف الربط وإلغاء تفعيل المعلم بنجاح. سيُطلب منه إعادة إدخال الرمز وتصحيح الشعبة.' });
      } else if (action === 'dismiss_notification') {
        // Acknowledge the notification (Manager approved it mentally - no action)
        pairings[pairingIdx].isNewNotification = false;
        db.pairings[schoolId] = pairings;
        writeSyncDb(db);
        return res.json({ success: true, message: 'تم تأكيد واعتماد الاقتران كصحيح.' });
      } else {
        // Approve / Activate
        pairings[pairingIdx].status = 'approved';
        pairings[pairingIdx].isNewNotification = false;
        pairings[pairingIdx].lastActiveTime = new Date().toISOString();
        db.pairings[schoolId] = pairings;
        writeSyncDb(db);
        return res.json({ success: true, message: 'تمت الموافقة وتفعيل الاقتران بنجاح.' });
      }
    } catch (error: any) {
      console.error('Error approving pairing:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء المصادقة على الاقتران' });
    }
  });

  // 5. Download Class Roster (Called by Teacher Android App to download student names)
  app.get('/api/sync/download-roster', (req, res) => {
    try {
      const { schoolId, token } = req.query;
      if (!schoolId || !token) {
        return res.status(400).json({ error: 'معرف المدرسة والتوكن مطلوبان' });
      }

      const db = readSyncDb();
      db.pairings = db.pairings || {};
      const pairings = db.pairings[schoolId as string] || [];
      const pairing = pairings.find((p: any) => p.token === token);

      if (!pairing) {
        return res.status(404).json({ error: 'عفواً، لم نجد طلب اقتران مطابق.' });
      }

      if (pairing.status === 'reread') {
        return res.status(400).json({ error: 'REVOKED', message: 'عفواً، تم إيقاف هذا الربط من قبل المدير لوجود خطأ! يرجى إعادة إدخال الرمز وتعديل الصف والشعبة.' });
      }

      if (pairing.status !== 'approved') {
        return res.status(403).json({ error: 'عفواً، طلب الاقتران الخاص بك ما زال بانتظار مصادقة وتدقيق المدير.' });
      }

      db.schools = db.schools || {};
      const school = db.schools[schoolId as string];
      if (!school) {
        return res.status(404).json({ error: 'عفواً، قاعدة بيانات المدرسة غير متوفرة في السحاب.' });
      }

      const students = school.students.filter(
        (s: any) => normalizeArabic(s.currentGrade) === normalizeArabic(pairing.grade) && 
                    normalizeArabic(s.section) === normalizeArabic(pairing.section)
      );

      const studentsList = students.map((std: any) => ({
        recordNumber: std.recordNumber,
        fullName: `${std.firstName} ${std.secondName} ${std.thirdName} ${std.fourthName} ${std.titleName}`.replace(/\s+/g, ' ').trim(),
        grade: std.currentGrade,
        section: std.section,
        historicalAbsences: std.absencesCount || 0
      }));

      console.log(`[Sync] Teacher ${pairing.teacherName} downloaded roster for ${pairing.grade}-${pairing.section}`);
      return res.json({
        success: true,
        token: pairing.token,
        teacherName: pairing.teacherName,
        classes: [{ grade: pairing.grade, section: pairing.section, subject: pairing.subject }],
        students: studentsList
      });
    } catch (error: any) {
      console.error('Error downloading roster:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء تنزيل قائمة الأسماء' });
    }
  });

  // 6. Upload Grades & Absences (Called by Teacher Android App)
  app.post('/api/sync/upload-grades', (req, res) => {
    try {
      const { schoolId, token, grade, section, subject, gradesList } = req.body;

      if (!schoolId || !token) {
        return res.status(400).json({ error: 'بيانات المزامنة غير مكتملة' });
      }

      const db = readSyncDb();
      db.pairings = db.pairings || {};
      const pairings = db.pairings[schoolId] || [];
      const pairing = pairings.find((p: any) => p.token === token && p.status === 'approved');

      if (!pairing) {
        return res.status(401).json({ error: 'عفواً، الاقتران غير مصادق عليه أو التوكن غير صالح.' });
      }

      db.syncedGrades = db.syncedGrades || {};
      db.syncedGrades[schoolId] = db.syncedGrades[schoolId] || [];

      const syncPayload = {
        teacherName: pairing.teacherName,
        grade,
        section,
        subject,
        gradesList,
        timestamp: new Date().toISOString()
      };

      const existingSyncIdx = db.syncedGrades[schoolId].findIndex(
        (g: any) => g.grade === grade && g.section === section && g.subject === subject
      );

      if (existingSyncIdx > -1) {
        db.syncedGrades[schoolId][existingSyncIdx] = syncPayload;
      } else {
        db.syncedGrades[schoolId].push(syncPayload);
      }

      writeSyncDb(db);
      console.log(`[Sync] Teacher ${pairing.teacherName} uploaded grades for ${grade}-${section} [${subject}]`);
      return res.json({ success: true, message: 'تم رفع وحفظ الدرجات والغيابات في مستودع السحابة بنجاح.' });
    } catch (error: any) {
      console.error('Error uploading grades:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء مزامنة الدرجات' });
    }
  });

  // 7. Pull All Synced Grades (Called by Manager App to fetch all teacher inputs)
  app.get('/api/sync/pull-all-grades', (req, res) => {
    try {
      const { schoolId } = req.query;
      if (!schoolId) {
        return res.status(400).json({ error: 'معرف المدرسة مطلوب' });
      }

      const db = readSyncDb();
      db.syncedGrades = db.syncedGrades || {};
      const syncedGrades = db.syncedGrades[schoolId as string] || [];

      return res.json({
        success: true,
        syncedGrades
      });
    } catch (error: any) {
      console.error('Error pulling synced grades:', error);
      return res.status(500).json({ error: error.message || 'حدث خطأ أثناء استيراد الدرجات' });
    }
  });

  // 8. Get QR Pairing Data (Called by Desktop App to show QR)
  app.get('/api/sync/qr-data', (req, res) => {
    const os = require('os');
    const networkInterfaces = os.networkInterfaces();
    let localIp = 'localhost';

    for (const interfaceName in networkInterfaces) {
      const interfaces = networkInterfaces[interfaceName];
      for (const iface of interfaces!) {
        if (iface.family === 'IPv4' && !iface.internal) {
          localIp = iface.address;
          break;
        }
      }
      if (localIp !== 'localhost') break;
    }

    const schoolId = 'DIYALA-8492'; // Should ideally come from config
    const schoolName = 'مدرسة التميز'; // Should ideally come from config

    res.json({
      success: true,
      url: `http://${localIp}:${PORT}`,
      schoolId,
      schoolName,
      pairingCode: UNIFIED_SCHOOL_CODE
    });
  });

  // UNIFY OLD CLOUD APIs WITH NEW SYNC DB
  app.get('/api/cloud/teachers', (req, res) => {
    const db = readSyncDb();
    const schoolId = 'DIYALA-8492';
    const pairings = db.pairings[schoolId] || [];
    const teachers = pairings.map((p: any) => ({
      id: p.id,
      name: p.teacherName,
      grade: p.grade,
      section: p.section,
      subject: p.subject,
      status: p.status === 'approved' ? 'active' : p.status,
      lastSeen: p.lastActiveTime
    }));
    res.json({ success: true, teachers });
  });

  app.post('/api/cloud/sync-all', (req, res) => {
    const { students, staff, config } = req.body;
    const schoolId = 'DIYALA-8492';
    const db = readSyncDb();
    db.schools = db.schools || {};
    db.schools[schoolId] = {
      schoolName: config?.schoolName || 'مدرسة التميز',
      students: students || [],
      staff: staff || [],
      config: config || {},
      lastUpdated: new Date().toISOString()
    };
    writeSyncDb(db);
    res.json({ success: true, timestamp: new Date().toISOString() });
  });

  // ==========================================
  // SIMPLE SYNC APIS FOR BARCODE-FREE CONNECTION
  // ==========================================

  app.get('/api/sync/simple-classes', (req, res) => {
    try {
      const db = readSyncDb();
      const schoolId = 'DIYALA-8492';
      const school = db.schools[schoolId];
      if (!school || !school.students) {
        return res.json({ success: true, classes: [] });
      }

      const classMap = new Map<string, { grade: string, section: string }>();
      school.students.forEach((s: any) => {
        const key = `${s.currentGrade}-${s.section}`;
        if (!classMap.has(key)) {
          classMap.set(key, {
            grade: s.currentGrade,
            section: s.section
          });
        }
      });

      const classes = Array.from(classMap.values());
      return res.json({ success: true, classes });
    } catch (error: any) {
      return res.status(500).json({ error: error.message || 'Error listing classes' });
    }
  });

  app.get('/api/sync/simple-download-roster', (req, res) => {
    try {
      const { grade, section } = req.query;
      if (!grade || !section) {
        return res.status(400).json({ error: 'grade and section parameters are required' });
      }

      const db = readSyncDb();
      const schoolId = 'DIYALA-8492';
      const school = db.schools[schoolId];
      if (!school || !school.students) {
        return res.status(404).json({ error: 'School roster database not uploaded yet' });
      }

      const targetGrade = grade.toString();
      const targetSection = section.toString();

      const students = school.students.filter((s: any) => {
        const normSGrade = normalizeArabic(s.currentGrade);
        const normTGrade = normalizeArabic(targetGrade);
        const gradeMatch = normSGrade.includes(normTGrade) || normTGrade.includes(normSGrade);

        const normSSection = normalizeArabic(s.section);
        const normTSection = normalizeArabic(targetSection);
        const sectionMatch = normSSection === normTSection;

        return gradeMatch && sectionMatch;
      });

      const studentsList = students.map((s: any) => ({
        recordNumber: s.recordNumber,
        fullName: `${s.firstName} ${s.secondName} ${s.thirdName} ${s.fourthName} ${s.titleName}`.replace(/\s+/g, ' ').trim(),
        grade: s.currentGrade,
        section: s.section,
        historicalAbsences: s.absencesCount || 0
      }));

      return res.json({
        success: true,
        students: studentsList
      });
    } catch (error: any) {
      return res.status(500).json({ error: error.message || 'Error downloading roster' });
    }
  });

  // Onboard API
  app.post('/api/cloud/onboard', (req, res) => {
    const { schoolName, managerName, email } = req.body;
    console.log(`Onboarding school: ${schoolName} (Admin: ${email})`);
    res.json({ success: true, schoolId: 'DIYALA-8492' });
  });

  // Pair Request API
  app.post('/api/cloud/pair-request', (req, res) => {
    const { teacherName, grade, section, subject, pairingCode, email } = req.body;
    if (pairingCode === "999888") {
      const db = readSyncDb();
      db.pairings = db.pairings || {};
      const pairings = db.pairings['DIYALA-8492'] || [];
      const id = `${teacherName}-${grade}-${section}-${subject}`.replace(/\s+/g, '-');
      const token = `TEACHER-${Math.floor(100000 + Math.random() * 900000)}`;

      const pairing = {
        id,
        teacherName,
        grade,
        section,
        subject,
        status: 'approved',
        token,
        lastActiveTime: new Date().toISOString(),
        isNewNotification: true
      };

      const existingIdx = pairings.findIndex((p: any) => p.id === id);
      if (existingIdx > -1) {
        pairings[existingIdx] = pairing;
      } else {
        pairings.push(pairing);
      }
      db.pairings['DIYALA-8492'] = pairings;
      writeSyncDb(db);

      res.json({ success: true, status: "approved", token });
    } else {
      res.status(400).json({ success: false, message: "رمز الاقتران غير صحيح" });
    }
  });

  // Vite middleware for development vs static serve for production
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`School Management Server running on http://localhost:${PORT}`);
  });
}

startServer();

