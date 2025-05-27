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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.Set;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityQueryParams;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class OrgUnitJdbcPredicate {

  public static void buildOrgUnitModeClause(
      StringBuilder sql,
      TrackedEntityQueryParams params,
      MapSqlParameterSource sqlParameters,
      String tableAlias) {

    if (params.hasOrganisationUnits()) {
      sql.append("and ");
      if (params.isOrganisationUnitMode(DESCENDANTS)) {
        addOrgUnitDescendantsCondition(sql, params, sqlParameters, tableAlias);
      } else if (params.isOrganisationUnitMode(CHILDREN)) {
        addOrgUnitsChildrenCondition(sql, params, sqlParameters, tableAlias);
      } else {
        sql.append(tableAlias);
        sql.append(".organisationunitid in (:orgUnits)");
        sqlParameters.addValue("orgUnits", getIdentifiers(params.getOrgUnits()));
      }
      sql.append(" ");
    }
  }

  public static void buildOwnershipClause(
      StringBuilder sql,
      TrackedEntityQueryParams params,
      MapSqlParameterSource sqlParameters,
      Set<OrganisationUnit> effectiveSearchOrgUnits,
      Set<OrganisationUnit> captureScopeOrgUnits,
      String programTableAlias,
      String orgUnitTableAlias) {
    if (params.isOrganisationUnitMode(ALL) || getCurrentUserDetails().isSuper()) {
      return;
    }

    SqlHelper sqlHelper = new SqlHelper(true);

    sql.append(sqlHelper.andOr());
    sql.append("((");
    sql.append(programTableAlias);
    sql.append(".accesslevel in ('OPEN', 'AUDITED') and (");
    sql.append(orgUnitTableAlias);
    sql.append(".path like any (:effectiveSearchScopePaths))) ");
    sqlParameters.addValue(
        "effectiveSearchScopePaths", getOrgUnitsPathArray(effectiveSearchOrgUnits));

    sql.append(sqlHelper.andOr());
    sql.append("(");
    sql.append(programTableAlias);
    sql.append(".accesslevel in ('PROTECTED', 'CLOSED') and (");
    sql.append(orgUnitTableAlias);
    sql.append(".path like any (:captureScopePaths))))");
    sqlParameters.addValue("captureScopePaths", getOrgUnitsPathArray(captureScopeOrgUnits));
  }

  private static void addOrgUnitDescendantsCondition(
      StringBuilder sql,
      TrackedEntityQueryParams params,
      MapSqlParameterSource sqlParameters,
      String tableAlias) {
    SqlHelper orHlp = new SqlHelper(true);

    sql.append("(");
    int index = 0;
    for (OrganisationUnit organisationUnit : params.getOrgUnits()) {
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
      TrackedEntityQueryParams params,
      MapSqlParameterSource sqlParameters,
      String tableAlias) {
    SqlHelper orHlp = new SqlHelper(true);

    sql.append("(");
    int index = 0;
    for (OrganisationUnit organisationUnit : params.getOrgUnits()) {
      String pathParamName = "orgUnitPath" + index;
      String pathParamValue = organisationUnit.getStoredPath() + "%";
      String parentHierarchyParamName = "parentHierarchyLevel" + index;
      int parentHierarchyParamValue = organisationUnit.getHierarchyLevel();
      String childHierarchyParamName = "childHierarchyLevel" + index;

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
