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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
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
public class PreCheckUpdatableFieldsValidationHookTest
{
    private final static String TRACKED_ENTITY_TYPE_ID = "TrackedEntityTypeId";

    private final static String PROGRAM_ID = "ProgramId";

    private final static String PROGRAM_STAGE_ID = "ProgramStageId";

    private final static String TRACKED_ENTITY_ID = "TrackedEntityId";

    private final static String ENROLLMENT_ID = "EnrollmentId";

    private final static String EVENT_ID = "EventId";

    private PreCheckUpdatableFieldsValidationHook validationHook;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerImportValidationContext ctx;

    @Mock
    private TrackerBundle bundle;

    @Before
    public void setUp()
    {
        validationHook = new PreCheckUpdatableFieldsValidationHook();

        when( ctx.getBundle() ).thenReturn( bundle );
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );

        when( ctx.getStrategy( any( TrackedEntity.class ) ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( ctx.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( ctx.getStrategy( any( Event.class ) ) ).thenReturn( TrackerImportStrategy.UPDATE );

        when( ctx.getTrackedEntityInstance( TRACKED_ENTITY_ID ) ).thenReturn( trackedEntityInstance() );
        when( ctx.getProgramInstance( ENROLLMENT_ID ) ).thenReturn( programInstance() );
        when( ctx.getProgramStageInstance( EVENT_ID ) ).thenReturn( programStageInstance() );
    }

    @Test
    public void verifyTrackedEntityValidationSuccess()
    {
        // given
        TrackedEntity trackedEntity = validTei();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyTrackedEntityValidationFailsWhenUpdateTrackedEntityType()
    {
        // given
        TrackedEntity trackedEntity = validTei();
        trackedEntity.setTrackedEntityType( "NewTrackedEntityTypeId" );

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1126 ) );
    }

    @Test
    public void verifyEnrollmentValidationSuccess()
    {
        // given
        Enrollment enrollment = validEnrollment();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyEnrollmentValidationFailsWhenUpdateProgram()
    {
        // given
        Enrollment enrollment = validEnrollment();
        enrollment.setProgram( "NewProgramId" );

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1127 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), containsString( "program" ) );
    }

    @Test
    public void verifyEnrollmentValidationFailsWhenUpdateTrackedEntity()
    {
        // given
        Enrollment enrollment = validEnrollment();
        enrollment.setTrackedEntity( "NewTrackedEntityId" );

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1127 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), containsString( "trackedEntity" ) );
    }

    @Test
    public void verifyEventValidationSuccess()
    {
        // given
        Event event = validEvent();

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyEventValidationFailsWhenUpdateProgramStage()
    {
        // given
        Event event = validEvent();
        event.setProgramStage( "NewProgramStageId" );

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1128 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), containsString( "programStage" ) );
    }

    @Test
    public void verifyEventValidationFailsWhenUpdateEnrollment()
    {
        // given
        Event event = validEvent();
        event.setEnrollment( "NewEnrollmentId" );

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1128 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), containsString( "enrollment" ) );
    }

    private TrackedEntity validTei()
    {
        return TrackedEntity.builder()
            .trackedEntity( TRACKED_ENTITY_ID )
            .trackedEntityType( TRACKED_ENTITY_TYPE_ID )
            .build();
    }

    private Enrollment validEnrollment()
    {
        return Enrollment.builder()
            .enrollment( ENROLLMENT_ID )
            .trackedEntity( TRACKED_ENTITY_ID )
            .program( PROGRAM_ID )
            .build();
    }

    private Event validEvent()
    {
        return Event.builder()
            .event( EVENT_ID )
            .programStage( PROGRAM_STAGE_ID )
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