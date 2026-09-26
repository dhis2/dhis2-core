/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.expression.dataitem;

import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.antlr.v4.runtime.CommonToken;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import org.junit.jupiter.api.Test;

class DimItemDataElementAndOperandTest {
  private static final String DE_UID = "deabcdefghA";

  private final DimItemDataElementAndOperand subject = new DimItemDataElementAndOperand();

  @Test
  void getSqlQuotesSubexpressionColumnForPostgres() {
    assertEquals("\"deabcdefghA\"", subject.getSql(context(), visitor(new PostgreSqlBuilder())));
  }

  @Test
  void getSqlQuotesSubexpressionColumnForDoris() {
    assertEquals("`deabcdefghA`", subject.getSql(context(), visitor(new DorisSqlBuilder("", ""))));
  }

  @Test
  void getSqlCastsBooleanSubexpressionColumnForDoris() {
    SqlBuilder sqlBuilder = new DorisSqlBuilder("", "");
    CommonExpressionVisitor visitor = visitor(sqlBuilder);
    visitor.getState().setReplaceNulls(true);

    DataElement dataElement = new DataElement();
    dataElement.setUid(DE_UID);
    dataElement.setValueType(ValueType.BOOLEAN);
    when(visitor.getIdObjectManager().get(DataElement.class, DE_UID)).thenReturn(dataElement);
    visitor.setParams(ExpressionParams.builder().dataType(BOOLEAN).build());

    assertEquals(
        "coalesce(CAST(`deabcdefghA` AS DECIMAL) != 0,false)", subject.getSql(context(), visitor));
  }

  private CommonExpressionVisitor visitor(SqlBuilder sqlBuilder) {
    CommonExpressionVisitor visitor =
        CommonExpressionVisitor.builder()
            .idObjectManager(mock(IdentifiableObjectManager.class))
            .sqlBuilder(sqlBuilder)
            .build();
    visitor.getState().setReplaceNulls(false);
    return visitor;
  }

  private ExprContext context() {
    ExprContext context = new ExprContext(null, 0);
    context.uid0 = new CommonToken(0, DE_UID);
    return context;
  }
}
