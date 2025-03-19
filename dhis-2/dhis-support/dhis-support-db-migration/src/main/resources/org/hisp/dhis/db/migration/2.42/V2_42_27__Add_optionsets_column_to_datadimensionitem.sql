
-- DHIS2-18370 - Visualization API: Support saving and loading "option" in "items"

-- ProgramDataElementOption
alter table datadimensionitem add column if not exists programdataelementoption_programid int8 constraint fk_datadimensionitem_programdataelementoption_programid references program (programid);
alter table datadimensionitem add column if not exists programdataelementoption_dataelementid int8 constraint fk_datadimensionitem_programdataelementoption_dataelementid references dataelement (dataelementid);
alter table datadimensionitem add column if not exists programdataelementoption_optionid int8 constraint fk_datadimensionitem_programdataelementoption_optionid references optionvalue (optionvalueid);


-- ProgramAttributeOption
alter table datadimensionitem add column if not exists programattributeoption_programid int8 constraint fk_datadimensionitem_programattributeoption_programid references program (programid);
alter table datadimensionitem add column if not exists programattributeoption_attributeid int8 constraint fk_datadimensionitem_programattributeoption_attributeid references trackedentityattribute (trackedentityattributeid);
alter table datadimensionitem add column if not exists programattributeoption_optionid int8 constraint fk_datadimensionitem_programattributeoption_optionid references optionvalue (optionvalueid);
