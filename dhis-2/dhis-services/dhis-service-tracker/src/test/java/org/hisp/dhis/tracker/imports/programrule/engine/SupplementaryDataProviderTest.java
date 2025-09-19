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
package org.hisp.dhis.tracker.imports.programrule.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplementaryDataProviderTest extends TestBase {

  private static final String ORG_UNIT_GROUP_UID = "OrgUnitGroupId";
  private static final String USER_GROUP_UID = "UserGroupId";

  private static final String NOT_NEEDED_ORG_UNIT_GROUP_UID = "NotNeededOrgUnitGroupId";

  @Mock private OrganisationUnitGroupService organisationUnitGroupService;

  @Mock private UserService userService;

  @Mock private UserGroupService userGroupService;

  @InjectMocks private SupplementaryDataProvider providerToTest;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private OrganisationUnitGroup orgUnitGroup;

  private UserGroup userGroupA;

  private UserDetails currentUser;

  @BeforeEach
  void setUp() {
    User user = makeUser("A");
    user.setUsername("A");
    user.setUserRoles(getUserRoles());

    userGroupA = createUserGroup('G', Set.of(user));
    userGroupA.setUid(USER_GROUP_UID);

    user.getGroups().add(userGroupA);
    currentUser = UserDetails.fromUser(user);

    orgUnitA = createOrganisationUnit('A');
    orgUnitB = createOrganisationUnit('B');
    orgUnitGroup = createOrganisationUnitGroup('A');
    orgUnitGroup.setUid(ORG_UNIT_GROUP_UID);
    orgUnitGroup.setMembers(Sets.newHashSet(orgUnitA));
    OrganisationUnitGroup notNeededOrgUnitGroup = createOrganisationUnitGroup('B');
    notNeededOrgUnitGroup.setUid(NOT_NEEDED_ORG_UNIT_GROUP_UID);
    notNeededOrgUnitGroup.setMembers(Sets.newHashSet(orgUnitB));
  }

  @Test
  void getUserRolesSupplementaryData() {
    when(organisationUnitGroupService.getOrganisationUnitGroup(ORG_UNIT_GROUP_UID))
        .thenReturn(orgUnitGroup);
    Map<String, List<String>> supplementaryData =
        providerToTest.getSupplementaryData(
            getProgramRule('C', "d2:inOrgUnitGroup('OrgUnitGroupId')"), currentUser);
    assertFalse(supplementaryData.isEmpty());
    assertEquals(getUserRoleUids(), Set.copyOf(supplementaryData.get("USER_ROLES")));
    assertFalse(supplementaryData.get(ORG_UNIT_GROUP_UID).isEmpty());
    assertEquals(orgUnitA.getUid(), supplementaryData.get(ORG_UNIT_GROUP_UID).get(0));
    assertNull(supplementaryData.get(NOT_NEEDED_ORG_UNIT_GROUP_UID));
  }

  @Test
  void getUserGroupsSupplementaryData() {
    Map<String, List<String>> supplementaryData =
        providerToTest.getSupplementaryData(
            getProgramRule('D', "d2:inUserGroup('UserGroupId')"), currentUser);
    assertFalse(supplementaryData.isEmpty());
    assertFalse(supplementaryData.get("USER_GROUPS").isEmpty());
    assertTrue(supplementaryData.get("USER_GROUPS").contains(userGroupA.getUid()));
  }

  private List<ProgramRule> getProgramRule(char ch, String condition) {
    ProgramRule programRule = createProgramRule(ch, null);
    programRule.setCondition(condition);
    return Lists.newArrayList(programRule);
  }

  private Set<String> getUserRoleUids() {
    return Set.copyOf(getUserRoles().stream().map(UserRole::getUid).toList());
  }

  private Set<UserRole> getUserRoles() {
    UserRole groupA = createUserRole('A');
    UserRole groupB = createUserRole('B');
    return Sets.newHashSet(groupA, groupB);
  }
}
