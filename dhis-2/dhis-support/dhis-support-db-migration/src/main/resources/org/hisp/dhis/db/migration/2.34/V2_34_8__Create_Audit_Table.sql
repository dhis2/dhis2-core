CREATE TABLE audit (
    auditId SERIAL PRIMARY KEY,
    auditType TEXT NOT NULL,
    auditScope TEXT NOT NULL,
    createdAt TIMESTAMP NOT NULL,
    createdBy TEXT NOT NULL,
    klass TEXT,
    uid TEXT,
    code TEXT,
    data BYTEA
);