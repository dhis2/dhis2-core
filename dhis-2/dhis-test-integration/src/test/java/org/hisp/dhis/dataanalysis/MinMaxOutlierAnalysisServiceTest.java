/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dataanalysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MinMaxOutlierAnalysisServiceTest extends PostgresIntegrationTestBase {

  @Autowired private MinMaxOutlierAnalysisService minMaxOutlierAnalysisService;

  @Autowired private MinMaxDataElementService minMaxDataElementService;

  @Autowired private DataElementService dataElementService;

  @Autowired private CategoryService categoryService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DataValueService dataValueService;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private DataElement dataElementC;

  private DataElement dataElementD;

  private List<DataElement> dataElementsA;

  private List<DataElement> dataElementsB;

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

  private List<Period> periodsA;

  private Date from = getDate(1998, 1, 1);

  private OrganisationUnit organisationUnitA;

  @BeforeEach
  void setUp() {
    categoryCombo = categoryService.getDefaultCategoryCombo();
    categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
    dataElementA = createDataElement('A', categoryCombo);
    dataElementB = createDataElement('B', categoryCombo);
    dataElementC = createDataElement('C', categoryCombo);
    dataElementD = createDataElement('D', categoryCombo);
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    dataElementService.addDataElement(dataElementC);
    dataElementService.addDataElement(dataElementD);

    dataElementsA = List.of(dataElementA, dataElementB);
    dataElementsB = List.of(dataElementC, dataElementD);

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

    periodsA = List.of(periodA, periodB, periodE, periodI, periodJ);

    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
  }

  @Test
  void testAnalyse() {
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodA, organisationUnitA, "5", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodB, organisationUnitA, "-50", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodC, organisationUnitA, "5", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodD, organisationUnitA, "-5", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodE, organisationUnitA, "10", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodF, organisationUnitA, "-10", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodG, organisationUnitA, "13", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodH, organisationUnitA, "-13", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodI, organisationUnitA, "41", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementA, periodJ, organisationUnitA, "-41", categoryOptionCombo));

    dataValueService.addDataValue(
        createDataValue(dataElementC, periodA, organisationUnitA, "7", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementC, periodE, organisationUnitA, "15", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementC, periodI, organisationUnitA, "17", categoryOptionCombo));
    dataValueService.addDataValue(
        createDataValue(dataElementC, periodJ, organisationUnitA, "23", categoryOptionCombo));

    minMaxDataElementService.addMinMaxDataElement(
        new MinMaxDataElement(
            dataElementA, organisationUnitA, categoryOptionCombo, -40, 40, false));

    minMaxDataElementService.addMinMaxDataElement(
        new MinMaxDataElement(dataElementC, organisationUnitA, categoryOptionCombo, 10, 20, false));

    List<DeflatedDataValue> resultA =
        minMaxOutlierAnalysisService.analyse(
            organisationUnitA, dataElementsA, periodsA, null, from);
    List<DeflatedDataValue> resultB =
        minMaxOutlierAnalysisService.analyse(
            organisationUnitA, dataElementsB, periodsA, null, from);

    assertEquals(3, resultA.size());
    assertEquals(2, resultB.size());
  }
}
