/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule;

import static org.hisp.dhis.programrule.ProgramRuleActionType.ASSIGN;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyWarnings;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1125;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1307;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1308;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1310;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
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
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramRuleAssignActionTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private ProgramRuleService programRuleService;

  @Autowired private ProgramRuleActionService programRuleActionService;

  @Autowired private ProgramRuleVariableService programRuleVariableService;

  @Autowired private SystemSettingManager systemSettingManager;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  private Program program;

  private DataElement dataElement1;

  private DataElement dataElement2;

  private DataElement optionDataElement;

  /** Not a data element of program stage {@code NpsdDv6kKSO}. */
  private DataElement dataElementNotInStage;

  private TrackedEntityAttribute attribute1;

  @Autowired protected UserService _userService;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    ObjectBundle bundle = setUpMetadata("tracker/simple_metadata.json");
    program = bundle.getPreheat().get(PreheatIdentifier.UID, Program.class, "BFcipDERJnf");
    dataElement1 = bundle.getPreheat().get(PreheatIdentifier.UID, DataElement.class, "DATAEL00001");
    dataElement2 = bundle.getPreheat().get(PreheatIdentifier.UID, DataElement.class, "DATAEL00002");
    optionDataElement =
        bundle.getPreheat().get(PreheatIdentifier.UID, DataElement.class, "DATAEL00005");
    dataElementNotInStage =
        bundle.getPreheat().get(PreheatIdentifier.UID, DataElement.class, "GieVkTxp4HG");
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

    injectAdminUser();

    ProgramRuleVariable programRuleVariablePreviousEvent =
        createProgramRuleVariableWithDataElement('C', program, dataElement1);
    programRuleVariablePreviousEvent.setSourceType(
        ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT);
    programRuleVariableService.addProgramRuleVariable(programRuleVariablePreviousEvent);

    trackerImportService.importTracker(
        new TrackerImportParams(),
        fromJson("tracker/programrule/tei_enrollment_completed_event.json"));
  }

  @Test
  void shouldNotImportWithWarningWhenAttributeWithSameValueIsAssignedByAssignRule()
      throws IOException {
    assignProgramRule();
    TrackerImportParams params = new TrackerImportParams();
    TrackerObjects trackerObjects =
        fromJson("tracker/programrule/te_enrollment_update_attribute_same_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyWarnings(importReport, E1310);
  }

  @Test
  void shouldImportWhenAttributeUsedToCalculateAssignedValueIsUpdatedInEnrollment()
      throws IOException {
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, "IOR1AXXl24H");
    TrackedEntityAttribute attribute2 = manager.get(TrackedEntityAttribute.class, "fRGt4l6yIRb");
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(attribute1, trackedEntity, "Tom"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(attribute2, trackedEntity, "Tom"));
    assignProgramRule();
    dbmsManager.clearSession();

    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    TrackerObjects trackerObjects =
        fromJson("tracker/programrule/te_enrollment_update_attribute_different_value.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyWarnings(importReport, E1310);
    List<String> attributeValues =
        manager.get(TrackedEntity.class, "IOR1AXXl24H").getTrackedEntityAttributeValues().stream()
            .filter(av -> av.getAttribute().getUid().equals("dIVt4l5vIOa"))
            .map(TrackedEntityAttributeValue::getValue)
            .toList();
    assertContainsOnly(List.of("John"), attributeValues);
  }

  @ParameterizedTest
  @CsvSource({"2024-02-10,THIRD", "2024-01-28,SECOND", "2024-01-19,FIRST"})
  void shouldImportEventAndCorrectlyAssignPreviousEventDataValue(
      String eventOccurredDate, String previousEventDataValue) throws IOException {
    TrackerImportParams params = new TrackerImportParams();
    TrackerObjects trackerObjects =
        fromJson("tracker/programrule/three_events_with_different_dates.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    trackerImportService.importTracker(params, trackerObjects);

    assignPreviousEventProgramRule();

    trackerObjects = fromJson("tracker/programrule/event_with_data_value.json");

    trackerObjects
        .getEvents()
        .get(0)
        .setOccurredAt(DateUtils.instantFromDateAsString(eventOccurredDate));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertHasOnlyWarnings(importReport, E1308);

    Event event = manager.get(Event.class, "D9PbzJY8bZZ");

    List<String> eventDataValues =
        event.getEventDataValues().stream()
            .filter(dv -> dv.getDataElement().equals("DATAEL00002"))
            .map(EventDataValue::getValue)
            .toList();
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
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    // Events are imported separately to have different createdAt
    TrackerObjects firstEvent = getEvent(firstEventUid, "2024-01-11", "FIRST");
    trackerImportService.importTracker(params, firstEvent);

    TrackerObjects fourthEvent = getEvent(fourthEventUid, "2024-01-26", "FOURTH");
    trackerImportService.importTracker(params, fourthEvent);

    TrackerObjects secondEvent = getEvent(secondEventUid, "2024-01-25", "SECOND");
    trackerImportService.importTracker(params, secondEvent);

    TrackerObjects thirdEvent = getEvent(thirdEventUid, "2024-01-25", "THIRD");
    trackerImportService.importTracker(params, thirdEvent);

    assignPreviousEventProgramRule();

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .events(
                List.of(
                    firstEvent.getEvents().get(0),
                    secondEvent.getEvents().get(0),
                    thirdEvent.getEvents().get(0),
                    fourthEvent.getEvents().get(0)))
            .build();

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

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
    TrackerImportParams params = new TrackerImportParams();
    TrackerObjects trackerObjects =
        fromJson("tracker/programrule/event_update_datavalue_same_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyWarnings(importReport, E1308);
  }

  @Test
  void shouldNotImportWhenDataElementWithDifferentValueIsAssignedByAssignRule() throws IOException {
    assignProgramRule();
    TrackerImportParams params = new TrackerImportParams();
    TrackerObjects trackerObjects =
        fromJson("tracker/programrule/event_update_datavalue_different_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E1307);
  }

  @Test
  void
      shouldImportWithWarningWhenDataElementWithDifferentValueIsAssignedByAssignRuleAndOverwriteKeyIsTrue()
          throws IOException {
    assignProgramRule();
    systemSettingManager.saveSystemSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE, true);
    TrackerImportParams params = new TrackerImportParams();
    TrackerObjects trackerObjects =
        fromJson("tracker/programrule/event_update_datavalue_different_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyWarnings(importReport, E1308);
  }

  @ParameterizedTest
  @MethodSource("getOptions")
  void shouldImportWithWarningWhenDataElementOfTypeOptionWithValidValueIsAssignedByAssignRule(
      String option, boolean hasValidValue) throws IOException {
    assignOptionProgramRule(option);
    TrackerObjects trackerObjects =
        fromJson("tracker/programrule/event_update_datavalue_different_value.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertHasOnlyWarnings(importReport, E1308);
    if (!hasValidValue) {
      assertHasOnlyErrors(importReport, E1125);
    } else {
      assertNoErrors(importReport);
    }
  }

  @Test
  void
      shouldImportWithWarningWhenDataElementWithDifferentAndEmptyValueIsAssignedByAssignRuleAndOverwriteKeyIsTrue()
          throws IOException {
    assignProgramRule();
    systemSettingManager.saveSystemSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE, true);
    TrackerImportParams params = new TrackerImportParams();
    TrackerObjects trackerObjects =
        fromJson("tracker/programrule/event_update_datavalue_empty_value.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyWarnings(importReport, E1308);
  }

  /**
   * The rule engine runs after validation and {@code DataValuesValidator} runs again afterwards, on
   * the payload the ASSIGN mutated. An ASSIGN to a data element absent from the payload adds a data
   * value, so that second pass sees a data element the first pass never did.
   *
   * <p>Nothing rejects it because the data elements of the program stage are projected from the
   * payload's data elements plus the data elements of all program rule actions, which {@link
   * org.hisp.dhis.tracker.imports.TrackerIdentifierCollector} preheats. Narrowing that collection
   * would leave the assigned data element out of the projection and E1305 would reject valid data.
   */
  @Test
  void shouldImportWithWarningWhenAssignedDataElementIsNotInThePayload() throws IOException {
    // DATAEL00002 belongs to program stage NpsdDv6kKSO but the payload only carries DATAEL00001
    assignConstantToDataElementProgramRule();
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params, eventWithDataValue());

    // E1305 would be reported here if the assigned data element were not part of the projection
    Assertions.assertAll(
        () -> assertNoErrors(importReport),
        () -> assertHasOnlyWarnings(importReport, E1308),
        () ->
            assertContainsOnly(List.of("ASSIGNED"), getValueForAssignedDataElement("D9PbzJY8bZZ")));
  }

  /**
   * A rule effect is only executed if its target data element belongs to the event's program stage,
   * a check {@code RuleActionEventMapper} makes against the projected data elements. A data element
   * outside the stage must not be assigned.
   */
  @Test
  void shouldNotAssignWhenDataElementIsNotPartOfTheProgramStage() throws IOException {
    // GieVkTxp4HG is not a data element of program stage NpsdDv6kKSO
    assignConstantToDataElementNotInStageProgramRule();
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport importReport = trackerImportService.importTracker(params, eventWithDataValue());

    dbmsManager.clearSession();

    Assertions.assertAll(
        () -> assertNoErrors(importReport),
        () -> assertIsEmpty(getValueForDataElement("D9PbzJY8bZZ", dataElementNotInStage.getUid())));
  }

  private TrackerObjects getEvent(String eventUid, String occurredDate, String value)
      throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/programrule/event_without_date.json");
    trackerObjects
        .getEvents()
        .get(0)
        .setOccurredAt(DateUtils.instantFromDateAsString(occurredDate));
    trackerObjects.getEvents().get(0).setEvent(eventUid);
    trackerObjects.getEvents().get(0).getDataValues().iterator().next().setValue(value);

    return trackerObjects;
  }

  /**
   * Event {@code D9PbzJY8bZZ} on program stage {@code NpsdDv6kKSO}, carrying a data value for
   * {@code DATAEL00001} only. The payload has no {@code occurredAt}, which E1031 rejects, so one is
   * set here. Its value does not matter to the callers.
   */
  private TrackerObjects eventWithDataValue() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/programrule/event_with_data_value.json");
    org.hisp.dhis.tracker.imports.domain.Event event = trackerObjects.getEvents().get(0);
    event.setOccurredAt(DateUtils.instantFromDateAsString("2019-01-28"));

    return TrackerObjects.builder().events(List.of(event)).build();
  }

  private List<String> getValueForAssignedDataElement(String eventUid) {
    return getValueForDataElement(eventUid, "DATAEL00002");
  }

  private List<String> getValueForDataElement(String eventUid, String dataElementUid) {
    return manager.get(Event.class, eventUid).getEventDataValues().stream()
        .filter(dv -> dv.getDataElement().equals(dataElementUid))
        .map(EventDataValue::getValue)
        .toList();
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

  private void assignOptionProgramRule(String option) {
    ProgramRule programRule = createProgramRule('O', program, null, "true");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, ASSIGN, optionDataElement, option);
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleAction);
    programRuleService.updateProgramRule(programRule);
  }

  /** Assigns a constant to a data element of the program stage that the payload does not carry. */
  private void assignConstantToDataElementProgramRule() {
    ProgramRule programRule = createProgramRule('H', program, null, "true");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, ASSIGN, dataElement2, "'ASSIGNED'");
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleAction);
    programRuleService.updateProgramRule(programRule);
  }

  /** Assigns a constant to a data element that is not part of the event's program stage. */
  private void assignConstantToDataElementNotInStageProgramRule() {
    ProgramRule programRule = createProgramRule('J', program, null, "true");
    programRuleService.addProgramRule(programRule);
    // GieVkTxp4HG is a NUMBER data element
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, ASSIGN, dataElementNotInStage, "1");
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleAction);
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

  public Stream<Arguments> getOptions() {
    return Stream.of(
        Arguments.of("\"option1\"", true),
        Arguments.of(null, true),
        Arguments.of("\"invalidOption\"", false));
  }
}
