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
package org.hisp.dhis.programrule.engine;

import static org.hisp.dhis.programrule.ProgramRuleActionType.ASSIGN;
import static org.hisp.dhis.programrule.ProgramRuleActionType.DISPLAYTEXT;
import static org.hisp.dhis.programrule.ProgramRuleActionType.ERRORONCOMPLETE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SCHEDULEMESSAGE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SENDMESSAGE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SETMANDATORYFIELD;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SHOWERROR;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SHOWWARNING;
import static org.hisp.dhis.programrule.ProgramRuleActionType.WARNINGONCOMPLETE;
import static org.hisp.dhis.programrule.engine.RuleActionKey.ATTRIBUTE_TYPE;
import static org.hisp.dhis.programrule.engine.RuleActionKey.CONTENT;
import static org.hisp.dhis.programrule.engine.RuleActionKey.FIELD;
import static org.hisp.dhis.programrule.engine.RuleActionKey.NOTIFICATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kotlinx.datetime.Instant;
import kotlinx.datetime.LocalDate;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.rules.api.DataItem;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.rules.models.Rule;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleDataValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.rules.models.RuleVariable;
import org.hisp.dhis.rules.models.RuleVariableAttribute;
import org.hisp.dhis.rules.models.RuleVariableCalculatedValue;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.util.DateUtils;
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
class ProgramRuleEntityMapperServiceTest extends DhisConvenienceTest {

  private static final String SAMPLE_VALUE_A = "textValueA";

  private static final Date NOW = new Date();
  private static final Date TOMORROW = DateUtils.addDays(NOW, 1);
  private static final Date YESTERDAY = DateUtils.addDays(NOW, -1);
  private static final Date AFTER_TOMORROW = DateUtils.addDays(NOW, 2);

  private List<ProgramRule> programRules = new ArrayList<>();

  private final List<ProgramRuleVariable> programRuleVariables = new ArrayList<>();

  private Program program;

  private ProgramStage programStage;

  private ProgramRuleVariable programRuleVariableA;

  private ProgramRuleVariable programRuleVariableB;

  private ProgramRuleVariable programRuleVariableC;

  private OrganisationUnit organisationUnit;

  private TrackedEntityAttribute trackedEntityAttribute;

  private DataElement dataElement;

  @Mock private ProgramRuleVariableService programRuleVariableService;

  @Mock private ConstantService constantService;

  @Mock private I18nManager i18nManager;

  @Mock private I18n i18n;

  @InjectMocks private DefaultProgramRuleEntityMapperService mapper;

  @BeforeEach
  public void initTest() {

    program = createProgram('P');
    programStage = createProgramStage('S', program);

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
    organisationUnit = createOrganisationUnit('O');

    trackedEntityAttribute = createTrackedEntityAttribute('A', ValueType.TEXT);

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

    programRuleVariables.add(programRuleVariableA);
    programRuleVariables.add(programRuleVariableB);
    programRuleVariables.add(programRuleVariableC);

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
    List<Rule> mappedProgramRules = mapper.toMappedProgramRules(programRules);

    for (int i = 0; i < mappedProgramRules.size(); i++) {
      assertRule(programRules.get(i), mappedProgramRules.get(i));
    }
  }

  @Test
  void shouldToMapProgramRuleVariables() {
    when(programRuleVariableService.getAllProgramRuleVariable()).thenReturn(programRuleVariables);
    RuleVariableAttribute ruleVariableAttribute;
    RuleVariableCalculatedValue ruleVariableCalculatedValue;

    List<RuleVariable> ruleVariables = mapper.toMappedProgramRuleVariables();

    assertEquals(ruleVariables.size(), programRuleVariables.size());

    for (RuleVariable variable : ruleVariables) {
      if (variable instanceof RuleVariableAttribute) {
        ruleVariableAttribute = (RuleVariableAttribute) variable;
        assertEquals(
            ruleVariableAttribute.getField(), programRuleVariableB.getAttribute().getUid());
        assertEquals(ruleVariableAttribute.getName(), programRuleVariableB.getName());
      }

      if (variable instanceof RuleVariableCalculatedValue) {
        ruleVariableCalculatedValue = (RuleVariableCalculatedValue) variable;
        assertEquals(ruleVariableCalculatedValue.getName(), programRuleVariableA.getName());
      }
    }
  }

  @Test
  void shouldFailToMapEventWhenProgramStageIsNull() {
    Event event = event();
    event.setProgramStage(null);

    assertThrows(NullPointerException.class, () -> mapper.toMappedRuleEvent(event));
  }

  @Test
  void shouldMapEventToRuleEvent() {
    Event event = event();

    RuleEvent ruleEvent = mapper.toMappedRuleEvent(event);

    assertEvent(event, ruleEvent);
  }

  @Test
  void shouldMapEventToRuleEventWhenOrganisationUnitCodeIsNull() {
    OrganisationUnit organisationUnitWithNullCode = createOrganisationUnit('A');
    organisationUnitWithNullCode.setCode(null);
    Event event = event();
    event.setOrganisationUnit(organisationUnitWithNullCode);

    RuleEvent ruleEvent = mapper.toMappedRuleEvent(event);

    assertEvent(event, ruleEvent);
  }

  @Test
  void shouldFailToMapEnrollmentWhenProgramIsNull() {
    Enrollment enrollment = enrollment();
    enrollment.setProgram(null);
    List<TrackedEntityAttributeValue> trackedEntityAttributeValues = Collections.emptyList();

    assertThrows(
        NullPointerException.class,
        () -> mapper.toMappedRuleEnrollment(enrollment, trackedEntityAttributeValues));
  }

  @Test
  void shouldMapEnrollmentToRuleEnrollment() {
    Enrollment enrollment = enrollment();
    TrackedEntityAttributeValue trackedEntityAttributeValue =
        createTrackedEntityAttributeValue('E', trackedEntity(), trackedEntityAttribute);
    trackedEntityAttributeValue.setValue(SAMPLE_VALUE_A);

    RuleEnrollment ruleEnrollment =
        mapper.toMappedRuleEnrollment(enrollment, List.of(trackedEntityAttributeValue));

    assertEnrollment(enrollment, ruleEnrollment);
  }

  @Test
  void shouldMapEnrollmentToRuleEnrollmentWhenOrganisationUnitCodeIsNull() {
    OrganisationUnit organisationUnitWithNullCode = createOrganisationUnit('A');
    organisationUnitWithNullCode.setCode(null);
    Enrollment enrollment = enrollment();
    enrollment.setOrganisationUnit(organisationUnitWithNullCode);
    TrackedEntityAttributeValue trackedEntityAttributeValue =
        createTrackedEntityAttributeValue('E', trackedEntity(), trackedEntityAttribute);
    trackedEntityAttributeValue.setValue(SAMPLE_VALUE_A);

    RuleEnrollment ruleEnrollment =
        mapper.toMappedRuleEnrollment(enrollment, List.of(trackedEntityAttributeValue));

    assertEnrollment(enrollment, ruleEnrollment);
  }

  @Test
  void testGetItemStore() {
    String env_variable = "Completed date";
    Constant constant = new Constant();
    constant.setValue(7.8);
    constant.setAutoFields();
    constant.setName("Gravity");
    List<Constant> constants = List.of(constant);

    when(constantService.getAllConstants()).thenReturn(constants);
    when(i18nManager.getI18n()).thenReturn(i18n);
    when(i18n.getString(anyString())).thenReturn(env_variable);

    Map<String, DataItem> itemStore =
        mapper.getItemStore(
            List.of(programRuleVariableA, programRuleVariableB, programRuleVariableC));

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

  private TrackedEntity trackedEntity() {
    return createTrackedEntity('I', organisationUnit, trackedEntityAttribute);
  }

  private Enrollment enrollment() {
    Enrollment enrollment = new Enrollment(NOW, TOMORROW, trackedEntity(), program);
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.setStatus(ProgramStatus.ACTIVE);
    enrollment.setAutoFields();
    return enrollment;
  }

  private Event event() {
    Event event = new Event(enrollment(), programStage);
    event.setUid(CodeGenerator.generateUid());
    event.setOrganisationUnit(organisationUnit);
    event.setAutoFields();
    event.setScheduledDate(YESTERDAY);
    event.setOccurredDate(AFTER_TOMORROW);
    event.setEventDataValues(Sets.newHashSet(eventDataValue(dataElement.getUid(), SAMPLE_VALUE_A)));
    return event;
  }

  private EventDataValue eventDataValue(String dataElementUid, String value) {
    EventDataValue eventDataValue = new EventDataValue();
    eventDataValue.setDataElement(dataElementUid);
    eventDataValue.setValue(value);
    eventDataValue.setAutoFields();
    return eventDataValue;
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
        expectedRuleAction.getTemplateUid(), actualRuleAction.getValues().get(NOTIFICATION));
  }

  private void assertSendMessage(
      ProgramRuleAction expectedRuleAction, RuleAction actualRuleAction) {
    assertNull(actualRuleAction.getData());
    assertEquals(
        expectedRuleAction.getTemplateUid(), actualRuleAction.getValues().get(NOTIFICATION));
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

  private void assertEvent(Event event, RuleEvent ruleEvent) {
    assertEquals(event.getUid(), ruleEvent.getEvent());
    assertEquals(event.getProgramStage().getUid(), ruleEvent.getProgramStage());
    assertEquals(event.getProgramStage().getName(), ruleEvent.getProgramStageName());
    assertEquals(event.getStatus().name(), ruleEvent.getStatus().name());
    assertDates(event.getOccurredDate(), ruleEvent.getEventDate());
    assertNull(ruleEvent.getCompletedDate());
    assertEquals(event.getOrganisationUnit().getUid(), ruleEvent.getOrganisationUnit());
    assertEquals(event.getOrganisationUnit().getCode(), ruleEvent.getOrganisationUnitCode());
    assertDataValue(event.getEventDataValues().iterator().next(), ruleEvent.getDataValues().get(0));
  }

  private void assertDates(Date expected, Instant actual) {
    assertEquals(expected.getTime(), actual.getValue$kotlinx_datetime().toEpochMilli());
  }

  private void assertDataValue(EventDataValue expectedDataValue, RuleDataValue actualDataValue) {
    assertEquals(expectedDataValue.getValue(), actualDataValue.getValue());
    assertEquals(expectedDataValue.getDataElement(), actualDataValue.getDataElement());
  }

  private void assertEnrollment(Enrollment enrollment, RuleEnrollment ruleEnrollment) {
    assertEquals(enrollment.getUid(), ruleEnrollment.getEnrollment());
    assertEquals(enrollment.getProgram().getName(), ruleEnrollment.getProgramName());
    assertDates(enrollment.getOccurredDate(), ruleEnrollment.getIncidentDate());
    assertDates(enrollment.getEnrollmentDate(), ruleEnrollment.getEnrollmentDate());
    assertEquals(enrollment.getStatus().name(), ruleEnrollment.getStatus().name());
    assertEquals(enrollment.getOrganisationUnit().getUid(), ruleEnrollment.getOrganisationUnit());
    assertEquals(
        enrollment.getOrganisationUnit().getCode(), ruleEnrollment.getOrganisationUnitCode());
    assertEquals(SAMPLE_VALUE_A, ruleEnrollment.getAttributeValues().get(0).getValue());
  }

  private void assertDates(Date expected, LocalDate actual) {
    assertEquals(DateUtils.toMediumDate(expected), actual.toString());
  }
}
