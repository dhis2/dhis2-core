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
package org.hisp.dhis.analytics.table.writer;

import static java.util.Calendar.JANUARY;
import static org.apache.commons.lang3.time.DateUtils.truncate;
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
 * Writer of rows to the analytics_ownership staging tables.
 *
 * @author Jim Grace
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JdbcOwnershipWriter {
  private final MappingBatchHandler batchHandler;

  /** Row of the current write, possibly modified. */
  private Map<String, Object> newRow;

  /** Previous row, if any. */
  private Map<String, Object> prevRow;

  /** Does the previous row exist? */
  private boolean previousRowExists = false;

  public static final String TRACKEDENTITY = "teuid";

  public static final String STARTDATE = "startdate";

  public static final String ENDDATE = "enddate";

  public static final String OU = "ou";

  private static Date FAR_PAST_DATE = new GregorianCalendar(1001, JANUARY, 1).getTime();

  /** Gets instance by a factory method (so it can be mocked). */
  public static JdbcOwnershipWriter getInstance(MappingBatchHandler batchHandler) {
    return new JdbcOwnershipWriter(batchHandler);
  }

  /**
   * Write a row to an analytics_ownership staging table. Work on a copy of the row, so we do not
   * change the original row. We cannot use immutable maps because some of the orgUnit levels are
   * null when the orgUnit isn't at the lowest level. Immutable maps don't allow null values.
   *
   * @param row map of values to write
   */
  public void write(Map<String, Object> row) {
    newRow = new HashMap<>(row);
    adjustNewRowDates();

    if (shouldContinuePreviousRow()) {
      continuePreviousRow();
    } else {
      writePreviousRowIfExists();
    }

    prevRow = newRow;
    previousRowExists = true;
  }

  /** Flush the last row to the output. We will have a row to flush unless we had no input. */
  public void flush() {
    writePreviousRowIfExists();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Adjust the dates in the new row.
   *
   * <p>If we are continuing entries for a tracked entity, then set the start date equal to the
   * previous end date plus one day, otherwise we are recording values for a different tacked entity
   * so set the start date equal to a far past date.
   *
   * <p>Remove the time portion of the end date, so it is just the date.
   */
  private void adjustNewRowDates() {
    newRow.put(STARTDATE, sameTrackedEntity() ? startAfterPrevious() : FAR_PAST_DATE);
    newRow.put(ENDDATE, truncate(newRow.get(ENDDATE), Calendar.DATE));
  }

  /** Do we have a previous row with the same tracked entity? */
  private boolean sameTrackedEntity() {
    return previousRowExists && sameValue(TRACKEDENTITY);
  }

  /** Start the new row one day after the previous one ends. */
  private Date startAfterPrevious() {
    return addDays((Date) prevRow.get(ENDDATE), 1);
  }

  /**
   * Should we continue the previous row?
   *
   * <p>It should be continued if the previous row is the same tracked entity and either the OU or
   * the ENDDATE has the same value.
   *
   * <p>If the ENDDATE is the same (now that the time of day has been removed), this means that
   * there were multiple ownership changes within the same day. In this event, we want to record
   * only a single ownership period that starts with the previous ownership start date and ends at
   * the end of the day.
   *
   * <p>If the OU is the same (perhaps after combining rows with the same ENDDATE), then also we
   * only want to record a single ownership that starts with the previous ownership start date and
   * ends with the new ownership end date.
   */
  private boolean shouldContinuePreviousRow() {
    return sameTrackedEntity() && (sameValue(ENDDATE) || sameValue(OU));
  }

  /**
   * Continue the previous row by transferring the previous row's start date to the new row. All
   * other properties such as OU and ENDDATE come from the new row.
   */
  private void continuePreviousRow() {
    newRow.put(STARTDATE, prevRow.get(STARTDATE));
  }

  /**
   * Returns true if the column has the same value between the previous row and the new row. Note
   * that the new row may have a null value.
   */
  private boolean sameValue(String colName) {
    return Objects.equals(prevRow.get(colName), newRow.get(colName));
  }

  /** Write out the previous row to the batch handler. */
  private void writePreviousRowIfExists() {
    if (previousRowExists) {
      batchHandler.addObject(prevRow);
    }
  }
}
