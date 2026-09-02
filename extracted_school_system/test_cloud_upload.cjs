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
  console.log('--- 1. Testing classes ---');
  const { data: d1, error: e1 } = await client.from('classes').upsert([
    { school_id: schoolId, name: 'الأول المتوسط', section: 'أ' }
  ], { onConflict: 'school_id,name,section' });
  console.log('classes error:', e1);

  console.log('--- 2. Testing subjects ---');
  const { data: d2, error: e2 } = await client.from('subjects').upsert([
    { school_id: schoolId, name: 'الرياضيات' }
  ], { onConflict: 'school_id,name' });
  console.log('subjects error:', e2);

  console.log('--- 3. Testing teacher_assignments ---');
  const { data: d3, error: e3 } = await client.from('teacher_assignments').upsert([
    { school_id: schoolId, teacher_id: '1', class_name: 'الأول المتوسط', section: 'أ', subject_name: 'الرياضيات' }
  ], { onConflict: 'school_id,teacher_id,class_name,section,subject_name' });
  console.log('assignments error:', e3);
}

main();
