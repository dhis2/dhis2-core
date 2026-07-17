-- Dual generation stamps for UserDetails soft-refresh (role/user authz versioning).
CREATE TABLE IF NOT EXISTS authz_version (
  scope VARCHAR(16) NOT NULL,
  key_name VARCHAR(255) NOT NULL,
  gen BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (scope, key_name)
);
