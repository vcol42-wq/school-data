-- ==============================================================================
-- SQL Script: Complete Cloud Wipe of All Schools & Associated Data
-- The Principal v6.0 Super Edition
-- ==============================================================================

-- 1. Truncate schools table with CASCADE (cleans teachers, classes, subjects, 
-- teacher_assignments, students, grades, attendance, daily_assignments, 
-- direct_messages, directives, schedules, etc.)
TRUNCATE TABLE public.schools CASCADE;

-- 2. Clean temporary session upload tokens
TRUNCATE TABLE public.session_upload_tokens CASCADE;

-- 3. Clean any demo desktop licenses
DELETE FROM public.desktop_licenses WHERE license_key LIKE 'BOSS-8492%' OR school_name LIKE '%تجريب%';

-- Verification Query: All should return 0
SELECT 'schools' AS tbl, count(*) FROM public.schools
UNION ALL
SELECT 'teachers', count(*) FROM public.teachers
UNION ALL
SELECT 'students', count(*) FROM public.students
UNION ALL
SELECT 'classes', count(*) FROM public.classes
UNION ALL
SELECT 'grades', count(*) FROM public.grades;
