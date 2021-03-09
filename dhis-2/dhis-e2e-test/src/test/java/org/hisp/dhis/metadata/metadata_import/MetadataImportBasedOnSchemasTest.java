package org.hisp.dhis.metadata.metadata_import;



import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.SchemasActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.schemas.PropertyType;
import org.hisp.dhis.dto.schemas.SchemaProperty;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataImportBasedOnSchemasTest
    extends ApiTest
{
    private LoginActions loginActions;

    private SchemasActions schemasActions;

    @BeforeAll
    public void beforeAll()
    {
        schemasActions = new SchemasActions();
        loginActions = new LoginActions();

        loginActions.loginAsSuperUser();
    }

    @ParameterizedTest
    @MethodSource( "getSchemaEndpoints" )
    // todo add better schema validation when spec is ready
    public void getMatchesSchema( String endpoint, String schema )
    {
        RestApiActions apiActions = new RestApiActions( endpoint );

        ApiResponse response = apiActions.get( "?fields=*" );

        response.validate()
            .statusCode( 200 )
            .body( endpoint, Matchers.notNullValue() );

        Object firstObject = response.extract( endpoint + "[0]" );

        if ( firstObject == null )
        {
            return;
        }

        schemasActions.validateObjectAgainstSchema( schema, firstObject )
            .validate().statusCode( 200 );
    }

    @ParameterizedTest
    @MethodSource( "getSchemaEndpoints" )
    public void postBasedOnSchema( String endpoint, String schema )
    {
        RestApiActions apiActions = new RestApiActions( endpoint );

        List blacklistedEndpoints = Arrays.asList( "jobConfigurations",
            "relationshipTypes",
            "messageConversations",
            "users" ); //blacklisted because contains conditionally required properties, which are not marked as required

        List<SchemaProperty> schemaProperties = schemasActions.getRequiredProperties( schema );

        Assumptions.assumeFalse( blacklistedEndpoints.contains( endpoint ), "N/A test case - blacklisted endpoint." );
        Assumptions.assumeFalse(
            schemaProperties.stream().anyMatch( schemaProperty -> schemaProperty.getPropertyType() == PropertyType.COMPLEX ),
            "N/A test case - body would require COMPLEX objects." );

        // post
        JsonObject object = DataGenerator.generateObjectMatchingSchema( schemaProperties );
        ApiResponse response = apiActions.post( object );

        // validate response;
        ResponseValidationHelper.validateObjectCreation( response );

        // validate removal;
        response = apiActions.delete( response.extractUid() );

        ResponseValidationHelper.validateObjectRemoval( response, endpoint + " was not deleted" );
    }

    private Stream<Arguments> getSchemaEndpoints()
    {
        ApiResponse apiResponse = schemasActions.get();

        String jsonPathIdentifier = "schemas.findAll{it.relativeApiEndpoint && it.metadata && it.singular != 'externalFileResource'}";
        List<String> apiEndpoints = apiResponse.extractList( jsonPathIdentifier + ".plural" );
        List<String> schemaEndpoints = apiResponse.extractList( jsonPathIdentifier + ".singular" );

        ArrayList<Arguments> arguments = new ArrayList<>();
        for ( int i = 0; i < apiEndpoints.size(); i++ )
        {
            arguments.add( Arguments.of( apiEndpoints.get( i ), schemaEndpoints.get( i ) ) );
        }

        return arguments.stream();
    }
}
