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

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.dataapproval.DataApproval.AUTH_ACCEPT_LOWER_LEVELS;
import static org.hisp.dhis.dataapproval.DataApproval.AUTH_APPROVE;
import static org.hisp.dhis.dataapproval.DataApproval.AUTH_APPROVE_LOWER_LEVELS;
import static org.hisp.dhis.dataapproval.DataApproval.AUTH_VIEW_UNAPPROVED_DATA;
import static org.hisp.dhis.user.UserRole.AUTHORITY_ALL;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.exceptions.DataApprovalException;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccessService;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
class DataApprovalServiceCategoryOptionGroupTest extends IntegrationTestBase {
  private static final String ACCESS_NONE = "--------";

  private static final String ACCESS_READ = "r-------";

  @Autowired private DataApprovalService dataApprovalService;

  @Autowired private DataApprovalLevelService dataApprovalLevelService;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired protected IdentifiableObjectManager identifiableObjectManager;

  @Autowired private SystemSettingManager systemSettingManager;

  @Autowired protected UserGroupAccessService userGroupAccessService;

  @Autowired protected UserGroupService userGroupService;

  @Autowired protected UserService _userService;

  @Autowired protected DataSetService dataSetService;

  // -------------------------------------------------------------------------
  // Supporting data
  // -------------------------------------------------------------------------
  private OrganisationUnit global;

  private OrganisationUnit americas;

  private OrganisationUnit asia;

  private OrganisationUnit brazil;

  private OrganisationUnit china;

  private OrganisationUnit india;

  private User userA;

  private Date dateA;

  private User superUser;

  private User globalConsultant;

  private User globalUser;

  private User globalApproveOnly;

  private User globalAcceptOnly;

  private User globalReadAll;

  private User globalAgencyAUser;

  private User globalAgencyBUser;

  private User brazilInteragencyUser;

  private User chinaInteragencyUser;

  private User chinaInteragencyApproveOnly;

  private User chinalInteragencyAcceptOnly;

  private User indiaInteragencyUser;

  private User brazilAgencyAUser;

  private User chinaAgencyAUser;

  private User chinaAgencyAApproveOnly;

  private User chinaAgencyAAcceptOnly;

  private User chinaAgencyBUser;

  private User indiaAgencyAUser;

  private User brazilPartner1User;

  private User chinaPartner1User;

  private User chinaPartner2User;

  private User indiaPartner1User;

  private CategoryOption brazilA1;

  private CategoryOption chinaA1_1;

  private CategoryOption chinaA1_2;

  private CategoryOption chinaA2;

  private CategoryOption chinaB2;

  private CategoryOption indiaA1;

  private CategoryOption worldwide;

  private org.hisp.dhis.category.Category mechanismCategory;

  private CategoryCombo mechanismCategoryCombo;

  private CategoryOptionCombo brazilA1Combo;

  private CategoryOptionCombo chinaA1_1Combo;

  private CategoryOptionCombo chinaA1_2Combo;

  private CategoryOptionCombo chinaA2Combo;

  private CategoryOptionCombo chinaB2Combo;

  private CategoryOptionCombo indiaA1Combo;

  private CategoryOptionCombo worldwideCombo;

  private CategoryOptionGroup agencyA;

  private CategoryOptionGroup agencyB;

  private CategoryOptionGroup partner1;

  private CategoryOptionGroup partner2;

  private CategoryOptionGroupSet agencies;

  private CategoryOptionGroupSet partners;

  private DataApprovalLevel globalLevel1;

  private DataApprovalLevel globalAgencyLevel2;

  private DataApprovalLevel countryLevel3;

  private DataApprovalLevel agencyLevel4;

  private DataApprovalLevel partnerLevel5;

  private DataApprovalWorkflow workflow1;

  private DataApprovalWorkflow workflow2;

  private PeriodType periodType;

  private Period periodA;

  private DataSet dataSetA;

  private DataSet dataSetB;

  // -------------------------------------------------------------------------
  // Set up/tear down helper methods
  // -------------------------------------------------------------------------

  private UserGroup getUserGroup(String userGroupName, Set<User> users) {
    UserGroup userGroup = new UserGroup();
    userGroup.setAutoFields();
    userGroup.setName(userGroupName);
    userGroup.setMembers(users);
    userGroupService.addUserGroup(userGroup);
    for (User user : users) {
      user.getGroups().add(userGroup);
      userService.updateUser(user);
    }
    return userGroup;
  }

  private Set<User> userSet(User... users) {
    return new HashSet<>(Arrays.asList(users));
  }

  private void setAccess(BaseIdentifiableObject object, UserGroup... userGroups) {
    object.getSharing().setPublicAccess(ACCESS_NONE);
    // Needed for sharing to work
    object.getSharing().setOwner(userA);
    object.setUser(userA);
    object.getSharing().resetUserGroupAccesses();
    object.getSharing().resetUserAccesses();
    for (UserGroup group : userGroups) {
      object.getSharing().addUserGroupAccess(new UserGroupAccess(group, ACCESS_READ));
    }
    identifiableObjectManager.updateNoAcl(object);
  }

  private void constrainByMechanism(User user) {
    user.getCatDimensionConstraints().add(mechanismCategory);
  }

  // -------------------------------------------------------------------------
  // Set up/tear down
  // -------------------------------------------------------------------------
  @Override
  public void setUpTest() throws Exception {
    userService = _userService;

    // ---------------------------------------------------------------------
    // Add supporting data
    // ---------------------------------------------------------------------

    // Organisation units
    global = createOrganisationUnit("Global");
    americas = createOrganisationUnit("Americas", global);
    asia = createOrganisationUnit("Asia", global);
    brazil = createOrganisationUnit("Brazil", americas);
    china = createOrganisationUnit("China", asia);
    india = createOrganisationUnit("India", asia);
    organisationUnitService.addOrganisationUnit(global);
    organisationUnitService.addOrganisationUnit(americas);
    organisationUnitService.addOrganisationUnit(asia);
    organisationUnitService.addOrganisationUnit(brazil);
    organisationUnitService.addOrganisationUnit(china);
    organisationUnitService.addOrganisationUnit(india);

    // Users
    userA = makeUser("A");
    userService.addUser(userA);
    dateA = new Date();
    superUser = createAndAddUser(true, "SuperUser", global, AUTHORITY_ALL);
    globalConsultant =
        createAndAddUser(
            "GlobalConsultant",
            global,
            AUTH_APPROVE,
            AUTH_ACCEPT_LOWER_LEVELS,
            AUTH_APPROVE_LOWER_LEVELS);
    globalUser = createAndAddUser("GlobalUser", global, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    globalApproveOnly = createAndAddUser("GlobalApproveOnly", global, AUTH_APPROVE);
    globalAcceptOnly = createAndAddUser("GlobalAcceptOnly", global, AUTH_ACCEPT_LOWER_LEVELS);
    globalReadAll = createAndAddUser("GlobalReadEverything", global, AUTH_VIEW_UNAPPROVED_DATA);
    globalAgencyAUser =
        createAndAddUser("GlobalAgencyAUser", global, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    globalAgencyBUser =
        createAndAddUser("GlobalAgencyBUser", global, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    brazilInteragencyUser =
        createAndAddUser("BrazilInteragencyUser", brazil, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    chinaInteragencyUser =
        createAndAddUser("ChinaInteragencyUser", china, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    chinaInteragencyApproveOnly =
        createAndAddUser("ChinaInteragencyApproveOnly", china, AUTH_APPROVE);
    chinalInteragencyAcceptOnly =
        createAndAddUser("ChinalInteragencyAcceptOnly", china, AUTH_ACCEPT_LOWER_LEVELS);
    indiaInteragencyUser =
        createAndAddUser("IndiaInteragencyUser", india, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    brazilAgencyAUser =
        createAndAddUser("BrazilAgencyAUser", brazil, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    chinaAgencyAUser =
        createAndAddUser("ChinaAgencyAUser", china, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    chinaAgencyAApproveOnly = createAndAddUser("ChinaAgencyAApproveOnly", china, AUTH_APPROVE);
    chinaAgencyAAcceptOnly =
        createAndAddUser("ChinaAgencyAAcceptOnly", china, AUTH_ACCEPT_LOWER_LEVELS);
    chinaAgencyBUser =
        createAndAddUser("ChinaAgencyBUser", china, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    indiaAgencyAUser =
        createAndAddUser("IndiaAgencyAUser", india, AUTH_APPROVE, AUTH_ACCEPT_LOWER_LEVELS);
    brazilPartner1User = createAndAddUser("BrazilPartner1User", brazil, AUTH_APPROVE);
    chinaPartner1User = createAndAddUser("ChinaPartner1User", china, AUTH_APPROVE);
    chinaPartner2User = createAndAddUser("ChinaPartner2User", china, AUTH_APPROVE);
    indiaPartner1User = createAndAddUser("IndiaPartner1User", india, AUTH_APPROVE);

    // User groups
    UserGroup globalUsers =
        getUserGroup(
            "GlobalUsers",
            userSet(
                globalUser, globalApproveOnly, globalAcceptOnly, globalConsultant, globalReadAll));
    UserGroup globalAgencyAUsers = getUserGroup("GlobalAgencyAUsers", userSet(globalAgencyAUser));
    UserGroup globalAgencyBUsers = getUserGroup("GlobalAgencyBUsers", userSet(globalAgencyBUser));
    UserGroup brazilInteragencyUsers =
        getUserGroup("BrazilInteragencyUsers", userSet(brazilInteragencyUser));
    UserGroup chinaInteragencyUsers =
        getUserGroup(
            "ChinaInteragencyUsers",
            userSet(
                chinaInteragencyUser, chinaInteragencyApproveOnly, chinalInteragencyAcceptOnly));
    UserGroup indiaInteragencyUsers =
        getUserGroup("IndiaInteragencyUsers", userSet(indiaInteragencyUser));
    UserGroup brazilAgencyAUsers = getUserGroup("BrazilAgencyAUsers", userSet(brazilAgencyAUser));
    UserGroup chinaAgencyAUsers =
        getUserGroup(
            "ChinaAgencyAUsers",
            userSet(chinaAgencyAUser, chinaAgencyAApproveOnly, chinaAgencyAAcceptOnly));
    UserGroup chinaAgencyBUsers = getUserGroup("ChinaAgencyBUsers", userSet(chinaAgencyBUser));
    UserGroup indiaAgencyAUsers = getUserGroup("IndiaAgencyAUsers", userSet(indiaAgencyAUser));
    UserGroup brazilPartner1Users =
        getUserGroup("BrazilPartner1Users", userSet(brazilPartner1User));
    UserGroup chinaPartner1Users = getUserGroup("ChinaPartner1Users", userSet(chinaPartner1User));
    UserGroup chinaPartner2Users = getUserGroup("ChinaPartner2Users", userSet(chinaPartner2User));
    UserGroup indiaPartner1Users = getUserGroup("IndiaPartner1Users", userSet(indiaPartner1User));

    // Attribute category options (mechanisms)
    brazilA1 = new CategoryOption("BrazilA1");
    chinaA1_1 = new CategoryOption("ChinaA1_1");
    chinaA1_2 = new CategoryOption("ChinaA1_2");
    chinaA2 = new CategoryOption("ChinaA2");
    chinaB2 = new CategoryOption("ChinaB2");
    indiaA1 = new CategoryOption("IndiaA1");
    worldwide = new CategoryOption("worldwide");
    brazilA1.setOrganisationUnits(Sets.newHashSet(brazil));
    chinaA1_1.setOrganisationUnits(Sets.newHashSet(china));
    chinaA1_2.setOrganisationUnits(Sets.newHashSet(china));
    chinaA2.setOrganisationUnits(Sets.newHashSet(china));
    chinaB2.setOrganisationUnits(Sets.newHashSet(china));
    indiaA1.setOrganisationUnits(Sets.newHashSet(india));
    // worldwide mechanism, unlike the others, is not limited by orgUnit
    categoryService.addCategoryOption(brazilA1);
    categoryService.addCategoryOption(chinaA1_1);
    categoryService.addCategoryOption(chinaA1_2);
    categoryService.addCategoryOption(chinaA2);
    categoryService.addCategoryOption(chinaB2);
    categoryService.addCategoryOption(indiaA1);
    categoryService.addCategoryOption(worldwide);
    setAccess(
        brazilA1,
        globalUsers,
        globalAgencyAUsers,
        brazilInteragencyUsers,
        brazilAgencyAUsers,
        brazilPartner1Users);
    setAccess(
        chinaA1_1,
        globalUsers,
        globalAgencyAUsers,
        chinaInteragencyUsers,
        chinaAgencyAUsers,
        chinaPartner1Users);
    setAccess(
        chinaA1_2,
        globalUsers,
        globalAgencyAUsers,
        chinaInteragencyUsers,
        chinaAgencyAUsers,
        chinaPartner1Users);
    setAccess(
        chinaA2,
        globalUsers,
        globalAgencyAUsers,
        chinaInteragencyUsers,
        chinaAgencyAUsers,
        chinaPartner2Users);
    setAccess(
        chinaB2,
        globalUsers,
        globalAgencyBUsers,
        chinaInteragencyUsers,
        chinaAgencyBUsers,
        chinaPartner2Users);
    setAccess(
        indiaA1,
        globalUsers,
        globalAgencyAUsers,
        indiaInteragencyUsers,
        indiaAgencyAUsers,
        indiaPartner1Users);
    setAccess(
        worldwide,
        globalUsers,
        globalAgencyAUsers,
        brazilInteragencyUsers,
        chinaInteragencyUsers,
        indiaInteragencyUsers);

    // Mechanism category and category combination (only 1 category)
    mechanismCategory =
        createCategory('A', brazilA1, chinaA1_1, chinaA1_2, chinaA2, chinaB2, indiaA1, worldwide);
    categoryService.addCategory(mechanismCategory);
    mechanismCategoryCombo = createCategoryCombo('A', mechanismCategory);
    categoryService.addCategoryCombo(mechanismCategoryCombo);

    // Constrain users by mechanism
    constrainByMechanism(globalAgencyAUser);
    constrainByMechanism(globalAgencyBUser);
    constrainByMechanism(brazilAgencyAUser);
    constrainByMechanism(chinaAgencyAUser);
    constrainByMechanism(chinaAgencyAApproveOnly);
    constrainByMechanism(chinaAgencyAAcceptOnly);
    constrainByMechanism(chinaAgencyBUser);
    constrainByMechanism(indiaAgencyAUser);
    constrainByMechanism(brazilPartner1User);
    constrainByMechanism(chinaPartner1User);
    constrainByMechanism(chinaPartner2User);
    constrainByMechanism(indiaPartner1User);
    userService.updateUser(globalAgencyAUser);
    userService.updateUser(globalAgencyBUser);
    userService.updateUser(brazilAgencyAUser);
    userService.updateUser(chinaAgencyAUser);
    userService.updateUser(chinaAgencyAApproveOnly);
    userService.updateUser(chinaAgencyAAcceptOnly);
    userService.updateUser(chinaAgencyBUser);
    userService.updateUser(indiaAgencyAUser);
    userService.updateUser(brazilPartner1User);
    userService.updateUser(chinaPartner1User);
    userService.updateUser(chinaPartner2User);
    userService.updateUser(indiaPartner1User);

    // Attribute option combos (one option per combo)
    brazilA1Combo = createCategoryOptionCombo(mechanismCategoryCombo, brazilA1);
    chinaA1_1Combo = createCategoryOptionCombo(mechanismCategoryCombo, chinaA1_1);
    chinaA1_2Combo = createCategoryOptionCombo(mechanismCategoryCombo, chinaA1_2);
    chinaA2Combo = createCategoryOptionCombo(mechanismCategoryCombo, chinaA2);
    chinaB2Combo = createCategoryOptionCombo(mechanismCategoryCombo, chinaB2);
    indiaA1Combo = createCategoryOptionCombo(mechanismCategoryCombo, indiaA1);
    worldwideCombo = createCategoryOptionCombo(mechanismCategoryCombo, worldwide);
    categoryService.addCategoryOptionCombo(brazilA1Combo);
    categoryService.addCategoryOptionCombo(chinaA1_1Combo);
    categoryService.addCategoryOptionCombo(chinaA1_2Combo);
    categoryService.addCategoryOptionCombo(chinaA2Combo);
    categoryService.addCategoryOptionCombo(chinaB2Combo);
    categoryService.addCategoryOptionCombo(indiaA1Combo);
    categoryService.addCategoryOptionCombo(worldwideCombo);
    mechanismCategoryCombo.getOptionCombos().add(brazilA1Combo);
    mechanismCategoryCombo.getOptionCombos().add(chinaA1_1Combo);
    mechanismCategoryCombo.getOptionCombos().add(chinaA1_2Combo);
    mechanismCategoryCombo.getOptionCombos().add(chinaA2Combo);
    mechanismCategoryCombo.getOptionCombos().add(chinaB2Combo);
    mechanismCategoryCombo.getOptionCombos().add(indiaA1Combo);
    mechanismCategoryCombo.getOptionCombos().add(worldwideCombo);
    categoryService.updateCategoryCombo(mechanismCategoryCombo);

    // Agency and partner category option groups
    agencyA = createCategoryOptionGroup('A', brazilA1, chinaA1_1, chinaA1_2, chinaA2, indiaA1);
    agencyB = createCategoryOptionGroup('B', chinaB2);
    partner1 = createCategoryOptionGroup('1', brazilA1, chinaA1_1, chinaA1_2, indiaA1);
    partner2 = createCategoryOptionGroup('2', chinaA2, chinaB2);
    categoryService.saveCategoryOptionGroup(agencyA);
    categoryService.saveCategoryOptionGroup(agencyB);
    categoryService.saveCategoryOptionGroup(partner1);
    categoryService.saveCategoryOptionGroup(partner2);
    setAccess(
        agencyA,
        globalUsers,
        globalAgencyAUsers,
        brazilInteragencyUsers,
        chinaInteragencyUsers,
        indiaInteragencyUsers,
        brazilAgencyAUsers,
        chinaAgencyAUsers,
        indiaAgencyAUsers);
    setAccess(agencyB, globalUsers, globalAgencyBUsers, chinaInteragencyUsers, chinaAgencyBUsers);
    setAccess(
        partner1,
        globalUsers,
        brazilInteragencyUsers,
        chinaInteragencyUsers,
        indiaInteragencyUsers,
        brazilAgencyAUsers,
        chinaAgencyAUsers,
        indiaAgencyAUsers,
        brazilPartner1Users,
        chinaPartner1Users,
        indiaPartner1Users);
    setAccess(partner2, globalUsers, chinaInteragencyUsers, chinaAgencyAUsers, chinaPartner2Users);

    // Agencies and partners category option group sets
    agencies = new CategoryOptionGroupSet("Agencies");
    partners = new CategoryOptionGroupSet("Partners");
    categoryService.saveCategoryOptionGroupSet(partners);
    categoryService.saveCategoryOptionGroupSet(agencies);
    setAccess(
        agencies,
        globalUsers,
        globalAgencyAUsers,
        globalAgencyBUsers,
        brazilInteragencyUsers,
        chinaInteragencyUsers,
        indiaInteragencyUsers,
        brazilAgencyAUsers,
        chinaAgencyAUsers,
        chinaAgencyBUsers,
        chinaAgencyBUsers,
        indiaAgencyAUsers);
    setAccess(
        partners,
        globalUsers,
        brazilInteragencyUsers,
        chinaInteragencyUsers,
        indiaInteragencyUsers,
        brazilAgencyAUsers,
        chinaAgencyAUsers,
        chinaAgencyBUsers,
        chinaAgencyBUsers,
        indiaAgencyAUsers,
        brazilPartner1Users,
        chinaPartner1Users,
        chinaPartner2Users,
        indiaPartner1Users);
    agencies.addCategoryOptionGroup(agencyA);
    agencies.addCategoryOptionGroup(agencyB);
    partners.addCategoryOptionGroup(partner1);
    partners.addCategoryOptionGroup(partner2);
    agencyA.getGroupSets().add(agencies);
    agencyB.getGroupSets().add(agencies);
    partner1.getGroupSets().add(partners);
    partner2.getGroupSets().add(partners);
    categoryService.updateCategoryOptionGroupSet(partners);
    categoryService.updateCategoryOptionGroupSet(agencies);
    categoryService.updateCategoryOptionGroup(agencyA);
    categoryService.updateCategoryOptionGroup(agencyB);
    categoryService.updateCategoryOptionGroup(partner1);
    categoryService.updateCategoryOptionGroup(partner2);

    // Data approval levels
    globalLevel1 = new DataApprovalLevel("GlobalLevel1", 1, null);
    globalAgencyLevel2 = new DataApprovalLevel("GlobalAgencyLevel2", 1, agencies);
    countryLevel3 = new DataApprovalLevel("CountryLevel3", 3, null);
    agencyLevel4 = new DataApprovalLevel("AgencyLevel4", 3, agencies);
    partnerLevel5 = new DataApprovalLevel("PartnerLevel5", 3, partners);
    dataApprovalLevelService.addDataApprovalLevel(globalLevel1, 1);
    dataApprovalLevelService.addDataApprovalLevel(globalAgencyLevel2, 2);
    dataApprovalLevelService.addDataApprovalLevel(countryLevel3, 3);
    dataApprovalLevelService.addDataApprovalLevel(agencyLevel4, 4);
    dataApprovalLevelService.addDataApprovalLevel(partnerLevel5, 5);
    periodType = periodService.reloadPeriodType(PeriodType.getPeriodTypeByName("Monthly"));

    // Period
    periodA = createPeriod("201801");
    periodService.addPeriod(periodA);

    // Data approval workflows
    workflow1 =
        new DataApprovalWorkflow(
            "workflow1",
            periodType,
            newHashSet(globalLevel1, countryLevel3, agencyLevel4, partnerLevel5));
    workflow2 =
        new DataApprovalWorkflow(
            "workflow2",
            periodType,
            newHashSet(globalLevel1, globalAgencyLevel2, agencyLevel4, partnerLevel5));
    dataApprovalService.addWorkflow(workflow1);
    dataApprovalService.addWorkflow(workflow2);

    // Data sets
    dataSetA = createDataSet('A', periodType, mechanismCategoryCombo);
    dataSetB = createDataSet('B', periodType, mechanismCategoryCombo);
    dataSetA.assignWorkflow(workflow1);
    dataSetB.assignWorkflow(workflow2);
    dataSetA.addOrganisationUnit(global);
    dataSetA.addOrganisationUnit(americas);
    dataSetA.addOrganisationUnit(asia);
    dataSetA.addOrganisationUnit(brazil);
    dataSetA.addOrganisationUnit(china);
    dataSetA.addOrganisationUnit(india);
    dataSetB.addOrganisationUnit(global);
    dataSetB.addOrganisationUnit(americas);
    dataSetB.addOrganisationUnit(asia);
    dataSetB.addOrganisationUnit(brazil);
    dataSetB.addOrganisationUnit(china);
    dataSetB.addOrganisationUnit(india);
    dataSetService.addDataSet(dataSetA);
    dataSetService.addDataSet(dataSetB);

    // System settings
    systemSettingManager.saveSystemSetting(SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD, 0);
    systemSettingManager.saveSystemSetting(SettingKey.ACCEPTANCE_REQUIRED_FOR_APPROVAL, true);
  }

  @Override
  public void tearDownTest() {
    systemSettingManager.saveSystemSetting(SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD, -1);
    systemSettingManager.saveSystemSetting(SettingKey.ACCEPTANCE_REQUIRED_FOR_APPROVAL, false);
    DataApprovalPermissionsEvaluator.invalidateCache();
  }

  // -------------------------------------------------------------------------
  // Test helper methods
  // -------------------------------------------------------------------------

  private void setUser(User user) {
    injectSecurityContext(user);
  }

  private String getStatusString(DataApprovalStatus status) {
    DataApprovalLevel dal = status.getActionLevel();
    String approval =
        dal == null
            ? "approval=null"
            : "ou="
                + (status.getOrganisationUnitUid() == null
                    ? "(null)"
                    : organisationUnitService
                        .getOrganisationUnit(status.getOrganisationUnitUid())
                        .getName())
                + " mechanism="
                + (status.getAttributeOptionComboUid() == null
                    ? "(null)"
                    : categoryService
                        .getCategoryOptionCombo(status.getAttributeOptionComboUid())
                        .getName())
                + " level="
                + (dal == null ? "(null)" : dal.getLevel());
    DataApprovalPermissions p = status.getPermissions();
    return approval
        + " "
        + (status.getState() == null ? "state=null" : status.getState().toString())
        + (p == null
            ? " permissions=null"
            : " approve="
                + (p.isMayApprove() ? "T" : "F")
                + " unapprove="
                + (p.isMayUnapprove() ? "T" : "F")
                + " accept="
                + (p.isMayAccept() ? "T" : "F")
                + " unaccept="
                + (p.isMayUnaccept() ? "T" : "F")
                + " read="
                + (p.isMayReadData() ? "T" : "F"));
  }

  private List<String> getUserApprovalsAndPermissions(
      User mockUserService,
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit orgUnit) {
    return getApprovalsExtended(mockUserService, workflow, period, orgUnit, null, null);
  }

  private List<String> getApprovalsExtended(
      User mockUserService,
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit orgUnit,
      OrganisationUnit ouFilter,
      CategoryOptionCombo aoc) {
    setUser(mockUserService);
    List<DataApprovalStatus> approvals =
        dataApprovalService.getUserDataApprovalsAndPermissions(
            workflow,
            period,
            orgUnit,
            ouFilter,
            mechanismCategoryCombo,
            aoc == null ? null : Set.of(aoc));
    return approvals.stream().map(status -> getStatusString(status)).sorted().collect(toList());
  }

  private String levels(User user, DataApprovalWorkflow workflow) {
    setUser(user);
    List<DataApprovalLevel> levels =
        dataApprovalLevelService.getUserDataApprovalLevels(user, workflow);
    String names = "";
    for (DataApprovalLevel level : levels) {
      names += (names.isEmpty() ? "" : ", ") + level.getName();
    }
    return names;
  }

  private boolean approve(
      User user,
      DataApprovalLevel dataApprovalLevel,
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo mechanismCombo) {
    DataApproval da =
        new DataApproval(
            dataApprovalLevel,
            workflow,
            period,
            organisationUnit,
            mechanismCombo,
            false,
            dateA,
            userA);
    setUser(user);
    try {
      dataApprovalService.approveData(Arrays.asList(da));
      return true;
    } catch (DataApprovalException ex) {
      return false;
    } catch (Throwable ex) {
      throw ex;
    }
  }

  private boolean unapprove(
      User user,
      DataApprovalLevel dataApprovalLevel,
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo mechanismCombo) {
    DataApproval da =
        new DataApproval(
            dataApprovalLevel,
            workflow,
            period,
            organisationUnit,
            mechanismCombo,
            false,
            dateA,
            userA);
    setUser(user);
    try {
      dataApprovalService.unapproveData(Arrays.asList(da));
      return true;
    } catch (DataApprovalException ex) {
      return false;
    } catch (Throwable ex) {
      throw ex;
    }
  }

  private boolean accept(
      User user,
      DataApprovalLevel dataApprovalLevel,
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo mechanismCombo) {
    DataApproval da =
        new DataApproval(
            dataApprovalLevel,
            workflow,
            period,
            organisationUnit,
            mechanismCombo,
            false,
            dateA,
            userA);
    setUser(user);
    try {
      dataApprovalService.acceptData(Arrays.asList(da));
      return true;
    } catch (DataApprovalException ex) {
      return false;
    } catch (Throwable ex) {
      throw ex;
    }
  }

  private boolean unaccept(
      User user,
      DataApprovalLevel dataApprovalLevel,
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo mechanismCombo) {
    DataApproval da =
        new DataApproval(
            dataApprovalLevel,
            workflow,
            period,
            organisationUnit,
            mechanismCombo,
            false,
            dateA,
            userA);
    setUser(user);
    try {
      dataApprovalService.unacceptData(Arrays.asList(da));
      return true;
    } catch (DataApprovalException ex) {
      return false;
    } catch (Throwable ex) {
      throw ex;
    }
  }

  private static void eq(Object expected, Object actual) {
    assertEquals(expected, actual);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  @Test
  void testGetUserDataApprovalLevels() {
    eq("GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5", levels(superUser, workflow1));
    eq(
        "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5",
        levels(globalConsultant, workflow1));
    eq("GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5", levels(globalUser, workflow1));
    eq(
        "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5",
        levels(globalApproveOnly, workflow1));
    eq(
        "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5",
        levels(globalAcceptOnly, workflow1));
    eq(
        "GlobalLevel1, CountryLevel3, AgencyLevel4, PartnerLevel5",
        levels(globalReadAll, workflow1));
    eq("CountryLevel3, AgencyLevel4, PartnerLevel5", levels(brazilInteragencyUser, workflow1));
    eq("CountryLevel3, AgencyLevel4, PartnerLevel5", levels(chinaInteragencyUser, workflow1));
    eq(
        "CountryLevel3, AgencyLevel4, PartnerLevel5",
        levels(chinaInteragencyApproveOnly, workflow1));
    eq(
        "CountryLevel3, AgencyLevel4, PartnerLevel5",
        levels(chinalInteragencyAcceptOnly, workflow1));
    eq("CountryLevel3, AgencyLevel4, PartnerLevel5", levels(indiaInteragencyUser, workflow1));
    eq("AgencyLevel4, PartnerLevel5", levels(globalAgencyAUser, workflow1));
    eq("AgencyLevel4, PartnerLevel5", levels(globalAgencyBUser, workflow1));
    eq("AgencyLevel4, PartnerLevel5", levels(brazilAgencyAUser, workflow1));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaAgencyAUser, workflow1));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaAgencyAApproveOnly, workflow1));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaAgencyAAcceptOnly, workflow1));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaAgencyBUser, workflow1));
    eq("AgencyLevel4, PartnerLevel5", levels(indiaAgencyAUser, workflow1));
    eq("PartnerLevel5", levels(brazilPartner1User, workflow1));
    eq("PartnerLevel5", levels(chinaPartner1User, workflow1));
    eq("PartnerLevel5", levels(chinaPartner2User, workflow1));
    eq("PartnerLevel5", levels(indiaPartner1User, workflow1));
    eq(
        "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5",
        levels(superUser, workflow2));
    eq(
        "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5",
        levels(globalConsultant, workflow2));
    eq(
        "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5",
        levels(globalUser, workflow2));
    eq(
        "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5",
        levels(globalApproveOnly, workflow2));
    eq(
        "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5",
        levels(globalAcceptOnly, workflow2));
    eq(
        "GlobalLevel1, GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5",
        levels(globalReadAll, workflow2));
    eq("GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", levels(globalAgencyAUser, workflow2));
    eq("GlobalAgencyLevel2, AgencyLevel4, PartnerLevel5", levels(globalAgencyBUser, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(brazilInteragencyUser, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaInteragencyUser, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaInteragencyApproveOnly, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(chinalInteragencyAcceptOnly, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(indiaInteragencyUser, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(brazilAgencyAUser, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaAgencyAUser, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaAgencyAApproveOnly, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaAgencyAAcceptOnly, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(chinaAgencyBUser, workflow2));
    eq("AgencyLevel4, PartnerLevel5", levels(indiaAgencyAUser, workflow2));
    eq("PartnerLevel5", levels(brazilPartner1User, workflow2));
    eq("PartnerLevel5", levels(chinaPartner1User, workflow2));
    eq("PartnerLevel5", levels(chinaPartner2User, workflow2));
    eq("PartnerLevel5", levels(indiaPartner1User, workflow2));
  }

  @Test
  void testApprovals() {
    // ---------------------------------------------------------------------
    // Nothing approved yet
    // ---------------------------------------------------------------------

    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getApprovalsExtended(superUser, workflow1, periodA, null, china, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getApprovalsExtended(superUser, workflow1, periodA, null, null, chinaA1_1Combo));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getApprovalsExtended(superUser, workflow1, periodA, null, null, worldwideCombo));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalConsultant, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalReadAll, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinalInteragencyAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyBUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(brazilPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner2User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(indiaPartner1User, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Approve ChinaA1_1 at level 5
    // ---------------------------------------------------------------------

    assertTrue(approve(superUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(unapprove(superUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(approve(globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unapprove(globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(approve(globalUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(globalApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(globalAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(approve(globalReadAll, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(brazilInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(
            chinaInteragencyApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(
            chinalInteragencyAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(indiaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(brazilAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyAApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyAAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyBUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(indiaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(brazilPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaPartner2User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(indiaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        approve(chinaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));

    // ---------------------------------------------------------------------
    // ChinaA1_1 is approved at level 5
    // ---------------------------------------------------------------------

    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalConsultant, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getApprovalsExtended(globalUser, workflow1, periodA, null, china, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F"),
        getApprovalsExtended(globalUser, workflow1, periodA, null, null, chinaA1_1Combo));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getApprovalsExtended(globalUser, workflow1, periodA, null, null, worldwideCombo));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalReadAll, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinalInteragencyAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyBUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(brazilPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner2User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(indiaPartner1User, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Approve ChinaA1_2 at level 5
    // ---------------------------------------------------------------------

    // TODO: test approving at wrong levels
    assertTrue(approve(superUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertTrue(unapprove(superUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertTrue(approve(globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertTrue(
        unapprove(globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(approve(globalUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(globalApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(globalAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(approve(globalReadAll, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(brazilInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(
            chinaInteragencyApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(
            chinalInteragencyAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(chinaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(indiaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(brazilAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(chinaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(chinaAgencyBUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(chinaAgencyAApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(chinaAgencyAAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(indiaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(brazilPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(chinaPartner2User, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertFalse(
        approve(indiaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));
    assertTrue(
        approve(chinaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_2Combo));

    // ---------------------------------------------------------------------
    // ChinaA1_1 is approved at level 5
    // ChinaA1_2 is approved at level 5
    // ---------------------------------------------------------------------

    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalConsultant, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalReadAll, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getApprovalsExtended(chinaInteragencyUser, workflow1, periodA, null, china, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F"),
        getApprovalsExtended(chinaInteragencyUser, workflow1, periodA, null, null, chinaA1_1Combo));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getApprovalsExtended(chinaInteragencyUser, workflow1, periodA, null, null, worldwideCombo));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinalInteragencyAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyBUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(brazilPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner2User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(indiaPartner1User, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Accept ChinaA1_1 at level 5
    // ---------------------------------------------------------------------

    assertTrue(accept(superUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(unaccept(superUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(accept(globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unaccept(globalConsultant, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(globalUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(globalApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(globalAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(globalReadAll, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(brazilInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(
            chinaInteragencyApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(
            chinalInteragencyAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(indiaInteragencyUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(brazilAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(chinaAgencyBUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(indiaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(brazilPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaPartner2User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(indiaPartner1User, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(accept(chinaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unaccept(chinaAgencyAUser, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaAgencyAApproveOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        accept(chinaAgencyAAcceptOnly, partnerLevel5, workflow1, periodA, china, chinaA1_1Combo));

    // ---------------------------------------------------------------------
    // ChinaA1_1 is accepted at level 5
    // ChinaA1_2 is approved at level 5
    // ---------------------------------------------------------------------

    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalConsultant, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalReadAll, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinalInteragencyAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyBUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(brazilPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner2User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(indiaPartner1User, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Approve ChinaA1_1 at level 4
    // ---------------------------------------------------------------------

    assertTrue(approve(superUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(unapprove(superUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(approve(globalConsultant, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unapprove(globalConsultant, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(approve(globalUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(globalApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(approve(globalAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(approve(globalReadAll, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(brazilInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(
            chinaInteragencyApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(
            chinalInteragencyAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(indiaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(brazilAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(approve(chinaAgencyBUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(approve(indiaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(brazilPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaPartner2User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(indiaPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(approve(chinaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unapprove(chinaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyAAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        approve(chinaAgencyAApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));

    // ---------------------------------------------------------------------
    // ChinaA1_1 is approved at level 4
    // ChinaA1_2 is approved at level 5
    // ---------------------------------------------------------------------

    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalConsultant, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalReadAll, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinalInteragencyAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyBUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(brazilPartner1User, workflow1, periodA, null));
    // (Note: Level 4 user can't see the level 3 approval, etc.)
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner2User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(indiaPartner1User, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Accept ChinaA1_1 at level 4
    // ---------------------------------------------------------------------

    assertTrue(accept(superUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(unaccept(superUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(accept(globalConsultant, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(unaccept(globalConsultant, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(globalUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(globalApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(globalAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(globalReadAll, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(brazilInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(indiaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(brazilAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(chinaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaAgencyAApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaAgencyAAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(chinaAgencyBUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(indiaAgencyAUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(brazilPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(chinaPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(chinaPartner2User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(indiaPartner1User, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        accept(chinaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unaccept(chinaInteragencyUser, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(
            chinaInteragencyApproveOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        accept(
            chinalInteragencyAcceptOnly, agencyLevel4, workflow1, periodA, china, chinaA1_1Combo));

    // ---------------------------------------------------------------------
    // ChinaA1_1 is accepted at level 4
    // ChinaA1_2 is approved at level 5
    // ---------------------------------------------------------------------

    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalConsultant, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalReadAll, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinalInteragencyAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyBUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(brazilPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner2User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(indiaPartner1User, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Approve ChinaA1_1 at level 3
    // ---------------------------------------------------------------------

    assertTrue(approve(superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(unapprove(superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(approve(globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unapprove(globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(approve(globalUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(globalApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(globalAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(approve(globalReadAll, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(brazilInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(indiaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(brazilAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyBUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyAApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyAAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(indiaAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(brazilPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(chinaPartner2User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(indiaPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        approve(chinaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unapprove(chinaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        approve(
            chinalInteragencyAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        approve(
            chinaInteragencyApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));

    // ---------------------------------------------------------------------
    // ChinaA1_1 is approved at level 3
    // ChinaA1_2 is approved at level 5
    // ---------------------------------------------------------------------

    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalConsultant, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalReadAll, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=3 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinalInteragencyAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyBUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(brazilPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner2User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(indiaPartner1User, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Accept ChinaA1_1 at level 3
    // ---------------------------------------------------------------------

    assertTrue(accept(superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(unaccept(superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(accept(globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unaccept(globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(globalReadAll, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(brazilInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(
            chinaInteragencyApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(
            chinalInteragencyAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(indiaInteragencyUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(brazilAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(chinaAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaAgencyAApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaAgencyAAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(chinaAgencyBUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(accept(indiaAgencyAUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(brazilPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(chinaPartner2User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(indiaPartner1User, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(accept(globalUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(unaccept(globalUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertFalse(
        accept(globalApproveOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(accept(globalAcceptOnly, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));

    // ---------------------------------------------------------------------
    // ChinaA1_1 is accepted at level 3
    // ChinaA1_2 is approved at level 5
    // ---------------------------------------------------------------------

    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalConsultant, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalReadAll, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinalInteragencyAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyBUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(brazilPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner2User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(indiaPartner1User, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Approve ChinaA1_1 at level 1
    // ---------------------------------------------------------------------

    // False because wrong org unit:
    assertFalse(approve(superUser, globalLevel1, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(approve(superUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(unapprove(superUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(approve(globalConsultant, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(
        unapprove(globalConsultant, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(approve(globalReadAll, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(brazilInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(chinaInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(indiaInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(brazilAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(chinaAgencyBUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(indiaAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(brazilPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(chinaPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(chinaPartner2User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(indiaPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(approve(globalUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(unapprove(globalUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        approve(globalAcceptOnly, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(
        approve(globalApproveOnly, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));

    // ---------------------------------------------------------------------
    // ChinaA1_1 is approved at level 1
    // ChinaA1_2 is approved at level 5
    // ---------------------------------------------------------------------

    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalConsultant, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(globalAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=1 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(globalReadAll, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaInteragencyApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinalInteragencyAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaInteragencyUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(brazilAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=F accept=F unaccept=F read=F",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAApproveOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=4 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyAAcceptOnly, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(chinaAgencyBUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=F unapprove=F accept=F unaccept=F read=F"),
        getUserApprovalsAndPermissions(indiaAgencyAUser, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(brazilPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA1_1 level=5 ACCEPTED_HERE approve=F unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner1User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(chinaPartner2User, workflow1, periodA, null));
    assertContainsOnly(
        List.of(
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(indiaPartner1User, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Unapprove ChinaA1_1 at level 1
    // ---------------------------------------------------------------------

    assertTrue(unapprove(superUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(approve(superUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(
        unapprove(globalConsultant, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(approve(globalConsultant, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(unapprove(globalReadAll, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(brazilInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(chinaInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(indiaInteragencyUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(brazilAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(chinaAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(chinaAgencyBUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(indiaAgencyAUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(brazilPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(chinaPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(chinaPartner2User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(indiaPartner1User, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(unapprove(globalUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(approve(globalUser, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertTrue(
        unapprove(globalApproveOnly, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertFalse(
        unapprove(globalAcceptOnly, globalLevel1, workflow1, periodA, global, chinaA1_1Combo));
    assertContainsOnly(
        List.of(
            "ou=Brazil mechanism=BrazilA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=Brazil mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaA1_1 level=3 ACCEPTED_HERE approve=T unapprove=T accept=F unaccept=T read=T",
            "ou=China mechanism=ChinaA1_2 level=5 APPROVED_HERE approve=F unapprove=T accept=T unaccept=F read=T",
            "ou=China mechanism=ChinaA2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=ChinaB2 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=China mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=IndiaA1 level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T",
            "ou=India mechanism=worldwide level=5 UNAPPROVED_READY approve=T unapprove=F accept=F unaccept=F read=T"),
        getUserApprovalsAndPermissions(superUser, workflow1, periodA, null));

    // ---------------------------------------------------------------------
    // Unaccept ChinaA1_1 at level 3
    // ---------------------------------------------------------------------

    assertTrue(unaccept(superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(accept(superUser, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(
        unaccept(globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
    assertTrue(accept(globalConsultant, countryLevel3, workflow1, periodA, china, chinaA1_1Combo));
  }
}
