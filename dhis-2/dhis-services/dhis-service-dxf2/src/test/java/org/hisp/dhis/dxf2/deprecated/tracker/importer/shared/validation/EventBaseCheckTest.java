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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.validation;

import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
class EventBaseCheckTest extends BaseValidationTest
{

    private EventBaseCheck rule;

    @BeforeEach
    void setUp()
    {
        rule = new EventBaseCheck();
    }

    @Test
    void verifyErrorOnInvalidDueDate()
    {
        event.setEvent( event.getUid() );
        event.setDueDate( "111-12-122" );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event, "Invalid event due date: " + event.getDueDate() );
    }

    @Test
    void verifyErrorOnInvalidEventDate()
    {
        event.setEvent( event.getUid() );
        event.setEventDate( "111-12-122" );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event, "Invalid event date: " + event.getEventDate() );
    }

    @Test
    void verifyErrorOnInvalidCreatedAtClientDate()
    {
        event.setEvent( event.getUid() );
        event.setCreatedAtClient( "111-12-122" );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event,
            "Invalid event created at client date: " + event.getCreatedAtClient() );
    }

    @Test
    void verifyErrorOnInvalidLastUpdatedAtClientDate()
    {
        event.setEvent( event.getUid() );
        event.setLastUpdatedAtClient( "111-12-122" );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event,
            "Invalid event last updated at client date: " + event.getLastUpdatedAtClient() );
    }

    @Test
    void verifyErrorOnMissingProgramInstance()
    {
        event.setEvent( event.getUid() );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( importSummary, event, null );
        assertHasConflict( importSummary, event, "No enrollment found for event: " + event.getEvent() );
    }

    @Test
    void verifyNoErrorOnNonCompletedProgramInstance()
    {
        event.setEvent( event.getUid() );
        Map<String, Enrollment> programInstanceMap = new HashMap<>();
        Enrollment enrollment = new Enrollment();
        programInstanceMap.put( event.getUid(), enrollment );
        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        assertNoError( importSummary );
    }

    @Test
    void verifyErrorOnEventWithDateNewerThanCompletedProgramInstance()
    {
        // Given
        ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
        importOptions.setUser( new User() );
        event.setEvent( event.getUid() );
        Map<String, Enrollment> programInstanceMap = new HashMap<>();
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus( ProgramStatus.COMPLETED );
        // Set enrollment end date to NOW - one month
        enrollment.setEndDate( Date.from( ZonedDateTime.now().minusMonths( 1 ).toInstant() ) );
        programInstanceMap.put( event.getUid(), enrollment );
        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );
        when( workContext.getImportOptions() ).thenReturn( importOptions );
        // When
        ImportSummary importSummary = rule.check( new ImmutableEvent( event ), workContext );
        // Then
        assertHasError( importSummary, event, null );
    }
}
