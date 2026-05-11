CREATE TABLE labels (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    color      VARCHAR(7)  NOT NULL,
    project_id BIGINT      NOT NULL REFERENCES projects(id)
);

CREATE UNIQUE INDEX idx_labels_project_name ON labels (project_id, LOWER(name));
