/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.http.HttpClientAdapter;
import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests metadata integrity check which identifies organisation units assigned to the default
 * category option {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/categories/default_category_option_organisation_units.yaml}
 */
class DataIntegrityCategoryOptionDefaultOrgUnitsControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  @Autowired private CategoryService categoryService;

  private final String check = "category_options_default_has_organisation_units";

  private final String detailsIdType = "categoryOptions";

  @Test
  void testDefaultCategoryOptionHasNoOrgUnits() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testNonDefaultCategoryOptionWithOrgUnitsIsIgnored() {
    String orgUnitId = createOrgUnit("Fish District", "FD");
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryOptions",
            "{ 'name': 'Red', 'shortName': 'Red', 'organisationUnits' : [{'id' : '"
                + orgUnitId
                + "'}]}"));

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testDefaultCategoryOptionHasOrgUnits() {
    String orgUnitA = createOrgUnit("Fish District", "FD");
    String orgUnitB = createOrgUnit("Pizza District", "PD");

    String defaultOptionId = categoryService.getDefaultCategoryOption().getUid();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/categoryOptions/{id}/organisationUnits",
            defaultOptionId,
            HttpClientAdapter.Body(
                "{'additions':[{'id':'" + orgUnitA + "'},{'id':'" + orgUnitB + "'}]}")));

    assertHasDataIntegrityIssues(
        detailsIdType,
        check,
        100,
        defaultOptionId,
        "default",
        "Assigned to 2 organisation unit(s)",
        true);
  }

  private String createOrgUnit(String name, String shortName) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{ 'name': '"
                + name
                + "', 'shortName': '"
                + shortName
                + "', 'openingDate' : '2022-01-01'}"));
  }
}
