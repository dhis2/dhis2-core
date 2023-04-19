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
package org.hisp.dhis.tracker.trackedentity;

import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luciano Fiandesio
 */
class TrackedEntityServiceTest extends IntegrationTestBase
{
    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    protected UserService _userService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private TrackerOwnershipManager trackerOwnershipManager;

    @Autowired
    private CurrentUserService currentUserService;

    private User user;

    private User admin;

    private OrganisationUnit orgUnitA;

    private OrganisationUnit orgUnitB;

    private TrackedEntityAttribute teaA;

    private TrackedEntityType trackedEntityTypeA;

    private Program programA;

    private Program programB;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceB;

    private ProgramStageInstance programStageInstanceA;

    private ProgramStageInstance programStageInstanceB;

    private TrackedEntityInstance trackedEntityA;

    private TrackedEntityInstance trackedEntityB;

    private TrackedEntityComment note1;

    private CategoryOptionCombo defaultCategoryOptionCombo;

    private Relationship relationshipA;

    private Relationship relationshipB;

    private Relationship relationshipC;

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
        admin = preCreateInjectAdminUser();

        orgUnitA = createOrganisationUnit( 'A' );
        manager.save( orgUnitA, false );
        orgUnitB = createOrganisationUnit( 'B' );
        manager.save( orgUnitB, false );
        OrganisationUnit orgUnitC = createOrganisationUnit( 'C' );
        manager.save( orgUnitC, false );

        user = createAndAddUser( false, "user", Set.of( orgUnitA ), Set.of( orgUnitA ),
            "F_EXPORT_DATA" );
        user.setTeiSearchOrganisationUnits( Set.of( orgUnitA, orgUnitB, orgUnitC ) );

        teaA = createTrackedEntityAttribute( 'A', ValueType.TEXT );
        manager.save( teaA, false );
        TrackedEntityAttribute teaB = createTrackedEntityAttribute( 'B', ValueType.TEXT );
        manager.save( teaB, false );
        TrackedEntityAttribute teaC = createTrackedEntityAttribute( 'C', ValueType.TEXT );
        manager.save( teaC, false );
        TrackedEntityAttribute teaD = createTrackedEntityAttribute( 'D', ValueType.TEXT );
        manager.save( teaD, false );
        TrackedEntityAttribute teaE = createTrackedEntityAttribute( 'E', ValueType.TEXT );
        manager.save( teaE, false );

        trackedEntityTypeA = createTrackedEntityType( 'A' );
        trackedEntityTypeA.getTrackedEntityTypeAttributes()
            .addAll( List.of( new TrackedEntityTypeAttribute( trackedEntityTypeA, teaA ),
                new TrackedEntityTypeAttribute( trackedEntityTypeA, teaB ) ) );
        trackedEntityTypeA.getSharing().setOwner( user );
        trackedEntityTypeA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.save( trackedEntityTypeA, false );

        CategoryCombo defaultCategoryCombo = manager.getByName( CategoryCombo.class, "default" );
        assertNotNull( defaultCategoryCombo );
        defaultCategoryOptionCombo = manager.getByName( CategoryOptionCombo.class, "default" );
        assertNotNull( defaultCategoryOptionCombo );

        programA = createProgram( 'A', new HashSet<>(), orgUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        programA.setTrackedEntityType( trackedEntityTypeA );
        programA.setCategoryCombo( defaultCategoryCombo );
        manager.save( programA, false );
        ProgramStage programStageA1 = createProgramStage( programA );
        programStageA1.setPublicAccess( AccessStringHelper.FULL );
        manager.save( programStageA1, false );
        ProgramStage programStageA2 = createProgramStage( programA );
        programStageA2.setPublicAccess( AccessStringHelper.FULL );
        manager.save( programStageA2, false );
        programA.setProgramStages(
            Stream.of( programStageA1, programStageA2 ).collect( Collectors.toCollection( HashSet::new ) ) );
        programA.getSharing().setOwner( admin );
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        programA.getProgramAttributes().add( new ProgramTrackedEntityAttribute( programA, teaC ) );
        manager.update( programA );

        User currentUser = currentUserService.getCurrentUser();

        programB = createProgram( 'B', new HashSet<>(), orgUnitA );
        programB.setProgramType( ProgramType.WITH_REGISTRATION );
        programB.setTrackedEntityType( trackedEntityTypeA );
        programB.setCategoryCombo( defaultCategoryCombo );
        programB.setAccessLevel( AccessLevel.PROTECTED );
        programB.getSharing()
            .addUserAccess( new org.hisp.dhis.user.sharing.UserAccess( currentUser, AccessStringHelper.FULL ) );
        manager.save( programB, false );
        ProgramStage programStageB1 = createProgramStage( programB );
        programStageB1.setPublicAccess( AccessStringHelper.FULL );
        manager.save( programStageB1, false );
        ProgramStage programStageB2 = createProgramStage( programB );
        programStageB2.setPublicAccess( AccessStringHelper.FULL );
        manager.save( programStageB2, false );
        programB.setProgramStages(
            Stream.of( programStageB1, programStageB2 ).collect( Collectors.toCollection( HashSet::new ) ) );
        programB.getSharing().setOwner( admin );
        programB.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programB );

        programB.getProgramAttributes().addAll( List.of( new ProgramTrackedEntityAttribute( programB, teaD ),
            new ProgramTrackedEntityAttribute( programB, teaE ) ) );
        manager.update( programB );

        trackedEntityA = createTrackedEntityInstance( orgUnitA );
        trackedEntityA.setTrackedEntityType( trackedEntityTypeA );
        manager.save( trackedEntityA, false );

        programInstanceA = programInstanceService.enrollTrackedEntityInstance( trackedEntityA, programA, new Date(),
            new Date(), orgUnitA );
        programStageInstanceA = new ProgramStageInstance();
        programStageInstanceA.setProgramInstance( programInstanceA );
        programStageInstanceA.setProgramStage( programStageA1 );
        programStageInstanceA.setOrganisationUnit( orgUnitA );
        programStageInstanceA.setAttributeOptionCombo( defaultCategoryOptionCombo );
        programStageInstanceA.setDueDate( parseDate( "2021-02-27T12:05:00.000" ) );
        programStageInstanceA.setCompletedDate( parseDate( "2021-02-27T11:05:00.000" ) );
        programStageInstanceA.setCompletedBy( "herb" );
        programStageInstanceA.setAssignedUser( user );
        note1 = new TrackedEntityComment( "note1", "ant" );
        note1.setUid( CodeGenerator.generateUid() );
        note1.setCreated( new Date() );
        note1.setLastUpdated( new Date() );
        programStageInstanceA.setComments( List.of( note1 ) );
        manager.save( programStageInstanceA, false );
        programInstanceA.setProgramStageInstances( Set.of( programStageInstanceA ) );
        programInstanceA.setFollowup( true );
        manager.save( programInstanceA, false );

        programInstanceB = programInstanceService.enrollTrackedEntityInstance( trackedEntityA, programB, new Date(),
            new Date(), orgUnitA );
        programStageInstanceB = new ProgramStageInstance();
        programStageInstanceB.setProgramInstance( programInstanceB );
        programStageInstanceB.setProgramStage( programStageB1 );
        programStageInstanceB.setOrganisationUnit( orgUnitA );
        programStageInstanceB.setAttributeOptionCombo( defaultCategoryOptionCombo );
        manager.save( programStageInstanceB, false );
        programInstanceB.setProgramStageInstances( Set.of( programStageInstanceB ) );
        manager.save( programInstanceB, false );

        trackedEntityB = createTrackedEntityInstance( orgUnitB );
        trackedEntityB.setTrackedEntityType( trackedEntityTypeA );
        manager.save( trackedEntityB, false );

        TrackedEntityInstance trackedEntityC = createTrackedEntityInstance( orgUnitC );
        trackedEntityC.setTrackedEntityType( trackedEntityTypeA );
        manager.save( trackedEntityC, false );

        trackerOwnershipManager.assignOwnership( trackedEntityA, programA, orgUnitA, true, true );
        trackerOwnershipManager.assignOwnership( trackedEntityA, programB, orgUnitA, true, true );

        attributeValueService
            .addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( teaA, trackedEntityA, "A" ) );
        attributeValueService
            .addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( teaB, trackedEntityA, "B" ) );
        attributeValueService
            .addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( teaC, trackedEntityA, "C" ) );
        attributeValueService
            .addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( teaE, trackedEntityA, "E" ) );

        RelationshipType relationshipTypeA = createRelationshipType( 'A' );
        relationshipTypeA.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipTypeA.getFromConstraint().setTrackedEntityType( trackedEntityTypeA );
        relationshipTypeA.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipTypeA.getToConstraint().setTrackedEntityType( trackedEntityTypeA );
        relationshipTypeA.getSharing().setOwner( user );
        manager.save( relationshipTypeA, false );

        relationshipA = new Relationship();
        relationshipA.setUid( CodeGenerator.generateUid() );
        relationshipA.setRelationshipType( relationshipTypeA );
        RelationshipItem fromA = new RelationshipItem();
        fromA.setTrackedEntityInstance( trackedEntityA );
        fromA.setRelationship( relationshipA );
        relationshipA.setFrom( fromA );
        RelationshipItem toA = new RelationshipItem();
        toA.setTrackedEntityInstance( trackedEntityB );
        toA.setRelationship( relationshipA );
        relationshipA.setTo( toA );
        relationshipA.setKey( RelationshipUtils.generateRelationshipKey( relationshipA ) );
        relationshipA.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationshipA ) );
        manager.save( relationshipA, false );

        RelationshipType relationshipTypeB = createRelationshipType( 'B' );
        relationshipTypeB.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipTypeB.getFromConstraint().setTrackedEntityType( trackedEntityTypeA );
        relationshipTypeB.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        relationshipTypeB.getToConstraint().setProgram( programA );
        relationshipTypeB.getSharing().setOwner( user );
        manager.save( relationshipTypeB, false );

        relationshipB = new Relationship();
        relationshipB.setUid( CodeGenerator.generateUid() );
        relationshipB.setRelationshipType( relationshipTypeB );
        RelationshipItem fromB = new RelationshipItem();
        fromB.setTrackedEntityInstance( trackedEntityA );
        fromB.setRelationship( relationshipB );
        relationshipB.setFrom( fromB );
        RelationshipItem toB = new RelationshipItem();
        toB.setProgramInstance( programInstanceA );
        toB.setRelationship( relationshipB );
        relationshipB.setTo( toB );
        relationshipB.setKey( RelationshipUtils.generateRelationshipKey( relationshipB ) );
        relationshipB.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationshipB ) );
        manager.save( relationshipB, false );

        RelationshipType relationshipTypeC = createRelationshipType( 'C' );
        relationshipTypeC.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipTypeC.getFromConstraint().setTrackedEntityType( trackedEntityTypeA );
        relationshipTypeC.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        relationshipTypeC.getToConstraint().setProgramStage( programStageA1 );
        relationshipTypeC.getSharing().setOwner( user );
        manager.save( relationshipTypeC, false );

        relationshipC = new Relationship();
        relationshipC.setUid( CodeGenerator.generateUid() );
        relationshipC.setRelationshipType( relationshipTypeC );
        RelationshipItem fromC = new RelationshipItem();
        fromC.setTrackedEntityInstance( trackedEntityA );
        fromC.setRelationship( relationshipC );
        relationshipC.setFrom( fromC );
        RelationshipItem toC = new RelationshipItem();
        toC.setProgramStageInstance( programStageInstanceA );
        toC.setRelationship( relationshipC );
        relationshipC.setTo( toC );
        relationshipC.setKey( RelationshipUtils.generateRelationshipKey( relationshipC ) );
        relationshipC.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationshipC ) );
        manager.save( relationshipC, false );

        injectSecurityContext( user );
    }

    @Test
    void shouldReturnEmptyCollectionGivenUserHasNoAccessToTrackedEntityType()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );

        trackedEntityTypeA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        trackedEntityTypeA.getSharing().setOwner( admin );
        manager.updateNoAcl( trackedEntityA );

        final List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.FALSE );

        assertIsEmpty( trackedEntities );
    }

    @Test
    void shouldReturnTrackedEntitiesGivenUserHasDataReadAccessToTrackedEntityType()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA, orgUnitB ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );

        final List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.FALSE );

        assertContainsOnly( List.of( trackedEntityA, trackedEntityB ), trackedEntities );
    }

    @Test
    void shouldIncludeAllAttributesOfGivenTrackedEntity()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setIncludeAllAttributes( true );
        queryParams.setTrackedEntityInstanceUids( Set.of( trackedEntityA.getUid() ) );

        final List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.FALSE );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );
        assertContainsOnly( Set.of( "A", "B", "C", "E" ),
            attributeNames( trackedEntities.get( 0 ).getTrackedEntityAttributeValues() ) );
    }

    @Test
    void shouldReturnTrackedEntityIncludingAllAttributesEnrollmentsEventsRelationshipsOwners()
        throws ForbiddenException,
        NotFoundException
    {
        // this was declared as "remove ownership"; unclear to me how this is removing ownership
        trackerOwnershipManager.assignOwnership( trackedEntityA, programB, orgUnitB, true, true );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );

        final List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.TRUE );

        assertContainsOnly( List.of( trackedEntityA.getUid() ), uids( trackedEntities ) );
        assertContainsOnly( Set.of( programInstanceA.getUid() ),
            uids( trackedEntities.get( 0 ).getProgramInstances() ) );

        assertAll(
            () -> assertEquals( 2, trackedEntities.get( 0 ).getProgramOwners().size() ),
            () -> assertContainsOnly( Set.of( trackedEntityA.getUid() ),
                trackedEntities.get( 0 ).getProgramOwners().stream().map( po -> po.getEntityInstance().getUid() )
                    .collect( Collectors.toSet() ) ),
            () -> assertContainsOnly( Set.of( orgUnitA.getUid(), orgUnitB.getUid() ),
                trackedEntities.get( 0 ).getProgramOwners().stream().map( po -> po.getOrganisationUnit().getUid() )
                    .collect( Collectors.toSet() ) ),
            () -> assertContainsOnly( Set.of( programA.getUid(), programB.getUid() ), trackedEntities.get( 0 )
                .getProgramOwners().stream().map( po -> po.getProgram().getUid() ).collect( Collectors.toSet() ) ) );
    }

    @Test
    void shouldReturnTrackedEntityIncludeAllAttributesInProtectedProgramNoAccess()
        throws ForbiddenException,
        NotFoundException
    {
        // this was declared as "remove ownership"; unclear to me how this is removing ownership
        trackerOwnershipManager.assignOwnership( trackedEntityA, programB, orgUnitB, true, true );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );

        final List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.FALSE );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );
        assertContainsOnly( Set.of( "A", "B", "C" ),
            attributeNames( trackedEntities.get( 0 ).getTrackedEntityAttributeValues() ) );
    }

    @Test
    void shouldReturnTrackedEntityIncludeSpecificProtectedProgram()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setProgram( programB );

        final List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.FALSE );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );
        assertContainsOnly( Set.of( "A", "B", "E" ),
            attributeNames( trackedEntities.get( 0 ).getTrackedEntityAttributeValues() ) );
    }

    @Test
    void shouldTrackedEntityIncludeSpecificOpenProgram()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setProgram( programA );
        TrackedEntityParams params = TrackedEntityParams.FALSE;

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams, params );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );
        assertContainsOnly( Set.of( "A", "B", "C" ),
            attributeNames( trackedEntities.get( 0 ).getTrackedEntityAttributeValues() ) );
    }

    @Test
    void shouldReturnEmptyCollectionGivenSingleQuoteInAttributeSearchInput()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams
            .addFilter( new QueryItem( teaA, QueryOperator.EQ, "M'M", ValueType.TEXT, AggregationType.NONE, null ) );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.FALSE );

        assertIsEmpty( trackedEntities );
    }

    @Test
    void shouldReturnTrackedEntityWithLastUpdatedParameter()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setLastUpdatedStartDate( Date.from( Instant.now().minus( 1, ChronoUnit.DAYS ) ) );
        queryParams.setLastUpdatedEndDate( new Date() );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.FALSE );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );

        // Update last updated start date to today
        queryParams.setLastUpdatedStartDate( Date.from( Instant.now().plus( 1, ChronoUnit.DAYS ) ) );

        assertIsEmpty( trackedEntityService.getTrackedEntities( queryParams, TrackedEntityParams.FALSE ) );
    }

    @Test
    @Disabled( "12098 This test is not working" )
    void shouldReturnTrackedEntityWithEventFilters()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setUserWithAssignedUsers( null, user, null );
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setProgram( programA );
        queryParams.setEventStatus( EventStatus.COMPLETED );
        queryParams.setEventStartDate( Date.from( Instant.now().minus( 10, ChronoUnit.DAYS ) ) );
        queryParams.setEventEndDate( Date.from( Instant.now().plus( 10, ChronoUnit.DAYS ) ) );
        TrackedEntityParams params = TrackedEntityParams.FALSE;

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityService
            .getTrackedEntities( queryParams, params );
        assertEquals( 4, trackedEntityInstances.size() );
        // Update status to active
        queryParams.setEventStatus( EventStatus.ACTIVE );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances = trackedEntityService
            .getTrackedEntities( queryParams, params );
        assertIsEmpty( limitedTrackedEntityInstances );
        // Update status to overdue
        queryParams.setEventStatus( EventStatus.OVERDUE );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances2 = trackedEntityService
            .getTrackedEntities( queryParams, params );
        assertIsEmpty( limitedTrackedEntityInstances2 );
        // Update status to schedule
        queryParams.setEventStatus( EventStatus.SCHEDULE );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances3 = trackedEntityService
            .getTrackedEntities( queryParams, params );
        assertIsEmpty( limitedTrackedEntityInstances3 );
        // Update status to schedule
        queryParams.setEventStatus( EventStatus.SKIPPED );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances4 = trackedEntityService
            .getTrackedEntities( queryParams, params );
        assertIsEmpty( limitedTrackedEntityInstances4 );
        // Update status to visited
        queryParams.setEventStatus( EventStatus.VISITED );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances5 = trackedEntityService
            .getTrackedEntities( queryParams, params );
        assertIsEmpty( limitedTrackedEntityInstances5 );
    }

    @Test
    void shouldIncludeDeletedEnrollmentAndEvents()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeDeleted( true );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.TRUE );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );
        TrackedEntityInstance trackedEntity = trackedEntities.get( 0 );
        Set<String> deletedEnrollments = trackedEntity.getProgramInstances().stream()
            .filter( ProgramInstance::isDeleted ).map( BaseIdentifiableObject::getUid ).collect( Collectors.toSet() );
        assertIsEmpty( deletedEnrollments );
        Set<String> deletedEvents = trackedEntity.getProgramInstances().stream()
            .flatMap( programInstance -> programInstance.getProgramStageInstances().stream() )
            .filter( ProgramStageInstance::isDeleted )
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toSet() );
        assertIsEmpty( deletedEvents );

        programInstanceService.deleteProgramInstance( programInstanceA );
        programStageInstanceService.deleteProgramStageInstance( programStageInstanceA );

        trackedEntities = trackedEntityService.getTrackedEntities( queryParams, TrackedEntityParams.TRUE );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );
        trackedEntity = trackedEntities.get( 0 );

        assertContainsOnly( Set.of( programInstanceA.getUid(), programInstanceB.getUid() ),
            uids( trackedEntity.getProgramInstances() ) );
        deletedEnrollments = trackedEntity.getProgramInstances().stream()
            .filter( ProgramInstance::isDeleted ).map( BaseIdentifiableObject::getUid ).collect( Collectors.toSet() );
        assertContainsOnly( Set.of( programInstanceA.getUid() ), deletedEnrollments );

        Set<ProgramStageInstance> events = trackedEntity.getProgramInstances().stream()
            .flatMap( programInstance -> programInstance.getProgramStageInstances().stream() )
            .collect( Collectors.toSet() );
        assertContainsOnly( Set.of( programStageInstanceA.getUid(), programStageInstanceB.getUid() ), uids( events ) );
        deletedEvents = events.stream()
            .filter( ProgramStageInstance::isDeleted )
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toSet() );
        assertContainsOnly( Set.of( programStageInstanceA.getUid() ), deletedEvents );

        queryParams.setIncludeDeleted( false );
        trackedEntities = trackedEntityService.getTrackedEntities( queryParams, TrackedEntityParams.TRUE );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );
        trackedEntity = trackedEntities.get( 0 );
        assertContainsOnly( Set.of( programInstanceB.getUid() ), uids( trackedEntity.getProgramInstances() ) );
        events = trackedEntity.getProgramInstances().stream()
            .flatMap( programInstance -> programInstance.getProgramStageInstances().stream() )
            .collect( Collectors.toSet() );
        assertContainsOnly( Set.of( programStageInstanceB.getUid() ), uids( events ) );
    }

    @Test
    void shouldReturnTrackedEntityAndEnrollmentsGivenTheyShouldBeIncluded()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setTrackedEntityInstanceUids( Set.of( trackedEntityA.getUid() ) );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityParams params = new TrackedEntityParams( false,
            TrackedEntityEnrollmentParams.TRUE, false, false, false );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams, params );

        assertContainsOnly( List.of( trackedEntityA.getUid() ), uids( trackedEntities ) );
        assertContainsOnly( Set.of( programInstanceA.getUid(), programInstanceB.getUid() ),
            uids( trackedEntities.get( 0 ).getProgramInstances() ) );
        // ensure that EnrollmentAggregate is called and attaches the enrollments attributes (program attributes)
        List<ProgramInstance> enrollments = new ArrayList<>( trackedEntities.get( 0 ).getProgramInstances() );
        Optional<ProgramInstance> enrollmentA = enrollments.stream()
            .filter( pi -> pi.getUid().equals( programInstanceA.getUid() ) ).findFirst();
        assertContainsOnly( Set.of( "C" ),
            attributeNames( enrollmentA.get().getEntityInstance().getTrackedEntityAttributeValues() ) );
    }

    @Test
    void shouldReturnTrackedEntityWithoutEnrollmentsGivenTheyShouldNotBeIncluded()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setTrackedEntityInstanceUids( Set.of( trackedEntityA.getUid() ) );
        queryParams.setIncludeAllAttributes( true );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.FALSE );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );
        assertIsEmpty( trackedEntities.get( 0 ).getProgramInstances() );
    }

    @Test
    void shouldReturnTrackedEntityWithEventsAndNotesGivenTheyShouldBeIncluded()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setTrackedEntityInstanceUids( Set.of( trackedEntityA.getUid() ) );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityParams params = new TrackedEntityParams( false,
            TrackedEntityEnrollmentParams.TRUE, true, false, false );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams, params );

        assertContainsOnly( List.of( trackedEntityA ), trackedEntities );
        assertContainsOnly( Set.of( programInstanceA.getUid(), programInstanceB.getUid() ),
            uids( trackedEntities.get( 0 ).getProgramInstances() ) );
        // ensure that EventAggregate is called and attaches the events with notes
        List<ProgramInstance> enrollments = new ArrayList<>( trackedEntities.get( 0 ).getProgramInstances() );
        Optional<ProgramInstance> enrollmentA = enrollments.stream()
            .filter( pi -> pi.getUid().equals( programInstanceA.getUid() ) ).findFirst();
        Set<ProgramStageInstance> events = enrollmentA.get().getProgramStageInstances();
        assertContainsOnly( Set.of( programStageInstanceA ), events );
        assertContainsOnly( Set.of( note1 ), events.stream().findFirst().get().getComments() );
    }

    @Test
    void shouldReturnTrackedEntityWithoutEventsGivenTheyShouldNotBeIncluded()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityParams params = new TrackedEntityParams( false,
            TrackedEntityEnrollmentParams.TRUE.withIncludeEvents( false ), false, false, false );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams, params );

        assertContainsOnly( List.of( trackedEntityA.getUid() ), uids( trackedEntities ) );
        assertContainsOnly( Set.of( programInstanceA.getUid(), programInstanceB.getUid() ),
            uids( trackedEntities.get( 0 ).getProgramInstances() ) );
        List<ProgramInstance> enrollments = new ArrayList<>( trackedEntities.get( 0 ).getProgramInstances() );
        Optional<ProgramInstance> enrollmentA = enrollments.stream()
            .filter( pi -> pi.getUid().equals( programInstanceA.getUid() ) ).findFirst();
        assertIsEmpty( enrollmentA.get().getProgramStageInstances() );
    }

    @Test
    void shouldReturnTrackedEntityMappedCorrectly()
        throws ForbiddenException,
        NotFoundException
    {
        final Date currentTime = new Date();
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams,
            TrackedEntityParams.FALSE );

        TrackedEntityInstance trackedEntity = trackedEntities.get( 0 );
        assertAll(
            () -> assertEquals( trackedEntityA.getUid(), trackedEntity.getUid() ),
            () -> assertEquals( trackedEntity.getTrackedEntityType().getUid(), trackedEntityTypeA.getUid() ),
            () -> assertEquals( orgUnitA.getUid(), trackedEntity.getOrganisationUnit().getUid() ),
            () -> assertFalse( trackedEntity.isInactive() ),
            () -> assertFalse( trackedEntity.isDeleted() ),
            () -> checkDate( currentTime, trackedEntity.getCreated() ),
            () -> checkDate( currentTime, trackedEntity.getCreatedAtClient() ),
            () -> checkDate( currentTime, trackedEntity.getLastUpdatedAtClient() ),
            () -> checkDate( currentTime, trackedEntity.getLastUpdated() ),
            // get stored by is always null
            () -> assertNull( trackedEntity.getStoredBy() ) );
    }

    @Test
    void shouldReturnEnrollmentMappedCorrectly()
        throws ForbiddenException,
        NotFoundException
    {
        final Date currentTime = new Date();
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityParams params = new TrackedEntityParams( false,
            TrackedEntityEnrollmentParams.TRUE, false, false, false );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams, params );

        List<ProgramInstance> enrollments = new ArrayList<>( trackedEntities.get( 0 ).getProgramInstances() );
        Optional<ProgramInstance> enrollmentOpt = enrollments.stream()
            .filter( pi -> pi.getUid().equals( programInstanceA.getUid() ) ).findFirst();
        assertTrue( enrollmentOpt.isPresent() );
        ProgramInstance enrollment = enrollmentOpt.get();
        assertAll(
            () -> assertEquals( programInstanceA.getId(), enrollment.getId() ),
            () -> assertEquals( trackedEntityA.getUid(), enrollment.getEntityInstance().getUid() ),
            () -> assertEquals( trackedEntityA.getTrackedEntityType().getUid(),
                enrollment.getEntityInstance().getTrackedEntityType().getUid() ),
            () -> assertEquals( orgUnitA.getUid(), enrollment.getOrganisationUnit().getUid() ),
            () -> assertEquals( orgUnitA.getName(), enrollment.getOrganisationUnit().getName() ),
            () -> assertEquals( programA.getUid(), enrollment.getProgram().getUid() ),
            () -> assertEquals( ProgramStatus.ACTIVE, enrollment.getStatus() ),
            () -> assertFalse( enrollment.isDeleted() ),
            () -> assertTrue( enrollment.getFollowup() ),
            () -> checkDate( currentTime, enrollment.getCreated() ),
            () -> checkDate( currentTime, enrollment.getCreatedAtClient() ),
            () -> checkDate( currentTime, enrollment.getLastUpdated() ),
            () -> checkDate( currentTime, enrollment.getLastUpdatedAtClient() ),
            () -> checkDate( currentTime, enrollment.getEnrollmentDate() ),
            () -> checkDate( currentTime, enrollment.getIncidentDate() ),
            () -> assertNull( enrollment.getStoredBy() ) );
    }

    @Test
    void shouldReturnEventMappedCorrectly()
        throws ForbiddenException,
        NotFoundException
    {
        final Date currentTime = new Date();
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityParams params = new TrackedEntityParams( false,
            TrackedEntityEnrollmentParams.TRUE, false, false, false );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams, params );

        List<ProgramInstance> enrollments = new ArrayList<>( trackedEntities.get( 0 ).getProgramInstances() );
        Optional<ProgramInstance> enrollmentOpt = enrollments.stream()
            .filter( pi -> pi.getUid().equals( programInstanceA.getUid() ) ).findFirst();
        assertTrue( enrollmentOpt.isPresent() );
        ProgramInstance enrollment = enrollmentOpt.get();
        Optional<ProgramStageInstance> eventOpt = enrollment.getProgramStageInstances().stream().findFirst();
        assertTrue( eventOpt.isPresent() );
        ProgramStageInstance event = eventOpt.get();
        assertAll(
            () -> assertEquals( programStageInstanceA.getId(), event.getId() ),
            () -> assertEquals( programStageInstanceA.getUid(), event.getUid() ),
            () -> assertEquals( EventStatus.ACTIVE, event.getStatus() ),
            () -> assertEquals( orgUnitA.getUid(), event.getOrganisationUnit().getUid() ),
            () -> assertEquals( orgUnitA.getName(), event.getOrganisationUnit().getName() ),
            () -> assertEquals( programInstanceA.getUid(), event.getProgramInstance().getUid() ),
            () -> assertEquals( programA.getUid(), event.getProgramInstance().getProgram().getUid() ),
            () -> assertEquals( ProgramStatus.ACTIVE, event.getProgramInstance().getStatus() ),
            () -> assertEquals( trackedEntityA.getUid(), event.getProgramInstance().getEntityInstance().getUid() ),
            () -> assertEquals( programStageInstanceA.getProgramStage().getUid(), event.getProgramStage().getUid() ),
            () -> assertEquals( defaultCategoryOptionCombo.getUid(), event.getAttributeOptionCombo().getUid() ),
            () -> assertFalse( event.isDeleted() ),
            () -> assertTrue( event.getProgramInstance().getFollowup() ),
            () -> assertEquals( user, event.getAssignedUser() ),
            () -> checkDate( currentTime, event.getCreated() ),
            () -> checkDate( currentTime, event.getLastUpdated() ),
            () -> checkDate( programStageInstanceA.getDueDate(), event.getDueDate() ),
            () -> checkDate( currentTime, event.getCreatedAtClient() ),
            () -> checkDate( currentTime, event.getLastUpdatedAtClient() ),
            () -> checkDate( programStageInstanceA.getCompletedDate(), event.getCompletedDate() ),
            () -> assertEquals( programStageInstanceA.getCompletedBy(), event.getCompletedBy() ) );
    }

    @Test
    void shouldReturnTrackedEntityWithRelationshipsTei2Tei()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityInstanceUids( Set.of( trackedEntityA.getUid() ) );
        TrackedEntityParams params = new TrackedEntityParams( true,
            TrackedEntityEnrollmentParams.FALSE, false, false, false );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams, params );

        TrackedEntityInstance trackedEntity = trackedEntities.get( 0 );
        Optional<RelationshipItem> relOpt = trackedEntity.getRelationshipItems().stream()
            .filter( i -> i.getRelationship().getUid().equals( relationshipA.getUid() ) ).findFirst();
        assertTrue( relOpt.isPresent() );
        Relationship actual = relOpt.get().getRelationship();
        assertAll(
            () -> assertEquals( trackedEntityA.getUid(), actual.getFrom().getTrackedEntityInstance().getUid() ),
            () -> assertEquals( trackedEntityB.getUid(), actual.getTo().getTrackedEntityInstance().getUid() ) );
    }

    @Test
    void returnTrackedEntityRelationshipsWithTei2Enrollment()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityInstanceUids( Set.of( trackedEntityA.getUid() ) );

        TrackedEntityParams params = new TrackedEntityParams( true,
            TrackedEntityEnrollmentParams.FALSE, false, false, false );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams, params );

        TrackedEntityInstance trackedEntity = trackedEntities.get( 0 );
        Optional<RelationshipItem> relOpt = trackedEntity.getRelationshipItems().stream()
            .filter( i -> i.getRelationship().getUid().equals( relationshipB.getUid() ) ).findFirst();
        assertTrue( relOpt.isPresent() );
        Relationship actual = relOpt.get().getRelationship();
        assertAll(
            () -> assertEquals( trackedEntityA.getUid(), actual.getFrom().getTrackedEntityInstance().getUid() ),
            () -> assertEquals( programInstanceA.getUid(), actual.getTo().getProgramInstance().getUid() ) );
    }

    @Test
    void shouldReturnTrackedEntityRelationshipsWithTei2Event()
        throws ForbiddenException,
        NotFoundException
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Set.of( orgUnitA ) );
        queryParams.setTrackedEntityInstanceUids( Set.of( trackedEntityA.getUid() ) );
        TrackedEntityParams params = new TrackedEntityParams( true,
            TrackedEntityEnrollmentParams.TRUE, false, false, false );

        List<TrackedEntityInstance> trackedEntities = trackedEntityService.getTrackedEntities( queryParams, params );

        TrackedEntityInstance trackedEntity = trackedEntities.get( 0 );
        Optional<RelationshipItem> relOpt = trackedEntity.getRelationshipItems().stream()
            .filter( i -> i.getRelationship().getUid().equals( relationshipC.getUid() ) ).findFirst();
        assertTrue( relOpt.isPresent() );
        Relationship actual = relOpt.get().getRelationship();
        assertAll(
            () -> assertEquals( trackedEntityA.getUid(), actual.getFrom().getTrackedEntityInstance().getUid() ),
            () -> assertEquals( programStageInstanceA.getUid(), actual.getTo().getProgramStageInstance().getUid() ) );
    }

    private static List<String> uids( Collection<? extends BaseIdentifiableObject> trackedEntities )
    {
        return trackedEntities.stream().map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() );
    }

    private Set<String> attributeNames( final Collection<TrackedEntityAttributeValue> attributes )
    {
        // depends on createTrackedEntityAttribute() prefixing with "Attribute"
        return attributes.stream()
            .map( a -> StringUtils.removeStart( a.getAttribute().getName(), "Attribute" ) )
            .collect( Collectors.toSet() );
    }

    protected ProgramStage createProgramStage( Program program )
    {
        ProgramStage programStage = createProgramStage( '1', program );
        programStage.setUid( CodeGenerator.generateUid() );
        programStage.setRepeatable( true );
        programStage.setEnableUserAssignment( true );
        programStage.setPublicAccess( AccessStringHelper.FULL );
        manager.save( programStage, false );
        return programStage;
    }

    private void checkDate( Date currentTime, Date date )
    {
        final long interval = currentTime.getTime() - date.getTime();
        long second = 1000;
        assertTrue( Math.abs( interval ) < second,
            "Timestamp is higher than expected interval. Expecting: " + (long) 1000 + " got: " + interval );
    }
}
