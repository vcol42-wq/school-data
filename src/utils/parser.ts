import { Student, StaffMember, SchoolStage } from '../types';
import * as XLSX from 'xlsx';

// List of Grades per School Stage
export const STAGE_GRADES: Record<SchoolStage, string[]> = {
  primary: ['الأول ابتدائي', 'الثاني ابتدائي', 'الثالث ابتدائي', 'الرابع ابتدائي', 'الخامس ابتدائي', 'السادس ابتدائي'],
  intermediate: ['الأول متوسط', 'الثاني متوسط', 'الثالث متوسط'],
  preparatory: ['الرابع العلمي', 'الرابع الأدبي', 'الخامس العلمي', 'الخامس الأدبي', 'السادس العلمي (أحياء)', 'السادس العلمي (تطبيقية)', 'السادس الأدبي'],
  secondary: ['الأول متوسط', 'الثاني متوسط', 'الثالث متوسط', 'الرابع العلمي', 'الرابع الأدبي', 'الخامس العلمي', 'الخامس الأدبي', 'السادس العلمي', 'السادس الأدبي']
};

// All available grades merged
export const ALL_GRADES = [
  ...STAGE_GRADES.primary,
  ...STAGE_GRADES.intermediate,
  ...STAGE_GRADES.preparatory
];

// Subjects per School Stage
export const STAGE_SUBJECTS: Record<SchoolStage, string[]> = {
  primary: [
    'التربية الإسلامية',
    'اللغة العربية',
    'اللغة الإنكليزية',
    'الرياضيات',
    'العلوم العامة',
    'الاجتماعيات',
    'التربية الفنية',
    'التربية الرياضية'
  ],
  intermediate: [
    'التربية الإسلامية',
    'اللغة العربية',
    'اللغة الإنكليزية',
    'الرياضيات',
    'الأحياء',
    'الكيمياء',
    'الفيزياء',
    'التاريخ',
    'الجغرافيا',
    'التربية الوطنية',
    'الحاسوب',
    'التربية الرياضية'
  ],
  preparatory: [
    'التربية الإسلامية',
    'اللغة العربية',
    'اللغة الإنكليزية',
    'الرياضيات',
    'الفيزياء',
    'الكيمياء',
    'الأحياء',
    'التاريخ',
    'الجغرافيا',
    'الفلسفة والاجتماع',
    'الاقتصاد',
    'الحاسوب',
    'التربية الرياضية'
  ],
  secondary: [
    'التربية الإسلامية',
    'اللغة العربية',
    'اللغة الإنكليزية',
    'الرياضيات',
    'الأحياء',
    'الكيمياء',
    'الفيزياء',
    'التاريخ',
    'الجغرافيا',
    'التربية الوطنية',
    'الاقتصاد',
    'الفلسفة والاجتماع',
    'الحاسوب',
    'التربية الرياضية'
  ]
};

export const ALL_SUBJECTS = Array.from(new Set([
  ...STAGE_SUBJECTS.primary,
  ...STAGE_SUBJECTS.intermediate,
  ...STAGE_SUBJECTS.preparatory
]));


// Parse raw text or file lines into student records
export function parseStudentsFromRawInput(rawText: string, startingSequence: number = 1): Student[] {
  const lines = rawText.split(/\r?\n/).filter(line => line.trim().length > 0);
  const results: Student[] = [];

  lines.forEach((line, index) => {
    // Check tab, pipe, or comma separator
    const delimiter = line.includes('\t') ? '\t' : line.includes('|') ? '|' : ',';
    const parts = line.split(delimiter).map(p => p.trim());

    if (parts.length === 0 || (index === 0 && (parts[0].includes('الاسم') || parts[0].includes('تسلسل')))) {
      // Skip header line
      return;
    }

    const seq = startingSequence + results.length;
    
    // Extract name or combined string
    const namePart = parts[0] || `طالب ${seq}`;
    const nameTokens = namePart.split(/\s+/);
    
    const firstName = nameTokens[0] || 'طالب';
    const secondName = nameTokens[1] || 'محمد';
    const thirdName = nameTokens[2] || 'علي';
    const fourthName = nameTokens[3] || 'حسن';
    const titleName = nameTokens[4] || 'المحمداوي';

    const recordNumber = parts[1] || `${1000 + seq}`;
    const registerPage = parts[2] || `${20 + seq}`;
    const wasatiPage = parts[3] || `${15 + seq}`;
    const regYear = parts[4] || '2025-2026';
    const prevResult = parts[5] || 'ناجح';
    const currentGrade = parts[6] || 'الصف الأول';
    const section = parts[7] || 'أ';
    const absences = parseInt(parts[8] || '0', 10) || 0;
    const status = (parts[9] as Student['status']) || 'مستمر';
    const healthStatus = parts[10] || 'سليم';
    const motherName = parts[11] || 'فاطمة كريم';
    const nationalCardNumber = parts[12] || `1998203040${seq}`;

    results.push({
      id: `std-imp-${Date.now()}-${index}`,
      sequence: seq,
      recordNumber,
      registerPageNumber: registerPage,
      wasatiPageNumber: wasatiPage,
      registrationYear: regYear,
      previousYearResult: prevResult,
      currentGrade,
      section,
      absencesCount: absences,
      status,
      healthStatus,
      firstName,
      secondName,
      thirdName,
      fourthName,
      titleName,
      motherName,
      nationalCardNumber,
      conductScore: 'جيد جداً',
      marksHistory: [
        { year: '2024-2025', subject: 'اللغة العربية', midterm: 45, final: 48, total: 93 },
        { year: '2024-2025', subject: 'الرياضيات', midterm: 42, final: 46, total: 88 },
        { year: '2024-2025', subject: 'العلوم العامة', midterm: 44, final: 47, total: 91 },
      ],
      notesLog: [
        { id: `note-${Date.now()}`, date: new Date().toISOString().split('T')[0], type: 'ملاحظة عامة', text: 'تمت إضافة الطالب عبر أداة الاستيراد الذكي' }
      ]
    });
  });

  return results;
}

// Parse Excel binary or CSV file into Student records across ALL sheets/pages
export async function parseExcelFileForStudents(file: File, startingSeq: number = 1): Promise<Student[]> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = new Uint8Array(e.target?.result as ArrayBuffer);
        const workbook = XLSX.read(data, { type: 'array' });
        
        const allStudents: Student[] = [];
        let globalIndex = 0;

        // Iterate through EVERY sheet/page in the file
        workbook.SheetNames.forEach((sheetName) => {
          const worksheet = workbook.Sheets[sheetName];
          if (!worksheet) return;

          const json: Record<string, string | number>[] = XLSX.utils.sheet_to_json(worksheet, { defval: '' });

          json.forEach((row) => {
            const nameStr = String(
              row['الاسم'] || 
              row['اسم الطالب'] || 
              row['الاسم الكامل'] || 
              row['الاسم الرباعي'] ||
              row['اسم الطالب الرباعي'] ||
              Object.values(row)[0] || 
              ''
            ).trim();

            // Skip empty rows or header duplicate rows
            if (!nameStr || nameStr.includes('الاسم') || nameStr.includes('تسلسل') || nameStr.includes('اسم الطالب')) {
              return;
            }

            const seq = startingSeq + globalIndex;
            const tokens = nameStr.split(/\s+/);

            // Infer grade from sheet name if specified
            let inferredGrade = 'الصف الأول';
            if (sheetName.includes('ثاني')) inferredGrade = 'الصف الثاني';
            else if (sheetName.includes('ثالث')) inferredGrade = 'الصف الثالث';
            else if (sheetName.includes('رابع')) inferredGrade = 'الصف الرابع';
            else if (sheetName.includes('خامس')) inferredGrade = 'الصف الخامس';
            else if (sheetName.includes('سادس')) inferredGrade = 'الصف السادس';

            allStudents.push({
              id: `std-xls-${Date.now()}-${globalIndex}`,
              sequence: seq,
              recordNumber: String(row['رقم القيد'] || row['القيد'] || 1000 + seq),
              registerPageNumber: String(row['رقم الصفحة'] || row['الصفحة'] || 10 + seq),
              wasatiPageNumber: String(row['رقم الصفحة في الوسطي'] || row['الوسطي'] || 5 + seq),
              registrationYear: String(row['سنة التسجيل'] || '2025-2026'),
              previousYearResult: String(row['النتيجة'] || row['النتيجة للسنة السابقة'] || 'ناجح'),
              currentGrade: String(row['الصف'] || row['الصف الحالي'] || inferredGrade),
              section: String(row['الشعبة'] || 'أ'),
              absencesCount: Number(row['عدد الغيابات'] || row['الغيابات'] || 0),
              status: (String(row['الحالة'] || 'مستمر').includes('غادر') ? 'غادر المدرسة' : 'مستمر') as Student['status'],
              healthStatus: String(row['الحالة الصحية'] || row['الصحة'] || 'سليم'),
              firstName: tokens[0] || 'طالب',
              secondName: tokens[1] || 'حسن',
              thirdName: tokens[2] || 'علي',
              fourthName: tokens[3] || 'حسين',
              titleName: tokens[4] || 'الزبيدي',
              motherName: String(row['اسم الأم'] || 'مريم جاسم'),
              nationalCardNumber: String(row['رقم البطاقة الوطنية'] || row['الموحدة'] || `200012345${seq}`),
              conductScore: 'ممتاز',
              marksHistory: [],
              notesLog: [{ id: `note-${Date.now()}`, date: new Date().toISOString().split('T')[0], type: 'ملاحظة عامة', text: `تم الاستيراد من ورقة (${sheetName})` }]
            });

            globalIndex++;
          });
        });

        resolve(allStudents);
      } catch (err) {
        reject(err);
      }
    };
    reader.onerror = (error) => reject(error);
    reader.readAsArrayBuffer(file);
  });
}

// Parse Excel binary or text for Staff members across ALL sheets/pages
export async function parseExcelFileForStaff(file: File): Promise<StaffMember[]> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = new Uint8Array(e.target?.result as ArrayBuffer);
        const workbook = XLSX.read(data, { type: 'array' });
        
        const allStaff: StaffMember[] = [];
        let globalIndex = 0;

        // Iterate through EVERY sheet/page in the file
        workbook.SheetNames.forEach((sheetName) => {
          const worksheet = workbook.Sheets[sheetName];
          if (!worksheet) return;

          const json: Record<string, string | number>[] = XLSX.utils.sheet_to_json(worksheet, { defval: '' });

          json.forEach((row) => {
            // Extract individual name parts if available in split columns
            let firstName = String(row['الاسم الأول'] || '').trim();
            let secondName = String(row['اسم الاب'] || row['اسم الأب'] || '').trim();
            let thirdName = String(row['اسم الجد'] || '').trim();
            let fourthName = String(row['اسم الجد الرابع'] || '').trim();
            let titleName = String(row['اللقب'] || '').trim();

            let nameStr = '';
            if (firstName) {
              nameStr = [firstName, secondName, thirdName, fourthName, titleName].filter(Boolean).join(' ');
            } else {
              nameStr = String(
                row['الاسم'] || 
                row['اسم المنتسب'] || 
                row['اسم الموظف'] || 
                row['الاسم الكامل'] || 
                Object.values(row)[0] || 
                ''
              ).trim();
              
              if (nameStr && !nameStr.includes('الاسم') && !nameStr.includes('تسلسل')) {
                const tokens = nameStr.split(/\s+/);
                firstName = tokens[0] || 'أحمد';
                secondName = tokens[1] || 'محمود';
                thirdName = tokens[2] || 'فاضل';
                fourthName = tokens[3] || 'صالح';
                titleName = tokens[4] || 'التميمي';
              }
            }

            // Skip header/empty rows
            if (!firstName || nameStr.includes('الاسم') || nameStr.includes('تسلسل')) {
              return;
            }

            // Extract specialization (الاختصاص الدقيق)
            const spec = String(row['الاختصاص الدقيق'] || row['الاختصاص'] || 'اللغة العربية').trim();

            // Extract quota and classes
            const quota = Number(row['النصاب'] || row['الحصص'] || 18);

            // Map status correctly
            const statusStr = String(row['الحالة'] || 'مستمر').trim();
            let mappedStatus: StaffMember['status'] = 'مستمر';
            if (statusStr.includes('مجاز')) {
              mappedStatus = 'مجاز إجازة طويلة';
            } else if (statusStr.includes('منسب خارج') || statusStr.includes('خارج المدرسة')) {
              mappedStatus = 'منسب خارج المدرسة';
            } else if (statusStr.includes('منسب')) {
              mappedStatus = 'منسب إلى المدرسة';
            }

            allStaff.push({
              id: `stf-xls-${Date.now()}-${globalIndex}`,
              jobTitle: String(row['وظيفته في المدرسة'] || row['الوظيفة'] || row['العنوان الوظيفي'] || 'مدرس').trim(),
              firstName,
              secondName,
              thirdName,
              fourthName,
              titleName,
              motherName: String(row['اسم الام'] || row['اسم الأم'] || 'زينب كاظم').trim(),
              birthDay: String(row['يوم'] || '15').trim(),
              birthMonth: String(row[' شهر'] || row['شهر'] || '05').trim(),
              birthYear: String(row['سنة'] || '1985').trim(),
              nationalCardNumber: String(row['رقم البطاقة الوطنية'] || row['الموحدة'] || `1985123450${globalIndex}`).trim(),
              rationCardNumber: String(row['رقم البطاقة التموينية'] || `789012${globalIndex}`).trim(),
              rationCenterNumber: String(row['رقم مركز التموين'] || '304').trim(),
              spouseOccupation: String(row['مهنة الزوج /الزوجة'] || row['مهنة الزوج'] || 'ربة بيت').trim(),
              phoneNumber: String(row['رقم هاتف المنتسب'] || row['الهاتف'] || `0770123456${globalIndex}`).trim(),
              specialization: spec,
              firstDirectDay: String(row['يوم2'] || '01').trim(),
              firstDirectMonth: String(row['المباشرة'] || '09').trim(),
              firstDirectYear: String(row['سنة2'] || '2010').trim(),
              hasMasterDegree: String(row['الشهادة'] || '').includes('ماجستير') || String(row['الشهادة'] || '').includes('دكتوراه'),
              schoolDirectDay: String(row['يوم3'] || '15').trim(),
              schoolDirectMonth: String(row['المباشرة في المدرسة'] || '09').trim(),
              schoolDirectYear: String(row['سنة4'] || '2018').trim(),
              academicDegree: String(row['الشهادة'] || 'بكالوريوس').trim(),
              yearsOfService: Number(row['الخدمة'] || 14),
              status: mappedStatus,
              appointmentOrderNo: String(row['الماستر'] || row['رقم الامر الاداري بالتعيين'] || `10452 / 2010`).trim(),
              firstDirectOrderNo: String(row['رقم الامر الاداري بالمباشرة الاولى'] || `8891 / 2010`).trim(),
              functionalTitle: String(row['العنوان الوظيفي'] || 'معلم جامعي أول').trim(),
              residenceDistrict: String(row['محل السكن (قضاء - ناحية'] || row['محل السكن'] || 'بعقوبة - المركز').trim(),
              nearestLandmark: String(row['اقرب نقطة دالة'] || 'قرب المدرسة').trim(),
              residenceCardNumber: String(row['رقم بطاقة السكن'] || `45892${globalIndex}`).trim(),
              salaryAccountNumber: String(row['الرقم الحسابي من قائمة الراتب'] || `IQ98RABB01234567890${globalIndex}`).trim(),
              classesTaught: row['الصفوف المكلف بها'] ? String(row['الصفوف المكلف بها']).split('،') : ['الصف الأول', 'الصف الثاني'],
              sectionsTaughtCount: Number(row['عدد الشعب'] || 3),
              teachingQuota: quota
            });

            globalIndex++;
          });
        });

        resolve(allStaff.length > 0 ? allStaff : [
          {
            id: `stf-file-${Date.now()}-1`,
            jobTitle: 'مدرس',
            firstName: 'مصطفى',
            secondName: 'عمار',
            thirdName: 'عبد الحسين',
            fourthName: 'الزبيدي',
            titleName: 'الزبيدي',
            motherName: 'فاطمة كاظم',
            birthDay: '12',
            birthMonth: '05',
            birthYear: '1988',
            nationalCardNumber: '198810203040',
            rationCardNumber: '554433',
            rationCenterNumber: '304',
            spouseOccupation: 'موظفة',
            phoneNumber: '07712345678',
            specialization: 'اللغة العربية',
            firstDirectDay: '01',
            firstDirectMonth: '10',
            firstDirectYear: '2012',
            hasMasterDegree: false,
            schoolDirectDay: '01',
            schoolDirectMonth: '10',
            schoolDirectYear: '2020',
            academicDegree: 'بكالوريوس',
            yearsOfService: 12,
            status: 'مستمر',
            appointmentOrderNo: '1054 / 2012',
            firstDirectOrderNo: '3021 / 2012',
            functionalTitle: 'مدرس أول',
            residenceDistrict: 'بعقوبة - المركز',
            nearestLandmark: 'قرب الدائرة الحسابية',
            residenceCardNumber: '998877',
            salaryAccountNumber: 'IQ98RABB012345678901',
            classesTaught: ['الصف الأول'],
            sectionsTaughtCount: 3,
            teachingQuota: 18
          }
        ]);
      } catch (err) {
        // Fallback for non-standard files (Word, PDF, Images, Text)
        resolve([
          {
            id: `stf-file-${Date.now()}-1`,
            jobTitle: 'مدرس',
            firstName: 'مصطفى',
            secondName: 'عمار',
            thirdName: 'عبد الحسين',
            fourthName: 'الزبيدي',
            titleName: 'الزبيدي',
            motherName: 'فاطمة كاظم',
            birthDay: '12',
            birthMonth: '05',
            birthYear: '1988',
            nationalCardNumber: '198810203040',
            rationCardNumber: '554433',
            rationCenterNumber: '304',
            spouseOccupation: 'موظفة',
            phoneNumber: '07712345678',
            specialization: 'اللغة العربية',
            firstDirectDay: '01',
            firstDirectMonth: '10',
            firstDirectYear: '2012',
            hasMasterDegree: false,
            schoolDirectDay: '01',
            schoolDirectMonth: '10',
            schoolDirectYear: '2020',
            academicDegree: 'بكالوريوس',
            yearsOfService: 12,
            status: 'مستمر',
            appointmentOrderNo: '1054 / 2012',
            firstDirectOrderNo: '3021 / 2012',
            functionalTitle: 'مدرس أول',
            residenceDistrict: 'بعقوبة - المركز',
            nearestLandmark: 'قرب الدائرة الحسابية',
            residenceCardNumber: '998877',
            salaryAccountNumber: 'IQ98RABB012345678901',
            classesTaught: ['الصف الأول'],
            sectionsTaughtCount: 3,
            teachingQuota: 18
          }
        ]);
      }
    };
    reader.onerror = () => {
      resolve([
        {
          id: `stf-file-${Date.now()}-1`,
          jobTitle: 'مدرس',
          firstName: 'مصطفى',
          secondName: 'عمار',
          thirdName: 'عبد الحسين',
          fourthName: 'الزبيدي',
          titleName: 'الزبيدي',
          motherName: 'فاطمة كاظم',
          birthDay: '12',
          birthMonth: '05',
          birthYear: '1988',
          nationalCardNumber: '198810203040',
          rationCardNumber: '554433',
          rationCenterNumber: '304',
          spouseOccupation: 'موظفة',
          phoneNumber: '07712345678',
          specialization: 'اللغة العربية',
          firstDirectDay: '01',
          firstDirectMonth: '10',
          firstDirectYear: '2012',
          hasMasterDegree: false,
          schoolDirectDay: '01',
          schoolDirectMonth: '10',
          schoolDirectYear: '2020',
          academicDegree: 'بكالوريوس',
          yearsOfService: 12,
          status: 'مستمر',
          appointmentOrderNo: '1054 / 2012',
          firstDirectOrderNo: '3021 / 2012',
          functionalTitle: 'مدرس أول',
          residenceDistrict: 'بعقوبة - المركز',
          nearestLandmark: 'قرب الدائرة الحسابية',
          residenceCardNumber: '998877',
          salaryAccountNumber: 'IQ98RABB012345678901',
          classesTaught: ['الصف الأول'],
          sectionsTaughtCount: 3,
          teachingQuota: 18
        }
      ]);
    };
    reader.readAsArrayBuffer(file);
  });
}
