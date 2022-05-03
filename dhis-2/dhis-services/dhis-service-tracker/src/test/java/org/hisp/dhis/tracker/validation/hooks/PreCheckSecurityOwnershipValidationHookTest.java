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

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1000;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1001;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1003;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1083;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1091;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1100;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1103;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1104;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

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
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
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
        when( bundle.getUser() ).thenReturn( user );

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
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( preheat.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( organisationUnitService.isInUserSearchHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackedEntityWithNoProgramInstancesUsingDeleteStrategy()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithNoProgramInstances() );

        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyCaptureScopeIsCheckedForTrackedEntityCreation()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( preheat.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifySearchScopeIsCheckedForTrackedEntityUpdate()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( preheat.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( organisationUnitService.isInUserSearchHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyCaptureScopeIsCheckedForTrackedEntityDeletion()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithNoProgramInstances() );

        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackedEntityWithDeletedProgramInstancesUsingDeleteStrategy()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithDeleteProgramInstances() );

        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackedEntityUsingDeleteStrategyAndUserWithCascadeAuthority()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getUser() ).thenReturn( deleteTeiAuthorisedUser() );
        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithProgramInstances() );

        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationFailsForTrackedEntityUsingDeleteStrategyAndUserWithoutCascadeAuthority()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( bundle.getTrackedEntityInstance( TEI_ID ) ).thenReturn( getTEIWithProgramInstances() );

        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        hasTrackerError( reporter, E1100, TrackerType.TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyValidationFailsForTrackedEntityWithUserNotInOrgUnitCaptureScopeHierarchy()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( preheat.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( false );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        hasTrackerError( reporter, E1000, TrackerType.TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyValidationFailsForTrackedEntityUpdateWithUserNotInOrgUnitSearchHierarchy()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( preheat.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( organisationUnitService.isInUserSearchHierarchyCached( user, organisationUnit ) )
            .thenReturn( false );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        hasTrackerError( reporter, E1003, TrackerType.TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyValidationFailsForTrackedEntityAndUserWithoutWriteAccess()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntityType( TEI_TYPE_ID )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( preheat.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );
        when( bundle.getStrategy( trackedEntity ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( organisationUnitService.isInUserSearchHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityType ) ).thenReturn( false );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateTrackedEntity( reporter, trackedEntity );

        hasTrackerError( reporter, E1001, TrackerType.TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyValidationSuccessForEnrollmentWhenProgramInstanceHasNoOrgUnitAssigned()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.UPDATE );

        ProgramInstance programInstance = getEnrollment( enrollment.getEnrollment() );
        programInstance.setOrganisationUnit( null );

        when( bundle.getProgramInstance( enrollment.getEnrollment() ) )
            .thenReturn( programInstance );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
        verify( organisationUnitService, times( 0 ) ).isInUserHierarchyCached( user, organisationUnit );

        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
        verify( organisationUnitService, times( 0 ) ).isInUserHierarchyCached( user, organisationUnit );
    }

    @Test
    void verifyValidationSuccessForEnrollment()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_ID ) ) ).thenReturn( program );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyCaptureScopeIsCheckedForEnrollmentCreation()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_ID ) ) ).thenReturn( program );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyCaptureScopeIsCheckedForEnrollmentDeletion()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( enrollmentUid )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getProgramInstance( enrollment.getEnrollment() ) )
            .thenReturn( getEnrollment( enrollment.getEnrollment() ) );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

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
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_ID ) ) ).thenReturn( program );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForEnrollmentWithoutEventsUsingDeleteStrategy()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent() ).thenReturn( Collections.emptyList() );
        when( bundle.getProgramInstance( enrollment.getEnrollment() ) )
            .thenReturn( getEnrollment( enrollment.getEnrollment() ) );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForEnrollmentUsingDeleteStrategyAndUserWithCascadeAuthority()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getUser() ).thenReturn( deleteEnrollmentAuthorisedUser() );
        when( preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent() )
            .thenReturn( Collections.singletonList( enrollment.getEnrollment() ) );
        when( bundle.getProgramInstance( enrollment.getEnrollment() ) )
            .thenReturn( getEnrollment( enrollment.getEnrollment() ) );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationFailsForEnrollmentWithoutEventsUsingDeleteStrategyAndUserNotInOrgUnitHierarchy()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent() ).thenReturn( Collections.emptyList() );
        when( bundle.getProgramInstance( enrollment.getEnrollment() ) )
            .thenReturn( getEnrollment( enrollment.getEnrollment() ) );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( false );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        hasTrackerError( reporter, E1000, TrackerType.ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyValidationFailsForEnrollmentUsingDeleteStrategyAndUserWithoutCascadeAuthority()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent() )
            .thenReturn( Collections.singletonList( enrollment.getEnrollment() ) );
        when( bundle.getProgramInstance( enrollment.getEnrollment() ) )
            .thenReturn( getEnrollment( enrollment.getEnrollment() ) );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        hasTrackerError( reporter, E1103, TrackerType.ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyValidationFailsForEnrollmentDeletionAndUserWithoutProgramWriteAccess()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( enrollmentUid )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( bundle.getProgramInstance( enrollment.getEnrollment() ) )
            .thenReturn( getEnrollment( enrollment.getEnrollment() ) );
        when( aclService.canDataWrite( user, program ) ).thenReturn( false );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        hasTrackerError( reporter, E1091, TrackerType.ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyValidationFailsForEnrollmentDeletionAndUserWithoutTrackedEntityTypeReadAccess()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( enrollmentUid )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.DELETE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( bundle.getProgramInstance( enrollment.getEnrollment() ) )
            .thenReturn( getEnrollment( enrollment.getEnrollment() ) );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( false );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEnrollment( reporter, enrollment );

        hasTrackerError( reporter, E1104, TrackerType.ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyValidationSuccessForEventUsingDeleteStrategy()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .enrollment( enrollmentUid )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.DELETE );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );

        when( bundle.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );

        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

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
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_ID ) ) ).thenReturn( program );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataWrite( user, program ) ).thenReturn( true );

        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackerEventCreation()
    {
        Event event = Event.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( getEnrollment( null ) );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_ID ) ) ).thenReturn( program );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForTrackerEventUpdate()
    {
        Event event = Event.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( getEnrollment( null ) );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_ID ) ) ).thenReturn( program );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( true );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForEventUsingUpdateStrategy()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .enrollment( enrollmentUid )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .status( EventStatus.COMPLETED )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        when( bundle.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );

        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForEventUsingUpdateStrategyOutsideCaptureScopeWithBrokenGlass()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        String eventUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .event( eventUid )
            .enrollment( enrollmentUid )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .status( EventStatus.COMPLETED )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        when( bundle.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );
        when( preheat.getProgramOwner() )
            .thenReturn( Collections.singletonMap( TEI_ID, Collections.singletonMap( PROGRAM_ID,
                new TrackedEntityProgramOwnerOrgUnit( TEI_ID, PROGRAM_ID, organisationUnit ) ) ) );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );
        when( ownershipAccessManager.hasAccess( user, TEI_ID, organisationUnit, program ) ).thenReturn( false );
        validatorToTest.validateEvent( reporter, event );

        hasTrackerError( reporter, TrackerErrorCode.E1102, TrackerType.EVENT, event.getUid() );

        when( ownershipAccessManager.hasAccess( user, TEI_ID, organisationUnit, program ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );
        validatorToTest.validateEvent( reporter, event );
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationSuccessForEventUsingUpdateStrategyAndUserWithAuthority()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .enrollment( enrollmentUid )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( bundle.getUser() ).thenReturn( changeCompletedEventAuthorisedUser() );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );

        when( bundle.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );

        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationFailsForTrackerEventCreationAndUserNotInOrgUnitCaptureScope()
    {
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( getEnrollment( null ) );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_ID ) ) ).thenReturn( program );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnit ) )
            .thenReturn( false );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEvent( reporter, event );

        hasTrackerError( reporter, E1000, TrackerType.EVENT, event.getUid() );
    }

    @Test
    void verifyValidationFailsForEventCreationThatIsCreatableInSearchScopeAndUserNotInOrgUnitSearchHierarchy()
    {
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .status( EventStatus.SCHEDULE )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( getEnrollment( null ) );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_ID ) ) ).thenReturn( program );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( organisationUnit );
        when( organisationUnitService.isInUserSearchHierarchyCached( user, organisationUnit ) )
            .thenReturn( false );
        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEvent( reporter, event );

        hasTrackerError( reporter, E1000, TrackerType.EVENT, event.getUid() );
    }

    @Test
    void verifyValidationFailsForEventUsingUpdateStrategyAndUserWithoutAuthority()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .enrollment( enrollmentUid )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );
        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        when( bundle.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );

        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );
        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEvent( reporter, event );

        hasTrackerError( reporter, E1083, TrackerType.EVENT, event.getUid() );
    }

    @Test
    void verifySuccessEventValidationWhenProgramStageInstanceHasNoOrgUnitAssigned()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .enrollment( enrollmentUid )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .programStage( MetadataIdentifier.ofUid( PS_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_ID ) )
            .status( EventStatus.COMPLETED )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );
        ProgramInstance programInstance = getEnrollment( enrollmentUid );

        ProgramStageInstance programStageInstance = getEvent();
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setOrganisationUnit( null );

        when( bundle.getProgramStageInstance( event.getEvent() ) ).thenReturn( programStageInstance );
        when( bundle.getProgramInstance( event.getEnrollment() ) ).thenReturn( programInstance );

        when( aclService.canDataRead( user, program.getTrackedEntityType() ) ).thenReturn( true );
        when( aclService.canDataRead( user, program ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programStage ) ).thenReturn( true );

        reporter = new ValidationErrorReporter( bundle );

        validatorToTest.validateEvent( reporter, event );

        verify( organisationUnitService, times( 0 ) ).isInUserHierarchyCached( user, organisationUnit );

        assertFalse( reporter.hasErrors() );
    }

    private TrackedEntityInstance getTEIWithNoProgramInstances()
    {
        TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstance.setUid( TEI_ID );
        trackedEntityInstance.setProgramInstances( Sets.newHashSet() );
        trackedEntityInstance.setTrackedEntityType( trackedEntityType );

        return trackedEntityInstance;
    }

    private TrackedEntityInstance getTEIWithDeleteProgramInstances()
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setDeleted( true );

        TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstance.setUid( TEI_ID );
        trackedEntityInstance.setProgramInstances( Sets.newHashSet( programInstance ) );
        trackedEntityInstance.setTrackedEntityType( trackedEntityType );

        return trackedEntityInstance;
    }

    private TrackedEntityInstance getTEIWithProgramInstances()
    {
        TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstance.setUid( TEI_ID );
        trackedEntityInstance.setProgramInstances( Sets.newHashSet( new ProgramInstance() ) );
        trackedEntityInstance.setTrackedEntityType( trackedEntityType );

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