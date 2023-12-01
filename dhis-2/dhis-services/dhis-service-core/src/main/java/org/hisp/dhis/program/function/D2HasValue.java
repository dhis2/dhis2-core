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
package org.hisp.dhis.program.function;

import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_BOOLEAN_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.program.ProgramExpressionItem;

/**
 * Program indicator function: d2 has value
 *
 * @author Jim Grace
 */
public class D2HasValue extends ProgramExpressionItem {
  @Override
  public Object getDescription(ExprContext ctx, CommonExpressionVisitor visitor) {
    getProgramArgType(ctx).getDescription(ctx, visitor);

    return DEFAULT_BOOLEAN_VALUE;
  }

  @Override
  public Object getSql(ExprContext ctx, CommonExpressionVisitor visitor) {
    boolean savedReplaceNulls = visitor.getState().isReplaceNulls();

    visitor.getState().setReplaceNulls(false);

    String argSql = (String) getProgramArgType(ctx).getSql(ctx, visitor);

    visitor.getState().setReplaceNulls(savedReplaceNulls);

    return "(" + argSql + " is not null)";
  }
}
