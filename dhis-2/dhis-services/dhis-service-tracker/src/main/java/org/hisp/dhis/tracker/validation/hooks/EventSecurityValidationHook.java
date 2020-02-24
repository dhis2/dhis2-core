package org.hisp.dhis.tracker.validation.hooks;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventSecurityValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 301;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );
        User actingUser = bundle.getPreheat().getUser();

        for ( Event event : bundle.getEvents() )
        {
            reporter.increment( event );

            ProgramStageInstance programStageInstance = PreheatHelper
                .getProgramStageInstance( bundle, event.getEvent() );
            ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );
            ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );
            OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, event.getOrgUnit() );
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, event.getTrackedEntity() );
            Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

            if ( program == null )
            {
                continue;
            }

            if ( bundle.getImportStrategy().isCreate() )
            {
                programStage = (programStage == null && program.isWithoutRegistration())
                    ? program.getProgramStageByStage( 1 ) : programStage;

                programInstance = getProgramInstance( actingUser, programInstance, trackedEntityInstance, program );
                if ( programStageInstance != null )
                {
                    programStage = programStageInstance.getProgramStage();
                }

                ProgramStageInstance newProgramStageInstance = new ProgramStageInstance( programInstance, programStage )
                    .setOrganisationUnit( organisationUnit )
                    .setStatus( event.getStatus() );

                List<String> errors = trackerAccessManager.canCreate( actingUser, newProgramStageInstance, false );
                if ( !errors.isEmpty() )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1050 )
                        .addArg( actingUser )
                        .addArg( String.join( ",", errors ) ) );
                }
            }

            if ( bundle.getImportStrategy().isUpdate() )
            {

                List<String> errors = trackerAccessManager.canUpdate( actingUser, programStageInstance, false );
                if ( !errors.isEmpty() )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1050 )
                        .addArg( actingUser )
                        .addArg( String.join( ",", errors ) ) );
                }
            }

            if ( bundle.getImportStrategy().isDelete() )
            {
                List<String> errors = trackerAccessManager.canDelete( actingUser, programStageInstance, false );
                if ( !errors.isEmpty() )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1050 )
                        .addArg( actingUser )
                        .addArg( String.join( ",", errors ) ) );
                }
            }

        }

        return reporter.getReportList();
    }

}
