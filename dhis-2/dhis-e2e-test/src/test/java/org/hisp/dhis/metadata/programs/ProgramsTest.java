package org.hisp.dhis.metadata.programs;



import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ProgramsTest
    extends ApiTest
{
    private LoginActions loginActions;

    private ProgramActions programActions;

    @BeforeAll
    public void beforeAll()
    {
        loginActions = new LoginActions();
        programActions = new ProgramActions();
    }

    @BeforeEach
    public void before()
    {
        loginActions.loginAsSuperUser();
    }

    @ParameterizedTest( name = "withType[{0}]" )
    @ValueSource( strings = { "WITH_REGISTRATION", "WITHOUT_REGISTRATION" } )
    public void shouldCreateProgram( String programType )
    {
        JsonObject object = programActions.getDummy();
        object.addProperty( "programType", programType );

        ApiResponse response = programActions.post( object );

        ResponseValidationHelper.validateObjectCreation( response );
    }
}
