/*
 * Copyright (c) 2004-2020, University of Oslo
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

package org.hisp.dhis.tracker.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.ProgramStageActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class NotificationsTests
    extends ApiTest
{
    private String trackerProgramStageId = "PaOOjwLVW23";

    private String trackerProgramId = Constants.TRACKER_PROGRAM_ID;

    private String enrollmentId;

    private TrackerActions trackerActions;

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        trackerActions = new TrackerActions();
        new LoginActions().loginAsSuperUser();

        setupData();
    }

    @Test
    public void shouldSendNotificationOnProgramStageCompletion()
        throws Exception
    {
        JsonObject object = new FileReaderUtils().read( new File( "src/test/resources/tracker/importer/events/event.json" ) )
            .replacePropertyValuesWithIds( "event" )
            .replacePropertyValuesWith( "programStage", trackerProgramStageId )
            .replacePropertyValuesWith( "program", trackerProgramId )
            .replacePropertyValuesWith( "status", "COMPLETED" )
            .replacePropertyValuesWith( "enrollment", enrollmentId )
            .get( JsonObject.class );

        ApiResponse response = new RestApiActions( "/messageConversations" ).get( "", new QueryParamsBuilder().add( "fields=*" ) );

        int size = response.getBody().getAsJsonArray( "messageConversations" ).size();

        trackerActions.postAndGetJobReport( object )
            .validateSuccessfulImport();

        response = new RestApiActions( "/messageConversations?fields=*" ).get( "", new QueryParamsBuilder().add( "fields=*" ) );

        response
            .validate()
            .statusCode( 200 )
            .body( "messageConversations", hasSize( greaterThanOrEqualTo( size + 1 ) ) )
            .body( "messageConversations.subject", hasItem( "TA program stage completion" ) );
    }

    @Test
    @Disabled
    public void shouldTriggerNotificationOnDueDates()
        throws Exception
    {
        JsonObject object = new FileReaderUtils().read( new File( "src/test/resources/tracker/importer/events/event.json" ) )
            .replacePropertyValuesWithIds( "event" )
            .replacePropertyValuesWith( "scheduledAt", Instant.now().minus( 0, ChronoUnit.DAYS ).toString() )
            .replacePropertyValuesWith( "programStage", trackerProgramStageId )
            .replacePropertyValuesWith( "program", trackerProgramId )
            .replacePropertyValuesWith( "status", "COMPLETED" )
            .replacePropertyValuesWith( "enrollment", enrollmentId )
            .get( JsonObject.class );

        TrackerApiResponse trackerApiResponse = trackerActions.postAndGetJobReport( object );
        trackerApiResponse.validateSuccessfulImport();

        trackerApiResponse.prettyPrint();

        ApiResponse response = new RestApiActions( "/messageConversations" )
            .get( "", new QueryParamsBuilder().add( "fields=subject" ) );

        response
            .validate()
            .statusCode( 200 )
            .body( "messageConversations", not( Matchers.emptyArray() ) )
            .body( "messageConversations.subject", hasItem( "TA program notification - due dates" ) );
    }

    private void setupData()
        throws Exception
    {
        ProgramStageActions programStageActions = new ProgramStageActions();

        JsonArray array = new FileReaderUtils().read( new File( "src/test/resources/tracker/notificationTemplates.json" ) )
            .get( JsonObject.class ).getAsJsonArray( "programNotificationTemplates" );

        array.forEach( nt -> {
            String programNotificationTemplate = new RestApiActions( "/programNotificationTemplates" ).post( nt.getAsJsonObject() )
                .extractUid();

            JsonObject programStage = JsonObjectBuilder.jsonObject( programStageActions.get( trackerProgramStageId ).getBody() )
                .addOrAppendToArray( "notificationTemplates",
                    new JsonObjectBuilder().addProperty( "id", programNotificationTemplate ).build() )
                .build();

            programStageActions.update( trackerProgramStageId, programStage )
                .validate().statusCode( 200 );

            programStageActions.get( trackerProgramStageId ).validate()
                .body( "notificationTemplates.id", hasItem( programNotificationTemplate ) );

            enrollmentId = trackerActions
                .postAndGetJobReport( new File( "src/test/resources/tracker/importer/teis/teiWithEnrollments.json" ) )
                .validateSuccessfulImport()
                .extractImportedEnrollments().get( 0 );
        } );

    }
}
