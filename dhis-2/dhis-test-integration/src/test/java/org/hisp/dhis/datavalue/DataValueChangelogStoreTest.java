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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DataValueChangelogStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataValueChangelogStore dataValueChangelogStore;
  @Autowired private DataDumpService dataDumpService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private CategoryService categoryService;
  @Autowired private PeriodService periodService;

  private CategoryOptionCombo coc1;
  private CategoryOptionCombo coc2;
  private CategoryOptionCombo coc3;

  @BeforeEach
  void setUp() {
    coc1 = createCategoryOptionCombo('1');
    coc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc1);

    coc2 = createCategoryOptionCombo('2');
    coc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc2);

    coc3 = createCategoryOptionCombo('3');
    coc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc3);

    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    manager.save(List.of(dataElementA, dataElementB, dataElementC));

    Period periodA =
        createPeriod(new MonthlyPeriodType(), getDate(2017, 1, 1), getDate(2017, 1, 31));
    Period periodB =
        createPeriod(new MonthlyPeriodType(), getDate(2018, 1, 1), getDate(2017, 1, 31));
    Period periodC =
        createPeriod(new MonthlyPeriodType(), getDate(2019, 1, 1), getDate(2017, 1, 31));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    periodService.addPeriod(periodC);

    OrganisationUnit orgUnitA = createOrganisationUnit('A');
    OrganisationUnit orgUnitB = createOrganisationUnit('B');
    OrganisationUnit orgUnitC = createOrganisationUnit('C');
    manager.save(List.of(orgUnitA, orgUnitB, orgUnitC));

    addDataValues(
        createDataValue(dataElementA, periodA, orgUnitA, coc1, coc1, "1"),
        createDataValue(dataElementA, periodB, orgUnitA, coc1, coc1, "2"),
        createDataValue(dataElementB, periodB, orgUnitB, coc2, coc2, "3"),
        createDataValue(dataElementB, periodC, orgUnitB, coc2, coc2, "4"),
        createDataValue(dataElementC, periodC, orgUnitC, coc3, coc3, "5"),
        createDataValue(dataElementC, periodA, orgUnitC, coc3, coc3, "6"));
  }

  @Test
  @DisplayName("Deleting audits by category option combo deletes the correct entries")
  void testAddGetDataValueAuditFromDataValue() {
    // state before delete
    List<DataValueChangelog> dvaCoc1Before =
        dataValueChangelogStore.getEntries(
            new DataValueChangelogQueryParams().setCategoryOptionCombo(UID.of(coc1)));
    List<DataValueChangelog> dvaCoc2Before =
        dataValueChangelogStore.getEntries(
            new DataValueChangelogQueryParams().setAttributeOptionCombo(UID.of(coc2)));
    List<DataValueChangelog> dvaCoc3Before =
        dataValueChangelogStore.getEntries(
            new DataValueChangelogQueryParams()
                .setCategoryOptionCombo(UID.of(coc3))
                .setAttributeOptionCombo(UID.of(coc3)));

    assertEquals(2, dvaCoc1Before.size(), "There should be 2 audits referencing Cat Opt Combo 1");
    assertEquals(2, dvaCoc2Before.size(), "There should be 2 audits referencing Cat Opt Combo 2");
    assertEquals(2, dvaCoc3Before.size(), "There should be 2 audits referencing Cat Opt Combo 3");

    // when
    dataValueChangelogStore.deleteByOptionCombo(UID.of(coc1));
    dataValueChangelogStore.deleteByOptionCombo(UID.of(coc2));

    // then
    List<DataValueChangelog> dvaCoc1After =
        dataValueChangelogStore.getEntries(
            new DataValueChangelogQueryParams().setCategoryOptionCombo(UID.of(coc1)));
    List<DataValueChangelog> dvaCoc2After =
        dataValueChangelogStore.getEntries(
            new DataValueChangelogQueryParams().setAttributeOptionCombo(UID.of(coc2)));
    List<DataValueChangelog> dvaCoc3After =
        dataValueChangelogStore.getEntries(
            new DataValueChangelogQueryParams()
                .setCategoryOptionCombo(UID.of(coc3))
                .setAttributeOptionCombo(UID.of(coc3)));

    assertTrue(dvaCoc1After.isEmpty(), "There should be 0 audits referencing Cat Opt Combo 1");
    assertTrue(dvaCoc2After.isEmpty(), "There should be 0 audits referencing Cat Opt Combo 2");
    assertEquals(2, dvaCoc3After.size(), "There should be 2 audits referencing Cat Opt Combo 3");
  }

  private void addDataValues(DataValue... values) {
    if (dataDumpService.upsertValues(values) < values.length) fail("Failed to upsert test data");
  }
}
