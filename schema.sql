-- ==============================================================================
-- Database Schema for Supabase School Connection (Multi-Tenant Architecture)
-- Unified Master Schema v5.0
-- ==============================================================================

-- 1. Schools Table (Stores School configuration, Pairing Code & Metadata)
CREATE TABLE IF NOT EXISTS schools (
    id TEXT PRIMARY KEY, -- e.g. "SCH-VCOL-6072" or School Code
    name TEXT NOT NULL,
    pairing_code TEXT NOT NULL DEFAULT '112233',
    admin_email TEXT,
    config JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Teachers Table
CREATE TABLE IF NOT EXISTS teachers (
    id TEXT PRIMARY KEY, -- Teacher ID / Sync Token
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    email TEXT,
    specialization TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Classes Table
CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    name TEXT NOT NULL, -- e.g. "الأول الابتدائي"
    section TEXT NOT NULL, -- e.g. "أ"
    UNIQUE (school_id, name, section)
);

-- 4. Subjects Table
CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    name TEXT NOT NULL, -- e.g. "الرياضيات"
    UNIQUE (school_id, name)
);

-- 5. Teacher Assignments Table (Links Teacher -> Class & Subject)
CREATE TABLE IF NOT EXISTS teacher_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    teacher_id TEXT REFERENCES teachers(id) ON DELETE CASCADE,
    class_name TEXT NOT NULL,
    section TEXT NOT NULL,
    subject_name TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (school_id, teacher_id, class_name, section, subject_name)
);

-- 6. Students Table
CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    record_number TEXT NOT NULL,
    first_name TEXT NOT NULL,
    second_name TEXT,
    third_name TEXT,
    fourth_name TEXT,
    title_name TEXT,
    full_name TEXT NOT NULL,
    current_grade TEXT NOT NULL,
    section TEXT NOT NULL,
    absences_count INTEGER DEFAULT 0,
    status TEXT DEFAULT 'مستمر',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (school_id, record_number)
);

-- 7. Grades Table
CREATE TABLE IF NOT EXISTS grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    student_record_number TEXT NOT NULL,
    subject TEXT NOT NULL,
    grade TEXT NOT NULL,
    section TEXT NOT NULL,
    marks JSONB NOT NULL, -- StudentMarksDto
    teacher_id TEXT REFERENCES teachers(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (school_id, student_record_number, subject)
);

-- 8. Attendance Table
CREATE TABLE IF NOT EXISTS attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    student_record_number TEXT NOT NULL,
    date_string TEXT NOT NULL, -- "yyyy-MM-dd"
    status TEXT NOT NULL DEFAULT 'absent', -- 'absent', 'present'
    subject TEXT NOT NULL,
    teacher_id TEXT REFERENCES teachers(id) ON DELETE SET NULL,
    period_number INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (school_id, student_record_number, date_string, subject, period_number)
);

-- 9. Daily Assignments Table (Homework & Lessons)
CREATE TABLE IF NOT EXISTS daily_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    teacher_id TEXT REFERENCES teachers(id) ON DELETE CASCADE,
    class_name TEXT NOT NULL,
    section TEXT NOT NULL,
    subject_name TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    due_date TEXT,
    is_private_tutoring BOOLEAN DEFAULT FALSE,
    student_record_number TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 10. Direct Messages Table (Chat & Announcements)
CREATE TABLE IF NOT EXISTS direct_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    sender_id TEXT NOT NULL,
    sender_role TEXT NOT NULL, -- 'teacher', 'student', 'principal'
    receiver_id TEXT NOT NULL,
    subject_name TEXT,
    message_text TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    is_read BOOLEAN DEFAULT FALSE
);

-- 11. Schedules Table (Full School Weekly Timetable Map)
CREATE TABLE IF NOT EXISTS schedules (
    id TEXT PRIMARY KEY, -- acts as school_id
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    schedule_map JSONB NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 12. Directives Table (Administrative Instructions)
CREATE TABLE IF NOT EXISTS directives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    target_role TEXT DEFAULT 'all',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 13. Join Requests Table
CREATE TABLE IF NOT EXISTS join_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT REFERENCES schools(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'teacher', -- 'teacher', 'student'
    full_name TEXT NOT NULL,
    device_id TEXT NOT NULL,
    class_name TEXT,
    section_name TEXT,
    subject_specialty TEXT,
    assigned_sections JSONB,
    pairing_code_attempt TEXT,
    status TEXT DEFAULT 'pending', -- 'pending', 'approved', 'rejected'
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (school_id, device_id)
);

-- 14. App Config Table (Shared settings e.g. Gemini AI Key)
CREATE TABLE IF NOT EXISTS app_config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    config_key TEXT GENERATED ALWAYS AS (key) STORED,
    config_value TEXT GENERATED ALWAYS AS (value) STORED,
    description TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Insert Default Gemini Key in app_config
INSERT INTO app_config (key, value, description)
VALUES ('gemini_api_key', 'AIzaSyA92sLsbAsl9HXjEQb2fsADFeOTsvi6aGQ', 'Default Gemini AI Key for all apps')
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- ==============================================================================
-- ENABLE ROW LEVEL SECURITY (RLS) ON ALL TABLES
-- ==============================================================================
ALTER TABLE schools ENABLE ROW LEVEL SECURITY;
ALTER TABLE teachers ENABLE ROW LEVEL SECURITY;
ALTER TABLE classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE students ENABLE ROW LEVEL SECURITY;
ALTER TABLE grades ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE direct_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE directives ENABLE ROW LEVEL SECURITY;
ALTER TABLE join_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_config ENABLE ROW LEVEL SECURITY;

-- ==============================================================================
-- DROP EXISTING POLICIES TO ENSURE CLEAN RE-CREATION
-- ==============================================================================
DO $$
DECLARE
    pol RECORD;
BEGIN
    FOR pol IN 
        SELECT policyname, tablename 
        FROM pg_policies 
        WHERE schemaname = 'public'
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I', pol.policyname, pol.tablename);
    END LOOP;
END $$;

-- ==============================================================================
-- DEFINE HIGH-PERFORMANCE RLS POLICIES (Multi-Tenant Isolation via x-school-id)
-- ==============================================================================

-- 1. Schools Policies:
-- Allow SELECT for all clients (Needed for Pairing Code / ID discovery during onboarding)
CREATE POLICY school_select_policy ON schools
    FOR SELECT USING (true);

-- Allow INSERT / UPDATE / DELETE only when matching header or service_role
CREATE POLICY school_modify_policy ON schools
    FOR ALL USING (
        id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 2. Teachers Policy:
CREATE POLICY teachers_tenant_policy ON teachers
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 3. Classes Policy:
CREATE POLICY classes_tenant_policy ON classes
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 4. Subjects Policy:
CREATE POLICY subjects_tenant_policy ON subjects
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 5. Teacher Assignments Policy:
CREATE POLICY assignments_tenant_policy ON teacher_assignments
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 6. Students Policy:
CREATE POLICY students_tenant_policy ON students
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 7. Grades Policy:
CREATE POLICY grades_tenant_policy ON grades
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 8. Attendance Policy:
CREATE POLICY attendance_tenant_policy ON attendance
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 9. Daily Assignments Policy:
CREATE POLICY daily_assignments_tenant_policy ON daily_assignments
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 10. Direct Messages Policy:
CREATE POLICY direct_messages_tenant_policy ON direct_messages
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 11. Schedules Policy:
CREATE POLICY schedules_tenant_policy ON schedules
    FOR ALL USING (
        id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 12. Directives Policy:
CREATE POLICY directives_tenant_policy ON directives
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 13. Join Requests Policy:
CREATE POLICY join_requests_tenant_policy ON join_requests
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
    );

-- 14. App Config Policy:
CREATE POLICY app_config_read_policy ON app_config
    FOR SELECT USING (true);
