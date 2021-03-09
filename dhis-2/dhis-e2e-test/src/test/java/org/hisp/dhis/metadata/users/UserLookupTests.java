

package org.hisp.dhis.metadata.users;

import com.google.gson.JsonArray;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UserLookupTests
    extends ApiTest
{
    private RestApiActions lookupActions;

    private UserActions userActions;

    @BeforeAll
    public void beforeAll()
    {
        lookupActions = new RestApiActions( "/userLookup" );
        userActions = new UserActions();

        new LoginActions().loginAsSuperUser();
    }

    @ParameterizedTest
    @CsvSource( { "PQD6wXJ2r5j,id", "taadmin,username" } )
    public void shouldLookupSpecificUser( String resource, String propertyToValidate )
    {
        ApiResponse response = lookupActions.get( resource );

        response
            .validate().statusCode( 200 )
            .body( propertyToValidate, equalTo( resource ) )
            .body( "id", notNullValue() )
            .body( "username", notNullValue() )
            .body( "firstName", notNullValue() )
            .body( "surname", notNullValue() )
            .body( "displayName", notNullValue() );
    }

    @ParameterizedTest
    @ValueSource( strings = { "tasuper", "tasuperadmin", "TA", "TA", "Admin", "Superuser" } )
    public void shouldLookupUserWithQuery( String query )
    {
        ApiResponse response = lookupActions.get( "?query=" + query );

        response.validate()
            .statusCode( 200 )
            .body( "users", hasSize( greaterThan( 0 ) ) );

        JsonArray users = response.extractJsonObject( "" ).getAsJsonArray( "users" );

        users.forEach( user -> {
            String str = user.getAsJsonObject().toString();
            assertThat( str, containsStringIgnoringCase( query ) );
        } );
    }

    @ParameterizedTest
    @ValueSource( strings = { "taadmin@", "@dhis2.org", "tasuperuser@dhis2.org" } )
    public void shouldLookupUserByEmail( String query )
    {
        ApiResponse response = lookupActions.get( "?query=" + query );

        response.validate()
            .statusCode( 200 )
            .body( "users", hasSize( greaterThan( 0 ) ) );

        List<String> users = response.extractList( "users.id" );

        users.forEach( user -> {
            userActions.get( user )
                .validate()
                .statusCode( 200 )
                .body( "email", containsStringIgnoringCase( query ) );
        } );
    }

}
