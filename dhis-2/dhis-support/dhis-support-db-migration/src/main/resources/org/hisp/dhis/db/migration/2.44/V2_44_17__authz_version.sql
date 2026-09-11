-- Generation stamps for UserDetails soft-refresh: per-user ('user', <uid>), per-role
-- ('role', <uid>), and the global change epoch ('epoch', 'epoch').
CREATE TABLE IF NOT EXISTS authz_version (
  scope VARCHAR(16) NOT NULL,
  key_name VARCHAR(255) NOT NULL,
  gen BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  PRIMARY KEY (scope, key_name)
);
