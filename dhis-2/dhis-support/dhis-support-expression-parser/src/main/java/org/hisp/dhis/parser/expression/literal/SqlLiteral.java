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
package org.hisp.dhis.parser.expression.literal;

import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.hisp.dhis.antlr.AntlrParserUtils.trimQuotes;
import static org.hisp.dhis.system.util.SqlUtils.escape;

import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.AntlrExprLiteral;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser.BooleanLiteralContext;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser.NumericLiteralContext;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser.StringLiteralContext;

/** Gets literal value Strings from an ANTLR parse tree for use in SQL queries. */
public class SqlLiteral implements AntlrExprLiteral {

  private final SqlBuilder sqlBuilder;

  public SqlLiteral(SqlBuilder sqlBuilder) {
    this.sqlBuilder = sqlBuilder;
  }

  @Override
  public Object getNumericLiteral(NumericLiteralContext ctx) {
    return sqlBuilder.cast(ctx.getText(), DataType.NUMERIC);
  }

  @Override
  public Object getStringLiteral(StringLiteralContext ctx) {
    return "'" + escape(unescapeJava(trimQuotes(ctx.getText()))) + "'";
  }

  @Override
  public Object getBooleanLiteral(BooleanLiteralContext ctx) {
    return ctx.getText();
  }
}
