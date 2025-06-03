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
package org.hisp.dhis.user;

import static io.hypersistence.utils.jdbc.validator.SQLStatementCountValidator.assertSelectCount;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.hypersistence.utils.jdbc.validator.SQLStatementCountValidator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Nguyen Hong Duc
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class UserStoreTest extends PostgresIntegrationTestBase {
  public static final String AUTH_A = "AuthA";

  public static final String AUTH_B = "AuthB";

  public static final String AUTH_C = "AuthC";

  public static final String AUTH_D = "AuthD";

  @Autowired private UserStore userStore;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private UserGroupService userGroupService;

  @Autowired private DbmsManager dbmsManager;

  private OrganisationUnit unit1;

  private OrganisationUnit unit2;

  private UserRole roleA;

  private UserRole roleB;

  private UserRole roleC;

  @BeforeAll
  void setUp() {
    unit1 = createOrganisationUnit('A');
    unit2 = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(unit1);
    organisationUnitService.addOrganisationUnit(unit2);

    roleA = createUserRole('A');
    roleB = createUserRole('B');
    roleC = createUserRole('C');
    roleA.getAuthorities().add(AUTH_A);
    roleA.getAuthorities().add(AUTH_B);
    roleA.getAuthorities().add(AUTH_C);
    roleA.getAuthorities().add(AUTH_D);
    roleB.getAuthorities().add(AUTH_A);
    roleB.getAuthorities().add(AUTH_B);
    roleC.getAuthorities().add(AUTH_C);
    userService.addUserRole(roleA);
    userService.addUserRole(roleB);
    userService.addUserRole(roleC);
  }

  @Test
  void testAddGetUser() {
    Set<OrganisationUnit> units = new HashSet<>();
    units.add(unit1);
    units.add(unit2);
    User userA = makeUser("A");
    User userB = makeUser("B");
    userA.setOrganisationUnits(units);
    userB.setOrganisationUnits(units);
    userStore.save(userA);
    long idA = userA.getId();
    userStore.save(userB);
    long idB = userB.getId();
    assertEquals(userA, userStore.get(idA));
    assertEquals(userB, userStore.get(idB));
    assertEquals(units, userStore.get(idA).getOrganisationUnits());
    assertEquals(units, userStore.get(idB).getOrganisationUnits());
  }

  @Test
  void testUpdateUser() {
    User userA = makeUser("A");
    User userB = makeUser("B");
    userStore.save(userA);
    long idA = userA.getId();
    userStore.save(userB);
    long idB = userB.getId();
    assertEquals(userA, userStore.get(idA));
    assertEquals(userB, userStore.get(idB));
    userA.setSurname("UpdatedSurnameA");
    userStore.update(userA);
    assertEquals(userStore.get(idA).getSurname(), "UpdatedSurnameA");
  }

  @Test
  void testDeleteUser() {
    User userA = makeUser("A");
    User userB = makeUser("B");
    userStore.save(userA);
    long idA = userA.getId();
    userStore.save(userB);
    long idB = userB.getId();
    assertEquals(userA, userStore.get(idA));
    assertEquals(userB, userStore.get(idB));
    userStore.delete(userA);
    assertNull(userStore.get(idA));
    assertNotNull(userStore.get(idB));
  }

  @Test
  void testGetCurrentUserGroupInfo() {
    UserGroup userGroupA = createUserGroup('A', null);
    userGroupService.addUserGroup(userGroupA);

    UserGroup userGroupB = createUserGroup('B', null);
    userGroupService.addUserGroup(userGroupB);

    User userA = makeUser("A");
    userA.setGroups(Set.of(userGroupA, userGroupB));
    userGroupA.setMembers(Set.of(userA));
    userGroupB.setMembers(Set.of(userA));
    userStore.save(userA);

    assertEquals(
        userA.getGroups().size(),
        userStore.getCurrentUserGroupInfo(userA.getUid()).getUserGroupUIDs().size());
  }

  @Test
  void testGetCurrentUserGroupInfoWithoutGroup() {
    User userA = makeUser("A");
    userStore.save(userA);
    // TODO: MAS this should be refactored out
    CurrentUserGroupInfo currentUserGroupInfo = userStore.getCurrentUserGroupInfo(userA.getUid());
    assertNotNull(currentUserGroupInfo);
    assertEquals(0, currentUserGroupInfo.getUserGroupUIDs().size());
    assertEquals(userA.getUid(), currentUserGroupInfo.getUserUID());
  }

  @Test
  void testGetDisplayName() {
    User userA = makeUser("A");
    userStore.save(userA);
    dbmsManager.flushSession();
    assertEquals("FirstNameA SurnameA", userStore.getDisplayName(userA.getUid()));
  }

  @Test
  void testAddGetUserTwo() {
    User userA = makeUser("A");
    User userB = makeUser("B");
    userStore.save(userA);
    long idA = userA.getId();
    userStore.save(userB);
    long idB = userB.getId();
    assertEquals(userA, userStore.get(idA));
    assertEquals(userB, userStore.get(idB));
  }

  @Test
  void testGetUserByUuid() {
    User userA = makeUser("A");
    User userB = makeUser("B");
    userStore.save(userA);
    userStore.save(userB);

    UUID uuidA = userA.getUuid();
    UUID uuidB = userB.getUuid();
    User ucA = userStore.getUserByUuid(uuidA);
    User ucB = userStore.getUserByUuid(uuidB);
    assertNotNull(ucA);
    assertNotNull(ucB);
    assertEquals(uuidA, ucA.getUuid());
    assertEquals(uuidB, ucB.getUuid());
  }

  @Test
  void testGetUserWithAuthority() {
    User userA = addUser("A", roleA);
    User userB = addUser("B", roleB, roleC);
    List<User> usersWithAuthorityA = userService.getUsersWithAuthority(AUTH_D);
    assertTrue(usersWithAuthorityA.contains(userA));
    List<User> usersWithAuthorityB = userService.getUsersWithAuthority(AUTH_D);
    assertFalse(usersWithAuthorityB.contains(userB));
  }

  @Test
  void testGetActiveUserGroupUserEmailsByUsername() {
    User userA = makeUser("A");
    userA.setDisabled(true);
    User userB = makeUser("B");
    userStore.save(userA);
    userStore.save(userB);

    UserGroup group = createUserGroup('A', Set.of(userA, userB));
    userGroupService.addUserGroup(group);

    Map<String, String> emailsByUsername =
        userStore.getActiveUserGroupUserEmailsByUsername(group.getUid());

    assertNotEquals(Map.of("usernamea", "emaila", "usernameb", "emailb"), emailsByUsername);
    assertEquals(Map.of("usernameb", "emailb"), emailsByUsername);
  }

  @Test
  void testGetUserByOpenId() {
    String openId1 = "ABC";
    String openId2 = "DEF";

    User userA = makeUser("A");
    User userB = makeUser("B");
    User userC = makeUser("C");
    User userD = makeUser("D");
    User userE = makeUser("E");

    userA.setOpenId(openId1);
    userB.setOpenId(openId1);
    userC.setOpenId(openId1);
    userD.setOpenId(openId1);
    userE.setOpenId(openId2);

    userA.setLastLogin(parseDate("2024-07-01"));
    userB.setLastLogin(parseDate("2024-07-02"));
    userC.setLastLogin(parseDate("2024-07-03"));
    userD.setLastLogin(null);
    userE.setLastLogin(parseDate("2024-07-04"));

    userC.setDisabled(true);

    userStore.save(userA);
    userStore.save(userB);
    userStore.save(userC);
    userStore.save(userD);
    userStore.save(userE);

    User foundUser = userStore.getUserByOpenId(openId1);
    assertEquals(userB.getUid(), foundUser.getUid());
  }

  @Test
  @DisplayName("Get users by org unit uid with expected select count")
  void getUsersByOrgUnitUidExpectedSelectCountTest() {
    // given 2 org units & 4 users
    OrganisationUnit ou1 = createOrganisationUnit("org unit test 1");
    OrganisationUnit ou2 = createOrganisationUnit("org unit test 2");
    organisationUnitService.addOrganisationUnit(ou1);
    organisationUnitService.addOrganisationUnit(ou2);

    User user1 = createAndAddUser("user1 test", ou1);
    User user2 = createAndAddUser("user2 test", ou1);
    User user3 = createAndAddUser("user3 test", ou2);
    User user4 = createAndAddUser("user4 test no orgs");
    userService.addUser(user1);
    userService.addUser(user2);
    userService.addUser(user3);
    userService.addUser(user4);

    // when retrieving users by org unit uid
    SQLStatementCountValidator.reset();
    List<User> users =
        userStore.getUsersWithOrgUnit(UserOrgUnitProperty.ORG_UNITS, Set.of(UID.of(ou1)));
    // getting each org unit to assert later that no other select queries triggered
    users.forEach(
        u ->
            assertTrue(
                u.getOrganisationUnits().stream()
                    .allMatch(ou -> ou.getUid().equals(ou1.getUid()))));

    // then only 1 select query is triggered
    assertSelectCount(1);
    assertEquals(2, users.size());
  }
}
