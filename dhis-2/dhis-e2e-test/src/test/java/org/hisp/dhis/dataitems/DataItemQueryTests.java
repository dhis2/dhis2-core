
package org.hisp.dhis.dataitems;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.dataitem.DataItemActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test cases related to GET "dataItems" endpoint. The tests and assertions are
 * based on the file "setup/metadata.json" => "programIndicators",
 * "dataElements".
 *
 * The test cases using default pagination will imply "paging=true", which is
 * the default when "paging" is omitted.
 *
 * @author maikel arabori
 */
public class DataItemQueryTests extends ApiTest
{
    private static final int OK = 200;

    private static final int CONFLICT = 409;

    private DataItemActions dataItemActions;

    @BeforeAll
    public void before()
    {
        dataItemActions = new DataItemActions();

        login();
    }

    @Test
    public void testGetAllDataItemsUsingDefaultPagination()
    {
        // When
        final ApiResponse response = dataItemActions.get();

        // Then
        response.validate().statusCode( is( OK ) );
        response.validate().body( "pager", isA( Object.class ) );
        response.validate().body( "dataItems", is( not( empty() ) ) );
        response.validate().body( "dataItems.dimensionItemType",
            (anyOf( hasItem( "PROGRAM_INDICATOR" ), hasItem( "DATA_ELEMENT" ) )) );
    }

    @Test
    public void testGetAllDataItemsWithoutPagination()
    {
        // Given
        final String noPagination = "?paging=false";

        // When
        final ApiResponse response = dataItemActions.get( noPagination );

        // Then
        response.validate().statusCode( is( OK ) );
        response.validate().body( "pager", is( nullValue() ) );
        response.validate().body( "dataItems", is( not( empty() ) ) );
        response.validate().body( "dataItems.dimensionItemType", hasItem( "PROGRAM_INDICATOR" ) );
        response.validate().body( "dataItems.dimensionItemType", hasItem( "DATA_ELEMENT" ) );
    }

    @Test
    public void testGetAllDataItemsUsingDefaultPaginationOrderedByCode()
    {
        // When
        final ApiResponse response = dataItemActions.get( "?order=name:asc" );

        // Then
        response.validate().statusCode( is( OK ) );
        response.validate().body( "pager", isA( Object.class ) );
        response.validate().body( "dataItems", is( not( empty() ) ) );
        response.validate().body( "dataItems.code", hasItem( "AAAAAAA-1234" ) );
    }

    @Test
    public void testFilterByDimensionTypeUsingDefaultPagination()
    {
        // Given
        final String theDimensionType = "PROGRAM_INDICATOR";
        final String theUrlParams = "?filter=dimensionItemType:in:[%s]";

        // When
        final ApiResponse response = dataItemActions.get( format( theUrlParams, theDimensionType ) );

        // Then
        response.validate().statusCode( is( OK ) );
        response.validate().body( "pager", isA( Object.class ) );
        response.validate().body( "dataItems", is( not( empty() ) ) );
        response.validate().body( "dataItems.dimensionItemType", everyItem( is( theDimensionType ) ) );
    }

    @Test
    public void testFilterUsingInvalidDimensionTypeUsingDefaultPagination()
    {
        // Given
        final String anyInvalidDimensionType = "INVALID_TYPE";
        final String theUrlParams = "?filter=dimensionItemType:in:[%s]";

        // When
        final ApiResponse response = dataItemActions.get( format( theUrlParams, anyInvalidDimensionType ) );

        // Then
        response.validate().statusCode( is( CONFLICT ) );
        response.validate().body( "pager", is( nullValue() ) );
        response.validate().body( "httpStatus", is( "Conflict" ) );
        response.validate().body( "httpStatusCode", is( CONFLICT ) );
        response.validate().body( "status", is( "ERROR" ) );
        response.validate().body( "errorCode", is( "E2016" ) );
        response.validate().body( "message", containsString(
            "Unable to parse element `" + anyInvalidDimensionType
                + "` on filter `dimensionItemType`. The values available are:" ) );
    }

    @Test
    public void testWhenDataIsNotFoundUsingDefaultPagination()
    {
        // Given
        final String theDimensionType = "PROGRAM_INDICATOR";
        final String aNonExistingName = "non-existing-Name";
        final String aValidFilteringAttribute = "name";
        final String theUrlParams = "?filter=dimensionItemType:in:[%s]&filter=" + aValidFilteringAttribute
            + ":ilike:%s";

        // When
        final ApiResponse response = dataItemActions
            .get( format( theUrlParams, theDimensionType, aNonExistingName ) );

        // Then
        response.validate().statusCode( is( OK ) );
        response.validate().body( "dataItems", is( empty() ) );
    }

    @Test
    public void testFilterByProgramUsingNonexistentAttributeAndDefaultPagination()
    {
        // Given
        final String theDimensionType = "PROGRAM_INDICATOR";
        final String theProgramId = Constants.EVENT_PROGRAM_ID;
        final String aNonExistingAttr = "nonExistingAttr";
        final String theUrlParams = "?filter=dimensionItemType:in:[%s]&filter=" + aNonExistingAttr
            + ":eq:%s&order=code:asc";

        // When
        final ApiResponse response = dataItemActions.get( format( theUrlParams, theDimensionType, theProgramId ) );

        // Then
        response.validate().statusCode( is( CONFLICT ) );
        response.validate().body( "pager", is( nullValue() ) );
        response.validate().body( "httpStatus", is( "Conflict" ) );
        response.validate().body( "httpStatusCode", is( CONFLICT ) );
        response.validate().body( "status", is( "ERROR" ) );
        response.validate().body( "errorCode", is( "E2017" ) );
        response.validate().body( "message", containsString( "Filter not supported: `" + aNonExistingAttr + "`" ) );
    }

    @Test
    public void testWhenFilteringByNonExistingNameWithoutPagination()
    {
        // Given
        final String theDimensionType = "PROGRAM_INDICATOR";
        final String aNonExistingName = "non-existing-name";
        final String theUrlParams = "?filter=dimensionItemType:in:[%s]&filter=name:ilike:%s&paging=false";

        // When
        final ApiResponse response = dataItemActions
            .get( format( theUrlParams, theDimensionType, aNonExistingName ) );

        // Then
        response.validate().statusCode( is( OK ) );
        response.validate().body( "pager", is( nullValue() ) );
        response.validate().body( "dataItems", is( empty() ) );
    }

    private void login()
    {
        new LoginActions().loginAsSuperUser();
    }
}
