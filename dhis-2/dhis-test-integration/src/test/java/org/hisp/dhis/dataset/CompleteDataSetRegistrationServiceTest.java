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
package org.hisp.dhis.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class CompleteDataSetRegistrationServiceTest extends SingleSetupIntegrationTestBase {

  @Autowired private CompleteDataSetRegistrationService completeDataSetRegistrationService;

  @Autowired private DataSetService dataSetService;

  @Autowired private DataElementService dataElementService;

  @Autowired private DataValueService dataValueService;

  @Autowired private PeriodService periodService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private CategoryService categoryService;

  private DataElement elementA;

  private DataElement elementB;

  private DataElement elementC;

  private DataElement elementD;

  private DataElement elementE;

  private DataSet dataSetA;

  private DataSet dataSetB;

  private DataSet dataSetC;

  private Period periodA;

  private Period periodB;

  private OrganisationUnit sourceA;

  private OrganisationUnit sourceB;

  private OrganisationUnit sourceC;

  private Date onTimeA;

  private CategoryOptionCombo optionCombo;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  @Override
  public void setUpTest() {
    sourceA = createOrganisationUnit('A');
    sourceB = createOrganisationUnit('B');
    sourceC = createOrganisationUnit('C');
    organisationUnitService.addOrganisationUnit(sourceA);
    organisationUnitService.addOrganisationUnit(sourceB);
    organisationUnitService.addOrganisationUnit(sourceC);
    periodA = createPeriod(new MonthlyPeriodType(), getDate(2000, 1, 1), getDate(2000, 1, 31));
    periodB = createPeriod(new MonthlyPeriodType(), getDate(2000, 2, 1), getDate(2000, 2, 28));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    elementA = createDataElement('A');
    elementB = createDataElement('B');
    elementC = createDataElement('C');
    elementD = createDataElement('D');
    elementE = createDataElement('E');
    dataElementService.addDataElement(elementA);
    dataElementService.addDataElement(elementB);
    dataElementService.addDataElement(elementC);
    dataElementService.addDataElement(elementD);
    dataElementService.addDataElement(elementE);
    dataSetA = createDataSet('A', new MonthlyPeriodType());
    dataSetB = createDataSet('B', new MonthlyPeriodType());
    dataSetC = createDataSet('C', new MonthlyPeriodType());
    dataSetA.addDataSetElement(elementA);
    dataSetA.addDataSetElement(elementB);
    dataSetA.addDataSetElement(elementC);
    dataSetA.addDataSetElement(elementD);
    dataSetA.addDataSetElement(elementE);
    dataSetA.getSources().add(sourceA);
    dataSetA.getSources().add(sourceB);
    dataSetB.getSources().add(sourceA);
    dataSetB.getSources().add(sourceB);
    dataSetC.getSources().add(sourceA);
    dataSetC.getSources().add(sourceB);
    dataSetService.addDataSet(dataSetA);
    dataSetService.addDataSet(dataSetB);
    dataSetService.addDataSet(dataSetC);
    optionCombo = categoryService.getDefaultCategoryOptionCombo();
    onTimeA = getDate(2000, 1, 10);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  @Test
  void testSaveGet() {
    CompleteDataSetRegistration registrationA =
        new CompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo, new Date(), "", new Date(), "", true);
    CompleteDataSetRegistration registrationB =
        new CompleteDataSetRegistration(
            dataSetB, periodB, sourceA, optionCombo, new Date(), "", new Date(), "", true);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationA);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationB);
    assertEquals(
        registrationA,
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo));
    assertEquals(
        registrationB,
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetB, periodB, sourceA, optionCombo));
  }

  @Test
  void testSaveAutoProperties() {
    CompleteDataSetRegistration registration =
        new CompleteDataSetRegistration(dataSetA, periodA, sourceA, optionCombo, true);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registration);
    registration =
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo);
    assertNotNull(registration);
    assertNotNull(registration.getDate());
    assertNotNull(registration.getLastUpdated());
  }

  @Test
  void testDelete() {
    CompleteDataSetRegistration registrationA =
        new CompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo, new Date(), "", new Date(), "", true);
    CompleteDataSetRegistration registrationB =
        new CompleteDataSetRegistration(
            dataSetB, periodB, sourceA, optionCombo, new Date(), "", new Date(), "", true);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationA);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationB);
    assertNotNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo));
    assertNotNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetB, periodB, sourceA, optionCombo));
    completeDataSetRegistrationService.deleteCompleteDataSetRegistration(registrationA);
    assertNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo));
    assertNotNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetB, periodB, sourceA, optionCombo));
  }

  @Test
  void testGetAll() {
    CompleteDataSetRegistration registrationA =
        new CompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo, new Date(), "", new Date(), "", true);
    CompleteDataSetRegistration registrationB =
        new CompleteDataSetRegistration(
            dataSetB, periodB, sourceA, optionCombo, new Date(), "", new Date(), "", true);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationA);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationB);
    List<CompleteDataSetRegistration> registrations =
        completeDataSetRegistrationService.getAllCompleteDataSetRegistrations();
    assertEquals(2, registrations.size());
    assertTrue(registrations.contains(registrationA));
    assertTrue(registrations.contains(registrationB));
  }

  @Test
  void testDeleteByDataSet() {
    CompleteDataSetRegistration registrationA =
        new CompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo, onTimeA, "", onTimeA, "", true);
    CompleteDataSetRegistration registrationB =
        new CompleteDataSetRegistration(
            dataSetA, periodB, sourceA, optionCombo, onTimeA, "", onTimeA, "", true);
    CompleteDataSetRegistration registrationC =
        new CompleteDataSetRegistration(
            dataSetB, periodA, sourceA, optionCombo, onTimeA, "", onTimeA, "", true);
    CompleteDataSetRegistration registrationD =
        new CompleteDataSetRegistration(
            dataSetB, periodB, sourceA, optionCombo, onTimeA, "", onTimeA, "", true);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationA);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationB);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationC);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationD);
    assertNotNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo));
    assertNotNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetA, periodB, sourceA, optionCombo));
    assertNotNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetB, periodA, sourceA, optionCombo));
    assertNotNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetB, periodB, sourceA, optionCombo));
    completeDataSetRegistrationService.deleteCompleteDataSetRegistrations(dataSetA);
    assertNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetA, periodA, sourceA, optionCombo));
    assertNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetA, periodB, sourceA, optionCombo));
    assertNotNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetB, periodA, sourceA, optionCombo));
    assertNotNull(
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            dataSetB, periodB, sourceA, optionCombo));
  }

  @Test
  void testGetMissingCompulsoryFields() {
    DataElementOperand compulsoryA = new DataElementOperand(elementA, optionCombo);
    DataElementOperand compulsoryB = new DataElementOperand(elementB, optionCombo);
    DataElementOperand compulsoryC = new DataElementOperand(elementC, optionCombo);
    dataSetA.addCompulsoryDataElementOperand(compulsoryA);
    dataSetA.addCompulsoryDataElementOperand(compulsoryB);
    dataSetA.addCompulsoryDataElementOperand(compulsoryC);
    dataValueService.addDataValue(
        new DataValue(elementA, periodA, sourceA, optionCombo, optionCombo, "10"));
    dataValueService.addDataValue(
        new DataValue(elementE, periodA, sourceA, optionCombo, optionCombo, "20"));
    List<DataElementOperand> missingFields =
        completeDataSetRegistrationService.getMissingCompulsoryFields(
            dataSetA, periodA, sourceA, optionCombo);
    Collections.sort(missingFields);
    assertEquals(2, missingFields.size());
    assertEquals("DataElementB", missingFields.get(0).getDataElement().getName());
    assertEquals("DataElementC", missingFields.get(1).getDataElement().getName());
  }
}
