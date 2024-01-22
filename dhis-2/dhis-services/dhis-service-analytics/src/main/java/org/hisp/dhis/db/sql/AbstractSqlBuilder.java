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

public abstract class AbstractSqlBuilder implements SqlBuilder {
  protected String getDataTypeName(DataType dataType) {
    switch (dataType) {
      case SMALLINT:
        return typeSmallInt();
      case INTEGER:
        return typeInteger();
      case BIGINT:
        return typeBigInt();
      case NUMERIC:
        return typeNumeric();
      case REAL:
        return typeReal();
      case DOUBLE:
        return typeDouble();
      case BOOLEAN:
        return typeBoolean();
      case CHARACTER_11:
        return typeCharacter(11);
      case CHARACTER_32:
        return typeCharacter(32);
      case VARCHAR_50:
        return typeVarchar(50);
      case VARCHAR_255:
        return typeVarchar(255);
      case VARCHAR_1200:
        return typeVarchar(1200);
      case TEXT:
        return typeText();
      case DATE:
        return typeDate();
      case TIMESTAMP:
        return typeTimestamp();
      case TIMESTAMPTZ:
        return typeTimestampTz();
      case TIME:
        return typeTime();
      case TIMETZ:
        return typeTimeTz();
      case GEOMETRY:
        return typeGeometry();
      case GEOMETRY_POINT:
        return typeGeometryPoint();
      case JSONB:
        return typeJsonb();
      default:
        throw new UnsupportedOperationException(
            String.format("Unsuported data type: %s", dataType));
    }
  }
}
