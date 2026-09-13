-- ==============================================================================
-- SQL Migration: Setup Daily Assignments, Direct Messages, and Directives
-- Run this script in the Supabase Dashboard -> SQL Editor
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Daily Assignments Table (Homework, Lessons, Tasks)
CREATE TABLE IF NOT EXISTS daily_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    teacher_id TEXT NOT NULL, -- Teacher ID / Name / Token (flexible to avoid FK violations)
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

-- Index for fast query by school, class, section, teacher
CREATE INDEX IF NOT EXISTS idx_daily_assignments_school_class ON daily_assignments(school_id, class_name, section);
CREATE INDEX IF NOT EXISTS idx_daily_assignments_teacher ON daily_assignments(school_id, teacher_id);
CREATE INDEX IF NOT EXISTS idx_daily_assignments_created ON daily_assignments(school_id, created_at DESC);

-- 2. Direct Messages Table (Teacher <-> Student direct communication)
CREATE TABLE IF NOT EXISTS direct_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    sender_id TEXT NOT NULL,
    sender_role TEXT NOT NULL DEFAULT 'teacher', -- 'teacher', 'student', 'principal'
    receiver_id TEXT NOT NULL,
    subject_name TEXT,
    message_text TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    is_read BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_direct_messages_lookup ON direct_messages(school_id, sender_id, receiver_id);
CREATE INDEX IF NOT EXISTS idx_direct_messages_created ON direct_messages(school_id, created_at ASC);

-- 3. Directives Table (Administrative Instructions / Alerts)
CREATE TABLE IF NOT EXISTS directives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id TEXT NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    target_role TEXT DEFAULT 'all',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_directives_school ON directives(school_id, is_active, created_at DESC);

-- ==============================================================================
-- ENABLE ROW LEVEL SECURITY (RLS)
-- ==============================================================================
ALTER TABLE daily_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE direct_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE directives ENABLE ROW LEVEL SECURITY;

-- Drop previous policies if any to avoid collision
DROP POLICY IF EXISTS daily_assignments_tenant_policy ON daily_assignments;
DROP POLICY IF EXISTS direct_messages_tenant_policy ON direct_messages;
DROP POLICY IF EXISTS directives_tenant_policy ON directives;

-- Tenant Policy for Daily Assignments
CREATE POLICY daily_assignments_tenant_policy ON daily_assignments
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
        OR current_user = 'postgres'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
        OR current_user = 'postgres'
    );

-- Tenant Policy for Direct Messages
CREATE POLICY direct_messages_tenant_policy ON direct_messages
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
        OR current_user = 'postgres'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
        OR current_user = 'postgres'
    );

-- Tenant Policy for Directives
CREATE POLICY directives_tenant_policy ON directives
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
        OR current_user = 'postgres'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '') 
        OR current_user = 'service_role'
        OR current_user = 'postgres'
    );

-- ==============================================================================
-- GRANT PERMISSIONS TO anon AND authenticated ROLES
-- ==============================================================================
GRANT ALL ON daily_assignments TO anon, authenticated, service_role;
GRANT ALL ON direct_messages TO anon, authenticated, service_role;
GRANT ALL ON directives TO anon, authenticated, service_role;
