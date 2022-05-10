-- This script relates to the issue https://jira.dhis2.org/browse/DHIS2-13036
-- Update programstageid column in relationshipconstraint table (programstageid is now mandatory for event type relationshipconstraint)


UPDATE relationshipconstraint rlc SET programstageid = ps.programstageid FROM programstage ps
        WHERE rlc.programid = ps.programid AND ps.programid IN
                (SELECT programid FROM programWHERE type = 'WITHOUT_REGISTRATION')
        AND rlc.programstageid IS NULL
        AND rlc.programid IS NOT NULL
        AND rlc.entity = 'PROGRAM_STAGE_INSTANCE'