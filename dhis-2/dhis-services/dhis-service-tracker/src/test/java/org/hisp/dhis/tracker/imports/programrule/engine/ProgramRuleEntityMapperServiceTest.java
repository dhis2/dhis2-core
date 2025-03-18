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
package org.hisp.dhis.tracker.imports.programrule.engine;

import static org.hisp.dhis.programrule.ProgramRuleActionType.ASSIGN;
import static org.hisp.dhis.programrule.ProgramRuleActionType.DISPLAYTEXT;
import static org.hisp.dhis.programrule.ProgramRuleActionType.ERRORONCOMPLETE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SCHEDULEMESSAGE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SENDMESSAGE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SETMANDATORYFIELD;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SHOWERROR;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SHOWWARNING;
import static org.hisp.dhis.programrule.ProgramRuleActionType.WARNINGONCOMPLETE;
import static org.hisp.dhis.tracker.imports.programrule.engine.RuleActionKey.ATTRIBUTE_TYPE;
import static org.hisp.dhis.tracker.imports.programrule.engine.RuleActionKey.CONTENT;
import static org.hisp.dhis.tracker.imports.programrule.engine.RuleActionKey.FIELD;
import static org.hisp.dhis.tracker.imports.programrule.engine.RuleActionKey.NOTIFICATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.rules.api.DataItem;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.rules.models.Rule;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.util.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar.
 */
@ExtendWith(MockitoExtension.class)
class ProgramRuleEntityMapperServiceTest extends TestBase {
  private final List<ProgramRule> programRules = new ArrayList<>();

  private Program program;

  private ProgramRuleVariable programRuleVariableA;

  private ProgramRuleVariable programRuleVariableB;

  private ProgramRuleVariable programRuleVariableC;

  private DataElement dataElement;

  @Mock private I18nManager i18nManager;

  @Mock private I18n i18n;

  @InjectMocks private DefaultProgramRuleEntityMapperService mapper;

  @BeforeEach
  public void initTest() {

    program = createProgram('P');

    TrackedEntityAttribute attribute = createTrackedEntityAttribute('Z');
    attribute.setName("Tracked_entity_attribute_A");

    DataElement dataElement1 = createDataElement('E');
    dataElement1.setFormName("DateElement_E");

    OptionSet optionSet = new OptionSet();
    Option option = new Option();
    option.setName("optionName");
    option.setCode("optionCode");

    optionSet.setOptions(List.of(option));

    DataElement dataElementWithOptionSet = createDataElement('F');
    dataElementWithOptionSet.setFormName("dataElementWithOptionSet");
    dataElementWithOptionSet.setOptionSet(optionSet);

    dataElement = createDataElement('D');
    dataElement.setValueType(ValueType.TEXT);

    programRuleVariableA =
        createProgramRuleVariable(
            'A',
            ProgramRuleVariableSourceType.CALCULATED_VALUE,
            program,
            createDataElement('D'),
            null);

    programRuleVariableB =
        createProgramRuleVariable(
            'B', ProgramRuleVariableSourceType.TEI_ATTRIBUTE, program, null, attribute);

    programRuleVariableC =
        createProgramRuleVariable(
            'C',
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            program,
            dataElement1,
            null);

    ProgramRuleAction assignAction = createProgramRuleAction('I', ASSIGN, "test_variable", "2+2");
    ProgramRuleAction displayText =
        createProgramRuleAction('D', DISPLAYTEXT, "test_variable", "2+2");
    ProgramRuleAction sendMessageAction = createProgramRuleAction('J', SENDMESSAGE, null, null);
    ProgramRuleAction showWarning = createProgramRuleAction('L', SHOWWARNING, "warning", "2+2");
    ProgramRuleAction showError = createProgramRuleAction('M', SHOWERROR, "error", "2+2");
    ProgramRuleAction warningOnComplete =
        createProgramRuleAction('N', WARNINGONCOMPLETE, "complete warning", "2+2");
    ProgramRuleAction errorOnComplete =
        createProgramRuleAction('O', ERRORONCOMPLETE, "complete error", "2+2");
    ProgramRuleAction setMandatoryField =
        createProgramRuleAction('P', SETMANDATORYFIELD, null, "2+2");
    ProgramRuleAction scheduleMessage =
        createProgramRuleAction('Q', SCHEDULEMESSAGE, "test_variable", "2+2");

    ProgramRule programRuleA =
        createProgramRule('A', Sets.newHashSet(assignAction, displayText, errorOnComplete), 1);
    ProgramRule programRuleB =
        createProgramRule('B', Sets.newHashSet(sendMessageAction, showWarning, showError), 4);
    ProgramRule programRuleD =
        createProgramRule(
            'D',
            Sets.newHashSet(
                sendMessageAction, warningOnComplete, setMandatoryField, scheduleMessage),
            null);

    programRules.add(programRuleA);
    programRules.add(programRuleB);
    programRules.add(programRuleD);
  }

  @Test
  void shouldMapProgramRules() {
    List<Rule> mappedProgramRules = mapper.toRules(programRules);

    for (int i = 0; i < mappedProgramRules.size(); i++) {
      assertRule(programRules.get(i), mappedProgramRules.get(i));
    }
  }

  @Test
  void testGetItemStore() {
    String envVariable = "Completed date";
    Constant constant = new Constant();
    constant.setValue(7.8);
    constant.setAutoFields();
    constant.setName("Gravity");

    when(i18nManager.getI18n()).thenReturn(i18n);
    when(i18n.getString(anyString())).thenReturn(envVariable);

    Map<String, DataItem> itemStore =
        mapper.getItemStore(
            List.of(programRuleVariableA, programRuleVariableB, programRuleVariableC),
            List.of(constant));

    assertNotNull(itemStore);
    assertTrue(itemStore.containsKey(programRuleVariableA.getName()));
    assertEquals(
        itemStore.get(programRuleVariableA.getName()).getDisplayName(),
        ObjectUtils.firstNonNull(
            programRuleVariableA.getDisplayName(), programRuleVariableA.getName()));

    assertTrue(itemStore.containsKey(programRuleVariableB.getName()));
    assertEquals(
        itemStore.get(programRuleVariableB.getName()).getDisplayName(),
        ObjectUtils.firstNonNull(
            programRuleVariableB.getAttribute().getDisplayName(),
            programRuleVariableB.getAttribute().getDisplayFormName(),
            programRuleVariableB.getAttribute().getName()));

    assertTrue(itemStore.containsKey(programRuleVariableC.getName()));
    assertEquals(
        itemStore.get(programRuleVariableC.getName()).getDisplayName(),
        ObjectUtils.firstNonNull(
            programRuleVariableC.getDataElement().getDisplayFormName(),
            programRuleVariableC.getDataElement().getFormName(),
            programRuleVariableC.getDataElement().getName()));

    assertTrue(itemStore.containsKey(constant.getUid()));
    assertEquals("Gravity", itemStore.get(constant.getUid()).getDisplayName());
  }

  private ProgramRule createProgramRule(
      char uniqueCharacter, Set<ProgramRuleAction> ruleActions, Integer priority) {
    ProgramRule programRule = createProgramRule(uniqueCharacter, program);
    programRule.setPriority(priority);
    programRule.setCondition("");
    programRule.setProgramRuleActions(ruleActions);

    return programRule;
  }

  private ProgramRuleAction createProgramRuleAction(
      char uniqueCharacter, ProgramRuleActionType type, String content, String data) {
    ProgramRuleAction programRuleAction = createProgramRuleAction(uniqueCharacter);
    programRuleAction.setProgramRuleActionType(type);

    switch (type) {
      case DISPLAYTEXT -> {
        programRuleAction.setLocation("feedback");
        programRuleAction.setContent("content");
        programRuleAction.setData("true");
      }
      case ASSIGN, SHOWERROR, SHOWWARNING, ERRORONCOMPLETE, WARNINGONCOMPLETE -> {
        programRuleAction.setContent(content);
        programRuleAction.setData(data);
        programRuleAction.setDataElement(dataElement);
      }
      case SETMANDATORYFIELD -> {
        programRuleAction.setDataElement(dataElement);
      }
      case SENDMESSAGE, SCHEDULEMESSAGE -> {
        ProgramNotificationTemplate notificationTemplate = new ProgramNotificationTemplate();
        notificationTemplate.setUid("uid0");
        programRuleAction.setTemplateUid(notificationTemplate.getUid());
        programRuleAction.setData(data);
      }
      default -> throw new IllegalStateException("Unexpected value: " + type);
    }

    return programRuleAction;
  }

  private ProgramRuleVariable createProgramRuleVariable(
      char uniqueCharacter,
      ProgramRuleVariableSourceType sourceType,
      Program program,
      DataElement dataElement,
      TrackedEntityAttribute attribute) {
    ProgramRuleVariable variable = createProgramRuleVariable(uniqueCharacter, program);
    variable.setSourceType(sourceType);
    variable.setProgram(program);
    variable.setDataElement(dataElement);
    variable.setAttribute(attribute);

    return variable;
  }

  private void assertRule(ProgramRule expectedRule, Rule actualRule) {
    String expectedProgramStage =
        expectedRule.getProgramStage() == null ? "" : expectedRule.getProgramStage().getUid();

    assertEquals(expectedRule.getUid(), actualRule.getUid());
    assertEquals(expectedProgramStage, actualRule.getProgramStage());
    assertEquals(expectedRule.getName(), actualRule.getName());
    assertEquals(expectedRule.getCondition(), actualRule.getCondition());
    assertEquals(expectedRule.getPriority(), actualRule.getPriority());
    assertActions(expectedRule.getProgramRuleActions(), actualRule.getActions());
  }

  private void assertActions(Set<ProgramRuleAction> programRuleActions, List<RuleAction> actions) {
    for (RuleAction actualAction : actions) {
      ProgramRuleAction expectedProgramRuleAction =
          programRuleActions.stream()
              .filter(a -> a.getProgramRuleActionType().name().equals(actualAction.getType()))
              .findFirst()
              .get();
      assertAction(expectedProgramRuleAction, actualAction);
    }
  }

  private void assertAction(ProgramRuleAction expectedRuleAction, RuleAction actualRuleAction) {
    switch (expectedRuleAction.getProgramRuleActionType()) {
      case ASSIGN, SHOWWARNING, WARNINGONCOMPLETE, SHOWERROR, ERRORONCOMPLETE ->
          assertGenericAction(expectedRuleAction, actualRuleAction);
      case SETMANDATORYFIELD -> assertMandatoryField(expectedRuleAction, actualRuleAction);
      case SENDMESSAGE -> assertSendMessage(expectedRuleAction, actualRuleAction);
      case SCHEDULEMESSAGE -> assertScheduleMessage(expectedRuleAction, actualRuleAction);
      default ->
          throw new IllegalStateException(
              "Unexpected value: " + expectedRuleAction.getProgramRuleActionType());
    }
  }

  private void assertScheduleMessage(
      ProgramRuleAction expectedRuleAction, RuleAction actualRuleAction) {
    assertEquals(expectedRuleAction.getData(), actualRuleAction.getData());
    assertEquals(
        expectedRuleAction.getNotificationTemplate().getUid(),
        actualRuleAction.getValues().get(NOTIFICATION));
  }

  private void assertSendMessage(
      ProgramRuleAction expectedRuleAction, RuleAction actualRuleAction) {
    assertNull(actualRuleAction.getData());
    assertEquals(
        expectedRuleAction.getNotificationTemplate().getUid(),
        actualRuleAction.getValues().get(NOTIFICATION));
  }

  private void assertMandatoryField(
      ProgramRuleAction expectedRuleAction, RuleAction actualRuleAction) {
    assertNull(actualRuleAction.getData());
    assertNull(actualRuleAction.getValues().get(CONTENT));
    assertEquals(
        expectedRuleAction.getDataElement().getUid(), actualRuleAction.getValues().get(FIELD));
    assertEquals(
        AttributeType.DATA_ELEMENT.name(), actualRuleAction.getValues().get(ATTRIBUTE_TYPE));
  }

  private void assertGenericAction(
      ProgramRuleAction expectedRuleAction, RuleAction actualRuleAction) {
    assertEquals(expectedRuleAction.getData(), actualRuleAction.getData());
    assertEquals(expectedRuleAction.getContent(), actualRuleAction.getValues().get(CONTENT));
    assertEquals(
        expectedRuleAction.getDataElement().getUid(), actualRuleAction.getValues().get(FIELD));
    assertEquals(
        AttributeType.DATA_ELEMENT.name(), actualRuleAction.getValues().get(ATTRIBUTE_TYPE));
  }
}
