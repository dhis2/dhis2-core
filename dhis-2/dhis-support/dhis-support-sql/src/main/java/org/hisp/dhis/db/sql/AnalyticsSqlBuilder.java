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
package org.hisp.dhis.db.sql;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.hisp.dhis.period.PeriodTypeEnum;

/**
 * Interface for resolving specific SQL queries for analytics, that requires custom logic that can't
 * be resolved by the default <code>SqlBuilder</code> implementations.
 */
public interface AnalyticsSqlBuilder extends SqlBuilder {

  DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  /**
   * Returns the correct SQL based on the underlying database for fetching the event data values.
   *
   * @return a SQL snippet.
   */
  String getEventDataValues();

  /**
   * Renders a timestamp string to a format that is compatible with the underlying database. The
   * returned timestamp format is expected to be in the format "yyyy-MM-dd HH:mm:ss.SSS". In case
   * the last three digits are zeros, the rendered timestamp is truncated to "yyyy-MM-dd
   * HH:mm:ss.S".
   *
   * @param timestampAsString the timestamp as a string.
   * @return the timestamp as a string in the correct format.
   */
  String renderTimestamp(String timestampAsString);

  /**
   * Renders a database-specific expression for extracting a period bucket value directly from a
   * stage date column.
   *
   * <p>Implementations may return an empty value to indicate that generic lookup-based rendering
   * should be used.
   *
   * @param stageDateColumn the stage date SQL column/expression (already quoted/aliased)
   * @param periodBucketColumn period bucket identifier such as {@code yearly}, {@code monthly},
   *     {@code daily}, {@code quarterly}
   * @return database-specific period bucket expression if supported
   */
  default Optional<String> renderStageDatePeriodBucket(
      String stageDateColumn, String periodBucketColumn) {
    return Optional.empty();
  }

  /**
   * Renders a database-specific DATE expression for the start date of the given period bucket.
   *
   * <p>Analytics database implementations are expected to support every period bucket used by the
   * event/enrollment aggregate query path. Returning an empty value indicates an unsupported
   * builder/period combination and should fail fast in callers.
   *
   * @param dateColumn the SQL column/expression containing the source date or timestamp
   * @param periodType the requested period type
   * @return database-specific DATE expression if supported
   */
  default Optional<String> renderDateFieldPeriodBucketDate(
      String dateColumn, PeriodTypeEnum periodType) {
    return Optional.empty();
  }

  /**
   * Indicates whether period identifier lookups against {@code analytics_rs_dateperiodstructure}
   * must be rendered as joins rather than correlated scalar subqueries.
   *
   * @return {@code true} when lookup-table joins must be used
   */
  default boolean useJoinForDatePeriodStructureLookup() {
    return false;
  }

  /**
   * Returns an SQL expression casting {@code expression} to {@code DATE}, propagating {@code NULL}
   * inputs to {@code NULL} results.
   *
   * <p>The default uses ANSI {@code cast(... as date)}, which Postgres and Doris already make
   * NULL-safe. Engines that throw on {@code NULL} input (e.g. ClickHouse {@code toDate}) must
   * override to return their NULL-tolerant equivalent.
   *
   * @param expression the source SQL expression
   * @return an SQL fragment that casts {@code expression} to a date and yields {@code NULL} when
   *     the input is {@code NULL}
   */
  default String castAsDate(String expression) {
    return "cast(" + expression + " as date)";
  }

  /**
   * Returns an SQL expression that yields {@code NULL} when {@code column} holds an empty string,
   * and the column value otherwise. This normalises empty text to {@code NULL} so that grouping
   * treats absent and empty text values the same way across analytics databases.
   *
   * <p>The default returns the column unchanged. Engines that store empty strings where other
   * engines store {@code NULL} (ClickHouse) override this to wrap the column in a {@code nullif}.
   *
   * @param column the text SQL column or expression.
   * @return a NULL-normalising SQL fragment.
   */
  default String nullIfEmpty(String column) {
    return column;
  }
}
