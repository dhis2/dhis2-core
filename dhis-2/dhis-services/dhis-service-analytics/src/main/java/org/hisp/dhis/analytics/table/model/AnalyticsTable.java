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
package org.hisp.dhis.analytics.table.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.springframework.util.Assert;

/**
 * Class representing an analytics database table.
 *
 * @author Lars Helge Overland
 */
@Getter
public class AnalyticsTable {
  /** Table name. */
  private String tableName;

  /** Temporary table name. */
  private String tempTableName;

  /** Analytics table type. */
  private AnalyticsTableType tableType;

  /** Columns representing dimensions. */
  private List<AnalyticsTableColumn> columns;

  /** Program of events in analytics table. */
  private Program program;

  /** Tracked entity type of enrollments in analytics table. */
  private TrackedEntityType trackedEntityType;

  /** Analytics table partitions for this base analytics table. */
  private List<AnalyticsTablePartition> tablePartitions = new UniqueArrayList<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public AnalyticsTable() {}

  public AnalyticsTable(AnalyticsTableType tableType, List<AnalyticsTableColumn> columns) {
    this.tableName = tableType.getTableName();
    this.tempTableName = tableType.getTempTableName();
    this.tableType = tableType;
    this.columns = columns;
  }

  public AnalyticsTable(
      AnalyticsTableType tableType, List<AnalyticsTableColumn> columns, Program program) {
    this.tableName = getTableName(tableType.getTableName(), program);
    this.tempTableName = getTableName(tableType.getTempTableName(), program);
    this.tableType = tableType;
    this.columns = columns;
    this.program = program;
  }

  public AnalyticsTable(
      AnalyticsTableType tableType,
      List<AnalyticsTableColumn> columns,
      TrackedEntityType trackedEntityType) {
    this.tableName = getTableName(tableType.getTableName(), trackedEntityType);
    this.tempTableName = getTableName(tableType.getTempTableName(), trackedEntityType);
    this.tableType = tableType;
    this.columns = columns;
    this.trackedEntityType = trackedEntityType;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /**
   * Returns a table name.
   *
   * @param baseName the table base name.
   * @param program the {@link Program}.
   * @return the table name.
   */
  public static String getTableName(String baseName, Program program) {
    return baseName + "_" + program.getUid().toLowerCase();
  }

  /**
   * Returns a table name.
   *
   * @param baseName the table base name.
   * @param trackedEntityType the {@link TrackedEntityType}.
   * @return the table name.
   */
  public static String getTableName(String baseName, TrackedEntityType trackedEntityType) {
    return baseName + "_" + trackedEntityType.getUid().toLowerCase();
  }

  /**
   * Returns columns of analytics value type dimension.
   *
   * @return a list of {@link AnalyticsTableColumn}.
   */
  public List<AnalyticsTableColumn> getDimensionColumns() {
    return columns.stream().filter(c -> AnalyticsValueType.DIMENSION == c.getValueType()).toList();
  }

  /**
   * Returns columns of analytics value type fact.
   *
   * @return a list of {@link AnalyticsTableColumn}.
   */
  public List<AnalyticsTableColumn> getFactColumns() {
    return columns.stream().filter(c -> AnalyticsValueType.FACT == c.getValueType()).toList();
  }

  /**
   * Returns the count of all columns.
   *
   * @return the count of all columns.
   */
  public int getColumnCount() {
    return getColumns().size();
  }

  /**
   * Adds an analytics partition table to this master table.
   *
   * @param year the year.
   * @param startDate the start date.
   * @param endDate the end date.
   * @return this analytics table.
   */
  public AnalyticsTable addPartitionTable(Integer year, Date startDate, Date endDate) {
    Assert.notNull(year, "Year must be specified");

    AnalyticsTablePartition tablePartition =
        new AnalyticsTablePartition(this, year, startDate, endDate);

    this.tablePartitions.add(tablePartition);

    return this;
  }

  public String getBaseName() {
    return tableType.getTableName();
  }

  public boolean hasPartitionTables() {
    return !tablePartitions.isEmpty();
  }

  public AnalyticsTablePartition getLatestPartition() {
    return tablePartitions.stream()
        .filter(AnalyticsTablePartition::isLatestPartition)
        .findAny()
        .orElse(null);
  }

  // -------------------------------------------------------------------------
  // hashCode, equals, toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    return Objects.hash(tableName, tableType);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null) {
      return false;
    }

    if (getClass() != object.getClass()) {
      return false;
    }

    AnalyticsTable other = (AnalyticsTable) object;

    return Objects.equals(tableName, other.tableName) && Objects.equals(tableType, other.tableType);
  }

  @Override
  public String toString() {
    return "[Table name: " + tableName + ", partitions: " + tablePartitions + "]";
  }
}
