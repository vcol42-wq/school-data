const { createClient } = require('@supabase/supabase-js');

const url = 'https://pexehlvkpdhmpukjydwd.supabase.co';
const key = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY';
const schoolId = 'SCH-VCOL-6072';

const client = createClient(url, key, {
  global: {
    headers: {
      'x-school-id': schoolId
    }
  }
});

async function main() {
  // Get first real teacher
  const { data: teachers } = await client.from('teachers').select('id, name').eq('school_id', schoolId).limit(1);
  console.log('Real teacher:', teachers);

  if (teachers && teachers.length > 0) {
    const realTeacherId = teachers[0].id;
    const { data: d3, error: e3 } = await client.from('teacher_assignments').upsert([
      { 
        school_id: schoolId, 
        teacher_id: realTeacherId, 
        class_name: 'الأول المتوسط', 
        section: 'أ', 
        subject_name: 'الرياضيات' 
      }
    ], { onConflict: 'school_id,teacher_id,class_name,section,subject_name' });
    console.log('Real Assignment upsert error:', e3);
  }
}

main();
