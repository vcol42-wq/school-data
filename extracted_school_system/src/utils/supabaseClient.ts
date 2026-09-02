import { createClient, SupabaseClient } from '@supabase/supabase-js';

// OFFICIAL PRODUCTION CREDENTIALS - DO NOT CHANGE
const STABLE_URL = 'https://pexehlvkpdhmpukjydwd.supabase.co';
const STABLE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY';

export const getSupabaseUrl = (): string => STABLE_URL;
export const getSupabaseKey = (): string => STABLE_KEY;

const clientCache = new Map<string, SupabaseClient>();

export const getSupabase = (customSchoolId?: string): SupabaseClient => {
  const schoolId = customSchoolId || localStorage.getItem('diyala_school_id') || 'school_01';
  if (!clientCache.has(schoolId)) {
    const client = createClient(STABLE_URL, STABLE_KEY, {
      auth: {
        persistSession: false,
        autoRefreshToken: false
      },
      global: {
        headers: {
          'x-school-id': schoolId
        }
      }
    });
    clientCache.set(schoolId, client);
  }
  return clientCache.get(schoolId)!;
};

export const refreshSupabaseClient = (schoolId?: string) => {
  if (schoolId) {
    clientCache.delete(schoolId);
  } else {
    clientCache.clear();
  }
  return getSupabase(schoolId);
};

// Dynamic proxy that always uses the active schoolId from localStorage
export const supabase: SupabaseClient = new Proxy({} as SupabaseClient, {
  get(_target, prop, receiver) {
    const activeClient = getSupabase();
    const value = Reflect.get(activeClient, prop, receiver);
    if (typeof value === 'function') {
      return value.bind(activeClient);
    }
    return value;
  }
});

export const isSupabaseConfigured = (): boolean => true;
