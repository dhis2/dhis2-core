/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Enrico Colasante
 */
public class PreCheckUidValidationHookTest
{
    private static final String INVALID_UID = "InvalidUID";

    private PreCheckUidValidationHook validationHook;

    private TrackerImportValidationContext ctx;

    @Before
    public void setUp()
    {
        validationHook = new PreCheckUidValidationHook();
        ctx = new TrackerImportValidationContext( TrackerBundle.builder().build() );
    }

    @Test
    public void verifyTrackedEntityValidationSuccess()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( CodeGenerator.generateUid() )
            .orgUnit( CodeGenerator.generateUid() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyTrackedEntityWithInvalidUidFails()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( INVALID_UID )
            .orgUnit( CodeGenerator.generateUid() )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1048 ) );

    }

    @Test
    public void verifyEnrollmentValidationSuccess()
    {
        // given
        Note note = Note.builder().note( CodeGenerator.generateUid() ).build();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .notes( Lists.newArrayList( note ) )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyEnrollmentWithInvalidUidFails()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( INVALID_UID )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1048 ) );
    }

    @Test
    public void verifyEnrollmentWithNoteWithInvalidUidFails()
    {
        // given
        Note note = Note.builder().note( INVALID_UID ).build();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .notes( Lists.newArrayList( note ) )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1048 ) );
    }

    @Test
    public void verifyEventValidationSuccess()
    {
        // given
        Note note = Note.builder().note( CodeGenerator.generateUid() ).build();
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .notes( Lists.newArrayList( note ) )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyEventWithInvalidUidFails()
    {
        // given
        Event event = Event.builder()
            .event( INVALID_UID )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1048 ) );
    }

    @Test
    public void verifyEventWithNoteWithInvalidUidFails()
    {
        // given
        Note note = Note.builder().note( INVALID_UID ).build();
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .notes( Lists.newArrayList( note ) )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1048 ) );
    }

    @Test
    public void verifyRelationshipValidationSuccess()
    {
        // given
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyRelationshipWithInvalidUidFails()
    {
        // given
        Relationship relationship = Relationship.builder()
            .relationship( INVALID_UID )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1048 ) );
    }
}