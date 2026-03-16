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
package org.hisp.dhis.analytics.event.data.stage;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.ColumnAndAlias;
import org.hisp.dhis.analytics.event.data.OrganisationUnitResolver;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link StageOrgUnitSqlService}.
 *
 * <p>Uses {@link OrganisationUnitResolver} to resolve org units and to reuse existing stage OU
 * context behavior.
 */
@Component
public class DefaultStageOrgUnitSqlService implements StageOrgUnitSqlService {
  private final OrganisationUnitResolver organisationUnitResolver;
  private final AnalyticsSqlBuilder sqlBuilder;

  /**
   * Creates a stage org unit SQL service.
   *
   * @param organisationUnitResolver resolver for stage org unit context and org unit levels
   * @param sqlBuilder SQL builder used for quoting
   */
  public DefaultStageOrgUnitSqlService(
      OrganisationUnitResolver organisationUnitResolver, AnalyticsSqlBuilder sqlBuilder) {
    this.organisationUnitResolver = organisationUnitResolver;
    this.sqlBuilder = sqlBuilder;
  }

  /** {@inheritDoc} */
  @Override
  public ColumnAndAlias selectColumn(
      QueryItem item, EventQueryParams params, boolean isGroupByClause) {
    OrganisationUnitResolver.StageOuCteContext stageOuContext =
        organisationUnitResolver.buildStageOuCteContext(item, params);
    if (isGroupByClause) {
      return ColumnAndAlias.ofColumn(stageOuContext.valueColumn());
    }
    return ColumnAndAlias.ofColumnAndAlias(stageOuContext.valueColumn(), item.getItemName());
  }

  /** {@inheritDoc} */
  @Override
  public String whereClause(QueryItem item, EventQueryParams params, AnalyticsType analyticsType) {
    Map<Integer, List<OrganisationUnit>> orgUnitsByLevel =
        organisationUnitResolver.resolveOrgUnitsGroupedByLevel(params, item);

    if (orgUnitsByLevel.isEmpty()) {
      return "";
    }

    StringJoiner conditions = new StringJoiner(" and ");

    for (Map.Entry<Integer, List<OrganisationUnit>> entry : orgUnitsByLevel.entrySet()) {
      int level = entry.getKey();
      List<OrganisationUnit> orgUnits = entry.getValue();

      String column =
          params
              .getOrgUnitField()
              .withSqlBuilder(sqlBuilder)
              .getOrgUnitLevelCol(level, analyticsType);

      String quotedUids =
          orgUnits.stream()
              .map(OrganisationUnit::getUid)
              .filter(StringUtils::isNotEmpty)
              .map(uid -> "'" + uid + "'")
              .collect(joining(","));

      conditions.add(column + " in (" + quotedUids + ")");
    }

    conditions.add(sqlBuilder.quoteAx("ps") + " = '" + item.getProgramStage().getUid() + "'");

    return "(" + conditions + ")";
  }
}
