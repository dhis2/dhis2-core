package org.hisp.dhis.antlr;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.hisp.dhis.antlr.literal.DefaultLiteral;
import org.hisp.dhis.parser.expression.antlr.ExpressionBaseVisitor;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;

import static org.hisp.dhis.antlr.AntlrParserUtils.castBoolean;
import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.BooleanLiteralContext;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExpressionContext;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.NumericLiteralContext;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.StringLiteralContext;

/**
 * Generic traversal of the ANTLR4 expression parse tree using the
 * visitor pattern.
 *
 */
public abstract class AntlrExpressionVisitor
    extends ExpressionBaseVisitor<Object>
{
    /**
     * Instance to call for each literal
     */
    protected AntlrExprLiteral expressionLiteral = new DefaultLiteral();


    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected AntlrExpressionVisitor()
    {
    }

    // -------------------------------------------------------------------------
    // Visitor methods
    // -------------------------------------------------------------------------

    @Override
    public final Object visitExpression( ExpressionContext ctx )
    {
        return visit( ctx.expr() );
    }

    @Override
    public Object visitExpr( ExpressionParser.ExprContext ctx )
    {
        if ( ctx.fun != null )
        {
            AntlrExprFunction function = AntlrParserUtils.COMMON_EXPRESSION_FUNCTIONS.get( ctx.fun.getType() );

            if ( function == null )
            {
                throw new ParserExceptionWithoutContext(
                    "Function " + ctx.fun.getText() + " not supported for this type of expression" );
            }

            return function.evaluate( ctx, this );
        }

        if ( ctx.expr().size() > 0 ) // If there's an expr, visit the expr
        {
            return visit( ctx.expr( 0 ) );
        }

        return visit( ctx.getChild( 0 ) ); // All others: visit first child.
    }

    @Override
    public Object visitNumericLiteral( NumericLiteralContext ctx )
    {
        return expressionLiteral.getNumericLiteral( ctx );
    }

    @Override
    public Object visitStringLiteral( StringLiteralContext ctx )
    {
        return expressionLiteral.getStringLiteral( ctx );
    }

    @Override
    public Object visitBooleanLiteral( BooleanLiteralContext ctx )
    {
        return expressionLiteral.getBooleanLiteral( ctx );
    }

    @Override
    public Object visitTerminal( TerminalNode node )
    {
        return node.getText(); // Needed to regenerate an expression.
    }

    // -------------------------------------------------------------------------
    // Logic for functions and items
    // -------------------------------------------------------------------------

    /**
     * Visits a context and casts the result as Double.
     *
     * @param ctx any context
     * @return the Double value
     */
    public Double castDoubleVisit( ParseTree ctx )
    {
        return castDouble( visit( ctx ) );
    }

    /**
     * Visits a context and casts the result as String.
     *
     * @param ctx any context
     * @return the Double value
     */
    public String castStringVisit( ParseTree ctx )
    {
        return castString( visit( ctx ) );
    }

    /**
     * Visits a context and casts the result as Boolean.
     *
     * @param ctx any context
     * @return the Boolean value
     */
    public Boolean castBooleanVisit( ParseTree ctx )
    {
        return castBoolean( visit( ctx ) );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public void setExpressionLiteral( AntlrExprLiteral expressionLiteral )
    {
        this.expressionLiteral = expressionLiteral;
    }
}
