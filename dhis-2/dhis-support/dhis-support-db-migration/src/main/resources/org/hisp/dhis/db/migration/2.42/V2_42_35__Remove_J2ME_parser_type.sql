-- Remove parser type J2ME_PARSER and migrate to varchar
-- https://dhis2.atlassian.net/browse/DHIS2-18213
do
$$
    declare
        J2ME_PARSER integer := 1;
    begin

        with j2me_commands as (select smscommandid from smscommands where parsertype = J2ME_PARSER),
             j2me_smscodes as (
                 -- delete rows from junction table and capture smscodes owned by J2ME_PARSER commands which have to be deleted
                 delete
                     from smscommandcodes using j2me_commands
                         where smscommandcodes.id = j2me_commands.smscommandid
                         returning smscommandcodes.codeid),
             j2me_smsspecialcharacter as (
                 -- delete rows from junction table and capture smsspecialcharacter owned by J2ME_PARSER commands which have to be deleted
                 delete
                     from smscommandspecialcharacters using j2me_commands
                         where smscommandspecialcharacters.smscommandid = j2me_commands.smscommandid
                         returning smscommandspecialcharacters.specialcharacterid),
             a as (
                 -- delete smscodes owned by J2ME_PARSER commands
                 delete
                     from smscodes using j2me_smscodes
                         where smscodes.smscodeid = j2me_smscodes.codeid),
             b as (
                 -- delete smsspecialcharacter owned by J2ME_PARSER commands
                 delete
                     from smsspecialcharacter using j2me_smsspecialcharacter
                         where smsspecialcharacter.specialcharacterid =
                               j2me_smsspecialcharacter.specialcharacterid)

        delete
        from smscommands
        where parsertype = J2ME_PARSER;

        -- use org.hisp.dhis.sms.parse.ParserType enum names instead of ordinals
        alter table smscommands
            alter column parsertype type varchar(50) using
                case parsertype
                    when 0 then 'KEY_VALUE_PARSER'
                    when 2 then 'ALERT_PARSER'
                    when 3 then 'UNREGISTERED_PARSER'
                    when 4 then 'TRACKED_ENTITY_REGISTRATION_PARSER'
                    when 5 then 'PROGRAM_STAGE_DATAENTRY_PARSER'
                    when 6 then 'EVENT_REGISTRATION_PARSER'
                    end;
    end
$$
;
