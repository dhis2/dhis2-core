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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.dataintegrity.DataIntegrityCheckType;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityCheck;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link DataIntegrityController} API with focus API returning {@link
 * org.hisp.dhis.dataintegrity.DataIntegrityCheck} information.
 *
 * @author Jan Bernitt
 */
class DataIntegrityChecksControllerTest extends AbstractDataIntegrityControllerTest {
  @Test
  void testGetAvailableChecks() {
    JsonList<JsonDataIntegrityCheck> checks =
        GET("/dataIntegrity").content().asList(JsonDataIntegrityCheck.class);
    assertFalse(checks.isEmpty());
    assertCheckExists("categories_no_options", checks);
    assertCheckExists("categories_one_default_category", checks);
    assertCheckExists("categories_one_default_category_option", checks);
    assertCheckExists("categories_one_default_category_combo", checks);
    assertCheckExists("categories_one_default_category_option_combo", checks);
    assertCheckExists("categories_unique_category_combo", checks);
    for (DataIntegrityCheckType type : DataIntegrityCheckType.values()) {
      assertCheckExists(type.getName(), checks);
    }
  }

  @Test
  void testGetAvailableChecksNamesAreUnique() {
    JsonList<JsonDataIntegrityCheck> checks =
        GET("/dataIntegrity").content().asList(JsonDataIntegrityCheck.class);

    assertEquals(checks.size(), Set.copyOf(checks.toList(JsonDataIntegrityCheck::getName)).size());
  }

  @Test
  void testGetAvailableChecksCodesAreUnique() {
    JsonList<JsonDataIntegrityCheck> checks =
        GET("/dataIntegrity").content().asList(JsonDataIntegrityCheck.class);

    assertEquals(checks.size(), Set.copyOf(checks.toList(JsonDataIntegrityCheck::getCode)).size());
  }

  @Test
  void testGetAvailableChecks_FilterUsingCode() {
    JsonList<JsonDataIntegrityCheck> checks =
        GET("/dataIntegrity?checks=CNO").content().asList(JsonDataIntegrityCheck.class);
    assertEquals(1, checks.size());
    assertEquals("categories_no_options", checks.get(0).getName());
  }

  @Test
  void testGetAvailableChecks_FilterUsingChecksPatterns() {
    JsonList<JsonDataIntegrityCheck> checks =
        GET("/dataIntegrity?checks=program*").content().asList(JsonDataIntegrityCheck.class);
    assertTrue(checks.size() > 0, "there should be matches");
    checks.forEach(check -> assertTrue(check.getName().toLowerCase().startsWith("program")));
  }

  @Test
  void testGetAvailableChecks_FilterUsingSection() {
    JsonList<JsonDataIntegrityCheck> checks =
        GET("/dataIntegrity?section=Program Rules").content().asList(JsonDataIntegrityCheck.class);
    assertTrue(checks.size() > 0, "there should be matches");
    checks.forEach(check -> assertEquals("Program Rules", check.getSection()));
  }

  /**
   * The point of this test is to check if the known i18n texts provided are resolved and assigned
   * to the correct field
   */
  @Test
  void testGetAvailableChecks_i18n() {
    JsonList<JsonDataIntegrityCheck> checks =
        GET("/dataIntegrity?checks=program_rule_variables_without_attribute")
            .content()
            .asList(JsonDataIntegrityCheck.class);
    assertEquals(1, checks.size());
    JsonDataIntegrityCheck check = checks.get(0);
    assertEquals("program_rule_variables_without_attribute", check.getName());
    assertEquals("Program rule variables lacking an attribute", check.getDisplayName());
    assertEquals("Program Rules", check.getSection());
    assertEquals(
        "Lists all programs with rule variables requiring an attribute source but that is not yet linked to an attribute",
        check.getDescription());
    assertEquals(
        "Assign an attribute to the variable in question or consider if the variable is not needed",
        check.getRecommendation());
  }

  private void assertCheckExists(String name, JsonList<JsonDataIntegrityCheck> checks) {
    assertTrue(checks.stream().anyMatch(check -> check.getName().equals(name)));
  }
}
