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
package org.hisp.dhis.validation;

import static java.util.Arrays.asList;
import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.expression.Operator.greater_than;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class ValidationRuleStoreTest extends TransactionalIntegrationTest {

  @Autowired private ValidationRuleStore validationRuleStore;

  @Autowired private IdentifiableObjectManager idObjectManager;

  private final Expression expressionA = new Expression("expressionA", "descriptionA");

  private final Expression expressionB = new Expression("expressionB", "descriptionB");

  private final Expression expressionC = new Expression("expressionC", "descriptionC");

  private final Expression expressionD = new Expression("expressionD", "descriptionD");

  private final PeriodType periodType = PeriodType.getAvailablePeriodTypes().iterator().next();

  private void assertValidationRule(char uniqueCharacter, ValidationRule actual) {
    assertEquals("ValidationRule" + uniqueCharacter, actual.getName());
    assertEquals("Description" + uniqueCharacter, actual.getDescription());
    assertNotNull(actual.getLeftSide().getExpression());
    assertNotNull(actual.getRightSide().getExpression());
    assertEquals(periodType, actual.getPeriodType());
  }

  // -------------------------------------------------------------------------
  // ValidationRule
  // -------------------------------------------------------------------------
  @Test
  void testSaveValidationRule() {
    ValidationRule ruleA = addValidationRule('A', equal_to, expressionA, expressionB, periodType);
    ruleA = validationRuleStore.get(ruleA.getId());
    assertValidationRule('A', ruleA);
    assertEquals(equal_to, ruleA.getOperator());
  }

  @Test
  void testUpdateValidationRule() {
    ValidationRule ruleA = addValidationRule('A', equal_to, expressionA, expressionB, periodType);
    ruleA = validationRuleStore.get(ruleA.getId());
    assertValidationRule('A', ruleA);
    assertEquals(equal_to, ruleA.getOperator());
    ruleA.setName("ValidationRuleB");
    ruleA.setDescription("DescriptionB");
    ruleA.setOperator(greater_than);
    validationRuleStore.update(ruleA);
    ruleA = validationRuleStore.get(ruleA.getId());
    assertValidationRule('B', ruleA);
    assertEquals(greater_than, ruleA.getOperator());
  }

  @Test
  void testDeleteValidationRule() {
    ValidationRule ruleA = addValidationRule('A', equal_to, expressionA, expressionB, periodType);
    ValidationRule ruleB = addValidationRule('B', equal_to, expressionC, expressionD, periodType);
    assertNotNull(validationRuleStore.get(ruleA.getId()));
    assertNotNull(validationRuleStore.get(ruleB.getId()));
    ruleA.clearExpressions();
    validationRuleStore.delete(ruleA);
    assertNull(validationRuleStore.get(ruleA.getId()));
    assertNotNull(validationRuleStore.get(ruleB.getId()));
    ruleB.clearExpressions();
    validationRuleStore.delete(ruleB);
    assertNull(validationRuleStore.get(ruleA.getId()));
    assertNull(validationRuleStore.get(ruleB.getId()));
  }

  @Test
  void testGetAllValidationRules() {
    ValidationRule ruleA = addValidationRule('A', equal_to, expressionA, expressionB, periodType);
    ValidationRule ruleB = addValidationRule('B', equal_to, expressionC, expressionD, periodType);
    assertContainsOnly(List.of(ruleA, ruleB), validationRuleStore.getAll());
  }

  @Test
  void testGetAllFormValidationRules() {
    addValidationRule('A', equal_to, expressionA, expressionB, periodType, true);
    ValidationRule ruleB = addValidationRule('B', equal_to, expressionC, expressionD, periodType);
    assertContainsOnly(List.of(ruleB), validationRuleStore.getAllFormValidationRules());
  }

  @Test
  void testGetValidationRuleByName() {
    ValidationRule ruleA = addValidationRule('A', equal_to, expressionA, expressionB, periodType);
    ValidationRule ruleB = addValidationRule('B', equal_to, expressionC, expressionD, periodType);
    assertEquals(ruleA, validationRuleStore.getByName("ValidationRuleA"));
    assertEquals(ruleB, validationRuleStore.getByName("ValidationRuleB"));
  }

  @Test
  void testGetValidationRuleCount() {
    addValidationRule('A', equal_to, expressionA, expressionB, periodType);
    addValidationRule('B', equal_to, expressionC, expressionD, periodType);
    assertEquals(2, validationRuleStore.getCount());
  }

  protected static Expression createExpression(char uniqueCharacter) {
    String expression = "Expression1" + uniqueCharacter;
    return new Expression(expression, expression);
  }

  @Test
  void testGetValidationRulesWithNotificationTemplates() {
    ValidationRule ruleA = addValidationRule('A', equal_to, expressionA, expressionB, periodType);
    ValidationRule ruleB = addValidationRule('B', equal_to, expressionC, expressionD, periodType);
    assertIsEmpty(validationRuleStore.getValidationRulesWithNotificationTemplates());
    // Add template
    addValidationNotificationTemplate('A', ruleA);
    assertContainsOnly(
        List.of(ruleA), validationRuleStore.getValidationRulesWithNotificationTemplates());
    // Add one more
    addValidationNotificationTemplate('B', ruleB);
    assertContainsOnly(
        List.of(ruleA, ruleB), validationRuleStore.getValidationRulesWithNotificationTemplates());
  }

  @Test
  void testGetValidationRulesWithoutGroups() {
    ValidationRule ruleA = addValidationRule('A', equal_to, expressionA, expressionB, periodType);
    ValidationRule ruleB = addValidationRule('B', equal_to, expressionC, expressionD, periodType);
    addValidationRuleGroup('A', ruleA);
    assertContainsOnly(List.of(ruleB), validationRuleStore.getValidationRulesWithoutGroups());
  }

  private ValidationNotificationTemplate addValidationNotificationTemplate(
      char uniqueCharacter, ValidationRule... rules) {
    ValidationNotificationTemplate template =
        createValidationNotificationTemplate("Template " + uniqueCharacter);
    asList(rules).forEach(template::addValidationRule);
    idObjectManager.save(template);
    return template;
  }

  private ValidationRule addValidationRule(
      char uniqueCharacter,
      Operator operator,
      Expression leftSide,
      Expression rightSide,
      PeriodType periodType) {
    return addValidationRule(uniqueCharacter, operator, leftSide, rightSide, periodType, false);
  }

  private ValidationRule addValidationRule(
      char uniqueCharacter,
      Operator operator,
      Expression leftSide,
      Expression rightSide,
      PeriodType periodType,
      boolean skipFormValidation) {
    ValidationRule rule =
        createValidationRule(
            Character.toString(uniqueCharacter),
            operator,
            leftSide,
            rightSide,
            periodType,
            skipFormValidation);
    validationRuleStore.save(rule);
    return rule;
  }

  private ValidationRuleGroup addValidationRuleGroup(
      char uniqueCharacter, ValidationRule... rules) {
    ValidationRuleGroup group = createValidationRuleGroup(uniqueCharacter);
    asList(rules).forEach(group::addValidationRule);
    idObjectManager.save(group);
    return group;
  }
}
