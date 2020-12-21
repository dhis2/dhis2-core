package org.hisp.dhis.tracker;

import org.hisp.dhis.tracker.bundle.TrackerBundle;

/**
 * @author Luciano Fiandesio
 */
public class ParamsConverter
{
    public static TrackerBundle convert( TrackerImportParams params )
    {
        return TrackerBundle.builder()
            .importMode( params.getImportMode() )
            .importStrategy( params.getImportStrategy() )
            .skipTextPatternValidation( params.isSkipPatternValidation() )
            .skipSideEffects( params.isSkipSideEffects() )
            .skipRuleEngine( params.isSkipRuleEngine() )
            .flushMode( params.getFlushMode() )
            .validationMode( params.getValidationMode() )
            .trackedEntities( params.getTrackedEntities() )
            .enrollments( params.getEnrollments() )
            .events( params.getEvents() )
            .relationships( params.getRelationships() )
            .user( params.getUser() )
            .build();
    }

}
