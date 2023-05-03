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
package org.hisp.dhis.tracker.export.relationship;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationshipServiceTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    protected UserService _userService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private IdentifiableObjectManager manager;

    private TrackedEntityInstance teiA;

    private TrackedEntityInstance teiB;

    private TrackedEntityInstance inaccessibleTei;

    private Event eventA;

    private Event inaccessiblePsi;

    private final RelationshipType teiToTeiType = createRelationshipType( 'A' );

    private final RelationshipType teiToPiType = createRelationshipType( 'B' );

    private final RelationshipType teiToPsiType = createRelationshipType( 'C' );

    private final RelationshipType teiToInaccessibleTeiType = createRelationshipType( 'D' );

    private final RelationshipType teiToPiInaccessibleType = createRelationshipType( 'E' );

    private final RelationshipType eventToEventType = createRelationshipType( 'F' );

    private ProgramInstance enrollmentA;

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
        User admin = preCreateInjectAdminUser();

        OrganisationUnit orgUnit = createOrganisationUnit( 'A' );
        manager.save( orgUnit, false );

        User user = createAndAddUser( false, "user", Set.of( orgUnit ), Set.of( orgUnit ),
            "F_EXPORT_DATA" );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityType.getSharing().setOwner( user );
        manager.save( trackedEntityType, false );

        TrackedEntityType inaccessibleTrackedEntityType = createTrackedEntityType( 'B' );
        inaccessibleTrackedEntityType.getSharing().setOwner( admin );
        inaccessibleTrackedEntityType.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( inaccessibleTrackedEntityType, false );

        teiA = createTrackedEntityInstance( orgUnit );
        teiA.setTrackedEntityType( trackedEntityType );
        manager.save( teiA, false );

        teiB = createTrackedEntityInstance( orgUnit );
        teiB.setTrackedEntityType( trackedEntityType );
        manager.save( teiB, false );

        inaccessibleTei = createTrackedEntityInstance( orgUnit );
        inaccessibleTei.setTrackedEntityType( inaccessibleTrackedEntityType );
        manager.save( inaccessibleTei, false );

        Program program = createProgram( 'A', new HashSet<>(), orgUnit );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        program.getSharing().setOwner( user );
        manager.save( program, false );
        ProgramStage programStage = createProgramStage( 'A', program );
        manager.save( programStage, false );
        ProgramStage inaccessibleProgramStage = createProgramStage( 'B', program );
        inaccessibleProgramStage.getSharing().setOwner( admin );
        inaccessibleProgramStage.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( inaccessibleProgramStage, false );
        program.setProgramStages( Set.of( programStage, inaccessibleProgramStage ) );
        manager.save( program, false );

        enrollmentA = programInstanceService.enrollTrackedEntityInstance( teiA, program, new Date(), new Date(),
            orgUnit );
        eventA = new Event();
        eventA.setEnrollment( enrollmentA );
        eventA.setProgramStage( programStage );
        eventA.setOrganisationUnit( orgUnit );
        manager.save( eventA, false );

        ProgramInstance enrollmentB = programInstanceService.enrollTrackedEntityInstance( teiB, program, new Date(),
            new Date(),
            orgUnit );
        inaccessiblePsi = new Event();
        inaccessiblePsi.setEnrollment( enrollmentB );
        inaccessiblePsi.setProgramStage( inaccessibleProgramStage );
        inaccessiblePsi.setOrganisationUnit( orgUnit );
        manager.save( inaccessiblePsi, false );

        teiToTeiType.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        teiToTeiType.getFromConstraint().setTrackedEntityType( trackedEntityType );
        teiToTeiType.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        teiToTeiType.getToConstraint().setTrackedEntityType( trackedEntityType );
        teiToTeiType.getSharing().setOwner( user );
        manager.save( teiToTeiType, false );

        teiToInaccessibleTeiType.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        teiToInaccessibleTeiType.getFromConstraint().setTrackedEntityType( trackedEntityType );
        teiToInaccessibleTeiType.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        teiToInaccessibleTeiType.getToConstraint().setTrackedEntityType( inaccessibleTrackedEntityType );
        teiToInaccessibleTeiType.getSharing().setOwner( user );
        manager.save( teiToInaccessibleTeiType, false );

        teiToPiType.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        teiToPiType.getFromConstraint().setTrackedEntityType( trackedEntityType );
        teiToPiType.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        teiToPiType.getToConstraint().setProgram( program );
        teiToPiType.getSharing().setOwner( user );
        manager.save( teiToPiType, false );

        teiToPiInaccessibleType.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        teiToPiInaccessibleType.getFromConstraint().setTrackedEntityType( trackedEntityType );
        teiToPiInaccessibleType.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        teiToPiInaccessibleType.getToConstraint().setProgram( program );
        teiToPiInaccessibleType.getSharing().setOwner( admin );
        teiToPiInaccessibleType.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( teiToPiInaccessibleType, false );

        teiToPsiType.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        teiToPsiType.getFromConstraint().setTrackedEntityType( trackedEntityType );
        teiToPsiType.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        teiToPsiType.getToConstraint().setProgramStage( programStage );
        teiToPsiType.getSharing().setOwner( user );
        manager.save( teiToPsiType, false );

        eventToEventType.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        eventToEventType.getFromConstraint().setTrackedEntityType( trackedEntityType );
        eventToEventType.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        eventToEventType.getToConstraint().setProgramStage( programStage );
        eventToEventType.getSharing().setOwner( user );
        manager.save( eventToEventType, false );

        injectSecurityContext( user );
    }

    @Test
    void shouldNotReturnRelationshipByTrackedEntityInstanceIfUserHasNoAccessToTrackedEntityType()
        throws ForbiddenException,
        NotFoundException
    {
        Relationship accessible = relationship( teiA, teiB );
        relationship( teiA, inaccessibleTei, teiToInaccessibleTeiType );

        List<Relationship> relationships = relationshipService.getRelationshipsByTrackedEntityInstance( teiA,
            new Paging() );

        assertContainsOnly( List.of( accessible.getUid() ),
            relationships.stream().map( Relationship::getUid ).collect( Collectors.toList() ) );
    }

    @Test
    void shouldNotReturnRelationshipByProgramInstanceIfUserHasNoAccessToRelationshipType()
        throws ForbiddenException,
        NotFoundException
    {
        Relationship accessible = relationship( teiA, enrollmentA );
        relationship( teiB, enrollmentA, teiToPiInaccessibleType );

        List<Relationship> relationships = relationshipService.getRelationshipsByProgramInstance( enrollmentA,
            new Paging() );

        assertContainsOnly( List.of( accessible.getUid() ),
            relationships.stream().map( Relationship::getUid ).collect( Collectors.toList() ) );
    }

    @Test
    void shouldNotReturnRelationshipByEventIfUserHasNoAccessToProgramStage()
        throws ForbiddenException,
        NotFoundException
    {
        Relationship accessible = relationship( teiA, eventA );
        relationship( eventA, inaccessiblePsi );

        List<Relationship> relationships = relationshipService.getRelationshipsByEvent( eventA,
            new Paging() );

        assertContainsOnly( List.of( accessible.getUid() ),
            relationships.stream().map( Relationship::getUid ).collect( Collectors.toList() ) );
    }

    private Relationship relationship( TrackedEntityInstance from, TrackedEntityInstance to )
    {
        return relationship( from, to, teiToTeiType );
    }

    private Relationship relationship( TrackedEntityInstance from, TrackedEntityInstance to, RelationshipType type )
    {
        Relationship relationship = new Relationship();
        relationship.setUid( CodeGenerator.generateUid() );
        relationship.setRelationshipType( type );
        relationship.setFrom( item( from ) );
        relationship.setTo( item( to ) );
        relationship.setKey( RelationshipUtils.generateRelationshipKey( relationship ) );
        relationship.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationship ) );

        manager.save( relationship );

        return relationship;
    }

    private Relationship relationship( TrackedEntityInstance from, ProgramInstance to )
    {
        return relationship( from, to, teiToPsiType );
    }

    private Relationship relationship( TrackedEntityInstance from, ProgramInstance to, RelationshipType type )
    {
        Relationship relationship = new Relationship();
        relationship.setUid( CodeGenerator.generateUid() );
        relationship.setRelationshipType( type );
        relationship.setFrom( item( from ) );
        relationship.setTo( item( to ) );
        relationship.setKey( RelationshipUtils.generateRelationshipKey( relationship ) );
        relationship.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationship ) );

        manager.save( relationship );

        return relationship;
    }

    private Relationship relationship( TrackedEntityInstance from, Event to )
    {
        return relationship( from, to, teiToPsiType );
    }

    private Relationship relationship( TrackedEntityInstance from, Event to, RelationshipType type )
    {
        Relationship relationship = new Relationship();
        relationship.setUid( CodeGenerator.generateUid() );
        relationship.setRelationshipType( type );
        relationship.setFrom( item( from ) );
        relationship.setTo( item( to ) );
        relationship.setKey( RelationshipUtils.generateRelationshipKey( relationship ) );
        relationship.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationship ) );

        manager.save( relationship );

        return relationship;
    }

    private void relationship( Event from, Event to )
    {
        relationship( from, to, eventToEventType );
    }

    private void relationship( Event from, Event to, RelationshipType type )
    {
        Relationship relationship = new Relationship();
        relationship.setUid( CodeGenerator.generateUid() );
        relationship.setRelationshipType( type );
        relationship.setFrom( item( from ) );
        relationship.setTo( item( to ) );
        relationship.setKey( RelationshipUtils.generateRelationshipKey( relationship ) );
        relationship.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationship ) );

        manager.save( relationship );
    }

    private RelationshipItem item( TrackedEntityInstance from )
    {
        RelationshipItem relationshipItem = new RelationshipItem();
        relationshipItem.setTrackedEntityInstance( from );
        return relationshipItem;
    }

    private RelationshipItem item( ProgramInstance from )
    {
        RelationshipItem relationshipItem = new RelationshipItem();
        relationshipItem.setProgramInstance( from );
        return relationshipItem;
    }

    private RelationshipItem item( Event from )
    {
        RelationshipItem relationshipItem = new RelationshipItem();
        relationshipItem.setEvent( from );
        return relationshipItem;
    }

    private static class Paging extends PagingAndSortingCriteriaAdapter
    {
    }
}
