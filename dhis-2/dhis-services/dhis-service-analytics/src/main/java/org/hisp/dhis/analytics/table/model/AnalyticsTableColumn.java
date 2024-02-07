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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.constraint.Nullable;

/**
 * Class representing an analytics database table column.
 *
 * @author Lars Helge Overland
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnalyticsTableColumn {
  /** Column name. */
  @EqualsAndHashCode.Include private final String name;

  /** Column data type. */
  private final DataType dataType;

  /** Column not null constraint, default is to allow null values. */
  private final Nullable nullable;

  /** Column collation. */
  private final Collation collation;

  /** Column analytics value type, i.e. dimension or fact. */
  private final AnalyticsValueType valueType;

  /** The expression to use in select clauses. */
  private final String selectExpression;

  /** Whether to skip or include an index for column. */
  private final Skip skipIndex;

  /** Index type, defaults to database default type {@link IndexType#BTREE}. */
  private final IndexType indexType;

  /** Index column names, defaults to column name. */
  private final List<String> indexColumns;

  /** Date of creation of the underlying data dimension. */
  private Date created;

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
  public AnalyticsTableColumn(String name, DataType dataType, String selectExpression) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = Nullable.NULL;
    this.collation = Collation.DEFAULT;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.selectExpression = selectExpression;
    this.skipIndex = Skip.INCLUDE;
    this.indexType = IndexType.BTREE;
    this.indexColumns = List.of();
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
      String name, DataType dataType, Collation collation, String selectExpression) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = Nullable.NULL;
    this.collation = collation;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.selectExpression = selectExpression;
    this.skipIndex = Skip.INCLUDE;
    this.indexType = IndexType.BTREE;
    this.indexColumns = List.of();
  }

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param nullable analytics table column not null constraint.
   * @param selectExpression source table select expression.
   */
  public AnalyticsTableColumn(
      String name, DataType dataType, Nullable nullable, String selectExpression) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = nullable;
    this.collation = Collation.DEFAULT;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.selectExpression = selectExpression;
    this.skipIndex = Skip.INCLUDE;
    this.indexType = IndexType.BTREE;
    this.indexColumns = List.of();
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
      String name, DataType dataType, String selectExpression, Skip skipIndex) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = Nullable.NULL;
    this.collation = Collation.DEFAULT;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.selectExpression = selectExpression;
    this.skipIndex = skipIndex;
    this.indexType = IndexType.BTREE;
    this.indexColumns = List.of();
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
      String name, DataType dataType, String selectExpression, IndexType indexType) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = Nullable.NULL;
    this.collation = Collation.DEFAULT;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.selectExpression = selectExpression;
    this.skipIndex = Skip.INCLUDE;
    this.indexType = indexType;
    this.indexColumns = List.of();
  }

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param selectExpression source table select expression.
   * @param created the created date.
   */
  public AnalyticsTableColumn(
      String name, DataType dataType, String selectExpression, Date created) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = Nullable.NULL;
    this.collation = Collation.DEFAULT;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.selectExpression = selectExpression;
    this.skipIndex = Skip.INCLUDE;
    this.indexType = IndexType.BTREE;
    this.indexColumns = List.of();
    this.created = created;
  }

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param nullable analytics table column not null constraint.
   * @param selectExpression source table select expression.
   * @param indexColumns index column names.
   */
  public AnalyticsTableColumn(
      String name,
      DataType dataType,
      Nullable nullable,
      String selectExpression,
      List<String> indexColumns) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = nullable;
    this.collation = Collation.DEFAULT;
    this.valueType = AnalyticsValueType.DIMENSION;
    this.selectExpression = selectExpression;
    this.skipIndex = Skip.INCLUDE;
    this.indexType = IndexType.BTREE;
    this.indexColumns = indexColumns;
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
      DataType dataType,
      Nullable notNull,
      AnalyticsValueType valueType,
      String selectExpression) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = notNull;
    this.collation = Collation.DEFAULT;
    this.valueType = valueType;
    this.skipIndex = Skip.INCLUDE;
    this.selectExpression = selectExpression;
    this.indexType = IndexType.BTREE;
    this.indexColumns = List.of();
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
    return Skip.SKIP == skipIndex;
  }

  @Override
  public String toString() {
    return "[" + name + " " + dataType + " " + nullable + "]";
  }
}
