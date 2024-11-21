/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.migration.v40;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.period.RelativePeriodEnum.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;

public class V2_40_30__Add_new_column_into_visualization_and_migrate_relative_periods
    extends BaseJavaMigration {
  public static final Map<String, String> SELECT_UPDATE_PAIRS =
      Map.of(
          "select v.visualizationid, v.relativeperiodsid from visualization v join relativeperiods r on r.relativeperiodsid = v.relativeperiodsid order by v.visualizationid",
          "update visualization set relativeperiods = ? where visualizationid = ?",
          "select re.reportid, re.relativeperiodsid from report re join relativeperiods r ON r.relativeperiodsid = re.relativeperiodsid order by re.reportid",
          "update report set relativeperiods = ? where reportid = ?",
          "select e.eventvisualizationid, e.relativeperiodsid from eventvisualization e join relativeperiods r ON r.relativeperiodsid = e.relativeperiodsid order by e.eventvisualizationid",
          "update eventvisualization set relativeperiods = ? where eventvisualizationid = ?",
          "select m.mapviewid, m.relativeperiodsid from mapview m join relativeperiods r ON r.relativeperiodsid = m.relativeperiodsid order by m.mapviewid",
          "update mapview set relativeperiods = ? where mapviewid = ?");
  private static final Logger log =
      getLogger(V2_40_30__Add_new_column_into_visualization_and_migrate_relative_periods.class);
  private static final Map<String, String> PERIODS = new HashMap<>();

  static {
    PERIODS.put("thisDay", TODAY.name());
    PERIODS.put("yesterday", YESTERDAY.name());
    PERIODS.put("last3Days", LAST_3_DAYS.name());
    PERIODS.put("last7Days", LAST_7_DAYS.name());
    PERIODS.put("last14Days", LAST_14_DAYS.name());
    PERIODS.put("last30Days", LAST_30_DAYS.name());
    PERIODS.put("last60Days", LAST_60_DAYS.name());
    PERIODS.put("last90Days", LAST_90_DAYS.name());
    PERIODS.put("last180Days", LAST_180_DAYS.name());
    PERIODS.put("thisMonth", THIS_MONTH.name());
    PERIODS.put("lastMonth", LAST_MONTH.name());
    PERIODS.put("thisBimonth", THIS_BIMONTH.name());
    PERIODS.put("lastBimonth", LAST_BIMONTH.name());
    PERIODS.put("thisQuarter", THIS_QUARTER.name());
    PERIODS.put("lastQuarter", LAST_QUARTER.name());
    PERIODS.put("thisSixMonth", THIS_SIX_MONTH.name());
    PERIODS.put("lastSixMonth", LAST_SIX_MONTH.name());
    PERIODS.put("weeksThisYear", WEEKS_THIS_YEAR.name());
    PERIODS.put("monthsThisYear", MONTHS_THIS_YEAR.name());
    PERIODS.put("biMonthsThisYear", BIMONTHS_THIS_YEAR.name());
    PERIODS.put("quartersThisYear", QUARTERS_THIS_YEAR.name());
    PERIODS.put("thisYear", THIS_YEAR.name());
    PERIODS.put("monthsLastYear", MONTHS_LAST_YEAR.name());
    PERIODS.put("quartersLastYear", QUARTERS_LAST_YEAR.name());
    PERIODS.put("lastYear", LAST_YEAR.name());
    PERIODS.put("last5Years", LAST_5_YEARS.name());
    PERIODS.put("last10Years", LAST_10_YEARS.name());
    PERIODS.put("last12Months", LAST_12_MONTHS.name());
    PERIODS.put("last6Months", LAST_6_MONTHS.name());
    PERIODS.put("last3Months", LAST_3_MONTHS.name());
    PERIODS.put("last6BiMonths", LAST_6_BIMONTHS.name());
    PERIODS.put("last4Quarters", LAST_4_QUARTERS.name());
    PERIODS.put("last2SixMonths", LAST_2_SIXMONTHS.name());
    PERIODS.put("thisFinancialYear", THIS_FINANCIAL_YEAR.name());
    PERIODS.put("lastFinancialYear", LAST_FINANCIAL_YEAR.name());
    PERIODS.put("last5FinancialYears", LAST_5_FINANCIAL_YEARS.name());
    PERIODS.put("last10FinancialYears", LAST_10_FINANCIAL_YEARS.name());
    PERIODS.put("thisWeek", THIS_WEEK.name());
    PERIODS.put("lastWeek", LAST_WEEK.name());
    PERIODS.put("thisBiWeek", THIS_BIWEEK.name());
    PERIODS.put("lastBiWeek", LAST_BIWEEK.name());
    PERIODS.put("last4Weeks", LAST_4_WEEKS.name());
    PERIODS.put("last4BiWeeks", LAST_4_BIWEEKS.name());
    PERIODS.put("last12Weeks", LAST_12_WEEKS.name());
    PERIODS.put("last52Weeks", LAST_52_WEEKS.name());
  }

  public void migrate(Context context) throws SQLException {
    if (!isMigrationAlreadyApplied(context)) {
      step1(context);
      step2(context);
    }
  }

  private boolean isMigrationAlreadyApplied(Context context) {
    String schema = null;
    try {
      schema = context.getConnection().getSchema();
    } catch (SQLException e) {
      log.error("Schema check: ", e);
    }
    final String checkColumnExists =
        "select exists (select 1 from information_schema.columns where "
            + (schema != null ? "table_schema='" + schema + "' and " : "")
            + "table_name='eventvisualization' and column_name='relativeperiods')";
    try (Statement statement = context.getConnection().createStatement();
        ResultSet rs = statement.executeQuery(checkColumnExists)) {
      while (rs.next()) {
        return rs.getBoolean(1);
      }
    } catch (SQLException e) {
      log.error("Check failed: ", e);
    }
    return false;
  }

  /**
   * Creates the new column "relativeperiods" in all required tables.
   *
   * @param context
   */
  private void step1(Context context) throws SQLException {
    try (Statement statement = context.getConnection().createStatement()) {
      log.info("Step 1: Add new column 'relativeperiods' into required tables.");
      statement.execute(
          "alter table if exists visualization add column if not exists relativeperiods jsonb");
      statement.execute(
          "alter table if exists report add column if not exists relativeperiods jsonb");
      statement.execute(
          "alter table if exists mapview add column if not exists relativeperiods jsonb");
      statement.execute(
          "alter table if exists eventvisualization add column if not exists relativeperiods jsonb");
    } catch (SQLException e) {
      log.error("Step 1 failed: ", e);
      throw e;
    }
  }

  /**
   * Populates the JSON columns based on the existing relative periods table.
   *
   * @param context
   * @throws SQLException
   */
  private static void step2(Context context) throws SQLException {
    log.info("Step 2: Migrate the relative periods into the new column 'relativeperiods'.");
    // For each parent table, populate the new "relativeperiods" column with the current relative
    // periods set in the "relativeperiods" table.
    for (Entry<String, String> selectUpdatePair : SELECT_UPDATE_PAIRS.entrySet()) {
      Map<Long, Long> idsMap = new HashMap<>();
      String parentTableSelect = selectUpdatePair.getKey();
      try (Statement statement = context.getConnection().createStatement();
          ResultSet rs = statement.executeQuery(parentTableSelect)) {
        // Get the parent table column id + the relativeperiodsid.
        while (rs.next()) {
          idsMap.put(rs.getLong(1), rs.getLong(2));
        }
        String parentTableUpdate = selectUpdatePair.getValue();
        try (PreparedStatement ps = context.getConnection().prepareStatement(parentTableUpdate)) {
          // For each entry of the map, copy the relative periods to the new column
          // 'relativeperiods', in the parent table.
          copyPeriodsToJsonColumn(context, idsMap, ps);
        } catch (SQLException e) {
          log.error("Step 2 failed: ", e);
          throw e;
        }
      }
    }
  }

  private static void copyPeriodsToJsonColumn(
      Context context, Map<Long, Long> idsMap, PreparedStatement ps) throws SQLException {
    List<String> periodList = new ArrayList<>();
    for (Entry<Long, Long> ids : idsMap.entrySet()) {
      Long relativePeriodsId = ids.getValue();
      Long parentTableId = ids.getKey();
      try (Statement st = context.getConnection().createStatement();
          ResultSet r =
              st.executeQuery(
                  "select * from relativeperiods r where relativeperiodsid = "
                      + relativePeriodsId)) {
        // Get the parent table column id + the relativeperiodsid.
        while (r.next()) {
          // Add the periods that are set to "true" into the list.
          for (String column : PERIODS.keySet()) {
            if (r.getBoolean(column)) {
              periodList.add(PERIODS.get(column));
            }
          }
        }
      } catch (SQLException e) {
        log.error("Step 2 failed reading relative periods: ", e);
        throw e;
      }
      PGobject jsonObject = new PGobject();
      jsonObject.setType("json");
      jsonObject.setValue(
          periodList.stream().map((String e) -> "\"" + e + "\"").collect(toList()).toString());
      ps.setObject(1, jsonObject);
      ps.setLong(2, parentTableId);
      ps.executeUpdate();
      // Clear the list of periods, so it can be reused in the next iteration.
      periodList.clear();
    }
  }
}
