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
package org.hisp.dhis.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hisp.dhis.expression.Operator.equal_to;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserServiceTarget;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccessService;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
class ValidationResultStoreTest extends TransactionalIntegrationTest {

  private static final String ACCESS_NONE = "--------";

  private static final String ACCESS_READ = "r-------";

  @Autowired private ValidationRuleStore validationRuleStore;

  @Autowired private ValidationResultStore validationResultStore;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired protected UserGroupAccessService userGroupAccessService;

  @Autowired protected UserGroupService userGroupService;

  @Autowired protected IdentifiableObjectManager identifiableObjectManager;

  @Autowired private UserService userService;

  @Autowired private CurrentUserService currentUserService;

  // -------------------------------------------------------------------------
  // Supporting data
  // -------------------------------------------------------------------------
  private Expression expressionA;

  private Expression expressionB;

  private ValidationRule validationRuleA;

  private ValidationRule validationRuleB;

  private ValidationResult validationResultAA;

  private ValidationResult validationResultAB;

  private ValidationResult validationResultAC;

  private ValidationResult validationResultBA;

  private ValidationResult validationResultBB;

  private ValidationResult validationResultBC;

  private ValidationResult validationResultCA;

  private Period periodA;

  private Period periodB;

  private OrganisationUnit sourceA;

  private OrganisationUnit sourceB;

  private OrganisationUnit sourceC;

  private CurrentUserService superUserService;

  private CurrentUserService userAService;

  private CurrentUserService userBService;

  private CurrentUserService userCService;

  private CurrentUserService userDService;

  private User userZ;

  private CategoryOption optionA;

  private CategoryOption optionB;

  private Category categoryA;

  private CategoryCombo categoryComboA;

  private CategoryOptionCombo optionComboA;

  private CategoryOptionCombo optionComboB;

  private CategoryOptionCombo optionComboC;

  private CategoryOptionGroup optionGroupA;

  private CategoryOptionGroup optionGroupB;

  private CategoryOptionGroupSet optionGroupSetB;

  // -------------------------------------------------------------------------
  // Set up/tear down helper methods
  // -------------------------------------------------------------------------
  private CurrentUserService getMockCurrentUserService(
      String userName, boolean superUserFlag, OrganisationUnit orgUnit, String... auths) {
    CurrentUserService mockCurrentUserService =
        new MockCurrentUserService(
            superUserFlag, Sets.newHashSet(orgUnit), Sets.newHashSet(orgUnit), auths);
    User user = mockCurrentUserService.getCurrentUser();
    user.setFirstName("Test");
    user.setSurname(userName);
    user.setUsername(userName);
    for (UserRole role : user.getUserRoles()) {
      role.setName(CodeGenerator.generateUid());
      userService.addUserRole(role);
    }
    userService.addUser(user);
    return mockCurrentUserService;
  }

  private void setPrivateAccess(BaseIdentifiableObject object, UserGroup... userGroups) {
    object.getSharing().setOwner(userZ);
    object.getSharing().setPublicAccess(ACCESS_NONE);
    for (UserGroup group : userGroups) {
      UserGroupAccess userGroupAccess = new UserGroupAccess();
      userGroupAccess.setAccess(ACCESS_READ);
      userGroupAccess.setUserGroup(group);
      object.getSharing().addUserGroupAccess(userGroupAccess);
    }
    identifiableObjectManager.updateNoAcl(object);
  }

  // -------------------------------------------------------------------------
  // Set up/tear down
  // -------------------------------------------------------------------------
  @Override
  public boolean emptyDatabaseAfterTest() {
    return true;
  }

  @Override
  public void setUpTest() throws Exception {
    // ---------------------------------------------------------------------
    // Add supporting data
    // ---------------------------------------------------------------------
    PeriodType periodType = PeriodType.getPeriodTypeByName("Monthly");
    periodA = createPeriod(new MonthlyPeriodType(), getDate(2017, 1, 1), getDate(2017, 1, 31));
    periodB = createPeriod(new MonthlyPeriodType(), getDate(2017, 2, 1), getDate(2017, 2, 28));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    sourceA = createOrganisationUnit('A');
    sourceB = createOrganisationUnit('B', sourceA);
    sourceC = createOrganisationUnit('C');
    organisationUnitService.addOrganisationUnit(sourceA);
    organisationUnitService.addOrganisationUnit(sourceB);
    organisationUnitService.addOrganisationUnit(sourceC);
    superUserService =
        getMockCurrentUserService("SuperUser", true, sourceA, UserRole.AUTHORITY_ALL);
    userAService = getMockCurrentUserService("UserA", false, sourceA);
    userBService = getMockCurrentUserService("UserB", false, sourceB);
    userCService = getMockCurrentUserService("UserC", false, sourceB);
    userDService = getMockCurrentUserService("UserD", false, sourceB);
    userZ = createUser('Z');
    userService.addUser(userZ);
    UserGroup userGroupC = createUserGroup('A', Sets.newHashSet(userCService.getCurrentUser()));
    UserGroup userGroupD = createUserGroup('B', Sets.newHashSet(userDService.getCurrentUser()));
    userGroupService.addUserGroup(userGroupC);
    userGroupService.addUserGroup(userGroupD);
    userCService.getCurrentUser().getGroups().add(userGroupC);
    userService.updateUser(userCService.getCurrentUser());
    userDService.getCurrentUser().getGroups().add(userGroupD);
    userService.updateUser(userDService.getCurrentUser());
    optionA = new CategoryOption("CategoryOptionA");
    optionB = new CategoryOption("CategoryOptionB");
    categoryService.addCategoryOption(optionA);
    categoryService.addCategoryOption(optionB);
    categoryA = createCategory('A', optionA, optionB);
    categoryService.addCategory(categoryA);
    categoryComboA = createCategoryCombo('A', categoryA);
    categoryService.addCategoryCombo(categoryComboA);
    optionComboA = createCategoryOptionCombo(categoryComboA, optionA);
    optionComboB = createCategoryOptionCombo(categoryComboA, optionB);
    optionComboC = createCategoryOptionCombo(categoryComboA, optionA, optionB);
    categoryService.addCategoryOptionCombo(optionComboA);
    categoryService.addCategoryOptionCombo(optionComboB);
    categoryService.addCategoryOptionCombo(optionComboC);
    optionGroupA = createCategoryOptionGroup('A', optionA);
    optionGroupB = createCategoryOptionGroup('B', optionB);
    categoryService.saveCategoryOptionGroup(optionGroupA);
    categoryService.saveCategoryOptionGroup(optionGroupB);
    optionGroupSetB = new CategoryOptionGroupSet("OptionGroupSetB");
    categoryService.saveCategoryOptionGroupSet(optionGroupSetB);
    optionGroupSetB.addCategoryOptionGroup(optionGroupA);
    optionGroupSetB.addCategoryOptionGroup(optionGroupB);
    optionGroupA.getGroupSets().add(optionGroupSetB);
    optionGroupB.getGroupSets().add(optionGroupSetB);
    setPrivateAccess(optionA, userGroupC);
    setPrivateAccess(optionB);
    setPrivateAccess(optionGroupA);
    setPrivateAccess(optionGroupB, userGroupD);
    categoryService.updateCategoryOptionGroupSet(optionGroupSetB);
    categoryService.updateCategoryOptionGroup(optionGroupA);
    categoryService.updateCategoryOptionGroup(optionGroupB);
    userCService.getCurrentUser().getCatDimensionConstraints().add(categoryA);
    userDService.getCurrentUser().getCogsDimensionConstraints().add(optionGroupSetB);
    expressionA = new Expression("expressionA", "descriptionA");
    expressionB = new Expression("expressionB", "descriptionB");
    validationRuleA = createValidationRule('A', equal_to, expressionA, expressionB, periodType);
    validationRuleB = createValidationRule('B', equal_to, expressionB, expressionA, periodType);
    validationRuleStore.save(validationRuleA);
    validationRuleStore.save(validationRuleB);
    validationResultAA =
        new ValidationResult(validationRuleA, periodA, sourceA, optionComboA, 1.0, 2.0, 3);
    validationResultAB =
        new ValidationResult(validationRuleA, periodA, sourceA, optionComboB, 1.0, 2.0, 3);
    validationResultAC =
        new ValidationResult(validationRuleA, periodA, sourceA, optionComboC, 1.0, 2.0, 3);
    validationResultBA =
        new ValidationResult(validationRuleB, periodB, sourceB, optionComboA, 1.0, 2.0, 3);
    validationResultBB =
        new ValidationResult(validationRuleB, periodB, sourceB, optionComboB, 1.0, 2.0, 3);
    validationResultBC =
        new ValidationResult(validationRuleB, periodB, sourceB, optionComboC, 1.0, 2.0, 3);
    validationResultCA =
        new ValidationResult(validationRuleB, periodB, sourceC, optionComboA, 1.0, 2.0, 3);
    validationResultAB.setNotificationSent(true);
  }

  @Override
  public void tearDownTest() {
    setDependency(
        CurrentUserServiceTarget.class,
        CurrentUserServiceTarget::setCurrentUserService,
        currentUserService,
        validationResultStore);
  }

  // -------------------------------------------------------------------------
  // Test helper methods
  // -------------------------------------------------------------------------
  private void setMockUserService(CurrentUserService mockUserService) {
    setDependency(
        CurrentUserServiceTarget.class,
        CurrentUserServiceTarget::setCurrentUserService,
        mockUserService,
        validationResultStore);
  }

  // -------------------------------------------------------------------------
  // Test ValidationResultStore
  // -------------------------------------------------------------------------
  @Test
  void testSaveValidationResult() throws Exception {
    Date beforeSave = new Date();
    validationResultStore.save(validationResultAA);
    Date afterSave = new Date();
    long id = validationResultAA.getId();
    ValidationResult validationResult = validationResultStore.get(id);
    assertNotNull(validationResult);
    assertEquals(validationResult.getValidationRule(), validationRuleA);
    assertEquals(validationResult.getPeriod(), periodA);
    assertEquals(validationResult.getOrganisationUnit(), sourceA);
    assertEquals(validationResult.getAttributeOptionCombo(), optionComboA);
    assertEquals(validationResult.getLeftsideValue(), (Double) 1.0);
    assertEquals(validationResult.getRightsideValue(), (Double) 2.0);
    assertEquals(validationResult.getDayInPeriod(), 3L);
    assertTrue(validationResult.getCreated().getTime() >= beforeSave.getTime());
    assertTrue(validationResult.getCreated().getTime() <= afterSave.getTime());
  }

  @Test
  void testDeleteValidationResult() throws Exception {
    validationResultStore.save(validationResultAA);
    long id = validationResultAA.getId();
    validationResultStore.delete(validationResultAA);
    assertNull(validationResultStore.get(id));
  }

  @Test
  void testGetAllUnreportedValidationResults() throws Exception {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    // Superuser can see all unreported results.
    setMockUserService(superUserService);
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.getAllUnreportedValidationResults());
    // User A can see all unreported results from sourceA or its children.
    setMockUserService(userAService);
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.getAllUnreportedValidationResults());
    // User B can see all unreported results from sourceB.
    setMockUserService(userBService);
    assertEqualSets(
        asList(validationResultBA, validationResultBB, validationResultBC),
        validationResultStore.getAllUnreportedValidationResults());
    // User C can see only optionA from sourceB.
    setMockUserService(userCService);
    assertEqualSets(
        singletonList(validationResultBA),
        validationResultStore.getAllUnreportedValidationResults());
    // User D can see only optionB from sourceB.
    setMockUserService(userDService);
    assertEqualSets(
        singletonList(validationResultBB),
        validationResultStore.getAllUnreportedValidationResults());
  }

  @Test
  void testGetById() throws Exception {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    setMockUserService(superUserService);
    assertEquals(validationResultAA, validationResultStore.getById(validationResultAA.getId()));
    assertEquals(validationResultAB, validationResultStore.getById(validationResultAB.getId()));
    assertEquals(validationResultAC, validationResultStore.getById(validationResultAC.getId()));
    assertEquals(validationResultBA, validationResultStore.getById(validationResultBA.getId()));
    assertEquals(validationResultBB, validationResultStore.getById(validationResultBB.getId()));
    assertEquals(validationResultBC, validationResultStore.getById(validationResultBC.getId()));
    setMockUserService(userAService);
    assertEquals(validationResultAA, validationResultStore.getById(validationResultAA.getId()));
    assertEquals(validationResultAB, validationResultStore.getById(validationResultAB.getId()));
    assertEquals(validationResultAC, validationResultStore.getById(validationResultAC.getId()));
    assertEquals(validationResultBA, validationResultStore.getById(validationResultBA.getId()));
    assertEquals(validationResultBB, validationResultStore.getById(validationResultBB.getId()));
    assertEquals(validationResultBC, validationResultStore.getById(validationResultBC.getId()));
    setMockUserService(userBService);
    assertNull(validationResultStore.getById(validationResultAA.getId()));
    assertNull(validationResultStore.getById(validationResultAB.getId()));
    assertNull(validationResultStore.getById(validationResultAC.getId()));
    assertEquals(validationResultBA, validationResultStore.getById(validationResultBA.getId()));
    assertEquals(validationResultBB, validationResultStore.getById(validationResultBB.getId()));
    assertEquals(validationResultBC, validationResultStore.getById(validationResultBC.getId()));
    setMockUserService(userCService);
    assertNull(validationResultStore.getById(validationResultAA.getId()));
    assertNull(validationResultStore.getById(validationResultAB.getId()));
    assertNull(validationResultStore.getById(validationResultAC.getId()));
    assertEquals(validationResultBA, validationResultStore.getById(validationResultBA.getId()));
    assertNull(validationResultStore.getById(validationResultBB.getId()));
    assertNull(validationResultStore.getById(validationResultBC.getId()));
    setMockUserService(userDService);
    assertNull(validationResultStore.getById(validationResultAA.getId()));
    assertNull(validationResultStore.getById(validationResultAB.getId()));
    assertNull(validationResultStore.getById(validationResultAC.getId()));
    assertNull(validationResultStore.getById(validationResultBA.getId()));
    assertEquals(validationResultBB, validationResultStore.getById(validationResultBB.getId()));
    assertNull(validationResultStore.getById(validationResultBC.getId()));
  }

  @Test
  void testQuery() throws Exception {
    List<ValidationResult> expected =
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC);
    save(expected);
    ValidationResultQuery query = new ValidationResultQuery();
    setMockUserService(superUserService);
    assertEqualSets(expected, validationResultStore.query(query));
    setMockUserService(userAService);
    assertEqualSets(expected, validationResultStore.query(query));
    setMockUserService(userBService);
    assertEqualSets(
        asList(validationResultBA, validationResultBB, validationResultBC),
        validationResultStore.query(query));
    setMockUserService(userCService);
    assertEqualSets(singletonList(validationResultBA), validationResultStore.query(query));
    setMockUserService(userDService);
    assertEqualSets(singletonList(validationResultBB), validationResultStore.query(query));
  }

  @Test
  void testQueryWithOrgUnitFilter() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    // test with superuser so user adds no extra restrictions
    setMockUserService(superUserService);
    ValidationResultQuery query = new ValidationResultQuery();
    // filter on A gives results for A
    query.setOu(singletonList(sourceA.getUid()));
    assertEqualSets(
        asList(validationResultAA, validationResultAB, validationResultAC),
        validationResultStore.query(query));
    // filter on B gives results for B
    query.setOu(singletonList(sourceB.getUid()));
    assertEqualSets(
        asList(validationResultBA, validationResultBB, validationResultBC),
        validationResultStore.query(query));
    // no match case
    query.setOu(singletonList(sourceC.getUid()));
    assertEqualSets(emptyList(), validationResultStore.query(query));
    // case with multiple units
    query.setOu(asList(sourceB.getUid(), sourceA.getUid()));
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.query(query));
    // now we restrict user to only be able to see Bs
    setMockUserService(userBService);
    // so filtering on As should not give any result
    query.setOu(singletonList(sourceA.getUid()));
    assertEqualSets(emptyList(), validationResultStore.query(query));
  }

  @Test
  void testQueryWithValidationRuleFilter() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    // test with superuser so user adds no extra restrictions
    setMockUserService(superUserService);
    ValidationResultQuery query = new ValidationResultQuery();
    // filter on A gives results for A
    query.setVr(singletonList(validationRuleA.getUid()));
    assertEqualSets(
        asList(validationResultAA, validationResultAB, validationResultAC),
        validationResultStore.query(query));
    // filter on B gives results for B
    query.setVr(singletonList(validationRuleB.getUid()));
    assertEqualSets(
        asList(validationResultBA, validationResultBB, validationResultBC),
        validationResultStore.query(query));
    // case with multiple units
    query.setVr(asList(validationRuleA.getUid(), validationRuleB.getUid()));
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.query(query));
    // now we restrict user to only be able to see Bs
    setMockUserService(userBService);
    // so filtering on As should not give any result
    query.setVr(singletonList(validationRuleA.getUid()));
    assertEqualSets(emptyList(), validationResultStore.query(query));
  }

  @Test
  void testQueryWithIsoPeriodFilter() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    // test with superuser so user adds no extra restrictions
    setMockUserService(superUserService);
    ValidationResultQuery query = new ValidationResultQuery();
    // periodA is Jan 2017, periodB is Feb 2017
    // monthly ISO pattern: YYYY-MM
    query.setPe(singletonList("2017-01"));
    assertEqualSets(
        asList(validationResultAA, validationResultAB, validationResultAC),
        validationResultStore.query(query));
    query.setPe(asList("2017-01", "2017-02"));
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.query(query));
    // QUARTERLY
    query.setPe(singletonList("2017Q1"));
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.query(query));
    // YEARLY
    query.setPe(singletonList("2017"));
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.query(query));
    // WEEKLY
    query.setPe(singletonList("2017W3"));
    assertEqualSets(
        asList(validationResultAA, validationResultAB, validationResultAC),
        validationResultStore.query(query));
  }

  @Test
  void testQueryWithCreatedDateFilter() {
    Date beforeA = new Date();
    wait1ms();
    save(asList(validationResultAA, validationResultAB, validationResultAC));
    wait1ms();
    Date beforeB = new Date();
    wait1ms();
    save(asList(validationResultBA, validationResultBB, validationResultBC));
    // B and onwards gives Bs
    ValidationResultQuery query = new ValidationResultQuery();
    query.setCreatedDate(beforeB);
    assertEqualSets(
        asList(validationResultBA, validationResultBB, validationResultBC),
        validationResultStore.query(query));
    // A and onwards gives As and Bs
    query.setCreatedDate(beforeA);
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.query(query));
    // after A and B onwards => none
    wait1ms();
    query.setCreatedDate(new Date());
    assertEqualSets(emptyList(), validationResultStore.query(query));
  }

  @Test
  void testQueryWithMultipleFilters() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    // test with superuser so user adds no extra restrictions
    setMockUserService(superUserService);
    // filter on A gives results for A
    ValidationResultQuery query = new ValidationResultQuery();
    query.setPe(singletonList("2017"));
    query.setVr(singletonList(validationRuleA.getUid()));
    query.setOu(singletonList(sourceA.getUid()));
    assertEqualSets(
        asList(validationResultAA, validationResultAB, validationResultAC),
        validationResultStore.query(query));
    // filter mutual exclusive gives empty result
    query.setVr(singletonList(validationRuleA.getUid()));
    query.setOu(singletonList(sourceB.getUid()));
    assertEqualSets(emptyList(), validationResultStore.query(query));
  }

  @Test
  void testCount() throws Exception {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    ValidationResultQuery query = new ValidationResultQuery();
    setMockUserService(superUserService);
    assertEquals(6, validationResultStore.count(query));
    setMockUserService(userAService);
    assertEquals(6, validationResultStore.count(query));
    setMockUserService(userBService);
    assertEquals(3, validationResultStore.count(query));
    setMockUserService(userCService);
    assertEquals(1, validationResultStore.count(query));
    setMockUserService(userDService);
    assertEquals(1, validationResultStore.count(query));
  }

  /**
   * The exact logic of the filters is tested in depth for the query method which shares the filter
   * logic with count. This test should just make sure that the count method used with filters has
   * no general issues.
   */
  @Test
  void testCountWithFilters() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    ValidationResultQuery query = new ValidationResultQuery();
    // org unit filter
    query.setOu(singletonList(sourceA.getUid()));
    assertEquals(3, validationResultStore.count(query));
    // period filter
    query.setVr(singletonList(validationRuleA.getUid()));
    assertEquals(3, validationResultStore.count(query));
    // period filter
    query.setPe(singletonList("2017-01"));
    assertEquals(3, validationResultStore.count(query));
  }

  @Test
  void testGetValidationResults() throws Exception {
    save(asList(validationResultAA, validationResultBA, validationResultCA));
    List<ValidationRule> rulesA = Lists.newArrayList(validationRuleA);
    List<ValidationRule> rulesAB = Lists.newArrayList(validationRuleA, validationRuleB);
    List<Period> periodsB = Lists.newArrayList(periodB);
    List<Period> periodsAB = Lists.newArrayList(periodA, periodB);
    assertEqualSets(
        singletonList(validationResultAA),
        validationResultStore.getValidationResults(null, false, rulesA, periodsAB));
    assertEqualSets(
        asList(validationResultBA, validationResultCA),
        validationResultStore.getValidationResults(null, true, rulesAB, periodsB));
    assertEqualSets(
        asList(validationResultAA, validationResultBA, validationResultCA),
        validationResultStore.getValidationResults(null, true, rulesAB, periodsAB));
    assertEqualSets(
        asList(validationResultAA, validationResultBA),
        validationResultStore.getValidationResults(sourceA, true, rulesAB, periodsAB));
    assertEqualSets(
        singletonList(validationResultAA),
        validationResultStore.getValidationResults(sourceA, false, rulesAB, periodsAB));
    assertEqualSets(
        singletonList(validationResultBA),
        validationResultStore.getValidationResults(sourceB, false, rulesAB, periodsAB));
  }

  @Test
  void testDeleteObject() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    validationResultStore.delete(validationResultAA);
    assertEqualSets(
        asList(
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.query(new ValidationResultQuery()));
  }

  @Test
  void testDeleteByRequestWithOrganisationUnit() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    ValidationResultsDeletionRequest request = new ValidationResultsDeletionRequest();
    request.setOu(singletonList(sourceA.getUid()));
    validationResultStore.delete(request);
    assertEqualSets(
        asList(validationResultBA, validationResultBB, validationResultBC),
        validationResultStore.query(new ValidationResultQuery()));
  }

  @Test
  void testDeleteByRequestWithValidationRule() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    ValidationResultsDeletionRequest request = new ValidationResultsDeletionRequest();
    request.setVr(singletonList(validationRuleA.getUid()));
    validationResultStore.delete(request);
    assertEqualSets(
        asList(validationResultBA, validationResultBB, validationResultBC),
        validationResultStore.query(new ValidationResultQuery()));
  }

  @Test
  void testDeleteByRequestWithPeriod() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    ValidationResultsDeletionRequest request = new ValidationResultsDeletionRequest();
    request.setPe(periodA.getUid());
    validationResultStore.delete(request);
    assertEqualSets(
        asList(validationResultBA, validationResultBB, validationResultBC),
        validationResultStore.query(new ValidationResultQuery()));
  }

  @Test
  void testDeleteByRequestWithCreatedPeriod() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    ValidationResultsDeletionRequest request = new ValidationResultsDeletionRequest();
    request.setCreated("" + LocalDate.now().getYear());
    validationResultStore.delete(request);
    assertEqualSets(emptyList(), validationResultStore.query(new ValidationResultQuery()));
  }

  @Test
  void testDeleteByRequestWithNotificationSent() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    ValidationResultsDeletionRequest request = new ValidationResultsDeletionRequest();
    // AB is saved with true, others
    request.setNotificationSent(true);
    // with false
    validationResultStore.delete(request);
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.query(new ValidationResultQuery()));
  }

  @Test
  void testDeleteByRequestWithMultipleCriteria() {
    save(
        asList(
            validationResultAA,
            validationResultAB,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC));
    ValidationResultsDeletionRequest request = new ValidationResultsDeletionRequest();
    request.setOu(singletonList(sourceA.getUid()));
    request.setVr(singletonList(validationRuleA.getUid()));
    request.setPe(periodA.getUid());
    request.setNotificationSent(true);
    validationResultStore.delete(request);
    // Ou, Vr and Pe match all As but notificationSent matches only AB
    assertEqualSets(
        asList(
            validationResultAA,
            validationResultAC,
            validationResultBA,
            validationResultBB,
            validationResultBC),
        validationResultStore.query(new ValidationResultQuery()));
  }

  private void save(Iterable<ValidationResult> results) {
    for (ValidationResult r : results) validationResultStore.save(r);
  }

  private static <T> void assertEqualSets(Collection<T> expected, Collection<T> actual) {
    assertEquals(expected.size(), actual.size());
    if (expected.size() == 1) {
      assertEquals(expected, actual);
    } else {
      assertEquals(new HashSet<>(expected), new HashSet<>(actual));
    }
  }

  private void wait1ms() {
    long now = System.currentTimeMillis();
    while (now >= System.currentTimeMillis()) // busy wait 1 ms
      ;
  }
}
