/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandStore;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionService;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataElementMergeProcessorTest extends TransactionalIntegrationTest {

  @Autowired private DataElementService dataElementService;
  @Autowired private DataElementMergeProcessor mergeProcessor;
  @Autowired private MinMaxDataElementService minMaxDataElementService;
  @Autowired private EventVisualizationService eventVisualizationService;
  @Autowired private OrganisationUnitService orgUnitService;
  @Autowired private SMSCommandService smsCommandService;
  @Autowired private PredictorService predictorService;
  @Autowired private CategoryService categoryService;
  @Autowired private ProgramStageDataElementService programStageDataElementService;
  @Autowired private ProgramStageSectionService programStageSectionService;
  @Autowired private ProgramNotificationTemplateService programNotificationTemplateService;
  @Autowired private ProgramRuleVariableService programRuleVariableService;
  @Autowired private ProgramRuleActionService programRuleActionService;
  @Autowired private IdentifiableObjectManager identifiableObjectManager;
  @Autowired private DataElementOperandStore dataElementOperandStore;
  @Autowired private DataSetStore dataSetStore;
  @Autowired private SectionService sectionService;

  private DataElement deSource1;
  private DataElement deSource2;
  private DataElement deTarget;
  private OrganisationUnit ou1;
  private OrganisationUnit ou2;
  private OrganisationUnit ou3;
  private OrganisationUnitLevel oul;
  private CategoryOptionCombo coc1;

  @BeforeEach
  public void setUp() {
    // data elements
    deSource1 = createDataElement('A');
    deSource2 = createDataElement('B');
    deTarget = createDataElement('C');
    dataElementService.addDataElement(deSource1);
    dataElementService.addDataElement(deSource2);
    dataElementService.addDataElement(deTarget);

    // org unit
    ou1 = createOrganisationUnit('A');
    ou2 = createOrganisationUnit('B');
    ou3 = createOrganisationUnit('C');
    orgUnitService.addOrganisationUnit(ou1);
    orgUnitService.addOrganisationUnit(ou2);
    orgUnitService.addOrganisationUnit(ou3);

    oul = new OrganisationUnitLevel(1, "Level 1");
    orgUnitService.addOrganisationUnitLevel(oul);

    // cat option combo
    coc1 = categoryService.getDefaultCategoryOptionCombo();
    categoryService.addCategoryOptionCombo(coc1);
  }

  @Test
  @DisplayName("Ensure setup data is present in system")
  void ensureDataIsPresentInSystem() {
    // given setup is complete
    // when trying to retrieve data
    List<DataElement> dataElements = dataElementService.getAllDataElements();
    List<OrganisationUnit> orgUnits = orgUnitService.getAllOrganisationUnits();

    // then
    assertEquals(3, dataElements.size());
    assertEquals(3, orgUnits.size());
  }

  // -------------------------------
  // ---- MIN MAX DATA ELEMENTS ----
  // -------------------------------
  @Test
  @DisplayName(
      "MinMaxDataElement references for DataElement are replaced as expected, source DataElements are not deleted")
  void minMaxDataElementMergeTest() throws ConflictException {
    // given
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(deSource1, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(deSource2, ou2, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(deTarget, ou3, coc1, 0, 100, false);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement1);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement2);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<MinMaxDataElement> minMaxSources =
        minMaxDataElementService.getAllByDataElement(List.of(deSource1, deSource2));
    List<MinMaxDataElement> minMaxTarget =
        minMaxDataElementService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, minMaxSources, minMaxTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "MinMaxDataElement references for DataElement are replaced as expected, source DataElements are deleted")
  void minMaxDataElementMergeDeleteSourcesTest() throws ConflictException {
    // given
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(deSource1, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(deSource2, ou2, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(deTarget, ou3, coc1, 0, 100, false);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement1);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement2);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<MinMaxDataElement> minMaxSources =
        minMaxDataElementService.getAllByDataElement(List.of(deSource1, deSource2));
    List<MinMaxDataElement> minMaxTarget =
        minMaxDataElementService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, minMaxSources, minMaxTarget, allDataElements);
  }

  @Test
  @DisplayName("MinMaxDataElements DB constraint error when updating")
  void testMinMaxDataElementMergeDbConstraint() {
    // given unique key DB constraint exists (orgUnit, dataElement, catOptionCombo)
    // create min max data elements all of which have the same org unit and cat option combo
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(deSource1, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(deSource2, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(deTarget, ou1, coc1, 0, 100, false);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement1);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement2);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when merge operation encounters DB constraint
    PersistenceException persistenceException =
        assertThrows(PersistenceException.class, () -> mergeProcessor.processMerge(mergeParams));
    assertNotNull(persistenceException.getMessage());

    // then DB constraint is thrown
    List<String> expectedStrings =
        List.of(
            "duplicate key value violates unique constraint",
            "minmaxdataelement_unique_key",
            "Detail: Key (sourceid, dataelementid, categoryoptioncomboid)",
            "already exists");

    assertTrue(
        expectedStrings.stream()
            .allMatch(
                exp -> persistenceException.getCause().getCause().getMessage().contains(exp)));
  }

  // -------------------------------
  // ---- EVENT VISUALIZATIONS ----
  // -------------------------------
  @Test
  @DisplayName(
      "EventVisualization references for DataElement are replaced as expected, source DataElements are not ")
  void eventVisualizationMergeTest() throws ConflictException {
    // given
    // event visualizations
    EventVisualization eventVis1 = createEventVisualization('1', null);
    eventVis1.setDataElementValueDimension(deTarget);
    EventVisualization eventVis2 = createEventVisualization('2', null);
    eventVis2.setDataElementValueDimension(deSource1);
    EventVisualization eventVis3 = createEventVisualization('3', null);
    eventVis3.setDataElementValueDimension(deSource2);

    eventVisualizationService.save(eventVis1);
    eventVisualizationService.save(eventVis2);
    eventVisualizationService.save(eventVis3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<EventVisualization> eventVizSources =
        eventVisualizationService.getAllByDataElement(List.of(deSource1, deSource2));
    List<EventVisualization> eventVizTarget =
        eventVisualizationService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(
        report, eventVizSources, eventVizTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "EventVisualization references for DataElement are replaced as expected, source DataElements are deleted")
  void eventVisualizationMergeDeleteSourcesTest() throws ConflictException {
    // given
    // min max data elements
    EventVisualization eventVis4 = createEventVisualization('4', null);
    eventVis4.setDataElementValueDimension(deTarget);
    EventVisualization eventVis5 = createEventVisualization('5', null);
    eventVis5.setDataElementValueDimension(deSource1);
    EventVisualization eventVis6 = createEventVisualization('6', null);
    eventVis6.setDataElementValueDimension(deSource2);

    eventVisualizationService.save(eventVis4);
    eventVisualizationService.save(eventVis5);
    eventVisualizationService.save(eventVis6);

    // params
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(deSource1.getUid(), deSource2.getUid())));
    mergeParams.setTarget(UID.of(deTarget.getUid()));
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<EventVisualization> eventVizSources =
        eventVisualizationService.getAllByDataElement(List.of(deSource1, deSource2));
    List<EventVisualization> eventVizTarget =
        eventVisualizationService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, eventVizSources, eventVizTarget, allDataElements);
  }

  // -------------------------------
  // ---- SMS CODES ----
  // -------------------------------
  @Test
  @DisplayName(
      "SMS Code references for DataElement are replaced as expected, source DataElements are not deleted")
  void smsCodeMergeTest() throws ConflictException {
    // given
    // sms codes
    SMSCode smsCode1 = createSmsCode("code source 1", deSource1);
    SMSCode smsCode2 = createSmsCode("code source 2", deSource2);
    SMSCode smsCode3 = createSmsCode("code target 3", deTarget);

    SMSCommand smsCommand = new SMSCommand();
    smsCommand.setName("CMD 1");
    smsCommand.setCodes(Set.of(smsCode1, smsCode2, smsCode3));

    smsCommandService.save(smsCommand);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<SMSCode> smsCommandSources =
        smsCommandService.getSmsCodesByDataElement(List.of(deSource1, deSource2));
    List<SMSCode> smsCommandTarget = smsCommandService.getSmsCodesByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(
        report, smsCommandSources, smsCommandTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "SMS Code references for DataElement are replaced as expected, source DataElements are deleted")
  void smsCodesMergeDeleteSourcesTest() throws ConflictException {
    SMSCode smsCode1 = createSmsCode("code source 1", deSource1);
    SMSCode smsCode2 = createSmsCode("code source 2", deSource2);
    SMSCode smsCode3 = createSmsCode("code target 3", deTarget);

    SMSCommand smsCommand = new SMSCommand();
    smsCommand.setName("CMD 1");
    smsCommand.setCodes(Set.of(smsCode1, smsCode2, smsCode3));

    smsCommandService.save(smsCommand);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<SMSCode> smsCommandSources =
        smsCommandService.getSmsCodesByDataElement(List.of(deSource1, deSource2));
    List<SMSCode> smsCommandTarget = smsCommandService.getSmsCodesByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(
        report, smsCommandSources, smsCommandTarget, allDataElements);
  }

  // -------------------------------
  // ---- Predictors ----
  // -------------------------------
  @Test
  @DisplayName(
      "Predictor references for DataElement are replaced as expected, source DataElements are not deleted")
  void predictorMergeTest() throws ConflictException {
    // given
    Predictor predictor1 = createPredictor('1', deSource1);
    Predictor predictor2 = createPredictor('2', deSource2);
    Predictor predictor3 = createPredictor('3', deTarget);

    predictorService.addPredictor(predictor1);
    predictorService.addPredictor(predictor2);
    predictorService.addPredictor(predictor3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Predictor> predictorSources =
        predictorService.getAllByDataElement(List.of(deSource1, deSource2));
    List<Predictor> predictorTarget = predictorService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(
        report, predictorSources, predictorTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Predictor references for DataElement are replaced as expected, source DataElements are deleted")
  void predictorMergeDeleteSourcesTest() throws ConflictException {
    // given
    Predictor predictor1 = createPredictor('1', deSource1);
    Predictor predictor2 = createPredictor('2', deSource2);
    Predictor predictor3 = createPredictor('3', deTarget);

    predictorService.addPredictor(predictor1);
    predictorService.addPredictor(predictor2);
    predictorService.addPredictor(predictor3);

    List<Predictor> predictorsSetup =
        predictorService.getAllByDataElement(List.of(deSource1, deSource2, deTarget));
    assertEquals(3, predictorsSetup.size());

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Predictor> predictorSources =
        predictorService.getAllByDataElement(List.of(deSource1, deSource2));
    List<Predictor> predictorTarget = predictorService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, predictorSources, predictorTarget, allDataElements);
  }

  // -------------------------------
  // -- ProgramStageDataElements --
  // -------------------------------
  @Test
  @DisplayName(
      "ProgramStageDataElement references for DataElement are replaced as expected, source DataElements are not deleted")
  void programStageDEMergeTest() throws ConflictException {
    // given
    Program program = createProgram('P');
    identifiableObjectManager.save(program);
    ProgramStage stage1 = createProgramStage('S', program);
    ProgramStage stage2 = createProgramStage('T', program);
    ProgramStage stage3 = createProgramStage('U', program);
    identifiableObjectManager.save(stage1);
    identifiableObjectManager.save(stage2);
    identifiableObjectManager.save(stage3);

    ProgramStageDataElement psde1 = createProgramStageDataElement(stage1, deSource1, 2);
    ProgramStageDataElement psde2 = createProgramStageDataElement(stage2, deSource2, 3);
    ProgramStageDataElement psde3 = createProgramStageDataElement(stage3, deTarget, 4);

    programStageDataElementService.addProgramStageDataElement(psde1);
    programStageDataElementService.addProgramStageDataElement(psde2);
    programStageDataElementService.addProgramStageDataElement(psde3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramStageDataElement> psdeSources =
        programStageDataElementService.getAllByDataElement(List.of(deSource1, deSource2));
    List<ProgramStageDataElement> psdeTarget =
        programStageDataElementService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, psdeSources, psdeTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "ProgramStageDataElement references for DataElement are replaced as expected, source DataElements are deleted")
  void programStageDEMergeDeleteSourcesTest() throws ConflictException {
    // given
    Program program = createProgram('P');
    identifiableObjectManager.save(program);
    ProgramStage stage1 = createProgramStage('S', program);
    ProgramStage stage2 = createProgramStage('T', program);
    ProgramStage stage3 = createProgramStage('U', program);
    identifiableObjectManager.save(stage1);
    identifiableObjectManager.save(stage2);
    identifiableObjectManager.save(stage3);

    ProgramStageDataElement psde1 = createProgramStageDataElement(stage1, deSource1, 2);
    ProgramStageDataElement psde2 = createProgramStageDataElement(stage2, deSource2, 3);
    ProgramStageDataElement psde3 = createProgramStageDataElement(stage3, deTarget, 4);

    programStageDataElementService.addProgramStageDataElement(psde1);
    programStageDataElementService.addProgramStageDataElement(psde2);
    programStageDataElementService.addProgramStageDataElement(psde3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramStageDataElement> psdeSources =
        programStageDataElementService.getAllByDataElement(List.of(deSource1, deSource2));
    List<ProgramStageDataElement> psdeTarget =
        programStageDataElementService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, psdeSources, psdeTarget, allDataElements);
  }

  @Test
  @DisplayName("ProgramStageDataElement DB constraint error when updating")
  void programStageDEMergeDbConstraint() {
    // given unique key DB constraint exists (programstageid, dataelementid)
    // create program stage data elements, all of which have the same stage
    Program program = createProgram('P');
    identifiableObjectManager.save(program);
    ProgramStage stage1 = createProgramStage('S', program);
    ProgramStage stage2 = createProgramStage('T', program);
    ProgramStage stage3 = createProgramStage('U', program);
    identifiableObjectManager.save(stage1);
    identifiableObjectManager.save(stage2);
    identifiableObjectManager.save(stage3);

    ProgramStageDataElement psde1 = createProgramStageDataElement(stage1, deSource1, 2);
    ProgramStageDataElement psde2 = createProgramStageDataElement(stage1, deSource2, 3);
    ProgramStageDataElement psde3 = createProgramStageDataElement(stage1, deTarget, 4);

    programStageDataElementService.addProgramStageDataElement(psde1);
    programStageDataElementService.addProgramStageDataElement(psde2);
    programStageDataElementService.addProgramStageDataElement(psde3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when merge operation encounters DB constraint
    PersistenceException persistenceException =
        assertThrows(PersistenceException.class, () -> mergeProcessor.processMerge(mergeParams));
    assertNotNull(persistenceException.getMessage());

    // then DB constraint is thrown
    List<String> expectedStrings =
        List.of(
            "duplicate key value violates unique constraint",
            "programstagedataelement_unique_key",
            "Detail: Key (programstageid, dataelementid)",
            "already exists");

    assertTrue(
        expectedStrings.stream()
            .allMatch(
                exp -> persistenceException.getCause().getCause().getMessage().contains(exp)));
  }

  // -------------------------------
  // -- ProgramStageSections --
  // -------------------------------
  @Test
  @DisplayName(
      "ProgramStageSection references for DataElement are replaced as expected, source DataElements are not deleted")
  void programStageSectionMergeTest() throws ConflictException {
    // given
    ProgramStageSection pss1 = createProgramStageSection('a', 1);
    pss1.getDataElements().add(deSource1);
    ProgramStageSection pss2 = createProgramStageSection('b', 2);
    pss2.getDataElements().add(deSource2);
    ProgramStageSection pss3 = createProgramStageSection('c', 3);
    pss3.getDataElements().add(deTarget);

    programStageSectionService.saveProgramStageSection(pss1);
    programStageSectionService.saveProgramStageSection(pss2);
    programStageSectionService.saveProgramStageSection(pss3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramStageSection> pssSources =
        programStageSectionService.getAllByDataElement(List.of(deSource1, deSource2));
    List<ProgramStageSection> pssTarget =
        programStageSectionService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, pssSources, pssTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "ProgramStageSection references for DataElement are replaced as expected, source DataElements are deleted")
  void programStageSectionMergeDeleteSourcesTest() throws ConflictException {
    // given
    ProgramStageSection pss1 = createProgramStageSection('d', 1);
    pss1.getDataElements().add(deSource1);
    ProgramStageSection pss2 = createProgramStageSection('e', 2);
    pss2.getDataElements().add(deSource2);
    ProgramStageSection pss3 = createProgramStageSection('F', 3);
    pss3.getDataElements().add(deTarget);

    programStageSectionService.saveProgramStageSection(pss1);
    programStageSectionService.saveProgramStageSection(pss2);
    programStageSectionService.saveProgramStageSection(pss3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramStageSection> pssSources =
        programStageSectionService.getAllByDataElement(List.of(deSource1, deSource2));
    List<ProgramStageSection> pssTarget =
        programStageSectionService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, pssSources, pssTarget, allDataElements);
  }

  // -------------------------------
  // -- ProgramNotificationTemplate --
  // -------------------------------
  @Test
  @DisplayName(
      "ProgramNotificationTemplate references for DataElement are replaced as expected, source DataElements are not deleted")
  void programNotificationTemplateMergeTest() throws ConflictException {
    // given
    ProgramNotificationTemplate pnt1 = createProgramNotificationTemplate("pnt1", 1, null, null);
    pnt1.setRecipientDataElement(deSource1);
    ProgramNotificationTemplate pnt2 = createProgramNotificationTemplate("pnt2", 1, null, null);
    pnt2.setRecipientDataElement(deSource2);
    ProgramNotificationTemplate pnt3 = createProgramNotificationTemplate("pnt3", 1, null, null);
    pnt3.setRecipientDataElement(deTarget);

    programNotificationTemplateService.save(pnt1);
    programNotificationTemplateService.save(pnt2);
    programNotificationTemplateService.save(pnt3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramNotificationTemplate> pntSources =
        programNotificationTemplateService.getByDataElement(List.of(deSource1, deSource2));
    List<ProgramNotificationTemplate> pntTarget =
        programNotificationTemplateService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, pntSources, pntTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "ProgramNotificationTemplate references for DataElement are replaced as expected, source DataElements are deleted")
  void programNotificationTemplateDeleteSourcesMergeTest() throws ConflictException {
    // given
    ProgramNotificationTemplate pnt1 = createProgramNotificationTemplate("pnt1", 1, null, null);
    pnt1.setRecipientDataElement(deSource1);
    ProgramNotificationTemplate pnt2 = createProgramNotificationTemplate("pnt2", 1, null, null);
    pnt2.setRecipientDataElement(deSource2);
    ProgramNotificationTemplate pnt3 = createProgramNotificationTemplate("pnt3", 1, null, null);
    pnt3.setRecipientDataElement(deTarget);

    programNotificationTemplateService.save(pnt1);
    programNotificationTemplateService.save(pnt2);
    programNotificationTemplateService.save(pnt3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramNotificationTemplate> pntSources =
        programNotificationTemplateService.getByDataElement(List.of(deSource1, deSource2));
    List<ProgramNotificationTemplate> pntTarget =
        programNotificationTemplateService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, pntSources, pntTarget, allDataElements);
  }

  // -------------------------------
  // -- ProgramRuleVariable --
  // -------------------------------
  @Test
  @DisplayName(
      "ProgramRuleVariable references for DataElement are replaced as expected, source DataElements are not deleted")
  void programRuleVariableMergeTest() throws ConflictException {
    // given
    Program program = createProgram('p');
    identifiableObjectManager.save(program);
    ProgramRuleVariable prv1 = createProgramRuleVariable('a', program);
    prv1.setDataElement(deSource1);
    ProgramRuleVariable prv2 = createProgramRuleVariable('b', program);
    prv2.setDataElement(deSource2);
    ProgramRuleVariable prv3 = createProgramRuleVariable('c', program);
    prv3.setDataElement(deTarget);

    programRuleVariableService.addProgramRuleVariable(prv1);
    programRuleVariableService.addProgramRuleVariable(prv2);
    programRuleVariableService.addProgramRuleVariable(prv3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramRuleVariable> prvSources =
        programRuleVariableService.getByDataElement(List.of(deSource1, deSource2));
    List<ProgramRuleVariable> prvTarget =
        programRuleVariableService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, prvSources, prvTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "ProgramRuleVariable references for DataElement are replaced as expected, source DataElements are deleted")
  void programRuleVariableSourcesDeletedMergeTest() throws ConflictException {
    // given
    Program program = createProgram('p');
    identifiableObjectManager.save(program);
    ProgramRuleVariable prv1 = createProgramRuleVariable('a', program);
    prv1.setDataElement(deSource1);
    ProgramRuleVariable prv2 = createProgramRuleVariable('b', program);
    prv2.setDataElement(deSource2);
    ProgramRuleVariable prv3 = createProgramRuleVariable('c', program);
    prv3.setDataElement(deTarget);

    programRuleVariableService.addProgramRuleVariable(prv1);
    programRuleVariableService.addProgramRuleVariable(prv2);
    programRuleVariableService.addProgramRuleVariable(prv3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramRuleVariable> prvSources =
        programRuleVariableService.getByDataElement(List.of(deSource1, deSource2));
    List<ProgramRuleVariable> prvTarget =
        programRuleVariableService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, prvSources, prvTarget, allDataElements);
  }

  // -------------------------------
  // -- ProgramRuleAction --
  // -------------------------------
  @Test
  @DisplayName(
      "ProgramRuleAction references for DataElement are replaced as expected, source DataElements are not deleted")
  void programRuleActionMergeTest() throws ConflictException {
    // given
    ProgramRuleAction pra1 = createProgramRuleAction('a');
    pra1.setDataElement(deSource1);
    ProgramRuleAction pra2 = createProgramRuleAction('b');
    pra2.setDataElement(deSource2);
    ProgramRuleAction pra3 = createProgramRuleAction('c');
    pra3.setDataElement(deTarget);

    programRuleActionService.addProgramRuleAction(pra1);
    programRuleActionService.addProgramRuleAction(pra2);
    programRuleActionService.addProgramRuleAction(pra3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramRuleAction> prvSources =
        programRuleActionService.getByDataElement(List.of(deSource1, deSource2));
    List<ProgramRuleAction> prvTarget =
        programRuleActionService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, prvSources, prvTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "ProgramRuleAction references for DataElement are replaced as expected, source DataElements are deleted")
  void programRuleActionSourcesDeletedMergeTest() throws ConflictException {
    // given
    ProgramRuleAction pra1 = createProgramRuleAction('a');
    pra1.setDataElement(deSource1);
    ProgramRuleAction pra2 = createProgramRuleAction('b');
    pra2.setDataElement(deSource2);
    ProgramRuleAction pra3 = createProgramRuleAction('c');
    pra3.setDataElement(deTarget);

    programRuleActionService.addProgramRuleAction(pra1);
    programRuleActionService.addProgramRuleAction(pra2);
    programRuleActionService.addProgramRuleAction(pra3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramRuleAction> prvSources =
        programRuleActionService.getByDataElement(List.of(deSource1, deSource2));
    List<ProgramRuleAction> prvTarget =
        programRuleActionService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, prvSources, prvTarget, allDataElements);
  }

  // -------------------------------
  // -- DataElementOperand --
  // -------------------------------
  @Test
  @DisplayName(
      "DataElementOperand references for DataElement are replaced as expected, source DataElements are not deleted")
  void dataElementOperandMergeTest() throws ConflictException {
    // given
    DataElementOperand deo1 = new DataElementOperand();
    deo1.setDataElement(deSource1);
    DataElementOperand deo2 = new DataElementOperand();
    deo2.setDataElement(deSource2);
    DataElementOperand deo3 = new DataElementOperand();
    deo3.setDataElement(deTarget);

    identifiableObjectManager.save(deo1);
    identifiableObjectManager.save(deo2);
    identifiableObjectManager.save(deo3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<DataElementOperand> deoSources =
        dataElementOperandStore.getByDataElement(List.of(deSource1, deSource2));
    List<DataElementOperand> prvTarget =
        dataElementOperandStore.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, deoSources, prvTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "DataElementOperand references for DataElement are replaced as expected, source DataElements are deleted")
  void dataElementOperandSourcesDeletedMergeTest() throws ConflictException {
    // given
    DataElementOperand deo1 = new DataElementOperand();
    deo1.setDataElement(deSource1);
    DataElementOperand deo2 = new DataElementOperand();
    deo2.setDataElement(deSource2);
    DataElementOperand deo3 = new DataElementOperand();
    deo3.setDataElement(deTarget);

    identifiableObjectManager.save(deo1);
    identifiableObjectManager.save(deo2);
    identifiableObjectManager.save(deo3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<DataElementOperand> deoSources =
        dataElementOperandStore.getByDataElement(List.of(deSource1, deSource2));
    List<DataElementOperand> prvTarget =
        dataElementOperandStore.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, deoSources, prvTarget, allDataElements);
  }

  // -------------------------------
  // -- DataSetElement --
  // -------------------------------
  @Test
  @DisplayName(
      "DataSetElement references for DataElement are replaced as expected, source DataElements are not deleted")
  void dataSetElementTest() throws ConflictException {
    // given
    DataSet ds1 = createDataSet('1', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet ds2 = createDataSet('2', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet ds3 = createDataSet('3', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));

    DataSetElement dse1 = new DataSetElement(ds1, deSource1);
    DataSetElement dse2 = new DataSetElement(ds2, deSource2);
    DataSetElement dse3 = new DataSetElement(ds3, deTarget);

    DataElement de1 = createDataElement('g');
    de1.getDataSetElements().add(dse1);

    DataElement de2 = createDataElement('h');
    de2.getDataSetElements().add(dse2);

    DataElement de3 = createDataElement('i');
    de3.getDataSetElements().add(dse3);

    ds1.setDataSetElements(Set.of(dse1));
    ds2.setDataSetElements(Set.of(dse2));
    ds3.setDataSetElements(Set.of(dse3));

    identifiableObjectManager.save(ds1);
    identifiableObjectManager.save(ds2);
    identifiableObjectManager.save(ds3);

    identifiableObjectManager.save(de1);
    identifiableObjectManager.save(de2);
    identifiableObjectManager.save(de3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<DataSetElement> dseSources =
        dataSetStore.getDataSetElementsByDataElement(List.of(deSource1, deSource2));
    List<DataSetElement> dseTarget =
        dataSetStore.getDataSetElementsByDataElement(List.of(deTarget));

    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    List<DataElement> allDataSetElsDataElement =
        allDataElements.stream()
            .flatMap(de -> de.getDataSetElements().stream())
            .map(DataSetElement::getDataElement)
            .distinct()
            .toList();

    assertFalse(report.hasErrorMessages());
    assertEquals(1, allDataSetElsDataElement.size(), "there should be only 1 data element present");
    assertTrue(
        allDataSetElsDataElement.contains(deTarget),
        "only the target data element should be present in data set elements");
    assertEquals(0, dseSources.size());
    assertEquals(3, dseTarget.size());
  }

  @Test
  @DisplayName(
      "DataSetElement references for DataElement are replaced as expected, source DataElements are deleted")
  void dataSetElementDeleteSourcesTest() throws ConflictException {
    // given
    DataSet ds1 = createDataSet('1', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet ds2 = createDataSet('2', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet ds3 = createDataSet('3', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));

    DataSetElement dse1 = new DataSetElement(ds1, deSource1);
    DataSetElement dse2 = new DataSetElement(ds2, deSource2);
    DataSetElement dse3 = new DataSetElement(ds3, deTarget);

    DataElement de1 = createDataElement('g');
    de1.getDataSetElements().add(dse1);

    DataElement de2 = createDataElement('h');
    de2.getDataSetElements().add(dse2);

    DataElement de3 = createDataElement('i');
    de3.getDataSetElements().add(dse3);

    ds1.setDataSetElements(Set.of(dse1));
    ds2.setDataSetElements(Set.of(dse2));
    ds3.setDataSetElements(Set.of(dse3));

    identifiableObjectManager.save(ds1);
    identifiableObjectManager.save(ds2);
    identifiableObjectManager.save(ds3);

    identifiableObjectManager.save(de1);
    identifiableObjectManager.save(de2);
    identifiableObjectManager.save(de3);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<DataSetElement> dseSources =
        dataSetStore.getDataSetElementsByDataElement(List.of(deSource1, deSource2));
    List<DataSetElement> dseTarget =
        dataSetStore.getDataSetElementsByDataElement(List.of(deTarget));

    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    List<DataElement> allDataSetElsDataElement =
        allDataElements.stream()
            .flatMap(de -> de.getDataSetElements().stream())
            .map(DataSetElement::getDataElement)
            .distinct()
            .toList();

    assertFalse(report.hasErrorMessages());
    assertEquals(1, allDataSetElsDataElement.size(), "there should be only 1 data element present");
    assertTrue(
        allDataSetElsDataElement.contains(deTarget),
        "only the target data element should be present in data set elements");
    assertEquals(0, dseSources.size());
    assertEquals(4, allDataElements.size());
    assertFalse(allDataElements.contains(deSource1));
    assertFalse(allDataElements.contains(deSource2));
    assertEquals(3, dseTarget.size());
  }

  @Test
  @DisplayName("DataSetElement DB constraint error when updating")
  void dataSetElementDbConstraintTest() {
    // given
    DataSet ds1 = createDataSet('1', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet ds2 = createDataSet('2', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));

    DataSetElement dse1 = new DataSetElement(ds1, deSource1);
    DataSetElement dse2 = new DataSetElement(ds1, deSource2);
    DataSetElement dse3 = new DataSetElement(ds2, deTarget);

    ds1.setDataSetElements(Set.of(dse1, dse2));
    ds2.setDataSetElements(Set.of(dse3));

    identifiableObjectManager.save(ds1);
    identifiableObjectManager.save(ds2);

    // params
    MergeParams mergeParams = getMergeParams();

    // when merge operation encounters DB constraint
    PersistenceException persistenceException =
        assertThrows(PersistenceException.class, () -> mergeProcessor.processMerge(mergeParams));
    assertNotNull(persistenceException.getMessage());

    // then DB constraint is thrown
    List<String> expectedStrings =
        List.of(
            "duplicate key value violates unique constraint",
            "datasetelement_unique_key",
            "Detail: Key (datasetid, dataelementid)",
            "already exists");

    assertTrue(
        expectedStrings.stream()
            .allMatch(
                exp -> persistenceException.getCause().getCause().getMessage().contains(exp)));
  }

  // -------------------------------
  // -- Sections --
  // -------------------------------
  @Test
  @DisplayName(
      "Section references for DataElement are replaced as expected, source DataElements are not deleted")
  void programSectionMergeTest() throws ConflictException {
    // given
    DataSet ds1 = createDataSet('1', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet ds2 = createDataSet('2', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    identifiableObjectManager.save(ds1);
    identifiableObjectManager.save(ds2);

    createSectionAndSave('a', ds1, deSource1);
    createSectionAndSave('b', ds2, deSource2);
    createSectionAndSave('c', ds2, deTarget);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Section> sectionSources = sectionService.getByDataElement(List.of(deSource1, deSource2));
    List<Section> sectionTarget = sectionService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, sectionSources, sectionTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Section references for DataElement are replaced as expected, source DataElements are deleted")
  void programSectionMergeDeleteSourcesTest() throws ConflictException {
    // given
    DataSet ds1 = createDataSet('1', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet ds2 = createDataSet('2', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    identifiableObjectManager.save(ds1);
    identifiableObjectManager.save(ds2);

    createSectionAndSave('a', ds1, deSource1);
    createSectionAndSave('b', ds2, deSource2);
    createSectionAndSave('c', ds2, deTarget);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Section> sectionSources = sectionService.getByDataElement(List.of(deSource1, deSource2));
    List<Section> sectionTarget = sectionService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, sectionSources, sectionTarget, allDataElements);
  }

  private void assertMergeSuccessfulSourcesNotDeleted(
      MergeReport report,
      Collection<?> sources,
      Collection<?> target,
      Collection<DataElement> dataElements) {
    assertFalse(report.hasErrorMessages());
    assertEquals(0, sources.size());
    assertEquals(3, target.size());
    assertEquals(3, dataElements.size());
    assertTrue(dataElements.containsAll(List.of(deTarget, deSource1, deSource2)));
  }

  private void assertMergeSuccessfulSourcesDeleted(
      MergeReport report,
      Collection<?> sources,
      Collection<?> target,
      Collection<DataElement> dataElements) {
    assertFalse(report.hasErrorMessages());
    assertEquals(0, sources.size());
    assertEquals(3, target.size());
    assertEquals(1, dataElements.size());
    assertTrue(dataElements.contains(deTarget));
  }

  private void createSectionAndSave(char c, DataSet ds, DataElement de) {
    Section section = new Section();
    section.setName("section " + c);
    section.setDataSet(ds);
    section.getDataElements().add(de);
    identifiableObjectManager.save(section);
  }

  private MergeParams getMergeParams() {
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(deSource1.getUid(), deSource2.getUid())));
    mergeParams.setTarget(UID.of(deTarget.getUid()));
    return mergeParams;
  }

  private SMSCode createSmsCode(String code, DataElement de) {
    SMSCode smsCode = new SMSCode();
    smsCode.setCode(code);
    smsCode.setDataElement(de);
    return smsCode;
  }

  private Predictor createPredictor(char id, DataElement de) {
    return createPredictor(
        de,
        coc1,
        String.valueOf(id),
        new Expression(String.valueOf(id), "test" + id),
        new Expression(String.valueOf(id), "test2" + id),
        PeriodType.getPeriodType(PeriodTypeEnum.DAILY),
        oul,
        0,
        0,
        0);
  }
}
