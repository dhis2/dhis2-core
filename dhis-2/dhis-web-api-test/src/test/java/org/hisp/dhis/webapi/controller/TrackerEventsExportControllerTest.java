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
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class TrackerEventsExportControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    void testGetEventByIdContainsCreatedByAndUpdateByInDataValues()
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
        User user = createUser( 'A' );
        manager.save( user );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setValueType( ValueType.NUMBER );
        manager.save( dataElement );

        ProgramInstance programInstance = new ProgramInstance( program, tei, orgUnit );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        manager.save( programInstance );

        ProgramStageInstance programStageInstance = createProgramStageInstance( programStage, programInstance,
            orgUnit );
        programStageInstance.setAutoFields();
        programStageInstance.setCreatedByUserInfo( UserInfoSnapshot.from( user ) );
        programStageInstance.setLastUpdatedByUserInfo( UserInfoSnapshot.from( user ) );
        EventDataValue eventDataValue = new EventDataValue();
        eventDataValue.setValue( "6" );
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
}