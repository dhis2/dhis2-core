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
package org.hisp.dhis.dataanalysis;

import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.Lists;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataEntryGroup;
import org.hisp.dhis.datavalue.DataEntryService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataAnalysisStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataAnalysisStore dataAnalysisStore;

  @Autowired private DataElementService dataElementService;

  @Autowired private CategoryService categoryService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DataEntryService dataEntryService;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private CategoryCombo categoryCombo;

  private CategoryOptionCombo categoryOptionCombo;

  private Period periodA;

  private Period periodB;

  private Period periodC;

  private Period periodD;

  private Period periodE;

  private Period periodF;

  private Period periodG;

  private Period periodH;

  private Period periodI;

  private Period periodJ;

  private Date from = getDate(1998, 1, 1);

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Set<OrganisationUnit> organisationUnits;

  @BeforeAll
  void setUp() {
    categoryCombo = categoryService.getDefaultCategoryCombo();
    categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
    dataElementA = createDataElement('A', categoryCombo);
    dataElementB = createDataElement('B', categoryCombo);
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    periodA = createPeriod(new MonthlyPeriodType(), getDate(2000, 3, 1), getDate(2000, 3, 31));
    periodB = createPeriod(new MonthlyPeriodType(), getDate(2000, 4, 1), getDate(2000, 4, 30));
    periodC = createPeriod(new MonthlyPeriodType(), getDate(2000, 5, 1), getDate(2000, 5, 30));
    periodD = createPeriod(new MonthlyPeriodType(), getDate(2000, 6, 1), getDate(2000, 6, 30));
    periodE = createPeriod(new MonthlyPeriodType(), getDate(2000, 7, 1), getDate(2000, 7, 30));
    periodF = createPeriod(new MonthlyPeriodType(), getDate(2000, 8, 1), getDate(2000, 8, 30));
    periodG = createPeriod(new MonthlyPeriodType(), getDate(2000, 9, 1), getDate(2000, 9, 30));
    periodH = createPeriod(new MonthlyPeriodType(), getDate(2000, 10, 1), getDate(2000, 10, 30));
    periodI = createPeriod(new MonthlyPeriodType(), getDate(2000, 11, 1), getDate(2000, 11, 30));
    periodJ = createPeriod(new MonthlyPeriodType(), getDate(2000, 12, 1), getDate(2000, 12, 30));
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    organisationUnits = new HashSet<>();
    organisationUnits.add(organisationUnitA);
    organisationUnits.add(organisationUnitB);
  }

  // ----------------------------------------------------------------------
  // Business logic tests
  // ----------------------------------------------------------------------
  @Test
  void testGetDataAnalysisMeasures() {
    addDataValues(
        createDataValue(dataElementA, periodA, organisationUnitA, "5", categoryOptionCombo),
        createDataValue(dataElementA, periodB, organisationUnitA, "2", categoryOptionCombo),
        createDataValue(dataElementA, periodC, organisationUnitA, "1", categoryOptionCombo),
        createDataValue(dataElementA, periodD, organisationUnitA, "12", categoryOptionCombo),
        createDataValue(dataElementA, periodE, organisationUnitA, "10", categoryOptionCombo),
        createDataValue(dataElementA, periodF, organisationUnitA, "7", categoryOptionCombo),
        createDataValue(dataElementA, periodG, organisationUnitA, "52", categoryOptionCombo),
        createDataValue(dataElementA, periodH, organisationUnitA, "23", categoryOptionCombo),
        createDataValue(dataElementA, periodI, organisationUnitA, "3", categoryOptionCombo),
        createDataValue(dataElementA, periodJ, organisationUnitA, "15", categoryOptionCombo));
    List<DataAnalysisMeasures> measures =
        dataAnalysisStore.getDataAnalysisMeasures(
            dataElementA, Lists.newArrayList(categoryOptionCombo), organisationUnitA, from);
    assertEquals(1, measures.size());
    assertEquals(measures.get(0).getAverage(), DELTA, 12.78);
    assertEquals(measures.get(0).getStandardDeviation(), DELTA, 15.26);
  }

  private void addDataValues(DataValue... values) {
    try {
      dataEntryService.upsertGroup(
          new DataEntryGroup.Options().allowDisconnected(),
          new DataEntryGroup(null, DataValue.toDataEntryValues(List.of(values))),
          transitory());
    } catch (ConflictException ex) {
      fail("Failed to upsert test data", ex);
    }
  }
}
