

package org.hisp.dhis.metadata.metadata_export;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.not;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataExportTests extends ApiTest
{
    private String userWithoutAccessUsername = "MetadataExportTestsUser" + DataGenerator.randomString();
    private String userWithoutAccessPassword = "Test1212?";

    private MetadataActions metadataActions;
    private LoginActions loginActions;

    @BeforeAll
    public void beforeAll() {
        metadataActions = new MetadataActions();
        loginActions = new LoginActions();

        new UserActions().addUser( userWithoutAccessUsername, userWithoutAccessPassword );
        
    }

    @Test
    public void shouldNotExportAllMetadataWithoutAuthority() {
        loginActions.loginAsUser( userWithoutAccessUsername, userWithoutAccessPassword );

        metadataActions.get(  ).validate()
            .statusCode( 409 )
            .body( "message", equalTo("Unfiltered access to metadata export requires super user or 'F_METADATA_EXPORT' authority.") );
    }

    @Test
    public void shouldExportFilteredMetadataWithoutAuthority() {
        loginActions.loginAsUser( userWithoutAccessUsername, userWithoutAccessPassword );

        metadataActions.get( "", new QueryParamsBuilder().add( "dataElements=true" ) )
            .validate()
            .statusCode( 200 )
            .body( "dataElements", not(emptyArray()));
    }

    @Test
    public void shouldExportAllMetadataAsSuperuser() {
        loginActions.loginAsSuperUser();

        metadataActions.get(  ).validate()
            .statusCode( 200 )
            .body( "relationshipTypes", not( emptyArray() ) )
            .body( "userRoles", not( emptyArray() ) );
    }
}
