CREATE TABLE potentialduplicate (
  potentialduplicateid BIGINT NOT NULL PRIMARY KEY,
  uid CHARACTER VARYING(11) NOT NULL UNIQUE,
  created TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  lastUpdated TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  lastUpdatedBy BIGINT NOT NULL,
  teiA CHARACTER VARYING(11) NOT NULL,
  teiB CHARACTER VARYING(11),
  status CHARACTER VARYING(255) NOT NULL
);

ALTER TABLE potentialduplicate
ADD CONSTRAINT potentialduplicate_teia_teib UNIQUE (teiA, teiB);

ALTER TABLE potentialduplicate
ADD CONSTRAINT potentialduplicate_lastupdatedby_user FOREIGN KEY (lastUpdatedBy) REFERENCES users(userid);