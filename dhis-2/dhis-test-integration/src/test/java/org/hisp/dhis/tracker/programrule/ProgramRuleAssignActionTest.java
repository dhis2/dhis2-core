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
package org.hisp.dhis.tracker.programrule;

import static org.hisp.dhis.programrule.ProgramRuleActionType.ASSIGN;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyWarnings;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1307;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1308;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1310;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramRuleAssignActionTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private ProgramRuleService programRuleService;

  @Autowired private ProgramRuleActionService programRuleActionService;

  @Autowired private ProgramRuleVariableService programRuleVariableService;

  @Autowired private SystemSettingManager systemSettingManager;

  private Program program;

  private DataElement dataElement1;

  private DataElement dataElement2;

  private TrackedEntityAttribute attribute1;

  @Override
  public void initTest() throws IOException {
    ObjectBundle bundle = setUpMetadata("tracker/simple_metadata.json");
    program = bundle.getPreheat().get(PreheatIdentifier.UID, Program.class, "BFcipDERJnf");
    dataElement1 = bundle.getPreheat().get(PreheatIdentifier.UID, DataElement.class, "DATAEL00001");
    dataElement2 = bundle.getPreheat().get(PreheatIdentifier.UID, DataElement.class, "DATAEL00002");
    attribute1 =
        bundle.getPreheat().get(PreheatIdentifier.UID, TrackedEntityAttribute.class, "dIVt4l5vIOa");
    TrackedEntityAttribute attribute2 =
        bundle.getPreheat().get(PreheatIdentifier.UID, TrackedEntityAttribute.class, "fRGt4l6yIRb");
    ProgramRuleVariable programRuleVariable =
        createProgramRuleVariableWithDataElement('A', program, dataElement2);
    ProgramRuleVariable programRuleVariableAttribute =
        createProgramRuleVariableWithTEA('B', program, attribute2);
    programRuleVariableService.addProgramRuleVariable(programRuleVariable);
    programRuleVariableService.addProgramRuleVariable(programRuleVariableAttribute);

    ProgramRuleVariable programRuleVariablePreviousEvent =
        createProgramRuleVariableWithDataElement('C', program, dataElement1);
    programRuleVariablePreviousEvent.setSourceType(
        ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT);
    programRuleVariableService.addProgramRuleVariable(programRuleVariablePreviousEvent);

    injectAdminUser();

    trackerImportService.importTracker(
        fromJson("tracker/programrule/tei_enrollment_completed_event.json"));
  }

  @Test
  void shouldNotImportWithWarningWhenAttributeWithSameValueIsAssignedByAssignRule()
      throws IOException {
    assignProgramRule();
    TrackerImportParams params =
        fromJson("tracker/programrule/te_enrollment_update_attribute_same_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyWarnings(importReport, E1310);
  }

  @ParameterizedTest
  @CsvSource({"2024-02-10,THIRD", "2024-01-28,SECOND", "2024-01-19,FIRST"})
  void shouldImportEventAndCorrectlyAssignPreviousEventDataValue(
      String eventOccurredDate, String previousEventDataValue) throws IOException {
    TrackerImportParams params =
        fromJson("tracker/programrule/three_events_with_different_dates.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    trackerImportService.importTracker(params);

    assignPreviousEventProgramRule();

    dbmsManager.clearSession();
    dbmsManager.flushSession();

    params = fromJson("tracker/programrule/event_with_data_value.json");

    params.getEvents().get(0).setOccurredAt(DateUtils.instantFromDateAsString(eventOccurredDate));

    ImportReport importReport = trackerImportService.importTracker(params);
    assertHasOnlyWarnings(importReport, E1308);

    ProgramStageInstance event = manager.get(ProgramStageInstance.class, "D9PbzJY8bZZ");

    List<String> eventDataValues =
        event.getEventDataValues().stream()
            .filter(dv -> dv.getDataElement().equals("DATAEL00002"))
            .map(EventDataValue::getValue)
            .collect(Collectors.toList());
    assertContainsOnly(List.of(previousEventDataValue), eventDataValues);
  }

  @Test
  void
      shouldImportEventAndCorrectlyAssignPreviousEventDataValueConsideringCreateAtWhenOccurredAtIsSame()
          throws IOException {
    String firstEventUid = CodeGenerator.generateUid();
    String secondEventUid = CodeGenerator.generateUid();
    String thirdEventUid = CodeGenerator.generateUid();
    String fourthEventUid = CodeGenerator.generateUid();

    // Events are imported separately to have different createdAt
    TrackerImportParams firstEvent = getEvent(firstEventUid, "2024-01-11", "FIRST");
    trackerImportService.importTracker(firstEvent);

    TrackerImportParams fourthEvent = getEvent(fourthEventUid, "2024-01-26", "FOURTH");
    trackerImportService.importTracker(fourthEvent);

    TrackerImportParams secondEvent = getEvent(secondEventUid, "2024-01-25", "SECOND");
    trackerImportService.importTracker(secondEvent);

    TrackerImportParams thirdEvent = getEvent(thirdEventUid, "2024-01-25", "THIRD");
    trackerImportService.importTracker(thirdEvent);

    assignPreviousEventProgramRule();

    dbmsManager.clearSession();
    dbmsManager.flushSession();

    TrackerImportParams trackerImportParams =
        TrackerImportParams.builder()
            .events(
                List.of(
                    firstEvent.getEvents().get(0),
                    secondEvent.getEvents().get(0),
                    thirdEvent.getEvents().get(0),
                    fourthEvent.getEvents().get(0)))
            .build();

    ImportReport importReport = trackerImportService.importTracker(trackerImportParams);

    List<String> firstEventDataValues = getValueForAssignedDataElement(firstEventUid);
    List<String> secondEventDataValues = getValueForAssignedDataElement(secondEventUid);
    List<String> thirdEventDataValues = getValueForAssignedDataElement(thirdEventUid);
    List<String> fourthEventDataValues = getValueForAssignedDataElement(fourthEventUid);

    Assertions.assertAll(
        () -> assertHasOnlyWarnings(importReport, E1308, E1308, E1308, E1308),
        () -> assertIsEmpty(firstEventDataValues),
        () -> assertContainsOnly(List.of("FIRST"), secondEventDataValues),
        () -> assertContainsOnly(List.of("SECOND"), thirdEventDataValues),
        () -> assertContainsOnly(List.of("THIRD"), fourthEventDataValues));
  }

  @Test
  void shouldImportWithWarningWhenDataElementWithSameValueIsAssignedByAssignRule()
      throws IOException {
    assignProgramRule();
    dbmsManager.clearSession();
    dbmsManager.flushSession();
    TrackerImportParams params =
        fromJson("tracker/programrule/event_update_datavalue_same_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyWarnings(importReport, E1308);
  }

  @Test
  void shouldNotImportWhenDataElementWithDifferentValueIsAssignedByAssignRule() throws IOException {
    assignProgramRule();
    TrackerImportParams params =
        fromJson("tracker/programrule/event_update_datavalue_different_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E1307);
  }

  @Test
  void
      shouldImportWithWarningWhenDataElementWithDifferentValueIsAssignedByAssignRuleAndOverwriteKeyIsTrue()
          throws IOException {
    assignProgramRule();
    systemSettingManager.saveSystemSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE, true);
    TrackerImportParams params =
        fromJson("tracker/programrule/event_update_datavalue_different_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyWarnings(importReport, E1308);
  }

  @Test
  void
      shouldImportWithWarningWhenDataElementWithDifferentAndEmptyValueIsAssignedByAssignRuleAndOverwriteKeyIsTrue()
          throws IOException {
    assignProgramRule();
    systemSettingManager.saveSystemSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE, true);
    dbmsManager.clearSession();
    dbmsManager.flushSession();
    TrackerImportParams params =
        fromJson("tracker/programrule/event_update_datavalue_empty_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyWarnings(importReport, E1308);
  }

  private TrackerImportParams getEvent(String eventUid, String occurredDate, String value)
      throws IOException {
    TrackerImportParams trackerImportParams =
        fromJson("tracker/programrule/event_without_date.json");
    trackerImportParams
        .getEvents()
        .get(0)
        .setOccurredAt(DateUtils.instantFromDateAsString(occurredDate));
    trackerImportParams.getEvents().get(0).setEvent(eventUid);
    trackerImportParams.getEvents().get(0).getDataValues().iterator().next().setValue(value);

    return trackerImportParams;
  }

  private List<String> getValueForAssignedDataElement(String eventUid) {
    return manager.get(ProgramStageInstance.class, eventUid).getEventDataValues().stream()
        .filter(dv -> dv.getDataElement().equals("DATAEL00002"))
        .map(EventDataValue::getValue)
        .collect(Collectors.toList());
  }

  private void assignProgramRule() {
    ProgramRule programRule = createProgramRule('F', program, null, "true");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, ASSIGN, dataElement1, "#{ProgramRuleVariableA}");
    ProgramRuleAction programRuleActionAttribute =
        createProgramRuleAction(programRule, ASSIGN, attribute1, "#{ProgramRuleVariableB}");
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRuleActionService.addProgramRuleAction(programRuleActionAttribute);
    programRule.getProgramRuleActions().add(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleActionAttribute);
    programRuleService.updateProgramRule(programRule);
  }

  private void assignPreviousEventProgramRule() {
    ProgramRule programRule = createProgramRule('G', program, null, "true");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, ASSIGN, dataElement2, "#{ProgramRuleVariableC}");
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleAction);
    programRuleService.updateProgramRule(programRule);
  }

  private ProgramRule createProgramRule(
      char uniqueCharacter, Program program, ProgramStage programStage, String condition) {
    ProgramRule programRule = createProgramRule(uniqueCharacter, program);
    programRule.setUid("ProgramRul" + uniqueCharacter);
    programRule.setProgramStage(programStage);
    programRule.setCondition(condition);
    return programRule;
  }

  private ProgramRuleAction createProgramRuleAction(
      ProgramRule programRule,
      ProgramRuleActionType actionType,
      DataElement dataElement,
      String data) {
    ProgramRuleAction programRuleAction = createProgramRuleAction('A', programRule);
    programRuleAction.setProgramRuleActionType(actionType);
    programRuleAction.setContent("CONTENT");
    programRuleAction.setDataElement(dataElement);
    programRuleAction.setData(data);

    return programRuleAction;
  }

  private ProgramRuleAction createProgramRuleAction(
      ProgramRule programRule,
      ProgramRuleActionType actionType,
      TrackedEntityAttribute attribute,
      String data) {
    ProgramRuleAction programRuleAction = createProgramRuleAction('A', programRule);
    programRuleAction.setProgramRuleActionType(actionType);
    programRuleAction.setContent("CONTENT");
    programRuleAction.setAttribute(attribute);
    programRuleAction.setData(data);

    return programRuleAction;
  }
}
