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

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.pow;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;
import static org.hisp.dhis.parser.expression.ParserUtils.*;

/**
 * Common resolution of expression operands and functions using the ANTLR4
 * visitor pattern.
 *
 * @author Jim Grace
 */
public abstract class CommonVisitor
    extends AbstractVisitor
{
    // -------------------------------------------------------------------------
    // Visitor methods
    // -------------------------------------------------------------------------

    @Override
    public Object visitOperator( ExprContext ctx )
    {
        switch ( ctx.op.getType() )
        {
            // -----------------------------------------------------------------
            // Parentheses
            // -----------------------------------------------------------------

            case PAREN:
                return visit( ctx.expr( 0 ) );

            // -----------------------------------------------------------------
            // Arithmetic operators (return Double)
            // -----------------------------------------------------------------

            case PLUS:
                if ( ctx.expr().size() == 1 ) // Unary plus operator
                {
                    return castDoubleVisit( ctx.expr( 0 ) );
                }
                else // Addition operator
                {
                    return castDoubleVisit( ctx.expr( 0 ) )
                        + castDoubleVisit( ctx.expr( 1 ) );
                }

            case MINUS:
                if ( ctx.expr().size() == 1 ) // Unary minus operator
                {
                    return - castDoubleVisit( ctx.expr( 0 ) );
                }
                else // Subtraction operator
                {
                    return castDoubleVisit( ctx.expr( 0 ) )
                        - castDoubleVisit( ctx.expr( 1 ) );
                }

            case POWER:
                return pow( castDoubleVisit( ctx.expr( 0 ) ),
                        castDoubleVisit( ctx.expr( 1 ) ) );

            case MUL:
                return castDoubleVisit( ctx.expr( 0 ) )
                    * castDoubleVisit( ctx.expr( 1 ) );

            case DIV:
                return castDoubleVisit( ctx.expr( 0 ) )
                    / castDoubleVisit( ctx.expr( 1 ) );

            case MOD:
                return castDoubleVisit( ctx.expr( 0 ) )
                    % castDoubleVisit( ctx.expr( 1 ) );

            // -----------------------------------------------------------------
            // Boolean operators (return Boolean)
            // -----------------------------------------------------------------

            case NOT:
            case EXCLAMATION_POINT:
                return ! castBooleanVisit( ctx.expr( 0 ) );

            case AND:
            case AMPERSAND_2:
                return andOperator( ctx );

            case OR:
            case VERTICAL_BAR_2:
                return orOperator( ctx );

            // -----------------------------------------------------------------
            // Comparison operators (return Boolean)
            // -----------------------------------------------------------------

            case EQ:
                return compare( ctx ) == 0;

            case NE:
                return compare( ctx ) != 0;

            case GT:
                return compare( ctx ) > 0;

            case LT:
                return compare( ctx ) < 0;

            case GEQ:
                return compare( ctx ) >= 0;

            case LEQ:
                return compare( ctx ) <= 0;

            default:
                throw new InternalParserException( "Expecting operator, found " + ctx.op.getText() );
        }
    }

    @Override
    public Object visitFunction( FunctionContext ctx )
    {
        switch ( ctx.fun.getType() )
        {
            case FIRST_NON_NULL:
                return firstNonNull( ctx.itemNumStringLiteral() );

            case GREATEST:
                return greatestOrLeast( ctx.expr(), 1.0 );

            case IF:
                return ifFunction( ctx );

            case IS_NOT_NULL:
                return visitAllowingNullValues( ctx.item() ) != null;

            case IS_NULL:
                return visitAllowingNullValues( ctx.item() ) == null;

            case LEAST:
                return greatestOrLeast( ctx.expr(), -1.0 );

            default:
                throw new InternalParserException( "Expecting function, found " + ctx.fun.getText() );
        }
    }

    // -------------------------------------------------------------------------
    // Protected methods
    // -------------------------------------------------------------------------

    /**
     * Returns logical and of two boolean values, pre-fetching both arguments
     * first. May be overridden if the second argument should be evaluated only
     * when the first argument is true.
     *
     * @param ctx expr context
     * @return the logical and value
     */
    protected Object andOperator( ExprContext ctx )
    {
        boolean arg0 = castBooleanVisit( ctx.expr( 0 ) );
        boolean arg1 = castBooleanVisit( ctx.expr( 1 ) );

        return arg0 && arg1;
    }

    /**
     * Returns logical or of two boolean values, pre-fetching both arguments
     * first. May be overridden if the second argument should be evaluated only
     * when the first argument is false.
     *
     * @param ctx expr context
     * @return the logical or value
     */
    protected Object orOperator( ExprContext ctx )
    {
        boolean arg0 = castBooleanVisit( ctx.expr( 0 ) );
        boolean arg1 = castBooleanVisit( ctx.expr( 1 ) );

        return arg0 || arg1;
    }

    /**
     * Evaluates if function, returning expr(1) or expr(2) depending on the
     * value of expr(0). Prefetches expr(1) or expr(2). May be overridden if
     * only one possible return values evaluated depending on the first expr.
     *
     * @param ctx function context
     * @return either expr(1) or expr(2)
     */
    protected Object ifFunction( FunctionContext ctx )
    {
        boolean arg0 = castBooleanVisit( ctx.expr( 0 ) );
        Object arg1 = visit( ctx.expr( 1 ) );
        Object arg2 = visit( ctx.expr( 2 ) );

        if ( arg1 != null )
        {
            castClass( arg1.getClass(), arg2 );
        }

        return arg0 ? arg1 : arg2;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns the first non-null value.
     *
     * @param contexts the item or literal value contexts.
     * @return the first non-null value.
     */
    protected Object firstNonNull( List<ItemNumStringLiteralContext> contexts )
    {
        List<Object> values = contexts.stream()
            .map( c -> getItemNumStringLiteral( c ) )
            .collect( Collectors.toList() );

        for ( Object val : values )
        {
            if ( val != null )
            {
                return val;
            }
        }
        return null;
    }

    private Object getItemNumStringLiteral( ItemNumStringLiteralContext ctx )
    {
        if ( ctx.item() != null )
        {
            return visitAllowingNullValues( ctx.item() );
        }
        else if ( ctx.numStringLiteral().stringLiteral() != null )
        {
            return unescapeJava( trimQuotes( ctx.getText() ) );
        }

        return ctx.getText();
    }

    /**
     * Returns the greatest or least value.
     *
     * @param contexts the expr contexts.
     * @param greatestLeast 1.0 for greatest, -1.0 for least.
     * @return the greatest or least value.
     */
    private Double greatestOrLeast( List<ExprContext> contexts, double greatestLeast )
    {
        List<Double> values = contexts.stream()
            .map( c -> castDouble( visit( c ) ) )
            .collect( Collectors.toList() );

        Double returnVal = null;

        for ( Double val : values )
        {
            if ( returnVal == null || val != null && ( val - returnVal ) * greatestLeast > 0 )
            {
                returnVal = val;
            }
        }
        return returnVal;
    }

    /**
     * Compares two Doubles, Strings or Booleans.
     *
     * @param ctx expr context
     * @return the results of the comparision.
     */
    private int compare( ExprContext ctx )
    {
        Object o1 = visit( ctx.expr( 0 ) );
        Object o2 = visit( ctx.expr( 1 ) );

        if ( o1 == null || o2 == null )
        {
            throw new InternalParserException( "found null when comparing '" + o1 + "' with '" + o2 + "'" );
        }
        else if ( o1 instanceof Double )
        {
            return ( (Double) o1).compareTo( castDouble( o2 ) );
        }
        else if ( o1 instanceof String )
        {
            return ( (String) o1).compareTo( castString( o2 ) );
        }
        else if ( o1 instanceof Boolean )
        {
            return ( (Boolean) o1).compareTo( castBoolean( o2 ) );
        }
        else
        {
            throw new InternalParserException( "trying to compare class " + o1.getClass().getName() );
        }
    }
}
