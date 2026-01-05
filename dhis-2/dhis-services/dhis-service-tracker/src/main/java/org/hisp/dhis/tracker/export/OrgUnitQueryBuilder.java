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

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.user.UserDetails;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OrgUnitQueryBuilder {
  /**
   * Appends an SQL clause to enforce program ownership and access level restrictions based on the
   * user scopes and org unit mode.
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
        .append(".accesslevel in ('OPEN', 'AUDITED') and ")
        .append(orgUnitTableAlias);
    if (orgUnitMode == CAPTURE) {
      sql.append(
          ".path like any (select concat(o.path, '%') from organisationunit o where o.uid in (:captureScopeOrgUnits)))");
    } else {
      sql.append(
          ".path like any (select concat(o.path, '%') from organisationunit o where o.uid in (:effectiveSearchScopeOrgUnits)))");
      sqlParameters.addValue(
          "effectiveSearchScopeOrgUnits", userDetails.getUserEffectiveSearchOrgUnitIds());
    }

    sql.append(" or (")
        .append(programTableAlias)
        .append(".accesslevel in ('PROTECTED', 'CLOSED') and ")
        .append(orgUnitTableAlias)
        .append(
            ".path like any (select concat(o.path, '%') from organisationunit o where o.uid in (:captureScopeOrgUnits)))");
    sqlParameters.addValue("captureScopeOrgUnits", userDetails.getUserOrgUnitIds());

    sql.append(
            " or (p.accesslevel = 'PROTECTED' and exists (select 1 from programtempowner where programid = ")
        .append(programTableAlias)
        .append(".programid and trackedentityid = ")
        .append(trackedEntityTableAlias)
        .append(".trackedentityid and userid = ")
        .append(userDetails.getId())
        .append(" and extract(epoch from validtill)-extract (epoch from now()::timestamp) > 0)))");
  }
}
