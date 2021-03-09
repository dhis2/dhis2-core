package org.hisp.dhis.metadata;



import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class DataElementsTest
    extends ApiTest
{
    private RestApiActions dataElementActions;

    private RestApiActions categoryComboActions;

    private LoginActions loginActions;

    private Stream<Arguments> getDataElementCombinations()
    {
        return Stream.of( new Arguments[] {
            Arguments.of( "AGGREGATE", "NUMBER", "SUM", false, null ),
            Arguments.of( "TRACKER", "TEXT", "CUSTOM", true, "DISAGGREGATION" ),
            Arguments.of( "TRACKER", "AGE", "NONE", true, "ATTRIBUTE" )
        } );
    }

    @BeforeAll
    public void beforeAll()
    {
        dataElementActions = new RestApiActions( "/dataElements" );
        categoryComboActions = new RestApiActions( "/categoryCombos" );
        loginActions = new LoginActions();

        loginActions.loginAsSuperUser();
    }

    @ParameterizedTest
    @MethodSource( "getDataElementCombinations" )
    public void shouldCreate( String domainType, String valueType, String aggregationType, boolean withCategoryCombo,
        String categoryComboDimensionType )
    {
        // arrange
        JsonObject body = generateBaseBody();
        body.addProperty( "domainType", domainType );
        body.addProperty( "valueType", valueType );
        body.addProperty( "aggregationType", aggregationType );

        if ( withCategoryCombo )
        {
            String categoryComboId = createCategoryCombo( categoryComboDimensionType );

            JsonObject categoryCombo = new JsonObject();
            categoryCombo.addProperty( "id", categoryComboId );

            body.add( "categoryCombo", categoryCombo );
        }

        // act
        ApiResponse response = dataElementActions.post( body );

        // assert
        ResponseValidationHelper.validateObjectCreation( response );
    }

    private JsonObject generateBaseBody()
    {
        JsonObject object = new JsonObject();
        object.addProperty( "name", DataGenerator.randomEntityName() );
        object.addProperty( "shortName", DataGenerator.randomEntityName() );

        return object;
    }

    public String createCategoryCombo( String dimensionType )
    {
        JsonObject body = new JsonObject();
        body.addProperty( "name", DataGenerator.randomEntityName() );
        body.addProperty( "dataDimensionType", dimensionType );

        return categoryComboActions.create( body );
    }
}
