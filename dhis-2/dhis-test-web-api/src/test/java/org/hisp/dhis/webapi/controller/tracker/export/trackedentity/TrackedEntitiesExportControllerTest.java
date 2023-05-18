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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertContainsAll;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertFirstRelationship;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonAttribute;
import org.hisp.dhis.webapi.controller.tracker.JsonDataValue;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntity;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntitiesExportControllerTest extends DhisControllerConvenienceTest
{
    private static final String TEA_UID = "TvjwTPToKHO";

    private static final String EVENT_OCCURRED_AT = "2023-03-23T12:23:00.000";

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private EnrollmentService enrollmentService;

    private OrganisationUnit orgUnit;

    private OrganisationUnit anotherOrgUnit;

    private Program program;

    private ProgramStage programStage;

    private TrackedEntityType trackedEntityType;

    private DataElement dataElement;

    private User owner;

    private User user;

    private TrackedEntityAttribute tea;

    private TrackedEntityAttribute tea2;

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

        tea = createTrackedEntityAttribute( 'A' );
        tea.setUid( TEA_UID );
        tea.getSharing().setOwner( owner );
        tea.getSharing().addUserAccess( userAccess() );
        manager.save( tea, false );

        tea2 = createTrackedEntityAttribute( 'B' );
        tea2.getSharing().setOwner( owner );
        tea2.getSharing().addUserAccess( userAccess() );
        manager.save( tea2, false );
        program.setProgramAttributes( List.of( createProgramTrackedEntityAttribute( program, tea2 ) ) );
        manager.save( program, false );

        trackedEntityType = trackedEntityTypeAccessible();

        TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute( trackedEntityType,
            tea );
        trackedEntityTypeAttribute.setMandatory( false );
        trackedEntityTypeAttribute.getSharing().setOwner( owner );
        trackedEntityTypeAttribute.getSharing().addUserAccess( userAccess() );
        manager.save( trackedEntityTypeAttribute, false );

        trackedEntityType.setTrackedEntityTypeAttributes( List.of( trackedEntityTypeAttribute ) );
        manager.save( trackedEntityType, false );
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
    void getTrackedEntitiesCannotHaveRepeatedAttributes()
    {
        assertContains( "Filter for attribute " + TEA_UID + " was specified more than once.",
            GET( "/tracker/trackedEntities?filter=" + TEA_UID + ":eq:test," + TEA_UID + ":gt:test2" )
                .error( HttpStatus.BAD_REQUEST )
                .getMessage() );
    }

    @Test
    void getTrackedEntityById()
    {
        TrackedEntity tei = trackedEntity();
        this.switchContextToUser( user );

        JsonTrackedEntity json = GET( "/tracker/trackedEntities/{id}", tei.getUid() )
            .content( HttpStatus.OK ).as( JsonTrackedEntity.class );

        assertFalse( json.isEmpty() );
        assertEquals( tei.getUid(), json.getTrackedEntity() );
        assertEquals( tei.getTrackedEntityType().getUid(), json.getTrackedEntityType() );
        assertEquals( tei.getOrganisationUnit().getUid(), json.getOrgUnit() );
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
        TrackedEntity tei = trackedEntity();
        this.switchContextToUser( user );

        JsonTrackedEntity json = GET(
            "/tracker/trackedEntities/{id}?fields=trackedEntityType,orgUnit",
            tei.getUid() ).content( HttpStatus.OK ).as( JsonTrackedEntity.class );

        assertHasOnlyMembers( json, "trackedEntityType", "orgUnit" );
        assertEquals( tei.getTrackedEntityType().getUid(), json.getTrackedEntityType() );
        assertEquals( tei.getOrganisationUnit().getUid(), json.getOrgUnit() );
    }

    @Test
    void getTrackedEntityByIdWithAttributesReturnsTrackedEntityTypeAttributesOnly()
    {
        TrackedEntity trackedEntity = trackedEntity();
        trackedEntity.setTrackedEntityAttributeValues(
            Set.of( attributeValue( tea, trackedEntity, "12" ), attributeValue( tea2, trackedEntity, "24" ) ) );
        enrollmentService.enrollTrackedEntity( trackedEntity, program, new Date(), new Date(), orgUnit );

        JsonList<JsonAttribute> attributes = GET( "/tracker/trackedEntities/{id}?fields=attributes[attribute,value]",
            trackedEntity.getUid() )
                .content( HttpStatus.OK ).getList( "attributes", JsonAttribute.class );

        assertAll( "include tracked entity type attributes only if no program query param is given",
            () -> assertEquals( 1, attributes.size(),
                () -> String.format( "expected 1 attribute instead got %s", attributes ) ),
            () -> assertEquals( tea.getUid(), attributes.get( 0 ).getAttribute() ),
            () -> assertEquals( "12", attributes.get( 0 ).getValue() ) );
    }

    @Test
    void getTrackedEntityByIdWithAttributesReturnsAllAttributes()
    {
        TrackedEntity trackedEntity = trackedEntity();
        trackedEntity.setTrackedEntityAttributeValues(
            Set.of( attributeValue( tea, trackedEntity, "12" ), attributeValue( tea2, trackedEntity, "24" ) ) );
        enrollmentService.enrollTrackedEntity( trackedEntity, program, new Date(), new Date(), orgUnit );

        JsonList<JsonAttribute> attributes = GET(
            "/tracker/trackedEntities/{id}?program={id}&fields=attributes[attribute,value]", trackedEntity.getUid(),
            program.getUid() )
                .content( HttpStatus.OK ).getList( "attributes", JsonAttribute.class );

        assertContainsAll( List.of( tea.getUid(), tea2.getUid() ), attributes, JsonAttribute::getAttribute );
        assertContainsAll( List.of( "12", "24" ), attributes, JsonAttribute::getValue );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationships()
    {
        TrackedEntity from = trackedEntity();
        TrackedEntity to = trackedEntity();
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
        TrackedEntity from = trackedEntity();
        TrackedEntity to = trackedEntity();
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
        TrackedEntity from = trackedEntity();
        TrackedEntity to = trackedEntityNotInSearchScope();
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
        TrackedEntity from = trackedEntityNotInSearchScope();
        TrackedEntity to = trackedEntityNotInSearchScope();
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .error( HttpStatus.FORBIDDEN ).getMessage()
            .contains( "User has no read access to organisation unit" ) );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipItemFrom()
    {
        TrackedEntity from = trackedEntityNotInSearchScope();
        TrackedEntity to = trackedEntity();
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .error( HttpStatus.FORBIDDEN ).getMessage()
            .contains( "User has no read access to organisation unit" ) );
    }

    @Test
    void getTrackedEntityByIdyWithFieldsRelationshipsNoAccessToTrackedEntityType()
    {
        TrackedEntityType type = trackedEntityTypeNotAccessible();
        TrackedEntity from = trackedEntity( type );
        TrackedEntity to = trackedEntity( type );
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .error( HttpStatus.FORBIDDEN ).getMessage()
            .contains( "User has no data read access to tracked entity" ) );
    }

    @Test
    void getTrackedEntityByIdNotFound()
    {
        assertEquals( "TrackedEntity with id Hq3Kc6HK4OZ could not be found.",
            GET( "/tracker/trackedEntities/Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    @Test
    void shouldReturnABadRequestErrorWhenInvalidUidIsPassedInThePath()
    {
        assertEquals(
            "Value invalidUid is not valid for parameter uid. UID must be an alphanumeric string of 11 characters",
            GET( "/tracker/trackedEntities/invalidUid" )
                .error( HttpStatus.BAD_REQUEST )
                .getMessage() );
    }

    @Test
    void getTrackedEntityReturnsCsvFormat()
    {
        WebClient.HttpResponse response = GET( "/tracker/trackedEntities.csv?program={programId}&orgUnit={orgUnitId}",
            program.getUid(), orgUnit.getUid() );

        assertEquals( HttpStatus.OK, response.status() );

        assertAll( () -> assertTrue( response.header( "content-type" ).contains( ContextUtils.CONTENT_TYPE_CSV ) ),
            () -> assertTrue( response.header( "content-disposition" ).contains( "filename=\"trackedEntities.csv\"" ) ),
            () -> assertTrue( response.content().toString().contains( "trackedEntity,trackedEntityType" ) ) );
    }

    @Test
    void getTrackedEntityReturnsCsvZipFormat()
    {
        WebClient.HttpResponse response = GET(
            "/tracker/trackedEntities.csv.zip?program={programId}&orgUnit={orgUnitId}",
            program.getUid(), orgUnit.getUid() );

        assertEquals( HttpStatus.OK, response.status() );

        assertAll( () -> assertTrue( response.header( "content-type" ).contains( ContextUtils.CONTENT_TYPE_CSV_ZIP ) ),
            () -> assertTrue(
                response.header( "content-disposition" ).contains( "filename=\"trackedEntities.csv.zip\"" ) ) );
    }

    @Test
    void getTrackedEntityReturnsCsvGZipFormat()
    {
        WebClient.HttpResponse response = GET(
            "/tracker/trackedEntities.csv.gz?program={programId}&orgUnit={orgUnitId}",
            program.getUid(), orgUnit.getUid() );

        assertEquals( HttpStatus.OK, response.status() );

        assertAll( () -> assertTrue( response.header( "content-type" ).contains( ContextUtils.CONTENT_TYPE_CSV_GZIP ) ),
            () -> assertTrue(
                response.header( "content-disposition" ).contains( "filename=\"trackedEntities.csv.gz\"" ) ) );
    }

    @Test
    void shouldGetEnrollmentWhenFieldsHasEnrollments()
    {
        TrackedEntity trackedEntity = trackedEntity();
        Enrollment enrollment = enrollmentService.enrollTrackedEntity( trackedEntity,
            program, new Date(), new Date(), orgUnit );

        JsonList<JsonEnrollment> json = GET( "/tracker/trackedEntities/{id}?fields=enrollments",
            trackedEntity.getUid() )
                .content( HttpStatus.OK ).getList( "enrollments", JsonEnrollment.class );

        JsonEnrollment jsonEnrollment = assertDefaultEnrollmentResponse( json, enrollment );

        assertTrue( jsonEnrollment.getArray( "relationships" ).isEmpty() );
        assertTrue( jsonEnrollment.getAttributes().isEmpty() );
        assertTrue( jsonEnrollment.getEvents().isEmpty() );
    }

    @Test
    void shouldGetNoEventRelationshipsWhenEventsHasNoRelationshipsAndFieldsIncludeAll()
    {
        TrackedEntity trackedEntity = trackedEntity();

        Enrollment enrollment = enrollmentService.enrollTrackedEntity( trackedEntity,
            program, new Date(), new Date(), orgUnit );

        Event event = eventWithDataValue( enrollment );

        enrollment.getEvents().add( event );
        manager.update( enrollment );

        JsonList<JsonEnrollment> json = GET( "/tracker/trackedEntities/{id}?fields=enrollments",
            trackedEntity.getUid() )
                .content( HttpStatus.OK ).getList( "enrollments", JsonEnrollment.class );

        JsonEnrollment jsonEnrollment = assertDefaultEnrollmentResponse( json, enrollment );
        assertTrue( jsonEnrollment.getArray( "relationships" ).isEmpty() );
        assertTrue( jsonEnrollment.getAttributes().isEmpty() );

        JsonEvent jsonEvent = assertDefaultEventResponse( jsonEnrollment, event );

        assertTrue( jsonEvent.getRelationships().isEmpty() );
    }

    @Test
    void shouldGetEventRelationshipsWhenEventHasRelationshipsAndFieldsIncludeEventRelationships()
    {
        TrackedEntity trackedEntity = trackedEntity();

        Enrollment enrollment = enrollmentService.enrollTrackedEntity( trackedEntity,
            program, new Date(), new Date(), orgUnit );

        Event event = eventWithDataValue( enrollment );
        enrollment.getEvents().add( event );
        manager.update( enrollment );

        Relationship teiToEventRelationship = relationship( trackedEntity, event );

        JsonList<JsonEnrollment> json = GET( "/tracker/trackedEntities/{id}?fields=enrollments",
            trackedEntity.getUid() )
                .content( HttpStatus.OK ).getList( "enrollments", JsonEnrollment.class );

        JsonEnrollment jsonEnrollment = assertDefaultEnrollmentResponse( json, enrollment );
        assertTrue( jsonEnrollment.getAttributes().isEmpty() );
        assertTrue( jsonEnrollment.getArray( "relationships" ).isEmpty() );

        JsonEvent jsonEvent = assertDefaultEventResponse( jsonEnrollment, event );

        JsonRelationship relationship = jsonEvent.getRelationships().get( 0 );

        assertEquals( teiToEventRelationship.getUid(), relationship.getRelationship() );
        assertEquals( trackedEntity.getUid(), relationship.getFrom().getTrackedEntity().getTrackedEntity() );
        assertEquals( event.getUid(), relationship.getTo().getEvent().getEvent() );
    }

    @Test
    void shouldGetNoEventRelationshipsWhenEventHasRelationshipsAndFieldsExcludeEventRelationships()
    {
        TrackedEntity trackedEntity = trackedEntity();

        Enrollment enrollment = enrollmentService.enrollTrackedEntity( trackedEntity,
            program, new Date(), new Date(), orgUnit );

        Event event = eventWithDataValue( enrollment );

        enrollment.getEvents().add( event );
        manager.update( enrollment );

        relationship( trackedEntity, event );

        JsonList<JsonEnrollment> json = GET(
            "/tracker/trackedEntities/{id}?fields=enrollments[*,events[!relationships]]",
            trackedEntity.getUid() )
                .content( HttpStatus.OK ).getList( "enrollments", JsonEnrollment.class );

        JsonEnrollment jsonEnrollment = assertDefaultEnrollmentResponse( json, enrollment );
        assertTrue( jsonEnrollment.getAttributes().isEmpty() );
        assertTrue( jsonEnrollment.getArray( "relationships" ).isEmpty() );

        JsonEvent jsonEvent = assertDefaultEventResponse( jsonEnrollment, event );

        assertHasNoMember( jsonEvent, "relationships" );
    }

    private Event eventWithDataValue( Enrollment enrollment )
    {
        Event event = new Event( enrollment, programStage,
            enrollment.getOrganisationUnit() );
        event.setAutoFields();
        event.setExecutionDate( DateUtils.parseDate( EVENT_OCCURRED_AT ) );

        dataElement = createDataElement( 'A' );
        dataElement.setValueType( ValueType.TEXT );
        manager.save( dataElement );

        EventDataValue eventDataValue = new EventDataValue();
        eventDataValue.setValue( "value" );
        eventDataValue.setDataElement( dataElement.getUid() );
        eventDataValue.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );
        eventDataValue.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        Set<EventDataValue> eventDataValues = Set.of( eventDataValue );
        event.setEventDataValues( eventDataValues );

        manager.save( event );
        return event;
    }

    private JsonEnrollment assertDefaultEnrollmentResponse( JsonList<JsonEnrollment> enrollments,
        Enrollment enrollment )
    {
        assertFalse( enrollments.isEmpty() );
        JsonEnrollment jsonEnrollment = enrollments.get( 0 );

        assertHasMember( jsonEnrollment, "enrollment" );

        assertEquals( enrollment.getUid(), jsonEnrollment.getEnrollment() );
        assertEquals( enrollment.getTrackedEntity().getUid(), jsonEnrollment.getTrackedEntity() );
        assertEquals( program.getUid(), jsonEnrollment.getProgram() );
        assertEquals( "ACTIVE", jsonEnrollment.getStatus() );
        assertEquals( orgUnit.getUid(), jsonEnrollment.getOrgUnit() );
        assertEquals( orgUnit.getName(), jsonEnrollment.getOrgUnitName() );
        assertFalse( jsonEnrollment.getBoolean( "deleted" ).booleanValue() );
        assertHasMember( jsonEnrollment, "enrolledAt" );
        assertHasMember( jsonEnrollment, "occurredAt" );
        assertHasMember( jsonEnrollment, "createdAt" );
        assertHasMember( jsonEnrollment, "createdAtClient" );
        assertHasMember( jsonEnrollment, "updatedAt" );
        assertHasMember( jsonEnrollment, "notes" );
        assertHasMember( jsonEnrollment, "followUp" );

        return jsonEnrollment;
    }

    private JsonEvent assertDefaultEventResponse( JsonEnrollment enrollment, Event event )
    {
        assertTrue( enrollment.isObject() );
        assertFalse( enrollment.isEmpty() );

        JsonEvent jsonEvent = enrollment.getEvents().get( 0 );

        assertEquals( event.getUid(), jsonEvent.getEvent() );
        assertEquals( event.getProgramStage().getUid(), jsonEvent.getProgramStage() );
        assertEquals( event.getEnrollment().getUid(), jsonEvent.getEnrollment() );
        assertEquals( program.getUid(), jsonEvent.getProgram() );
        assertEquals( "ACTIVE", jsonEvent.getStatus() );
        assertEquals( orgUnit.getUid(), jsonEvent.getOrgUnit() );
        assertEquals( orgUnit.getName(), jsonEvent.getOrgUnitName() );
        assertFalse( jsonEvent.getDeleted() );
        assertHasMember( jsonEvent, "createdAt" );
        assertHasMember( jsonEvent, "occurredAt" );
        assertEquals( EVENT_OCCURRED_AT, jsonEvent.getString( "occurredAt" ).string() );
        assertHasMember( jsonEvent, "createdAtClient" );
        assertHasMember( jsonEvent, "updatedAt" );
        assertHasMember( jsonEvent, "notes" );
        assertHasMember( jsonEvent, "followup" );

        JsonDataValue dataValue = jsonEvent.getDataValues().get( 0 );

        assertEquals( dataElement.getUid(), dataValue.getDataElement() );
        assertEquals( event.getEventDataValues().iterator().next().getValue(), dataValue.getValue() );
        assertHasMember( dataValue, "createdAt" );
        assertHasMember( dataValue, "updatedAt" );
        assertHasMember( dataValue, "createdBy" );
        assertHasMember( dataValue, "updatedBy" );

        return jsonEvent;
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

    private Relationship relationship( TrackedEntity from, TrackedEntity to )
    {
        RelationshipType type = relationshipTypeAccessible( RelationshipEntity.TRACKED_ENTITY_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        return relationship( type, fromTrackedEntity( from ), toTrackedEntity( to ) );
    }

    private Relationship relationship( TrackedEntity from, Event to )
    {
        RelationshipType type = relationshipTypeAccessible( RelationshipEntity.TRACKED_ENTITY_INSTANCE,
            RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        RelationshipItem fromItem = fromTrackedEntity( from );
        RelationshipItem toItem = toEvent( to );
        Relationship relationship = relationship( type, fromItem, toItem );
        fromItem.setRelationship( relationship );
        toItem.setRelationship( relationship );
        to.getRelationshipItems().add( toItem );
        manager.save( to, false );
        manager.save( relationship, false );
        return relationship;
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

    private RelationshipItem fromTrackedEntity( TrackedEntity from )
    {
        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setTrackedEntity( from );
        from.getRelationshipItems().add( fromItem );
        return fromItem;
    }

    private RelationshipItem toTrackedEntity( TrackedEntity to )
    {
        RelationshipItem toItem = new RelationshipItem();
        toItem.setTrackedEntity( to );
        to.getRelationshipItems().add( toItem );
        return toItem;
    }

    private RelationshipItem toEvent( Event to )
    {
        RelationshipItem toItem = new RelationshipItem();
        toItem.setEvent( to );
        to.getRelationshipItems().add( toItem );
        return toItem;
    }

    private void assertTrackedEntityWithinRelationship( TrackedEntity expected, JsonRelationshipItem json )
    {
        JsonRelationshipItem.JsonTrackedEntity jsonTEI = json.getTrackedEntity();
        assertFalse( jsonTEI.isEmpty(), "trackedEntity should not be empty" );
        assertEquals( expected.getUid(), jsonTEI.getTrackedEntity() );
        assertHasNoMember( json, "trackedEntityType" );
        assertHasNoMember( json, "orgUnit" );
        assertHasNoMember( json, "relationships" ); // relationships are not
                                                   // returned within
                                                   // relationships
        assertTrue( jsonTEI.getArray( "attributes" ).isEmpty() );
    }

    private TrackedEntityAttributeValue attributeValue( TrackedEntityAttribute tea, TrackedEntity tei,
        String value )
    {
        return new TrackedEntityAttributeValue( tea, tei, value );
    }
}