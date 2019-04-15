package org.hisp.dhis.expression.item;

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

import org.hisp.dhis.expression.ExpressionExprVisitor;
import org.hisp.dhis.parser.expression.ExprItem;
import org.hisp.dhis.parser.expression.ExprVisitor;
import org.hisp.dhis.parser.expression.ParserExceptionWithoutContext;

import static org.hisp.dhis.parser.expression.ParserUtils.DOUBLE_VALUE_IF_NULL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ItemContext;

/**
 * Expression item
 *
 * @author Jim Grace
 */
public abstract class AbstractExpressionItem
    implements ExprItem
{
    @Override
    public final Object getDescriptions( ItemContext ctx, ExprVisitor visitor )
    {
        return getDescriptions( ctx, (ExpressionExprVisitor) visitor );
    }

    @Override
    public final Object getItemIds( ItemContext ctx, ExprVisitor visitor )
    {
        return getItemIds( ctx, (ExpressionExprVisitor) visitor );
    }

    @Override
    public final Object getOrgUnitGroups( ItemContext ctx, ExprVisitor visitor )
    {
        return getOrgUnitGroups( ctx, (ExpressionExprVisitor) visitor );
    }

    @Override
    public final Object evaluate( ItemContext ctx, ExprVisitor visitor )
    {
        return evaluate( ctx, (ExpressionExprVisitor) visitor );
    }

    @Override
    public Object getSql( ItemContext ctx, ExprVisitor visitor )
    {
        throw new ParserExceptionWithoutContext( "Internal parsing error: getSql called for expression item" );
    }

    public abstract Object getDescriptions( ItemContext ctx, ExpressionExprVisitor visitor );

    public Object getItemIds( ItemContext ctx, ExpressionExprVisitor visitor )
    {
        return DOUBLE_VALUE_IF_NULL;
    }

    public Object getOrgUnitGroups( ItemContext ctx, ExpressionExprVisitor visitor )
    {
        return DOUBLE_VALUE_IF_NULL;
    }

    public abstract Object evaluate( ItemContext ctx, ExpressionExprVisitor visitor );
}
