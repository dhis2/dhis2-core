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
package org.hisp.dhis.dataapproval;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hisp.dhis.security.acl.AccessStringHelper.CATEGORY_OPTION_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeApprovedException;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
class DataApprovalServiceTest extends IntegrationTestBase {
  private static final String AUTH_APPR_LEVEL = "F_SYSTEM_SETTING";

  private static final boolean ACCEPTED = true;

  private static final boolean NOT_ACCEPTED = false;

  @Autowired private DataApprovalService dataApprovalService;

  @Autowired private DataApprovalStore dataApprovalStore;

  @Autowired private DataApprovalLevelService dataApprovalLevelService;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired protected UserService _userService;

  @Autowired protected CurrentUserService currentUserService;

  @Autowired protected DataSetService dataSetService;

  @Autowired private SystemSettingManager systemSettingManager;

  // -------------------------------------------------------------------------
  // Supporting data
  // -------------------------------------------------------------------------
  private CategoryCombo defaultCategoryCombo;

  private CategoryOptionCombo defaultOptionCombo;

  private PeriodType periodType;

  // Monthly: Jan
  private Period periodA;

  // Monthly: Feb
  private Period periodB;

  // Monthly: Mar
  private Period periodC;

  // Monthly: December
  private Period periodM12;

  // Daily
  private Period periodD;

  // Quarterly
  private Period periodQ;

  // Weekly
  private Period periodW;

  // Weekly
  private Period periodW4;

  // Weekly
  private Period periodW5;

  // Yearly
  private Period periodY;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private OrganisationUnit organisationUnitC;

  private OrganisationUnit organisationUnitD;

  private OrganisationUnit organisationUnitE;

  private OrganisationUnit organisationUnitF;

  private DataApprovalLevel level1;

  private DataApprovalLevel level2;

  private DataApprovalLevel level3;

  private DataApprovalLevel level4;

  private DataApprovalLevel level2ABCD;

  private DataApprovalLevel level2EFGH;

  private DataApprovalWorkflow workflow0;

  private DataApprovalWorkflow workflow1;

  private DataApprovalWorkflow workflow12;

  private DataApprovalWorkflow workflow12A;

  private DataApprovalWorkflow workflow12B;

  private DataApprovalWorkflow workflow12A_H;

  private DataApprovalWorkflow workflow3;

  private DataApprovalWorkflow workflow1234;

  private DataApprovalWorkflow workflow13;

  private DataApprovalWorkflow workflow24;

  private DataSet dataSetA;

  private DataSet dataSetB;

  private DataSet dataSetC;

  private DataSet dataSetD;

  private DataSet dataSetE;

  private DataSet dataSetF;

  private DataSet dataSetG;

  private DataSet dataSetH;

  private DataSet dataSetI;

  private DataSet dataSetJ;

  private User userA;

  private User userB;

  private CategoryOption optionA;

  private CategoryOption optionB;

  private CategoryOption optionC;

  private CategoryOption optionD;

  private CategoryOption optionE;

  private CategoryOption optionF;

  private CategoryOption optionG;

  private CategoryOption optionH;

  private CategoryOptionCombo optionComboAE;

  private CategoryOptionCombo optionComboAF;

  private CategoryOptionCombo optionComboAG;

  private CategoryOptionCombo optionComboAH;

  private CategoryOptionCombo optionComboBE;

  private CategoryOptionCombo optionComboBF;

  private CategoryOptionCombo optionComboBG;

  private CategoryOptionCombo optionComboBH;

  private CategoryOptionCombo optionComboCE;

  private CategoryOptionCombo optionComboCF;

  private CategoryOptionCombo optionComboCG;

  private CategoryOptionCombo optionComboCH;

  private CategoryOptionCombo optionComboDE;

  private CategoryOptionCombo optionComboDF;

  private CategoryOptionCombo optionComboDG;

  private CategoryOptionCombo optionComboDH;

  private Category categoryA;

  private Category categoryB;

  private CategoryCombo categoryComboA;

  private CategoryOptionGroup groupAB;

  private CategoryOptionGroup groupCD;

  private CategoryOptionGroup groupEF;

  private CategoryOptionGroup groupGH;

  private CategoryOptionGroupSet groupSetABCD;

  private CategoryOptionGroupSet groupSetEFGH;

  // -------------------------------------------------------------------------
  // Set up/tear down
  // -------------------------------------------------------------------------
  @Override
  public void setUpTest() {
    userService = _userService;
    // ---------------------------------------------------------------------
    // Add supporting data
    // ---------------------------------------------------------------------
    defaultCategoryCombo = categoryService.getDefaultCategoryCombo();
    defaultOptionCombo = categoryService.getDefaultCategoryOptionCombo();
    periodType = periodService.reloadPeriodType(PeriodType.getPeriodTypeByName("Monthly"));
    // Monthly: Jan
    periodA = createPeriod("201401");
    // Monthly: Feb
    periodB = createPeriod("201402");
    // Monthly: Mar
    periodC = createPeriod("201403");
    // Monthly: December
    periodM12 = createPeriod("201412");
    // Daily
    periodD = createPeriod("20140105");
    // Quarterly
    periodQ = createPeriod("2014Q1");
    // Weekly, Monday 2013-12-30 to
    periodW = createPeriod("2014W1");
    // Sunday 2014-01-05
    // Weekly, Monday 2014-01-20 to
    periodW4 = createPeriod("2014W4");
    // Sunday 2014-01-26
    // Weekly, Monday 2014-01-27 to
    periodW5 = createPeriod("2014W5");
    // Sunday 2014-02-02
    // Yearly
    periodY = createPeriod("2014");
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    periodService.addPeriod(periodY);
    periodService.addPeriod(periodW);
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    periodService.addPeriod(periodC);
    periodService.addPeriod(periodQ);
    //
    // Organisation unit hierarchy:
    //
    // A - Level 1
    // |
    // B - Level 2
    // |\
    // C E - Level 3
    // | |
    // D F - Level 4
    //
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitB = createOrganisationUnit('B', organisationUnitA);
    organisationUnitC = createOrganisationUnit('C', organisationUnitB);
    organisationUnitD = createOrganisationUnit('D', organisationUnitC);
    organisationUnitE = createOrganisationUnit('E', organisationUnitB);
    organisationUnitF = createOrganisationUnit('F', organisationUnitE);
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    organisationUnitService.addOrganisationUnit(organisationUnitC);
    organisationUnitService.addOrganisationUnit(organisationUnitD);
    organisationUnitService.addOrganisationUnit(organisationUnitE);
    organisationUnitService.addOrganisationUnit(organisationUnitF);
    level1 = new DataApprovalLevel("level1", 1, null);
    level2 = new DataApprovalLevel("level2", 2, null);
    level3 = new DataApprovalLevel("level3", 3, null);
    level4 = new DataApprovalLevel("level4", 4, null);
    dataApprovalLevelService.addDataApprovalLevel(level1);
    dataApprovalLevelService.addDataApprovalLevel(level2);
    dataApprovalLevelService.addDataApprovalLevel(level3);
    dataApprovalLevelService.addDataApprovalLevel(level4);
    workflow0 = new DataApprovalWorkflow("workflow0", periodType, newHashSet());
    workflow1 = new DataApprovalWorkflow("workflow1", periodType, newHashSet(level1));
    workflow12 = new DataApprovalWorkflow("workflow12", periodType, newHashSet(level1, level2));
    workflow12A = new DataApprovalWorkflow("workflow12A", periodType, newHashSet(level1, level2));
    workflow12B = new DataApprovalWorkflow("workflow12B", periodType, newHashSet(level1, level2));
    workflow3 = new DataApprovalWorkflow("workflow3", periodType, newHashSet(level3));
    workflow1234 =
        new DataApprovalWorkflow(
            "workflow1234", periodType, newHashSet(level1, level2, level3, level4));
    workflow13 = new DataApprovalWorkflow("workflow13", periodType, newHashSet(level1, level3));
    workflow24 = new DataApprovalWorkflow("workflow24", periodType, newHashSet(level2, level4));
    workflow0.setUid("workflow000");
    workflow1.setUid("workflow001");
    workflow12.setUid("workflow012");
    workflow12A.setUid("workflow12A");
    workflow12B.setUid("workflow12B");
    workflow3.setUid("workflow003");
    workflow1234.setUid("workflo1234");
    workflow13.setUid("workflow013");
    workflow24.setUid("workflow024");
    dataApprovalService.addWorkflow(workflow0);
    dataApprovalService.addWorkflow(workflow1);
    dataApprovalService.addWorkflow(workflow12);
    dataApprovalService.addWorkflow(workflow12A);
    dataApprovalService.addWorkflow(workflow12B);
    dataApprovalService.addWorkflow(workflow3);
    dataApprovalService.addWorkflow(workflow1234);
    dataApprovalService.addWorkflow(workflow13);
    dataApprovalService.addWorkflow(workflow24);
    dataSetA = createDataSet('A', periodType, defaultCategoryCombo);
    dataSetB = createDataSet('B', periodType, defaultCategoryCombo);
    dataSetC = createDataSet('C', periodType, defaultCategoryCombo);
    dataSetD = createDataSet('D', periodType, defaultCategoryCombo);
    dataSetE = createDataSet('E', periodType, defaultCategoryCombo);
    dataSetF = createDataSet('F', periodType, defaultCategoryCombo);
    dataSetG = createDataSet('G', periodType, defaultCategoryCombo);
    dataSetI = createDataSet('I', periodType, defaultCategoryCombo);
    dataSetJ = createDataSet('J', periodType, defaultCategoryCombo);
    dataSetA.assignWorkflow(workflow0);
    dataSetB.assignWorkflow(workflow1);
    dataSetC.assignWorkflow(workflow12);
    dataSetD.assignWorkflow(workflow12A);
    dataSetE.assignWorkflow(workflow12B);
    dataSetF.assignWorkflow(workflow3);
    dataSetG.assignWorkflow(workflow1234);
    dataSetI.assignWorkflow(workflow13);
    dataSetJ.assignWorkflow(workflow24);
    dataSetA.addOrganisationUnit(organisationUnitA);
    dataSetA.addOrganisationUnit(organisationUnitB);
    dataSetA.addOrganisationUnit(organisationUnitC);
    dataSetA.addOrganisationUnit(organisationUnitD);
    dataSetA.addOrganisationUnit(organisationUnitE);
    dataSetA.addOrganisationUnit(organisationUnitF);
    dataSetC.addOrganisationUnit(organisationUnitA);
    dataSetC.addOrganisationUnit(organisationUnitB);
    dataSetC.addOrganisationUnit(organisationUnitC);
    dataSetC.addOrganisationUnit(organisationUnitD);
    dataSetD.addOrganisationUnit(organisationUnitA);
    dataSetD.addOrganisationUnit(organisationUnitB);
    dataSetE.addOrganisationUnit(organisationUnitA);
    dataSetE.addOrganisationUnit(organisationUnitB);
    dataSetF.addOrganisationUnit(organisationUnitC);
    dataSetF.addOrganisationUnit(organisationUnitD);
    dataSetG.addOrganisationUnit(organisationUnitA);
    dataSetG.addOrganisationUnit(organisationUnitB);
    dataSetG.addOrganisationUnit(organisationUnitC);
    dataSetG.addOrganisationUnit(organisationUnitD);
    dataSetG.addOrganisationUnit(organisationUnitE);
    dataSetG.addOrganisationUnit(organisationUnitF);
    dataSetI.addOrganisationUnit(organisationUnitA);
    dataSetI.addOrganisationUnit(organisationUnitB);
    dataSetI.addOrganisationUnit(organisationUnitC);
    dataSetI.addOrganisationUnit(organisationUnitD);
    dataSetI.addOrganisationUnit(organisationUnitE);
    dataSetI.addOrganisationUnit(organisationUnitF);
    dataSetJ.addOrganisationUnit(organisationUnitA);
    dataSetJ.addOrganisationUnit(organisationUnitB);
    dataSetJ.addOrganisationUnit(organisationUnitC);
    dataSetJ.addOrganisationUnit(organisationUnitD);
    dataSetJ.addOrganisationUnit(organisationUnitE);
    dataSetJ.addOrganisationUnit(organisationUnitF);
    dataSetService.addDataSet(dataSetA);
    dataSetService.addDataSet(dataSetB);
    dataSetService.addDataSet(dataSetC);
    dataSetService.addDataSet(dataSetD);
    dataSetService.addDataSet(dataSetE);
    dataSetService.addDataSet(dataSetF);
    dataSetService.addDataSet(dataSetG);
    dataSetService.addDataSet(dataSetI);
    dataSetService.addDataSet(dataSetJ);
    userA = createUser('A');
    userB = createUser('B');
    userService.addUser(userA);
    userService.addUser(userB);
  }

  @Override
  public void tearDownTest() {
    DataApprovalPermissionsEvaluator.invalidateCache();
  }

  // -------------------------------------------------------------------------
  // Categories supportive methods
  // -------------------------------------------------------------------------
  private void setUpCategories() {
    optionA = new CategoryOption("CategoryOptionA");
    optionB = new CategoryOption("CategoryOptionB");
    optionC = new CategoryOption("CategoryOptionC");
    optionD = new CategoryOption("CategoryOptionD");
    optionE = new CategoryOption("CategoryOptionE");
    optionF = new CategoryOption("CategoryOptionF");
    optionG = new CategoryOption("CategoryOptionG");
    optionH = new CategoryOption("CategoryOptionH");
    categoryService.addCategoryOption(optionA);
    categoryService.addCategoryOption(optionB);
    categoryService.addCategoryOption(optionC);
    categoryService.addCategoryOption(optionD);
    categoryService.addCategoryOption(optionE);
    categoryService.addCategoryOption(optionF);
    categoryService.addCategoryOption(optionG);
    categoryService.addCategoryOption(optionH);
    optionA.setPublicAccess(CATEGORY_OPTION_DEFAULT);
    optionB.setPublicAccess(CATEGORY_OPTION_DEFAULT);
    optionC.setPublicAccess(CATEGORY_OPTION_DEFAULT);
    optionD.setPublicAccess(CATEGORY_OPTION_DEFAULT);
    optionE.setPublicAccess(CATEGORY_OPTION_DEFAULT);
    optionF.setPublicAccess(CATEGORY_OPTION_DEFAULT);
    optionG.setPublicAccess(CATEGORY_OPTION_DEFAULT);
    optionH.setPublicAccess(CATEGORY_OPTION_DEFAULT);
    categoryService.updateCategoryOption(optionA);
    categoryService.updateCategoryOption(optionB);
    categoryService.updateCategoryOption(optionC);
    categoryService.updateCategoryOption(optionD);
    categoryService.updateCategoryOption(optionE);
    categoryService.updateCategoryOption(optionF);
    categoryService.updateCategoryOption(optionG);
    categoryService.updateCategoryOption(optionH);
    categoryA = createCategory('A', optionA, optionB, optionC, optionD);
    categoryB = createCategory('B', optionE, optionF, optionG, optionH);
    categoryService.addCategory(categoryA);
    categoryService.addCategory(categoryB);
    categoryComboA = createCategoryCombo('A', categoryA, categoryB);
    categoryService.addCategoryCombo(categoryComboA);
    optionComboAE = createCategoryOptionCombo(categoryComboA, optionA, optionE);
    optionComboAF = createCategoryOptionCombo(categoryComboA, optionA, optionF);
    optionComboAG = createCategoryOptionCombo(categoryComboA, optionA, optionG);
    optionComboAH = createCategoryOptionCombo(categoryComboA, optionA, optionH);
    optionComboBE = createCategoryOptionCombo(categoryComboA, optionB, optionE);
    optionComboBF = createCategoryOptionCombo(categoryComboA, optionB, optionF);
    optionComboBG = createCategoryOptionCombo(categoryComboA, optionB, optionG);
    optionComboBH = createCategoryOptionCombo(categoryComboA, optionB, optionH);
    optionComboCE = createCategoryOptionCombo(categoryComboA, optionC, optionE);
    optionComboCF = createCategoryOptionCombo(categoryComboA, optionC, optionF);
    optionComboCG = createCategoryOptionCombo(categoryComboA, optionC, optionG);
    optionComboCH = createCategoryOptionCombo(categoryComboA, optionC, optionH);
    optionComboDE = createCategoryOptionCombo(categoryComboA, optionD, optionE);
    optionComboDF = createCategoryOptionCombo(categoryComboA, optionD, optionF);
    optionComboDG = createCategoryOptionCombo(categoryComboA, optionD, optionG);
    optionComboDH = createCategoryOptionCombo(categoryComboA, optionD, optionH);
    optionComboAE.setUid("optionComAE");
    optionComboAF.setUid("optionComAF");
    optionComboAG.setUid("optionComAG");
    optionComboAH.setUid("optionComAH");
    optionComboBE.setUid("optionComBE");
    optionComboBF.setUid("optionComBF");
    optionComboBG.setUid("optionComBG");
    optionComboBH.setUid("optionComBH");
    optionComboCE.setUid("optionComCE");
    optionComboCF.setUid("optionComCF");
    optionComboCG.setUid("optionComCG");
    optionComboCH.setUid("optionComCH");
    optionComboDE.setUid("optionComDE");
    optionComboDF.setUid("optionComDF");
    optionComboDG.setUid("optionComDG");
    optionComboDH.setUid("optionComDH");
    categoryService.addCategoryOptionCombo(optionComboAE);
    categoryService.addCategoryOptionCombo(optionComboAF);
    categoryService.addCategoryOptionCombo(optionComboAG);
    categoryService.addCategoryOptionCombo(optionComboAH);
    categoryService.addCategoryOptionCombo(optionComboBE);
    categoryService.addCategoryOptionCombo(optionComboBF);
    categoryService.addCategoryOptionCombo(optionComboBG);
    categoryService.addCategoryOptionCombo(optionComboBH);
    categoryService.addCategoryOptionCombo(optionComboCE);
    categoryService.addCategoryOptionCombo(optionComboCF);
    categoryService.addCategoryOptionCombo(optionComboCG);
    categoryService.addCategoryOptionCombo(optionComboCH);
    categoryService.addCategoryOptionCombo(optionComboDE);
    categoryService.addCategoryOptionCombo(optionComboDF);
    categoryService.addCategoryOptionCombo(optionComboDG);
    categoryService.addCategoryOptionCombo(optionComboDH);
    categoryComboA.getOptionCombos().add(optionComboAE);
    categoryComboA.getOptionCombos().add(optionComboAF);
    categoryComboA.getOptionCombos().add(optionComboAG);
    categoryComboA.getOptionCombos().add(optionComboAH);
    categoryComboA.getOptionCombos().add(optionComboBE);
    categoryComboA.getOptionCombos().add(optionComboBF);
    categoryComboA.getOptionCombos().add(optionComboBG);
    categoryComboA.getOptionCombos().add(optionComboBH);
    categoryComboA.getOptionCombos().add(optionComboCF);
    categoryComboA.getOptionCombos().add(optionComboCG);
    categoryComboA.getOptionCombos().add(optionComboCH);
    categoryComboA.getOptionCombos().add(optionComboDE);
    categoryComboA.getOptionCombos().add(optionComboDF);
    categoryComboA.getOptionCombos().add(optionComboDG);
    categoryComboA.getOptionCombos().add(optionComboDH);
    categoryService.updateCategoryCombo(categoryComboA);
    groupAB = createCategoryOptionGroup('A', optionA, optionB);
    groupCD = createCategoryOptionGroup('C', optionC, optionD);
    groupEF = createCategoryOptionGroup('E', optionE, optionF);
    groupGH = createCategoryOptionGroup('G', optionG, optionH);
    categoryService.saveCategoryOptionGroup(groupAB);
    categoryService.saveCategoryOptionGroup(groupCD);
    categoryService.saveCategoryOptionGroup(groupEF);
    categoryService.saveCategoryOptionGroup(groupGH);
    groupSetABCD = new CategoryOptionGroupSet("GroupSetABCD");
    groupSetEFGH = new CategoryOptionGroupSet("GroupSetEFGH");
    categoryService.saveCategoryOptionGroupSet(groupSetABCD);
    categoryService.saveCategoryOptionGroupSet(groupSetEFGH);
    groupSetABCD.addCategoryOptionGroup(groupAB);
    groupSetABCD.addCategoryOptionGroup(groupCD);
    groupSetEFGH.addCategoryOptionGroup(groupAB);
    groupSetEFGH.addCategoryOptionGroup(groupEF);
    groupAB.getGroupSets().add(groupSetABCD);
    groupCD.getGroupSets().add(groupSetABCD);
    groupEF.getGroupSets().add(groupSetEFGH);
    groupGH.getGroupSets().add(groupSetEFGH);
    level2ABCD = new DataApprovalLevel("level2ABCD", 2, groupSetABCD);
    level2EFGH = new DataApprovalLevel("level2EFGH", 2, groupSetEFGH);
    dataApprovalLevelService.addDataApprovalLevel(level2EFGH);
    dataApprovalLevelService.addDataApprovalLevel(level2ABCD);
    workflow12A_H =
        new DataApprovalWorkflow(
            "workflow12A_H", periodType, newHashSet(level1, level2, level2ABCD, level2EFGH));
    workflow12A_H.setUid("workflo12AH");
    dataApprovalService.addWorkflow(workflow12A_H);
    dataSetH = createDataSet('H', periodType, categoryComboA);
    dataSetH.assignWorkflow(workflow12A_H);
    dataSetH.addOrganisationUnit(organisationUnitA);
    dataSetH.addOrganisationUnit(organisationUnitB);
    dataSetService.addDataSet(dataSetH);
    workflow12A_H.getDataSets().add(dataSetH);
    dataApprovalService.updateWorkflow(workflow12A_H);
  }

  // -------------------------------------------------------------------------
  // Basic DataApproval
  // -------------------------------------------------------------------------
  @Test
  void testAddDuplicateDataApproval() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level2,
            workflow12,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow12,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date, // Same
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalA));
    // Redundant, so call is ignored.
    dataApprovalService.approveData(newArrayList(dataApprovalB));
  }

  @Test
  void testLastUpdatedBy() {
    User approveUserA =
        createUser("approveA", DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    approveUserA.setOrganisationUnits(singleton(organisationUnitA));
    userService.updateUser(approveUserA);
    User approveUserB =
        createUser("approveB", DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    approveUserB.setOrganisationUnits(singleton(organisationUnitA));
    userService.updateUser(approveUserB);
    injectSecurityContext(approveUserA);
    Date now = new Date();
    DataApproval da =
        new DataApproval(
            level2,
            workflow12,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            now,
            this.userA);
    dataApprovalService.approveData(singletonList(da));
    da = dataApprovalService.getDataApproval(da);
    assertEquals(da.getLastUpdatedBy(), approveUserA);
    assertFalse(da.getLastUpdated().before(now));
    dataApprovalService.unapproveData(singletonList(da));
    injectSecurityContext(approveUserB);
    Date later = new Date();
    dataApprovalService.approveData(singletonList(da));
    da = dataApprovalService.getDataApproval(da);
    assertEquals(da.getLastUpdatedBy(), approveUserB);
    assertFalse(da.getLastUpdated().before(later));
  }

  @Test
  void testAddDataApprovalWithWrongPeriodType() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    Date date = new Date();
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level2,
                  workflow12,
                  periodW,
                  organisationUnitB,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not be able to insert a weekly approval for a monthly workflow.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected error.
    }
    // Should encounter non error with the correct period type:
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level2,
                workflow12,
                periodA,
                organisationUnitB,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
  }

  @Test
  void testAddAllAndGetDataApprovalStatus() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1,
            workflow12A,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow12A,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalC =
        new DataApproval(
            level2,
            workflow12A,
            periodB,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalD =
        new DataApproval(
            level2,
            workflow12B,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    // Must
    dataApprovalService.approveData(newArrayList(dataApprovalB, dataApprovalC, dataApprovalD));
    // be
    // approved
    // before
    // A.
    dataApprovalService.approveData(newArrayList(dataApprovalA));
    DataApprovalStatus status =
        dataApprovalService.getDataApprovalStatus(
            workflow12A, periodA, organisationUnitA, defaultOptionCombo);
    assertEquals(DataApprovalState.APPROVED_HERE, status.getState());
    assertNotNull(status);
    assertEquals(organisationUnitA.getUid(), status.getOrganisationUnitUid());
    assertEquals(date, status.getCreated());
    assertEquals(userA.getId(), status.getCreator().getId());
    DataApprovalLevel level = status.getApprovedLevel();
    assertNotNull(level);
    assertEquals(level1.getName(), level.getName());
    status =
        dataApprovalService.getDataApprovalStatus(
            workflow12A, periodA, organisationUnitB, defaultOptionCombo);
    assertEquals(DataApprovalState.APPROVED_ABOVE, status.getState());
    assertNotNull(status);
    assertEquals(organisationUnitB.getUid(), status.getOrganisationUnitUid());
    assertEquals(date, status.getCreated());
    assertEquals(userA.getId(), status.getCreator().getId());
    level = status.getApprovedLevel();
    assertNotNull(level);
    assertEquals(level2.getName(), level.getName());
    status =
        dataApprovalService.getDataApprovalStatus(
            workflow12A, periodB, organisationUnitB, defaultOptionCombo);
    assertEquals(DataApprovalState.APPROVED_HERE, status.getState());
    assertNotNull(status);
    assertEquals(organisationUnitB.getUid(), status.getOrganisationUnitUid());
    assertEquals(date, status.getCreated());
    assertEquals(userA.getId(), status.getCreator().getId());
    level = status.getApprovedLevel();
    assertNotNull(level);
    assertEquals(level2.getName(), level.getName());
    status =
        dataApprovalService.getDataApprovalStatus(
            workflow12B, periodA, organisationUnitB, defaultOptionCombo);
    assertEquals(DataApprovalState.APPROVED_HERE, status.getState());
    assertNotNull(status);
    assertEquals(organisationUnitB.getUid(), status.getOrganisationUnitUid());
    assertEquals(date, status.getCreated());
    assertEquals(userA.getId(), status.getCreator().getId());
    level = status.getApprovedLevel();
    assertNotNull(level);
    assertEquals(level2.getName(), level.getName());
    status =
        dataApprovalService.getDataApprovalStatus(
            workflow12B, periodB, organisationUnitB, defaultOptionCombo);
    assertEquals(DataApprovalState.UNAPPROVED_READY, status.getState());
    level = status.getApprovedLevel();
    assertNull(level);
  }

  @Test
  void testIsApprovedTrue() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);

    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow12,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            new Date(),
            userB);
    dataApprovalService.approveData(newArrayList(dataApprovalB));

    // Get a period without periodId
    Period testPeriodA = createPeriod(periodA.getIsoDate());

    assertTrue(
        dataApprovalService.isApproved(
            workflow12, testPeriodA, organisationUnitB, defaultOptionCombo));
  }

  @Test
  void testIsApprovedFalse() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);

    // Get a period without periodId
    Period testPeriodA = createPeriod(periodA.getIsoDate());

    assertFalse(
        dataApprovalService.isApproved(
            workflow12, testPeriodA, organisationUnitB, defaultOptionCombo));
  }

  @Test
  void testIsApprovedPeriodDoesNotExist() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);

    // Get a period without periodId (and that isn't in the database)
    Period testPeriodX = createPeriod("201010");

    assertFalse(
        dataApprovalService.isApproved(
            workflow12, testPeriodX, organisationUnitB, defaultOptionCombo));
  }

  @Test
  void testDeleteDataApproval() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1,
            workflow12,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow12,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userB);
    dataApprovalService.approveData(newArrayList(dataApprovalB));
    dataApprovalService.approveData(newArrayList(dataApprovalA));
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow12, periodA, organisationUnitA, defaultOptionCombo)
            .getState()
            .isApproved());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow12, periodA, organisationUnitB, defaultOptionCombo)
            .getState()
            .isApproved());
    // Only
    dataApprovalService.unapproveData(newArrayList(dataApprovalA));
    // A
    // should
    // be
    // deleted.
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow12, periodA, organisationUnitA, defaultOptionCombo)
            .getState()
            .isApproved());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow12, periodA, organisationUnitB, defaultOptionCombo)
            .getState()
            .isApproved());
    dataApprovalService.unapproveData(newArrayList(dataApprovalB));
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow12, periodA, organisationUnitA, defaultOptionCombo)
            .getState()
            .isApproved());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow12, periodA, organisationUnitB, defaultOptionCombo)
            .getState()
            .isApproved());
  }

  @Test
  void testGetDataApprovalState() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    // No levels defined.
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow0, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow0, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow0, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow0, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow0, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow0, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    // Levels defined.
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    // Skip levels.
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    Date date = new Date();
    // Approved for organisation unit F
    DataApproval dataApprovalF =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitF,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalF));
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    // Also approved also for organisation unit E
    DataApproval dataApprovalE1 =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitE,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalE2 =
        new DataApproval(
            level3,
            workflow13,
            periodA,
            organisationUnitE,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalE1));
    dataApprovalService.approveData(newArrayList(dataApprovalE2));
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    // Also approved for organisation unit D
    DataApproval dataApprovalD =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalD));
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    // Also approved for organisation unit C
    DataApproval dataApprovalC1 =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalC2 =
        new DataApproval(
            level3,
            workflow13,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalC1));
    dataApprovalService.approveData(newArrayList(dataApprovalC2));
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    // Also approved for organisation unit B
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow1234,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalB));
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    // Also approved for organisation unit A
    DataApproval dataApprovalA1 =
        new DataApproval(
            level1,
            workflow1234,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalA2 =
        new DataApproval(
            level1,
            workflow13,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalA1));
    dataApprovalService.approveData(newArrayList(dataApprovalA2));
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow13, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
  }

  @Test
  void testGetDataApprovalStateAboveUserOrgUnitLevel() {
    transactionTemplate.execute(
        status -> {
          createUserAndInjectSecurityContext(
              singleton(organisationUnitD), false, DataApproval.AUTH_APPROVE);
          assertEquals(
              "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
              statusAndPermissions(workflow12, periodA, organisationUnitD, defaultOptionCombo));
          dataApprovalStore.addDataApproval(
              new DataApproval(
                  level2,
                  workflow12,
                  periodA,
                  organisationUnitB,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  new Date(),
                  userA));
          dbmsManager.clearSession();
          return null;
        });
    assertEquals(
        "APPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12, periodA, organisationUnitD, defaultOptionCombo));
  }

  @Test
  void testGetDataApprovalStateOrgUnitAssignments() {
    dataSetG.removeOrganisationUnit(organisationUnitC);
    dataSetG.removeOrganisationUnit(organisationUnitE);
    dataSetG.removeOrganisationUnit(organisationUnitF);
    dataSetService.updateDataSet(dataSetG);
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    Date date = new Date();
    // Approved for organisation unit D
    DataApproval dataApprovalD =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalD));
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    // Also approved for organisation unit C
    DataApproval dataApprovalC =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalC));
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVABLE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
  }

  @Test
  void testGetDataApprovalStateAbove() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    Date date = new Date();
    // Approved for organisation unit C
    DataApproval dataApprovalC =
        new DataApproval(
            level3,
            workflow3,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalService.approveData(newArrayList(dataApprovalC));
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow3, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow3, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
  }

  @Test
  void testGetDataApprovalStateWithMultipleChildren() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    Date date = new Date();
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level4,
                workflow1234,
                periodA,
                organisationUnitF,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level4,
                workflow1234,
                periodA,
                organisationUnitD,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level3,
                workflow1234,
                periodA,
                organisationUnitE,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level3,
                workflow1234,
                periodA,
                organisationUnitC,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_HERE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.APPROVED_ABOVE,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getState());
  }

  @Test
  void testGetDataApprovalStateOtherPeriodTypes() {
    switchToApprovalUser(
        organisationUnitA, DataApproval.AUTH_APPROVE, DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodB, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_WAITING,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodY, organisationUnitA, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodY, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodD, organisationUnitD, defaultOptionCombo)
            .getState());
    assertEquals(
        DataApprovalState.UNAPPROVED_READY,
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodW, organisationUnitD, defaultOptionCombo)
            .getState());
  }

  @Test
  void testMayApproveSameLevel() {
    Date date = new Date();
    transactionTemplate.execute(
        status -> {
          switchToApprovalUser(organisationUnitB, DataApproval.AUTH_APPROVE, AUTH_APPR_LEVEL);
          dbmsManager.clearSession();
          return null;
        });
    // Level 4 (organisationUnitD and organisationUnitF ready)
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitB, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_READY level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitD, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitE, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_READY level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitF, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitB, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_READY level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitD, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitE, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_READY level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitF, defaultOptionCombo));
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level4,
                  workflow1234,
                  periodA,
                  organisationUnitD,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit C.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected error, so add the data through dataApprovalStore:
    }
    transactionTemplate.execute(
        status -> {
          // Level 3 (organisationUnitC) and Level 4 (organisationUnitF) ready
          dataApprovalStore.addDataApproval(
              new DataApproval(
                  level4,
                  workflow1234,
                  periodA,
                  organisationUnitD,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA));
          dbmsManager.clearSession();
          return null;
        });
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level4,
                  workflow24,
                  periodA,
                  organisationUnitD,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit C.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected error, so add the data through dataApprovalStore:
    }
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level4,
            workflow24,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitB, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_READY level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitD, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitE, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_READY level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitF, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitB, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitD, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitE, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_READY level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitF, defaultOptionCombo));
    // Level 2 (organisationUnitB) ready
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level4,
                  workflow1234,
                  periodA,
                  organisationUnitF,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit F.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected error, so add the data through dataApprovalStore:
    }
    transactionTemplate.execute(
        status -> {
          dataApprovalStore.addDataApproval(
              new DataApproval(
                  level4,
                  workflow1234,
                  periodA,
                  organisationUnitF,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA));
          dbmsManager.clearSession();
          return null;
        });
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level4,
                  workflow24,
                  periodA,
                  organisationUnitF,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit F.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected error, so add the data through dataApprovalStore:
    }
    transactionTemplate.execute(
        status -> {
          dataApprovalStore.addDataApproval(
              new DataApproval(
                  level4,
                  workflow24,
                  periodA,
                  organisationUnitF,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA));
          dbmsManager.clearSession();
          return null;
        });
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level3,
                  workflow1234,
                  periodA,
                  organisationUnitE,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit E.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected error, so add the data through dataApprovalStore:
    }
    transactionTemplate.execute(
        status -> {
          dataApprovalStore.addDataApproval(
              new DataApproval(
                  level3,
                  workflow1234,
                  periodA,
                  organisationUnitE,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA));
          dbmsManager.clearSession();
          return null;
        });
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level3,
                  workflow1234,
                  periodA,
                  organisationUnitC,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit C.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected error, so add the data through dataApprovalStore:
    }
    transactionTemplate.execute(
        status -> {
          dataApprovalStore.addDataApproval(
              new DataApproval(
                  level3,
                  workflow1234,
                  periodA,
                  organisationUnitC,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA));
          dbmsManager.clearSession();
          return null;
        });
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitB, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level3 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=level4 approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitD, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level3 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitE, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=level4 approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitF, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitB, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitD, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitE, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitF, defaultOptionCombo));
    // Level 1 (organisationUnitA) ready
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level2,
                workflow1234,
                periodA,
                organisationUnitB,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level2,
                workflow24,
                periodA,
                organisationUnitB,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertEquals(
        "APPROVED_HERE level=level2 approve=F unapprove=T accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitB, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=level3 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=level4 approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitD, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=level3 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitE, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=level4 approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitF, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level2 approve=F unapprove=T accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitB, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitD, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitE, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow24, periodA, organisationUnitF, defaultOptionCombo));
    // Level 1 (organisationUnitA) try to approve
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level1,
                  workflow1234,
                  periodA,
                  organisationUnitA,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit A.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected
    }
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level1,
                  workflow24,
                  periodA,
                  organisationUnitA,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit A.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected
    }
  }

  @Test
  void testMayApproveLowerLevels() {
    switchToApprovalUser(
        organisationUnitB, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL);
    Date date = new Date();
    // Level 4 (organisationUnitD and organisationUnitF ready)
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    // Level 3 (organisationUnitC) and Level 4 (organisationUnitF) ready
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level4,
                workflow1234,
                periodA,
                organisationUnitD,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    // Level 2 (organisationUnitB) ready
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level4,
                workflow1234,
                periodA,
                organisationUnitF,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level3,
                workflow1234,
                periodA,
                organisationUnitE,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level3,
                workflow1234,
                periodA,
                organisationUnitC,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    // Level 1 (organisationUnitA) ready
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level2,
                  workflow1234,
                  periodA,
                  organisationUnitB,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit B.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected error, so add the data through dataApprovalStore:
    }
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level2,
            workflow1234,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    // Level 1 (organisationUnitA) try to approve
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level1,
                  workflow1234,
                  periodA,
                  organisationUnitA,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit A.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected
    }
  }

  @Test
  void testMayApproveSameAndLowerLevels() {
    createUserAndInjectSecurityContext(
        singleton(organisationUnitB),
        false,
        DataApproval.AUTH_APPROVE,
        DataApproval.AUTH_APPROVE_LOWER_LEVELS,
        AUTH_APPR_LEVEL);
    Date date = new Date();
    // Level 4 (organisationUnitD and organisationUnitF ready)
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    // Level 3 (organisationUnitC) and Level 4 (organisationUnitF) ready
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level4,
                workflow1234,
                periodA,
                organisationUnitD,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    // Level 2 (organisationUnitB) ready
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level4,
                workflow1234,
                periodA,
                organisationUnitF,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level3,
                workflow1234,
                periodA,
                organisationUnitE,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level3,
                workflow1234,
                periodA,
                organisationUnitC,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    // Level 1 (organisationUnitA) ready
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level2,
                workflow1234,
                periodA,
                organisationUnitB,
                defaultOptionCombo,
                NOT_ACCEPTED,
                date,
                userA)));
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    // Level 1 (organisationUnitA) try to approve
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level1,
                  workflow1234,
                  periodA,
                  organisationUnitA,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("User should not have permission to approve org unit A.");
    } catch (DataMayNotBeApprovedException ex) {
      // Expected
    }
  }

  @Test
  void testMayApproveNoAuthority() {
    createUserAndInjectSecurityContext(singleton(organisationUnitB), false, AUTH_APPR_LEVEL);
    Date date = new Date();
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitE,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level2,
            workflow1234,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayApprove());
  }

  @Test
  void testMayUnapproveSameLevel() {
    switchToApprovalUser(organisationUnitB, DataApproval.AUTH_APPROVE, AUTH_APPR_LEVEL);
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1,
            workflow1234,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow1234,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalC =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalD =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalE =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitE,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalF =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitF,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalStore.addDataApproval(dataApprovalD);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalF);
    dataApprovalStore.addDataApproval(dataApprovalE);
    dataApprovalStore.addDataApproval(dataApprovalC);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalB);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalA);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
  }

  @Test
  void testMayUnapproveLowerLevels() {
    switchToApprovalUser(
        organisationUnitB, DataApproval.AUTH_APPROVE_LOWER_LEVELS, AUTH_APPR_LEVEL);
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1,
            workflow1234,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow1234,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalC =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalD =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalE =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitE,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalF =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitF,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalStore.addDataApproval(dataApprovalD);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalF);
    dataApprovalStore.addDataApproval(dataApprovalE);
    dataApprovalStore.addDataApproval(dataApprovalC);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalB);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalA);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
  }

  @Test
  void testMayUnapproveWithAcceptAuthority() {
    switchToApprovalUser(organisationUnitB, DataApproval.AUTH_ACCEPT_LOWER_LEVELS, AUTH_APPR_LEVEL);
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1,
            workflow1234,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow1234,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalC =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalD =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalE =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitE,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalF =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitF,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalStore.addDataApproval(dataApprovalD);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalF);
    dataApprovalStore.addDataApproval(dataApprovalE);
    dataApprovalStore.addDataApproval(dataApprovalC);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertTrue(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalB);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalA);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
  }

  @Test
  void testMayUnapproveNoAuthority() {
    createUserAndInjectSecurityContext(singleton(organisationUnitB), false, AUTH_APPR_LEVEL);
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1,
            workflow1234,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow1234,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalC =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalD =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalE =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitE,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalF =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitF,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    dataApprovalStore.addDataApproval(dataApprovalD);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalF);
    dataApprovalStore.addDataApproval(dataApprovalE);
    dataApprovalStore.addDataApproval(dataApprovalC);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalB);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    dataApprovalStore.addDataApproval(dataApprovalA);
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitA, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitB, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitC, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitD, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitE, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
    assertFalse(
        dataApprovalService
            .getDataApprovalStatus(workflow1234, periodA, organisationUnitF, defaultOptionCombo)
            .getPermissions()
            .isMayUnapprove());
  }

  @Test
  void testGetDataApprovalStatuses() {
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1,
            workflow1234,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow1234,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalC =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalD =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalE =
        new DataApproval(
            level3,
            workflow1234,
            periodA,
            organisationUnitE,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalF =
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitF,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    transactionTemplate.execute(
        status -> {
          createUserAndInjectSecurityContext(singleton(organisationUnitB), false, AUTH_APPR_LEVEL);
          dataApprovalStore.addDataApproval(dataApprovalD);
          dataApprovalStore.addDataApproval(dataApprovalF);
          dataApprovalStore.addDataApproval(dataApprovalE);
          dbmsManager.flushSession();
          return null;
        });
    List<DataApproval> approvals =
        newArrayList(
            dataApprovalA,
            dataApprovalB,
            dataApprovalC,
            dataApprovalD,
            dataApprovalE,
            dataApprovalF);
    Map<DataApproval, DataApprovalStatus> map =
        dataApprovalService.getDataApprovalStatuses(approvals);
    assertEquals("null", statusString(map.get(dataApprovalA)));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusString(map.get(dataApprovalB)));
    assertEquals(
        "UNAPPROVED_READY level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusString(map.get(dataApprovalC)));
    assertEquals(
        "APPROVED_HERE level=level4 approve=F unapprove=F accept=F unaccept=F read=T",
        statusString(map.get(dataApprovalD)));
    assertEquals(
        "APPROVED_HERE level=level3 approve=F unapprove=F accept=F unaccept=F read=T",
        statusString(map.get(dataApprovalE)));
    assertEquals(
        "APPROVED_ABOVE level=level4 approve=F unapprove=F accept=F unaccept=F read=T",
        statusString(map.get(dataApprovalF)));
  }

  // -------------------------------------------------------------------------
  // Test with Categories
  // -------------------------------------------------------------------------
  @Test
  void testApprovalsWithCategories() {
    Date date = new Date();
    transactionTemplate.execute(
        status -> {
          systemSettingManager.saveSystemSetting(SettingKey.ACCEPTANCE_REQUIRED_FOR_APPROVAL, true);
          setUpCategories();
          createUserAndInjectSecurityContext(
              singleton(organisationUnitA),
              false,
              DataApproval.AUTH_APPROVE,
              DataApproval.AUTH_APPROVE_LOWER_LEVELS,
              DataApproval.AUTH_ACCEPT_LOWER_LEVELS,
              AUTH_APPR_LEVEL);
          dbmsManager.flushSession();
          return null;
        });
    // Category A -> Options A,B,C,D
    // Category B -> Options E,F,G,H
    //
    // Option combo A -> Options A,E -> Groups A,C
    // Option combo B -> Options A,F -> Groups A,C
    // Option combo C -> Options A,G -> Groups A,D
    // Option combo D -> Options A,H -> Groups A,D
    // Option combo E -> Options B,E -> Groups B,C
    // Option combo F -> Options B,F -> Groups B,C
    // Option combo G -> Options B,G -> Groups B,D
    // Option combo H -> Options B,H -> Groups B,D
    // Option combo I -> Options C,E -> Groups B,D
    // Option combo J -> Options C,F -> Groups B,D
    // Option combo K -> Options C,G -> Groups B,D
    List<DataApprovalLevel> levels = dataApprovalLevelService.getAllDataApprovalLevels();
    assertEquals(6, levels.size());
    assertEquals("level1", levels.get(0).getName());
    assertEquals("level2", levels.get(1).getName());
    assertEquals("level2ABCD", levels.get(2).getName());
    assertEquals("level2EFGH", levels.get(3).getName());
    assertEquals("level3", levels.get(4).getName());
    assertEquals("level4", levels.get(5).getName());
    levels = workflow12A_H.getSortedLevels();
    assertEquals(4, levels.size());
    assertEquals("level1", levels.get(0).getName());
    assertEquals("level2", levels.get(1).getName());
    assertEquals("level2ABCD", levels.get(2).getName());
    assertEquals("level2EFGH", levels.get(3).getName());
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, null));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, null));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAE));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAE));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAF));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAF));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAG));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAG));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboCE));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboCE));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboCF));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboCF));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboCG));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboCG));
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level1,
                  workflow12A_H,
                  periodA,
                  organisationUnitA,
                  optionComboAE,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("organisationUnitB should not be ready for approval at level 1.");
    } catch (DataMayNotBeApprovedException ex) {
      // Can't add this approval before approving at a lower level.
    }
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level2,
                  workflow12A_H,
                  periodA,
                  organisationUnitA,
                  optionComboAE,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("organisationUnitB should not be ready for approval at level 2.");
    } catch (DataMayNotBeApprovedException ex) {
      // Can't add this approval before approving at a lower level.
    }
    try {
      dataApprovalService.approveData(
          newArrayList(
              new DataApproval(
                  level2ABCD,
                  workflow12A_H,
                  periodA,
                  organisationUnitA,
                  optionComboAE,
                  NOT_ACCEPTED,
                  date,
                  userA)));
      fail("organisationUnitB should not be ready for approval at level 2ABCD.");
    } catch (DataMayNotBeApprovedException ex) {
      // Can't add this approval before approving at a lower level.
    }
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level2EFGH,
                workflow12A_H,
                periodA,
                organisationUnitB,
                optionComboAE,
                NOT_ACCEPTED,
                date,
                userA)));
    // assertEquals( "UNAPPROVED_WAITING level=null approve=F unapprove=F
    // accept=F unaccept=F read=T",
    // statusAndPermissions( workflow12A_H, periodA, organisationUnitA, null
    // ) );
    // assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F
    // accept=F unaccept=F read=T",
    // statusAndPermissions( workflow12A_H, periodA, organisationUnitB, null
    // ) );
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAE));
    assertEquals(
        "APPROVED_HERE level=level2EFGH approve=F unapprove=T accept=T unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAE));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAF));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAF));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAG));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAG));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboCE));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboCE));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboCF));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboCF));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboCG));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboCG));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level2EFGH,
                workflow12A_H,
                periodA,
                organisationUnitB,
                optionComboAF,
                NOT_ACCEPTED,
                date,
                userA)));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level2EFGH,
                workflow12A_H,
                periodA,
                organisationUnitB,
                optionComboCG,
                NOT_ACCEPTED,
                date,
                userA)));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, null));
    // assertEquals( "UNAPPROVED_READY level=null approve=T unapprove=F
    // accept=F unaccept=F read=T", statusAndPermissions( workflow12A_H,
    // periodA, organisationUnitB, null ) );
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAE));
    assertEquals(
        "APPROVED_HERE level=level2EFGH approve=F unapprove=T accept=T unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAE));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAF));
    assertEquals(
        "APPROVED_HERE level=level2EFGH approve=F unapprove=T accept=T unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAF));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAG));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAG));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboCE));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboCE));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboCF));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboCF));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboCG));
    assertEquals(
        "APPROVED_HERE level=level2EFGH approve=F unapprove=T accept=T unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboCG));
    dataApprovalService.acceptData(
        newArrayList(
            new DataApproval(
                level2EFGH,
                workflow12A_H,
                periodA,
                organisationUnitB,
                optionComboAF,
                ACCEPTED,
                date,
                userA)));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level2ABCD,
                workflow12A_H,
                periodA,
                organisationUnitB,
                optionComboAF,
                NOT_ACCEPTED,
                date,
                userA)));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAF));
    assertEquals(
        "APPROVED_HERE level=level2ABCD approve=F unapprove=T accept=T unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAF));
    dataApprovalService.acceptData(
        newArrayList(
            new DataApproval(
                level2ABCD,
                workflow12A_H,
                periodA,
                organisationUnitB,
                optionComboAF,
                ACCEPTED,
                date,
                userA)));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level2,
                workflow12A_H,
                periodA,
                organisationUnitB,
                optionComboAF,
                NOT_ACCEPTED,
                date,
                userA)));
    dataApprovalService.acceptData(
        newArrayList(
            new DataApproval(
                level2,
                workflow12A_H,
                periodA,
                organisationUnitB,
                optionComboAF,
                ACCEPTED,
                date,
                userA)));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAF));
    assertEquals(
        "ACCEPTED_HERE level=level2 approve=T unapprove=T accept=F unaccept=T read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAF));
    dataApprovalService.approveData(
        newArrayList(
            new DataApproval(
                level1,
                workflow12A_H,
                periodA,
                organisationUnitA,
                optionComboAF,
                NOT_ACCEPTED,
                date,
                userA)));
    assertEquals(
        "APPROVED_HERE level=level1 approve=F unapprove=T accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitA, optionComboAF));
    assertEquals(
        "APPROVED_ABOVE level=level2 approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12A_H, periodA, organisationUnitB, optionComboAF));
  }

  @Test
  void testWorkflows() {
    createUserAndInjectSecurityContext(
        singleton(organisationUnitB),
        false,
        AUTH_APPR_LEVEL,
        DataApproval.AUTH_APPROVE,
        DataApproval.AUTH_APPROVE_LOWER_LEVELS);
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_READY level=null approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow3, periodA, organisationUnitC, defaultOptionCombo));
    Date date = new Date();
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitD,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level4,
            workflow1234,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level2,
            workflow12,
            periodA,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level3,
            workflow3,
            periodA,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=T accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_ABOVE level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow12, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level3 approve=F unapprove=T accept=F unaccept=F read=T",
        statusAndPermissions(workflow3, periodA, organisationUnitC, defaultOptionCombo));
  }

  @Test
  void testPeriodsEndingDuringWorkflowApproval() {
    Date date = new Date();
    transactionTemplate.execute(
        status -> {
          switchToApprovalUser(organisationUnitC, AUTH_APPR_LEVEL, DataApproval.AUTH_APPROVE);
          dataApprovalStore.addDataApproval(
              new DataApproval(
                  level4,
                  workflow1234,
                  periodA,
                  organisationUnitC,
                  defaultOptionCombo,
                  NOT_ACCEPTED,
                  date,
                  userA));
          dbmsManager.flushSession();
          return null;
        });
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodW, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodW4, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodB, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodW5, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodY, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodC, organisationUnitC, defaultOptionCombo));
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level4,
            workflow1234,
            periodB,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodW, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodW4, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodB, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodW5, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodC, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodY, organisationUnitC, defaultOptionCombo));
    dataApprovalStore.addDataApproval(
        new DataApproval(
            level4,
            workflow1234,
            periodM12,
            organisationUnitC,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodA, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodW, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodW4, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodB, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodW5, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "APPROVED_HERE level=level4 approve=T unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodY, organisationUnitC, defaultOptionCombo));
    assertEquals(
        "UNAPPROVED_WAITING level=null approve=F unapprove=F accept=F unaccept=F read=T",
        statusAndPermissions(workflow1234, periodC, organisationUnitC, defaultOptionCombo));
  }

  @Test
  void testGetApprovedByOfAcceptedHere() {
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1,
            workflow1234,
            periodA,
            organisationUnitA,
            defaultOptionCombo,
            ACCEPTED,
            date,
            userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2,
            workflow1234,
            periodB,
            organisationUnitB,
            defaultOptionCombo,
            NOT_ACCEPTED,
            date,
            userA);
    transactionTemplate.execute(
        status -> {
          systemSettingManager.saveSystemSetting(SettingKey.ACCEPTANCE_REQUIRED_FOR_APPROVAL, true);
          createUserAndInjectSecurityContext(singleton(organisationUnitA), false, AUTH_APPR_LEVEL);
          dataApprovalStore.addDataApproval(dataApprovalA);
          dataApprovalStore.addDataApproval(dataApprovalB);
          dbmsManager.flushSession();
          return null;
        });
    DataApprovalStatus status =
        dataApprovalService.getDataApprovalStatus(
            workflow1234, periodA, organisationUnitA, defaultOptionCombo);
    assertEquals(level1, status.getApprovedLevel());
    assertEquals(organisationUnitA.getUid(), status.getOrganisationUnitUid());
    assertEquals(DataApprovalState.ACCEPTED_HERE, status.getState());
    assertEquals(false, status.getPermissions().isMayAccept());
    assertEquals(userA.getName(), status.getPermissions().getApprovedBy());
    assertEquals(date, status.getPermissions().getApprovedAt());
  }

  /**
   * Returns approval status and permissions information as a string. This allows a test to compare
   * the result against a string and test several things at once. More importantly, it shows in the
   * log all of the ways in which the test status and permissions differs from expected, instead of
   * showing only one different value. This can save time in understanding the difference between
   * the expected value and the test result.
   *
   * @param workflow Approval workflow
   * @param period Approval period
   * @param organisationUnit Approval orgaisation unit
   * @param attributeOptionCombo Approval attribute option combination
   * @return A String representing the state, level, and allowed user actions
   */
  private String statusAndPermissions(
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo attributeOptionCombo) {
    DataApprovalStatus status =
        dataApprovalService.getDataApprovalStatus(
            workflow, period, organisationUnit, attributeOptionCombo);
    return statusString(status);
  }

  /**
   * Returns approval status as a string.
   *
   * @param status Approval status
   * @return A string representing the state, level, and allowed user actions
   */
  private String statusString(DataApprovalStatus status) {
    if (status == null) {
      return "null";
    }
    DataApprovalPermissions permissions = status.getPermissions();
    return status.getState().toString()
        + " level="
        + (status.getApprovedLevel() == null ? "null" : status.getApprovedLevel().getName())
        + " approve="
        + (permissions.isMayApprove() ? "T" : "F")
        + " unapprove="
        + (permissions.isMayUnapprove() ? "T" : "F")
        + " accept="
        + (permissions.isMayAccept() ? "T" : "F")
        + " unaccept="
        + (permissions.isMayUnaccept() ? "T" : "F")
        + " read="
        + (permissions.isMayReadData() ? "T" : "F");
  }

  private User switchToApprovalUser(
      OrganisationUnit unit, String authApprove, String authApproveLowerLevels) {
    return createUserAndInjectSecurityContext(
        singleton(unit), false, authApprove, authApproveLowerLevels);
  }
}
