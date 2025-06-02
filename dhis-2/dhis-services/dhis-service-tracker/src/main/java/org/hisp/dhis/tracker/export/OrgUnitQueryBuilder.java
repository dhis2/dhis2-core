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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.organisationunit.OrganisationUnit;
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
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OrgUnitQueryBuilder {

  /** Appends an SQL clause to filter by org units based on the given org unit mode. */
  public static void buildOrgUnitModeClause(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      Set<OrganisationUnit> orgUnits,
      OrganisationUnitSelectionMode orgUnitMode,
      String tableAlias) {

    switch (orgUnitMode) {
      case DESCENDANTS -> addOrgUnitDescendantsCondition(sql, orgUnits, sqlParameters, tableAlias);
      case CHILDREN -> addOrgUnitsChildrenCondition(sql, orgUnits, sqlParameters, tableAlias);
      case SELECTED -> {
        sql.append(tableAlias).append(".organisationunitid in (:orgUnits) ");
        sqlParameters.addValue("orgUnits", getIdentifiers(orgUnits));
      }
      case ALL, CAPTURE, ACCESSIBLE -> {
        // these modes don't accept org units, so skip predicate
      }
    }
  }

  /**
   * Appends an SQL clause to enforce program ownership and access level restrictions based on the
   * user scopes and org unit mode.
   */
  public static void buildOwnershipClause(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      OrganisationUnitSelectionMode orgUnitMode,
      Set<OrganisationUnit> effectiveSearchOrgUnits,
      Set<OrganisationUnit> captureScopeOrgUnits,
      String programTableAlias,
      String orgUnitTableAlias) {
    if (orgUnitMode == ALL || getCurrentUserDetails().isSuper()) {
      return;
    }

    SqlHelper sqlHelper = new SqlHelper(true);

    sql.append(sqlHelper.andOr())
        .append("((")
        .append(programTableAlias)
        .append(".accesslevel in ('OPEN', 'AUDITED') and ")
        .append(orgUnitTableAlias);
    if (orgUnitMode == CAPTURE) {
      sql.append(".path like any (:captureScopePaths))");
    } else {
      sql.append(".path like any (:effectiveSearchScopePaths))");
      sqlParameters.addValue(
          "effectiveSearchScopePaths", getOrgUnitsPathArray(effectiveSearchOrgUnits));
    }

    sql.append(sqlHelper.andOr())
        .append("(")
        .append(programTableAlias)
        .append(".accesslevel in ('PROTECTED', 'CLOSED') and ")
        .append(orgUnitTableAlias)
        .append(".path like any (:captureScopePaths)))");
    sqlParameters.addValue("captureScopePaths", getOrgUnitsPathArray(captureScopeOrgUnits));
  }

  private static void addOrgUnitDescendantsCondition(
      StringBuilder sql,
      Set<OrganisationUnit> orgUnits,
      MapSqlParameterSource sqlParameters,
      String tableAlias) {
    SqlHelper orHlp = new SqlHelper(true);

    sql.append("(");
    int index = 0;
    for (OrganisationUnit organisationUnit : orgUnits) {
      String paramName = "orgUnitPath" + index;
      String paramValue = organisationUnit.getStoredPath() + "%";

      sql.append(orHlp.or()).append(tableAlias).append(".path like :").append(paramName);

      sqlParameters.addValue(paramName, paramValue);
      index++;
    }
    sql.append(")");
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

  private static String[] getOrgUnitsPathArray(Set<OrganisationUnit> orgUnits) {
    return orgUnits.stream().map(ou -> ou.getStoredPath() + "%").toArray(String[]::new);
  }
}
