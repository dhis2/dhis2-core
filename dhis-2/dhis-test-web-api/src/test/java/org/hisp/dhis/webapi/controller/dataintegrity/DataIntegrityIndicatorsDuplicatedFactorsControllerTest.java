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
 * Tests for duplicated indicator types, namely those which have the same factor. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/indicators/indicator_duplicate_types.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityIndicatorsDuplicatedFactorsControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String check = "indicator_types_duplicated";

  private static final String detailsIdType = "indicatorTypes";

  @Test
  void testDuplicatedIndicatorFactorsExist() {
    String IndicatorA =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorTypes", "{ 'name': 'Per cent', 'factor' : 100, 'number' : false }"));

    String IndicatorB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorTypes",
                "{ 'name': 'Per one hundred', 'factor' : 100, 'number' : false }"));

    assertStatus(
        HttpStatus.CREATED,
        POST("/indicatorTypes", "{ 'name': 'Per thousand', 'factor' : 1000, 'number' : false }"));

    assertNamedMetadataObjectExists("indicatorTypes", "Per cent");
    assertNamedMetadataObjectExists("indicatorTypes", "Per one hundred");

    assertHasDataIntegrityIssues(
        detailsIdType, check, 66, Set.of(IndicatorA, IndicatorB), Set.of(), Set.of(), true);
  }

  @Test
  void testIndicatorFactorsUnique() {

    assertStatus(
        HttpStatus.CREATED,
        POST("/indicatorTypes", "{ 'name': 'Percent', 'factor' : 100, 'number' : false }"));

    assertStatus(
        HttpStatus.CREATED,
        POST("/indicatorTypes", "{ 'name': 'Per thousand', 'factor' : 1000, 'number' : false }"));

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testDuplicatedIndicatorFactorsRuns() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }
}
