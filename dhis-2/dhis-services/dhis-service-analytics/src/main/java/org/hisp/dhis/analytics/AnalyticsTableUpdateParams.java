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
package org.hisp.dhis.analytics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
public class AnalyticsTableUpdateParams {
  /**
   * Number of last years for which to update tables. A zero value indicates the "latest" data
   * stored since last full analytics table generation.
   */
  private Integer lastYears;

  /** Indicates whether to skip update of resource tables. */
  boolean skipResourceTables;

  /** Analytics table types to skip. */
  private Set<AnalyticsTableType> skipTableTypes = new HashSet<>();

  /** Analytics table programs to skip. */
  private Set<String> skipPrograms = new HashSet<>();

  /** Job ID. */
  private JobConfiguration jobId;

  /** Start time for update process. */
  private Date startTime;

  /** Time of last successful analytics table update. */
  private Date lastSuccessfulUpdate;

  /** Current date, only used for testing */
  private Date today;

  private AnalyticsTableUpdateParams() {
    this.startTime = new Date();
  }

  // -------------------------------------------------------------------------
  // Get methods
  // -------------------------------------------------------------------------

  public Integer getLastYears() {
    return lastYears;
  }

  public boolean isSkipResourceTables() {
    return skipResourceTables;
  }

  public Set<AnalyticsTableType> getSkipTableTypes() {
    return skipTableTypes;
  }

  public Set<String> getSkipPrograms() {
    return skipPrograms;
  }

  public JobConfiguration getJobId() {
    return jobId;
  }

  public Date getStartTime() {
    return startTime;
  }

  public Date getLastSuccessfulUpdate() {
    return lastSuccessfulUpdate;
  }

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
        .add("skip table types", skipTableTypes)
        .add("skip programs", skipPrograms)
        .add("start time", DateUtils.getLongDateString(startTime))
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

  // -------------------------------------------------------------------------
  // Builder of immutable instances
  // -------------------------------------------------------------------------

  /** Returns a new instance of this parameter object. */
  public AnalyticsTableUpdateParams instance() {
    AnalyticsTableUpdateParams params = new AnalyticsTableUpdateParams();

    params.lastYears = this.lastYears;
    params.skipResourceTables = this.skipResourceTables;
    params.skipTableTypes = new HashSet<>(this.skipTableTypes);
    params.skipPrograms = new HashSet<>(this.skipPrograms);
    params.jobId = this.jobId;
    params.startTime = this.startTime;
    params.lastSuccessfulUpdate = this.lastSuccessfulUpdate;

    return this;
  }

  public static Builder newBuilder() {
    return new AnalyticsTableUpdateParams.Builder();
  }

  public static Builder newBuilder(AnalyticsTableUpdateParams analyticsTableUpdateParams) {
    return new AnalyticsTableUpdateParams.Builder(analyticsTableUpdateParams);
  }

  /** Builder for {@link AnalyticsTableUpdateParams} instances. */
  public static class Builder {
    private final AnalyticsTableUpdateParams params;

    protected Builder() {
      this.params = new AnalyticsTableUpdateParams();
    }

    protected Builder(AnalyticsTableUpdateParams analyticsTableUpdateParams) {
      this.params = analyticsTableUpdateParams.instance();
    }

    public Builder withLastYears(Integer lastYears) {
      this.params.lastYears = lastYears;
      return this;
    }

    public Builder withLatestPartition() {
      this.params.lastYears = AnalyticsTablePartition.LATEST_PARTITION;
      return this;
    }

    public Builder withSkipResourceTables(boolean skipResourceTables) {
      this.params.skipResourceTables = skipResourceTables;
      return this;
    }

    public Builder withSkipTableTypes(Set<AnalyticsTableType> skipTableTypes) {
      this.params.skipTableTypes = skipTableTypes;
      return this;
    }

    public Builder withSkipPrograms(Set<String> skipPrograms) {
      this.params.skipPrograms = skipPrograms;
      return this;
    }

    public Builder withJobId(JobConfiguration jobId) {
      this.params.jobId = jobId;
      return this;
    }

    public Builder withLastSuccessfulUpdate(Date lastSuccessfulUpdate) {
      this.params.lastSuccessfulUpdate = lastSuccessfulUpdate;
      return this;
    }

    public Builder withStartTime(Date startTime) {
      this.params.startTime = startTime;
      return this;
    }

    /**
     * This builder property is only used for testing purposes.
     *
     * @param date A mock Date
     */
    public Builder withToday(Date date) {

      this.params.today = date;
      return this;
    }

    public AnalyticsTableUpdateParams build() {
      checkNotNull(this.params.startTime);
      return this.params;
    }
  }
}
