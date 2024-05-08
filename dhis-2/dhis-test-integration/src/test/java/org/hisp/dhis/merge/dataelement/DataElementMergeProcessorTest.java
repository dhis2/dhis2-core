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

import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataElementMergeProcessorTest extends IntegrationTestBase {

  @Autowired private DataElementService dataElementService;
  @Autowired private DataElementMergeProcessor mergeProcessor;
  @Autowired private IdentifiableObjectManager idObjectManager;
  @Autowired private MinMaxDataElementStore minMaxDataElementStore;
  @Autowired private MinMaxDataElementService minMaxDataElementService;
  @Autowired private AnalyticalObjectStore<EventVisualization> eventVisualizationStore;
  @Autowired private OrganisationUnitService orgUnitService;
  @Autowired private SMSCommandStore smsCommandStore;

  private DataElement deSource1;
  private DataElement deSource2;
  private DataElement deTarget;
  private OrganisationUnit ou1;
  private OrganisationUnit ou2;
  private OrganisationUnit ou3;
  private CategoryOptionCombo coc1;

  @Override
  public void setUpTest() {
    // data elements
    deSource1 = createDataElement('A');
    deSource2 = createDataElement('B');
    deTarget = createDataElement('C');
    idObjectManager.save(List.of(deSource1, deSource2, deTarget));

    // org unit
    ou1 = createOrganisationUnit('A');
    ou2 = createOrganisationUnit('B');
    ou3 = createOrganisationUnit('C');
    idObjectManager.save(List.of(ou1, ou2, ou3));

    // cat option combo
    coc1 = categoryService.getDefaultCategoryOptionCombo();
    idObjectManager.save(coc1);
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
    // min max data elements
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(deSource1, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(deSource2, ou2, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(deTarget, ou3, coc1, 0, 100, false);
    minMaxDataElementStore.save(minMaxDataElement1);
    minMaxDataElementStore.save(minMaxDataElement2);
    minMaxDataElementStore.save(minMaxDataElement3);

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

    assertFalse(report.hasErrorMessages());
    assertEquals(0, minMaxSources.size());
    assertEquals(3, minMaxTarget.size());
    assertEquals(3, allDataElements.size());
    assertTrue(allDataElements.containsAll(List.of(deTarget, deSource1, deSource2)));
  }

  @Test
  @DisplayName(
      "MinMaxDataElement references for DataElement are replaced as expected, source DataElements are deleted")
  void minMaxDataElementMergeDeleteSourcesTest() throws ConflictException {
    // given
    // min max data elements
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(deSource1, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(deSource2, ou2, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(deTarget, ou3, coc1, 0, 100, false);
    minMaxDataElementStore.save(minMaxDataElement1);
    minMaxDataElementStore.save(minMaxDataElement2);
    minMaxDataElementStore.save(minMaxDataElement3);

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

    assertFalse(report.hasErrorMessages());
    assertEquals(0, minMaxSources.size());
    assertEquals(3, minMaxTarget.size());
    assertEquals(1, allDataElements.size());
    assertTrue(allDataElements.contains(deTarget));
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
    minMaxDataElementStore.save(minMaxDataElement1);
    minMaxDataElementStore.save(minMaxDataElement2);
    minMaxDataElementStore.save(minMaxDataElement3);

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

    idObjectManager.save(eventVis1);
    idObjectManager.save(eventVis2);
    idObjectManager.save(eventVis3);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<EventVisualization> eventVizSources =
        eventVisualizationStore.getEventVisualizationsByDataElement(List.of(deSource1, deSource2));
    List<EventVisualization> allByDataElement =
        eventVisualizationStore.getEventVisualizationsByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, eventVizSources.size());
    assertEquals(3, allByDataElement.size());
    assertEquals(3, allDataElements.size());
    assertTrue(allDataElements.containsAll(List.of(deTarget, deSource1, deSource2)));
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

    idObjectManager.save(eventVis4);
    idObjectManager.save(eventVis5);
    idObjectManager.save(eventVis6);

    // params
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(deSource1.getUid(), deSource2.getUid())));
    mergeParams.setTarget(UID.of(deTarget.getUid()));
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<EventVisualization> eventVizSources =
        eventVisualizationStore.getEventVisualizationsByDataElement(List.of(deSource1, deSource2));
    List<EventVisualization> allByDataElement =
        eventVisualizationStore.getEventVisualizationsByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, eventVizSources.size());
    assertEquals(3, allByDataElement.size());
    assertEquals(1, allDataElements.size());
    assertTrue(allDataElements.contains(deTarget));
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

    smsCommandStore.save(smsCommand);

    // params
    MergeParams mergeParams = getMergeParams();

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<SMSCode> smsCommands =
        smsCommandStore.getCodesByDataElement(List.of(deSource1, deSource2));
    List<SMSCode> allByDataElement = smsCommandStore.getCodesByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, smsCommands.size());
    assertEquals(3, allByDataElement.size());
    assertEquals(3, allDataElements.size());
    assertTrue(allDataElements.containsAll(List.of(deTarget, deSource1, deSource2)));
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

    smsCommandStore.save(smsCommand);

    // params
    MergeParams mergeParams = getMergeParams();
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<SMSCode> smsCommands =
        smsCommandStore.getCodesByDataElement(List.of(deSource1, deSource2));
    List<SMSCode> allByDataElement = smsCommandStore.getCodesByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, smsCommands.size());
    assertEquals(3, allByDataElement.size());
    assertEquals(1, allDataElements.size());
    assertTrue(allDataElements.contains(deTarget));
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
}
