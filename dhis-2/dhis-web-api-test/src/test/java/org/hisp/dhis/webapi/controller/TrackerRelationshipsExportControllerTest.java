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

import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertEnrollmentWithinRelationship;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertEventWithinRelationship;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertFirstRelationship;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertHasOnlyMembers;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertHasOnlyUid;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertNoRelationships;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertRelationship;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertTrackedEntityWithinRelationship;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Date;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class TrackerRelationshipsExportControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    private OrganisationUnit orgUnit;

    private OrganisationUnit anotherOrgUnit;

    private Program program;

    private ProgramStage programStage;

    private User owner;

    private User user;

    private TrackedEntityType trackedEntityType;

    @BeforeEach
    void setUp()
    {
        owner = makeUser( "owner" );

        orgUnit = createOrganisationUnit( 'A' );
        orgUnit.getSharing().setOwner( owner );
        manager.save( orgUnit, false );

        anotherOrgUnit = createOrganisationUnit( 'B' );
        anotherOrgUnit.getSharing().setOwner( owner );
        manager.save( anotherOrgUnit, false );

        user = createUserWithId( "tester", CodeGenerator.generateUid() );
        user.addOrganisationUnit( orgUnit );
        user.setTeiSearchOrganisationUnits( Set.of( orgUnit ) );
        this.userService.updateUser( user );

        program = createProgram( 'A' );
        program.addOrganisationUnit( orgUnit );
        program.getSharing().setOwner( owner );
        program.getSharing().addUserAccess( userAccess() );
        manager.save( program, false );

        programStage = createProgramStage( 'A', program );
        programStage.getSharing().setOwner( owner );
        programStage.getSharing().addUserAccess( userAccess() );
        manager.save( programStage, false );

        trackedEntityType = trackedEntityTypeAccessible();
    }

    @Test
    void getRelationshipsById()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        Relationship r = relationship( from, to );

        JsonObject relationship = GET( "/tracker/relationships/{uid}", r.getUid() )
            .content( HttpStatus.OK );

        assertHasOnlyMembers( relationship, "relationship", "relationshipType", "from", "to" );
        assertRelationship( r, relationship );
        assertHasOnlyUid( from.getUid(), "event", relationship.getObject( "from" ) );
        assertHasOnlyUid( to.getUid(), "trackedEntity", relationship.getObject( "to" ) );
    }

    @Test
    void getRelationshipsByIdWithFieldsAll()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        Relationship r = relationship( from, to );

        JsonObject relationship = GET( "/tracker/relationships/{uid}?fields=*", r.getUid() )
            .content( HttpStatus.OK );

        assertRelationship( r, relationship );
        assertEventWithinRelationship( from, relationship.getObject( "from" ) );
        assertTrackedEntityWithinRelationship( to, relationship.getObject( "to" ) );
    }

    @Test
    void getRelationshipsByIdWithFields()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        Relationship r = relationship( from, to );

        JsonObject json = GET( "/tracker/relationships/{uid}?fields=relationship,from[event]", r.getUid() )
            .content( HttpStatus.OK );

        assertHasOnlyMembers( json, "relationship", "from" );
        assertEquals( r.getUid(), json.getString( "relationship" ).string(), "relationship UID" );
        assertHasOnlyMembers( json.getObject( "from" ), "event" );
        assertEquals( from.getUid(), json.getObject( "from" ).getObject( "event" ).getString( "event" ).string(),
            "event UID" );
    }

    @Test
    void getRelationshipsByIdNotFound()
    {
        assertEquals( "Relationship not found for uid: Hq3Kc6HK4OZ",
            GET( "/tracker/relationships/Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    @Test
    void getRelationshipsMissingParam()
    {
        assertEquals( "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
            GET( "/tracker/relationships" )
                .error( HttpStatus.BAD_REQUEST )
                .getMessage() );
    }

    @Test
    void getRelationshipsBadRequestWithMultipleParams()
    {
        assertEquals( "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
            GET( "/tracker/relationships?trackedEntity=Hq3Kc6HK4OZ&enrollment=Hq3Kc6HK4OZ&event=Hq3Kc6HK4OZ" )
                .error( HttpStatus.BAD_REQUEST )
                .getMessage() );
    }

    @Test
    void getRelationshipsByEvent()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        Relationship r = relationship( from, to );

        JsonObject json = GET( "/tracker/relationships?event={uid}", from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "instances" );
        JsonObject jsonRelationship = assertFirstRelationship( r, relationships );
        assertHasOnlyMembers( jsonRelationship, "relationship", "relationshipType", "from", "to" );
        assertHasOnlyUid( from.getUid(), "event", jsonRelationship.getObject( "from" ) );
        assertHasOnlyUid( to.getUid(), "trackedEntity", jsonRelationship.getObject( "to" ) );
    }

    @Test
    void getRelationshipsByEventWithFields()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        Relationship r = relationship( from, to );

        JsonObject json = GET( "/tracker/relationships?event={uid}&fields=relationship,from[event]", from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "instances" );
        assertEquals( 1, relationships.size(), "one relationship expected" );
        JsonObject jsonRelationship = relationships.get( 0 ).as( JsonObject.class );
        assertHasOnlyMembers( jsonRelationship, "relationship", "from" );
        assertEquals( r.getUid(), jsonRelationship.getString( "relationship" ).string(), "relationship UID" );
        assertHasOnlyMembers( jsonRelationship.getObject( "from" ), "event" );
        assertEquals( from.getUid(),
            jsonRelationship.getObject( "from" ).getObject( "event" ).getString( "event" ).string(), "event UID" );
    }

    @Test
    void getRelationshipsByEventNotFound()
    {

        assertEquals( "No event 'Hq3Kc6HK4OZ' found.",
            GET( "/tracker/relationships?event=Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    @Test
    void getRelationshipsByEnrollment()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramInstance from = programInstance( to );
        Relationship r = relationship( from, to );

        JsonObject json = GET( "/tracker/relationships?enrollment=" + from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "instances" );
        JsonObject jsonRelationship = assertFirstRelationship( r, relationships );
        assertHasOnlyMembers( jsonRelationship, "relationship", "relationshipType", "from", "to" );
        assertHasOnlyUid( from.getUid(), "enrollment", jsonRelationship.getObject( "from" ) );
        assertHasOnlyUid( to.getUid(), "trackedEntity", jsonRelationship.getObject( "to" ) );
    }

    @Test
    void getRelationshipsByEnrollmentWithFieldsAll()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramInstance from = programInstance( to );
        Relationship r = relationship( from, to );

        JsonObject json = GET( "/tracker/relationships?enrollment={uid}&fields=*", from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "instances" );
        JsonObject jsonRelationship = assertFirstRelationship( r, relationships );
        assertEnrollmentWithinRelationship( from, jsonRelationship.getObject( "from" ) );
        assertTrackedEntityWithinRelationship( to, jsonRelationship.getObject( "to" ) );
    }

    @Test
    void getRelationshipsByEnrollmentNotFound()
    {

        assertEquals( "No enrollment 'Hq3Kc6HK4OZ' found.",
            GET( "/tracker/relationships?enrollment=Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    @Test
    void getRelationshipsByTrackedEntityRelationshipEnrollmentToTrackedEntity()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramInstance from = programInstance( to );
        Relationship r = relationship( from, to );

        JsonObject json = GET( "/tracker/relationships?trackedEntity={tei}", to.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "instances" );
        JsonObject jsonRelationship = assertFirstRelationship( r, relationships );
        assertHasOnlyMembers( jsonRelationship, "relationship", "relationshipType", "from", "to" );
        assertHasOnlyUid( from.getUid(), "enrollment", jsonRelationship.getObject( "from" ) );
        assertHasOnlyUid( to.getUid(), "trackedEntity", jsonRelationship.getObject( "to" ) );
    }

    @Test
    void getRelationshipsByTei()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramInstance from = programInstance( to );
        Relationship r = relationship( from, to );

        JsonObject json = GET( "/tracker/relationships?tei=" + to.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "instances" );
        JsonObject jsonRelationship = assertFirstRelationship( r, relationships );
        assertHasOnlyMembers( jsonRelationship, "relationship", "relationshipType", "from", "to" );
        assertHasOnlyUid( from.getUid(), "enrollment", jsonRelationship.getObject( "from" ) );
        assertHasOnlyUid( to.getUid(), "trackedEntity", jsonRelationship.getObject( "to" ) );
    }

    @Test
    void getRelationshipsByTrackedEntityRelationshipTeiToTei()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstance();
        Relationship r = relationship( from, to );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/relationships?trackedEntity={tei}", from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "instances" );
        JsonObject jsonRelationship = assertFirstRelationship( r, relationships );
        assertHasOnlyMembers( jsonRelationship, "relationship", "relationshipType", "from", "to" );
        assertHasOnlyUid( from.getUid(), "trackedEntity", jsonRelationship.getObject( "from" ) );
        assertHasOnlyUid( to.getUid(), "trackedEntity", jsonRelationship.getObject( "to" ) );
    }

    @Test
    void getRelationshipsByTrackedEntityRelationshipsNoAccessToRelationshipType()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstance();
        relationship( relationshipTypeNotAccessible(), from, to );
        this.switchContextToUser( user );

        assertNoRelationships( GET( "/tracker/relationships?trackedEntity={tei}", from.getUid() )
            .content( HttpStatus.OK ) );
    }

    @Test
    void getRelationshipsByTrackedEntityRelationshipsNoAccessToRelationshipItemTo()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstanceNotInSearchScope();
        relationship( from, to );
        this.switchContextToUser( user );

        assertNoRelationships( GET( "/tracker/relationships?trackedEntity={tei}", from.getUid() )
            .content( HttpStatus.OK ) );
    }

    @Test
    void getRelationshipsByTrackedEntityRelationshipsNoAccessToBothRelationshipItems()
    {
        TrackedEntityInstance from = trackedEntityInstanceNotInSearchScope();
        TrackedEntityInstance to = trackedEntityInstanceNotInSearchScope();
        relationship( from, to );
        this.switchContextToUser( user );

        assertNoRelationships( GET( "/tracker/relationships?trackedEntity={tei}", from.getUid() )
            .content( HttpStatus.OK ) );
    }

    @Test
    void getRelationshipsByTrackedEntityRelationshipsNoAccessToRelationshipItemFrom()
    {
        TrackedEntityInstance from = trackedEntityInstanceNotInSearchScope();
        TrackedEntityInstance to = trackedEntityInstance();
        relationship( from, to );
        this.switchContextToUser( user );

        assertNoRelationships( GET( "/tracker/relationships?trackedEntity={tei}", from.getUid() )
            .content( HttpStatus.OK ) );
    }

    @Test
    void getRelationshipsByTrackedEntityRelationshipsNoAccessToTrackedEntityType()
    {
        TrackedEntityType type = trackedEntityTypeNotAccessible();
        TrackedEntityInstance from = trackedEntityInstance( type );
        TrackedEntityInstance to = trackedEntityInstance( type );
        relationship( from, to );
        this.switchContextToUser( user );

        assertNoRelationships( GET( "/tracker/relationships?trackedEntity={tei}", from.getUid() )
            .content( HttpStatus.OK ) );
    }

    @Test
    void getRelationshipsByTrackedEntityNotFound()
    {
        assertEquals( "No trackedEntity 'Hq3Kc6HK4OZ' found.",
            GET( "/tracker/relationships?trackedEntity=Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    private TrackedEntityType trackedEntityTypeAccessible()
    {
        TrackedEntityType type = trackedEntityType( 'A' );
        type.getSharing().addUserAccess( userAccess() );
        manager.save( type, false );
        return type;
    }

    private TrackedEntityType trackedEntityTypeNotAccessible()
    {
        TrackedEntityType type = trackedEntityType( 'B' );
        manager.save( type, false );
        return type;
    }

    private TrackedEntityType trackedEntityType( char uniqueChar )
    {
        TrackedEntityType type = createTrackedEntityType( uniqueChar );
        type.getSharing().setOwner( owner );
        type.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        return type;
    }

    private TrackedEntityInstance trackedEntityInstance()
    {
        TrackedEntityInstance tei = trackedEntityInstance( orgUnit );
        manager.save( tei, false );
        return tei;
    }

    private TrackedEntityInstance trackedEntityInstanceNotInSearchScope()
    {
        TrackedEntityInstance tei = trackedEntityInstance( anotherOrgUnit );
        manager.save( tei, false );
        return tei;
    }

    private TrackedEntityInstance trackedEntityInstance( TrackedEntityType trackedEntityType )
    {
        TrackedEntityInstance tei = trackedEntityInstance( orgUnit, trackedEntityType );
        manager.save( tei, false );
        return tei;
    }

    private TrackedEntityInstance trackedEntityInstance( OrganisationUnit orgUnit )
    {
        return trackedEntityInstance( orgUnit, trackedEntityType );
    }

    private TrackedEntityInstance trackedEntityInstance( OrganisationUnit orgUnit, TrackedEntityType trackedEntityType )
    {
        TrackedEntityInstance tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        tei.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        tei.getSharing().setOwner( owner );
        return tei;
    }

    private ProgramInstance programInstance( TrackedEntityInstance tei )
    {
        ProgramInstance programInstance = new ProgramInstance( program, tei, orgUnit );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.COMPLETED );
        manager.save( programInstance );
        return programInstance;
    }

    private ProgramStageInstance programStageInstance( ProgramInstance programInstance )
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance( programInstance, programStage, orgUnit );
        programStageInstance.setAutoFields();
        manager.save( programStageInstance );
        return programStageInstance;
    }

    private UserAccess userAccess()
    {
        UserAccess a = new UserAccess();
        a.setUser( user );
        a.setAccess( AccessStringHelper.FULL );
        return a;
    }

    private RelationshipType relationshipTypeAccessible( RelationshipEntity from,
        RelationshipEntity to )
    {
        RelationshipType type = relationshipType( from, to );
        type.getSharing().addUserAccess( userAccess() );
        manager.save( type, false );
        return type;
    }

    private RelationshipType relationshipTypeNotAccessible()
    {
        return relationshipType( RelationshipEntity.TRACKED_ENTITY_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
    }

    private RelationshipType relationshipType( RelationshipEntity from, RelationshipEntity to )
    {
        RelationshipType type = createRelationshipType( 'A' );
        type.getFromConstraint().setRelationshipEntity( from );
        type.getToConstraint().setRelationshipEntity( to );
        type.getSharing().setOwner( owner );
        type.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( type, false );
        return type;
    }

    private Relationship relationship( TrackedEntityInstance from, TrackedEntityInstance to )
    {

        RelationshipType type = relationshipTypeAccessible( RelationshipEntity.TRACKED_ENTITY_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        return relationship( type, from, to );
    }

    private Relationship relationship( RelationshipType type, TrackedEntityInstance from, TrackedEntityInstance to )
    {
        Relationship r = new Relationship();

        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setTrackedEntityInstance( from );
        from.getRelationshipItems().add( fromItem );
        r.setFrom( fromItem );
        fromItem.setRelationship( r );

        RelationshipItem toItem = new RelationshipItem();
        toItem.setTrackedEntityInstance( to );
        to.getRelationshipItems().add( toItem );
        r.setTo( toItem );
        toItem.setRelationship( r );

        r.setRelationshipType( type );
        r.setKey( type.getUid() );
        r.setInvertedKey( type.getUid() );
        r.setAutoFields();
        r.getSharing().setOwner( owner );
        manager.save( r, false );
        return r;
    }

    private Relationship relationship( ProgramStageInstance from, TrackedEntityInstance to )
    {
        Relationship r = new Relationship();

        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setProgramStageInstance( from );
        from.getRelationshipItems().add( fromItem );
        r.setFrom( fromItem );
        fromItem.setRelationship( r );

        RelationshipItem toItem = new RelationshipItem();
        toItem.setTrackedEntityInstance( to );
        to.getRelationshipItems().add( toItem );
        r.setTo( toItem );
        toItem.setRelationship( r );

        RelationshipType type = relationshipTypeAccessible(
            RelationshipEntity.PROGRAM_STAGE_INSTANCE, RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        r.setRelationshipType( type );
        r.setKey( type.getUid() );
        r.setInvertedKey( type.getUid() );

        r.setAutoFields();
        r.getSharing().setOwner( owner );
        manager.save( r, false );
        return r;
    }

    private Relationship relationship( ProgramInstance from, TrackedEntityInstance to )
    {
        Relationship r = new Relationship();

        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setProgramInstance( from );
        from.getRelationshipItems().add( fromItem );
        r.setFrom( fromItem );
        fromItem.setRelationship( r );

        RelationshipItem toItem = new RelationshipItem();
        toItem.setTrackedEntityInstance( to );
        to.getRelationshipItems().add( toItem );
        r.setTo( toItem );
        toItem.setRelationship( r );

        RelationshipType type = relationshipTypeAccessible(
            RelationshipEntity.PROGRAM_INSTANCE, RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        r.setRelationshipType( type );
        r.setKey( type.getUid() );
        r.setInvertedKey( type.getUid() );

        r.setAutoFields();
        r.getSharing().setOwner( owner );
        manager.save( r, false );
        return r;
    }

}