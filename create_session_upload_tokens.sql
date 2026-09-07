-- ==============================================================================
-- Session Upload Tokens Table (for secure grade upload PIN management)
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

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

-- Index for fast token lookup
CREATE INDEX IF NOT EXISTS idx_session_tokens_lookup ON session_upload_tokens(
    school_id, 
    grade, 
    section, 
    subject, 
    token_code
);

-- Index for expiration check
CREATE INDEX IF NOT EXISTS idx_session_tokens_expiry ON session_upload_tokens(
    school_id, 
    expires_at
);

-- Cleanup function for expired tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS void AS $$
BEGIN
    DELETE FROM session_upload_tokens 
    WHERE expires_at < NOW() OR is_valid = FALSE;
END;
$$ LANGUAGE plpgsql;

-- Enable RLS for session tokens
ALTER TABLE session_upload_tokens ENABLE ROW LEVEL SECURITY;

-- RLS Policy for session tokens
DROP POLICY IF EXISTS session_tokens_tenant_policy ON session_upload_tokens;
CREATE POLICY session_tokens_tenant_policy ON session_upload_tokens
    FOR ALL USING (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    )
    WITH CHECK (
        school_id = coalesce(current_setting('request.headers', true)::json->>'x-school-id', '')
        OR current_user = 'service_role'
    );

-- Grant appropriate permissions
GRANT SELECT ON session_upload_tokens TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON session_upload_tokens TO anon, authenticated;
