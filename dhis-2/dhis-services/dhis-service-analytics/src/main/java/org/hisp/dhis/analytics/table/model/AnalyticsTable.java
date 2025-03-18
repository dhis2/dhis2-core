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
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.TablePartition;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityType;

/**
 * Class representing an analytics database table. Note that the table name initially represents a
 * staging table. The name of the main table can be retrieved with {@link
 * AnalyticsTable#getMainName()}.
 *
 * @author Lars Helge Overland
 */
@Getter
public class AnalyticsTable extends Table {
  /** Analytics table type. */
  private final AnalyticsTableType tableType;

  /** Columns representing dimensions. */
  private final List<AnalyticsTableColumn> analyticsTableColumns;

  /** Program of events in analytics table. */
  private Program program;

  /** Tracked entity type of enrollments in analytics table. */
  private TrackedEntityType trackedEntityType;

  /** Analytics table partitions for this base analytics table. */
  private List<AnalyticsTablePartition> tablePartitions = new UniqueArrayList<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Constructor. Sets the name to represent a staging table.
   *
   * @param tableType the {@link AnalyticsTableType}.
   * @param columns the list of {@link Column}.
   * @param sortKey the sort key.
   * @param logged the {@link Logged} property.
   */
  public AnalyticsTable(
      AnalyticsTableType tableType,
      List<AnalyticsTableColumn> columns,
      List<String> sortKey,
      Logged logged) {
    super(
        toStaging(tableType.getTableName()),
        toColumns(columns),
        List.of(),
        sortKey,
        List.of(),
        logged);
    this.tableType = tableType;
    this.analyticsTableColumns = columns;
  }

  /**
   * Constructor. Sets the name to represent a staging table.
   *
   * @param tableType the {@link AnalyticsTableType}.
   * @param columns the list of {@link Column}.
   * @param logged the {@link Logged} property.
   * @param program the {@link Program}.
   */
  public AnalyticsTable(
      AnalyticsTableType tableType,
      List<AnalyticsTableColumn> columns,
      Logged logged,
      Program program) {
    super(toStaging(getTableName(tableType, program)), toColumns(columns), List.of(), logged);
    this.tableType = tableType;
    this.analyticsTableColumns = columns;
    this.program = program;
  }

  /**
   * Constructor. Sets the name to represent a staging table.
   *
   * @param tableType the {@link AnalyticsTableType}.
   * @param columns the list of {@link Column}.
   * @param logged the {@link Logged} property.
   * @param trackedEntityType the {@link TrackedEntityType}.
   */
  public AnalyticsTable(
      AnalyticsTableType tableType,
      List<AnalyticsTableColumn> columns,
      Logged logged,
      TrackedEntityType trackedEntityType) {
    super(
        toStaging(getTableName(tableType, trackedEntityType)),
        toColumns(columns),
        List.of(),
        logged);
    this.tableType = tableType;
    this.analyticsTableColumns = columns;
    this.trackedEntityType = trackedEntityType;
  }

  // -------------------------------------------------------------------------
  // Static methods
  // -------------------------------------------------------------------------

  /**
   * Converts the given list of analytics table columns to a list of columns.
   *
   * @param columns the list of {@link AnalyticsTableColumn}.
   * @return a list of {@link Column}.
   */
  private static List<Column> toColumns(List<AnalyticsTableColumn> columns) {
    return columns.stream()
        .map(c -> new Column(c.getName(), c.getDataType(), c.getNullable(), c.getCollation()))
        .toList();
  }

  /**
   * Converts the given analytics table partition to a table partition.
   *
   * @param partition the {@link AnalyticsTablePartition}.
   * @return a {@link TablePartition}.
   */
  private TablePartition toTablePartition(AnalyticsTablePartition partition) {
    return new TablePartition(partition.getName(), "year", partition.getYear());
  }

  /**
   * Returns a table name.
   *
   * @param tableType the {@link AnalyticsTableType}.
   * @param program the {@link Program}.
   * @return the table name.
   */
  public static String getTableName(AnalyticsTableType tableType, Program program) {
    return tableType.getTableName() + "_" + program.getUid().toLowerCase();
  }

  /**
   * Returns a table name.
   *
   * @param tableType the {@link AnalyticsTableType}.
   * @param trackedEntityType the {@link TrackedEntityType}.
   * @return the table name.
   */
  public static String getTableName(
      AnalyticsTableType tableType, TrackedEntityType trackedEntityType) {
    return tableType.getTableName() + "_" + trackedEntityType.getUid().toLowerCase();
  }

  // -------------------------------------------------------------------------
  // Logic methods
  // -------------------------------------------------------------------------

  /**
   * Returns the name which represents the main analytics table.
   *
   * @return the name which represents the main analytics table.
   */
  public String getMainName() {
    return fromStaging(getName());
  }

  /**
   * Returns columns of analytics value type dimension.
   *
   * @return a list of {@link AnalyticsTableColumn}.
   */
  public List<AnalyticsTableColumn> getDimensionColumns() {
    return analyticsTableColumns.stream()
        .filter(c -> AnalyticsValueType.DIMENSION == c.getValueType())
        .toList();
  }

  /**
   * Returns columns of analytics value type fact.
   *
   * @return a list of {@link AnalyticsTableColumn}.
   */
  public List<AnalyticsTableColumn> getFactColumns() {
    return analyticsTableColumns.stream()
        .filter(c -> AnalyticsValueType.FACT == c.getValueType())
        .toList();
  }

  /**
   * Adds an analytics partition table to this master table.
   *
   * @param checks the partition checks.
   * @param year the year.
   * @param startDate the start date.
   * @param endDate the end date.
   * @return this analytics table.
   */
  public AnalyticsTable addTablePartition(
      List<String> checks, Integer year, Date startDate, Date endDate) {
    Objects.requireNonNull(year);

    AnalyticsTablePartition partition =
        new AnalyticsTablePartition(this, checks, year, startDate, endDate);

    super.addPartition(toTablePartition(partition));

    this.tablePartitions.add(partition);

    return this;
  }

  /**
   * Indicates whether this analytics table has any partitions.
   *
   * @return true if this analytics table has any partitions.
   */
  public boolean hasTablePartitions() {
    return !tablePartitions.isEmpty();
  }

  /**
   * Returns the latest partition, or null if no latest partition exists.
   *
   * @return a {@link AnalyticsTablePartition} or null.
   */
  public AnalyticsTablePartition getLatestTablePartition() {
    return tablePartitions.stream()
        .filter(AnalyticsTablePartition::isLatestPartition)
        .findAny()
        .orElse(null);
  }

  // implement toString method

  @Override
  public String toString() {
    return "[Table name: "
        + getName()
        + ", partitions: "
        + (tablePartitions.isEmpty()
            ? ""
            : StringUtils.join(",", tablePartitions.stream().map(Table::getName).toArray()))
        + "]";
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
