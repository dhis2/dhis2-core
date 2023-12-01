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

import java.util.Set;
import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Tests metadata check for indicators with identical formulas.* {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/indicators/indicator_duplicated_terms.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityIndicatorsExactDuplicatesControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String check = "indicators_exact_duplicates";

  @Test
  void testIndicatorsExactDuplicatesExist() {
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
                    + " 'numerator' : 'abc123 + def456', 'numeratorDescription' : 'One', 'denominator' : '1', "
                    + "'denominatorDescription' : 'One'} }"));

    String indicatorB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators",
                "{ 'name': 'Indicator B', 'shortName': 'Indicator B',  'indicatorType' : {'id' : '"
                    + indicatorTypeA
                    + "'},"
                    + " 'numerator' : ' abc123 + def456 ', 'numeratorDescription' : 'One', 'denominator' : '1', "
                    + "'denominatorDescription' : 'One'} }"));

    assertHasDataIntegrityIssues(
        "indicators",
        check,
        100,
        Set.of(indicatorA, indicatorB),
        Set.of("Indicator A", "Indicator B"),
        Set.of(),
        true);
  }

  @Test
  void testIndicators() {
    String indicatorTypeA =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorTypes", "{ 'name': 'Per cent', 'factor' : 100, 'number' : false }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/indicators",
            "{ 'name': 'Indicator A', 'shortName': 'Indicator A',  'indicatorType' : {'id' : '"
                + indicatorTypeA
                + "'},"
                + " 'numerator' : 'abc123 + def456', 'numeratorDescription' : 'One', 'denominator' : '1', "
                + "'denominatorDescription' : 'One'} }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/indicators",
            "{ 'name': 'Indicator B', 'shortName': 'Indicator B',  'indicatorType' : {'id' : '"
                + indicatorTypeA
                + "'},"
                + " 'numerator' : 'abc123 + def456', 'numeratorDescription' : 'One', 'denominator' : 'xyz675', "
                + "'denominatorDescription' : 'One'} }"));

    assertHasNoDataIntegrityIssues("indicators", check, true);
  }

  @Test
  void testDuplicatedIndicatorFactorsRuns() {
    assertHasNoDataIntegrityIssues("indicators", check, false);
  }
}
