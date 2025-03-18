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
import lombok.Builder;
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
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnalyticsTableColumn {
  /** Column name. */
  @EqualsAndHashCode.Include private final String name;

  /** Column data type. */
  private final DataType dataType;

  /** Column not null constraint, default is to allow null values. */
  @Builder.Default private final Nullable nullable = Nullable.NULL;

  /** Column collation. */
  @Builder.Default private final Collation collation = Collation.DEFAULT;

  /** Column analytics value type, i.e. dimension or fact. */
  @Builder.Default private final AnalyticsValueType valueType = AnalyticsValueType.DIMENSION;

  /** The expression to use in select clauses. */
  private final String selectExpression;

  /** Whether to skip or include an index for column. */
  @Builder.Default private final Skip skipIndex = Skip.INCLUDE;

  /** Index type, defaults to database default type {@link IndexType#BTREE}. */
  @Builder.Default private final IndexType indexType = IndexType.BTREE;

  /** Index column names. */
  @Builder.Default private final List<String> indexColumns = List.of();

  /** The column type indicates the column origin. */
  @Builder.Default
  private final AnalyticsDimensionType dimensionType = AnalyticsDimensionType.STATIC;

  /** Date of creation of the underlying data dimension. */
  private final Date created;

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

  /** Indicates whether the column type is set to a non-default value. */
  public boolean isStaticDimension() {
    return AnalyticsDimensionType.STATIC == dimensionType;
  }

  @Override
  public String toString() {
    return "[" + name + " " + dataType + " " + nullable + "]";
  }
}
