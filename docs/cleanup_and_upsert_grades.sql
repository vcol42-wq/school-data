-- cleanup_and_upsert_grades.sql
-- Migration: normalize grades by month, remove duplicates, add unique index, create view and upsert example

BEGIN;

-- 1) Ensure month column exists and populate it from created_at
ALTER TABLE IF EXISTS grades ADD COLUMN IF NOT EXISTS month date;
UPDATE grades SET month = date_trunc('month', created_at)::date WHERE month IS NULL;

-- 2) Remove duplicate entries keeping the latest created_at per (student_id, subject_id, month)
-- Assumes grades.id is primary key (uuid/int). If not present, switch to ctid-based approach.
WITH ranked AS (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY student_id, subject_id, month ORDER BY created_at DESC) rn
  FROM grades
  WHERE student_id IS NOT NULL AND subject_id IS NOT NULL AND month IS NOT NULL
)
DELETE FROM grades g
USING ranked r
WHERE g.id = r.id AND r.rn > 1;

-- 3) Create unique index to prevent future duplicates per month
CREATE UNIQUE INDEX IF NOT EXISTS uniq_monthly_grade ON grades(student_id, subject_id, month);

-- 4) Optional: Add audit table for grade changes
CREATE TABLE IF NOT EXISTS grades_audit (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  grade_id uuid,
  student_id uuid,
  subject_id uuid,
  month date,
  old_grade numeric,
  new_grade numeric,
  uploader_id uuid,
  action text,
  created_at timestamptz DEFAULT now(),
  payload jsonb
);

-- 5) Create a view that shows the last recorded grade per student/subject/month
CREATE OR REPLACE VIEW last_grade_per_month AS
SELECT student_id, subject_id, month, grade, created_at, uploader_id
FROM (
  SELECT *, ROW_NUMBER() OVER (PARTITION BY student_id, subject_id, month ORDER BY created_at DESC) rn
  FROM grades
) t WHERE rn = 1;

COMMIT;

-- 6) UPSERT example (use from application side or expose as SQL function)
-- INSERT INTO grades(student_id, subject_id, month, grade, created_at, uploader_id)
-- VALUES ('<student_uuid>','<subject_uuid>', date_trunc('month', now())::date, 85, now(), '<uploader_uuid>')
-- ON CONFLICT (student_id, subject_id, month)
-- DO UPDATE SET grade = EXCLUDED.grade, created_at = EXCLUDED.created_at, uploader_id = EXCLUDED.uploader_id;

-- 7) Notes:
-- - Test on a snapshot before running on production. Backup the grades table.
-- - If grades.id does not exist, replace the deletion step with a ctid-based approach.
-- - Consider adding triggers to populate month automatically on INSERT.
-- - Consider enforcing NOT NULL for student_id, subject_id depending on your schema.
