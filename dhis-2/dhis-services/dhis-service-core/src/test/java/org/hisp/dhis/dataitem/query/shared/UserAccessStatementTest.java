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
package org.hisp.dhis.dataitem.query.shared;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_GROUP_UIDS;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_UID;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.READ_ACCESS;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.ownerAccessCondition;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.publicAccessCondition;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.sharingConditions;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.userAccessCondition;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.userGroupAccessCondition;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.CHECK_USER_GROUPS_ACCESS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.HAS_USER_GROUP_IDS;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Unit tests for UserAccessStatement.
 *
 * @author maikel arabori
 */
class UserAccessStatementTest {

  @Test
  void testSharingConditionsUsingOneTableAliasWhenGroupUserIdsIsSet() {
    // Given
    final String tableAlias = "t";
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(USER_GROUP_UIDS, "uid-1, uid-2");
    // When
    final String actualStatement = sharingConditions(tableAlias, READ_ACCESS, theParameterSource);
    // Then
    assertThat(actualStatement, containsString(publicAccessCondition(tableAlias, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(tableAlias)));
    assertThat(actualStatement, containsString(userAccessCondition(tableAlias, READ_ACCESS)));
    assertThat(actualStatement, containsString(userGroupAccessCondition(tableAlias, READ_ACCESS)));
  }

  @Test
  void testSharingConditionsUsingOneTableAliasWhenGroupUserIdsIsSetToNull() {
    // Given
    final String tableAlias = "t";
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(USER_GROUP_UIDS, null);
    // When
    final String actualStatement = sharingConditions(tableAlias, READ_ACCESS, theParameterSource);
    // Then
    assertThat(actualStatement, containsString(publicAccessCondition(tableAlias, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(tableAlias)));
    assertThat(actualStatement, containsString(userAccessCondition(tableAlias, READ_ACCESS)));
    assertThat(
        actualStatement, not(containsString(userGroupAccessCondition(tableAlias, READ_ACCESS))));
  }

  @Test
  void testSharingConditionsUsingOneTableAliasWhenGroupUserIdsIsSetToEmpty() {
    // Given
    final String tableAlias = "t";
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(USER_GROUP_UIDS, "");
    // When
    final String actualStatement = sharingConditions(tableAlias, READ_ACCESS, theParameterSource);
    // Then
    assertThat(actualStatement, containsString(publicAccessCondition(tableAlias, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(tableAlias)));
    assertThat(actualStatement, containsString(userAccessCondition(tableAlias, READ_ACCESS)));
    assertThat(
        actualStatement, not(containsString(userGroupAccessCondition(tableAlias, READ_ACCESS))));
  }

  @Test
  void testSharingConditionsUsingOneTableAliasWhenGroupUserIdsIsNotSet() {
    // Given
    final String tableAlias = "t";
    final MapSqlParameterSource noParameterSource = new MapSqlParameterSource();
    // When
    final String actualStatement = sharingConditions(tableAlias, READ_ACCESS, noParameterSource);
    // Then
    assertThat(actualStatement, containsString(publicAccessCondition(tableAlias, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(tableAlias)));
    assertThat(actualStatement, containsString(userAccessCondition(tableAlias, READ_ACCESS)));
    assertThat(
        actualStatement, not(containsString(userGroupAccessCondition(tableAlias, READ_ACCESS))));
  }

  @Test
  void testSharingConditionsUsingTwoTableAliasWhenGroupUserIdsIsSet() {
    // Given
    final String aColumn1 = "anyColumn";
    final String aColumn2 = "otherColumn";
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(USER_GROUP_UIDS, "uid-1, uid-2");
    // When
    final String actualStatement =
        sharingConditions(aColumn1, aColumn2, READ_ACCESS, theParameterSource);
    // Then
    assertThat(actualStatement, containsString(publicAccessCondition(aColumn1, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(aColumn1)));
    assertThat(actualStatement, containsString(userAccessCondition(aColumn1, READ_ACCESS)));
    assertThat(actualStatement, containsString(userGroupAccessCondition(aColumn1, READ_ACCESS)));
    assertThat(actualStatement, containsString(publicAccessCondition(aColumn2, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(aColumn2)));
    assertThat(actualStatement, containsString(userAccessCondition(aColumn2, READ_ACCESS)));
    assertThat(actualStatement, containsString(userGroupAccessCondition(aColumn2, READ_ACCESS)));
  }

  @Test
  void testSharingConditionsUsingTwoTableAliasWhenGroupUserIdsIsSetToNull() {
    // Given
    final String aColumn1 = "anyColumn";
    final String aColumn2 = "otherColumn";
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(USER_GROUP_UIDS, null);
    // When
    final String actualStatement =
        sharingConditions(aColumn1, aColumn2, READ_ACCESS, theParameterSource);
    // Then
    assertThat(actualStatement, containsString(publicAccessCondition(aColumn1, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(aColumn1)));
    assertThat(actualStatement, containsString(userAccessCondition(aColumn1, READ_ACCESS)));
    assertThat(
        actualStatement, not(containsString(userGroupAccessCondition(aColumn1, READ_ACCESS))));
    assertThat(actualStatement, containsString(publicAccessCondition(aColumn2, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(aColumn2)));
    assertThat(actualStatement, containsString(userAccessCondition(aColumn2, READ_ACCESS)));
    assertThat(
        actualStatement, not(containsString(userGroupAccessCondition(aColumn2, READ_ACCESS))));
  }

  @Test
  void testSharingConditionsUsingTwoTableAliasWhenGroupUserIdsIsSetToEmpty() {
    // Given
    String aColumn1 = "anyColumn";
    String aColumn2 = "otherColumn";
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(USER_GROUP_UIDS, "");
    // When
    final String actualStatement =
        sharingConditions(aColumn1, aColumn2, READ_ACCESS, theParameterSource);
    // Then
    assertThat(actualStatement, containsString(publicAccessCondition(aColumn1, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(aColumn1)));
    assertThat(actualStatement, containsString(userAccessCondition(aColumn1, READ_ACCESS)));
    assertThat(
        actualStatement, not(containsString(userGroupAccessCondition(aColumn1, READ_ACCESS))));
    assertThat(actualStatement, containsString(publicAccessCondition(aColumn2, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(aColumn2)));
    assertThat(actualStatement, containsString(userAccessCondition(aColumn2, READ_ACCESS)));
    assertThat(
        actualStatement, not(containsString(userGroupAccessCondition(aColumn2, READ_ACCESS))));
  }

  @Test
  void testSharingConditionsUsingTwoTableAliasWhenGroupUserIdsIsNotSet() {
    // Given
    String aColumn1 = "anyColumn";
    String aColumn2 = "otherColumn";
    final MapSqlParameterSource noParameterSource = new MapSqlParameterSource();
    // When
    final String actualStatement =
        sharingConditions(aColumn1, aColumn2, READ_ACCESS, noParameterSource);
    // Then
    assertThat(actualStatement, containsString(publicAccessCondition(aColumn1, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(aColumn1)));
    assertThat(actualStatement, containsString(userAccessCondition(aColumn1, READ_ACCESS)));
    assertThat(
        actualStatement, not(containsString(userGroupAccessCondition(aColumn1, READ_ACCESS))));
    assertThat(actualStatement, containsString(publicAccessCondition(aColumn2, READ_ACCESS)));
    assertThat(actualStatement, containsString(ownerAccessCondition(aColumn2)));
    assertThat(actualStatement, containsString(userAccessCondition(aColumn2, READ_ACCESS)));
    assertThat(
        actualStatement, not(containsString(userGroupAccessCondition(aColumn2, READ_ACCESS))));
  }

  @Test
  void testOwnerAccessCondition() {
    // Given
    String aColumn = "anyColumn";
    final String expectedStatement =
        "(jsonb_extract_path_text("
            + aColumn
            + ", 'owner') is null or "
            + "jsonb_extract_path_text("
            + aColumn
            + ", 'owner') = 'null' or "
            + "jsonb_extract_path_text("
            + aColumn
            + ", 'owner') = :userUid)";
    // When
    final String actualStatement = ownerAccessCondition(aColumn);
    // Then
    assertThat(actualStatement, is(expectedStatement));
  }

  @Test
  void testOwnerAccessConditionWhenTableAliasIsNull() {
    // Given
    final String nullTableAlias = null;
    // When throws
    final IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> ownerAccessCondition(nullTableAlias));
    // Then
    assertThat(
        thrown.getMessage(), containsString("The argument columnName cannot be null/blank."));
  }

  @Test
  void testPublicAccessCondition() {
    // Given
    String aColumn = "anyColumn";
    final String expectedStatement =
        "(jsonb_extract_path_text("
            + aColumn
            + ", 'public') is null or "
            + "jsonb_extract_path_text("
            + aColumn
            + ", 'public') = 'null' or "
            + "jsonb_extract_path_text("
            + aColumn
            + ", 'public') like 'r%')";
    // When
    final String actualStatement = publicAccessCondition(aColumn, READ_ACCESS);
    // Then
    assertThat(actualStatement, is(expectedStatement));
  }

  @Test
  void testPublicAccessConditionWhenTableAliasIsEmpty() {
    // Given
    final String nullTableAlias = null;
    // When throws
    final IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> publicAccessCondition(nullTableAlias, READ_ACCESS));
    // Then
    assertThat(
        thrown.getMessage(), containsString("The argument columnName cannot be null/blank."));
  }

  @Test
  void testUserAccessCondition() {
    // Given
    String aColumn = "anyColumn";
    final String expectedStatement =
        "(jsonb_has_user_id("
            + aColumn
            + ", :"
            + USER_UID
            + ") = true "
            + " and jsonb_check_user_access("
            + aColumn
            + ", :"
            + USER_UID
            + ", 'r%') = true)";
    // When
    final String actualStatement = userAccessCondition(aColumn, READ_ACCESS);
    // Then
    assertThat(actualStatement, is(expectedStatement));
  }

  @Test
  void testUserAccessConditionWhenTableAliasIsEmpty() {
    // Given
    final String nullTableAlias = null;
    // When throws
    final IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> userAccessCondition(nullTableAlias, READ_ACCESS));
    // Then
    assertThat(
        thrown.getMessage(), containsString("The argument columnName cannot be null/blank."));
  }

  @Test
  void testUserGroupAccessCondition() {
    // Given
    String aColumn = "anyColumn";
    final String expectedStatement =
        "("
            + HAS_USER_GROUP_IDS
            + "("
            + aColumn
            + ", :"
            + USER_GROUP_UIDS
            + ") = true "
            + " and "
            + CHECK_USER_GROUPS_ACCESS
            + "("
            + aColumn
            + ", 'r%', :"
            + USER_GROUP_UIDS
            + ") = true)";
    // When
    final String actualStatement = userGroupAccessCondition(aColumn, READ_ACCESS);
    // Then
    assertThat(actualStatement, is(expectedStatement));
  }

  @Test
  void testUserGroupAccessConditionWhenTableAliasIsBlank() {
    // Given
    final String nullTableAlias = null;
    // When throws
    final IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> userGroupAccessCondition(nullTableAlias, READ_ACCESS));
    // Then
    assertThat(
        thrown.getMessage(), containsString("The argument columnName cannot be null/blank."));
  }
}
