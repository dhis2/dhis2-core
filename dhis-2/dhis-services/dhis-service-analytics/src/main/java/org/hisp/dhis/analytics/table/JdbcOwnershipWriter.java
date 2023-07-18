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
package org.hisp.dhis.analytics.table;

import static java.util.Calendar.DECEMBER;
import static java.util.Calendar.JANUARY;
import static org.apache.commons.lang3.time.DateUtils.truncate;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.addDays;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.jdbc.batchhandler.MappingBatchHandler;

/**
 * Writer of rows to the analytics_ownership temp tables.
 *
 * @author Jim Grace
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JdbcOwnershipWriter {
  private final MappingBatchHandler batchHandler;

  /** Previous row for this TEI, if any. */
  private Map<String, Object> prevRow = null;

  /** Row of the current write, possibly modified. */
  private Map<String, Object> newRow;

  public static final String TEIUID = quote("teiuid");

  public static final String STARTDATE = quote("startdate");

  public static final String ENDDATE = quote("enddate");

  public static final String OU = quote("ou");

  private static final Date FAR_PAST_DATE = new GregorianCalendar(1000, JANUARY, 1).getTime();

  private static final Date FAR_FUTURE_DATE = new GregorianCalendar(9999, DECEMBER, 31).getTime();

  /** Gets instance by a factory method (so it can be mocked). */
  public static JdbcOwnershipWriter getInstance(MappingBatchHandler batchHandler) {
    return new JdbcOwnershipWriter(batchHandler);
  }

  /**
   * Write a row to an analytics_ownership temp table. Work on a copy of the row, so we do not
   * change the original row. We cannot use immutable maps because the orgUnit levels contain nulls
   * when the orgUnit is not at the lowest level, and immutable maps do not allow null values. Also,
   * the end date is null in the last record for each TEI.
   *
   * @param row map of values to write
   */
  public void write(Map<String, Object> row) {
    newRow = new HashMap<>(row);

    if (newRow.get(ENDDATE) != null) {
      // Remove the time of day portion of the ENDDATE.
      newRow.put(ENDDATE, truncate(newRow.get(ENDDATE), Calendar.DATE));
    }

    if (prevRow == null) {
      startNewTei();
    } else if (sameValue(OU) || sameValue(ENDDATE)) {
      combineWithPreviousRow();
    } else {
      writePreviousRow();
    }
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Process the first row for a TEI. Save it as the previous row and set the start date for far in
   * the past. If the previous row does not have an "enddate", we enforce a default one.
   */
  private void startNewTei() {
    prevRow = newRow;

    // Ensure a default "enddate" value.
    prevRow.putIfAbsent(ENDDATE, FAR_FUTURE_DATE);

    prevRow.put(STARTDATE, FAR_PAST_DATE);
  }

  /**
   * Combine the current row with the previous row by updating the previous row's OU and ENDDATE. If
   * the ENDDATE is the same this means there were multiple assignments during a day, and we want to
   * record only the last OU assignment for the day. If the OU is the same this means there were
   * successive assignments to the same OU (possibly after collapsing the same ENDDATE assignments
   * such as if a TEI was switched during a day to a different OU and then switched back again.) In
   * this case we can combine the two successive records for the same OU into a single record.
   *
   * <p>If this is the last (and not only) row for this TEI, write it out.
   */
  private void combineWithPreviousRow() {
    prevRow.put(OU, newRow.get(OU));
    prevRow.put(ENDDATE, newRow.get(ENDDATE));

    writeRowIfLast(prevRow);
  }

  /**
   * The new row is for a different ownership period of the same TEI. So write out the old row and
   * start the new row after the old row's end date. Then also write out the new row if it is the
   * last for this TEI.
   */
  private void writePreviousRow() {
    batchHandler.addObject(prevRow);

    newRow.put(STARTDATE, addDays((Date) prevRow.get(ENDDATE), 1));

    prevRow = newRow;

    writeRowIfLast(prevRow);
  }

  /**
   * If the passed row is the last for this TEI (no end date), then set the end date to far in the
   * future and write it out. However, if this is the only row for this TEI (from the beginning of
   * time to the end of time), then don't write it because the ownership never changed and analytics
   * queries can always use the enrollment orgUnit.
   *
   * <p>After, there will be no previous row for this TEI.
   */
  private void writeRowIfLast(Map<String, Object> row) {
    if (hasNullValue(row, ENDDATE)) // If the last row...
    {
      row.put(ENDDATE, FAR_FUTURE_DATE);

      if (!FAR_PAST_DATE.equals(row.get(STARTDATE))) {
        batchHandler.addObject(row);
      }

      prevRow = null;
    }
  }

  /** Returns true if the map has a null value. */
  private boolean hasNullValue(Map<String, Object> row, String colName) {
    return row.get(colName) == null;
  }

  /**
   * Returns true if the column has the same value between the previous row and the new row. (Note
   * that the new row may have a null value!)
   */
  private boolean sameValue(String colName) {
    return Objects.equals(prevRow.get(colName), newRow.get(colName));
  }
}
