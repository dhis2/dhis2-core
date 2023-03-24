/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.enrollment;

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.dxf2.events.EnrollmentEventsParams;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EnrollmentServiceTest extends TransactionalIntegrationTest
{
    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    protected UserService _userService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private IdentifiableObjectManager manager;

    private User admin;

    private User user;

    private User userWithoutOrgUnit;

    private Program programA;

    private ProgramStage programStageA;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceB;

    private ProgramStageInstance programStageInstanceA;

    private TrackedEntityInstance trackedEntityA;

    private TrackedEntityType trackedEntityTypeA;

    private TrackedEntityAttribute trackedEntityAttributeA;

    private RelationshipType relationshipTypeA;

    private Relationship relationshipA;

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
        admin = preCreateInjectAdminUser();

        OrganisationUnit orgUnitA = createOrganisationUnit( 'A' );
        manager.save( orgUnitA, false );
        OrganisationUnit orgUnitB = createOrganisationUnit( 'B' );
        manager.save( orgUnitB, false );
        OrganisationUnit orgUnitC = createOrganisationUnit( 'C' );
        manager.save( orgUnitC, false );

        user = createAndAddUser( false, "user", Set.of( orgUnitA ), Set.of( orgUnitA ),
            "F_EXPORT_DATA" );
        user.setTeiSearchOrganisationUnits( Set.of( orgUnitA, orgUnitB, orgUnitC ) );
        userWithoutOrgUnit = createUserWithAuth( "userWithoutOrgUnit" );

        trackedEntityTypeA = createTrackedEntityType( 'A' );
        trackedEntityTypeA.getSharing().setOwner( user );
        manager.save( trackedEntityTypeA, false );

        trackedEntityA = createTrackedEntityInstance( orgUnitA );
        trackedEntityA.setTrackedEntityType( trackedEntityTypeA );
        manager.save( trackedEntityA, false );

        TrackedEntityInstance trackedEntityB = createTrackedEntityInstance( orgUnitB );
        trackedEntityB.setTrackedEntityType( trackedEntityTypeA );
        manager.save( trackedEntityB, false );

        TrackedEntityInstance trackedEntityC = createTrackedEntityInstance( orgUnitC );
        trackedEntityC.setTrackedEntityType( trackedEntityTypeA );
        manager.save( trackedEntityC, false );

        programA = createProgram( 'A', new HashSet<>(), orgUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        programA.setTrackedEntityType( trackedEntityTypeA );
        programA.getSharing().setOwner( admin );
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.save( programA, false );

        trackedEntityAttributeA = createTrackedEntityAttribute( 'A' );
        trackedEntityAttributeA.getSharing().setOwner( admin );
        manager.save( trackedEntityAttributeA, false );
        TrackedEntityAttributeValue trackedEntityAttributeValueA = new TrackedEntityAttributeValue();
        trackedEntityAttributeValueA.setAttribute( trackedEntityAttributeA );
        trackedEntityAttributeValueA.setEntityInstance( trackedEntityA );
        trackedEntityAttributeValueA.setValue( "12" );
        trackedEntityA.setTrackedEntityAttributeValues( Set.of( trackedEntityAttributeValueA ) );
        manager.update( trackedEntityA );
        programA.setProgramAttributes(
            List.of( createProgramTrackedEntityAttribute( programA, trackedEntityAttributeA ) ) );
        manager.update( programA );

        programStageA = createProgramStage( 'A', programA );
        manager.save( programStageA, false );
        ProgramStage inaccessibleProgramStage = createProgramStage( 'B', programA );
        inaccessibleProgramStage.getSharing().setOwner( admin );
        inaccessibleProgramStage.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( inaccessibleProgramStage, false );
        programA.setProgramStages( Set.of( programStageA, inaccessibleProgramStage ) );
        manager.save( programA, false );

        relationshipTypeA = createRelationshipType( 'A' );
        relationshipTypeA.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipTypeA.getFromConstraint().setTrackedEntityType( trackedEntityTypeA );
        relationshipTypeA.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        relationshipTypeA.getToConstraint().setProgram( programA );
        relationshipTypeA.getSharing().setOwner( user );
        manager.save( relationshipTypeA, false );

        relationshipA = new Relationship();
        relationshipA.setUid( CodeGenerator.generateUid() );
        relationshipA.setRelationshipType( relationshipTypeA );
        RelationshipItem from = new RelationshipItem();
        from.setTrackedEntityInstance( trackedEntityA );
        from.setRelationship( relationshipA );
        relationshipA.setFrom( from );
        RelationshipItem to = new RelationshipItem();
        to.setProgramInstance( programInstanceA );
        to.setRelationship( relationshipA );
        relationshipA.setTo( to );
        relationshipA.setKey( RelationshipUtils.generateRelationshipKey( relationshipA ) );
        relationshipA.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationshipA ) );
        manager.save( relationshipA, false );

        programInstanceA = programInstanceService.enrollTrackedEntityInstance( trackedEntityA, programA, new Date(),
            new Date(),
            orgUnitA );
        programStageInstanceA = new ProgramStageInstance();
        programStageInstanceA.setProgramInstance( programInstanceA );
        programStageInstanceA.setProgramStage( programStageA );
        programStageInstanceA.setOrganisationUnit( orgUnitA );
        manager.save( programStageInstanceA, false );
        programInstanceA.setProgramStageInstances( Set.of( programStageInstanceA ) );
        programInstanceA.setRelationshipItems( Set.of( from, to ) );
        manager.save( programInstanceA, false );

        programInstanceB = programInstanceService.enrollTrackedEntityInstance( trackedEntityB, programA, new Date(),
            new Date(),
            orgUnitB );

        injectSecurityContext( user );
    }

    @Test
    void shouldGetEnrollmentWhenUserHasReadWriteAccessToProgramAndAccessToOrgUnit()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.updateNoAcl( programA );

        ProgramInstance enrollment = enrollmentService.getEnrollment( programInstanceA.getUid(),
            EnrollmentParams.FALSE );

        assertNotNull( enrollment );
        assertEquals( programInstanceA.getUid(), enrollment.getUid() );
    }

    @Test
    void shouldGetEnrollmentWhenUserHasReadAccessToProgramAndAccessToOrgUnit()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.updateNoAcl( programA );

        ProgramInstance enrollment = enrollmentService.getEnrollment( programInstanceA.getUid(),
            EnrollmentParams.FALSE );

        assertNotNull( enrollment );
        assertEquals( programInstanceA.getUid(), enrollment.getUid() );
    }

    @Test
    void shouldGetEnrollmentWithEventsWhenUserHasAccessToEvent()
    {
        EnrollmentParams params = EnrollmentParams.FALSE;
        params = params.withEnrollmentEventsParams( EnrollmentEventsParams.TRUE );

        ProgramInstance enrollment = enrollmentService.getEnrollment( programInstanceA.getUid(), params );

        assertNotNull( enrollment );
        assertContainsOnly( List.of( programStageInstanceA.getUid() ), enrollment.getProgramStageInstances().stream()
            .map( ProgramStageInstance::getUid ).collect( Collectors.toList() ) );
    }

    @Test
    void shouldGetEnrollmentWithoutEventsWhenUserHasNoAccessToProgramStage()
    {
        programStageA.getSharing().setOwner( admin );
        programStageA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.updateNoAcl( programStageA );

        EnrollmentParams params = EnrollmentParams.FALSE;
        params = params.withIncludeEvents( true );

        ProgramInstance enrollment = enrollmentService.getEnrollment( programInstanceA.getUid(), params );

        assertNotNull( enrollment );
        assertIsEmpty( enrollment.getProgramStageInstances() );
    }

    @Test
    void shouldGetEnrollmentWithRelationshipsWhenUserHasAccessToThem()
    {
        EnrollmentParams params = EnrollmentParams.FALSE;
        params = params.withIncludeRelationships( true );

        ProgramInstance enrollment = enrollmentService.getEnrollment( programInstanceA.getUid(), params );

        assertNotNull( enrollment );
        assertContainsOnly( Set.of( relationshipA.getUid() ), relationshipUids( enrollment ) );
    }

    @Test
    void shouldGetEnrollmentWithoutRelationshipsWhenUserHasAccessToThem()
    {
        relationshipTypeA.getSharing().setOwner( admin );
        relationshipTypeA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );

        EnrollmentParams params = EnrollmentParams.FALSE;
        params = params.withIncludeRelationships( true );

        ProgramInstance enrollment = enrollmentService.getEnrollment( programInstanceA.getUid(), params );

        assertNotNull( enrollment );
        assertIsEmpty( enrollment.getRelationshipItems() );
    }

    @Test
    void shouldGetEnrollmentWithAttributesWhenUserHasAccessToThem()
    {
        EnrollmentParams params = EnrollmentParams.FALSE;
        params = params.withIncludeAttributes( true );

        ProgramInstance enrollment = enrollmentService.getEnrollment( programInstanceA.getUid(), params );

        assertNotNull( enrollment );
        assertContainsOnly( List.of( trackedEntityAttributeA.getUid() ), attributeUids( enrollment ) );
    }

    @Test
    void shouldFailGettingEnrollmentWhenUserHasNoAccessToProgramsTrackedEntityType()
    {
        trackedEntityTypeA.getSharing().setOwner( admin );
        trackedEntityTypeA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.updateNoAcl( trackedEntityTypeA );

        IllegalQueryException exception = assertThrows( IllegalQueryException.class,
            () -> enrollmentService.getEnrollment( programInstanceA.getUid(), EnrollmentParams.FALSE ) );
        assertContains( "access to tracked entity type", exception.getMessage() );
    }

    @Test
    void shouldFailGettingEnrollmentWhenUserHasReadAccessToProgramButNoAccessToOrgUnit()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.updateNoAcl( programA );

        injectSecurityContext( userWithoutOrgUnit );

        IllegalQueryException exception = assertThrows( IllegalQueryException.class,
            () -> enrollmentService.getEnrollment( programInstanceA.getUid(), EnrollmentParams.FALSE ) );
        assertContains( "OWNERSHIP_ACCESS_DENIED", exception.getMessage() );
    }

    @Test
    void shouldFailGettingEnrollmentWhenUserHasReadWriteAccessToProgramButNoAccessToOrgUnit()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.updateNoAcl( programA );

        injectSecurityContext( userWithoutOrgUnit );

        IllegalQueryException exception = assertThrows( IllegalQueryException.class,
            () -> enrollmentService.getEnrollment( programInstanceA.getUid(), EnrollmentParams.FALSE ) );
        assertContains( "OWNERSHIP_ACCESS_DENIED", exception.getMessage() );
    }

    @Test
    void shouldFailGettingEnrollmentWhenUserHasNoAccessToProgramButAccessToOrgUnit()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.updateNoAcl( programA );

        IllegalQueryException exception = assertThrows( IllegalQueryException.class,
            () -> enrollmentService.getEnrollment( programInstanceA.getUid(), EnrollmentParams.FALSE ) );
        assertContains( "access to program", exception.getMessage() );
    }

    @Test
    void shouldGetEnrollmentsWhenUserHasReadAccessToProgramAndSearchScopeAccessToOrgUnit()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.updateNoAcl( programA );

        ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
        params.setProgram( programA );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ACCESSIBLE );
        params.setUser( user );

        Enrollments enrollments = enrollmentService.getEnrollments( params );

        assertNotNull( enrollments );
        assertContainsOnly( List.of( programInstanceA.getUid(), programInstanceB.getUid() ), toUid( enrollments ) );
    }

    @Test
    void shouldGetEnrollmentsByTrackedEntityWhenUserHasAccessToTrackedEntityType()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.updateNoAcl( programA );

        ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
        params.setOrganisationUnits( Set.of( trackedEntityA.getOrganisationUnit() ) );
        params.setTrackedEntityInstanceUid( trackedEntityA.getUid() );
        params.setUser( user );

        Enrollments enrollments = enrollmentService.getEnrollments( params );

        assertNotNull( enrollments );
        assertContainsOnly( List.of( programInstanceA.getUid() ), toUid( enrollments ) );
    }

    @Test
    void shouldFailGettingEnrollmentsByTrackedEntityWhenUserHasNoAccessToTrackedEntityType()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.updateNoAcl( programA );

        trackedEntityTypeA.getSharing().setOwner( admin );
        trackedEntityTypeA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.updateNoAcl( trackedEntityTypeA );

        ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
        params.setOrganisationUnits( Set.of( trackedEntityA.getOrganisationUnit() ) );
        params.setTrackedEntityInstanceUid( trackedEntityA.getUid() );
        params.setUser( user );

        IllegalQueryException exception = assertThrows( IllegalQueryException.class,
            () -> enrollmentService.getEnrollments( params ) );
        assertContains( "access to tracked entity type", exception.getMessage() );
    }

    private static List<String> toUid( Enrollments enrollments )
    {
        return enrollments.getEnrollments().stream()
            .map( ProgramInstance::getUid )
            .collect( Collectors.toList() );
    }

    private static List<String> attributeUids( ProgramInstance programInstance )
    {
        return programInstance.getEntityInstance().getTrackedEntityAttributeValues().stream()
            .map( v -> v.getAttribute().getUid() )
            .collect( Collectors.toList() );
    }

    private static Set<String> relationshipUids( ProgramInstance programInstance )
    {
        return programInstance.getRelationshipItems().stream()
            .map( r -> r.getRelationship().getUid() )
            .collect( Collectors.toSet() );
    }
}
