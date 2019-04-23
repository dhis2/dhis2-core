package org.hisp.dhis.program.item;

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

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.parser.expression.ExprItem;
import org.hisp.dhis.parser.expression.ExprVisitor;
import org.hisp.dhis.parser.expression.ParserExceptionWithoutContext;
import org.hisp.dhis.program.ProgramIndicatorExprVisitor;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ItemContext;

/**
 * Program indicator expression item
 *
 * @author Jim Grace
 */
public abstract class ProgramItem
    implements ExprItem
{
    @Override
    public final Object getDescription( ItemContext ctx, ExprVisitor visitor )
    {
        return getDescription( ctx, (ProgramIndicatorExprVisitor) visitor );
    }

    @Override
    public final Object getItemId( ItemContext ctx, ExprVisitor visitor )
    {
        throw new ParserExceptionWithoutContext( "Internal parsing error: getItemId called for program indicator item" );
    }

    @Override
    public final Object getOrgUnitGroup( ItemContext ctx, ExprVisitor visitor )
    {
        throw new ParserExceptionWithoutContext( "Internal parsing error: getOrgUnitGroup called for program indicator item" );
    }

    @Override
    public final Object evaluate( ItemContext ctx, ExprVisitor visitor )
    {
        throw new ParserExceptionWithoutContext( "Internal parsing error: evaluate called for program indicator item" );
    }

    @Override
    public Object getSql( ItemContext ctx, ExprVisitor visitor )
    {
        return getSql( ctx, (ProgramIndicatorExprVisitor) visitor );
    }

    /**
     * Collects the description of a program indicator expression item.
     *
     * @param ctx the expression context
     * @param visitor the program indicator expression tree visitor
     * @return a dummy value for the item (of the right type)
     */
    public abstract Object getDescription( ItemContext ctx, ProgramIndicatorExprVisitor visitor );

    /**
     * Generates SQL for a program indicator expression item.
     *
     * @param ctx the expression context
     * @param visitor the tree visitor
     * @return the generated SQL (as a String) for the function
     */
    public abstract Object getSql( ItemContext ctx, ProgramIndicatorExprVisitor visitor );

    /**
     * Replace null values with 0 or ''.
     *
     * @param column the column (may be a subquery)
     * @param valueType the type of value that might be null
     * @return SQL to replace a null value with 0 or '' depending on type
     */
    protected String replaceNullValues( String column, ValueType valueType )
    {
        return valueType.isNumeric() || valueType.isBoolean()
            ? "coalesce(" + column + "::numeric,0)"
            : "coalesce(" + column + ",'')";
    }


}
