package org.hisp.dhis.dxf2.events.importer.shared.validation;

import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Luciano Fiandesio
 */
public class EventBaseCheckTest extends BaseValidationTest
{
    private EventBaseCheck rule;

    @Before
    public void setUp()
    {

        rule = new EventBaseCheck();
    }

    @Test
    public void verifyErrorOnInvalidDueDate()
    {
        event.setEvent( event.getUid() );
        event.setDueDate( "111-12-122" );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event, "Invalid event due date: " + event.getDueDate() );

    }

    @Test
    public void verifyErrorOnInvalidEventDate()
    {
        event.setEvent( event.getUid() );
        event.setEventDate( "111-12-122" );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event, "Invalid event date: " + event.getEventDate() );

    }

    @Test
    public void verifyErrorOnInvalidCreatedAtClientDate()
    {
        event.setEvent( event.getUid() );
        event.setCreatedAtClient( "111-12-122" );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event,
            "Invalid event created at client date: " + event.getCreatedAtClient() );

    }

    @Test
    public void verifyErrorOnInvalidLastUpdatedAtClientDate()
    {
        event.setEvent( event.getUid() );
        event.setLastUpdatedAtClient( "111-12-122" );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event,
            "Invalid event last updated at client date: " + event.getLastUpdatedAtClient() );
    }

    @Test
    public void verifyErrorOnMissingProgramInstance()
    {
        event.setEvent( event.getUid() );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event, "No program instance found for event: " + event.getEvent() );
    }

    @Test
    public void verifyNoErrorOnNonCompletedProgramInstance()
    {
        event.setEvent( event.getUid() );

        Map<String, ProgramInstance> programInstanceMap = new HashMap<>();
        ProgramInstance programInstance = new ProgramInstance();
        programInstanceMap.put( event.getUid(), programInstance );
        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );

        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );

        assertNoError( importSummary );
    }

    @Test
    public void verifyErrorOnEventWithDateNewerThanCompletedProgramInstance()
    {
        // Given
        ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
        importOptions.setUser( new User() );
        event.setEvent( event.getUid() );

        Map<String, ProgramInstance> programInstanceMap = new HashMap<>();
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setStatus( ProgramStatus.COMPLETED );
        // Set program instance end date to NOW - one month
        programInstance.setEndDate( Date.from( ZonedDateTime.now().minusMonths( 1 ).toInstant() ) );
        programInstanceMap.put( event.getUid(), programInstance );

        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );
        when( workContext.getImportOptions() ).thenReturn( importOptions );

        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );

        // Then
        assertHasError( importSummary, event, null );
    }
}