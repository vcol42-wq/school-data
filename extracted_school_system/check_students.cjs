const STABLE_URL = 'https://pexehlvkpdhmpukjydwd.supabase.co';
const STABLE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY';

async function main() {
  const headers = {
    apikey: STABLE_KEY,
    Authorization: `Bearer ${STABLE_KEY}`
  };

  // 1. Get schools
  const resSchools = await fetch(`${STABLE_URL}/rest/v1/schools?select=*`, { headers });
  const schools = await resSchools.json();
  console.log('Schools in Supabase:');
  console.log(schools.map(s => ({ id: s.id, name: s.name })));

  for (const sch of schools) {
    const resStds = await fetch(`${STABLE_URL}/rest/v1/students?school_id=eq.${sch.id}&select=id,record_number,full_name,current_grade,section`, {
      headers: { ...headers, 'x-school-id': sch.id }
    });
    const stds = await resStds.json();
    console.log(`\n=== School: ${sch.id} (${sch.name}) ===`);
    console.log(`Total students in DB: ${Array.isArray(stds) ? stds.length : 0}`);
    if (Array.isArray(stds) && stds.length > 0) {
      console.log('Sample 3 students:', stds.slice(0, 3));
      const sectionBreakdown = {};
      stds.forEach(s => {
        const key = `${s.current_grade} - ${s.section}`;
        sectionBreakdown[key] = (sectionBreakdown[key] || 0) + 1;
      });
      console.log('Students count per section:', sectionBreakdown);
    }
  }
}

main().catch(console.error);
