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

/**
 * Class representing an analytics database table column.
 *
 * @author Lars Helge Overland
 */
public class AnalyticsTableColumn {
  /** The column name. */
  private final String name;

  /** The column data type. */
  private final ColumnDataType dataType;

  /** Column not null constraint, default is to allow null values. */
  private ColumnNotNullConstraint notNull = ColumnNotNullConstraint.NULL;

  /** The column SQL alias. */
  private String alias;

  /** Date of creation of the underlying data dimension. */
  private Date created;

  /** Whether to skip building an index for this column. */
  private boolean skipIndex = false;

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
   * @param alias source table column alias and name.
   */
  public AnalyticsTableColumn(String name, ColumnDataType dataType, String alias) {
    this.name = name;
    this.dataType = dataType;
    this.notNull = ColumnNotNullConstraint.NULL;
    this.alias = alias;
  }

  /**
   * Constructor.
   *
   * @param name analytics table column name.
   * @param dataType analytics table column data type.
   * @param notNull analytics table column not null constraint.
   * @param alias source table column alias and name.
   */
  public AnalyticsTableColumn(
      String name, ColumnDataType dataType, ColumnNotNullConstraint notNull, String alias) {
    this(name, dataType, alias);
    this.notNull = notNull;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Indicates whether explicit index columns have been specified, defaults to this column name. */
  public boolean hasIndexColumns() {
    return !indexColumns.isEmpty();
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
   * Sets the index type.
   *
   * @param indexType the index type.
   */
  public AnalyticsTableColumn withIndexType(IndexType indexType) {
    this.indexType = indexType;
    return this;
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  public String getName() {
    return name;
  }

  public ColumnDataType getDataType() {
    return dataType;
  }

  public String getAlias() {
    return alias;
  }

  public ColumnNotNullConstraint getNotNull() {
    return notNull;
  }

  public Date getCreated() {
    return created;
  }

  public boolean isSkipIndex() {
    return skipIndex;
  }

  public IndexType getIndexType() {
    return indexType;
  }

  public List<String> getIndexColumns() {
    return indexColumns;
  }
}
