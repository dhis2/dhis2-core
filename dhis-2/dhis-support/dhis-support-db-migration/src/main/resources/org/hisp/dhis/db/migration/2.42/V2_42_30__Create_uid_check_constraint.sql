CREATE DOMAIN uid_type AS VARCHAR(11)
    CHECK (
        VALUE ~ '^[A-Za-z]' AND    -- First character must be a letter
        VALUE ~ '^[A-Za-z0-9]{11}$' -- All 11 characters must be alphanumeric
        );

Alter table potentialduplicate alter column original set DATA TYPE uid_type;
Alter table potentialduplicate alter column duplicate set DATA TYPE uid_type;