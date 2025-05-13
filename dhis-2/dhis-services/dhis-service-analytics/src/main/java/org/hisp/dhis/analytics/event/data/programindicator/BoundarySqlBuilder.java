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
package org.hisp.dhis.analytics.event.data.programindicator;

import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_ENROLLMENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_INCIDENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_SCHEDULED_DATE;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.ProgramIndicator;

/**
 * Builds a string containing SQL 'AND' conditions for all applicable period boundaries defined in
 * the ProgramIndicator. Reviewed for correctness.
 */
@UtilityClass
@Slf4j
public class BoundarySqlBuilder {

  /**
   * Returns a String that starts with a single space **or** the empty string. It is already
   * prefixed with `" and "` for each boundary so callers can concatenate directly into a WHERE
   * clause.
   */
  public String buildSql(
      Set<AnalyticsPeriodBoundary> boundaries,
      String defaultEventTimeColumn, // e.g. "occurreddate"
      ProgramIndicator pi,
      Date reportingStart,
      Date reportingEnd,
      SqlBuilder qb) {

    if (boundaries == null || boundaries.isEmpty()) {
      return "";
    }

    StringBuilder sql = new StringBuilder();
    SimpleDateFormat df = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT);

    for (AnalyticsPeriodBoundary boundary : boundaries) {
      if (boundary == null
          || boundary.getAnalyticsPeriodBoundaryType() == null
          || boundary.getBoundaryTarget() == null) continue;

      /* Resolve DB column */
      String dbColumn = resolveDbColumn(boundary, defaultEventTimeColumn);

      // Skip if unsupported boundary type
      if (dbColumn == null) {
        log.warn(
            "Unsupported boundary type {} for PI {}",
            boundary.getAnalyticsPeriodBoundaryType(),
            pi.getUid());
        continue;
      }

      // Try to resolve boundary date
      Date bd = boundary.getBoundaryDate(reportingStart, reportingEnd);
      if (bd == null) {
        log.warn("Cannot compute date for boundary {} in PI {}", boundary.getUid(), pi.getUid());
        continue;
      }

      // Build operator + clause
      String op = boundary.getAnalyticsPeriodBoundaryType().isEndBoundary() ? "<" : ">=";
      sql.append(" and ")
          .append(qb.quote(dbColumn))
          .append(' ')
          .append(op)
          .append(' ')
          .append(qb.singleQuote(df.format(bd)));
    }
    return sql.toString();
  }

  private String resolveDbColumn(AnalyticsPeriodBoundary boundary, String defaultEventTimeColumn) {
    if (boundary.isEventDateBoundary()) return defaultEventTimeColumn;
    if (boundary.isEnrollmentDateBoundary()) return DB_ENROLLMENT_DATE;
    if (boundary.isIncidentDateBoundary()) return DB_INCIDENT_DATE;
    if (boundary.isScheduledDateBoundary()) return DB_SCHEDULED_DATE;
    return null;
  }
}
