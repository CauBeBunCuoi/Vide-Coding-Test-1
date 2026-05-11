CREATE TABLE project_members (
    project_id BIGINT      NOT NULL REFERENCES projects(id),
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    role       VARCHAR(10) NOT NULL DEFAULT 'MEMBER'
                           CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX idx_pm_user ON project_members (user_id);
