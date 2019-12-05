CREATE TABLE audit (
    auditId SERIAL PRIMARY KEY,
    auditType TEXT,
    auditScope TEXT,
    createdAt TIMESTAMP,
    createdBy TEXT,
    klass TEXT,
    uid TEXT,
    code TEXT,
    data TEXT
);