CREATE TABLE login_attempts (
    login_attempt_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    CONSTRAINT fk_login_attempts_user_id
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX idx_login_attempts_user_id ON login_attempts(user_id);
CREATE INDEX idx_login_attempts_email ON login_attempts(email);
CREATE INDEX idx_login_attempts_attempted_at ON login_attempts(attempted_at);