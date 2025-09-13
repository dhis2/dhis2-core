/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.test.utils.Assertions.assertContainsAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
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

  @Autowired private DataDumpService dataDumpService;
  @Autowired private DataExportStore dataExportStore;
  @Autowired private DataValueStore dataValueStore;

  @Test
  @DisplayName(
      "Merging duplicate DataValues (cat opt combos) leaves only the last updated (source) value remaining")
  void mergeDvWithDuplicatesKeepSource() throws Exception {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mdv1");

    Period p1 = createPeriod(DateUtils.getDate(2024, 1, 1), DateUtils.getDate(2023, 2, 1));

    DataElement de = createDataElement('z');
    manager.persist(de);

    OrganisationUnit ou = createOrganisationUnit("org u 1");
    manager.persist(ou);

    // data values with same period, org unit, data element and attr opt combo
    // which will be identified as duplicates during merging
    DataValue dv4 = persistDataValue('4', p1, "dv test 4, untouched");
    dv4.setCategoryOptionCombo(categoryMetadata.coc4());
    dv4.setAttributeOptionCombo(categoryMetadata.coc4());
    dv4.setDataElement(de);
    dv4.setSource(ou);
    addDataValues(dv4);
    Thread.sleep(2L);

    DataValue dv1 = persistDataValue('1', p1, "dv test 1");
    dv1.setCategoryOptionCombo(categoryMetadata.coc1());
    dv1.setAttributeOptionCombo(categoryMetadata.coc4());
    dv1.setDataElement(de);
    dv1.setSource(ou);
    addDataValues(dv1);
    Thread.sleep(2L);

    DataValue dv3 = persistDataValue('3', p1, "dv test 3");
    dv3.setCategoryOptionCombo(categoryMetadata.coc3());
    dv3.setAttributeOptionCombo(categoryMetadata.coc4());
    dv3.setDataElement(de);
    dv3.setSource(ou);
    addDataValues(dv3);
    Thread.sleep(2L);

    DataValue dv2 = persistDataValue('2', p1, "dv test 2 - last updated");
    dv2.setCategoryOptionCombo(categoryMetadata.coc2());
    dv2.setAttributeOptionCombo(categoryMetadata.coc4());
    dv2.setDataElement(de);
    dv2.setSource(ou);
    addDataValues(dv2);

    // check pre merge state
    List<DataExportValue> preMergeState =
        dataExportStore.getAllDataValues().stream()
            .filter(dv -> dv.dataElement().getValue().equals(de.getUid()))
            .toList();

    assertEquals(4, preMergeState.size(), "there should be 4 data values");
    checkCocIdsPresent(
        preMergeState,
        List.of(
            categoryMetadata.coc1().getUid(),
            categoryMetadata.coc2().getUid(),
            categoryMetadata.coc3().getUid(),
            categoryMetadata.coc4().getUid()));

    // when
    mergeDataValues(
        categoryMetadata.coc3(), List.of(categoryMetadata.coc1(), categoryMetadata.coc2()));

    // then
    List<DataExportValue> postMergeState =
        dataExportStore.getAllDataValues().stream()
            .filter(dv -> dv.dataElement().getValue().equals(de.getUid()))
            .toList();

    assertEquals(2, postMergeState.size(), "there should be 2 data values");
    checkCocIdsPresent(
        preMergeState, List.of(categoryMetadata.coc3().getUid(), categoryMetadata.coc4().getUid()));

    checkDataValuesPresent(
        postMergeState, List.of("dv test 2 - last updated", "dv test 4, untouched"));
  }

  @Test
  @DisplayName(
      "Merging duplicate DataValues (cat opt combos) leaves only the last updated (target) value remaining")
  void mergeDvWithDuplicatesKeepTarget() {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mdv2");

    Period p1 = createPeriod(DateUtils.getDate(2024, 1, 1), DateUtils.getDate(2023, 2, 1));

    DataElement de = createDataElement('z');
    manager.persist(de);

    OrganisationUnit ou = createOrganisationUnit("org u 1");
    manager.persist(ou);

    // data values with same period, org unit, data element and attr opt combo
    // which will be identified as duplicates during merging
    DataValue dv1 = persistDataValue('1', p1, "dv test 1");
    dv1.setCategoryOptionCombo(categoryMetadata.coc1());
    dv1.setAttributeOptionCombo(categoryMetadata.coc4());
    dv1.setDataElement(de);
    dv1.setSource(ou);
    dv1.setLastUpdated(DateUtils.parseDate("2024-12-01"));

    DataValue dv2 = persistDataValue('2', p1, "dv test 2");
    dv2.setCategoryOptionCombo(categoryMetadata.coc2());
    dv2.setAttributeOptionCombo(categoryMetadata.coc4());
    dv2.setDataElement(de);
    dv2.setSource(ou);
    dv2.setLastUpdated(DateUtils.parseDate("2025-01-02"));

    DataValue dv3 = persistDataValue('3', p1, "dv test 3 - last updated");
    dv3.setCategoryOptionCombo(categoryMetadata.coc3());
    dv3.setAttributeOptionCombo(categoryMetadata.coc4());
    dv3.setDataElement(de);
    dv3.setSource(ou);
    dv3.setLastUpdated(DateUtils.parseDate("2025-01-06"));

    DataValue dv4 = persistDataValue('4', p1, "dv test 4, untouched");
    dv4.setCategoryOptionCombo(categoryMetadata.coc4());
    dv4.setAttributeOptionCombo(categoryMetadata.coc4());
    dv4.setDataElement(de);
    dv4.setSource(ou);
    dv4.setLastUpdated(DateUtils.parseDate("2024-11-02"));

    addDataValues(dv1, dv2, dv3, dv4);

    // check pre merge state
    List<DataExportValue> preMergeState =
        dataExportStore.getAllDataValues().stream()
            .filter(dv -> dv.dataElement().getValue().equals(de.getUid()))
            .toList();

    assertEquals(4, preMergeState.size(), "there should be 4 data values");
    checkCocIdsPresent(
        preMergeState,
        List.of(
            categoryMetadata.coc1().getUid(),
            categoryMetadata.coc2().getUid(),
            categoryMetadata.coc3().getUid(),
            categoryMetadata.coc4().getUid()));

    // when
    mergeDataValues(
        categoryMetadata.coc3(), List.of(categoryMetadata.coc1(), categoryMetadata.coc2()));

    // then
    List<DataExportValue> postMergeState =
        dataExportStore.getAllDataValues().stream()
            .filter(dv -> dv.dataElement().getValue().equals(de.getUid()))
            .toList();

    assertEquals(2, postMergeState.size(), "there should be 2 data values");
    checkCocIdsPresent(
        postMergeState,
        List.of(categoryMetadata.coc3().getUid(), categoryMetadata.coc4().getUid()));

    checkDataValuesPresent(
        postMergeState, List.of("dv test 3 - last updated", "dv test 4, untouched"));
  }

  @Test
  @DisplayName(
      "Merging non-duplicate DataValues (cat opt combos) updates the cat opt combo value only")
  void mergeDvWithNoDuplicates() {
    // given
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("mdv3");

    Period p1 = createPeriod(DateUtils.getDate(2024, 1, 1), DateUtils.getDate(2023, 2, 1));
    Period p2 = createPeriod(DateUtils.getDate(2024, 2, 1), DateUtils.getDate(2023, 3, 1));
    Period p3 = createPeriod(DateUtils.getDate(2024, 3, 1), DateUtils.getDate(2023, 4, 1));
    Period p4 = createPeriod(DateUtils.getDate(2024, 4, 1), DateUtils.getDate(2023, 5, 1));

    DataElement de = createDataElement('z');
    manager.persist(de);

    OrganisationUnit ou = createOrganisationUnit("org u 1");
    manager.persist(ou);

    // data values with different period, so no duplicates detected during merging
    DataValue dv1 = persistDataValue('1', p1, "dv test 1");
    dv1.setCategoryOptionCombo(categoryMetadata.coc1());
    dv1.setAttributeOptionCombo(categoryMetadata.coc4());
    dv1.setDataElement(de);
    dv1.setSource(ou);

    DataValue dv2 = persistDataValue('2', p2, "dv test 2 - last updated");
    dv2.setCategoryOptionCombo(categoryMetadata.coc2());
    dv2.setAttributeOptionCombo(categoryMetadata.coc4());
    dv2.setDataElement(de);
    dv2.setSource(ou);

    DataValue dv3 = persistDataValue('3', p3, "dv test 3");
    dv3.setCategoryOptionCombo(categoryMetadata.coc3());
    dv3.setAttributeOptionCombo(categoryMetadata.coc4());
    dv3.setDataElement(de);
    dv3.setSource(ou);

    DataValue dv4 = persistDataValue('4', p4, "dv test 4, untouched");
    dv4.setCategoryOptionCombo(categoryMetadata.coc4());
    dv4.setAttributeOptionCombo(categoryMetadata.coc4());
    dv4.setDataElement(de);
    dv4.setSource(ou);

    addDataValues(dv1, dv2, dv3, dv4);

    // check pre merge state
    List<DataExportValue> preMergeState =
        dataExportStore.getAllDataValues().stream()
            .filter(dv -> dv.dataElement().getValue().equals(de.getUid()))
            .toList();

    assertEquals(4, preMergeState.size(), "there should be 4 data values");
    checkCocIdsPresent(
        preMergeState,
        List.of(
            categoryMetadata.coc1().getUid(),
            categoryMetadata.coc2().getUid(),
            categoryMetadata.coc3().getUid(),
            categoryMetadata.coc4().getUid()));

    // when
    mergeDataValues(
        categoryMetadata.coc3(), List.of(categoryMetadata.coc1(), categoryMetadata.coc2()));

    // then
    List<DataExportValue> postMergeState =
        dataExportStore.getAllDataValues().stream()
            .filter(dv -> dv.dataElement().getValue().equals(de.getUid()))
            .toList();

    assertEquals(4, postMergeState.size(), "there should still be 4 data values");
    checkCocIdsPresent(
        postMergeState,
        List.of(categoryMetadata.coc3().getUid(), categoryMetadata.coc4().getUid()));

    checkDataValuesPresent(
        postMergeState,
        List.of("dv test 1", "dv test 2 - last updated", "dv test 3", "dv test 4, untouched"));
  }

  private void checkDataValuesPresent(List<DataExportValue> dataValues, List<String> values) {
    assertContainsAll(values, dataValues.stream().map(DataExportValue::value).toList());
  }

  private void checkCocIdsPresent(List<DataExportValue> dataValues, List<String> cocIds) {
    assertContainsAll(
        cocIds, dataValues.stream().map(dv -> dv.categoryOptionCombo().getValue()).toList());
  }

  private void mergeDataValues(CategoryOptionCombo target, List<CategoryOptionCombo> sources) {
    dataValueStore.mergeDataValuesWithCategoryOptionCombos(
        target.getId(), IdentifiableObjectUtils.getIdentifiersSet(sources));
    entityManager.flush();
    entityManager.clear();
  }

  private void addDataValues(DataValue... values) {
    if (dataDumpService.addValuesForJdbcTest(values) < values.length)
      fail("Failed to upsert test data");
  }

  private DataValue persistDataValue(char uniqueChar, Period period, String value) {
    DataElement dataElement = createDataElement(uniqueChar);
    dataElement.setValueType(ValueType.TEXT);
    CategoryOptionCombo defaultCategoryOptionCombo = createCategoryOptionCombo(uniqueChar);
    defaultCategoryOptionCombo.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    OrganisationUnit organisationUnitA = createOrganisationUnit(uniqueChar);
    period.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY));
    manager.persist(dataElement);
    manager.persist(organisationUnitA);
    manager.persist(defaultCategoryOptionCombo);
    CategoryOption categoryOption = createCategoryOption(uniqueChar);
    categoryOption.setCategoryOptionCombos(Sets.newHashSet(defaultCategoryOptionCombo));
    manager.persist(categoryOption);
    defaultCategoryOptionCombo.getCategoryOptions().add(categoryOption);
    return createDataValue(
        dataElement, period, organisationUnitA, value, defaultCategoryOptionCombo);
  }
}
