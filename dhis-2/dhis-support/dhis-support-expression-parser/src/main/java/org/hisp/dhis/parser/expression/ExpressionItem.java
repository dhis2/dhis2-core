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
package org.hisp.dhis.parser.expression;

import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_DOUBLE_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.hisp.dhis.antlr.AntlrExprItem;
import org.hisp.dhis.antlr.AntlrExpressionVisitor;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;

/**
 * A parsed item from an ANTLR expression, as processed in the backend.
 *
 * @author Jim Grace
 */
public interface ExpressionItem
    extends AntlrExprItem
{
    ExpressionItemMethod ITEM_GET_DESCRIPTIONS = ExpressionItem::getDescription;

    ExpressionItemMethod ITEM_GET_EXPRESSION_INFO = ExpressionItem::getExpressionInfo;

    ExpressionItemMethod ITEM_EVALUATE = ExpressionItem::evaluate;

    ExpressionItemMethod ITEM_GET_SQL = ExpressionItem::getSql;

    /**
     * Collects the description of an individual data item, to use later in
     * constructing a description of the expression as a whole.
     * <p>
     * This method only needs to be overridden for items that have UIDs that
     * need to be translated into human-readable object names.
     * <p>
     * For other items, evaluate all paths to be sure that we collect the
     * description of any items that may be within this expression.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return a dummy value for the item (of the right type, for type checking)
     */
    default Object getDescription( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        return evaluateAllPaths( ctx, visitor );
    }

    /**
     * Collects the information we need from an expression such as the ids of
     * metadata items for which we will need to find the metadata, and then (at
     * least for some items) the corresponding data.
     * <p>
     * This method only needs to be overridden for items that have UIDs that
     * need to be looked up in the database.
     * <p>
     * For other items, evaluate all paths to be sure that we collect the UIDs
     * of any items that may be within this expression. But don't return null
     * from this function, to make sure that no part of the expression is
     * skipped.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return a dummy value for the item
     */
    default Object getExpressionInfo( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        Object value = evaluateAllPaths( ctx, visitor );

        return value == null ? DEFAULT_DOUBLE_VALUE : value;
    }

    /**
     * Returns the value of the expression item. Also used for syntax checking.
     * (For program indicator-only items, this may return a dummy value because
     * the real evaluation is done in SQL.)
     * <p>
     * For the lower-level Antlr... items, this method calls the lower-level
     * evaluate routine.
     * <p>
     * For all other items, this method must be overridden.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return a dummy value (of the right type) for the item
     */
    default Object evaluate( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        return evaluate( ctx, (AntlrExpressionVisitor) visitor );
    }

    /**
     * Provides a default implementation for the lower-level Antlr... evaluate
     * method.
     * <p>
     * The lower-level Antlr... items must provide this method.
     * <p>
     * If a higher-level item does not override the method evaluate(...
     * CommonExpressionVisitor ...) then this default implementation will be
     * called, resulting in an exception.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return a dummy value (of the right type) for the item
     */
    @Override
    default Object evaluate( ExprContext ctx, AntlrExpressionVisitor visitor )
    {
        throw new ParserExceptionWithoutContext( "evaluate not implemented for " + ctx.getText() );
    }

    /**
     * Finds the value of an expression function, evaluating all the arguments
     * of logical functions that might not always evaluate all arguments based
     * on the truth value of some arguments (e.g. if, and, or, firstNonNull).
     * <p>
     * For those few logical functions that may not normally evaluate all
     * arguments, this method must be overridden.
     * <p>
     * For other items, this method does not need to be overridden.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return the value of the function, evaluating all args
     */
    default Object evaluateAllPaths( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        return evaluate( ctx, visitor );
    }

    /**
     * Generates the SQL for a program indicator expression item.
     * <p>
     * This method must be overridden for all items used in program indicator
     * expressions, otherwise an exception will be thrown.
     * <p>
     * For other items, this method does not need to be overridden.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return the generated SQL (as a String) for the function
     */
    default Object getSql( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        throw new ParserExceptionWithoutContext( "Not valid in this context: " + ctx.getText() );
    }
}
