package org.hisp.dhis.analytics.event.data.programindicator;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.ProgramIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * Builds a string containing SQL 'AND' conditions for all applicable period boundaries defined in
 * the ProgramIndicator. Reviewed for correctness.
 *
 */
@UtilityClass
@Slf4j
public class BoundarySqlBuilder {

    /**
     * Returns a String that starts with a single space **or** the empty string.
     * It is already prefixed with `" and "` for each boundary so callers can
     * concatenate directly into a WHERE clause.
     */
    public String buildSql(
            Set<AnalyticsPeriodBoundary> boundaries,
            String defaultEventTimeColumn,      // e.g. "occurreddate"
            ProgramIndicator pi,
            Date reportingStart,
            Date reportingEnd,
            SqlBuilder qb) {

        if (boundaries == null || boundaries.isEmpty()) {
            return "";
        }

        StringBuilder sql = new StringBuilder();
        SimpleDateFormat df = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT);

        for (AnalyticsPeriodBoundary b : boundaries) {
            if (b == null) continue;

            /* 1. Resolve DB column */
            String dbColumn;
            if (b.isEventDateBoundary())      dbColumn = defaultEventTimeColumn;
            else if (b.isEnrollmentDateBoundary()) dbColumn = AnalyticsPeriodBoundary.DB_ENROLLMENT_DATE;
            else if (b.isIncidentDateBoundary())   dbColumn = AnalyticsPeriodBoundary.DB_INCIDENT_DATE;
            else if (b.isScheduledDateBoundary())  dbColumn = AnalyticsPeriodBoundary.DB_SCHEDULED_DATE;
            else {
                log.warn("Unsupported boundary type {} for PI {}", b.getAnalyticsPeriodBoundaryType(), pi.getUid());
                continue;
            }

            /* 2. Resolve boundary date */
            Date bd = b.getBoundaryDate(reportingStart, reportingEnd);
            if (bd == null) {
                log.warn("Cannot compute date for boundary {} in PI {}", b.getUid(), pi.getUid());
                continue;
            }

            /* 3. Build operator + clause */
            String op = b.getAnalyticsPeriodBoundaryType().isEndBoundary() ? "<" : ">=";
            sql.append(" and ")
                    .append(qb.quote(dbColumn))
                    .append(' ')
                    .append(op)
                    .append(' ')
                    .append(qb.singleQuote(df.format(bd)));
        }
        return sql.toString();
    }
}
