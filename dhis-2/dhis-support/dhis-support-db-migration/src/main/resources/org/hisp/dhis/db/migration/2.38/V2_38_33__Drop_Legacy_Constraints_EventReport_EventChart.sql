-- This script relates to the issue https://jira.dhis2.org/browse/DHIS2-12647
-- Removes all foreign keys/constraints from legacy (unused) tables related to "event chart" and "event report".
-- This is causing collection issues (during updated) on the new set of tables for "event visualization".

-- 1) Removing all FKs from all "eventchart*" tables
DO
$$
    DECLARE
        t      text;
        tables text[] := array ['eventchart','eventchart_attributedimensions','eventchart_categorydimensions',
            'eventchart_categoryoptiongroupsetdimensions','eventchart_columns','eventchart_dataelementdimensions',
            'eventchart_filters','eventchart_itemorgunitgroups','eventchart_organisationunits',
            'eventchart_orgunitgroupsetdimensions','eventchart_orgunitlevels','eventchart_periods',
            'eventchart_programindicatordimensions','eventchart_rows'];
        r      record;
    BEGIN
        FOREACH t IN ARRAY tables
            LOOP
                FOR r IN (
                    SELECT constraint_name
                    FROM information_schema.table_constraints
                    WHERE table_name = t
                      AND constraint_type = 'FOREIGN KEY'
                )
                    LOOP
                        RAISE INFO '%','dropping ' || r.constraint_name || 'FROM TABLE ' || t;
                        EXECUTE CONCAT('ALTER TABLE IF EXISTS ' || t || ' DROP CONSTRAINT IF EXISTS ' || r.constraint_name);
                    END LOOP;
            END LOOP;
    END;
$$ LANGUAGE plpgsql;


-- 2) Removing all FKs from all "eventreport*" tables
DO
$$
    DECLARE
        t      text;
        tables text[] := array ['eventreport','eventreport_attributedimensions','eventreport_categorydimensions',
            'eventreport_categoryoptiongroupsetdimensions','eventreport_columns','eventreport_dataelementdimensions',
            'eventreport_filters','eventreport_itemorgunitgroups','eventreport_organisationunits',
            'eventreport_orgunitgroupsetdimensions','eventreport_orgunitlevels','eventreport_periods',
            'eventreport_programindicatordimensions','eventreport_rows'];
        r      record;
    BEGIN
        FOREACH t IN ARRAY tables
            LOOP
                FOR r IN (
                    SELECT constraint_name
                    FROM information_schema.table_constraints
                    WHERE table_name = t
                      AND constraint_type = 'FOREIGN KEY'
                )
                    LOOP
                        RAISE INFO '%','dropping ' || r.constraint_name || 'FROM TABLE ' || t;
                        EXECUTE CONCAT('ALTER TABLE IF EXISTS ' || t || ' DROP CONSTRAINT IF EXISTS ' || r.constraint_name);
                    END LOOP;
            END LOOP;
    END;
$$ LANGUAGE plpgsql;
