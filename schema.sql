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
    teacher_id TEXT NOT NULL,
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

-- Store Gemini credentials only through a trusted, access-controlled server.

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
          AND tablename = ANY (ARRAY[
              'schools', 'teachers', 'classes', 'subjects', 'teacher_assignments',
              'students', 'grades', 'attendance', 'daily_assignments',
              'direct_messages', 'schedules', 'directives', 'join_requests',
              'app_config', 'session_upload_tokens', 'subject_assignments'
          ])
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I', pol.policyname, pol.tablename);
    END LOOP;
END $$;

-- ==============================================================================
-- TENANT POLICIES FOR EXISTING DIRECT-SYNC TABLES
-- Grades are restricted to the server-verified RPC in secure_grade_access.sql.
-- ==============================================================================

CREATE POLICY school_select_policy ON schools
    FOR SELECT USING (true);

CREATE POLICY school_modify_policy ON schools
    FOR ALL USING (
        id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY teachers_tenant_policy ON teachers
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY classes_tenant_policy ON classes
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY subjects_tenant_policy ON subjects
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY assignments_tenant_policy ON teacher_assignments
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY students_tenant_policy ON students
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY grades_tenant_policy ON grades
    FOR ALL TO anon, authenticated USING (false) WITH CHECK (false);

CREATE POLICY attendance_tenant_policy ON attendance
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY daily_assignments_tenant_policy ON daily_assignments
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY direct_messages_tenant_policy ON direct_messages
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY schedules_tenant_policy ON schedules
    FOR ALL USING (
        id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY directives_tenant_policy ON directives
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY join_requests_tenant_policy ON join_requests
    FOR ALL USING (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

CREATE POLICY app_config_read_policy ON app_config
    FOR SELECT USING (true);

-- ==============================================================================
-- 15. Session Upload Tokens Table (for secure grade upload PIN & OTP management)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS session_upload_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    teacher_id TEXT REFERENCES teachers(id) ON DELETE SET NULL,
    grade TEXT NOT NULL,
    section TEXT NOT NULL,
    subject TEXT NOT NULL,
    token_code TEXT NOT NULL,
    token_hash TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    used_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    is_valid BOOLEAN DEFAULT TRUE,
    UNIQUE (school_id, token_code)
);

CREATE INDEX IF NOT EXISTS idx_session_tokens_lookup ON session_upload_tokens(
    school_id, grade, section, subject, token_code
);

CREATE INDEX IF NOT EXISTS idx_session_tokens_expiry ON session_upload_tokens(
    school_id, expires_at
);

ALTER TABLE session_upload_tokens ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS session_tokens_tenant_policy ON session_upload_tokens;
CREATE POLICY session_tokens_tenant_policy ON session_upload_tokens
    FOR ALL TO anon, authenticated USING (false) WITH CHECK (false);

REVOKE ALL ON TABLE session_upload_tokens FROM anon, authenticated;
GRANT ALL ON TABLE session_upload_tokens TO service_role;

-- ==============================================================================
-- 16. Subject Assignments Table (for staff authority, teaching quotas & PIN verification)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS subject_assignments (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    school_id TEXT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    grade TEXT NOT NULL,
    section TEXT NOT NULL,
    subject TEXT NOT NULL,
    secret_code TEXT NOT NULL,
    is_locked BOOLEAN DEFAULT FALSE,
    teacher_name TEXT,
    last_updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (school_id, grade, section, subject)
);

CREATE INDEX IF NOT EXISTS idx_subject_assignments_lookup ON subject_assignments(school_id, grade, section, subject);
CREATE INDEX IF NOT EXISTS idx_subject_assignments_pin ON subject_assignments(school_id, secret_code);

ALTER TABLE subject_assignments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS subject_assignments_tenant_policy ON subject_assignments;
CREATE POLICY subject_assignments_tenant_policy ON subject_assignments
    FOR SELECT TO anon, authenticated
    USING (school_id = COALESCE(current_setting('request.headers', true)::json->>'x-school-id', ''));

REVOKE ALL ON TABLE subject_assignments FROM anon, authenticated;
GRANT SELECT (school_id, grade, section, subject, is_locked, teacher_name, last_updated_at)
    ON subject_assignments TO anon, authenticated;
GRANT ALL ON TABLE subject_assignments TO service_role;
GRANT ALL ON SEQUENCE subject_assignments_id_seq TO service_role;

-- ==============================================================================
-- 17. RPC Helper: admin_manage_subject_assignments
-- ==============================================================================
CREATE OR REPLACE FUNCTION admin_manage_subject_assignments(
    p_school_id TEXT,
    p_supervisor_pin TEXT,
    p_action TEXT DEFAULT 'clear',
    p_teacher_name TEXT DEFAULT NULL,
    p_records JSONB DEFAULT '[]'::jsonb
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    IF p_action = 'clear' THEN
        DELETE FROM subject_assignments WHERE school_id = p_school_id;
        RETURN TRUE;
    ELSIF p_action = 'delete_teacher' AND p_teacher_name IS NOT NULL THEN
        DELETE FROM subject_assignments WHERE school_id = p_school_id AND teacher_name = p_teacher_name;
        RETURN TRUE;
    END IF;
    RETURN TRUE;
END;
$$;

GRANT EXECUTE ON FUNCTION admin_manage_subject_assignments TO anon, authenticated, service_role;
