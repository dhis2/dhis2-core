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
package org.hisp.dhis.tracker.imports.programrule.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import kotlinx.datetime.LocalDateTime;
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
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.rules.api.DataItem;
import org.hisp.dhis.rules.models.RuleDataValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.rules.models.RuleEventStatus;
import org.hisp.dhis.rules.models.RuleVariable;
import org.hisp.dhis.rules.models.RuleVariableAttribute;
import org.hisp.dhis.rules.models.RuleVariableCalculatedValue;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

/**
 * @author Zubair Asghar.
 */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ProgramRuleEntityMapperServiceTest extends TestBase {

  private static final String SAMPLE_VALUE_A = "textValueA";

  private static final String SAMPLE_VALUE_B = "textValueB";

  private List<ProgramRule> programRules = new ArrayList<>();

  private List<ProgramRuleVariable> programRuleVariables = new ArrayList<>();

  private Program program;

  private ProgramStage programStage;

  private ProgramRule programRuleA = null;

  private ProgramRule programRuleB = null;

  private ProgramRule programRuleC = null;

  private ProgramRule programRuleD = null;

  private ProgramRuleAction assignAction = null;

  private ProgramRuleAction sendMessageAction = null;

  private ProgramRuleAction displayText = null;

  private ProgramRuleVariable programRuleVariableA = null;

  private ProgramRuleVariable programRuleVariableB = null;

  private ProgramRuleVariable programRuleVariableC = null;

  private ProgramRuleVariable programRuleVariableD = null;

  private OrganisationUnit organisationUnit;

  private TrackedEntityAttribute trackedEntityAttribute;

  private TrackedEntityAttributeValue trackedEntityAttributeValue;

  private DataElement dataElement;

  private EventDataValue eventDataValueA;

  private EventDataValue eventDataValueB;

  private Enrollment enrollment;

  private Enrollment enrollmentB;

  private TrackedEntity trackedEntity;

  private Event eventA;

  private Event eventB;

  private Event eventC;

  private RuleEvent expectedRuleEvent;

  @Mock private ProgramRuleService programRuleService;

  @Mock private ProgramRuleVariableService programRuleVariableService;

  @Mock private ConstantService constantService;

  @Mock private I18nManager i18nManager;

  @Mock private I18n i18n;

  private DefaultProgramRuleEntityMapperService subject;

  @BeforeEach
  public void initTest() {
    subject =
        new DefaultProgramRuleEntityMapperService(
            programRuleVariableService, constantService, i18nManager);

    setUpProgramRules();
  }

  @Test
  void testMappedRuleVariableValues() {
    when(programRuleVariableService.getAllProgramRuleVariable()).thenReturn(programRuleVariables);
    RuleVariableAttribute ruleVariableAttribute;
    RuleVariableCalculatedValue ruleVariableCalculatedValue;

    List<RuleVariable> ruleVariables = subject.toMappedProgramRuleVariables();

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
  void testExceptionWhenMandatoryFieldIsMissingInRuleEvent() {
    assertThrows(NullPointerException.class, () -> subject.toMappedRuleEvent(eventC));
  }

  @Test
  void testMappedRuleEvent() {
    RuleEvent ruleEvent = subject.toMappedRuleEvent(eventA);

    assertEquals(expectedRuleEvent, ruleEvent);
  }

  @Test
  void testMappedRuleEvents() {
    List<RuleEvent> ruleEvents = subject.toMappedRuleEvents(Sets.newHashSet(eventA, eventB));

    assertEquals(2, ruleEvents.size());
  }

  @Test
  void testExceptionWhenMandatoryValueMissingMappedEnrollment() {
    List<TrackedEntityAttributeValue> trackedEntityAttributeValues = Collections.emptyList();
    assertThrows(
        NullPointerException.class,
        () -> subject.toMappedRuleEnrollment(enrollmentB, trackedEntityAttributeValues));
  }

  @Test
  void testMappedEnrollment() {
    RuleEnrollment ruleEnrollment =
        subject.toMappedRuleEnrollment(enrollment, List.of(trackedEntityAttributeValue));

    assertEquals(ruleEnrollment.getEnrollment(), enrollment.getUid());
    assertEquals(ruleEnrollment.getOrganisationUnit(), enrollment.getOrganisationUnit().getUid());
    assertEquals(1, ruleEnrollment.getAttributeValues().size());
    assertEquals(SAMPLE_VALUE_A, ruleEnrollment.getAttributeValues().get(0).getValue());
  }

  @Test
  void testGetItemStore() {
    String envVariable = "Completed date";
    Constant constant = new Constant();
    constant.setValue(7.8);
    constant.setAutoFields();
    constant.setName("Gravity");
    List<Constant> constants = List.of(constant);

    when(constantService.getAllConstants()).thenReturn(constants);
    when(i18nManager.getI18n()).thenReturn(i18n);
    when(i18n.getString(anyString())).thenReturn(envVariable);

    Map<String, DataItem> itemStore =
        subject.getItemStore(
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

  private void setUpProgramRules() {
    Date now = new Date();
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

    programRuleVariableA = createProgramRuleVariable('A', program);
    programRuleVariableB = createProgramRuleVariable('B', program);
    programRuleVariableC = createProgramRuleVariable('C', program);
    programRuleVariableD = createProgramRuleVariable('D', program);

    programRuleVariableA =
        setProgramRuleVariable(
            programRuleVariableA,
            ProgramRuleVariableSourceType.CALCULATED_VALUE,
            program,
            null,
            createDataElement('D'),
            null);

    programRuleVariableB =
        setProgramRuleVariable(
            programRuleVariableB,
            ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
            program,
            null,
            null,
            attribute);

    programRuleVariableC =
        setProgramRuleVariable(
            programRuleVariableC,
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            program,
            null,
            dataElement1,
            null);

    programRuleVariableD =
        setProgramRuleVariable(
            programRuleVariableC,
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            program,
            null,
            dataElementWithOptionSet,
            null);

    programRuleVariables.add(programRuleVariableA);
    programRuleVariables.add(programRuleVariableB);
    programRuleVariables.add(programRuleVariableC);
    programRuleVariables.add(programRuleVariableD);

    programRuleA = createProgramRule('A', program);
    programRuleB = createProgramRule('B', program);
    programRuleD = createProgramRule('D', program);

    assignAction = createProgramRuleAction('I');
    sendMessageAction = createProgramRuleAction('J');
    displayText = createProgramRuleAction('D');

    assignAction =
        setProgramRuleAction(assignAction, ProgramRuleActionType.ASSIGN, "test_variable", "2+2");
    displayText =
        setProgramRuleAction(
            displayText, ProgramRuleActionType.DISPLAYTEXT, "test_variable", "2+2");
    sendMessageAction =
        setProgramRuleAction(sendMessageAction, ProgramRuleActionType.SENDMESSAGE, null, "");

    programRuleA = setProgramRule(programRuleA, "", Sets.newHashSet(assignAction, displayText), 1);
    programRuleB = setProgramRule(programRuleB, "", Sets.newHashSet(sendMessageAction), 4);
    programRuleD = setProgramRule(programRuleD, "", Sets.newHashSet(sendMessageAction), null);

    programRules.add(programRuleA);
    programRules.add(programRuleB);
    programRules.add(programRuleC);
    programRules.add(programRuleD);

    dataElement = createDataElement('D');
    dataElement.setValueType(ValueType.TEXT);
    organisationUnit = createOrganisationUnit('O');

    trackedEntityAttribute = createTrackedEntityAttribute('A', ValueType.TEXT);
    trackedEntity = createTrackedEntity('I', organisationUnit, trackedEntityAttribute);
    trackedEntityAttributeValue =
        createTrackedEntityAttributeValue('E', trackedEntity, trackedEntityAttribute);
    trackedEntityAttributeValue.setValue(SAMPLE_VALUE_A);
    trackedEntity.setTrackedEntityAttributeValues(Sets.newHashSet(trackedEntityAttributeValue));

    eventDataValueA = new EventDataValue();
    eventDataValueA.setDataElement(dataElement.getUid());
    eventDataValueA.setValue(SAMPLE_VALUE_A);
    eventDataValueA.setAutoFields();

    RuleDataValue ruleDataValue =
        new RuleDataValue(
            Instant.Companion.fromEpochMilliseconds(now.getTime()),
            programStage.getUid(),
            dataElement.getUid(),
            SAMPLE_VALUE_A);

    eventDataValueB = new EventDataValue();
    eventDataValueB.setDataElement(dataElement.getUid());
    eventDataValueB.setValue(SAMPLE_VALUE_B);
    eventDataValueB.setAutoFields();

    enrollmentB = new Enrollment(now, now, trackedEntity, program);
    enrollment = new Enrollment(now, now, trackedEntity, program);
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(now);
    enrollment.setOccurredDate(now);
    enrollment.setTrackedEntity(trackedEntity);

    eventA = new Event(enrollment, programStage);
    eventB = new Event(enrollment, programStage);
    eventC = new Event(enrollment, programStage);

    eventA.setOrganisationUnit(organisationUnit);
    eventA.setAutoFields();
    eventA.setScheduledDate(now);
    eventA.setOccurredDate(now);
    eventA.setEventDataValues(Sets.newHashSet(eventDataValueA));

    eventB.setOrganisationUnit(organisationUnit);
    eventB.setAutoFields();
    eventB.setScheduledDate(now);
    eventB.setOccurredDate(now);
    eventB.setEventDataValues(Sets.newHashSet(eventDataValueB));

    expectedRuleEvent =
        new RuleEvent(
            eventA.getUid(),
            programStage.getUid(),
            programStage.getName(),
            RuleEventStatus.valueOf(eventA.getStatus().name()),
            Instant.Companion.fromEpochMilliseconds(now.getTime()),
            LocalDateTime.Formats.INSTANCE.getISO().parse(DateUtils.toIso8601NoTz(now)).getDate(),
            null,
            organisationUnit.getUid(),
            organisationUnit.getCode(),
            List.of(ruleDataValue));
  }

  private ProgramRule setProgramRule(
      ProgramRule programRule,
      String condition,
      Set<ProgramRuleAction> ruleActions,
      Integer priority) {
    programRule.setPriority(priority);
    programRule.setCondition(condition);
    programRule.setProgramRuleActions(ruleActions);

    return programRule;
  }

  private ProgramRuleAction setProgramRuleAction(
      ProgramRuleAction programRuleActionA,
      ProgramRuleActionType type,
      String content,
      String data) {
    programRuleActionA.setProgramRuleActionType(type);

    if (type == ProgramRuleActionType.ASSIGN) {
      programRuleActionA.setContent(content);
      programRuleActionA.setData(data);
    }

    if (type == ProgramRuleActionType.DISPLAYTEXT) {
      programRuleActionA.setLocation("feedback");
      programRuleActionA.setContent("content");
      programRuleActionA.setData("true");
    }

    if (type == ProgramRuleActionType.SENDMESSAGE) {
      ProgramNotificationTemplate notificationTemplate = new ProgramNotificationTemplate();
      notificationTemplate.setUid("uid0");
      programRuleActionA.setTemplateUid(notificationTemplate.getUid());
      programRuleActionA.setData(data);
    }

    return programRuleActionA;
  }

  private ProgramRuleVariable setProgramRuleVariable(
      ProgramRuleVariable variable,
      ProgramRuleVariableSourceType sourceType,
      Program program,
      ProgramStage programStage,
      DataElement dataElement,
      TrackedEntityAttribute attribute) {
    variable.setSourceType(sourceType);
    variable.setProgram(program);
    variable.setProgramStage(programStage);
    variable.setDataElement(dataElement);
    variable.setAttribute(attribute);

    return variable;
  }
}
