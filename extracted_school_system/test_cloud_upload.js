import { getSupabase } from './src/utils/supabaseClient.js';

const client = getSupabase('SCH-VCOL-6072');

async function testAll() {
  console.log('Testing classes...');
  const { data: d1, error: e1 } = await client.from('classes').upsert([
    { school_id: 'SCH-VCOL-6072', name: 'الأول المتوسط', section: 'أ' }
  ], { onConflict: 'school_id,name,section' });
  console.log('Classes result:', { d1, e1 });

  console.log('Testing subjects...');
  const { data: d2, error: e2 } = await client.from('subjects').upsert([
    { school_id: 'SCH-VCOL-6072', name: 'الرياضيات' }
  ], { onConflict: 'school_id,name' });
  console.log('Subjects result:', { d2, e2 });

  console.log('Testing teacher_assignments...');
  const { data: d3, error: e3 } = await client.from('teacher_assignments').upsert([
    { school_id: 'SCH-VCOL-6072', teacher_id: '1', class_name: 'الأول المتوسط', section: 'أ', subject_name: 'الرياضيات' }
  ], { onConflict: 'school_id,teacher_id,class_name,section,subject_name' });
  console.log('Assignments result:', { d3, e3 });
}

testAll();
