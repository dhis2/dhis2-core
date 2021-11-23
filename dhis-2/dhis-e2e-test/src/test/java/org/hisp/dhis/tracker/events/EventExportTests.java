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

package org.hisp.dhis.tracker.events;

import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.RelationshipTypeActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker.RelationshipActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventExportTests
    extends ApiTest
{
    private String eventId;

    private String relationshipId;

    private EventActions eventActions;

    private String eventProgramId = Constants.EVENT_PROGRAM_ID;

    private String eventProgramStageID = Constants.EVENT_PROGRAM_STAGE_ID;

    private RelationshipTypeActions relationshipTypeActions;

    private RelationshipActions relationshipActions;

    @BeforeAll
    public void beforeAll()
    {
        relationshipActions = new RelationshipActions();
        relationshipTypeActions = new RelationshipTypeActions();
        eventActions = new EventActions();

        new LoginActions().loginAsAdmin();

        String relationshipTypeId = relationshipTypeActions
            .createRelationshipType( "PROGRAM_STAGE_INSTANCE", eventProgramId, "PROGRAM_STAGE_INSTANCE", eventProgramId, true );

        eventId = createEvent();
        relationshipId = createRelationship( eventId, createEvent(), relationshipTypeId );
    }

    @ValueSource( strings = {
        "?event=eventId&fields=*",
        "?event=eventId&fields=relationships",
        "?program=programId&fields=*",
        "?program=programId&fields=relationships"
    } )
    @ParameterizedTest
    public void shouldFetchRelationships( String queryParams )
    {
        ApiResponse response = eventActions.get( queryParams.replace( "eventId", eventId ).replace( "programId", eventProgramId ) );
        String body = "relationships";

        if ( response.extractList( "events" ) != null )
        {
            body = "events[0].relationships";
        }

        response
            .validate()
            .body( body, hasSize( Matchers.greaterThanOrEqualTo( 1 ) ) )
            .body( body + ".relationship", hasItems( relationshipId ) );
    }

    @ValueSource( strings = {
        "?event=eventId",
        "?event=eventId&fields=*,!relationships",
        "?program=programId&fields=*,!relationships"
    } )
    @ParameterizedTest
    public void shouldSkipRelationshipsForEventId( String queryParams )
    {
        ApiResponse response = eventActions.get( queryParams.replace( "eventId", eventId ).replace( "programId", eventProgramId ) );
        String body = "relationships";

        if ( response.extractList( "events" ) != null )
        {
            body = "events[0].relationships";
        }

        response
            .validate()
            .body( body, anyOf( nullValue(), hasSize( 0 ) ) );
    }

    private String createEvent()
    {
        JsonObject obj = eventActions.createEventBody( Constants.ORG_UNIT_IDS[0], eventProgramId, eventProgramStageID );

        ApiResponse response = eventActions.post( obj, new QueryParamsBuilder().add( "skipCache=true" ) );
        response.validate().statusCode( 200 );

        return response.extractUid();
    }

    private String createRelationship( String eventId, String event2Id, String relationshipTypeId )
    {
        JsonObject rel = relationshipActions.createRelationshipBody( relationshipTypeId, "event", eventId, "event", event2Id );

        return relationshipActions.create( rel );
    }

}
