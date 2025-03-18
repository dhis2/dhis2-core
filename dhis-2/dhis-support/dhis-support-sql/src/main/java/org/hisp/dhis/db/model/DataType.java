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

import java.util.EnumSet;

/**
 * Enumeration of database data types.
 *
 * @author Lars Helge Overland
 */
public enum DataType {
  SMALLINT,
  BIGINT,
  INTEGER,
  DECIMAL,
  FLOAT,
  DOUBLE,
  BOOLEAN,
  CHARACTER_11,
  VARCHAR_50,
  VARCHAR_255,
  TEXT,
  DATE,
  TIMESTAMP,
  TIMESTAMPTZ,
  GEOMETRY,
  GEOMETRY_POINT,
  JSONB;

  private static final EnumSet<DataType> TYPES_NUMERIC =
      EnumSet.of(SMALLINT, BIGINT, INTEGER, DECIMAL, FLOAT, DOUBLE);

  private static final EnumSet<DataType> TYPES_CHARACTER =
      EnumSet.of(CHARACTER_11, VARCHAR_50, VARCHAR_255, TEXT);

  /**
   * Indicates if the data type is numeric.
   *
   * @return true if numeric.
   */
  public boolean isNumeric() {
    return TYPES_NUMERIC.contains(this);
  }

  /**
   * Indicates if the data type is boolean.
   *
   * @return true if boolean.
   */
  public boolean isBoolean() {
    return BOOLEAN == this;
  }

  /**
   * Indicates if the data type is character based.
   *
   * @return true if character based.
   */
  public boolean isCharacter() {
    return TYPES_CHARACTER.contains(this);
  }
}
