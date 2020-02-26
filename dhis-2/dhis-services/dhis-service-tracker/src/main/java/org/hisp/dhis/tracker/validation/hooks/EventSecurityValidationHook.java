package org.hisp.dhis.tracker.validation.hooks;

import org.hisp.dhis.event.EventStatus;
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
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.*;

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

            if ( organisationUnit != null &&
                !organisationUnitService.isInUserHierarchyCached( actingUser, organisationUnit ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1000 )
                    .addArg( actingUser )
                    .addArg( organisationUnit ) );
            }

            if ( bundle.getImportStrategy().isCreate() )
            {
                validateCreate( reporter, actingUser, event, programStageInstance, programStage, programInstance,
                    organisationUnit,
                    trackedEntityInstance, program );
            }
            else if ( bundle.getImportStrategy().isUpdate() || bundle.getImportStrategy().isDelete() )
            {
                // This is checked and reported in the required properties validation hook.
                if ( programStageInstance == null )
                {
                    continue;
                }

                validateUpdateAndDelete( bundle, reporter, actingUser, event, programStageInstance );
            }

        }

        return reporter.getReportList();
    }

    protected void validateUpdateAndDelete( TrackerBundle bundle, ValidationErrorReporter reporter, User actingUser,
        Event event, ProgramStageInstance programStageInstance )
    {
        Objects.requireNonNull( programStageInstance, PROGRAM_INSTANCE_CAN_T_BE_NULL );
        Objects.requireNonNull( actingUser, USER_CAN_T_BE_NULL );
        Objects.requireNonNull( event, EVENT_CAN_T_BE_NULL );

        if ( bundle.getImportStrategy().isUpdate() )
        {
            List<String> errors = trackerAccessManager.canUpdate( actingUser, programStageInstance, false );
            if ( !errors.isEmpty() )
            {
                reporter.addError( newReport( TrackerErrorCode.E1050 )
                    .addArg( actingUser )
                    .addArg( String.join( ",", errors ) ) );
            }

            if ( event.getStatus() != programStageInstance.getStatus()
                && EventStatus.COMPLETED == programStageInstance.getStatus()
                && (!actingUser.isSuper() && !actingUser.isAuthorized( "F_UNCOMPLETE_EVENT" )) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1083 )
                    .addArg( actingUser ) );
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

    protected void validateCreate( ValidationErrorReporter reporter, User actingUser, Event event,
        ProgramStageInstance programStageInstance, ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, TrackedEntityInstance trackedEntityInstance, Program program )
    {
        Objects.requireNonNull( actingUser, USER_CAN_T_BE_NULL );
        Objects.requireNonNull( event, EVENT_CAN_T_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CAN_T_BE_NULL );

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
}
