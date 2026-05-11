-- Dev seed data. All passwords = Test1234! (BCrypt)
INSERT INTO users (username, email, password) VALUES
    ('alex_lead',    'alex@example.com',   '$2a$12$Placeholder.BCryptHashForTest1234x'),
    ('sam_dev',      'sam@example.com',    '$2a$12$Placeholder.BCryptHashForTest1234x'),
    ('jordan_free',  'jordan@example.com', '$2a$12$Placeholder.BCryptHashForTest1234x');

INSERT INTO projects (name, description, owner_id) VALUES
    ('TaskFlow Demo', 'Sample project for development', 1);

INSERT INTO project_members (project_id, user_id, role) VALUES
    (1, 1, 'OWNER'),
    (1, 2, 'MEMBER');
