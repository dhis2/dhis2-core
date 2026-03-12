-- DB-level audit table and triggers (feature flag: audit.db.enabled).
-- The trigger function short-circuits when app.dhis2_user is not set, so
-- these objects are zero-overhead unless the feature is active.

-- ---------------------------------------------------------------------------
-- 0. Drop old application-level audit tables (replaced by db_audit)
-- ---------------------------------------------------------------------------

-- Drop trigger from V2_43_17 that wrote to datavalueaudit (table is being dropped below)
DROP TRIGGER IF EXISTS trg_datavalue_audit ON datavalue;
DROP FUNCTION IF EXISTS log_datavalue_audit();

DROP TABLE IF EXISTS audit CASCADE;
DROP TABLE IF EXISTS dataapprovalaudit CASCADE;
DROP TABLE IF EXISTS datavalueaudit CASCADE;
DROP TABLE IF EXISTS programtempownershipaudit CASCADE;
DROP TABLE IF EXISTS trackedentityattributevalueaudit CASCADE;
DROP TABLE IF EXISTS trackedentitydatavalueaudit CASCADE;
DROP TABLE IF EXISTS trackedentityinstanceaudit CASCADE;

-- ---------------------------------------------------------------------------
-- 1. Audit config table
--
-- Stores runtime configuration for the audit system. After deployment, set
-- the archive directory with a plain UPDATE (no special privileges required):
--
--   UPDATE db_audit_config
--   SET    value = '/your/archive/path'
--   WHERE  key   = 'archive_dir';
-- ---------------------------------------------------------------------------

CREATE TABLE db_audit_config (
    key         TEXT PRIMARY KEY,
    value       TEXT NOT NULL,
    description TEXT
);

INSERT INTO db_audit_config (key, value, description) VALUES
    ('archive_dir', '', 'Filesystem path where daily CSV archives are written. '
                        'Must be writable by the postgres OS user. '
                        'Set before running archive_db_audit().');

-- ---------------------------------------------------------------------------
-- 3. Audit table (partitioned by changed_at for future monthly partitions)
-- ---------------------------------------------------------------------------

CREATE TABLE db_audit (
    db_audit_id  UUID        NOT NULL DEFAULT gen_random_uuid(),
    table_name   TEXT        NOT NULL,
    operation    TEXT        NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    record_uid   TEXT,                    -- NULL for tables with no uid column (e.g. datavalue)
    record_pk    TEXT        NOT NULL,    -- PK as JSON object, e.g. {"eventid":42} or {"dataelementid":1,"periodid":2,...}
    old_data     JSONB,                   -- NULL for INSERT
    new_data     JSONB,                   -- NULL for DELETE
    changed_by   TEXT        NOT NULL DEFAULT 'unknown',
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- changed_at in PK is required by PostgreSQL for range-partitioned tables
    PRIMARY KEY (db_audit_id, changed_at)
) PARTITION BY RANGE (changed_at);

-- Catch-all default partition; monthly partitions can be added at any time
-- without any schema change to the parent table.
CREATE TABLE db_audit_default PARTITION OF db_audit DEFAULT;

CREATE INDEX idx_db_audit_table_name ON db_audit_default (table_name);
CREATE INDEX idx_db_audit_changed_at  ON db_audit_default (changed_at);
CREATE INDEX idx_db_audit_changed_by  ON db_audit_default (changed_by);
CREATE INDEX idx_db_audit_record_uid  ON db_audit_default (record_uid)
    WHERE record_uid IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 4. Generic trigger function
--
-- Calling convention (trigger arguments):
--   TG_ARGV[0 .. TG_NARGS-2]  = PK column names (1 for simple PKs, N for composite)
--   TG_ARGV[TG_NARGS-1]       = UID column name (always 'uid'; NULL stored when absent)
--
-- record_pk is always stored as a JSON object for consistency, e.g.:
--   single:    {"eventid": "42"}
--   composite: {"dataelementid": "1", "periodid": "7", ...}
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION dhis2_db_audit_fn()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_user     TEXT;
    v_row_json JSONB;
    v_pk_json  JSONB := '{}';
    v_uid_val  TEXT;
    v_old_data JSONB;
    v_new_data JSONB;
    i          INT;
BEGIN
    -- Short-circuit: COALESCE handles NULL (variable never set in this session,
    -- e.g. StatelessSession writes) as well as '' (set to empty string).
    IF COALESCE(current_setting('app.dhis2_user', true), '') = '' THEN
        RETURN NULL;
    END IF;

    v_user := current_setting('app.dhis2_user', true);

    IF TG_OP = 'DELETE' THEN
        v_row_json := to_jsonb(OLD);
        v_old_data := v_row_json;
        v_new_data := NULL;
    ELSIF TG_OP = 'INSERT' THEN
        v_row_json := to_jsonb(NEW);
        v_old_data := NULL;
        v_new_data := v_row_json;
    ELSE -- UPDATE
        v_old_data := to_jsonb(OLD);
        v_new_data := to_jsonb(NEW);
        v_row_json := v_new_data;
    END IF;

    -- Build PK JSON object from all args except the last (which is the UID column).
    FOR i IN 0 .. TG_NARGS - 2 LOOP
        v_pk_json := v_pk_json || jsonb_build_object(TG_ARGV[i], v_row_json ->> TG_ARGV[i]);
    END LOOP;

    -- Last arg is the UID column name; NULL stored when the column is absent.
    v_uid_val := v_row_json ->> TG_ARGV[TG_NARGS - 1];

    -- The INSERT is wrapped in an EXCEPTION block so that audit failures never
    -- abort the originating business transaction. PostgreSQL implements this as
    -- an internal savepoint: on failure the savepoint rolls back (the audit row
    -- is lost) but the outer transaction continues normally. The overhead of the
    -- savepoint is incurred on every trigger call, but is vastly preferable to
    -- letting an audit write kill a user-facing operation.
    BEGIN
        INSERT INTO db_audit
            (table_name, operation, record_uid, record_pk, old_data, new_data, changed_by, changed_at)
        VALUES
            (TG_TABLE_NAME, TG_OP, v_uid_val, v_pk_json::TEXT, v_old_data, v_new_data, v_user, NOW());
    EXCEPTION WHEN OTHERS THEN
        RAISE WARNING 'db_audit trigger failed on table=% op=%: %', TG_TABLE_NAME, TG_OP, SQLERRM;
    END;

    RETURN NULL; -- AFTER trigger; return value is ignored
END;
$$;

-- ---------------------------------------------------------------------------
-- 5. Bind triggers to all auditable tables
--
-- PK columns are discovered from information_schema at migration time — no
-- naming convention assumed. Tables that do not exist are silently skipped
-- (handles partial schemas / test environments). Tables with no primary key
-- emit a WARNING and are skipped.
--
-- Trigger arguments passed to dhis2_db_audit_fn:
--   pk_col1 [, pk_col2 ...], 'uid'
-- ---------------------------------------------------------------------------

DO $$
DECLARE
    t        TEXT;
    pk_cols  TEXT[];
    all_args TEXT;
    tables   TEXT[] := ARRAY[
        -- Tracker data
        'trackedentity', 'enrollment', 'trackerevent', 'singleevent',
        'trackedentityattributevalue',
        -- Aggregate data
        'datavalue',
        -- Organisation structure
        'organisationunit', 'orgunitgroup', 'orgunitgroupset', 'orgunitlevel',
        -- Data elements & categories
        'dataelement', 'dataelementgroup', 'dataelementgroupset',
        'category', 'categoryoption', 'categorycombo', 'categoryoptioncombo',
        'categoryoptiongroupset', 'categoryoptiongroup',
        -- Data sets
        'dataset', 'datasetnotificationtemplate', 'section',
        -- Indicators
        'indicator', 'indicatorgroup', 'indicatorgroupset', 'indicatortype',
        -- Programs & tracker metadata
        'program', 'programstage', 'programstageelement',
        'programstagedataelement', 'programstagesection',
        'trackedentitytype', 'trackedentityattribute',
        'programrule', 'programruleaction', 'programrulevariable',
        -- Users & security
        'userinfo', 'userrole', 'usergroup',
        -- Maps & visualisations
        'map', 'mapview', 'chart', 'reporttable', 'visualization', 'eventvisualization',
        'dashboard', 'dashboarditem',
        -- Other metadata
        'constant', 'attribute', 'optionset', 'option', 'optiongroup', 'optiongroupset',
        'predictor', 'predictorgroup',
        'validationrule', 'validationrulegroup', 'validationnotificationtemplate',
        'messageconversation',
        'document', 'interpretation', 'report', 'sqlview', 'externalfilter',
        'dataentryform', 'notificationtemplate',
        'legend', 'legendset',
        'pushanalysis', 'programnotificationtemplate',
        'apitoken', 'oauth2client'
        -- jobconfiguration intentionally excluded: updated heavily by the scheduler
        -- via StatelessSession (no authenticated user context; not a meaningful audit target)
    ];
BEGIN
    FOREACH t IN ARRAY tables LOOP
        -- Skip tables that don't exist in this schema
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = t
        ) THEN CONTINUE; END IF;

        -- Discover the actual PK columns from the system catalog
        SELECT array_agg(kcu.column_name::TEXT ORDER BY kcu.ordinal_position)
        INTO pk_cols
        FROM information_schema.key_column_usage  kcu
        JOIN information_schema.table_constraints tc
          ON  tc.constraint_name = kcu.constraint_name
          AND tc.table_schema    = kcu.table_schema
          AND tc.table_name      = kcu.table_name
        WHERE tc.constraint_type = 'PRIMARY KEY'
          AND kcu.table_schema   = 'public'
          AND kcu.table_name     = t;

        IF pk_cols IS NULL THEN
            RAISE WARNING 'Table % has no primary key — audit trigger skipped', t;
            CONTINUE;
        END IF;

        -- Build trigger argument list: pk_col1, pk_col2, ..., 'uid'
        -- (last arg is always the UID column name; function returns NULL when absent)
        SELECT string_agg(quote_literal(col), ', ')
        INTO all_args
        FROM unnest(array_cat(pk_cols, ARRAY['uid']::TEXT[])) AS col;

        EXECUTE format(
            'CREATE TRIGGER db_audit_%1$s
             AFTER INSERT OR UPDATE OR DELETE ON %1$I
             FOR EACH ROW EXECUTE FUNCTION dhis2_db_audit_fn(%2$s)',
            t,
            all_args
        );
    END LOOP;
END;
$$;

-- ---------------------------------------------------------------------------
-- 6. Archive function
--
-- Archives all db_audit rows older than today to daily CSV files, then
-- deletes the archived rows. Missed runs are caught up automatically —
-- each call processes every outstanding day, not just yesterday.
--
-- Archive dir is read from db_audit_config at runtime. Set it with:
--   UPDATE db_audit_config SET value = '/your/path' WHERE key = 'archive_dir';
--
-- Can be triggered manually at any time:
--   SELECT archive_db_audit();
--
-- NOTE: COPY TO file requires the calling user to hold the
-- pg_write_server_files role. A superuser must run this once:
--   GRANT pg_write_server_files TO <dhis2_db_user>;
-- After that grant the function works for all callers including pg_cron.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION archive_db_audit()
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    archive_dir  TEXT;
    archive_day  DATE;
    archive_file TEXT;
    deleted_rows BIGINT;
BEGIN
    SELECT value INTO archive_dir
    FROM   db_audit_config
    WHERE  key = 'archive_dir';

    IF archive_dir IS NULL OR archive_dir = '' THEN
        RAISE EXCEPTION
            'audit archive directory is not configured. '
            'Run: UPDATE db_audit_config SET value = ''/your/archive/path'' WHERE key = ''archive_dir'';';
    END IF;

    FOR archive_day IN
        SELECT DISTINCT changed_at::DATE AS day
        FROM   db_audit
        WHERE  changed_at < CURRENT_DATE
        ORDER  BY day ASC
    LOOP
        archive_file := archive_dir
                     || '/db_audit_'
                     || to_char(archive_day, 'YYYY_MM_DD')
                     || '.csv';

        EXECUTE format(
            'COPY (SELECT * FROM db_audit WHERE changed_at::DATE = %L) TO %L WITH CSV HEADER',
            archive_day,
            archive_file
        );

        DELETE FROM db_audit WHERE changed_at::DATE = archive_day;
        GET DIAGNOSTICS deleted_rows = ROW_COUNT;

        RAISE NOTICE 'db_audit: archived % rows for % → %',
            deleted_rows, archive_day, archive_file;
    END LOOP;

    IF NOT FOUND THEN
        RAISE NOTICE 'db_audit: nothing to archive (no rows older than today)';
    END IF;
END;
$$;

-- ---------------------------------------------------------------------------
-- 7. Schedule via pg_cron (daily at 12:00)
--
-- pg_cron must be installed and listed in shared_preload_libraries.
-- The DO block below registers the job only when pg_cron is available,
-- so the migration does not fail on environments without the extension.
-- ---------------------------------------------------------------------------

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'pg_cron') THEN
        -- schedule_in_database ensures the job runs against the current database
        -- (e.g. 'dhis2') rather than the pg_cron default ('postgres'), regardless
        -- of how the instance is named.
        PERFORM cron.schedule_in_database(
            'archive-db-audit',   -- job name (idempotent: upserts if already exists)
            '0 12 * * *',         -- every day at 12:00
            'SELECT archive_db_audit()',
            current_database()    -- resolve DB name at migration time
        );
        RAISE NOTICE 'pg_cron job ''archive-db-audit'' scheduled (daily 12:00) in database %', current_database();
    ELSE
        RAISE NOTICE 'pg_cron not available — run SELECT archive_db_audit() manually or install pg_cron';
    END IF;
END;
$$;
