/*
 * Copyright (c) 2004-2021, University of Oslo
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

package org.hisp.dhis.tracker.importer.events;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventNotesTests
    extends ApiTest
{
    private TrackerActions trackerActions;

    @BeforeAll
    public void beforeAll()
    {
        trackerActions = new TrackerActions();

        new LoginActions().loginAsSuperUser();
    }

    @Test
    public void shouldUpdateEventWithANote()
    {
        //arrange
        JsonObject ob = buildEventWithNote();

        String eventId = trackerActions.postAndGetJobReport( ob )
            .validateSuccessfulImport()
            .extractImportedEvents().get( 0 );

        JsonObjectBuilder.jsonObject( ob ).addPropertyByJsonPath( "events[0]", "event", eventId );

        // act

        TrackerApiResponse response = trackerActions.postAndGetJobReport( ob );

        // assert
        response.validateSuccessfulImport()
            .validate()
            .body( "stats.updated", equalTo( 1 ) );
    }

    @Test
    public void shouldNotAddAnotherNote()
    {
        //arrange
        JsonObject ob = buildEventWithNote();

        String eventId = trackerActions.postAndGetJobReport( ob )
            .validateSuccessfulImport()
            .extractImportedEvents().get( 0 );

        ob = trackerActions.get( "/events/" + eventId ).getBody();

        ob = JsonObjectBuilder.jsonObject( ob ).wrapIntoArray( "events" );

        // act

        TrackerApiResponse response = trackerActions.postAndGetJobReport( ob );

        // assert
        response.validateSuccessfulImport()
            .validateWarningReport()
            .body( "trackerType", everyItem( equalTo( "EVENT" ) ) )
            .body( "warningCode", hasItem( "E1119" ) );
    }

    private JsonObject buildEventWithNote()
    {
        JsonObject ob = trackerActions
            .buildEvent( Constants.ORG_UNIT_IDS[1], Constants.EVENT_PROGRAM_ID, Constants.EVENT_PROGRAM_STAGE_ID );

        JsonObjectBuilder.jsonObject( ob )
            .addArrayByJsonPath( "events[0]", "notes",
                new JsonObjectBuilder().addProperty( "value", DataGenerator.randomString() ).build() );
        return ob;
    }

}
