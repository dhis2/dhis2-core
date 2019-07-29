-- Creates covering index for querying categories from category options
-- Since the index is required just for a backport the covering part of the index is ordered descendant,
-- which enables an easy exchange in version 2.33.
DROP INDEX IF EXISTS in_categories_categoryoptions_coid_bp;
CREATE INDEX in_categories_categoryoptions_coid_bp ON categories_categoryoptions(categoryoptionid, categoryid DESC);