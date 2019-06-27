
-- Add column programstageid to trackedentitydataelementdimension

alter table trackedentitydataelementdimension 
add column programstageid int8;

-- Add foreign key to program stage

alter table trackedentitydataelementdimension 
add constraint fk_tedataelementdimension_programstageid 
foreign key (programstageid) references programstage(programstageid);
