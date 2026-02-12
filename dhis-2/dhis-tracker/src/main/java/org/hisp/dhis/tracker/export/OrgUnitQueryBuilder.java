/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.UserDetails;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Utility class for constructing SQL query clauses related to org units.
 *
 * <p>This class centralizes the logic for generating SQL conditions and parameter mappings that
 * filter query results based on org unit mode and access controls.
 *
 * <p>Additionally, this class provides methods to build ownership related query clauses that
 * enforce access control constraints based on the org unit hierarchy and program access levels.
 * These clauses ensure that only data accessible to the current user and within the defined org
 * scope is included in query results.
 *
 * <p>The class is designed to be used in constructing dynamic SQL queries where org unit filtering
 * and ownership constraints are required. It manages the injection of SQL fragments and named
 * parameters consistently to facilitate secure and maintainable query building.
 *
 * <p>This class is non-instantiable and exposes all functionality through static methods.
 */
@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class OrgUnitQueryBuilder {

  /** Appends an SQL clause to filter by org units based on the given org unit mode. */
  public static void buildOrgUnitModeClause(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      Set<OrganisationUnit> orgUnits,
      OrganisationUnitSelectionMode orgUnitMode,
      String tableAlias,
      String clause) {

    if (orgUnitModeDoesNotAcceptOrgUnitParams(orgUnitMode)) {
      return;
    }

    sql.append(clause);

    switch (orgUnitMode) {
      case DESCENDANTS -> addOrgUnitDescendantsCondition(sql, orgUnits, sqlParameters, tableAlias);
      case CHILDREN -> addOrgUnitsChildrenCondition(sql, orgUnits, sqlParameters, tableAlias);
      case SELECTED -> {
        sql.append(tableAlias).append(".organisationunitid in (:orgUnits) ");
        sqlParameters.addValue("orgUnits", getIdentifiers(orgUnits));
      }
      default -> throw new IllegalArgumentException("Unknown org unit mode: " + orgUnitMode);
    }
  }

  private static boolean orgUnitModeDoesNotAcceptOrgUnitParams(
      OrganisationUnitSelectionMode orgUnitMode) {
    return orgUnitMode == ALL || orgUnitMode == CAPTURE || orgUnitMode == ACCESSIBLE;
  }

  /**
   * Appends an SQL clause to enforce program ownership and access level restrictions based on the
   * user scopes and org unit mode. The program's access level is resolved at query execution time
   * via the given program table alias.
   */
  public static void buildOwnershipClause(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      OrganisationUnitSelectionMode orgUnitMode,
      String programTableAlias,
      String orgUnitTableAlias,
      String trackedEntityTableAlias,
      Supplier<String> clauseSupplier) {
    UserDetails userDetails = getCurrentUserDetails();

    if (orgUnitMode == ALL || userDetails.isSuper()) {
      return;
    }

    sql.append(clauseSupplier.get())
        .append("((")
        .append(programTableAlias)
        .append(".accesslevel in ('OPEN', 'AUDITED') and ");
    addScopePathPredicate(sql, sqlParameters, orgUnitMode, orgUnitTableAlias, userDetails);
    sql.append(")");

    sql.append(" or (")
        .append(programTableAlias)
        .append(".accesslevel in ('PROTECTED', 'CLOSED') and ");
    addCaptureScopePathPredicate(sql, sqlParameters, orgUnitTableAlias, userDetails);
    sql.append(")");

    sql.append(" or (").append(programTableAlias).append(".accesslevel = 'PROTECTED' and ");
    addTempOwnerPredicate(
        sql, trackedEntityTableAlias, programTableAlias + ".programid", userDetails.getId());
    sql.append("))");
  }

  /**
   * Appends an SQL clause to enforce program ownership and access level restrictions when the
   * program is known at query build time. This eliminates the need to join the program table. Only
   * the branches relevant to the program's access level are emitted. Uses literal path prefixes
   * resolved from the given {@link SearchScope} for index-friendly LIKE predicates.
   *
   * <p>Does nothing when the given scope is {@linkplain SearchScope#restricted() unrestricted}.
   */
  public static void buildOwnershipClause(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      Program program,
      SearchScope searchScope,
      String orgUnitTableAlias,
      String trackedEntityTableAlias,
      Supplier<String> clauseSupplier) {
    if (!searchScope.restricted()) {
      return;
    }

    AccessLevel accessLevel = program.getAccessLevel();

    sql.append(clauseSupplier.get()).append("(");
    addPathPrefixPredicate(
        sql, sqlParameters, searchScope.forAccessLevel(accessLevel), orgUnitTableAlias, "scope");

    if (accessLevel == AccessLevel.PROTECTED) {
      sql.append(" or ");
      addTempOwnerPredicate(
          sql, trackedEntityTableAlias, String.valueOf(program.getId()), searchScope.userId());
    }

    sql.append(")");
  }

  /**
   * Appends a path-prefix predicate using the user's search or capture scope depending on the org
   * unit mode. Uses the capture scope when the mode is {@code CAPTURE}, otherwise the effective
   * search scope.
   */
  private static void addScopePathPredicate(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      OrganisationUnitSelectionMode orgUnitMode,
      String orgUnitTableAlias,
      UserDetails userDetails) {
    if (orgUnitMode == CAPTURE) {
      addCaptureScopePathPredicate(sql, sqlParameters, orgUnitTableAlias, userDetails);
    } else {
      sql.append(orgUnitTableAlias)
          .append(
              ".path like any (select concat(o.path, '%') from organisationunit o where o.uid in (:effectiveSearchScopeOrgUnits))");
      sqlParameters.addValue(
          "effectiveSearchScopeOrgUnits", userDetails.getUserEffectiveSearchOrgUnitIds());
    }
  }

  /** Appends a path-prefix predicate using the user's capture scope org units. */
  private static void addCaptureScopePathPredicate(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      String orgUnitTableAlias,
      UserDetails userDetails) {
    sql.append(orgUnitTableAlias)
        .append(
            ".path like any (select concat(o.path, '%') from organisationunit o where o.uid in (:captureScopeOrgUnits))");
    sqlParameters.addValue("captureScopeOrgUnits", userDetails.getUserOrgUnitIds());
  }

  /**
   * Appends a path-prefix predicate using literal path values from the given org unit set. Emits
   * {@code (alias.path like :prefix0 or alias.path like :prefix1 ...)} with each parameter bound to
   * {@code orgUnit.getStoredPath() + "%"}.
   */
  private static void addPathPrefixPredicate(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      Set<OrganisationUnit> scopeOrgUnits,
      String orgUnitTableAlias,
      String paramPrefix) {
    if (scopeOrgUnits.isEmpty()) {
      sql.append("false");
      return;
    }

    SqlHelper orHlp = new SqlHelper(true);
    sql.append("(");
    int index = 0;
    for (OrganisationUnit orgUnit : scopeOrgUnits) {
      String paramName = paramPrefix + "Path" + index;
      sql.append(orHlp.or()).append(orgUnitTableAlias).append(".path like :").append(paramName);
      sqlParameters.addValue(paramName, orgUnit.getStoredPath() + "%");
      index++;
    }
    sql.append(")");
  }

  /**
   * Appends a temporary ownership EXISTS predicate. The {@code programIdExpression} can be a table
   * column reference (e.g. {@code "p.programid"}) or a literal ID value.
   */
  private static void addTempOwnerPredicate(
      StringBuilder sql, String trackedEntityTableAlias, String programIdExpression, long userId) {
    sql.append("exists (select 1 from programtempowner where programid = ")
        .append(programIdExpression)
        .append(" and trackedentityid = ")
        .append(trackedEntityTableAlias)
        .append(".trackedentityid and userid = ")
        .append(userId)
        .append(" and extract(epoch from validtill)-extract (epoch from now()::timestamp) > 0)");
  }

  private static void addOrgUnitDescendantsCondition(
      StringBuilder sql,
      Set<OrganisationUnit> orgUnits,
      MapSqlParameterSource sqlParameters,
      String tableAlias) {
    addPathPrefixPredicate(sql, sqlParameters, orgUnits, tableAlias, "orgUnit");
  }

  private static void addOrgUnitsChildrenCondition(
      StringBuilder sql,
      Set<OrganisationUnit> orgUnits,
      MapSqlParameterSource sqlParameters,
      String tableAlias) {
    SqlHelper orHlp = new SqlHelper(true);

    sql.append("(");
    int index = 0;
    for (OrganisationUnit organisationUnit : orgUnits) {
      String pathParamName = "orgUnitPath" + index;
      String parentHierarchyParamName = "parentHierarchyLevel" + index;
      String childHierarchyParamName = "childHierarchyLevel" + index;

      String pathParamValue = organisationUnit.getStoredPath() + "%";
      int parentHierarchyParamValue = organisationUnit.getHierarchyLevel();

      sql.append(orHlp.or())
          .append(" ")
          .append(tableAlias)
          .append(".path like :")
          .append(pathParamName)
          .append(" and (")
          .append(tableAlias)
          .append(".hierarchylevel = :")
          .append(parentHierarchyParamName)
          .append(" or ")
          .append(tableAlias)
          .append(".hierarchylevel = :")
          .append(childHierarchyParamName)
          .append(")");

      sqlParameters.addValue(pathParamName, pathParamValue);
      sqlParameters.addValue(parentHierarchyParamName, parentHierarchyParamValue);
      sqlParameters.addValue(childHierarchyParamName, parentHierarchyParamValue + 1);
      index++;
    }
    sql.append(")");
  }
}
