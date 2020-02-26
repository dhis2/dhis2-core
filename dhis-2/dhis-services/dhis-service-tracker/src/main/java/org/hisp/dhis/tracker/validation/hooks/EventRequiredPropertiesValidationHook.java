package org.hisp.dhis.tracker.validation.hooks;

import com.google.common.base.Preconditions;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.*;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventRequiredPropertiesValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 300;
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

            if ( programStageInstance == null && isValidId( bundle.getIdentifier(), event.getEvent() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1071 )
                    .addArg( event ) );
            }

            if ( programStageInstance == null
                && (bundle.getImportStrategy().isDelete() || bundle.getImportStrategy().isUpdate()) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1082 )
                    .addArg( event.getEvent() ) );
            }

            if ( organisationUnit == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1011 )
                    .addArg( event.getOrgUnit() ) );
            }

            if ( program == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1034 )
                    .addArg( event ) );

                continue;
            }

            programStage = (programStage == null && program.isWithoutRegistration())
                ? program.getProgramStageByStage( 1 ) : programStage;

            if ( programStage == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1035 )
                    .addArg( event ) );
            }

            programInstance = validateProgramInstance( reporter, actingUser, event, programStage, programInstance,
                trackedEntityInstance, program );

            if ( !programInstance.getProgram().hasOrganisationUnit( organisationUnit ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1041 )
                    .addArg( organisationUnit ) );
            }
        }

        return reporter.getReportList();
    }

    protected ProgramInstance validateProgramInstance( ValidationErrorReporter reporter, User actingUser, Event event,
        ProgramStage programStage, ProgramInstance programInstance, TrackedEntityInstance trackedEntityInstance,
        Program program )
    {
        Objects.requireNonNull( event, EVENT_CAN_T_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CAN_T_BE_NULL );
        Preconditions.checkNotNull( actingUser, USER_CAN_T_BE_NULL );

        if ( program.isRegistration() )
        {
            if ( trackedEntityInstance == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1036 )
                    .addArg( event ) );
            }

            if ( programInstance == null && trackedEntityInstance != null )
            {
                List<ProgramInstance> activeProgramInstances = new ArrayList<>( programInstanceService
                    .getProgramInstances( trackedEntityInstance, program, ProgramStatus.ACTIVE ) );

                if ( activeProgramInstances.isEmpty() )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1037 )
                        .addArg( trackedEntityInstance )
                        .addArg( program ) );
                }
                else if ( activeProgramInstances.size() > 1 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1038 )
                        .addArg( trackedEntityInstance )
                        .addArg( program ) );
                }
                else
                {
                    programInstance = activeProgramInstances.get( 0 );
                }
            }

            if ( programStage != null && programInstance != null &&
                !programStage.getRepeatable() && programInstance.hasProgramStageInstance( programStage ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1039 ) );
            }
        }
        else
        {

            // NOTE: This is cached in the prev. event importer? What do we do here?
            List<ProgramInstance> activeProgramInstances = programInstanceService
                .getProgramInstances( program, ProgramStatus.ACTIVE );

            if ( activeProgramInstances.isEmpty() )
            {
                ProgramInstance pi = new ProgramInstance();
                pi.setEnrollmentDate( new Date() );
                pi.setIncidentDate( new Date() );
                pi.setProgram( program );
                pi.setStatus( ProgramStatus.ACTIVE );
                pi.setStoredBy( actingUser.getUsername() );

                programInstance = pi;
            }
            else if ( activeProgramInstances.size() > 1 )
            {
                reporter.addError( newReport( TrackerErrorCode.E1040 )
                    .addArg( program ) );
            }
            else
            {
                programInstance = activeProgramInstances.get( 0 );
            }
        }

        return programInstance;
    }

}
