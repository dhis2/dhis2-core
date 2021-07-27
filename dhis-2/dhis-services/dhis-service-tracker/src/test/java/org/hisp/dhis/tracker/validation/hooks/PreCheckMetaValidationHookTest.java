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
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
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
public class PreCheckMetaValidationHookTest
{
    private static final String ORG_UNIT_UID = "OrgUnitUid";

    private static final String TRACKED_ENTITY_TYPE_UID = "TrackedEntityTypeUid";

    private static final String TRACKED_ENTITY_UID = "TrackedEntityUid";

    private static final String PROGRAM_UID = "ProgramUid";

    private static final String PROGRAM_STAGE_UID = "ProgramStageUid";

    private static final String RELATIONSHIP_TYPE_UID = "RelationshipTypeUid";

    private PreCheckMetaValidationHook validatorToTest;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerImportValidationContext ctx;

    @Before
    public void setUp()
    {
        validatorToTest = new PreCheckMetaValidationHook();

        TrackerBundle bundle = TrackerBundle.builder().build();

        when( ctx.getBundle() ).thenReturn( bundle );
    }

    @Test
    public void verifyTrackedEntityValidationSuccess()
    {
        // given
        TrackedEntity tei = validTei();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, tei );

        // when
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( new OrganisationUnit() );
        when( ctx.getTrackedEntityType( TRACKED_ENTITY_TYPE_UID ) ).thenReturn( new TrackedEntityType() );

        validatorToTest.validateTrackedEntity( reporter, tei );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyTrackedEntityValidationFailsWhenOrgUnitIsNotPresentInDb()
    {
        // given
        TrackedEntity tei = validTei();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, tei );

        // when
        when( ctx.getTrackedEntityType( TRACKED_ENTITY_TYPE_UID ) ).thenReturn( new TrackedEntityType() );

        validatorToTest.validateTrackedEntity( reporter, tei );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1049 ) );
    }

    @Test
    public void verifyTrackedEntityValidationFailsWhenTrackedEntityTypeIsNotPresentInDb()
    {
        // given
        TrackedEntity tei = validTei();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, tei );

        // when
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( new OrganisationUnit() );

        validatorToTest.validateTrackedEntity( reporter, tei );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1005 ) );
    }

    @Test
    public void verifyEnrollmentValidationSuccess()
    {
        // given
        Enrollment enrollment = validEnrollment();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );

        // when
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( new OrganisationUnit() );
        when( ctx.getTrackedEntityInstance( TRACKED_ENTITY_UID ) ).thenReturn( new TrackedEntityInstance() );
        when( ctx.getProgram( PROGRAM_UID ) ).thenReturn( new Program() );

        validatorToTest.validateEnrollment( reporter, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyEnrollmentValidationSuccessWhenTeiIsInPayload()
    {
        // given
        Enrollment enrollment = validEnrollment();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );

        // when
        when( ctx.getReference( TRACKED_ENTITY_UID ) )
            .thenReturn( Optional.of( new ReferenceTrackerEntity( "", "" ) ) );
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( new OrganisationUnit() );
        when( ctx.getProgram( PROGRAM_UID ) ).thenReturn( new Program() );

        validatorToTest.validateEnrollment( reporter, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyEnrollmentValidationFailsWhenOrgUnitIsNotPresentInDb()
    {
        // given
        Enrollment enrollment = validEnrollment();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );

        // when
        when( ctx.getProgram( PROGRAM_UID ) ).thenReturn( new Program() );
        when( ctx.getTrackedEntityInstance( TRACKED_ENTITY_UID ) ).thenReturn( new TrackedEntityInstance() );

        validatorToTest.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1070 ) );
    }

    @Test
    public void verifyEnrollmentValidationFailsWhenTrackedEntityIsNotPresentInDbOrPayload()
    {
        // given
        Enrollment enrollment = validEnrollment();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );

        // when
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( new OrganisationUnit() );
        when( ctx.getProgram( PROGRAM_UID ) ).thenReturn( new Program() );

        validatorToTest.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1068 ) );
    }

    @Test
    public void verifyEnrollmentValidationFailsWhenProgramIsNotPresentInDb()
    {
        // given
        Enrollment enrollment = validEnrollment();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );

        // when
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( new OrganisationUnit() );
        when( ctx.getTrackedEntityInstance( TRACKED_ENTITY_UID ) ).thenReturn( new TrackedEntityInstance() );

        validatorToTest.validateEnrollment( reporter, enrollment );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1069 ) );
    }

    @Test
    public void verifyEventValidationSuccess()
    {
        // given
        Event event = validEvent();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );

        // when
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( new OrganisationUnit() );
        when( ctx.getProgram( PROGRAM_UID ) ).thenReturn( new Program() );
        when( ctx.getProgramStage( PROGRAM_STAGE_UID ) ).thenReturn( new ProgramStage() );

        validatorToTest.validateEvent( reporter, event );

        // then
        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyEventValidationFailsWhenProgramIsNotPresentInDb()
    {
        // given
        Event event = validEvent();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );

        // when
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( new OrganisationUnit() );
        when( ctx.getProgramStage( PROGRAM_STAGE_UID ) ).thenReturn( new ProgramStage() );

        validatorToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1010 ) );
    }

    @Test
    public void verifyEventValidationFailsWhenProgramStageIsNotPresentInDb()
    {
        // given
        Event event = validEvent();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );

        // when
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( new OrganisationUnit() );
        when( ctx.getProgram( PROGRAM_UID ) ).thenReturn( new Program() );

        validatorToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1013 ) );
    }

    @Test
    public void verifyEventValidationFailsWhenOrgUnitIsNotPresentInDb()
    {
        // given
        Event event = validEvent();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );

        // when
        when( ctx.getProgram( PROGRAM_UID ) ).thenReturn( new Program() );
        when( ctx.getProgramStage( PROGRAM_STAGE_UID ) ).thenReturn( new ProgramStage() );

        validatorToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1011 ) );
    }

    @Test
    public void verifyRelationshipValidationSuccess()
    {
        // given
        Relationship relationship = validRelationship();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );

        // when
        when( ctx.getRelationShipType( RELATIONSHIP_TYPE_UID ) ).thenReturn( new RelationshipType() );

        validatorToTest.validateRelationship( reporter, relationship );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyRelationshipValidationFailsWhenRelationshipTypeIsNotPresentInDb()
    {
        // given
        Relationship relationship = validRelationship();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );

        // when
        validatorToTest.validateRelationship( reporter, relationship );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E4006 ) );
    }

    private TrackedEntity validTei()
    {
        return TrackedEntity.builder()
            .trackedEntity( TRACKED_ENTITY_UID )
            .orgUnit( ORG_UNIT_UID )
            .trackedEntityType( TRACKED_ENTITY_TYPE_UID )
            .build();
    }

    private Enrollment validEnrollment()
    {
        return Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .trackedEntity( TRACKED_ENTITY_UID )
            .orgUnit( ORG_UNIT_UID )
            .program( PROGRAM_UID )
            .build();
    }

    private Event validEvent()
    {
        return Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( PROGRAM_STAGE_UID )
            .orgUnit( ORG_UNIT_UID )
            .program( PROGRAM_UID )
            .build();
    }

    private Relationship validRelationship()
    {
        return Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( RELATIONSHIP_TYPE_UID )
            .build();
    }
}