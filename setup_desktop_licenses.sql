-- ==============================================================================
-- Schema for Desktop App Licenses & Distribution
-- The Principal v6.0 Super Edition - Lifetime Activation Architecture
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Desktop Licenses Table (One-Time Lifetime Product Keys)
CREATE TABLE IF NOT EXISTS public.desktop_licenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    license_key TEXT UNIQUE NOT NULL, -- e.g. "BOSS-L1-8492-3310-7B" or "BOSS-8492-3321-4491"
    product_code TEXT NOT NULL DEFAULT 'BOSS', -- e.g. "BOSS", "ACCT", "ATND"
    license_tier TEXT NOT NULL DEFAULT 'L1', -- "L1" (Lifetime 1 Device)
    school_name TEXT NOT NULL,
    contact_phone TEXT,
    contact_email TEXT,
    machine_fingerprint TEXT, -- Bound to the PC on first activation
    is_activated BOOLEAN NOT NULL DEFAULT false,
    activated_at TIMESTAMPTZ,
    notes TEXT, -- Payment details (ZainCash, Qi Card, etc.)
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Desktop & Mobile Releases Table (Dynamic Download URLs)
CREATE TABLE IF NOT EXISTS public.app_releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version TEXT NOT NULL DEFAULT 'v6.0',
    desktop_setup_url TEXT NOT NULL,
    desktop_portable_url TEXT,
    android_apk_url TEXT,
    file_size TEXT DEFAULT '210 MB',
    release_notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Initial Release Seed (Default Links)
INSERT INTO public.app_releases (version, desktop_setup_url, desktop_portable_url, android_apk_url, file_size, release_notes)
VALUES (
    'v6.0',
    'https://drive.google.com/file/d/1MyvouuykZmwDx7Lm5hj2qNVIIDbZlqFj/view?usp=sharing',
    'https://drive.google.com/file/d/1MyvouuykZmwDx7Lm5hj2qNVIIDbZlqFj/view?usp=sharing',
    'https://github.com/vcol42-wq/school-data/releases/download/v6.0/The_School_System_Unified_v6.0.apk',
    '139 MB',
    'النسخة الرسمية الشاملة لمنظومة The Principal v6.0 Super Edition - رابط Google Drive المعتمد بالتحديثات الجديدة'
)
ON CONFLICT DO NOTHING;

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.desktop_licenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_releases ENABLE ROW LEVEL SECURITY;

-- 5. RLS Policies:
-- Allow anyone to read active release links
DROP POLICY IF EXISTS "Public can view active releases" ON public.app_releases;
CREATE POLICY "Public can view active releases"
ON public.app_releases FOR SELECT
USING (is_active = true);

-- Allow public update of releases (if needed for link refresh)
DROP POLICY IF EXISTS "Allow release links update" ON public.app_releases;
CREATE POLICY "Allow release links update"
ON public.app_releases FOR ALL
USING (true)
WITH CHECK (true);

-- Allow reading license status by license_key
DROP POLICY IF EXISTS "Anyone can check license validity" ON public.desktop_licenses;
CREATE POLICY "Anyone can check license validity"
ON public.desktop_licenses FOR SELECT
USING (true);

-- Allow activating an unactivated license
DROP POLICY IF EXISTS "Allow activation of unused license" ON public.desktop_licenses;
CREATE POLICY "Allow activation of unused license"
ON public.desktop_licenses FOR UPDATE
USING (is_activated = false OR machine_fingerprint IS NOT NULL)
WITH CHECK (true);

-- Allow insert by anon/authenticated (for license generation)
DROP POLICY IF EXISTS "Allow license generation" ON public.desktop_licenses;
CREATE POLICY "Allow license generation"
ON public.desktop_licenses FOR INSERT
WITH CHECK (true);

-- 6. Grant Permissions to API Roles (Crucial for PostgREST & Anon Key)
GRANT ALL ON public.desktop_licenses TO anon, authenticated, service_role;
GRANT ALL ON public.app_releases TO anon, authenticated, service_role;

-- 7. Reload Schema Cache for PostgREST API
NOTIFY pgrst, 'reload schema';


