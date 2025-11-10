INSERT INTO query (id, text) VALUES
 (1, 'SELECT 1 AS result'),
 (2, 'SELECT 2 AS result'),
 (3, 'SELEC BAD SQL');           -- this one will fail

INSERT INTO query_job (id, query_id, status, error) VALUES
 (10, 1, 'RUNNING', NULL),
 (11, 2, 'QUEUED', NULL),
 (12, 3, 'QUEUED', NULL);