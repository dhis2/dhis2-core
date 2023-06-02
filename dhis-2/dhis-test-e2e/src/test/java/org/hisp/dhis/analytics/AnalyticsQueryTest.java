/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.analytics;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;

import java.util.List;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for "/analytics" endpoint.
 *
 * @author dusan bernat
 */
public class AnalyticsQueryTest extends AnalyticsApiTest
{
    private RestApiActions analyticsActions;

    @BeforeAll
    public void setup()
    {
        analyticsActions = new RestApiActions( "analytics" );
    }

    @Test
    public void testAnalyticsGetWithTextDataElementAggregationTypeNone()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=dx:M3xtLkYBlKI.fyjPqlHE7Dn,pe:202107" )
            .add( "filter=ou:USER_ORGUNIT" )
            .add( "displayProperty=NAME" )
            .add( "desc=lastupdated" )
            .add( "skipMeta=true" )
            .add( "skipData=false" );

        // When
        ApiResponse response = analyticsActions.get( params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "rows", hasSize( equalTo( 2 ) ) );

        validateRow( response, 0,
            List.of( "M3xtLkYBlKI.fyjPqlHE7Dn", "202107", "" ) );

        validateRow( response, 1,
            List.of( "M3xtLkYBlKI.fyjPqlHE7Dn", "202107", "Some insecticide resistance" ) );
    }

    @Test
    public void testAnalyticsGetWithLongTextDataElementAggregationTypeSum()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=dx:mxc1T932aWM,pe:202210" )
            .add( "filter=ou:USER_ORGUNIT" )
            .add( "displayProperty=NAME" )
            .add( "desc=lastupdated" )
            .add( "skipMeta=true" )
            .add( "skipData=false" );

        // When
        ApiResponse response = analyticsActions.get( params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "rows", hasSize( equalTo( 1 ) ) );

        validateRow( response, 0,
            List.of( "mxc1T932aWM",
                "202210",
                "Cholera is an infection of the small intestine caused by the bacterium Vibrio cholerae.\n\nThe main symptoms are watery diarrhea and vomiting. This may result in dehydration and in severe cases grayish-bluish skin.[1] Transmission occurs primarily by drinking water or eating food that has been contaminated by the feces (waste product) of an infected person, including one with no apparent symptoms.\n\nThe severity of the diarrhea and vomiting can lead to rapid dehydration and electrolyte imbalance, and death in some cases. The primary treatment is oral rehydration therapy, typically with oral rehydration solution, to replace water and electrolytes. If this is not tolerated or does not provide improvement fast enough, intravenous fluids can also be used. Antibacterial drugs are beneficial in those with severe disease to shorten its duration and severity." ) );
    }
}