

package org.hisp.dhis.tracker.events;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventImportValidationTests
    extends ApiTest
{
    private EventActions eventActions;
    private ProgramActions programActions;

    private static String ouId = Constants.ORG_UNIT_IDS[0];
    private static String eventProgramId;
    private static String eventProgramStageId;
    private static String trackerProgramId;
    private static String ouIdWithoutAccess;

    @BeforeAll
    public void beforeAll()
    {
        eventActions = new EventActions();
        programActions = new ProgramActions();

        new LoginActions().loginAsAdmin();

        setupData();
    }

    private static Stream<Arguments> provideValidationArguments()
    {
        return Stream.of(
            Arguments.arguments( null, eventProgramId, eventProgramStageId, "Event.orgUnit does not point to a valid organisation unit" ),
            Arguments.arguments( ouIdWithoutAccess, eventProgramId, eventProgramStageId, "Program is not assigned to this organisation unit" ),
            Arguments.arguments( ouId, null, eventProgramStageId, "Event.program does not point to a valid program" ),
            Arguments.arguments( ouId, trackerProgramId, null, "Event.programStage does not point to a valid programStage" ));
    }

    @ParameterizedTest
    @MethodSource( "provideValidationArguments" )
    public void eventImportShouldValidateReferences(String ouId, String programId, String programStageId, String message) {
        JsonObject jsonObject = eventActions.createEventBody( ouId, programId, programStageId );

        eventActions.post( jsonObject )
            .validate().statusCode( 409 )
            .body( "status", equalTo("ERROR") )
            .rootPath( "response" )
            .body( "ignored", equalTo( 1 ) )
            .body( "importSummaries.description[0]", containsStringIgnoringCase( message ) );
    }

    @Test
    public void eventImportShouldValidateEventDate() {
        JsonObject object = eventActions.createEventBody( ouId, eventProgramId, eventProgramStageId );

        object.addProperty( "eventDate", "" );
        object.addProperty( "status", "ACTIVE" );

        eventActions.post( object )
            .validate().statusCode( 409 )
            .body( "status", equalTo("ERROR") )
            .rootPath( "response" )
            .body( "ignored", equalTo( 1 ) )
            .body( "importSummaries.description[0]", containsString( "Event date is required" ) );
    }


    private void setupData()
    {
        eventProgramId = programActions
            .get( "", new QueryParamsBuilder().addAll( "filter=programType:eq:WITHOUT_REGISTRATION", "filter=name:$like:TA", "pageSize=1" ) )
            .extractString("programs.id[0]");

        assertNotNull( eventProgramId, "Failed to find a suitable event program");

        eventProgramStageId = programActions.programStageActions.get( "", new QueryParamsBuilder().add( "filter=program.id:eq:" +
            eventProgramId ))
            .extractString("programStages.id[0]");

        assertNotNull( eventProgramStageId, "Failed to find a program stage" );

        trackerProgramId = programActions
            .get( "", new QueryParamsBuilder().addAll( "filter=programType:eq:WITH_REGISTRATION", "filter=name:$like:TA", "pageSize=1" ) )
            .extractString("programs.id[0]");

        assertNotNull( trackerProgramId, "Failed to find a suitable tracker program");

        ouIdWithoutAccess = new OrgUnitActions().createOrgUnit();
    }
}
