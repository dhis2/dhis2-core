/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.hisp.dhis.db.model.constraint.Nullable;

/**
 * Represents a database table column.
 *
 * @author Lars Helge Overland
 */
@Getter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Column {
  /** Column name. Required. */
  @EqualsAndHashCode.Include private final String name;

  /** Column data type. Required. */
  private final DataType dataType;

  /** Column not null constraint. Required. */
  private final Nullable nullable;

  /** Column collation. Required. */
  private final Collation collation;

  /**
   * Constructor.
   *
   * @param name the column name.
   * @param dataType the {@link DataType}.
   */
  public Column(String name, DataType dataType) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = Nullable.NULL;
    this.collation = Collation.DEFAULT;
  }

  /**
   * Constructor.
   *
   * @param name the column name.
   * @param dataType the {@link DataType}.
   * @param nullable the {@link Nullable} constraint.
   */
  public Column(String name, DataType dataType, Nullable nullable) {
    this.name = name;
    this.dataType = dataType;
    this.nullable = nullable;
    this.collation = Collation.DEFAULT;
  }

  /**
   * Indicates whether the column is not null.
   *
   * @return true if the column is not null.
   */
  public boolean isNotNull() {
    return Nullable.NOT_NULL == nullable;
  }

  /**
   * Indicates whether the collation is set to a non-default value.
   *
   * @return true if the collation is set to a non-default value.
   */
  public boolean hasCollation() {
    return collation != null && Collation.DEFAULT != collation;
  }
}
