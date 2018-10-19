-- TrackedEntityAttribute shortName not null constraint
ALTER TABLE trackedentityattribute ALTER COLUMN shortname SET NOT NULL;

-- FIELD MASK
ALTER TABLE dataelement
ADD COLUMN IF NOT EXISTS fieldmask character varying(255);

ALTER TABLE trackedentityattribute
ADD COLUMN IF NOT EXISTS fieldmask character varying(255);


--FEATURE TYPE and GEOMETRY
ALTER TABLE program
ADD COLUMN IF NOT EXISTS featuretype character varying(255),
ADD COLUMN IF NOT EXISTS capturecoordinates boolean;

ALTER TABLE trackedentityinstance
ADD COLUMN IF NOT EXISTS geometry geometry;


ALTER TABLE trackedentitytype
ADD COLUMN IF NOT EXISTS featuretype character varying(255);


ALTER TABLE programinstance
ADD COLUMN IF NOT EXISTS geometry geometry;


--VALIDATION STRATEGY for programstage

ALTER TABLE programstage
ADD COLUMN IF NOT EXISTS validationstrategy character varying(32);



--USER INFO CHANGES

-- Add social media columns to userinfo
ALTER TABLE userinfo
ADD COLUMN IF NOT EXISTS whatsapp character varying(255),
ADD COLUMN IF NOT EXISTS skype character varying(255),
ADD COLUMN IF NOT EXISTS facebookmessenger character varying(255),
ADD COLUMN IF NOT EXISTS telegram character varying(255),
ADD COLUMN IF NOT EXISTS twitter character varying(255),
ADD COLUMN IF NOT EXISTS avatar integer;

--Foreign key reference avatar into fileresource table
ALTER TABLE userinfo 
DROP CONSTRAINT IF EXISTS fk_user_fileresourceid;

ALTER TABLE userinfo
ADD CONSTRAINT fk_user_fileresourceid FOREIGN KEY (avatar) REFERENCES fileresource (fileresourceid);


--MESSAGE ATTACHMENTS

--Create mapping table messageattachments
CREATE TABLE IF NOT EXISTS messageattachments (
    messageid integer NOT NULL,
    fileresourceid integer NOT NULL
);


--Droping existing foreign key constraints in messageattachments
ALTER TABLE messageattachments 
DROP CONSTRAINT IF EXISTS messageattachments_pkey,
DROP CONSTRAINT IF EXISTS fk_messageattachments_fileresourceid;

--Adding foreign key constraints for messageattachments
ALTER TABLE messageattachments
ADD CONSTRAINT messageattachments_pkey PRIMARY KEY (messageid, fileresourceid),
ADD CONSTRAINT fk_messageattachments_fileresourceid FOREIGN KEY (fileresourceid) REFERENCES fileresource(fileresourceid);