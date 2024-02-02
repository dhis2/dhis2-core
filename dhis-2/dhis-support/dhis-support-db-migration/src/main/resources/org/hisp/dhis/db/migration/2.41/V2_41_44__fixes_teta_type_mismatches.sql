CREATE or replace FUNCTION can_be_casted(s text, type text) RETURNS bool AS
$$
BEGIN
    execute 'SELECT $1::' || type || ';' USING s;
    return true;
EXCEPTION
    WHEN OTHERS THEN
        RETURN false;
END;
$$ LANGUAGE plpgsql STRICT;

DO
$do$
    DECLARE
        _sql text;
        cur CURSOR FOR
            select distinct
                'update trackedentityattribute set valuetype=''TEXT'' where uid = ''' || uid ||
                ''''
            from (select tea.uid,
                         tea.valuetype,
                         teav.value,
                         case
                             when tea.valuetype in ('NUMBER', 'UNIT_INTERVAL', 'PERCENTAGE')
                                 then can_be_casted(teav.value, 'double precision')
                             when tea.valuetype like '%INTEGER%'
                                 then can_be_casted(teav.value, 'integer')
                             when tea.valuetype in ('DATE', 'DATETIME', 'AGE')
                                 then can_be_casted(teav.value, 'timestamp')
                             end as safe_to_cast
                  from trackedentityattribute tea
                           join trackedentityattributevalue teav
                                on tea.trackedentityattributeid =
                                   teav.trackedentityattributeid) as t1
            where safe_to_cast = false;
    BEGIN
        open cur;
        loop
            fetch cur into _sql;
            exit when not found;
            EXECUTE _sql;
        end loop;
    END
$do$;

DROP function if exists can_be_casted(s text, type text);
