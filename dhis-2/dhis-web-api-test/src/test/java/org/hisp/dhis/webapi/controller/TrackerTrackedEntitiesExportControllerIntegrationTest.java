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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
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
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration tests using Postgres are needed as the implementation of
 * /tracker/trackedEntities relies on JSONB which is not (really/fully)
 * supported by h2 DB. Use of the TransactionTemplate is needed as
 * multi-threaded code is tested and Spring does not propagate transactions to
 * newly spawned threads.
 */
@Transactional
class TrackerTrackedEntitiesExportControllerIntegrationTest extends DhisControllerIntegrationTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private TransactionTemplate txTemplate;

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
        // TODO clean up; what truly needs to be in a transaction and what does
        // not?
        doInTransaction( () -> {
            owner = createUser( "owner" );

            orgUnit = createOrganisationUnit( 'A' );
            orgUnit.getSharing().setOwner( owner );
            orgUnit.getSharing().setPublicAccess( AccessStringHelper.FULL );
            manager.save( orgUnit, false );

            trackedEntityType = createTrackedEntityType( 'A' );
            trackedEntityType.getSharing().setOwner( owner );
            trackedEntityType.getSharing().setPublicAccess( AccessStringHelper.FULL );
            manager.save( trackedEntityType, false );
        } );

        doInTransaction( () -> {
            user = createUserWithId( "testuser", CodeGenerator.generateUid() );
            user.addOrganisationUnit( orgUnit );
            user.setTeiSearchOrganisationUnits( Set.of( orgUnit ) );
            manager.update( user );
            relationshipType = relationshipType( RelationshipEntity.TRACKED_ENTITY_INSTANCE,
                RelationshipEntity.TRACKED_ENTITY_INSTANCE );
            manager.save( relationshipType, false );
        } );

        // TODO programs, programStages also need to go in doInTransaction if I
        // wanted to have the TrackedEntityInstanceAggregate security context
        // see them
        program = createProgram( 'A' );
        program.addOrganisationUnit( orgUnit );
        program.getSharing().setOwner( owner );
        program.getSharing().setPublicAccess( AccessStringHelper.FULL );
        manager.save( program, false );

        programStage = createProgramStage( 'A', program );
        programStage.getSharing().setOwner( owner );
        programStage.getSharing().setPublicAccess( AccessStringHelper.FULL );
        manager.save( programStage, false );
    }

    @Test
    void one()
    {

        assertNotNull( this.getCurrentUser() );
    }

    @Test
    void two()
    {

        assertNotNull( this.getCurrentUser() );
    }

    @Disabled
    @Test
    void getTrackedEntities()
    {

        // TODO clean this up
        TrackedEntityInstance tei1 = trackedEntityInstance();
        TrackedEntityInstance tei2 = trackedEntityInstance();
        doInTransaction( () -> {
            manager.save( tei1, false );
            manager.save( tei2, false );
        } );
        manager.flush();
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities?orgUnit={ou}&trackedEntityType={type}", orgUnit.getUid(),
            trackedEntityType.getUid() )
                .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        List<String> teis = json.getList( "instances", JsonObject.class ).stream()
            .map( e -> e.getString( "trackedEntity" ).string() ).collect( Collectors.toList() );
        assertContainsOnly( teis, tei1.getUid(), tei2.getUid() );
    }

    @Disabled
    @Test
    void getTrackedEntitiesWithTrackedEntityParam()
    {

        TrackedEntityInstance tei1 = trackedEntityInstance();
        TrackedEntityInstance tei2 = trackedEntityInstance();
        doInTransaction( () -> {
            manager.save( tei1, false );
            manager.save( tei2, false );
        } );
        manager.flush();
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities?trackedEntity={uid}", tei1.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        List<String> teis = json.getList( "instances", JsonObject.class ).stream()
            .map( e -> e.getString( "trackedEntity" ).string() ).collect( Collectors.toList() );
        assertContainsOnly( teis, tei1.getUid() );
    }

    private TrackedEntityInstance trackedEntityInstance()
    {
        TrackedEntityInstance tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        tei.getSharing().setOwner( owner );
        tei.getSharing().setPublicAccess( AccessStringHelper.FULL );
        tei.getSharing().addUserAccess( userAccess() );
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

    protected void doInTransaction( Runnable operation )
    {
        final int defaultPropagationBehaviour = txTemplate.getPropagationBehavior();
        System.out.println( defaultPropagationBehaviour );
        txTemplate.setPropagationBehavior( TransactionDefinition.PROPAGATION_REQUIRES_NEW );
        txTemplate.execute( status -> {
            operation.run();
            return null;
        } );
        // restore original propagation behaviour
        txTemplate.setPropagationBehavior( defaultPropagationBehaviour );
    }
}