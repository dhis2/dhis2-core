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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.json.domain.JsonError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests rule selection in {@link DataAnalysisController#performValidationRulesAnalysis}.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class ValidationRulesAnalysisControllerTest extends AbstractDataValueControllerTest {

  private static final String RULE_A = "VrAnalysisA";
  private static final String RULE_B = "VrAnalysisB";
  private static final String RULE_C = "VrAnalysisC";

  @BeforeEach
  void setUpRules() {
    // the value 13 violates A (< 10) and B (< 5) but not C (< 20)
    assertStatus(
        HttpStatus.OK,
        POST(
            "/metadata",
            """
            {"validationRules": [%s, %s, %s]}
            """
                .formatted(rule(RULE_A, 10), rule(RULE_B, 5), rule(RULE_C, 20))));
    addDataValue("2021-01", "13", null, false);
  }

  @Test
  void testAnalysis_SingleRule() {
    assertEquals(List.of(RULE_A), runAnalysis("'vr':['" + RULE_A + "']"));
  }

  @Test
  void testAnalysis_MultipleRules() {
    // A is violated too, so it must not show up when only B and C are selected
    assertEquals(List.of(RULE_B), runAnalysis("'vr':['" + RULE_B + "','" + RULE_C + "']"));
  }

  @Test
  void testAnalysis_NoRuleSelection_RunsAllRules() {
    assertEquals(List.of(RULE_A, RULE_B), runAnalysis(null));
  }

  @Test
  void testAnalysis_EmptyRuleList_RunsAllRules() {
    assertEquals(List.of(RULE_A, RULE_B), runAnalysis("'vr':[]"));
  }

  @Test
  void testAnalysis_Group() {
    String group =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/validationRuleGroups",
                "{'name':'G','shortName':'G','validationRules':[{'id':'" + RULE_B + "'}]}"));
    assertEquals(List.of(RULE_B), runAnalysis("'vrg':'" + group + "'"));
  }

  @Test
  void testAnalysis_UnknownRule() {
    JsonError error =
        postAnalysis("'vr':['" + RULE_A + "','VrAnalysisX']").error(HttpStatus.NOT_FOUND);
    assertTrue(error.getMessage().contains("VrAnalysisX"), error.getMessage());
  }

  @Test
  void testAnalysis_NullRule() {
    JsonError error = postAnalysis("'vr':['" + RULE_A + "',null]").error(HttpStatus.BAD_REQUEST);
    assertEquals("vr must not contain null entries", error.getMessage());
  }

  @Test
  void testAnalysis_UnknownGroup() {
    JsonError error = postAnalysis("'vrg':'VrgAnalysisX'").error(HttpStatus.NOT_FOUND);
    assertTrue(error.getMessage().contains("VrgAnalysisX"), error.getMessage());
  }

  @Test
  void testAnalysis_RulesAndGroup() {
    JsonError error =
        postAnalysis("'vr':['" + RULE_A + "'],'vrg':'VrgAnalysisX'").error(HttpStatus.BAD_REQUEST);
    assertEquals("Only one of vr and vrg can be specified", error.getMessage());
  }

  @Test
  void testAnalysis_TooManyRules() {
    String uids =
        IntStream.rangeClosed(1, 51)
            .mapToObj("'VrAnalys%03d'"::formatted)
            .collect(Collectors.joining(","));
    JsonError error = postAnalysis("'vr':[" + uids + "]").error(HttpStatus.BAD_REQUEST);
    assertEquals("At most 50 validation rules can be specified in vr", error.getMessage());
  }

  private List<String> runAnalysis(String ruleSelection) {
    return postAnalysis(ruleSelection).content(HttpStatus.OK).asList(JsonObject.class).stream()
        .map(result -> result.getString("validationRuleId").string())
        .sorted()
        .toList();
  }

  private HttpResponse postAnalysis(String ruleSelection) {
    String body =
        "{'ou':'%s','startDate':'2021-01-01','endDate':'2021-01-31'%s}"
            .formatted(orgUnitId, ruleSelection == null ? "" : "," + ruleSelection);
    return POST("/dataAnalysis/validationRules", body);
  }

  private String rule(String uid, int max) {
    return """
        {
          "id": "%s",
          "name": "Rule %s",
          "leftSide": {"expression": "#{%s}", "description": "value"},
          "rightSide": {"expression": "%d", "description": "max"},
          "operator": "less_than",
          "periodType": "Monthly"
        }
        """
        .formatted(uid, uid, dataElementId, max);
  }
}
