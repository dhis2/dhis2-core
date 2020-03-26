-- Dropping PKEY constraint
ALTER TABLE interpretation_comments DROP CONSTRAINT interpretation_comments_pkey;

-- Adding new PKEY constraint
ALTER TABLE interpretation_comments ADD PRIMARY KEY (interpretationid, interpretationcommentid);

-- Removing unused column: sort_order
ALTER TABLE interpretation_comments DROP COLUMN IF EXISTS sort_order;

-- Removing duplicated comments
DELETE FROM interpretation_comments ic1 USING interpretation_comments ic2
WHERE ic1.interpretationcommentid < ic2.interpretationcommentid
AND ic1.interpretationcommentid = ic2.interpretationcommentid;
