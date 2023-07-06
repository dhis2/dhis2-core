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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.OrgUnitFieldType.OWNER_AT_START;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ORG_UNIT_GROUPSET_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ORG_UNIT_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.OWNERSHIP_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;
import static org.hisp.dhis.util.DateUtils.plusOneDay;

import java.util.Date;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.program.AnalyticsType;

/**
 * Joiner of organisation unit tables for event/enrollment analytics query.
 *
 * @author Jim Grace
 */
public final class OrgUnitTableJoiner {
  private OrgUnitTableJoiner() {
    throw new UnsupportedOperationException("util");
  }

  /**
   * Generates SQL to join any needed organisation unit tables.
   *
   * @param params a {@see EventQueryParams}
   * @param analyticsType a {@see AnalyticsType}
   */
  public static String joinOrgUnitTables(EventQueryParams params, AnalyticsType analyticsType) {
    String sql = "";

    if (params.getOrgUnitField().getType().isOwnership()) {
      sql += joinPeriodStructureAndOwnershipTables(params);
    }

    if (params.getOrgUnitField().isJoinOrgUnitTables(analyticsType)) {
      sql += joinOrgUnitStructureTables(params, analyticsType);
    }

    return sql;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Joins ownership table, first joining periodstructure table if needed.
   *
   * <p>The dates ranges in the ownership table are based on the ownership at the start of each day.
   * To get the ownership at the end of a day, we must add one to the date, to get the ownership at
   * the start of the next day.
   *
   * <p>Note that there are no entries in the _periodstructure table for daily periods. In this case
   * the period start and end dates are the same and they are the date found in the analytics daily
   * column.
   */
  private static String joinPeriodStructureAndOwnershipTables(EventQueryParams params) {
    boolean isOwnerAtStart = params.getOrgUnitField().getType() == OWNER_AT_START;

    String sql = "";

    String compareDate;

    if (params.useStartEndDates()) {
      Date date =
          (isOwnerAtStart) ? params.getEarliestStartDate() : plusOneDay(params.getLatestEndDate());

      compareDate = "'" + getMediumDateString(date) + "'";
    } else {
      if (params.getPeriodType().equalsIgnoreCase(PeriodTypeEnum.DAILY.getName())) {
        compareDate =
            (isOwnerAtStart)
                ? "cast(ax.\"daily\" as date)"
                : "cast(ax.\"daily\" as date) + INTERVAL '1 day'";
      } else {
        compareDate =
            (isOwnerAtStart)
                ? "periodstruct.\"startdate\""
                : "periodstruct.\"enddate\" + INTERVAL '1 day'";

        sql += joinPeriodStructure(params);
      }
    }

    return sql + joinOwnershipTable(params, compareDate);
  }

  /** Joins the periodstructure table if needed in order to join the ownership table. */
  private static String joinPeriodStructure(EventQueryParams params) {
    return "left join _periodstructure periodstruct on "
        + quote(ANALYTICS_TBL_ALIAS, params.getPeriodType().toLowerCase())
        + " = periodstruct.\"iso\" ";
  }

  /** Joins the ownership table. */
  private static String joinOwnershipTable(EventQueryParams params, String compareDate) {
    String ownershipTable =
        PartitionUtils.getTableName(
            AnalyticsTableType.OWNERSHIP.getTableName(), params.getProgram());

    return "left join "
        + ownershipTable
        + " as "
        + OWNERSHIP_TBL_ALIAS
        + " on "
        + quote(ANALYTICS_TBL_ALIAS, "tei")
        + " = "
        + quote(OWNERSHIP_TBL_ALIAS, "teiuid")
        + " and "
        + compareDate
        + " between "
        + quote(OWNERSHIP_TBL_ALIAS, "startdate")
        + " and "
        + quote(OWNERSHIP_TBL_ALIAS, "enddate")
        + " ";
  }

  /** Joins the orgunitstructure table and, if needed, the orgunitgroupsetstructure table. */
  private static String joinOrgUnitStructureTables(
      EventQueryParams params, AnalyticsType analyticsType) {
    String orgUnitJoinCol = params.getOrgUnitField().getOrgUnitJoinCol(analyticsType);

    String sql =
        "left join _orgunitstructure as "
            + ORG_UNIT_STRUCT_ALIAS
            + " on "
            + orgUnitJoinCol
            + " = "
            + quote(ORG_UNIT_STRUCT_ALIAS, "organisationunituid")
            + " ";

    if (params.hasOrganisationUnitGroupSets()) {
      sql +=
          "left join _organisationunitgroupsetstructure as "
              + ORG_UNIT_GROUPSET_STRUCT_ALIAS
              + " on "
              + quote(ORG_UNIT_STRUCT_ALIAS, "organisationunitid")
              + " = "
              + quote(ORG_UNIT_GROUPSET_STRUCT_ALIAS, "organisationunitid")
              + " ";
    }

    return sql;
  }
}
