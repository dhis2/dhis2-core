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
import static org.hamcrest.Matchers.empty;
import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1002;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1030;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1032;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1063;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1080;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1081;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1082;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1113;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1114;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4015;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class PreCheckExistenceValidationHookTest
{

    private PreCheckExistenceValidationHook validationHook;

    private final static String SOFT_DELETED_TEI_UID = "SoftDeletedTEIId";

    private final static String TEI_UID = "TEIId";

    private final static String NOT_PRESENT_TEI_UID = "NotPresentTEIId";

    private final static String SOFT_DELETED_ENROLLMENT_UID = "SoftDeletedEnrollmentId";

    private final static String ENROLLMENT_UID = "EnrollmentId";

    private final static String NOT_PRESENT_ENROLLMENT_UID = "NotPresentEnrollmentId";

    private final static String SOFT_DELETED_EVENT_UID = "SoftDeletedEventId";

    private final static String EVENT_UID = "EventId";

    private final static String NOT_PRESENT_EVENT_UID = "NotPresentEventId";

    private final static String NOT_PRESENT_RELATIONSHIP_UID = "NotPresentRelationshipId";

    private final static String RELATIONSHIP_UID = "RelationshipId";

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerImportValidationContext ctx;

    @BeforeEach
    public void setUp()
    {
        validationHook = new PreCheckExistenceValidationHook();

        when( ctx.getBundle() ).thenReturn( bundle );
        when( ctx.getStrategy( any( Event.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getStrategy( any( TrackedEntity.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getTrackedEntityInstance( SOFT_DELETED_TEI_UID ) ).thenReturn( getSoftDeletedTei() );
        when( ctx.getTrackedEntityInstance( TEI_UID ) ).thenReturn( getTei() );
        when( ctx.getProgramInstance( SOFT_DELETED_ENROLLMENT_UID ) ).thenReturn( getSoftDeletedEnrollment() );
        when( ctx.getProgramInstance( ENROLLMENT_UID ) ).thenReturn( getEnrollment() );
        when( ctx.getProgramStageInstance( SOFT_DELETED_EVENT_UID ) ).thenReturn( getSoftDeletedEvent() );
        when( ctx.getProgramStageInstance( EVENT_UID ) ).thenReturn( getEvent() );
        when( ctx.getRelationship( getPayloadRelationship() ) ).thenReturn( getRelationship() );
    }

    @Test
    void verifyTrackedEntityValidationSuccessWhenIsCreateAndTeiIsNotPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        // when
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationSuccessWhenTeiIsNotPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationSuccessWhenIsUpdate()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenIsSoftDeleted()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( SOFT_DELETED_TEI_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        hasTrackerError( reporter, E1114, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenIsCreateAndTEIIsAlreadyPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_UID )
            .build();

        // when
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        hasTrackerError( reporter, E1002, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenIsUpdateAndTEIIsNotPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        // when
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.UPDATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        hasTrackerError( reporter, E1063, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenIsCreateAndEnrollmentIsNotPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenEnrollmentIsNotPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenIsUpdate()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( ENROLLMENT_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsSoftDeleted()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( SOFT_DELETED_ENROLLMENT_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        hasTrackerError( reporter, E1113, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsCreateAndEnrollmentIsAlreadyPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        hasTrackerError( reporter, E1080, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsUpdateAndEnrollmentIsNotPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.UPDATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        hasTrackerError( reporter, E1081, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEventValidationSuccessWhenIsCreateAndEventIsNotPresent()
    {
        // given
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        // when
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventValidationSuccessWhenEventIsNotPresent()
    {
        // given
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventValidationSuccessWhenIsUpdate()
    {
        // given
        Event event = Event.builder()
            .event( EVENT_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventValidationFailsWhenIsSoftDeleted()
    {
        // given
        Event event = Event.builder()
            .event( SOFT_DELETED_EVENT_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        hasTrackerError( reporter, E1082, EVENT, event.getUid() );
    }

    @Test
    void verifyEventValidationFailsWhenIsCreateAndEventIsAlreadyPresent()
    {
        // given
        Event event = Event.builder()
            .event( EVENT_UID )
            .build();

        // when
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        hasTrackerError( reporter, E1030, EVENT, event.getUid() );
    }

    @Test
    void verifyEventValidationFailsWhenIsUpdateAndEventIsNotPresent()
    {
        // given
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        // when
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        hasTrackerError( reporter, E1032, EVENT, event.getUid() );
    }

    @Test
    void verifyRelationshipValidationSuccessWhenIsCreate()
    {
        // given
        Relationship rel = Relationship.builder()
            .relationship( NOT_PRESENT_RELATIONSHIP_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, rel );
        validationHook.validateRelationship( reporter, rel );

        // then
        assertFalse( reporter.hasErrors() );
        assertThat( reporter.getWarningsReportList(), empty() );
    }

    @Test
    void verifyRelationshipValidationFailsWhenUpdate()
    {
        // given
        Relationship rel = getPayloadRelationship();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, rel );
        validationHook.validateRelationship( reporter, rel );

        // then
        assertFalse( reporter.hasErrors() );
        assertTrue( reporter.hasWarningReport( r -> E4015.equals( r.getWarningCode() ) &&
            TrackerType.RELATIONSHIP.equals( r.getTrackerType() ) &&
            rel.getUid().equals( r.getUid() ) ) );
    }

    private TrackedEntityInstance getSoftDeletedTei()
    {
        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( SOFT_DELETED_TEI_UID );
        trackedEntityInstance.setDeleted( true );
        return trackedEntityInstance;
    }

    private TrackedEntityInstance getTei()
    {
        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( TEI_UID );
        trackedEntityInstance.setDeleted( false );
        return trackedEntityInstance;
    }

    private ProgramInstance getSoftDeletedEnrollment()
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( SOFT_DELETED_ENROLLMENT_UID );
        programInstance.setDeleted( true );
        return programInstance;
    }

    private ProgramInstance getEnrollment()
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( ENROLLMENT_UID );
        programInstance.setDeleted( false );
        return programInstance;
    }

    private ProgramStageInstance getSoftDeletedEvent()
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( SOFT_DELETED_EVENT_UID );
        programStageInstance.setDeleted( true );
        return programStageInstance;
    }

    private ProgramStageInstance getEvent()
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( EVENT_UID );
        programStageInstance.setDeleted( false );
        return programStageInstance;
    }

    private Relationship getPayloadRelationship()
    {
        return Relationship.builder()
            .relationship( RELATIONSHIP_UID )
            .build();
    }

    private org.hisp.dhis.relationship.Relationship getRelationship()
    {
        org.hisp.dhis.relationship.Relationship relationship = new org.hisp.dhis.relationship.Relationship();
        relationship.setUid( EVENT_UID );
        return relationship;
    }
}