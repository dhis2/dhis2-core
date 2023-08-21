/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.webapi.controller.AbstractGistControllerTest.assertHasNoPager;
import static org.hisp.dhis.webapi.controller.AbstractGistControllerTest.assertHasPager;
import static org.hisp.dhis.webapi.controller.AbstractGistControllerTest.assertHasPagerLinks;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.dimension.DimensionController} using (mocked)
 * REST requests.
 *
 * @author Jan Bernitt
 */
class DimensionControllerTest extends DhisControllerConvenienceTest {

  @Autowired private CategoryService categoryService;
  @Autowired private OrganisationUnitGroupService orgUnitGroupService;

  @Test
  void testGetDimensionsForDataSet() {
    CategoryCombo categoryCombo = createCategoryCombo(0);
    String dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName':'MDS', 'periodType':'Monthly', 'categoryCombo':{'id':'"
                    + categoryCombo.getUid()
                    + "'}}"));
    JsonObject response = GET("/dimensions/dataSet/{ds}", dsId).content();
    JsonArray dimensions = response.getArray("dimensions");
    assertEquals(1, dimensions.size());
    JsonObject gender = dimensions.getObject(0);
    assertEquals("Gender0", gender.getString("name").string());
  }

  @Test
  void testGetDimensions() {
    addCategoryCombos(1);
    JsonObject response = GET("/dimensions").content();
    JsonArray dimensions = response.getArray("dimensions");
    assertEquals(1, dimensions.size());
    assertHasPager(response, 1, 50);
  }

  @Test
  void testGetDimensionsWithDefaultPaging() {
    addCategoryCombos(55);
    JsonObject response = GET("/dimensions").content();
    assertHasPager(response, 1, 50, 55);
    assertHasPagerLinks(response, 1);
  }

  @Test
  void testGetDimensionsPage2() {
    addCategoryCombos(105);
    JsonObject response = GET("/dimensions?page=2").content();
    assertHasPager(response, 2, 50, 105);
    assertHasPagerLinks(response, 2);
  }

  @Test
  void testGetDimensionsWithCustomPageSize() {
    addCategoryCombos(105);
    JsonObject response = GET("/dimensions?pageSize=23").content();
    JsonArray dimensions = response.getArray("dimensions");

    assertEquals(23, dimensions.size());
    assertHasPager(response, 1, 23, 105);
    assertHasPagerLinks(response, 1);
  }

  @Test
  void testGetDimensionsWithMultipleQueryParams() {
    addCategoryCombos(105);
    JsonObject response = GET("/dimensions?pageSize=44&fields=id&page=3").content();
    JsonArray dimensions = response.getArray("dimensions");

    JsonObject dimension = dimensions.getObject(0);
    String displayName = dimension.getString("displayName").string();
    String id = dimension.getString("id").string();

    assertNull(displayName);
    assertNotNull(id);
    assertEquals(17, dimensions.size());
    assertHasPager(response, 3, 44, 105);
    assertHasPagerLinks(response, 3);
  }

  @Test
  void testGetDimensionsFilteredByType() {
    addCategoryCombos(105);
    addOrganisationGroupSet(5);
    JsonObject response =
        GET("/dimensions?fields=id,dimensionType&filter=dimensionType:eq:ORGANISATION_UNIT_GROUP_SET")
            .content();
    JsonArray dimensions = response.getArray("dimensions");

    assertNotNull(dimensions);
    assertEquals(5, dimensions.size());

    JsonObject dimension = dimensions.getObject(0);
    String dimensionType = dimension.getString("dimensionType").string();
    assertEquals("ORGANISATION_UNIT_GROUP_SET", dimensionType);

    assertHasPager(response, 1, 50, 5);
    assertHasPagerLinks(response, 1);
  }

  @Test
  void testGetDimensionsWithNoPaging() {
    addCategoryCombos(55);
    JsonObject response = GET("/dimensions?paging=false").content();
    JsonArray dimensions = response.getArray("dimensions");

    assertEquals(55, dimensions.size());
    assertHasNoPager(response);
  }

  @Test
  void testGetFilteredDimensionsWithDisplayNameOnly() {
    addCategoryCombos(55);
    JsonObject response = GET("/dimensions?fields=displayName").content();
    JsonArray dimensions = response.getArray("dimensions");
    JsonObject dimension = dimensions.getObject(0);
    String displayName = dimension.getString("displayName").string();
    String id = dimension.getString("id").string();

    assertNotNull(displayName);
    assertNull(id);
  }

  @Test
  void testGetDimensionsWhenNone() {
    JsonObject response = GET("/dimensions").content();
    assertHasPager(response, 1, 50, 0);
    assertHasPagerLinks(response, 1);
  }

  private void addCategoryCombos(int counter) {
    for (int i = 0; i < counter; i++) {
      createCategoryCombo(i);
    }
  }

  private void addOrganisationGroupSet(int counter) {
    for (int i = 0; i < counter; i++) {
      createOrganisationUnitGroupSet(i);
    }
  }

  private OrganisationUnitGroupSet createOrganisationUnitGroupSet(int postfix) {
    OrganisationUnitGroupSet ogs = createOrganisationUnitGroupSet('a');
    ogs.setName(ogs.getName() + postfix);
    ogs.setShortName(ogs.getShortName() + postfix);
    ogs.setCode(ogs.getCode() + postfix);
    orgUnitGroupService.addOrganisationUnitGroupSet(ogs);
    return ogs;
  }

  private CategoryCombo createCategoryCombo(int postfix) {
    CategoryOption categoryOptionA = new CategoryOption("Male" + postfix);
    CategoryOption categoryOptionB = new CategoryOption("Female" + postfix);
    categoryService.addCategoryOption(categoryOptionA);
    categoryService.addCategoryOption(categoryOptionB);
    Category categoryA = new Category("Gender" + postfix, DataDimensionType.DISAGGREGATION);
    categoryA.setShortName(categoryA.getName());
    categoryA.addCategoryOption(categoryOptionA);
    categoryA.addCategoryOption(categoryOptionB);
    categoryService.addCategory(categoryA);
    CategoryCombo categoryComboA =
        new CategoryCombo("Gender" + postfix, DataDimensionType.DISAGGREGATION);
    categoryComboA.addCategory(categoryA);
    categoryService.addCategoryCombo(categoryComboA);
    return categoryComboA;
  }
}
