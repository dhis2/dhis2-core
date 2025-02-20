-- both tracker sync jobs are removed
delete
from jobconfiguration
where jobtype in ('TRACKER_PROGRAMS_DATA_SYNC', 'EVENT_PROGRAMS_DATA_SYNC');

-- tracker related job parameters are removed from the meta data sync job. keep the data values page
-- size parameter and its value
update jobconfiguration
set jsonbjobparameters =
        jsonb_set(
                jsonbjobparameters,
                '{1}',
                (select jsonb_build_object(
                                'dataValuesPageSize',
                                (select to_jsonb((jsonbjobparameters -> 1 -> 'dataValuesPageSize')::int))
                        ))
        )
where jobtype = 'META_DATA_SYNC';
