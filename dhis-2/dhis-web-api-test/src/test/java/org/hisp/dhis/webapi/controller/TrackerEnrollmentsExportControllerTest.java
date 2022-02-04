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
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class TrackerEnrollmentsExportControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    void testGetEnrollmentByIdContainsFollowUp()
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

        ProgramInstance programInstance = new ProgramInstance( program, tei, orgUnit );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.COMPLETED );
        programInstance.setFollowup( true );
        manager.save( programInstance );

        JsonObject enrollment = GET( "/tracker/enrollments/{id}", programInstance.getUid() ).content( HttpStatus.OK );

        assertTrue( enrollment.isObject() );
        assertFalse( enrollment.isEmpty() );
        assertEquals( programInstance.getUid(), enrollment.getString( "enrollment" ).string() );
        assertEquals( tei.getUid(), enrollment.getString( "trackedEntity" ).string() );
        assertEquals( program.getUid(), enrollment.getString( "program" ).string() );
        assertEquals( "COMPLETED", enrollment.getString( "status" ).string() );
        assertEquals( orgUnit.getUid(), enrollment.getString( "orgUnit" ).string() );
        assertEquals( orgUnit.getName(), enrollment.getString( "orgUnitName" ).string() );
        assertTrue( enrollment.has( "enrolledAt", "occurredAt" ) );
        assertTrue( enrollment.getBoolean( "followUp" ).booleanValue() );
        assertFalse( enrollment.getBoolean( "deleted" ).booleanValue() );
        assertTrue( enrollment.getArray( "events" ).isEmpty() );
        assertTrue( enrollment.getArray( "relationships" ).isEmpty() );
        assertTrue( enrollment.getArray( "attributes" ).isEmpty() );
        assertTrue( enrollment.getArray( "notes" ).isEmpty() );
    }
}