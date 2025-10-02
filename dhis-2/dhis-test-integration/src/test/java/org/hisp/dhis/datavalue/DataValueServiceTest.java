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
package org.hisp.dhis.datavalue;

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.period.PeriodTypeEnum.DAILY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kristian Nordal
 */
@Transactional
class DataValueServiceTest extends PostgresIntegrationTestBase {
  @Autowired private CategoryService categoryService;

  @Autowired private DataElementService dataElementService;

  @Autowired private DataExportService dataExportService;
  @Autowired private DataValueService dataValueService;
  @Autowired private DataExportStore dataExportStore;

  @Autowired private DataDumpService dataDumpService;

  @Autowired private OrganisationUnitService organisationUnitService;

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private DataElement deD;

  private CategoryOptionCombo optionCombo;

  private Period peA;

  private Period peB;

  private Period peC;

  private Period peX;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private OrganisationUnit ouD;

  @BeforeEach
  void setUp() {
    deA = createDataElement('A');
    deB = createDataElement('B');
    deC = createDataElement('C');
    deD = createDataElement('D');
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);
    dataElementService.addDataElement(deC);
    dataElementService.addDataElement(deD);
    peA = PeriodType.getPeriodType(DAILY).createPeriod(getDay(5));
    peB = peA.next();
    peC = peB.next();
    peX = PeriodType.getPeriodType(DAILY).createPeriod(getDay(27));
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    ouD = createOrganisationUnit('D');
    organisationUnitService.addOrganisationUnit(ouA);
    organisationUnitService.addOrganisationUnit(ouB);
    organisationUnitService.addOrganisationUnit(ouC);
    organisationUnitService.addOrganisationUnit(ouD);
    optionCombo = categoryService.getDefaultCategoryOptionCombo();
  }

  // -------------------------------------------------------------------------
  // Basic DataValue
  // -------------------------------------------------------------------------

  @Test
  void testAddDataValue() throws ConflictException {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deC, peC, ouA, optionCombo, optionCombo, "3");
    addDataValues(dataValueA, dataValueB, dataValueC);
    DataExportValue dvA =
        dataExportService.exportValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo));
    assertNotNull(dvA);
    assertNotNull(dvA.created());
    assertEquals(ouA.getUid(), dvA.orgUnit().getValue());
    assertEquals(deA.getUid(), dvA.dataElement().getValue());
    assertEquals(peA.getIsoDate(), dvA.period());
    assertEquals("1", dvA.value());
    DataExportValue dvB =
        dataExportService.exportValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo));
    assertNotNull(dvB);
    assertNotNull(dvB.created());
    assertEquals(ouA.getUid(), dvB.orgUnit().getValue());
    assertEquals(deB.getUid(), dvB.dataElement().getValue());
    assertEquals(peA.getIsoDate(), dvB.period());
    assertEquals("2", dvB.value());
    DataExportValue dvC =
        dataExportService.exportValue(new DataEntryKey(deC, peC, ouA, optionCombo, optionCombo));
    assertNotNull(dvC);
    assertNotNull(dvC.created());
    assertEquals(ouA.getUid(), dvC.orgUnit().getValue());
    assertEquals(deC.getUid(), dvC.dataElement().getValue());
    assertEquals(peC.getIsoDate(), dvC.period());
    assertEquals("3", dvC.value());
  }

  @Test
  void testUpdateDataValue() throws ConflictException {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "2");
    addDataValues(dataValueA, dataValueB);
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deB, peA, ouB, optionCombo, optionCombo)));
    dataValueA.setValue("5");
    addDataValues(dataValueA);
    DataExportValue dvA =
        dataExportService.exportValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo));
    assertNotNull(dvA);
    assertEquals("5", dvA.value());
    DataExportValue dvB =
        dataExportService.exportValue(new DataEntryKey(deB, peA, ouB, optionCombo, optionCombo));
    assertNotNull(dvB);
    assertEquals("2", dvB.value());
  }

  @Test
  void testDeleteAndGetDataValue() throws ConflictException {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deC, peC, ouD, optionCombo, optionCombo, "3");
    DataValue dataValueD = new DataValue(deD, peC, ouB, optionCombo, optionCombo, "4");
    addDataValues(dataValueA, dataValueB, dataValueC, dataValueD);
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
    deleteDataValue(dataValueA);
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
    deleteDataValue(dataValueB);
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
    deleteDataValue(dataValueC);
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNotNull(
        dataExportService.exportValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
    deleteDataValue(dataValueD);
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNull(
        dataExportService.exportValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
  }

  // -------------------------------------------------------------------------
  // Collections of DataValues
  // -------------------------------------------------------------------------

  @Test
  void testGetDataValuesDataExportParamsA() throws ConflictException {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deA, peA, ouB, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deA, peB, ouA, optionCombo, optionCombo, "3");
    DataValue dataValueD = new DataValue(deA, peB, ouB, optionCombo, optionCombo, "4");
    DataValue dataValueE = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "5");
    DataValue dataValueF = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "6");
    DataValue dataValueG = new DataValue(deB, peB, ouA, optionCombo, optionCombo, "7");
    DataValue dataValueH = new DataValue(deB, peB, ouB, optionCombo, optionCombo, "8");
    DataValue dataValueI = new DataValue(deA, peC, ouA, optionCombo, optionCombo, "9");
    DataValue dataValueJ = new DataValue(deA, peC, ouB, optionCombo, optionCombo, "10");
    addDataValues(
        dataValueA,
        dataValueB,
        dataValueC,
        dataValueD,
        dataValueE,
        dataValueF,
        dataValueG,
        dataValueH,
        dataValueI,
        dataValueJ);
    DataExportParams params =
        DataExportParams.builder()
            .dataElement(Set.of(deA.getUid()))
            .period(Set.of(peA.getIsoDate(), peB.getIsoDate(), peC.getIsoDate()))
            .orgUnit(Set.of(ouA.getUid()))
            .build();
    Stream<DataExportValue> values = dataExportService.exportValues(params);
    assertEquals(Set.of("1", "3", "9"), values.map(DataExportValue::value).collect(toSet()));
    params =
        DataExportParams.builder()
            .dataElement(Set.of(deB.getUid()))
            .period(Set.of(peA.getIsoDate()))
            .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
            .build();
    values = dataExportService.exportValues(params);
    assertEquals(Set.of("5", "6"), values.map(DataExportValue::value).collect(toSet()));
  }

  @Test
  void testGetDataValuesDataExportParamsB() throws ConflictException {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deA, peA, ouB, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deA, peB, ouA, optionCombo, optionCombo, "3");
    DataValue dataValueD = new DataValue(deA, peB, ouB, optionCombo, optionCombo, "4");
    DataValue dataValueE = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "5");
    DataValue dataValueF = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "6");
    DataValue dataValueG = new DataValue(deB, peB, ouA, optionCombo, optionCombo, "7");
    DataValue dataValueH = new DataValue(deB, peB, ouB, optionCombo, optionCombo, "8");
    DataValue dataValueI = new DataValue(deA, peC, ouA, optionCombo, optionCombo, "9");
    DataValue dataValueJ = new DataValue(deA, peC, ouB, optionCombo, optionCombo, "10");
    addDataValues(
        dataValueA,
        dataValueB,
        dataValueC,
        dataValueD,
        dataValueE,
        dataValueF,
        dataValueG,
        dataValueH,
        dataValueI,
        dataValueJ);

    assertEquals(
        4,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid(), deB.getUid()))
                    .period(Set.of(peB.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        2,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid(), deB.getUid()))
                    .period(Set.of(peA.getIsoDate()))
                    .orgUnit(Set.of(ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        4,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid()))
                    .period(Set.of(peA.getIsoDate(), peC.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        4,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deB.getUid()))
                    .period(Set.of(peA.getIsoDate(), peB.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        1,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deB.getUid()))
                    .period(Set.of(peB.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid()))
                    .build())
            .count());
    assertEquals(
        1,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid()))
                    .period(Set.of(peA.getIsoDate()))
                    .orgUnit(Set.of(ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        1,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid()))
                    .startDate(peA.getStartDate())
                    .endDate(peA.getEndDate())
                    .orgUnit(Set.of(ouB.getUid()))
                    .build())
            .count());
  }

  @Test
  void testGetDataValuesExportParamsC() throws ConflictException {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deA, peA, ouB, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "3");
    DataValue dataValueD = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "4");
    addDataValues(dataValueA, dataValueB, dataValueC, dataValueD);

    assertEquals(
        2,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid()))
                    .period(Set.of(peA.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
                    .build())
            .count());
  }

  @Test
  void testGetDataValuesNonExistingPeriodA() throws ConflictException {

    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    addDataValues(dataValueA);

    assertEquals(
        0,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid()))
                    .period(Set.of(peX.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid()))
                    .build())
            .count());
  }

  @Test
  void testGetDataValuesNonExistingPeriodB() throws ConflictException {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deA, peA, ouB, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "3");
    DataValue dataValueD = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "4");
    addDataValues(dataValueA, dataValueB, dataValueC, dataValueD);

    assertEquals(
        2,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid()))
                    .period(Set.of(peA.getIsoDate(), peX.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
                    .build())
            .count());
  }

  @Test
  void testGetAllDataValues() {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo);
    dataValueA.setValue("1");
    DataValue dataValueB = new DataValue(deB, peA, ouA, optionCombo, optionCombo);
    dataValueB.setValue("2");
    DataValue dataValueC = new DataValue(deC, peC, ouD, optionCombo, optionCombo);
    dataValueC.setValue("3");
    DataValue dataValueD = new DataValue(deD, peC, ouB, optionCombo, optionCombo);
    dataValueD.setValue("4");
    addDataValues(dataValueA, dataValueB, dataValueC, dataValueD);
    assertEquals(4, dataExportStore.getAllDataValues().size());
  }

  @Test
  void testGetDataValuesDataElementsPeriodsOrgUnits() throws ConflictException {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deA, peA, ouB, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deA, peB, ouA, optionCombo, optionCombo, "3");
    DataValue dataValueD = new DataValue(deA, peB, ouB, optionCombo, optionCombo, "4");
    DataValue dataValueE = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "5");
    DataValue dataValueF = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "6");
    DataValue dataValueG = new DataValue(deB, peB, ouA, optionCombo, optionCombo, "7");
    DataValue dataValueH = new DataValue(deB, peB, ouB, optionCombo, optionCombo, "8");
    DataValue dataValueI = new DataValue(deA, peC, ouA, optionCombo, optionCombo, "9");
    DataValue dataValueJ = new DataValue(deA, peC, ouB, optionCombo, optionCombo, "10");
    addDataValues(
        dataValueA,
        dataValueB,
        dataValueC,
        dataValueD,
        dataValueE,
        dataValueF,
        dataValueG,
        dataValueH,
        dataValueI,
        dataValueJ);
    assertEquals(
        4,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid(), deB.getUid()))
                    .period(Set.of(peB.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        2,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid(), deB.getUid()))
                    .period(Set.of(peA.getIsoDate()))
                    .orgUnit(Set.of(ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        2,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid()))
                    .period(Set.of(peC.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        4,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid()))
                    .period(Set.of(peA.getIsoDate(), peC.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        4,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deB.getUid()))
                    .period(Set.of(peA.getIsoDate(), peB.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
                    .build())
            .count());
    assertEquals(
        1,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deB.getUid()))
                    .period(Set.of(peB.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid()))
                    .build())
            .count());
    assertEquals(
        1,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deB.getUid()))
                    .period(Set.of(peB.getIsoDate()))
                    .orgUnit(Set.of(ouA.getUid()))
                    .build())
            .count());
    assertEquals(
        1,
        dataExportService
            .exportValues(
                DataExportParams.builder()
                    .dataElement(Set.of(deA.getUid()))
                    .period(Set.of(peA.getIsoDate()))
                    .orgUnit(Set.of(ouB.getUid()))
                    .build())
            .count());
  }

  @Test
  void testGetDataValueCountLastUpdatedBetween() {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deA, peA, ouB, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "3");
    addDataValues(dataValueA, dataValueB, dataValueC);
    assertEquals(
        3, dataValueService.getDataValueCountLastUpdatedBetween(getDate(1970, 1, 1), null, false));
    assertEquals(
        3, dataValueService.getDataValueCountLastUpdatedBetween(getDate(1970, 1, 1), null, true));
    deleteDataValue(dataValueC);
    assertEquals(
        3, dataValueService.getDataValueCountLastUpdatedBetween(getDate(1970, 1, 1), null, true));
    assertEquals(
        2, dataValueService.getDataValueCountLastUpdatedBetween(getDate(1970, 1, 1), null, false));
    deleteDataValue(dataValueB);
    assertEquals(
        3, dataValueService.getDataValueCountLastUpdatedBetween(getDate(1970, 1, 1), null, true));
    assertEquals(
        1, dataValueService.getDataValueCountLastUpdatedBetween(getDate(1970, 1, 1), null, false));
  }

  @Test
  void testValidateMissingDataElement() {
    DataExportParams params =
        DataExportParams.builder()
            .period(Set.of(peB.getIsoDate()))
            .orgUnit(Set.of(ouA.getUid()))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportService.exportValues(params));
    assertEquals(ErrorCode.E2001, ex.getCode());
  }

  @Test
  void testValidateMissingPeriod() {
    DataExportParams params =
        DataExportParams.builder()
            .dataElement(Set.of(deA.getUid(), deB.getUid()))
            .orgUnit(Set.of(ouB.getUid()))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportService.exportValues(params));
    assertEquals(ErrorCode.E2002, ex.getCode());
  }

  @Test
  void testValidatePeriodAndStartEndDate() {
    DataExportParams params =
        DataExportParams.builder()
            .dataElement(Set.of(deA.getUid(), deB.getUid()))
            .period(Set.of(peA.getIsoDate()))
            .startDate(getDate(2022, 1, 1))
            .endDate(getDate(2022, 3, 1))
            .orgUnit(Set.of(ouB.getUid()))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportService.exportValues(params));
    assertEquals(ErrorCode.E2003, ex.getCode());
  }

  @Test
  void testValidateMissingOrgUnit() {
    DataExportParams params =
        DataExportParams.builder()
            .dataElement(Set.of(deA.getUid(), deB.getUid()))
            .period(Set.of(peB.getIsoDate()))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportService.exportValues(params));
    assertEquals(ErrorCode.E2006, ex.getCode());
  }

  private void addDataValues(DataValue... values) {
    if (dataDumpService.upsertValues(values) < values.length) fail("Failed to upsert test data");
  }

  private void deleteDataValue(DataValue dv) {
    dv.setDeleted(true);
    addDataValues(dv);
  }
}
