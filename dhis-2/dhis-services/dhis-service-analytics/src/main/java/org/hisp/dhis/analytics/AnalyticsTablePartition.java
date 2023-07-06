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

import java.util.Date;
import java.util.Objects;
import org.hisp.dhis.analytics.table.PartitionUtils;

/**
 * Class representing an analytics database table partition.
 *
 * @author Lars Helge Overland
 */
public class AnalyticsTablePartition {
  public static final Integer LATEST_PARTITION = 0;

  /** The master analytics table for this partition. */
  private AnalyticsTable masterTable;

  /**
   * The year for which this partition may contain data. A zero value indicates the "latest" data
   * stored since last full analytics table generation.
   */
  private Integer year;

  /** The start date for which this partition may contain data, inclusive. */
  private Date startDate;

  /** The end date for which this partition may contain data, exclusive. */
  private Date endDate;

  /** Indicates whether data approval applies to this partition. */
  private boolean dataApproval;

  public AnalyticsTablePartition(
      AnalyticsTable masterTable,
      Integer year,
      Date startDate,
      Date endDate,
      boolean dataApproval) {
    this.masterTable = masterTable;
    this.year = year;
    this.startDate = startDate;
    this.endDate = endDate;
    this.dataApproval = dataApproval;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public String getTableName(boolean forTempTable) {
    String name =
        forTempTable
            ? masterTable.getBaseName() + AnalyticsTableManager.TABLE_TEMP_SUFFIX
            : masterTable.getBaseName();

    if (masterTable.getProgram() != null) {
      name = PartitionUtils.getTableName(name, masterTable.getProgram());
    } else if (masterTable.getTrackedEntityType() != null) {
      name += PartitionUtils.SEP + masterTable.getTrackedEntityType().getUid().toLowerCase();
    }
    if (year != null) {
      name += PartitionUtils.SEP + year;
    }

    return name;
  }

  public String getTableName() {
    return getTableName(false);
  }

  public String getTempTableName() {
    return getTableName(true);
  }

  public boolean isLatestPartition() {
    return Objects.equals(year, LATEST_PARTITION);
  }

  public AnalyticsTable getMasterTable() {
    return masterTable;
  }

  public Integer getYear() {
    return year;
  }

  public Date getStartDate() {
    return startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public boolean isDataApproval() {
    return dataApproval;
  }

  @Override
  public String toString() {
    return getTableName();
  }
}
