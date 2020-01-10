package org.hisp.dhis.parser.expression;

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

import org.hisp.dhis.antlr.AntlrExprFunction;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

/**
 * Visits a parsed expression function.
 *
 * @author Jim Grace
 */
public interface ExprFunction
    extends AntlrExprFunction
{
    /**
     * Collects item ids inside the function for later database lookup.
     * This is the same as the evaluate method for most functions, but for
     * aggregation functions, it collects item ids as aggregation item ids.
     *
     * @param ctx     the expression context
     * @param visitor the tree visitor
     * @return a dummy value for the item
     */
    Object getItemId( ExprContext ctx, CommonExpressionVisitor visitor );

    /**
     * Finds the value of an expression function, evaluating all
     * the arguments of logical functions (e.g. if, and, or, firstNonNull).
     * Otherwise, this is the same as the evaluate method.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return the value of the function, evaluating all args
     */
    Object evaluateAllPaths( ExprContext ctx, CommonExpressionVisitor visitor );

    /**
     * Generates SQL for an expression function.
     *
     * @param ctx     the expression context
     * @param visitor the tree visitor
     * @return the generated SQL (as a String) for the function
     */
    Object getSql( ExprContext ctx, CommonExpressionVisitor visitor );
}
