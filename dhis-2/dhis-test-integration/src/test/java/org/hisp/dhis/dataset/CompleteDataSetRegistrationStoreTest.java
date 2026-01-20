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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ConflictException;
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

/**
 * @author david mackessy
 */
@Transactional
class CompleteDataSetRegistrationStoreTest extends PostgresIntegrationTestBase {

  @Autowired private CompleteDataSetRegistrationService completeDataSetRegistrationService;
  @Autowired private CompleteDataSetRegistrationStore completeDataSetRegistrationStore;
  @Autowired private DataSetService dataSetService;
  @Autowired private PeriodService periodService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private CategoryService categoryService;

  private DataElement elementA;
  private DataElement elementB;
  private DataElement elementC;
  private DataSet dataSetA;
  private DataSet dataSetB;
  private DataSet dataSetC;
  private Period periodA;
  private Period periodB;
  private OrganisationUnit sourceA;
  private OrganisationUnit sourceB;
  private OrganisationUnit sourceC;

  @BeforeEach
  void setUp() {
    sourceA = createOrganisationUnit('A');
    sourceB = createOrganisationUnit('B');
    sourceC = createOrganisationUnit('C');
    manager.save(List.of(sourceA, sourceB, sourceC));

    periodA = createPeriod(new MonthlyPeriodType(), getDate(2000, 1, 1), getDate(2000, 1, 31));
    periodB = createPeriod(new MonthlyPeriodType(), getDate(2000, 2, 1), getDate(2000, 2, 28));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);

    elementA = createDataElement('A');
    elementB = createDataElement('B');
    elementC = createDataElement('C');
    manager.save(List.of(elementA, elementB, elementC));

    dataSetA = createDataSet('A', new MonthlyPeriodType());
    dataSetB = createDataSet('B', new MonthlyPeriodType());
    dataSetC = createDataSet('C', new MonthlyPeriodType());
    dataSetA.addDataSetElement(elementA);
    dataSetB.addDataSetElement(elementB);
    dataSetC.addDataSetElement(elementC);

    dataSetA.getSources().add(sourceA);
    dataSetB.getSources().add(sourceB);
    dataSetC.getSources().add(sourceA);
    dataSetService.addDataSet(dataSetA);
    dataSetService.addDataSet(dataSetB);
    dataSetService.addDataSet(dataSetC);
  }

  @Test
  @DisplayName("Get all CompleteDataSetRegistration by CategoryOptionCombo")
  void testSaveGet() throws ConflictException {
    // given
    CategoryOptionCombo aoc1 = createCategoryOptionCombo('1');
    aoc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(aoc1);

    CategoryOptionCombo aoc2 = createCategoryOptionCombo('2');
    aoc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(aoc2);

    CategoryOptionCombo aoc3 = createCategoryOptionCombo('3');
    aoc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(aoc3);

    CompleteDataSetRegistration registrationA =
        new CompleteDataSetRegistration(
            dataSetA, periodA, sourceA, aoc1, new Date(), "", new Date(), "", true);
    CompleteDataSetRegistration registrationB =
        new CompleteDataSetRegistration(
            dataSetB, periodB, sourceA, aoc2, new Date(), "", new Date(), "", true);
    CompleteDataSetRegistration registrationC =
        new CompleteDataSetRegistration(
            dataSetC, periodB, sourceB, aoc3, new Date(), "", new Date(), "", true);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationA);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationB);
    completeDataSetRegistrationService.saveCompleteDataSetRegistration(registrationC);

    // when
    List<CompleteDataSetRegistration> allByCategoryOptionCombo =
        completeDataSetRegistrationStore.getAllByCategoryOptionCombo(
            UID.of(aoc1.getUid(), aoc2.getUid()));
    assertEquals(2, allByCategoryOptionCombo.size());
    assertTrue(
        allByCategoryOptionCombo.containsAll(List.of(registrationA, registrationB)),
        "retrieved registrations contains 2 registrations referencing the 2 attribute opt combos passed in");
    assertFalse(
        allByCategoryOptionCombo.contains(registrationC),
        "retrieved registrations do not contain a registration referencing a AOC not used in the query");
  }
}
