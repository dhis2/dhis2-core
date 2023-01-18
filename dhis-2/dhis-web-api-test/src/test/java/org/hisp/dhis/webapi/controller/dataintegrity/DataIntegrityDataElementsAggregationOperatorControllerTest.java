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

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Generally, non-numeric data elements should have their aggregation type set
 * to NONE, while numeric data elements should have their aggregation type set
 * to something other than NONE.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/data_elements/aggregate_des_inconsistent_agg_operator.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDataElementsAggregationOperatorControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private final String check = "data_elements_aggregate_aggregation_operator";

    private final String detailsIdType = "dataElements";

    private String dataElementB;

    @Test
    void testDataElementInconsistentAggregation()
    {
        setUpDataElements();

        dataElementB = assertStatus( HttpStatus.CREATED,
            POST( "/dataElements",
                "{ 'name': 'ANC3', 'shortName': 'ANC3', 'valueType' : 'TEXT'," +
                    "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }" ) );

        assertHasDataIntegrityIssues( detailsIdType, check, 33,
            dataElementB, "ANC3", null, true );

    }

    @Test
    void testDataElementsConsistentAggregation()
    {

        setUpDataElements();

        dataElementB = assertStatus( HttpStatus.CREATED,
            POST( "/dataElements",
                "{ 'name': 'ANC3', 'shortName': 'ANC3', 'valueType' : 'TEXT'," +
                    "'domainType' : 'AGGREGATE', 'aggregationType' : 'NONE'  }" ) );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testDataElementsAggregationDividedByZero()
    {

        assertHasNoDataIntegrityIssues( detailsIdType, check, false );

    }

    void setUpDataElements()
    {
        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER'," +
                    "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }" ) );

        dataElementB = assertStatus( HttpStatus.CREATED,
            POST( "/dataElements",
                "{ 'name': 'ANC2', 'shortName': 'ANC2', 'valueType' : 'NUMBER'," +
                    "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }" ) );
    }
}