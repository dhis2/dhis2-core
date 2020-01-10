package org.hisp.dhis.parser.expression.item;

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

import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;

import static org.hisp.dhis.parser.expression.ParserUtils.DOUBLE_VALUE_IF_NULL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ItemContext;

/**
 * Expression item Constant
 *
 * @author Jim Grace
 */
public class ItemConstant
    extends BaseExprItem
{
    @Override
    public Object getDescription( ItemContext ctx, CommonExpressionVisitor visitor )
    {
        Constant constant = visitor.getConstantMap().get( ctx.uid0.getText() );

        if ( constant == null )
        {
            throw new ParserExceptionWithoutContext( "No constant defined for " + ctx.uid0.getText() );
        }

        visitor.getItemDescriptions().put( ctx.getText(), constant.getDisplayName() );

        return DOUBLE_VALUE_IF_NULL;
    }

    @Override
    public Object getItemId( ItemContext ctx, CommonExpressionVisitor visitor )
    {
        return DOUBLE_VALUE_IF_NULL;
    };

    @Override
    public Object getOrgUnitGroup( ItemContext ctx, CommonExpressionVisitor visitor )
    {
        return DOUBLE_VALUE_IF_NULL;
    };

    @Override
    public Object evaluate( ItemContext ctx, CommonExpressionVisitor visitor )
    {
        Constant constant = visitor.getConstantMap().get( ctx.uid0.getText() );

        if ( constant == null ) // Shouldn't happen for a valid expression.
        {
            throw new ParserExceptionWithoutContext( "Can't find constant to evaluate " + ctx.uid0.getText() );
        }

        return constant.getValue();
    }

    @Override
    public Object getSql( ItemContext ctx, CommonExpressionVisitor visitor )
    {
        Constant constant = visitor.getConstantMap().get( ctx.uid0.getText() );

        if ( constant == null )
        {
            throw new ParserExceptionWithoutContext( "Can't find constant for SQL " + ctx.uid0.getText() );
        }

        return Double.valueOf( constant.getValue() ).toString();
    }

    @Override
    public Object regenerate( ItemContext ctx, CommonExpressionVisitor visitor )
    {
        Constant constant = visitor.getConstantMap().get( ctx.uid0.getText() );

        if ( constant == null )
        {
            return ctx.getText();
        }

        return Double.valueOf( constant.getValue() ).toString();
    }
}
