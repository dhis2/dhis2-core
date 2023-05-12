/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.dxf2.deprecated.tracker.importer.update.validation;

import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
class ExpirationDaysCheckTest extends BaseValidationTest
{

    private ExpirationDaysCheck rule;

    @BeforeEach
    void setUp()
    {
        rule = new ExpirationDaysCheck();
        // Prepare import options
        ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
        importOptions.setUser( new User() );
        when( workContext.getImportOptions() ).thenReturn( importOptions );
    }

    @Test
    void failWhenProgramStageInstanceHasNoCompletedDateAndProgramHasExpiryDays()
    {
        // Given
        // Prepare program
        Program program = createProgram( 'P' );
        program.setCompleteEventsExpiryDays( 3 );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );
        // Prepare program stage instance
        Map<String, Event> psiMap = new HashMap<>();
        Event psi = new Event();
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
    void failWhenEventHasNoCompletedDateAndProgramHasExpiryDays()
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
    void failWhenProgramStageInstanceCompletedDateFallsAfterCurrentDay()
    {
        // Given
        // Prepare program
        Program program = createProgram( 'P' );
        program.setCompleteEventsExpiryDays( 3 );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );
        // Prepare program stage instance
        Map<String, Event> psiMap = new HashMap<>();
        Event psi = new Event();
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
    void failWhenProgramStageInstanceHasNoExecDateAndProgramHasPeriodType()
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
        Map<String, Event> psiMap = new HashMap<>();
        Event psi = new Event();
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
    void failWhenProgramStageInstanceHasExecutionDateBeforeAllowedProgramExpiryDaysBasedOnPeriod()
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
        Map<String, Event> psiMap = new HashMap<>();
        Event psi = new Event();
        // month length + 5
        psi.setExecutionDate( getTodayMinusDays( 35 ) );
        // days
        psi.setUid( event.getUid() );
        psiMap.put( event.getUid(), psi );
        when( workContext.getProgramStageInstanceMap() ).thenReturn( psiMap );
        // Prepare event
        event.setProgram( program.getUid() );
        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        // Then
        assertHasError( importSummary, event,
            "The program's expiry date has passed. It is not possible to make changes to this event" );
    }

    @Test
    void failWhenEventHasNoDateAndProgramExpiryDaysBasedOnPeriod()
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
    void failWhenEventHasDateBeforeCurrentDateAndProgramExpiryDaysBasedOnPeriod()
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

    private Date getTodayMinusDays( int days )
    {
        LocalDateTime localDateTime = new Date().toInstant().atZone( ZoneId.systemDefault() ).toLocalDateTime()
            .minusDays( days );
        return Date.from( localDateTime.atZone( ZoneId.systemDefault() ).toInstant() );
    }
}
