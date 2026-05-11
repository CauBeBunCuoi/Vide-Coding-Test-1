-- tasks
CREATE INDEX idx_tasks_project_status   ON tasks (project_id, status);
CREATE INDEX idx_tasks_project_assignee ON tasks (project_id, assignee_id);
CREATE INDEX idx_tasks_project_priority ON tasks (project_id, priority);
CREATE INDEX idx_tasks_project_deadline ON tasks (project_id, deadline);
CREATE INDEX idx_tasks_created_by       ON tasks (created_by);

-- comments
CREATE INDEX idx_comments_task_created ON comments (task_id, created_at);

-- refresh_tokens
CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);

-- login_attempts
CREATE INDEX idx_login_attempts_email_time ON login_attempts (email, attempted_at);
