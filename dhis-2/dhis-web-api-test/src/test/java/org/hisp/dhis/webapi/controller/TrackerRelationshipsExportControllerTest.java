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
import org.hisp.dhis.commons.util.RelationshipUtils;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class TrackerRelationshipsExportControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    void testGetRelationshipByEvent()
    {
        OrganisationUnit orgUnit = createOrganisationUnit( 'A' );
        manager.save( orgUnit );
        Program program = createProgram( 'A' );
        manager.save( program );
        ProgramStage programStage = createProgramStage( 'A', program );
        manager.save( programStage );
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        manager.save( trackedEntityType );
        TrackedEntityInstance tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        manager.save( tei );
        RelationshipType rType = createRelationshipType( 'A' );
        rType.getFromConstraint().setRelationshipEntity( RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        rType.getToConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        manager.save( rType );

        ProgramInstance programInstance = new ProgramInstance( program, tei, orgUnit );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.COMPLETED );
        manager.save( programInstance );

        ProgramStageInstance programStageInstance = new ProgramStageInstance( programInstance, programStage );
        programStageInstance.setAutoFields();
        manager.save( programStageInstance );

        Relationship r = new Relationship();
        RelationshipItem rItem1 = new RelationshipItem();
        rItem1.setProgramStageInstance( programStageInstance );
        RelationshipItem rItem2 = new RelationshipItem();
        rItem2.setTrackedEntityInstance( tei );
        r.setFrom( rItem1 );
        r.setTo( rItem2 );
        r.setRelationshipType( rType );
        r.setKey( RelationshipUtils.generateRelationshipKey( r ) );
        r.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( r ) );
        manager.save( r );

        JsonObject relationship = GET( "/tracker/relationships?event=" + programStageInstance.getUid() )
            .content( HttpStatus.OK );

        assertTrue( relationship.isObject() );
        assertFalse( relationship.isEmpty() );
        JsonObject jsonRelationship = relationship.getArray( "instances" ).get( 0 ).as( JsonObject.class );
        assertEquals( r.getUid(), jsonRelationship.getString( "relationship" ).string() );
        assertEquals( r.getRelationshipType().getUid(), jsonRelationship.getString( "relationshipType" ).string() );

        JsonObject jsonEvent = jsonRelationship.getObject( "from" ).getObject( "event" );
        assertEquals( programStageInstance.getUid(), jsonEvent.getString( "event" ).string() );
        assertEquals( programStageInstance.getStatus().toString(), jsonEvent.getString( "status" ).string() );
        assertEquals( programStageInstance.getProgramStage().getUid(), jsonEvent.getString( "programStage" ).string() );
        assertEquals( programInstance.getUid(), jsonEvent.getString( "enrollment" ).string() );
        assertTrue( jsonEvent.getArray( "relationships" ).isEmpty() );

        JsonObject jsonTEI = jsonRelationship.getObject( "to" ).getObject( "trackedEntity" );
        assertEquals( tei.getUid(), jsonTEI.getString( "trackedEntity" ).string() );
        assertEquals( trackedEntityType.getUid(), jsonTEI.getString( "trackedEntityType" ).string() );
        assertEquals( orgUnit.getUid(), jsonTEI.getString( "orgUnit" ).string() );
        assertTrue( jsonTEI.getArray( "relationships" ).isEmpty() );
    }

    @Test
    void testGetRelationshipByEnrollment()
    {
        OrganisationUnit orgUnit = createOrganisationUnit( 'A' );
        manager.save( orgUnit );
        Program program = createProgram( 'A' );
        manager.save( program );
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        manager.save( trackedEntityType );
        TrackedEntityInstance tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        manager.save( tei );
        RelationshipType rType = createRelationshipType( 'A' );
        rType.getFromConstraint().setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        rType.getToConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        manager.save( rType );

        ProgramInstance programInstance = new ProgramInstance( program, tei, orgUnit );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.COMPLETED );
        manager.save( programInstance );

        Relationship r = new Relationship();
        RelationshipItem rItem1 = new RelationshipItem();
        rItem1.setProgramInstance( programInstance );
        RelationshipItem rItem2 = new RelationshipItem();
        rItem2.setTrackedEntityInstance( tei );
        r.setFrom( rItem1 );
        r.setTo( rItem2 );
        r.setRelationshipType( rType );
        r.setKey( RelationshipUtils.generateRelationshipKey( r ) );
        r.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( r ) );
        manager.save( r );

        JsonObject relationship = GET( "/tracker/relationships?enrollment=" + programInstance.getUid() )
            .content( HttpStatus.OK );

        assertTrue( relationship.isObject() );
        assertFalse( relationship.isEmpty() );
        JsonObject jsonRelationship = relationship.getArray( "instances" ).get( 0 ).as( JsonObject.class );
        assertEquals( r.getUid(), jsonRelationship.getString( "relationship" ).string() );
        assertEquals( r.getRelationshipType().getUid(), jsonRelationship.getString( "relationshipType" ).string() );

        JsonObject jsonEnrollment = jsonRelationship.getObject( "from" ).getObject( "enrollment" );
        assertEquals( programInstance.getUid(), jsonEnrollment.getString( "enrollment" ).string() );
        assertEquals( tei.getUid(), jsonEnrollment.getString( "trackedEntity" ).string() );
        assertEquals( program.getUid(), jsonEnrollment.getString( "program" ).string() );
        assertEquals( orgUnit.getUid(), jsonEnrollment.getString( "orgUnit" ).string() );
        assertTrue( jsonEnrollment.getArray( "events" ).isEmpty() );
        assertTrue( jsonEnrollment.getArray( "relationships" ).isEmpty() );

        JsonObject jsonTEI = jsonRelationship.getObject( "to" ).getObject( "trackedEntity" );
        assertEquals( tei.getUid(), jsonTEI.getString( "trackedEntity" ).string() );
        assertEquals( trackedEntityType.getUid(), jsonTEI.getString( "trackedEntityType" ).string() );
    }

    @Test
    void testGetRelationshipByTrackedEntity()
    {
        OrganisationUnit orgUnit = createOrganisationUnit( 'A' );
        manager.save( orgUnit );
        Program program = createProgram( 'A' );
        manager.save( program );
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        manager.save( trackedEntityType );
        TrackedEntityInstance tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        manager.save( tei );
        RelationshipType rType = createRelationshipType( 'A' );
        rType.getFromConstraint().setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        rType.getToConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        manager.save( rType );

        ProgramInstance programInstance = new ProgramInstance( program, tei, orgUnit );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.COMPLETED );
        manager.save( programInstance );

        Relationship r = new Relationship();
        RelationshipItem rItem1 = new RelationshipItem();
        rItem1.setProgramInstance( programInstance );
        RelationshipItem rItem2 = new RelationshipItem();
        rItem2.setTrackedEntityInstance( tei );
        r.setFrom( rItem1 );
        r.setTo( rItem2 );
        r.setRelationshipType( rType );
        r.setKey( RelationshipUtils.generateRelationshipKey( r ) );
        r.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( r ) );
        manager.save( r );

        JsonObject relationship = GET( "/tracker/relationships?tei=" + tei.getUid() )
            .content( HttpStatus.OK );

        assertTrue( relationship.isObject() );
        assertFalse( relationship.isEmpty() );
        JsonObject jsonRelationship = relationship.getArray( "instances" ).get( 0 ).as( JsonObject.class );
        assertEquals( r.getUid(), jsonRelationship.getString( "relationship" ).string() );
        assertEquals( r.getRelationshipType().getUid(), jsonRelationship.getString( "relationshipType" ).string() );

        JsonObject jsonEnrollment = jsonRelationship.getObject( "from" ).getObject( "enrollment" );
        assertEquals( programInstance.getUid(), jsonEnrollment.getString( "enrollment" ).string() );
        assertEquals( tei.getUid(), jsonEnrollment.getString( "trackedEntity" ).string() );
        assertEquals( program.getUid(), jsonEnrollment.getString( "program" ).string() );
        assertEquals( orgUnit.getUid(), jsonEnrollment.getString( "orgUnit" ).string() );
        assertTrue( jsonEnrollment.getArray( "events" ).isEmpty() );
        assertTrue( jsonEnrollment.getArray( "relationships" ).isEmpty() );

        JsonObject jsonTEI = jsonRelationship.getObject( "to" ).getObject( "trackedEntity" );
        assertEquals( tei.getUid(), jsonTEI.getString( "trackedEntity" ).string() );
        assertEquals( trackedEntityType.getUid(), jsonTEI.getString( "trackedEntityType" ).string() );
    }
}