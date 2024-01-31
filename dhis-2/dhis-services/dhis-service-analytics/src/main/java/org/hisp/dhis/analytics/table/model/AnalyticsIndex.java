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

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hisp.dhis.db.model.IndexType;

/**
 * Class representing an index on a database table column.
 *
 * @author Lars Helge Overland
 */
@Getter
@EqualsAndHashCode
public class AnalyticsIndex {
  /** Table name. */
  private final String tableName;

  /** Index type. */
  private final IndexType indexType;

  /** Table column names. */
  private final List<String> columns;

  /** Function to be used by the index. Optional. */
  private IndexFunction function;

  /**
   * Constructor.
   *
   * @param tableName the table name.
   * @param indexType the index type.
   * @param columns the index column names.
   */
  public AnalyticsIndex(String tableName, IndexType indexType, List<String> columns) {
    this.tableName = tableName;
    this.indexType = indexType;
    this.columns = columns;
  }

  /**
   * Constructor.
   *
   * @param tableName the table name.
   * @param indexType the index type.
   * @param columns the index column names.
   * @param function the index function.
   */
  public AnalyticsIndex(
      String tableName, IndexType indexType, List<String> columns, IndexFunction function) {
    this(tableName, indexType, columns);
    this.function = function;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /**
   * Indicates whether the index has a function.
   *
   * @return true if the index has a function.
   */
  public boolean hasFunction() {
    return function != null;
  }
}
