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

import static org.hisp.dhis.programrule.ProgramRuleActionType.SHOWERROR;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SHOWWARNING;
import static org.hisp.dhis.tracker.Assertions.assertHasError;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyWarnings;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrorsAndNoWarnings;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1300;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgramRuleTest extends PostgresIntegrationTestBase {
  private static final UID ENROLLMENT_UID = UID.of("TvctPPhpD8u");

  private static final UID EVENT_UID = UID.of("D9PbzJY8bJO");

  private static final UID PROGRAM_EVENT_UID = UID.of("PEVENT12345");

  @Autowired private TestSetup testSetup;
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private ProgramRuleService programRuleService;

  @Autowired private ProgramRuleActionService programRuleActionService;

  @Autowired private ProgramRuleVariableService programRuleVariableService;

  @Autowired private ConstantService constantService;

  private Program program;

  private Program programWithoutRegistration;

  private DataElement dataElement1;

  private ProgramStage programStageOnInsert;

  private ProgramStage programStageOnComplete;

  @BeforeAll
  void setUp() throws IOException {
    ObjectBundle bundle = testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    program = bundle.getPreheat().get(PreheatIdentifier.UID, Program.class, "BFcipDERJnf");
    programWithoutRegistration =
        bundle.getPreheat().get(PreheatIdentifier.UID, Program.class, "BFcipDERJne");
    dataElement1 = bundle.getPreheat().get(PreheatIdentifier.UID, DataElement.class, "DATAEL00001");
    DataElement dataElement2 =
        bundle.getPreheat().get(PreheatIdentifier.UID, DataElement.class, "DATAEL00002");
    TrackedEntityAttribute trackedEntityAttribute =
        bundle.getPreheat().get(PreheatIdentifier.UID, TrackedEntityAttribute.class, "dIVt4l5vIOa");
    programStageOnInsert =
        bundle.getPreheat().get(PreheatIdentifier.UID, ProgramStage.class, "NpsdDv6kKSO");
    programStageOnComplete =
        bundle.getPreheat().get(PreheatIdentifier.UID, ProgramStage.class, "NpsdDv6kKS2");
    ProgramRuleVariable programRuleVariable =
        createProgramRuleVariableWithDataElement('A', program, dataElement2);
    ProgramRuleVariable programRuleVariable2 =
        createProgramRuleVariableWithTEA('B', program, trackedEntityAttribute);
    programRuleVariableService.addProgramRuleVariable(programRuleVariable);
    programRuleVariableService.addProgramRuleVariable(programRuleVariable2);
    constantService.saveConstant(constant());
  }

  @Test
  void shouldImportEnrollmentWithNoWarningsWhenThereAreNoProgramRules() throws IOException {
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_enrollment.json"));

    assertNoErrorsAndNoWarnings(report);
  }

  @Test
  void shouldImportEnrollmentWithWarningsWhenAWarningIsTriggered() throws IOException {
    alwaysTrueWarningProgramRule();
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_enrollment.json"));

    assertHasOnlyWarnings(report, E1300);
  }

  @Test
  void shouldNotImportEnrollmentWhenAnErrorIsTriggered() throws IOException {
    alwaysTrueErrorProgramRule();
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_enrollment.json"));

    assertHasOnlyErrors(report, E1300);
  }

  @Test
  void shouldImportSingleEventWithWarningsWhenAWarningIsTriggered() throws IOException {
    alwaysTrueWarningSingleEventProgramRule();
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/program_event.json"));

    assertHasOnlyWarnings(report, E1300);
  }

  @Test
  void shouldNotImportSingleEventWhenAnErrorIsTriggeredBasedOnConditionEvaluatingAConstant()
      throws IOException {
    conditionWithConstantEvaluatesToTrue();
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/program_event.json"));

    assertHasOnlyErrors(report, E1300);
  }

  @Test
  void shouldNotImportSingleEventWhenAnErrorIsTriggered() throws IOException {
    alwaysTrueErrorSingleEventProgramRule();
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/program_event.json"));

    assertHasOnlyErrors(report, E1300);
  }

  @Test
  void shouldImportEventWithNoWarningsWhenThereAreNoProgramRules() throws IOException {
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_enrollment_completed_event.json"));

    assertNoErrorsAndNoWarnings(report);
  }

  @Test
  void shouldImportEventWithWarningsWhenAWarningIsTriggered() throws IOException {
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_enrollment.json"));
    assertNoErrors(report);

    alwaysTrueWarningProgramRule();
    report =
        trackerImportService.importTracker(
            new TrackerImportParams(), testSetup.fromJson("tracker/programrule/event.json"));

    assertHasOnlyWarnings(report, E1300);
  }

  @Test
  void shouldNotImportEventWhenAnErrorIsTriggered() throws IOException {
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_enrollment.json"));
    assertNoErrors(report);

    alwaysTrueErrorProgramRule();
    report =
        trackerImportService.importTracker(
            new TrackerImportParams(), testSetup.fromJson("tracker/programrule/event.json"));

    assertHasOnlyErrors(report, E1300);
  }

  @Test
  void shouldNotValidateEventAndValidateEnrollmentWhenAnErrorIsTriggeredOnEvent()
      throws IOException {
    onCompleteErrorProgramRule();
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_enrollment_completed_event.json"));

    assertHasOnlyErrors(report, E1300);
  }

  @Test
  void shouldNotImportEventAndEnrollmentWhenAnErrorIsTriggeredOnEnrollment() throws IOException {
    onCompleteErrorProgramRule();
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_completed_enrollment_event.json"));

    assertAll(
        () -> assertHasError(report, E1300, ENROLLMENT_UID),
        () -> assertHasError(report, ValidationCode.E5000, EVENT_UID));
  }

  @Test
  void shouldImportEventWhenAnErrorIsTriggeredOnEnrollmentAlreadyPresentInDB() throws IOException {
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_completed_enrollment.json"));
    assertNoErrorsAndNoWarnings(report);

    onCompleteErrorProgramRule();
    report =
        trackerImportService.importTracker(
            new TrackerImportParams(), testSetup.fromJson("tracker/programrule/event.json"));

    assertNoErrorsAndNoWarnings(report);
  }

  @Test
  void shouldNotImportWhenErrorIsTriggeredOnAllEntities() throws IOException {
    alwaysTrueErrorProgramRule();
    alwaysTrueErrorSingleEventProgramRule();
    ImportReport report =
        trackerImportService.importTracker(
            new TrackerImportParams(),
            testSetup.fromJson("tracker/programrule/te_enrollment_event_programevent.json"));

    assertAll(
        () -> assertHasError(report, E1300, ENROLLMENT_UID),
        () -> assertHasError(report, E1300, EVENT_UID),
        () -> assertHasError(report, E1300, PROGRAM_EVENT_UID));
  }

  @Test
  void shouldImportWithWarningWhenARuleWithASyntaxErrorIsTriggered() throws IOException {
    syntaxErrorRule();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/programrule/te_enrollment.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertHasOnlyWarnings(importReport, E1300);
  }

  @Test
  void shouldImportWithWarningWhenAWarningIsTriggeredOnEventInSameProgramStage()
      throws IOException {
    programStageWarningRule();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/programrule/te_enrollment_completed_event.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertHasOnlyWarnings(importReport, E1300);
  }

  @Test
  void shouldImportWithNoWarningsWhenAWarningIsTriggeredOnEventInDifferentProgramStage()
      throws IOException {
    programStageWarningRule();
    TrackerObjects trackerObjects =
        testSetup.fromJson(
            "tracker/programrule/te_enrollment_completed_event_from_another_program_stage.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertNoErrorsAndNoWarnings(importReport);
  }

  @Test
  void shouldImportWithNoWarningsWhenAWarningIsTriggeredOnActiveEventInOnCompleteProgramStage()
      throws IOException {
    programStage2WarningRule();
    TrackerObjects trackerObjects =
        testSetup.fromJson(
            "tracker/programrule/te_enrollment_event_from_another_program_stage.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertNoErrorsAndNoWarnings(importReport);
  }

  @Test
  void shouldImportWithWarningsWhenAWarningIsTriggeredOnCompletedEventInOnCompleteProgramStage()
      throws IOException {
    programStage2WarningRule();
    TrackerObjects trackerObjects =
        testSetup.fromJson(
            "tracker/programrule/te_enrollment_completed_event_from_another_program_stage.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertHasOnlyWarnings(importReport, E1300);
  }

  @Test
  void shouldImportWithNoWarningsWhenAWarningIsTriggeredWithADataElementFromADifferentProgramStage()
      throws IOException {
    programStage2WrongDataElementWarningRule();
    TrackerObjects trackerObjects =
        testSetup.fromJson(
            "tracker/programrule/te_enrollment_completed_event_from_another_program_stage.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertNoErrorsAndNoWarnings(importReport);
  }

  @Test
  void shouldNotImportWithWhenDataElementHasValue() throws IOException {
    showErrorWhenVariableHasValueRule();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/programrule/te_completed_enrollment_event.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertHasOnlyErrors(importReport, E1300);
  }

  @Test
  void shouldImportWhenTEAHasNullValue() throws IOException {
    errorProgramRuleWithD2HasValue();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/programrule/te_enrollment_completed_event.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertNoErrors(importReport.getValidationReport());
  }

  @Test
  void shouldImportWithNoWarningsWhenDataElementHasNoValue() throws IOException {
    showErrorWhenVariableHasValueRule();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/programrule/te_enrollment_event_with_no_data_value.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertNoErrorsAndNoWarnings(importReport);
  }

  @Test
  void shouldImportWithNoWarningsWhenDataElementHasNullValue() throws IOException {
    showErrorWhenVariableHasValueRule();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/programrule/te_enrollment_event_with_null_data_value.json");

    ImportReport importReport =
        trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertNoErrorsAndNoWarnings(importReport);
  }

  private void alwaysTrueErrorProgramRule() {
    storeProgramRule('A', program, ProgramRuleActionType.SHOWERROR);
  }

  private void onCompleteErrorProgramRule() {
    storeProgramRule('B', program, ProgramRuleActionType.ERRORONCOMPLETE);
  }

  private void alwaysTrueWarningProgramRule() {
    storeProgramRule('C', program, SHOWWARNING);
  }

  private void alwaysTrueWarningSingleEventProgramRule() {
    storeProgramRule('D', programWithoutRegistration, SHOWWARNING);
  }

  private void alwaysTrueErrorSingleEventProgramRule() {
    storeProgramRule('E', programWithoutRegistration, ProgramRuleActionType.SHOWERROR);
  }

  private void programStageWarningRule() {
    storeProgramRule('G', program, programStageOnInsert, SHOWWARNING);
  }

  private void syntaxErrorRule() {
    ProgramRule programRule = createProgramRule('H', program, null, "SYNTAX ERROR");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, SHOWERROR, null, null);
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleAction);
    programRuleService.updateProgramRule(programRule);
  }

  private void programStage2WarningRule() {
    storeProgramRule('I', program, programStageOnComplete, SHOWWARNING);
  }

  private void programStage2WrongDataElementWarningRule() {
    ProgramRule programRule = createProgramRule('J', program, programStageOnComplete, "true");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, SHOWWARNING, dataElement1, null);
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleAction);
    programRuleService.updateProgramRule(programRule);
  }

  private void errorProgramRuleWithD2HasValue() {
    ProgramRule programRule =
        createProgramRule('H', program, null, "d2:hasValue(#{ProgramRuleVariableB})");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, SHOWERROR, null, null);
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleAction);
    programRuleService.updateProgramRule(programRule);
  }

  private void showErrorWhenVariableHasValueRule() {
    ProgramRule programRule =
        createProgramRule(
            'K', program, programStageOnInsert, "d2:hasValue(#{ProgramRuleVariableA})");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, SHOWERROR, null, null);
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleAction);
    programRuleService.updateProgramRule(programRule);
  }

  private void conditionWithConstantEvaluatesToTrue() {
    ProgramRule programRule =
        createProgramRule('L', programWithoutRegistration, null, "C{NAgjOfWMXg6} < 10");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, SHOWERROR, null, null);
    programRuleActionService.addProgramRuleAction(programRuleAction);
    programRule.getProgramRuleActions().add(programRuleAction);
    programRuleService.updateProgramRule(programRule);
  }

  private void storeProgramRule(
      char uniqueCharacter, Program program, ProgramRuleActionType actionType) {
    storeProgramRule(uniqueCharacter, program, null, actionType);
  }

  private void storeProgramRule(
      char uniqueCharacter,
      Program program,
      ProgramStage programStage,
      ProgramRuleActionType actionType) {
    ProgramRule programRule = createProgramRule(uniqueCharacter, program, programStage, "true");
    programRuleService.addProgramRule(programRule);
    ProgramRuleAction programRuleAction =
        createProgramRuleAction(programRule, actionType, null, null);
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

  private Constant constant() {
    Constant constant = new Constant();
    constant.setValue(7.8);
    constant.setUid("NAgjOfWMXg6");
    constant.setName("Gravity");
    constant.setShortName("Gravity");
    return constant;
  }
}
