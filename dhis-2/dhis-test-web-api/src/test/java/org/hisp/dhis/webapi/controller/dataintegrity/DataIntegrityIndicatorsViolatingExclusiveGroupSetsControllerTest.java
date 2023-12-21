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
 * Test for metadata check which identifies indicators which are members of multiple groups within a
 * group set. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/indicators/indicators_violating_exclusive_group_sets.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityIndicatorsViolatingExclusiveGroupSetsControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private final String check = "indicators_violating_exclusive_group_sets";

  private String indicatorA;

  private String indicatorB;

  private String indicatorGroupA;

  private String indicatorGroupB;

  @Test
  void testIndicatorsViolateGroupSets() {

    setupIndicators();

    indicatorGroupA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups",
                // language=JSON
                """
                            {
                            "name": "Group A",
                             "shortName" : "Group A",
                            "indicators": [
                                {
                                "id": "%s"
                                },
                                {
                                "id": "%s"
                                }
                            ]
                            }
                            """
                    .formatted(indicatorA, indicatorB)));

    indicatorGroupB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups",
                // language=JSON
                """
                            {
                            "name": "Group B",
                             "shortName" : "Group B",
                            "indicators": [
                                {
                                "id": "%s"
                                }
                            ]
                            }
                            """
                    .formatted(indicatorB)));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/indicatorGroupSets",
            // language=JSON
            """
                    {
                    "name": "Indicator Group Set",
                    "shortName": "Indicator Group Set",
                    "indicatorGroups": [
                        {
                        "id": "%s"
                        },
                        {
                        "id": "%s"
                        }
                    ]
                    }
                    """
                .formatted(indicatorGroupA, indicatorGroupB)));

    assertHasDataIntegrityIssues("indicators", check, 50, indicatorB, "Indicator B", null, true);
  }

  @Test
  void testIndicatorsInGroupSets() {

    setupIndicators();

    indicatorGroupA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups",
                // language=JSON
                """
                            {
                            "name": "Group A",
                             "shortName" : "Group A",
                            "indicators": [
                                {
                                "id": "%s"
                                }
                            ]
                            }
                            """
                    .formatted(indicatorA)));

    indicatorGroupB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups",
                // language=JSON
                """
                            {
                            "name": "Group B",
                             "shortName" : "Group B",
                            "indicators": [
                                {
                                "id": "%s"
                                }
                            ]
                            }
                            """
                    .formatted(indicatorB)));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/indicatorGroupSets",
            // language=JSON
            """
                    {
                    "name": "Indicator Group Set",
                    "shortName": "Indicator Group Set",
                    "indicatorGroups": [
                        {
                        "id": "%s"
                        },
                        {
                        "id": "%s"
                        }
                    ]
                    }
                    """
                .formatted(indicatorGroupA, indicatorGroupB)));

    assertHasNoDataIntegrityIssues("indicators", check, true);
  }

  @Test
  void testIndicatorInGroupSetsDivideByZero() {

    assertHasNoDataIntegrityIssues("indicators", check, false);
  }

  void setupIndicators() {
    String indicatorTypeA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorTypes",
                """
                        {
                          "name": "Per cent",
                          "factor": 100,
                          "number": false
                        }
                        """
                    .formatted()));

    indicatorA = createSimpleIndicator("Indicator A", indicatorTypeA);
    indicatorB = createSimpleIndicator("Indicator B", indicatorTypeA);
  }
}
