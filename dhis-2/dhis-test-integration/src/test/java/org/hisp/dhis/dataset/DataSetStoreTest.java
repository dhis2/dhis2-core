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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
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
}
