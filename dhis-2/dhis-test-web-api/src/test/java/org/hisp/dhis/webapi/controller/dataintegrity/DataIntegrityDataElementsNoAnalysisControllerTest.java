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
 * Test for aggregate data elements which do not have any analysis. The condition we test for here
 * is whether the data element is part of an indicator, which is also used in a favorite. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/data_elements/aggregate_no_analysis.yaml
 * }
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDataElementsNoAnalysisControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private static final String check = "data_elements_aggregate_no_analysis";

  private static final String detailsIdType = "dataElements";

  @Test
  void testDataElementNoAnalysis() {

    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    assertHasDataIntegrityIssues(detailsIdType, check, 100, dataElementA, "ANC1", null, true);
  }

  @Test
  void testDataElementHasAnalysis() {

    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    String indicatorTypeA =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorTypes", "{ 'name': 'Per cent', 'factor' : 100, 'number' : false }"));

    String indicatorA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators",
                "{ 'name': 'Indicator A', 'shortName': 'Indicator A',  'indicatorType' : {'id' : '"
                    + indicatorTypeA
                    + "'},"
                    + " 'numerator' : ' "
                    + dataElementA
                    + "', 'numeratorDescription' : 'ANC1', 'denominator' : '1', "
                    + "'denominatorDescription' : '1'} }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/visualizations?skipTranslations=true&skipSharing=true",
            "{'type':'SINGLE_VALUE','columns':[{'dimension':'dx','items':[{'id':'"
                + indicatorA
                + "'}]}],'rows':[],'filters':[{'dimension':'ou','items':[{'id':'USER_ORGUNIT'}]}, "
                + "{'dimension':'pe','items':[{'id':'LAST_12_MONTHS'}]}],'axes':[],'colorSet':'DEFAULT', "
                + "'cumulativeValues':false,'hideEmptyRowItems':'NONE','seriesKey':{},'legend':{},"
                + "'noSpaceBetweenColumns':false,'percentStackedValues':false,'regressionType':'NONE', "
                + "'showData':true,'aggregationType':'DEFAULT','completedOnly':false,'hideSubtitle':false,"
                + "'hideTitle':false,'sortOrder':0,'series':[],'fontStyle':{},'outlierAnalysis':null,'colTotals':false,"
                + "'colSubTotals':false,'rowTotals':false,'rowSubTotals':false,'showDimensionLabels':false,"
                + "'hideEmptyColumns':false,'hideEmptyRows':false,'skipRounding':false,'numberType':'VALUE', "
                + "'showHierarchy':false,'displayDensity':'NORMAL','fontSize':'NORMAL','digitGroupSeparator':'SPACE',"
                + "'fixColumnHeaders':false,'fixRowHeaders':false,'regression':false,'cumulative':false,'topLimit':0,'"
                + ""
                + "reportingParams':{'organisationUnit':false,'reportingPeriod':false,'parentOrganisationUnit':false,"
                + "'grandParentOrganisationUnit':false},'name':'Test viz'}"));

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testDataElementHasAnalysisRuns() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }
}
