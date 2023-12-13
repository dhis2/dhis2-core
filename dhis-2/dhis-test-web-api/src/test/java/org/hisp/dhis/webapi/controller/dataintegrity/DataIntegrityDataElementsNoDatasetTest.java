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
 * Test for aggregate data elements which are not part of any data dataset. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/data_elements/aggregate_des_no_datasets.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDataElementsNoDatasetControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private static final String check = "aggregate_des_no_datasets";

  private static final String detailsIdType = "dataElements";

  @Test
  void testDataElementNotInDataSet() {

    setupDataset();

    String dataElementB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC2', 'shortName': 'ANC2', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    assertHasDataIntegrityIssues(detailsIdType, check, 50, dataElementB, "ANC2", null, true);
  }

  @Test
  void testDataElementInDataSet() {
    setupDataset();

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testDataElementsinDataSetDivideByZero() {

    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }

  void setupDataset() {
    String defaultCatCombo = getDefaultCatCombo();
    String dataSetA = generateUid();

    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    String datasetMetadata =
        "{ 'id':'"
            + dataSetA
            + "', 'name': 'Test Monthly', 'shortName': 'Test Monthly', 'periodType' : 'Monthly',"
            + "'categoryCombo' : {'id': '"
            + defaultCatCombo
            + "'}, "
            + "'dataSetElements' : [{'dataSet' : {'id':'"
            + dataSetA
            + "'}, 'id':'"
            + generateUid()
            + "', 'dataElement': {'id' : '"
            + dataElementA
            + "'}}]}";

    assertStatus(HttpStatus.CREATED, POST("/dataSets", datasetMetadata));
  }
}
