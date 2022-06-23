package org.hisp.dhis.analytics.event;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.analytics.event.EventValidationHelper.validateHeader;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for Events "/query" endpoint.
 * 
 * @author maikel arabori
 */
public class EventQueryTest extends AnalyticsApiTest
{

    private AnalyticsEventActions analyticsEventActions = new AnalyticsEventActions();

    @Test
    public void queryWithProgramAndProgramStageWhenTotalPagesIsFalse()
    {
        // Given
        final QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd" )
            .add( "stage=dBwrot7S420" )
            .add( "displayProperty=NAME" )
            .add( "totalPages=false" )
            .add( "outputType=EVENT" );

        // When
        final ApiResponse response = analyticsEventActions.query().get( "/lxAQ7Zs9VYR.json", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 15 ) ) )
            .body( "rows", hasSize( equalTo( 3 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 50 ) )
            .body( "metaData.pager.isLastPage", is( true ) )
            .body( "metaData.pager", not( hasKey( "total" ) ) )
            .body( "metaData.pager", not( hasKey( "pageCount" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.dBwrot7S420.name", equalTo( "Antenatal care visit" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.items.lxAQ7Zs9VYR.name", equalTo( "Antenatal care visit" ) )
            .body( "metaData.items.LAST_12_MONTHS.name", equalTo( "Last 12 months" ) )
            .body( "metaData.dimensions.pe", hasSize( equalTo( 0 ) ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) )
            .body( "height", equalTo( 3 ) )
            .body( "width", equalTo( 15 ) )
            .body( "headerWidth", equalTo( 15 ) );

        // Validate headers
        validateHeader( response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "eventdate", "Visit date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 3, "storedby", "Stored by", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 4, "createdbydisplayname", "Created by (display name)", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 5, "lastupdatedbydisplayname", "Last updated by (display name)", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 6, "lastupdated", "Last Updated", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 7, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 8, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 9, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 10, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 11, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "programstatus", "Program status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 14, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );
    }

    @Test
    public void queryWithProgramAndProgramStageWhenTotalPagesIsTrueByDefault()
    {
        // Given
        final QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd" )
            .add( "stage=dBwrot7S420" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" );

        // When
        final ApiResponse response = analyticsEventActions.query().get( "/lxAQ7Zs9VYR.json", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 15 ) ) )
            .body( "rows", hasSize( equalTo( 3 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 50 ) )
            .body( "metaData.pager.total", equalTo( 3 ) )
            .body( "metaData.pager.pageCount", equalTo( 1 ) )
            .body( "metaData.pager", not( hasKey( "isLastPage" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.dBwrot7S420.name", equalTo( "Antenatal care visit" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.items.lxAQ7Zs9VYR.name", equalTo( "Antenatal care visit" ) )
            .body( "metaData.items.LAST_12_MONTHS.name", equalTo( "Last 12 months" ) )
            .body( "metaData.dimensions.pe", hasSize( equalTo( 0 ) ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) )
            .body( "height", equalTo( 3 ) )
            .body( "width", equalTo( 15 ) )
            .body( "headerWidth", equalTo( 15 ) );

        // Validate headers
        validateHeader( response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "eventdate", "Visit date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 3, "storedby", "Stored by", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 4, "createdbydisplayname", "Created by (display name)", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 5, "lastupdatedbydisplayname", "Last updated by (display name)", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 6, "lastupdated", "Last Updated", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 7, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 8, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 9, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 10, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 11, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "programstatus", "Program status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 14, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );
    }
}
