CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token_hash VARCHAR(64) UNIQUE NOT NULL,
    token_id VARCHAR(64) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(80),
    replaced_by_token_id VARCHAR(64),
    user_agent VARCHAR(500),
    ip_address VARCHAR(80),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS update_refresh_tokens_modtime ON refresh_tokens;
CREATE TRIGGER update_refresh_tokens_modtime
    BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW
    EXECUTE PROCEDURE update_modified_column();

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash
    ON refresh_tokens(token_hash);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_expires_at
    ON refresh_tokens(user_id, expires_at);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active_user
    ON refresh_tokens(user_id, revoked_at, expires_at)
    WHERE revoked_at IS NULL;
