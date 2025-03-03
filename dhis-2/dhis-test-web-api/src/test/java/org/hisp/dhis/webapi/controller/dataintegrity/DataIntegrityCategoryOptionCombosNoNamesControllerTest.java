/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.CREATED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests metadata integrity check category option combinations with no names.
 *
 * <p>{@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/categories/category_option_combos_no_name.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityCategoryOptionCombosNoNames extends AbstractDataIntegrityIntegrationTest {

  private final String check = "category_option_combos_no_names";

  private String categoryOptionRed;
  private String categoryOptionLarge;

  @Test
  void testCategoryOptionCombosHaveNames() {

    assertHasNoDataIntegrityIssues("categoryOptionCombos", check, true);
  }

  @BeforeEach
  void setupTest() {
    categoryOptionRed =
        assertStatus(CREATED, POST("/categoryOptions", "{ 'name': 'Red', 'shortName': 'Red' }"));

    categoryOptionLarge =
        assertStatus(
            CREATED, POST("/categoryOptions", "{ 'name': 'Large', 'shortName': 'Large' }"));

    String categoryColor =
        assertStatus(
            CREATED,
            POST(
                "/categories",
                "{ 'name': 'Color', 'shortName': 'Color', 'dataDimensionType': 'DISAGGREGATION' ,"
                    + "'categoryOptions' : [{'id' : '"
                    + categoryOptionRed
                    + "'} ] }"));

    String categorySize =
        assertStatus(
            CREATED,
            POST(
                "/categories",
                "{ 'name': 'Size', 'shortName': 'Size', 'dataDimensionType': 'DISAGGREGATION' ,"
                    + "'categoryOptions' : [{'id' : '"
                    + categoryOptionLarge
                    + "'} ] }"));

    POST(
            "/categoryCombos",
            "{ 'name' : 'Color', "
                + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                + "{'id' : '"
                + categoryColor
                + "'},"
                + "{'id' : '"
                + categorySize
                + "'}]} ")
        .content(CREATED);

    assertNamedMetadataObjectExists("categoryOptionCombos", "default");
    assertCocExists("Red, Large");
  }
}
