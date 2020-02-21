package org.hisp.dhis.tracker.validation.hooks;

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventDateValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 302;
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
            ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, event.getTrackedEntityInstance() );
            Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

            if ( EventStatus.ACTIVE == event.getStatus() && event.getEventDate() == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1031 )
                    .addArg( event ) );
                continue;
            }

            if ( program == null )
            {
                continue;
            }

            programInstance = getProgramInstance( actingUser, programInstance, trackedEntityInstance, program );
            program = programInstance.getProgram();

            validateExpiryDays( reporter, event, program, programStageInstance );
            validateDates( reporter, event );
        }

        return reporter.getReportList();
    }

    private void validateExpiryDays( ValidationErrorReporter errorReporter,
        Event event,
        Program program,
        ProgramStageInstance programStageInstance )
    {
//        if ( importOptions == null || importOptions.getUser() == null ||
//            importOptions.getUser().isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
//        {
//            return;
//        }

        if ( program == null )
        {
            return;
        }

        if ( program.getCompleteEventsExpiryDays() > 0
            && EventStatus.COMPLETED == event.getStatus()
            || (programStageInstance != null && EventStatus.COMPLETED == programStageInstance.getStatus()) )
        {

            Date referenceDate = null;

            if ( programStageInstance != null )
            {
                referenceDate = programStageInstance.getCompletedDate();
            }

            else if ( event.getCompletedDate() != null )
            {
                referenceDate = DateUtils.parseDate( event.getCompletedDate() );
            }

            if ( referenceDate == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1042 )
                    .addArg( event ) );
            }

            if ( (new Date()).after(
                DateUtils.getDateAfterAddition( referenceDate, program.getCompleteEventsExpiryDays() ) ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1043 )
                    .addArg( event ) );
            }

        }

        PeriodType periodType = program.getExpiryPeriodType();

        if ( periodType != null && program.getExpiryDays() > 0 )
        {
            if ( programStageInstance != null )
            {
                Date today = new Date();

                if ( programStageInstance.getExecutionDate() == null )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1044 )
                        .addArg( event ) );
                }

                Period period = periodType.createPeriod( programStageInstance.getExecutionDate() );

                if ( today.after( DateUtils.getDateAfterAddition( period.getEndDate(), program.getExpiryDays() ) ) )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1045 )
                        .addArg( event ) );
                }
            }
            else
            {
                String referenceDate = event.getEventDate() != null ? event.getEventDate() : event.getDueDate();
                if ( referenceDate == null )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1046 )
                        .addArg( event ) );
                }

                Period period = periodType.createPeriod( new Date() );

                if ( DateUtils.parseDate( referenceDate ).before( period.getStartDate() ) )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1047 )
                        .addArg( event ) );
                }
            }
        }

    }

    private void validateDates( ValidationErrorReporter errorReporter, Event event )
    {
        if ( event.getDueDate() != null && !isValidDateString( event.getDueDate() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1051 )
                .addArg( event.getDueDate() ) );
        }

        if ( event.getEventDate() != null && !isValidDateString( event.getEventDate() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1052 )
                .addArg( event.getEventDate() ) );
        }

        if ( event.getCreatedAtClient() != null && !isValidDateString( event.getCreatedAtClient() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1053 )
                .addArg( event.getCreatedAtClient() ) );
        }

        if ( event.getLastUpdatedAtClient() != null && !isValidDateString( event.getLastUpdatedAtClient() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1054 )
                .addArg( event.getLastUpdatedAtClient() ) );
        }

    }

}
