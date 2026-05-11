CREATE TABLE tasks (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'TODO'
                             CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    priority    VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM'
                             CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    deadline    DATE,
    project_id  BIGINT       NOT NULL REFERENCES projects(id),
    assignee_id BIGINT                REFERENCES users(id),
    created_by  BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
