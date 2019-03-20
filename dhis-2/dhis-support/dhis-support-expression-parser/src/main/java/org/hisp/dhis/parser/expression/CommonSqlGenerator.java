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

import com.google.common.base.Joiner;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringEscapeUtils.escapeSql;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;
import static org.hisp.dhis.parser.expression.ParserUtils.*;

/**
 * Generates SQL for common expression operands and functions.
 *
 * @author Jim Grace
 */
public abstract class CommonSqlGenerator
    extends AbstractVisitor
{
    // -------------------------------------------------------------------------
    // Visitor methods
    // -------------------------------------------------------------------------

    @Override
    public String visitOperator( ExprContext ctx )
    {
        if ( ctx.programNullTest != null )
        {
            return castString( visitIgnoringMissingValues( ctx.item()) )
                + " == " + ctx.programNullTest.getText();
        }

        List<String> args = ctx.expr().stream()
            .map( c -> castStringVisit( c ) )
            .collect( Collectors.toList() );

        switch ( ctx.op.getType() )
        {
            // -----------------------------------------------------------------
            // Parentheses
            // -----------------------------------------------------------------

            case PAREN:
                return "(" + args.get( 0 ) + ")";

            // -----------------------------------------------------------------
            // Arithmetic Functions (return Double)
            // -----------------------------------------------------------------

            case PLUS:
                if ( ctx.expr().size() == 1 ) // Unary plus operator
                {
                    return "+ " + args.get( 0 );
                }
                else // Addition operator
                {
                    return args.get( 0 ) +
                        " + " + args.get( 1 );
                }

            case MINUS:
                if ( ctx.expr().size() == 1 ) // Unary minus operator
                {
                    return "- " + args.get( 0 );
                }
                else // Subtraction operator
                {
                    return args.get( 0 ) +
                        " - " + args.get( 1 );
                }

            case POWER:
                return args.get( 0 ) +
                    " ^ " + args.get( 1 );

            case MUL:
                return args.get( 0 ) +
                    " * " + args.get( 1 );

            case DIV:
                return args.get( 0 ) +
                    " / " + args.get( 1 );

            case MOD:
                return args.get( 0 ) +
                    " % " + args.get( 1 );

            // -----------------------------------------------------------------
            // Boolean functions (return Boolean)
            // -----------------------------------------------------------------

            case NOT:
            case EXCLAMATION_POINT:
                return "not " + args.get( 0 );

            case AND:
            case AMPERSAND_2:
                return args.get( 0 ) +
                    " and " + args.get( 1 );

            case OR:
            case VERTICAL_BAR_2:
                return args.get( 0 ) +
                    " or " + args.get( 1 );

            // -----------------------------------------------------------------
            // Comparison functions (return Boolean)
            // -----------------------------------------------------------------

            case EQ:
                return args.get( 0 ) +
                    " == " + args.get( 1 );

            case NE:
                return args.get( 0 ) +
                    " != " + args.get( 1 );

            case GT:
                return args.get( 0 ) +
                    " > " + args.get( 1 );

            case LT:
                return args.get( 0 ) +
                    " < " + args.get( 1 );

            case GEQ:
                return args.get( 0 ) +
                    " >= " + args.get( 1 );

            case LEQ:
                return args.get( 0 ) +
                    " <= " + args.get( 1 );

            default:
                throw new InternalParserException( "Expecting operator, found " + ctx.op.getText() );
        }
    }

    @Override
    public String visitFunction( FunctionContext ctx )
    {
        List<String> args = ctx.expr().stream()
            .map( c -> castStringVisit( c ) )
            .collect( Collectors.toList() );

        switch ( ctx.fun.getType() )
        {
            case FIRST_NON_NULL:
                List<String> values = ctx.itemNumStringLiteral().stream()
                    .map( c -> castString( getNullTest( c ) ) )
                    .collect( Collectors.toList() );

                return "coalesce(" + Joiner.on( "," ).join( values ) + ")";

            case GREATEST:
                return "greatest(" + Joiner.on( "," ).join( args ) + ")";

            case IF:
                return " case when " + args.get( 0 ) +
                    " then " + args.get( 1 ) +
                    " else " + args.get( 2 ) + " end";

            case IS_NOT_NULL:
                return getNullTest( ctx.itemNumStringLiteral( 0 ) ) + " is not null";

            case IS_NULL:
                return getNullTest( ctx.itemNumStringLiteral( 0 ) ) + " is null";

            case LEAST:
                return "least(" + Joiner.on( "," ).join( args ) + ")";

            default:
                throw new InternalParserException( "Expecting function, found " + ctx.fun.getText() );
        }
    }

    @Override
    public String visitLiteral( LiteralContext ctx )
    {
        if ( ctx.stringLiteral() != null )
        {
            return sqlStringLiteral( ctx.getText() );
        }

        return ctx.getText();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String getNullTest( ItemNumStringLiteralContext ctx )
    {
        if ( ctx.item() != null )
        {
            return castString( visitIgnoringMissingValues( ctx.item() ) );
        }
        else if ( ctx.numStringLiteral().stringLiteral() != null )
        {
            return sqlStringLiteral( ctx.getText() );
        }

        return ctx.getText(); // Numeric
    }

    private String sqlStringLiteral( String s )
    {
        return "'" + escapeSql( unescapeJava( trimQuotes( s ) ) ) + "'";
    }
}
