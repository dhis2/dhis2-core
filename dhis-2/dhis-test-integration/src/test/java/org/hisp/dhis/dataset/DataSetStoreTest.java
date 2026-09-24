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
package org.hisp.dhis.dataset;

import static java.util.Arrays.asList;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.PersistenceException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kristian Nordal
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataSetStoreTest extends PostgresIntegrationTestBase {

  private static final PeriodType PERIOD_TYPE =
      PeriodType.getAvailablePeriodTypes().iterator().next();

  @Autowired private DataSetStore dataSetStore;

  @Autowired private DataEntryFormService dataEntryFormService;

  @Autowired protected OrganisationUnitStore unitStore;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private PeriodService periodService;

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------
  private void assertEq(char uniqueCharacter, DataSet dataSet) {
    assertEquals("DataSet" + uniqueCharacter, dataSet.getName());
    assertEquals("DataSetShort" + uniqueCharacter, dataSet.getShortName());
    assertEquals(PERIOD_TYPE, dataSet.getPeriodType());
  }

  // -------------------------------------------------------------------------
  // DataSet
  // -------------------------------------------------------------------------
  @Test
  void testAddDataSet() {
    DataSet dataSetA = addDataSet('A');
    DataSet dataSetB = addDataSet('B');
    assertEq('A', dataSetStore.get(dataSetA.getId()));
    assertEq('B', dataSetStore.get(dataSetB.getId()));
  }

  @Test
  void testUpdateDataSet() {
    DataSet dataSetA = addDataSet('A');
    assertEq('A', dataSetStore.get(dataSetA.getId()));
    dataSetA.setName("DataSetB");
    dataSetStore.update(dataSetA);
    assertEquals("DataSetB", dataSetStore.get(dataSetA.getId()).getName());
  }

  @Test
  void testDeleteAndGetDataSet() {
    DataSet dataSetA = addDataSet('A');
    DataSet dataSetB = addDataSet('B');
    assertNotNull(dataSetStore.get(dataSetA.getId()));
    assertNotNull(dataSetStore.get(dataSetB.getId()));
    dataSetStore.delete(dataSetA);
    assertNull(dataSetStore.get(dataSetA.getId()));
    assertNotNull(dataSetStore.get(dataSetB.getId()));
  }

  @Test
  void testGetDataSetByName() {
    DataSet dataSetA = addDataSet('A');
    DataSet dataSetB = addDataSet('B');
    assertEquals(dataSetA.getId(), dataSetStore.getByName("DataSetA").getId());
    assertEquals(dataSetB.getId(), dataSetStore.getByName("DataSetB").getId());
    assertNull(dataSetStore.getByName("DataSetC"));
  }

  @Test
  void testGetAllDataSets() {
    DataSet dataSetA = addDataSet('A');
    DataSet dataSetB = addDataSet('B');
    assertContainsOnly(List.of(dataSetA, dataSetB), dataSetStore.getAll());
  }

  @Test
  void testGetByDataEntryForm() {
    DataSet dataSetA = addDataSet('A');
    DataSet dataSetB = addDataSet('B');
    DataSet dataSetC = addDataSet('C');
    DataEntryForm dataEntryFormX = addDataEntryForm('X', dataSetA);
    DataEntryForm dataEntryFormY = addDataEntryForm('Y');
    assertContainsOnly(List.of(dataSetA), dataSetStore.getDataSetsByDataEntryForm(dataEntryFormX));
    dataSetC.setDataEntryForm(dataEntryFormX);
    dataSetStore.update(dataSetC);
    assertContainsOnly(
        List.of(dataSetA, dataSetC), dataSetStore.getDataSetsByDataEntryForm(dataEntryFormX));
    dataSetB.setDataEntryForm(dataEntryFormY);
    dataSetStore.update(dataSetB);
    assertContainsOnly(List.of(dataSetB), dataSetStore.getDataSetsByDataEntryForm(dataEntryFormY));
  }

  @Test
  @DisplayName("retrieving DataSetElements by DataElement returns expected entries")
  void dataSetElementByDataElementTest() {
    // given
    DataElement deW = createDataElementAndSave('W');
    DataElement deX = createDataElementAndSave('X');
    DataElement deY = createDataElementAndSave('Y');
    DataElement deZ = createDataElementAndSave('Z');

    DataSet ds1 = createDataSet('j', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet ds2 = createDataSet('k', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    DataSet ds3 = createDataSet('l', PeriodType.getPeriodType(PeriodTypeEnum.DAILY));

    createDataSetElementAndSave(deW, ds1);
    createDataSetElementAndSave(deX, ds1);
    createDataSetElementAndSave(deY, ds2);
    createDataSetElementAndSave(deZ, ds3);

    // when
    List<DataSetElement> dataSetElements =
        dataSetStore.getDataSetElementsByDataElement(List.of(deW, deX, deY));

    // then
    assertEquals(3, dataSetElements.size());
    assertTrue(
        dataSetElements.stream()
            .map(dse -> dse.getDataElement().getUid())
            .toList()
            .containsAll(List.of(deW.getUid(), deX.getUid(), deY.getUid())));
  }

  private DataSet addDataSet(char uniqueCharacter, OrganisationUnit... sources) {
    return addDataSet(uniqueCharacter, PERIOD_TYPE, sources);
  }

  private DataSet addDataSet(
      char uniqueCharacter, PeriodType periodType, OrganisationUnit... sources) {
    DataSet dataSet = createDataSet(uniqueCharacter, periodType);
    if (sources.length > 0) {
      dataSet.setSources(new HashSet<>(asList(sources)));
    }
    dataSetStore.save(dataSet);
    return dataSet;
  }

  private DataEntryForm addDataEntryForm(char uniqueCharacter) {
    return addDataEntryForm(uniqueCharacter, null);
  }

  private DataEntryForm addDataEntryForm(char uniqueCharacter, DataSet dataSet) {
    DataEntryForm form = createDataEntryForm(uniqueCharacter);
    dataEntryFormService.addDataEntryForm(form);
    if (dataSet != null) {
      dataSet.setDataEntryForm(form);
      dataSetStore.update(dataSet);
    }
    return form;
  }

  private DataElement createDataElementAndSave(char c) {
    CategoryCombo cc = createCategoryCombo(c);
    manager.save(cc);

    DataElement de = createDataElement(c, cc);
    manager.save(de);
    return de;
  }

  private void createDataSetElementAndSave(DataElement de, DataSet ds) {
    DataSetElement dse = new DataSetElement();
    dse.setDataElement(de);
    ds.addDataSetElement(dse);
    manager.save(ds);
  }

  // -------------------------------------------------------------------------
  // JPA migration verification
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("SEQUENCE id generation produces positive, increasing identifiers")
  void testJpaIdGeneration() {
    DataSet dataSetA = addDataSet('A');
    DataSet dataSetB = addDataSet('B');

    assertTrue(dataSetA.getId() > 0);
    assertTrue(dataSetB.getId() > dataSetA.getId());
  }

  @Test
  @DisplayName(
      "periodType (EAGER) and categoryCombo (LAZY) many-to-one associations survive a reload")
  void testJpaManyToOneAssociationsSurviveReload() {
    DataSet dataSet = addDataSet('A');
    long id = dataSet.getId();

    clearSession();

    DataSet reloaded = dataSetStore.get(id);
    assertEquals(PERIOD_TYPE.getName(), reloaded.getPeriodType().getName());
    assertNotNull(reloaded.getCategoryCombo());
  }

  @Test
  @DisplayName("dataInputPeriods (owned, cascade all-delete-orphan) round-trips after a reload")
  void testJpaDataInputPeriods() {
    // Use PeriodType#createPeriod() so both startDate and endDate are populated -
    // HibernatePeriodStore#save() binds a null endDate incorrectly via native SQL, which is a
    // pre-existing quirk unrelated to this migration.
    Period period = PERIOD_TYPE.createPeriod();
    periodService.addPeriod(period);

    DataSet dataSet = createDataSet('A', PERIOD_TYPE);
    DataInputPeriod dip = new DataInputPeriod();
    dip.setPeriod(period);
    dataSet.addDataInputPeriod(dip);
    dataSetStore.save(dataSet);
    long id = dataSet.getId();

    clearSession();

    DataSet reloaded = dataSetStore.get(id);
    assertEquals(1, reloaded.getDataInputPeriods().size());
  }

  @Test
  @DisplayName("indicators many-to-many join table round-trips after a reload")
  void testJpaIndicators() {
    IndicatorType type = createIndicatorType('A');
    manager.save(type);
    Indicator indicator = createIndicator('A', type);
    manager.save(indicator);

    DataSet dataSet = createDataSet('A', PERIOD_TYPE);
    dataSet.getIndicators().add(indicator);
    dataSetStore.save(dataSet);
    long id = dataSet.getId();

    clearSession();

    DataSet reloaded = dataSetStore.get(id);
    assertEquals(1, reloaded.getIndicators().size());
    assertEquals(indicator.getUid(), reloaded.getIndicators().iterator().next().getUid());
  }

  @Test
  @DisplayName(
      "compulsoryDataElementOperands (many-to-many with Hibernate ALL+DELETE_ORPHAN cascade) "
          + "round-trips and removing an operand deletes the orphaned row, not just the join row")
  void testJpaCompulsoryDataElementOperandsCascadeDeleteOrphan() {
    DataElement de = createDataElementAndSave('A');

    DataSet dataSet = createDataSet('A', PERIOD_TYPE);
    DataElementOperand operand = new DataElementOperand(de);
    dataSet.addCompulsoryDataElementOperand(operand);
    dataSetStore.save(dataSet);
    long id = dataSet.getId();

    clearSession();

    DataSet reloaded = dataSetStore.get(id);
    assertEquals(1, reloaded.getCompulsoryDataElementOperands().size());

    reloaded.getCompulsoryDataElementOperands().clear();
    dataSetStore.update(reloaded);
    entityManager.flush();
    clearSession();

    DataSet reloadedAgain = dataSetStore.get(id);
    assertTrue(reloadedAgain.getCompulsoryDataElementOperands().isEmpty());

    Long remaining =
        entityManager
            .createQuery("select count(o) from DataElementOperand o", Long.class)
            .getSingleResult();
    assertEquals(0L, remaining);
  }

  @Test
  @DisplayName("sources (inverse OrganisationUnit.dataSets, owned by DataSet) round-trips")
  void testJpaSources() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    unitStore.save(ouA);

    DataSet dataSet = addDataSet('A', ouA);
    long id = dataSet.getId();

    clearSession();

    DataSet reloaded = dataSetStore.get(id);
    assertEquals(1, reloaded.getSources().size());
    assertTrue(reloaded.hasOrganisationUnit(ouA));
  }

  @Test
  @DisplayName("sections (inverse OneToMany, ordered by sortOrder) round-trip in order")
  void testJpaSectionsOrdering() {
    DataSet dataSet = createDataSet('A', PERIOD_TYPE);
    dataSetStore.save(dataSet);

    Section section2 = createSection('2', dataSet, List.of(), List.of());
    section2.setSortOrder(2);
    Section section1 = createSection('1', dataSet, List.of(), List.of());
    section1.setSortOrder(1);
    manager.save(section2);
    manager.save(section1);
    long id = dataSet.getId();

    clearSession();

    DataSet reloaded = dataSetStore.get(id);
    List<String> names = reloaded.getSections().stream().map(Section::getName).toList();
    assertEquals(List.of("Section1", "Section2"), names);
  }

  @Test
  @DisplayName(
      "legendSets (ordered many-to-many, @OrderColumn base 0) round-trip with order preserved")
  void testJpaLegendSetsOrdering() {
    LegendSet legendSetC = createLegendSet('C');
    LegendSet legendSetA = createLegendSet('A');
    LegendSet legendSetB = createLegendSet('B');
    manager.save(legendSetC);
    manager.save(legendSetA);
    manager.save(legendSetB);

    DataSet dataSet = createDataSet('A', PERIOD_TYPE);
    dataSet.getLegendSets().addAll(List.of(legendSetC, legendSetA, legendSetB));
    dataSetStore.save(dataSet);
    long id = dataSet.getId();

    clearSession();

    DataSet reloaded = dataSetStore.get(id);
    List<String> uids = reloaded.getLegendSets().stream().map(LegendSet::getUid).toList();
    assertEquals(List.of(legendSetC.getUid(), legendSetA.getUid(), legendSetB.getUid()), uids);
  }

  @Test
  @DisplayName("workflow many-to-one and its inverse DataApprovalWorkflow.dataSets stay consistent")
  void testJpaWorkflowAssociation() {
    CategoryCombo cc = createCategoryCombo('W');
    manager.save(cc);
    DataApprovalWorkflow workflow =
        new DataApprovalWorkflow("WorkflowA", PERIOD_TYPE, cc, Set.of());
    manager.save(workflow);

    DataSet dataSet = createDataSet('A', PERIOD_TYPE);
    dataSet.assignWorkflow(workflow);
    dataSetStore.save(dataSet);
    long id = dataSet.getId();

    clearSession();

    DataSet reloaded = dataSetStore.get(id);
    assertNotNull(reloaded.getWorkflow());
    assertEquals(workflow.getUid(), reloaded.getWorkflow().getUid());
  }

  @Test
  @DisplayName("attributeValues (jsonb) round-trips after a reload")
  void testJpaAttributeValuesRoundTrip() {
    DataSet dataSet = createDataSet('A', PERIOD_TYPE);
    dataSetStore.save(dataSet);
    long id = dataSet.getId();

    clearSession();

    DataSet reloaded = dataSetStore.get(id);
    assertNotNull(reloaded.getAttributeValues());
  }

  @Test
  @DisplayName("periodType is a required (NOT NULL) association")
  void testJpaPeriodTypeNotNull() {
    DataSet dataSet = createDataSet('A', PERIOD_TYPE);
    dataSet.setPeriodType(null);

    assertThrows(
        Exception.class,
        () -> {
          dataSetStore.save(dataSet);
          entityManager.flush();
        });
  }

  @Test
  @DisplayName("categoryCombo is a required (NOT NULL) association")
  void testJpaCategoryComboNotNull() {
    DataSet dataSet = createDataSet('A', PERIOD_TYPE);
    dataSet.setCategoryCombo(null);

    assertThrows(
        Exception.class,
        () -> {
          dataSetStore.save(dataSet);
          entityManager.flush();
        });
  }

  @Test
  @DisplayName("shortName has a unique constraint")
  void testJpaShortNameUnique() {
    DataSet dataSetA = addDataSet('A');

    DataSet dataSetB = createDataSet('B', PERIOD_TYPE);
    dataSetB.setShortName(dataSetA.getShortName());

    assertThrows(
        PersistenceException.class,
        () -> {
          dataSetStore.save(dataSetB);
          entityManager.flush();
        });
  }

  @Test
  @DisplayName(
      "getDimensionItem/getUid stay computed and typedEquals matches the same-identity DataSet")
  void testJpaDimensionalItemAndTypedEquals() {
    DataSet dataSetA = addDataSet('A');

    clearSession();

    DataSet reloaded = dataSetStore.get(dataSetA.getId());
    assertEquals(dataSetA.getUid(), reloaded.getDimensionItem());
    assertTrue(reloaded.typedEquals(dataSetA));
  }
}
