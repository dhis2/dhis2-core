/* Add new columns */
ALTER TABLE relationship
    ADD COLUMN IF NOT EXISTS key VARCHAR;
ALTER TABLE relationship
    ADD COLUMN IF NOT EXISTS inverted_key VARCHAR;

/* Populating the new columns, where either column are NULL */
UPDATE relationship R
SET key          = SQ.key,
    inverted_key = SQ.inverted_key
FROM (SELECT R.relationshipid,
             CONCAT(RT.uid, '_', FRI.uid, '_', TRI.uid) as key,
             CONCAT(RT.uid, '_', TRI.uid, '_', FRI.uid) as inverted_key
      FROM relationship R
               INNER JOIN (
          SELECT F.relationshipitemid, COALESCE(TEI.uid, PI.uid, PSI.uid) as uid
          FROM relationshipitem F
                   LEFT JOIN trackedentityinstance TEI ON TEI.trackedentityinstanceid = F.trackedentityinstanceid
                   LEFT JOIN programinstance PI ON PI.programinstanceid = F.programinstanceid
                   LEFT JOIN programstageinstance PSI ON PSI.programstageinstanceid = F.programstageinstanceid
      ) FRI ON FRI.relationshipitemid = R.from_relationshipitemid
               INNER JOIN (
          SELECT T.relationshipitemid, COALESCE(TEI.uid, PI.uid, PSI.uid) as uid
          FROM relationshipitem T
                   LEFT JOIN trackedentityinstance TEI ON TEI.trackedentityinstanceid = T.trackedentityinstanceid
                   LEFT JOIN programinstance PI ON PI.programinstanceid = T.programinstanceid
                   LEFT JOIN programstageinstance PSI ON PSI.programstageinstanceid = T.programstageinstanceid
      ) TRI ON TRI.relationshipitemid = R.to_relationshipitemid
               INNER JOIN relationshiptype RT ON RT.relationshiptypeid = R.relationshiptypeid) SQ
WHERE R.relationshipid = SQ.relationshipid
  AND (R.key IS NULL
    OR R.inverted_key IS NULL);

/* Add NOT NULL constraints to the columns */
ALTER TABLE relationship
    ALTER COLUMN key
        SET NOT NULL;
ALTER TABLE relationship
    ALTER COLUMN inverted_key
        SET NOT NULL;

/* Add normal btree indexes to the new columns to facilitate quick lookups */
CREATE INDEX in_relationship_key ON relationship (key);
CREATE INDEX in_relationship_inverted_key ON relationship (inverted_key);