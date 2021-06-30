package org.hisp.dhis.metadata;



import org.apache.commons.lang3.RandomStringUtils;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.MetadataPaginationActions;
import org.hisp.dhis.actions.metadata.OptionActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hisp.dhis.actions.metadata.MetadataPaginationActions.DEFAULT_METADATA_FIELDS;
import static org.hisp.dhis.actions.metadata.MetadataPaginationActions.DEFAULT_METADATA_FILTER;

/**
 * @author Luciano Fiandesio
 */
public class MetadataPaginationTest
    extends
    ApiTest
{
    private MetadataPaginationActions paginationActions;

    private int startPage = 1;
    private int pageSize = 5;
    @BeforeEach
    public void setUp()
    {
        LoginActions loginActions = new LoginActions();
        OptionActions optionActions = new OptionActions();
        paginationActions = new MetadataPaginationActions( "/optionSets" );
        loginActions.loginAsSuperUser();

        // Creates 100 Option Sets
        for ( int i = 0; i < 100; i++ )
        {
            optionActions.createOptionSet( RandomStringUtils.randomAlphabetic( 10 ), "INTEGER", (String[]) null );
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

        paginationActions.assertPagination( response, 100, 100 / pageSize, pageSize, startPage);
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

        paginationActions.assertPagination( response, 100, 100 / pageSize, pageSize, startPage);
    }

}
