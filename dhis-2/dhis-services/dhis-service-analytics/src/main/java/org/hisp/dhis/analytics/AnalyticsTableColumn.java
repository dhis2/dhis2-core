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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Class representing an analytics database table column.
 *
 * @author Lars Helge Overland
 */
@Getter
@EqualsAndHashCode
public class AnalyticsTableColumn {
  /** The column name. */
  private final String name;

  /** The column data type. */
  private final ColumnDataType dataType;

  /** Column not null constraint, default is to allow null values. */
  private ColumnNotNullConstraint notNull = ColumnNotNullConstraint.NULL;

  /** The expression to use in select clauses. */
  private final String selectExpression;

  /** The column collation. */
  private Collation collation;

  /** Explicit index type, defaults to database default type {@link IndexType#BTREE}. */
  private IndexType indexType = IndexType.BTREE;

  /** Whether to skip building an index for this column. */
  private boolean skipIndex = false;

  /** Date of creation of the underlying data dimension. */
  private Date created;

  /** Explicit index column names, defaults to column name. */
  private List<String> indexColumns = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param selectExpression source table select expression.
   */
  public AnalyticsTableColumn(String name, ColumnDataType dataType, String selectExpression) {
    this.name = name;
    this.dataType = dataType;
    this.selectExpression = selectExpression;
  }

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param selectExpression source table select expression.
   */
  public AnalyticsTableColumn(
      String name, ColumnDataType dataType, String selectExpression, Collation collation) {
    this.name = name;
    this.dataType = dataType;
    this.notNull = ColumnNotNullConstraint.NULL;
    this.selectExpression = selectExpression;
    this.collation = collation;
  }

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param notNull analytics table column not null constraint.
   * @param selectExpression source table select expression.
   */
  public AnalyticsTableColumn(
      String name,
      ColumnDataType dataType,
      ColumnNotNullConstraint notNull,
      String selectExpression) {
    this(name, dataType, selectExpression);
    this.notNull = notNull;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Indicates whether explicit index columns are specified, defaults to this column name. */
  public boolean hasIndexColumns() {
    return !indexColumns.isEmpty();
  }

  /**
   * Indicates whether a collation is specified.
   *
   * @return
   */
  public boolean hasCollation() {
    return collation != null;
  }

  // -------------------------------------------------------------------------
  // Builder methods
  // -------------------------------------------------------------------------

  /**
   * Sets the created date.
   *
   * @param created the created date of the underlying dimension.
   */
  public AnalyticsTableColumn withCreated(Date created) {
    this.created = created;
    return this;
  }

  /**
   * Sets the index type.
   *
   * @param indexType the index type.
   */
  public AnalyticsTableColumn withIndexType(IndexType indexType) {
    this.indexType = indexType;
    return this;
  }

  /**
   * Sets whether to skip indexes.
   *
   * @param skipIndex indicates whether to skip indexing this column.
   */
  public AnalyticsTableColumn withSkipIndex(boolean skipIndex) {
    this.skipIndex = skipIndex;
    return this;
  }

  /**
   * Sets the index columns.
   *
   * @param indexColumns columns to index, defaults to this column name.
   */
  public AnalyticsTableColumn withIndexColumns(List<String> indexColumns) {
    this.indexColumns = indexColumns;
    return this;
  }
}
