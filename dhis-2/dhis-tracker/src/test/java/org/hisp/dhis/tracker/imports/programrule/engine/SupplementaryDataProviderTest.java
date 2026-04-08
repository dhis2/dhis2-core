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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import com.google.common.collect.Sets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import org.hisp.dhis.rules.api.RuleSupplementaryData;
import org.hisp.dhis.tracker.test.TrackerTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class SupplementaryDataProviderTest extends TrackerTestBase {

  private static final String ORG_UNIT_GROUP_UID = "OrgUnitGroupId";
  private static final String ORG_UNIT_GROUP_NAME = "OrganisationUnitGroupA";
  private static final String ORG_UNIT_UID = "OrgUnitIdAAA";
  private static final String USER_GROUP_UID = "UserGroupId";
  private static final String NOT_NEEDED_ORG_UNIT_GROUP_UID = "NotNeededOrgUnitGroupId";

  @Mock private NamedParameterJdbcTemplate namedJdbcTemplate;

  private SupplementaryDataProvider providerToTest;

  private UserGroup userGroupA;

  private UserDetails currentUser;

  @BeforeEach
  void setUp() {
    providerToTest = new SupplementaryDataProvider(namedJdbcTemplate);

    User user = makeUser("A");
    user.setUsername("A");
    user.setUserRoles(getUserRoles());

    userGroupA = createUserGroup('G', Set.of(user));
    userGroupA.setUid(USER_GROUP_UID);

    user.getGroups().add(userGroupA);
    currentUser = UserDetails.fromUser(user);
  }

  @Test
  void shouldReturnEmtpyOrgUnitGroupIfGroupDoesNotExist() {
    String unknownOrgUnitGroup = "UnknownOrgUnitGroup";
    mockQueryRows(/* no rows */ );
    RuleSupplementaryData supplementaryData =
        providerToTest.getSupplementaryData(Set.of(unknownOrgUnitGroup), currentUser);
    assertFalse(supplementaryData.getOrgUnitGroups().isEmpty());
    assertTrue(supplementaryData.getOrgUnitGroups().get(unknownOrgUnitGroup).isEmpty());
  }

  @Test
  void shouldReturnOrgUnitGroupMembersWhenLookedUpByUid() {
    mockQueryRows(new String[] {ORG_UNIT_GROUP_UID, ORG_UNIT_GROUP_NAME, ORG_UNIT_UID});
    RuleSupplementaryData supplementaryData =
        providerToTest.getSupplementaryData(Set.of(ORG_UNIT_GROUP_UID), currentUser);
    assertFalse(supplementaryData.getOrgUnitGroups().isEmpty());
    assertTrue(supplementaryData.getOrgUnitGroups().get(ORG_UNIT_GROUP_UID).contains(ORG_UNIT_UID));
    assertNull(supplementaryData.getOrgUnitGroups().get(NOT_NEEDED_ORG_UNIT_GROUP_UID));
  }

  @Test
  void shouldReturnOrgUnitGroupMembersWhenLookedUpByName() {
    mockQueryRows(new String[] {ORG_UNIT_GROUP_UID, ORG_UNIT_GROUP_NAME, ORG_UNIT_UID});
    RuleSupplementaryData supplementaryData =
        providerToTest.getSupplementaryData(Set.of(ORG_UNIT_GROUP_NAME), currentUser);
    assertFalse(supplementaryData.getOrgUnitGroups().isEmpty());
    assertTrue(
        supplementaryData.getOrgUnitGroups().get(ORG_UNIT_GROUP_NAME).contains(ORG_UNIT_UID));
  }

  @Test
  void getUserGroupsSupplementaryData() {
    RuleSupplementaryData supplementaryData =
        providerToTest.getSupplementaryData(Set.of(), currentUser);
    assertFalse(supplementaryData.getUserGroups().isEmpty());
    assertTrue(supplementaryData.getUserGroups().contains(userGroupA.getUid()));
  }

  /**
   * Stubs the JDBC template to invoke the {@link RowCallbackHandler} once per supplied row. Each
   * row is an array of three strings: {@code [uid, name, ou_uid]}.
   */
  private void mockQueryRows(String[]... rows) {
    doAnswer(
            inv -> {
              RowCallbackHandler handler = inv.getArgument(2);
              for (String[] row : rows) {
                ResultSet rs = mockResultSet(row[0], row[1], row[2]);
                handler.processRow(rs);
              }
              return null;
            })
        .when(namedJdbcTemplate)
        .query(anyString(), any(SqlParameterSource.class), any(RowCallbackHandler.class));
  }

  private ResultSet mockResultSet(String uid, String name, String ouUid) throws SQLException {
    ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
    org.mockito.Mockito.when(rs.getString("uid")).thenReturn(uid);
    org.mockito.Mockito.when(rs.getString("name")).thenReturn(name);
    org.mockito.Mockito.when(rs.getString("ou_uid")).thenReturn(ouUid);
    return rs;
  }

  private Set<UserRole> getUserRoles() {
    UserRole groupA = createUserRole('A');
    UserRole groupB = createUserRole('B');
    return Sets.newHashSet(groupA, groupB);
  }
}
