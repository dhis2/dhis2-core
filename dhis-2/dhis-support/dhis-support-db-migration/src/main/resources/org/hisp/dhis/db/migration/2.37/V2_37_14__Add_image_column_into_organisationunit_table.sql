ALTER TABLE organisationunit
  ADD COLUMN IF NOT EXISTS image bigint;

ALTER TABLE organisationunit
  ADD CONSTRAINT fk_organisationUnit_fileresourceid FOREIGN KEY (image) REFERENCES fileresource(fileresourceid);
