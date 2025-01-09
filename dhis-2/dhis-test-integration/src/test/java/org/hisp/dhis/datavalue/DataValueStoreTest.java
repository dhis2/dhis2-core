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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
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
  @DisplayName("Get all DataValues by DataElement")
  void getDataValuesByDataElement() {
    // given
    Period p1 =
        createPeriod(DateUtils.getDate(2023, 1, 1, 1, 1), DateUtils.getDate(2023, 2, 1, 1, 1));
    Period p2 =
        createPeriod(DateUtils.getDate(2023, 3, 1, 1, 1), DateUtils.getDate(2023, 4, 1, 1, 1));
    Period p3 =
        createPeriod(DateUtils.getDate(2023, 5, 1, 1, 1), DateUtils.getDate(2023, 6, 1, 1, 1));
    Period p4 =
        createPeriod(DateUtils.getDate(2023, 7, 1, 1, 1), DateUtils.getDate(2023, 8, 1, 1, 1));

    DataElement de1 = createDataElement('1');
    DataElement de2 = createDataElement('2');
    DataElement de3 = createDataElement('3');
    manager.persist(de1);
    manager.persist(de2);
    manager.persist(de3);

    DataValue dv1 = createDataValue('B', p1, "dv test 1");
    dv1.setDataElement(de1);
    DataValue dv2 = createDataValue('C', p2, "dv test 2");
    dv2.setDataElement(de2);
    DataValue dv3 = createDataValue('D', p3, "dv test 3");
    dv3.setDataElement(de2);
    DataValue dv4 = createDataValue('E', p4, "dv test 4");
    dv4.setDataElement(de3);

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);
    dataValueStore.addDataValue(dv4);

    // when
    List<DataValue> allDataValuesByDataElement =
        dataValueStore.getAllDataValuesByDataElement(List.of(de1, de2));

    // then
    assertEquals(3, allDataValuesByDataElement.size());
    assertTrue(
        allDataValuesByDataElement.containsAll(List.of(dv1, dv2, dv3)),
        "retrieved data values contain 3 data values referencing the 2 data elements passed in");
    assertFalse(
        allDataValuesByDataElement.contains(dv4),
        "retrieved data values do not contain a data value referencing any of the 2 data elements passed in");
  }

  @Test
  @DisplayName("Get all DataValues by CategoryOptionCombo")
  void getDataValuesByCoc() {
    // given
    Period p1 =
        createPeriod(DateUtils.getDate(2023, 1, 1, 1, 1), DateUtils.getDate(2023, 2, 1, 1, 1));
    Period p2 =
        createPeriod(DateUtils.getDate(2023, 3, 1, 1, 1), DateUtils.getDate(2023, 4, 1, 1, 1));
    Period p3 =
        createPeriod(DateUtils.getDate(2023, 5, 1, 1, 1), DateUtils.getDate(2023, 6, 1, 1, 1));

    CategoryOptionCombo coc1 = createCategoryOptionCombo('1');
    coc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc1);

    CategoryOptionCombo coc2 = createCategoryOptionCombo('2');
    coc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc2);

    CategoryOptionCombo coc3 = createCategoryOptionCombo('3');
    coc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc3);

    DataValue dv1 = createDataValue('B', p1, "dv test 1");
    dv1.setCategoryOptionCombo(coc1);
    DataValue dv2 = createDataValue('C', p2, "dv test 2");
    dv2.setCategoryOptionCombo(coc2);
    DataValue dv3 = createDataValue('D', p3, "dv test 3");
    dv3.setCategoryOptionCombo(coc3);

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // when
    List<DataValue> allDataValuesByCoc =
        dataValueStore.getAllDataValuesByCatOptCombo(UID.of(coc1, coc2));

    // then
    assertEquals(2, allDataValuesByCoc.size());
    assertTrue(
        allDataValuesByCoc.containsAll(List.of(dv1, dv2)),
        "retrieved data values contain 2 data values referencing the 2 category opt combos passed in");
    assertFalse(
        allDataValuesByCoc.contains(dv3),
        "retrieved data values do not contain a data value referencing a COC not used in the query");
  }

  @Test
  @DisplayName("Get all DataValues by AttributeOptionCombo")
  void getDataValuesByAoc() {
    // given
    Period p1 =
        createPeriod(DateUtils.getDate(2023, 1, 1, 1, 1), DateUtils.getDate(2023, 2, 1, 1, 1));
    Period p2 =
        createPeriod(DateUtils.getDate(2023, 3, 1, 1, 1), DateUtils.getDate(2023, 4, 1, 1, 1));
    Period p3 =
        createPeriod(DateUtils.getDate(2023, 5, 1, 1, 1), DateUtils.getDate(2023, 6, 1, 1, 1));

    CategoryOptionCombo aoc1 = createCategoryOptionCombo('1');
    aoc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(aoc1);

    CategoryOptionCombo aoc2 = createCategoryOptionCombo('2');
    aoc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(aoc2);

    CategoryOptionCombo aoc3 = createCategoryOptionCombo('3');
    aoc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(aoc3);

    DataValue dv1 = createDataValue('B', p1, "dv test 1");
    dv1.setAttributeOptionCombo(aoc1);
    DataValue dv2 = createDataValue('C', p2, "dv test 2");
    dv2.setAttributeOptionCombo(aoc2);
    DataValue dv3 = createDataValue('D', p3, "dv test 3");
    dv3.setAttributeOptionCombo(aoc3);

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);

    // when
    List<DataValue> allDataValuesByAoc =
        dataValueStore.getAllDataValuesByAttrOptCombo(UID.of(aoc1, aoc2));

    // then
    assertEquals(2, allDataValuesByAoc.size());
    assertTrue(
        allDataValuesByAoc.containsAll(List.of(dv1, dv2)),
        "retrieved data values contain 2 data values referencing the 2 attribute opt combos passed in");
    assertFalse(
        allDataValuesByAoc.contains(dv3),
        "retrieved data values do not contain a data value referencing a AOC not used in the query");
  }

  @Test
  @DisplayName(
      "Merging duplicate DataValues (cat opt combos) leaves only the last updated value remaining")
  void mergeDvWithDuplicates() {
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

    dataValueStore.addDataValue(dv1);
    dataValueStore.addDataValue(dv2);
    dataValueStore.addDataValue(dv3);
    dataValueStore.addDataValue(dv4);

    // check pre merge state
    List<DataValue> preMergeState = dataValueStore.getAllDataValuesByDataElement(List.of(de));

    assertEquals(4, preMergeState.size(), "there should be 4 data values");
    assertTrue(
        preMergeState.stream()
            .map(dv -> dv.getCategoryOptionCombo().getId())
            .collect(Collectors.toSet())
            .containsAll(
                List.of(
                    categoryMetadata.coc1().getId(),
                    categoryMetadata.coc2().getId(),
                    categoryMetadata.coc3().getId(),
                    categoryMetadata.coc4().getId())),
        "All data values have different category option combos");

    entityManager.flush();

    // when
    dataValueStore.mergeDataValuesWithCategoryOptionCombos(
        categoryMetadata.coc3(), List.of(categoryMetadata.coc1(), categoryMetadata.coc2()));
    entityManager.flush();
    entityManager.clear();

    // then
    List<DataValue> postMergeState = dataValueStore.getAllDataValuesByDataElement(List.of(de));

    assertEquals(2, postMergeState.size(), "there should be 2 data values");
    assertTrue(
        postMergeState.stream()
            .map(dv -> dv.getCategoryOptionCombo().getId())
            .collect(Collectors.toSet())
            .containsAll(List.of(categoryMetadata.coc3().getId(), categoryMetadata.coc4().getId())),
        "Only 2 expected cat opt combos should be present");

    assertTrue(
        postMergeState.stream()
            .map(DataValue::getValue)
            .collect(Collectors.toSet())
            .containsAll(List.of("dv test 2 - last updated", "dv test 4, untouched")),
        "Only latest DataValue and untouched DataValue should be present");

    assertTrue(
        postMergeState.stream()
            .map(DataValue::getLastUpdated)
            .collect(Collectors.toSet())
            .containsAll(
                List.of(DateUtils.parseDate("2025-01-08"), DateUtils.parseDate("2024-11-02"))),
        "Only latest lastUpdated value and untouched lastUpdated should exist");
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
