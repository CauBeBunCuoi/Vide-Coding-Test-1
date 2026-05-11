CREATE TABLE comments (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    content    TEXT      NOT NULL,
    task_id    BIGINT    NOT NULL REFERENCES tasks(id)  ON DELETE CASCADE,
    author_id  BIGINT    NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
