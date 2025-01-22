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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataValueStoreTest extends PostgresIntegrationTestBase {
  private @PersistenceContext EntityManager manager;
  @Autowired private DataValueStore dataValueStore;

  @Test
  void testGetSoftDeletedDataValue() {
    Period period = createPeriod(new Date(), new Date());
    DataValue dataValue = createDataValue('A', period, "test");
    dataValueStore.addDataValue(dataValue);
    dataValue.setDeleted(true);
    dataValueStore.updateDataValue(dataValue);
    DataValue deletedDataValue = dataValueStore.getSoftDeletedDataValue(dataValue);
    assertEquals(dataValue.getDataElement().getId(), deletedDataValue.getDataElement().getId());
    assertEquals(dataValue.getSource().getId(), deletedDataValue.getSource().getId());
    assertEquals(
        dataValue.getCategoryOptionCombo().getId(),
        deletedDataValue.getCategoryOptionCombo().getId());
    assertEquals(
        dataValue.getAttributeOptionCombo().getId(),
        deletedDataValue.getAttributeOptionCombo().getId());
    assertEquals(dataValue.getValue(), deletedDataValue.getValue());
  }

  @Test
  @DisplayName(
      "Merging duplicate DataValues (cat opt combos) leaves only the last updated (source) value remaining")
  void mergeDvWithDuplicatesKeepSource() {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata();

    Period p1 = createPeriod(DateUtils.getDate(2024, 1, 1), DateUtils.getDate(2023, 2, 1));

    DataElement de = createDataElement('z');
    manager.persist(de);

    OrganisationUnit ou = createOrganisationUnit("org u 1");
    manager.persist(ou);

    // data values with same period, org unit, data element and attr opt combo
    // which will be identified as duplicates during merging
    DataValue dv1 = createDataValue('1', p1, "dv test 1");
    dv1.setCategoryOptionCombo(categoryMetadata.coc1());
    dv1.setAttributeOptionCombo(categoryMetadata.coc4());
    dv1.setDataElement(de);
    dv1.setSource(ou);
    dv1.setLastUpdated(DateUtils.parseDate("2024-12-01"));

    DataValue dv2 = createDataValue('2', p1, "dv test 2 - last updated");
    dv2.setCategoryOptionCombo(categoryMetadata.coc2());
    dv2.setAttributeOptionCombo(categoryMetadata.coc4());
    dv2.setDataElement(de);
    dv2.setSource(ou);
    dv2.setLastUpdated(DateUtils.parseDate("2025-01-08"));

    DataValue dv3 = createDataValue('3', p1, "dv test 3");
    dv3.setCategoryOptionCombo(categoryMetadata.coc3());
    dv3.setAttributeOptionCombo(categoryMetadata.coc4());
    dv3.setDataElement(de);
    dv3.setSource(ou);
    dv3.setLastUpdated(DateUtils.parseDate("2024-12-06"));

    DataValue dv4 = createDataValue('4', p1, "dv test 4, untouched");
    dv4.setCategoryOptionCombo(categoryMetadata.coc4());
    dv4.setAttributeOptionCombo(categoryMetadata.coc4());
    dv4.setDataElement(de);
    dv4.setSource(ou);
    dv4.setLastUpdated(DateUtils.parseDate("2024-11-02"));

    addDataValues(dv1, dv2, dv3, dv4);

    // check pre merge state
    List<DataValue> preMergeState =
        dataValueStore.getAllDataValues().stream()
            .filter(dv -> dv.getDataElement().getUid().equals(de.getUid()))
            .toList();

    assertEquals(4, preMergeState.size(), "there should be 4 data values");
    checkCocIdsPresent(
        preMergeState,
        List.of(
            categoryMetadata.coc1().getId(),
            categoryMetadata.coc2().getId(),
            categoryMetadata.coc3().getId(),
            categoryMetadata.coc4().getId()));

    // when
    mergeDataValues(
        categoryMetadata.coc3(), List.of(categoryMetadata.coc1(), categoryMetadata.coc2()));

    // then
    List<DataValue> postMergeState =
        dataValueStore.getAllDataValues().stream()
            .filter(dv -> dv.getDataElement().getUid().equals(de.getUid()))
            .toList();

    assertEquals(2, postMergeState.size(), "there should be 2 data values");
    checkCocIdsPresent(
        preMergeState, List.of(categoryMetadata.coc3().getId(), categoryMetadata.coc4().getId()));

    checkDataValuesPresent(
        postMergeState, List.of("dv test 2 - last updated", "dv test 4, untouched"));

    checkDatesPresent(
        postMergeState,
        List.of(DateUtils.parseDate("2025-01-08"), DateUtils.parseDate("2024-11-02")));
  }

  @Test
  @DisplayName(
      "Merging duplicate DataValues (cat opt combos) leaves only the last updated (target) value remaining")
  void mergeDvWithDuplicatesKeepTarget() {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata();

    Period p1 = createPeriod(DateUtils.getDate(2024, 1, 1), DateUtils.getDate(2023, 2, 1));

    DataElement de = createDataElement('z');
    manager.persist(de);

    OrganisationUnit ou = createOrganisationUnit("org u 1");
    manager.persist(ou);

    // data values with same period, org unit, data element and attr opt combo
    // which will be identified as duplicates during merging
    DataValue dv1 = createDataValue('1', p1, "dv test 1");
    dv1.setCategoryOptionCombo(categoryMetadata.coc1());
    dv1.setAttributeOptionCombo(categoryMetadata.coc4());
    dv1.setDataElement(de);
    dv1.setSource(ou);
    dv1.setLastUpdated(DateUtils.parseDate("2024-12-01"));

    DataValue dv2 = createDataValue('2', p1, "dv test 2");
    dv2.setCategoryOptionCombo(categoryMetadata.coc2());
    dv2.setAttributeOptionCombo(categoryMetadata.coc4());
    dv2.setDataElement(de);
    dv2.setSource(ou);
    dv2.setLastUpdated(DateUtils.parseDate("2025-01-02"));

    DataValue dv3 = createDataValue('3', p1, "dv test 3 - last updated");
    dv3.setCategoryOptionCombo(categoryMetadata.coc3());
    dv3.setAttributeOptionCombo(categoryMetadata.coc4());
    dv3.setDataElement(de);
    dv3.setSource(ou);
    dv3.setLastUpdated(DateUtils.parseDate("2025-01-06"));

    DataValue dv4 = createDataValue('4', p1, "dv test 4, untouched");
    dv4.setCategoryOptionCombo(categoryMetadata.coc4());
    dv4.setAttributeOptionCombo(categoryMetadata.coc4());
    dv4.setDataElement(de);
    dv4.setSource(ou);
    dv4.setLastUpdated(DateUtils.parseDate("2024-11-02"));

    addDataValues(dv1, dv2, dv3, dv4);

    // check pre merge state
    List<DataValue> preMergeState =
        dataValueStore.getAllDataValues().stream()
            .filter(dv -> dv.getDataElement().getUid().equals(de.getUid()))
            .toList();

    assertEquals(4, preMergeState.size(), "there should be 4 data values");
    checkCocIdsPresent(
        preMergeState,
        List.of(
            categoryMetadata.coc1().getId(),
            categoryMetadata.coc2().getId(),
            categoryMetadata.coc3().getId(),
            categoryMetadata.coc4().getId()));

    // when
    mergeDataValues(
        categoryMetadata.coc3(), List.of(categoryMetadata.coc1(), categoryMetadata.coc2()));

    // then
    List<DataValue> postMergeState =
        dataValueStore.getAllDataValues().stream()
            .filter(dv -> dv.getDataElement().getUid().equals(de.getUid()))
            .toList();

    assertEquals(2, postMergeState.size(), "there should be 2 data values");
    checkCocIdsPresent(
        postMergeState, List.of(categoryMetadata.coc3().getId(), categoryMetadata.coc4().getId()));

    checkDataValuesPresent(
        postMergeState, List.of("dv test 3 - last updated", "dv test 4, untouched"));

    checkDatesPresent(
        postMergeState,
        List.of(DateUtils.parseDate("2025-01-06"), DateUtils.parseDate("2024-11-02")));
  }

  @Test
  @DisplayName(
      "Merging non-duplicate DataValues (cat opt combos) updates the cat opt combo value only")
  void mergeDvWithNoDuplicates() {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata();

    Period p1 = createPeriod(DateUtils.getDate(2024, 1, 1), DateUtils.getDate(2023, 2, 1));
    Period p2 = createPeriod(DateUtils.getDate(2024, 2, 1), DateUtils.getDate(2023, 3, 1));
    Period p3 = createPeriod(DateUtils.getDate(2024, 3, 1), DateUtils.getDate(2023, 4, 1));
    Period p4 = createPeriod(DateUtils.getDate(2024, 4, 1), DateUtils.getDate(2023, 5, 1));

    DataElement de = createDataElement('z');
    manager.persist(de);

    OrganisationUnit ou = createOrganisationUnit("org u 1");
    manager.persist(ou);

    // data values with different period, so no duplicates detected during merging
    DataValue dv1 = createDataValue('1', p1, "dv test 1");
    dv1.setCategoryOptionCombo(categoryMetadata.coc1());
    dv1.setAttributeOptionCombo(categoryMetadata.coc4());
    dv1.setDataElement(de);
    dv1.setSource(ou);
    dv1.setLastUpdated(DateUtils.parseDate("2024-12-01"));

    DataValue dv2 = createDataValue('2', p2, "dv test 2 - last updated");
    dv2.setCategoryOptionCombo(categoryMetadata.coc2());
    dv2.setAttributeOptionCombo(categoryMetadata.coc4());
    dv2.setDataElement(de);
    dv2.setSource(ou);
    dv2.setLastUpdated(DateUtils.parseDate("2025-01-08"));

    DataValue dv3 = createDataValue('3', p3, "dv test 3");
    dv3.setCategoryOptionCombo(categoryMetadata.coc3());
    dv3.setAttributeOptionCombo(categoryMetadata.coc4());
    dv3.setDataElement(de);
    dv3.setSource(ou);
    dv3.setLastUpdated(DateUtils.parseDate("2024-12-06"));

    DataValue dv4 = createDataValue('4', p4, "dv test 4, untouched");
    dv4.setCategoryOptionCombo(categoryMetadata.coc4());
    dv4.setAttributeOptionCombo(categoryMetadata.coc4());
    dv4.setDataElement(de);
    dv4.setSource(ou);
    dv4.setLastUpdated(DateUtils.parseDate("2024-11-02"));

    addDataValues(dv1, dv2, dv3, dv4);

    // check pre merge state
    List<DataValue> preMergeState =
        dataValueStore.getAllDataValues().stream()
            .filter(dv -> dv.getDataElement().getUid().equals(de.getUid()))
            .toList();

    assertEquals(4, preMergeState.size(), "there should be 4 data values");
    checkCocIdsPresent(
        preMergeState,
        List.of(
            categoryMetadata.coc1().getId(),
            categoryMetadata.coc2().getId(),
            categoryMetadata.coc3().getId(),
            categoryMetadata.coc4().getId()));

    // when
    mergeDataValues(
        categoryMetadata.coc3(), List.of(categoryMetadata.coc1(), categoryMetadata.coc2()));

    // then
    List<DataValue> postMergeState =
        dataValueStore.getAllDataValues().stream()
            .filter(dv -> dv.getDataElement().getUid().equals(de.getUid()))
            .toList();

    assertEquals(4, postMergeState.size(), "there should still be 4 data values");
    checkCocIdsPresent(
        postMergeState, List.of(categoryMetadata.coc3().getId(), categoryMetadata.coc4().getId()));

    checkDataValuesPresent(
        postMergeState,
        List.of("dv test 1", "dv test 2 - last updated", "dv test 3", "dv test 4, untouched"));

    checkDatesPresent(
        postMergeState,
        List.of(
            DateUtils.parseDate("2025-01-08"),
            DateUtils.parseDate("2024-11-02"),
            DateUtils.parseDate("2024-12-01"),
            DateUtils.parseDate("2024-12-06")));
  }

  private void checkDatesPresent(List<DataValue> dataValues, List<Date> dates) {
    assertTrue(
        dataValues.stream()
            .map(DataValue::getLastUpdated)
            .collect(Collectors.toSet())
            .containsAll(dates),
        "Expected dates should be present");
  }

  private void checkDataValuesPresent(List<DataValue> dataValues, List<String> values) {
    assertTrue(
        dataValues.stream()
            .map(DataValue::getValue)
            .collect(Collectors.toSet())
            .containsAll(values),
        "Expected DataValues should be present");
  }

  private void checkCocIdsPresent(List<DataValue> dataValues, List<Long> cocIds) {
    assertTrue(
        dataValues.stream()
            .map(dv -> dv.getCategoryOptionCombo().getId())
            .collect(Collectors.toSet())
            .containsAll(cocIds),
        "Data values have expected category option combos");
  }

  private void mergeDataValues(CategoryOptionCombo target, List<CategoryOptionCombo> sources) {
    dataValueStore.mergeDataValuesWithCategoryOptionCombos(
        target.getId(), IdentifiableObjectUtils.getIdentifiersSet(sources));
    entityManager.flush();
    entityManager.clear();
  }

  private void addDataValues(DataValue... dvs) {
    for (DataValue dv : dvs) dataValueStore.addDataValue(dv);
    entityManager.flush();
  }

  private DataValue createDataValue(char uniqueChar, Period period, String value) {
    DataElement dataElement = createDataElement(uniqueChar);
    dataElement.setValueType(ValueType.TEXT);
    CategoryOptionCombo defaultCategoryOptionCombo = createCategoryOptionCombo(uniqueChar);
    defaultCategoryOptionCombo.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    OrganisationUnit organisationUnitA = createOrganisationUnit(uniqueChar);
    period.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.persist(dataElement);
    manager.persist(organisationUnitA);
    manager.persist(period);
    manager.persist(defaultCategoryOptionCombo);
    CategoryOption categoryOption = createCategoryOption(uniqueChar);
    categoryOption.setCategoryOptionCombos(Sets.newHashSet(defaultCategoryOptionCombo));
    manager.persist(categoryOption);
    defaultCategoryOptionCombo.getCategoryOptions().add(categoryOption);
    return createDataValue(
        dataElement, period, organisationUnitA, value, defaultCategoryOptionCombo);
  }
}
