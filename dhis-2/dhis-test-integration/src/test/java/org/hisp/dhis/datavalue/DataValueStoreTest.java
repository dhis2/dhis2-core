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
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
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
