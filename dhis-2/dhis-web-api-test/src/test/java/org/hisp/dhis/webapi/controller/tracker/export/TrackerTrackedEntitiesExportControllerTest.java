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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertFirstRelationship;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
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
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
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
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class TrackerTrackedEntitiesExportControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ProgramInstanceService programInstanceService;

    private OrganisationUnit orgUnit;

    private OrganisationUnit anotherOrgUnit;

    private Program program;

    private ProgramStage programStage;

    private TrackedEntityType trackedEntityType;

    private DataElement dataElement;

    private User owner;

    private User user;

    @BeforeEach
    void setUp()
    {
        owner = createUser( "owner" );

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
        programStage = createProgramStage( 'A', program );
        programStage.getSharing().setOwner( owner );
        programStage.getSharing().addUserAccess( userAccess() );
        manager.save( programStage, false );

        trackedEntityType = trackedEntityTypeAccessible();
    }

    @Test
    void getTrackedEntitiesNeedsProgramOrType()
    {
        assertEquals( "Either Program or Tracked entity type should be specified",
            GET( "/tracker/trackedEntities" )
                .error( HttpStatus.CONFLICT )
                .getMessage() );
    }

    @Test
    @Ignore( "failed on 38, need investigation" )
    void getTrackedEntitiesNeedsProgramOrTrackedEntityType()
    {
        this.switchContextToUser( user );

        assertEquals( "Either Program or Tracked entity type should be specified",
            GET( "/tracker/trackedEntities?orgUnit={ou}", orgUnit.getUid() )
                .error( HttpStatus.CONFLICT )
                .getMessage() );
    }

    @Test
    void getTrackedEntitiesNeedsAtLeastOneOrgUnit()
    {
        assertEquals( "At least one organisation unit must be specified",
            GET( "/tracker/trackedEntities?program={program}", program.getUid() )
                .error( HttpStatus.CONFLICT )
                .getMessage() );
    }

    @Test
    void getTrackedEntityById()
    {
        TrackedEntityInstance tei = trackedEntityInstance();
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities/{id}", tei.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertEquals( tei.getUid(), json.getString( "trackedEntity" ).string() );
        assertEquals( tei.getTrackedEntityType().getUid(), json.getString( "trackedEntityType" ).string() );
        assertEquals( tei.getOrganisationUnit().getUid(), json.getString( "orgUnit" ).string() );
        assertHasMember( json, "createdAt" );
        assertHasMember( json, "createdAtClient" );
        assertHasMember( json, "updatedAtClient" );
        assertHasNoMember( json, "relationships" );
        assertHasNoMember( json, "enrollments" );
        assertHasNoMember( json, "events" );
        assertHasNoMember( json, "programOwners" );
    }

    @Test
    void getTrackedEntityByIdWithFields()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstance();
        relationship( from, to );
        this.switchContextToUser( user );

        JsonObject json = GET(
            "/tracker/trackedEntities/{id}?fields=trackedEntity,orgUnit,relationships[relationship,relationshipType]",
            from.getUid() )
                .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertHasOnlyMembers( json, "trackedEntity", "orgUnit", "relationships" );
        assertHasOnlyMembers( json.getArray( "relationships" ).getObject( 0 ), "relationship", "relationshipType" );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationships()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstance();
        Relationship r = relationship( from, to );
        this.switchContextToUser( user );

        JsonList<JsonRelationship> rels = GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK ).getList( "relationships", JsonRelationship.class );

        assertEquals( 1, rels.size() );
        JsonRelationship relationship = assertFirstRelationship( r, rels );
        assertTrackedEntityWithinRelationship( from, relationship.getFrom() );
        assertTrackedEntityWithinRelationship( to, relationship.getTo() );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipType()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstance();
        relationship( relationshipTypeNotAccessible(), fromTrackedEntity( from ), toTrackedEntity( to ) );
        this.switchContextToUser( user );

        JsonList<JsonRelationship> relationships = GET( "/tracker/trackedEntities/{id}?fields=relationships",
            from.getUid() )
                .content( HttpStatus.OK ).getList( "relationships", JsonRelationship.class );

        assertEquals( 0, relationships.size(), "user needs access to relationship type to access the relationship" );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipItemTo()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstanceNotInSearchScope();
        relationship( from, to );
        this.switchContextToUser( user );

        JsonList<JsonRelationship> relationships = GET( "/tracker/trackedEntities/{id}?fields=relationships",
            from.getUid() )
                .content( HttpStatus.OK ).getList( "relationships", JsonRelationship.class );

        assertEquals( 0, relationships.size(), "user needs access to from and to items to access the relationship" );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToBothRelationshipItems()
    {
        TrackedEntityInstance from = trackedEntityInstanceNotInSearchScope();
        TrackedEntityInstance to = trackedEntityInstanceNotInSearchScope();
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .error( HttpStatus.CONFLICT ).getMessage()
            .contains( "User has no read access to organisation unit" ) );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipItemFrom()
    {
        TrackedEntityInstance from = trackedEntityInstanceNotInSearchScope();
        TrackedEntityInstance to = trackedEntityInstance();
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .error( HttpStatus.CONFLICT ).getMessage()
            .contains( "User has no read access to organisation unit" ) );
    }

    @Test
    void getTrackedEntityByIdyWithFieldsRelationshipsNoAccessToTrackedEntityType()
    {
        TrackedEntityType type = trackedEntityTypeNotAccessible();
        TrackedEntityInstance from = trackedEntityInstance( type );
        TrackedEntityInstance to = trackedEntityInstance( type );
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .error( HttpStatus.CONFLICT ).getMessage()
            .contains( "User has no data read access to tracked entity" ) );
    }

    @Test
    void getTrackedEntityByIdNotFound()
    {
        assertEquals( "TrackedEntityInstance not found for uid: Hq3Kc6HK4OZ",
            GET( "/tracker/trackedEntities/Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    @Test
    void shouldGetEnrollmentWhenFieldsHasEnrollments()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstance();

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( trackedEntityInstance,
            program, new Date(), new Date(), orgUnit );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=enrollments", trackedEntityInstance.getUid() )
            .content( HttpStatus.OK );

        JsonObject enrollment = assertDefaultEnrollmentResponse( json, programInstance );

        assertTrue( enrollment.getArray( "relationships" ).isEmpty() );
        assertTrue( enrollment.getArray( "attributes" ).isEmpty() );
        assertTrue( enrollment.getArray( "events" ).isEmpty() );
    }

    @Test
    void shouldGetNoEventRelationshipsWhenEventsHasNoRelationshipsAndFieldsIncludeAll()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstance();

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( trackedEntityInstance,
            program, new Date(), new Date(), orgUnit );

        ProgramStageInstance programStageInstance = programStageInstanceWithDataValue( programInstance );

        programInstance.getProgramStageInstances().add( programStageInstance );
        manager.update( programInstance );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=enrollments", trackedEntityInstance.getUid() )
            .content( HttpStatus.OK );

        JsonObject enrollment = assertDefaultEnrollmentResponse( json, programInstance );
        assertTrue( enrollment.getArray( "relationships" ).isEmpty() );
        assertTrue( enrollment.getArray( "attributes" ).isEmpty() );

        JsonObject event = assertDefaultEventResponse( enrollment, programStageInstance );

        assertTrue( event.getArray( "relationships" ).isEmpty() );
    }

    @Test
    void shouldGetEventRelationshipsWhenEventHasRelationshipsAndFieldsIncludeEventRelationships()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstance();

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( trackedEntityInstance,
            program, new Date(), new Date(), orgUnit );

        ProgramStageInstance programStageInstance = programStageInstanceWithDataValue( programInstance );

        programInstance.getProgramStageInstances().add( programStageInstance );
        manager.update( programInstance );

        Relationship teiToEventRelationship = relationship( trackedEntityInstance,
            programStageInstance );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=enrollments", trackedEntityInstance.getUid() )
            .content( HttpStatus.OK );

        JsonObject enrollment = assertDefaultEnrollmentResponse( json, programInstance );
        assertTrue( enrollment.getArray( "attributes" ).isEmpty() );
        assertTrue( enrollment.getArray( "relationships" ).isEmpty() );

        JsonObject event = assertDefaultEventResponse( enrollment, programStageInstance );

        JsonObject relationship = event.getArray( "relationships" ).get( 0 ).as( JsonObject.class );

        assertEquals( teiToEventRelationship.getUid(), relationship.getString( "relationship" ).string() );
        assertEquals( trackedEntityInstance.getUid(),
            relationship.getObject( "from" ).getObject( "trackedEntity" ).getString( "trackedEntity" ).string() );
        assertEquals( programStageInstance.getUid(),
            relationship.getObject( "to" ).getObject( "event" ).getString( "event" ).string() );
    }

    @Test
    void shouldGetNoEventRelationshipsWhenEventHasRelationshipsAndFieldsExcludeEventRelationships()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstance();

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( trackedEntityInstance,
            program, new Date(), new Date(), orgUnit );

        ProgramStageInstance programStageInstance = programStageInstanceWithDataValue( programInstance );

        programInstance.getProgramStageInstances().add( programStageInstance );
        manager.update( programInstance );

        relationship( trackedEntityInstance, programStageInstance );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=enrollments[*,events[!relationships]]",
            trackedEntityInstance.getUid() )
                .content( HttpStatus.OK );

        JsonObject enrollment = assertDefaultEnrollmentResponse( json, programInstance );
        assertTrue( enrollment.getArray( "attributes" ).isEmpty() );
        assertTrue( enrollment.getArray( "relationships" ).isEmpty() );

        JsonObject event = assertDefaultEventResponse( enrollment, programStageInstance );

        assertHasNoMember( event, "relationships" );
    }

    private ProgramStageInstance programStageInstanceWithDataValue( ProgramInstance programInstance )
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance( programInstance, programStage,
            programInstance.getOrganisationUnit() );
        programStageInstance.setAutoFields();

        dataElement = createDataElement( 'A' );
        dataElement.setValueType( ValueType.TEXT );
        manager.save( dataElement );

        EventDataValue eventDataValue = new EventDataValue();
        eventDataValue.setValue( "value" );
        eventDataValue.setDataElement( dataElement.getUid() );
        eventDataValue.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );
        eventDataValue.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        Set<EventDataValue> eventDataValues = Set.of( eventDataValue );
        programStageInstance.setEventDataValues( eventDataValues );

        manager.save( programStageInstance );
        return programStageInstance;
    }

    private JsonObject assertDefaultEnrollmentResponse( JsonObject enrollments, ProgramInstance programInstance )
    {
        assertTrue( enrollments.isObject() );
        assertFalse( enrollments.isEmpty() );
        assertHasOnlyMembers( enrollments, "enrollments" );

        JsonObject enrollment = enrollments.getArray( "enrollments" ).get( 0 ).as( JsonObject.class );

        assertHasMember( enrollment, "enrollment" );

        assertEquals( programInstance.getUid(), enrollment.getString( "enrollment" ).string() );
        assertEquals( programInstance.getEntityInstance().getUid(), enrollment.getString( "trackedEntity" ).string() );
        assertEquals( program.getUid(), enrollment.getString( "program" ).string() );
        assertEquals( "ACTIVE", enrollment.getString( "status" ).string() );
        assertEquals( orgUnit.getUid(), enrollment.getString( "orgUnit" ).string() );
        assertEquals( orgUnit.getName(), enrollment.getString( "orgUnitName" ).string() );
        assertFalse( enrollment.getBoolean( "deleted" ).booleanValue() );
        assertHasMember( enrollment, "enrolledAt" );
        assertHasMember( enrollment, "occurredAt" );
        assertHasMember( enrollment, "createdAt" );
        assertHasMember( enrollment, "createdAtClient" );
        assertHasMember( enrollment, "updatedAt" );
        assertHasMember( enrollment, "notes" );
        assertHasMember( enrollment, "followUp" );

        return enrollment;
    }

    private JsonObject assertDefaultEventResponse( JsonObject enrollment, ProgramStageInstance programStageInstance )
    {
        assertTrue( enrollment.isObject() );
        assertFalse( enrollment.isEmpty() );

        JsonObject event = enrollment.getArray( "events" ).get( 0 ).as( JsonObject.class );

        assertEquals( programStageInstance.getUid(), event.getString( "event" ).string() );
        assertEquals( programStageInstance.getProgramStage().getUid(), event.getString( "programStage" ).string() );
        assertEquals( programStageInstance.getProgramInstance().getUid(), event.getString( "enrollment" ).string() );
        assertEquals( program.getUid(), event.getString( "program" ).string() );
        assertEquals( "ACTIVE", event.getString( "status" ).string() );
        assertEquals( orgUnit.getUid(), event.getString( "orgUnit" ).string() );
        assertEquals( orgUnit.getName(), event.getString( "orgUnitName" ).string() );
        assertFalse( event.getBoolean( "deleted" ).booleanValue() );
        assertHasMember( event, "createdAt" );
        assertHasMember( event, "createdAtClient" );
        assertHasMember( event, "updatedAt" );
        assertHasMember( event, "notes" );
        assertHasMember( event, "followup" );

        JsonObject dataValue = event.getArray( "dataValues" ).get( 0 ).as( JsonObject.class );

        assertEquals( dataElement.getUid(), dataValue.getString( "dataElement" ).string() );
        assertEquals( programStageInstance.getEventDataValues().iterator().next().getValue(),
            dataValue.getString( "value" ).string() );
        assertHasMember( dataValue, "createdAt" );
        assertHasMember( dataValue, "updatedAt" );
        assertHasMember( dataValue, "createdBy" );
        assertHasMember( dataValue, "updatedBy" );

        return event;
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
        return relationship( type, fromTrackedEntity( from ), toTrackedEntity( to ) );
    }

    private Relationship relationship( TrackedEntityInstance from, ProgramStageInstance to )
    {
        RelationshipType type = relationshipTypeAccessible( RelationshipEntity.TRACKED_ENTITY_INSTANCE,
            RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        return relationship( type, fromTrackedEntity( from ), toProgramStageInstance( to ) );
    }

    private Relationship relationship( RelationshipType type, RelationshipItem fromItem, RelationshipItem toItem )
    {
        Relationship r = new Relationship();

        r.setTo( toItem );
        toItem.setRelationship( r );

        r.setFrom( fromItem );
        fromItem.setRelationship( r );

        r.setRelationshipType( type );
        r.setKey( type.getUid() );
        r.setInvertedKey( type.getUid() );
        r.setAutoFields();
        r.getSharing().setOwner( owner );
        manager.save( r, false );
        return r;
    }

    private RelationshipItem fromTrackedEntity( TrackedEntityInstance from )
    {
        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setTrackedEntityInstance( from );
        from.getRelationshipItems().add( fromItem );
        return fromItem;
    }

    private RelationshipItem toTrackedEntity( TrackedEntityInstance to )
    {
        RelationshipItem toItem = new RelationshipItem();
        toItem.setTrackedEntityInstance( to );
        to.getRelationshipItems().add( toItem );
        return toItem;
    }

    private RelationshipItem toProgramStageInstance( ProgramStageInstance to )
    {
        RelationshipItem toItem = new RelationshipItem();
        toItem.setProgramStageInstance( to );
        to.getRelationshipItems().add( toItem );
        return toItem;
    }

    private void assertTrackedEntityWithinRelationship( TrackedEntityInstance expected, JsonObject json )
    {
        JsonObject jsonTEI = json.getObject( "trackedEntity" );
        assertFalse( jsonTEI.isEmpty(), "trackedEntity should not be empty" );
        assertEquals( expected.getUid(), jsonTEI.getString( "trackedEntity" ).string(), "trackedEntity UID" );
        assertHasNoMember( json, "trackedEntityType" );
        assertHasNoMember( json, "orgUnit" );
        assertHasNoMember( json, "relationships" ); // relationships are not
                                                    // returned within
                                                    // relationships
        assertTrue( jsonTEI.getArray( "attributes" ).isEmpty() );
    }

}