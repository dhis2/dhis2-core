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

import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Test for data elements which belong to datasets of different period types.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/data_elements/aggregate_des_datasets_different_period_types.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDataElementsDifferentPeriodTypesControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private final String check = "data_elements_aggregate_with_different_period_types";

    private final String detailsIdType = "dataElements";

    private String dataElementA;

    private String defaultCatCombo;

    @Test
    void testDataElementsHaveDifferentPeriodTypes()
    {

        setUpTest();

        String datasetUID = generateUid();
        String datasetMetadata = "{ 'id':'" + datasetUID
            + "', 'name': 'Test Weekly', 'shortName': 'Test Weekly', 'periodType' : 'Weekly'," +
            "'categoryCombo' : {'id': '" + defaultCatCombo + "'}, " +
            "'dataSetElements' : [{'dataSet' : {'id':'" + datasetUID + "'}, 'id':'" + generateUid() +
            "', 'dataElement': {'id' : '" + dataElementA + "'}}]}";
        assertStatus( HttpStatus.CREATED,
            POST( "/dataSets", datasetMetadata ) );

        assertHasDataIntegrityIssues( detailsIdType, check, 100,
            dataElementA, "ANC1", "Test Weekly", true );

    }

    @Test
    void testDataElementHasSamePeriodType()
    {

        setUpTest();
        String datasetUID = generateUid();
        String datasetMetadata = "{ 'id':'" + datasetUID
            + "', 'name': 'Test Monthly 2', 'shortName': 'Test Monthly 2', 'periodType' : 'Monthly'," +
            "'categoryCombo' : {'id': '" + defaultCatCombo + "'}, " +
            "'dataSetElements' : [{'dataSet' : {'id':'" + datasetUID + "'}, 'id':'" + generateUid() +
            "', 'dataElement': {'id' : '" + dataElementA + "'}}]}";
        assertStatus( HttpStatus.CREATED,
            POST( "/dataSets", datasetMetadata ) );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testDataElementPeriodTypeCheckRuns()
    {
        assertHasNoDataIntegrityIssues( detailsIdType, check, false );
    }

    void setUpTest()
    {

        defaultCatCombo = getDefaultCatCombo();

        dataElementA = assertStatus( HttpStatus.CREATED,
            POST( "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER'," +
                    "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }" ) );

        String datasetUID = generateUid();
        String datasetMetadata = "{ 'id':'" + datasetUID
            + "', 'name': 'Test Monthly', 'shortName': 'Test Monthly', 'periodType' : 'Monthly'," +
            "'categoryCombo' : {'id': '" + defaultCatCombo + "'}, " +
            "'dataSetElements' : [{'dataSet' : {'id':'" + datasetUID + "'}, 'id':'" + generateUid() +
            "', 'dataElement': {'id' : '" + dataElementA + "'}}]}";

        assertStatus( HttpStatus.CREATED,
            POST( "/dataSets", datasetMetadata ) );
    }
}