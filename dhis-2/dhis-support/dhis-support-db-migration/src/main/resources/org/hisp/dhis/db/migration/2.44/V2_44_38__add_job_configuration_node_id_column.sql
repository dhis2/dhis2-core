-- add UUID column for the node ID running a job
ALTER TABLE jobconfiguration ADD COLUMN IF NOT EXISTS nodeid uuid;

-- index only non-null value rows to keep the index small (running set)
CREATE INDEX idx_jobconfiguration_nodeid ON jobconfiguration (nodeid) WHERE nodeid IS NOT NULL;