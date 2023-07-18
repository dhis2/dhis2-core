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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryComboStore;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.category.CategoryStore;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests the {@link DataIntegrityController} API with focus on checks for {@link Category}s and its
 * related objects.
 *
 * <p>This includes both the {@link org.hisp.dhis.dataintegrity.DataIntegritySummary} and the {@link
 * org.hisp.dhis.dataintegrity.DataIntegrityDetails} results.
 *
 * @author Jan Bernitt
 */
class DataIntegrityCategoriesControllerTest extends AbstractDataIntegrityControllerTest {
  @Autowired private CategoryService categoryService;

  @Autowired private CategoryStore categoryStore;

  @Autowired private CategoryOptionStore categoryOptionStore;

  @Autowired private CategoryComboStore categoryComboStore;

  @Autowired private CategoryOptionComboStore categoryOptionComboStore;

  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  void testSummaryCategories_no_options() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categories",
            "{'name': 'CatDog', 'shortName': 'CD', 'dataDimensionType': 'ATTRIBUTE'}"));

    postSummary("categories-no-options");
    JsonDataIntegritySummary summary = getSummary("categories-no-options");
    assertEquals(1, summary.getCount());
    assertEquals(50, summary.getPercentage().intValue());
  }

  @Test
  void testDetailsCategories_no_options() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{'name': 'CatDog', 'shortName': 'CD', 'dataDimensionType': 'ATTRIBUTE'}"));

    postDetails("categories-no-options");
    JsonDataIntegrityDetails details = getDetails("categories-no-options");

    assertEquals(1, details.getIssues().size());
    assertEquals(uid, details.getIssues().get(0).getId());
    assertEquals("CatDog", details.getIssues().get(0).getName());
  }

  /**
   * Ideally we would have created a second category named default, but we have made this impossible
   * by unique constraint. So the best we can do is changing the UID of the existing default to some
   * other UID.
   */
  @Test
  void testSummaryCategories_one_default_category() {
    String uid = CodeGenerator.generateUid();
    assertEquals(uid, updateDefaultCategoryToUid(uid));

    postSummary("categories_one_default_category");
    JsonDataIntegritySummary summary = getSummary("categories_one_default_category");

    assertEquals(1, summary.getCount());
    assertNull(summary.getPercentage());
  }

  /**
   * Ideally we would have created a second category named default, but we have made this impossible
   * by unique constraint. So the best we can do is changing the UID of the existing default to some
   * other UID.
   */
  @Test
  void testDetailsCategories_one_default_category() {
    String uid = CodeGenerator.generateUid();
    assertEquals(uid, updateDefaultCategoryToUid(uid));

    postDetails("categories_one_default_category");
    JsonDataIntegrityDetails details = getDetails("categories_one_default_category");

    assertEquals(1, details.getIssues().size());
    assertEquals(uid, details.getIssues().get(0).getId());
    assertEquals("default", details.getIssues().get(0).getName());
  }

  /**
   * Ideally we would have created a second category option named default, but we have made this
   * impossible by unique constraint. So the best we can do is changing the UID of the existing
   * default to some other UID.
   */
  @Test
  void testSummaryCategories_one_default_category_option() {
    String uid = CodeGenerator.generateUid();
    assertEquals(uid, updateDefaultCategoryOptionToUid(uid));

    postSummary("categories_one_default_category_option");
    JsonDataIntegritySummary summary = getSummary("categories_one_default_category_option");

    assertEquals(1, summary.getCount());
    assertNull(summary.getPercentage());
  }

  /**
   * Ideally we would have created a second category option named default, but we have made this
   * impossible by unique constraint. So the best we can do is changing the UID of the existing
   * default to some other UID.
   */
  @Test
  void testDetailsCategories_one_default_category_option() {
    String uid = CodeGenerator.generateUid();
    assertEquals(uid, updateDefaultCategoryOptionToUid(uid));

    postDetails("categories_one_default_category_option");
    JsonDataIntegrityDetails details = getDetails("categories_one_default_category_option");

    assertEquals(1, details.getIssues().size());
    assertEquals(uid, details.getIssues().get(0).getId());
    assertEquals("default", details.getIssues().get(0).getName());
  }

  /**
   * Ideally we would have created a second category combo named default, but we have made this
   * impossible by unique constraint. So the best we can do is changing the UID of the existing
   * default to some other UID.
   */
  @Test
  void testSummaryCategories_one_default_category_combo() {
    String uid = CodeGenerator.generateUid();
    assertEquals(uid, updateDefaultCategoryComboToUid(uid));

    postSummary("categories_one_default_category_combo");
    JsonDataIntegritySummary summary = getSummary("categories_one_default_category_combo");

    assertEquals(1, summary.getCount());
    assertNull(summary.getPercentage());
  }

  /**
   * Ideally we would have created a second category combo named default, but we have made this
   * impossible by unique constraint. So the best we can do is changing the UID of the existing
   * default to some other UID.
   */
  @Test
  void testDetailsCategories_one_default_category_combo() {
    String uid = CodeGenerator.generateUid();
    assertEquals(uid, updateDefaultCategoryComboToUid(uid));

    postDetails("categories_one_default_category_combo");
    JsonDataIntegrityDetails details = getDetails("categories_one_default_category_combo");

    assertEquals(1, details.getIssues().size());
    assertEquals(uid, details.getIssues().get(0).getId());
    assertEquals("default", details.getIssues().get(0).getName());
  }

  /**
   * Ideally we would have created a second category option combo named default, but we have made
   * this impossible by unique constraint. So the best we can do is changing the UID of the existing
   * default to some other UID.
   */
  @Test
  void testSummaryCategories_one_default_category_option_combo() {
    String uid = CodeGenerator.generateUid();
    assertEquals(uid, updateDefaultCategoryOptionComboToUid(uid));

    postSummary("categories_one_default_category_option_combo");
    JsonDataIntegritySummary summary = getSummary("categories_one_default_category_option_combo");

    assertEquals(1, summary.getCount());
    assertNull(summary.getPercentage());
  }

  /**
   * Ideally we would have created a second category option combo named default, but we have made
   * this impossible by unique constraint. So the best we can do is changing the UID of the existing
   * default to some other UID.
   */
  @Test
  void testDetailsCategories_one_default_category_option_combo() {
    String uid = CodeGenerator.generateUid();
    assertEquals(uid, updateDefaultCategoryOptionComboToUid(uid));

    postDetails("categories_one_default_category_option_combo");
    JsonDataIntegrityDetails details = getDetails("categories_one_default_category_option_combo");

    assertEquals(1, details.getIssues().size());
    assertEquals(uid, details.getIssues().get(0).getId());
    assertEquals("default", details.getIssues().get(0).getName());
  }

  private String updateDefaultCategoryToUid(String uid) {
    transactionTemplate.execute(
        status -> {
          Category category = categoryService.getDefaultCategory();
          category.setUid(uid);
          categoryStore.save(category);
          return null;
        });
    // OBS! we need to read this to force the TX to be applied
    return categoryService.getDefaultCategory().getUid();
  }

  private String updateDefaultCategoryOptionToUid(String uid) {
    transactionTemplate.execute(
        status -> {
          CategoryOption option = categoryService.getDefaultCategoryOption();
          option.setUid(uid);
          categoryOptionStore.save(option);
          return null;
        });
    // OBS! we need to read this to force the TX to be applied
    return categoryService.getDefaultCategoryOption().getUid();
  }

  private String updateDefaultCategoryComboToUid(String uid) {
    transactionTemplate.execute(
        status -> {
          CategoryCombo combo = categoryService.getDefaultCategoryCombo();
          combo.setUid(uid);
          categoryComboStore.save(combo);
          return null;
        });
    // OBS! we need to read this to force the TX to be applied
    return categoryService.getDefaultCategoryCombo().getUid();
  }

  private String updateDefaultCategoryOptionComboToUid(String uid) {
    transactionTemplate.execute(
        status -> {
          CategoryOptionCombo combo = categoryService.getDefaultCategoryOptionCombo();
          combo.setUid(uid);
          categoryOptionComboStore.save(combo);
          return null;
        });
    // OBS! we need to read this to force the TX to be applied
    return categoryService.getDefaultCategoryOptionCombo().getUid();
  }
}
