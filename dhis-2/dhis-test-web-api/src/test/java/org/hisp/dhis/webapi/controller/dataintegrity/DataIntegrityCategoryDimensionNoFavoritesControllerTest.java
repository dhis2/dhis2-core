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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataIntegrityCategoryDimensionNoFavoritesControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private final String check = "categories_dimensions_no_visualizations";

  private String categoryOptionRed;

  @Test
  void testCategoryDimensionsNoFavorites() {

    categoryOptionRed =
        assertStatus(
            HttpStatus.CREATED, POST("/categoryOptions", "{ 'name': 'Red', 'shortName': 'Red' }"));

    String categoryColor =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name': 'Color', 'shortName': 'Color', 'dataDimensionType': 'DISAGGREGATION' , 'dataDimension' : true,"
                    + "'categoryOptions' : [{'id' : '"
                    + categoryOptionRed
                    + "'} ] }"));

    /* Note that we have three categories now because of default */
    assertHasDataIntegrityIssues(
        "categories", check, 33, categoryColor, "Color", "DISAGGREGATION", true);
  }

  @Test
  void testCategoryNotDimensionNotFlagged() {

    assertHasNoDataIntegrityIssues("categories", check, true);
  }

  @BeforeEach
  void setupTest() {
    String categoryOptionSour =
        assertStatus(
            HttpStatus.CREATED,
            POST("/categoryOptions", "{ 'name': 'Sour', 'shortName': 'Sour' }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categories",
            "{ 'name': 'Taste', 'shortName': 'Taste', 'dataDimensionType': 'DISAGGREGATION' , 'dataDimension' : false,"
                + "'categoryOptions' : [{'id' : '"
                + categoryOptionSour
                + "'} ] }"));
  }
}
