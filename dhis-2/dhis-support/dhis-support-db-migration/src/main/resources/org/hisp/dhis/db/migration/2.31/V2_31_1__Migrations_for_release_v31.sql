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
update trackedentityattribute SET shortname = name where shortname is null;
alter table trackedentityattribute alter column shortname SET not null;

-- FIELD MASK
alter table dataelement
add column if not exists fieldmask character varying(255);

alter table trackedentityattribute
add column if not exists fieldmask character varying(255);


--FEATURE TYPE and GEOMETRY
alter table program
add column if not exists featuretype character varying(255),
add column if not exists capturecoordinates boolean;

alter table trackedentityinstance
add column if not exists geometry geometry;


alter table trackedentitytype
add column if not exists featuretype character varying(255);


alter table programinstance
add column if not exists geometry geometry;

update trackedentitytype SET featuretype = 'NONE' where featuretype is null;

update program SET featuretype = 'POINT' where capturecoordinates = true AND featuretype is null;
update program SET featuretype = 'NONE' where capturecoordinates = false AND featuretype is null;

update programinstance SET geometry = ST_GeomFromText('POINT(' || longitude || ' ' || latitude || ')', 4326) where longitude is not null AND latitude is not null AND geometry is null;

alter table programinstance drop column if exists latitude;
alter table programinstance drop column if exists longitude;
  

 

--VALIDATION STRATEGY for programstage

alter table programstage
add column if not exists validationstrategy character varying(32);


 --New enum column was added into ProgramStage. fill default values and make it not null
update programstage SET validationstrategy = 'NONE' where validcompleteonly = false;
update programstage SET validationstrategy = 'ON_COMPLETE' where validcompleteonly = true;
alter table programstage alter column validationstrategy SET not null;
alter table programstage drop column if exists validation;
update programstage SET validationstrategy = 'ON_COMPLETE' where validationstrategy = 'NONE';


--USER INFO CHANGES

-- Add social media columns to userinfo
alter table userinfo
add column if not exists whatsapp character varying(255),
add column if not exists skype character varying(255),
add column if not exists facebookmessenger character varying(255),
add column if not exists telegram character varying(255),
add column if not exists twitter character varying(255),
add column if not exists avatar integer;

--Foreign key reference avatar into fileresource table
alter table userinfo 
drop constraint if exists fk_user_fileresourceid;

alter table userinfo
add constraint fk_user_fileresourceid foreign key (avatar) references fileresource (fileresourceid);


--MESSAGE ATTACHMENTS

--Create mapping table messageattachments
create table if not exists messageattachments (
    messageid integer not null,
    fileresourceid integer not null
);


--Droping existing foreign key constraints in messageattachments
alter table messageattachments 
drop constraint if exists messageattachments_pkey,
drop constraint if exists fk_messageattachments_fileresourceid;

--Adding foreign key constraints for messageattachments
alter table messageattachments
add constraint messageattachments_pkey primary key (messageid, fileresourceid),
add constraint fk_messageattachments_fileresourceid foreign key (fileresourceid) references fileresource(fileresourceid);

--Corrections in case of column type mismatch from demodb and hbm
alter table programnotificationtemplate
alter column messagetemplate TYPE text;

alter table organisationunit
alter column openingdate TYPE date,
alter column closeddate TYPE date;



--Creating tables keyjsonvalueusergroupaccesses and keyjsonvalueuseraccesses if not already created

create table if not exists keyjsonvalueusergroupaccesses (
    keyjsonvalueid integer not null,
    usergroupaccessid integer not null
);

create table if not exists keyjsonvalueuseraccesses (
    keyjsonvalueid integer not null,
    useraccessid integer not null
);


--Droping existing foreign key constraints in keyjsonvalueuseraccesses
alter table keyjsonvalueuseraccesses 
drop constraint if exists keyjsonvalueuseraccesses_pkey,
drop constraint if exists fk_keyjsonvalue_useraccessid;

--Adding foreign key constraints for keyjsonvalueuseraccesses
alter table keyjsonvalueuseraccesses
add constraint keyjsonvalueuseraccesses_pkey primary key (keyjsonvalueid, useraccessid),
add constraint fk_keyjsonvalue_useraccessid foreign key (useraccessid) references useraccess(useraccessid);

--Droping existing foreign key constraints in keyjsonvalueusergroupaccesses
alter table keyjsonvalueusergroupaccesses 
drop constraint if exists keyjsonvalueusergroupaccesses_pkey,
drop constraint if exists fk_keyjsonvalue_usergroupaccessid;

--Adding foreign key constraints for keyjsonvalueusergroupaccesses
alter table keyjsonvalueusergroupaccesses
add constraint keyjsonvalueusergroupaccesses_pkey primary key (keyjsonvalueid, usergroupaccessid),
add constraint fk_keyjsonvalue_usergroupaccessid foreign key (usergroupaccessid) references usergroupaccess(usergroupaccessid);

--Adding columns userid and publicaccess into keyjsonvalue if not already present
alter table keyjsonvalue 
add column if not exists userid integer,
add column if not exists publicaccess character varying(8);

--Droping existing foreign key constraints to make the script idempotent
alter table keyjsonvalue 
drop constraint if exists fk_keyjsonvalue_userid;

--Adding foreign key constraings for keyjsonvalue
alter table keyjsonvalue
add constraint fk_keyjsonvalue_userid foreign key (userid) references userinfo(userinfoid);


--Updating all existing datastore items to have public read write access.
update keyjsonvalue SET publicaccess='rw------' where publicaccess is null;


--Creating tables chart_yearlyseries if not already created
create table if not exists chart_yearlyseries (
    chartid integer not null,
    sort_order integer not null,
    yearlyseries character varying(255)
);

--Droping existing constraints in chart_yearlyseries
alter table chart_yearlyseries 
drop constraint if exists chart_yearlyseries_pkey,
drop constraint if exists fk_yearlyseries_chartid;

--Adding constraints for chart_yearlyseries
alter table chart_yearlyseries
add constraint chart_yearlyseries_pkey primary key (chartid, sort_order),
add constraint fk_yearlyseries_chartid foreign key (chartid) references chart(chartid);


alter table chart 
add column if not exists startdate timestamp,
add column if not exists enddate timestamp;

alter table reporttable 
add column if not exists startdate timestamp,
add column if not exists enddate timestamp;


--Add jsonb value columns to datastore and user datastore
alter table keyjsonvalue add column if not exists jbvalue jsonb;
alter table userkeyjsonvalue add column if not exists jbvalue jsonb;


--Migrate existing values into jsonb column
--not IDEMPOTENT
update keyjsonvalue SET jbvalue = value::jsonb where jbvalue is null;
update userkeyjsonvalue SET jbvalue = value::jsonb where jbvalue is null;


--Delete old value column
alter table keyjsonvalue drop column if exists value;
alter table userkeyjsonvalue drop column if exists value;

--Deleting cors whitelist system setting
delete from systemsetting where name = 'keyCorsWhitelist';

-- Drops validcompleteonly column from programstage table. It is not needed anymore as we have a validationstrategy column instead.
alter table programstage drop column if exists validcompleteonly;


--Object Translations to jsonb

alter table dataelement add column if not exists translations jsonb; 
alter table categorycombo add column if not exists translations jsonb; 
alter table attribute add column if not exists translations jsonb; 
alter table categoryoptioncombo add column if not exists translations jsonb; 
alter table categoryoptiongroupset add column if not exists translations jsonb; 
alter table categoryoptiongroup add column if not exists translations jsonb; 
alter table dataelementcategoryoption add column if not exists translations jsonb; 
alter table chart add column if not exists translations jsonb; 
alter table colorset add column if not exists translations jsonb; 
alter table color add column if not exists translations jsonb; 
alter table constant add column if not exists translations jsonb; 
alter table dashboarditem add column if not exists translations jsonb; 
alter table dashboard add column if not exists translations jsonb; 
alter table dataapprovallevel add column if not exists translations jsonb; 
alter table dataapprovalworkflow add column if not exists translations jsonb; 
alter table dataelementcategory add column if not exists translations jsonb; 
alter table dataelementgroupset add column if not exists translations jsonb; 
alter table dataelementgroup add column if not exists translations jsonb; 
alter table dataentryform add column if not exists translations jsonb; 
alter table dataset add column if not exists translations jsonb; 
alter table document add column if not exists translations jsonb; 
alter table eventchart add column if not exists translations jsonb; 
alter table eventreport add column if not exists translations jsonb; 
alter table indicatorgroupset add column if not exists translations jsonb; 
alter table indicator add column if not exists translations jsonb; 
alter table indicatortype add column if not exists translations jsonb; 
alter table maplegendset add column if not exists translations jsonb; 
alter table maplegend add column if not exists translations jsonb; 
alter table map add column if not exists translations jsonb; 
alter table mapview add column if not exists translations jsonb; 
alter table optiongroupset add column if not exists translations jsonb; 
alter table optiongroup add column if not exists translations jsonb; 
alter table optionset add column if not exists translations jsonb; 
alter table optionvalue add column if not exists translations jsonb; 
alter table organisationunit add column if not exists translations jsonb; 
alter table orgunitgroupset add column if not exists translations jsonb; 
alter table orgunitgroup add column if not exists translations jsonb; 
alter table orgunitlevel add column if not exists translations jsonb; 
alter table predictorgroup add column if not exists translations jsonb; 
alter table programindicatorgroup add column if not exists translations jsonb; 
alter table programindicator add column if not exists translations jsonb; 
alter table programmessage add column if not exists translations jsonb; 
alter table programrule add column if not exists translations jsonb; 
alter table programsection add column if not exists translations jsonb; 
alter table programstagesection add column if not exists translations jsonb; 
alter table programstage add column if not exists translations jsonb; 
alter table program_attribute_group add column if not exists translations jsonb; 
alter table program add column if not exists translations jsonb; 
alter table relationshiptype add column if not exists translations jsonb;
alter table reporttable add column if not exists translations jsonb; 
alter table report add column if not exists translations jsonb; 
alter table trackedentityattribute add column if not exists translations jsonb; 
alter table trackedentitytype add column if not exists translations jsonb; 
alter table usergroup add column if not exists translations jsonb; 
alter table userrole add column if not exists translations jsonb; 
alter table validationrule add column if not exists translations jsonb; 
alter table indicatorgroup add column if not exists translations jsonb; 
alter table validationrulegroup add column if not exists translations jsonb;

update dataelement o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join dataelementtranslations t on ot.objecttranslationid = t.objecttranslationid  where t.dataelementid = o.dataelementid );
update attribute o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join attributetranslations t on ot.objecttranslationid = t.objecttranslationid  where t.attributeid = o.attributeid );
update categorycombo o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join categorycombotranslations t on ot.objecttranslationid = t.objecttranslationid  where t.categorycomboid = o.categorycomboid );
update categoryoptioncombo o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join categoryoptioncombotranslations t on ot.objecttranslationid = t.objecttranslationid  where t.categoryoptioncomboid = o.categoryoptioncomboid );
update categoryoptiongroupset o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join categoryoptiongroupsettranslations t on ot.objecttranslationid = t.objecttranslationid  where t.categoryoptiongroupsetid = o.categoryoptiongroupsetid );
update categoryoptiongroup o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join categoryoptiongrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.categoryoptiongroupid = o.categoryoptiongroupid );
update dataelementcategoryoption o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join categoryoptiontranslations t on ot.objecttranslationid = t.objecttranslationid  where t.categoryoptionid = o.categoryoptionid );
update chart o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join charttranslations t on ot.objecttranslationid = t.objecttranslationid  where t.chartid = o.chartid );
update colorset o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join colorsettranslations t on ot.objecttranslationid = t.objecttranslationid  where t.colorsetid = o.colorsetid );
update color o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join colortranslations t on ot.objecttranslationid = t.objecttranslationid  where t.colorid = o.colorid );                   
update constant o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join constanttranslations t on ot.objecttranslationid = t.objecttranslationid  where t.colorid = o.constantid );                                                 
update dashboarditem o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join dashboarditemtranslations t on ot.objecttranslationid = t.objecttranslationid  where t.dashboarditemid = o.dashboarditemid );
update dashboard o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join dashboardtranslations t on ot.objecttranslationid = t.objecttranslationid  where t.dashboardid = o.dashboardid );
update dataapprovallevel o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join dataapprovalleveltranslations t on ot.objecttranslationid = t.objecttranslationid  where t.dataapprovallevelid = o.dataapprovallevelid );
update dataapprovalworkflow o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join dataapprovalworkflowtranslations t on ot.objecttranslationid = t.objecttranslationid  where t.workflowid = o.workflowid );                           
update dataelementcategory o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join dataelementcategorytranslations t on ot.objecttranslationid = t.objecttranslationid  where t.categoryid = o.categoryid );                           
update dataelementgroupset o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join dataelementgroupsettranslations t on ot.objecttranslationid = t.objecttranslationid  where t.dataelementgroupsetid = o.dataelementgroupsetid );                           
update dataelementgroup o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join dataelementgrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.dataelementgroupid = o.dataelementgroupid );
update dataentryform o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join dataentryformtranslations t on ot.objecttranslationid = t.objecttranslationid  where t.dataentryformid = o.dataentryformid );
update dataset o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join datasettranslations t on ot.objecttranslationid = t.objecttranslationid  where t.datasetid = o.datasetid );              
update document o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join documenttranslations t on ot.objecttranslationid = t.objecttranslationid  where t.documentid = o.documentid );              
update eventchart o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join eventcharttranslations t on ot.objecttranslationid = t.objecttranslationid  where t.eventchartid = o.eventchartid );              
update eventreport o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join eventreporttranslations t on ot.objecttranslationid = t.objecttranslationid  where t.eventreportid = o.eventreportid );              
update indicatorgroupset o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join indicatorgroupsettranslations t on ot.objecttranslationid = t.objecttranslationid  where t.indicatorgroupsetid = o.indicatorgroupsetid );                                
update indicator o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join indicatortranslations t on ot.objecttranslationid = t.objecttranslationid  where t.indicatorid = o.indicatorid );
update indicatortype o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join indicatortypetranslations t on ot.objecttranslationid = t.objecttranslationid  where t.indicatortypeid = o.indicatortypeid );
update maplegendset o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join maplegendsettranslations t on ot.objecttranslationid = t.objecttranslationid  where t.maplegendsetid = o.maplegendsetid );
update maplegend o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join maplegendtranslations t on ot.objecttranslationid = t.objecttranslationid  where t.maplegendid = o.maplegendid );
update map o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join maptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.mapid = o.mapid );
update mapview o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join mapviewtranslations t on ot.objecttranslationid = t.objecttranslationid  where t.mapviewid = o.mapviewid );
update optiongroupset o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join optiongroupsettranslations t on ot.objecttranslationid = t.objecttranslationid  where t.optiongroupsetid = o.optiongroupsetid );
update optiongroup o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join optiongrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.optiongroupid = o.optiongroupid );                         
update optionset o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join optionsettranslations t on ot.objecttranslationid = t.objecttranslationid  where t.optionsetid = o.optionsetid );                
update optionvalue o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join optionvaluetranslations t on ot.objecttranslationid = t.objecttranslationid  where t.optionvalueid = o.optionvalueid );                               
update organisationunit o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join organisationunittranslations t on ot.objecttranslationid = t.objecttranslationid  where t.organisationunitid = o.organisationunitid );                                                  
update orgunitgroupset o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join orgunitgroupsettranslations t on ot.objecttranslationid = t.objecttranslationid  where t.orgunitgroupsetid = o.orgunitgroupsetid );                                                  
update orgunitgroup o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join orgunitgrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.orgunitgroupid = o.orgunitgroupid );
update orgunitlevel o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join orgunitleveltranslations t on ot.objecttranslationid = t.objecttranslationid  where t.orgunitlevelid = o.orgunitlevelid );                                       
update predictorgroup o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join predictorgrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.predictorgroupid = o.predictorgroupid );
update programindicatorgroup o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join programindicatorgrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.programindicatorgroupid = o.programindicatorgroupid );
update programindicator o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join programindicatortranslations t on ot.objecttranslationid = t.objecttranslationid  where t.programindicatorid = o.programindicatorid );
update programmessage o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join programmessagetranslations t on ot.objecttranslationid = t.objecttranslationid  where t.id = o.id );           
update programrule o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join programruletranslations t on ot.objecttranslationid = t.objecttranslationid  where t.programruleid = o.programruleid );                 
update programsection o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join programsectiontranslations t on ot.objecttranslationid = t.objecttranslationid  where t.programsectionid = o.programsectionid );
update programstagesection o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join programstagesectiontranslations t on ot.objecttranslationid = t.objecttranslationid  where t.programstagesectionid = o.programstagesectionid );
update programstage o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join programstagetranslations t on ot.objecttranslationid = t.objecttranslationid  where t.programstageid = o.programstageid );
update program_attribute_group o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join programtrackedentityattributegrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.programtrackedentityattributegroupid = o.programtrackedentityattributegroupid );        
update program o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join programtranslations t on ot.objecttranslationid = t.objecttranslationid  where t.programid = o.programid );
update relationshiptype o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join relationshiptypetranslations t on ot.objecttranslationid = t.objecttranslationid  where t.relationshiptypeid = o.relationshiptypeid );
update reporttable o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join reporttabletranslations t on ot.objecttranslationid = t.objecttranslationid  where t.reporttableid = o.reporttableid );
update report o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join reporttranslations t on ot.objecttranslationid = t.objecttranslationid  where t.reportid = o.reportid );
update trackedentityattribute o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join trackedentityattributetranslations t on ot.objecttranslationid = t.objecttranslationid  where t.trackedentityattributeid = o.trackedentityattributeid );
update trackedentitytype o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join trackedentitytranslations t on ot.objecttranslationid = t.objecttranslationid  where t.trackedentitytypeid = o.trackedentitytypeid );
update usergroup o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join usergrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.usergroupid = o.usergroupid );
update userrole o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join userroletranslations t on ot.objecttranslationid = t.objecttranslationid  where t.userroleid = o.userroleid );
update validationrule o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join validationruletranslations t on ot.objecttranslationid = t.objecttranslationid  where t.validationruleid = o.validationruleid );
update indicatorgroup o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join indicatorgrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.indicatorgroupid = o.indicatorgroupid );
update validationrulegroup o set translations = (select to_jsonb(array_agg(jsonb_build_object('locale',locale,'property', property,'value', value ))) from objecttranslation ot inner join validationrulegrouptranslations t on ot.objecttranslationid = t.objecttranslationid  where t.validationrulegroupid = o.validationrulegroupid );
        
drop table if exists attributetranslations;
drop table if exists categorycombotranslations;
drop table if exists categoryoptioncombotranslations;
drop table if exists categoryoptiongroupsettranslations;
drop table if exists categoryoptiongrouptranslations;
drop table if exists categoryoptiontranslations;
drop table if exists charttranslations;
drop table if exists colorsettranslations;
drop table if exists colortranslations;
drop table if exists constanttranslations;
drop table if exists dashboarditemtranslations;
drop table if exists dashboardtranslations;
drop table if exists dataapprovalleveltranslations;
drop table if exists dataapprovalworkflowtranslations;
drop table if exists dataelementcategorytranslations;
drop table if exists dataelementgroupsettranslations;
drop table if exists dataelementgrouptranslations;
drop table if exists dataelementoperandtranslations;
drop table if exists dataelementtranslations;
drop table if exists dataentryformtranslations;
drop table if exists datasetsectiontranslations;
drop table if exists datasettranslations;
drop table if exists documenttranslations;
drop table if exists eventcharttranslations;
drop table if exists eventreporttranslations;
drop table if exists indicatorgroupsettranslations;
drop table if exists indicatorgrouptranslations;
drop table if exists indicatortranslations;
drop table if exists indicatortypetranslations;
drop table if exists interpretationcommenttranslations;
drop table if exists interpretationtranslations;
drop table if exists maplayertranslations;
drop table if exists maplegendsettranslations;
drop table if exists maplegendtranslations;
drop table if exists maptranslations;
drop table if exists mapviewtranslations;
drop table if exists messageconversationtranslations;
drop table if exists messagetranslations;
drop table if exists metadatafiltertranslations;
drop table if exists optiongroupsettranslations;
drop table if exists optiongrouptranslations;
drop table if exists optionsettranslations;
drop table if exists optionvaluetranslations;
drop table if exists organisationunittranslations;
drop table if exists orgunitgroupsettranslations;
drop table if exists orgunitgrouptranslations;
drop table if exists orgunitleveltranslations;
drop table if exists periodtranslations;
drop table if exists predictorgrouptranslations;
drop table if exists programattributestranslations;
drop table if exists programindicatorgrouptranslations;
drop table if exists programindicatortranslations;
drop table if exists programinstancetranslations;
drop table if exists programmessagetranslations;
drop table if exists programruleactiontranslations;
drop table if exists programruletranslations;
drop table if exists programrulevariabletranslations;
drop table if exists programsectiontranslations;
drop table if exists programstagedataelementtranslations;
drop table if exists programstageinstancetranslations;
drop table if exists programstagesectiontranslations;
drop table if exists programstagetranslations;
drop table if exists programtrackedentityattributegrouptranslations;
drop table if exists programtranslations;
drop table if exists programvalidationtranslations;
drop table if exists relationshiptypetranslations;
drop table if exists reporttabletranslations;
drop table if exists reporttranslations;
drop table if exists statisticstranslations;
drop table if exists trackedentityattributegrouptranslations;
drop table if exists trackedentityattributetranslations;
drop table if exists trackedentityinstanceremindertranslations;
drop table if exists trackedentityinstancetranslations;
drop table if exists trackedentitytranslations;
drop table if exists usergrouptranslations;
drop table if exists userinfotranslations;
drop table if exists userroletranslations;
drop table if exists usertranslations;
drop table if exists validationcriteriatranslations;
drop table if exists validationrulegrouptranslations;
drop table if exists validationruletranslations;


