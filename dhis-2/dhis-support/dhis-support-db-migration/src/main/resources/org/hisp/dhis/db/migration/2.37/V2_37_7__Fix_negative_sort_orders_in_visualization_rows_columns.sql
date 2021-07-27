-- Some sort_order columns migrated from charts are set to -1.
-- This script will set them to zero (0).
-- As charts have one dimension for column and one for row, we don't need to handle multiple dimension scenarios.
-- DHIS2-10805
UPDATE visualization_columns SET sort_order = 0 WHERE sort_order = -1;
UPDATE visualization_rows SET sort_order = 0 WHERE sort_order = -1;
