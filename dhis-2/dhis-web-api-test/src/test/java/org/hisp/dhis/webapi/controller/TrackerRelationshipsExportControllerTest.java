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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

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
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
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

    private Program program;

    private ProgramStage programStage;

    private TrackedEntityType trackedEntityType;

    @BeforeEach
    void setUp()
    {
        orgUnit = createOrganisationUnit( 'A' );
        manager.save( orgUnit );
        program = createProgram( 'A' );
        manager.save( program );
        programStage = createProgramStage( 'A', program );
        manager.save( programStage );
        trackedEntityType = createTrackedEntityType( 'A' );
        manager.save( trackedEntityType );
    }

    @Test
    void getRelationshipsById()
    {
        TrackedEntityInstance tei = trackedEntityInstance();
        ProgramInstance programInstance = programInstance( tei );
        ProgramStageInstance programStageInstance = programStageInstance( programInstance );
        RelationshipType rType = relationshipType( RelationshipEntity.PROGRAM_STAGE_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        Relationship r = relationship( rType, programStageInstance, tei );

        JsonObject relationship = GET( "/tracker/relationships/" + r.getUid() )
            .content( HttpStatus.OK );

        assertRelationship( relationship, r );
        assertEvent( relationship.getObject( "from" ), programStageInstance );
        assertTrackedEntity( relationship.getObject( "to" ), tei );
    }

    @Test
    void getRelationshipsByIdNotFound()
    {
        assertEquals( "No relationship 'Hq3Kc6HK4OZ' found.",
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
        TrackedEntityInstance tei = trackedEntityInstance();
        ProgramInstance programInstance = programInstance( tei );
        ProgramStageInstance programStageInstance = programStageInstance( programInstance );
        RelationshipType rType = relationshipType( RelationshipEntity.PROGRAM_STAGE_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        Relationship r = relationship( rType, programStageInstance, tei );

        JsonObject relationship = GET( "/tracker/relationships?event=" + programStageInstance.getUid() )
            .content( HttpStatus.OK );

        JsonObject jsonRelationship = assertFirstRelationship( relationship, r );
        assertEvent( jsonRelationship.getObject( "from" ), programStageInstance );
        assertTrackedEntity( jsonRelationship.getObject( "to" ), tei );
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
        TrackedEntityInstance tei = trackedEntityInstance();
        ProgramInstance programInstance = programInstance( tei );
        RelationshipType rType = relationshipType( RelationshipEntity.PROGRAM_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        Relationship r = relationship( rType, programInstance, tei );

        JsonObject relationship = GET( "/tracker/relationships?enrollment=" + programInstance.getUid() )
            .content( HttpStatus.OK );

        JsonObject jsonRelationship = assertFirstRelationship( relationship, r );
        assertEnrollment( jsonRelationship.getObject( "from" ), programInstance );
        assertTrackedEntity( jsonRelationship.getObject( "to" ), tei );
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
    void getRelationshipsByTrackedEntity()
    {
        TrackedEntityInstance tei = trackedEntityInstance();
        ProgramInstance programInstance = programInstance( tei );
        RelationshipType rType = relationshipType( RelationshipEntity.PROGRAM_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        Relationship r = relationship( rType, programInstance, tei );

        JsonObject relationship = GET( "/tracker/relationships?trackedEntity=" + tei.getUid() )
            .content( HttpStatus.OK );

        JsonObject jsonRelationship = assertFirstRelationship( relationship, r );
        assertEnrollment( jsonRelationship.getObject( "from" ), programInstance );
        assertTrackedEntity( jsonRelationship.getObject( "to" ), tei );
    }

    @Test
    void getRelationshipsByTei()
    {
        TrackedEntityInstance tei = trackedEntityInstance();
        ProgramInstance programInstance = programInstance( tei );
        RelationshipType rType = relationshipType( RelationshipEntity.PROGRAM_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        Relationship r = relationship( rType, programInstance, tei );

        JsonObject relationship = GET( "/tracker/relationships?tei=" + tei.getUid() )
            .content( HttpStatus.OK );

        JsonObject jsonRelationship = assertFirstRelationship( relationship, r );
        assertEnrollment( jsonRelationship.getObject( "from" ), programInstance );
        assertTrackedEntity( jsonRelationship.getObject( "to" ), tei );
    }

    @Test
    void getRelationshipsByTrackedEntityNotFound()
    {
        assertEquals( "No trackedEntity 'Hq3Kc6HK4OZ' found.",
            GET( "/tracker/relationships?trackedEntity=Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    private TrackedEntityInstance trackedEntityInstance()
    {
        TrackedEntityInstance tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        manager.save( tei );
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
        ProgramStageInstance programStageInstance = new ProgramStageInstance( programInstance, programStage );
        programStageInstance.setAutoFields();
        manager.save( programStageInstance );
        return programStageInstance;
    }

    private RelationshipType relationshipType( RelationshipEntity programInstance1,
        RelationshipEntity trackedEntityInstance )
    {
        RelationshipType rType = createRelationshipType( 'A' );
        rType.getFromConstraint().setRelationshipEntity( programInstance1 );
        rType.getToConstraint().setRelationshipEntity( trackedEntityInstance );
        manager.save( rType );
        return rType;
    }

    private Relationship relationship( RelationshipType type, ProgramStageInstance from, TrackedEntityInstance to )
    {
        Relationship r = new Relationship();
        RelationshipItem rItem1 = new RelationshipItem();
        rItem1.setProgramStageInstance( from );
        RelationshipItem rItem2 = new RelationshipItem();
        rItem2.setTrackedEntityInstance( to );
        r.setFrom( rItem1 );
        r.setTo( rItem2 );
        r.setRelationshipType( type );
        r.setKey( type.getUid() );
        r.setInvertedKey( type.getUid() );
        r.setAutoFields();
        manager.save( r );
        return r;
    }

    private Relationship relationship( RelationshipType type, ProgramInstance from, TrackedEntityInstance to )
    {
        Relationship r = new Relationship();
        RelationshipItem rItem1 = new RelationshipItem();
        rItem1.setProgramInstance( from );
        RelationshipItem rItem2 = new RelationshipItem();
        rItem2.setTrackedEntityInstance( to );
        r.setFrom( rItem1 );
        r.setTo( rItem2 );
        r.setRelationshipType( type );
        r.setKey( type.getUid() );
        r.setInvertedKey( type.getUid() );
        r.setAutoFields();
        manager.save( r );
        return r;
    }

    private void assertRelationship( JsonObject json, Relationship r )
    {
        assertFalse( json.isEmpty() );
        assertEquals( r.getUid(), json.getString( "relationship" ).string() );
        assertEquals( r.getRelationshipType().getUid(), json.getString( "relationshipType" ).string() );
    }

    private JsonObject assertFirstRelationship( JsonObject body, Relationship r )
    {
        return assertNthRelationship( body, r, 0 );
    }

    private JsonObject assertNthRelationship( JsonObject body, Relationship r, int n )
    {
        assertFalse( body.isEmpty() );
        JsonArray rels = body.getArray( "instances" );
        assertFalse( rels.isEmpty() );
        assertTrue( rels.size() >= n );
        JsonObject jsonRelationship = rels.get( n ).as( JsonObject.class );
        assertRelationship( jsonRelationship, r );
        return jsonRelationship;
    }

    private void assertEvent( JsonObject json, ProgramStageInstance programStageInstance )
    {
        assertEquals( programStageInstance.getUid(), json.getString( "event" ).string() );
    }

    private void assertTrackedEntity( JsonObject json, TrackedEntityInstance tei )
    {
        assertEquals( tei.getUid(), json.getString( "trackedEntity" ).string() );
    }

    private void assertEnrollment( JsonObject json, ProgramInstance programInstance )
    {
        assertEquals( programInstance.getUid(), json.getString( "enrollment" ).string() );
    }
}