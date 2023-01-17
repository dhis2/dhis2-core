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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonIndicator;
import org.hisp.dhis.webapi.json.domain.JsonIndicatorGroup;
import org.junit.jupiter.api.Test;

/**
 * Test metadata check for indicators which are not part of an indicator group
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/indicators/indicator_nongrouped.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityIndicatorsNotGroupedControllerTest extends AbstractDataIntegrityIntegrationTest
{

    private static final String check = "indicators_not_grouped";

    private String indicatorA;

    private String indicatorB;

    @Test
    void testIndicatorsWithoutGroupsExist()
    {

        setUpTest();
        assertStatus( HttpStatus.CREATED,
            POST( "/indicatorGroups",
                "{ 'name' : 'An indicator group', 'indicators' : [{'id' : '" + indicatorA + "'}]}" ) );

        JsonObject content = GET( "/indicatorGroups?fields=id,name,indicators[id,name]" ).content();
        JsonList<JsonIndicatorGroup> myIndicatorGroup = content.getList( "indicatorGroups", JsonIndicatorGroup.class );
        assertEquals( 1, myIndicatorGroup.size() );
        JsonList<JsonIndicator> myIndicators = myIndicatorGroup.get( 0 ).getIndicators();
        assertEquals( 1, myIndicators.size() );
        assertEquals( indicatorA, myIndicators.get( 0 ).getId() );

        assertHasDataIntegrityIssues( "indicators", check, 50, indicatorB,
            null, null, true );
    }

    @Test
    void testIndicatorsInGroups()
    {

        setUpTest();
        assertStatus( HttpStatus.CREATED,
            POST( "/indicatorGroups",
                "{ 'name' : 'An indicator group', 'indicators' : [{'id' : '" + indicatorA + "'}, " +
                    " {'id' : '" + indicatorB + "'}]}" ) );

        assertHasNoDataIntegrityIssues( "indicators", check, true );
    }

    @Test
    void testIndicatorsInGroupsRuns()
    {
        assertHasNoDataIntegrityIssues( "indicators", check, false );
    }

    void setUpTest()
    {

        String indicatorTypeA = assertStatus( HttpStatus.CREATED,
            POST( "/indicatorTypes",
                "{ 'name': 'Per cent', 'factor' : 100, 'number' : false }" ) );

        indicatorA = assertStatus( HttpStatus.CREATED,
            POST( "/indicators",
                "{ 'name': 'Indicator A', 'shortName': 'Indicator A',  'indicatorType' : {'id' : '" + indicatorTypeA
                    + "'}," +
                    " 'numerator' : 'abc123', 'numeratorDescription' : 'One', 'denominator' : 'abc123', " +
                    "'denominatorDescription' : 'Zero'} }" ) );

        indicatorB = assertStatus( HttpStatus.CREATED,
            POST( "/indicators",
                "{ 'name': 'Indicator B', 'shortName': 'Indicator B', 'indicatorType' : {'id' : '" + indicatorTypeA
                    + "'}," +
                    " 'numerator' : 'abc123', 'numeratorDescription' : 'One', 'denominator' : 'abc123', " +
                    "'denominatorDescription' : 'Zero'}" ) );

    }

}
