/* Converts numerous attribute boolean columns into a single jsonb column
   with an array containing enum names of those columns that were true */

/* create jsonb set column */
alter table attribute add column if not exists objecttypes jsonb;

/* make sure we have a function to create jsonb arrays without nulls */
create or replace function jsonb_build_array_without_nulls(variadic anyarray)
    returns jsonb language sql immutable as $$
select jsonb_agg(elem)
from unnest($1) as elem
where elem is not null
$$;

/* Update is wrapped in a function so it only runs when flag columns still exist */
create function update_object_type_columns_to_jsonb() returns void as
$$
begin
    perform column_name from information_schema.columns where table_name= 'attribute' and column_name = 'dataelementattribute';
    if found then
        /* aggregate columns to new jsonb column (only true remains) */
        update attribute set objecttypes = jsonb_build_array_without_nulls(
                case when dataelementattribute = true then 'DATA_ELEMENT' end,
                case when indicatorattribute = true then 'INDICATOR' end,
                case when organisationunitattribute = true then 'ORGANISATION_UNIT' end,
                case when userattribute = true then 'USER' end,
                case when dataelementgroupattribute = true then 'DATA_ELEMENT_GROUP' end,
                case when indicatorgroupattribute = true then 'INDICATOR_GROUP' end,
                case when organisationunitgroupattribute = true then 'ORGANISATION_UNIT_GROUP' end,
                case when usergroupattribute = true then 'USER_GROUP' end,
                case when datasetattribute = true then 'DATA_SET' end,
                case when organisationunitgroupsetattribute = true then 'ORGANISATION_UNIT_GROUP_SET' end,
                case when programattribute = true then 'PROGRAM' end,
                case when programstageattribute = true then 'PROGRAM_STAGE' end,
                case when trackedentitytypeattribute = true then 'TRACKED_ENTITY_TYPE' end,
                case when trackedentityattributeattribute = true then 'TRACKED_ENTITY_ATTRIBUTE' end,
                case when categoryoptionattribute = true then 'CATEGORY_OPTION' end,
                case when categoryoptiongroupattribute = true then 'CATEGORY_OPTION_GROUP' end,
                case when documentattribute = true then 'DOCUMENT' end,
                case when optionattribute = true then 'OPTION' end,
                case when optionsetattribute = true then 'OPTION_SET' end,
                case when constantattribute = true then 'CONSTANT' end,
                case when legendsetattribute = true then 'LEGEND_SET' end,
                case when programindicatorattribute = true then 'PROGRAM_INDICATOR' end,
                case when sqlviewattribute = true then 'SQL_VIEW' end,
                case when sectionattribute = true then 'SECTION' end,
                case when categoryoptioncomboattribute = true then 'CATEGORY_OPTION_COMBO' end,
                case when categoryoptiongroupsetattribute = true then 'CATEGORY_OPTION_GROUP_SET' end,
                case when dataelementgroupsetattribute = true then 'DATA_ELEMENT_GROUP_SET' end,
                case when validationruleattribute = true then 'VALIDATION_RULE' end,
                case when validationrulegroupattribute = true then 'VALIDATION_RULE_GROUP' end,
                case when categoryattribute = true then 'CATEGORY' end,
                case when visualizationattribute = true then 'VISUALIZATION' end,
                case when mapattribute = true then 'MAP' end,
                case when eventreportattribute = true then 'EVENT_REPORT' end,
                case when eventchartattribute = true then 'EVENT_CHART' end,
                case when relationshiptypeattribute = true then 'RELATIONSHIP_TYPE' end
            )
        where 1=1;
    end if;
end;
$$ language plpgsql;
/* run the update*/
select update_object_type_columns_to_jsonb();
/* clean up the update function */
drop function if exists update_object_type_columns_to_jsonb();

/*set empty array for any null value*/
update attribute set objecttypes = '[]'::jsonb where objecttypes is null;

/* make new column not null now that all have a jsonb value */
alter table attribute alter column objecttypes set not null;

/* drop all the flag columns*/
alter table attribute
    drop column if exists dataelementattribute,
    drop column if exists indicatorattribute,
    drop column if exists organisationunitattribute,
    drop column if exists userattribute,
    drop column if exists dataelementgroupattribute,
    drop column if exists indicatorgroupattribute,
    drop column if exists organisationunitgroupattribute,
    drop column if exists usergroupattribute,
    drop column if exists datasetattribute,
    drop column if exists organisationunitgroupsetattribute,
    drop column if exists programattribute,
    drop column if exists programstageattribute,
    drop column if exists trackedentitytypeattribute,
    drop column if exists trackedentityattributeattribute,
    drop column if exists categoryoptionattribute,
    drop column if exists categoryoptiongroupattribute,
    drop column if exists documentattribute,
    drop column if exists optionattribute,
    drop column if exists optionsetattribute,
    drop column if exists constantattribute,
    drop column if exists legendsetattribute,
    drop column if exists programindicatorattribute,
    drop column if exists sqlviewattribute,
    drop column if exists sectionattribute,
    drop column if exists categoryoptioncomboattribute,
    drop column if exists categoryoptiongroupsetattribute,
    drop column if exists dataelementgroupsetattribute,
    drop column if exists validationruleattribute,
    drop column if exists validationrulegroupattribute,
    drop column if exists categoryattribute,
    drop column if exists visualizationattribute,
    drop column if exists mapattribute,
    drop column if exists eventreportattribute,
    drop column if exists eventchartattribute,
    drop column if exists relationshiptypeattribute;