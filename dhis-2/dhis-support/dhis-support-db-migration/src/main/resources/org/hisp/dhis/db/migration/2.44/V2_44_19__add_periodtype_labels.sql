ALTER TABLE periodtype ADD COLUMN IF NOT EXISTS translations jsonb NOT NULL DEFAULT '[]';

CREATE TABLE relativeperiod (
  name character varying(50) PRIMARY KEY,
  label character varying(255),
  translations jsonb NOT NULL DEFAULT '[]'
);