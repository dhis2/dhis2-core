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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.constraint.Nullable;

/**
 * Class representing an analytics database table column.
 *
 * @author Lars Helge Overland
 */
@Getter
@EqualsAndHashCode
public class AnalyticsTableColumn {
  /** Column name. */
  private final String name;

  /** Column data type. */
  private final ColumnDataType dataType;

  /** Column not null constraint, default is to allow null values. */
  private final Nullable nullable;

  /** Column collation. */
  private final Collation collation;

  /** Column analytics value type, i.e. dimension or fact. */
  private final AnalyticsValueType valueType;

  /** The expression to use in select clauses. */
  private final String selectExpression;

  /** Date of creation of the underlying data dimension. */
  private Date created;

  /** Explicit index type, defaults to database default type {@link IndexType#BTREE}. */
  private IndexType indexType = IndexType.BTREE;

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
    this.nullable = Nullable.NULL;
    this.collation = Collation.DEFAULT;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.selectExpression = selectExpression;
  }

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param collation the analytics table column collation.
   * @param selectExpression source table select expression.
   */
  public AnalyticsTableColumn(
      String name, ColumnDataType dataType, Collation collation, String selectExpression) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = Nullable.NULL;
    this.collation = collation;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.selectExpression = selectExpression;
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
      String name, ColumnDataType dataType, Nullable notNull, String selectExpression) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = notNull;
    this.collation = Collation.DEFAULT;
    this.selectExpression = selectExpression;
    this.valueType = AnalyticsValueType.DIMENSION;
  }

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param selectExpression source table select expression.
   * @param indexType the index type.
   */
  public AnalyticsTableColumn(
      String name, ColumnDataType dataType, String selectExpression, IndexType indexType) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = Nullable.NULL;
    this.collation = Collation.DEFAULT;
    this.selectExpression = selectExpression;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.indexType = indexType;
  }

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param selectExpression source table select expression.
   */
  public AnalyticsTableColumn(
      String name,
      ColumnDataType dataType,
      Nullable notNull,
      AnalyticsValueType valueType,
      String selectExpression) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = notNull;
    this.collation = Collation.DEFAULT;
    this.valueType = valueType;
    this.selectExpression = selectExpression;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Indicates whether this column is not null. */
  public boolean isNotNull() {
    return Nullable.NOT_NULL == nullable;
  }

  /** Indicates whether explicit index columns are specified, defaults to this column name. */
  public boolean hasIndexColumns() {
    return !indexColumns.isEmpty();
  }

  /** Indicates whether the collation is set to a non-default value. */
  public boolean hasCollation() {
    return collation != null && Collation.DEFAULT != collation;
  }

  /** Indicates whether an index should not be created for this column. */
  public boolean isSkipIndex() {
    return IndexType.NONE == indexType;
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
   * Sets the index columns.
   *
   * @param indexColumns columns to index, defaults to this column name.
   */
  public AnalyticsTableColumn withIndexColumns(List<String> indexColumns) {
    this.indexColumns = indexColumns;
    return this;
  }
}
