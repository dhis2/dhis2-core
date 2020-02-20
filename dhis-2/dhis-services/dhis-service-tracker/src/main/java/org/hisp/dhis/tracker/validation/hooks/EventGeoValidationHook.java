package org.hisp.dhis.tracker.validation.hooks;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventGeoValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 304;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        for ( Event event : bundle.getEvents() )
        {
            reporter.increment( event );

            ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );
            ProgramStageInstance programStageInstance = PreheatHelper
                .getProgramStageInstance( bundle, event.getEvent() );
            Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

            if ( program == null )
            {
                continue;
            }

            programStage = (programStage == null && program.isWithoutRegistration())
                ? program.getProgramStageByStage( 1 ) : programStage;
            if ( programStage == null )
            {
                continue;
            }

            if ( programStageInstance != null )
            {
                programStage = programStageInstance.getProgramStage();
            }

            validateGeo( reporter,
                event.getGeometry(),
                event.getCoordinate() != null ? event.getCoordinate().getCoordinateString() : null,
                programStage.getFeatureType() );

        }

        return reporter.getReportList();
    }
}
