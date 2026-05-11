CREATE TABLE login_attempts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email        VARCHAR(100) NOT NULL,
    attempted_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    success      BOOLEAN      NOT NULL
);
