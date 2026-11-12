/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.association;

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.CHECK_USER_ACCESS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.CHECK_USER_GROUPS_ACCESS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.EXTRACT_PATH_TEXT;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.HAS_USER_GROUP_IDS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.HAS_USER_ID;
import static org.hisp.dhis.security.acl.AclService.LIKE_READ_METADATA;
import static org.hisp.dhis.system.util.SqlUtils.singleQuote;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;

@RequiredArgsConstructor
public abstract class AbstractOrganisationUnitAssociationsQueryBuilder {
  private static final String OUTER_QUERY =
      """
      select inner_query_alias.uid, inner_query_alias.agg_ou_uid
      from (
      ${innerQuery}
      ) as inner_query_alias""";

  private static final String INNER_QUERY =
      """
      select base_table_alias.uid, base_table_alias.sharing, array_agg(ou.uid) agg_ou_uid
      from ${baseTable} base_table_alias
      ${orgUnitJoin}
      where base_table_alias.uid in (${uids})
      ${orgUnitFilter}
      group by base_table_alias.uid, base_table_alias.sharing""";

  /** Joins all associated org units. */
  private static final String ORG_UNIT_JOIN =
      """
      left join ${relationshipTable} relationship_table_alias
        on base_table_alias.${joinColumn} = relationship_table_alias.${joinColumn}
      left join organisationunit ou
        on relationship_table_alias.${orgUnitJoinColumn} = ou.organisationunitid""";

  /**
   * Joins only the associated org units within the user hierarchy. The hierarchy condition is part
   * of the join, rather than a filter applied after an outer join, so that Postgres can start from
   * the user's subtree via the GIN index on {@code patharray} instead of joining every associated
   * org unit and filtering afterwards.
   */
  private static final String USER_HIERARCHY_ORG_UNIT_JOIN =
      """
      left join (
        ${relationshipTable} relationship_table_alias
        join organisationunit ou
          on relationship_table_alias.${orgUnitJoinColumn} = ou.organisationunitid
          and ${userHierarchyCondition}
      ) on base_table_alias.${joinColumn} = relationship_table_alias.${joinColumn}""";

  /**
   * Keeps objects with at least one associated org unit within the user hierarchy, and objects
   * without any associated org units (returned with a single null org unit).
   */
  private static final String USER_HIERARCHY_OR_NO_ORG_UNITS_FILTER =
      """
      and (
        ou.organisationunitid is not null
        or not exists (
          select 1 from ${relationshipTable} relationship_table_alias
          where base_table_alias.${joinColumn} = relationship_table_alias.${joinColumn}))""";

  protected abstract String getRelationshipTableName();

  protected abstract String getOrgUnitJoinColumnName();

  protected abstract String getJoinColumnName();

  protected abstract String getBaseTableName();

  public String buildSqlQuery(Set<String> uids, Set<String> userOrgUnitPaths, User currentUser) {
    String sql =
        replace(OUTER_QUERY, "innerQuery", innerQuery(uids, userOrgUnitPaths, currentUser));
    if (nonSuperUser(currentUser)) {
      return sql + "\nwhere " + getSharingConditions(LIKE_READ_METADATA);
    }
    return sql;
  }

  public String buildSqlQueryForRawAssociation(Set<String> uids) {
    return replace(OUTER_QUERY, "innerQuery", innerQuery(uids, null, null));
  }

  private String innerQuery(Set<String> uids, Set<String> userOrgUnitPaths, User currentUser) {
    boolean restrictToUserHierarchy = nonSuperUser(currentUser);
    Map<String, String> joinVariables =
        Map.of(
            "relationshipTable", getRelationshipTableName(),
            "joinColumn", getJoinColumnName(),
            "orgUnitJoinColumn", getOrgUnitJoinColumnName(),
            "userHierarchyCondition", getUserHierarchyCondition(userOrgUnitPaths));
    String orgUnitJoin =
        replace(
            restrictToUserHierarchy ? USER_HIERARCHY_ORG_UNIT_JOIN : ORG_UNIT_JOIN, joinVariables);
    String orgUnitFilter =
        restrictToUserHierarchy
            ? replace(USER_HIERARCHY_OR_NO_ORG_UNITS_FILTER, joinVariables)
            : "";

    return replace(
        INNER_QUERY,
        Map.of(
            "baseTable",
            getBaseTableName(),
            "orgUnitJoin",
            orgUnitJoin,
            "orgUnitFilter",
            orgUnitFilter,
            "uids",
            uids.stream().map(SqlUtils::singleQuote).collect(joining(","))));
  }

  private String getSharingConditions(String access) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    Set<String> userGroupIds = currentUser.getUserGroupIds();
    return String.join(
        " or ",
        getOwnerCondition(currentUser.getUid()),
        getPublicSharingCondition(access),
        getUserGroupAccessCondition(userGroupIds, access),
        getUserAccessCondition(currentUser.getUid(), access));
  }

  private String getOwnerCondition(String userUid) {
    return String.join(
        " or ",
        jsonbFunction(EXTRACT_PATH_TEXT, "owner") + " = " + singleQuote(userUid),
        jsonbFunction(EXTRACT_PATH_TEXT, "owner") + " is null");
  }

  private String getPublicSharingCondition(String access) {
    return String.join(
        " or ",
        jsonbFunction(EXTRACT_PATH_TEXT, "public") + " like " + singleQuote(access),
        jsonbFunction(EXTRACT_PATH_TEXT, "public") + " is null");
  }

  private String getUserAccessCondition(String userUid, String access) {
    return Stream.of(
            jsonbFunction(HAS_USER_ID, userUid), jsonbFunction(CHECK_USER_ACCESS, userUid, access))
        .collect(joining(" and ", "(", ")"));
  }

  private String getUserGroupAccessCondition(Set<String> userGroupIds, String access) {
    if (CollectionUtils.isEmpty(userGroupIds)) {
      return "1=0";
    }
    String groupUids = "{" + String.join(",", userGroupIds) + "}";
    return Stream.of(
            jsonbFunction(HAS_USER_GROUP_IDS, groupUids),
            jsonbFunction(CHECK_USER_GROUPS_ACCESS, access, groupUids))
        .collect(joining(" and ", "(", ")"));
  }

  private String jsonbFunction(String functionName, String... params) {
    return String.join(
        "",
        functionName,
        "(",
        String.join(
            ",",
            "inner_query_alias.sharing",
            Arrays.stream(params).map(SqlUtils::singleQuote).collect(joining(","))),
        ")");
  }

  private boolean nonSuperUser(User currentUser) {
    return Objects.nonNull(currentUser) && !currentUser.isSuper();
  }

  /**
   * Descendant-or-self test for the user org units: the {@code patharray} of an org unit contains
   * the UID of each of its ancestors, so it overlaps the user org unit UIDs exactly when it is
   * within the user hierarchy. Only the UID of each user org unit is needed, not its full path.
   */
  private String getUserHierarchyCondition(Set<String> userOrgUnitPaths) {
    if (CollectionUtils.isEmpty(userOrgUnitPaths)) {
      return "false";
    }
    String userOrgUnitUids =
        userOrgUnitPaths.stream()
            .map(path -> path.substring(path.lastIndexOf('/') + 1))
            .map(SqlUtils::singleQuote)
            .collect(joining(","));
    return replace("ou.patharray && ARRAY[${uids}]::varchar[]", "uids", userOrgUnitUids);
  }
}
