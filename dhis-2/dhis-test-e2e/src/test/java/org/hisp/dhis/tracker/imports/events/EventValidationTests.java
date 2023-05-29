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
package org.hisp.dhis.tracker.imports.events;

import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.deprecated.tracker.EventActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.EventDataBuilder;
import org.hisp.dhis.tracker.imports.databuilder.TeiDataBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventValidationTests
    extends TrackerApiTest
{
    private static final String OU_ID = Constants.ORG_UNIT_IDS[0];

    private static String eventProgramId = Constants.EVENT_PROGRAM_ID;

    private static String eventProgramStageId;

    private static String trackerProgramId = Constants.TRACKER_PROGRAM_ID;

    private static String anotherTrackerProgramId = Constants.ANOTHER_TRACKER_PROGRAM_ID;

    private static String trackerProgramStageId;

    private static String ouIdWithoutAccess;

    private ProgramActions programActions;

    private EventActions eventActions;

    private String enrollment;

    private static Stream<Arguments> provideValidationArguments()
    {
        return Stream.of(
            Arguments.of( OU_ID, trackerProgramId, trackerProgramStageId, "E1033" ),
            Arguments.arguments( null, eventProgramId, eventProgramStageId, "E1123" ),
            Arguments.arguments( ouIdWithoutAccess, eventProgramId, eventProgramStageId, "E1029" ),
            Arguments.arguments( OU_ID, trackerProgramId, null, "E1123" ),
            Arguments.arguments( OU_ID, trackerProgramId, eventProgramStageId, "E1089" ) );
    }

    @BeforeAll
    public void beforeAll()
    {
        programActions = new ProgramActions();
        eventActions = new EventActions();

        loginActions.loginAsSuperUser();
        setupData();
    }

    @Test
    public void shouldNotImportDeletedEvents()
        throws Exception
    {
        JsonObject eventBody = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/events/event.json" ) );

        String eventId = trackerImportExportActions.postAndGetJobReport( eventBody )
            .validateSuccessfulImport()
            .extractImportedEvents().get( 0 );

        eventActions.softDelete( eventId );

        TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport( eventBody );

        response.validateErrorReport()
            .body( "errorCode", hasItem( "E1082" ) );
    }

    @CsvSource( { "ACTIVE,,OccurredAt date is missing.", "SCHEDULE,,ScheduledAt date is missing." } )
    @ParameterizedTest
    public void shouldValidateEventProperties( String status, String occurredAt, String error )
    {
        JsonObject object = new EventDataBuilder()
            .setStatus( status )
            .setEventDate( occurredAt )
            .setEnrollment( enrollment )
            .array( OU_ID, trackerProgramId, trackerProgramStageId );

        TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport( object );
        response.validateErrorReport()
            .body( "message[0]", containsStringIgnoringCase( error ) );
    }

    @Test
    public void shouldSetDueDate()
    {
        JsonObject eventBody = new EventDataBuilder()
            .setEnrollment( enrollment )
            .array( OU_ID, trackerProgramId, trackerProgramStageId );

        TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport( eventBody );

        String eventId = response.validateSuccessfulImport().extractImportedEvents().get( 0 );

        JsonObject event = trackerImportExportActions.get( "/events/" + eventId ).getBody();

        assertEquals( event.get( "eventDate" ), event.get( "dueDate" ) );
    }

    @ParameterizedTest
    @MethodSource( "provideValidationArguments" )
    public void eventImportShouldValidateReferences( String ouId, String programId, String programStageId,
        String errorCode )
    {
        JsonObject jsonObject = new EventDataBuilder().array( ouId, programId, programStageId );

        TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport( jsonObject );

        response.validateErrorReport()
            .body( "errorCode", hasItem( equalTo( errorCode ) ) );
    }

    @Test
    public void eventImportShouldValidateProgramFromProgramStage()
    {
        JsonObject jsonObject = new EventDataBuilder()
            .setEnrollment( enrollment )
            .array( OU_ID, anotherTrackerProgramId, trackerProgramStageId );

        TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport( jsonObject );

        response.validateErrorReport()
            .body( "errorCode", hasItem( equalTo( "E1079" ) ) );
    }

    @Test
    public void eventImportShouldPassValidationWhenOnlyEventProgramIsDefined()
    {
        JsonObject jsonObject = new EventDataBuilder().array( OU_ID, eventProgramId, null );

        TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport( jsonObject );

        response.validateSuccessfulImport();
    }

    @Test
    public void shouldValidateCategoryCombo()
    {
        ApiResponse program = programActions.get( "", new QueryParamsBuilder().add( "programType=WITHOUT_REGISTRATION" )
            .add( "filter=categoryCombo.code:!eq:default" )
            .add( "filter=name:like:TA" )
            .add( "fields=id,categoryCombo[categories[categoryOptions]]" ) );

        String programId = program.extractString( "programs.id[0]" );
        Assumptions.assumeFalse( StringUtils.isEmpty( programId ) );

        JsonObject object = new EventDataBuilder()
            .setProgram( programId )
            .setAttributeCategoryOptions( Arrays.asList( "invalid-option" ) )
            .setOu( OU_ID ).array();

        trackerImportExportActions.postAndGetJobReport( object )
            .validateErrorReport()
            .body( "errorCode", hasItem( "E1116" ) );
    }

    @Test
    public void shouldReturnErrorWhenUpdatingSoftDeletedEvent()
    {
        JsonObject events = new EventDataBuilder().array( OU_ID, eventProgramId, null );

        // Create Event
        TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport( events );

        response.validateSuccessfulImport();

        String eventId = response.extractImportedEvents().get( 0 );
        JsonObject eventsToDelete = new EventDataBuilder()
            .setId( eventId )
            .array();

        // Delete Event
        TrackerApiResponse deleteResponse = trackerImportExportActions.postAndGetJobReport( eventsToDelete,
            new QueryParamsBuilder().add( "importStrategy=DELETE" ) );

        deleteResponse.validateSuccessfulImport();

        JsonObject eventsToImportAgain = new EventDataBuilder()
            .setId( eventId )
            .array( OU_ID, eventProgramId, null );

        // Update Event
        TrackerApiResponse responseImportAgain = trackerImportExportActions.postAndGetJobReport( eventsToImportAgain );

        responseImportAgain
            .validateErrorReport()
            .body( "errorCode", Matchers.hasItem( "E1082" ) );
    }

    private void setupData()
    {
        eventProgramStageId = programActions.programStageActions
            .get( "", new QueryParamsBuilder().add( "filter=program.id:eq:" +
                eventProgramId ) )
            .extractString( "programStages.id[0]" );

        assertNotNull( eventProgramStageId, "Failed to find a program stage" );

        trackerProgramStageId = programActions.programStageActions
            .get( "", new QueryParamsBuilder().addAll( "filter=program.id:eq:" +
                trackerProgramId, "filter=repeatable:eq:true" ) )
            .extractString( "programStages.id[0]" );

        ouIdWithoutAccess = new OrgUnitActions().createOrgUnit();
        new UserActions().grantCurrentUserAccessToOrgUnit( ouIdWithoutAccess );

        enrollment = trackerImportExportActions
            .postAndGetJobReport( new TeiDataBuilder().buildWithEnrollment( OU_ID, trackerProgramId ) )
            .validateSuccessfulImport().extractImportedEnrollments().get( 0 );
    }
}
