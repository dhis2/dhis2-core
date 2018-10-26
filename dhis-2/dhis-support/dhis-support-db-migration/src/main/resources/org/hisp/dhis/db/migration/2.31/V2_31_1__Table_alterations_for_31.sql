--Function for creating uids. 
create or replace function generate_uid()  returns text as
$$
declare
chars  text [] := '{0,1,2,3,4,5,6,7,8,9,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z}';
 result text := chars [11 + random() * (array_length(chars, 1) - 11)];
begin
 for i in 1..10 loop
 result := result || chars [1 + random() * (array_length(chars, 1) - 1)];
 end loop;
return result;
end;
$$
language plpgsql;



-- TrackedEntityAttribute shortName not null constraint
UPDATE trackedentityattribute SET shortname = name WHERE shortname IS NULL;
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

UPDATE trackedentitytype SET featuretype = 'NONE' where featuretype is null;

UPDATE program SET featuretype = 'POINT' WHERE capturecoordinates = true AND featuretype IS NULL;
UPDATE program SET featuretype = 'NONE' WHERE capturecoordinates = false AND featuretype IS NULL;

UPDATE programinstance SET geometry = ST_GeomFromText('POINT(' || longitude || ' ' || latitude || ')', 4326) WHERE longitude IS NOT NULL AND latitude IS NOT NULL AND geometry IS NULL;

ALTER TABLE programinstance DROP COLUMN IF EXISTS latitude;
ALTER TABLE programinstance DROP COLUMN IF EXISTS longitude;
  

 

--VALIDATION STRATEGY for programstage

ALTER TABLE programstage
ADD COLUMN IF NOT EXISTS validationstrategy character varying(32);


 --New enum column was added into ProgramStage. fill default values and make it NOT NULL
UPDATE programstage SET validationstrategy = 'NONE' WHERE validcompleteonly = false;
UPDATE programstage SET validationstrategy = 'ON_COMPLETE' WHERE validcompleteonly = true;
ALTER TABLE programstage ALTER COLUMN validationstrategy SET NOT NULL;
ALTER TABLE programstage DROP COLUMN IF EXISTS validation;
UPDATE programstage SET validationstrategy = 'ON_COMPLETE' WHERE validationstrategy = 'NONE';


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

--Corrections in case of column type mismatch from demodb and hbm
ALTER TABLE programnotificationtemplate
ALTER COLUMN messagetemplate TYPE text;

ALTER TABLE organisationunit
ALTER COLUMN openingdate TYPE date,
ALTER COLUMN closeddate TYPE date;