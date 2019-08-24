ALTER TABLE programstage
ADD COLUMN nextscheduledateid BIGINT;

ALTER TABLE programstage
ADD CONSTRAINT fk_programstage_nextscheduledateid FOREIGN KEY (nextscheduledateid)
REFERENCES dataelement(dataelementid);