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
package org.hisp.dhis.db.sql;

import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.IndexFunction;
import org.hisp.dhis.db.model.IndexType;

/**
 * Abstract SQL builder class.
 *
 * @author Lars Helge Overland
 */
public abstract class AbstractSqlBuilder implements SqlBuilder {

  protected static final String QUOTE = "\"";
  protected static final String SINGLE_QUOTE = "'";
  protected static final String BACKSLASH = "\\";
  protected static final String COMMA = ", ";
  protected static final String DOT = ".";
  protected static final String EMPTY = "";
  protected static final String ALIAS_AX = "ax";

  /**
   * Returns the database name of the given data type.
   *
   * @param dataType the {@link DataType}.
   * @return the data type name.
   */
  @Override
  public String getDataTypeName(DataType dataType) {
    switch (dataType) {
      case SMALLINT:
        return dataTypeSmallInt();
      case INTEGER:
        return dataTypeInteger();
      case BIGINT:
        return dataTypeBigInt();
      case NUMERIC:
        return dataTypeNumeric();
      case REAL:
        return dataTypeReal();
      case DOUBLE:
        return dataTypeDouble();
      case BOOLEAN:
        return dataTypeBoolean();
      case CHARACTER_11:
        return dataTypeCharacter(11);
      case CHARACTER_32:
        return dataTypeCharacter(32);
      case VARCHAR_50:
        return dataTypeVarchar(50);
      case VARCHAR_255:
        return dataTypeVarchar(255);
      case VARCHAR_1200:
        return dataTypeVarchar(1200);
      case TEXT:
        return dataTypeText();
      case DATE:
        return dataTypeDate();
      case TIMESTAMP:
        return dataTypeTimestamp();
      case TIMESTAMPTZ:
        return dataTypeTimestampTz();
      case TIME:
        return dataTypeTime();
      case TIMETZ:
        return dataTypeTimeTz();
      case GEOMETRY:
        return dataTypeGeometry();
      case GEOMETRY_POINT:
        return dataTypeGeometryPoint();
      case JSONB:
        return dataTypeJsonb();
      default:
        throw new UnsupportedOperationException(
            String.format("Unsuported data type: %s", dataType));
    }
  }

  /**
   * Returns the database name of the given index type.
   *
   * @param indexType the {@link IndexType}.
   * @return the index type name.
   */
  @Override
  public String getIndexTypeName(IndexType indexType) {
    switch (indexType) {
      case BTREE:
        return indexTypeBtree();
      case GIST:
        return indexTypeGist();
      case GIN:
        return indexTypeGin();
      default:
        throw new UnsupportedOperationException(
            String.format("Unsuported index type: %s", indexType));
    }
  }

  /**
   * Returns the database name of the given index function.
   *
   * @param indexFunction the {@link IndexFunction}.
   * @return the index function name.
   */
  @Override
  public String getIndexFunctionName(IndexFunction indexFunction) {
    switch (indexFunction) {
      case UPPER:
        return indexFunctionUpper();
      case LOWER:
        return indexFunctionLower();
      default:
        throw new UnsupportedOperationException(
            String.format("Unsuported index function: %s", indexFunction));
    }
  }

  /**
   * Returns a quoted column string. If the index has a function, the quoted column is wrapped in
   * the function call.
   *
   * @param index the {@link Index}.
   * @param column the column name.
   * @return an index column string.
   */
  protected String toIndexColumn(Index index, String column) {
    String functionName = index.hasFunction() ? getIndexFunctionName(index.getFunction()) : null;
    String indexColumn = quote(column);
    return index.hasFunction() ? String.format("%s(%s)", functionName, indexColumn) : indexColumn;
  }
}
