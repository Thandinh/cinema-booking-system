CREATE TABLE IF NOT EXISTS auth_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    username VARCHAR(255),
    event_type VARCHAR(80) NOT NULL,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(1000),
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS update_auth_audit_logs_modtime ON auth_audit_logs;
CREATE TRIGGER update_auth_audit_logs_modtime
    BEFORE UPDATE ON auth_audit_logs
    FOR EACH ROW
    EXECUTE PROCEDURE update_modified_column();

CREATE INDEX IF NOT EXISTS idx_auth_audit_logs_created_at
    ON auth_audit_logs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_auth_audit_logs_user_created_at
    ON auth_audit_logs(user_id, created_at DESC)
    WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auth_audit_logs_event_created_at
    ON auth_audit_logs(event_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_auth_audit_logs_success_created_at
    ON auth_audit_logs(success, created_at DESC);
