/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.model;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.db.model.constraint.Unique;

/**
 * Represents a database index.
 *
 * @author Lars Helge Overland
 */
@Getter
@RequiredArgsConstructor
public class Index {
  /** Index name. Required. */
  private final String name;

  /** Index type, defaults to {@link IndexType.BTREE}. Required. */
  private final IndexType indexType;

  /** Index uniqueness constraint. Required. */
  private final Unique unique;

  /** Index column names. Required. */
  private final List<String> columns;

  /** SQL {©code where} condition for index. Optional. */
  private final String condition;

  /**
   * Constructor.
   *
   * @param name the index name.
   * @param columns the list of index column names.
   */
  public Index(String name, List<String> columns) {
    this.name = name;
    this.indexType = IndexType.BTREE;
    this.unique = Unique.NON_UNIQUE;
    this.columns = columns;
    this.condition = null;
  }

  /**
   * Constructor.
   *
   * @param name the index name.
   * @param unique the uniqueness property.
   * @param columns the list of index column names.
   */
  public Index(String name, Unique unique, List<String> columns) {
    this.name = name;
    this.indexType = IndexType.BTREE;
    this.unique = unique;
    this.columns = columns;
    this.condition = null;
  }
}
