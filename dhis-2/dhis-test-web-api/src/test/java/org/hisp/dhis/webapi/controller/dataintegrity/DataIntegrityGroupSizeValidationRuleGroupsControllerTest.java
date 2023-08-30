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
 * Test metadata check for minimum validation rule group size. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/groups/group_size_validation_rule_groups.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityGroupSizeValidationRuleGroupsControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String check = "validation_rule_groups_scarce";

  private static final String detailsIdType = "validationRuleGroups";

  private String validationRuleA;

  @Test
  void testValidationRuleGroupsTooSmall() {

    setUpTest();
    String indicatorGroupA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/validationRuleGroups",
                "{ 'name' : 'One', 'shortName' : 'One', 'validationRules' : [{'id' : '"
                    + validationRuleA
                    + "'}]}"));

    String indicatorGroupB =
        assertStatus(
            HttpStatus.CREATED,
            POST("/validationRuleGroups", "{ 'name' : 'None', 'shortName' : 'None'} "));

    assertHasDataIntegrityIssues(
        detailsIdType,
        check,
        66,
        Set.of(indicatorGroupA, indicatorGroupB),
        Set.of("One", "None"),
        Set.of("0", "1"),
        true);
  }

  @Test
  void testValidationRuleGroupsSizeOK() {

    setUpTest();

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testIndicatorsInGroupsRuns() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }

  void setUpTest() {

    validationRuleA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/validationRules",
                "{'importance':'MEDIUM','operator':'not_equal_to','leftSide':{'missingValueStrategy':'NEVER_SKIP', "
                    + "'description':'Test','expression':'abc123'},"
                    + "'rightSide':{'missingValueStrategy': 'NEVER_SKIP', 'description':'Test2',"
                    + "'expression':'xyz456'},'periodType':'Monthly','name':'Test rule A'}"));

    String validationRuleB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/validationRules",
                "{'importance':'MEDIUM','operator':'not_equal_to','leftSide':{'missingValueStrategy':'NEVER_SKIP', "
                    + "'description':'Test 3','expression':'abc123'},"
                    + "'rightSide':{'missingValueStrategy': 'NEVER_SKIP', 'description':'Test 4',"
                    + "'expression':'xyz456'},'periodType':'Monthly','name':'Test rule B'}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/validationRuleGroups",
            "{ 'name' : 'A validation rule group', 'shortName' : 'A validation rule group', 'validationRules' : [{'id' : '"
                + validationRuleA
                + "'}, "
                + "{'id' : '"
                + validationRuleB
                + "'}]}"));
  }
}
