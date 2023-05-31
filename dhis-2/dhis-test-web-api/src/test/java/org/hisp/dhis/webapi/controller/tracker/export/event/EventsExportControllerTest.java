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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonDataValue;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonNote;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationshipItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventsExportControllerTest extends DhisControllerConvenienceTest
{
    private static final String DATA_ELEMENT_VALUE = "value";

    @Autowired
    private IdentifiableObjectManager manager;

    private OrganisationUnit orgUnit;

    private OrganisationUnit anotherOrgUnit;

    private Program program;

    private ProgramStage programStage;

    private User owner;

    private User user;

    private TrackedEntityType trackedEntityType;

    private EventDataValue dv;

    private DataElement de;

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

        de = createDataElement( 'A' );
        de.getSharing().setOwner( owner );
        manager.save( de, false );

        dv = new EventDataValue();
        dv.setDataElement( de.getUid() );
        dv.setStoredBy( "user" );
        dv.setValue( DATA_ELEMENT_VALUE );

        trackedEntityType = trackedEntityTypeAccessible();
    }

    @Test
    void getEventById()
    {
        Event event = event( enrollment( trackedEntity() ) );

        JsonEvent json = GET( "/tracker/events/{id}", event.getUid() )
            .content( HttpStatus.OK ).as( JsonEvent.class );

        assertDefaultResponse( json, event );
    }

    @Test
    void getEventByIdWithFields()
    {
        Event event = event( enrollment( trackedEntity() ) );

        JsonEvent jsonEvent = GET( "/tracker/events/{id}?fields=orgUnit,status", event.getUid() )
            .content( HttpStatus.OK ).as( JsonEvent.class );

        assertHasOnlyMembers( jsonEvent, "orgUnit", "status" );
        assertEquals( event.getOrganisationUnit().getUid(), jsonEvent.getOrgUnit() );
        assertEquals( event.getStatus().toString(), jsonEvent.getStatus() );
    }

    @Test
    void getEventByIdWithNotes()
    {
        Event event = event( enrollment( trackedEntity() ) );
        event.setComments( List.of( note( "oqXG28h988k", "my notes", owner.getUid() ) ) );
        manager.update( event );

        JsonEvent jsonEvent = GET( "/tracker/events/{uid}?fields=notes", event.getUid() )
            .content( HttpStatus.OK ).as( JsonEvent.class );

        JsonNote note = jsonEvent.getNotes().get( 0 );
        assertEquals( "oqXG28h988k", note.getNote() );
        assertEquals( "my notes", note.getValue() );
        assertEquals( owner.getUid(), note.getStoredBy() );
    }

    @Test
    void getEventByIdWithDataValues()
    {
        Event event = event( enrollment( trackedEntity() ) );
        event.getEventDataValues().add( dv );
        manager.update( event );

        JsonEvent eventJson = GET( "/tracker/events/{id}?fields=dataValues", event.getUid() )
            .content( HttpStatus.OK ).as( JsonEvent.class );

        assertHasOnlyMembers( eventJson, "dataValues" );
        JsonDataValue dataValue = eventJson.getDataValues().get( 0 );
        assertEquals( de.getUid(), dataValue.getDataElement() );
        assertEquals( dv.getValue(), dataValue.getValue() );
        assertHasMember( dataValue, "createdAt" );
        assertHasMember( dataValue, "updatedAt" );
        assertHasMember( dataValue, "storedBy" );
    }

    @Test
    void getEventByIdWithFieldsRelationships()
    {
        TrackedEntity to = trackedEntity();
        Event from = event( enrollment( to ) );
        Relationship relationship = relationship( from, to );

        JsonList<JsonRelationship> relationships = GET( "/tracker/events/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK ).getList( "relationships", JsonRelationship.class );

        JsonRelationship jsonRelationship = relationships.get( 0 );
        assertEquals( relationship.getUid(), jsonRelationship.getRelationship() );

        JsonRelationshipItem.JsonEvent event = jsonRelationship.getFrom().getEvent();
        assertEquals( relationship.getFrom().getEvent().getUid(), event.getEvent() );
        assertEquals( relationship.getFrom().getEvent().getEnrollment().getUid(),
            event.getEnrollment() );

        JsonRelationshipItem.JsonTrackedEntity trackedEntity = jsonRelationship.getTo().getTrackedEntity();
        assertEquals( relationship.getTo().getTrackedEntity().getUid(), trackedEntity.getTrackedEntity() );

        assertHasMember( jsonRelationship, "relationshipName" );
        assertHasMember( jsonRelationship, "relationshipType" );
        assertHasMember( jsonRelationship, "createdAt" );
        assertHasMember( jsonRelationship, "updatedAt" );
        assertHasMember( jsonRelationship, "bidirectional" );
    }

    @Test
    void getEventByIdRelationshipsNoAccessToRelationshipType()
    {
        TrackedEntity to = trackedEntity();
        Event from = event( enrollment( to ) );
        relationship( relationshipTypeNotAccessible(), from, to );
        this.switchContextToUser( user );

        JsonList<JsonRelationship> relationships = GET( "/tracker/events/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK ).getList( "relationships", JsonRelationship.class );

        assertEquals( 0, relationships.size() );
    }

    @Test
    void getEventByIdRelationshipsNoAccessToRelationshipItemTo()
    {
        TrackedEntityType type = trackedEntityTypeNotAccessible();
        TrackedEntity to = trackedEntity( type );
        Event from = event( enrollment( to ) );
        relationship( from, to );
        this.switchContextToUser( user );

        JsonList<JsonRelationship> relationships = GET( "/tracker/events/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK ).getList( "relationships", JsonRelationship.class );

        assertEquals( 0, relationships.size() );
    }

    @Test
    void getEventByIdRelationshipsNoAccessToBothRelationshipItems()
    {
        TrackedEntity to = trackedEntityNotInSearchScope();
        Event from = event( enrollment( to ) );
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/events/{id}", from.getUid() )
            .error( HttpStatus.FORBIDDEN ).getMessage()
            .contains( "OWNERSHIP_ACCESS_DENIED" ) );
    }

    @Test
    void getEventByIdRelationshipsNoAccessToRelationshipItemFrom()
    {
        TrackedEntityType type = trackedEntityTypeNotAccessible();
        TrackedEntity from = trackedEntity( type );
        Event to = event( enrollment( from ) );
        relationship( from, to );
        this.switchContextToUser( user );

        JsonList<JsonRelationship> relationships = GET( "/tracker/events/{id}?fields=relationships", to.getUid() )
            .content( HttpStatus.OK ).getList( "relationships", JsonRelationship.class );

        assertEquals( 0, relationships.size() );
    }

    @Test
    void getEventByIdContainsCreatedByAndUpdateByAndAssignedUserInDataValues()
    {

        TrackedEntity tei = trackedEntity();
        Enrollment enrollment = enrollment( tei );
        Event programStageInstance = event( enrollment );
        programStageInstance.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );
        programStageInstance.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        programStageInstance.setAssignedUser( user );
        EventDataValue eventDataValue = new EventDataValue();
        eventDataValue.setValue( "6" );

        eventDataValue.setDataElement( de.getUid() );
        eventDataValue.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );
        eventDataValue.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        Set<EventDataValue> eventDataValues = Set.of( eventDataValue );
        programStageInstance.setEventDataValues( eventDataValues );
        manager.save( programStageInstance );

        JsonObject event = GET( "/tracker/events/{id}", programStageInstance.getUid() ).content( HttpStatus.OK );

        assertTrue( event.isObject() );
        assertFalse( event.isEmpty() );
        assertEquals( programStageInstance.getUid(), event.getString( "event" ).string() );
        assertEquals( enrollment.getUid(), event.getString( "enrollment" ).string() );
        assertEquals( orgUnit.getUid(), event.getString( "orgUnit" ).string() );
        assertEquals( user.getUsername(), event.getString( "createdBy.username" ).string() );
        assertEquals( user.getUsername(), event.getString( "updatedBy.username" ).string() );
        assertEquals( user.getDisplayName(), event.getString( "assignedUser.displayName" ).string() );
        assertFalse( event.getArray( "dataValues" ).isEmpty() );
        assertEquals( user.getUsername(),
            event.getArray( "dataValues" ).getObject( 0 ).getString( "createdBy.username" ).string() );
        assertEquals( user.getUsername(),
            event.getArray( "dataValues" ).getObject( 0 ).getString( "updatedBy.username" ).string() );
    }

    @Test
    void getEventByIdNotFound()
    {
        assertEquals( "Event with id Hq3Kc6HK4OZ could not be found.",
            GET( "/tracker/events/Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    @Test
    void getEventsFailsIfGivenAttributeCategoryOptionsAndDeprecatedAttributeCos()
    {
        assertStartsWith( "Only one parameter of 'attributeCos' (deprecated",
            GET( "/tracker/events?attributeCategoryOptions=Hq3Kc6HK4OZ&attributeCos=Hq3Kc6HK4OZ" )
                .error( HttpStatus.BAD_REQUEST )
                .getMessage() );
    }

    @Test
    void getEventsFailsIfGivenAttributeCcAndAttributeCategoryCombo()
    {
        assertStartsWith( "Only one parameter of 'attributeCc' and 'attributeCategoryCombo'",
            GET( "/tracker/events?attributeCc=FQnYqKlIHxd&attributeCategoryCombo=YApXsOpwiXk" )
                .error( HttpStatus.BAD_REQUEST )
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

    private TrackedEntity trackedEntity()
    {
        TrackedEntity tei = trackedEntity( orgUnit );
        manager.save( tei, false );
        return tei;
    }

    private TrackedEntity trackedEntityNotInSearchScope()
    {
        TrackedEntity tei = trackedEntity( anotherOrgUnit );
        manager.save( tei, false );
        return tei;
    }

    private TrackedEntity trackedEntity( TrackedEntityType trackedEntityType )
    {
        TrackedEntity tei = trackedEntity( orgUnit, trackedEntityType );
        manager.save( tei, false );
        return tei;
    }

    private TrackedEntity trackedEntity( OrganisationUnit orgUnit )
    {
        return trackedEntity( orgUnit, trackedEntityType );
    }

    private TrackedEntity trackedEntity( OrganisationUnit orgUnit, TrackedEntityType trackedEntityType )
    {
        TrackedEntity tei = createTrackedEntity( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        tei.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        tei.getSharing().setOwner( owner );
        return tei;
    }

    private Enrollment enrollment( TrackedEntity tei )
    {
        Enrollment enrollment = new Enrollment( program, tei, tei.getOrganisationUnit() );
        enrollment.setAutoFields();
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setIncidentDate( new Date() );
        enrollment.setStatus( ProgramStatus.COMPLETED );
        manager.save( enrollment );
        return enrollment;
    }

    private Event event( Enrollment enrollment )
    {
        Event event = new Event( enrollment, programStage,
            enrollment.getOrganisationUnit() );
        event.setAutoFields();
        manager.save( event );
        return event;
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

    private Relationship relationship( Event from, TrackedEntity to )
    {
        return relationship(
            relationshipTypeAccessible( RelationshipEntity.PROGRAM_STAGE_INSTANCE,
                RelationshipEntity.TRACKED_ENTITY_INSTANCE ),
            from,
            to );
    }

    private Relationship relationship( RelationshipType type, Event from, TrackedEntity to )
    {
        Relationship r = new Relationship();

        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setEvent( from );
        from.getRelationshipItems().add( fromItem );
        fromItem.setRelationship( r );
        r.setFrom( fromItem );

        RelationshipItem toItem = new RelationshipItem();
        toItem.setTrackedEntity( to );
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

    private Relationship relationship( TrackedEntity from, Event to )
    {
        Relationship r = new Relationship();

        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setTrackedEntity( from );
        from.getRelationshipItems().add( fromItem );
        r.setFrom( fromItem );
        fromItem.setRelationship( r );

        RelationshipItem toItem = new RelationshipItem();
        toItem.setEvent( to );
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

    private TrackedEntityComment note( String note, String value, String storedBy )
    {
        TrackedEntityComment comment = new TrackedEntityComment( value, storedBy );
        comment.setUid( note );
        manager.save( comment, false );
        return comment;
    }

    private void assertDefaultResponse( JsonObject json, Event event )
    {
        // note that some fields are not included in the response because they
        // are not part of the setup
        // i.e attributeOptionCombo, ...
        assertTrue( json.isObject() );
        assertFalse( json.isEmpty() );
        assertEquals( event.getUid(), json.getString( "event" ).string(), "event UID" );
        assertEquals( "ACTIVE", json.getString( "status" ).string() );
        assertEquals( program.getUid(), json.getString( "program" ).string() );
        assertEquals( programStage.getUid(), json.getString( "programStage" ).string() );
        assertEquals( event.getEnrollment().getUid(), json.getString( "enrollment" ).string() );
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