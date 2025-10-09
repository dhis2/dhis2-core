/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics;

import com.google.common.base.MoreObjects;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.util.DateUtils;

/**
 * Class representing parameters for the analytics table generation process.
 *
 * @author Lars Helge Overland
 */
@Getter
@Builder(toBuilder = true, builderMethodName = "newBuilder")
public class AnalyticsTableUpdateParams {
  /**
   * Number of last years for which to update tables. A zero value indicates the "latest" data
   * stored since last full analytics table generation.
   */
  private final Integer lastYears;

  /** Indicates whether to skip update of resource tables. */
  private final boolean skipResourceTables;

  /** Indicates whether to skip update of analytics tables, outliers stats columns. */
  private final boolean skipOutliers;

  /** Indicates whether to refresh the period resource table before analytics table update. */
  private final boolean refreshPeriodResourceTable;

  /** Analytics table types to skip. */
  @Builder.Default private final Set<AnalyticsTableType> skipTableTypes = new HashSet<>();

  /** Analytics table programs to skip. */
  @Builder.Default private final Set<String> skipPrograms = new HashSet<>();

  /** Job ID. */
  private final JobConfiguration jobId;

  /** Start time for update process. */
  @Builder.Default private final Date startTime = new Date();

  /** Time of last successful analytics table update. */
  private final Date lastSuccessfulUpdate;

  /** Current date, only used for testing */
  private final Date today;

  /** Map of arbitrary extra parameters. */
  @Builder.Default private final Map<String, Object> extraParameters = new HashMap<>();

  /**
   * Adds an extra parameter.
   *
   * @param prefix the parameter key prefix.
   * @param key the parameter key.
   * @param value the parameter value.
   */
  public void addExtraParam(String prefix, String key, Object value) {
    extraParameters.put(prefix + key, value);
  }

  /**
   * Retrieves the extra parameter with the given key.
   *
   * @param prefix the parameter key prefix.
   * @param key the parameter key.
   * @return a parameter object.
   */
  public Object getExtraParam(String prefix, String key) {
    return extraParameters.get(prefix + key);
  }

  // -------------------------------------------------------------------------
  // Get methods
  // -------------------------------------------------------------------------

  public boolean isSkipPrograms() {
    return !skipPrograms.isEmpty();
  }

  /**
   * Indicates whether this is a partial update of analytics tables, i.e. if only certain partitions
   * are to be updated and not all partitions including the main analytics tables.
   */
  public boolean isPartialUpdate() {
    return lastYears != null || isLatestUpdate();
  }

  /** Indicates whether this is an update of the "latest" partition. */
  public boolean isLatestUpdate() {
    return Objects.equals(lastYears, AnalyticsTablePartition.LATEST_PARTITION);
  }

  // -------------------------------------------------------------------------
  // toString
  // -------------------------------------------------------------------------

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("last years", lastYears)
        .add("skip resource tables", skipResourceTables)
        .add("refresh period resource table", refreshPeriodResourceTable)
        .add("skip table types", skipTableTypes)
        .add("skip programs", skipPrograms)
        .add("skip outliers statistics", skipOutliers)
        .add("start time", DateUtils.toLongDate(startTime))
        .toString();
  }

  /**
   * Returns the from date based on the last years property, i.e. the first day of year relative to
   * the last years property.
   *
   * @return the from date based on the last years property.
   */
  public Date getFromDate() {
    Date earliest = null;

    if (lastYears != null) {
      Calendar calendar = PeriodType.getCalendar();
      DateTimeUnit dateTimeUnit =
          today == null ? calendar.today() : DateTimeUnit.fromJdkDate(today);
      dateTimeUnit = calendar.minusYears(dateTimeUnit, lastYears - 1);
      dateTimeUnit.setMonth(1);
      dateTimeUnit.setDay(1);

      earliest = dateTimeUnit.toJdkDate();
    }

    return earliest;
  }

  /**
   * Indicates whether a from date exists.
   *
   * @return true if a from date exists.
   */
  public boolean hasFromDate() {
    return getFromDate() != null;
  }

  public AnalyticsTableUpdateParams withLatestPartition() {
    return this.toBuilder()
        .lastYears(AnalyticsTablePartition.LATEST_PARTITION)
        .refreshPeriodResourceTable(true)
        .build();
  }
}
