/*
 * Copyright (c) 2004-2018, University of Oslo
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

package org.hisp.dhis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventsTests
    extends ApiTest
{
    String programId;

    String orgUnitId;

    String dataElementId;

    String createdEvent;

    private UserActions userActions;

    private OrgUnitActions orgUnitActions;

    private EventActions eventActions;

    private ProgramActions programActions;

    @BeforeEach
    public void beforeEach()
    {
        orgUnitActions = new OrgUnitActions();
        eventActions = new EventActions();
        programActions = new ProgramActions();
        userActions = new UserActions();

        new LoginActions().loginAsDefaultUser();
        orgUnitId = orgUnitActions.createOrgUnit();
        userActions.grantCurrentUserAccessToOrgUnit( orgUnitId );
        // create program

        programId = programActions.createEventProgram( orgUnitId ).extractUid();
        programActions.addProgramStage( programId ).validate().statusCode( 200 );
        dataElementId = createDataElement();
    }

    @Test
    public void event_anonymous_create()
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty( "orgUnit", orgUnitId );
        jsonObject.addProperty( "program", programId );

        JsonArray dataValues = new JsonArray();
        JsonObject dataElement = new JsonObject();
        dataElement.addProperty( "dataElement", dataElementId );

        dataValues.add( dataElement );
        jsonObject.add( "dataValues", dataValues );

        ApiResponse response = eventActions.post( jsonObject );

        createdEvent = response.extractUid();

        response.validate()
            .statusCode( 200 )
            .body( "response.imported", Matchers.is( 1 ) );
    }

    private String createDataElement()
    {
        String randomString = DataGenerator.randomString();

        JsonObject body = new JsonObject();
        body.addProperty( "name", "AutoTest DE " + randomString );
        body.addProperty( "shortName", "AutoTest DE " + randomString );
        body.addProperty( "domainType", "TRACKER" );
        body.addProperty( "valueType", "NUMBER" );
        body.addProperty( "aggregationType", "SUM" );

        return new RestApiActions( "/dataElements" ).create( body );
    }

    @AfterEach
    public void cleanup()
    {
        eventActions.delete( createdEvent );
    }
}
