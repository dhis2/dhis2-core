package org.hisp.dhis.dxf2.events.importer.update.validation;

import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Luciano Fiandesio
 */
public class ExpirationDaysCheckTest extends BaseValidationTest
{
    private ExpirationDaysCheck rule;

    @Before
    public void setUp()
    {
        rule = new ExpirationDaysCheck();

        // Prepare import options
        ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
        importOptions.setUser( new User() );
        when( workContext.getImportOptions() ).thenReturn( importOptions );

    }

    @Test
    public void failWhenProgramStageInstanceHasNoCompletedDateAndProgramHasExpiryDays()
    {
        // Given

        // Prepare program
        Program program = createProgram( 'P' );
        program.setCompleteEventsExpiryDays( 3 );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );

        // Prepare program stage instance
        Map<String, ProgramStageInstance> psiMap = new HashMap<>();
        ProgramStageInstance psi = new ProgramStageInstance();
        psi.setStatus( EventStatus.COMPLETED );
        psi.setUid( event.getUid() );
        psiMap.put( event.getUid(), psi );
        when( workContext.getProgramStageInstanceMap() ).thenReturn( psiMap );

        // Prepare event
        event.setProgram( program.getUid() );
        event.setStatus( EventStatus.COMPLETED );

        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );

        // Then
        assertHasError( importSummary, event, "Event needs to have completed date" );
    }

    @Test
    public void failWhenEventHasNoCompletedDateAndProgramHasExpiryDays()
    {
        // Given

        // Prepare program
        Program program = createProgram( 'P' );
        program.setCompleteEventsExpiryDays( 3 );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );

        // Prepare event
        event.setProgram( program.getUid() );
        event.setStatus( EventStatus.COMPLETED );

        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );

        // Then
        assertHasError( importSummary, event, "Event needs to have completed date" );
    }

    @Test
    public void failWhenProgramStageInstanceCompletedDateFallsAfterCurrentDay()
    {
        // Given

        // Prepare program
        Program program = createProgram( 'P' );
        program.setCompleteEventsExpiryDays( 3 );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );

        // Prepare program stage instance
        Map<String, ProgramStageInstance> psiMap = new HashMap<>();
        ProgramStageInstance psi = new ProgramStageInstance();
        psi.setStatus( EventStatus.COMPLETED );
        psi.setCompletedDate( getTodayMinusDays( 5 ) );
        psi.setUid( event.getUid() );
        psiMap.put( event.getUid(), psi );
        when( workContext.getProgramStageInstanceMap() ).thenReturn( psiMap );

        // Prepare event
        event.setProgram( program.getUid() );
        event.setStatus( EventStatus.COMPLETED );

        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );

        // Then
        assertHasError( importSummary, event,
            "The event's completeness date has expired. Not possible to make changes to this event" );
    }

    @Test
    public void failWhenProgramStageInstanceHasNoExecDateAndProgramHasPeriodType()
    {
        // Given

        // Prepare program
        Program program = createProgram( 'P' );
        program.setCompleteEventsExpiryDays( 3 );
        program.setExpiryPeriodType( PeriodType.getPeriodTypeFromIsoString( "202001" ) );
        program.setExpiryDays( 3 );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );

        // Prepare program stage instance
        Map<String, ProgramStageInstance> psiMap = new HashMap<>();
        ProgramStageInstance psi = new ProgramStageInstance();
        psi.setCompletedDate( getTodayPlusDays( 5 ) );
        psi.setUid( event.getUid() );
        psiMap.put( event.getUid(), psi );
        when( workContext.getProgramStageInstanceMap() ).thenReturn( psiMap );

        // Prepare event
        event.setProgram( program.getUid() );

        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );

        // Then
        assertHasError( importSummary, event, "Event needs to have event date" );
    }

    @Test
    public void failWhenProgramStageInstanceHasExecutionDateBeforeAllowedProgramExpiryDaysBasedOnPeriod()
    {
        // Given
        final String monthlyPeriodType = new SimpleDateFormat( "yyyyMM" ).format( new Date() );
        // Prepare program
        Program program = createProgram( 'P' );
        program.setExpiryPeriodType( PeriodType.getPeriodTypeFromIsoString( monthlyPeriodType ) );
        program.setExpiryDays( 3 );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );

        // Prepare program stage instance
        Map<String, ProgramStageInstance> psiMap = new HashMap<>();
        ProgramStageInstance psi = new ProgramStageInstance();
        psi.setExecutionDate( getTodayMinusDays( 32 ) ); // month length + two days
        psi.setUid( event.getUid() );
        psiMap.put( event.getUid(), psi );
        when( workContext.getProgramStageInstanceMap() ).thenReturn( psiMap );

        // Prepare event
        event.setProgram( program.getUid() );

        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );

        // Then
        assertHasError( importSummary, event, "The program's expiry date has passed. It is not possible to make changes to this event" );
    }

    @Test
    public void failWhenEventHasNoDateAndProgramExpiryDaysBasedOnPeriod()
    {
        // Given
        final String monthlyPeriodType = new SimpleDateFormat( "yyyyMM" ).format( new Date() );
        // Prepare program
        Program program = createProgram( 'P' );
        program.setExpiryPeriodType( PeriodType.getPeriodTypeFromIsoString( monthlyPeriodType ) );
        program.setExpiryDays( 3 );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );

        // Prepare event
        event.setProgram( program.getUid() );

        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );

        // Then
        assertHasError( importSummary, event, "Event needs to have at least one (event or schedule) date" );
    }

    @Test
    public void failWhenEventHasDateBeforeCurrentDateAndProgramExpiryDaysBasedOnPeriod()
    {
        // Given
        final String monthlyPeriodType = new SimpleDateFormat( "yyyyMM" ).format( new Date() );
        // Prepare program
        Program program = createProgram( 'P' );
        program.setExpiryPeriodType( PeriodType.getPeriodTypeFromIsoString( monthlyPeriodType ) );
        program.setExpiryDays( 3 );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );

        // Prepare event
        event.setProgram( program.getUid() );
        event.setEventDate( new SimpleDateFormat( "yyyy-MM-dd" ).format( getTodayMinusDays( 100 ) ) );

        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );

        // Then
        assertHasError( importSummary, event,
            "The event's date belongs to an expired period. It is not possible to create such event" );
    }

    private Date getTodayPlusDays( int days )
    {
        LocalDateTime localDateTime = new Date().toInstant().atZone( ZoneId.systemDefault() ).toLocalDateTime()
            .plusDays( days );
        return Date.from( localDateTime.atZone( ZoneId.systemDefault() ).toInstant() );
    }

    private Date getTodayMinusDays( int days)
    {
        LocalDateTime localDateTime = new Date().toInstant().atZone( ZoneId.systemDefault() ).toLocalDateTime()
            .minusDays( days );
        return Date.from( localDateTime.atZone( ZoneId.systemDefault() ).toInstant() );
    }


}