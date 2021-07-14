package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link PredictorController} using (mocked) REST requests.
 */
public class PredictorControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testGetExpressionDescription()
    {
        JsonWebMessage response = POST( "/predictors/expression/description", "0" )
            .content().as( JsonWebMessage.class );
        assertWebMessage( "OK", 200, "OK", "Valid", response );
        assertEquals( "0", response.getDescription() );
    }

    @Test
    public void testGetExpressionDescription_InvalidExpression()
    {
        JsonWebMessage response = POST( "/predictors/expression/description", "1 <> 1" )
            .content().as( JsonWebMessage.class );
        assertWebMessage( "OK", 200, "ERROR", "Expression is not well-formed", response );
        assertNull( response.getDescription() );
    }

    @Test
    public void testGetSkipTestDescription()
    {
        JsonWebMessage response = POST( "/predictors/skipTest/description", "1 != 1" )
            .content().as( JsonWebMessage.class );
        assertWebMessage( "OK", 200, "OK", "Valid", response );
        assertEquals( "1 != 1", response.getDescription() );
    }

    @Test
    public void testGetSkipTestDescription_InvalidExpression()
    {
        JsonWebMessage response = POST( "/predictors/skipTest/description", "1 <> 1" )
            .content().as( JsonWebMessage.class );
        assertWebMessage( "OK", 200, "ERROR", "Expression is not well-formed", response );
        assertNull( response.getDescription() );
    }

    @Test
    public void testRunPredictor()
    {
        String pId = postNewPredictor();

        assertWebMessage( "OK", 200, "OK", "Generated 0 predictions",
            POST( "/predictors/" + pId + "/run?startDate=2020-01-01&endDate=2021-01-01" ).content() );
    }

    @Test
    public void testRunPredictors()
    {
        assertWebMessage( "OK", 200, "OK", "Generated 0 predictions",
            POST( "/predictors/run?startDate=2020-01-01&endDate=2021-01-01" ).content() );
    }

    private String postNewPredictor()
    {
        String ccId = GET(
            "/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default" )
                .content().getObject( 0 ).getString( "id" ).string();

        String deId = assertStatus( HttpStatus.CREATED, POST( "/dataElements/",
            "{'name':'My data element', 'shortName':'DE1', 'code':'DE1', 'valueType':'INTEGER', " +
                "'aggregationType':'SUM', 'zeroIsSignificant':false, 'domainType':'AGGREGATE', " +
                "'categoryCombo': {'id': '" + ccId + "'}}" ) );

        return assertStatus( HttpStatus.CREATED,
            POST( "/predictors/",
                "{'name':'Pred1'," +
                    "'output': {'id':'" + deId + "'}, " +
                    "'generator': {'expression': '1 != 1'}," +
                    "'periodType': 'Monthly'," +
                    "'sequentialSampleCount':4," +
                    "'annualSampleCount':3," +
                    "'organisationUnitLevels': []" +
                    " }" ) );
    }
}
