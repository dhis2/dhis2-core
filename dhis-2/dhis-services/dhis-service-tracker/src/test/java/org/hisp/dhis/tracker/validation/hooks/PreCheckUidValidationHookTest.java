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

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1048;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

/**
 * @author Enrico Colasante
 */
class PreCheckUidValidationHookTest
{

    private static final String INVALID_UID = "InvalidUID";

    private PreCheckUidValidationHook validationHook;

    private TrackerImportValidationContext ctx;

    @BeforeEach
    void setUp()
    {
        validationHook = new PreCheckUidValidationHook();
        ctx = new TrackerImportValidationContext( TrackerBundle.builder().build() );
    }

    @Test
    void verifyTrackedEntityValidationSuccess()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder().trackedEntity( CodeGenerator.generateUid() )
            .orgUnit( CodeGenerator.generateUid() ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityWithInvalidUidFails()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder().trackedEntity( INVALID_UID )
            .orgUnit( CodeGenerator.generateUid() ).build();
        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );
        // then
        hasTrackerError( reporter, E1048, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyEnrollmentValidationSuccess()
    {
        // given
        Note note = Note.builder().note( CodeGenerator.generateUid() ).build();
        Enrollment enrollment = Enrollment.builder().enrollment( CodeGenerator.generateUid() )
            .notes( Lists.newArrayList( note ) ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );
        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentWithInvalidUidFails()
    {
        // given
        Enrollment enrollment = Enrollment.builder().enrollment( INVALID_UID ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );
        // then
        hasTrackerError( reporter, E1048, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentWithNoteWithInvalidUidFails()
    {
        // given
        Note note = Note.builder().note( INVALID_UID ).build();
        Enrollment enrollment = Enrollment.builder().enrollment( CodeGenerator.generateUid() )
            .notes( Lists.newArrayList( note ) ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );
        // then
        hasTrackerError( reporter, E1048, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEventValidationSuccess()
    {
        // given
        Note note = Note.builder().note( CodeGenerator.generateUid() ).build();
        Event event = Event.builder().event( CodeGenerator.generateUid() ).notes( Lists.newArrayList( note ) ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );
        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventWithInvalidUidFails()
    {
        // given
        Event event = Event.builder().event( INVALID_UID ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );
        // then
        hasTrackerError( reporter, E1048, EVENT, event.getUid() );
    }

    @Test
    void verifyEventWithNoteWithInvalidUidFails()
    {
        // given
        Note note = Note.builder().note( INVALID_UID ).build();
        Event event = Event.builder().event( CodeGenerator.generateUid() ).notes( Lists.newArrayList( note ) ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );
        // then
        hasTrackerError( reporter, E1048, EVENT, event.getUid() );
    }

    @Test
    void verifyRelationshipValidationSuccess()
    {
        // given
        Relationship relationship = Relationship.builder().relationship( CodeGenerator.generateUid() ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );
        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyRelationshipWithInvalidUidFails()
    {
        // given
        Relationship relationship = Relationship.builder().relationship( INVALID_UID ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );
        // then
        hasTrackerError( reporter, E1048, RELATIONSHIP, relationship.getUid() );
    }
}
