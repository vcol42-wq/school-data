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
  console.log('Testing GET students from Supabase...');
  const { data: students, error } = await client
    .from('students')
    .select('*')
    .eq('school_id', schoolId)
    .limit(10);

  console.log('Students count fetched:', students ? students.length : 0);
  console.log('Sample student:', students ? students[0] : null);
  console.log('Error:', error);
}

main();
