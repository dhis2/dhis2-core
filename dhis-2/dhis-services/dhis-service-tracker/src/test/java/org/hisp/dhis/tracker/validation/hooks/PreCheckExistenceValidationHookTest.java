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
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Enrico Colasante
 */
public class PreCheckExistenceValidationHookTest
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

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerImportValidationContext ctx;

    @Before
    public void setUp()
    {
        validationHook = new PreCheckExistenceValidationHook();

        when( ctx.getBundle() ).thenReturn( bundle );
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getTrackedEntityInstance( SOFT_DELETED_TEI_UID ) ).thenReturn( getSoftDeletedTei() );
        when( ctx.getTrackedEntityInstance( TEI_UID ) ).thenReturn( getTei() );
        when( ctx.getProgramInstance( SOFT_DELETED_ENROLLMENT_UID ) ).thenReturn( getSoftDeletedEnrollment() );
        when( ctx.getProgramInstance( ENROLLMENT_UID ) ).thenReturn( getEnrollment() );
        when( ctx.getProgramStageInstance( SOFT_DELETED_EVENT_UID ) ).thenReturn( getSoftDeletedEvent() );
        when( ctx.getProgramStageInstance( EVENT_UID ) ).thenReturn( getEvent() );
        when( ctx.getRelationship( getPayloadRelationship() ) ).thenReturn( getRelationship() );
    }

    @Test
    public void verifyTrackedEntityValidationSuccessWhenIsCreateAndTeiIsNotPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        // when
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertFalse( reporter.hasErrors() );
        verify( ctx ).setStrategy( trackedEntity, TrackerImportStrategy.CREATE );
    }

    @Test
    public void verifyTrackedEntityValidationSuccessWhenTeiIsNotPresent()
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
        verify( ctx ).setStrategy( trackedEntity, TrackerImportStrategy.CREATE );
    }

    @Test
    public void verifyTrackedEntityValidationSuccessWhenIsUpdate()
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
        verify( ctx ).setStrategy( trackedEntity, TrackerImportStrategy.UPDATE );
    }

    @Test
    public void verifyTrackedEntityValidationFailsWhenIsSoftDeleted()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( SOFT_DELETED_TEI_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1114 ) );
    }

    @Test
    public void verifyTrackedEntityValidationFailsWhenIsCreateAndTEIIsAlreadyPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_UID )
            .build();

        // when
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1002 ) );
    }

    @Test
    public void verifyTrackedEntityValidationFailsWhenIsUpdateAndTEIIsNotPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        // when
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.UPDATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1063 ) );
    }

    @Test
    public void verifyEnrollmentValidationSuccessWhenIsCreateAndEnrollmentIsNotPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
        verify( ctx ).setStrategy( enrollment, TrackerImportStrategy.CREATE );
    }

    @Test
    public void verifyEnrollmentValidationSuccessWhenEnrollmentIsNotPresent()
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
        verify( ctx ).setStrategy( enrollment, TrackerImportStrategy.CREATE );
    }

    @Test
    public void verifyEnrollmentValidationSuccessWhenIsUpdate()
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
        verify( ctx ).setStrategy( enrollment, TrackerImportStrategy.UPDATE );
    }

    @Test
    public void verifyEnrollmentValidationFailsWhenIsSoftDeleted()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( SOFT_DELETED_ENROLLMENT_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1113 ) );
    }

    @Test
    public void verifyEnrollmentValidationFailsWhenIsCreateAndEnrollmentIsAlreadyPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1080 ) );
    }

    @Test
    public void verifyEnrollmentValidationFailsWhenIsUpdateAndEnrollmentIsNotPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.UPDATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1081 ) );
    }

    @Test
    public void verifyEventValidationSuccessWhenIsCreateAndEventIsNotPresent()
    {
        // given
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        // when
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
        verify( ctx ).setStrategy( event, TrackerImportStrategy.CREATE );
    }

    @Test
    public void verifyEventValidationSuccessWhenEventIsNotPresent()
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
        verify( ctx ).setStrategy( event, TrackerImportStrategy.CREATE );
    }

    @Test
    public void verifyEventValidationSuccessWhenIsUpdate()
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
        verify( ctx ).setStrategy( event, TrackerImportStrategy.UPDATE );
    }

    @Test
    public void verifyEventValidationFailsWhenIsSoftDeleted()
    {
        // given
        Event event = Event.builder()
            .event( SOFT_DELETED_EVENT_UID )
            .build();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1082 ) );
    }

    @Test
    public void verifyEventValidationFailsWhenIsCreateAndEventIsAlreadyPresent()
    {
        // given
        Event event = Event.builder()
            .event( EVENT_UID )
            .build();

        // when
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1030 ) );
    }

    @Test
    public void verifyEventValidationFailsWhenIsUpdateAndEventIsNotPresent()
    {
        // given
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        // when
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.UPDATE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1032 ) );
    }

    @Test
    public void verifyRelationshipValidationSuccessWhenIsCreate()
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
    public void verifyRelationshipValidationFailsWhenUpdate()
    {
        // given
        Relationship rel = getPayloadRelationship();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, rel );
        validationHook.validateRelationship( reporter, rel );

        // then
        assertFalse( reporter.hasErrors() );
        assertThat( reporter.getWarningsReportList().get( 0 ).getWarningCode(), is( E4015 ) );
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