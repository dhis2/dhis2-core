package org.hisp.dhis.parser.expression;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ItemContext;

/**
 * Evaluates a parsed expression item.
 *
 * @author Jim Grace
 */
public interface ExprItem
{
    /**
     * Collects the description of an expression item.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return a dummy value for the item (of the right type)
     */
    Object getDescription( ItemContext ctx, CommonExpressionVisitor visitor );

    /**
     * Collects the item id for later database lookup
     * (applies to expression service items).
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return a dummy value for the item
     */
    Object getItemId( ItemContext ctx, CommonExpressionVisitor visitor );

    /**
     * Collects the organisation unit group for which we will need counts
     * (applies to expression service items).
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return a dummy value for the item
     */
    Object getOrgUnitGroup( ItemContext ctx, CommonExpressionVisitor visitor );

    /**
     * Returns the database value of the item
     * (applies to expression service items).
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return a dummy value (of the right type) for the item
     */
    Object evaluate( ItemContext ctx, CommonExpressionVisitor visitor );

    /**
     * Generates SQL for an expression item
     * (applies to Program Indicator items).
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return the generated SQL (as a String) for the function
     */
    Object getSql( ItemContext ctx, CommonExpressionVisitor visitor );

    /**
     * Regenerates the original item syntax from the parse tree,
     * or in some cases substitutes a value if one is present.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return the regenerated expression (as a String) for the function
     */
    Object regenerate( ItemContext ctx, CommonExpressionVisitor visitor );
}
