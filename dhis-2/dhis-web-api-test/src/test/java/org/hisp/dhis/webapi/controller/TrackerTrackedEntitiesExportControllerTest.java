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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class TrackerTrackedEntitiesExportControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private TrackedEntityInstanceService teiService;

    private OrganisationUnit orgUnit;

    private Program program;

    private ProgramStage programStage;

    private TrackedEntityType trackedEntityType;

    private RelationshipType relationshipType;

    private User owner;

    private User user;

    @BeforeEach
    void setUp()
    {
        owner = createUser( "owner" );

        orgUnit = createOrganisationUnit( 'A' );
        orgUnit.getSharing().setOwner( owner );
        orgUnit.getSharing().setPublicAccess( AccessStringHelper.FULL );
        manager.save( orgUnit, false );

        user = createUserWithId( "testuser", CodeGenerator.generateUid() );
        user.addOrganisationUnit( orgUnit );
        user.setTeiSearchOrganisationUnits( Set.of( orgUnit ) );
        this.userService.updateUser( user );

        program = createProgram( 'A' );
        program.addOrganisationUnit( orgUnit );
        program.getSharing().setOwner( owner );
        program.getSharing().setPublicAccess( AccessStringHelper.FULL );
        manager.save( program, false );

        programStage = createProgramStage( 'A', program );
        programStage.getSharing().setOwner( owner );
        programStage.getSharing().setPublicAccess( AccessStringHelper.FULL );
        manager.save( programStage, false );

        trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityType.getSharing().setOwner( owner );
        trackedEntityType.getSharing().setPublicAccess( AccessStringHelper.FULL );
        manager.save( trackedEntityType, false );

        relationshipType = relationshipType( RelationshipEntity.TRACKED_ENTITY_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
    }

    @Test
    void getTrackedEntityInstancesWithTrackedEntityParam()
    {
        TrackedEntityInstance tei1 = trackedEntityInstance();
        TrackedEntityInstance tei2 = trackedEntityInstance();
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities?trackedEntity={uid}", tei1.getUid() )
            .content( HttpStatus.OK );

        assertNotNull( teiService.getTrackedEntityInstance( tei1.getUid() ) );

        assertFalse( json.isEmpty() );
        JsonList<JsonObject> teis = json.getList( "instances", JsonObject.class );
        assertEquals( 2, teis.size() );
        System.out.println( teis.stream().map( e -> e.getString( "trackedEntity" ) ).collect( Collectors.toList() ) );
    }

    @Test
    void getTrackedEntityInstancesNeedsAtLeastOneOrgUnit()
    {
        assertEquals( "At least one organisation unit must be specified",
            GET( "/tracker/trackedEntities?program={program}", program.getUid() )
                .error( HttpStatus.CONFLICT )
                .getMessage() );
    }

    @Test
    void getTrackedEntityInstancesNeedsProgramOrType()
    {
        assertEquals( "Either Program or Tracked entity type should be specified",
            GET( "/tracker/trackedEntities" )
                .error( HttpStatus.CONFLICT )
                .getMessage() );
    }

    @Test
    void getTrackedEntityInstanceById()
    {
        TrackedEntityInstance tei = trackedEntityInstance();

        JsonObject json = GET( "/tracker/trackedEntities/{id}", tei.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertEquals( tei.getUid(), json.getString( "trackedEntity" ).string() );
        assertTrue( json.getArray( "relationships" ).isEmpty(), "relationships are not returned by default" );
    }

    @Test
    void getTrackedEntityInstanceByIdDoesNotReturnRelationshipsByDefault()
    {
        TrackedEntityInstance tei1 = trackedEntityInstance();
        TrackedEntityInstance tei2 = trackedEntityInstance();
        Relationship r = relationship( relationshipType, tei1, tei2 );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities/{id}", tei1.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertEquals( tei1.getUid(), json.getString( "trackedEntity" ).string() );
        assertTrue( json.getArray( "relationships" ).isEmpty(), "relationships are not returned by default" );
    }

    @Test
    void getTrackedEntityInstanceByIdWithFieldsRelationships()
    {
        TrackedEntityInstance tei1 = trackedEntityInstance();
        TrackedEntityInstance tei2 = trackedEntityInstance();
        Relationship r = relationship( relationshipType, tei1, tei2 );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=relationships", tei1.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertEquals( tei1.getUid(), json.getString( "trackedEntity" ).string(),
            "returned even though `fields` is only set to relationships DHIS2-12660" );
        JsonArray rels = json.getArray( "relationships" );
        assertFalse( rels.isEmpty(), "relationships are returned if `fields` contains relationships" );
        assertEquals( 1, rels.size() );
        assertRelationship( rels.getObject( 0 ), r );
        assertTrackedEntity( rels.getObject( 0 ).getObject( "from" ), tei1 );
        assertTrackedEntity( rels.getObject( 0 ).getObject( "to" ), tei2 );
    }

    @Test
    void getTrackedEntityInstanceByIdWithFieldsRelationshipsNoAccessToRelationshipType()
    {
        // TODO make pretty
        RelationshipType rType = createRelationshipType( 'A' );
        rType.getFromConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        rType.getToConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        rType.getSharing().setOwner( owner );
        rType.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( rType, false );

        TrackedEntityInstance tei1 = trackedEntityInstance();
        TrackedEntityInstance tei2 = trackedEntityInstance();
        Relationship r = relationship( rType, tei1, tei2 );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=relationships", tei1.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertEquals( tei1.getUid(), json.getString( "trackedEntity" ).string(),
            "returned even though `fields` is only set to relationships DHIS2-12660" );
        assertTrue( json.getArray( "relationships" ).isEmpty() );
    }

    @Test
    void getTrackedEntityByIdNotFound()
    {
        assertEquals( "TrackedEntityInstance not found for uid: Hq3Kc6HK4OZ",
            GET( "/tracker/trackedEntities/Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    private TrackedEntityInstance trackedEntityInstance()
    {
        TrackedEntityInstance tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        tei.getSharing().setOwner( owner );
        tei.getSharing().setPublicAccess( AccessStringHelper.FULL );
        tei.getSharing().addUserAccess( userAccess() );
        manager.save( tei, false );
        return tei;
    }

    private UserAccess userAccess()
    {
        UserAccess a = new UserAccess();
        a.setUser( user );
        a.setAccess( AccessStringHelper.FULL );
        return a;
    }

    private RelationshipType relationshipType( RelationshipEntity tei1,
        RelationshipEntity tei2 )
    {
        RelationshipType rType = createRelationshipType( 'A' );
        rType.getFromConstraint().setRelationshipEntity( tei1 );
        rType.getToConstraint().setRelationshipEntity( tei2 );
        rType.getSharing().setOwner( owner );
        rType.getSharing().setPublicAccess( AccessStringHelper.FULL );
        rType.getSharing().addUserAccess( userAccess() );
        manager.save( rType, false );
        return rType;
    }

    private Relationship relationship( RelationshipType type, TrackedEntityInstance from, TrackedEntityInstance to )
    {
        Relationship r = new Relationship();

        RelationshipItem rItem1 = new RelationshipItem();
        rItem1.setTrackedEntityInstance( from );
        from.getRelationshipItems().add( rItem1 );
        r.setFrom( rItem1 );
        rItem1.setRelationship( r );

        RelationshipItem rItem2 = new RelationshipItem();
        rItem2.setTrackedEntityInstance( to );
        to.getRelationshipItems().add( rItem2 );
        r.setTo( rItem2 );
        rItem2.setRelationship( r );

        r.setRelationshipType( type );
        r.setKey( type.getUid() );
        r.setInvertedKey( type.getUid() );
        r.setAutoFields();
        r.getSharing().setOwner( owner );
        r.getSharing().setPublicAccess( AccessStringHelper.FULL );
        r.getSharing().addUserAccess( userAccess() );
        manager.save( r, false );
        return r;
    }

    private void assertRelationship( JsonObject json, Relationship r )
    {
        assertFalse( json.isEmpty() );
        assertEquals( r.getUid(), json.getString( "relationship" ).string() );
        assertEquals( r.getRelationshipType().getUid(), json.getString( "relationshipType" ).string() );
    }

    private void assertTrackedEntity( JsonObject json, TrackedEntityInstance tei )
    {
        JsonObject jsonTEI = json.getObject( "trackedEntity" );
        assertEquals( tei.getUid(), jsonTEI.getString( "trackedEntity" ).string() );
        assertFalse( jsonTEI.has( "trackedEntityType" ) );
        assertFalse( jsonTEI.has( "orgUnit" ) );
        assertFalse( jsonTEI.has( "relationships" ) );
        assertTrue( jsonTEI.getArray( "attributes" ).isEmpty() );
    }
}