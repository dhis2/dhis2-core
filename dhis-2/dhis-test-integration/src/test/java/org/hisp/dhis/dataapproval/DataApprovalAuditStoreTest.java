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
import static org.hisp.dhis.dataapproval.DataApprovalAction.APPROVE;
import static org.hisp.dhis.dataapproval.DataApprovalAction.UNAPPROVE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jim Grace
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataApprovalAuditStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataApprovalAuditStore dataApprovalAuditStore;

  @Autowired private DataApprovalLevelService dataApprovalLevelService;

  @Autowired private DataApprovalService dataApprovalService;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @Autowired private OrganisationUnitService organisationUnitService;

  // -------------------------------------------------------------------------
  // Supporting data
  // -------------------------------------------------------------------------
  private DataApprovalLevel level1;

  private DataApprovalLevel level2;

  private DataApprovalWorkflow workflowA;

  private DataApprovalWorkflow workflowB;

  private Period periodA;

  private Period periodB;

  private OrganisationUnit sourceA;

  private OrganisationUnit sourceB;

  private User userA;

  private CategoryOption optionA;

  private CategoryOption optionB;

  private Category categoryA;

  private CategoryCombo categoryComboA;

  private CategoryOptionCombo optionComboA;

  private CategoryOptionCombo optionComboB;

  private Date dateA;

  private Date dateB;

  private DataApprovalAudit auditA;

  private DataApprovalAudit auditB;

  @BeforeAll
  void setUp() {
    // ---------------------------------------------------------------------
    // Add supporting data
    // ---------------------------------------------------------------------
    level1 = new DataApprovalLevel("01", 1, null);
    level2 = new DataApprovalLevel("02", 2, null);
    dataApprovalLevelService.addDataApprovalLevel(level1);
    dataApprovalLevelService.addDataApprovalLevel(level2);
    PeriodType periodType = PeriodType.getPeriodTypeByName("Monthly");
    CategoryCombo defaultCategoryCombo = categoryService.getDefaultCategoryCombo();
    workflowA =
        new DataApprovalWorkflow("workflowA", periodType, defaultCategoryCombo, newHashSet(level1));
    workflowB =
        new DataApprovalWorkflow(
            "workflowB", periodType, defaultCategoryCombo, newHashSet(level1, level2));
    dataApprovalService.addWorkflow(workflowA);
    dataApprovalService.addWorkflow(workflowB);
    periodA = createPeriod(new MonthlyPeriodType(), getDate(2017, 1, 1), getDate(2017, 1, 31));
    periodB = createPeriod(new MonthlyPeriodType(), getDate(2018, 1, 1), getDate(2018, 1, 31));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    sourceA = createOrganisationUnit('A');
    sourceB = createOrganisationUnit('B', sourceA);
    organisationUnitService.addOrganisationUnit(sourceA);
    organisationUnitService.addOrganisationUnit(sourceB);
    userA = makeUser("A");
    userService.addUser(userA);
    optionA = new CategoryOption("CategoryOptionA");
    optionB = new CategoryOption("CategoryOptionB");
    categoryService.addCategoryOption(optionA);
    categoryService.addCategoryOption(optionB);
    categoryA = createCategory('A', optionA, optionB);
    categoryService.addCategory(categoryA);
    categoryComboA = createCategoryCombo('A', categoryA);
    categoryService.addCategoryCombo(categoryComboA);
    optionComboA = createCategoryOptionCombo(categoryComboA, optionA);
    optionComboB = createCategoryOptionCombo(categoryComboA, optionA, optionB);
    categoryService.addCategoryOptionCombo(optionComboA);
    categoryService.addCategoryOptionCombo(optionComboB);
    dateA = getDate(2017, 1, 1);
    dateB = getDate(2018, 1, 1);
    DataApproval approvalA =
        new DataApproval(level1, workflowA, periodA, sourceA, optionComboA, false, dateA, userA);
    DataApproval approvalB =
        new DataApproval(level2, workflowB, periodB, sourceB, optionComboB, false, dateB, userA);
    auditA = new DataApprovalAudit(approvalA, APPROVE);
    auditB = new DataApprovalAudit(approvalB, UNAPPROVE);
  }

  // -------------------------------------------------------------------------
  // DataApprovalAudit
  // -------------------------------------------------------------------------
  @Test
  void testSave() {
    dataApprovalAuditStore.save(auditA);
    dataApprovalAuditStore.save(auditB);
    List<DataApprovalAudit> audits =
        dataApprovalAuditStore.getDataApprovalAudits(new DataApprovalAuditQueryParams());
    assertEquals(2, audits.size());
    assertEquals(auditA, audits.get(0));
    assertEquals(auditB, audits.get(1));
  }

  @Test
  void testDelete() {
    dataApprovalAuditStore.save(auditA);
    dataApprovalAuditStore.save(auditB);
    dataApprovalAuditStore.delete(auditA);
    List<DataApprovalAudit> audits =
        dataApprovalAuditStore.getDataApprovalAudits(new DataApprovalAuditQueryParams());
    assertEquals(1, audits.size());
    assertEquals(auditB, audits.get(0));
  }

  @Test
  void testDeleteDataApprovalAudits() {
    dataApprovalAuditStore.save(auditA);
    dataApprovalAuditStore.save(auditB);
    dataApprovalAuditStore.deleteDataApprovalAudits(sourceB);
    List<DataApprovalAudit> audits =
        dataApprovalAuditStore.getDataApprovalAudits(new DataApprovalAuditQueryParams());
    assertEquals(1, audits.size());
    assertEquals(auditA, audits.get(0));
  }

  @Test
  void TestGetDataApprovalAudits() {
    dataApprovalAuditStore.save(auditA);
    dataApprovalAuditStore.save(auditB);
    DataApprovalAuditQueryParams params;
    List<DataApprovalAudit> audits;
    params = new DataApprovalAuditQueryParams();
    params.setWorkflows(Sets.newHashSet(workflowA));
    audits = dataApprovalAuditStore.getDataApprovalAudits(params);
    assertEquals(1, audits.size());
    assertEquals(auditA, audits.get(0));
    params = new DataApprovalAuditQueryParams();
    params.setLevels(Sets.newHashSet(level1));
    audits = dataApprovalAuditStore.getDataApprovalAudits(params);
    assertEquals(1, audits.size());
    assertEquals(auditA, audits.get(0));
    params = new DataApprovalAuditQueryParams();
    params.setOrganisationUnits(Sets.newHashSet(sourceA));
    audits = dataApprovalAuditStore.getDataApprovalAudits(params);
    assertEquals(1, audits.size());
    assertEquals(auditA, audits.get(0));
    params = new DataApprovalAuditQueryParams();
    params.setAttributeOptionCombos(Sets.newHashSet(optionComboA));
    audits = dataApprovalAuditStore.getDataApprovalAudits(params);
    assertEquals(1, audits.size());
    assertEquals(auditA, audits.get(0));
    params = new DataApprovalAuditQueryParams();
    params.setStartDate(dateB);
    audits = dataApprovalAuditStore.getDataApprovalAudits(params);
    assertEquals(1, audits.size());
    assertEquals(auditB, audits.get(0));
    params = new DataApprovalAuditQueryParams();
    params.setEndDate(dateB);
    audits = dataApprovalAuditStore.getDataApprovalAudits(params);
    assertEquals(1, audits.size());
    assertEquals(auditA, audits.get(0));

    User userB = createAndAddUser(newHashSet(sourceB), null);
    injectSecurityContextUser(userB);

    params = new DataApprovalAuditQueryParams();
    audits = dataApprovalAuditStore.getDataApprovalAudits(params);
    assertEquals(1, audits.size());
    assertEquals(auditB, audits.get(0));
  }

  @Test
  @DisplayName("Deleting audits by category option combo deletes the correct entries")
  void deleteByCocTest() {
    // given
    CategoryOptionCombo coc1 = createCategoryOptionCombo('1');
    coc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc1);

    CategoryOptionCombo coc2 = createCategoryOptionCombo('2');
    coc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc2);

    CategoryOptionCombo coc3 = createCategoryOptionCombo('3');
    coc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc3);

    DataApproval approvalX =
        new DataApproval(level1, workflowA, periodA, sourceA, coc1, false, dateA, userA);
    DataApproval approvalY =
        new DataApproval(level2, workflowB, periodB, sourceB, coc2, false, dateB, userA);
    DataApproval approvalZ =
        new DataApproval(level2, workflowB, periodB, sourceA, coc3, false, dateB, userA);

    DataApprovalAudit auditA = new DataApprovalAudit(approvalX, APPROVE);
    DataApprovalAudit auditB = new DataApprovalAudit(approvalY, UNAPPROVE);
    DataApprovalAudit auditC = new DataApprovalAudit(approvalZ, UNAPPROVE);
    dataApprovalAuditStore.save(auditA);
    dataApprovalAuditStore.save(auditB);
    dataApprovalAuditStore.save(auditC);

    // when
    dataApprovalAuditStore.deleteDataApprovalAudits(coc1);
    dataApprovalAuditStore.deleteDataApprovalAudits(coc2);

    // then
    List<DataApprovalAudit> audits =
        dataApprovalAuditStore.getDataApprovalAudits(new DataApprovalAuditQueryParams());
    assertEquals(1, audits.size());
    assertEquals(auditC, audits.get(0));
  }
}
