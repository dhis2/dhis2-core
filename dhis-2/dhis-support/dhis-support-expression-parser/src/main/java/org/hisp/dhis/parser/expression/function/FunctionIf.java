/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.parser.expression.function;

import static org.hisp.dhis.antlr.AntlrParserUtils.castClass;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;

/**
 * Function if
 *
 * <pre>
 *
 * In-memory Logic:
 *
 *     arg0   returns
 *     ----   -------
 *     true   arg1
 *     false  arg2
 *     null   null
 *
 * SQL logic (CASE WHEN arg0 THEN arg1 ELSE arg2 END):
 *
 *     arg0   returns
 *     ----   -------
 *     true   arg1
 *     false  arg2
 *     null   arg2
 * </pre>
 *
 * @author Jim Grace
 */
public class FunctionIf
    implements ExpressionItem
{
    @Override
    public Object evaluate( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        Boolean arg0 = visitor.castBooleanVisit( ctx.expr( 0 ) );

        if ( arg0 == null )
        {
            return null;
        }

        return (arg0)
            ? visitor.visit( ctx.expr( 1 ) )
            : visitor.visit( ctx.expr( 2 ) );
    }

    @Override
    public Object evaluateAllPaths( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        Boolean arg0 = visitor.castBooleanVisit( ctx.expr( 0 ) );
        Object arg1 = visitor.visit( ctx.expr( 1 ) );
        Object arg2 = visitor.visit( ctx.expr( 2 ) );

        if ( arg1 != null )
        {
            castClass( arg1.getClass(), arg2 );
        }

        return arg0 != null && arg0 ? arg1 : arg2;
    }

    @Override
    public Object getSql( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        return " case when " + visitor.sqlBooleanVisit( ctx.expr( 0 ) ) +
            " then " + visitor.castStringVisit( ctx.expr( 1 ) ) +
            " else " + visitor.castStringVisit( ctx.expr( 2 ) ) + " end";
    }
}
