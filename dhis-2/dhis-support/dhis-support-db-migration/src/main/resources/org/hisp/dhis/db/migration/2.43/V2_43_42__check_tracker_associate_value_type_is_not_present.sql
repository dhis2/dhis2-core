-- Fail if unsupported TRACKER_ASSOCIATE value type is used in the DB
do $$
    begin
        if ((select count(*) from trackedentityattribute where valuetype = 'TRACKER_ASSOCIATE') + (select count(*) from dataelement where valuetype = 'TRACKER_ASSOCIATE') > 0)
        then begin
            raise exception 'There is inconsistent data in your DB. Please check https://github.com/dhis2/dhis2-releases/blob/master/releases/2.43/migration-notes.md#tracker-associate to have more information on the issue and to find ways to fix it.';
        end;
        end if;
    end;
$$;
