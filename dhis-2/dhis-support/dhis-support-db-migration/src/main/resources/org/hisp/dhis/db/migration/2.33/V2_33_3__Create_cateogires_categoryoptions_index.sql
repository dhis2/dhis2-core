-- Creates covering index for querying categories from category options
CREATE INDEX in_categories_categoryoptions_coid ON categories_categoryoptions(categoryoptionid, categoryid);