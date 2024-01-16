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
 * Test metadata check for indicator group sets which are composed of less than two indicator
 * groups. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/indicators/group_size_indicator_group_sets.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityGroupSizeIndicatorGroupSetsControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String check = "indicator_group_sets_scarce";

  private static final String detailsIdType = "indicatorGroupSets";

  private String indicatorGroupA;

  @Test
  void testIndicatorGroupSetSizeTooSmall() {

    setUpTest();
    String indicatorGroupSetA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroupSets",
                "{ 'name' : 'IGS1', 'shortName' : 'IGS1', 'indicatorGroups' : [{'id' : '"
                    + indicatorGroupA
                    + "'}]}"));

    String indicatorGroupSetB =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorGroupSets", "{ 'name' : 'IGS2', 'shortName' : 'IGS2' }"));

    assertHasDataIntegrityIssues(
        detailsIdType,
        check,
        66,
        Set.of(indicatorGroupSetA, indicatorGroupSetB),
        Set.of("IGS1", "IGS2"),
        Set.of("0", "1"),
        true);
  }

  @Test
  void testIndicatorGroupSetSizeOK() {

    setUpTest();

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testIndicatorsInGroupsRuns() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }

  void setUpTest() {

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
                    + " 'numerator' : 'abc123', 'numeratorDescription' : 'One', 'denominator' : 'abc123', "
                    + "'denominatorDescription' : 'Zero'} }"));

    String indicatorB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators",
                "{ 'name': 'Indicator B', 'shortName': 'Indicator B', 'indicatorType' : {'id' : '"
                    + indicatorTypeA
                    + "'},"
                    + " 'numerator' : 'abc123', 'numeratorDescription' : 'One', 'denominator' : 'abc123', "
                    + "'denominatorDescription' : 'Zero'}"));

    indicatorGroupA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups",
                "{ 'name' : 'An indicator group', 'shortName' : 'An indicator group', 'indicators' : [{'id' : '"
                    + indicatorA
                    + "'}]}"));

    String indicatorGroupB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups",
                "{ 'name' : 'Another indicator group', 'shortName' : 'Another indicator group', 'indicators' : [{'id' : '"
                    + indicatorB
                    + "'}]}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/indicatorGroupSets",
            "{ 'name' : 'An indicator group set', 'shortName' : 'An indicator group set', 'indicatorGroups' : [{'id' : '"
                + indicatorGroupA
                + "'}, "
                + "{'id' : '"
                + indicatorGroupB
                + "'}]}"));
  }
}
