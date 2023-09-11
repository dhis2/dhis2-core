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
package org.hisp.dhis.analytics.orgunit.data;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.feedback.ErrorCode.E7302;
import static org.hisp.dhis.system.util.SqlUtils.quote;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.TableInfoReader;
import org.hisp.dhis.analytics.orgunit.OrgUnitAnalyticsManager;
import org.hisp.dhis.analytics.orgunit.OrgUnitQueryParams;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Service("org.hisp.dhis.analytics.orgunit.OrgUnitAnalyticsManager")
@RequiredArgsConstructor
public class JdbcOrgUnitAnalyticsManager implements OrgUnitAnalyticsManager {
  private final TableInfoReader tableInfoReader;
  private final JdbcTemplate jdbcTemplate;

  @Override
  public Map<String, Integer> getOrgUnitData(OrgUnitQueryParams params) {
    checkForMissingOrgUnitGroupSetColumns(params);

    Map<String, Integer> dataMap = new HashMap<>();

    Set<String> columns = getMetadataColumns(params);

    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(getQuerySql(params));

    while (rowSet.next()) {
      String key = columns.stream().map(rowSet::getString).collect(joining(DIMENSION_SEP));
      int value = rowSet.getInt("count");

      dataMap.put(key, value);
    }

    return dataMap;
  }

  /**
   * Checks if there is an org. unit dimension column, specified in the given params, not present in
   * the respective DB table ("_organisationunitgroupsetstructure"). If a dimension column (which in
   * this case represents an org. unit group set) is found to be missing in the table, it will not
   * be possible to query for the missing org. unit group set. In such cases, the request cannot be
   * processed.
   *
   * @param params the query params {@link OrgUnitQueryParams}.
   * @throws QueryRuntimeException if there are missing columns.
   */
  private void checkForMissingOrgUnitGroupSetColumns(OrgUnitQueryParams params) {
    Set<String> missingColumns =
        tableInfoReader.checkColumnsPresence(
            "_organisationunitgroupsetstructure", getOrgUnitGroupSetUids(params));

    boolean hasMissingColumns = isNotEmpty(missingColumns);

    if (hasMissingColumns) {
      throw new QueryRuntimeException(new ErrorMessage(E7302, missingColumns));
    }
  }

  /**
   * Returns dimension columns for the given query params.
   *
   * @param params the {@link OrgUnitQueryParams}.
   * @return a set of columns.
   */
  private Set<String> getOrgUnitGroupSetUids(OrgUnitQueryParams params) {
    return params.getOrgUnitGroupSets().stream()
        .map(OrganisationUnitGroupSet::getUid)
        .collect(toUnmodifiableSet());
  }

  /**
   * Returns metadata column names for the given query. Note that the "orgunit" column is always
   * returned.
   *
   * @param params the {@link OrgUnitQueryParams}.
   * @return a set of column names.
   */
  private Set<String> getMetadataColumns(OrgUnitQueryParams params) {
    Set<String> columns = new HashSet<>(getOrgUnitGroupSetUids(params));
    columns.add("orgunit");

    return columns;
  }

  /**
   * Returns a SQL query based on the given query.
   *
   * @param params the {@link OrgUnitQueryParams}.
   * @return a SQL query.
   */
  private String getQuerySql(OrgUnitQueryParams params) {
    String levelCol = String.format("ous.uidlevel%d", params.getOrgUnitLevel());

    Set<String> orgUnits =
        params.getOrgUnits().stream().map(OrganisationUnit::getUid).collect(toSet());

    Set<String> quotedGroupSets =
        params.getOrgUnitGroupSets().stream()
            .map(OrganisationUnitGroupSet::getUid)
            .map(uid -> quote("ougs", uid))
            .collect(toSet());

    return "select "
        + levelCol
        + " as orgunit, "
        + getCommaDelimitedString(quotedGroupSets)
        + ", "
        + "count(ougs.organisationunitid) as count "
        + "from "
        + quote("_orgunitstructure")
        + " ous "
        + "inner join "
        + quote("_organisationunitgroupsetstructure")
        + " "
        + "ougs on ous.organisationunitid = ougs.organisationunitid "
        + "where "
        + levelCol
        + " in ("
        + getQuotedCommaDelimitedString(orgUnits)
        + ") "
        + "group by "
        + levelCol
        + ", "
        + getCommaDelimitedString(quotedGroupSets)
        + ";";
  }
}
