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
import static org.hamcrest.Matchers.containsString;
import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1126;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1127;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1128;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
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
class PreCheckUpdatableFieldsValidationHookTest
{

    private final static String TRACKED_ENTITY_TYPE_ID = "TrackedEntityTypeId";

    private final static String PROGRAM_ID = "ProgramId";

    private final static String PROGRAM_STAGE_ID = "ProgramStageId";

    private final static String TRACKED_ENTITY_ID = "TrackedEntityId";

    private final static String ENROLLMENT_ID = "EnrollmentId";

    private final static String EVENT_ID = "EventId";

    private PreCheckUpdatableFieldsValidationHook validationHook;

    @Mock
    private TrackerBundle bundle;

    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        validationHook = new PreCheckUpdatableFieldsValidationHook();

        when( bundle.getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );

        when( bundle.getStrategy( any( TrackedEntity.class ) ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( bundle.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( bundle.getStrategy( any( Event.class ) ) ).thenReturn( TrackerImportStrategy.UPDATE );

        when( bundle.getTrackedEntityInstance( TRACKED_ENTITY_ID ) ).thenReturn( trackedEntityInstance() );
        when( bundle.getProgramInstance( ENROLLMENT_ID ) ).thenReturn( programInstance() );
        when( bundle.getProgramStageInstance( EVENT_ID ) ).thenReturn( programStageInstance() );

        preheat = new TrackerPreheat();
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        preheat.setIdSchemes( idSchemes );
        reporter = new ValidationErrorReporter( idSchemes );
        when( bundle.getPreheat() ).thenReturn( preheat );
    }

    @Test
    void verifyTrackedEntityValidationSuccess()
    {
        // given
        TrackedEntity trackedEntity = validTei();

        // when
        validationHook.validateTrackedEntity( reporter, bundle, trackedEntity );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenUpdateTrackedEntityType()
    {
        // given
        TrackedEntity trackedEntity = validTei();
        trackedEntity.setTrackedEntityType( MetadataIdentifier.ofUid( "NewTrackedEntityTypeId" ) );

        // when
        validationHook.validateTrackedEntity( reporter, bundle, trackedEntity );

        // then
        hasTrackerError( reporter, E1126, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyEnrollmentValidationSuccess()
    {
        // given
        Enrollment enrollment = validEnrollment();

        // when
        validationHook.validateEnrollment( reporter, bundle, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenUpdateProgram()
    {
        // given
        Enrollment enrollment = validEnrollment( "NewProgramId" );

        // when
        validationHook.validateEnrollment( reporter, bundle, enrollment );

        // then
        hasTrackerError( reporter, E1127, ENROLLMENT, enrollment.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), containsString( "program" ) );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenUpdateTrackedEntity()
    {
        // given
        Enrollment enrollment = validEnrollment();
        enrollment.setTrackedEntity( "NewTrackedEntityId" );

        // when
        validationHook.validateEnrollment( reporter, bundle, enrollment );

        // then
        hasTrackerError( reporter, E1127, ENROLLMENT, enrollment.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), containsString( "trackedEntity" ) );
    }

    @Test
    void verifyEventValidationSuccess()
    {
        // given
        Event event = validEvent();

        // when
        validationHook.validateEvent( reporter, bundle, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventValidationFailsWhenUpdateProgramStage()
    {
        // given
        Event event = validEvent();
        event.setProgramStage( MetadataIdentifier.ofUid( "NewProgramStageId" ) );

        // when
        validationHook.validateEvent( reporter, bundle, event );

        // then
        hasTrackerError( reporter, E1128, EVENT, event.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), containsString( "programStage" ) );
    }

    @Test
    void verifyEventValidationFailsWhenUpdateEnrollment()
    {
        // given
        Event event = validEvent();
        event.setEnrollment( "NewEnrollmentId" );

        // when
        validationHook.validateEvent( reporter, bundle, event );

        // then
        hasTrackerError( reporter, E1128, EVENT, event.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), containsString( "enrollment" ) );
    }

    private TrackedEntity validTei()
    {
        return TrackedEntity.builder()
            .trackedEntity( TRACKED_ENTITY_ID )
            .trackedEntityType( MetadataIdentifier.ofUid( TRACKED_ENTITY_TYPE_ID ) )
            .build();
    }

    private Enrollment validEnrollment()
    {
        return validEnrollment( PROGRAM_ID );
    }

    private Enrollment validEnrollment( String uid )
    {
        return Enrollment.builder()
            .enrollment( ENROLLMENT_ID )
            .trackedEntity( TRACKED_ENTITY_ID )
            .program( MetadataIdentifier.ofUid( uid ) )
            .build();
    }

    private Event validEvent()
    {
        return Event.builder()
            .event( EVENT_ID )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .enrollment( ENROLLMENT_ID )
            .build();
    }

    private TrackedEntityInstance trackedEntityInstance()
    {
        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( TRACKED_ENTITY_TYPE_ID );

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( TRACKED_ENTITY_ID );
        trackedEntityInstance.setTrackedEntityType( trackedEntityType );
        return trackedEntityInstance;
    }

    private ProgramInstance programInstance()
    {
        Program program = new Program();
        program.setUid( PROGRAM_ID );

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( ENROLLMENT_ID );
        programInstance.setProgram( program );
        programInstance.setEntityInstance( trackedEntityInstance() );
        return programInstance;
    }

    private ProgramStageInstance programStageInstance()
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setUid( PROGRAM_STAGE_ID );

        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( EVENT_ID );
        programStageInstance.setProgramInstance( programInstance() );
        programStageInstance.setProgramStage( programStage );
        return programStageInstance;
    }
}