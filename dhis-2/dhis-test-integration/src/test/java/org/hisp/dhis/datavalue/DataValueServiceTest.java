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

import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
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

  @Autowired private DataValueService dataValueService;
  @Autowired private DataValueStore dataValueStore;

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
  void testAddDataValue() {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deC, peC, ouA, optionCombo, optionCombo, "3");
    addDataValues(dataValueA, dataValueB, dataValueC);
    DataExportValue dvA =
        dataValueService.getDataValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo));
    assertNotNull(dvA);
    assertNotNull(dvA.created());
    assertEquals(ouA.getUid(), dvA.orgUnit().getValue());
    assertEquals(deA.getUid(), dvA.dataElement().getValue());
    assertEquals(peA.getIsoDate(), dvA.period());
    assertEquals("1", dvA.value());
    DataExportValue dvB =
        dataValueService.getDataValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo));
    assertNotNull(dvB);
    assertNotNull(dvB.created());
    assertEquals(ouA.getUid(), dvB.orgUnit().getValue());
    assertEquals(deB.getUid(), dvB.dataElement().getValue());
    assertEquals(peA.getIsoDate(), dvB.period());
    assertEquals("2", dvB.value());
    DataExportValue dvC =
        dataValueService.getDataValue(new DataEntryKey(deC, peC, ouA, optionCombo, optionCombo));
    assertNotNull(dvC);
    assertNotNull(dvC.created());
    assertEquals(ouA.getUid(), dvC.orgUnit().getValue());
    assertEquals(deC.getUid(), dvC.dataElement().getValue());
    assertEquals(peC.getIsoDate(), dvC.period());
    assertEquals("3", dvC.value());
  }

  @Test
  void testUpdateDataValue() {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "2");
    addDataValues(dataValueA, dataValueB);
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deB, peA, ouB, optionCombo, optionCombo)));
    dataValueA.setValue("5");
    addDataValues(dataValueA);
    DataExportValue dvA =
        dataValueService.getDataValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo));
    assertNotNull(dvA);
    assertEquals("5", dvA.value());
    DataExportValue dvB =
        dataValueService.getDataValue(new DataEntryKey(deB, peA, ouB, optionCombo, optionCombo));
    assertNotNull(dvB);
    assertEquals("2", dvB.value());
  }

  @Test
  void testDeleteAndGetDataValue() {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deC, peC, ouD, optionCombo, optionCombo, "3");
    DataValue dataValueD = new DataValue(deD, peC, ouB, optionCombo, optionCombo, "4");
    addDataValues(dataValueA, dataValueB, dataValueC, dataValueD);
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
    deleteDataValue(dataValueA);
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
    deleteDataValue(dataValueB);
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
    deleteDataValue(dataValueC);
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNotNull(
        dataValueService.getDataValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
    deleteDataValue(dataValueD);
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deA, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deB, peA, ouA, optionCombo, optionCombo)));
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deC, peC, ouD, optionCombo, optionCombo)));
    assertNull(
        dataValueService.getDataValue(new DataEntryKey(deD, peC, ouB, optionCombo, optionCombo)));
  }

  // -------------------------------------------------------------------------
  // Collections of DataValues
  // -------------------------------------------------------------------------

  @Test
  void testGetDataValuesDataExportParamsA() {
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
    DataExportStoreParams params =
        new DataExportStoreParams()
            .setDataElements(Set.of(deA))
            .setPeriods(Set.of(peA, peB, peC))
            .setOrganisationUnits(Set.of(ouA));
    List<DataExportValue> values = dataValueService.getDataValues(params);
    assertEquals(
        Set.of("1", "3", "9"), values.stream().map(DataExportValue::value).collect(toSet()));
    params =
        new DataExportStoreParams()
            .setDataElements(Set.of(deB))
            .setPeriods(Set.of(peA))
            .setOrganisationUnits(Set.of(ouA, ouB));
    values = dataValueService.getDataValues(params);
    assertEquals(Set.of("5", "6"), values.stream().map(DataExportValue::value).collect(toSet()));
  }

  @Test
  void testGetDataValuesDataExportParamsB() {
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
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA, deB))
                    .setPeriods(Set.of(peB))
                    .setOrganisationUnits(Set.of(ouA, ouB)))
            .size());
    assertEquals(
        2,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA, deB))
                    .setPeriods(Set.of(peA))
                    .setOrganisationUnits(Set.of(ouB)))
            .size());
    assertEquals(
        4,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA))
                    .setPeriods(Set.of(peA, peC))
                    .setOrganisationUnits(Set.of(ouA, ouB)))
            .size());
    assertEquals(
        4,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deB))
                    .setPeriods(Set.of(peA, peB))
                    .setOrganisationUnits(Set.of(ouA, ouB)))
            .size());
    assertEquals(
        1,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deB))
                    .setPeriods(Set.of(peB))
                    .setOrganisationUnits(Set.of(ouA)))
            .size());
    assertEquals(
        1,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA))
                    .setPeriods(Set.of(peA))
                    .setOrganisationUnits(Set.of(ouB)))
            .size());
    assertEquals(
        1,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA))
                    .setStartDate(peA.getStartDate())
                    .setEndDate(peA.getEndDate())
                    .setOrganisationUnits(Set.of(ouB)))
            .size());
  }

  @Test
  void testGetDataValuesExportParamsC() {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deA, peA, ouB, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "3");
    DataValue dataValueD = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "4");
    addDataValues(dataValueA, dataValueB, dataValueC, dataValueD);

    assertEquals(
        2,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA))
                    .setCategoryOptionCombos(Set.of(optionCombo))
                    .setPeriods(Set.of(peA))
                    .setOrganisationUnits(Set.of(ouA, ouB)))
            .size());
  }

  @Test
  void testGetDataValuesNonExistingPeriodA() {

    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    addDataValues(dataValueA);

    assertEquals(
        0,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA))
                    .setCategoryOptionCombos(Set.of(optionCombo))
                    .setPeriods(Set.of(peX))
                    .setOrganisationUnits(Set.of(ouA)))
            .size());
  }

  @Test
  void testGetDataValuesNonExistingPeriodB() {
    DataValue dataValueA = new DataValue(deA, peA, ouA, optionCombo, optionCombo, "1");
    DataValue dataValueB = new DataValue(deA, peA, ouB, optionCombo, optionCombo, "2");
    DataValue dataValueC = new DataValue(deB, peA, ouA, optionCombo, optionCombo, "3");
    DataValue dataValueD = new DataValue(deB, peA, ouB, optionCombo, optionCombo, "4");
    addDataValues(dataValueA, dataValueB, dataValueC, dataValueD);

    assertEquals(
        2,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA))
                    .setCategoryOptionCombos(Set.of(optionCombo))
                    .setPeriods(Set.of(peA, peX))
                    .setOrganisationUnits(Set.of(ouA, ouB)))
            .size());
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
    assertEquals(4, dataValueStore.getAllDataValues().size());
  }

  @Test
  void testGetDataValuesDataElementsPeriodsOrgUnits() {
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
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA, deB))
                    .setPeriods(Set.of(peB))
                    .setOrganisationUnits(Set.of(ouA, ouB)))
            .size());
    assertEquals(
        2,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA, deB))
                    .setPeriods(Set.of(peA))
                    .setOrganisationUnits(Set.of(ouB)))
            .size());
    assertEquals(
        2,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA))
                    .setPeriods(Set.of(peC))
                    .setOrganisationUnits(Set.of(ouA, ouB)))
            .size());
    assertEquals(
        4,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA))
                    .setPeriods(Set.of(peA, peC))
                    .setOrganisationUnits(Set.of(ouA, ouB)))
            .size());
    assertEquals(
        4,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deB))
                    .setPeriods(Set.of(peA, peB))
                    .setOrganisationUnits(Set.of(ouA, ouB)))
            .size());
    assertEquals(
        1,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deB))
                    .setPeriods(Set.of(peB))
                    .setOrganisationUnits(Set.of(ouA)))
            .size());
    assertEquals(
        1,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deB))
                    .setPeriods(Set.of(peB))
                    .setOrganisationUnits(Set.of(ouA)))
            .size());
    assertEquals(
        1,
        dataValueService
            .getDataValues(
                new DataExportStoreParams()
                    .setDataElements(Set.of(deA))
                    .setPeriods(Set.of(peA))
                    .setOrganisationUnits(Set.of(ouB)))
            .size());
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
  void testVAlidateMissingDataElement() {
    assertIllegalQueryEx(
        assertThrows(
            IllegalQueryException.class,
            () ->
                dataValueService.validate(
                    new DataExportStoreParams()
                        .setPeriods(Set.of(peB))
                        .setOrganisationUnits(Set.of(ouA)))),
        ErrorCode.E2001);
  }

  @Test
  void testValidateMissingPeriod() {
    assertIllegalQueryEx(
        assertThrows(
            IllegalQueryException.class,
            () ->
                dataValueService.validate(
                    new DataExportStoreParams()
                        .setDataElements(Set.of(deA, deB))
                        .setOrganisationUnits(Set.of(ouB)))),
        ErrorCode.E2002);
  }

  @Test
  void testValidatePeriodAndStartEndDate() {
    assertIllegalQueryEx(
        assertThrows(
            IllegalQueryException.class,
            () ->
                dataValueService.validate(
                    new DataExportStoreParams()
                        .setDataElements(Set.of(deA, deB))
                        .setPeriods(Set.of(peA))
                        .setStartDate(getDate(2022, 1, 1))
                        .setEndDate(getDate(2022, 3, 1))
                        .setOrganisationUnits(Set.of(ouB)))),
        ErrorCode.E2003);
  }

  @Test
  void testValidateMissingOrgUnit() {
    assertIllegalQueryEx(
        assertThrows(
            IllegalQueryException.class,
            () ->
                dataValueService.validate(
                    new DataExportStoreParams()
                        .setDataElements(Set.of(deA, deB))
                        .setPeriods(Set.of(peB)))),
        ErrorCode.E2006);
  }

  private void addDataValues(DataValue... values) {
    if (dataDumpService.upsertValues(values) < values.length) fail("Failed to upsert test data");
  }

  private void deleteDataValue(DataValue dv) {
    dv.setDeleted(true);
    addDataValues(dv);
  }
}
