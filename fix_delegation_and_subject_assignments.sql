-- ==============================================================================
-- SQL Fix & Migration: Clean Delegation & Authorize subject_assignments Table
-- نفذ هذا الاستعلام في لوحة تحكم Supabase Dashboard -> SQL Editor
-- لتطهير السجلات المشوهة فورياً وتفعيل صلاحيات الحذف والتعديل عبر RLS
-- ==============================================================================

BEGIN;

-- 1. حذف السجلات المشوهة التي تحتوي على أحرف مفردة بدلاً من المواد الحقيقية
DELETE FROM subject_assignments 
WHERE length(trim(subject)) <= 1 
   OR subject ~ '^[أ-يa-zA-Z]$';

-- 2. حذف السجلات التي تحتوي على مسميات وصفات إدارية بدلاً من المواد التدريسية
DELETE FROM subject_assignments 
WHERE subject LIKE '%مفرغ%' 
   OR subject LIKE '%إدارة%' 
   OR subject LIKE '%تفرغ%'
   OR subject LIKE '%شاغر%'
   OR subject LIKE '%معاون%'
   OR subject LIKE '%مدير%';

-- 3. حذف السجلات القديمة غير المعيارية للصفوف (مثل 'الأول' بدلاً من 'الأول المتوسط')
DELETE FROM subject_assignments 
WHERE grade NOT LIKE '%المتوسط%' 
  AND grade NOT LIKE '%الإعدادي%' 
  AND grade NOT LIKE '%الابتدائي%';

-- 4. إزالة أي تكرار لنفس (المدرسة، الصف، الشعبة، المادة) مع الإبقاء على الأحدث دائماً
DELETE FROM subject_assignments a
USING subject_assignments b
WHERE a.school_id = b.school_id
  AND a.grade = b.grade
  AND a.section = b.section
  AND a.subject = b.subject
  AND a.last_updated_at < b.last_updated_at;

-- 5. Expose assignment metadata only; PINs are stored separately and accessed
-- exclusively through server-verified grade and administrator functions.
ALTER TABLE IF EXISTS subject_assignments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS subject_assignments_tenant_policy ON subject_assignments;
CREATE POLICY subject_assignments_tenant_policy ON subject_assignments
    FOR SELECT TO anon, authenticated USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '')
    );

REVOKE ALL ON TABLE subject_assignments FROM PUBLIC, anon, authenticated;
GRANT SELECT (school_id, grade, section, subject, is_locked, teacher_name, last_updated_at)
    ON subject_assignments TO anon, authenticated;
GRANT ALL ON TABLE subject_assignments TO service_role;

COMMIT;

-- فحص الناتج المتبقي النظيف للمدرسة:
SELECT count(*) AS clean_records_count FROM subject_assignments WHERE school_id = 'SCH-KAB2-9359';
