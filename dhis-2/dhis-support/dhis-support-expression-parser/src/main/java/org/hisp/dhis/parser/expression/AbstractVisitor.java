package org.hisp.dhis.parser.expression;

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

import org.antlr.v4.runtime.ParserRuleContext;
import org.hisp.dhis.parser.expression.antlr.ExpressionBaseVisitor;

import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;

/**
 * Abstraction over the ANTLR-generated ExpressionBaseVisitor, to use in
 * the visitor pattern for the Expression grammar.
 * <p/>
 * Performs basic parsing of the expr label, and separates out the operator
 * processing for easy access by subclasses. (Operator parsing must be included
 * in expr grammar because of left recursive parsing constraints.)
 * <p/>
 * Throws errors for visiting various types unless overridden by subclasses.
 * <p/>
 * Provides basic literal value parsing (which may be overridden by
 * subclasses when needed.)
 *
 * @author Jim Grace
 */
public abstract class AbstractVisitor
    extends ExpressionBaseVisitor<Object>
{
    /**
     * By default, ignore missing values: change to 0 or ''.
     */
    protected boolean ignoreMissingValues = true;

    // -------------------------------------------------------------------------
    // Visitor methods not to be overridden by subclasses
    // -------------------------------------------------------------------------

    @Override
    public final Object visitExpression( ExpressionContext ctx )
    {
        return visit( ctx.expr() );
    }

    @Override
    public final Object visitExpr( ExprContext ctx )
    {
        if ( ctx.op != null )
        {
            return visitOperator( ctx );
        }
        else if ( ctx.expr().size() > 0 ) // There's an expr: visit the expr
        {
            return visit( ctx.expr( 0 ) );
        }

        return visit( ctx.getChild( 0 ) ); // All others
    }

    // -------------------------------------------------------------------------
    // Visitor methods to be overridden by subclasses as needed
    // -------------------------------------------------------------------------

    /**
     * Visit an operator. This method is defined here, but may be overridden
     * by subclasses in the same way as the visitor methods overridden here.
     *
     * @param ctx the expresison context
     * @return the operator return value (throws an exception for now)
     */
    public Object visitOperator( ExprContext ctx )
    {
        throw new ParserExceptionWithoutContext( "Operator not valid in this expression" );
    }

    @Override
    public Object visitFunction( FunctionContext ctx )
    {
        throw new ParserExceptionWithoutContext( "Function not valid in this expression" );
    }

    @Override
    public Object visitItem( ItemContext ctx )
    {
        throw new ParserExceptionWithoutContext( "Item not valid in this expression" );
    }

    @Override
    public Object visitProgramVariable( ProgramVariableContext ctx )
    {
        throw new ParserExceptionWithoutContext( "Program variable not valid in this expression" );
    }

    @Override
    public Object visitProgramFunction( ProgramFunctionContext ctx )
    {
        throw new ParserExceptionWithoutContext( "Program function not valid in this expression" );
    }

    @Override
    public Object visitLiteral( LiteralContext ctx )
    {
        if ( ctx.numericLiteral() != null )
        {
            return Double.valueOf( ctx.getText() );
        }
        else if ( ctx.stringLiteral() != null )
        {
            return unescapeJava( trimQuotes( ctx.getText() ) );
        }
        else
        {
            return Boolean.valueOf( ctx.getText() );
        }
    }

    // -------------------------------------------------------------------------
    // Convenience methods for subclasses
    // -------------------------------------------------------------------------

    /**
     * Visits a context and casts the result as Double.
     *
     * @param ctx any context
     * @return the Double value
     */
    protected Double castDoubleVisit( ParserRuleContext ctx )
    {
        return castDouble( visit( ctx ) );
    }

    /**
     * Visits a context and casts the result as String.
     *
     * @param ctx any context
     * @return the Double value
     */
    protected String castStringVisit( ParserRuleContext ctx )
    {
        return castString( visit( ctx ) );
    }

    /**
     * Visits a context and casts the result as Boolean.
     *
     * @param ctx any context
     * @return the Boolean value
     */
    protected Boolean castBooleanVisit( ParserRuleContext ctx )
    {
        return castBoolean( visit( ctx ) );
    }

    /**
     * Visits a context while ignoring missing values (replacing them with
     * 0 or ''), even if we would otherwise not be ignoring them.
     *
     * @param ctx any context
     * @return the value while ignoring missing values
     */
    public Object visitIgnoringMissingValues( ParserRuleContext ctx )
    {
        return visitIgnoreMissingValues( ctx, true );
    }

    /**
     * Visits a context while allowing missing values to be returned as null
     * (not replacing them with 0 or ''), even if we would otherwise be
     * replacing them.
     *
     * @param ctx any context
     * @return the value while ignoring missing values
     */
    public Object visitAllowingNullValues( ParserRuleContext ctx )
    {
        return visitIgnoreMissingValues( ctx, false );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Object visitIgnoreMissingValues( ParserRuleContext ctx, boolean tempIgnoreMissingValues )
    {
        boolean savedIgnoreMissingValues = ignoreMissingValues;

        ignoreMissingValues = tempIgnoreMissingValues;

        Object result = visit( ctx );

        ignoreMissingValues = savedIgnoreMissingValues;

        return result;
    }
}
