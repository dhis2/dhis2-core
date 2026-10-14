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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the metadata import, merge and JSON shape of the expressions ({@code Expression}) embedded
 * in validation rules.
 */
@Transactional
class ValidationRuleExpressionControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String RULE_UID = "ValRuleExp1";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata",
            """
            {"validationRules": [{
              "id": "%s",
              "name": "Expression rule",
              "periodType": "Monthly",
              "operator": "less_than_or_equal_to",
              "leftSide": {
                "expression": "1 + 1",
                "description": "Left",
                "missingValueStrategy": "NEVER_SKIP",
                "slidingWindow": true,
                "translations": [{"locale": "fr", "property": "DESCRIPTION", "value": "Gauche"}]
              },
              "rightSide": {"expression": "2 + 2", "description": "Right"}
            }]}"""
                .formatted(RULE_UID)));
  }

  @Test
  void testPutOfExportedRuleWithChangedLeftSideUpdatesTheExpression() {
    String exported = GET("/validationRules/" + RULE_UID).content(HttpStatus.OK).toJson();
    assertTrue(exported.contains("\"expression\":\"1 + 1\""), exported);

    assertStatus(
        HttpStatus.OK,
        PUT(
            "/validationRules/" + RULE_UID,
            exported.replace("\"expression\":\"1 + 1\"", "\"expression\":\"5 + 5\"")));

    JsonObject rule = reloadRule();
    JsonObject leftSide = rule.getObject("leftSide");
    assertEquals("5 + 5", leftSide.getString("expression").string());
    assertEquals("Left", leftSide.getString("description").string());
    assertEquals("NEVER_SKIP", leftSide.getString("missingValueStrategy").string());
    assertTrue(leftSide.getBoolean("slidingWindow").booleanValue());
    JsonObject translation = leftSide.getArray("translations").getObject(0);
    assertEquals("fr", translation.getString("locale").string());
    assertEquals("Gauche", translation.getString("value").string());
    assertEquals("2 + 2", rule.getObject("rightSide").getString("expression").string());
    assertEquals(2, jdbcTemplate.queryForObject("select count(*) from expression", Integer.class));
  }

  /** The keys of an embedded expression, as on play/dev. */
  @Test
  void testEmbeddedExpressionJsonKeys() {
    JsonObject leftSide = reloadRule().getObject("leftSide");

    assertEquals(
        Set.of(
            "expression",
            "description",
            "displayDescription",
            "missingValueStrategy",
            "slidingWindow",
            "translations"),
        Set.copyOf(leftSide.names()));
  }

  /**
   * The validation rule as exported after the session is flushed and cleared and the second-level
   * cache evicted, so it is read from the database.
   */
  private JsonObject reloadRule() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/validationRules/" + RULE_UID).content(HttpStatus.OK);
  }
}
