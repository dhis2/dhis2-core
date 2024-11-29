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
package org.hisp.dhis.user;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class UserServiceTest extends PostgresIntegrationTestBase {

  @Autowired private UserGroupService userGroupService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private SystemSettingsService settingsService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private PasswordManager passwordManager;

  private OrganisationUnit unitA;

  private OrganisationUnit unitB;

  private OrganisationUnit unitC;

  private OrganisationUnit unitD;

  private OrganisationUnit unitE;

  private UserRole roleA;

  private UserRole roleB;

  private UserRole roleC;

  @BeforeAll
  void setUp() {
    unitA = createOrganisationUnit('A');
    unitB = createOrganisationUnit('B');
    unitC = createOrganisationUnit('C', unitA);
    unitD = createOrganisationUnit('D', unitB);
    unitE = createOrganisationUnit('E');
    organisationUnitService.addOrganisationUnit(unitA);
    organisationUnitService.addOrganisationUnit(unitB);
    organisationUnitService.addOrganisationUnit(unitC);
    organisationUnitService.addOrganisationUnit(unitD);
    organisationUnitService.addOrganisationUnit(unitE);
    roleA = createUserRole('A');
    roleB = createUserRole('B');
    roleC = createUserRole('C');
    roleA.getAuthorities().add("AuthA");
    roleA.getAuthorities().add("AuthB");
    roleA.getAuthorities().add("AuthC");
    roleA.getAuthorities().add("AuthD");
    roleB.getAuthorities().add("AuthA");
    roleB.getAuthorities().add("AuthB");
    roleC.getAuthorities().add("AuthC");
    userService.addUserRole(roleA);
    userService.addUserRole(roleB);
    userService.addUserRole(roleC);
  }

  @BeforeEach
  final void setup() {
    injectAdminIntoSecurityContext();
  }

  private UserQueryParams getDefaultParams() {
    return new UserQueryParams().setCanSeeOwnRoles(true);
  }

  @Test
  void testAddGetUser() {
    User userA = addUser("A", unitA, unitB);
    User userB = addUser("B", unitA, unitB);
    Set<OrganisationUnit> expected = new HashSet<>(asList(unitA, unitB));
    assertEquals(expected, userService.getUser(userA.getId()).getOrganisationUnits());
    assertEquals(expected, userService.getUser(userB.getId()).getOrganisationUnits());
  }

  @Test
  void testGetUserByUsernames() {
    addUser("a");
    addUser("b");

    assertEquals(2, userService.getUsersByUsernames(asList("usernamea", "usernameb")).size());
    assertEquals(
        2, userService.getUsersByUsernames(asList("usernamea", "usernameb", "usernamex")).size());
    assertEquals(0, userService.getUsersByUsernames(List.of("usernamec")).size());
  }

  @Test
  void testUpdateUser() {
    User userA = addUser("A");
    User userB = addUser("B");
    assertEquals(userA, userService.getUser(userA.getId()));
    assertEquals(userB, userService.getUser(userB.getId()));
    userA.setSurname("UpdatedSurnameA");
    userService.updateUser(userA);
    assertEquals("UpdatedSurnameA", userService.getUser(userA.getId()).getSurname());
  }

  @Test
  void testDeleteUser() {
    User userA = addUser("A");
    User userB = addUser("B");
    assertEquals(userA, userService.getUser(userA.getId()));
    assertEquals(userB, userService.getUser(userB.getId()));
    userService.deleteUser(userA);
    assertNull(userService.getUser(userA.getId()));
    assertNotNull(userService.getUser(userB.getId()));
  }

  @Test
  void testDeleteCreatedByUser() {
    User userA = addUser("A");

    DataElement dataElement = createDataElement('A');
    dataElement.setCreatedBy(userA);
    idObjectManager.save(dataElement);

    assertThrows(DeleteNotAllowedException.class, () -> userService.deleteUser(userA));
  }

  @Test
  void testDeleteLastUpdatedByUser() {
    User userA = createUserWithAuth("A", "ALL");
    injectSecurityContext(UserDetails.fromUser(userA));

    DataElement dataElement = createDataElement('A');
    idObjectManager.save(dataElement);

    dataElement.setDescription("Updated");
    idObjectManager.update(dataElement);

    assertThrows(DeleteNotAllowedException.class, () -> userService.deleteUser(userA));
  }

  @Test
  void testUserByQuery() {
    User userA = addUser("A", user -> user.setFirstName("Chris"));
    User userB = addUser("B", user -> user.setFirstName("Chris"));
    assertContainsOnly(
        List.of(userA, userB), userService.getUsers(getDefaultParams().setQuery("Chris")));
    assertContainsOnly(
        List.of(userA, userB), userService.getUsers(getDefaultParams().setQuery("hris SURNAM")));
    assertContainsOnly(
        List.of(userA), userService.getUsers(getDefaultParams().setQuery("hris SurnameA")));
    assertContainsOnly(
        List.of(userB), userService.getUsers(getDefaultParams().setQuery("urnameB")));
    assertContainsOnly(List.of(userA), userService.getUsers(getDefaultParams().setQuery("MAilA")));
    assertContainsOnly(
        List.of(userA, userB), userService.getUsers(getDefaultParams().setQuery("userNAME")));
    assertContainsOnly(
        List.of(userA), userService.getUsers(getDefaultParams().setQuery("ernameA")));
  }

  @Test
  void testUserByOrgUnits() {
    User userA = addUser("A", unitA);
    addUser("B", unitB);
    User userC = addUser("C", unitC);
    addUser("D", unitD);
    UserQueryParams params = getDefaultParams().addOrganisationUnit(unitA).setUser(userA);
    assertContainsOnly(List.of(userA), userService.getUsers(params));
    params =
        getDefaultParams()
            .addOrganisationUnit(unitA)
            .setIncludeOrgUnitChildren(true)
            .setUser(userA);
    assertContainsOnly(List.of(userA, userC), userService.getUsers(params));
  }

  @Test
  void testUserByDataViewOrgUnits() {
    User userA = addUser("A", unitA);
    userA.getDataViewOrganisationUnits().add(unitA);
    userService.updateUser(userA);
    User userB = addUser("B", unitB);
    userB.getDataViewOrganisationUnits().add(unitA);
    userService.updateUser(userB);
    User userC = addUser("C", unitC);
    userC.getDataViewOrganisationUnits().add(unitC);
    userService.updateUser(userC);
    User userD = addUser("D", unitD);
    userD.getDataViewOrganisationUnits().add(unitD);
    userService.updateUser(userD);
    UserQueryParams params = getDefaultParams().addDataViewOrganisationUnit(unitA).setUser(userA);
    assertContainsOnly(List.of(userA, userB), userService.getUsers(params));
    params =
        getDefaultParams()
            .addDataViewOrganisationUnit(unitA)
            .setIncludeOrgUnitChildren(true)
            .setUser(userA);
    assertContainsOnly(List.of(userA, userB, userC), userService.getUsers(params));
  }

  @Test
  void testUserByTeiSearchOrgUnits() {
    User userA = addUser("A", unitA);
    userA.getTeiSearchOrganisationUnits().add(unitA);
    userService.updateUser(userA);
    User userB = addUser("B", unitB);
    userB.getTeiSearchOrganisationUnits().add(unitA);
    userService.updateUser(userB);
    User userC = addUser("C", unitC);
    userC.getTeiSearchOrganisationUnits().add(unitC);
    userService.updateUser(userC);
    User userD = addUser("D", unitD);
    userD.getTeiSearchOrganisationUnits().add(unitD);
    userService.updateUser(userD);
    UserQueryParams params = getDefaultParams().addTeiSearchOrganisationUnit(unitA).setUser(userA);
    assertContainsOnly(List.of(userA, userB), userService.getUsers(params));
    params =
        getDefaultParams()
            .addTeiSearchOrganisationUnit(unitA)
            .setIncludeOrgUnitChildren(true)
            .setUser(userA);
    assertContainsOnly(List.of(userA, userB, userC), userService.getUsers(params));
  }

  @Test
  void testUserByUserGroups() {
    User userA = addUser("A");
    User userB = addUser("B");
    User userC = addUser("C");
    User userD = addUser("D");
    UserGroup ugA = createUserGroup('A', newHashSet(userA, userB));
    UserGroup ugB = createUserGroup('B', newHashSet(userB, userC));
    UserGroup ugC = createUserGroup('C', newHashSet(userD));
    userGroupService.addUserGroup(ugA);
    userGroupService.addUserGroup(ugB);
    userGroupService.addUserGroup(ugC);
    assertContainsOnly(
        List.of(userA, userB),
        userService.getUsers(getDefaultParams().setUserGroups(newHashSet(ugA))));
    assertContainsOnly(
        List.of(userA, userB, userC),
        userService.getUsers(getDefaultParams().setUserGroups(newHashSet(ugA, ugB))));
    assertContainsOnly(
        List.of(userA, userB, userD),
        userService.getUsers(getDefaultParams().setUserGroups(newHashSet(ugA, ugC))));
  }

  @Test
  void testGetUserOrgUnits() {
    User currentUser = addUser("Z", unitA, unitB);
    User userA = addUser("A", unitA);
    User userB = addUser("B", unitB);
    User userC = addUser("C", unitC);
    User userD = addUser("D", unitD);
    addUser("E", unitE);
    UserQueryParams params = getDefaultParams().setUser(currentUser).setUserOrgUnits(true);
    assertContainsOnly(List.of(currentUser, userA, userB), userService.getUsers(params));
    params =
        getDefaultParams()
            .setUser(currentUser)
            .setUserOrgUnits(true)
            .setIncludeOrgUnitChildren(true);
    assertContainsOnly(
        List.of(currentUser, userA, userB, userC, userD), userService.getUsers(params));
  }

  @Test
  void testManagedGroups() {
    settingsService.put("keyCanGrantOwnUserAuthorityGroups", true);
    settingsService.clearCurrentSettings();
    // TODO find way to override in parameters
    User userA = addUser("A");
    User userB = addUser("B");
    User userC = addUser("C");
    User userD = addUser("D");
    UserGroup userGroup1 = createUserGroup('A', newHashSet(userA, userB));
    UserGroup userGroup2 = createUserGroup('B', newHashSet(userC, userD));
    userA.getGroups().add(userGroup1);
    userB.getGroups().add(userGroup1);
    userC.getGroups().add(userGroup2);
    userD.getGroups().add(userGroup2);
    userGroup1.setManagedGroups(newHashSet(userGroup2));
    userGroup2.setManagedByGroups(newHashSet(userGroup1));
    long group1 = userGroupService.addUserGroup(userGroup1);
    long group2 = userGroupService.addUserGroup(userGroup2);
    assertEquals(1, userGroupService.getUserGroup(group1).getManagedGroups().size());
    assertTrue(userGroupService.getUserGroup(group1).getManagedGroups().contains(userGroup2));
    assertEquals(1, userGroupService.getUserGroup(group2).getManagedByGroups().size());
    assertTrue(userGroupService.getUserGroup(group2).getManagedByGroups().contains(userGroup1));
    assertTrue(userA.canManage(userGroup2));
    assertTrue(userB.canManage(userGroup2));
    assertFalse(userC.canManage(userGroup1));
    assertFalse(userD.canManage(userGroup1));
    assertTrue(userA.canManage(userC));
    assertTrue(userA.canManage(userD));
    assertTrue(userB.canManage(userC));
    assertTrue(userA.canManage(userD));
    assertFalse(userC.canManage(userA));
    assertFalse(userC.canManage(userB));
    assertTrue(userC.isManagedBy(userGroup1));
    assertTrue(userD.isManagedBy(userGroup1));
    assertFalse(userA.isManagedBy(userGroup2));
    assertFalse(userB.isManagedBy(userGroup2));
    assertTrue(userC.isManagedBy(userA));
    assertTrue(userC.isManagedBy(userB));
    assertTrue(userD.isManagedBy(userA));
    assertTrue(userD.isManagedBy(userB));
    assertFalse(userA.isManagedBy(userC));
    assertFalse(userA.isManagedBy(userD));
  }

  @Test
  void testGetByPhoneNumber() {
    settingsService.put("keyCanGrantOwnUserAuthorityGroups", true);
    settingsService.clearCurrentSettings();
    addUser("A", user -> user.setPhoneNumber("73647271"));
    User userB = addUser("B", user -> user.setPhoneNumber("23452134"));
    addUser("C", user -> user.setPhoneNumber("14543232"));
    List<User> users = userService.getUsersByPhoneNumber("23452134");
    assertEquals(1, users.size());
    assertEquals(userB, users.get(0));
  }

  @Test
  void testGetByUuid() {
    User userA = addUser("A");
    User userB = addUser("B");
    assertEquals(userA, userService.getUserByUuid(userA.getUuid()));
    assertEquals(userB, userService.getUserByUuid(userB.getUuid()));
  }

  @Test
  void testGetByIdentifier() {
    User userA = addUser("A");
    User userB = addUser("B");
    addUser("C");
    // Match
    assertEquals(userA, userService.getUserByIdentifier(userA.getUid()));
    assertEquals(userA, userService.getUserByIdentifier(userA.getUuid().toString()));
    assertEquals(userA, userService.getUserByIdentifier(userA.getUsername()));
    assertEquals(userB, userService.getUserByIdentifier(userB.getUid()));
    assertEquals(userB, userService.getUserByIdentifier(userB.getUuid().toString()));
    assertEquals(userB, userService.getUserByIdentifier(userB.getUsername()));
    // No match
    assertNull(userService.getUserByIdentifier("hYg6TgAfN71"));
    assertNull(userService.getUserByIdentifier("cac39761-3ef9-4774-8b1e-b96cedbc57a9"));
    assertNull(userService.getUserByIdentifier("johndoe"));
  }

  @Test
  void testGetOrdered() {
    User userA =
        addUser(
            "A",
            user -> {
              user.setSurname("Yong");
              user.setFirstName("Anne");
              user.setEmail("lost@space.com");
              user.getOrganisationUnits().add(unitA);
            });
    User userB =
        addUser(
            "B",
            user -> {
              user.setSurname("Arden");
              user.setFirstName("Jenny");
              user.setEmail("Inside@other.com");
              user.getOrganisationUnits().add(unitA);
            });
    User userC =
        addUser(
            "C",
            user -> {
              user.setSurname("Smith");
              user.setFirstName("Igor");
              user.setEmail("home@other.com");
              user.getOrganisationUnits().add(unitA);
            });
    UserQueryParams params = getDefaultParams().addOrganisationUnit(unitA);
    List<User> allUsersA = userService.getUsers(params, singletonList("email:idesc"));
    assertEquals(asList(getAdminUser(), userA, userB, userC), allUsersA);

    List<User> allUsersB = userService.getUsers(params, null);
    assertEquals(asList(userB, userC, getAdminUser(), userA), allUsersB);

    List<User> allUserC = userService.getUsers(params, singletonList("firstName:asc"));
    assertEquals(asList(userA, getAdminUser(), userC, userB), allUserC);
  }

  @Test
  void testGetManagedGroupsLessAuthoritiesDisjointRoles() {
    settingsService.put("keyCanGrantOwnUserAuthorityGroups", false);
    settingsService.clearCurrentSettings();

    User userA = addUser("A", roleA);
    User userB = addUser("B", roleB, roleC);
    User userC = addUser("C", roleA, roleC);
    User userD = addUser("D", roleC);
    User userE = addUser("E", roleA);
    User userF = addUser("F", roleC);
    UserGroup userGroup1 = createUserGroup('A', newHashSet(userA, userB));
    UserGroup userGroup2 = createUserGroup('B', newHashSet(userC, userD, userE, userF));
    userA.getGroups().add(userGroup1);
    userB.getGroups().add(userGroup1);
    userC.getGroups().add(userGroup2);
    userD.getGroups().add(userGroup2);
    userE.getGroups().add(userGroup2);
    userF.getGroups().add(userGroup2);
    userGroup1.setManagedGroups(newHashSet(userGroup2));
    userGroup2.setManagedByGroups(newHashSet(userGroup1));
    userGroupService.addUserGroup(userGroup1);
    userGroupService.addUserGroup(userGroup2);
    UserQueryParams params =
        new UserQueryParams().setCanManage(true).setAuthSubset(true).setUser(userA);
    assertContainsOnly(
        List.of(userD.getUsername(), userF.getUsername()),
        userService.getUsers(params).stream().map(User::getUsername).toList());
    assertEquals(2, userService.getUserCount(params));
    params.setUser(userB);
    assertIsEmpty(userService.getUsers(params));
    assertEquals(0, userService.getUserCount(params));
    params.setUser(userC);
    assertIsEmpty(userService.getUsers(params));
    assertEquals(0, userService.getUserCount(params));
  }

  @Test
  void testGetManagedGroupsSearch() {
    addUser("A");
    User userB = addUser("B");
    addUser("C");
    addUser("D");
    addUser("E");
    addUser("F");
    UserQueryParams params = getDefaultParams().setQuery("rstnameB");
    assertContainsOnly(List.of(userB), userService.getUsers(params));
    assertEquals(1, userService.getUserCount(params));
  }

  @Test
  void testGetManagedGroupsSelfRegistered() {
    User userA = addUser("A", User::setSelfRegistered, true);
    addUser("B");
    User userC = addUser("C", User::setSelfRegistered, true);
    addUser("D");
    UserQueryParams params = getDefaultParams().setSelfRegistered(true);
    assertContainsOnly(List.of(userA, userC), userService.getUsers(params));
    assertEquals(2, userService.getUserCount(params));
  }

  @Test
  void testGetManagedGroupsOrganisationUnit() {
    User userA = addUser("A", unitA, unitB);
    addUser("B", unitB);
    User userC = addUser("C", unitA);
    addUser("D", unitB);

    UserQueryParams params = getDefaultParams().addOrganisationUnit(unitA);
    clearSecurityContext();
    List<User> users = userService.getUsers(params);
    assertContainsOnly(List.of(userA, userC), users);
    assertEquals(2, userService.getUserCount(params));
  }

  @Test
  void testGetInvitations() {
    addUser("A");
    User userB = addUser("B", User::setInvitation, true);
    addUser("C");
    User userD = addUser("D", User::setInvitation, true);
    UserQueryParams params = getDefaultParams().setInvitationStatus(UserInvitationStatus.ALL);
    assertContainsOnly(List.of(userB, userD), userService.getUsers(params));
    assertEquals(2, userService.getUserCount(params));
    params.setInvitationStatus(UserInvitationStatus.EXPIRED);
    assertIsEmpty(userService.getUsers(params));
    assertEquals(0, userService.getUserCount(params));
  }

  @Test
  void testGetExpiringUserAccounts() {
    ZonedDateTime now = ZonedDateTime.now();
    Date inFiveDays = Date.from(now.plusDays(5).toInstant());
    Date inSixDays = Date.from(now.plusDays(6).toInstant());
    Date inEightDays = Date.from(now.plusDays(8).toInstant());
    addUser("A");
    addUser("B", User::setAccountExpiry, inFiveDays);
    addUser("C");
    addUser("D", User::setAccountExpiry, inSixDays);
    addUser("E", User::setAccountExpiry, inEightDays);
    List<UserAccountExpiryInfo> soonExpiringAccounts = userService.getExpiringUserAccounts(7);
    Set<String> soonExpiringAccountNames =
        soonExpiringAccounts.stream().map(UserAccountExpiryInfo::getUsername).collect(toSet());
    assertEquals(new HashSet<>(asList("usernameb", "usernamed")), soonExpiringAccountNames);

    soonExpiringAccounts = userService.getExpiringUserAccounts(9);
    soonExpiringAccountNames =
        soonExpiringAccounts.stream().map(UserAccountExpiryInfo::getUsername).collect(toSet());
    assertEquals(
        new HashSet<>(asList("usernameb", "usernamed", "usernamee")), soonExpiringAccountNames);

    for (UserAccountExpiryInfo expiryInfo : soonExpiringAccounts) {
      assertEquals(expiryInfo.getUsername().replace("username", "email"), expiryInfo.getEmail());
    }
  }

  @Test
  void testDisableUsersInactiveSince() {
    ZonedDateTime now = ZonedDateTime.now();
    Date twoMonthsAgo = Date.from(now.minusMonths(2).toInstant());
    Date threeMonthAgo = Date.from(now.minusMonths(3).toInstant());
    Date fourMonthAgo = Date.from(now.minusMonths(4).toInstant());
    Date twentyTwoDaysAgo = Date.from(now.minusDays(22).toInstant());
    User userA = addUser("A", User::setLastLogin, threeMonthAgo);
    User userB =
        addUser(
            "B",
            credentials -> {
              credentials.setDisabled(true);
              credentials.setLastLogin(fourMonthAgo);
            });
    addUser("C", User::setLastLogin, twentyTwoDaysAgo);
    addUser("D");
    // User A gets disabled, B would but already was, C is active, D last
    // login is still null
    assertEquals(1, userService.disableUsersInactiveSince(twoMonthsAgo));
    // being a super-user is the simplest way to filter purely on the set
    // parameters
    injectAdminIntoSecurityContext();
    UserQueryParams params = getDefaultParams().setDisabled(true);
    List<User> users = userService.getUsers(params);
    assertEquals(
        new HashSet<>(asList(userA.getUid(), userB.getUid())),
        users.stream().map(User::getUid).collect(toSet()));
  }

  @Test
  void testFindNotifiableUsersWithLastLoginBetween() {
    ZonedDateTime now = ZonedDateTime.now();
    Date oneMonthsAgo = Date.from(now.minusMonths(1).toInstant());
    Date twoMonthsAgo = Date.from(now.minusMonths(2).toInstant());
    Date threeMonthAgo = Date.from(now.minusMonths(3).toInstant());
    Date fourMonthAgo = Date.from(now.minusMonths(4).toInstant());
    Date twentyTwoDaysAgo = Date.from(now.minusDays(22).toInstant());

    addUser("A", User::setLastLogin, threeMonthAgo);
    addUser(
        "B",
        credentials -> {
          credentials.setDisabled(true);
          credentials.setLastLogin(Date.from(now.minusMonths(4).plusDays(2).toInstant()));
        });
    addUser("C", User::setLastLogin, twentyTwoDaysAgo);
    addUser("D");

    Map<String, Optional<Locale>> users =
        userService.findNotifiableUsersWithLastLoginBetween(threeMonthAgo, twoMonthsAgo);
    assertEquals(Set.of("emaila"), users.keySet());
    assertEquals(
        Set.of("emaila"),
        userService.findNotifiableUsersWithLastLoginBetween(fourMonthAgo, oneMonthsAgo).keySet());
    assertEquals(
        Set.of("emaila", "emailc"),
        userService
            .findNotifiableUsersWithLastLoginBetween(fourMonthAgo, Date.from(now.toInstant()))
            .keySet());
  }

  @Test
  void testDisableTwoFaWithAdminUser() throws ForbiddenException {
    User userToModify = createAndAddUser("A");
    userService.generateTwoFactorOtpSecretForApproval(userToModify);
    userService.updateUser(userToModify);

    List<ErrorReport> errors = new ArrayList<>();
    userService.privilegedTwoFactorDisable(getAdminUser(), userToModify.getUid(), errors::add);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDisableTwoFaWithManageUser() throws ForbiddenException {
    User userToModify = createAndAddUser("A");
    userService.generateTwoFactorOtpSecretForApproval(userToModify);

    UserGroup userGroupA = createUserGroup('A', Sets.newHashSet(userToModify));
    userGroupService.addUserGroup(userGroupA);

    userToModify.getGroups().add(userGroupA);
    userService.updateUser(userToModify);

    User currentUser = createAndAddUser("B", unitA, "F_USER_ADD_WITHIN_MANAGED_GROUP");
    UserGroup userGroupB = createUserGroup('B', Collections.emptySet());
    userGroupB.addManagedGroup(userGroupA);
    userGroupService.addUserGroup(userGroupB);
    userGroupService.updateUserGroup(userGroupA);

    currentUser.getGroups().add(userGroupB);
    userService.updateUser(currentUser);

    List<ErrorReport> errors = new ArrayList<>();
    userService.privilegedTwoFactorDisable(currentUser, userToModify.getUid(), errors::add);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testGetDisplayNameNull() {
    assertNull(userService.getDisplayName("notExist"));
  }

  @Test
  void testBCryptedPasswordOnInputError() {
    User user = new User();
    user.setFirstName("test");
    user.setSurname("tester");
    user.setUsername("test");
    user.setPassword("password");
    userService.addUser(user);

    String encodedPassword = passwordManager.encode("password");

    assertThrows(
        IllegalArgumentException.class,
        () -> userService.encodeAndSetPassword(user, encodedPassword),
        "Raw password look like BCrypt encoded password, this is most certainly a bug");
  }
}
