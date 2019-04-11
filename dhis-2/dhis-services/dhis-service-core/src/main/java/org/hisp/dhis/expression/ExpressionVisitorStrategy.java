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

package org.hisp.dhis.expression;

import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.parser.expression.ParserExceptionWithoutContext;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.hisp.dhis.common.DimensionItemType.*;
import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.A_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.HASH_BRACE;

/**
 * @author Luciano Fiandesio
 */
public interface ExpressionVisitorStrategy
{

    Map<Integer, ExpressionVisitorStrategy> strategyMap = new HashMap<Integer, ExpressionVisitorStrategy>()
    {
        {
            put( HASH_BRACE, hashBraceStrategy() );
            put( A_BRACE, aBraceStrategy() );
        }
    };

    Object resolve(ExpressionVisitorType type, ExpressionParser.ItemContext ctx) ;

    static ExpressionVisitorStrategy hashBraceStrategy()
    {
        return (type, ctx) -> {

            if (type == ExpressionVisitorType.ITEMS) {
                if ( isDataElementOperandSyntax( ctx ) )
                {
                    return getExpressionItem( ctx.getText(),
                            new DimensionalItemId( DATA_ELEMENT_OPERAND,
                                    ctx.uid0.getText(),
                                    ctx.uid1 == null ? null : ctx.uid1.getText(),
                                    ctx.uid2 == null ? null : ctx.uid2.getText() ) );
                }
                else
                {
                    return getExpressionItem( ctx.getText(),
                            new DimensionalItemId( DATA_ELEMENT,
                                    ctx.uid0.getText() ) );
                }
            } else {
                if ( isDataElementOperandSyntax( ctx ) )
                {
                    return getItemValue(
                            ctx.uid0.getText() + "." +
                                    ( ctx.uid1 == null ? "*" : ctx.uid1.getText() ) +
                                    ( ctx.uid2 == null ? "" : "." + ctx.uid2.getText() ) );
                }
                else // Data element:
                {
                    return getItemValue(
                            ctx.uid0.getText() );
                }


            }
        };
    }

    static ExpressionVisitorStrategy aBraceStrategy()
    {

        return (type, ctx) -> {

            if (type == ExpressionVisitorType.ITEMS) {
                if ( !isExpressionProgramAttribute( ctx ) )
                {
                    throw new ParserExceptionWithoutContext( "Program attribute must have two UIDs: " + ctx.getText() );
                }

                return getExpressionItem( ctx.getText(),
                        new DimensionalItemId( PROGRAM_ATTRIBUTE,
                                ctx.uid0.getText(),
                                ctx.uid1.getText() ) );
            } else {
                if ( !isExpressionProgramAttribute( ctx ) )
                {
                    throw new ParserExceptionWithoutContext( "Program attribute must have two UIDs: " + ctx.getText() );
                } else {
                    return getItemValue(
                            ctx.uid0.getText() + "." +
                                    ctx.uid1.getText() );
                }


            }
        };
    }

    static Object getItemValue(String text) {
        return null;
    }

    static Object getExpressionItem(String text, DimensionalItemId dimensionalItemId) {
        return null;
    }

    static ExpressionVisitorStrategy getStrategy(int operand) {
        return strategyMap.get(operand);
    }


}
