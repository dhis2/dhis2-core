package org.hisp.dhis.dxf2.events.importer.update.validation;

import static org.hisp.dhis.dxf2.importsummary.ImportSummary.error;
import static org.hisp.dhis.dxf2.importsummary.ImportSummary.success;

import java.util.Date;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Luciano Fiandesio
 */
public class ExpirationDaysCheck implements Checker
{
    @Override
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        final ImportOptions importOptions = ctx.getImportOptions();
        final Program program = ctx.getProgramsMap().get( event.getProgram() );
        final ProgramStageInstance programStageInstance = ctx.getProgramStageInstanceMap().get( event.getEvent() );

        if ( importOptions == null || importOptions.getUser() == null
            || importOptions.getUser().isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
        {
            return success();
        }

        if ( program != null )
        {
            ImportSummary importSummary = checkEventOrPsiCompletedDate( program, event, programStageInstance );

            if ( importSummary.isStatus( ImportStatus.ERROR ) )
            {
                return importSummary;
            }

            return checkEventOrPsiExpirationDate( program, event, programStageInstance );

        }

        return success();
    }

    private ImportSummary checkEventOrPsiCompletedDate( Program program, ImmutableEvent event,
        ProgramStageInstance programStageInstance )
    {
        if ( program.getCompleteEventsExpiryDays() > 0 )
        {
            if ( event.getStatus() == EventStatus.COMPLETED
                || programStageInstance != null && programStageInstance.getStatus() == EventStatus.COMPLETED )
            {
                Date referenceDate = null;

                if ( programStageInstance != null )
                {
                    referenceDate = programStageInstance.getCompletedDate();
                }
                else
                {
                    if ( event.getCompletedDate() != null )
                    {
                        referenceDate = DateUtils.parseDate( event.getCompletedDate() );
                    }
                }

                if ( referenceDate == null )
                {
                    return error( "Event needs to have completed date", event.getEvent() );
                }

                if ( (new Date()).after(
                    DateUtils.getDateAfterAddition( referenceDate, program.getCompleteEventsExpiryDays() ) ) )
                {
                    return error(
                        "The event's completeness date has expired. Not possible to make changes to this event",
                        event.getEvent() );
                }
            }
        }
        return success();
    }

    private ImportSummary checkEventOrPsiExpirationDate( Program program, ImmutableEvent event,
        ProgramStageInstance programStageInstance )
    {

        PeriodType periodType = program.getExpiryPeriodType();

        if ( periodType != null && program.getExpiryDays() > 0 )
        {
            if ( programStageInstance != null )
            {
                Date today = new Date();

                if ( programStageInstance.getExecutionDate() == null )
                {
                    return error( "Event needs to have event date", event.getEvent() );
                }

                Period period = periodType.createPeriod( programStageInstance.getExecutionDate() );
                if ( today.after( DateUtils.getDateAfterAddition( period.getEndDate(), program.getExpiryDays() ) ) )
                {
                    return error(
                        "The program's expiry date has passed. It is not possible to make changes to this event",
                        event.getEvent() );
                }
            }
            else
            {
                String referenceDate = event.getEventDate() != null ? event.getEventDate() : event.getDueDate();

                if ( referenceDate == null )
                {
                    return error( "Event needs to have at least one (event or schedule) date", event.getEvent() );
                }

                Period period = periodType.createPeriod( new Date() );

                if ( DateUtils.parseDate( referenceDate ).before( period.getStartDate() ) )
                {
                    return error(
                        "The event's date belongs to an expired period. It is not possible to create such event",
                        event.getEvent() );
                }
            }
        }
        return success();

    }
}
