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

import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertHasOnlyMembers;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertRelationship;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.UserInfoSnapshot;
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

class TrackerEventsExportControllerTest extends DhisControllerConvenienceTest
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
    void getEventById()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        relationship( from, to );

        JsonObject json = GET( "/tracker/events/{id}", from.getUid() )
            .content( HttpStatus.OK );

        assertDefaultResponse( json, from );
    }

    @Test
    void getEventByIdWithFields()
    {
        TrackedEntityInstance tei = trackedEntityInstance();
        ProgramStageInstance event = programStageInstance( programInstance( tei ) );

        JsonObject json = GET( "/tracker/events/{id}?fields=orgUnit,status", event.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertHasOnlyMembers( json, "orgUnit", "status" );
    }

    @Test
    void getEventByIdWithFieldsRelationships()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        Relationship r = relationship( from, to );

        JsonObject json = GET( "/tracker/events/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray rels = json.getArray( "relationships" );
        assertFalse( rels.isEmpty(),
            "relationships are currently returned even if we exclude them using the fields param" );
        assertEquals( 1, rels.size() );
        assertRelationship( r, rels.getObject( 0 ) );
        assertEventWithinRelationship( from, rels.getObject( 0 ).getObject( "from" ) );
        assertTrackedEntityWithinRelationship( to, rels.getObject( 0 ).getObject( "to" ) );
    }

    @Test
    void getEventByIdRelationshipsNoAccessToRelationshipType()
    {
        TrackedEntityInstance to = trackedEntityInstance();
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        relationship( relationshipTypeNotAccessible(), from, to );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/events/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "relationships" );
        assertEquals( 1, relationships.size(),
            "other endpoints return an empty relationships array, this one currently returns [null]" );
        assertTrue( relationships.get( 0 ).isNull(),
            "other endpoints return an empty relationships array, this one currently returns [null]" );
    }

    @Test
    void getEventByIdRelationshipsNoAccessToRelationshipItemTo()
    {
        TrackedEntityType type = trackedEntityTypeNotAccessible();
        TrackedEntityInstance to = trackedEntityInstance( type );
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        relationship( from, to );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/events/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "relationships" );
        assertEquals( 1, relationships.size(),
            "other endpoints return an empty relationships array, this one currently returns [null]" );
        assertTrue( relationships.get( 0 ).isNull(),
            "other endpoints return an empty relationships array, this one currently returns [null]" );
    }

    @Test
    void getEventByIdRelationshipsNoAccessToBothRelationshipItems()
    {
        TrackedEntityInstance to = trackedEntityInstanceNotInSearchScope();
        ProgramStageInstance from = programStageInstance( programInstance( to ) );
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/events/{id}", from.getUid() )
            .error( HttpStatus.CONFLICT ).getMessage()
            .contains( "OWNERSHIP_ACCESS_DENIED" ) );
    }

    @Test
    void getEventByIdRelationshipsNoAccessToRelationshipItemFrom()
    {
        TrackedEntityType type = trackedEntityTypeNotAccessible();
        TrackedEntityInstance from = trackedEntityInstance( type );
        ProgramStageInstance to = programStageInstance( programInstance( from ) );
        relationship( from, to );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/events/{id}?fields=relationships", to.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        JsonArray relationships = json.getArray( "relationships" );
        assertEquals( 1, relationships.size(),
            "other endpoints return an empty relationships array, this one currently returns [null]" );
        assertTrue( relationships.get( 0 ).isNull(),
            "other endpoints return an empty relationships array, this one currently returns [null]" );
    }

    @Test
    void getEventByIdContainsCreatedByAndUpdateByInDataValues()
    {

        TrackedEntityInstance tei = trackedEntityInstance();
        ProgramInstance programInstance = programInstance( tei );
        ProgramStageInstance programStageInstance = programStageInstance( programInstance );
        programStageInstance.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );
        programStageInstance.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        EventDataValue eventDataValue = new EventDataValue();
        eventDataValue.setValue( "6" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setValueType( ValueType.NUMBER );
        manager.save( dataElement );
        eventDataValue.setDataElement( dataElement.getUid() );
        eventDataValue.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );
        eventDataValue.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        Set<EventDataValue> eventDataValues = Set.of( eventDataValue );
        programStageInstance.setEventDataValues( eventDataValues );
        manager.save( programStageInstance );

        JsonObject event = GET( "/tracker/events/{id}", programStageInstance.getUid() ).content( HttpStatus.OK );

        assertTrue( event.isObject() );
        assertFalse( event.isEmpty() );
        assertEquals( programStageInstance.getUid(), event.getString( "event" ).string() );
        assertEquals( programInstance.getUid(), event.getString( "enrollment" ).string() );
        assertEquals( orgUnit.getUid(), event.getString( "orgUnit" ).string() );
        assertEquals( user.getUsername(), event.getString( "createdBy.username" ).string() );
        assertEquals( user.getUsername(), event.getString( "updatedBy.username" ).string() );
        assertFalse( event.getArray( "dataValues" ).isEmpty() );
        assertEquals( user.getUsername(),
            event.getArray( "dataValues" ).getObject( 0 ).getString( "createdBy.username" ).string() );
        assertEquals( user.getUsername(),
            event.getArray( "dataValues" ).getObject( 0 ).getString( "updatedBy.username" ).string() );
    }

    @Test
    void getEventByIdNotFound()
    {
        assertEquals( "Event not found for uid: Hq3Kc6HK4OZ",
            GET( "/tracker/events/Hq3Kc6HK4OZ" )
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
        ProgramInstance programInstance = new ProgramInstance( program, tei, tei.getOrganisationUnit() );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.COMPLETED );
        manager.save( programInstance );
        return programInstance;
    }

    private ProgramStageInstance programStageInstance( ProgramInstance programInstance )
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance( programInstance, programStage,
            programInstance.getOrganisationUnit() );
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
        return relationshipType( RelationshipEntity.PROGRAM_STAGE_INSTANCE,
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

    private Relationship relationship( ProgramStageInstance from, TrackedEntityInstance to )
    {
        return relationship(
            relationshipTypeAccessible( RelationshipEntity.PROGRAM_STAGE_INSTANCE,
                RelationshipEntity.TRACKED_ENTITY_INSTANCE ),
            from,
            to );
    }

    private Relationship relationship( RelationshipType type, ProgramStageInstance from, TrackedEntityInstance to )
    {
        Relationship r = new Relationship();

        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setProgramStageInstance( from );
        from.getRelationshipItems().add( fromItem );
        fromItem.setRelationship( r );
        r.setFrom( fromItem );

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

    private Relationship relationship( TrackedEntityInstance from, ProgramStageInstance to )
    {
        Relationship r = new Relationship();

        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setTrackedEntityInstance( from );
        from.getRelationshipItems().add( fromItem );
        r.setFrom( fromItem );
        fromItem.setRelationship( r );

        RelationshipItem toItem = new RelationshipItem();
        toItem.setProgramStageInstance( to );
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

    private void assertEventWithinRelationship( ProgramStageInstance expected, JsonObject json )
    {
        JsonObject jsonEvent = json.getObject( "event" );
        assertFalse( jsonEvent.isEmpty(), "event should not be empty" );
        assertEquals( expected.getUid(), jsonEvent.getString( "event" ).string(), "event UID" );
        assertFalse( jsonEvent.has( "status" ) );
        assertFalse( jsonEvent.has( "orgUnit" ) );
        assertFalse( jsonEvent.has( "programStage" ) );
        assertFalse( jsonEvent.has( "relationships" ), "relationships is not returned within relationship items" );
    }

    private void assertTrackedEntityWithinRelationship( TrackedEntityInstance expected, JsonObject json )
    {
        JsonObject jsonTEI = json.getObject( "trackedEntity" );
        assertFalse( jsonTEI.isEmpty(), "trackedEntity should not be empty" );
        assertEquals( expected.getUid(), jsonTEI.getString( "trackedEntity" ).string(), "trackedEntity UID" );
        assertFalse( jsonTEI.has( "trackedEntityType" ) );
        assertFalse( jsonTEI.has( "orgUnit" ) );
        assertFalse( jsonTEI.has( "relationships" ), "relationships is not returned within relationship items" );
        assertTrue( jsonTEI.getArray( "attributes" ).isEmpty() );
    }

    private void assertDefaultResponse( JsonObject json, ProgramStageInstance programStageInstance )
    {
        // note that some fields are not included in the response because they
        // are not part of the setup
        // i.e attributeOptionCombo, ...
        assertTrue( json.isObject() );
        assertFalse( json.isEmpty() );
        assertEquals( programStageInstance.getUid(), json.getString( "event" ).string(), "event UID" );
        assertEquals( "ACTIVE", json.getString( "status" ).string() );
        assertEquals( program.getUid(), json.getString( "program" ).string() );
        assertEquals( programStage.getUid(), json.getString( "programStage" ).string() );
        assertEquals( programStageInstance.getProgramInstance().getUid(), json.getString( "enrollment" ).string() );
        assertEquals( orgUnit.getUid(), json.getString( "orgUnit" ).string() );
        assertEquals( orgUnit.getName(), json.getString( "orgUnitName" ).string() );
        assertFalse( json.getBoolean( "followup" ).booleanValue() );
        assertFalse( json.getBoolean( "deleted" ).booleanValue() );
        assertHasMember( json, "createdAt" );
        assertHasMember( json, "createdAtClient" );
        assertHasMember( json, "updatedAt" );
        assertHasMember( json, "updatedAtClient" );
        assertHasMember( json, "dataValues" );
        assertHasMember( json, "notes" );
        assertHasNoMember( json, "attributeOptionCombo" );
        assertHasNoMember( json, "attributeCategoryOptions" );
        assertHasNoMember( json, "relationships" );
    }

}