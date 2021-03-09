package org.hisp.dhis.actions.metadata;



import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.config.TestConfiguration;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author Luciano Fiandesio
 */
public class MetadataPaginationActions
    extends
    RestApiActions
{
    public static String DEFAULT_METADATA_FIELDS = "displayName,shortName,id,lastUpdated,created,displayDescription,code,publicAccess,access,href,level,displayName,publicAccess,lastUpdated,order";

    public static String DEFAULT_METADATA_FILTER = "name:ne:default";

    public static String DEFAULT_METADATA_SORT = "displayName:ASC";

    public MetadataPaginationActions( String endpoint )
    {
        super( endpoint );
    }

    /**
     * Executes a metadata request using pagination directives
     *
     * @param filter a List of String, containing the expressions to filter metadata
     *        on
     * @param fields a List of String, containing the name of the fields to return
     * @param sort a List of String, containing the sort expressions
     * @param page the page to return
     * @param pageSize the number of elements to return for each page
     * @return an {@see ApiResponse} object
     */
    public ApiResponse getPaginated( List<String> filter, List<String> fields, List<String> sort, int page,
        int pageSize )
    {
        assert filter != null;
        assert fields != null && !fields.isEmpty();
        assert sort != null && !sort.isEmpty();
        QueryParamsBuilder params = new QueryParamsBuilder().add( "filter=" + String.join( ",", filter ) )
            .add( "fields=" + String.join( ",", fields ) ).add( "order=" + String.join( ",", sort ) )
            .add( "page=" + page ).add( "pageSize=" + pageSize );

        return get( "", params );

    }

    /**
     * Executes a metadata request using pagination directives. Uses a default
     * filter expression
     *
     * @param fields a List of String, containing the name of the fields to return
     * @param sort a List of String, containing the sort expressions
     * @param page the page to return
     * @param pageSize the number of elements to return for each page
     * @return an {@see ApiResponse} object
     */
    public ApiResponse getPaginated( List<String> fields, List<String> sort, int page, int pageSize )
    {
        return getPaginated( toParamList( DEFAULT_METADATA_FILTER ), fields, sort, page, pageSize );
    }

    /**
     * Executes a metadata request using pagination directives. Uses a default
     * filter and sort expression
     *
     * @param fields a List of String, containing the name of the fields to return
     * @param page the page to return
     * @param pageSize the number of elements to return for each page
     * @return an {@see ApiResponse} object
     */
    public ApiResponse getPaginated( List<String> fields, int page, int pageSize )
    {
        return getPaginated( toParamList( DEFAULT_METADATA_FILTER ), fields, toParamList( DEFAULT_METADATA_SORT ), page,
            pageSize );
    }

    public ApiResponse getPaginatedWithFiltersOnly( List<String> filters, int page, int pageSize )
    {
        return getPaginated( filters, toParamList( DEFAULT_METADATA_FIELDS ), toParamList( DEFAULT_METADATA_SORT ), page,
            pageSize );
    }


    /**
     * Executes a metadata request using pagination directives. Uses a default
     * filter, fields and sort expression
     *
     * @param page the page to return
     * @param pageSize the number of elements to return for each page
     * @return an {@see ApiResponse} object
     */
    public ApiResponse getPaginated( int page, int pageSize )
    {
        return getPaginated( toParamList( DEFAULT_METADATA_FILTER ), toParamList( DEFAULT_METADATA_FIELDS ),
            toParamList( DEFAULT_METADATA_SORT ), page, pageSize );
    }

    /**
     * Assert on the pagination ("pager") data within the API response
     *
     * @param response an {@see ApiResponse} object
     * @param expectedTotal the expected minimum total number of metadata items
     * @param expectedPageCount the expected minimum total number of pages
     * @param expectedPageSize the expected value for page size
     * @param expectedPage the expected value for the page
     */
    public void assertPagination( ApiResponse response, int expectedTotal, int expectedPageCount, int expectedPageSize,
        int expectedPage )
    {
        response.validate().statusCode( 200 ).rootPath( "pager" )
            .body( "pageCount", greaterThanOrEqualTo( expectedPageCount ) )
            .body( "total", greaterThanOrEqualTo( expectedTotal ) )
            .body( "pageSize", is( expectedPageSize ) )
            .body( "page", is( expectedPage ) )
            .body( "nextPage", startsWith( TestConfiguration.get().baseUrl() + endpoint ) )
            .body( "nextPage", containsString( "pageSize=" + expectedPageSize ) )
            .body( "nextPage", containsString( "page=" + (expectedPage + 1) ) );

    }

    public void assertPagination( ApiResponse response, int expectedTotal, int expectedPageCount, int expectedPageSize,
        int expectedPage, String entityWrapper )
    {
        response.validate().statusCode( 200 ).rootPath( "pager" )
            .body( "pageCount", greaterThanOrEqualTo( expectedPageCount ) )
            .body( "total", greaterThanOrEqualTo( expectedTotal ) )
            .body( "pageSize", is( expectedPageSize ) )
            .body( "page", is( expectedPage ) )
            .body( "nextPage", startsWith( TestConfiguration.get().baseUrl() + endpoint ) )
            .body( "nextPage", containsString( "pageSize=" + expectedPageSize ) )
            .body( "nextPage", containsString( "page=" + (expectedPage + 1) ) );

        assert response.extractList( entityWrapper ).size() == expectedPageSize;

    }

    private List<String> toParamList( String string )
    {
        return Arrays.asList( string.split( "," ) );
    }
}
