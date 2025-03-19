/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.rules.models.RuleEngineValidationException;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.imports.programrule.engine.ProgramRuleEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgramRuleEngineDescriptionTest extends PostgresIntegrationTestBase {
  private static final UID PROGRAM_UID = UID.of("BFcipDERJnf");

  private String conditionTextAtt =
      "A{Program_Rule_Variable_Text_Attr} == 'text_att' || d2:hasValue(V{current_date})";

  private String conditionWithD2HasValue = "d2:hasValue('Program_Rule_Variable_Text_Attr')";

  private String conditionWithD2HasValue2 = "d2:hasValue(A{Program_Rule_Variable_Text_Attr})";

  private String conditionNumericAtt =
      "A{Program_Rule_Variable_Numeric_Attr} == 12 || d2:hasValue(V{current_date})";

  private String conditionNumericAttWithOR =
      "A{Program_Rule_Variable_Numeric_Attr} == 12 or d2:hasValue(V{current_date})";

  private String conditionNumericAttWithAND =
      "A{Program_Rule_Variable_Numeric_Attr} == 12 and d2:hasValue(V{current_date})";

  private String conditionTextDE = "#{Program_Rule_Variable_Text_DE} == 'text_de'";

  private String incorrectConditionTextDE = "#{Program_Rule_Variable_Text_DE} == 'text_de' +";

  private String extractDataMatrixValueExpression =
      "d2:extractDataMatrixValue('serial number',"
          + " ']d201084700069915412110081996195256\u001D10DXB2005\u001D17220228') == 'some text'";

  private String conditionNumericDE = "#{Program_Rule_Variable_Numeric_DE} == 14";

  private String conditionLiteralString = "1 > 2";

  private String conditionWithD2DaysBetween =
      "d2:daysBetween(V{completed_date},V{current_date}) > 0";

  private DataElement textDataElement;

  private DataElement numericDataElement;

  private TrackedEntityAttribute textAttribute;

  private TrackedEntityAttribute numericAttribute;

  private Constant constantPI;

  private Constant constantArea;

  private Program program;

  private ProgramRule programRuleTextAtt;

  private ProgramRule programRuleWithD2HasValue;

  private ProgramRule programRuleNumericAtt;

  private ProgramRule programRuleTextDE;

  private ProgramRule programRuleNumericDE;

  private ProgramRuleVariable programRuleVariableTextDE;

  private ProgramRuleVariable programRuleVariableTextAtt;

  private ProgramRuleVariable programRuleVariableNumericDE;

  private ProgramRuleVariable programRuleVariableNumericAtt;

  private ProgramRuleVariable programRuleVariableCalculatedValue1;

  private ProgramRuleVariable programRuleVariableCalculatedValue2;

  @Autowired private ProgramRuleEngine programRuleEngine;

  @Autowired private DataElementService dataElementService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private ConstantService constantService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramRuleVariableService ruleVariableService;

  @Autowired private ProgramRuleService programRuleService;

  @BeforeAll
  void setUp() {
    constantPI = createConstant('P', 3.14);
    constantArea = createConstant('A', 22.1);
    textDataElement = createDataElement('D');
    textDataElement.setValueType(ValueType.TEXT);
    numericDataElement = createDataElement('E');
    numericDataElement.setValueType(ValueType.NUMBER);
    textAttribute = createTrackedEntityAttribute('A');
    textAttribute.setValueType(ValueType.TEXT);
    numericAttribute = createTrackedEntityAttribute('B');
    numericAttribute.setValueType(ValueType.NUMBER);
    constantService.saveConstant(constantPI);
    constantService.saveConstant(constantArea);
    dataElementService.addDataElement(textDataElement);
    dataElementService.addDataElement(numericDataElement);
    attributeService.addTrackedEntityAttribute(textAttribute);
    attributeService.addTrackedEntityAttribute(numericAttribute);
    program = createProgram('P');
    program.setUid(PROGRAM_UID.getValue());
    programService.addProgram(program);
    programRuleVariableTextAtt = createProgramRuleVariableWithTEA('R', program, textAttribute);
    programRuleVariableNumericAtt =
        createProgramRuleVariableWithTEA('S', program, numericAttribute);
    programRuleVariableTextDE =
        createProgramRuleVariableWithDataElement('T', program, textDataElement);
    programRuleVariableNumericDE =
        createProgramRuleVariableWithDataElement('U', program, numericDataElement);
    programRuleVariableCalculatedValue1 =
        createProgramRuleVariableWithSourceType(
            'X', program, ProgramRuleVariableSourceType.CALCULATED_VALUE, ValueType.NUMBER);
    programRuleVariableCalculatedValue2 =
        createProgramRuleVariableWithSourceType(
            'Y', program, ProgramRuleVariableSourceType.CALCULATED_VALUE, ValueType.NUMBER);
    programRuleVariableCalculatedValue1.setName("prv1");
    programRuleVariableCalculatedValue2.setName("prv2");

    programRuleVariableTextAtt.setName("Program_Rule_Variable_Text_Attr");
    programRuleVariableNumericAtt.setName("Program_Rule_Variable_Numeric_Attr");
    programRuleVariableTextDE.setName("Program_Rule_Variable_Text_DE");
    programRuleVariableNumericDE.setName("Program_Rule_Variable_Numeric_DE");
    ruleVariableService.addProgramRuleVariable(programRuleVariableTextAtt);
    ruleVariableService.addProgramRuleVariable(programRuleVariableNumericAtt);
    ruleVariableService.addProgramRuleVariable(programRuleVariableTextDE);
    ruleVariableService.addProgramRuleVariable(programRuleVariableNumericDE);
    ruleVariableService.addProgramRuleVariable(programRuleVariableCalculatedValue1);
    ruleVariableService.addProgramRuleVariable(programRuleVariableCalculatedValue2);
    programRuleTextAtt = createProgramRule('P', program);
    programRuleWithD2HasValue = createProgramRule('D', program);
    programRuleNumericAtt = createProgramRule('Q', program);
    programRuleTextDE = createProgramRule('R', program);
    programRuleNumericDE = createProgramRule('S', program);
    programRuleTextAtt.setCondition(conditionTextAtt);
    programRuleWithD2HasValue.setCondition(conditionWithD2HasValue);
    programRuleNumericAtt.setCondition(conditionNumericAtt);
    programRuleTextDE.setCondition(conditionTextDE);
    programRuleNumericDE.setCondition(conditionNumericDE);
    programRuleService.addProgramRule(programRuleTextAtt);
    programRuleService.addProgramRule(programRuleWithD2HasValue);
    programRuleService.addProgramRule(programRuleNumericAtt);
    programRuleService.addProgramRule(programRuleTextDE);
    programRuleService.addProgramRule(programRuleNumericDE);
  }

  @Test
  void testProgramRuleWithTextTrackedEntityAttribute() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition(programRuleTextAtt.getCondition(), program);
    assertNotNull(result);
    assertEquals("AttributeA == 'text_att' || d2:hasValue(Current date)", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithCalculatedValueRuleVariable() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition("#{prv1}+#{prv2}>0", program);

    assertNotNull(result);
    assertEquals("prv1 + prv2 > 0", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithD2HasValueTrackedEntityAttribute() throws BadRequestException {
    RuleValidationResult result =
        validateRuleCondition(programRuleWithD2HasValue.getCondition(), program);
    assertNotNull(result);
    assertEquals("d2:hasValue(AttributeA)", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithD2HasValueTrackedEntityAttribute2() throws BadRequestException {
    programRuleWithD2HasValue.setCondition(conditionWithD2HasValue2);
    RuleValidationResult result =
        validateRuleCondition(programRuleWithD2HasValue.getCondition(), program);
    assertNotNull(result);
    assertEquals("d2:hasValue(AttributeA)", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithNumericTrackedEntityAttribute() throws BadRequestException {
    RuleValidationResult result =
        validateRuleCondition(programRuleNumericAtt.getCondition(), program);
    assertNotNull(result);
    assertEquals("AttributeB == 12 || d2:hasValue(Current date)", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithNumericTrackedEntityAttributeWithOr() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition(conditionNumericAttWithOR, program);
    assertNotNull(result);
    assertEquals("AttributeB == 12 or d2:hasValue(Current date)", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithNumericTrackedEntityAttributeWithAnd() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition(conditionNumericAttWithAND, program);
    assertNotNull(result); // new environment variable must be added in this map
    assertEquals("AttributeB == 12 and d2:hasValue(Current date)", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithTextDataElement() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition(programRuleTextDE.getCondition(), program);
    assertNotNull(result);
    assertEquals("DataElementD == 'text_de'", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithNumericDataElement() throws BadRequestException {
    RuleValidationResult result =
        validateRuleCondition(programRuleNumericDE.getCondition(), program);
    assertNotNull(result);
    assertEquals("DataElementE == 14", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithLiterals() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition(conditionLiteralString, program);
    assertNotNull(result);
    assertEquals("1 > 2", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testProgramRuleWithD2DaysBetween() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition(conditionWithD2DaysBetween, program);
    assertNotNull(result);
    assertEquals("d2:daysBetween(Completed date,Current date) > 0", result.getDescription());
    assertTrue(result.getValid());
  }

  @Test
  void testIncorrectRuleWithLiterals() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition("1 > 2 +", program);
    assertNotNull(result);
    assertFalse(result.getValid());
    assertInstanceOf(RuleEngineValidationException.class, result.getException());
  }

  @Test
  void testIncorrectRuleWithDataElement() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition(incorrectConditionTextDE, program);
    assertNotNull(result);
    assertFalse(result.getValid());
    assertInstanceOf(RuleEngineValidationException.class, result.getException());
  }

  @Test
  void testExtractDataMatrixValue() throws BadRequestException {
    RuleValidationResult result = validateRuleCondition(extractDataMatrixValueExpression, program);
    assertNotNull(result);
    assertTrue(result.getValid());
  }

  @Test
  void testDataFieldExpressionDescription() throws BadRequestException {
    RuleValidationResult result =
        programRuleEngine.getDataExpressionDescription("1 + 2 +", PROGRAM_UID);
    assertNotNull(result);
    assertFalse(result.getValid());
    assertInstanceOf(RuleEngineValidationException.class, result.getException());
    result =
        programRuleEngine.getDataExpressionDescription(
            "d2:daysBetween(V{completed_date},V{current_date}) > 0 )", PROGRAM_UID);
    assertNotNull(result);
    assertFalse(result.getValid());
    assertInstanceOf(RuleEngineValidationException.class, result.getException());
    result =
        programRuleEngine.getDataExpressionDescription(conditionWithD2DaysBetween, PROGRAM_UID);
    assertNotNull(result);
    assertTrue(result.getValid());
    assertEquals("d2:daysBetween(Completed date,Current date) > 0", result.getDescription());
    result =
        programRuleEngine.getDataExpressionDescription(
            programRuleNumericDE.getCondition(), PROGRAM_UID);
    assertNotNull(result);
    assertTrue(result.getValid());
    assertEquals("DataElementE == 14", result.getDescription());
    result =
        programRuleEngine.getDataExpressionDescription(
            programRuleNumericAtt.getCondition(), PROGRAM_UID);
    assertNotNull(result);
    assertTrue(result.getValid());
    assertEquals("AttributeB == 12 || d2:hasValue(Current date)", result.getDescription());
    result = programRuleEngine.getDataExpressionDescription("'2020-12-12'", PROGRAM_UID);
    assertNotNull(result);
    assertTrue(result.getValid());
    assertEquals("'2020-12-12'", result.getDescription());
    result = programRuleEngine.getDataExpressionDescription("1 + 1", PROGRAM_UID);
    assertNotNull(result);
    assertTrue(result.getValid());
    assertEquals("1 + 1", result.getDescription());
    result = programRuleEngine.getDataExpressionDescription("'sample text'", PROGRAM_UID);
    assertNotNull(result);
    assertTrue(result.getValid());
    assertEquals("'sample text'", result.getDescription());
  }

  private RuleValidationResult validateRuleCondition(String condition, Program program)
      throws BadRequestException {
    return programRuleEngine.getDescription(condition, UID.of(program.getUid()));
  }
}
