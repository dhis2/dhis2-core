UPDATE relationshipconstraint
SET dataview = R.dataview
    FROM (
        SELECT RC.relationshipconstraintid as relationshipconstraintid,
            json_build_object(
                'attributes',
                COALESCE(
                    json_agg(COALESCE(TEA.uid)) FILTER (
                        WHERE RC.entity IN ('TRACKED_ENTITY_INSTANCE','PROGRAM_INSTANCE')
                    ),
                    '[]'
                )::jsonb,
                'dataelements',
                COALESCE(
                    json_agg(COALESCE(DE.uid)) FILTER (
                        WHERE RC.entity = 'PROGRAM_STAGE_INSTANCE'
                    ),
                    '[]'
                )::jsonb
            ) AS dataview
        FROM relationshipconstraint RC
            LEFT JOIN trackedentitytypeattribute TETA ON TETA.trackedentitytypeid = RC.trackedentitytypeid
            LEFT JOIN program_attributes PTEA ON PTEA.programid = RC.programid
            LEFT JOIN programstagedataelement PSDE ON PSDE.programstageid IN (
                SELECT programstageid
                FROM programstage
                WHERE (
                        RC.entity = 'PROGRAM_STAGE_INSTANCE'
                        AND programid = RC.programid
                    )
                    OR programstageid = RC.programstageid
            )
            LEFT JOIN trackedentityattribute TEA ON TEA.trackedentityattributeid = TETA.trackedentityattributeid
            OR TEA.trackedentityattributeid = PTEA.trackedentityattributeid
            LEFT JOIN dataelement DE ON DE.dataelementid = PSDE.dataelementid
        WHERE (
                TETA.displayinlist = TRUE
                OR PTEA.displayinlist = TRUE
                OR PSDE.displayinreports = TRUE
            )
        GROUP BY relationshipconstraintid
    ) R
WHERE   relationshipconstraint.relationshipconstraintid = R.relationshipconstraintid;