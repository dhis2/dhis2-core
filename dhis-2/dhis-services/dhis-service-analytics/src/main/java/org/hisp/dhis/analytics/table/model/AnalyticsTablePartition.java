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
package org.hisp.dhis.analytics.table.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.hisp.dhis.db.model.Table;

/**
 * Class representing an analytics database table partition.
 *
 * @author Lars Helge Overland
 */
@Getter
public class AnalyticsTablePartition extends Table {
  public static final Integer LATEST_PARTITION = 0;

  /** The master analytics table for this partition. */
  private final AnalyticsTable masterTable;

  /**
   * The year for which this partition may contain data, where 0 indicates the "latest" data stored
   * since last full analytics table generation.
   */
  private final Integer year;

  /** The start date for which this partition may contain data, inclusive. */
  private final Date startDate;

  /** The end date for which this partition may contain data, exclusive. */
  private final Date endDate;

  /**
   * Constructor. Sets the name to represent a staging table partition.
   *
   * @param masterTable the master {@link Table} of this partition.
   * @param checks the partition checks.
   * @param year the year which represents this partition.
   * @param startDate the start date of data for this partition.
   * @param endDate the end date of data for this partition.
   */
  public AnalyticsTablePartition(
      AnalyticsTable masterTable, List<String> checks, Integer year, Date startDate, Date endDate) {
    super(
        toStaging(getTableName(masterTable.getMainName(), year)),
        List.of(),
        List.of(),
        checks,
        masterTable.getLogged(),
        masterTable);
    this.masterTable = masterTable;
    this.year = year;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  /**
   * Constructor. Sets the name to represent a staging table partition.
   *
   * @param table the master {@link AnalyticsTable} of this partition.
   */
  public AnalyticsTablePartition(AnalyticsTable table) {
    super(
        toStaging(getTableName(table.getMainName(), null)),
        List.of(),
        List.of(),
        List.of(),
        table.getLogged(),
        table);
    this.masterTable = table;
    this.year = null;
    this.startDate = null;
    this.endDate = null;
  }

  // -------------------------------------------------------------------------
  // Static methods
  // -------------------------------------------------------------------------

  /**
   * Returns a table partition name.
   *
   * @param baseName the base name.
   * @param year the year.
   * @return a table partition name.
   */
  private static String getTableName(String baseName, Integer year) {
    String name = baseName;

    if (year != null) {
      name += "_" + year;
    }

    return name;
  }

  // -------------------------------------------------------------------------
  // Logic methods
  // -------------------------------------------------------------------------

  /**
   * Returns the main table partition name.
   *
   * @return the main table partition name.
   */
  public String getMainName() {
    return fromStaging(getName());
  }

  /**
   * Indicates whether this partition represents the latest data partition.
   *
   * @return true if this partition represents the latest data partition.
   */
  public boolean isLatestPartition() {
    return Objects.equals(year, LATEST_PARTITION);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object != null && getClass() != object.getClass()) {
      return false;
    }
    return super.equals(object);
  }
}
