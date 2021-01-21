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

package org.hisp.dhis.tracker.importer.events;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventValidationTests
    extends ApiTest
{
    private static final String OU_ID = Constants.ORG_UNIT_IDS[0];

    private static String eventProgramId = Constants.EVENT_PROGRAM_ID;

    private static String eventProgramStageId;

    private static String trackerProgramId = Constants.TRACKER_PROGRAM_ID;

    private static String trackerProgramStageId;

    private static String ouIdWithoutAccess;

    private ProgramActions programActions;

    private TrackerActions trackerActions;

    private EventActions eventActions;

    private String enrollment;

    private static Stream<Arguments> provideValidationArguments()
    {
        return Stream.of(
            Arguments.of( OU_ID, trackerProgramId, trackerProgramStageId, "E1033" ),
            Arguments.arguments( null, eventProgramId, eventProgramStageId,
                "E1011" ),
            Arguments.arguments( ouIdWithoutAccess, eventProgramId, eventProgramStageId,
                "E1029" ),
            Arguments.arguments( OU_ID, trackerProgramId, null, "E1086" ),
            Arguments.arguments( OU_ID, trackerProgramId, eventProgramStageId, "E1089" ) );
    }

    @BeforeAll
    public void beforeAll()
    {
        programActions = new ProgramActions();
        trackerActions = new TrackerActions();
        eventActions = new EventActions();

        new LoginActions().loginAsSuperUser();
        setupData();
    }

    @Test
    public void shouldNotImportDeletedEvents()
        throws Exception
    {
        JsonObject eventBody = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/events/event.json" ) );

        String eventId = trackerActions.postAndGetJobReport( eventBody )
            .validateSuccessfulImport()
            .extractImportedEvents().get( 0 );

        eventActions.softDelete( eventId );

        TrackerApiResponse response = trackerActions.postAndGetJobReport( eventBody );

        response.validateErrorReport()
            .body( "errorCode", hasItem( "E1082" ) );
    }

    @CsvSource( { "ACTIVE,,OccurredAt date is missing.", "SCHEDULE,,ScheduledAt date is missing." } )
    @ParameterizedTest
    public void shouldValidateEventProperties( String status, String occurredAt, String error )
    {
        JsonObject object = trackerActions.buildEvent( OU_ID, trackerProgramId, trackerProgramStageId );

        JsonObject event = object.getAsJsonArray( "events" ).get( 0 ).getAsJsonObject();
        event.addProperty( "occurredAt", occurredAt );
        event.addProperty( "status", status );
        event.addProperty( "enrollment", enrollment );

        TrackerApiResponse response = trackerActions.postAndGetJobReport( object );
        response.validateErrorReport()
            .body( "message[0]", containsStringIgnoringCase( error ) );
    }

    @Test
    public void shouldSetDueDate()
    {
        JsonObject object = trackerActions.buildEvent( OU_ID, trackerProgramId, trackerProgramStageId );
        object.getAsJsonArray( "events" ).get( 0 ).getAsJsonObject().addProperty( "enrollment", enrollment );

        TrackerApiResponse response = trackerActions.postAndGetJobReport( object );

        String eventId = response.validateSuccessfulImport().extractImportedEvents().get( 0 );
        JsonObject obj = trackerActions.get( "/events/" + eventId ).getBody();

        assertEquals( obj.get( "eventDate" ), obj.get( "dueDate" ) );
    }

    @ParameterizedTest
    @MethodSource( "provideValidationArguments" )
    public void eventImportShouldValidateReferences( String ouId, String programId, String programStageId, String errorCode )
    {
        JsonObject jsonObject = trackerActions.buildEvent( ouId, programId, programStageId );

        TrackerApiResponse response = trackerActions.postAndGetJobReport( jsonObject );

        response.validateErrorReport()
            .body( "errorCode", hasItem( equalTo( errorCode ) ) );
    }

    private void setupData()
    {
        eventProgramStageId = programActions.programStageActions.get( "", new QueryParamsBuilder().add( "filter=program.id:eq:" +
            eventProgramId ) )
            .extractString( "programStages.id[0]" );

        assertNotNull( eventProgramStageId, "Failed to find a program stage" );

        trackerProgramStageId = programActions.programStageActions
            .get( "", new QueryParamsBuilder().addAll( "filter=program.id:eq:" +
                trackerProgramId, "filter=repeatable:eq:true" ) )
            .extractString( "programStages.id[0]" );

        ouIdWithoutAccess = new OrgUnitActions().createOrgUnit();
        new UserActions().grantCurrentUserAccessToOrgUnit( ouIdWithoutAccess );

        enrollment = trackerActions.postAndGetJobReport( trackerActions.buildTeiAndEnrollment( OU_ID, trackerProgramId ) )
            .validateSuccessfulImport().extractImportedEnrollments().get( 0 );

    }
}
