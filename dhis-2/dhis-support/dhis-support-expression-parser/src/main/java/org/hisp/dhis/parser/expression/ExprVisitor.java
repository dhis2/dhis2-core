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
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.parser.expression.antlr.ExpressionBaseVisitor;
import org.hisp.dhis.parser.expression.literal.DefaultLiteral;

import java.util.*;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;
import static org.hisp.dhis.parser.expression.ParserUtils.*;

/**
 * Common traversal of the ANTLR4 expression parse tree using the
 * visitor pattern.
 *
 * @author Jim Grace
 */
public abstract class ExprVisitor
    extends ExpressionBaseVisitor<Object>
{
    protected ConstantService constantService;

    /**
     * Map of ExprFunction instances to call for each expression function
     */
    protected Map<Integer, ExprFunction> functionMap;

    /**
     * Map of ExprItem instances to call for each expression item
     */
    protected Map<Integer, ExprItem> itemMap;

    /**
     * Method to call within the ExprFunction instance
     */
    protected ExprFunctionMethod functionMethod;

    /**
     * Method to call within the ExprItem instance
     */
    protected ExprItemMethod itemMethod;

    /**
     * Instance to call for each literal
     */
    protected ExprLiteral expressionLiteral = new DefaultLiteral();

    /**
     * By default, replace nulls with 0 or ''.
     */
    protected boolean replaceNulls = true;

    /**
     * Used to collect the string replacements to build a description.
     */
    protected Map<String, String> itemDescriptions = new HashMap<>();

    /**
     * Constants to use in evaluating an expression.
     */
    Map<String, Double> constantMap = new HashMap<>();

    // -------------------------------------------------------------------------
    // Visitor methods
    // -------------------------------------------------------------------------

    @Override
    public final Object visitExpression( ExpressionContext ctx )
    {
        return visit( ctx.expr() );
    }

    @Override
    public Object visitExpr( ExprContext ctx )
    {
        if ( ctx.fun == null )
        {
            if ( ctx.expr().size() > 0 ) // There's an expr: visit the expr
            {
                return visit( ctx.expr( 0 ) );
            }

            return visit( ctx.getChild( 0 ) ); // All others
        }

        ExprFunction function = functionMap.get( ctx.fun.getType() );

        if ( function == null )
        {
            throw new ParserExceptionWithoutContext( "Function " + ctx.fun.getText() + " not supported for this type of expression" );
        }

        return functionMethod.apply( function, ctx, this );
    }

    @Override
    public Object visitItem( ItemContext ctx )
    {
        ExprItem item = itemMap.get( ctx.it.getType() );

        if ( item == null )
        {
            throw new ParserExceptionWithoutContext( "Item " + ctx.it.getText() + " not supported for this type of expression" );
        }

        return itemMethod.apply( item, ctx, this );
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

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public ConstantService getConstantService()
    {
        return constantService;
    }

    public Map<String, String> getItemDescriptions()
    {
        return itemDescriptions;
    }

    public Map<String, Double> getConstantMap()
    {
        return constantMap;
    }

    public void setConstantMap( Map<String, Double> constantMap )
    {
        this.constantMap = constantMap;
    }

    public boolean getReplaceNulls()
    {
        return replaceNulls;
    }

    public void setExpressionLiteral( ExprLiteral expressionLiteral )
    {
        this.expressionLiteral = expressionLiteral;
    }

    // -------------------------------------------------------------------------
    // Convenience methods for functions and items
    // -------------------------------------------------------------------------

    /**
     * Visits a context and casts the result as Double.
     *
     * @param ctx any context
     * @return the Double value
     */
    public Double castDoubleVisit( ParserRuleContext ctx )
    {
        return castDouble( visit( ctx ) );
    }

    /**
     * Visits a context and casts the result as String.
     *
     * @param ctx any context
     * @return the Double value
     */
    public String castStringVisit( ParserRuleContext ctx )
    {
        return castString( visit( ctx ) );
    }

    /**
     * Visits a context and casts the result as Boolean.
     *
     * @param ctx any context
     * @return the Boolean value
     */
    public Boolean castBooleanVisit( ParserRuleContext ctx )
    {
        return castBoolean( visit( ctx ) );
    }

    /**
     * Visits a context while allowing null values (not replacing them
     * with 0 or ''), even if we would otherwise be replacing them.
     *
     * @param ctx any context
     * @return the value while allowing nulls
     */
    public Object visitAllowingNulls( ParserRuleContext ctx )
    {
        boolean savedReplaceNulls = replaceNulls;

        replaceNulls = false;

        Object result = visit( ctx );

        replaceNulls = savedReplaceNulls;

        return result;
    }

    /**
     * Gets the value of an item or numeric string literal
     *
     * If an item, gets the value while allowing nulls.
     *
     * @param ctx item or numeric string literal context
     * @return the value of the item or numeric string literal
     */
    public Object getItemNumStringLiteral( ItemNumStringLiteralContext ctx )
    {
        if ( ctx.item() != null )
        {
            return visitAllowingNulls( ctx.item() );
        }
        else if ( ctx.numStringLiteral().stringLiteral() != null )
        {
            return expressionLiteral.getStringLiteral( ctx.numStringLiteral().stringLiteral() );
        }

        return ctx.getText();
    }
}
