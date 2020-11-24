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
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker_v2.TrackerActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImporter_eventValidationTests
    extends ApiTest
{
    private static final String OU_ID = Constants.ORG_UNIT_IDS[0];

    private static String eventProgramId;

    private static String eventProgramStageId;

    private static String trackerProgramId;

    private static String ouIdWithoutAccess;

    private ProgramActions programActions;

    private TrackerActions trackerActions;

    private static Stream<Arguments> provideValidationArguments()
    {
        return Stream.of(
            Arguments.arguments( null, eventProgramId, eventProgramStageId,
                "Could not find OrganisationUnit: `NULL`, linked to Event." ),
            Arguments.arguments( ouIdWithoutAccess, eventProgramId, eventProgramStageId,
                "Program is not assigned to this organisation unit" ),
            Arguments.arguments( OU_ID, null, eventProgramStageId, "Event.program does not point to a valid program" ),
            Arguments.arguments( OU_ID, trackerProgramId, null, "Event.programStage does not point to a valid programStage" ) );
    }

    @BeforeAll
    public void beforeAll()
    {
        programActions = new ProgramActions();
        trackerActions = new TrackerActions();

        new LoginActions().loginAsAdmin();

        setupData();
    }

    @Test
    public void shouldValidateEventDate()
    {
        JsonObject object = trackerActions.createEventsBody( OU_ID, eventProgramId, eventProgramStageId );

        JsonObject event = object.getAsJsonArray( "events" ).get( 0 ).getAsJsonObject();
        event.addProperty( "occurredAt", "" );
        event.addProperty( "status", "ACTIVE" );

        trackerActions.postAndGetJobReport( object )
            .validateErrorReport()
            .validate()
            .body( "validationReport.errorReports.message[0]", containsStringIgnoringCase( "OccurredAt date is missing." ) );
    }

    @ParameterizedTest
    @MethodSource( "provideValidationArguments" )
    public void eventImportShouldValidateReferences( String ouId, String programId, String programStageId, String message )
    {
        JsonObject jsonObject = trackerActions.createEventsBody( ouId, programId, programStageId );

        System.out.println( jsonObject );
        TrackerApiResponse response = trackerActions.postAndGetJobReport( jsonObject );

        response.validateErrorReport()
            .validate()
            .body( "validationReport.errorReports[0].message", containsStringIgnoringCase( message ) );
    }

    private void setupData()
    {
        eventProgramId = Constants.EVENT_PROGRAM_ID;

        eventProgramStageId = programActions.programStageActions.get( "", new QueryParamsBuilder().add( "filter=program.id:eq:" +
            eventProgramId ) )
            .extractString( "programStages.id[0]" );

        assertNotNull( eventProgramStageId, "Failed to find a program stage" );

        trackerProgramId = Constants.TRACKER_PROGRAM_ID;

        ouIdWithoutAccess = new OrgUnitActions().createOrgUnit();
    }
}
