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

import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackerEnrollmentsExportControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    private OrganisationUnit orgUnit;

    private Program program;

    private TrackedEntityInstance tei;

    private ProgramInstance programInstance;

    private TrackedEntityAttribute trackedEntityAttribute;

    private Relationship relationship;

    private ProgramStage programStage;

    private ProgramStageInstance programStageInstance;

    private DataElement dataElement;

    private static final String ATTRIBUTE_VALUE = "value";

    @BeforeEach
    void setUp()
    {
        orgUnit = createOrganisationUnit( 'A' );
        manager.save( orgUnit );

        program = createProgram( 'A' );
        manager.save( program );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        manager.save( trackedEntityType );

        trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        manager.save( trackedEntityAttribute );

        ProgramTrackedEntityAttribute programTrackedEntityAttribute = createProgramTrackedEntityAttribute( program,
            trackedEntityAttribute );
        manager.save( programTrackedEntityAttribute );

        tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        manager.save( tei );

        programStage = createProgramStage( 'A', program );
        manager.save( programStage );

        programInstance = new ProgramInstance( program, tei, orgUnit );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.COMPLETED );
        programInstance.setFollowup( true );
        manager.save( programInstance );

        manager.save( programInstance );
        programStageInstance = event();
    }

    @Test
    void getEnrollmentById()
    {

        JsonObject json = GET( "/tracker/enrollments/{id}", programInstance.getUid() ).content( HttpStatus.OK );

        assertDefaultResponse( json );
    }

    @Test
    void getEnrollmentByIdWithFields()
    {

        JsonObject json = GET( "/tracker/enrollments/{id}?fields=orgUnit,status", programInstance.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertHasOnlyMembers( json, "orgUnit", "status" );
    }

    @Test
    void getEnrollmentByIdNotFound()
    {
        assertEquals( "Enrollment with id Hq3Kc6HK4OZ could not be found.",
            GET( "/tracker/enrollments/Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    @Test
    void getEnrollmentByIdWithAttributeFields()
    {
        saveTrackedEntityAttributeValue();

        assertWithAttributeResponse( (GET( "/tracker/enrollments/{id}?fields=attributes", programInstance.getUid() )
            .content( HttpStatus.OK )) );
    }

    @Test
    void getEnrollmentByIdWithRelationshipsFields()
    {
        manager.save( relationship( programInstance, tei ) );

        assertWithRelationshipResponse(
            (GET( "/tracker/enrollments/{id}?fields=relationships", programInstance.getUid() )
                .content( HttpStatus.OK )) );
    }

    @Test
    void getEnrollmentByIdWithEventsFields()
    {
        programInstance.setProgramStageInstances( Set.of( programStageInstance ) );
        manager.update( programInstance );

        assertWithEventResponse( (GET( "/tracker/enrollments/{id}?fields=events", programInstance.getUid() )
            .content( HttpStatus.OK )) );
    }

    @Test
    void getEnrollmentByIdWithExcludedFields()
    {
        saveTrackedEntityAttributeValue();

        manager.save( relationship( programInstance, tei ) );

        programInstance.setProgramStageInstances( Set.of( programStageInstance ) );
        manager.update( programInstance );

        assertTrue(
            (GET( "/tracker/enrollments/{id}?fields=!attributes,!relationships,!events", programInstance.getUid() )
                .content( HttpStatus.OK )).isEmpty() );
    }

    private void saveTrackedEntityAttributeValue()
    {
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setEntityInstance( tei );
        trackedEntityAttributeValue.setStoredBy( "user" );
        trackedEntityAttributeValue.setValue( ATTRIBUTE_VALUE );
        tei.setTrackedEntityAttributeValues( Set.of( trackedEntityAttributeValue ) );
        manager.update( tei );
    }

    private ProgramStageInstance event()
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance( programInstance, programStage,
            programInstance.getOrganisationUnit() );
        programStageInstance.setAutoFields();

        EventDataValue eventDataValue = new EventDataValue();
        eventDataValue.setValue( "value" );
        dataElement = createDataElement( 'A' );
        dataElement.setValueType( ValueType.TEXT );
        manager.save( dataElement );
        eventDataValue.setDataElement( dataElement.getUid() );
        Set<EventDataValue> eventDataValues = Set.of( eventDataValue );
        programStageInstance.setEventDataValues( eventDataValues );
        manager.save( programStageInstance );
        return programStageInstance;
    }

    private Relationship relationship( ProgramInstance from, TrackedEntityInstance to )
    {
        relationship = new Relationship();

        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setProgramInstance( from );
        from.getRelationshipItems().add( fromItem );
        relationship.setFrom( fromItem );
        fromItem.setRelationship( relationship );

        RelationshipItem toItem = new RelationshipItem();
        toItem.setTrackedEntityInstance( to );
        to.getRelationshipItems().add( toItem );
        relationship.setTo( toItem );
        toItem.setRelationship( relationship );

        RelationshipType type = createRelationshipType( 'A' );
        type.getFromConstraint().setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        type.getToConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        type.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( type, false );

        relationship.setRelationshipType( type );
        relationship.setKey( type.getUid() );
        relationship.setInvertedKey( type.getUid() );
        relationship.setAutoFields();

        manager.save( relationship, false );
        return relationship;
    }

    private void assertWithEventResponse( JsonObject json )
    {
        assertTrue( json.isObject() );
        assertFalse( json.isEmpty() );
        assertHasOnlyMembers( json, "events" );

        JsonObject event = json.getArray( "events" ).get( 0 ).asObject();

        assertHasMember( event, "event" );

        assertEquals( programStageInstance.getUid(), event.getString( "event" ).string() );
        assertEquals( programInstance.getUid(), event.getString( "enrollment" ).string() );
        assertEquals( tei.getUid(), event.getString( "trackedEntity" ).string() );
        assertEquals( dataElement.getUid(),
            event.getArray( "dataValues" ).get( 0 ).asObject().getString( "dataElement" ).string() );
        assertEquals( program.getUid(), event.getString( "program" ).string() );

        assertHasMember( event, "status" );
        assertHasMember( event, "program" );
        assertHasMember( event, "followup" );
        assertEquals( orgUnit.getUid(), event.getString( "orgUnit" ).string() );
        assertEquals( orgUnit.getName(), event.getString( "orgUnitName" ).string() );
        assertFalse( event.getBoolean( "deleted" ).booleanValue() );
    }

    private void assertWithRelationshipResponse( JsonObject json )
    {
        assertTrue( json.isObject() );
        assertFalse( json.isEmpty() );
        assertHasOnlyMembers( json, "relationships" );

        JsonObject relationships = json.getArray( "relationships" ).get( 0 ).asObject();
        assertHasMember( relationships, "relationship" );

        assertEquals( relationship.getUid(), relationships.getString( "relationship" ).string() );

        assertHasMember( relationships, "relationship" );
        assertHasMember( relationships, "relationshipName" );
        assertHasMember( relationships, "relationshipType" );
        assertHasMember( relationships, "createdAt" );
        assertHasMember( relationships, "updatedAt" );
        assertHasMember( relationships, "bidirectional" );
        assertHasMember( relationships, "from" );
        assertHasMember( relationships, "to" );

        assertHasMember( relationships.getObject( "from" ), "enrollment" );
        assertHasMember( relationships.getObject( "to" ), "trackedEntity" );
    }

    private void assertWithAttributeResponse( JsonObject json )
    {
        assertTrue( json.isObject() );
        assertFalse( json.isEmpty() );
        assertHasOnlyMembers( json, "attributes" );

        JsonObject attribute = json.getArray( "attributes" ).get( 0 ).asObject();
        assertHasMember( attribute, "attribute" );

        assertEquals( trackedEntityAttribute.getUid(), attribute.getString( "attribute" ).string() );

        assertHasMember( attribute, "createdAt" );
        assertHasMember( attribute, "updatedAt" );
        assertHasMember( attribute, "value" );
        assertHasMember( attribute, "displayName" );
        assertHasMember( attribute, "valueType" );
        assertHasMember( attribute, "code" );
        assertHasMember( attribute, "storedBy" );
    }

    private void assertDefaultResponse( JsonObject json )
    {
        assertTrue( json.isObject() );
        assertFalse( json.isEmpty() );
        assertEquals( programInstance.getUid(), json.getString( "enrollment" ).string() );
        assertEquals( tei.getUid(), json.getString( "trackedEntity" ).string() );
        assertEquals( program.getUid(), json.getString( "program" ).string() );
        assertEquals( "COMPLETED", json.getString( "status" ).string() );
        assertEquals( orgUnit.getUid(), json.getString( "orgUnit" ).string() );
        assertEquals( orgUnit.getName(), json.getString( "orgUnitName" ).string() );
        assertTrue( json.getBoolean( "followUp" ).booleanValue() );
        assertFalse( json.getBoolean( "deleted" ).booleanValue() );
        assertHasMember( json, "enrolledAt" );
        assertHasMember( json, "occurredAt" );
        assertHasMember( json, "createdAt" );
        assertHasMember( json, "createdAtClient" );
        assertHasMember( json, "updatedAt" );
        assertHasMember( json, "notes" );
        assertHasNoMember( json, "relationships" );
        assertHasNoMember( json, "events" );
        assertHasNoMember( json, "attributes" );
    }

}