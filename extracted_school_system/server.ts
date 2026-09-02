import express from 'express';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { GoogleGenerativeAI } from '@google/generative-ai';
import dotenv from 'dotenv';
import cors from 'cors';

dotenv.config();

let activeDirname: string;
try {
  activeDirname = path.dirname(fileURLToPath(import.meta.url));
} catch (e) {
  activeDirname = __dirname;
}

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(cors());
  app.use(express.json({ limit: '100mb' })); // Higher limit for complete school data

  const DEFAULT_KEY = process.env.GEMINI_API_KEY || '';

  app.post('/api/ai-assistant', async (req, res) => {
    try {
      const { query, audioBase64, schoolName, students = [], staff = [], config, userApiKey } = req.body;

      const apiKey = userApiKey || DEFAULT_KEY;
      const genAI = new GoogleGenerativeAI(apiKey);

      const model = genAI.getGenerativeModel({
        model: "gemini-1.5-flash",
        systemInstruction: `أنت مساعد المدير الذكي والمحترف لنظام الإدارة المدرسية الموحد (The Principal).
        مهمتك هي مساعدة المدير في إدارة المدرسة بكفاءة من خلال تحليل البيانات وتقديم الرؤى الإدارية.
        اسم المدرسة الحالية: ${schoolName || 'غير محدد'}

        لديك القدرة على:
        1. تقديم إحصائيات دقيقة حول الطلاب والمعلمين.
        2. تحديد الطلاب المتراجعين دراسياً أو الذين لديهم غيابات كثيرة.
        3. التنقل بين أقسام التطبيق.
        4. اقتراح نصائح إدارية وتربوية.

        تحدث دائماً بلغة عربية فصيحة، رسمية، ومهذبة.`,
        tools: [
          {
            functionDeclarations: [
              {
                name: "get_school_statistics",
                description: "استخراج إحصائيات عامة عن المدرسة (عدد الطلاب، الشعب، الملاك).",
              },
              {
                name: "get_low_grade_students",
                description: "تحديد الطلاب الذين تقل درجاتهم عن حد معين في مادة ما أو في المعدل.",
                parameters: {
                  type: "OBJECT",
                  properties: {
                    threshold: { type: "NUMBER", description: "الدرجة الدنيا (مثلاً 50)" },
                    subject: { type: "STRING", description: "اسم المادة (اختياري)" }
                  },
                  required: ["threshold"]
                }
              },
              {
                name: "navigate_to_page",
                description: "الانتقال إلى صفحة معينة في التطبيق.",
                parameters: {
                  type: "OBJECT",
                  properties: {
                    view: {
                      type: "STRING",
                      enum: ["launcher", "schedule", "students", "student_grades", "staff", "stats", "settings", "sync_center", "management_tips"],
                      description: "اسم الصفحة الهدف"
                    }
                  },
                  required: ["view"]
                }
              }
            ]
          }
        ]
      });

      const chat = model.startChat();
      let result;

      if (audioBase64) {
        result = await chat.sendMessage([
          { inlineData: { data: audioBase64, mimeType: "audio/webm" } },
          { text: "أجب على طلب المدير الصوتي بناءً على صلاحياتك والبيانات المتاحة." }
        ]);
      } else {
        result = await chat.sendMessage(query);
      }

      let response = result.response;
      let functionCalls = response.functionCalls();

      // Handle function calls
      if (functionCalls && functionCalls.length > 0) {
        const call = functionCalls[0];
        let functionResponse;

        if (call.name === "get_school_statistics") {
          const stats = {
            totalStudents: students.length,
            activeStudents: students.filter((s: any) => s.status === 'مستمر').length,
            totalStaff: staff.length,
            sectionsCount: new Set(students.map((s: any) => s.section)).size
          };
          functionResponse = stats;
        } else if (call.name === "get_low_grade_students") {
          const threshold = call.args.threshold as number || 50;
          const subject = call.args.subject as string;

          const lowStudents = students.filter((s: any) => {
            const marks = s.marksHistory || [];
            if (subject) {
              return marks.some((m: any) => m.subject === subject && (m.finalGrade || m.midterm || 0) < threshold);
            }
            // Check overall average if no subject specified
            const avg = marks.length > 0
              ? marks.reduce((acc: number, m: any) => acc + (m.finalGrade || m.midterm || 0), 0) / marks.length
              : 100;
            return avg < threshold;
          }).map((s: any) => ({ name: `${s.firstName} ${s.secondName} ${s.thirdName}`, grade: s.currentGrade, section: s.section }));

          functionResponse = { students: lowStudents.slice(0, 10), count: lowStudents.length };
        } else if (call.name === "navigate_to_page") {
          res.json({
            success: true,
            responseText: `حاضر حضرة المدير، سأقوم بنقلك إلى صفحة ${call.args.view} الآن.`,
            action: "NAVIGATE",
            targetView: call.args.view
          });
          return;
        }

        // Send function result back to model for final text response
        const finalResult = await chat.sendMessage([{
          functionResponse: {
            name: call.name,
            response: functionResponse
          }
        }]);

        res.json({ success: true, responseText: finalResult.response.text() });
      } else {
        res.json({ success: true, responseText: response.text() });
      }

    } catch (error: any) {
      console.error('AI Error:', error);
      res.status(500).json({ success: false, responseText: "عذراً، حدث خطأ في تحليل البيانات الذكي." });
    }
  });

  app.use(express.static(activeDirname));
  app.get('*', (req, res) => {
    const indexPath = path.join(activeDirname, 'index.html');
    if (fs.existsSync(indexPath)) res.sendFile(indexPath);
    else res.status(404).send('System Error');
  });

  const server = app.listen(PORT, '0.0.0.0', () => {
    console.log(`Principal AI Engine v6.5 Active on port ${PORT}`);
  });

  server.on('error', (err: any) => {
    if (err.code === 'EADDRINUSE') {
      console.warn(`Port ${PORT} is already in use by another instance or dev server. Reusing existing server.`);
    } else {
      console.error('Server startup error:', err);
    }
  });
}

startServer().catch(err => {
  console.warn('Server init warning:', err);
});

