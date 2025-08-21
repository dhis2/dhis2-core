/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.analytics.DataQueryParams.LEVEL_PREFIX;
import static org.hisp.dhis.analytics.event.data.AbstractJdbcEventAnalyticsManager.COL_VALUE;
import static org.hisp.dhis.analytics.event.data.AbstractJdbcEventAnalyticsManager.OUTER_SQL_ALIAS;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/** Helper class to support SQL/query handling for enrollments. */
@NoArgsConstructor(access = PRIVATE)
public class EnrollmentQueryHelper {

  private static final String QUOTE = "\"";

  private static final String DOT = ".";

  /**
   * Based on the given headers, it returns a set of respective database columns. It includes the
   * required alias.
   *
   * @param headers the list of {@link GridHeader}.
   * @return the set of database columns.
   */
  public static Set<String> getHeaderColumns(List<GridHeader> headers, String sql) {
    Set<String> headerColumns = new LinkedHashSet<>();

    for (GridHeader header : headers) {
      String headerName = header.getName();

      if (!headerName.equalsIgnoreCase(COL_VALUE)
          && !headerName.equalsIgnoreCase(PERIOD_DIM_ID)
          && !headerName.equalsIgnoreCase(ORGUNIT_DIM_ID)) {

        if (sql.contains(headerName)) {
          headerName = quote(headerName);
        } else if (headerName.contains(DOT)) {
          // Gets only the column name from the header in the URL.
          // This has to match the column of the analytics table.
          // ie.: A03MvHHogjR.a3kGcGDCuk6 -> a3kGcGDCuk6
          headerName = quote(headerName.split("\\.")[1]);
        } else {
          headerName = quote(headerName);
        }

        headerColumns.add(OUTER_SQL_ALIAS + DOT + headerName);
      }
    }

    return headerColumns;
  }

  /**
   * Based on the given headers and params, it returns a set of respective database columns. If the
   * header contains an column name that references a program stage item, it will be skipped.
   * Program Stage items are handled as separate CTE.
   *
   * @param headers the list of {@link GridHeader}.
   * @param params the {@link EventQueryParams}.
   * @return the set of database columns.
   */
  public static Set<String> getHeaderColumns(List<GridHeader> headers, EventQueryParams params) {
    Set<String> headerColumns = new LinkedHashSet<>();
    List<String> itemsToSkip =
        params.getItems().stream()
            .filter(QueryItem::hasProgramStage)
            .map(item -> "%s.%s".formatted(item.getProgramStage().getUid(), item.getItemId()))
            .toList();

    for (GridHeader header : headers) {
      String headerName = header.getName();

      if (!headerName.equalsIgnoreCase(COL_VALUE)
          && !headerName.equalsIgnoreCase(PERIOD_DIM_ID)
          && !headerName.equalsIgnoreCase(ORGUNIT_DIM_ID)
          && !itemsToSkip.contains(headerName)) {
        headerColumns.add(headerName);
      }
    }

    return headerColumns;
  }

  /**
   * Based on the given params, it returns a set of respective database columns representing org.
   * unit levels.
   *
   * @param params the {@link EventQueryParams}.
   * @return the set of database columns.
   */
  public static Set<String> getOrgUnitLevelColumns(EventQueryParams params) {
    if (!params.isOrganisationUnitMode(SELECTED) && !params.isOrganisationUnitMode(CHILDREN)) {
      Set<String> levels = new LinkedHashSet<>();

      for (DimensionalItemObject itemObject : params.getDimensionOptions(ORGUNIT_DIM_ID)) {
        String level = LEVEL_PREFIX + ((OrganisationUnit) itemObject).getLevel();
        levels.add(level);
      }

      return levels;
    }

    return Set.of();
  }

  /**
   * Based on the given params, it returns a set of respective database columns representing
   * periods. It includes the required alias.
   *
   * @param params the list of {@link EventQueryParams}.
   * @return the set of database columns.
   */
  public static Set<String> getPeriodColumns(EventQueryParams params) {
    Set<String> periods = new LinkedHashSet<>();

    for (DimensionalObject dim : params.getDimensions()) {
      if (dim.getDimensionType() == PERIOD) {
        for (DimensionalItemObject itemObject : dim.getItems()) {
          String column =
              OUTER_SQL_ALIAS
                  + DOT
                  + ((Period) itemObject).getPeriodType().getPeriodTypeEnum().getName();
          periods.add(column);
        }
      }
    }

    return periods;
  }

  /**
   * It adds double quotes to the given value.
   *
   * @param value the value to quote.
   * @return a double quoted value.
   */
  private static String quote(String value) {
    String escaped = value.replace(QUOTE, (QUOTE + QUOTE));
    return QUOTE + escaped + QUOTE;
  }
}
