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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUidsNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datadimensionitem.DataDimensionItemStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandStore;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormStore;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationStore;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorStore;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorStore;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorStore;
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
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * All the tests in this class basically test the same thing:
 *
 * <p>- Create metadata which have source DataElement references
 *
 * <p>- Perform a DataElement merge, passing a target DataElement
 *
 * <p>- Check that source DataElements have had their references removed/replaced with the target
 * DataElement
 */
class DataElementMergeProcessorTest extends TransactionalIntegrationTest {

  @Autowired private DataElementService dataElementService;
  @Autowired private DataElementMergeProcessor mergeProcessor;
  @Autowired private MinMaxDataElementService minMaxDataElementService;
  @Autowired private EventVisualizationStore eventVisualizationStore;
  @Autowired private AnalyticalObjectStore<EventVisualization> analyticalEventVizStore;
  @Autowired private AnalyticalObjectStore<MapView> mapViewStore;
  @Autowired private OrganisationUnitService orgUnitService;
  @Autowired private SMSCommandService smsCommandService;
  @Autowired private PredictorStore predictorStore;
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
  @Autowired private DataElementGroupService dataElementGroupService;
  @Autowired private IndicatorStore indicatorStore;
  @Autowired private DataEntryFormStore dataEntryFormStore;
  @Autowired private ProgramIndicatorStore programIndicatorStore;
  @Autowired private EventStore eventStore;
  @Autowired private DataDimensionItemStore dataDimensionItemStore;

  private DataElement deSource1;
  private DataElement deSource2;
  private DataElement deTarget;
  private DataElement deRandom;
  private OrganisationUnit ou1;
  private OrganisationUnit ou2;
  private OrganisationUnit ou3;
  private OrganisationUnitLevel oul;
  private CategoryOptionCombo coc1;
  private Program program;

  @BeforeEach
  public void setUp() {
    // data elements
    deSource1 = createDataElement('A');
    deSource2 = createDataElement('B');
    deTarget = createDataElement('C');
    deRandom = createDataElement('D');
    dataElementService.addDataElement(deSource1);
    dataElementService.addDataElement(deSource2);
    dataElementService.addDataElement(deTarget);
    dataElementService.addDataElement(deRandom);

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

    program = createProgram('q');
    identifiableObjectManager.save(program);
  }

  @Test
  @DisplayName("Ensure setup data is present in system")
  void ensureDataIsPresentInSystem() {
    // given setup is complete
    // when trying to retrieve data
    List<DataElement> dataElements = dataElementService.getAllDataElements();
    List<OrganisationUnit> orgUnits = orgUnitService.getAllOrganisationUnits();

    // then
    assertEquals(4, dataElements.size());
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
    MinMaxDataElement minMaxDataElement4 =
        new MinMaxDataElement(deRandom, ou3, coc1, 0, 100, false);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement1);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement2);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement3);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement4);

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
    MinMaxDataElement minMaxDataElement4 =
        new MinMaxDataElement(deRandom, ou3, coc1, 0, 100, false);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement1);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement2);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement3);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement4);

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
    MinMaxDataElement minMaxDataElement4 =
        new MinMaxDataElement(deRandom, ou3, coc1, 0, 100, false);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement1);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement2);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement3);
    minMaxDataElementService.addMinMaxDataElement(minMaxDataElement4);

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
    EventVisualization ev1 = createEventVisualization('1', null);
    ev1.setDataElementValueDimension(deTarget);
    EventVisualization ev2 = createEventVisualization('2', null);
    ev2.setDataElementValueDimension(deSource1);
    EventVisualization ev3 = createEventVisualization('3', null);
    ev3.setDataElementValueDimension(deSource2);
    EventVisualization ev4 = createEventVisualization('4', null);
    ev4.setDataElementValueDimension(deRandom);

    identifiableObjectManager.save(List.of(ev1, ev2, ev3, ev4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<EventVisualization> eventVizSources =
        eventVisualizationStore.getByDataElement(List.of(deSource1, deSource2));
    List<EventVisualization> eventVizTarget =
        eventVisualizationStore.getByDataElement(List.of(deTarget));
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
    EventVisualization ev5 = createEventVisualization('5', null);
    ev5.setDataElementValueDimension(deTarget);
    EventVisualization ev6 = createEventVisualization('6', null);
    ev6.setDataElementValueDimension(deSource1);
    EventVisualization ev7 = createEventVisualization('7', null);
    ev7.setDataElementValueDimension(deSource2);
    EventVisualization ev8 = createEventVisualization('8', null);
    ev8.setDataElementValueDimension(deRandom);

    identifiableObjectManager.save(List.of(ev5, ev6, ev7, ev8));

    // params
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(deSource1.getUid(), deSource2.getUid())));
    mergeParams.setTarget(UID.of(deTarget.getUid()));
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<EventVisualization> eventVizSources =
        eventVisualizationStore.getByDataElement(List.of(deSource1, deSource2));
    List<EventVisualization> eventVizTarget =
        eventVisualizationStore.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, eventVizSources, eventVizTarget, allDataElements);
  }

  // -------------------------------------------
  // ---- TrackedEntityDataElementDimension ----
  // -------------------------------------------
  @Test
  @DisplayName(
      "TrackedEntityDataElementDimension references to source DataElements are replaced as expected, source DataElements are not deleted")
  void trackedEntityDataElDimMergeTest() throws ConflictException {
    // given
    // event visualizations
    TrackedEntityDataElementDimension teded1 = new TrackedEntityDataElementDimension();
    teded1.setDataElement(deSource1);
    TrackedEntityDataElementDimension teded2 = new TrackedEntityDataElementDimension();
    teded2.setDataElement(deSource2);
    TrackedEntityDataElementDimension teded3 = new TrackedEntityDataElementDimension();
    teded3.setDataElement(deTarget);
    TrackedEntityDataElementDimension teded4 = new TrackedEntityDataElementDimension();
    teded4.setDataElement(deRandom);

    TrackedEntityDataElementDimension teded5 = new TrackedEntityDataElementDimension();
    teded5.setDataElement(deSource1);
    TrackedEntityDataElementDimension teded6 = new TrackedEntityDataElementDimension();
    teded6.setDataElement(deSource2);
    TrackedEntityDataElementDimension teded7 = new TrackedEntityDataElementDimension();
    teded7.setDataElement(deTarget);
    TrackedEntityDataElementDimension teded8 = new TrackedEntityDataElementDimension();
    teded8.setDataElement(deRandom);

    // Mapview
    MapView m1 = createMapView("e");
    m1.addTrackedEntityDataElementDimension(teded1);
    MapView m2 = createMapView("f");
    m2.addTrackedEntityDataElementDimension(teded2);
    MapView m3 = createMapView("g");
    m3.addTrackedEntityDataElementDimension(teded3);
    MapView m4 = createMapView("h");
    m4.addTrackedEntityDataElementDimension(teded4);
    MapView m5 = createMapView("i");

    identifiableObjectManager.save(List.of(m1, m2, m3, m4, m5));

    // event viz
    EventVisualization ev1 = createEventVisualization('e', program);
    ev1.addTrackedEntityDataElementDimension(teded5);
    EventVisualization ev2 = createEventVisualization('f', program);
    ev2.addTrackedEntityDataElementDimension(teded6);
    EventVisualization ev3 = createEventVisualization('g', program);
    ev3.addTrackedEntityDataElementDimension(teded7);
    EventVisualization ev4 = createEventVisualization('h', program);
    ev4.addTrackedEntityDataElementDimension(teded8);
    EventVisualization ev5 = createEventVisualization('h', program);

    identifiableObjectManager.save(List.of(ev1, ev2, ev3, ev4, ev5));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    // event viz
    List<EventVisualization> sourcesEventViz =
        analyticalEventVizStore.getByDataElementDimensionsWithAnyOf(List.of(deSource1, deSource2));
    List<EventVisualization> targetEventViz =
        analyticalEventVizStore.getByDataElementDimensionsWithAnyOf(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    // map view
    List<MapView> sourceMapViews =
        mapViewStore.getByDataElementDimensionsWithAnyOf(List.of(deSource1, deSource2));
    List<MapView> targetMapView =
        mapViewStore.getByDataElementDimensionsWithAnyOf(List.of(deTarget));

    assertMergeSuccessfulSourcesNotDeleted(
        report, sourcesEventViz, targetEventViz, allDataElements);
    assertMergeSuccessfulSourcesNotDeleted(report, sourceMapViews, targetMapView, allDataElements);
  }

  @Test
  @DisplayName(
      "TrackedEntityDataElementDimension references to source DataElements are replaced as expected, source DataElements are deleted")
  void trackedEntityDataElDimMergeSourcesDeletedTest() throws ConflictException {
    // given
    // event visualizations
    TrackedEntityDataElementDimension teded1 = new TrackedEntityDataElementDimension();
    teded1.setDataElement(deSource1);
    TrackedEntityDataElementDimension teded2 = new TrackedEntityDataElementDimension();
    teded2.setDataElement(deSource2);
    TrackedEntityDataElementDimension teded3 = new TrackedEntityDataElementDimension();
    teded3.setDataElement(deTarget);
    TrackedEntityDataElementDimension teded4 = new TrackedEntityDataElementDimension();
    teded4.setDataElement(deRandom);

    TrackedEntityDataElementDimension teded5 = new TrackedEntityDataElementDimension();
    teded5.setDataElement(deSource1);
    TrackedEntityDataElementDimension teded6 = new TrackedEntityDataElementDimension();
    teded6.setDataElement(deSource2);
    TrackedEntityDataElementDimension teded7 = new TrackedEntityDataElementDimension();
    teded7.setDataElement(deTarget);
    TrackedEntityDataElementDimension teded8 = new TrackedEntityDataElementDimension();
    teded8.setDataElement(deRandom);

    // Mapview
    MapView m1 = createMapView("e");
    m1.addTrackedEntityDataElementDimension(teded1);
    MapView m2 = createMapView("f");
    m2.addTrackedEntityDataElementDimension(teded2);
    MapView m3 = createMapView("g");
    m3.addTrackedEntityDataElementDimension(teded3);
    MapView m4 = createMapView("h");
    m4.addTrackedEntityDataElementDimension(teded4);
    MapView m5 = createMapView("i");

    identifiableObjectManager.save(List.of(m1, m2, m3, m4, m5));

    // event viz
    EventVisualization ev1 = createEventVisualization('e', program);
    ev1.addTrackedEntityDataElementDimension(teded5);
    EventVisualization ev2 = createEventVisualization('f', program);
    ev2.addTrackedEntityDataElementDimension(teded6);
    EventVisualization ev3 = createEventVisualization('g', program);
    ev3.addTrackedEntityDataElementDimension(teded7);
    EventVisualization ev4 = createEventVisualization('h', program);
    ev4.addTrackedEntityDataElementDimension(teded8);
    EventVisualization ev5 = createEventVisualization('h', program);

    identifiableObjectManager.save(List.of(ev1, ev2, ev3, ev4, ev5));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    // event viz
    List<EventVisualization> sourcesEventViz =
        analyticalEventVizStore.getByDataElementDimensionsWithAnyOf(List.of(deSource1, deSource2));
    List<EventVisualization> targetEventViz =
        analyticalEventVizStore.getByDataElementDimensionsWithAnyOf(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    // map view
    List<MapView> sourceMapViews =
        mapViewStore.getByDataElementDimensionsWithAnyOf(List.of(deSource1, deSource2));
    List<MapView> targetMapView =
        mapViewStore.getByDataElementDimensionsWithAnyOf(List.of(deTarget));

    assertMergeSuccessfulSourcesDeleted(report, sourcesEventViz, targetEventViz, allDataElements);
    assertMergeSuccessfulSourcesDeleted(report, sourceMapViews, targetMapView, allDataElements);
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
    SMSCode smsCode4 = createSmsCode("code target 4", deRandom);

    SMSCommand smsCommand = new SMSCommand();
    smsCommand.setName("CMD 1");
    smsCommand.setCodes(Set.of(smsCode1, smsCode2, smsCode3, smsCode4));

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
    SMSCode smsCode4 = createSmsCode("code target 4", deRandom);

    SMSCommand smsCommand = new SMSCommand();
    smsCommand.setName("CMD 1");
    smsCommand.setCodes(Set.of(smsCode1, smsCode2, smsCode3, smsCode4));

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
    Predictor predictor4 = createPredictor('4', deRandom);

    identifiableObjectManager.save(List.of(predictor1, predictor2, predictor3, predictor4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Predictor> predictorSources =
        predictorStore.getAllByDataElement(List.of(deSource1, deSource2));
    List<Predictor> predictorTarget = predictorStore.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(
        report, predictorSources, predictorTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Predictor references for DataElement are replaced as expected, source DataElements are deleted")
  void predictorMergeDeleteSourcesTest() throws ConflictException {
    // given
    Predictor p1 = createPredictor('1', deSource1);
    Predictor p2 = createPredictor('2', deSource2);
    Predictor p3 = createPredictor('3', deTarget);
    Predictor p4 = createPredictor('4', deRandom);

    identifiableObjectManager.save(List.of(p1, p2, p3, p4));

    List<Predictor> predictorsSetup =
        predictorStore.getAllByDataElement(List.of(deSource1, deSource2, deTarget));
    assertEquals(3, predictorsSetup.size());

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Predictor> predictorSources =
        predictorStore.getAllByDataElement(List.of(deSource1, deSource2));
    List<Predictor> predictorTarget = predictorStore.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, predictorSources, predictorTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Predictor generator expression references with DataElement are replaced as expected, source DataElements are not deleted")
  void predictorGeneratorMergeTest() throws ConflictException {
    // given
    Predictor p1 = createPredictorWithGenerator('1', deRandom, deSource1);
    Predictor p2 = createPredictorWithGenerator('2', deRandom, deSource2);
    Predictor p3 = createPredictorWithGenerator('3', deRandom, deTarget);
    Predictor p4 = createPredictorWithGenerator('4', deRandom, deRandom);

    identifiableObjectManager.save(List.of(p1, p2, p3, p4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Predictor> predictorSources =
        predictorStore.getAllWithGeneratorContainingDataElement(
            getUidsNonNull(List.of(deSource1, deSource2)));
    List<Predictor> predictorTarget =
        predictorStore.getAllWithGeneratorContainingDataElement(getUidsNonNull(List.of(deTarget)));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(
        report, predictorSources, predictorTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Predictor generator expression references with DataElement are replaced as expected, source DataElements are deleted")
  void predictorGeneratorMergeSourcesDeletedTest() throws ConflictException {
    // given
    Predictor p1 = createPredictorWithGenerator('1', deRandom, deSource1);
    Predictor p2 = createPredictorWithGenerator('2', deRandom, deSource2);
    Predictor p3 = createPredictorWithGenerator('3', deRandom, deTarget);
    Predictor p4 = createPredictorWithGenerator('4', deRandom, deRandom);

    identifiableObjectManager.save(List.of(p1, p2, p3, p4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Predictor> predictorSources =
        predictorStore.getAllWithGeneratorContainingDataElement(
            getUidsNonNull(List.of(deSource1, deSource2)));
    List<Predictor> predictorTarget =
        predictorStore.getAllWithGeneratorContainingDataElement(getUidsNonNull(List.of(deTarget)));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, predictorSources, predictorTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Predictor sample skip test expression references with DataElement are replaced as expected, source DataElements are not deleted")
  void predictorSampleSkipTestMergeTest() throws ConflictException {
    // given
    Predictor p1 = createPredictorWithSkipTest('1', deRandom, deSource1);
    Predictor p2 = createPredictorWithSkipTest('2', deRandom, deSource2);
    Predictor p3 = createPredictorWithSkipTest('3', deRandom, deTarget);
    Predictor p4 = createPredictorWithSkipTest('4', deRandom, deRandom);

    identifiableObjectManager.save(List.of(p1, p2, p3, p4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Predictor> predictorSources =
        predictorStore.getAllWithSampleSkipTestContainingDataElement(
            getUidsNonNull(List.of(deSource1, deSource2)));
    List<Predictor> predictorTarget =
        predictorStore.getAllWithSampleSkipTestContainingDataElement(
            getUidsNonNull(List.of(deTarget)));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(
        report, predictorSources, predictorTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Predictor sample skip test expression references with DataElement are replaced as expected, source DataElements are deleted")
  void predictorSampleSkipTestMergeSourcesDeletedTest() throws ConflictException {
    // given
    Predictor p1 = createPredictorWithSkipTest('1', deRandom, deSource1);
    Predictor p2 = createPredictorWithSkipTest('2', deRandom, deSource2);
    Predictor p3 = createPredictorWithSkipTest('3', deRandom, deTarget);
    Predictor p4 = createPredictorWithSkipTest('4', deRandom, deRandom);

    identifiableObjectManager.save(List.of(p1, p2, p3, p4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Predictor> predictorSources =
        predictorStore.getAllWithSampleSkipTestContainingDataElement(
            getUidsNonNull(List.of(deSource1, deSource2)));
    List<Predictor> predictorTarget =
        predictorStore.getAllWithSampleSkipTestContainingDataElement(
            getUidsNonNull(List.of(deTarget)));
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
    ProgramStage stage1 = createProgramStage('S', program);
    ProgramStage stage2 = createProgramStage('T', program);
    ProgramStage stage3 = createProgramStage('U', program);
    ProgramStage stage4 = createProgramStage('V', program);
    identifiableObjectManager.save(List.of(stage1, stage2, stage3, stage4));

    ProgramStageDataElement psde1 = createProgramStageDataElement(stage1, deSource1, 2);
    ProgramStageDataElement psde2 = createProgramStageDataElement(stage2, deSource2, 3);
    ProgramStageDataElement psde3 = createProgramStageDataElement(stage3, deTarget, 4);
    ProgramStageDataElement psde4 = createProgramStageDataElement(stage4, deRandom, 5);
    identifiableObjectManager.save(List.of(psde1, psde2, psde3, psde4));

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
    ProgramStage stage1 = createProgramStage('S', program);
    ProgramStage stage2 = createProgramStage('T', program);
    ProgramStage stage3 = createProgramStage('U', program);
    ProgramStage stage4 = createProgramStage('V', program);
    identifiableObjectManager.save(List.of(stage1, stage2, stage3, stage4));

    ProgramStageDataElement psde1 = createProgramStageDataElement(stage1, deSource1, 2);
    ProgramStageDataElement psde2 = createProgramStageDataElement(stage2, deSource2, 3);
    ProgramStageDataElement psde3 = createProgramStageDataElement(stage3, deTarget, 4);
    ProgramStageDataElement psde4 = createProgramStageDataElement(stage4, deRandom, 5);
    identifiableObjectManager.save(List.of(psde1, psde2, psde3, psde4));

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
    ProgramStage stage1 = createProgramStage('S', program);
    ProgramStage stage2 = createProgramStage('T', program);
    ProgramStage stage3 = createProgramStage('U', program);
    ProgramStage stage4 = createProgramStage('V', program);
    identifiableObjectManager.save(List.of(stage1, stage2, stage3, stage4));

    ProgramStageDataElement psde1 = createProgramStageDataElement(stage1, deSource1, 2);
    ProgramStageDataElement psde2 = createProgramStageDataElement(stage1, deSource2, 3);
    ProgramStageDataElement psde3 = createProgramStageDataElement(stage1, deTarget, 4);
    ProgramStageDataElement psde4 = createProgramStageDataElement(stage4, deRandom, 5);
    identifiableObjectManager.save(List.of(psde1, psde2, psde3, psde4));

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
    ProgramStageSection pss4 = createProgramStageSection('d', 4);
    pss4.getDataElements().add(deRandom);

    identifiableObjectManager.save(List.of(pss1, pss2, pss3, pss4));

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
    ProgramStageSection pss1 = createProgramStageSection('e', 1);
    pss1.getDataElements().add(deSource1);
    ProgramStageSection pss2 = createProgramStageSection('f', 2);
    pss2.getDataElements().add(deSource2);
    ProgramStageSection pss3 = createProgramStageSection('g', 3);
    pss3.getDataElements().add(deTarget);
    ProgramStageSection pss4 = createProgramStageSection('h', 4);
    pss4.getDataElements().add(deRandom);

    identifiableObjectManager.save(List.of(pss1, pss2, pss3, pss4));

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
    ProgramNotificationTemplate pnt4 = createProgramNotificationTemplate("pnt4", 1, null, null);
    pnt4.setRecipientDataElement(deRandom);

    identifiableObjectManager.save(List.of(pnt1, pnt2, pnt3, pnt4));

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
    ProgramNotificationTemplate pnt4 = createProgramNotificationTemplate("pnt4", 1, null, null);
    pnt4.setRecipientDataElement(deRandom);

    identifiableObjectManager.save(List.of(pnt1, pnt2, pnt3, pnt4));

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
    ProgramRuleVariable prv1 = createProgramRuleVariable('a', program);
    prv1.setDataElement(deSource1);
    ProgramRuleVariable prv2 = createProgramRuleVariable('b', program);
    prv2.setDataElement(deSource2);
    ProgramRuleVariable prv3 = createProgramRuleVariable('c', program);
    prv3.setDataElement(deTarget);
    ProgramRuleVariable prv4 = createProgramRuleVariable('d', program);
    prv4.setDataElement(deRandom);

    identifiableObjectManager.save(List.of(prv1, prv2, prv3, prv4));

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
    ProgramRuleVariable prv1 = createProgramRuleVariable('a', program);
    prv1.setDataElement(deSource1);
    ProgramRuleVariable prv2 = createProgramRuleVariable('b', program);
    prv2.setDataElement(deSource2);
    ProgramRuleVariable prv3 = createProgramRuleVariable('c', program);
    prv3.setDataElement(deTarget);
    ProgramRuleVariable prv4 = createProgramRuleVariable('d', program);
    prv4.setDataElement(deRandom);

    identifiableObjectManager.save(List.of(prv1, prv2, prv3, prv4));

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
    ProgramRuleAction pra4 = createProgramRuleAction('d');
    pra4.setDataElement(deRandom);

    identifiableObjectManager.save(List.of(pra1, pra2, pra3, pra4));

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
    ProgramRuleAction pra4 = createProgramRuleAction('d');
    pra4.setDataElement(deRandom);

    identifiableObjectManager.save(List.of(pra1, pra2, pra3, pra4));

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

  // ---------------------------------
  // -- ProgramIndicator expression --
  // ---------------------------------
  @Test
  @DisplayName(
      "ProgramIndicator expression references with DataElement have the DataElement ref replaced as expected, source DataElements are not deleted")
  void programIndicatorExpressionMergeTest() throws ConflictException {
    // given
    ProgramIndicator pi1 =
        createProgramIndicator('a', program, "#{12345.%s}".formatted(deSource1.getUid()), "");
    ProgramIndicator pi2 =
        createProgramIndicator('b', program, "#{12345.%s}".formatted(deSource2.getUid()), "");
    ProgramIndicator pi3 =
        createProgramIndicator('c', program, "#{12345.%s}".formatted(deTarget.getUid()), "");
    ProgramIndicator pi4 =
        createProgramIndicator('d', program, "#{12345.%s}".formatted(deRandom.getUid()), "");

    identifiableObjectManager.save(List.of(pi1, pi2, pi3, pi4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramIndicator> piSources =
        programIndicatorStore.getAllWithExpressionContainingStrings(
            List.of(deSource1.getUid(), deSource2.getUid()));
    List<ProgramIndicator> piTarget =
        programIndicatorStore.getAllWithExpressionContainingStrings(List.of(deTarget.getUid()));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, piSources, piTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "ProgramIndicator expression references with DataElement have the DataElement ref replaced as expected, source DataElements are deleted")
  void programIndicatorExpressionMergeSourcesDeletedTest() throws ConflictException {
    // given
    ProgramIndicator pi1 =
        createProgramIndicator('a', program, "#{12345.%s}".formatted(deSource1.getUid()), "");
    ProgramIndicator pi2 =
        createProgramIndicator('b', program, "#{12345.%s}".formatted(deSource2.getUid()), "");
    ProgramIndicator pi3 =
        createProgramIndicator('c', program, "#{12345.%s}".formatted(deTarget.getUid()), "");
    ProgramIndicator pi4 =
        createProgramIndicator('d', program, "#{12345.%s}".formatted(deRandom.getUid()), "");

    identifiableObjectManager.save(List.of(pi1, pi2, pi3, pi4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramIndicator> piSources =
        programIndicatorStore.getAllWithExpressionContainingStrings(
            List.of(deSource1.getUid(), deSource2.getUid()));
    List<ProgramIndicator> piTarget =
        programIndicatorStore.getAllWithExpressionContainingStrings(List.of(deTarget.getUid()));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, piSources, piTarget, allDataElements);
  }

  // -----------------------------
  // -- ProgramIndicator filter --
  // -----------------------------
  @Test
  @DisplayName(
      "ProgramIndicator filter references with DataElement have the DataElement ref replaced as expected, source DataElements are not deleted")
  void programIndicatorFilterMergeTest() throws ConflictException {
    // given
    ProgramIndicator pi1 =
        createProgramIndicator('a', program, "", "#{12345.%s}".formatted(deSource1.getUid()));
    ProgramIndicator pi2 =
        createProgramIndicator('b', program, "", "#{12345.%s}".formatted(deSource2.getUid()));
    ProgramIndicator pi3 =
        createProgramIndicator('c', program, "", "#{12345.%s}".formatted(deTarget.getUid()));
    ProgramIndicator pi4 =
        createProgramIndicator('d', program, "", "#{12345.%s}".formatted(deRandom.getUid()));

    identifiableObjectManager.save(List.of(pi1, pi2, pi3, pi4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramIndicator> piSources =
        programIndicatorStore.getAllWithFilterContainingStrings(
            List.of(deSource1.getUid(), deSource2.getUid()));
    List<ProgramIndicator> piTarget =
        programIndicatorStore.getAllWithFilterContainingStrings(List.of(deTarget.getUid()));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, piSources, piTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "ProgramIndicator filter references with DataElement have the DataElement ref replaced as expected, source DataElements are deleted")
  void programIndicatorFilterMergeSourcesDeletedTest() throws ConflictException {
    // given
    ProgramIndicator pi1 =
        createProgramIndicator('a', program, "", "#{12345.%s}".formatted(deSource1.getUid()));
    ProgramIndicator pi2 =
        createProgramIndicator('b', program, "", "#{12345.%s}".formatted(deSource2.getUid()));
    ProgramIndicator pi3 =
        createProgramIndicator('c', program, "", "#{12345.%s}".formatted(deTarget.getUid()));
    ProgramIndicator pi4 =
        createProgramIndicator('d', program, "", "#{12345.%s}".formatted(deRandom.getUid()));

    identifiableObjectManager.save(List.of(pi1, pi2, pi3, pi4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<ProgramIndicator> piSources =
        programIndicatorStore.getAllWithFilterContainingStrings(
            List.of(deSource1.getUid(), deSource2.getUid()));
    List<ProgramIndicator> piTarget =
        programIndicatorStore.getAllWithFilterContainingStrings(List.of(deTarget.getUid()));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, piSources, piTarget, allDataElements);
  }

  // -----------------------------
  // -- Event eventDataValues --
  // -----------------------------
  @Test
  @DisplayName(
      "Event eventDataValues references with DataElement have the DataElement ref replaced as expected, source DataElements are not deleted")
  void eventMergeTest() throws ConflictException {
    // given
    TrackedEntity trackedEntity = createTrackedEntity(ou1);
    identifiableObjectManager.save(trackedEntity);
    Enrollment enrollment = createEnrollment(program, trackedEntity, ou1);
    identifiableObjectManager.save(enrollment);
    ProgramStage stage = createProgramStage('s', 2);
    identifiableObjectManager.save(stage);

    Event e1 = createEvent(stage, enrollment, ou1);
    e1.setAttributeOptionCombo(coc1);
    Event e2 = createEvent(stage, enrollment, ou1);
    e2.setAttributeOptionCombo(coc1);
    Event e3 = createEvent(stage, enrollment, ou1);
    e3.setAttributeOptionCombo(coc1);
    Event e4 = createEvent(stage, enrollment, ou1);
    e4.setAttributeOptionCombo(coc1);

    EventDataValue edv1 = new EventDataValue(deSource1.getUid(), "value1");
    EventDataValue edv11 = new EventDataValue(deSource1.getUid(), "value11");
    EventDataValue edv2 = new EventDataValue(deSource2.getUid(), "value2");
    EventDataValue edv3 = new EventDataValue(deTarget.getUid(), "value3");
    EventDataValue edv4 = new EventDataValue(deRandom.getUid(), "value4");
    Set<EventDataValue> edvs1 = new HashSet<>();
    edvs1.add(edv1);
    edvs1.add(edv11);
    edvs1.add(edv2);
    Set<EventDataValue> edvs2 = new HashSet<>();
    Set<EventDataValue> edvs3 = new HashSet<>();
    Set<EventDataValue> edvs4 = new HashSet<>();
    edvs2.add(edv2);
    edvs3.add(edv3);
    edvs4.add(edv4);

    e1.setEventDataValues(edvs1);
    e2.setEventDataValues(edvs2);
    e3.setEventDataValues(edvs3);
    e4.setEventDataValues(edvs4);
    identifiableObjectManager.save(List.of(e1, e2, e3, e4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Event> eventSources =
        eventStore.getAllWithEventDataValuesRootKeysContainingAnyOf(
            List.of(deSource1.getUid(), deSource2.getUid()));
    List<Event> eventTarget =
        eventStore.getAllWithEventDataValuesRootKeysContainingAnyOf(List.of(deTarget.getUid()));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, eventSources, eventTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Event eventDataValues references with DataElement have the DataElement ref replaced as expected, source DataElements are deleted")
  void eventMergeSourcesDeletedTest() throws ConflictException {
    // given
    TrackedEntity trackedEntity = createTrackedEntity(ou1);
    identifiableObjectManager.save(trackedEntity);
    Enrollment enrollment = createEnrollment(program, trackedEntity, ou1);
    identifiableObjectManager.save(enrollment);
    ProgramStage stage = createProgramStage('s', 2);
    identifiableObjectManager.save(stage);

    Event e1 = createEvent(stage, enrollment, ou1);
    e1.setAttributeOptionCombo(coc1);
    Event e2 = createEvent(stage, enrollment, ou1);
    e2.setAttributeOptionCombo(coc1);
    Event e3 = createEvent(stage, enrollment, ou1);
    e3.setAttributeOptionCombo(coc1);
    Event e4 = createEvent(stage, enrollment, ou1);
    e4.setAttributeOptionCombo(coc1);

    EventDataValue edv1 = new EventDataValue(deSource1.getUid(), "value1");
    EventDataValue edv11 = new EventDataValue(deSource1.getUid(), "value11");
    EventDataValue edv2 = new EventDataValue(deSource2.getUid(), "value2");
    EventDataValue edv3 = new EventDataValue(deTarget.getUid(), "value3");
    EventDataValue edv4 = new EventDataValue(deRandom.getUid(), "value4");
    Set<EventDataValue> edvs1 = new HashSet<>();
    edvs1.add(edv1);
    edvs1.add(edv11);
    edvs1.add(edv2);
    Set<EventDataValue> edvs2 = new HashSet<>();
    Set<EventDataValue> edvs3 = new HashSet<>();
    Set<EventDataValue> edvs4 = new HashSet<>();
    edvs2.add(edv2);
    edvs3.add(edv3);
    edvs4.add(edv4);

    e1.setEventDataValues(edvs1);
    e2.setEventDataValues(edvs2);
    e3.setEventDataValues(edvs3);
    e4.setEventDataValues(edvs4);
    identifiableObjectManager.save(List.of(e1, e2, e3, e4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Event> eventSources =
        eventStore.getAllWithEventDataValuesRootKeysContainingAnyOf(
            List.of(deSource1.getUid(), deSource2.getUid()));
    List<Event> eventTarget =
        eventStore.getAllWithEventDataValuesRootKeysContainingAnyOf(List.of(deTarget.getUid()));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, eventSources, eventTarget, allDataElements);
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
    DataElementOperand deo4 = new DataElementOperand();
    deo4.setDataElement(deRandom);

    identifiableObjectManager.save(List.of(deo1, deo2, deo3, deo4));

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
    DataElementOperand deo4 = new DataElementOperand();
    deo4.setDataElement(deRandom);

    identifiableObjectManager.save(List.of(deo1, deo2, deo3, deo4));

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
    DataSet ds4 = createDataSet('4', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));

    DataSetElement dse1 = new DataSetElement(ds1, deSource1);
    DataSetElement dse2 = new DataSetElement(ds2, deSource2);
    DataSetElement dse3 = new DataSetElement(ds3, deTarget);
    DataSetElement dse4 = new DataSetElement(ds4, deRandom);

    DataElement de1 = createDataElement('g');
    de1.getDataSetElements().add(dse1);
    DataElement de2 = createDataElement('h');
    de2.getDataSetElements().add(dse2);
    DataElement de3 = createDataElement('i');
    de3.getDataSetElements().add(dse3);
    DataElement de4 = createDataElement('j');
    de4.getDataSetElements().add(dse4);

    ds1.setDataSetElements(Set.of(dse1));
    ds2.setDataSetElements(Set.of(dse2));
    ds3.setDataSetElements(Set.of(dse3));
    ds4.setDataSetElements(Set.of(dse4));

    identifiableObjectManager.save(List.of(ds1, ds2, ds3, ds4, de1, de2, de3, de4));

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
    assertEquals(2, allDataSetElsDataElement.size(), "there should be only 2 data element present");
    assertTrue(
        allDataSetElsDataElement.containsAll(List.of(deTarget, deRandom)),
        "only the target & random data element should be present in data set elements, no sources");
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
    DataSet ds4 = createDataSet('4', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));

    DataSetElement dse1 = new DataSetElement(ds1, deSource1);
    DataSetElement dse2 = new DataSetElement(ds2, deSource2);
    DataSetElement dse3 = new DataSetElement(ds3, deTarget);
    DataSetElement dse4 = new DataSetElement(ds4, deRandom);

    DataElement de1 = createDataElement('g');
    de1.getDataSetElements().add(dse1);
    DataElement de2 = createDataElement('h');
    de2.getDataSetElements().add(dse2);
    DataElement de3 = createDataElement('i');
    de3.getDataSetElements().add(dse3);
    DataElement de4 = createDataElement('j');
    de4.getDataSetElements().add(dse4);

    ds1.setDataSetElements(Set.of(dse1));
    ds2.setDataSetElements(Set.of(dse2));
    ds3.setDataSetElements(Set.of(dse3));
    ds4.setDataSetElements(Set.of(dse4));

    identifiableObjectManager.save(List.of(ds1, ds2, ds3, ds4, de1, de2, de3, de4));

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
    assertEquals(2, allDataSetElsDataElement.size(), "there should be only 2 data element present");
    assertTrue(
        allDataSetElsDataElement.contains(deTarget),
        "only the target & random data element should be present in data set elements, no sources");
    assertEquals(0, dseSources.size());
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
    identifiableObjectManager.save(List.of(ds1, ds2));

    createSectionAndSave('a', ds1, deSource1);
    createSectionAndSave('b', ds2, deSource2);
    createSectionAndSave('c', ds2, deTarget);
    createSectionAndSave('d', ds2, deRandom);

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
    identifiableObjectManager.save(List.of(ds1, ds2));

    createSectionAndSave('a', ds1, deSource1);
    createSectionAndSave('b', ds2, deSource2);
    createSectionAndSave('c', ds2, deTarget);
    createSectionAndSave('d', ds2, deRandom);

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

  // -------------------------------
  // -- DataElementGroup --
  // -------------------------------
  @Test
  @DisplayName(
      "DataElementGroup references for DataElement are replaced as expected, source DataElements are not deleted")
  void dataElementGroupMergeTest() throws ConflictException {
    // given
    DataElementGroup deg1 = createDataElementGroup('1');
    deg1.addDataElement(deSource1);
    DataElementGroup deg2 = createDataElementGroup('2');
    deg2.addDataElement(deSource2);
    DataElementGroup deg3 = createDataElementGroup('3');
    deg3.addDataElement(deTarget);
    DataElementGroup deg4 = createDataElementGroup('4');
    deg4.addDataElement(deRandom);
    identifiableObjectManager.save(List.of(deg1, deg2, deg3, deg4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<DataElementGroup> degSources =
        dataElementGroupService.getByDataElement(List.of(deSource1, deSource2));
    List<DataElementGroup> degTarget = dataElementGroupService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, degSources, degTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "DataElementGroup references for DataElement are replaced as expected, source DataElements are deleted")
  void dataElementGroupMergeSourceDeletedTest() throws ConflictException {
    // given
    DataElementGroup deg1 = createDataElementGroup('1');
    deg1.addDataElement(deSource1);
    DataElementGroup deg2 = createDataElementGroup('2');
    deg2.addDataElement(deSource2);
    DataElementGroup deg3 = createDataElementGroup('3');
    deg3.addDataElement(deTarget);
    DataElementGroup deg4 = createDataElementGroup('4');
    deg4.addDataElement(deRandom);
    identifiableObjectManager.save(List.of(deg1, deg2, deg3, deg4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<DataElementGroup> degSources =
        dataElementGroupService.getByDataElement(List.of(deSource1, deSource2));
    List<DataElementGroup> degTarget = dataElementGroupService.getByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, degSources, degTarget, allDataElements);
  }

  // -------------------------------
  // -- Indicator numerator --
  // -------------------------------
  @Test
  @DisplayName(
      "Indicator numerator references for DataElement are replaced as expected, source DataElements are not deleted")
  void indicatorNumeratorMergeTest() throws ConflictException {
    // given
    IndicatorType it = createIndicatorType('a');
    identifiableObjectManager.save(it);
    Indicator i1 = createIndicator('1', it);
    i1.setNumerator(String.format("#{expression.with.de.uid.%s}", deSource1.getUid()));
    Indicator i2 = createIndicator('2', it);
    i2.setNumerator(String.format("#{expression.with.de.uid.%s}", deSource2.getUid()));
    Indicator i3 = createIndicator('3', it);
    i3.setNumerator(String.format("#{expression.with.de.uid.%s}", deTarget.getUid()));
    Indicator i4 = createIndicator('4', it);
    i4.setNumerator(String.format("#{expression.with.de.uid.%s}", deRandom.getUid()));

    identifiableObjectManager.save(List.of(i1, i2, i3, i4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Indicator> sourceIndicators1 =
        indicatorStore.getIndicatorsWithNumeratorContaining(deSource1.getUid());
    List<Indicator> sourceIndicators2 =
        indicatorStore.getIndicatorsWithNumeratorContaining(deSource2.getUid());
    sourceIndicators1.addAll(sourceIndicators2);

    List<Indicator> indTarget =
        indicatorStore.getIndicatorsWithNumeratorContaining(deTarget.getUid());
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertTrue(
        indTarget.stream()
            .allMatch(
                i ->
                    i.getNumerator()
                        .equals(String.format("#{expression.with.de.uid.%s}", deTarget.getUid()))),
        "all expressions match expected string");
    assertMergeSuccessfulSourcesNotDeleted(report, sourceIndicators1, indTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Indicator numerator references for DataElement are replaced as expected, source DataElements are deleted")
  void indicatorNumeratorMergeSourcesDeletedTest() throws ConflictException {
    // given
    IndicatorType it = createIndicatorType('a');
    identifiableObjectManager.save(it);
    Indicator i1 = createIndicator('1', it);
    i1.setNumerator(String.format("#{expression.with.de.uid.%s}", deSource1.getUid()));
    Indicator i2 = createIndicator('2', it);
    i2.setNumerator(String.format("#{expression.with.de.uid.%s}", deSource2.getUid()));
    Indicator i3 = createIndicator('3', it);
    i3.setNumerator(String.format("#{expression.with.de.uid.%s}", deTarget.getUid()));
    Indicator i4 = createIndicator('4', it);
    i4.setNumerator(String.format("#{expression.with.de.uid.%s}", deRandom.getUid()));

    identifiableObjectManager.save(List.of(i1, i2, i3, i4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Indicator> sourceIndicators1 =
        indicatorStore.getIndicatorsWithNumeratorContaining(deSource1.getUid());
    List<Indicator> sourceIndicators2 =
        indicatorStore.getIndicatorsWithNumeratorContaining(deSource2.getUid());
    sourceIndicators1.addAll(sourceIndicators2);

    List<Indicator> indTarget =
        indicatorStore.getIndicatorsWithNumeratorContaining(deTarget.getUid());
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, sourceIndicators1, indTarget, allDataElements);
  }

  // -------------------------------
  // -- Indicator denominator --
  // -------------------------------
  @Test
  @DisplayName(
      "Indicator denominator references for DataElement are replaced as expected, source DataElements are not deleted")
  void indicatorDenominatorMergeTest() throws ConflictException {
    // given
    IndicatorType it = createIndicatorType('a');
    identifiableObjectManager.save(it);
    Indicator i1 = createIndicator('1', it);
    i1.setDenominator(String.format("#{expression.with.de.uid.%s}", deSource1.getUid()));
    Indicator i2 = createIndicator('2', it);
    i2.setDenominator(String.format("#{expression.with.de.uid.%s}", deSource2.getUid()));
    Indicator i3 = createIndicator('3', it);
    i3.setDenominator(String.format("#{expression.with.de.uid.%s}", deTarget.getUid()));
    Indicator i4 = createIndicator('4', it);
    i4.setDenominator(String.format("#{expression.with.de.uid.%s}", deRandom.getUid()));

    identifiableObjectManager.save(List.of(i1, i2, i3, i4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Indicator> sourceIndicators1 =
        indicatorStore.getIndicatorsWithDenominatorContaining(deSource1.getUid());
    List<Indicator> sourceIndicators2 =
        indicatorStore.getIndicatorsWithDenominatorContaining(deSource2.getUid());
    sourceIndicators1.addAll(sourceIndicators2);

    List<Indicator> indTarget =
        indicatorStore.getIndicatorsWithDenominatorContaining(deTarget.getUid());
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, sourceIndicators1, indTarget, allDataElements);
  }

  @Test
  @DisplayName(
      "Indicator denominator references for DataElement are replaced as expected, source DataElements are deleted")
  void indicatorDenominatorMergeSourcesDeletedTest() throws ConflictException {
    // given
    IndicatorType it = createIndicatorType('a');
    identifiableObjectManager.save(it);
    Indicator i1 = createIndicator('1', it);
    i1.setDenominator(String.format("#{expression.with.de.uid.%s}", deSource1.getUid()));
    Indicator i2 = createIndicator('2', it);
    i2.setDenominator(String.format("#{expression.with.de.uid.%s}", deSource2.getUid()));
    Indicator i3 = createIndicator('3', it);
    i3.setDenominator(String.format("#{expression.with.de.uid.%s}", deTarget.getUid()));
    Indicator i4 = createIndicator('4', it);
    i4.setDenominator(String.format("#{expression.with.de.uid.%s}", deRandom.getUid()));

    identifiableObjectManager.save(List.of(i1, i2, i3, i4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<Indicator> sourceIndicators1 =
        indicatorStore.getIndicatorsWithDenominatorContaining(deSource1.getUid());
    List<Indicator> sourceIndicators2 =
        indicatorStore.getIndicatorsWithDenominatorContaining(deSource2.getUid());
    sourceIndicators1.addAll(sourceIndicators2);

    List<Indicator> indTarget =
        indicatorStore.getIndicatorsWithDenominatorContaining(deTarget.getUid());
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, sourceIndicators1, indTarget, allDataElements);
  }

  // ---------------------------------------
  // -- Indicator numerator + denominator --
  // ---------------------------------------
  @Test
  @DisplayName(
      "Indicator numerator + denominator references for DataElement are replaced as expected, source DataElements are not deleted")
  void indicatorNumeratorDenominatorMergeTest() throws ConflictException {
    // given
    IndicatorType it = createIndicatorType('a');
    identifiableObjectManager.save(it);
    Indicator i1 = createIndicator('1', it);
    i1.setNumerator(String.format("#{expression.with.de.uid.%s}", deSource1.getUid()));
    i1.setDenominator(String.format("#{expression.with.de.uid.%s}", deSource1.getUid()));
    Indicator i2 = createIndicator('2', it);
    i2.setNumerator(String.format("#{expression.with.de.uid.%s}", deSource2.getUid()));
    i2.setDenominator(String.format("#{expression.with.de.uid.%s}", deSource2.getUid()));
    Indicator i3 = createIndicator('3', it);
    i3.setNumerator(String.format("#{expression.with.de.uid.%s}", deTarget.getUid()));
    i3.setDenominator(String.format("#{expression.with.de.uid.%s}", deTarget.getUid()));
    Indicator i4 = createIndicator('4', it);
    i4.setNumerator(String.format("#{expression.with.de.uid.%s}", deRandom.getUid()));
    i4.setDenominator(String.format("#{expression.with.de.uid.%s}", deRandom.getUid()));

    identifiableObjectManager.save(List.of(i1, i2, i3, i4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    Set<Indicator> sourceIndicators1 =
        new HashSet<>(
            Set.copyOf(indicatorStore.getIndicatorsWithNumeratorContaining(deSource1.getUid())));
    List<Indicator> sourceIndicators2 =
        indicatorStore.getIndicatorsWithNumeratorContaining(deSource2.getUid());
    List<Indicator> sourceIndicators3 =
        indicatorStore.getIndicatorsWithDenominatorContaining(deSource1.getUid());
    List<Indicator> sourceIndicators4 =
        indicatorStore.getIndicatorsWithDenominatorContaining(deSource2.getUid());
    sourceIndicators1.addAll(sourceIndicators2);
    sourceIndicators1.addAll(sourceIndicators3);
    sourceIndicators1.addAll(sourceIndicators4);

    Set<Indicator> indTarget1 =
        new HashSet<>(indicatorStore.getIndicatorsWithNumeratorContaining(deTarget.getUid()));
    List<Indicator> indTarget2 =
        indicatorStore.getIndicatorsWithDenominatorContaining(deTarget.getUid());
    indTarget1.addAll(indTarget2);

    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, sourceIndicators1, indTarget1, allDataElements);
  }

  @Test
  @DisplayName(
      "Indicator numerator + denominator references for DataElement are replaced as expected, source DataElements are deleted")
  void indicatorNumeratorDenominatorMergeSourcesDeletedTest() throws ConflictException {
    // given
    IndicatorType it = createIndicatorType('a');
    identifiableObjectManager.save(it);
    Indicator i1 = createIndicator('1', it);
    i1.setDenominator(String.format("#{expression.with.de.uid.%s}", deSource1.getUid()));
    Indicator i2 = createIndicator('2', it);
    i2.setDenominator(String.format("#{expression.with.de.uid.%s}", deSource2.getUid()));
    Indicator i3 = createIndicator('3', it);
    i3.setDenominator(String.format("#{expression.with.de.uid.%s}", deTarget.getUid()));
    Indicator i4 = createIndicator('4', it);
    i4.setNumerator(String.format("#{expression.with.de.uid.%s}", deRandom.getUid()));
    i4.setDenominator(String.format("#{expression.with.de.uid.%s}", deRandom.getUid()));

    identifiableObjectManager.save(List.of(i1, i2, i3, i4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    Set<Indicator> sourceIndicators1 =
        new HashSet<>(indicatorStore.getIndicatorsWithDenominatorContaining(deSource1.getUid()));
    List<Indicator> sourceIndicators2 =
        indicatorStore.getIndicatorsWithDenominatorContaining(deSource2.getUid());
    sourceIndicators1.addAll(sourceIndicators2);

    Set<Indicator> indTarget =
        new HashSet<>(indicatorStore.getIndicatorsWithDenominatorContaining(deTarget.getUid()));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, sourceIndicators1, indTarget, allDataElements);
  }

  // ------------------------
  // -- DataEntryForm html --
  // ------------------------
  @Test
  @DisplayName(
      "DataEntryForm html references for DataElement are replaced as expected, source DataElements are not deleted")
  void formHtmlMergeTest() throws ConflictException {
    // given
    DataEntryForm form1 =
        createDataEntryForm(
            'a', String.format("<body>form-with.#{%s}.uid11</body>", deSource1.getUid()));
    DataEntryForm form2 =
        createDataEntryForm(
            'b', String.format("<body>form-with.#{%s}.uid11</body>", deSource2.getUid()));
    DataEntryForm form3 =
        createDataEntryForm(
            'c', String.format("<body>form-with.#{%s}.uid11</body>", deTarget.getUid()));
    DataEntryForm form4 =
        createDataEntryForm(
            'd', String.format("<body>form-with.#{%s}.uid11</body>", deRandom.getUid()));
    identifiableObjectManager.save(List.of(form1, form2, form3, form4));

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    Set<DataEntryForm> sourceForms =
        new HashSet<>(
            Set.copyOf(dataEntryFormStore.getDataEntryFormsHtmlContaining(deSource1.getUid())));
    List<DataEntryForm> sourceForms2 =
        dataEntryFormStore.getDataEntryFormsHtmlContaining(deSource2.getUid());
    sourceForms.addAll(sourceForms2);

    Set<DataEntryForm> targetForms =
        new HashSet<>(dataEntryFormStore.getDataEntryFormsHtmlContaining(deTarget.getUid()));

    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, sourceForms, targetForms, allDataElements);
  }

  @Test
  @DisplayName(
      "DataEntryForm html references for DataElement are replaced as expected, source DataElements are deleted")
  void formHtmlMergeSourceDeletedTest() throws ConflictException {
    // given
    DataEntryForm form1 =
        createDataEntryForm(
            'a', String.format("<body>form-with.#{%s}.uid11</body>", deSource1.getUid()));
    DataEntryForm form2 =
        createDataEntryForm(
            'b', String.format("<body>form-with.#{%s}.uid11</body>", deSource2.getUid()));
    DataEntryForm form3 =
        createDataEntryForm(
            'c', String.format("<body>form-with.#{%s}.uid11</body>", deTarget.getUid()));
    DataEntryForm form4 =
        createDataEntryForm(
            'd', String.format("<body>form-with.#{%s}.uid11</body>", deRandom.getUid()));
    identifiableObjectManager.save(List.of(form1, form2, form3, form4));

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    Set<DataEntryForm> sourceForms =
        new HashSet<>(
            Set.copyOf(dataEntryFormStore.getDataEntryFormsHtmlContaining(deSource1.getUid())));
    List<DataEntryForm> sourceForms2 =
        dataEntryFormStore.getDataEntryFormsHtmlContaining(deSource2.getUid());
    sourceForms.addAll(sourceForms2);

    Set<DataEntryForm> targetForms =
        new HashSet<>(dataEntryFormStore.getDataEntryFormsHtmlContaining(deTarget.getUid()));

    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, sourceForms, targetForms, allDataElements);
  }

  // ------------------------
  // -- DataDimensionItem --
  // ------------------------
  @Test
  @DisplayName(
      "DataDimensionItems with references for DataElement are replaced as expected, source DataElements are not deleted")
  void dataDimItemMergeTest() throws ConflictException {
    // given
    DataDimensionItem item1 = DataDimensionItem.create(deSource1);
    DataDimensionItem item2 = DataDimensionItem.create(deSource2);
    DataDimensionItem item3 = DataDimensionItem.create(deTarget);
    DataDimensionItem item4 = DataDimensionItem.create(deRandom);

    dataDimensionItemStore.save(item1);
    dataDimensionItemStore.save(item2);
    dataDimensionItemStore.save(item3);
    dataDimensionItemStore.save(item4);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<DataDimensionItem> sourceItems =
        dataDimensionItemStore.getDataElementDataDimensionItems(List.of(deSource1, deSource2));
    List<DataDimensionItem> targetItems =
        dataDimensionItemStore.getDataElementDataDimensionItems(List.of(deTarget));

    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesNotDeleted(report, sourceItems, targetItems, allDataElements);
  }

  @Test
  @DisplayName(
      "DataDimensionItems with references for DataElement are replaced as expected, source DataElements are deleted")
  void dataDimItemMergeSourcesDeletedTest() throws ConflictException {
    // given
    DataDimensionItem item1 = DataDimensionItem.create(deSource1);
    DataDimensionItem item2 = DataDimensionItem.create(deSource2);
    DataDimensionItem item3 = DataDimensionItem.create(deTarget);
    DataDimensionItem item4 = DataDimensionItem.create(deRandom);

    dataDimensionItemStore.save(item1);
    dataDimensionItemStore.save(item2);
    dataDimensionItemStore.save(item3);
    dataDimensionItemStore.save(item4);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<DataDimensionItem> sourceItems =
        dataDimensionItemStore.getDataElementDataDimensionItems(List.of(deSource1, deSource2));
    List<DataDimensionItem> targetItems =
        dataDimensionItemStore.getDataElementDataDimensionItems(List.of(deTarget));

    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertMergeSuccessfulSourcesDeleted(report, sourceItems, targetItems, allDataElements);
  }

  private void assertMergeSuccessfulSourcesNotDeleted(
      MergeReport report,
      Collection<?> sources,
      Collection<?> target,
      Collection<DataElement> dataElements) {
    assertFalse(report.hasErrorMessages());
    assertEquals(0, sources.size(), "Expect 0 source data elements present");
    assertEquals(3, target.size(), "Expect 3 target data elements present");
    assertEquals(4, dataElements.size(), "Expect 4 data elements present");
    assertTrue(dataElements.containsAll(List.of(deTarget, deSource1, deSource2)));
  }

  private void assertMergeSuccessfulSourcesDeleted(
      MergeReport report,
      Collection<?> sources,
      Collection<?> target,
      Collection<DataElement> dataElements) {
    assertFalse(report.hasErrorMessages());
    assertEquals(0, sources.size(), "Expect 0 source data elements present");
    assertEquals(3, target.size(), "Expect 3 target data elements present");
    assertEquals(2, dataElements.size(), "Expect 2 data elements present");
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

  private Predictor createPredictor(
      char id, DataElement de, DataElement generator, DataElement skipTest) {
    return createPredictor(
        de,
        coc1,
        String.valueOf(id),
        new Expression(String.format("#{%s.uid00001}", generator.getUid()), "test" + id),
        new Expression(String.format("#{%s.uid00001}", skipTest.getUid()), "test2" + id),
        PeriodType.getPeriodType(PeriodTypeEnum.DAILY),
        oul,
        0,
        0,
        0);
  }

  private Predictor createPredictorWithGenerator(char id, DataElement de, DataElement generator) {
    return createPredictor(id, de, generator, deRandom);
  }

  private Predictor createPredictorWithSkipTest(char id, DataElement de, DataElement skipTest) {
    return createPredictor(id, de, deRandom, skipTest);
  }

  private Predictor createPredictor(char id, DataElement de) {
    return createPredictor(id, de, deRandom, deRandom);
  }
}