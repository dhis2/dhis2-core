package org.hisp.dhis.metadata.users;



import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataPaginationActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hisp.dhis.actions.metadata.MetadataPaginationActions.DEFAULT_METADATA_FIELDS;
import static org.hisp.dhis.actions.metadata.MetadataPaginationActions.DEFAULT_METADATA_FILTER;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class UserPaginationTest extends ApiTest
{
    private MetadataPaginationActions paginationActions;
    private UserActions userActions;

    private int startPage = 2;
    private int pageSize = 5;
    private int total = 50;

    @BeforeEach
    public void setUp()
    {
        LoginActions loginActions = new LoginActions();
        userActions = new UserActions();

        paginationActions = new MetadataPaginationActions( "/users" );
        loginActions.loginAsSuperUser();

        // Creates Users
        for ( int i = 0; i < total; i++ )
        {
            userActions.addUser( DataGenerator.randomString() + i, DataGenerator.randomString() + i,  DataGenerator.randomString() + i, DataGenerator.randomString() + "Abcd1234!" + i );
        }
    }

    @Test
    public void checkPaginationResultsForcingInMemoryPagination()
    {
        // this test forces the metadata query engine to execute an "in memory" sorting and pagination
        // since the sort ("order") value is set to 'displayName' that is a "virtual" field (that is, not a database column)
        // The metadata query engine can not execute a sql query using this field, since it does not exist
        // on the table. Therefore, the engine loads the entire content of the table in memory and
        // executes a sort + pagination "in memory"

        ApiResponse response = paginationActions.getPaginated( startPage, pageSize );

        response.validate().statusCode( 200 );

        paginationActions.assertPagination( response, total, total / pageSize, pageSize, startPage );
    }

    @Test
    public void checkPaginationResultsWithBothDatabaseAndInMemory()
    {
        ApiResponse response = paginationActions.getPaginatedWithFiltersOnly(
            Arrays.asList( DEFAULT_METADATA_FILTER.split( "," ) ), startPage, pageSize );

        response.validate().statusCode( 200 );

        paginationActions.assertPagination( response, total, total / pageSize, pageSize, startPage, "users" );
    }

    @Test
    public void checkPaginationResultsForcingDatabaseOnlyPagination()
    {
        // this test forces the metadata query engine to execute the query (including pagination) on the database only.
        // The sort ("order") value is set to 'id' that is mapped to a DB column.

        ApiResponse response = paginationActions.getPaginated(
            Arrays.asList( DEFAULT_METADATA_FILTER.split( "," ) ),
            Arrays.asList( DEFAULT_METADATA_FIELDS.split( "," ) ),
            Collections.singletonList("id:ASC"),
            startPage, pageSize );

        response.validate().statusCode( 200 );

        paginationActions.assertPagination( response, total, total / pageSize, pageSize, startPage );
    }
}
