Testing and deployment instructions for cleanup_and_upsert_grades.sql

1) Backup
- Create a dump of the grades table before running:
  pg_dump --table=grades --data-only --column-inserts -f grades_backup.sql <connection params>

2) Run on staging first
- Apply the SQL on a staging copy of the DB to validate behavior.

3) Run migration
- Connect to Supabase SQL editor and run cleanup_and_upsert_grades.sql

4) Verify results
- Check duplicates:
  SELECT student_id, subject_id, month, COUNT(*) cnt FROM grades GROUP BY student_id, subject_id, month HAVING COUNT(*)>1;

- Check total rows before/after to ensure reasonable deletion of duplicates.

5) Integrate app logic
- Android/Backend should use UPSERT pattern shown in the SQL file to avoid duplicates.

6) UI expectations
- The app should show last_grade_per_month view when listing final grades per month.
- For student detail, query grades WHERE student_id = $1 ORDER BY created_at DESC to show timeline.

7) Rollback
- If anything goes wrong, restore from grades_backup.sql

Notes:
- If your grades table lacks an id primary key, modify the deletion query to use ctid or another unique identifier.
- If you need, I can prepare an id-based migration adding an id column safely.
