-- Emergency fail-closed migration for direct public-client access.
-- Apply once in Supabase SQL Editor. This intentionally pauses current
-- Android/desktop cloud sync until verified school membership is implemented.
-- Do not restore x-school-id-only policies or grant anon/authenticated access.

BEGIN;

DO $$
DECLARE
    table_name TEXT;
    policy_name TEXT;
    protected_tables TEXT[] := ARRAY[
        'schools',
        'teachers',
        'classes',
        'subjects',
        'teacher_assignments',
        'students',
        'grades',
        'attendance',
        'daily_assignments',
        'direct_messages',
        'schedules',
        'directives',
        'join_requests',
        'app_config',
        'session_upload_tokens',
        'subject_assignments',
        'desktop_licenses'
    ];
BEGIN
    FOREACH table_name IN ARRAY protected_tables LOOP
        IF to_regclass(format('public.%I', table_name)) IS NOT NULL THEN
            EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', table_name);

            FOR policy_name IN
                SELECT p.policyname
                FROM pg_policies AS p
                WHERE p.schemaname = 'public'
                  AND p.tablename = table_name
            LOOP
                EXECUTE format('DROP POLICY %I ON public.%I', policy_name, table_name);
            END LOOP;

            EXECUTE format(
                'CREATE POLICY deny_until_authenticated_school_membership ON public.%I '
                'FOR ALL TO anon, authenticated USING (false) WITH CHECK (false)',
                table_name
            );
            EXECUTE format(
                'REVOKE ALL ON TABLE public.%I FROM anon, authenticated',
                table_name
            );
            EXECUTE format(
                'GRANT ALL ON TABLE public.%I TO service_role',
                table_name
            );
        END IF;
    END LOOP;
END
$$;

-- Release links are intentionally public-readable, but never client-writable.
DO $$
BEGIN
    IF to_regclass('public.app_releases') IS NOT NULL THEN
        DROP POLICY IF EXISTS "Allow release links update" ON public.app_releases;
        DROP POLICY IF EXISTS "Public can view active releases" ON public.app_releases;
        CREATE POLICY "Public can view active releases"
            ON public.app_releases FOR SELECT TO anon, authenticated
            USING (is_active = true);
        REVOKE ALL ON TABLE public.app_releases FROM anon, authenticated;
        GRANT SELECT ON TABLE public.app_releases TO anon, authenticated;
        GRANT ALL ON TABLE public.app_releases TO service_role;
    END IF;
END
$$;

-- Prevent future tables created by the migration owner from inheriting public
-- client table grants. Apply this migration as the schema/table owner.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    REVOKE ALL ON TABLES FROM anon, authenticated;

DO $$
BEGIN
    IF to_regclass('public.subject_assignments_id_seq') IS NOT NULL THEN
        REVOKE ALL ON SEQUENCE public.subject_assignments_id_seq FROM anon, authenticated;
        GRANT ALL ON SEQUENCE public.subject_assignments_id_seq TO service_role;
    END IF;
END
$$;

COMMIT;

-- Verify no client table grants remain on the protected relations.
SELECT protected.table_name,
       (
           has_table_privilege('anon', format('public.%I', protected.table_name), 'SELECT')
           OR has_table_privilege('anon', format('public.%I', protected.table_name), 'INSERT')
           OR has_table_privilege('anon', format('public.%I', protected.table_name), 'UPDATE')
           OR has_table_privilege('anon', format('public.%I', protected.table_name), 'DELETE')
       ) AS anon_has_table_access,
       (
           has_table_privilege('authenticated', format('public.%I', protected.table_name), 'SELECT')
           OR has_table_privilege('authenticated', format('public.%I', protected.table_name), 'INSERT')
           OR has_table_privilege('authenticated', format('public.%I', protected.table_name), 'UPDATE')
           OR has_table_privilege('authenticated', format('public.%I', protected.table_name), 'DELETE')
       ) AS authenticated_has_table_access
FROM unnest(ARRAY[
    'schools', 'teachers', 'classes', 'subjects', 'teacher_assignments',
    'students', 'grades', 'attendance', 'daily_assignments', 'direct_messages',
    'schedules', 'directives', 'join_requests', 'app_config',
    'session_upload_tokens', 'subject_assignments', 'desktop_licenses'
]) AS protected(table_name)
WHERE to_regclass(format('public.%I', protected.table_name)) IS NOT NULL
ORDER BY protected.table_name;
