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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
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
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4016;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4017;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class PreCheckExistenceValidationHookTest
{
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

    private final static String SOFT_DELETED_RELATIONSHIP_UID = "SoftDeletedRelationshipId";

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private PreCheckExistenceValidationHook validationHook = new PreCheckExistenceValidationHook();

    @BeforeEach
    void setUp()
    {

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getIdSchemes() ).thenReturn( TrackerIdSchemeParams.builder().build() );
    }

    @Test
    void verifyTrackedEntityValidationSuccessWhenIsCreateAndTeiIsNotPresent()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationSuccessWhenTeiIsNotPresent()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        when( bundle.getStrategy( any( TrackedEntity.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationSuccessWhenIsUpdate()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_UID )
            .build();

        when( bundle.getTrackedEntityInstance( TEI_UID ) ).thenReturn( getTei() );
        when( bundle.getStrategy( any( TrackedEntity.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenIsSoftDeleted()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( SOFT_DELETED_TEI_UID )
            .build();

        when( bundle.getTrackedEntityInstance( SOFT_DELETED_TEI_UID ) ).thenReturn( getSoftDeletedTei() );
        when( bundle.getStrategy( any( TrackedEntity.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        hasTrackerError( reporter, E1114, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenIsCreateAndTEIIsAlreadyPresent()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_UID )
            .build();

        when( bundle.getTrackedEntityInstance( TEI_UID ) ).thenReturn( getTei() );
        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        hasTrackerError( reporter, E1002, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenIsUpdateAndTEIIsNotPresent()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.UPDATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        hasTrackerError( reporter, E1063, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenIsCreateAndEnrollmentIsNotPresent()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenEnrollmentIsNotPresent()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        when( bundle.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenIsUpdate()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( ENROLLMENT_UID )
            .build();

        when( bundle.getProgramInstance( ENROLLMENT_UID ) ).thenReturn( getEnrollment() );
        when( bundle.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsSoftDeleted()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( SOFT_DELETED_ENROLLMENT_UID )
            .build();

        when( bundle.getProgramInstance( SOFT_DELETED_ENROLLMENT_UID ) ).thenReturn( getSoftDeletedEnrollment() );
        when( bundle.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEnrollment( reporter, enrollment );

        hasTrackerError( reporter, E1113, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsCreateAndEnrollmentIsAlreadyPresent()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( ENROLLMENT_UID )
            .build();

        when( bundle.getProgramInstance( ENROLLMENT_UID ) ).thenReturn( getEnrollment() );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEnrollment( reporter, enrollment );

        hasTrackerError( reporter, E1080, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsUpdateAndEnrollmentIsNotPresent()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.UPDATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEnrollment( reporter, enrollment );

        hasTrackerError( reporter, E1081, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEventValidationSuccessWhenIsCreateAndEventIsNotPresent()
    {
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventValidationSuccessWhenEventIsNotPresent()
    {
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        when( bundle.getStrategy( any( Event.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventValidationSuccessWhenIsUpdate()
    {
        Event event = Event.builder()
            .event( EVENT_UID )
            .build();

        when( bundle.getProgramStageInstance( EVENT_UID ) ).thenReturn( getEvent() );
        when( bundle.getStrategy( any( Event.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventValidationFailsWhenIsSoftDeleted()
    {
        Event event = Event.builder()
            .event( SOFT_DELETED_EVENT_UID )
            .build();

        when( bundle.getProgramStageInstance( SOFT_DELETED_EVENT_UID ) ).thenReturn( getSoftDeletedEvent() );
        when( bundle.getStrategy( any( Event.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEvent( reporter, event );

        hasTrackerError( reporter, E1082, EVENT, event.getUid() );
    }

    @Test
    void verifyEventValidationFailsWhenIsCreateAndEventIsAlreadyPresent()
    {
        Event event = Event.builder()
            .event( EVENT_UID )
            .build();

        when( bundle.getProgramStageInstance( EVENT_UID ) ).thenReturn( getEvent() );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEvent( reporter, event );

        hasTrackerError( reporter, E1030, EVENT, event.getUid() );
    }

    @Test
    void verifyEventValidationFailsWhenIsUpdateAndEventIsNotPresent()
    {
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateEvent( reporter, event );

        hasTrackerError( reporter, E1032, EVENT, event.getUid() );
    }

    @Test
    void verifyRelationshipValidationSuccessWhenIsCreate()
    {
        Relationship rel = Relationship.builder()
            .relationship( NOT_PRESENT_RELATIONSHIP_UID )
            .build();

        when( bundle.getStrategy( rel ) ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateRelationship( reporter, rel );

        assertFalse( reporter.hasErrors() );
        assertThat( reporter.getWarningsReportList(), empty() );
    }

    @Test
    void verifyRelationshipValidationSuccessWithWarningWhenUpdate()
    {
        Relationship rel = getPayloadRelationship();

        when( bundle.getStrategy( rel ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( bundle.getRelationship( RELATIONSHIP_UID ) ).thenReturn( getRelationship() );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateRelationship( reporter, rel );

        assertFalse( reporter.hasErrors() );
        assertTrue( reporter.hasWarningReport( r -> E4015.equals( r.getWarningCode() ) &&
            TrackerType.RELATIONSHIP.equals( r.getTrackerType() ) &&
            rel.getUid().equals( r.getUid() ) ) );
    }

    @Test
    void verifyRelationshipValidationFailsWhenIsCreateAndRelationshipIsAlreadyPresent()
    {
        Relationship rel = getPayloadRelationship();

        when( bundle.getStrategy( rel ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( bundle.getRelationship( RELATIONSHIP_UID ) ).thenReturn( getRelationship() );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateRelationship( reporter, rel );

        hasTrackerError( reporter, E4015, RELATIONSHIP, rel.getUid() );
    }

    @Test
    void verifyRelationshipValidationFailsWhenIsDeleteAndRelationshipIsNotPresent()
    {
        Relationship rel = Relationship.builder()
            .relationship( NOT_PRESENT_RELATIONSHIP_UID )
            .build();

        when( bundle.getStrategy( rel ) ).thenReturn( TrackerImportStrategy.DELETE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateRelationship( reporter, rel );

        hasTrackerError( reporter, E4016, RELATIONSHIP, rel.getUid() );
    }

    @Test
    void verifyRelationshipValidationFailsWhenIsSoftDeleted()
    {
        Relationship rel = Relationship.builder()
            .relationship( SOFT_DELETED_RELATIONSHIP_UID )
            .build();

        when( bundle.getRelationship( SOFT_DELETED_RELATIONSHIP_UID ) ).thenReturn( softDeletedRelationship() );
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle );
        validationHook.validateRelationship( reporter, rel );

        hasTrackerError( reporter, E4017, RELATIONSHIP, rel.getUid() );
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

    private org.hisp.dhis.relationship.Relationship softDeletedRelationship()
    {
        org.hisp.dhis.relationship.Relationship relationship = new org.hisp.dhis.relationship.Relationship();
        relationship.setUid( SOFT_DELETED_RELATIONSHIP_UID );
        relationship.setDeleted( true );
        return relationship;
    }

    private org.hisp.dhis.relationship.Relationship getRelationship()
    {
        org.hisp.dhis.relationship.Relationship relationship = new org.hisp.dhis.relationship.Relationship();
        relationship.setUid( RELATIONSHIP_UID );
        return relationship;
    }
}