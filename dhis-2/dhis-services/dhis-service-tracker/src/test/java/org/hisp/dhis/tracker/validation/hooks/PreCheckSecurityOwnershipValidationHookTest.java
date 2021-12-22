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
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1083;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1100;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1103;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class PreCheckSecurityOwnershipValidationHookTest extends DhisConvenienceTest
{

    private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

    private static final String TEI_ID = "TEI_ID";

    private static final String TEI_TYPE_ID = "TEI_TYPE_ID";

    private static final String PROGRAM_ID = "PROGRAM_ID";

    private static final String PS_ID = "PS_ID";

    private PreCheckSecurityOwnershipValidationHook validatorToTest;

    @Mock
    private TrackerImportValidationContext ctx;

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @Mock
    private AclService aclService;

    @Mock
    private TrackerOwnershipManager ownershipAccessManager;

    @Mock
    private OrganisationUnitService organisationUnitService;

    private User user;

    private ValidationErrorReporter reporter;

    private OrganisationUnit organisationUnit;

    private TrackedEntityType trackedEntityType;

    private Program program;

    private ProgramStage programStage;

    @BeforeEach
    public void setUp()
    {
        user = createUser( 'A' );
        bundle = TrackerBundle.builder().user( user ).preheat( preheat ).build();

        when( ctx.getBundle() ).thenReturn( bundle );

        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnit.setUid( ORG_UNIT_ID );

        trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityType.setUid( TEI_TYPE_ID );
        program = createProgram( 'A' );
        program.setUid( PROGRAM_ID );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        program.setTrackedEntityType( trackedEntityType );

        programStage = createProgramStage( 'A', program );
        programStage.setUid( PS_ID );

        validatorToTest = new PreCheckSecurityOwnershipValidationHook( aclService, ownershipAccessManager,
            organisationUnitService );
    }

    @Test
    void verifyValidationSuccessForTrackedEntity()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( CodeGenerator.generateUid() )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntityType( TEI_TYPE_ID )
            .build();
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( ctx.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( organisationUnitService.isInUserSearchHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, trackedEntity );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackedEntityWithNoProgramInstancesUsingDeleteStrategy()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntityType( TEI_TYPE_ID )
            .build();
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( ctx.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( ctx.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithNoProgramInstances() );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, trackedEntity );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyCaptureScopeIsCheckedForTrackedEntityCreation()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntityType( TEI_TYPE_ID )
            .build();
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( ctx.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, trackedEntity );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifySearchScopeIsCheckedForTrackedEntityUpdate()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntityType( TEI_TYPE_ID )
            .build();
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( ctx.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserSearchHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, trackedEntity );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyCaptureScopeIsCheckedForTrackedEntityDeletion()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntityType( TEI_TYPE_ID )
            .build();
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( ctx.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( ctx.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithNoProgramInstances() );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, trackedEntity );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackedEntityWithDeletedProgramInstancesUsingDeleteStrategy()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntityType( TEI_TYPE_ID )
            .build();
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( ctx.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( ctx.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithDeleteProgramInstances() );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, trackedEntity );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackedEntityUsingDeleteStrategyAndUserWithCascadeAuthority()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntityType( TEI_TYPE_ID )
            .build();
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( ctx.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( ctx.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithProgramInstances() );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        bundle.setUser( deleteTeiAuthorisedUser() );
        reporter = new ValidationErrorReporter( ctx, trackedEntity );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationFailsForTrackedEntityUsingDeleteStrategyAndUserWithoutCascadeAuthority()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntityType( TEI_TYPE_ID )
            .build();
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( ctx.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( ctx.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( ctx.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithProgramInstances() );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, trackedEntity );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1100 ) );
    }

    @Test
    void verifyValidationSuccessForEnrollment()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyCaptureScopeIsCheckedForEnrollmentCreation()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyCaptureScopeIsCheckedForEnrollmentDeletion()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( enrollmentUid )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyCaptureScopeIsCheckedForEnrollmentProgramWithoutRegistration()
    {
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        String enrollmentUid = CodeGenerator.generateUid();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( enrollmentUid )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForEnrollmentWithoutEventsUsingDeleteStrategy()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( ctx.programInstanceHasEvents( enrollment.getEnrollment() ) ).thenReturn( false );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForEnrollmentUsingDeleteStrategyAndUserWithCascadeAuthority()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( ctx.programInstanceHasEvents( enrollment.getEnrollment() ) ).thenReturn( true );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        bundle.setUser( deleteEnrollmentAuthorisedUser() );
        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationFailsForEnrollmentUsingDeleteStrategyAndUserWithoutCascadeAuthority()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( ctx.programInstanceHasEvents( enrollment.getEnrollment() ) ).thenReturn( true );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1103 ) );
    }

    @Test
    void verifyValidationSuccessForEventUsingDeleteStrategy()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .enrollment( enrollmentUid )
            .orgUnit( ORG_UNIT_ID )
            .programStage( PS_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.DELETE );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        when( ctx.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( ctx.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForNonTrackerEventUsingCreateStrategy()
    {
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .enrollment( enrollmentUid )
            .orgUnit( ORG_UNIT_ID )
            .programStage( PS_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( ctx.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );

        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackerEventCreation()
    {
        Event event = Event.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( ORG_UNIT_ID )
            .programStage( PS_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( ctx.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        when( ctx.getProgramInstance( event.getEnrollment() ) ).thenReturn( getEnrollment( null ) );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackerEventUpdate()
    {
        Event event = Event.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( ORG_UNIT_ID )
            .programStage( PS_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        when( ctx.getProgramInstance( event.getEnrollment() ) ).thenReturn( getEnrollment( null ) );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForEventUsingUpdateStrategy()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .enrollment( enrollmentUid )
            .orgUnit( ORG_UNIT_ID )
            .programStage( PS_ID )
            .program( PROGRAM_ID )
            .status( EventStatus.COMPLETED )
            .build();
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( ctx.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        when( ctx.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( ctx.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForEventUsingUpdateStrategyAndUserWithAuthority()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .enrollment( enrollmentUid )
            .orgUnit( ORG_UNIT_ID )
            .programStage( PS_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( ctx.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        when( ctx.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( ctx.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        bundle.setUser( changeCompletedEventAuthorisedUser() );
        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationFailsForEventUsingUpdateStrategyAndUserWithoutAuthority()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .enrollment( enrollmentUid )
            .orgUnit( ORG_UNIT_ID )
            .programStage( PS_ID )
            .program( PROGRAM_ID )
            .build();
        when( ctx.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        when( ctx.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( ctx.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );
        when( ctx.getProgram( PROGRAM_ID ) ).thenReturn( program );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( E1083 ) );
    }

    private TrackedEntityInstance getTEIWithNoProgramInstances()
    {
        TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstance.setUid( TEI_ID );
        trackedEntityInstance.setProgramInstances( Sets.newHashSet() );
        return trackedEntityInstance;
    }

    private TrackedEntityInstance getTEIWithDeleteProgramInstances()
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setDeleted( true );

        TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstance.setUid( TEI_ID );
        trackedEntityInstance.setProgramInstances( Sets.newHashSet( programInstance ) );

        return trackedEntityInstance;
    }

    private TrackedEntityInstance getTEIWithProgramInstances()
    {
        TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstance.setUid( TEI_ID );
        trackedEntityInstance.setProgramInstances( Sets.newHashSet( new ProgramInstance() ) );

        return trackedEntityInstance;
    }

    private ProgramInstance getEnrollment( String enrollmentUid )
    {
        if ( StringUtils.isEmpty( enrollmentUid ) )
        {
            enrollmentUid = CodeGenerator.generateUid();
        }
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( enrollmentUid );
        programInstance.setOrganisationUnit( organisationUnit );
        programInstance.setEntityInstance( getTEIWithNoProgramInstances() );
        programInstance.setProgram( program );
        programInstance.setStatus( ProgramStatus.ACTIVE );
        return programInstance;
    }

    private ProgramStageInstance getEvent()
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setOrganisationUnit( organisationUnit );
        programStageInstance.setProgramInstance( new ProgramInstance() );
        programStageInstance.setStatus( EventStatus.COMPLETED );
        return programStageInstance;
    }

    private User deleteTeiAuthorisedUser()
    {
        return createUser( 'A', Lists.newArrayList( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) );
    }

    private User deleteEnrollmentAuthorisedUser()
    {
        return createUser( 'A', Lists.newArrayList( Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority() ) );
    }

    private User changeCompletedEventAuthorisedUser()
    {
        return createUser( 'A', Lists.newArrayList( "F_UNCOMPLETE_EVENT" ) );
    }
}