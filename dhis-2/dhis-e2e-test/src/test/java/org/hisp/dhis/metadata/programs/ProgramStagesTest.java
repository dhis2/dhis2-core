package org.hisp.dhis.metadata.programs;



import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ProgramStagesTest
    extends ApiTest
{
    private LoginActions loginActions;

    private ProgramActions programActions;

    private RestApiActions programStageActions;

    private String programId;

    private String programStageId;

    @BeforeAll
    public void beforeAll()
    {
        loginActions = new LoginActions();
        programActions = new ProgramActions();
        programStageActions = programActions.programStageActions;

        loginActions.loginAsSuperUser();
        programId = programActions.createTrackerProgram().extractUid();
        programStageId = programActions.createProgramStage( "Tracker program stage 1" );
    }

    @Test
    public void shouldAddProgramStageToProgram()
    {
        // arrange

        JsonObject programBody = programActions.get( programId ).getBody();
        JsonArray programStages = new JsonArray();

        JsonObject programStage = new JsonObject();
        programStage.addProperty( "id", programStageId );

        programStages.add( programStage );

        programBody.add( "programStages", programStages );

        // act
        ApiResponse response = programActions.update( programId, programBody );

        // assert
        ResponseValidationHelper.validateObjectUpdate( response, 200 );

        response = programActions.get( programId );
        response.validate().statusCode( 200 )
            .body( "programStages", not( emptyArray() ) )
            .body( "programStages.id", not( emptyArray() ) )
            .body( "programStages.id", hasItem( programStageId ) );

        response = programStageActions.get( programStageId );
        response.validate().statusCode( 200 )
            .body( "program", notNullValue() )
            .body( "program.id", equalTo( programId ) );

    }
}
