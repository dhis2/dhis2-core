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
package org.hisp.dhis.dataapproval;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
class DataApprovalStoreUserTest extends PostgresIntegrationTestBase {

  @Autowired private DataApprovalStore dataApprovalStore;

  @Autowired private DataApprovalService dataApprovalService;

  @Autowired private DataApprovalLevelService dataApprovalLevelService;

  @Autowired protected PeriodService periodService;

  @Autowired protected DataSetService dataSetService;

  @Autowired private OrganisationUnitService organisationUnitService;

  // -------------------------------------------------------------------------
  // Supporting data
  // -------------------------------------------------------------------------
  private Period periodA;

  private DataApprovalLevel level1;

  private DataApprovalLevel level2;

  private DataApprovalLevel level3;

  private DataApprovalWorkflow workflowA;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private OrganisationUnit orgUnitC;

  private OrganisationUnit orgUnitD;

  private User currentUser;

  @BeforeEach
  void setUp() {
    periodA = createPeriod("201801");
    periodService.addPeriod(periodA);
    level1 = new DataApprovalLevel("Level1", 1, null);
    level2 = new DataApprovalLevel("Level2", 2, null);
    level3 = new DataApprovalLevel("Level3", 3, null);
    dataApprovalLevelService.addDataApprovalLevel(level1);
    dataApprovalLevelService.addDataApprovalLevel(level2);
    dataApprovalLevelService.addDataApprovalLevel(level3);
    PeriodType periodType = PeriodType.getPeriodTypeByName("Monthly");
    CategoryCombo defaultCategoryCombo = categoryService.getDefaultCategoryCombo();
    workflowA =
        new DataApprovalWorkflow(
            "workflowA", periodType, defaultCategoryCombo, newHashSet(level1, level2, level3));
    dataApprovalService.addWorkflow(workflowA);
    DataSet dataSetA = createDataSet('A');
    dataSetA.assignWorkflow(workflowA);
    dataSetService.addDataSet(dataSetA);
    orgUnitA = createOrganisationUnit('A');
    orgUnitB = createOrganisationUnit('B', orgUnitA);
    orgUnitC = createOrganisationUnit('C', orgUnitB);
    orgUnitD = createOrganisationUnit('D', orgUnitA);
    organisationUnitService.addOrganisationUnit(orgUnitA);
    organisationUnitService.addOrganisationUnit(orgUnitB);
    organisationUnitService.addOrganisationUnit(orgUnitC);
    organisationUnitService.addOrganisationUnit(orgUnitD);
    orgUnitA.addDataSet(dataSetA);
    orgUnitB.addDataSet(dataSetA);
    orgUnitC.addDataSet(dataSetA);
    orgUnitD.addDataSet(dataSetA);
    organisationUnitService.updateOrganisationUnit(orgUnitA);
    organisationUnitService.updateOrganisationUnit(orgUnitB);
    organisationUnitService.updateOrganisationUnit(orgUnitC);
    organisationUnitService.updateOrganisationUnit(orgUnitD);

    currentUser = createAndAddUser(true, "username", newHashSet(orgUnitA), newHashSet(orgUnitA));
    injectSecurityContextUser(currentUser);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void testGetDataApprovalStatuses() {
    CategoryOption catOptionA = new CategoryOption("CategoryOptionA");
    catOptionA.addOrganisationUnit(orgUnitB);
    categoryService.addCategoryOption(catOptionA);
    Category catA = createCategory('A', catOptionA);
    categoryService.addCategory(catA);
    CategoryCombo catComboA = createCategoryCombo('A', catA);
    categoryService.addCategoryCombo(catComboA);
    CategoryOptionCombo catOptionComboA = createCategoryOptionCombo(catComboA, catOptionA);
    categoryService.addCategoryOptionCombo(catOptionComboA);
    List<DataApprovalStatus> statuses =
        dataApprovalStore.getDataApprovalStatuses(
            workflowA,
            periodA,
            Lists.newArrayList(orgUnitA),
            orgUnitA.getHierarchyLevel(),
            null,
            catComboA,
            null,
            dataApprovalLevelService.getUserDataApprovalLevelsOrLowestLevel(currentUser, workflowA),
            dataApprovalLevelService.getDataApprovalLevelMap());
    assertEquals(1, statuses.size());
    DataApprovalStatus status = statuses.get(0);
    assertEquals(DataApprovalState.UNAPPROVED_WAITING, status.getState());
    assertEquals(orgUnitA.getUid(), status.getOrganisationUnitUid());
    assertEquals(orgUnitA.getName(), status.getOrganisationUnitName());
    assertEquals(catOptionComboA.getUid(), status.getAttributeOptionComboUid());
    statuses =
        dataApprovalStore.getDataApprovalStatuses(
            workflowA,
            periodA,
            Lists.newArrayList(orgUnitB),
            orgUnitB.getHierarchyLevel(),
            null,
            catComboA,
            null,
            dataApprovalLevelService.getUserDataApprovalLevelsOrLowestLevel(currentUser, workflowA),
            dataApprovalLevelService.getDataApprovalLevelMap());
    assertEquals(1, statuses.size());
    status = statuses.get(0);
    assertEquals(DataApprovalState.UNAPPROVED_WAITING, status.getState());
    assertEquals(orgUnitB.getUid(), status.getOrganisationUnitUid());
    assertEquals(orgUnitB.getName(), status.getOrganisationUnitName());
    assertEquals(catOptionComboA.getUid(), status.getAttributeOptionComboUid());
    statuses =
        dataApprovalStore.getDataApprovalStatuses(
            workflowA,
            periodA,
            Lists.newArrayList(orgUnitC),
            orgUnitC.getHierarchyLevel(),
            null,
            catComboA,
            null,
            dataApprovalLevelService.getUserDataApprovalLevelsOrLowestLevel(currentUser, workflowA),
            dataApprovalLevelService.getDataApprovalLevelMap());
    assertEquals(1, statuses.size());
    status = statuses.get(0);
    assertEquals(DataApprovalState.UNAPPROVED_READY, status.getState());
    assertEquals(orgUnitC.getUid(), status.getOrganisationUnitUid());
    assertEquals(orgUnitC.getName(), status.getOrganisationUnitName());
    assertEquals(catOptionComboA.getUid(), status.getAttributeOptionComboUid());
    statuses =
        dataApprovalStore.getDataApprovalStatuses(
            workflowA,
            periodA,
            Lists.newArrayList(orgUnitD),
            orgUnitD.getHierarchyLevel(),
            null,
            catComboA,
            null,
            null,
            null);
    assertEquals(0, statuses.size());
  }
}
