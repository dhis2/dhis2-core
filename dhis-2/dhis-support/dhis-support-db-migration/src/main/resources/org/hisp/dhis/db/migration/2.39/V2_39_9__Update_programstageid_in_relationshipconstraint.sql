-- This script relates to the issue https://jira.dhis2.org/browse/DHIS2-13036
-- Update programstageid column in relationshipconstraint table (programstageid is now mandatory for event type relationshipconstraint)


UPDATE relationshipconstraint rlc SET programstageid = ps.programstageid FROM programstage ps WHERE rlc.programid = ps.programid
    and ps.programid in (select programid from program where programid in
    (select programid from relationshipconstraint where entity='PROGRAM_STAGE_INSTANCE' and programstageid is null) and type='WITHOUT_REGISTRATION')