package org.hisp.dhis.analytics;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.hisp.dhis.ReadOnlyApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;

/**
 * @author maikel arabori
 */
public class EventsTest extends ReadOnlyApiTest
{

    private AnalyticsEventActions analyticsEventActions;

    @BeforeAll
    public void beforeAll()
    {
        super.login();
        analyticsEventActions = new AnalyticsEventActions();
    }

    @Test
    public void lineListWithProgramAndProgramStageNoTotalPages()
    {
        // Given
        final QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd" )
            .add( "stage=dBwrot7S420" )
            .add( "displayProperty=NAME" )
            .add( "totalPages=false" )
            .add( "outputType=EVENT" );

        analyticsEventActions.query().get(
            "/lxAQ7Zs9VYR.json", ContentType.JSON.toString(), ContentType.JSON.toString(), params )
            .validate()
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
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) );

    }
}
