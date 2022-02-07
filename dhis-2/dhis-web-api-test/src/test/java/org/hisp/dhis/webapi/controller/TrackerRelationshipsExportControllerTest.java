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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.hisp.dhis.common.IdentifiableObjectManager;
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
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class TrackerRelationshipsExportControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    public void testGetRelationshipByEvent()
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
        manager.save( r );

        JsonObject relationship = GET( "/tracker/relationships?event=" + programStageInstance.getUid() )
            .content( HttpStatus.OK );

        assertTrue( relationship.isObject() );
        assertFalse( relationship.isEmpty() );
        JsonObject jsonRelationship = relationship.getArray( "instances" ).get( 0 ).as( JsonObject.class );
        assertEquals( r.getUid(), jsonRelationship.getString( "relationship" ).string() );
        assertEquals( r.getRelationshipType().getUid(), jsonRelationship.getString( "relationshipType" ).string() );
        assertEquals( programStageInstance.getUid(),
            jsonRelationship.getObject( "from" ).getString( "event" ).string() );
        assertEquals( tei.getUid(), jsonRelationship.getObject( "to" ).getString( "trackedEntity" ).string() );
    }
}