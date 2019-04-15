package org.hisp.dhis.parser.expression.function;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.parser.expression.ExprVisitor;

import static org.hisp.dhis.parser.expression.ParserUtils.castClass;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

/**
 * Expression function If
 *
 * @author Jim Grace
 */
public class FunctionIf
    extends AbstractExpressionFunction
{
    @Override
    public Object evaluate( ExprContext ctx, ExprVisitor visitor )
    {
        Boolean arg0 = visitor.castBooleanVisit( ctx.expr( 0 ) );
        Object arg1 = visitor.visit( ctx.expr( 1 ) );
        Object arg2 = visitor.visit( ctx.expr( 2 ) );

        if ( arg1 != null )
        {
            castClass( arg1.getClass(), arg2 );
        }

        return arg0 ? arg1 : arg2;
    }

    @Override
    public Object evaluateConditional( ExprContext ctx, ExprVisitor visitor )
    {
        return visitor.castBooleanVisit( ctx.expr( 0 ) )
            ? visitor.visit( ctx.expr( 1 ) )
            : visitor.visit( ctx.expr( 2 ) );
    }

    @Override
    public Object getSql( ExprContext ctx, ExprVisitor visitor )
    {
        return " case when " + visitor.castStringVisit( ctx.expr( 0 ) ) +
            " then " + visitor.castStringVisit( ctx.expr( 1 ) ) +
            " else " + visitor.castStringVisit( ctx.expr( 2 ) ) + " end";
    }
}
